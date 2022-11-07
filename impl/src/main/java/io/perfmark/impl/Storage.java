/*
 * Copyright 2019 Google LLC
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
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * Storage is responsible for storing and returning recorded marks. This is a low level class and
 * not intended for use by users. Instead, the {@code TraceEventWriter} and {@code TraceEventViewer}
 * classes provide easier to use APIs for accessing PerfMark data.
 *
 * <p>This code is <strong>NOT</strong> API stable, and may be removed in the future, or changed
 * without notice.
 */
public final class Storage {
  static final AtomicLong markHolderIdAllocator = new AtomicLong(1);
  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.
  static final ConcurrentMap<Reference<Thread>, MarkHolderHandle> allMarkHolders =
      new ConcurrentHashMap<Reference<Thread>, MarkHolderHandle>();
  private static final LocalMarkHolder localMarkHolder = new MostlyThreadLocalMarkHolder();
  private static final MarkHolderProvider markHolderProvider;
  private static final ReferenceQueue<Thread> threadReferenceQueue = new ReferenceQueue<Thread>();
  private static volatile long lastGlobalIndexClear = Generator.INIT_NANO_TIME - 1;

  static {
    MarkHolderProvider provider = null;
    Throwable[] problems = new Throwable[3];
    try {
      String markHolderOverride = System.getProperty("io.perfmark.PerfMark.markHolderProvider");
      if (markHolderOverride != null && !markHolderOverride.isEmpty()) {
        Class<?> clz = Class.forName(markHolderOverride);
        provider = clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance();
      }
    } catch (Throwable t) {
      problems[0] = t;
    }
    if (provider == null) {
      try {
        Class<?> clz =
            Class.forName(
                "io.perfmark.java9.SecretVarHandleMarkHolderProvider$VarHandleMarkHolderProvider");
        provider = clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance();
      } catch (Throwable t) {
        problems[1] = t;
      }
    }
    if (provider == null) {
      try {
        Class<?> clz =
            Class.forName(
                "io.perfmark.java6.SecretSynchronizedMarkHolderProvider$SynchronizedMarkHolderProvider");
        provider = clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance();
      } catch (Throwable t) {
        problems[2] = t;
      }
    }
    if (provider == null) {
      markHolderProvider = new NoopMarkHolderProvider();
    } else {
      markHolderProvider = provider;
    }
    try {
      if (Boolean.getBoolean("io.perfmark.PerfMark.debug")) {
        // See the comment in io.perfmark.PerfMark for why this is invoked reflectively.
        Class<?> logClass = Class.forName("java.util.logging.Logger");
        Object logger = logClass.getMethod("getLogger", String.class).invoke(null, Storage.class.getName());
        Class<?> levelClass = Class.forName("java.util.logging.Level");
        Object level = levelClass.getField("FINE").get(null);
        Method logProblemMethod = logClass.getMethod("log", levelClass, String.class, Throwable.class);

        for (Throwable problem : problems) {
          if (problem == null) {
            continue;
          }
          logProblemMethod.invoke(logger, level, "Error loading MarkHolderProvider", problem);
        }
        Method logSuccessMethod = logClass.getMethod("log", levelClass, String.class, Object[].class);
        logSuccessMethod.invoke(logger, level, "Using {0}", new Object[] {markHolderProvider.getClass().getName()});
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  public static long getInitNanoTime() {
    return Generator.INIT_NANO_TIME;
  }

  /**
   * Returns a list of {@link MarkList}s across all reachable threads.
   *
   * @return all reachable MarkLists.
   */
  public static List<MarkList> read() {
    long lastReset = lastGlobalIndexClear;
    drainThreadQueue();
    List<MarkList> markLists = new ArrayList<MarkList>(allMarkHolders.size());
    for (Iterator<MarkHolderHandle> it = allMarkHolders.values().iterator(); it.hasNext();) {
      MarkHolderHandle handle = it.next();
      Thread writer = handle.threadRef().get();
      if (writer == null) {
        handle.softenMarkHolderReference();
      }
      MarkHolder markHolder = handle.markHolder();
      if (markHolder == null) {
        it.remove();
        handle.clearSoftReference();
        continue;
      }
      String threadName = handle.getAndUpdateThreadName();
      long threadId = handle.getAndUpdateThreadId();
      boolean concurrentWrites = !(Thread.currentThread() == writer || writer == null);
      markLists.add(
          MarkList.newBuilder()
              .setMarks(markHolder.read(concurrentWrites))
              .setThreadName(threadName)
              .setThreadId(threadId)
              .setMarkListId(handle.markHolderId)
              .build());
    }
    return Collections.unmodifiableList(markLists);
  }

  static void startAnyway(long gen, String taskName, @Nullable String tagName, long tagId) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.start(gen, taskName, tagName, tagId, System.nanoTime());
    localMarkHolder.release(mh);
  }

  static void startAnyway(long gen, String taskName) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.start(gen, taskName, System.nanoTime());
    localMarkHolder.release(mh);
  }

  static void startAnyway(long gen, String taskName, String subTaskName) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.start(gen, taskName, subTaskName, System.nanoTime());
    localMarkHolder.release(mh);
  }

