package io.perfmark.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class Storage {
  private static final long INIT_NANO_TIME = System.nanoTime();

  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.
  static final ConcurrentMap<MarkHolderRef, Reference<? extends Thread>> allMarkHolders =
      new ConcurrentHashMap<MarkHolderRef, Reference<? extends Thread>>();
  private static final ThreadLocal<MarkHolder> localMarkHolder = new MarkHolderThreadLocal();
  static final MarkHolderProvider markHolderProvider;
  private static final Logger logger;

  static {
    List<MarkHolderProvider> providers = new ArrayList<MarkHolderProvider>();
    List<Throwable> fines = new ArrayList<Throwable>();
    List<Throwable> warnings = new ArrayList<Throwable>();
    Class<?> clz = null;
    try {
      clz =
          Class.forName(
              "io.perfmark.java9.SecretVarHandleMarkHolderProvider$VarHandleMarkHolderProvider");
    } catch (ClassNotFoundException e) {
      fines.add(e);
    } catch (Throwable t) {
      warnings.add(t);
    }
    if (clz != null) {
      try {
        providers.add(clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance());
      } catch (Throwable t) {
        warnings.add(t);
      }
      clz = null;
    }
    try {
      clz =
          Class.forName(
              "io.perfmark.java6.SecretSynchronizedMarkHolderProvider$SynchronizedMarkHolderProvider");
    } catch (ClassNotFoundException e) {
      fines.add(e);
    } catch (Throwable t) {
      warnings.add(t);
    }
    if (clz != null) {
      try {
        providers.add(clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance());
      } catch (Throwable t) {
        warnings.add(t);
      }
      clz = null;
    }

    if (!providers.isEmpty()) {
      markHolderProvider = providers.get(0);
    } else {
      markHolderProvider = new NoopMarkHolderProvider();
    }

    logger = Logger.getLogger(Storage.class.getName());

    for (Throwable error : warnings) {
      logger.log(Level.WARNING, "Error loading MarkHolderProvider", error);
    }
    for (Throwable error : fines) {
      logger.log(Level.FINE, "Error loading MarkHolderProvider", error);
    }
  }

  public static long getInitNanoTime() {
    return INIT_NANO_TIME;
  }

  /** Returns a list of {@link MarkList}s across all reachable threads. */
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
    long noThreadIds = MarkList.NO_THREAD_ID;
    for (int i = 0; i < markHolders.size(); i++) {
      final long threadId;
      final String threadName;
      @Nullable Thread writer = threads.get(i);
      if (writer == null) {
        threadId = noThreadIds--;
        threadName = MarkList.NO_THREAD_NAME;
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

  static void startAnyways(long gen, String taskName, @Nullable String tagName, long tagId) {
    localMarkHolder.get().start(gen, taskName, tagName, tagId, System.nanoTime());
  }

  static void startAnyways(
      long gen, String taskName, Marker marker, @Nullable String tagName, long tagId) {
    localMarkHolder.get().start(gen, taskName, marker, tagName, tagId, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName) {
    localMarkHolder.get().start(gen, taskName, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName, Marker marker) {
    localMarkHolder.get().start(gen, taskName, marker, System.nanoTime());
  }

  static void stopAnyways(long gen, String taskName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, tagName, tagId, nanoTime);
  }

  static void stopAnyways(
      long gen, String taskName, Marker marker, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, marker, tagName, tagId, nanoTime);
  }

  static void stopAnyways(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, nanoTime);
  }

  static void stopAnyways(long gen, String taskName, Marker marker) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, marker, nanoTime);
  }

  static void eventAnyways(long gen, String eventName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, tagName, tagId, nanoTime, 0);
  }

  static void eventAnyways(
      long gen, String taskName, Marker marker, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, taskName, marker, tagName, tagId, nanoTime, 0);
  }

  static void eventAnyways(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, nanoTime, 0);
  }

  static void eventAnyways(long gen, String taskName, Marker marker) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, taskName, marker, nanoTime, 0);
  }

  static void linkAnyways(long gen, long linkId) {
    localMarkHolder.get().link(gen, linkId);
  }

  static void linkAnyways(long gen, long linkId, Marker marker) {
    localMarkHolder.get().link(gen, linkId, marker);
  }

  public static void resetForTest() {
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

    static void cleanQueue(Map<?, ?> allSpans) {
      Reference<?> ref;
      while ((ref = markHolderRefQueue.poll()) != null) {
        ref.clear();
        allSpans.remove(ref);
      }
    }
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

  private Storage() {
    throw new AssertionError("nope");
  }
}
