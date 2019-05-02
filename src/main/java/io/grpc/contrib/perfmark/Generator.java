package io.grpc.contrib.perfmark;

abstract class Generator {
  protected Generator() {}

  protected abstract void setGeneration(long generation);

  protected abstract long getGeneration();
}
