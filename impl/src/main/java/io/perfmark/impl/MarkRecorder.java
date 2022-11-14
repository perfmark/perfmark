package io.perfmark.impl;

public abstract class MarkRecorder {

  protected MarkRecorder() {}

  public abstract void start(long gen, String taskName, String tagName, long tagId, long nanoTime);

  public abstract void start(long gen, String taskName, long nanoTime);

  public abstract void start(long gen, String taskName, String subTaskName, long nanoTime);

  public abstract void link(long gen, long linkId);

  public abstract void stop(long gen, long nanoTime);

  public abstract void stop(long gen, String taskName, String tagName, long tagId, long nanoTime);

  public abstract void stop(long gen, String taskName, long nanoTime);

  public abstract void stop(long gen, String taskName, String subTaskName, long nanoTime);

  public abstract void event(long gen, String eventName, String tagName, long tagId, long nanoTime);

  public abstract void event(long gen, String eventName, long nanoTime);

  public abstract void event(long gen, String eventName, String subEventName, long nanoTime);

  public abstract void attachTag(long gen, String tagName, long tagId);

  public abstract void attachKeyedTag(long gen, String name, String value);

  public abstract void attachKeyedTag(long gen, String name, long value0);

  public abstract void attachKeyedTag(long gen, String name, long value0, long value1);
}
