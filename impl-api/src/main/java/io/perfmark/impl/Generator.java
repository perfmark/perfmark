package io.perfmark.impl;

public abstract class Generator {
  public static final int GEN_OFFSET = 8;
  public static final long FAILURE = -2L << GEN_OFFSET;

  protected Generator() {}

  public abstract void setGeneration(long generation);

  public abstract long getGeneration();
}
