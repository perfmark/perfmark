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
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Storage is responsible for storing and returning recorded marks. This is a low level class and
 * not intended for use by users. Instead, the {@code TraceEventWriter} and {@code TraceEventViewer}
 * classes provide easier to use APIs for accessing PerfMark data.
 *
 * <p>This code is <strong>NOT</strong> API stable, and may be removed in the future, or changed
 * without notice.
 */
public final class Storage {
  /*
   * Invariants:
   * <ul>
   *   <li>A Thread may have at most one MarkRecorder at a time.</li>
   *   <li>If a MarkHolder is detached from its Thread, it cannot be written to again.</li>
   * </ul>
   */
  static final String MARK_RECORDER_PROVIDER_PROP = "io.perfmark.PerfMark.markRecorderProvider";
  static final AtomicLong markRecorderIdAllocator = new AtomicLong(1);
  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.

  private static final ConcurrentMap<Object, Reference<MarkHolder>> allMarkHolders =
      new ConcurrentHashMap<Object, Reference<MarkHolder>>();
  private static final MarkRecorderProvider markRecorderProvider;
  private static final LocalMarkRecorder localMarkRecorder = new MostlyThreadLocalMarkRecorder();
  private static volatile long lastGlobalIndexClear = Generator.INIT_NANO_TIME - 1;

