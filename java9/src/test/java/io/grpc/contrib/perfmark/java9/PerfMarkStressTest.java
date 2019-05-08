package io.grpc.contrib.perfmark.java9;

import io.grpc.contrib.perfmark.Link;
import io.grpc.contrib.perfmark.PerfMark;
import io.grpc.contrib.perfmark.PerfMarkStorage;
import io.grpc.contrib.perfmark.Tag;
import io.grpc.contrib.perfmark.impl.Mark;
import io.grpc.contrib.perfmark.impl.MarkList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkStressTest {

  @Test
  public void fibonacci() {
    ForkJoinPool fjp = new ForkJoinPool(8);
    final class Fibonacci extends RecursiveTask<Long> {

      private final long input;
      private final Link link;

      Fibonacci(long input, Link link) {
        this.input = input;
        this.link = link;
      }

      @Override
      protected Long compute() {
        Tag tag = PerfMark.createTag(input);
        PerfMark.startTask("compute", tag);
        link.link();
        try {
          if (input >= 20) {
            Link link2 = PerfMark.link();
            ForkJoinTask<Long> task1 = new Fibonacci(input - 1, link2).fork();
            Fibonacci task2 = new Fibonacci(input - 2, link2);
            return task2.compute() + task1.join();
          } else {
            return computeUnboxed(input);
          }
        } finally {
          PerfMark.stopTask("compute", tag);
        }
      }

      private long computeUnboxed(long n) {
        if (n <= 1) {
          return n;
        }
        return computeUnboxed(n - 1) + computeUnboxed(n - 2);
      }
    }
    PerfMark.setEnabled(true);
    PerfMark.startTask("calc");
    Link link = PerfMark.link();
    ForkJoinTask<Long> task = new Fibonacci(30, link);
    fjp.execute(task);
    PerfMark.stopTask("calc");
    Long res;
    try {
      res = task.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.err.println(res);
    for (MarkList markList : PerfMarkStorage.read()) {
      List<Mark> marks = markList.getMarks();
      List<Task> roots = new MarkParser().parse(marks);
      System.err.println("Thread " + markList.getThreadId() + " " + marks.size());
      System.err.println(roots);
      //for (int i = marks.size() - 1; i >= marks.size() - 20 && i >= 0; i--) {
      //  System.err.println(marks.get(i));
      //}
    }

    fjp.shutdown();
  }

  private static final class Task {
    private final Mark start;
    private final Mark end;
    private final boolean startFake;
    private final boolean endFake;
    private final List<Task> children;
    private final Long inLinkId;
    private final List<Long> outLinkIdList;

    private Task(MutableTask mutableTask) {
      if (mutableTask.start == null) {
        throw new NullPointerException("start");
      }
      this.start = mutableTask.start;
      if (mutableTask.end == null) {
        throw new NullPointerException("end");
      }
      this.end = mutableTask.end;
      this.startFake = mutableTask.startFake;
      this.endFake = mutableTask.endFake;
      this.children = Collections.unmodifiableList(new ArrayList<>(mutableTask.children));
      this.inLinkId = mutableTask.inLinkId;
      this.outLinkIdList =
          Collections.unmodifiableList(new ArrayList<>(mutableTask.outLinkIdList));
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      toString(sb, "");
      return sb.toString();
    }


    private void toString(StringBuilder sb, String prefix) {
      sb.append(prefix).append(start.getTaskName());
      if (start.getOperation() == Mark.Operation.TASK_START) {
        sb.append('@').append(start.getTagId());
      }
      sb.append('\n');
      sb.append(prefix).append(end.getNanoTime() - start.getNanoTime()).append('\n');
      if (!children.isEmpty()) {
        String childPrefix = prefix + " ";
        for (Task child : children) {
          child.toString(sb, childPrefix);
        }
      }
    }

    static final class MutableTask {
      Mark start;
      Mark end;
      boolean startFake;
      boolean endFake;
      List<Task> children = new ArrayList<>();
      Long inLinkId;
      List<Long> outLinkIdList = new ArrayList<>();
    }
  }

  private static final class MarkParser {
    private final Deque<Mark> fakeStarts = new ArrayDeque<>();
    private final Deque<Mark> unmatchedMarks = new ArrayDeque<>();
    private final Deque<Mark> fakeEnds = new ArrayDeque<>();
    private Long firstNanoTime;
    private long lastNanoTime;

    private void setNanoTimeBounds(Mark mark) {
      switch (mark.getOperation()) {
        case TASK_NOTAG_START:
        case TASK_START:
        case TASK_NOTAG_END:
        case TASK_END:
          if (firstNanoTime == null) {
            firstNanoTime = mark.getNanoTime();
          }
          lastNanoTime = mark.getNanoTime();
          return;
        case LINK:
          return;
        case NONE:
          break;
      }
      throw new AssertionError();
    }

    List<Task> parse(List<Mark> marks) {
      addFakes(marks);
      Deque<Task.MutableTask> tasks = new ArrayDeque<>();
      List<Task> roots = new ArrayList<>();
      for (Mark mark : fakeStarts) {
        Task.MutableTask task = new Task.MutableTask();
        task.start = mark;
        task.startFake = true;
        tasks.addLast(task);
      }
      loop: for (Mark mark : marks) {
        Task.MutableTask task;
        switch (mark.getOperation()) {
          case TASK_START:
          case TASK_NOTAG_START:
            task = new Task.MutableTask();
            task.start = mark;
            tasks.addLast(task);
            continue loop;
          case TASK_END:
          case TASK_NOTAG_END:
            task = tasks.removeLast();
            task.end = mark;
            // todo: verify task names match if both non null
            Task t = new Task(task);
            Task.MutableTask parent = tasks.peekLast();
            if (parent != null) {
              parent.children.add(t);
            } else {
              roots.add(t);
            }
            continue loop;
          case LINK:
            long linkId = mark.getLinkId();
            task = tasks.peekLast();
            if (linkId > 0) {
              task.outLinkIdList.add(linkId);
            } else if (linkId < 0 && task.inLinkId == null) {
              task.inLinkId = linkId;
            } else {
              // linking was disable when the link was created.  ignore
            }
            continue loop;
          case NONE:
            break;
        }
        throw new AssertionError();
      }
      return roots;
    }

    private void addFakes(List<Mark> marks) {
      loop: for (Mark mark : marks) {
        setNanoTimeBounds(mark);
        switch (mark.getOperation()) {
          case TASK_START:
          case TASK_NOTAG_START:
            unmatchedMarks.addLast(mark);
            continue loop;
          case TASK_END:
          case TASK_NOTAG_END:
            if (!unmatchedMarks.isEmpty()) {
              unmatchedMarks.removeLast();
            } else {
              fakeStarts.addFirst(createFakeStart(mark));
            }
            continue loop;
          case LINK:
            continue loop;
          case NONE:
            break;
        }
        throw new AssertionError();
      }
      for (Mark unmatchedMark : unmatchedMarks) {
        fakeEnds.addFirst(createFakeEnd(unmatchedMark));
      }
      unmatchedMarks.clear();
    }

    private Mark createFakeEnd(Mark start) {
      if (start.getMarker() != null) {
        switch (start.getOperation()) {
          case TASK_START:
            return Mark.create(
                start.getMarker(),
                start.getTagName(),
                start.getTagId(),
                lastNanoTime,
                start.getGeneration(),
                Mark.Operation.TASK_END);
          case TASK_NOTAG_START:
            return Mark.create(
                start.getMarker(),
                Mark.NO_TAG_NAME,
                Mark.NO_TAG_ID,
                lastNanoTime,
                start.getGeneration(),
                Mark.Operation.TASK_NOTAG_END);
          case LINK:
          case NONE:
          case TASK_END:
          case TASK_NOTAG_END:
            break;
        }
        throw new AssertionError();
      } else {
        String taskName = String.valueOf(start.getTaskName()); // Uses "null" if null.
        switch (start.getOperation()) {
          case TASK_START:
            return Mark.create(
                taskName,
                start.getTagName(),
                start.getTagId(),
                lastNanoTime,
                start.getGeneration(),
                Mark.Operation.TASK_END);
          case TASK_NOTAG_START:
            return Mark.create(
                taskName,
                Mark.NO_TAG_NAME,
                Mark.NO_TAG_ID,
                lastNanoTime,
                start.getGeneration(),
                Mark.Operation.TASK_NOTAG_END);
          case LINK:
          case NONE:
          case TASK_END:
          case TASK_NOTAG_END:
            break;
        }
        throw new AssertionError();
      }
    }

    private Mark createFakeStart(Mark end) {
      if (end.getMarker() != null) {
        switch (end.getOperation()) {
          case TASK_END:
            return Mark.create(
                end.getMarker(),
                end.getTagName(),
                end.getTagId(),
                firstNanoTime,
                end.getGeneration(),
                Mark.Operation.TASK_START);
          case TASK_NOTAG_END:
            return Mark.create(
                end.getMarker(),
                Mark.NO_TAG_NAME,
                Mark.NO_TAG_ID,
                firstNanoTime,
                end.getGeneration(),
                Mark.Operation.TASK_NOTAG_START);
          case LINK:
          case NONE:
          case TASK_START:
          case TASK_NOTAG_START:
            break;
        }
        throw new AssertionError();
      } else {
        String taskName = String.valueOf(end.getTaskName()); // Uses "null" if null.
        switch (end.getOperation()) {
          case TASK_END:
            return Mark.create(
                taskName,
                end.getTagName(),
                end.getTagId(),
                firstNanoTime,
                end.getGeneration(),
                Mark.Operation.TASK_START);
          case TASK_NOTAG_END:
            return Mark.create(
                taskName,
                Mark.NO_TAG_NAME,
                Mark.NO_TAG_ID,
                firstNanoTime,
                end.getGeneration(),
                Mark.Operation.TASK_NOTAG_START);
          case LINK:
          case NONE:
          case TASK_START:
          case TASK_NOTAG_START:
            break;
        }
        throw new AssertionError();
      }
    }
  }

  /*
  * 0
  * start 1
  * start 2
  * end 1
  * start 2
  * end 1
  * fakend 0
  *
  * from start to finish, the depth should never go negative
  * the depth should always end at 0
  * the depth shoudl aways start at 0
  *
  * 0
  * start 1
  * start 2
  * end 1
  * start 2
  * end 1
  * fakeend 0
  *
  * 0
  * start 1
  * start 2
  * fakeend 1
  * fakeend 0
  *
  * 0
  * fakestart 1
  * fakestart 2
  * end 1
  * end 0
  *
  * 0
  * fakestart 1
  * fakestart 2
  * end 1
  * end 0
  * start 1
  * end 0
  * start 1
  * start 2
  * fakeend 1
  * fakeend 0
  *
  *
  *
  *
  *
  *
  *
  *
  *
  * */
}
