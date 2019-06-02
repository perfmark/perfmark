package io.perfmark;

import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Marker;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Stores PerfMark data across threads.   This class maintains the index to {@link MarkHolder}s
 * for reading.
 */
public final class PerfMarkStorage {
  private static final long initNanoTime = System.nanoTime();
  private static final List<String> FALLBACK_MARK_HOLDERS =
      Collections.unmodifiableList(Arrays.asList(
          "io.perfmark.java9.SecretVarHandleMarkHolderProvider$VarHandleMarkHolderProvider",
          "io.perfmark.java6.SecretSynchronizedMarkHolderProvider$SynchronizedMarkHolderProvider"));
  static final long NO_THREAD_ID = -1;
  static final String NO_THREAD_NAME = "(unknownThread)";
  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.
  static final ConcurrentMap<MarkHolderRef, Reference<? extends Thread>> allMarkHolders =
      new ConcurrentHashMap<MarkHolderRef, Reference<? extends Thread>>();
  private static final ThreadLocal<MarkHolder> localMarkHolder = new MarkHolderThreadLocal();
  static final MarkHolderProvider markHolderProvider;
  private static final Logger logger;

  static {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<MarkHolderProvider> markHolders =
        getLoadable(
            errors,
            MarkHolderProvider.class,
            FALLBACK_MARK_HOLDERS,
            PerfMarkStorage.class.getClassLoader());
    Level level;
    if (markHolders.isEmpty()) {
      markHolderProvider = new NoopMarkHolderProvider();
      level = Level.WARNING;
    } else {
      markHolderProvider = markHolders.get(0);
      level = Level.FINE;
    }
    logger = Logger.getLogger(PerfMarkStorage.class.getName());
    logger.log(level, "Using {0}", new Object[] {markHolderProvider.getClass()});
    for (Throwable error : errors) {
      logger.log(level, "Error encountered loading mark holder", error);
    }
  }

  public static long getInitNanoTime() {
    return initNanoTime;
  }

  /**
   * Returns a list of {@link MarkList}s across all reachable threads.
   */
  public static List<MarkList> read() {
    MarkHolderRef.cleanQueue(allMarkHolders);
    List<Thread> threads = new ArrayList<Thread>();
    List<Long> markHolderIds = new ArrayList<Long>();
    List<MarkHolder> markHolders = new ArrayList<MarkHolder>();
    // Capture a snapshot of the index with as little skew as possible.  Don't pre-size the lists
    // since it would mean scanning allMarkHolders twice.  Instead, try to get a strong ref to each
    // of the MarkHolders before they could get GC'd.
    for (Map.Entry<MarkHolderRef, Reference<? extends Thread>> entry : allMarkHolders.entrySet()) {
      MarkHolder mh = entry.getKey().get();
      if (mh == null) {
        continue;
      }
      @Nullable Thread writer = entry.getValue().get();
      markHolders.add(mh);
      markHolderIds.add(entry.getKey().markHolderId);
      threads.add(writer);
    }
    assert markHolders.size() == threads.size();
    List<MarkList> markLists = new ArrayList<MarkList>(markHolders.size());
    long noThreadIds = NO_THREAD_ID;
    for (int i = 0; i < markHolders.size(); i++) {
      final long threadId;
      final String threadName;
      @Nullable Thread writer = threads.get(i);
      if (writer == null) {
        threadId = noThreadIds--;
        threadName = NO_THREAD_NAME;
      } else {
        threadId = writer.getId();
        threadName = writer.getName();
      }
      boolean readerIsWriter = Thread.currentThread() == writer;
      markLists.add(
          MarkList.newBuilder()
              .setMarks(markHolders.get(i).read(readerIsWriter))
              .setThreadName(threadName)
              .setThreadId(threadId)
              .setMarkListId(markHolderIds.get(i))
              .build());
    }
    return Collections.unmodifiableList(markLists);
  }

  static void startAnyways(long gen, String taskName, Tag tag) {
    localMarkHolder.get().start(gen, taskName, tag.tagName, tag.tagId, System.nanoTime());
  }

