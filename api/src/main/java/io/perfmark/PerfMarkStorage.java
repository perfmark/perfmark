package io.perfmark;

import io.perfmark.impl.Mark;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PerfMarkStorage {
  private static final List<String> FALLBACK_MARK_HOLDERS =
      Collections.unmodifiableList(Arrays.asList(
          "io.perfmark.java9.SecretVarHandleMarkHolderProvider$VarHandleMarkHolderProvider"));
  static final long NO_THREAD_ID = -1;
  static final String NO_THREAD_NAME = "(unknownThread)";
  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.
  static final ConcurrentMap<MarkHolderRef, Reference<? extends Thread>> allMarkHolders =
      new ConcurrentHashMap<MarkHolderRef, Reference<? extends Thread>>();
  private static final ThreadLocal<MarkHolder> localMarkHolder = new MarkHolderThreadLocal();
  private static final MarkHolderProvider markHolderProvider;
  private static final Logger logger;

  static {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<MarkHolderProvider> markHolders =
        PerfMark.getLoadable(
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

  public static List<MarkList> read() {
    MarkHolderRef.cleanQueue(allMarkHolders);
    List<MarkList> markLists = new ArrayList<MarkList>(allMarkHolders.size());
    for (Map.Entry<MarkHolderRef, Reference<? extends Thread>> entry : allMarkHolders.entrySet()) {
      MarkHolder mh = entry.getKey().get();
      if (mh == null) {
        continue;
      }
      Thread writer = entry.getValue().get();
      final long threadId;
      final String threadName;
      if (writer == null) {
        threadId = NO_THREAD_ID;
        threadName = NO_THREAD_NAME;
      } else {
        threadId = writer.getId();
        threadName = writer.getName();
      }
      boolean readerIsWriter = Thread.currentThread() == writer;
      markLists.add(
          MarkList.create(mh.read(readerIsWriter), Mark.NO_NANOTIME, threadName, threadId));
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

  private PerfMarkStorage() {
    throw new AssertionError("nope");
  }
}
