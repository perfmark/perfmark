package io.grpc.contrib.perfmark.impl;

import java.util.List;

public abstract class MarkHolder {

  public abstract void start(long gen, String taskName, String tagName, long tagId, long nanoTime);

  public abstract void start(long gen, Marker marker, String tagName, long tagId, long nanoTime);

  public abstract void start(long gen, String taskName, long nanoTime);

  public abstract void start(long gen, Marker marker, long nanoTime);

  public abstract void link(long gen, long linkId, Marker marker);

  public abstract void stop(long gen, String taskName, String tagName, long tagId, long nanoTime);

  public abstract void stop(long gen, Marker marker, String tagName, long tagId, long nanoTime);

  public abstract void stop(long gen, String taskName, long nanoTime);

  public abstract void stop(long gen, Marker marker, long nanoTime);

  public abstract void resetForTest();

  public abstract List<Mark> read(boolean readerIsWriter);

  protected MarkHolder() {}
}
