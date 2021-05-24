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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.
  static final ConcurrentMap<MarkHolderTuple, Boolean> allMarkHolders =
      new ConcurrentHashMap<MarkHolderTuple, Boolean>();
  private static final ThreadLocal<MarkHolder> localMarkHolder = new MarkHolderThreadLocal();
  static final MarkHolderProvider markHolderProvider;

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
        Logger localLogger = Logger.getLogger(Storage.class.getName());
        for (Throwable problem : problems) {
          if (problem == null) {
            continue;
          }
          localLogger.log(Level.FINE, "Error loading MarkHolderProvider", problem);
        }
        localLogger.log(Level.FINE, "Using {0}", new Object[] {markHolderProvider.getClass().getName()});
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
    List<MarkList> markLists = new ArrayList<MarkList>(allMarkHolders.size());
    for (MarkHolderTuple tuple : allMarkHolders.keySet()) {
      String threadName = tuple.getAndUpdateThreadName();
      MarkHolder mh = tuple.markHolderRef.get();
      if (mh == null) {
        tuple.clean();
        allMarkHolders.remove(tuple);
        continue;
      }
      Thread writer = tuple.threadRef.get();
      boolean concurrentWrites = !(Thread.currentThread() == writer || writer == null);
      markLists.add(
          MarkList.newBuilder()
              .setMarks(mh.read(concurrentWrites))
              .setThreadName(threadName)
              .setThreadId(tuple.threadId)
              .setMarkListId(tuple.markHolderId)
              .build());
    }
    return Collections.unmodifiableList(markLists);
  }

  static void startAnyways(long gen, String taskName, @Nullable String tagName, long tagId) {
    localMarkHolder.get().start(gen, taskName, tagName, tagId, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName) {
    localMarkHolder.get().start(gen, taskName, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName, String subTaskName) {
    localMarkHolder.get().start(gen, taskName, subTaskName, System.nanoTime());
  }

  static void stopAnyways(long gen) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, nanoTime);
  }

  static void stopAnyways(long gen, String taskName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, tagName, tagId, nanoTime);
  }

  static void stopAnyways(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, nanoTime);
  }

  static void stopAnyways(long gen, String taskName, String subTaskName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, subTaskName, nanoTime);
  }

  static void eventAnyways(long gen, String eventName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, tagName, tagId, nanoTime);
  }

  static void eventAnyways(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, nanoTime);
  }

  static void eventAnyways(long gen, String eventName, String subEventName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, subEventName, nanoTime);
  }

  static void linkAnyways(long gen, long linkId) {
    localMarkHolder.get().link(gen, linkId);
  }

  static void attachTagAnyways(long gen, @Nullable String tagName, long tagId) {
    localMarkHolder.get().attachTag(gen, tagName, tagId);
  }

  static void attachKeyedTagAnyways(long gen, @Nullable String tagName, String tagValue) {
    localMarkHolder.get().attachKeyedTag(gen, tagName, tagValue);
  }

  static void attachKeyedTagAnyways(long gen, @Nullable String tagName, long tagValue) {
    localMarkHolder.get().attachKeyedTag(gen, tagName, tagValue);
  }

  static void attachKeyedTagAnyways(
      long gen, @Nullable String tagName, long tagValue0, long tagValue1) {
    localMarkHolder.get().attachKeyedTag(gen, tagName, tagValue0, tagValue1);
  }

  public static void resetForTest() {
    localMarkHolder.remove();
    allMarkHolders.clear();
  }

  static void clearSoftRefsForTest() {
    for (MarkHolderTuple tuple : allMarkHolders.keySet()) {
      tuple.markHolderRef.enqueue();
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

  private static final class MarkHolderThreadLocal extends ThreadLocal<MarkHolder> {

    MarkHolderThreadLocal() {}

    @Override
    protected MarkHolder initialValue() {
      MarkHolder holder = markHolderProvider.create();
      MarkHolderTuple ref = new MarkHolderTuple(Thread.currentThread(), holder);
      allMarkHolders.put(ref, Boolean.TRUE);
      return holder;
    }
  }

  private static final class MarkHolderTuple {
    private static final AtomicLong markHolderIdAllocator = new AtomicLong();

    final Reference<Thread> threadRef;
    final Reference<MarkHolder> markHolderRef;
    final AtomicReference<String> threadName;
    final long threadId;
    final long markHolderId;

    MarkHolderTuple(Thread thread, MarkHolder holder) {
      this.threadRef = new WeakReference<Thread>(thread);
      this.markHolderRef = new SoftReference<MarkHolder>(holder);
      this.threadName = new AtomicReference<String>(thread.getName());
      this.threadId = thread.getId();
      this.markHolderId = markHolderIdAllocator.incrementAndGet();
    }

    String getAndUpdateThreadName() {
      Thread t = threadRef.get();
      String name;
      if (t != null) {
        threadName.lazySet(name = t.getName());
      } else {
        name = threadName.get();
      }
      return name;
    }

    void clean() {
      threadRef.enqueue();
      markHolderRef.enqueue();
      threadName.set(null);
    }
  }

  private Storage() {}
}
