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

  @Test
  public void taskTagStartStop() {
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task", 3);
    mh.stop(gen, "task", 4);

    List<Mark> marks = mh.read(false);
    assertEquals(2, marks.size());
    List<Mark> expected = List.of(Mark.taskStart(gen, 3, "task"), Mark.taskEnd(gen, 4, "task"));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_subTask() {
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task", "subtask", 3);
    mh.stop(gen, "task", "subtask", 4);

    List<Mark> marks = mh.read(false);
    assertEquals(2, marks.size());
    List<Mark> expected = List.of(
            Mark.taskStart(gen, 3, "task", "subtask"), Mark.taskEnd(gen, 4, "task", "subtask"));
    assertEquals(expected, marks);
  }

  @Test
  public void taskTagStartStop_tag() {
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task", "tag", 9, 3);
    mh.stop(gen, "task", "tag", 9, 4);

    List<Mark> marks = mh.read(false);
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
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task1", 3);
    mh.start(gen, "task2", 4);
    mh.start(gen, "task3", 5);
    mh.stop(gen, 6);
    mh.stop(gen, "task2", 7);
    mh.stop(gen, "task1", 8);

    List<Mark> marks = mh.read(false);

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
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task", 3);
    mh.attachTag(gen, "tag", 8);
    mh.stop(gen, "task", 4);

    List<Mark> marks = mh.read(false);
    assertEquals(3, marks.size());
    List<Mark> expected =
        List.of(
            Mark.taskStart(gen, 3, "task"), Mark.tag(gen, "tag", 8), Mark.taskEnd(gen, 4, "task"));
    assertEquals(expected, marks);
  }

  @Test
  public void attachKeyedTag() {
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task", 3);
    mh.attachKeyedTag(gen, "key1", 8);
    mh.attachKeyedTag(gen, "key2", 8, 9);
    mh.attachKeyedTag(gen, "key3", "value");
    mh.stop(gen, "task", 4);

    List<Mark> marks = mh.read(false);
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
    MarkHolder mh = getMarkHolder();
    mh.event(gen, "task1", 8);
    mh.event(gen, "task2", 5);

    List<Mark> marks = mh.read(false);

    assertEquals(2, marks.size());
    List<Mark> expected = List.of(Mark.event(gen, 8, "task1"), Mark.event(gen, 5, "task2"));
    assertEquals(expected, marks);
  }

  @Test
  public void event_tag() {
    MarkHolder mh = getMarkHolder();
    mh.event(gen, "task1", "tag1", 7, 8);
    mh.event(gen, "task2", "tag2", 6, 5);

    List<Mark> marks = mh.read(false);

    assertEquals(2, marks.size());
    List<Mark> expected =
        List.of(
            Mark.event(gen, 8, "task1", "tag1", 7), Mark.event(gen, 5, "task2", "tag2", 6));
    assertEquals(expected, marks);
  }

  @Test
  public void event_subevent() {
    MarkHolder mh = getMarkHolder();
    mh.event(gen, "task1", "subtask3", 8);
    mh.event(gen, "task2", "subtask4", 5);

    List<Mark> marks = mh.read(false);

    assertEquals(2, marks.size());
    List<Mark> expected =
        List.of(
            Mark.event(gen, 8, "task1", "subtask3"), Mark.event(gen, 5, "task2", "subtask4"));
    assertEquals(expected, marks);
  }

  @Test
  public void linkInLinkOut() {
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task1", 3);
    mh.link(gen, 9);
    mh.link(gen, -9);
    mh.stop(gen, "task1", 4);

    List<Mark> marks = mh.read(false);

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
    MarkHolder mh = getMarkHolder();
    mh.start(gen, "task", 3);

    List<Mark> marks = mh.read(false);

    assertEquals(marks.size(), 1);
  }
}
