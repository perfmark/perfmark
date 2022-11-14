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
import io.perfmark.impl.MarkRecorder;
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
final class VarHandleMarkHolder extends MarkRecorder {
  private static final long GEN_MASK = (1 << Generator.GEN_OFFSET) - 1;
  private static final long START_OP = 1; // Mark.Operation.TASK_START.ordinal();
  private static final long START_S_OP = 2;
  private static final long START_T_OP = 3; // Mark.Operation.TASK_START_T.ordinal();
  private static final long STOP_OP = 4; // Mark.Operation.TASK_END.ordinal();
  private static final long STOP_V_OP = 5;
  private static final long STOP_T_OP = 6; // Mark.Operation.TASK_END_T.ordinal();
  private static final long STOP_S_OP = 7;
  private static final long EVENT_OP = 8; // Mark.Operation.EVENT.ordinal();
  private static final long EVENT_T_OP = 9; // Mark.Operation.EVENT_T.ordinal();
  private static final long EVENT_S_OP = 10;
  private static final long LINK_OP = 11; // Mark.Operation.LINK.ordinal();
  private static final long ATTACH_T_OP = 12; // Mark.Operation.ATTACH_TAG.ordinal();
  private static final long ATTACH_SS_OP = 13;
  private static final long ATTACH_SN_OP = 14;
  private static final long ATTACH_SNN_OP = 15;

  private static final VarHandle IDX;
  private static final VarHandle STRINGS;
  private static final VarHandle LONGS;

