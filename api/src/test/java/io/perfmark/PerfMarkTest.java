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
import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Storage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
    Class<?> implClz = Class.forName("io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
    Field propertyField = implClz.getDeclaredField("START_ENABLED_PROPERTY");
    propertyField.setAccessible(true);
    String startEnabledProperty = (String) propertyField.get(null);
    Logger logger = Logger.getLogger(PerfMark.class.getName());
    Filter oldFilter = logger.getFilter();
    // This causes a cycle in case PerfMark tries to log during init.
    // Also, it silences initial nagging about missing generators.
    logger.setFilter(
        new Filter() {
          @Override
          public boolean isLoggable(LogRecord record) {
            PerfMark.startTask("isLoggable");
            try {
              return false;
            } finally {
              PerfMark.stopTask("isLoggable");
            }
          }
        });
    // Try to get PerfMark to accidentally log that it is enabled.  We are careful to not
    // accidentally cause class initialization early here, as START_ENABLED_PROPERTY is a
    // constant.
    String oldProperty = System.getProperty(startEnabledProperty);
    System.setProperty(startEnabledProperty, "true");
    try {
      Class.forName(PerfMark.class.getName());
    } finally {
      if (oldProperty == null) {
        System.clearProperty(startEnabledProperty);
      } else {
        System.setProperty(startEnabledProperty, oldProperty);
      }
      logger.setFilter(oldFilter);
    }
  }

  @Test
  public void allMethodForward_taskName() {
    Storage.resetForTest();
    PerfMark.setEnabled(true);

    long gen = getGen();

    Tag tag1 = PerfMark.createTag(1);
    Tag tag2 = PerfMark.createTag("two");
    Tag tag3 = PerfMark.createTag("three", 3);
    PerfMark.startTask("task1", tag1);
    PerfMark.startTask("task2", tag2);
    PerfMark.startTask("task3", tag3);
    PerfMark.startTask("task4");
    PerfMark.attachTag(PerfMark.createTag("extra"));
    PerfMark.attachTag("name", "extra2", String::valueOf);
    Link link = PerfMark.linkOut();
    PerfMark.linkIn(link);
    PerfMark.stopTask("task4");
    PerfMark.stopTask("task3", tag3);
    PerfMark.stopTask("task2", tag2);
    PerfMark.stopTask("task1", tag1);

    List<Mark> marks = Storage.readForTest();

    Truth.assertThat(marks).hasSize(18);
    List<Mark> expected =
        Arrays.asList(
            Mark.taskStart(gen, marks.get(0).getNanoTime(), "task1"),
            Mark.tag(gen, tag1.tagName, tag1.tagId),
            Mark.taskStart(gen, marks.get(2).getNanoTime(), "task2"),
            Mark.tag(gen, tag2.tagName, tag2.tagId),
            Mark.taskStart(gen, marks.get(4).getNanoTime(), "task3"),
            Mark.tag(gen, tag3.tagName, tag3.tagId),
            Mark.taskStart(gen, marks.get(6).getNanoTime(), "task4"),
            Mark.tag(gen, "extra", NO_TAG_ID),
            Mark.keyedTag(gen, "name", "extra2"),
            Mark.link(gen, link.linkId),
            Mark.link(gen, -link.linkId),
            Mark.taskEnd(gen, marks.get(11).getNanoTime(), "task4"),
            Mark.tag(gen, tag3.tagName, tag3.tagId),
            Mark.taskEnd(gen, marks.get(13).getNanoTime(), "task3"),
            Mark.tag(gen, tag2.tagName, tag2.tagId),
            Mark.taskEnd(gen, marks.get(15).getNanoTime(), "task2"),
            Mark.tag(gen, tag1.tagName, tag1.tagId),
            Mark.taskEnd(gen, marks.get(17).getNanoTime(), "task1"));
    assertEquals(expected, marks);
  }

  @Test
  public void attachTag_nullFunctionFailsSilently() {
    Storage.resetForTest();
    PerfMark.setEnabled(true);

    PerfMark.attachTag("name", "extra2", null);

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(0);
  }

  @Test
  public void attachTag_functionFailureSucceeds() {
    Storage.resetForTest();
    PerfMark.setEnabled(true);

    PerfMark.attachTag(
        "name",
        "extra2",
        v -> {
          throw new RuntimeException("bad");
        });

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(0);
  }

  @Test
  public void attachTag_functionFailureObjectFailureSucceeds() {
    Storage.resetForTest();
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
    Truth.assertThat(marks).hasSize(0);
  }

  @Test
  public void attachTag_doubleFunctionFailureSucceeds() {
    Storage.resetForTest();
    PerfMark.setEnabled(true);

    PerfMark.attachTag(
        "name",
        "extra2",
        v -> {
          throw new RuntimeException("bad") {
            @Override
            public String toString() {
              throw new RuntimeException("worse");
            }
          };
        });

    List<Mark> marks = Storage.readForTest();
    Truth.assertThat(marks).hasSize(0);
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
}
