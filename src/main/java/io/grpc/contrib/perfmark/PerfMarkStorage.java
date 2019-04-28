package io.grpc.contrib.perfmark;


import io.grpc.contrib.perfmark.MarkList.Mark.Operation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * PerfMark storage is where perfmark events are stored.  It is a thread local repository of the
 * last N events.
 */
final class PerfMarkStorage {

  private static final long NO_NANOTIME = 123456789876543210L;

  static void startAnyways(long gen, String taskName, Tag tag, Marker marker) {
    SpanHolder.current().start(gen, taskName, tag.tagName, tag.tagId, marker, System.nanoTime());
  }

  static void stopAnyways(long gen, Marker marker) {
    long nanoTime = System.nanoTime();
    SpanHolder.current().stop(gen, nanoTime, marker);
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
    private static final int MAX_EVENTS_BITS = 15;
    private static final int MAX_EVENTS = 1 << MAX_EVENTS_BITS;
    private static final long MAX_EVENTS_MASK = MAX_EVENTS - 1;
    private static final long GEN_MASK = (1 << PerfMark.GEN_OFFSET) - 1;

    private static final long START_OP = Operation.TASK_START.ordinal();
    private static final long STOP_OP = Operation.TASK_END.ordinal();
    private static final long LINK_OP = Operation.LINK.ordinal();

    private static final AtomicLongFieldUpdater<SpanHolder> idxHandle =
        AtomicLongFieldUpdater.newUpdater(SpanHolder.class, "idx");

    private static final ConcurrentMap<SpanHolderRef, SpanHolderRef> allSpans =
        new ConcurrentHashMap<>();
    private static final ThreadLocal<SpanHolder> localSpan = new ThreadLocal<SpanHolder>() {
      @Override
      protected SpanHolder initialValue() {
        return new SpanHolder();
      }
    };

    private final Thread currentThread;
    private final SpanHolderRef ref;
    private final long startTime;

    // where to write to next
    @SuppressWarnings("unused") // Used Reflectively
    private volatile long idx;
    private final String[] taskNames = new String[MAX_EVENTS];
    private final String[] tagNames = new String[MAX_EVENTS];
    private final long[] tagIds= new long[MAX_EVENTS];
    private final Marker[] markers = new Marker[MAX_EVENTS];
    private final long[] nanoTimes = new long[MAX_EVENTS];
    private final long[] genOps = new long[MAX_EVENTS];

    SpanHolder() {
      SpanHolderRef.cleanQueue(allSpans);
      this.currentThread = Thread.currentThread();
      this.ref = new SpanHolderRef(this);
      allSpans.put(ref, ref);
      this.startTime = System.nanoTime();
    }

    static SpanHolder current() {
      return localSpan.get();
    }

    void start(long gen, String taskName, String tagName, long tagId, Marker marker, long nanoTime) {
      long localIdx = idxHandle.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      taskNames[i] = taskName;
      tagNames[i] = tagName;
      tagIds[i] = tagId;
      markers[i] = marker;
      nanoTimes[i] = nanoTime;
      genOps[i] = gen | START_OP;
      idxHandle.lazySet(this, localIdx + 1);
    }

    void link(long gen, long linkId, Marker marker) {
      long localIdx = idxHandle.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      taskNames[i] = null;
      tagNames[i] = null;
      tagIds[i] = linkId;
      markers[i] = marker;
      nanoTimes[i] = NO_NANOTIME;
      genOps[i] = gen | LINK_OP;
      idxHandle.lazySet(this, localIdx + 1);
    }

    void stop(long gen, long nanoTime, Marker marker) {
      long localIdx = idxHandle.get(this);
      int i = (int) (localIdx & MAX_EVENTS_MASK);
      taskNames[i] = null;
      tagNames[i] = null;
      tagIds[i] = 0;
      markers[i] = marker;
      nanoTimes[i] = nanoTime;
      genOps[i] = gen | STOP_OP;
      idxHandle.lazySet(this, localIdx + 1);
    }

    void resetForTest() {
      assert Thread.currentThread() == this.currentThread;
      Arrays.fill(taskNames, null);
      Arrays.fill(tagNames, null);
      Arrays.fill(tagIds, 0);
      Arrays.fill(markers, null);
      Arrays.fill(nanoTimes, 0);
      Arrays.fill(genOps, 0);
      idxHandle.lazySet(this, 0L);
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

    private MarkList read() {
      final boolean selfRead = Thread.currentThread() == this.currentThread;
      final Deque<MarkList.Mark> marks = new ArrayDeque<>(MAX_EVENTS);
      final String[] localTaskNames = new String[MAX_EVENTS];
      final String[] localTagNames = new String[MAX_EVENTS];
      final long[] localTagIds= new long[MAX_EVENTS];
      final Marker[] localMarkers = new Marker[MAX_EVENTS];
      final long[] localNanoTimes = new long[MAX_EVENTS];
      final long[] localGenOps = new long[MAX_EVENTS];

      long startIdx = idxHandle.get(this);
      int copy = (int) Math.min(startIdx, MAX_EVENTS);
      System.arraycopy(this.taskNames, 0, localTaskNames, 0, copy);
      System.arraycopy(this.tagNames, 0, localTagNames, 0, copy);
      System.arraycopy(this.tagIds, 0, localTagIds, 0, copy);
      System.arraycopy(this.markers, 0, localMarkers, 0, copy);
      System.arraycopy(this.nanoTimes, 0, localNanoTimes, 0, copy);
      System.arraycopy(this.genOps, 0, localGenOps, 0, copy);
      long endIdx = idxHandle.get(this);
      if (endIdx < startIdx) {
        throw new AssertionError();
      }
      // If we are reading from ourselves (such as in a test), we can assume there isn't an in
      // progress write modifying the oldest entry.
      endIdx += !selfRead ? 1 : 0;
      long eventsToDrop = endIdx - startIdx;
      for (int i = 0; i < copy - eventsToDrop; i++) {
        int readIdx = (int) ((startIdx - i - 1) & MAX_EVENTS_MASK);
        long gen = localGenOps[readIdx] & ~GEN_MASK;
        Operation op = Operation.valueOf((int) (localGenOps[readIdx] & GEN_MASK));
        if (op == Operation.NONE) {
          break; // this should be impossible, unless resetForTest was called.
        }
        marks.addFirst(new MarkList.Mark(
            localTaskNames[readIdx],
            localTagNames[readIdx],
            localTagIds[readIdx],
            localMarkers[readIdx],
            localNanoTimes[readIdx],
            gen,
            op));
      }

      return new MarkList(
          Collections.unmodifiableList(new ArrayList<>(marks)), startTime, currentThread);
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
