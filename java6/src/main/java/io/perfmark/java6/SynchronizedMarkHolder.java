package io.perfmark.java6;

import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.Marker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

final class SynchronizedMarkHolder extends MarkHolder {
  private static final long START_OP = Mark.Operation.TASK_START.ordinal();
  private static final long START_T_OP = Mark.Operation.TASK_START_T.ordinal();
  private static final long START_M_OP = Mark.Operation.TASK_START_M.ordinal();
  private static final long START_TM_OP = Mark.Operation.TASK_START_TM.ordinal();
  private static final long STOP_OP = Mark.Operation.TASK_END.ordinal();
  private static final long STOP_T_OP = Mark.Operation.TASK_END_T.ordinal();
  private static final long STOP_M_OP = Mark.Operation.TASK_END_M.ordinal();
  private static final long STOP_TM_OP = Mark.Operation.TASK_END_TM.ordinal();
  private static final long EVENT_OP = Mark.Operation.EVENT.ordinal();
  private static final long EVENT_T_OP = Mark.Operation.EVENT_T.ordinal();
  private static final long EVENT_M_OP = Mark.Operation.EVENT_M.ordinal();
  private static final long EVENT_TM_OP = Mark.Operation.EVENT_TM.ordinal();
  private static final long LINK_OP = Mark.Operation.LINK.ordinal();
  private static final long LINK_M_OP = Mark.Operation.LINK_M.ordinal();

  private final int maxEvents;

  // where to write to next
  private int idx;
  private final String[] taskNames;
  private final Marker[] markers;
  private final String[] tagNames;
  private final long[] tagIds;
  private final long[] nanoTimes;
  private final long[] durationNanoTimes;
  private final long[] genOps;

  SynchronizedMarkHolder() {
    this(32768);
  }

  SynchronizedMarkHolder(int maxEvents) {
    if (((maxEvents - 1) & maxEvents) != 0) {
      throw new IllegalArgumentException(maxEvents + " is not a power of two");
    }
    if (maxEvents <= 0) {
      throw new IllegalArgumentException(maxEvents + " is not positive");
    }
    this.maxEvents = maxEvents;
    this.taskNames = new String[maxEvents];
    this.markers = new Marker[maxEvents];
    this.tagNames = new String[maxEvents];
    this.tagIds = new long[maxEvents];
    this.nanoTimes = new long[maxEvents];
    this.durationNanoTimes = new long[maxEvents];
    this.genOps = new long[maxEvents];
  }

  @Override
  public synchronized void start(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    taskNames[idx] = taskName;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_T_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void start(
      long gen, String taskName, Marker marker, String tagName, long tagId, long nanoTime) {
    taskNames[idx] = taskName;
    markers[idx] = marker;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_TM_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void start(long gen, String taskName, long nanoTime) {
    taskNames[idx] = taskName;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void start(long gen, String taskName, Marker marker, long nanoTime) {
    taskNames[idx] = taskName;
    markers[idx] = marker;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_M_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void link(long gen, long linkId) {
    nanoTimes[idx] = Mark.NO_NANOTIME;
    tagIds[idx] = linkId;
    genOps[idx] = gen + LINK_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void link(long gen, long linkId, Marker marker) {
    markers[idx] = marker;
    nanoTimes[idx] = Mark.NO_NANOTIME;
    tagIds[idx] = linkId;
    genOps[idx] = gen + LINK_M_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    taskNames[idx] = taskName;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_T_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(
      long gen, String taskName, Marker marker, String tagName, long tagId, long nanoTime) {
    taskNames[idx] = taskName;
    markers[idx] = marker;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_TM_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(long gen, String taskName, long nanoTime) {
    taskNames[idx] = taskName;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(long gen, String taskName, Marker marker, long nanoTime) {
    taskNames[idx] = taskName;
    markers[idx] = marker;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_M_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(
      long gen, String eventName, String tagName, long tagId, long nanoTime, long durationNanos) {
    taskNames[idx] = eventName;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_T_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(
      long gen,
      String taskName,
      Marker marker,
      String tagName,
      long tagId,
      long nanoTime,
      long durationNanos) {
    taskNames[idx] = taskName;
    markers[idx] = marker;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_TM_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(long gen, String eventName, long nanoTime, long durationNanos) {
    taskNames[idx] = eventName;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(
      long gen, String eventName, Marker marker, long nanoTime, long durationNanos) {
    taskNames[idx] = eventName;
    markers[idx] = marker;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_M_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void resetForTest() {
    Arrays.fill(taskNames, null);
    Arrays.fill(markers, null);
    Arrays.fill(tagNames, null);
    Arrays.fill(tagIds, 0);
    Arrays.fill(nanoTimes, 0);
    Arrays.fill(durationNanoTimes, 0);
    Arrays.fill(genOps, 0);
    idx = 0;
  }

  @Override
  public List<Mark> read(boolean concurrentWrites) {
    final String[] localTaskNames = new String[maxEvents];
    final Marker[] localMarkers = new Marker[maxEvents];
    final String[] localTagNames = new String[maxEvents];
    final long[] localTagIds = new long[maxEvents];
    final long[] localNanoTimes = new long[maxEvents];
    final long[] localGenOps = new long[maxEvents];
    int localIdx;

    synchronized (this) {
      System.arraycopy(taskNames, 0, localTaskNames, 0, maxEvents);
      System.arraycopy(markers, 0, localMarkers, 0, maxEvents);
      System.arraycopy(tagNames, 0, localTagNames, 0, maxEvents);
      System.arraycopy(tagIds, 0, localTagIds, 0, maxEvents);
      System.arraycopy(nanoTimes, 0, localNanoTimes, 0, maxEvents);
      System.arraycopy(genOps, 0, localGenOps, 0, maxEvents);
      localIdx = idx;
    }
    Deque<Mark> marks = new ArrayDeque<Mark>(maxEvents);
    for (int i = 0; i < maxEvents; i++) {
      if (localIdx-- == 0) {
        localIdx += maxEvents;
      }
      long gen = localGenOps[localIdx] & ~0xFFL;
      Mark.Operation op = Mark.Operation.valueOf((int) (localGenOps[localIdx] & 0xFFL));
      if (op == Mark.Operation.NONE) {
        break;
      }
      marks.addFirst(
          Mark.create(
              localTaskNames[localIdx],
              localMarkers[localIdx],
              localTagNames[localIdx],
              localTagIds[localIdx],
              localNanoTimes[localIdx],
              gen,
              op));
    }

    return Collections.unmodifiableList(new ArrayList<Mark>(marks));
  }

  @Override
  public int maxMarks() {
    return maxEvents;
  }
}
