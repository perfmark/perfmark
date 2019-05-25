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
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicReference;
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
    ForkJoinTask<Long> task = new Fibonacci(35, link);
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
      long uniqueLinkPairId = 1;

      long currentThreadId = -1;
      long currentMarkListId = -1;
      final Deque<Mark> taskStack = new ArrayDeque<>();
      final Map<Long, LinkTuple> linkIdToLinkOut = new LinkedHashMap<>();
      final List<LinkTuple> linkIdToLinkIn = new ArrayList<>();

      @Override
      protected void enterGeneration(long generation) {
        linkIdToLinkOut.clear();
        linkIdToLinkIn.clear();
      }

      @Override
      protected void exitGeneration() {
        for (LinkTuple linkIn : linkIdToLinkIn) {
          long inLinkId = linkIn.link.getLinkId();
          long outLinkId = -inLinkId;
          LinkTuple linkOut = linkIdToLinkOut.get(outLinkId);
          if (linkOut == null) {
            // TODO: log?
            continue;
          }
          if (linkOut.markListId == linkIn.markListId) {
            continue;
          }
          // The name must be the same to match links together.
          String name = "link("
              + linkOut.lastTaskStart.getTaskName()
              + " -> "
              + linkIn.lastTaskStart.getTaskName()
              + ")";
          long localUniqueLinkPairId = uniqueLinkPairId++;
          traceEvents.add(
              TraceEvent.EVENT.name(name)
                  .tid(linkOut.threadId)
                  .pid(pid)
                  .phase("s")
                  .id(localUniqueLinkPairId)
                  .arg("linkid", linkOut.link.getLinkId())
                  .traceClockNanos(linkOut.lastTaskStart.getNanoTime() - initNanoTime));

          traceEvents.add(
              TraceEvent.EVENT.name(name)
                  .tid(linkIn.threadId)
                  .pid(pid)
                  .phase("t")
                  .id(localUniqueLinkPairId)
                  .arg("linkid", linkOut.link.getLinkId())
                  .traceClockNanos(linkIn.lastTaskStart.getNanoTime() - initNanoTime));
        }
        super.exitGeneration();
      }

      @Override
      protected void enterMarkList(String threadName, long threadId, long markListId) {
        currentThreadId = threadId;
        currentMarkListId = markListId;
        traceEvents.add(
            TraceEvent.EVENT
                .name("thread_name")
                .phase("M")
                .pid(pid)
                .arg("name", threadName)
                .arg("markListId", markListId)
                .tid(currentThreadId));
      }

      @Override
      protected void onTaskStart(Mark mark, boolean unmatchedStart, boolean unmatchedEnd) {
        assert !(unmatchedStart && unmatchedEnd);
        List<String> categories = Collections.emptyList();
        if (unmatchedStart) {
          categories = Collections.singletonList("unknownStart");
        } else if (unmatchedEnd) {
          categories = Collections.singletonList("unfinished");
        }
        taskStack.add(mark);
        traceEvents.add(
            TraceEvent.EVENT
                .name(mark.getTaskName())
                .phase("B")
                .pid(pid)
                .args(tagArgs(mark.getTagName(), mark.getTagId()))
                .categories(categories)
                .tid(currentThreadId)
                .traceClockNanos(mark.getNanoTime() - initNanoTime));
      }

      @Override
      protected void onTaskEnd(Mark mark, boolean unmatchedStart, boolean unmatchedEnd) {
        assert !(unmatchedStart && unmatchedEnd);
        List<String> categories = Collections.emptyList();
        if (unmatchedStart) {
          categories = Collections.singletonList("unknownStart");
        } else if (unmatchedEnd) {
          categories = Collections.singletonList("unfinished");
        }
        taskStack.pollLast();
        // TODO: maybe complain about task name mismatch?
        traceEvents.add(
            TraceEvent.EVENT
                .name(mark.getTaskName())
                .phase("E")
                .pid(pid)
                .args(tagArgs(mark.getTagName(), mark.getTagId()))
                .categories(categories)
                .tid(currentThreadId)
                .traceClockNanos(mark.getNanoTime() - initNanoTime));
      }

      final class LinkTuple {
        final Mark lastTaskStart;
        final Mark link;
        final long threadId;
        final long markListId;

        LinkTuple(Mark lastTaskStart, Mark link, long threadId, long markListId) {
          this.lastTaskStart = lastTaskStart;
          this.link = link;
          this.threadId = threadId;
          this.markListId = markListId;
        }
      }

      @Override
      protected void onLink(Mark mark) {
        if (taskStack.isEmpty()) {
          // In a mark list of only links (i.e. no starts or ends) it's possible there are no tasks
          // to bind to.  This is probably due to not calling link() correctly.
          logger.warning("Link not associated with any task");
          return;
        }
        LinkTuple linkTuple =
            new LinkTuple(taskStack.peekLast(), mark, currentThreadId, currentMarkListId);
        if (mark.getLinkId() > 0) {
          LinkTuple old = linkIdToLinkOut.put(mark.getLinkId(), linkTuple);
          assert old == null;
        } else if (mark.getLinkId() < 0) {
          linkIdToLinkIn.add(linkTuple);
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
    @SuppressWarnings("unused")
    final List<TraceEvent> traceEvents;

    @SerializedName("displayTimeUnit")
    @SuppressWarnings("unused")
    final String displayTimeUnit = "ns";

    @SerializedName("systemTraceEvents")
    @SuppressWarnings("unused")
    final String systemTraceData = "";

    @SerializedName("samples")
    @SuppressWarnings("unused")
    final List<Object> samples = new ArrayList<>();

    @SerializedName("stackFrames")
    @SuppressWarnings("unused")
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
      tagMap.put("id", tagId);
    }
    return Collections.unmodifiableMap(tagMap);
  }

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
