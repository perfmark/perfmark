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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A "thread local" variable that uses weak refs to track the thread to value mapping.
 * This class is useful when {@link ThreadLocal} instances are inconvenient.
 *
 * <p>Like All classes in this package, this class is not API stable.
 */
public class ConcurrentThreadLocal<T> {
  private final ConcurrentMap<ThreadRef, T> refs = new ConcurrentHashMap<ThreadRef, T>();
  private final ReferenceQueue<Thread> queue = new ReferenceQueue<Thread>();

  public T get() {
    T value;
    if ((value = ThreadRef.get(refs)) != null) {
      return value;
    }
    if ((value = initialValueInternal()) != null) {
      refs.put(ThreadRef.newRef(queue), value);
    }
    return value;
  }

  public void remove() {
    ThreadRef.removeAndClearRef(refs);
    drainQueue();
  }

  public void set(T value) {
    ThreadRef.removeAndClearRef(refs);
    refs.put(ThreadRef.newRef(queue), value);
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
}
