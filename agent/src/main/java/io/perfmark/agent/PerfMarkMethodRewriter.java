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

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Modified PerfMark startTask and stopTask call sits to include tags about where in code they came from.
 */
final class PerfMarkMethodRewriter extends MethodVisitor {

  private static final String PERFMARK_CLZ = "io/perfmark/PerfMark";
  private static final String TASKCLOSEABLE_CLZ = "io/perfmark/TaskCloseable";

  /**
   * May be {@code null} if absent.
   */
  private static final Constructor<? extends StackTraceElement> MODULE_CTOR = getStackTraceElementCtorSafe();

  private final Runnable onChange;
  private final String classLoaderName;
  private final String moduleName;
  private final String moduleVersion;
  // The internal fully qualified name. (e.g. java/lang/String)
  private final String className;
  private final String methodName;
  private final String fileName;

  private int lineNumber = -1;

  /**
   * Builds the rewriter to add debug into to trace calls.
   *
   * @param classLoaderName the loader of this class.  May be {@code null}.
   * @param moduleName the module of this class.  May be {@code null}.
   * @param moduleVersion the module version of this class.  May be {@code null}.
   * @param className the class that contains this method.  Must be non-{@code null}.
   * @param methodName the method currently being visited.  Must be non-{@code null}.
   * @param fileName the class that contains this method.  May be {@code null}.
   * @param onChange runnable to invoke if any changes are made to the class definition.  May be {@code null}.
   * @param methodVisitor the delegate to call.  May be {@code null}.
   */
  PerfMarkMethodRewriter(
      String classLoaderName, String moduleName, String moduleVersion, String className, String methodName,
      String fileName, Runnable onChange, MethodVisitor methodVisitor) {
    super(Opcodes.ASM9, methodVisitor);
    this.classLoaderName = classLoaderName;
    this.moduleName = moduleName;
    this.moduleVersion = moduleVersion;
    this.className = className;
    this.methodName = methodName;
    this.fileName = fileName;
    this.onChange = onChange;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    lineNumber = line;
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    boolean pretag;
    switch (opcode) {
      case INVOKESTATIC:
        pretag = PERFMARK_CLZ.equals(owner) && name.equals("stopTask") && !TASKCLOSEABLE_CLZ.equals(className);
        break;
      case INVOKEVIRTUAL:
        pretag = TASKCLOSEABLE_CLZ.equals(owner) && name.equals("close");
        break;
      default:
        pretag = false;
        break;
    }
    if (pretag) {
      visitLdcInsn("PerfMark.stopCallSite");
      visitLdcInsn(callSite());
      super.visitMethodInsn(INVOKESTATIC, PERFMARK_CLZ, "attachTag", "(Ljava/lang/String;Ljava/lang/String;)V", false);
      if (onChange != null) {
        onChange.run();
      }
    }

    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

    if (opcode == INVOKESTATIC
        && PERFMARK_CLZ.equals(owner)
        && (name.equals("startTask") || name.equals("traceTask"))) {
      visitLdcInsn("PerfMark.startCallSite");
      visitLdcInsn(callSite());
      super.visitMethodInsn(INVOKESTATIC, PERFMARK_CLZ, "attachTag", "(Ljava/lang/String;Ljava/lang/String;)V", false);
      if (onChange != null) {
        onChange.run();
      }
    }
  }

  private String callSite() {
    StackTraceElement elem = null;
    try {
      elem = moduleElement();
    } catch (Throwable t) {
      // TODO(carl-mastrangelo): this should log.
    }
    if (elem == null) {
      elem = new StackTraceElement(className, methodName, fileName, lineNumber);
    }
    return elem.toString();
  }

  /**
   * Builds the current callsite.  Visible for testing.
   *
   * @return stack trace element using the modern constructor, or {@code null} if absent.
   */
  StackTraceElement moduleElement() throws InvocationTargetException, InstantiationException, IllegalAccessException {
    StackTraceElement elem = null;
    if (MODULE_CTOR != null) {
      elem = MODULE_CTOR.newInstance(
          classLoaderName, moduleName, moduleVersion, className, methodName, fileName, lineNumber);
    }
    return elem;
  }

  private static Constructor<StackTraceElement> getStackTraceElementCtorSafe() {
    try {
      return getStackTraceElementCtor();
    } catch (Throwable t) {
      return null;
    }
  }

  /**
   * Gets the more modern constructor.  Visible for testing.
   *
   * @return the module-based constructor, or {@code null} if it is absent.
   */
  static Constructor<StackTraceElement> getStackTraceElementCtor() {
    Constructor<StackTraceElement> ctor = null;
      try {
        ctor = StackTraceElement.class.getConstructor(
            String.class, String.class, String.class, String.class, String.class, String.class, int.class);
      } catch (NoSuchMethodException e) {
        // normal on JDK 8 and below, but include an assert in case the descriptor was wrong.
        assert StackTraceElement.class.getConstructors().length < 2 : e;
      }
      return ctor;
  }
}
