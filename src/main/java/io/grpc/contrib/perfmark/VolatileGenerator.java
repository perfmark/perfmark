package io.grpc.contrib.perfmark;

final class VolatileGenerator extends Generator {

  VolatileGenerator() {}

  private volatile long gen;

  @Override
  protected void setGeneration(long generation) {
    gen = generation;
  }

  @Override
  protected long getGeneration() {
    return gen;
  }
}
