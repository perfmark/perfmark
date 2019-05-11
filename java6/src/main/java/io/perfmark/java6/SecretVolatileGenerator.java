package io.perfmark.java6;

import io.perfmark.impl.Generator;

final class SecretVolatileGenerator {

  // @UsedReflectively
  public static final class VolatileGenerator extends Generator {

    // @UsedReflectively
    public VolatileGenerator() {}

    private volatile long gen;

    @Override
    public void setGeneration(long generation) {
      gen = generation;
    }

    @Override
    public long getGeneration() {
      return gen;
    }

    @Override
    public long costOfGetNanos() {
      return 5;
    }

    @Override
    public long costOfSetNanos() {
      return 10;
    }
  }

  private SecretVolatileGenerator() {
    throw new AssertionError("nope");
  }
}
