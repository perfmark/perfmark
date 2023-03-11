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

final class ThreadRefInfo extends ThreadInfo {
  private final ThreadRef threadRef;
  private volatile String threadName;
  private volatile long threadId;

  @SuppressWarnings("deprecation")
  ThreadRefInfo(ThreadRef threadRef) {
    this.threadRef = threadRef;
    Thread thread = threadRef.get();
    if (thread == null) {
      throw new IllegalArgumentException("can't use terminated thread");
    }
    this.threadName = thread.getName();
    this.threadId = thread.getId();
  }

  @Override
  @SuppressWarnings("deprecation") // thread id is only recently deprecated
  public String getName() {
    String localThreadName = this.threadName;
    Thread t = threadRef.get();
    if (t == null) {
      return localThreadName;
    } else {
      localThreadName = maybeUpdateThreadName(t.getName(), localThreadName);
      if (t.getState() == Thread.State.TERMINATED) {
        maybeUpdateThreadId(t.getId(), this.threadId);
        threadRef.enqueueSafe();
      }
    }
    return localThreadName;
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
        maybeUpdateThreadName(t.getName(), this.threadName);
        threadRef.enqueueSafe();
      }
    }
    return localThreadId;
  }

  private String maybeUpdateThreadName(String tName, String localThreadName) {
    if (!tName.equals(localThreadName)) {
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
    Thread t = threadRef.get();
    return t == null || t.getState() == Thread.State.TERMINATED;
  }

  @Override
  public boolean isCurrentThread() {
    Thread t = threadRef.get();
    return t == Thread.currentThread();
  }
}