  static void startAnyways(long gen, Marker marker, Tag tag) {
    localMarkHolder.get().start(gen, marker, tag.tagName, tag.tagId, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName) {
    localMarkHolder.get().start(gen, taskName, System.nanoTime());
  }

  static void startAnyways(long gen, Marker marker) {
    localMarkHolder.get().start(gen, marker, System.nanoTime());
  }

  static void stopAnyways(long gen, String taskName, Tag tag) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, tag.tagName, tag.tagId, nanoTime);
  }

  static void stopAnyways(long gen, Marker marker, Tag tag) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, marker, tag.tagName, tag.tagId, nanoTime);
  }

  static void stopAnyways(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, nanoTime);
  }

  static void stopAnyways(long gen, Marker marker) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, marker, nanoTime);
  }

  static void eventAnyways(long gen, String eventName, Tag tag) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, tag.tagName, tag.tagId, nanoTime, 0);
  }

  static void eventAnyways(long gen, Marker marker, Tag tag) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, marker, tag.tagName, tag.tagId, nanoTime, 0);
  }

  static void eventAnyways(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, nanoTime, 0);
  }

  static void eventAnyways(long gen, Marker marker) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, marker, nanoTime, 0);
  }

  static void linkAnyways(long gen, long linkId, Marker marker) {
    localMarkHolder.get().link(gen, linkId, marker);
  }

  static void resetForTest() {
    localMarkHolder.get().resetForTest();
  }

  private static final class MarkHolderThreadLocal extends ThreadLocal<MarkHolder> {

    MarkHolderThreadLocal() {}

    @Override
    protected MarkHolder initialValue() {
      MarkHolderRef.cleanQueue(allMarkHolders);
      MarkHolder holder = markHolderProvider.create();
      MarkHolderRef ref = new MarkHolderRef(holder);
      Reference<Thread> writer = new WeakReference<Thread>(Thread.currentThread());
      allMarkHolders.put(ref, writer);
      return holder;
    }
  }

  private static final class MarkHolderRef extends WeakReference<MarkHolder> {
    private static final ReferenceQueue<MarkHolder> markHolderRefQueue =
        new ReferenceQueue<MarkHolder>();
    private static final AtomicLong markHolderIdAllocator = new AtomicLong();

    final long markHolderId = markHolderIdAllocator.incrementAndGet();

    MarkHolderRef(MarkHolder holder) {
      super(holder, markHolderRefQueue);
    }

    static void cleanQueue(Map<MarkHolderRef, ?> allSpans) {
      Reference<?> ref;
      while ((ref = markHolderRefQueue.poll()) != null) {
        ref.clear();
        allSpans.remove(ref);
      }
    }
  }

  static <T> List<T> getLoadable(
      List<? super Throwable> errors,
      Class<T> clz,
      List<? extends String> fallbackClassNames,
      ClassLoader cl) {
    Map<Class<? extends T>, T> loadables = new LinkedHashMap<Class<? extends T>, T>();
    List<Throwable> serviceLoaderErrors = new ArrayList<Throwable>();
    try {
      ServiceLoader<T> loader = ServiceLoader.load(clz, cl);
      Iterator<T> it = loader.iterator();
      for (int i = 0; i < 10 && serviceLoaderErrors.size() < 10; i++) {
        try {
          if (it.hasNext()) {
            T next = it.next();
            Class<? extends T> subClz = next.getClass().asSubclass(clz);
            if (!loadables.containsKey(subClz)) {
              loadables.put(subClz, next);
            }
          } else {
            break;
          }
        } catch (ServiceConfigurationError sce) {
          if (!serviceLoaderErrors.isEmpty()) {
            Throwable last = serviceLoaderErrors.get(serviceLoaderErrors.size() - 1);
            if (errorsEqual(sce, last)) {
              continue;
            }
          }
          serviceLoaderErrors.add(sce);
        }
      }
    } catch (ServiceConfigurationError sce) {
      serviceLoaderErrors.add(sce);
    } finally {
      errors.addAll(serviceLoaderErrors);
    }
    for (String fallbackClassName : fallbackClassNames) {
      try {
        Class<?> fallbackClz = Class.forName(fallbackClassName, true, cl);
        if (!loadables.containsKey(fallbackClz)) {
          Class<? extends T> subClz = fallbackClz.asSubclass(clz);
          loadables.put(subClz, subClz.getDeclaredConstructor().newInstance());
        }
      } catch (Throwable t) {
        errors.add(t);
      }
    }
    return Collections.unmodifiableList(new ArrayList<T>(loadables.values()));
  }

  @SuppressWarnings("ReferenceEquality") // No Java 8 yet
  private static boolean errorsEqual(Throwable t1, Throwable t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }
    if (t1.getClass() == t2.getClass()) {
      String msg1 = t1.getMessage();
      String msg2 = t2.getMessage();
      if (msg1 == msg2 || (msg1 != null && msg1.equals(msg2))) {
        if (Arrays.equals(t1.getStackTrace(), t2.getStackTrace())) {
          return errorsEqual(t1.getCause(), t2.getCause());
        }
      }
    }
    return false;
  }

  private PerfMarkStorage() {
    throw new AssertionError("nope");
  }
}
