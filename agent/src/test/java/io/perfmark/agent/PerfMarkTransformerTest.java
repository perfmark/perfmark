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

import com.google.common.truth.Truth;
import io.perfmark.Link;
import io.perfmark.PerfMark;
import io.perfmark.Tag;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Storage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
//@Ignore // disabled until marker support is added back in
public class PerfMarkTransformerTest {

  @Test
  public void deriveFileName() {
    String file = PerfMarkTransformer.deriveFileName("io/perfmark/Clz");

    assertEquals("Clz.java", file);
  }

  @Test
  public void deriveFileName_innerClass() {
    String file = PerfMarkTransformer.deriveFileName("io/perfmark/Clz$Inner");

    assertEquals("Clz.java", file);
  }

  @Test
  @Ignore
  public void transform_autoAnnotate() throws Exception {
    // This test currently depends on the transformer treating this test class specially.
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzAutoRecord.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    for (Mark mark : marks) {
      StackTraceElement element = null;
      assertThat(element.getClassName()).isEqualTo(TransformerTestClasses.ClzAutoRecord.class.getName());
      assertThat(element.getMethodName()).isEqualTo("recordMe");
      assertThat(element.getFileName()).isEqualTo("PerfMarkTransformerTest.java");
      // TODO: reenable.
      // assertThat(element.getLineNumber()).isGreaterThan(0);
    }
  }

  @Test
  public void transform_record() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.SomeRecord.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    for (Mark mark : marks) {
      StackTraceElement element = null;
      assertThat(element.getClassName()).isEqualTo(TransformerTestClasses.SomeRecord.class.getName());
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

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzCtorLambda.class);
    Constructor<?> ctor = clz.getConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    ctor.newInstance(this);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    for (Mark mark : marks) {
      StackTraceElement element = null;
      assertThat(element.getClassName()).isEqualTo(TransformerTestClasses.ClzCtorLambda.class.getName());
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
      StackTraceElement element = null;
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

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithLinks.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(6);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithLinks");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    // assume links have not been modified

    assertEquals(marks.get(4).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(4).getTagStringValue()).contains("TransformerTestClasses$ClzWithLinks");
    Truth.assertThat(marks.get(4).getTagStringValue()).contains("init");

    assertEquals(marks.get(5).withTaskName("task"), marks.get(5));
  }

  @Test
  public void transform_ctor() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithCtor.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(10);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("init");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("init");

    // Ignore the regular tag at 8

    assertEquals(marks.get(9).withTaskName("task"), marks.get(9));
  }

  @Test
  public void transform_init() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithInit.class);
    Constructor<?> ctor = clz.getDeclaredConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(10);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("init");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("init");

    // Ignore the regular tag at 8

    assertEquals(marks.get(9).withTaskName("task"), marks.get(9));
  }

  @Test
  public void transform_clinit() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithClinit.class);
    Constructor<?> ctor = clz.getDeclaredConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(10);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("clinit");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("clinit");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("clinit");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("clinit");

    // Ignore the regular tag at 8

    assertEquals(marks.get(9).withTaskName("task"), marks.get(9));
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
    assertThat(marks).hasSize(10);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.taskStart");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("init");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.taskStop");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("init");

    // Ignore the regular tag at 8

    assertEquals(marks.get(9).withTaskName("task"), marks.get(9));
  }

  private static byte[] getBytes(Class<?> clz) throws IOException {
    String className = clz.getName().replace('.', '/') + ".class";
    try (InputStream stream = clz.getClassLoader().getResourceAsStream(className)) {
      return stream.readAllBytes();
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

  private static Class<?> transformAndLoad2(Class<?> clz) throws IOException {
    byte[] data = getBytes(clz);
    try {
      return new ClassLoader(clz.getClassLoader()) {

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
          if (name.equals(clz.getName())) {
           byte[] newClassBytes =  new PerfMarkTransformer().transform(this, name, clz, null, data);
           Class<?> newClass = defineClass(name, newClassBytes, 0, newClassBytes.length);
           if (resolve) {
             resolveClass(newClass);
           }
            return newClass;
          }
          return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
          return super.findClass(name);
        }
      }.loadClass(clz.getName());
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  private static Class<?> transformAndLoad(Class<?> clz) throws IOException {
    return transformAndLoad2(clz);
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
