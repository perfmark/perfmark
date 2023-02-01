/*
 * Copyright 2022 Carl Mastrangelo
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

package io.perfmark.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * This class tries to access the mark recorder in a thread local.  If the class is unable to store the created
 * MarkHolder in the thread local, a concurrent map of Thread to MarkHolders is used for lookup.  In Java 19
 * Thread Locals can be disabled due to use of Virtual threads.
 *
 * <p>
 *   Note that this class avoids using {@link #initialValue()} because it may be called multiple times, in the
 *   case that ThreadLocals are unsettable.
 */
final class MostlyThreadLocal extends ThreadLocal<MarkRecorder> {
  private static final int BITS = 11;
  private static final int SIZE = 1 << BITS;
  private static final int MASK = SIZE - 1;
  private final ReferenceQueue<Thread> referenceQueue = new ReferenceQueue<Thread>();
  private final ConcurrentHashMap<MarkRecorderHandle, MarkRecorderHandle> allHandles =
      new ConcurrentHashMap<MarkRecorderHandle, MarkRecorderHandle>();
  private final AtomicReferenceArray<CopyOnWriteArrayList<MarkRecorderHandle>> threadToHandles =
      new AtomicReferenceArray<CopyOnWriteArrayList<MarkRecorderHandle>>(SIZE);

  MostlyThreadLocal() {}

  @Override
  public MarkRecorder get() {
    MarkRecorder markRecorder = super.get();
    if (markRecorder != null) {
      return markRecorder;
    }
    return getAndSetSlow();
  }

  private MarkRecorder getAndSetSlow() {
    assert super.get() == null;
    MarkRecorder markRecorder;
    Thread thread = Thread.currentThread();
    int index = indexOf(thread);
    CopyOnWriteArrayList<MarkRecorderHandle> handles = threadToHandles.get(index);
    if (handles == null) {
      markRecorder = Storage.allocateMarkRecorder();
      try {
        set(markRecorder);
        return markRecorder;
      } catch (UnsupportedOperationException e) {
        // ignore.
      }
      handles = getOrCreateHandles(index);
    } else {
      MarkRecorderHandle markRecorderHandle;
      if ((markRecorderHandle = getConcurrent(handles)) != null) {
        return markRecorderHandle.markRecorder;
      }
      markRecorder = Storage.allocateMarkRecorder();
    }
    handles.add(0, new MarkRecorderHandle(markRecorder, thread, referenceQueue));
    return markRecorder;
  }

  @Override
  public void remove() {
    drainQueue();
    Thread thread = Thread.currentThread();
    CopyOnWriteArrayList<MarkRecorderHandle> handles = threadToHandles.get(indexOf(thread));
    if (handles == null) {
      super.remove();
    } else {
      assert super.get() == null;
      for (MarkRecorderHandle handle : handles) {
        Thread t = handle.get();
        if (t == null || t == thread) {
          handles.remove(handle);
          handle.clear();
        }
      }
    }
  }

  private void drainQueue() {
    while (true) {
      MarkRecorderHandle handle = (MarkRecorderHandle) referenceQueue.poll();
      if (handle == null) {
        break;
      }
      CopyOnWriteArrayList<MarkRecorderHandle> handles = threadToHandles.get(handle.index);
      handles.remove(handle);
    }
  }

  private static int indexOf(Thread thread) {
    return System.identityHashCode(thread) & MASK;
  }

  private CopyOnWriteArrayList<MarkRecorderHandle> getOrCreateHandles(int index) {
    CopyOnWriteArrayList<MarkRecorderHandle> handles;
    do {
      if ((handles = threadToHandles.get(index)) != null) {
        break;
      }
    } while (!threadToHandles.compareAndSet(index, null, (handles = new CopyOnWriteArrayList<MarkRecorderHandle>())));
    return handles;
  }

  /**
   * May return {@code null} if not found.
   */
  private static MarkRecorderHandle getConcurrent(CopyOnWriteArrayList<MarkRecorderHandle> handles) {
    if (!handles.isEmpty()) {
      MarkRecorderHandle handle;
      try {
        handle = handles.get(0);
      } catch (IndexOutOfBoundsException e) {
        return null;
      }
      if (handle.get() == Thread.currentThread()) {
        return handle;
      }
      return slowGetConcurrent(handles);
    }
    return null;
  }

  /**
   * May return {@code null} if not found.
   */
  private static MarkRecorderHandle slowGetConcurrent(CopyOnWriteArrayList<MarkRecorderHandle> handles) {
    for (MarkRecorderHandle handle : handles) {
      Thread thread = handle.get();
      if (thread == null) {
        handles.remove(handle);
      } else if (thread == Thread.currentThread()) {
        return handle;
      }
    }
    return null;
  }

  private static final class MarkRecorderHandle extends WeakReference<Thread> {
    private final int index;
    private final MarkRecorder markRecorder;

    public MarkRecorderHandle(
        MarkRecorder markRecorder, Thread referent, ReferenceQueue<? super Thread> referenceQueue) {
      super(referent, referenceQueue);
      this.index = indexOf(referent);
      this.markRecorder = markRecorder;
    }
  }
}
