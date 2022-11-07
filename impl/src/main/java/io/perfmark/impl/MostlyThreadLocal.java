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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * This class tries to access the mark holder in a thread local.  If the class is unable to store the created
 * MarkHolder in the thread local, a concurrent map of Thread to MarkHolders is used for lookup.  In Java 19
 * Thread Locals can be disabled due to use of Virtual threads.
 *
 * <p>
 *   Note that this class avoids using {@link #initialValue()} because it may be called multiple times, in the
 *   case that ThreadLocals are unsettable.
 */
final class MostlyThreadLocal extends ThreadLocal<MarkHolder> {
  private static final int BITS = 11;
  private static final int SIZE = 1 << BITS;
  private static final int MASK = SIZE - 1;
  private static final AtomicReferenceArray<CopyOnWriteArrayList<Storage.MarkHolderHandle>> threadToHandles =
      new AtomicReferenceArray<CopyOnWriteArrayList<Storage.MarkHolderHandle>>(SIZE);

  MostlyThreadLocal() {}

  @Override
  public MarkHolder get() {
    MarkHolder markHolder = super.get();
    if (markHolder != null) {
      return markHolder;
    }
    return getAndSetSlow();
  }

  private MarkHolder getAndSetSlow() {
    assert super.get() == null;
    Storage.MarkHolderHandle markHolderHandle;
    MarkHolder markHolder;
    Storage.MarkHolderAndHandle markHolderAndHandle;
    Thread thread = Thread.currentThread();
    CopyOnWriteArrayList<Storage.MarkHolderHandle> handles = getHandles(thread);
    if (handles == null) {
      markHolderAndHandle = Storage.allocateMarkHolder();
      markHolder = markHolderAndHandle.markHolder();
      assert markHolder != null;
      markHolderHandle = markHolderAndHandle.handle();
      try {
        set(markHolder);
        return markHolder;
      } catch (UnsupportedOperationException e) {
        // ignore.
      }
      handles = getOrCreateHandles(thread);
    } else {
      if ((markHolderHandle = getConcurrent(handles)) != null) {
        if ((markHolder = markHolderHandle.markHolder()) != null) {
          return markHolder;
        }
      }
      markHolderAndHandle = Storage.allocateMarkHolder();
      markHolder = markHolderAndHandle.markHolder();
      assert markHolder != null;
      markHolderHandle = markHolderAndHandle.handle();
    }
    handles.add(markHolderHandle);
    return markHolder;
  }

  @Override
  public void remove() {
    Thread thread = Thread.currentThread();
    CopyOnWriteArrayList<Storage.MarkHolderHandle> handles = getHandles(thread);
    if (handles == null) {
      super.remove();
    } else {
      assert super.get() == null;
      for (Storage.MarkHolderHandle handle : handles) {
        Thread t = handle.threadRef().get();
        if (t == null || t == thread) {
          handles.remove(handle);
        }
      }
    }
  }

  /**
   * Returns the handles for the given thread index bucket, or {@code null}.
   */
  private static CopyOnWriteArrayList<Storage.MarkHolderHandle> getHandles(Thread thread) {
    int hashCode = System.identityHashCode(thread);
    int index = hashCode & MASK;
    return threadToHandles.get(index);
  }

  private static CopyOnWriteArrayList<Storage.MarkHolderHandle> getOrCreateHandles(Thread thread) {
    int hashCode = System.identityHashCode(thread);
    int index = hashCode & MASK;
    CopyOnWriteArrayList<Storage.MarkHolderHandle> handles;
    do {
      if ((handles = threadToHandles.get(index)) != null) {
        break;
      }
    } while (
        !threadToHandles.compareAndSet(index, null, (handles = new CopyOnWriteArrayList<Storage.MarkHolderHandle>())));
    return handles;
  }

  /**
   * May return {@code null} if not found.
   */
  private static Storage.MarkHolderHandle getConcurrent(CopyOnWriteArrayList<Storage.MarkHolderHandle> handles) {
    if (!handles.isEmpty()) {
      Storage.MarkHolderHandle handle;
      try {
        handle = handles.get(0);
      } catch (IndexOutOfBoundsException e) {
        return null;
      }
      if (handle.threadRef().get() == Thread.currentThread()) {
        return handle;
      }
      return slowGetConcurrent(handles);
    }
    return null;
  }

  /**
   * May return {@code null} if not found.
   */
  private static Storage.MarkHolderHandle slowGetConcurrent(CopyOnWriteArrayList<Storage.MarkHolderHandle> handles) {
    for (Storage.MarkHolderHandle handle : handles) {
      Thread thread = handle.threadRef().get();
      if (thread == null) {
        handles.remove(handle);
      } else if (thread == Thread.currentThread()) {
        return handle;
      }
    }
    return null;
  }
}
