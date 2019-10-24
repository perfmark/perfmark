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

import static io.perfmark.impl.Mark.NO_NANOTIME;
import static io.perfmark.impl.Mark.NO_TAG_ID;
import static io.perfmark.impl.Mark.NO_TAG_NAME;
import static io.perfmark.impl.Mark.Operation.ATTACH_TAG;
import static io.perfmark.impl.Mark.Operation.LINK;
import static io.perfmark.impl.Mark.Operation.TASK_END;
import static io.perfmark.impl.Mark.Operation.TASK_END_T;
import static io.perfmark.impl.Mark.Operation.TASK_START;
import static io.perfmark.impl.Mark.Operation.TASK_START_T;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Marker;
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
    Class<?> implClz = Class.forName("io.perfmark.impl.SecretPerfMarkImpl");
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
            PerfMark.tracer().startTask("isLoggable");
            try {
              return false;
            } finally {
              PerfMark.tracer().stopTask("isLoggable");
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
  public void cantExtend() {
    try {
      new PerfMark();
      fail();
    } catch (UnsupportedOperationException e) {
      // ok.
    }
  }

  @Test
  public void allMethodForward_taskName() {
    Storage.resetForTest();
    PerfMark.tracer().setEnabled(true);

    long gen = getGen();

    Tag tag1 = PerfMark.tracer().createTag(1);
    Tag tag2 = PerfMark.tracer().createTag("two");
    Tag tag3 = PerfMark.tracer().createTag("three", 3);
    PerfMark.tracer().startTask("task1", tag1);
    PerfMark.tracer().startTask("task2", tag2);
    PerfMark.tracer().startTask("task3", tag3);
    PerfMark.tracer().startTask("task4");
    PerfMark.tracer().attachTag(PerfMark.tracer().createTag("extra"));
    Link link = PerfMark.tracer().linkOut();
    PerfMark.tracer().linkIn(link);
    PerfMark.tracer().stopTask("task4");
    PerfMark.tracer().stopTask("task3", tag3);
    PerfMark.tracer().stopTask("task2", tag2);
    PerfMark.tracer().stopTask("task1", tag1);

    List<Mark> marks = Storage.readForTest().getMarks();

    Truth.assertThat(marks).hasSize(11);
    List<Mark> expected =
        Arrays.asList(
            Mark.create(
                "task1",
                Marker.NONE,
                tag1.tagName,
                tag1.tagId,
                marks.get(0).getNanoTime(),
                gen,
                TASK_START_T),
            Mark.create(
                "task2",
                Marker.NONE,
                tag2.tagName,
                tag2.tagId,
                marks.get(1).getNanoTime(),
                gen,
                TASK_START_T),
            Mark.create(
                "task3",
                Marker.NONE,
                tag3.tagName,
                tag3.tagId,
                marks.get(2).getNanoTime(),
                gen,
                TASK_START_T),
            Mark.create(
                "task4",
                Marker.NONE,
                NO_TAG_NAME,
                NO_TAG_ID,
                marks.get(3).getNanoTime(),
                gen,
                TASK_START),
            Mark.create(null, Marker.NONE, "extra", NO_TAG_ID, NO_NANOTIME, gen, ATTACH_TAG),
            Mark.create(null, Marker.NONE, NO_TAG_NAME, link.linkId, NO_NANOTIME, gen, LINK),
            Mark.create(null, Marker.NONE, NO_TAG_NAME, -link.linkId, NO_NANOTIME, gen, LINK),
            Mark.create(
                "task4",
                Marker.NONE,
                NO_TAG_NAME,
                NO_TAG_ID,
                marks.get(7).getNanoTime(),
                gen,
                TASK_END),
            Mark.create(
                "task3",
                Marker.NONE,
                tag3.tagName,
                tag3.tagId,
                marks.get(8).getNanoTime(),
                gen,
                TASK_END_T),
            Mark.create(
                "task2",
                Marker.NONE,
                tag2.tagName,
                tag2.tagId,
                marks.get(9).getNanoTime(),
                gen,
                TASK_END_T),
            Mark.create(
                "task1",
                Marker.NONE,
                tag1.tagName,
                tag1.tagId,
                marks.get(10).getNanoTime(),
                gen,
                TASK_END_T));
    assertEquals(expected, marks);
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
