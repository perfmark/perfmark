package io.perfmark;

/**
 * Noop Generator for use when no other generator can be used.
 */
final class NoopGenerator extends Generator {

  NoopGenerator() {}

  @Override
  public void setGeneration(long generation) {}

  @Override
  public long getGeneration() {
    return FAILURE;
  }
}
