package io.perfmark;

import static io.perfmark.impl.Mark.NO_NANOTIME;
import static io.perfmark.impl.Mark.NO_TAG_ID;
import static io.perfmark.impl.Mark.NO_TAG_NAME;
import static io.perfmark.impl.Mark.Operation.LINK;
import static io.perfmark.impl.Mark.Operation.TASK_END;
import static io.perfmark.impl.Mark.Operation.TASK_END_T;
import static io.perfmark.impl.Mark.Operation.TASK_START;
import static io.perfmark.impl.Mark.Operation.TASK_START_T;
import static org.junit.Assert.assertEquals;

import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
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
    Link link = PerfMark.link();
    link.link();
    PerfMark.stopTask("task4");
    PerfMark.stopTask("task3", tag3);
    PerfMark.stopTask("task2", tag2);
    PerfMark.stopTask("task1", tag1);

    List<Mark> marks = getMine(Storage.read());

    assertEquals(marks.size(), 10);
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
            Mark.create(null, Marker.NONE, NO_TAG_NAME, link.linkId, NO_NANOTIME, gen, LINK),
            Mark.create(null, Marker.NONE, NO_TAG_NAME, -link.linkId, NO_NANOTIME, gen, LINK),
            Mark.create(
                "task4",
                Marker.NONE,
                NO_TAG_NAME,
                NO_TAG_ID,
                marks.get(6).getNanoTime(),
                gen,
                TASK_END),
            Mark.create(
                "task3",
                Marker.NONE,
                tag3.tagName,
                tag3.tagId,
                marks.get(7).getNanoTime(),
                gen,
                TASK_END_T),
            Mark.create(
                "task2",
                Marker.NONE,
                tag2.tagName,
                tag2.tagId,
                marks.get(8).getNanoTime(),
                gen,
                TASK_END_T),
            Mark.create(
                "task1",
                Marker.NONE,
                tag1.tagName,
                tag1.tagId,
                marks.get(9).getNanoTime(),
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

  @SuppressWarnings("deprecation") // We must be alive to find our own, so it's okay.
  private static List<Mark> getMine(List<MarkList> markLists) {
    for (MarkList markList : markLists) {
      if (markList.getThreadId() == Thread.currentThread().getId()) {

        return markList.getMarks();
      }
    }
    return null;
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
