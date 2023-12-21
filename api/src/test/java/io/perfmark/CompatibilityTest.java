/*
 * Copyright 2022 Google LLC
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

package io.perfmark;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CompatibilityTest {
  private static final int STABLE_VERSION = 15;

  private static final List<String> VERSIONS =
      List.of(
          "0.13.37",
          "0.14.0",
          "0.15.0",
          "0.16.0",
          "0.17.0",
          // "0.18.0", missing, not sure why
          "0.19.0",
          "0.20.1",
          "0.21.0",
          "0.22.0",
          "0.23.0",
          "0.24.0",
          "0.25.0",
          "0.26.0",
          "0.27.0");

  @Parameterized.Parameters(name = "version v{0}")
  @SuppressWarnings("StringSplitter")
  public static Iterable<Object[]> params() {
    List<Object[]> params = new ArrayList<>();
    for (var version : VERSIONS) {
      String fileName = "perfmark-api-" + version + ".jar";
      URL jarPath = CompatibilityTest.class.getResource(fileName);
      if (jarPath == null) {
        throw new AssertionError("Can't load version " + version);
      }
      params.add(new Object[] {version, Integer.valueOf(version.split("\\.")[1]), jarPath});
    }

    return params;
  }

  @Parameterized.Parameter(0)
  public String semanticVersion;

  @Parameterized.Parameter(1)
  public int minorVersion;

  @Parameterized.Parameter(2)
  public URL jarPath;

  private final Class<?> currentPerfMarkClz = PerfMark.class;

  private Class<?> perfMarkClz;
  private Class<?> storageClz;

  @Before
  public void setUp() throws Exception {
    ClassLoader loader = new ApiOverrideClassLoader();

    perfMarkClz = Class.forName("io.perfmark.PerfMark", false, loader);
    assertNotEquals(currentPerfMarkClz, perfMarkClz);

    storageClz = Class.forName("io.perfmark.impl.Storage", false, loader);
    var marks = (List) storageClz.getMethod("readForTest").invoke(null);
    assertThat(marks).isNull();
  }

  @After
  public void tearDown() throws Exception {
    if (storageClz != null) {
      storageClz.getMethod("resetForThread").invoke(null);
    }
  }

  @Test
  public void checkPublicMethods_disabledOnMissingImpl() throws Exception {
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);

    ClassLoader loader = new ApiOverrideNoImplClassLoader();

    Class<?> perfMarkClz = Class.forName("io.perfmark.PerfMark", false, loader);
    assertNotEquals(currentPerfMarkClz, perfMarkClz);
    assertNotEquals(this.perfMarkClz, perfMarkClz);

    var tag = perfMarkClz.getMethod("createTag").invoke(null);
    var link = perfMarkClz.getMethod("link").invoke(null);

    for (Method method : perfMarkClz.getMethods()) {
      if (!Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      var paramTypes = method.getParameterTypes();
      Object[] args = new Object[paramTypes.length];
      for (int i = 0; i < paramTypes.length; i++) {
        if (paramTypes[i].getName().startsWith("io.perfmark.")) {
          paramTypes[i] =
              Class.forName(paramTypes[i].getName(), false, currentPerfMarkClz.getClassLoader());
        }
        if (paramTypes[i] == long.class) {
          args[i] = 0L;
        } else if (paramTypes[i] == boolean.class) {
          args[i] = true;
        } else if (paramTypes[i].getSimpleName().equals("Link")) {
          args[i] = link;
        } else if (paramTypes[i].getSimpleName().equals("Tag")) {
          args[i] = tag;
        } else if (Object.class.isAssignableFrom(paramTypes[i])) {
          args[i] = null;
        } else {
          throw new AssertionError("unknown param");
        }
      }

      method.invoke(null, args);
    }
  }

  @Test
  public void checkPublicMethods() throws Exception {
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);
    for (Method method : perfMarkClz.getMethods()) {
      var paramTypes = method.getParameterTypes();
      for (int i = 0; i < paramTypes.length; i++) {
        if (paramTypes[i].getName().startsWith("io.perfmark.")) {
          paramTypes[i] =
              Class.forName(paramTypes[i].getName(), false, currentPerfMarkClz.getClassLoader());
        }
      }
      Class<?> returnType = method.getReturnType();
      if (returnType.getName().startsWith("io.perfmark.")) {
        returnType =
            Class.forName(returnType.getName(), false, currentPerfMarkClz.getClassLoader());
      }

      var m = currentPerfMarkClz.getMethod(method.getName(), paramTypes);
      assertNotNull(method.getName(), m);
      if (returnType == void.class) {
        return;
      }
      assertEquals(m.getReturnType(), returnType);
    }
  }

  @Test
  public void startStopTaskWorks() throws Exception {
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);
    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("startTask", String.class).invoke(null, "task1");
    perfMarkClz.getMethod("stopTask", String.class).invoke(null, "task1");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    // 13 - 16 should safely disable themselves, nag, but ultimately produce no data.
    if (minorVersion >= 17) {
      assertThat(marks).hasSize(2);
    } else {
      assertThat(marks).isNull();
    }
  }

  @Test
  public void startStopTaskWorks_tag() throws Exception {
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);
    Class<?> tagClz = Class.forName("io.perfmark.Tag", false, perfMarkClz.getClassLoader());
    Object tag = perfMarkClz.getMethod("createTag").invoke(null);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("startTask", String.class, tagClz).invoke(null, "task1", tag);
    perfMarkClz.getMethod("stopTask", String.class, tagClz).invoke(null, "task1", tag);

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    // 13 - 16 should safely disable themselves, nag, but ultimately produce no data.
    if (minorVersion >= 17) {
      assertThat(marks).hasSize(4);
    } else {
      assertThat(marks).isNull();
    }
  }

  @Test
  public void startStopTaskWorks_namedFunction() throws Exception {
    Assume.assumeTrue(minorVersion >= 22);
    Class<?> fnClz =
        Class.forName("io.perfmark.StringFunction", false, perfMarkClz.getClassLoader());
    Object fn =
        Proxy.newProxyInstance(
            perfMarkClz.getClassLoader(), new Class<?>[] {fnClz}, (proxy, method, args) -> "hi");

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("startTask", Object.class, fnClz).invoke(null, new Object(), fn);
    perfMarkClz.getMethod("stopTask").invoke(null);

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(2);
  }

  @Test
  public void startStopTaskWorks_subTask() throws Exception {
    Assume.assumeTrue(minorVersion >= 20);
    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("startTask", String.class, String.class).invoke(null, "task1", "sub");
    perfMarkClz.getMethod("stopTask", String.class, String.class).invoke(null, "task1", "sub");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(2);
  }

  @Test
  public void traceTask() throws Exception {
    Assume.assumeTrue(minorVersion >= 23);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    try (var c =
        (Closeable) perfMarkClz.getMethod("traceTask", String.class).invoke(null, "task1")) {}

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(2);
  }

  @Test
  public void traceTask_namedFunction() throws Exception {
    Assume.assumeTrue(minorVersion >= 23);

    Class<?> fnClz =
        Class.forName("io.perfmark.StringFunction", false, perfMarkClz.getClassLoader());
    Object fn =
        Proxy.newProxyInstance(
            perfMarkClz.getClassLoader(), new Class<?>[] {fnClz}, (proxy, method, args) -> "hi");

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    try (var c =
        (Closeable)
            perfMarkClz
                .getMethod("traceTask", Object.class, fnClz)
                .invoke(null, new Object(), fn)) {}

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(2);
  }

  @Test
  public void event_tag() throws Exception {
    // Versions Prior to 15 Had a duration associated with an event that was removed before
    // stability.  This means
    // Classloading in PerfMark init can't find the MarkHolder.event() incompatibility between older
    // and later versions.
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);

    Class<?> tagClz = Class.forName("io.perfmark.Tag", false, perfMarkClz.getClassLoader());
    Object tag = perfMarkClz.getMethod("createTag").invoke(null);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("event", String.class, tagClz).invoke(null, "event1", tag);
    perfMarkClz.getMethod("event", String.class).invoke(null, "event2");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    // 13 - 16 should safely disable themselves, nag, but ultimately produce no data.
    if (minorVersion >= 17) {
      assertThat(marks).hasSize(2);
    } else {
      assertThat(marks).isNull();
    }
  }

  @Test
  public void event_subEvent() throws Exception {
    Assume.assumeTrue(minorVersion >= 20);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("event", String.class, String.class).invoke(null, "event1", "subevent");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(1);
  }

  @Test
  public void createTags() throws Exception {
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);
    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("createTag", long.class).invoke(null, 2);
    perfMarkClz.getMethod("createTag", String.class).invoke(null, "tag2");
    perfMarkClz.getMethod("createTag", String.class, long.class).invoke(null, "tag2", 2);

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).isNull();
  }

  @Test
  public void link_doesNothing() throws Exception {
    // This is broken in early versions, sorry.
    Assume.assumeTrue(minorVersion >= STABLE_VERSION);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);
    perfMarkClz.getMethod("link").invoke(null);

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).isNull();
  }

  @Test
  public void linkInLinkOut() throws Exception {
    Assume.assumeTrue(minorVersion >= 17);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);

    perfMarkClz.getMethod("startTask", String.class).invoke(null, "task1");
    Object link = perfMarkClz.getMethod("linkOut").invoke(null);
    perfMarkClz.getMethod("stopTask", String.class).invoke(null, "task1");

    perfMarkClz.getMethod("startTask", String.class).invoke(null, "task2");
    perfMarkClz.getMethod("linkIn", link.getClass()).invoke(null, link);
    perfMarkClz.getMethod("stopTask", String.class).invoke(null, "task2");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(6);
  }

  @Test
  public void attachTag_tag() throws Exception {
    Assume.assumeTrue(minorVersion >= 18);

    Class<?> tagClz = Class.forName("io.perfmark.Tag", false, perfMarkClz.getClassLoader());
    Object tag = perfMarkClz.getMethod("createTag").invoke(null);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);

    perfMarkClz.getMethod("startTask", String.class).invoke(null, "task1");
    perfMarkClz.getMethod("attachTag", tagClz).invoke(null, tag);
    perfMarkClz.getMethod("stopTask", String.class).invoke(null, "task1");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(3);
  }

  @Test
  public void attachTag_namedTag() throws Exception {
    Assume.assumeTrue(minorVersion >= 20);

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);

    perfMarkClz.getMethod("startTask", String.class).invoke(null, "task1");
    perfMarkClz.getMethod("attachTag", String.class, String.class).invoke(null, "name1", "val");
    perfMarkClz.getMethod("attachTag", String.class, long.class).invoke(null, "name2", 22L);
    perfMarkClz
        .getMethod("attachTag", String.class, long.class, long.class)
        .invoke(null, "uuid", 22L, 55L);
    perfMarkClz.getMethod("stopTask", String.class).invoke(null, "task1");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(5);
  }

  @Test
  public void attachTag_namedFunction() throws Exception {
    Assume.assumeTrue(minorVersion >= 22);

    Class<?> fnClz =
        Class.forName("io.perfmark.StringFunction", false, perfMarkClz.getClassLoader());
    Object fn =
        Proxy.newProxyInstance(
            perfMarkClz.getClassLoader(), new Class<?>[] {fnClz}, (proxy, method, args) -> "hi");

    perfMarkClz.getMethod("setEnabled", boolean.class).invoke(null, true);

    perfMarkClz.getMethod("startTask", String.class).invoke(null, "task1");
    perfMarkClz
        .getMethod("attachTag", String.class, Object.class, fnClz)
        .invoke(null, "name1", new Object(), fn);
    perfMarkClz.getMethod("stopTask", String.class).invoke(null, "task1");

    List marks = (List) storageClz.getMethod("readForTest").invoke(null);

    assertThat(marks).hasSize(3);
  }

  @Test
  public void implOverride() throws Exception {
    Assume.assumeTrue(minorVersion >= 17);

    Class<?> implClass = Class.forName("io.perfmark.Impl", false, perfMarkClz.getClassLoader());
    Class<?> secretImplClass =
        Class.forName(
            "io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl",
            false,
            perfMarkClz.getClassLoader());

    for (Method method : implClass.getDeclaredMethods()) {
      if ((method.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }
      Method m2 = secretImplClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
      assertThat(m2).isNotNull();
    }
  }

  private final class ApiOverrideClassLoader extends ClassLoader {
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("io.perfmark.")) {
        for (var loader :
            List.of(new URLClassLoader(new URL[] {jarPath}, null), getClass().getClassLoader())) {
          try (var stream = loader.getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (stream == null) {
              continue;
            }
            var data = stream.readAllBytes();
            var clz = defineClass(name, data, 0, data.length);
            if (resolve) {
              resolveClass(clz);
            }
            return clz;
          } catch (IOException e) {
            throw (ClassNotFoundException) new ClassNotFoundException().initCause(e);
          }
        }
        throw new ClassNotFoundException("not in here: " + name);
      }
      return super.loadClass(name, resolve);
    }
  }

  private final class ApiOverrideNoImplClassLoader extends ClassLoader {
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("io.perfmark.")) {
        if (name.startsWith("io.perfmark.impl.")) {
          throw new ClassNotFoundException(name);
        }
        for (var loader :
            List.of(new URLClassLoader(new URL[] {jarPath}, null), getClass().getClassLoader())) {
          try (var stream = loader.getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (stream == null) {
              continue;
            }
            var data = stream.readAllBytes();
            var clz = defineClass(name, data, 0, data.length);
            if (resolve) {
              resolveClass(clz);
            }
            return clz;
          } catch (IOException e) {
            throw (ClassNotFoundException) new ClassNotFoundException().initCause(e);
          }
        }
        throw new ClassNotFoundException("not in here: " + name);
      }
      return super.loadClass(name, resolve);
    }
  }
}
