package io.grpc.contrib.perfmark;

public abstract class PerfMarkCloseable implements AutoCloseable {

    @Override
    public abstract void close();

    static final PerfMarkCloseable NOOP = new NoopAutoCloseable();
    static final PerfMarkCloseable MARKING = new MarkingAutoCloseable();

    PerfMarkCloseable() {}

    private static final class NoopAutoCloseable extends PerfMarkCloseable {
        @Override
        public void close() {}

        NoopAutoCloseable() {}
    }

    private static final class MarkingAutoCloseable extends PerfMarkCloseable {
        @Override
        public void close() {
            PerfMark.stopTask();
        }

        MarkingAutoCloseable() {}
    }
}

