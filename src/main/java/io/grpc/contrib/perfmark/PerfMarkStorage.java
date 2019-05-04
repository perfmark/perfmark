package io.grpc.contrib.perfmark;


import io.grpc.contrib.perfmark.MarkList.Mark.Operation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

/**
 * PerfMark storage is where perfmark events are stored.  It is a thread local repository of the
 * last N events.
 */
final class PerfMarkStorage {

  static void startAnyways(long gen, String taskName, Tag tag) {
    SpanHolder.current().start(gen, taskName, tag.tagName, tag.tagId, System.nanoTime());
  }

  static void startAnyways(long gen, Marker marker, Tag tag) {
    SpanHolder.current().start(gen, marker, tag.tagName, tag.tagId, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName) {
    SpanHolder.current().start(gen, taskName, System.nanoTime());
  }

  static void startAnyways(long gen, Marker marker) {
    SpanHolder.current().start(gen, marker, System.nanoTime());
  }

  static void stopAnyways(long gen, String taskName, Tag tag) {
    long nanoTime = System.nanoTime();
    SpanHolder.current().stop(gen, taskName, tag.tagName, tag.tagId, nanoTime);
  }

  static void stopAnyways(long gen, Marker marker, Tag tag) {
    long nanoTime = System.nanoTime();
    SpanHolder.current().stop(gen, marker, tag.tagName, tag.tagId, nanoTime);
  }

  static void stopAnyways(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    SpanHolder.current().stop(gen, taskName, nanoTime);
  }

  static void stopAnyways(long gen, Marker marker) {
    long nanoTime = System.nanoTime();
    SpanHolder.current().stop(gen, marker, nanoTime);
  }

  static void linkAnyways(long gen, long linkId, Marker marker) {
    SpanHolder.current().link(gen, linkId, marker);
  }

  static void resetForTest() {
    SpanHolder.current().resetForTest();
  }

  static List<MarkList> read() {
    return SpanHolder.readAll();
  }

  static final class SpanHolder {
    private static final int MAX_EVENTS = 16384;
    private static final long MAX_EVENTS_MASK = MAX_EVENTS - 1;
    private static final long GEN_MASK = (1 << PerfMark.GEN_OFFSET) - 1;

    private static final long START_OP = Operation.TASK_START.ordinal();
    private static final long START_NOTAG_OP = Operation.TASK_NOTAG_START.ordinal();
    private static final long STOP_OP = Operation.TASK_END.ordinal();
    private static final long STOP_NOTAG_OP = Operation.TASK_NOTAG_END.ordinal();
    private static final long LINK_OP = Operation.LINK.ordinal();

    private static final ConcurrentMap<SpanHolderRef, SpanHolderRef> allSpans =
        new ConcurrentHashMap<>();
    private static final ThreadLocal<SpanHolder> localSpan = new ThreadLocal<SpanHolder>() {
      @Override
      protected SpanHolder initialValue() {
        return new SpanHolder();
      }
    };

    private static final VarHandle IDX;
    private static final VarHandle OBJECTS;
    private static final VarHandle STRINGS;
    private static final VarHandle LONGS;

    static {
      try {
        IDX = MethodHandles.lookup().findVarHandle(SpanHolder.class, "idx", long.class);
        OBJECTS = MethodHandles.arrayElementVarHandle(Object[].class);
        STRINGS = MethodHandles.arrayElementVarHandle(String[].class);
        LONGS = MethodHandles.arrayElementVarHandle(long[].class);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    @Nullable private final Thread writerThread;
    private final SpanHolderRef ref;
    private final long startTime;

    // where to write to next
    @SuppressWarnings("unused") // Used Reflectively
    private volatile long idx;
    private final Object[] taskNameOrMarkers = new Object[MAX_EVENTS];
    private final String[] tagNames = new String[MAX_EVENTS];
    private final long[] tagIds= new long[MAX_EVENTS];
    private final long[] nanoTimes = new long[MAX_EVENTS];
    private final long[] genOps = new long[MAX_EVENTS];

    SpanHolder() {
      this(Thread.currentThread());
    }

    SpanHolder(@Nullable Thread writerThread) {
      SpanHolderRef.cleanQueue(allSpans);
      this.writerThread = writerThread;
      this.ref = new SpanHolderRef(this);
      allSpans.put(ref, ref);
      this.startTime = System.nanoTime();
    }

    static SpanHolder current() {
      return localSpan.get();
    }

    void start(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      OBJECTS.setOpaque(taskNameOrMarkers, i, taskName);
      STRINGS.setOpaque(tagNames, i, tagName);
      LONGS.setOpaque(tagIds, i, tagId);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + START_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void start(long gen, Marker marker, String tagName, long tagId, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      OBJECTS.setOpaque(taskNameOrMarkers, i, marker);
      STRINGS.setOpaque(tagNames, i, tagName);
      LONGS.setOpaque(tagIds, i, tagId);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + START_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void start(long gen, String taskName, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      OBJECTS.setOpaque(taskNameOrMarkers, i, taskName);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + START_NOTAG_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void start(long gen, Marker marker, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      OBJECTS.setOpaque(taskNameOrMarkers, i, marker);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + START_NOTAG_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void link(long gen, long linkId, Marker marker) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      LONGS.setOpaque(tagIds, i, linkId);
      OBJECTS.setOpaque(taskNameOrMarkers, i, marker);
      LONGS.setOpaque(genOps, i, gen + LINK_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void stop(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      STRINGS.setOpaque(tagNames, i, tagName);
      LONGS.setOpaque(tagIds, i, tagId);
      OBJECTS.setOpaque(taskNameOrMarkers, i, taskName);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + STOP_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void stop(long gen, Marker marker, String tagName, long tagId, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      STRINGS.setOpaque(tagNames, i, tagName);
      LONGS.setOpaque(tagIds, i, tagId);
      OBJECTS.setOpaque(taskNameOrMarkers, i, marker);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + STOP_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void stop(long gen, String taskName, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      OBJECTS.setOpaque(taskNameOrMarkers, i, taskName);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + STOP_NOTAG_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void stop(long gen, Marker marker, long nanoTime) {
      long localIdx = (long) IDX.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      VarHandle.acquireFence();
      OBJECTS.setOpaque(taskNameOrMarkers, i, marker);
      LONGS.setOpaque(nanoTimes, i, nanoTime);
      LONGS.setOpaque(genOps, i, gen + STOP_NOTAG_OP);
      IDX.setRelease(this, localIdx + 1);
    }

    void resetForTest() {
      assert Thread.currentThread() == this.writerThread;
      Arrays.fill(taskNameOrMarkers, null);
      Arrays.fill(tagNames, null);
      Arrays.fill(tagIds, 0);
      Arrays.fill(nanoTimes, 0);
      Arrays.fill(genOps, 0);
      IDX.setVolatile(this, 0L);
    }

    static List<MarkList> readAll() {
      List<MarkList> markLists = new ArrayList<>(allSpans.size());
      SpanHolderRef.cleanQueue(allSpans);
      for (SpanHolderRef ref : allSpans.keySet()) {
        SpanHolder sh = ref.get();
        if (sh == null) {
          continue;
        }
        markLists.add(sh.read());
      }
      return Collections.unmodifiableList(markLists);
    }

    static final class ReadState {
      final Object[] localTaskNameOrMarkers = new Object[MAX_EVENTS];
      final String[] localTagNames = new String[MAX_EVENTS];
      final long[] localTagIds= new long[MAX_EVENTS];
      final long[] localNanoTimes = new long[MAX_EVENTS];
      final long[] localGenOps = new long[MAX_EVENTS];

      ReadState() {}
    }

    static final ThreadLocal<ReadState> states = new ThreadLocal<ReadState>() {
      @Override
      protected ReadState initialValue() {
        return new ReadState();
      }
    };

    MarkList read() {
      // long sizingRead = (long) IDX.getVolatile(this);
      ReadState rs = states.get();
      long startIdx = (long) IDX.getVolatile(this);
      int size = (int) Math.min(startIdx, MAX_EVENTS);
      for (int i = 0; i < size; i++) {
        rs.localTaskNameOrMarkers[i] = (Object) OBJECTS.getOpaque(taskNameOrMarkers, i);
        rs.localTagNames[i] = (String) STRINGS.getOpaque(tagNames, i);
        rs.localTagIds[i] = (long) LONGS.getOpaque(tagIds, i);
        rs.localNanoTimes[i] = (long) LONGS.getOpaque(nanoTimes, i);
        rs.localGenOps[i] = (long) LONGS.getOpaque(genOps, i);
      }
      long endIdx = (long) IDX.getVolatile(this);
      if (endIdx < startIdx) {
        throw new AssertionError();
      }
      // If we are reading from ourselves (such as in a test), we can assume there isn't an in
      // progress write modifying the oldest entry.  Additionally, if the writer has not yet
      // wrapped around, the last entry cannot have been corrupted.
      boolean tailValid = Thread.currentThread() == this.writerThread || endIdx < MAX_EVENTS - 1;
      endIdx += !tailValid ? 1 : 0;
      long eventsToDrop = endIdx - startIdx;
      final Deque<MarkList.Mark> marks = new ArrayDeque<>(size);
      for (int i = 0; i < size - eventsToDrop; i++) {
        int readIdx = (int) ((startIdx - i - 1) & MAX_EVENTS_MASK);
        long gen = rs.localGenOps[readIdx] & ~GEN_MASK;
        Operation op = Operation.valueOf((int) (rs.localGenOps[readIdx] & GEN_MASK));
        if (op == Operation.NONE) {
          throw new ConcurrentModificationException("Read of storage was not threadsafe");
        }
        marks.addFirst(new MarkList.Mark(
            rs.localTaskNameOrMarkers[readIdx],
            rs.localTagNames[readIdx],
            rs.localTagIds[readIdx],
            rs.localNanoTimes[readIdx],
            gen,
            op));
      }

      return new MarkList(
          Collections.unmodifiableList(new ArrayList<>(marks)), startTime, writerThread);
    }
  }

  private static final class SpanHolderRef extends WeakReference<SpanHolder> {
    private static final ReferenceQueue<SpanHolder> spanHolderQueue = new ReferenceQueue<>();

    SpanHolderRef(SpanHolder holder) {
      super(holder, spanHolderQueue);
    }

    static void cleanQueue(Map<SpanHolderRef, SpanHolderRef> allSpans) {
      Reference<?> ref;
      while ((ref = spanHolderQueue.poll()) != null) {
        ref.clear();
        allSpans.remove(ref);
      }
    }
  }
}
