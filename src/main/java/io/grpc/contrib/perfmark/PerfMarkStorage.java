package io.grpc.contrib.perfmark;


import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * PerfMark storage is where perfmark events are stored.  It is a thread local repository of the last N events.
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

    private static final class SpanHolder {
        private static final int MAX_EVENTS = 20000;

        private static final long START_OP = Op.START.ordinal();
        private static final long STOP_OP = Op.STOP.ordinal();
        private static final long LINK_OP = Op.LINK.ordinal();

        enum Op {
            NONE,
            START,
            STOP,
            LINK,
            ;
        }

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
        private int idx;
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

        synchronized void start(long gen, String taskName, String tagName, long tagId, Marker marker, long nanoTime) {
            taskNames[idx] = taskName;
            tagNames[idx] = tagName;
            tagIds[idx] = tagId;
            markers[idx] = marker;
            nanoTimes[idx] = nanoTime;
            genOps[idx] = gen | START_OP;
            if (++idx == MAX_EVENTS) {
                idx = 0;
            }
        }

        synchronized void link(long gen, long linkId, Marker marker) {
            taskNames[idx] = null;
            tagNames[idx] = null;
            tagIds[idx] = linkId;
            markers[idx] = marker;
            nanoTimes[idx] = NO_NANOTIME;
            genOps[idx] = gen | LINK_OP;
            if (++idx == MAX_EVENTS) {
                idx = 0;
            }
        }

        synchronized void stop(long gen, long nanoTime, Marker marker) {
            taskNames[idx] = null;
            tagNames[idx] = null;
            tagIds[idx] = 0;
            markers[idx] = marker;
            nanoTimes[idx] = nanoTime;
            genOps[idx] = gen | STOP_OP;
            if (++idx == MAX_EVENTS) {
                idx = 0;
            }
        }

        void read() {
            final int idx;
            final String[] taskNames = new String[MAX_EVENTS];
            final String[] tagNames = new String[MAX_EVENTS];
            final long[] tagIds= new long[MAX_EVENTS];
            final Marker[] markers = new Marker[MAX_EVENTS];
            final long[] nanoTimes = new long[MAX_EVENTS];
            final long[] genOps = new long[MAX_EVENTS];
            synchronized (this) {
                idx = this.idx;
                System.arraycopy(this.taskNames, 0, taskNames, 0, MAX_EVENTS);
                System.arraycopy(this.tagNames, 0, tagNames, 0, MAX_EVENTS);
                System.arraycopy(this.tagIds, 0, tagIds, 0, MAX_EVENTS);
                System.arraycopy(this.markers, 0, markers, 0, MAX_EVENTS);
                System.arraycopy(this.nanoTimes, 0, nanoTimes, 0, MAX_EVENTS);
                System.arraycopy(this.genOps, 0, genOps, 0, MAX_EVENTS);
            }

        }
    }

    private static final class SpanHolderRef extends WeakReference<SpanHolder> {
        static final ReferenceQueue<SpanHolder> spanHolderQueue = new ReferenceQueue<>();

        SpanHolderRef(SpanHolder holder) {
            super(holder, spanHolderQueue);
        }

        private SpanHolderRef() {
            super(null, null);
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
