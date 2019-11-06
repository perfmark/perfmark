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

import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.Marker;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

final class SynchronizedMarkHolder extends MarkHolder {
  private static final long GEN_MASK = (1 << Generator.GEN_OFFSET) - 1;

  private static final long START_N1S1_OP = Mark.Operation.TASK_START_N1S1.ordinal();
  private static final long START_N1S2_OP = Mark.Operation.TASK_START_N1S2.ordinal();
  private static final long STOP_N1S1_OP = Mark.Operation.TASK_END_N1S1.ordinal();
  private static final long STOP_N1S2_OP = Mark.Operation.TASK_END_N1S2.ordinal();
  private static final long EVENT_N1S1_OP = Mark.Operation.EVENT_N1S1.ordinal();
  private static final long EVENT_N1S2_OP = Mark.Operation.EVENT_N1S2.ordinal();
  private static final long EVENT_N2S2_OP = Mark.Operation.EVENT_N2S2.ordinal();
  private static final long EVENT_N2S3_OP = Mark.Operation.EVENT_N2S3.ordinal();
  private static final long LINK_OP = Mark.Operation.LINK.ordinal();
  private static final long MARK_OP = Mark.Operation.MARK.ordinal();
  private static final long TAG_N0S1_OP = Mark.Operation.TAG_N0S1.ordinal();
  private static final long TAG_N1S0_OP = Mark.Operation.TAG_N1S0.ordinal();
  private static final long TAG_N1S1_OP = Mark.Operation.TAG_N1S1.ordinal();
  private static final long TAG_KEYED_N0S2_OP = Mark.Operation.TAG_KEYED_N0S2.ordinal();

  private final int maxEvents;
  private final long maxEventsMask;

  // where to write to next
  private long nIdx;
  private long sIdx;
  private long mIdx;

