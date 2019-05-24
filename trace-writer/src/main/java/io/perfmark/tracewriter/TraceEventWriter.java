package io.perfmark.tracewriter;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.perfmark.PerfMark;
import io.perfmark.PerfMarkStorage;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview
public final class TraceEventWriter {

  public static void main(String [] args) {
    new TraceEventWriter();
  }

  private TraceEventWriter() {
    PerfMark.setEnabled(true);

    int z = 1;
    for (int i = 1; i < 33000; i++) {
      PerfMark.startTask("Hi");
      z = z * z + i;
      PerfMark.stopTask("Hi");
    }
    System.out.println(z);

    List<MarkList> markLists = PerfMarkStorage.read();
    final long initNanoTime = PerfMarkStorage.getInitNanoTime();

    final List<TraceEvent> traceEvents = new ArrayList<>();

    new MarkListWalker() {

      long currentThreadId = -1;

      @Override
      protected void enterMarkList(String threadName, long threadId) {
        currentThreadId = threadId;
      }

      @Override
      protected void onTaskStart(Mark mark, boolean isFake) {
        traceEvents.add(new DurationBegin(
            mark.getTaskName(), mark.getNanoTime() - initNanoTime, currentThreadId));
      }

      @Override
      protected void onTaskEnd(Mark mark, boolean isFake) {
        traceEvents.add(new DurationEnd(
            mark.getTaskName(), mark.getNanoTime() - initNanoTime, currentThreadId));
      }

      @Override
      protected void onLink(Mark mark, boolean isFake) {
        if (mark.getLinkId() > 0) {
          traceEvents.add(new FlowBegin(
              mark.getTaskName(), mark.getLinkId(), currentThreadId));
        } else if (mark.getLinkId() < 0) {
          traceEvents.add(new FlowInstant(
              mark.getTaskName(), -mark.getLinkId(), currentThreadId));
        }
      }
    }.walk(markLists);

    try (Writer f = new FileWriter(new File("/tmp/tracey.json"))) {
      new Gson().toJson(new TraceEventObject(traceEvents), f);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  static final class DurationBegin extends TraceEvent {
    DurationBegin(String name, long absNanoTime, long threadId) {
      super(name, Collections.<String>emptyList(), "B", absNanoTime, 0L, threadId);
    }
  }

  static final class DurationEnd extends TraceEvent {
    DurationEnd(String name, long absNanoTime, long threadId) {
      super(name, Collections.<String>emptyList(), "E", absNanoTime, 0L, threadId);
    }
  }

  static final class FlowBegin extends TraceEvent {
    @SerializedName("id")
    final long id;

    FlowBegin(String name, long id, long threadId) {
      super(name, Collections.<String>emptyList(), "s", /* absNanoTime= */ null, 0L, threadId);
      this.id = id;
    }
  }

  static final class FlowInstant extends TraceEvent {
    @SerializedName("id")
    final long id;

    FlowInstant(String name, long id, long threadId) {
      super(name, Collections.<String>emptyList(), "s", /* absNanoTime= */ null, 0L, threadId);
      this.id = id;
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
/*
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
*/

}
