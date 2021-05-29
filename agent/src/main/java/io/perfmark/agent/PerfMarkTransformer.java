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
import org.objectweb.asm.AnnotationVisitor;
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
  private static final String[] POST_TAG = new String [] {"startTask", "traceTask"};

  /**
   * Methods that should be tagged with a marker before they have been invoked.
   */
  private static final String[] PRE_TAG = new String[] {"stopTask"};

  @Override
  public byte[] transform(
      ClassLoader loader,
      final String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    //try (TaskCloseable ignored = PerfMark.traceTask("PerfMarkTransformer.transform")) {
    //  PerfMark.attachTag("classname", className);
    //   PerfMark.attachTag("size", classfileBuffer.length);
      return transformInternal(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    //}
  }

  byte[] transformInternal(
      ClassLoader loader,
      final String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    return transform(className, classfileBuffer);
  }

  private static byte[] transform(String className, byte[] classfileBuffer) {
    ClassReader cr = new ClassReader(classfileBuffer);
    ChangedState changed = new ChangedState();
    int api = Opcodes.ASM8;
    cr.accept(
        new PerfMarkRewriter(changed, false, api, null, className),
        ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    if (changed.changed) {
      ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS) {
        @Override
        protected String getCommonSuperClass(String type1, String type2) {
          throw new UnsupportedOperationException("can't reflectively look up classes");
        }
      };
      cr.accept(new PerfMarkRewriter(changed, true, api, cw, className), 0);
      return cw.toByteArray();
    }
    return null;
  }

  static final class PerfMarkRewriter extends ClassVisitor {

    private final String className;
    final ChangedState changed;
    final boolean keepGoing;
    String fileName;

    PerfMarkRewriter(ChangedState changed, boolean keepGoing, int api, ClassVisitor writer, String className) {
      super(api, writer);
      this.className = className;
      this.fileName = deriveFileName(className);
      this.changed = changed;
      this.keepGoing = keepGoing;
    }

    @Override
    public void visitSource(String sourceFileName, String debug) {
      this.fileName = sourceFileName;
      super.visitSource(sourceFileName, debug);
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      if (changed.changed && !keepGoing) {
        return null;
      }
      if (className.equals("io.perfmark.TaskCloseable") && name.equals("close") && descriptor.equals("()V")) {
        return null;
      }
      return new PerfMarkMethodVisitor(
          name, super.visitMethod(access, name+"$perfmark", descriptor, signature, exceptions));
    }

    private final class PerfMarkInstrumentedMethodVisitor extends MethodVisitor {
      private final String methodName;
/*
* A visitor to visit a Java method. The methods of this class must be called in the following order:
* ( visitParameter )*
* [ visitAnnotationDefault ]
* ( visitAnnotation | visitAnnotableParameterCount | visitParameterAnnotation visitTypeAnnotation | visitAttribute )*
* [ visitCode (
*     visitFrame | visit<i>X</i>Insn | visitLabel | visitInsnAnnotation | visitTryCatchBlock
*         | visitTryCatchAnnotation | visitLocalVariable | visitLocalVariableAnnotation | visitLineNumber )*
*     visitMaxs ]
* visitEnd.
*
*
* In addition, the visit<i>X</i>Insn and visitLabel methods must be called in the sequential order of the bytecode
* instructions of the visited code, visitInsnAnnotation must be called after the annotated instruction,
*  visitTryCatchBlock must be called before the labels passed as arguments have been visited,
* visitTryCatchBlockAnnotation must be called after the corresponding try catch block has been visited, and the
* visitLocalVariable, visitLocalVariableAnnotation and visitLineNumber methods must be called after the labels passed
* as arguments have been visited.
*/
      PerfMarkInstrumentedMethodVisitor(String methodName, MethodVisitor delegate) {
        // This doesn't match the overall api level since it depends on the method order of MethodVisitor
        super(Opcodes.ASM9, delegate);
        this.methodName = methodName;
      }

      int parameters;
      String[] parameterNames = new String[2];
      int[] parameterAccesses = new int[2];

      @Override
      public void visitParameter(String name, int access) {
        if (parameterNames.length == parameters) {
          String[] parameterNames = new String[1 + this.parameterNames.length * 2];
          System.arraycopy(this.parameterNames, 0, parameterNames, 0, parameters);
          this.parameterNames = parameterNames;

          int[] parameterAccesses = new int[1+this.parameterAccesses.length * 2];
          System.arraycopy(this.parameterAccesses, 0, parameterAccesses, 0, parameters);
          this.parameterAccesses = parameterAccesses;
        }
        parameterNames[parameters] = name;
        parameterAccesses[parameters] = access;
        parameters++;
      }

      @Override
      public AnnotationVisitor visitAnnotationDefault() {
        return new AnnotationVisitor(2) {

        };
      }

      @Override
      public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        super.visitAnnotableParameterCount(parameterCount, visible);
      }

      @Override
      public void visitCode() {
        super.visitCode();
      }
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
        if (changed.changed && !keepGoing) {
          return;
        }
        if ((owner.equals(SRC_OWNER) && contains(PRE_TAG, name))
              || (owner.equals("io/perfmark/TaskCloseable") && name.equals("close"))) {
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
          changed.changed = true;
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        if (owner.equals(SRC_OWNER) && contains(POST_TAG, name)) {
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
          changed.changed = true;
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

  private static final class ChangedState {
    boolean changed;
  }

  // Avoid pulling in Collections classes, which makes class transforms harder.
  private static boolean contains(String[] haystack, String needle) {
    for (String item : haystack) {
      if (item.equals(needle)) {
        return true;
      }
    }
    return false;
  }

  PerfMarkTransformer() {}
}