  static {
    try {
      IDX = MethodHandles.lookup().findVarHandle(VarHandleMarkHolder.class, "idx", long.class);
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
  private final String[] tagNames;
  private final long[] tagIds;
  private final long[] nanoTimes;
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
    this.tagNames = new String[maxEvents];
    this.tagIds = new long[maxEvents];
    this.nanoTimes = new long[maxEvents];
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
  public void start(long gen, String taskName, String subTaskName, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    STRINGS.setOpaque(tagNames, i, subTaskName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + START_S_OP);
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
  public void stop(long gen, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + STOP_V_OP);
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
  public void stop(long gen, String taskName, String subTaskName, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, taskName);
    STRINGS.setOpaque(tagNames, i, subTaskName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + STOP_S_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(long gen, String eventName, String tagName, long tagId, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    STRINGS.setOpaque(tagNames, i, tagName);
    LONGS.setOpaque(tagIds, i, tagId);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + EVENT_T_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(long gen, String eventName, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + EVENT_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void event(long gen, String eventName, String subEventName, long nanoTime) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(taskNames, i, eventName);
    STRINGS.setOpaque(tagNames, i, subEventName);
    LONGS.setOpaque(nanoTimes, i, nanoTime);
    LONGS.setOpaque(genOps, i, gen + EVENT_S_OP);
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
  public void attachKeyedTag(long gen, String name, long value) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(tagNames, i, name);
    LONGS.setOpaque(tagIds, i, value);
    LONGS.setOpaque(genOps, i, gen + ATTACH_SN_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void attachKeyedTag(long gen, String name, long value0, long value1) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(tagNames, i, name);
    LONGS.setOpaque(tagIds, i, value0);
    LONGS.setOpaque(nanoTimes, i, value1);
    LONGS.setOpaque(genOps, i, gen + ATTACH_SNN_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }

  @Override
  public void attachKeyedTag(long gen, String name, String value) {
    long localIdx = (long) IDX.get(this);
    int i = (int) (localIdx & maxEventsMax);
    STRINGS.setOpaque(tagNames, i, name);
    STRINGS.setOpaque(taskNames, i, value);
    LONGS.setOpaque(genOps, i, gen + ATTACH_SS_OP);
    IDX.setRelease(this, localIdx + 1);
    VarHandle.storeStoreFence();
  }


  final class MarkHolderForward extends MarkHolder {

    @Override
    public void resetForTest() {
      VarHandleMarkHolder.this.resetForTest();
    }

    @Override
    public List<Mark> read(boolean concurrentWrites) {
      return null;
    }
  }

  @Override
  public void resetForTest() {
    Arrays.fill(taskNames, null);
    Arrays.fill(tagNames, null);
    Arrays.fill(tagIds, 0);
    Arrays.fill(nanoTimes, 0);
    Arrays.fill(genOps, 0);
    IDX.setRelease(this, 0L);
    VarHandle.storeStoreFence();
  }

  @Override
  public List<Mark> read(boolean concurrentWrites) {
    final String[] localTaskNames = new String[maxEvents];
    final String[] localTagNames = new String[maxEvents];
    final long[] localTagIds = new long[maxEvents];
    final long[] localNanoTimes = new long[maxEvents];
    final long[] localGenOps = new long[maxEvents];
    long startIdx = (long) IDX.getOpaque(this);
    VarHandle.loadLoadFence();
    int size = (int) Math.min(startIdx, maxEvents);
    for (int i = 0; i < size; i++) {
      localTaskNames[i] = (String) STRINGS.getOpaque(taskNames, i);
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
      int opVal = (int) (localGenOps[readIdx] & GEN_MASK);
      switch (opVal) {
        case (int) START_T_OP:
          marks.addFirst(Mark.tag(gen, localTagNames[readIdx], localTagIds[readIdx]));
          // fallthrough
        case (int) START_OP:
          marks.addFirst(Mark.taskStart(gen, localNanoTimes[readIdx], localTaskNames[readIdx]));
          break;
        case (int) START_S_OP:
          marks.addFirst(
              Mark.taskStart(
                  gen, localNanoTimes[readIdx], localTaskNames[readIdx], localTagNames[readIdx]));
          break;
        case (int) STOP_V_OP:
          marks.addFirst(Mark.taskEnd(gen, localNanoTimes[readIdx]));
          break;
        case (int) STOP_S_OP:
          marks.addFirst(
              Mark.taskEnd(
                  gen, localNanoTimes[readIdx], localTaskNames[readIdx], localTagNames[readIdx]));
          break;
        case (int) STOP_OP:
          marks.addFirst(Mark.taskEnd(gen, localNanoTimes[readIdx], localTaskNames[readIdx]));
          break;
        case (int) STOP_T_OP:
          marks.addFirst(Mark.taskEnd(gen, localNanoTimes[readIdx], localTaskNames[readIdx]));
          marks.addFirst(Mark.tag(gen, localTagNames[readIdx], localTagIds[readIdx]));
          break;
        case (int) EVENT_OP:
          marks.addFirst(Mark.event(gen, localNanoTimes[readIdx], localTaskNames[readIdx]));
          break;
        case (int) EVENT_T_OP:
          marks.addFirst(
              Mark.event(
                  gen,
                  localNanoTimes[readIdx],
                  localTaskNames[readIdx],
                  localTagNames[readIdx],
                  localTagIds[readIdx]));
          break;
        case (int) EVENT_S_OP:
          marks.addFirst(
              Mark.event(
                  gen, localNanoTimes[readIdx], localTaskNames[readIdx], localTagNames[readIdx]));
          break;
        case (int) LINK_OP:
          marks.addFirst(Mark.link(gen, localTagIds[readIdx]));
          break;
        case (int) ATTACH_T_OP:
          marks.addFirst(Mark.tag(gen, localTagNames[readIdx], localTagIds[readIdx]));
          break;
        case (int) ATTACH_SS_OP:
          marks.addFirst(Mark.keyedTag(gen, localTagNames[readIdx], localTaskNames[readIdx]));
          break;
        case (int) ATTACH_SN_OP:
          marks.addFirst(Mark.keyedTag(gen, localTagNames[readIdx], localTagIds[readIdx]));
          break;
        case (int) ATTACH_SNN_OP:
          marks.addFirst(
              Mark.keyedTag(
                  gen, localTagNames[readIdx], localTagIds[readIdx], localNanoTimes[readIdx]));
          break;
        default:
          throw new ConcurrentModificationException("Read of storage was not threadsafe " + opVal);
      }
    }

    return Collections.unmodifiableList(new ArrayList<>(marks));
  }

  @Override
  public int maxMarks() {
    return maxEvents;
  }
}
