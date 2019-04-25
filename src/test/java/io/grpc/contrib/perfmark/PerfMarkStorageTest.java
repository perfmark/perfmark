package io.grpc.contrib.perfmark;

import static io.grpc.contrib.perfmark.Mark.Operation.LINK;
import static io.grpc.contrib.perfmark.Mark.Operation.TASK_END;
import static io.grpc.contrib.perfmark.Mark.Operation.TASK_START;
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

  @Before
  public void setUp() {
    PerfMarkStorage.reset();
  }

  @Test
  public void taskStartStop() {
    PerfMarkStorage.startAnyways(gen, "task", Tag.NO_TAG, Marker.NONE);
    PerfMarkStorage.stopAnyways(gen, Marker.NONE);

    List<Mark> marks = PerfMarkStorage.read();

    assertEquals(marks.size(), 2);
    List<Mark> expected = Arrays.asList(
        new Mark("task", null, 0, Marker.NONE, marks.get(0).getNanoTime(), gen, TASK_START),
        new Mark(null, null, 0, Marker.NONE, marks.get(1).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStartStopStop() {
    PerfMarkStorage.startAnyways(gen, "task1", Tag.NO_TAG, Marker.NONE);
    PerfMarkStorage.startAnyways(gen, "task2", Tag.NO_TAG, Marker.NONE);
    PerfMarkStorage.stopAnyways(gen, Marker.NONE);
    PerfMarkStorage.stopAnyways(gen, Marker.NONE);

    List<Mark> marks = PerfMarkStorage.read();

    assertEquals(marks.size(), 4);
    List<Mark> expected = Arrays.asList(
        new Mark("task1", null, 0, Marker.NONE, marks.get(0).getNanoTime(), gen, TASK_START),
        new Mark("task2", null, 0, Marker.NONE, marks.get(1).getNanoTime(), gen, TASK_START),
        new Mark(null, null, 0, Marker.NONE, marks.get(2).getNanoTime(), gen, TASK_END),
        new Mark(null, null, 0, Marker.NONE, marks.get(3).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut() {
    PerfMarkStorage.startAnyways(gen, "task", Tag.NO_TAG, Marker.NONE);
    Link link = PerfMark.link();
    link.link();
    PerfMarkStorage.stopAnyways(gen, Marker.NONE);

    List<Mark> marks = PerfMarkStorage.read();

    assertEquals(marks.size(), 4);
    List<Mark> expected = Arrays.asList(
        new Mark("task", null, 0, Marker.NONE, marks.get(0).getNanoTime(), gen, TASK_START),
        new Mark(null, null, link.getId(), Marker.NONE, marks.get(1).getNanoTime(), gen, LINK),
        new Mark(null, null, -link.getId(), Marker.NONE, marks.get(2).getNanoTime(), gen, LINK),
        new Mark(null, null, 0, Marker.NONE, marks.get(3).getNanoTime(), gen, TASK_END));
    assertEquals(expected, marks);
  }
}
