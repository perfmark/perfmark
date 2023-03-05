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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A "thread local" variable that uses weak refs to track the thread to value mapping.
 * This class is useful when {@link ThreadLocal} instances are inconvenient.
 *
 */
public class ConcurrentThreadLocal<T> {
  private final ConcurrentMap<ThreadRef, T> refs = new ConcurrentHashMap<ThreadRef, T>();
  private final ReferenceQueue<Thread> queue = new ReferenceQueue<Thread>();

  public T get() {
    T value = refs.get(ThreadRef.IDENTITY);
    if (value == null) {
      refs.put(new ThreadRef(Thread.currentThread(), queue), value = initialValueInternal());
    }
    return value;
  }

  public void remove() {
    refs.remove(ThreadRef.IDENTITY);
    drainQueue();
  }

  public void set(T value) {
    if (refs.containsKey(ThreadRef.IDENTITY)) {
      refs.replace(ThreadRef.IDENTITY, value);
    } else {
      refs.put(new ThreadRef(Thread.currentThread(), queue), value);
    }
  }

  protected T initialValue() {
    return null;
  }

  private T initialValueInternal() {
    drainQueue();
    return initialValue();
  }

  private void drainQueue() {
    Reference<? extends Thread> ref = queue.poll();
    if (ref == null) {
      return;
    }
    drainQueue(ref);
  }

  private void drainQueue(Reference<? extends Thread> ref) {
    do {
      refs.remove((ThreadRef) ref);
    } while ((ref = queue.poll()) != null);
  }

  private static final class ThreadRef extends WeakReference<Thread> {
    private static final ThreadRef IDENTITY = new ThreadRef(null, null);

    private final int hashCode;

    ThreadRef(Thread thread, ReferenceQueue<? super Thread> queue) {
      super(thread, queue);
      this.hashCode = System.identityHashCode(thread);
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
  }
}
