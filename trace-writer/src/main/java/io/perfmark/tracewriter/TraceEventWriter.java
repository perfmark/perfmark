package io.perfmark.tracewriter;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.perfmark.Link;
import io.perfmark.PerfMark;
import io.perfmark.PerfMarkStorage;
import io.perfmark.Tag;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.internal.jline.internal.Nullable;

// https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview
public final class TraceEventWriter {

  private static final Logger logger = Logger.getLogger(TraceEventWriter.class.getName());

  public static void main(String [] args) {
    new TraceEventWriter();
  }

  private TraceEventWriter() {
    PerfMark.setEnabled(true);


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
          if (input >= 25) {
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
    PerfMark.startTask("calc");
    Link link = PerfMark.link();
    ForkJoinTask<Long> task = new Fibonacci(28, link);
    fjp.execute(task);
    PerfMark.stopTask("calc");
    Long res;
    try {
      res = task.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.err.println(res);

    fjp.shutdown();

    List<MarkList> markLists = PerfMarkStorage.read();
    final long initNanoTime = PerfMarkStorage.getInitNanoTime();

    final List<TraceEvent> traceEvents = new ArrayList<>();
    final long pid = getPid();

    new MarkListWalker() {

      long currentThreadId = -1;
      Deque<Long> taskStarts = new ArrayDeque<>();
      Deque<String> taskNames = new ArrayDeque<>();

      @Override
      protected void enterMarkList(String threadName, long threadId) {
        currentThreadId = threadId;
      }

      @Override
      protected void onTaskStart(Mark mark, boolean isFake) {
        taskStarts.addLast(mark.getNanoTime() - initNanoTime);
        taskNames.addLast(mark.getTaskName() + mark.getTagId());
        traceEvents.add(
            TraceEvent.EVENT
                .name(mark.getTaskName() + mark.getTagId())
                .phase("B")
                .pid(pid)
                .args(tagArgs(mark.getTagName(), mark.getTagId()))
                .categories(isFake ? Arrays.asList("synthetic") : Collections.<String>emptyList())
                .tid(currentThreadId)
                .traceClockNanos(mark.getNanoTime() - initNanoTime));
      }

      @Override
      protected void onTaskEnd(Mark mark, boolean isFake) {
        taskStarts.pollLast();
        // TODO: maybe complain about task name mismatch?
        taskNames.pollLast();
        traceEvents.add(
            TraceEvent.EVENT
                .name(mark.getTaskName() + mark.getTagId())
                .phase("E")
                .pid(pid)
                .args(tagArgs(mark.getTagName(), mark.getTagId()))
                .categories(isFake ? Arrays.asList("synthetic") : Collections.<String>emptyList())
                .tid(currentThreadId)
                .traceClockNanos(mark.getNanoTime() - initNanoTime));
      }

      @Override
      protected void onLink(Mark mark, boolean isFake) {
        if (taskNames.isEmpty()) {
          // In a mark list of only links (i.e. no starts or ends) it's possible there are no tasks
          // to bind to.  This is probably due to not calling link() correctly.
          logger.warning("Link not associated with any task");
          return;
        }
        assert !isFake;
        if (mark.getLinkId() > 0) {
          traceEvents.add(
              TraceEvent.EVENT.name("perfmark:outlink")
                  .tid(currentThreadId)
                  .pid(pid)
                  .phase("s")
                  .id(mark.getLinkId())
                  .arg("outtask", taskNames.peekLast())
                  .traceClockNanos(taskStarts.peekLast()));
        } else if (mark.getLinkId() < 0) {
          traceEvents.add(
              TraceEvent.EVENT.name("perfmark:inlink")
                  .tid(currentThreadId)
                  .pid(pid)
                  .phase("t")
                  .id(-mark.getLinkId())
                  .arg("intask", taskNames.peekLast())
                  .traceClockNanos(taskStarts.peekLast()));
        }
      }
    }.walk(markLists);

    try (Writer f = Files.newBufferedWriter(new File("/tmp/tracey.json").toPath(), UTF_8)) {
      Gson gson = new GsonBuilder()
          .setPrettyPrinting()
          .create();
      gson.toJson(new TraceEventObject(traceEvents), f);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static final class TraceEventObject {
    @SerializedName("traceEvents")
    final List<TraceEvent> traceEvents;

    @SerializedName("displayTimeUnit")
    final String displayTimeUnit = "ns";

    @SerializedName("systemTraceEvents")
    final String systemTraceData = "";

    @SerializedName("samples")
    final List<Object> samples = new ArrayList<>();

    @SerializedName("stackFrames")
    final Map<String, ?> stackFrames = new HashMap<>();

    TraceEventObject(List<TraceEvent> traceEvents) {
      this.traceEvents = Collections.unmodifiableList(new ArrayList<>(traceEvents));
    }
  }

  private static Map<String, ?> tagArgs(@Nullable String tagName, long tagId) {
    Map<String, Object> tagMap = new LinkedHashMap<>(2);
    if (tagName != null) {
      tagMap.put("tag", tagName);
    }
    if (tagId != Mark.NO_TAG_ID) {
      tagMap.put("tagId", tagId);
    }
    return Collections.unmodifiableMap(tagMap);
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

   * */


  private static long getPid() {
    List<Throwable> errors = new ArrayList<>(0);
    Level level = Level.FINE;
    try {
      try {
        Class<?> clz = Class.forName("java.lang.ProcessHandle");
        Method currentMethod = clz.getMethod("current");
        Object processHandle = currentMethod.invoke(null);
        Method pidMethod = clz.getMethod("pid");
        return (long) pidMethod.invoke(processHandle);
      } catch (Exception | Error e) {
        errors.add(e);
      }
      try {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int index = name.indexOf('@');
        if (index != -1) {
          return Long.parseLong(name.substring(0, index));
        }
      } catch (Exception | Error e) {
        errors.add(e);
      }
      level = Level.WARNING;
    } finally {
      for (Throwable error : errors) {
        logger.log(level, "Error getting pid", error);
      }
    }
    return -1;
  }
}
