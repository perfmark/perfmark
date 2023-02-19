/*
 * Copyright 2021 Carl Mastrangelo
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

package io.perfmark.testing;

import static org.junit.Assert.assertEquals;

import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.MarkRecorder;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Base class for Mark holders.
 *
 * <p>This should really not be a super class, but JUnit4 makes doing this hard.
 */
@RunWith(JUnit4.class)
public abstract class MarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;

  protected MarkHolder getMarkHolder() {
    throw new UnsupportedOperationException("not implemented");
  }

  protected MarkRecorder getMarkRecorder() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Test
  public void taskTagStartStop() {
    var mr = getMarkRecorder();
    mr.start(gen, "task", 3);
    mr.stop(gen, "task", 4);

    List<Mark> marks = getOnly(getMarkHolder().read());
    assertEquals(2, marks.size());
    List<Mark> expected = List.of(Mark.taskStart(gen, 3, "task"), Mark.taskEnd(gen, 4, "task"));
    assertEquals(expected, marks);
  }

  private static MarkList getOnly(List<MarkList> markLists) {
    if (markLists.size() != 1) {
      throw new AssertionError("wrong number " + markLists);
    }
    return markLists.get(0);
  }

  @Test
  public void taskTagStartStop_subTask() {
    var mr = getMarkRecorder();
    mr.start(gen, "task", "subtask", 3);
    mr.stop(gen, "task", "subtask", 4);

    List<Mark> marks = getOnly(getMarkHolder().read());
    assertEquals(2, marks.size());
    List<Mark> expected = List.of(
            Mark.taskStart(gen, 3, "task", "subtask"), Mark.taskEnd(gen, 4, "task", "subtask"));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_tag() {
    var mr = getMarkRecorder();
    mr.start(gen, "task", "tag", 9, 3);
    mr.stop(gen, "task", "tag", 9, 4);

    List<Mark> marks = getOnly(getMarkHolder().read());
    assertEquals(4, marks.size());
    List<Mark> expected =
        List.of(
            Mark.taskStart(gen, 3, "task"),
            Mark.tag(gen, "tag", 9),
            Mark.tag(gen, "tag", 9),
            Mark.taskEnd(gen, 4, "task"));
    assertEquals(expected, marks);
  }

  @Test
  public void taskStartStartStopStop() {
    var mr = getMarkRecorder();
    mr.start(gen, "task1", 3);
    mr.start(gen, "task2", 4);
    mr.start(gen, "task3", 5);
    mr.stop(gen, 6);
    mr.stop(gen, "task2", 7);
    mr.stop(gen, "task1", 8);

    List<Mark> marks = getOnly(getMarkHolder().read());

    assertEquals(6, marks.size());
    List<Mark> expected =
        List.of(
            Mark.taskStart(gen, 3, "task1"),
            Mark.taskStart(gen, 4, "task2"),
            Mark.taskStart(gen, 5, "task3"),
            Mark.taskEnd(gen, 6),
            Mark.taskEnd(gen, 7, "task2"),
            Mark.taskEnd(gen, 8, "task1"));
    assertEquals(expected, marks);
  }

  @Test
  public void attachTag() {
    var mr = getMarkRecorder();
    mr.start(gen, "task", 3);
    mr.attachTag(gen, "tag", 8);
    mr.stop(gen, "task", 4);

    List<Mark> marks = getOnly(getMarkHolder().read());
    assertEquals(3, marks.size());
    List<Mark> expected =
        List.of(
            Mark.taskStart(gen, 3, "task"), Mark.tag(gen, "tag", 8), Mark.taskEnd(gen, 4, "task"));
    assertEquals(expected, marks);
  }

  @Test
  public void attachKeyedTag() {
    var mr = getMarkRecorder();
    mr.start(gen, "task", 3);
    mr.attachKeyedTag(gen, "key1", 8);
    mr.attachKeyedTag(gen, "key2", 8, 9);
    mr.attachKeyedTag(gen, "key3", "value");
    mr.stop(gen, "task", 4);

    List<Mark> marks = getOnly(getMarkHolder().read());
    assertEquals(5, marks.size());
    List<Mark> expected =
        List.of(
            Mark.taskStart(gen, 3, "task"),
            Mark.keyedTag(gen, "key1", 8),
            Mark.keyedTag(gen, "key2", 8, 9),
            Mark.keyedTag(gen, "key3", "value"),
            Mark.taskEnd(gen, 4, "task"));
    assertEquals(expected, marks);
  }

  @Test
  public void event() {
    var mr = getMarkRecorder();
    mr.event(gen, "task1", 8);
    mr.event(gen, "task2", 5);

    List<Mark> marks = getOnly(getMarkHolder().read());

    assertEquals(2, marks.size());
    List<Mark> expected = List.of(Mark.event(gen, 8, "task1"), Mark.event(gen, 5, "task2"));
    assertEquals(expected, marks);
  }

  @Test
  public void event_tag() {
    var mr = getMarkRecorder();
    mr.event(gen, "task1", "tag1", 7, 8);
    mr.event(gen, "task2", "tag2", 6, 5);

    List<Mark> marks = getOnly(getMarkHolder().read());

    assertEquals(2, marks.size());
    List<Mark> expected =
        List.of(
            Mark.event(gen, 8, "task1", "tag1", 7), Mark.event(gen, 5, "task2", "tag2", 6));
    assertEquals(expected, marks);
  }

  @Test
  public void event_subevent() {
    var mr = getMarkRecorder();
    mr.event(gen, "task1", "subtask3", 8);
    mr.event(gen, "task2", "subtask4", 5);

    List<Mark> marks = getOnly(getMarkHolder().read());

    assertEquals(2, marks.size());
    List<Mark> expected =
        List.of(
            Mark.event(gen, 8, "task1", "subtask3"), Mark.event(gen, 5, "task2", "subtask4"));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut() {
    var mr = getMarkRecorder();
    mr.start(gen, "task1", 3);
    mr.link(gen, 9);
    mr.link(gen, -9);
    mr.stop(gen, "task1", 4);

    List<Mark> marks = getOnly(getMarkHolder().read());

    assertEquals(marks.size(), 4);
    List<Mark> expected =
        List.of(
            Mark.taskStart(gen, 3, "task1"),
            Mark.link(gen, 9),
            Mark.link(gen, -9),
            Mark.taskEnd(gen, 4, "task1"));
    assertEquals(expected, marks);
  }

  @Test
  public void read_getsAllIfWriter() {
    var mr = getMarkRecorder();
    mr.start(gen, "task", 3);

    List<Mark> marks = getOnly(getMarkHolder().read());

    assertEquals(marks.size(), 1);
  }
}
