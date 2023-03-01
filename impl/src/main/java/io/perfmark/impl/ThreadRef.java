/*
 * Copyright 2023 Carl Mastrangelo
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

final class ThreadRef extends WeakReference<Thread> {
  private static final ThreadRef IDENTITY = new ThreadRef();

  private final int hashCode;
  private final ThreadInfo threadInfo;

  static ThreadRef newRef(ReferenceQueue<Thread> queue) {
    return new ThreadRef(Thread.currentThread(), queue);
  }

  @SuppressWarnings("deprecation") // thread id is only recently deprecated
  private ThreadRef(Thread thread, ReferenceQueue<Thread> queue) {
    super(thread, queue);
    this.hashCode = System.identityHashCode(thread);
    this.threadInfo = new ThreadRefInfo(thread.getName(), thread.getId());
  }

  private ThreadRef() {
    super(null);
    this.hashCode = 0;
    this.threadInfo = null;
  }

  ThreadInfo asThreadInfo() {
    if (threadInfo == null) {
      throw new UnsupportedOperationException("Can't be called on the identity thread ref.");
    }
    return threadInfo;
  }

  ThreadRef identity() {
    return IDENTITY;
  }

  @Override
  public void clear() {
    // noop
  }

  @Override
  public boolean enqueue() {
    // noop
    return false;
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public int hashCode() {
    return this == IDENTITY ? System.identityHashCode(Thread.currentThread()) : hashCode;
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ThreadRef)) {
      return false;
    }
    Thread that = ((ThreadRef) obj).get();
    Thread thiz = this == IDENTITY ? Thread.currentThread() : get();
    return thiz == that;
  }

  private final class ThreadRefInfo extends ThreadInfo {
    private volatile String threadName;
    private volatile long threadId;

    ThreadRefInfo(String threadName, long threadId) {
      this.threadName = threadName;
      this.threadId = threadId;
    }

    @Override
    @SuppressWarnings("deprecation") // thread id is only recently deprecated
    public String getName() {
      String localThreadName = this.threadName;
      Thread t = ThreadRef.this.get();
      if (t == null) {
        return localThreadName;
      } else {
        localThreadName = maybeUpdateThreadName(t.getName(), localThreadName);
        if (t.getState() == Thread.State.TERMINATED) {
          maybeUpdateThreadId(t.getId(), this.threadId);
          ThreadRef.super.enqueue();
        }
      }
      return localThreadName;
    }

    @Override
    @SuppressWarnings("deprecation") // thread id is only recently deprecated
    public long getId() {
      long localThreadId = this.threadId;
      Thread t = ThreadRef.this.get();
      if (t == null) {
        return localThreadId;
      } else {
        localThreadId = maybeUpdateThreadId(t.getId(), localThreadId);
        if (t.getState() == Thread.State.TERMINATED) {
          maybeUpdateThreadName(t.getName(), this.threadName);
          ThreadRef.super.enqueue();
        }
      }
      return localThreadId;
    }

    private String maybeUpdateThreadName(String tName, String localThreadName) {
      if (!localThreadName.equals(tName)) {
        this.threadName = tName;
        return tName;
      } else {
        return localThreadName;
      }
    }

    private long maybeUpdateThreadId(long threadId, long localThreadId) {
      if (localThreadId != threadId) {
        this.threadId = threadId;
        return threadId;
      } else {
        return localThreadId;
      }
    }

    @Override
    public boolean isTerminated() {
      Thread t = ThreadRef.this.get();
      return t == null || t.getState() == Thread.State.TERMINATED;
    }

    @Override
    public boolean isCurrentThread() {
      Thread t = ThreadRef.this.get();
      return t == Thread.currentThread();
    }
  }
}
