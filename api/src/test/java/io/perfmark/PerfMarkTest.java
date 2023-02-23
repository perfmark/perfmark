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

package io.perfmark;

import static io.perfmark.impl.Mark.NO_TAG_ID;
import static org.junit.Assert.assertEquals;

import com.google.common.truth.Truth;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Storage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkTest {

  /**
   * This test checks to see if PerfMark can be used from a Logger, which is used for recording if
   * there is trouble turning on. PerfMark should set a noop implementation before recording any
   * problems with boot.
   */
  @Test
  public void noBootCycle() throws Exception {
    AtomicReference<LogRecord> ref = new AtomicReference<>();
    ClassLoader loader =
        new TestClassLoader(
            getClass().getClassLoader(), "io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
    Class<?> clz = Class.forName(PerfMark.class.getName(), false, loader);

    Class<?> filterClz = Class.forName(TracingFilter.class.getName(), false, loader);
    Constructor<? extends Filter> ctor =
        filterClz
            .asSubclass(Filter.class)
            .getDeclaredConstructor(Class.class, AtomicReference.class);
    ctor.setAccessible(true);
    Filter filter = ctor.newInstance(clz, ref);
    Logger logger = Logger.getLogger(PerfMark.class.getName());
    Level oldLevel = logger.getLevel();
    Filter oldFilter = logger.getFilter();
    logger.setLevel(Level.ALL);
    logger.setFilter(filter);
    try {
      runWithProperty(
          System.getProperties(),
          "io.perfmark.PerfMark.debug",
          "true",
          () -> {
            try {
              // Force Initialization.
              Class.forName(PerfMark.class.getName(), true, loader);
            } finally {
              logger.setFilter(oldFilter);
            }
            return null;
          });
    } finally {
      logger.setFilter(oldFilter);
      logger.setLevel(oldLevel);
    }

    // The actual SecretPerfMarkImpl is not part of the custom class loader above, so it will be a
    // class mismatch when
    // it tries to implement Impl.
    // The message will be the default still, so check for that, to prove it did something.
    Truth.assertThat(ref.get()).isNotNull();
    Truth.assertThat(ref.get().getMessage()).contains("Error during PerfMark.<clinit>");
  }







  @Test
  public void allMethodForward_taskName() {
    Storage.resetForThread();
    PerfMark.setEnabled(true);

    long gen = getGen();

    Tag tag1 = PerfMark.createTag(1);
    Tag tag2 = PerfMark.createTag("two");
    Tag tag3 = PerfMark.createTag("three", 3);
    PerfMark.startTask("task1", tag1);
    PerfMark.startTask("task2", tag2);
    PerfMark.startTask("task3", tag3);
    PerfMark.startTask("task4");
    PerfMark.startTask("task5", String::valueOf);
    PerfMark.attachTag(PerfMark.createTag("extra"));
    PerfMark.attachTag("name", "extra2", String::valueOf);
    Link link = PerfMark.linkOut();
    PerfMark.linkIn(link);
    PerfMark.stopTask();
    PerfMark.stopTask("task4");
    PerfMark.stopTask("task3", tag3);
    PerfMark.stopTask("task2", tag2);
    PerfMark.stopTask("task1", tag1);
    try (TaskCloseable task6 = PerfMark.traceTask("task6")) {
      try (TaskCloseable task7 = PerfMark.traceTask("task7", String::valueOf)) {}
    }

    List<Mark> marks = Storage.readForTest();

    Truth.assertThat(marks).hasSize(24);
    List<Mark> expected =
        Arrays.asList(
            Mark.taskStart(gen, marks.get(0).getNanoTime(), "task1"),
            Mark.tag(gen, tag1.tagName, tag1.tagId),
            Mark.taskStart(gen, marks.get(2).getNanoTime(), "task2"),
            Mark.tag(gen, tag2.tagName, tag2.tagId),
            Mark.taskStart(gen, marks.get(4).getNanoTime(), "task3"),
            Mark.tag(gen, tag3.tagName, tag3.tagId),
            Mark.taskStart(gen, marks.get(6).getNanoTime(), "task4"),
            Mark.taskStart(gen, marks.get(7).getNanoTime(), "task5"),
            Mark.tag(gen, "extra", NO_TAG_ID),
            Mark.keyedTag(gen, "name", "extra2"),
            Mark.link(gen, link.linkId),
            Mark.link(gen, -link.linkId),
            Mark.taskEnd(gen, marks.get(12).getNanoTime()),
            Mark.taskEnd(gen, marks.get(13).getNanoTime(), "task4"),
            Mark.tag(gen, tag3.tagName, tag3.tagId),
            Mark.taskEnd(gen, marks.get(15).getNanoTime(), "task3"),
            Mark.tag(gen, tag2.tagName, tag2.tagId),
            Mark.taskEnd(gen, marks.get(17).getNanoTime(), "task2"),
            Mark.tag(gen, tag1.tagName, tag1.tagId),
            Mark.taskEnd(gen, marks.get(19).getNanoTime(), "task1"),
            Mark.taskStart(gen, marks.get(20).getNanoTime(), "task6"),
            Mark.taskStart(gen, marks.get(21).getNanoTime(), "task7"),
            Mark.taskEnd(gen, marks.get(22).getNanoTime()),
            Mark.taskEnd(gen, marks.get(23).getNanoTime()));
    assertEquals(expected, marks);
  }

  @Test
  public void attachTag_nullFunctionFailsSilently() {
    Storage.resetForThread();
    PerfMark.setEnabled(true);

    PerfMark.attachTag("name", "extra2", null);

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(1);
  }

  @Test
  public void attachTag_functionFailureSucceeds() {
    Storage.resetForThread();
    PerfMark.setEnabled(true);

    PerfMark.attachTag(
        "name",
        "extra2",
        v -> {
          throw new RuntimeException("bad");
        });

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(1);
  }

  @Test
  public void attachTag_functionFailureObjectFailureSucceeds() {
    Storage.resetForThread();
    PerfMark.setEnabled(true);
    Object o =
        new Object() {
          @Override
          public String toString() {
            throw new RuntimeException("worse");
          }
        };

    PerfMark.attachTag(
        "name",
        o,
        v -> {
          throw new RuntimeException("bad");
        });

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(1);
  }

  @Test
  public void attachTag_doubleFunctionFailureSucceeds() {
    Storage.resetForThread();
    PerfMark.setEnabled(true);

    PerfMark.attachTag(
        "name",
        "extra2",
        v -> {
          throw new RuntimeException("bad") {
            @Override
            public String getMessage() {
              throw new RuntimeException("worse");
            }
          };
        });

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(1);
  }

  public static final class FakeGenerator extends Generator {

    long generation;

    @Override
    public void setGeneration(long generation) {
      this.generation = generation;
    }

    @Override
    public long getGeneration() {
      return generation;
    }
  }

  private static long getGen() {
    try {
      Class<?> implClz = Class.forName("io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
      Method method = implClz.getDeclaredMethod("getGen");
      method.setAccessible(true);
      return (long) method.invoke(null);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @CanIgnoreReturnValue
  static <T> T runWithProperty(
      Properties properties, String name, String value, Callable<T> runnable) throws Exception {
    if (properties.containsKey(name)) {
      String oldProp;
      oldProp = properties.getProperty(name);
      try {
        System.setProperty(name, value);
        return runnable.call();
      } finally {
        properties.setProperty(name, oldProp);
      }
    } else {
      try {
        System.setProperty(name, value);
        return runnable.call();
      } finally {
        properties.remove(name);
      }
    }
  }

  static class TestClassLoader extends ClassLoader {

    private final List<String> classesToDrop;

    TestClassLoader(ClassLoader parent, String... classesToDrop) {
      super(parent);
      this.classesToDrop = Arrays.asList(classesToDrop);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (classesToDrop.contains(name)) {
        throw new ClassNotFoundException();
      }
      if (!name.startsWith("io.perfmark.")) {
        return super.loadClass(name, resolve);
      }
      try (InputStream is = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
        if (is == null) {
          throw new ClassNotFoundException(name);
        }
        byte[] data = is.readAllBytes();
        Class<?> clz = defineClass(name, data, 0, data.length);
        if (resolve) {
          resolveClass(clz);
        }
        return clz;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class TracingFilter implements Filter {
    private final AtomicReference<LogRecord> ref;

    TracingFilter(Class<?> clz, AtomicReference<LogRecord> ref) {
      assertEquals(PerfMark.class, clz);
      this.ref = ref;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
      PerfMark.startTask("isLoggable");
      try {
        ref.compareAndExchange(null, record);
        return false;
      } finally {
        PerfMark.stopTask("isLoggable");
      }
    }
  }
}
