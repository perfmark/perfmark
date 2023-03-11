/*
 * Copyright 2023 Carl Mastrangelo
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

package io.perfmark.impl;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConcurrentThreadLocalTest {


  @Test
  public void canBeGarbageCollected() throws Exception {
    Object concurrentThreadLocal;
    WeakReference<ClassLoader> loaderRef;
    WeakReference<Class<?>> klassRef;
    {
      var classLoader = new CleanupClassLoader(getClass().getClassLoader());
      loaderRef = new WeakReference<>(classLoader);
      Class<?> klass = Class.forName(ConcurrentThreadLocal.class.getName(), true, classLoader);
      klassRef = new WeakReference<>(klass);
      concurrentThreadLocal = klass.getConstructor().newInstance();
      // Simulate some other object that might keep this class loader alive.
      klass.getMethod("set", Object.class)
          .invoke(concurrentThreadLocal, Class.forName(Mark.class.getName(), true, classLoader));

    }
    assertNotNull(Mark.class.getName(), klassRef.get().getMethod("get").invoke(concurrentThreadLocal));
    concurrentThreadLocal = null;
    long sleepMs = 10;
    while (loaderRef.get() != null && sleepMs <= 10_000) {
      System.gc();
      Thread.sleep(sleepMs);
      sleepMs = sleepMs + (sleepMs >> 1);
    }
    if (loaderRef.get() != null) {
      throw new AssertionError("Not GC'd in time");
    }
  }

  private static final class CleanupClassLoader extends ClassLoader {

    CleanupClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("io.perfmark.")) {
        try (var s = getResourceAsStream(name.replace(".", "/") + ".class")) {
          var bytes = s.readAllBytes();
          var clz = defineClass(name, bytes, 0, bytes.length);
          if (resolve) {
            resolveClass(clz);
          }
          return clz;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        return super.loadClass(name, resolve);
      }
    }
  }
}
