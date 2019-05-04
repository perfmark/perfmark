package io.grpc.contrib.perfmark;

import static io.grpc.contrib.perfmark.MarkList.Mark.Operation.LINK;
import static io.grpc.contrib.perfmark.MarkList.Mark.Operation.TASK_END;
import static io.grpc.contrib.perfmark.MarkList.Mark.Operation.TASK_NOTAG_END;
import static io.grpc.contrib.perfmark.MarkList.Mark.Operation.TASK_NOTAG_START;
import static io.grpc.contrib.perfmark.MarkList.Mark.Operation.TASK_START;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class PerfMarkStorageTest {

  private static long gen;

  @BeforeClass
  public static void setUpStatic() {
    PerfMark.setEnabled(true);
    gen = PerfMark.getActualGeneration();
  }

  @Test
  public void taskTagStartStop_name() {
    PerfMarkStorage.resetForTest();
    Tag tag = new Tag("tag", 2);
    PerfMarkStorage.startAnyways(gen, "task", tag);
    PerfMarkStorage.stopAnyways(gen, "task", tag);

    List<MarkList.Mark> marks = getMine(PerfMarkStorage.read()).getMarks();

    assertEquals(marks.size(), 2);
    List<MarkList.Mark> expected = Arrays.asList(
        new MarkList.Mark("task", "tag", 2, marks.get(0).getNanoTime(), gen, TASK_START),
        new MarkList.Mark("task", "tag", 2, marks.get(1).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_marker() {
    PerfMarkStorage.resetForTest();
    Tag tag = new Tag("tag", 2);
    Marker marker = Marker.create("task");
    PerfMarkStorage.startAnyways(gen, marker, tag);
    PerfMarkStorage.stopAnyways(gen, marker, tag);

    List<MarkList.Mark> marks = getMine(PerfMarkStorage.read()).getMarks();

    assertEquals(marks.size(), 2);
    List<MarkList.Mark> expected = Arrays.asList(
        new MarkList.Mark(marker, "tag", 2, marks.get(0).getNanoTime(), gen, TASK_START),
        new MarkList.Mark(marker, "tag", 2, marks.get(1).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStartStopStop() {
    PerfMarkStorage.resetForTest();
    PerfMarkStorage.startAnyways(gen, "task1");
    PerfMarkStorage.startAnyways(gen, "task2");
    PerfMarkStorage.stopAnyways(gen, "task2");
    PerfMarkStorage.stopAnyways(gen, "task1");

    List<MarkList.Mark> marks = getMine(PerfMarkStorage.read()).getMarks();

    assertEquals(marks.size(), 4);
    List<MarkList.Mark> expected = Arrays.asList(
        new MarkList.Mark("task1", null, 0, marks.get(0).getNanoTime(), gen, TASK_NOTAG_START),
        new MarkList.Mark("task2", null, 0, marks.get(1).getNanoTime(), gen, TASK_NOTAG_START),
        new MarkList.Mark("task2", null, 0, marks.get(2).getNanoTime(), gen, TASK_NOTAG_END),
        new MarkList.Mark("task1", null, 0, marks.get(3).getNanoTime(), gen, TASK_NOTAG_END));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut() {
    PerfMarkStorage.resetForTest();
    PerfMarkStorage.startAnyways(gen, "task");
    Link link = PerfMark.link();
    link.link();
    PerfMarkStorage.stopAnyways(gen, "task");

    List<MarkList.Mark> marks = getMine(PerfMarkStorage.read()).getMarks();

    assertEquals(marks.size(), 4);
    List<MarkList.Mark> expected = Arrays.asList(
        new MarkList.Mark("task", null, 0, marks.get(0).getNanoTime(), gen, TASK_NOTAG_START),
        new MarkList.Mark(Marker.NONE, null, link.getId(), marks.get(1).getNanoTime(), gen, LINK),
        new MarkList.Mark(Marker.NONE, null, -link.getId(), marks.get(2).getNanoTime(), gen, LINK),
        new MarkList.Mark("task", null, 0, marks.get(3).getNanoTime(), gen, TASK_NOTAG_END));
    assertEquals(expected, marks);
  }

  private static MarkList getMine(List<MarkList> markLists) {
    for (MarkList list : markLists) {
      if (list.getThreadId() == Thread.currentThread().getId()) {
        return list;
      }
    }
    throw new AssertionError("can't find my marks!");
  }
}