  static void stopAnyway(long gen) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.stop(gen, nanoTime);
    localMarkHolder.release(mh);
  }

  static void stopAnyway(long gen, String taskName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.stop(gen, taskName, tagName, tagId, nanoTime);
    localMarkHolder.release(mh);
  }

  static void stopAnyway(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.stop(gen, taskName, nanoTime);
    localMarkHolder.release(mh);
  }

  static void stopAnyway(long gen, String taskName, String subTaskName) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.stop(gen, taskName, subTaskName, nanoTime);
    localMarkHolder.release(mh);
  }

  static void eventAnyway(long gen, String eventName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.event(gen, eventName, tagName, tagId, nanoTime);
    localMarkHolder.release(mh);
  }

  static void eventAnyway(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.event(gen, eventName, nanoTime);
    localMarkHolder.release(mh);
  }

  static void eventAnyway(long gen, String eventName, String subEventName) {
    long nanoTime = System.nanoTime();
    MarkHolder mh = localMarkHolder.acquire();
    mh.event(gen, eventName, subEventName, nanoTime);
    localMarkHolder.release(mh);
  }

  static void linkAnyway(long gen, long linkId) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.link(gen, linkId);
    localMarkHolder.release(mh);
  }

  static void attachTagAnyway(long gen, @Nullable String tagName, long tagId) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.attachTag(gen, tagName, tagId);
    localMarkHolder.release(mh);
  }

  static void attachKeyedTagAnyway(long gen, @Nullable String tagName, String tagValue) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.attachKeyedTag(gen, tagName, tagValue);
    localMarkHolder.release(mh);
  }

  static void attachKeyedTagAnyway(long gen, @Nullable String tagName, long tagValue) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.attachKeyedTag(gen, tagName, tagValue);
    localMarkHolder.release(mh);
  }

  static void attachKeyedTagAnyway(
      long gen, @Nullable String tagName, long tagValue0, long tagValue1) {
    MarkHolder mh = localMarkHolder.acquire();
    mh.attachKeyedTag(gen, tagName, tagValue0, tagValue1);
    localMarkHolder.release(mh);
  }

  /**
   * Removes all data for the calling Thread.  Other threads may Still have stored data.
   */
  public static void clearLocalStorage() {
    for (Iterator<MarkHolderHandle> it = allMarkHolders.values().iterator(); it.hasNext();)  {
      MarkHolderHandle handle = it.next();
      if (handle.threadRef.get() == Thread.currentThread()) {
        it.remove();
        handle.threadRef.clearInternal();
        handle.softenMarkHolderReference();
        handle.clearSoftReference();
      }
    }
    localMarkHolder.clear();
  }

  /**
   * Removes the global Read index on all storage, but leaves local storage in place.  Because writer threads may still
   * be writing to the same buffer (which they have a strong ref to), this function only removed data that is truly
   * unwritable anymore.   In addition, it captures a timestamp to which marks to include when reading.  Thus, the data
   * isn't fully removed.  To fully remove all data, each tracing thread must call {@link #clearLocalStorage}.
   */
  public static void clearGlobalIndex() {
    lastGlobalIndexClear = System.nanoTime() - 1;
    for (Iterator<MarkHolderHandle> it = allMarkHolders.values().iterator(); it.hasNext();) {
      MarkHolderHandle handle = it.next();
      handle.softenMarkHolderReference();
      Thread writer = handle.threadRef().get();
      if (writer == null) {
        it.remove();
        handle.clearSoftReference();
      }
    }
  }

  private static void drainThreadQueue() {
    while (true) {
      Reference<?> ref = threadReferenceQueue.poll();
      if (ref == null) {
        return;
      }
      MarkHolderHandle handle = allMarkHolders.get(ref);
      if (handle != null) {
        handle.softenMarkHolderReference();
        if (handle.markHolder() == null) {
          allMarkHolders.remove(ref);
          handle.clearSoftReference();
        }
      }
    }
  }

  @Nullable
  public static MarkList readForTest() {
    List<MarkList> lists = read();
    for (MarkList list : lists) {
      // This is slightly wrong as the thread ID could be reused.
      if (list.getThreadId() == Thread.currentThread().getId()) {
        return list;
      }
    }
    return null;
  }

  public static final class MarkHolderHandle {
    private static final SoftReference<MarkHolder> EMPTY = new SoftReference<MarkHolder>(null);

    private final UnmodifiableWeakReference<Thread> threadRef;
    private final AtomicReference<MarkHolder> markHolderRef;
    private volatile SoftReference<MarkHolder> softMarkHolderRef;

    private volatile String threadName;
    private volatile long threadId;
    private final long markHolderId;

    MarkHolderHandle(Thread thread, MarkHolder markHolder, long markHolderId) {
      this.threadRef = new UnmodifiableWeakReference<Thread>(thread, threadReferenceQueue);
      this.markHolderRef = new AtomicReference<MarkHolder>(markHolder);
      this.threadName = thread.getName();
      this.threadId = thread.getId();
      this.markHolderId = markHolderId;
    }

    /**
     * Returns the MarkHolder.  May return {@code null} if the Thread is gone.  If {@code null} is returned,
     * then {@code getThreadRef().get() == null}.  If a non-{@code null} value is returned, the thread may be dead or
     * alive.  Additionally, since the {@link #threadRef} may be externally cleared, it is not certain that the Thread
     * is dead.
     */
    public MarkHolder markHolder() {
      MarkHolder markHolder = markHolderRef.get();
      if (markHolder == null) {
        markHolder = softMarkHolderRef.get();
        assert markHolder != null || threadRef.get() == null;
      }
      return markHolder;
    }

    /**
     * Returns a weak reference to the Thread that created the MarkHolder.
     */
    public WeakReference<? extends Thread> threadRef() {
      return threadRef;
    }

    void softenMarkHolderReference() {
      synchronized (markHolderRef) {
        MarkHolder markHolder = markHolderRef.get();
        if (markHolder != null) {
          softMarkHolderRef = new SoftReference<MarkHolder>(markHolder);
          markHolderRef.set(null);
        }
      }
    }

    void clearSoftReference() {
      Thread thread = threadRef.get();
      if (thread != null) {
        throw new IllegalStateException("Thread still alive " + thread);
      }
      synchronized (markHolderRef) {
        MarkHolder markHolder = markHolderRef.get();
        if (markHolder != null) {
          throw new IllegalStateException("Handle not yet softened");
        }
        softMarkHolderRef.clear();
        softMarkHolderRef = EMPTY;
        threadName = null;
        threadId = -255;
      }
    }

    String getAndUpdateThreadName() {
      Thread t = threadRef.get();
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
    long getAndUpdateThreadId() {
      Thread t = threadRef.get();
      long id;
      if (t != null) {
        threadId = (id = t.getId());
      } else {
        id = threadId;
      }
      return id;
    }
  }

  /**
   * This class is needed to work around a race condition where a newly created MarkHolder could be GC'd before
   * the caller of {@link #allocateMarkHolder} can consume the results.  The provided MarkHolder is strongly reachable.
   */
  public static final class MarkHolderAndHandle {
    private final MarkHolder markHolder;
    private final MarkHolderHandle handle;

    MarkHolderAndHandle(MarkHolder markHolder, MarkHolderHandle handle) {
      this.markHolder = markHolder;
      this.handle = handle;

      MarkHolder tmp = handle.markHolder();
      if (markHolder != tmp && tmp != null) {
        throw new IllegalArgumentException("Holder Handle mismatch");
      }
    }

    public MarkHolder markHolder() {
      return markHolder;
    }

    public MarkHolderHandle handle() {
      return handle;
    }
  }

  public static MarkHolderAndHandle allocateMarkHolder() {
    drainThreadQueue();
    long markHolderId = markHolderIdAllocator.getAndIncrement();
    MarkHolder holder = markHolderProvider.create(markHolderId);
    MarkHolderHandle handle = new MarkHolderHandle(Thread.currentThread(), holder, markHolderId);
    allMarkHolders.put(handle.threadRef, handle);
    return new MarkHolderAndHandle(holder, handle);
  }

  private static final class UnmodifiableWeakReference<T> extends WeakReference<T> {

    UnmodifiableWeakReference(T referent, ReferenceQueue<T> q) {
      super(referent, q);
    }

    @Override
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public void clear() {}

    @Override
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public boolean enqueue() {
      return false;
    }

    void clearInternal() {
      super.clear();
    }
  }

  private Storage() {}
}
