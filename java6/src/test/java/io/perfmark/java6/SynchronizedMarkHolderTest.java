package io.perfmark.java6;

import static io.perfmark.impl.Mark.NO_NANOTIME;
import static io.perfmark.impl.Mark.NO_TAG_ID;
import static io.perfmark.impl.Mark.NO_TAG_NAME;
import static io.perfmark.impl.Mark.Operation.EVENT;
import static io.perfmark.impl.Mark.Operation.EVENT_M;
import static io.perfmark.impl.Mark.Operation.EVENT_T;
import static io.perfmark.impl.Mark.Operation.EVENT_TM;
import static io.perfmark.impl.Mark.Operation.LINK;
import static io.perfmark.impl.Mark.Operation.LINK_M;
import static io.perfmark.impl.Mark.Operation.TASK_END;
import static io.perfmark.impl.Mark.Operation.TASK_END_M;
import static io.perfmark.impl.Mark.Operation.TASK_END_T;
import static io.perfmark.impl.Mark.Operation.TASK_END_TM;
import static io.perfmark.impl.Mark.Operation.TASK_START;
import static io.perfmark.impl.Mark.Operation.TASK_START_M;
import static io.perfmark.impl.Mark.Operation.TASK_START_T;
import static io.perfmark.impl.Mark.Operation.TASK_START_TM;
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

public class SynchronizedMarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;

  public final MarkHolder mh = new SynchronizedMarkHolder();

  @Test
  public void taskTagStartStop() {
    mh.start(gen, "task", 3);
    mh.stop(gen, "task", 4);

    List<Mark> marks = mh.read(false);
    assertEquals(2, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_START),
            Mark.create("task", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_tag() {
    mh.start(gen, "task", "tag", 9, 3);
    mh.stop(gen, "task", "tag", 9, 4);

    List<Mark> marks = mh.read(false);
    assertEquals(2, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task", Marker.NONE, "tag", 9, 3, gen, TASK_START_T),
            Mark.create("task", Marker.NONE, "tag", 9, 4, gen, TASK_END_T));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_marker() {
    Marker marker = io.perfmark.impl.Internal.createMarker("(link)");
    mh.start(gen, "task", marker, 3);
    mh.stop(gen, "task", marker, 4);

    List<Mark> marks = mh.read(false);
    assertEquals(2, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task", marker, NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_START_M),
            Mark.create("task", marker, NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_END_M));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStop_tag_marker() {
    Marker marker = io.perfmark.impl.Internal.createMarker("(link)");
    mh.start(gen, "task", marker, "tag", 2, 3);
    mh.stop(gen, "task", marker, "tag", 2, 4);

    List<Mark> marks = mh.read(false);
    assertEquals(2, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task", marker, "tag", 2, 3, gen, TASK_START_TM),
            Mark.create("task", marker, "tag", 2, 4, gen, TASK_END_TM));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStartStopStop() {
    mh.start(gen, "task1", 3);
    mh.start(gen, "task2", 4);
    mh.stop(gen, "task2", 5);
    mh.stop(gen, "task1", 6);

    List<Mark> marks = mh.read(false);

    assertEquals(4, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task1", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_START),
            Mark.create("task2", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_START),
            Mark.create("task2", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 5, gen, TASK_END),
            Mark.create("task1", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 6, gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void event() {
    Marker marker = Internal.createMarker("event");

    mh.event(gen, "task1", 8, -1);
    mh.event(gen, "ev", marker, 7, -1);
    mh.event(gen, "task2", 5, -1);
    mh.event(gen, "ev", marker, 6, -1);

    List<Mark> marks = mh.read(false);

    assertEquals(4, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task1", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 8, gen, EVENT),
            Mark.create("ev", marker, NO_TAG_NAME, NO_TAG_ID, 7, gen, EVENT_M),
            Mark.create("task2", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 5, gen, EVENT),
            Mark.create("ev", marker, NO_TAG_NAME, NO_TAG_ID, 6, gen, EVENT_M));
    assertEquals(expected, marks);
  }

  @Test
  public void event_tag() {
    Marker marker = Internal.createMarker("event");

    mh.event(gen, "task1", "tag", 4, 8, -1);
    mh.event(gen, "ev", marker, "tag", 4, 7, -1);
    mh.event(gen, "task2", "tag", 4, 5, -1);
    mh.event(gen, "ev", marker, "tag", 4, 6, -1);

    List<Mark> marks = mh.read(false);

    assertEquals(4, marks.size());
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task1", Marker.NONE, "tag", 4, 8, gen, EVENT_T),
            Mark.create("ev", marker, "tag", 4, 7, gen, EVENT_TM),
            Mark.create("task2", Marker.NONE, "tag", 4, 5, gen, EVENT_T),
            Mark.create("ev", marker, "tag", 4, 6, gen, EVENT_TM));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut() {
    mh.start(gen, "task1", 3);
    mh.link(gen, 9);
    mh.link(gen, -9);
    mh.stop(gen, "task1", 4);

    List<Mark> marks = mh.read(false);

    assertEquals(marks.size(), 4);
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task1", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_START),
            Mark.create(null, Marker.NONE, NO_TAG_NAME, 9, NO_NANOTIME, gen, LINK),
            Mark.create(null, Marker.NONE, NO_TAG_NAME, -9, NO_NANOTIME, gen, LINK),
            Mark.create("task1", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut_marker() {
    Marker marker = Internal.createMarker("(link)");
    mh.start(gen, "task1", 3);
    mh.link(gen, 9, marker);
    mh.link(gen, -9, marker);
    mh.stop(gen, "task1", 4);

    List<Mark> marks = mh.read(false);

    assertEquals(marks.size(), 4);
    List<Mark> expected =
        Arrays.asList(
            Mark.create("task1", Marker.NONE, NO_TAG_NAME, NO_TAG_ID, 3, gen, TASK_START),
            Mark.create(null, marker, NO_TAG_NAME, 9, NO_NANOTIME, gen, LINK_M),
            Mark.create(null, marker, NO_TAG_NAME, -9, NO_NANOTIME, gen, LINK_M),
            Mark.create("task1", marker, NO_TAG_NAME, NO_TAG_ID, 4, gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void read_getsAllIfWriter() {
    mh.start(gen, "task", 3);

    List<Mark> marks = mh.read(false);

    assertEquals(marks.size(), 1);
  }

  @Test
  public void read_getsAllButLastIfNotWriter() {
    Assume.assumeTrue("holder " + mh + " is not fixed size", mh instanceof SynchronizedMarkHolder);
    int events = mh.maxMarks() - 1;
    for (int i = 0; i < events; i++) {
      mh.start(gen, "task", 3);
    }

    List<Mark> marks = mh.read(true);
    assertEquals(events, marks.size());
  }

  @Test
  public void read_getsAllIfNotWriterButNoWrap() {
    Assume.assumeTrue("holder " + mh + " is not fixed size", mh instanceof SynchronizedMarkHolder);
    int events = mh.maxMarks() - 2;
    for (int i = 0; i < events; i++) {
      mh.start(gen, "task", 3);
    }

    List<Mark> marks = mh.read(true);
    assertEquals(events, marks.size());
  }
}
