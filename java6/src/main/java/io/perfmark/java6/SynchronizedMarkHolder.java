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
  private static final long START_NOTAG_OP = Mark.Operation.TASK_NOTAG_START.ordinal();
  private static final long STOP_OP = Mark.Operation.TASK_END.ordinal();
  private static final long STOP_NOTAG_OP = Mark.Operation.TASK_NOTAG_END.ordinal();
  private static final long EVENT_OP = Mark.Operation.EVENT.ordinal();
  private static final long EVENT_NOTAG_OP = Mark.Operation.EVENT_NOTAG.ordinal();
  private static final long LINK_OP = Mark.Operation.LINK.ordinal();

  private final int maxEvents;

  // where to write to next
  private int idx;
  private final Object[] taskNameOrMarkers;
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
    this.taskNameOrMarkers = new Object[maxEvents];
    this.tagNames = new String[maxEvents];
    this.tagIds= new long[maxEvents];
    this.nanoTimes = new long[maxEvents];
    this.durationNanoTimes = new long[maxEvents];
    this.genOps = new long[maxEvents];
  }

  @Override
  public synchronized void start(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    taskNameOrMarkers[idx] = taskName;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void start(
      long gen, Marker marker, String tagName, long tagId, long nanoTime) {
    taskNameOrMarkers[idx] = marker;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void start(long gen, String taskName, long nanoTime) {
    taskNameOrMarkers[idx] = taskName;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_NOTAG_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void start(long gen, Marker marker, long nanoTime) {
    taskNameOrMarkers[idx] = marker;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + START_NOTAG_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void link(long gen, long linkId, Marker marker) {
    taskNameOrMarkers[idx] = marker;
    nanoTimes[idx] = Mark.NO_NANOTIME;
    tagIds[idx] = linkId;
    genOps[idx] = gen + LINK_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    taskNameOrMarkers[idx] = taskName;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(
      long gen, Marker marker, String tagName, long tagId, long nanoTime) {
    taskNameOrMarkers[idx] = marker;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(long gen, String taskName, long nanoTime) {
    taskNameOrMarkers[idx] = taskName;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_NOTAG_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void stop(long gen, Marker marker, long nanoTime) {
    taskNameOrMarkers[idx] = marker;
    nanoTimes[idx] = nanoTime;
    genOps[idx] = gen + STOP_NOTAG_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(
      long gen, String eventName, String tagName, long tagId, long nanoTime, long durationNanos) {
    taskNameOrMarkers[idx] = eventName;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(
      long gen, Marker marker, String tagName, long tagId, long nanoTime, long durationNanos) {
    taskNameOrMarkers[idx] = marker;
    tagNames[idx] = tagName;
    tagIds[idx] = tagId;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(long gen, String eventName, long nanoTime, long durationNanos) {
    taskNameOrMarkers[idx] = eventName;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_NOTAG_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void event(long gen, Marker marker, long nanoTime, long durationNanos) {
    taskNameOrMarkers[idx] = marker;
    nanoTimes[idx] = nanoTime;
    durationNanoTimes[idx] = durationNanos;
    genOps[idx] = gen + EVENT_NOTAG_OP;
    if (++idx == maxEvents) {
      idx = 0;
    }
  }

  @Override
  public synchronized void resetForTest() {
    Arrays.fill(taskNameOrMarkers, null);
    Arrays.fill(tagNames, null);
    Arrays.fill(tagIds, 0);
    Arrays.fill(nanoTimes, 0);
    Arrays.fill(durationNanoTimes, 0);
    Arrays.fill(genOps, 0);
    idx = 0;
  }

  @Override
  public List<Mark> read(boolean readerIsWriter) {
    final Object[] localTaskNameOrMarkers = new Object[maxEvents];
    final String[] localTagNames = new String[maxEvents];
    final long[] localTagIds= new long[maxEvents];
    final long[] localNanoTimes = new long[maxEvents];
    final long[] localGenOps = new long[maxEvents];
    int localIdx;

    synchronized (this) {
      System.arraycopy(taskNameOrMarkers, 0, localTaskNameOrMarkers, 0, maxEvents);
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
      Object taskNameOrMarker = localTaskNameOrMarkers[localIdx];
      if (taskNameOrMarker instanceof Marker) {
        marks.addFirst(Mark.create(
            (Marker) taskNameOrMarker,
            localTagNames[localIdx],
            localTagIds[localIdx],
            localNanoTimes[localIdx],
            gen,
            op));
      } else if (taskNameOrMarker instanceof String) {
        marks.addFirst(Mark.create(
            (String) taskNameOrMarker,
            localTagNames[localIdx],
            localTagIds[localIdx],
            localNanoTimes[localIdx],
            gen,
            op));
      } else {
        throw new RuntimeException("Bad marker or string " + taskNameOrMarker);
      }
    }

    return Collections.unmodifiableList(new ArrayList<Mark>(marks));
  }

  @Override
  public int maxMarks() {
    return maxEvents;
  }
}
