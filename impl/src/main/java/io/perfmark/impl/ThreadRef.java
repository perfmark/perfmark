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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class ThreadRef extends WeakReference<Thread> {
  private static final ThreadRef IDENTITY = new ThreadRef();

  private final int hashCode;

  static ThreadRef newRef(ReferenceQueue<Thread> queue) {
    return new ThreadRef(Thread.currentThread(), queue);
  }

  static <T> T get(Map<ThreadRef, T> map) {
    return map.get(IDENTITY);
  }

  static <T> T removeAndClearRef(Map<ThreadRef, T> map) {
    // TODO(carl-mastrangelo): actually clear the ref
    return map.remove(IDENTITY);
  }

  private ThreadRef(Thread thread, ReferenceQueue<Thread> queue) {
    super(thread, queue);
    this.hashCode = System.identityHashCode(thread);
  }

  private ThreadRef() {
    super(null);
    this.hashCode = 0;
  }

  @Override
  @Deprecated // Don't call, does nothing.
  public void clear() {
    // noop
  }

  @Override
  @Deprecated // Don't call, does nothing.
  public boolean enqueue() {
    // noop
    return false;
  }

  void clearSafe() {
    super.clear();
  }

  boolean enqueueSafe() {
    return super.enqueue();
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public int hashCode() {
    if (this == IDENTITY) {
      return System.identityHashCode(Thread.currentThread());
    } else {
      return hashCode;
    }
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
    ThreadRef that = ((ThreadRef) obj);
    boolean isEqual;
    if (this == IDENTITY) {
      isEqual = Thread.currentThread() == that.get();
    } else {
      isEqual = this.get() == that.get();
    }
    return isEqual;
  }
}
