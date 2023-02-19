/*
 * Copyright 2022 Google LLC
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
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import java.lang.ref.WeakReference;
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

  private final WeakReference<Thread> threadReference;
  private final long markRecorderId;
  private volatile String threadName;
  private volatile long threadId;

  private final int maxEvents;
  private final long maxEventsMask;

  // where to write to next
  private long nIdx;
  private long sIdx;

  private final long[] nums;
  private final String[] strings;

  SynchronizedMarkHolder(int maxEvents, long markRecorderId) {
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
    Thread t = Thread.currentThread();
    this.threadReference = new WeakReference<Thread>(t);
    this.threadName = t.getName();
    this.threadId = t.getId();
    this.markRecorderId = markRecorderId;
  }

  // This must be externally synchronized.
  void writeNnss(long genOp, long n0, long n1, String s0, String s1) {
    assert Thread.holdsLock(this);
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    nums[(int) (nIdx++ & maxEventsMask)] = n1;
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    strings[(int) (sIdx++ & maxEventsMask)] = s1;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  // This must be externally synchronized.
  void writeNss(long genOp, long n0, String s0, String s1) {
    assert Thread.holdsLock(this);
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    strings[(int) (sIdx++ & maxEventsMask)] = s1;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  // This must be externally synchronized.
  void writeNs(long genOp, long n0, String s0) {
    assert Thread.holdsLock(this);
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  // This must be externally synchronized.
  void writeN(long genOp, long n0) {
    assert Thread.holdsLock(this);
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  // This must be externally synchronized.
  void writeNns(long genOp, long n0, long n1, String s0) {
    assert Thread.holdsLock(this);
    nums[(int) (nIdx++ & maxEventsMask)] = n0;
    nums[(int) (nIdx++ & maxEventsMask)] = n1;
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  // This must be externally synchronized.
  void writeSs(long genOp, String s0, String s1) {
    assert Thread.holdsLock(this);
    strings[(int) (sIdx++ & maxEventsMask)] = s0;
    strings[(int) (sIdx++ & maxEventsMask)] = s1;
    nums[(int) (nIdx++ & maxEventsMask)] = genOp;
  }

  @Override
  public synchronized void resetForThread() {
    if (threadReference.get() == null) {
      Storage.unregisterMarkHolder(this);
    }
    if (threadReference.get() != Thread.currentThread()) {
      return;
    }
    Arrays.fill(nums, 0);
    Arrays.fill(strings, null);
    nIdx = 0;
    sIdx = 0;
  }

  @Override
  public synchronized void resetForAll() {
    resetForThread();
  }

  @Override
  public List<MarkList> read() {
    Kyoo<Long> numQ;
    Kyoo<String> stringQ;
    {
      final long[] nums = new long[maxEvents];
      final String[] strings = new String[maxEvents];
      final long nIdx;
      final long sIdx;

      synchronized (this) {
        System.arraycopy(this.nums, 0, nums, 0, maxEvents);
        System.arraycopy(this.strings, 0, strings, 0, maxEvents);
        nIdx = this.nIdx;
        sIdx = this.sIdx;
      }
      Long[] numsBoxed = new Long[nums.length];
      for (int i = 0; i < nums.length; i++) {
        numsBoxed[i] = nums[i];
      }
      numQ = new Kyoo<Long>(numsBoxed, nIdx, (int) Math.min(nIdx, maxEvents));
      stringQ = new Kyoo<String>(strings, sIdx, (int) Math.min(sIdx, maxEvents));
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
          || op.getStrings() > stringQ.size()) {
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
          n1 = numQ.remove();
          s2 = stringQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.taskStart(gen, n1, s1, s2));
          break;
        case TASK_END_N1S0:
          n1 = numQ.remove();
          marks.addFirst(Mark.taskEnd(gen, n1));
          break;
        case TASK_END_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.taskEnd(gen, n1, s1));
          break;
        case TASK_END_N1S2:
          n1 = numQ.remove();
          s2 = stringQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.taskEnd(gen, n1, s1, s2));
          break;
        case EVENT_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.event(gen, n1, s1));
          break;
        case EVENT_N1S2:
          n1 = numQ.remove();
          s2 = stringQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.event(gen, n1, s1, s2));
          break;
        case EVENT_N2S2:
          n2 = numQ.remove();
          s2 = stringQ.remove();
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.event(gen, n1, s1, s2, n2));
          break;
        case EVENT_N2S3:
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
          s2 = stringQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.keyedTag(gen, s1, s2));
          break;
        case TAG_KEYED_N1S1:
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.keyedTag(gen, s1, n1));
          break;
        case TAG_KEYED_N2S1:
          n2 = numQ.remove();
          n1 = numQ.remove();
          s1 = stringQ.remove();
          marks.addFirst(Mark.keyedTag(gen, s1, n1, n2));
          break;
        case NONE:
          throw new UnsupportedOperationException();
      }
    }
    if (marks.isEmpty()) {
      return Collections.emptyList();
    }
    MarkList marksList = MarkList.newBuilder()
        .setMarkRecorderId(markRecorderId)
        .setThreadId(getAndUpdateThreadId())
        .setThreadName(getAndUpdateThreadName())
        .setMarks(new ArrayList<Mark>(marks))
        .build();

    return Collections.singletonList(marksList);
  }

  private String getAndUpdateThreadName() {
    Thread t = threadReference.get();
    String name;
    if (t != null) {
      threadName = (name = t.getName());
    } else {
      name = threadName;
    }
    return name;
  }

  /**
   * Some threads change their id over time, so we need to sync it if available.
   */
  private long getAndUpdateThreadId() {
    Thread t = threadReference.get();
    long id;
    if (t != null) {
      threadId = (id = t.getId());
    } else {
      id = threadId;
    }
    return id;
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
