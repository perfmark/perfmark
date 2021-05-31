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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

final class PerfMarkClassVisitor extends ClassVisitor {

  static final String[] ALL_METHODS = new String[0];

  private final Runnable changeDetector = new ChangeDetector();

  private final String classLoaderName;
  private final String className;
  private final String[] methodsToRewrite;
  private final String[] methodsToWrap;

  private String fileName;
  private String moduleName;
  private String moduleVersion;
  private boolean isInterface;
  @SuppressWarnings("unused")
  private boolean madeChanges;

  PerfMarkClassVisitor(
      String classLoaderName, String className, String[] methodsToRewrite, String[] methodsToWrap,
      ClassVisitor classVisitor) {
    super(Opcodes.ASM9, classVisitor);
    this.classLoaderName = classLoaderName;
    this.className = className;
    this.methodsToRewrite = methodsToRewrite == ALL_METHODS ? ALL_METHODS : methodsToRewrite.clone();
    this.methodsToWrap = methodsToWrap == ALL_METHODS ? ALL_METHODS : methodsToWrap.clone();
  }

  @Override
  public ModuleVisitor visitModule(String name, int access, String version) {
    this.moduleName = name;
    this.moduleVersion = version;
    return super.visitModule(name, access, version);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public void visitSource(String source, String debug) {
    this.fileName = source;
    super.visitSource(source, debug);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String methodName, String descriptor, String signature, String[] exceptions) {
    final MethodVisitor superDelegate;
    final MethodWrappingWriter methodWrapper;
    if (shouldWrap(methodName, access)) {
      if (className.startsWith("io/perfmark/") || className.startsWith("jdk/")) {
        // Avoid recursion for now.
        return super.visitMethod(access, methodName, descriptor, signature, exceptions);
      }
      String bodyMethodName = methodName + "$perfmark";
      int newAccess;
      if (!isInterface) {
        newAccess = (access | Opcodes.ACC_PRIVATE) & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC);
      } else {
        newAccess = access;
      }
      MethodVisitor visitor = super.visitMethod(newAccess, bodyMethodName, descriptor, signature, exceptions);
      MethodVisitorRecorder recorder = new MethodVisitorRecorder(visitor);
      superDelegate = recorder;
      methodWrapper =
          new MethodWrappingWriter(
              recorder, access, methodName, descriptor, signature, exceptions, isInterface, className, bodyMethodName,
              cv);
    } else {
      superDelegate = super.visitMethod(access, methodName, descriptor, signature, exceptions);
      methodWrapper = null;
    }

    final MethodVisitor visitor;
    if (shouldRewrite(methodName)) {
      visitor = new PerfMarkMethodRewriter(
          classLoaderName, moduleName, moduleVersion, className, methodName, fileName, changeDetector, superDelegate);
    } else {
      visitor = superDelegate;
    }

    return new MethodVisitorWrapper(methodWrapper, visitor);
  }

  boolean shouldWrap(String methodName, int access) {
    if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
      return false;
    }
    if (methodsToWrap == ALL_METHODS) {
      // not yet supported
      if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
        return false;
      }
      return true;
    }
    for (String method : methodsToWrap) {
      if (method.equals(methodName)) {
        return true;
      }
    }
    return false;
  }

  boolean shouldRewrite(String methodName) {
    if (methodsToRewrite == ALL_METHODS) {
      return true;
    }
    for (String method : methodsToRewrite) {
      if (method.equals(methodName)) {
        return true;
      }
    }
    return false;
  }

  private final class MethodVisitorWrapper extends MethodVisitor {

    private final MethodWrappingWriter methodWrapper;

    /**
     *
     * @param methodWrapper the writer of the "wrapper" method.  May be {@code null}.
     * @param delegate method visitor to inject in the additional method. May be {@code null}.
     */
    MethodVisitorWrapper(MethodWrappingWriter methodWrapper, MethodVisitor delegate) {
      super(PerfMarkClassVisitor.this.api, delegate);
      this.methodWrapper = methodWrapper;
    }

    @Override
    public void visitEnd() {
      super.visitEnd();
      if (methodWrapper != null) {
        madeChanges = true;
        methodWrapper.visit();
      }

    }
  }

  private final class ChangeDetector implements Runnable {
    @Override
    public void run() {
      madeChanges = true;
    }
  }
}
