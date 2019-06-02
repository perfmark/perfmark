package io.perfmark.java9;

import static io.perfmark.impl.Mark.NO_NANOTIME;
import static io.perfmark.impl.Mark.NO_TAG_ID;
import static io.perfmark.impl.Mark.NO_TAG_NAME;
import static io.perfmark.impl.Mark.Operation.EVENT;
import static io.perfmark.impl.Mark.Operation.EVENT_NOTAG;
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
import io.perfmark.impl.Marker;
import java.util.Arrays;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VarHandleMarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;

  public final MarkHolder mh = new VarHandleMarkHolder();

  @Test
  public void taskTagStartStop_name() {
    mh.start(gen, "task", "tag", 2, 3);
    mh.stop(gen, "task", "tag", 2, 4);

    List<Mark> marks = mh.read(true);
    assertEquals(2, marks.size());
    List<Mark> expected = Arrays.asList(
        Mark.create("task", "tag", 2, 3, gen, TASK_START),
        Mark.create("task", "tag", 2, 4, gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_marker() {
    Marker marker = Internal.createMarker("(link)");
    mh.start(gen, marker, "tag", 2, 3);
    mh.stop(gen, marker, "tag", 2, 4);

    List<Mark> marks = mh.read(true);
    assertEquals(2, marks.size());
    List<Mark> expected = Arrays.asList(
        Mark.create(marker, "tag", 2, 3, gen, TASK_START),
        Mark.create(marker, "tag", 2, 4, gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStop_name() {
    mh.start(gen, "task", 3);
    mh.stop(gen, "task", 4);

    List<Mark> marks = mh.read(true);
    assertEquals(2, marks.size());
    List<Mark> expected = Arrays.asList(
        Mark.create("task", NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_NOTAG_START),
        Mark.create("task", NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_NOTAG_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStop_marker() {
    Marker marker = Internal.createMarker("(link)");
    mh.start(gen, marker, 3);
    mh.stop(gen, marker, 4);

    List<Mark> marks = mh.read(true);
    assertEquals(2, marks.size());
    List<Mark> expected = Arrays.asList(
        Mark.create(marker, NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_NOTAG_START),
        Mark.create(marker, NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_NOTAG_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStartStopStop() {
    mh.start(gen, "task1", 3);
    mh.start(gen, "task2", 4);
    mh.stop(gen, "task2", 5);
    mh.stop(gen, "task1", 6);

    List<Mark> marks = mh.read(true);

    assertEquals(4, marks.size());
    List<Mark> expected = Arrays.asList(
        Mark.create("task1", NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_NOTAG_START),
        Mark.create("task2", NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_NOTAG_START),
        Mark.create("task2", NO_TAG_NAME, NO_TAG_ID, 5, gen, TASK_NOTAG_END),
        Mark.create("task1", NO_TAG_NAME, NO_TAG_ID, 6, gen, TASK_NOTAG_END));
    assertEquals(expected, marks);
  }

  @Test
  public void event() {
    Marker marker = Internal.createMarker("event");

    mh.event(gen, "task1", "tag1", 9, 8, -1);
    mh.event(gen, marker, "tag1", 9, 7, -1);
    mh.event(gen, "task2", 5, -1);
    mh.event(gen, marker, 6, -1);

    List<Mark> marks = mh.read(true);

    assertEquals(4, marks.size());
    List<Mark> expected = Arrays.asList(
        Mark.create("task1", "tag1", 9, 8, gen, EVENT),
        Mark.create(marker, "tag1", 9, 7, gen, EVENT),
        Mark.create("task2", NO_TAG_NAME, NO_TAG_ID, 5, gen, EVENT_NOTAG),
        Mark.create(marker, NO_TAG_NAME, NO_TAG_ID, 6, gen, EVENT_NOTAG));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut() {
    Marker marker = Internal.createMarker("(link)");
    mh.start(gen, "task1", 3);
    mh.link(gen, 9, marker);
    mh.link(gen, -9, marker);
    mh.stop(gen, "task1", 4);

    List<Mark> marks = mh.read(true);

    assertEquals(marks.size(), 4);
    List<Mark> expected = Arrays.asList(
        Mark.create("task1", NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_NOTAG_START),
        Mark.create(marker, NO_TAG_NAME, 9, NO_NANOTIME, gen, LINK),
        Mark.create(marker, NO_TAG_NAME, -9, NO_NANOTIME, gen, LINK),
        Mark.create("task1", NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_NOTAG_END));
    assertEquals(expected, marks);
  }

  @Test
  public void read_getsAllIfWriter() {
    mh.start(gen, "task", 3);

    List<Mark> marks = mh.read(true);

    assertEquals(marks.size(), 1);
  }

  @Test
  public void read_getsAllButLastIfNotWriter() {
    Assume.assumeTrue("holder " + mh + " is not fixed size", mh instanceof VarHandleMarkHolder);
    int events = mh.maxMarks() - 1;
    for (int i = 0; i < events; i++) {
      mh.start(gen, "task", 3);
    }

    List<Mark> marks = mh.read(false);
    assertEquals(events - 1, marks.size());
  }

  @Test
  public void read_getsAllIfNotWriterButNoWrap() {
    Assume.assumeTrue("holder " + mh + " is not fixed size", mh instanceof VarHandleMarkHolder);
    int events = mh.maxMarks() - 2;
    for (int i = 0; i < events; i++) {
      mh.start(gen, "task", 3);
    }

    List<Mark> marks = mh.read(false);
    assertEquals(events, marks.size());
  }
}