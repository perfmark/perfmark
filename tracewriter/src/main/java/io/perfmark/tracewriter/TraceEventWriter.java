/*
 * Copyright 2019 Google LLC
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

package io.perfmark.tracewriter;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.annotations.SerializedName;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the PerfMark results to a "Trace Event" JSON file usable by the Chromium Profiler
 * "Catapult". The format is defined at
 * https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview
 *
 * <p>This code is <strong>NOT</strong> API stable, and may be removed in the future, or changed
 * without notice.
 *
 * @since 0.16.0
 */
public final class TraceEventWriter {

  private static final Logger logger = Logger.getLogger(TraceEventWriter.class.getName());

  /**
   * Writes trace events the home directory. By default, it prefers the location in {@code
   * $XDG_DATA_HOME/perfmark} environment variable. If unset, it attempts to use {@code
   * $HOME/.local/share/perfmark}.
   *
   * <p>Authors note: if you are on Windows, or the above defaults aren't right, I'm not really sure
   * where else is a good place to put this data. Please file an issue at https://www.perfmark.io/
   * if you have a preference.
   *
   * <p>Updated in 0.17.0 to return the created path.
   *
   * @throws IOException if there is an error writing to the file.
   * @return the path used to create the trace file.
   */
  @CanIgnoreReturnValue
  public static Path writeTraceEvents() throws IOException {
    Path p = pickNextDest(guessDirectory());
    try (OutputStream os = Files.newOutputStream(p);
        OutputStream gzos = new GZIPOutputStream(os);
        Writer osw = new OutputStreamWriter(gzos, UTF_8)) {
      writeTraceEvents(osw);
    }
    logger.info("Wrote trace to " + p);
    return p;
  }

  /**
   * Writes all trace events in in JSON format to the given destination.
   *
   * @param destination the destination for the JSON data.
   * @throws IOException if there are errors build the JSON, or can't write to the destination.
   */
  public static void writeTraceEvents(Writer destination) throws IOException {
    writeTraceEvents(
        destination, Storage.read(), Storage.getInitNanoTime(), System.nanoTime(), getPid());
  }

  /**
   * Writes the trace events gathered from {@link Storage#read()}. This method is not API stable. It
   * will be eventually.
   *
   * @param destination the destination for the JSON data.
   * @param markLists the data to use to build the trace event JSON
   * @param initNanoTime the time PerfMark classes were first loaded as specified by {@link
   *     System#nanoTime()}
   * @param nowNanoTime the current time as specified by {@link System#nanoTime()}.
   * @param pid the PID of the current process.
   * @throws IOException if there are errors build the JSON, or can't write to the destination.
   */
  public static void writeTraceEvents(
      Writer destination,
      List<? extends MarkList> markLists,
      long initNanoTime,
      long nowNanoTime,
      long pid)
      throws IOException {
    List<TraceEvent> traceEvents = new ArrayList<>();
    new TraceEventWalker(traceEvents, pid, initNanoTime).walk(markLists, nowNanoTime);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try {
      gson.toJson(new TraceEventObject(traceEvents), destination);
    } catch (JsonIOException e) {
      throw new IOException(e);
    }
  }

