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

package io.perfmark.java9;

import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.Marker;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.List;

/** VarHandleMarkHolder is a MarkHolder optimized for wait free writes and few reads. */
final class VarHandleMarkHolder extends MarkHolder {
  private static final long GEN_MASK = (1 << Generator.GEN_OFFSET) - 1;
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
  private static final long ATTACH_T_OP = Mark.Operation.ATTACH_TAG.ordinal();

  private static final VarHandle IDX;
  private static final VarHandle MARKERS;
  private static final VarHandle STRINGS;
  private static final VarHandle LONGS;

  static {
    try {
      IDX = MethodHandles.lookup().findVarHandle(VarHandleMarkHolder.class, "idx", long.class);
      MARKERS = MethodHandles.arrayElementVarHandle(Marker[].class);
      STRINGS = MethodHandles.arrayElementVarHandle(String[].class);
      LONGS = MethodHandles.arrayElementVarHandle(long[].class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private final int maxEvents;
  private final long maxEventsMax;

  // where to write to next
  @SuppressWarnings("unused") // Used Reflectively
  private volatile long idx;

  private final String[] taskNames;
  private final Marker[] markers;
  private final String[] tagNames;
  private final long[] tagIds;
  private final long[] nanoTimes;
  private final long[] durationNanoTimes;
  private final long[] genOps;

  VarHandleMarkHolder() {
    this(32768);
  }

  VarHandleMarkHolder(int maxEvents) {
    if (((maxEvents - 1) & maxEvents) != 0) {
      throw new IllegalArgumentException(maxEvents + " is not a power of two");
    }
    if (maxEvents <= 0) {
      throw new IllegalArgumentException(maxEvents + " is not positive");
    }
    this.maxEvents = maxEvents;
    this.maxEventsMax = maxEvents - 1L;
    this.taskNames = new String[maxEvents];
    this.markers = new Marker[maxEvents];
    this.tagNames = new String[maxEvents];
    this.tagIds = new long[maxEvents];
    this.nanoTimes = new long[maxEvents];
    this.durationNanoTimes = new long[maxEvents];
    this.genOps = new long[maxEvents];
  }

  @Override
  public void start(long gen, String taskName, String tagName, long tagId, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + START_T_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void start(
      long gen, String taskName, Marker marker, String tagName, long tagId, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    MARKERS.setOpaque(markers, i, marker);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + START_TM_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void start(long gen, String taskName, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + START_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void start(long gen, String taskName, Marker marker, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    MARKERS.setOpaque(markers, i, marker);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + START_M_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void link(long gen, long linkId) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    LONGS.setOpaque(tagIds, i, linkId);
    LONGS.setOpaque(genOps, i, gen + LINK_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void link(long gen, long linkId, Marker marker) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    LONGS.setOpaque(tagIds, i, linkId);
    MARKERS.setOpaque(markers, i, marker);
    LONGS.setOpaque(genOps, i, gen + LINK_M_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void stop(long gen, String taskName, String tagName, long tagId, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + STOP_T_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void stop(
      long gen, String taskName, Marker marker, String tagName, long tagId, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    MARKERS.setOpaque(markers, i, marker);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + STOP_TM_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void stop(long gen, String taskName, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + STOP_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void stop(long gen, String taskName, Marker marker, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    MARKERS.setOpaque(markers, i, marker);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + STOP_M_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(
      long gen, String eventName, String tagName, long tagId, long nanoTime, long durationNanos) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(durationNanoTimes, i, durationNanos);
    LONGS.setOpaque(genOps, i, gen + EVENT_T_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(
      long gen,
      String eventName,
      Marker marker,
      String tagName,
      long tagId,
      long nanoTime,
      long durationNanos) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    MARKERS.setOpaque(markers, i, marker);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(durationNanoTimes, i, durationNanos);
    LONGS.setOpaque(genOps, i, gen + EVENT_TM_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(long gen, String eventName, long nanoTime, long durationNanos) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(durationNanoTimes, i, durationNanos);
    LONGS.setOpaque(genOps, i, gen + EVENT_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(long gen, String eventName, Marker marker, long nanoTime, long durationNanos) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    MARKERS.setOpaque(markers, i, marker);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(durationNanoTimes, i, durationNanos);
    LONGS.setOpaque(genOps, i, gen + EVENT_M_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void attachTag(long gen, String tagName, long tagId) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(genOps, i, gen + ATTACH_T_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void resetForTest() {
    Arrays.fill(taskNames, null);
    Arrays.fill(markers, null);
    Arrays.fill(tagNames, null);
    Arrays.fill(tagIds, 0);
    Arrays.fill(nanoTimes, 0);
    Arrays.fill(durationNanoTimes, 0);
    Arrays.fill(genOps, 0);
    IDX.setRelease(this, 0L);
    VarHandle.storeStoreFence();
  }

  @Override
  public List<Mark> read(boolean concurrentWrites) {
    final String[] localTaskNames = new String[maxEvents];
    final Marker[] localMarkers = new Marker[maxEvents];
    final String[] localTagNames = new String[maxEvents];
    final long[] localTagIds = new long[maxEvents];
    final long[] localNanoTimes = new long[maxEvents];
    final long[] localGenOps = new long[maxEvents];
    long startIdx = (long) IDX.getOpaque(this);
    VarHandle.loadLoadFence();
    int size = (int) Math.min(startIdx, maxEvents);
    for (int i = 0; i < size; i++) {
      localTaskNames[i] = (String) STRINGS.getOpaque(taskNames, i);
      localMarkers[i] = (Marker) MARKERS.getOpaque(markers, i);
      localTagNames[i] = (String) STRINGS.getOpaque(tagNames, i);
      localTagIds[i] = (long) LONGS.getOpaque(tagIds, i);
      localNanoTimes[i] = (long) LONGS.getOpaque(nanoTimes, i);
      localGenOps[i] = (long) LONGS.getOpaque(genOps, i);
    }
    VarHandle.loadLoadFence();
    long endIdx = (long) IDX.getOpaque(this);
    if (endIdx < startIdx) {
      throw new AssertionError();
    }
    // If we are reading from ourselves (such as in a test), we can assume there isn't an in
    // progress write modifying the oldest entry.  Additionally, if the writer has not yet
    // wrapped around, the last entry cannot have been corrupted.
    boolean tailValid = !concurrentWrites || endIdx < maxEvents - 1;
    endIdx += !tailValid ? 1 : 0;
    long eventsToDrop = endIdx - startIdx;
    final Deque<Mark> marks = new ArrayDeque<>(size);
    for (int i = 0; i < size - eventsToDrop; i++) {
      int readIdx = (int) ((startIdx - i - 1) & maxEventsMax);
      long gen = localGenOps[readIdx] & ~GEN_MASK;
      Mark.Operation op = Mark.Operation.valueOf((int) (localGenOps[readIdx] & GEN_MASK));
      if (op == Mark.Operation.NONE) {
        throw new ConcurrentModificationException("Read of storage was not threadsafe");
      }
      marks.addFirst(
          Mark.create(
              localTaskNames[readIdx],
              localMarkers[readIdx],
              localTagNames[readIdx],
              localTagIds[readIdx],
              localNanoTimes[readIdx],
              gen,
              op));
    }

    return Collections.unmodifiableList(new ArrayList<>(marks));
  }

  @Override
  public int maxMarks() {
    return maxEvents;
  }
}
