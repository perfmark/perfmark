package io.perfmark;

import io.perfmark.impl.Generator;

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
