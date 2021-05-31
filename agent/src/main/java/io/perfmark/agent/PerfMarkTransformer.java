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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

final class PerfMarkTransformer implements ClassFileTransformer {

  /** May be {@code null}. */
  private static final Method CLASS_LOADER_GET_NAME = getClassLoaderNameMethodSafe();

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
    System.err.println("  Attempting " + className);
    try {
      return transformInternal(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    } catch (Exception e) {
      System.err.println(e.toString());
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    //}
  }

  byte[] transformInternal(
      ClassLoader loader,
      final String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    assert !className.contains(".") : "Binary name with `.` detected rather than internal name";
    String classLoaderName = getClassLoaderName(loader);
    return transform(classLoaderName, className, classfileBuffer);
  }

  private static byte[] transform(String classLoaderName, String className, byte[] classfileBuffer) {
    ClassReader cr = new ClassReader(classfileBuffer);
    //cr.accept(perfMarkClassVisitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    if (true) {
      ClassWriter cw = new NonMergingClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      PerfMarkClassVisitor perfMarkClassVisitor =
          new PerfMarkClassVisitor(
              classLoaderName, className, PerfMarkClassVisitor.ALL_METHODS, new String[0], cw);
      cr.accept(perfMarkClassVisitor, 0);
      return cw.toByteArray();
    }
    return null;
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

  /**
   * Returns the name for the class loader.  Visible for testing.
   *
   * @param loader the class loader.  May be {@code null}.
   * @return The name of the class loader, or {@code null} if unavailable
   */
  static String getClassLoaderName(ClassLoader loader) {
    if (loader == null) {
      return null;
    }
    if (CLASS_LOADER_GET_NAME != null) {
      try {
        return (String) CLASS_LOADER_GET_NAME.invoke(loader);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof Error) {
          throw (Error) e.getCause();
        } else if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        } else {
          throw new RuntimeException(e.getCause());
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  private static Method getClassLoaderNameMethodSafe() {
    try {
      return getClassLoaderNameMethod();
    } catch (Throwable t) {
      safeLog(t, "Can't get loader method");
    }
    return null;
  }

  /**
   * Gets name method for the Class loader for JDK9+.  Visible for testing.
   *
   * @return the {@code getName} method, or {@code null} if it is absent.
   */
  static Method getClassLoaderNameMethod() {
    try {
      return ClassLoader.class.getMethod("getName");
    } catch (NoSuchMethodException e) {
      safeLog(e, "getName method missing");
      // expected
    }
    return null;
  }

  @SuppressWarnings("UnusedVariable")
  private static void safeLog(Throwable t, String message, Object... args) {
    // TODO(carl-mastrangelo): implement.
  }

  PerfMarkTransformer() {}
}