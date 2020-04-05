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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.perfmark.Link;
import io.perfmark.PerfMark;
import io.perfmark.Tag;
import io.perfmark.agent.PerfMarkTransformer.PerfMarkClassReader;
import io.perfmark.impl.Internal;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Storage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@Ignore // disabled until marker support is added back in
public class PerfMarkTransformerTest {

  @Test
  public void deriveFileName() {
    String file = PerfMarkClassReader.deriveFileName("io/perfmark/Clz");

    assertEquals("Clz.java", file);
  }

  @Test
  public void deriveFileName_innerClass() {
    String file = PerfMarkClassReader.deriveFileName("io/perfmark/Clz$Inner");

    assertEquals("Clz.java", file);
  }

  public static final class ClzAutoRecord {
    public ClzAutoRecord() {
      recordMe();
    }

    void recordMe() {
      // seemingly nothing here.
    }
  }

  @Test
  public void transform_auto() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(ClzAutoRecord.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzAutoRecord.class.getName());
      assertThat(element.getMethodName()).isEqualTo("recordMe");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      // TODO: reenable.
      // assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_lambda() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    final class ClzLocal implements Executor {
      public ClzLocal() {
        execute(
            () -> {
              PerfMark.startTask("task");
              PerfMark.stopTask("task");
            });
      }

      @Override
      public void execute(Runnable command) {
        command.run();
      }
    }

    Class<?> clz = transformAndLoad(ClzLocal.class);
    Constructor<?> ctor = clz.getConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzLocal.class.getName());
      assertThat(element.getMethodName()).isEqualTo("lambda$new$0");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_methodRef() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    final class ClzLocal {
      public ClzLocal() {
        @SuppressWarnings("unused")
        Object o = execute(PerfMark::linkOut);
      }

      Link execute(Supplier<Link> supplier) {
        return supplier.get();
      }
    }

    Class<?> clz = transformAndLoad(ClzLocal.class);
    Constructor<?> ctor = clz.getConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(1);
    // I'm not sure what to do with methodrefs, so just leave it alone for now.
  }

  public interface InterfaceWithDefaults {
    default void record() {
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
    }
  }

  @Test
  public void transform_interface() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    final class Bar implements InterfaceWithDefaults {
      public Bar() {
        record();
      }
    }

    Class<?> clz = transformAndLoad(Bar.class);
    Constructor<?> ctor = clz.getConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);

    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(InterfaceWithDefaults.class.getName());
      assertThat(element.getMethodName()).isEqualTo("record");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_link() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    final class ClzLocal {
      public ClzLocal() {
        PerfMark.startTask("task");
        Link link = PerfMark.linkOut();
        PerfMark.linkIn(link);
        PerfMark.stopTask("task");
      }
    }

    Class<?> clz = transformAndLoad(ClzLocal.class);
    Constructor<?> ctor = clz.getConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzLocal.class.getName());
      assertThat(element.getMethodName()).isEqualTo("<init>");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_ctor() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    final class ClzLocal {
      public ClzLocal() {
        Tag tag = PerfMark.createTag("tag", 1);
        PerfMark.startTask("task");
        PerfMark.stopTask("task");
        PerfMark.startTask("task", tag);
        PerfMark.stopTask("task", tag);
      }
    }

    Class<?> clz = transformAndLoad(ClzLocal.class);
    Constructor<?> ctor = clz.getConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzLocal.class.getName());
      assertThat(element.getMethodName()).isEqualTo("<init>");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_init() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    final class ClzLocal {
      {
        Tag tag = PerfMark.createTag("tag", 1);
        PerfMark.startTask("task");
        PerfMark.stopTask("task");
        PerfMark.startTask("task", tag);
        PerfMark.stopTask("task", tag);
      }
    }

    Class<?> clz = transformAndLoad(ClzLocal.class);
    Constructor<?> ctor = clz.getDeclaredConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzLocal.class.getName());
      assertThat(element.getMethodName()).isEqualTo("<init>");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  private static final class ClzWithClinit {
    static {
      Tag tag = PerfMark.createTag("tag", 1);
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
      PerfMark.startTask("task", tag);
      PerfMark.stopTask("task", tag);
    }
  }

  @Test
  public void transform_clinit() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(ClzWithClinit.class);
    Constructor<?> ctor = clz.getDeclaredConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzWithClinit.class.getName());
      assertThat(element.getMethodName()).isEqualTo("<clinit>");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_toplevel() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(ClzFooter.class);
    Constructor<?> ctor = clz.getDeclaredConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    for (Mark mark : marks) {
      assertNotNull(mark.getMarker());
      StackTraceElement element = Internal.getElement(mark.getMarker());
      assertThat(element.getClassName()).isEqualTo(ClzFooter.class.getName());
      assertThat(element.getMethodName()).isEqualTo("<init>");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  private static byte[] getBytes(Class<?> clz) throws IOException {
    String className = clz.getName().replace('.', '/') + ".class";
    try (InputStream stream = clz.getClassLoader().getResourceAsStream(className);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      int read;
      byte[] buf = new byte[1024];
      while ((read = stream.read(buf)) != -1) {
        baos.write(buf, 0, read);
      }

      return baos.toByteArray();
    }
  }

  private static final class TestClassLoader extends ClassLoader {
    TestClassLoader() {
      super(PerfMarkTransformerTest.class.getClassLoader());
    }

    Class<?> defineClass(String name, byte[] data) {
      return defineClass(name, data, 0, data.length);
    }

    Class<?> findLoadedClz(String name) {
      return findLoadedClass(name);
    }
  }

  private static Class<?> transformAndLoad(Class<?> clz) throws IOException {
    return transformAndLoad(clz, new TestClassLoader());
  }

  private static Class<?> transformAndLoad(Class<?> clz, TestClassLoader cl) throws IOException {
    if (clz.getClassLoader() == null) {
      return clz;
    }
    if (clz.getSuperclass() != Object.class && clz.getSuperclass() != null) {
      transformAndLoad(clz.getSuperclass(), cl);
    }
    for (Class<?> iface : clz.getInterfaces()) {
      transformAndLoad(iface, cl);
    }
    String name = clz.getName();
    if (cl.findLoadedClz(name) != null) {
      return cl.findLoadedClz(name);
    }
    byte[] bytes = getBytes(clz);
    byte[] newBytes =
        new PerfMarkTransformer().transform(cl, name, clz, /* protectionDomain= */ null, bytes);
    return cl.defineClass(name, newBytes);
  }
}

final class ClzFooter {
  {
    Tag tag = PerfMark.createTag("tag", 1);
    PerfMark.startTask("task");
    PerfMark.stopTask("task");
    PerfMark.startTask("task", tag);
    PerfMark.stopTask("task", tag);
  }
}