  private final long[] nums;
  private final String[] strings;
  private final Marker[] markers;

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
    this.maxEventsMask = maxEvents - 1L;
    this.nums = new long[maxEvents];
    this.strings = new String[maxEvents];
    this.markers = new Marker[maxEvents];
  }

  private void writeNnss(long genOp, long n0, long n1, String s0, String s1) {
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    nums[(int) (nIdx++ & maxEventsMask)] = n1;
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    strings[(int) (sIdx++ & maxEventsMask)] = s1;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  private void writeNs(long genOp, long n0, String s0) {
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  private void writeN(long genOp, long n0) {
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  @Override
  public synchronized void start(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    writeNs(gen + START_N1S1_OP, nanoTime, taskName);
    writeNs(gen + TAG_N1S1_OP, tagId, tagName);
  }

  @Override
  public synchronized void start(long gen, String taskName, long nanoTime) {
    writeNs(gen + START_N1S1_OP, nanoTime, taskName);
  }

  @Override
  public synchronized void link(long gen, long linkId) {
    writeN(gen + LINK_OP, linkId);
  }

  @Override
  public synchronized void stop(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    writeNs(gen + TAG_N1S1_OP, tagId, tagName);
    writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
  }

  @Override
  public synchronized void stop(long gen, String taskName, long nanoTime) {
    writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
  }

  @Override
  public synchronized void event(
      long gen, String eventName, String tagName, long tagId, long nanoTime) {
    writeNnss(gen + EVENT_N2S2_OP, nanoTime, tagId, eventName, tagName);
  }

  @Override
  public synchronized void event(long gen, String eventName, long nanoTime) {
    writeNs(gen + EVENT_N1S1_OP, nanoTime, eventName);
  }

  @Override
  public synchronized void attachTag(long gen, String tagName, long tagId) {
    writeNs(gen + TAG_N1S1_OP, tagId, tagName);
  }

  @Override
  public synchronized void resetForTest() {
    Arrays.fill(nums, 0);
    Arrays.fill(strings, null);
    Arrays.fill(markers, null);
    nIdx = 0;
    sIdx = 0;
    mIdx = 0;
  }

  @Override
  public List<Mark> read(boolean concurrentWrites) {
    Kyoo<Long> numQ;
    Kyoo<String> stringQ;
    Kyoo<Marker> markerQ;
    {
      final long[] nums = new long[maxEvents];
      final String[] strings = new String[maxEvents];
      final Marker[] markers = new Marker[maxEvents];
      final long nIdx;
      final long sIdx;
      final long mIdx;

      synchronized (this) {
        System.arraycopy(this.nums, 0, nums, 0, maxEvents);
        System.arraycopy(this.strings, 0, strings, 0, maxEvents);
        System.arraycopy(this.markers, 0, markers, 0, maxEvents);
        nIdx = this.nIdx;
        sIdx = this.sIdx;
        mIdx = this.mIdx;
      }
      Long[] numsBoxed = new Long[nums.length];
      for (int i = 0; i < nums.length; i++) {
        numsBoxed[i] = nums[i];
      }
      numQ = new Kyoo<Long>(numsBoxed, nIdx, (int) Math.min(nIdx, maxEvents));
      stringQ = new Kyoo<String>(strings, sIdx, (int) Math.min(sIdx, maxEvents));
      markerQ = new Kyoo<Marker>(markers, mIdx, (int) Math.min(mIdx, maxEvents));
    }

    Deque<Mark> marks = new ArrayDeque<Mark>(maxEvents);

    while (true) {
      if (numQ.isEmpty()) {
        break;
      }
      long genOp = numQ.remove();
      long gen = genOp & ~GEN_MASK;
      Mark.Operation op = Mark.Operation.valueOf((int) (genOp & GEN_MASK));

      if (op.getNumbers() > numQ.size()
          || op.getStrings() > stringQ.size()
          || op.getMarkers() > markerQ.size()) {
        break;
      }
      long n1;
      String s1;
      long n2;
      String s2;
      switch (op) {
        case TASK_START_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.taskStart(gen, n1, s1));
          break;
        case TASK_START_N1S2:
          throw new UnsupportedOperationException();
        case TASK_END_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.taskEnd(gen, n1, s1));
          break;
        case TASK_END_N1S2:
          throw new UnsupportedOperationException();
        case EVENT_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.event(gen, n1, s1));
          break;
        case EVENT_N1S2:
          throw new UnsupportedOperationException();
        case EVENT_N2S2:
          n2 = numQ.remove();
          s2 = stringQ.remove();
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.event(gen, n1, s1, s2, n2));
          break;
        case EVENT_N2S3:
          throw new UnsupportedOperationException();
        case MARK:
          throw new UnsupportedOperationException();
        case LINK:
          n1 = numQ.remove();
          marks.addFirst(Mark.link(gen, n1));
          break;
        case TAG_N0S1:
          throw new UnsupportedOperationException();
        case TAG_N1S0:
          throw new UnsupportedOperationException();
        case TAG_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.tag(gen, s1, n1));
          break;
        case TAG_KEYED_N0S2:
          throw new UnsupportedOperationException();
        case NONE:
          throw new UnsupportedOperationException();
      }
    }

    return Collections.unmodifiableList(new ArrayList<Mark>(marks));
  }

  @Override
  public int maxMarks() {
    return maxEvents;
  }

  private final class Kyoo<T> extends AbstractCollection<T> implements Queue<T> {

    private final T[] elements;
    private final long wIdx;
    private final int size;

    private int ri;

    Kyoo(T[] elements, long wIdx, int size) {
      this.elements = elements;
      this.wIdx = wIdx;
      this.size = size;
    }

    @Override
    public Iterator<T> iterator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
      return size - ri;
    }

    @Override
    public boolean offer(T t) {
      throw new UnsupportedOperationException();
    }

    @Override
    public T remove() {
      checkSize();
      return poll();
    }

    @Override
    public T poll() {
      if (size() == 0) {
        return null;
      }
      int rIdx = (int) (((wIdx - 1) - ri++) & maxEventsMask);
      return elements[rIdx];
    }

    @Override
    public T element() {
      checkSize();
      return peek();
    }

    @Override
    public T peek() {
      if (size() == 0) {
        return null;
      }
      int rIdx = (int) (((wIdx - 1) - ri) & maxEventsMask);
      return elements[rIdx];
    }

    private void checkSize() {
      if (size() == 0) {
        throw new IllegalStateException();
      }
    }
  }
}
