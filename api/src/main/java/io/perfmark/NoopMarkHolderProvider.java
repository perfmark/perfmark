package io.perfmark;

import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;
import io.perfmark.impl.Marker;
import java.util.Collections;
import java.util.List;

final class NoopMarkHolderProvider extends MarkHolderProvider {
  NoopMarkHolderProvider() {}

  @Override
  public MarkHolder create() {
    return new NoopMarkHolder();
  }

  private static final class NoopMarkHolder extends MarkHolder {

    NoopMarkHolder() {}

    @Override
    public void start(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

    @Override
    public void start(long gen, Marker marker, String tagName, long tagId, long nanoTime) {}

    @Override
    public void start(long gen, String taskName, long nanoTime) {}

    @Override
    public void start(long gen, Marker marker, long nanoTime) {}

    @Override
    public void link(long gen, long linkId, Marker marker) {}

    @Override
    public void stop(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

    @Override
    public void stop(long gen, Marker marker, String tagName, long tagId, long nanoTime) {}

    @Override
    public void stop(long gen, String taskName, long nanoTime) {}

    @Override
    public void stop(long gen, Marker marker, long nanoTime) {}

    @Override
    public void event(
        long gen,
        String eventName,
        String tagName,
        long tagId,
        long nanoTime,
        long durationNanos) {}

    @Override
    public void event(
        long gen, Marker marker, String tagName, long tagId, long nanoTime, long durationNanos) {}

    @Override
    public void event(long gen, String eventName, long nanoTime, long durationNanos) {}

    @Override
    public void event(long gen, Marker marker, long nanoTime, long durationNanos) {}

    @Override
    public void resetForTest() {}

    @Override
    public List<Mark> read(boolean readerIsWriter) {
      return Collections.emptyList();
    }
  }
}
