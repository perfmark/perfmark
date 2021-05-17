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
import io.perfmark.PerfMark;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Storage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class) public class PerfMarkTransformerTest {

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
  }

  @Test
  public void transform_record() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.SomeRecord.class);
    Constructor<?> ctor = clz.getConstructor(int.class);
    ctor.setAccessible(true);
    ctor.newInstance(2);
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$SomeRecord");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("<init>");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$SomeRecord");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("<init>");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));
  }

  @Test
  public void transform_lambda() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzCtorLambda.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);
    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzCtorLambda");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("lambda");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzCtorLambda");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("lambda");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));
  }

  @Test
  public void transform_methodRef() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithMethodRefs.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(2);
    // I'm not sure what to do with methodrefs, so just leave it alone for now.
  }

  @Test
  public void transform_interface() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz =
        transformAndLoad(TransformerTestClasses.Bar.class, TransformerTestClasses.InterfaceWithDefaults.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();

    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(10);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$InterfaceWithDefaults");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("record");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$InterfaceWithDefaults");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("record");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$InterfaceWithDefaults");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("record");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("TransformerTestClasses$InterfaceWithDefaults");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("record");

    // Ignore the regular tag at 8

    assertEquals(marks.get(9).withTaskName("task"), marks.get(9));
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

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithLinks");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    // assume links have not been modified

    assertEquals(marks.get(4).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(4).getTagStringValue()).contains("TransformerTestClasses$ClzWithLinks");
    Truth.assertThat(marks.get(4).getTagStringValue()).contains("init");

    assertEquals(marks.get(5).withTaskName("task"), marks.get(5));
  }

  @Test
  public void transform_closeable() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithCloseable.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithCloseable");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithCloseable");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(Mark.Operation.TASK_END_N1S0, marks.get(3).getOperation());
  }

  @Test
  public void transform_wrongCloseable() throws Exception {
    // If the wrong static type is used, the agent won't be able to instrument it.  Add a test to document this
    // behavior.
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(TransformerTestClasses.ClzWithWrongCloseable.class);
    Constructor<?> ctor = clz.getConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(3);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithWrongCloseable");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(Mark.Operation.TASK_END_N1S0, marks.get(2).getOperation());
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

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$ClzWithCtor");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("init");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.stopCallSite");
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

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$ClzWithInit");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("init");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.stopCallSite");
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

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("clinit");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("clinit");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("TransformerTestClasses$ClzWithClinit");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("clinit");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.stopCallSite");
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

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("init");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("init");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));

    assertEquals(marks.get(4).withTaskName("task"), marks.get(4));

    // Ignore the regular tag at 5

    assertEquals(marks.get(6).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(6).getTagStringValue()).contains("init");

    assertEquals(marks.get(7).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("ClzFooter");
    Truth.assertThat(marks.get(7).getTagStringValue()).contains("init");

    // Ignore the regular tag at 8

    assertEquals(marks.get(9).withTaskName("task"), marks.get(9));
  }

  @Test
  public void transform_anonymousClass() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    Class<?> clz = transformAndLoad(new Runnable() {
      // avoid IntelliJ thinking this should be a lambda.
      public volatile int a;
      @Override
      public void run() {
        PerfMark.startTask("task");
        PerfMark.stopTask("task");
      }
    }.getClass());
    Constructor<?> ctor = clz.getDeclaredConstructor(PerfMarkTransformerTest.class);
    ctor.setAccessible(true);
    Runnable instance = (Runnable) ctor.newInstance(this);
    instance.run();
    List<Mark> marks = Storage.readForTest();
    assertThat(marks).hasSize(4);

    assertEquals(marks.get(0).withTaskName("task"), marks.get(0));

    assertEquals(marks.get(1).getTagKey(), "PerfMark.startCallSite");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("PerfMarkTransformerTest$");
    Truth.assertThat(marks.get(1).getTagStringValue()).contains("run");

    assertEquals(marks.get(2).getTagKey(), "PerfMark.stopCallSite");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("PerfMarkTransformerTest$");
    Truth.assertThat(marks.get(2).getTagStringValue()).contains("run");

    assertEquals(marks.get(3).withTaskName("task"), marks.get(3));
  }

  private static Class<?> transformAndLoad(Class<?> toLoad, Class<?> ...extra) throws IOException {
    Map<String, Class<?>> toTransform = new HashMap<>();
    for (Class<?>  clz : extra) {
      toTransform.put(clz.getName(), clz);
    }
    toTransform.put(toLoad.getName(), toLoad);
    try {
      return new ClassLoader(toLoad.getClassLoader()) {

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
          Class<?> existing = toTransform.get(name);
          if (existing == null) {
            return super.loadClass(name, resolve);
          }
          String resourceName = name.replace('.', '/') + ".class";
          byte[] data;
          try (InputStream stream = getResourceAsStream(resourceName)) {
            data = stream.readAllBytes();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          byte[] newClassBytes = new PerfMarkTransformer().transform(this, name, existing, null, data);
          Class<?> newClass = defineClass(name, newClassBytes, 0, newClassBytes.length);
          if (resolve) {
            resolveClass(newClass);
          }
          return newClass;
        }
      }.loadClass(toLoad.getName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
