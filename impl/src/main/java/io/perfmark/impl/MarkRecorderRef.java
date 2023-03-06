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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a MarkRecorder ID and the Thread that created it.
 */
public final class MarkRecorderRef {
  private static final AtomicLong markRecorderAlloc = new AtomicLong(1);

  public static long allocateMarkRecorderId() {
    return markRecorderAlloc.getAndIncrement();
  }

  private final long markRecorderId;
  private final ThreadInfo threadInfo;

  /**
   * Creates a new MarkRecorderRef that can update the thread name and ID from the given Thread Reference.
   */
  public static MarkRecorderRef newRef(ThreadInfo threadInfo) {
    return new MarkRecorderRef(markRecorderAlloc.getAndIncrement(), threadInfo);
  }

  /**
   * Creates a new MarkRecorderRef that can update the thread name and ID from the current thread.
   */
  public static MarkRecorderRef newRef() {
    ThreadRef threadRef = ThreadRef.newRef(null);
    return new MarkRecorderRef(markRecorderAlloc.getAndIncrement(), new ThreadRefInfo(threadRef));
  }

  private MarkRecorderRef(long markRecorderId, ThreadInfo threadInfo) {
    if (markRecorderId <= 0) {
      throw new IllegalArgumentException("non-positive markRecorderId");
    }
    this.markRecorderId = markRecorderId;
    if (threadInfo == null) {
      throw new NullPointerException("threadInfo is null");
    }
    this.threadInfo = threadInfo;
  }

  public long markRecorderId() {
    return markRecorderId;
  }

  public ThreadInfo threadInfo() {
    return threadInfo;
  }

  @Override
  public int hashCode() {
    return (int)(markRecorderId ^ (markRecorderId >>> 32));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MarkRecorderRef that = (MarkRecorderRef) o;
    return markRecorderId == that.markRecorderId;
  }

  @Override
  public String toString() {
    return "MarkRecorderRef[" + markRecorderId + ", alive=" + !threadInfo().isTerminated() + "]";
  }
}
