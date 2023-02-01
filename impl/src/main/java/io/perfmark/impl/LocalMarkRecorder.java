package io.perfmark.impl;

/**
 * A local MarkRecorder is a class that gets the "current" MarkRecorder based on context.  For example, a thread local
 * MarkRecorder could use this class to pull the local MarkRecorder from a threadlocal variable.  Other
 * implementations are possible as well.
 */
public abstract class LocalMarkRecorder {
  protected abstract MarkRecorder getOrCreate();

  protected void clear() {}

  protected LocalMarkRecorder() {}
}