  static {
    MarkRecorderProvider provider = null;
    Throwable[] problems = new Throwable[3];
    try {
      String markRecorderOverride = System.getProperty(MARK_RECORDER_PROVIDER_PROP);
      if (markRecorderOverride != null && !markRecorderOverride.isEmpty()) {
        Class<?> clz = Class.forName(markRecorderOverride);
        provider = clz.asSubclass(MarkRecorderProvider.class).getConstructor().newInstance();
      }
    } catch (Throwable t) {
      problems[0] = t;
    }
    if (provider == null) {
      try {
        Class<?> clz =
            Class.forName(
                "io.perfmark.java9.SecretVarHandleMarkRecorderProvider$VarHandleMarkRecorderProvider");
        provider = clz.asSubclass(MarkRecorderProvider.class).getConstructor().newInstance();
      } catch (Throwable t) {
        problems[1] = t;
      }
    }
    if (provider == null) {
      try {
        Class<?> clz =
            Class.forName(
                "io.perfmark.java6.SecretSynchronizedMarkRecorderProvider$SynchronizedMarkRecorderProvider");
        provider = clz.asSubclass(MarkRecorderProvider.class).getConstructor().newInstance();
      } catch (Throwable t) {
        problems[2] = t;
      }
    }
    if (provider == null) {
      markRecorderProvider = new NoopMarkRecorderProvider();
    } else {
      markRecorderProvider = provider;
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
          logProblemMethod.invoke(logger, level, "Error loading MarkRecorderProvider", problem);
        }
        Method logSuccessMethod = logClass.getMethod("log", levelClass, String.class, Object[].class);
        logSuccessMethod.invoke(logger, level, "Using {0}", new Object[] {markRecorderProvider.getClass().getName()});
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  public static long getInitNanoTime() {
    return Generator.INIT_NANO_TIME;
  }

  /**
   * Returns a list of {@link MarkList}s across all reachable threads.  MarkLists with no Marks may be removed.
   *
   * @return all reachable MarkLists.
   */
  public static List<MarkList> read() {
    long lastReset = lastGlobalIndexClear;
    List<MarkList> markLists = new ArrayList<MarkList>();
    for (Iterator<Reference<MarkHolder>> it = allMarkHolders.values().iterator(); it.hasNext();) {
      Reference<MarkHolder> ref = it.next();
      MarkHolder markHolder = ref.get();
      if (markHolder == null) {
        it.remove();
        continue;
      }
      markHolder.read(markLists);
    }
    // Avoid doing this in the upper loop to avoid tearing the reads too much.
    Set<Long> markRecorderIds = new HashSet<Long>(markLists.size());
    for (MarkList list : markLists) {
      if (!markRecorderIds.add(list.getMarkRecorderId())) {
        throw new IllegalStateException("Duplicate MarkRecorder IDs in MarkHolders " + list.getMarkRecorderId());
      }
    }
    return Collections.unmodifiableList(markLists);
  }

  static void startAnyway(long gen, String taskName, String tagName, long tagId) {
    localMarkRecorder.get().start(gen, taskName, tagName, tagId, System.nanoTime());
  }

  static void startAnyway(long gen, String taskName) {
    localMarkRecorder.get().start(gen, taskName, System.nanoTime());
  }

  static void startAnyway(long gen, String taskName, String subTaskName) {
    localMarkRecorder.get().start(gen, taskName, subTaskName, System.nanoTime());
  }

  static void stopAnyway(long gen) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, nanoTime);
  }

  static void stopAnyway(long gen, String taskName, String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, taskName, tagName, tagId, nanoTime);
  }

  static void stopAnyway(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, taskName, nanoTime);
  }

  static void stopAnyway(long gen, String taskName, String subTaskName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, taskName, subTaskName, nanoTime);
  }

  static void eventAnyway(long gen, String eventName, String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().event(gen, eventName, tagName, tagId, nanoTime);
  }

  static void eventAnyway(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().event(gen, eventName, nanoTime);
  }

  static void eventAnyway(long gen, String eventName, String subEventName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().event(gen, eventName, subEventName, nanoTime);
  }

  static void linkAnyway(long gen, long linkId) {
    localMarkRecorder.get().link(gen, linkId);
  }

  static void attachTagAnyway(long gen, String tagName, long tagId) {
    localMarkRecorder.get().attachTag(gen, tagName, tagId);
  }

  static void attachKeyedTagAnyway(long gen, String tagName, String tagValue) {
    localMarkRecorder.get().attachKeyedTag(gen, tagName, tagValue);
  }

  static void attachKeyedTagAnyway(long gen, String tagName, long tagValue) {
    localMarkRecorder.get().attachKeyedTag(gen, tagName, tagValue);
  }

  static void attachKeyedTagAnyway(
      long gen, String tagName, long tagValue0, long tagValue1) {
    localMarkRecorder.get().attachKeyedTag(gen, tagName, tagValue0, tagValue1);
  }

  /**
   * Removes all data for the calling Thread.  Other threads may Still have stored data.
   */
  public static void resetForThread() {
    for (Iterator<Reference<MarkHolder>> it = allMarkHolders.values().iterator(); it.hasNext();) {
      Reference<MarkHolder> ref = it.next();
      MarkHolder holder = ref.get();
      if (holder == null) {
        it.remove();
        continue;
      }
      holder.resetForThread();
    }
  }

  /**
   * Removes the global Read index on all storage, but leaves local storage in place.  Because writer threads may still
   * be writing to the same buffer (which they have a strong ref to), this function only removed data that is truly
   * unwritable anymore.   In addition, it captures a timestamp to which marks to include when reading.  Thus, the data
   * isn't fully removed.  To fully remove all data, each tracing thread must call {@link #resetForThread}.
   */
  public static void resetForAll() {
    lastGlobalIndexClear = System.nanoTime() - 1;
    for (Iterator<Map.Entry<Object, Reference<MarkHolder>>> it = allMarkHolders.entrySet().iterator(); it.hasNext();) {
      Map.Entry<Object, Reference<MarkHolder>> entry = it.next();
      Reference<MarkHolder> ref = entry.getValue();
      MarkHolder holder = ref.get();
      if (holder == null) {
        it.remove();
        continue;
      }
      entry.setValue(new WeakReference<MarkHolder>(holder));
      holder.resetForAll();
    }
  }

  /**
   * Note: it is the responsibility of the caller to keep a strong reference to the markHolder.
   */
  public static void registerMarkHolder(MarkHolder markHolder) {
    if (markHolder == null) {
      throw new NullPointerException("markHolder");
    }
    allMarkHolders.put(new Object(), new SoftReference<MarkHolder>(markHolder));
  }

  /**
   * This method is meant to aid in cleanup.  It is not efficient so don't use it in production.
   */
  public static void unregisterMarkHolder(MarkHolder markHolder) {
    if (markHolder == null) {
      throw new NullPointerException("markHolder");
    }
    for (Iterator<Reference<MarkHolder>> it = allMarkHolders.values().iterator(); it.hasNext();) {
      Reference<MarkHolder> ref = it.next();
      MarkHolder holder = ref.get();
      if (holder == null) {
        it.remove();
        continue;
      }
      if (holder == markHolder) {
        it.remove();
        break;
      }
    }
  }

  /**
   * May Return {@code null}.
   */
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

  public static MarkRecorder allocateMarkRecorder() {
    long markRecorderId = markRecorderIdAllocator.getAndIncrement();
    return markRecorderProvider.createMarkRecorder(markRecorderId);
  }

  private Storage() {}
}
