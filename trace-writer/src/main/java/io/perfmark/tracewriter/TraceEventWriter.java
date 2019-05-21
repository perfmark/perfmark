package io.perfmark.tracewriter;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.perfmark.PerfMark;
import io.perfmark.PerfMarkStorage;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class TraceEventWriter {

  public static void main(String [] args) {
    new TraceEventWriter();
  }

  private TraceEventWriter() {
    PerfMark.setEnabled(true);

    int z = 1;
    for (int i = 1; i < 50000; i++) {
      PerfMark.startTask("Hi");
      z = z * z + i;
      PerfMark.stopTask("Hi");
    }
    System.out.println(z);

    List<MarkList> markLists = PerfMarkStorage.read();
    long initNanoTime = PerfMarkStorage.getInitNanoTime();

    List<TraceEvent> traceEvents = new ArrayList<>();
    for(MarkList markList : markLists) {
      List<Mark> marks = markList.getMarks();
      List<Task> tasks = new MarkParser().parse(marks);
      for (Task task : tasks) {
        walkTasks(traceEvents, markList.getThreadId(), task, initNanoTime);
      }
    }

    try (Writer f = new FileWriter(new File("/tmp/tracey.json"))) {
      new Gson().toJson(new TraceEventObject(traceEvents), f);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void walkTasks(List<? super TraceEvent> traceEvents, long threadId, Task task, long initNanoTime) {
    traceEvents.add(new DurationBegin(task.start.getTaskName(), task.start.getNanoTime() - initNanoTime, threadId));
    for (Task child : task.children) {
      walkTasks(traceEvents, threadId, task, initNanoTime);
    }
    traceEvents.add(new DurationEnd(task.end.getTaskName(), task.end.getNanoTime() - initNanoTime, threadId));
  }

  static final class TraceEventObject {
    @SerializedName("traceEvents")
    final List<? extends TraceEvent> traceEvents;

    @SerializedName("displayTimeUnit")
    final String displayTimeUnit = "ns";

    @SerializedName("systemTraceEvents")
    final String systemTraceData = "";

    @SerializedName("samples")
    final List<Object> samples = new ArrayList<>();

    @SerializedName("stackFrames")
    final Map<String, ?> stackFrames = new HashMap<>();

    TraceEventObject(List<? extends TraceEvent> traceEvents) {
      this.traceEvents = Collections.unmodifiableList(new ArrayList<>(traceEvents));
    }
  }

  abstract static class TraceEvent {
    @SerializedName("ph")
    final String phase;

    @SerializedName("name")
    final String name;

    @SerializedName("cat")
    final String categories;

    @SerializedName("ts")
    final double traceClockMicros;

    @SerializedName("pid")
    final long pid;

    @SerializedName("tid")
    final long tid;

    @SerializedName("args")
    final Map<String, ?> args = new HashMap<>();

    @SerializedName("cname")
    final String colorName = "red";

    TraceEvent(String name, String categories, Phase phase, long nanoTime, long pid, long tid) {
      this.name = name;
      this.categories = categories;
      this.phase = phase.symbol;
      this.traceClockMicros = nanoTime / 1000.0;
      this.pid = pid;
      this.tid = tid;
    }
  }

  static final class DurationBegin extends TraceEvent {
    DurationBegin(String name, long nanoTime, long threadId) {
      super(name, "none", Phase.BEGIN, nanoTime, 1, threadId);
    }
  }

  static final class DurationEnd extends TraceEvent {
    DurationEnd(String name, long nanoTime, long threadId) {
      super(name, "none", Phase.END, nanoTime, 1, threadId);
    }
  }

  enum Phase {
    // Duration Events
    BEGIN("B"),
    END("E"),

    // Completion Events
    COMPLETE("X"),

    // Instant Events
    INSTANT("i"),
    @Deprecated INSTANT_DEPRECATED("I"),

    // Counter Events
    COUNTER("C"),

    // Async Events
    ASYNC_NESTABLE_START("b"),
    ASYNC_NESTABLE_INSTANT("n"),
    ASYNC_NESTABLE_END("e"),
    @Deprecated ASYNC_DEPRECATED_START("S"),
    @Deprecated ASYNC_DEPRECATED_STEP_INTO("T"),
    @Deprecated ASYNC_DEPRECATED_STEP_PAST("p"),
    @Deprecated ASYNC_DEPRECATED_END("F"),

    // Flow Events
    FLOW_START("s"),
    FLOW_STEP("t"),
    FLOW_END("f"),

    SAMPLE("P"),

    // Object Events
    CREATED("N"),
    SNAPSHOT("O"),
    DESTROYED("D"),

    METADATA("M"),

    // Memory Dump Events
    MEMORY_DUMP_GLOBAL("V"),
    MEMORY_DUMP_PROCESS("v"),

    // Mark Events
    MARK("R"),

    CLOCK_SYNC("c"),

    CONTEXT_ENTER("("),
    CONTEXT_EXIT(")"),

    ;

    private final String symbol;

    Phase(String symbol) {
      this.symbol = symbol;
    }
  }

  static final class Trace {
    Phase phase;
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
       for (MarkList markList : PerfMarkStorage.read()) {
      List<Mark> marks = markList.getMarks();
      List<Task> roots = new MarkParser().parse(marks);
      System.err.println("Thread " + markList.getThreadId() + " " + marks.size());
      System.err.println(roots);
      //for (int i = marks.size() - 1; i >= marks.size() - 20 && i >= 0; i--) {
      //  System.err.println(marks.get(i));
      //}
    }
   *
   * */
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
}