  private static Path pickNextDest(Path dir) throws IOException {
    String fmt = "perfmark-trace-%03d.json.gz";
    int lo;
    int hi = 0;
    while (true) {
      Path candidate = dir.resolve(String.format(fmt, hi));
      if (!Files.exists(candidate)) {
        lo = hi >>> 1;
        break;
      }
      if (hi == 0) {
        hi++;
      } else if (hi >>> 1 >= Integer.MAX_VALUE) {
        throw new IOException("too many files in dir");
      } else {
        hi <<= 1;
      }
    }
    // After this point, hi must always point to a non-existent file.
    while (hi > lo) {
      int mid = (hi + lo) >>> 1; // take THAT, overflow!
      Path candidate = dir.resolve(String.format(fmt, mid));
      if (Files.exists(candidate)) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return dir.resolve(String.format(fmt, hi));
  }

  private static Path guessDirectory() throws IOException {
    final String PERFMARK_TRACE_DIR = "perfmark";
    final String sep = File.separator;

    List<Path> dataHomeChoices = new ArrayList<>();
    String dataHome = System.getenv("XDG_DATA_HOME");
    if (dataHome != null) {
      dataHomeChoices.add(new File(dataHome + sep + PERFMARK_TRACE_DIR).toPath());
    }
    String home = System.getProperty("user.home");
    if (home != null) {
      dataHomeChoices.add(
          new File(home + sep + ".local" + sep + "share" + sep + PERFMARK_TRACE_DIR).toPath());
    }
    for (Path path : dataHomeChoices) {
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      } else {
        if (!Files.isDirectory(path)) {
          continue;
        }
      }
      return path;
    }
    return new File(".").toPath();
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

  private static final class TraceEventWalker extends MarkListWalker {

    private static final class TaskStart {
      final Mark mark;
      final int traceEventIdx;

      TaskStart(Mark mark, int traceEventIdx) {
        this.mark = mark;
        this.traceEventIdx = traceEventIdx;
      }
    }

    private long uniqueLinkPairId = 1;
    private long currentThreadId = -1;
    private long currentMarkListId = -1;
    private final Deque<TaskStart> taskStack = new ArrayDeque<>();
    private final Map<Long, LinkTuple> linkIdToLinkOut = new LinkedHashMap<>();
    private final List<LinkTuple> linkIdToLinkIn = new ArrayList<>();

    private final long pid;
    private final long initNanoTime;
    private final List<TraceEvent> traceEvents;

    TraceEventWalker(List<TraceEvent> traceEvents, long pid, long initNanoTime) {
      this.pid = pid;
      this.initNanoTime = initNanoTime;
      this.traceEvents = traceEvents;
    }

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
          // continue;
        }
        // The name must be the same to match links together.
        String name =
            "link("
                + taskName(linkOut.lastTaskStart)
                + " -> "
                + taskName(linkIn.lastTaskStart)
                + ")";
        long localUniqueLinkPairId = uniqueLinkPairId++;
        traceEvents.add(
            TraceEvent.EVENT
                .name(name)
                .tid(linkOut.threadId)
                .pid(pid)
                .phase("s")
                .id(localUniqueLinkPairId)
                .args(TraceEvent.TagMap.EMPTY.withKeyed("linkid", linkOut.link.getLinkId()))
                .traceClockNanos(linkOut.lastTaskStart.getNanoTime() - initNanoTime));

        traceEvents.add(
            TraceEvent.EVENT
                .name(name)
                .tid(linkIn.threadId)
                .pid(pid)
                .phase("t")
                .id(localUniqueLinkPairId)
                .args(TraceEvent.TagMap.EMPTY.withKeyed("linkid", linkOut.link.getLinkId()))
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
              .args(
                  TraceEvent.TagMap.EMPTY
                      .withKeyed("name", threadName)
                      .withKeyed("markListId", markListId))
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
      TraceEvent traceEvent =
          TraceEvent.EVENT
              .name(taskName(mark))
              .phase("B")
              .pid(pid)
              .categories(categories)
              .tid(currentThreadId)
              .traceClockNanos(mark.getNanoTime() - initNanoTime);
      traceEvents.add(traceEvent);
      taskStack.add(new TaskStart(mark, traceEvents.size() - 1));
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
      // TODO: maybe copy the args from the start task
      taskStack.pollLast();
      TraceEvent traceEvent =
          TraceEvent.EVENT
              .name(taskName(mark))
              .phase("E")
              .pid(pid)
              .categories(categories)
              .tid(currentThreadId)
              .traceClockNanos(mark.getNanoTime() - initNanoTime);
      traceEvents.add(traceEvent);
    }

    @Override
    protected void onAttachTag(Mark mark) {
      TaskStart taskStart = taskStack.peekLast();
      TraceEvent taskEvent = traceEvents.get(taskStart.traceEventIdx);
      TraceEvent.TagMap args = taskEvent.args();
      out:
      {
        switch (mark.getOperation()) {
          case TAG_N0S1:
            args = args.withUnkeyed(mark.getTagStringValue(), Mark.NO_TAG_ID);
            break out;
          case TAG_N1S0:
            args = args.withUnkeyed(Mark.NO_TAG_NAME, mark.getTagFirstNumeric());
            break out;
          case TAG_N1S1:
            args = args.withUnkeyed(mark.getTagStringValue(), mark.getTagFirstNumeric());
            break out;
          case TAG_KEYED_N0S2:
            args = args.withKeyed(mark.getTagKey(), mark.getTagStringValue());
            break out;
          case TAG_KEYED_N1S1:
            args = args.withKeyed(mark.getTagKey(), mark.getTagFirstNumeric());
            break out;
          case TAG_KEYED_N2S1:
            args =
                args.withKeyed(
                    mark.getTagKey(), mark.getTagFirstNumeric(), mark.getTagSecondNumeric());
            break out;
          case NONE:
          case TASK_START_N1S1:
          case TASK_START_N1S2:
          case TASK_END_N1S1:
          case TASK_END_N1S2:
          case EVENT_N1S1:
          case EVENT_N1S2:
          case EVENT_N2S2:
          case EVENT_N2S3:
          case MARK:
          case LINK:
            break;
        }
        throw new AssertionError(mark.getOperation());
      }
      traceEvents.set(taskStart.traceEventIdx, taskEvent.args(args));
    }

    @Override
    protected void onEvent(Mark mark) {
      TraceEvent.TagMap tagMap = TraceEvent.TagMap.EMPTY;
      out:
      {
        switch (mark.getOperation()) {
          case EVENT_N2S2:
          case EVENT_N2S3:
            tagMap = tagMap.withUnkeyed(mark.getTagStringValue(), mark.getTagFirstNumeric());
            break out;
          case NONE:
          case TASK_START_N1S1:
          case TASK_START_N1S2:
          case TASK_END_N1S1:
          case TASK_END_N1S2:
          case EVENT_N1S1:
          case EVENT_N1S2:
          case MARK:
          case LINK:
          case TAG_N0S1:
          case TAG_N1S0:
          case TAG_N1S1:
          case TAG_KEYED_N0S2:
          case TAG_KEYED_N1S1:
          case TAG_KEYED_N2S1:
            break;
        }
        throw new AssertionError(mark.getOperation());
      }
      TraceEvent traceEvent =
          TraceEvent.EVENT
              .name(taskName(mark))
              .phase("i")
              .pid(pid)
              .args(tagMap)
              .tid(currentThreadId)
              .traceClockNanos(mark.getNanoTime() - initNanoTime);
      traceEvents.add(traceEvent);
    }

    static final class LinkTuple {
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
          new LinkTuple(taskStack.peekLast().mark, mark, currentThreadId, currentMarkListId);
      if (mark.getLinkId() > 0) {
        LinkTuple old = linkIdToLinkOut.put(mark.getLinkId(), linkTuple);
        assert old == null;
      } else if (mark.getLinkId() < 0) {
        linkIdToLinkIn.add(linkTuple);
      }
    }
  }

  private static String taskName(Mark mark) {
    switch (mark.getOperation()) {
      case TASK_START_N1S1:
      case TASK_END_N1S1:
      case EVENT_N1S1:
      case EVENT_N2S2:
        return mark.getTaskName();
      case TASK_START_N1S2:
      case TASK_END_N1S2:
      case EVENT_N1S2:
      case EVENT_N2S3:
        return mark.getTaskName() + '.' + mark.getSubTaskName();
      case MARK:
      case LINK:
      case TAG_N0S1:
      case TAG_KEYED_N0S2:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
      case TAG_N1S0:
      case TAG_N1S1:
      case NONE:
        throw new UnsupportedOperationException(mark.toString());
    }
    throw new AssertionError(mark.getOperation());
  }
}
