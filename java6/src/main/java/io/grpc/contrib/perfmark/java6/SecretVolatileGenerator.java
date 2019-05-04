package io.grpc.contrib.perfmark.java6;

import io.grpc.contrib.perfmark.impl.Generator;

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
  }
}
