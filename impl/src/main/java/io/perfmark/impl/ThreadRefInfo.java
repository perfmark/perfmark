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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Objects;

final class ThreadRefInfo extends ThreadInfo {
  private final ThreadRef threadRef;
  private volatile SoftReference<String> threadNameRef;
  private volatile long threadId;

  @SuppressWarnings("deprecation")
  ThreadRefInfo(ThreadRef threadRef) {
    this.threadRef = threadRef;
    Thread thread = threadRef.get();
    if (thread == null) {
      throw new IllegalArgumentException("can't use terminated thread");
    }
    this.threadNameRef = new SoftReference<>(thread.getName());
    this.threadId = thread.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation") // thread id is only recently deprecated
  public String getName() {
    final SoftReference<String> localThreadNameRef = this.threadNameRef;
    final String threadName;
    Thread t = threadRef.get();
    if (t != null) {
      threadName = t.getName();
      maybeUpdateThreadName(threadName, localThreadNameRef);
      if (t.getState() == Thread.State.TERMINATED) {
        maybeUpdateThreadId(t.getId(), this.threadId);
        threadRef.enqueueSafe();
      }
    } else if ((threadName = localThreadNameRef.get()) == null) {
      return "Thread name GCed";
    }
    return threadName;
  }

  @Override
  @SuppressWarnings("deprecation") // thread id is only recently deprecated
  public long getId() {
    long localThreadId = this.threadId;
    Thread t = threadRef.get();
    if (t == null) {
      return localThreadId;
    } else {
      localThreadId = maybeUpdateThreadId(t.getId(), localThreadId);
      if (t.getState() == Thread.State.TERMINATED) {
        maybeUpdateThreadName(t.getName(), this.threadNameRef);
        threadRef.enqueueSafe();
      }
    }
    return localThreadId;
  }

  private void maybeUpdateThreadName(String threadName, Reference<String> localThreadNameRef) {
    if (!Objects.equals(threadName, localThreadNameRef.get())) {
      localThreadNameRef.enqueue();
      this.threadNameRef = new SoftReference<>(threadName);
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
    Thread t = threadRef.get();
    return t == null || t.getState() == Thread.State.TERMINATED;
  }

  @Override
  public boolean isCurrentThread() {
    Thread t = threadRef.get();
    return t == Thread.currentThread();
  }

  // For Testing
  void clearThreadNameRef() {
    threadNameRef.enqueue();
  }
}
