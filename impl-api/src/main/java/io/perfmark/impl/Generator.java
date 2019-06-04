package io.perfmark.impl;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A Generator keeps track of what generation the PerfMark library is on. A generation is a way to
 * group PerfMark recorded tasks and links together. This allows dynamically enabling and disabling
 * PerfMark. Each time the library is enabled, a new generation is started and associated with all
 * the recorded data.
 *
 * <p>Normal users are not expected to use this class.
 */
@NotThreadSafe
public abstract class Generator {
  /**
   * The number of reserved bits at the bottom of the generation. All generations should be
   * left-shifted by this amount.
   */
  public static final int GEN_OFFSET = 8;
  /**
   * Represents a failure to enable PerfMark library. This can be used by subclasses to indicate a
   * previous failure to set the generation. It can also be used to indicate the generation count
   * has overflowed.
   */
  public static final long FAILURE = -2L << GEN_OFFSET;

  protected Generator() {}

  /**
   * Sets the current generation count. This should be eventually noticeable for callers of {@link
   * #getGeneration()}. An odd number means the library is enabled, while an even number means the
   * library is disabled.
   *
   * @param generation the generation id, shifted left by {@link #GEN_OFFSET}.
   */
  public abstract void setGeneration(long generation);

  /**
   * Gets the current generation, shifted left by {@link #GEN_OFFSET}. An odd number means the
   * library is enabled, while an even number means the library is disabled.
   *
   * @return the current generation or {@link #FAILURE}.
   */
  public abstract long getGeneration();

  /**
   * Returns the approximate cost to change the generation.
   *
   * @return an approximate number of nanoseconds needed to change the generator value.
   */
  public long costOfSetNanos() {
    return 1000000;
  }

  /**
   * Returns the approximate cost to read the generation.
   *
   * @return an approximate number of nanoseconds needed to read the generator value.
   */
  public long costOfGetNanos() {
    return 10;
  }
}
