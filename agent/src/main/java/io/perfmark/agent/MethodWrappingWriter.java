/*
 * Copyright 2021 Carl Mastrangelo
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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class MethodWrappingWriter {

  private final MethodVisitorRecorder recorder;
  private final int access;
  private final String methodName;
  private final String descriptor;
  private final String signature;
  private final String[] exceptions;
  private final String className;
  private final String bodyMethodName;
  private final ClassVisitor classVisitor;
  private final boolean isInterface;

  MethodWrappingWriter(
      MethodVisitorRecorder recorder, int access, String methodName, String descriptor, String signature,
      String[] exceptions, boolean isInterface, String className, String bodyMethodName, ClassVisitor cv) {
    this.recorder = recorder;
    this.access = access;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.signature = signature;
    this.exceptions = exceptions;
    this.isInterface = isInterface;
    this.className = className;
    this.bodyMethodName = bodyMethodName;
    this.classVisitor = cv;
  }

  void visit() {
    MethodVisitor mv = classVisitor.visitMethod(access, methodName, descriptor, signature, exceptions);
    if (mv == null) {
      return;
    }
    // TODO(carl-mastrangelo): add start / stop call tags here
    recorder.replay(mv);

    mv.visitCode();
    Label start = new Label();
    Label end = new Label();
    mv.visitLabel(start);
    mv.visitLineNumber(recorder.firstLine, start);
    mv.visitLdcInsn(className + "::" + methodName);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/perfmark/PerfMark", "startTask", "(Ljava/lang/String;)V", false);

    if (((access & Opcodes.ACC_STATIC) == 0)) {
      mv.visitVarInsn(Opcodes.ALOAD, 0);
    }

    int params = 1;
    char ret = 0;
    assert descriptor.charAt(0) == '(';
    out: for (int i = 1; i < descriptor.length(); i++) {
      char c = descriptor.charAt(i);
      switch (c) {
        case ')':
          ret = descriptor.charAt(i + 1);
          break out;
        case 'L':
          mv.visitVarInsn(Opcodes.ALOAD, params++);
          i = descriptor.indexOf(';', i);
          assert i > 0;
          break;
        case 'Z':
        case 'C':
        case 'B':
        case 'S':
        case 'I':
          mv.visitVarInsn(Opcodes.ILOAD, params++);
          break;
        case 'F':
          mv.visitVarInsn(Opcodes.FLOAD, params++);
          break;
        case 'J':
          mv.visitVarInsn(Opcodes.LLOAD, params++);
          break;
        case 'D':
          mv.visitVarInsn(Opcodes.DLOAD, params++);
          break;
        case '[':
          mv.visitVarInsn(Opcodes.ALOAD, params++);
          while (descriptor.charAt(++i) == '[') {
            // empty body on purpose.
          }
          if (descriptor.charAt(i) == 'L') {
            i = descriptor.indexOf(';', i);
          }

          break;
        default:
          throw new RuntimeException("Bad descriptor " + c + " in " + descriptor);
      }
    }

    int invoke;
    if ((access & Opcodes.ACC_STATIC) != 0) {
      invoke = Opcodes.INVOKESTATIC;
    } else if (isInterface) {
      invoke = Opcodes.INVOKEINTERFACE;
    } else {
      invoke = Opcodes.INVOKESPECIAL;
    }

    mv.visitMethodInsn(invoke, className.replace(".", "/"), bodyMethodName, descriptor, isInterface);

    mv.visitLabel(end);
    mv.visitLineNumber(recorder.lastLine, end);
    mv.visitLdcInsn(className + ":::" + methodName);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/perfmark/PerfMark", "stopTask", "(Ljava/lang/String;)V", false);

    switch (ret) {
      case 'V':
        mv.visitInsn(Opcodes.RETURN);
        break;
      case 'L':
      case '[':
        mv.visitInsn(Opcodes.ARETURN);
        break;
      case 'Z':
      case 'C':
      case 'B':
      case 'S':
      case 'I':
        mv.visitInsn(Opcodes.IRETURN);
        break;
      case 'F':
        mv.visitInsn(Opcodes.FRETURN);
        break;
      case 'J':
        mv.visitInsn(Opcodes.LRETURN);
        break;
      case 'D':
        mv.visitInsn(Opcodes.DRETURN);
        break;
      default:
        throw new RuntimeException("Bad Descriptor " + ret);
    }

    mv.visitMaxs(params, params + 1);
    mv.visitEnd();
  }
}
