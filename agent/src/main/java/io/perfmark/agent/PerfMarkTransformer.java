/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.perfmark.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class PerfMarkTransformer implements ClassFileTransformer {

  private static final String SRC_OWNER = "io/perfmark/PerfMark";

  /**
   * Methods that should be tagged with a marker after they have been invoked.
   */
  private static final Set<String> POST_TAG =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("startTask", "traceTask")));

  /**
   * Methods that should be tagged with a marker before they have been invoked.
   */
  private static final Set<String> PRE_TAG =
      Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("stopTask")));

  @Override
  public byte[] transform(
      ClassLoader loader,
      final String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    List<String> meth1 =
        Arrays.asList("io.perfmark.agent.PerfMarkTransformerTest$ClzAutoRecord", "recordMe");
    return transform(className, Collections.singletonList(meth1), classfileBuffer);
  }

  private static byte[] transform(
      String className, List<List<String>> methodsToAnnotate, byte[] classfileBuffer) {
    ClassReader cr = new ClassReader(classfileBuffer);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cr.accept(new PerfMarkRewriter(Opcodes.ASM8, cw, className), ClassReader.SKIP_FRAMES);
    return cw.toByteArray();
  }

  static final class PerfMarkRewriter extends ClassVisitor {

    private final String className;
    String fileName;
    int fieldId;

    PerfMarkRewriter(int api, ClassVisitor writer, String className) {
      super(api, writer);
      this.className = className;
      this.fileName = deriveFileName(className);
    }

    @Override
    public void visitSource(String sourceFileName, String debug) {
      this.fileName = sourceFileName;
      super.visitSource(sourceFileName, debug);
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      return new PerfMarkMethodVisitor(
          name, super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    private final class PerfMarkMethodVisitor extends MethodVisitor {
      private final String methodName;

      private int lineNumber = -1;

      PerfMarkMethodVisitor(String methodName, MethodVisitor delegate) {
        super(PerfMarkRewriter.this.api, delegate);
        this.methodName = methodName;
      }

      @Override
      public void visitLineNumber(int line, Label start) {
        this.lineNumber = line;
        super.visitLineNumber(line, start);
      }

      @Override
      public void visitMethodInsn(
          int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (owner.equals(SRC_OWNER) && PRE_TAG.contains(name)) {
          String tag =
              new StackTraceElement(className, methodName, fileName, lineNumber).toString();
          visitLdcInsn("PerfMark.stopCallSite");
          visitLdcInsn(tag);
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              SRC_OWNER,
              "attachTag",
              "(Ljava/lang/String;Ljava/lang/String;)V",
              false);
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        if (owner.equals(SRC_OWNER) && POST_TAG.contains(name)) {
          String tag =
              new StackTraceElement(className, methodName, fileName, lineNumber).toString();
          visitLdcInsn("PerfMark.startCallSite");
          visitLdcInsn(tag);
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              SRC_OWNER,
              "attachTag",
              "(Ljava/lang/String;Ljava/lang/String;)V",
              false);
        }
      }
    }
  }

  static String deriveFileName(String className) {
    String clzName = className.replace('/', '.');
    int dollar = clzName.indexOf('$');
    String fileName;
    if (dollar == -1) {
      fileName = clzName;
    } else {
      fileName = clzName.substring(0, dollar);
    }
    if (!fileName.isEmpty()) {
      int dot = fileName.lastIndexOf('.');
      if (dot != -1) {
        fileName = fileName.substring(dot + 1);
      }
    }
    // TODO: this is broken for private top level classes.
    if (!fileName.isEmpty()) {
      fileName += ".java";
    } else {
      fileName = null;
    }
    return fileName;
  }

  PerfMarkTransformer() {}
}
