package io.perfmark;

import static io.perfmark.impl.Mark.NO_NANOTIME;
import static io.perfmark.impl.Mark.NO_TAG_ID;
import static io.perfmark.impl.Mark.NO_TAG_NAME;
import static io.perfmark.impl.Mark.Operation.EVENT;
import static io.perfmark.impl.Mark.Operation.LINK;
import static io.perfmark.impl.Mark.Operation.TASK_END;
import static io.perfmark.impl.Mark.Operation.TASK_NOTAG_END;
import static io.perfmark.impl.Mark.Operation.TASK_NOTAG_START;
import static io.perfmark.impl.Mark.Operation.TASK_START;
import static org.junit.Assert.assertEquals;

import io.perfmark.impl.Generator;
import io.perfmark.impl.Internal;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Marker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    Logger logger = Logger.getLogger(PerfMark.class.getName());
    Filter oldFilter = logger.getFilter();
    // This causes a cycle in case PerfMark tries to log during init.
    // Also, it silences initial nagging about missing generators.
    logger.setFilter(new Filter() {
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
    String oldProperty = System.getProperty(PerfMark.START_ENABLED_PROPERTY);
    System.setProperty(PerfMark.START_ENABLED_PROPERTY, "true");
    try {
      Class.forName(PerfMark.class.getName());
    } finally {
      if (oldProperty == null) {
        System.clearProperty(PerfMark.START_ENABLED_PROPERTY);
      } else {
        System.setProperty(PerfMark.START_ENABLED_PROPERTY, oldProperty);
      }
      logger.setFilter(oldFilter);
    }
  }

  @Test
  public void allMethodForward_taskName() {
    PerfMarkStorage.resetForTest();
    PerfMark.setEnabled(true);

    long gen = PerfMark.getGen();

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

    List<Mark> marks = getMine(PerfMarkStorage.read());

    assertEquals(marks.size(), 10);
    List<Mark> expected = Arrays.asList(
        Mark.create("task1", tag1.tagName, tag1.tagId, marks.get(0).getNanoTime(), gen, TASK_START),
        Mark.create("task2", tag2.tagName, tag2.tagId, marks.get(1).getNanoTime(), gen, TASK_START),
        Mark.create("task3", tag3.tagName, tag3.tagId, marks.get(2).getNanoTime(), gen, TASK_START),
        Mark.create(
            "task4", NO_TAG_NAME, NO_TAG_ID, marks.get(3).getNanoTime(), gen, TASK_NOTAG_START),
        Mark.create(Marker.NONE, NO_TAG_NAME, link.getId(), NO_NANOTIME, gen, LINK),
        Mark.create(Marker.NONE, NO_TAG_NAME, -link.getId(), NO_NANOTIME, gen, LINK),
        Mark.create(
            "task4", NO_TAG_NAME, NO_TAG_ID, marks.get(6).getNanoTime(), gen, TASK_NOTAG_END),
        Mark.create("task3", tag3.tagName, tag3.tagId, marks.get(7).getNanoTime(), gen, TASK_END),
        Mark.create("task2", tag2.tagName, tag2.tagId, marks.get(8).getNanoTime(), gen, TASK_END),
        Mark.create("task1", tag1.tagName, tag1.tagId, marks.get(9).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void allMethodForward_marker() {
    PerfMarkStorage.resetForTest();
    PerfMark.setEnabled(true);

    long gen = PerfMark.getGen();

    Tag tag1 = PerfMark.createTag(1);
    Tag tag2 = PerfMark.createTag("two");
    Tag tag3 = PerfMark.createTag("three", 3);
    Marker marker1 = Internal.createMarker("task1");
    Marker marker2 = Internal.createMarker("task2");
    Marker marker3 = Internal.createMarker("task3");
    Marker marker4 = Internal.createMarker("task4");
    Marker marker5 = Internal.createMarker("(link)");
    PerfMark.PackageAccess.Public.startTask(marker1, tag1);
    PerfMark.PackageAccess.Public.startTask(marker2, tag2);
    PerfMark.PackageAccess.Public.startTask(marker3, tag3);
    PerfMark.PackageAccess.Public.startTask(marker4);
    Link link = PerfMark.PackageAccess.Public.link(marker5);
    PerfMark.PackageAccess.Public.link(link.getId(), marker5);
    PerfMark.PackageAccess.Public.stopTask(marker4);
    PerfMark.PackageAccess.Public.stopTask(marker3, tag3);
    PerfMark.PackageAccess.Public.stopTask(marker2, tag2);
    PerfMark.PackageAccess.Public.stopTask(marker1, tag1);

    List<Mark> marks = getMine(PerfMarkStorage.read());

    assertEquals(marks.size(), 10);
    List<Mark> expected = Arrays.asList(
        Mark.create(marker1, tag1.tagName, tag1.tagId, marks.get(0).getNanoTime(), gen, TASK_START),
        Mark.create(marker2, tag2.tagName, tag2.tagId, marks.get(1).getNanoTime(), gen, TASK_START),
        Mark.create(marker3, tag3.tagName, tag3.tagId, marks.get(2).getNanoTime(), gen, TASK_START),
        Mark.create(
            marker4, NO_TAG_NAME, NO_TAG_ID, marks.get(3).getNanoTime(), gen, TASK_NOTAG_START),
        Mark.create(marker5, NO_TAG_NAME, link.getId(), NO_NANOTIME, gen, LINK),
        Mark.create(marker5, NO_TAG_NAME, -link.getId(), NO_NANOTIME, gen, LINK),
        Mark.create(
            marker4, NO_TAG_NAME, NO_TAG_ID, marks.get(6).getNanoTime(), gen, TASK_NOTAG_END),
        Mark.create(marker3, tag3.tagName, tag3.tagId, marks.get(7).getNanoTime(), gen, TASK_END),
        Mark.create(marker2, tag2.tagName, tag2.tagId, marks.get(8).getNanoTime(), gen, TASK_END),
        Mark.create(marker1, tag1.tagName, tag1.tagId, marks.get(9).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void allCloseablesForward_taskName() {
    PerfMarkStorage.resetForTest();
    PerfMark.setEnabled(true);

    long gen = PerfMark.getGen();

    Tag tag = PerfMark.createTag(1);
    Link link;
    try (PerfMarkCloseable closeable1 = PerfMark.record("task1", tag)) {
      try (PerfMarkCloseable closeable2 = PerfMark.record("task2")) {
        link = PerfMark.link();
        link.link();
      }
    }

    List<Mark> marks = getMine(PerfMarkStorage.read());

    assertEquals(marks.size(), 6);
    List<Mark> expected = Arrays.asList(
        Mark.create("task1", tag.tagName, tag.tagId, marks.get(0).getNanoTime(), gen, TASK_START),
        Mark.create(
            "task2", NO_TAG_NAME, NO_TAG_ID, marks.get(1).getNanoTime(), gen, TASK_NOTAG_START),
        Mark.create(Marker.NONE, NO_TAG_NAME, link.getId(), NO_NANOTIME, gen, LINK),
        Mark.create(Marker.NONE, NO_TAG_NAME, -link.getId(), NO_NANOTIME, gen, LINK),
        Mark.create(
            "task2", NO_TAG_NAME, NO_TAG_ID, marks.get(4).getNanoTime(), gen, TASK_NOTAG_END),
        Mark.create("task1", tag.tagName, tag.tagId, marks.get(5).getNanoTime(), gen, TASK_END));
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

  public static final class FakeMarkHolderProvider extends MarkHolderProvider {

    @Override
    public MarkHolder create() {
      return new FakeMarkHolder();
    }
  }

  public static final class FakeMarkHolder extends MarkHolder {
    private final List<Mark> marks = new ArrayList<>();

    @Override
    public void start(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      marks.add(Mark.create(taskName, tagName, tagId, nanoTime, gen, TASK_START));
    }

    @Override
    public void start(long gen, Marker marker, String tagName, long tagId, long nanoTime) {
      marks.add(Mark.create(marker, tagName, tagId, nanoTime, gen, TASK_START));
    }

    @Override
    public void start(long gen, String taskName, long nanoTime) {
      marks.add(Mark.create(
          taskName,
          Mark.NO_TAG_NAME,
          Mark.NO_TAG_ID,
          nanoTime,
          gen,
          Mark.Operation.TASK_NOTAG_START));
    }

    @Override
    public void start(long gen, Marker marker, long nanoTime) {
      marks.add(Mark.create(
          marker,
          Mark.NO_TAG_NAME,
          Mark.NO_TAG_ID,
          nanoTime,
          gen,
          Mark.Operation.TASK_NOTAG_START));
    }

    @Override
    public void link(long gen, long linkId, Marker marker) {
      marks.add(Mark.create(
          marker,
          Mark.NO_TAG_NAME,
          linkId,
          Mark.NO_NANOTIME,
          gen,
          Mark.Operation.LINK));
    }

    @Override
    public void stop(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      marks.add(Mark.create(
          taskName,
          tagName,
          tagId,
          nanoTime,
          gen,
          TASK_END));
    }

    @Override
    public void stop(long gen, Marker marker, String tagName, long tagId, long nanoTime) {
      marks.add(Mark.create(
          marker,
          tagName,
          tagId,
          nanoTime,
          gen,
          TASK_END));
    }

    @Override
    public void stop(long gen, String taskName, long nanoTime) {
      marks.add(Mark.create(
          taskName,
          Mark.NO_TAG_NAME,
          Mark.NO_TAG_ID,
          nanoTime,
          gen,
          Mark.Operation.TASK_NOTAG_END));
    }

    @Override
    public void stop(long gen, Marker marker, long nanoTime) {
      marks.add(Mark.create(
          marker,
          Mark.NO_TAG_NAME,
          Mark.NO_TAG_ID,
          nanoTime,
          gen,
          Mark.Operation.TASK_NOTAG_END));
    }

    @Override
    public void event(
        long gen, String eventName, String tagName, long tagId, long nanoTime, long durationNanos) {
      marks.add(Mark.create(eventName, tagName, tagId, nanoTime, gen, EVENT));
    }

    @Override
    public void event(
        long gen, Marker marker, String tagName, long tagId, long nanoTime, long durationNanos) {
      marks.add(Mark.create(marker, tagName, tagId, nanoTime, gen, EVENT));
    }

    @Override
    public void event(long gen, String eventName, long nanoTime, long durationNanos) {
      marks.add(Mark.create(
          eventName,
          Mark.NO_TAG_NAME,
          Mark.NO_TAG_ID,
          nanoTime,
          gen,
          Mark.Operation.EVENT_NOTAG));
    }

    @Override
    public void event(long gen, Marker marker, long nanoTime, long durationNanos) {
      marks.add(Mark.create(
          marker,
          Mark.NO_TAG_NAME,
          Mark.NO_TAG_ID,
          nanoTime,
          gen,
          Mark.Operation.EVENT_NOTAG));
    }

    @Override
    public void resetForTest() {
      marks.clear();
    }

    @Override
    public List<Mark> read(boolean readerIsWriter) {
      return Collections.unmodifiableList(new ArrayList<>(marks));
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
}
