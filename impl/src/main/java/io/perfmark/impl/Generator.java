/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.perfmark.impl;

/**
 * A Generator keeps track of what generation the PerfMark library is on. A generation is a way to
 * group PerfMark recorded tasks and links together. This allows dynamically enabling and disabling
 * PerfMark. Each time the library is enabled, a new generation is started and associated with all
 * the recorded data.
 *
 * <p>This class is not threadsafe.  Synchronization is handled externally.
 *
 * <p>Normal users are not expected to use this class.
 */
public class Generator {
  /**
   * This field is here as a hack. This class is a shared dependency of both {@link
   * SecretPerfMarkImpl} and {@link Storage}. The impl needs to record the first time an event
   * occurs, but doesn't call Storage#clinit until PerfMark is enabled. This leads to the timings
   * being off in the trace event viewer, since the "start" time is since it was enabled, rather
   * than when the first PerfMark call happens.
   */
  static final long INIT_NANO_TIME = System.nanoTime();

  static final long NO_NANO_TIME = INIT_NANO_TIME - 1;

  /**
   * This field is also here as a hack, capturing the time the class loaded.
   */
  static final long INIT_CURRENT_TIME_MILLIS = System.currentTimeMillis();

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
  public void setGeneration(long generation) {}

  /**
   * Gets the current generation, shifted left by {@link #GEN_OFFSET}. An odd number means the
   * library is enabled, while an even number means the library is disabled.
   *
   * @return the current generation or {@link #FAILURE}.
   */
  public long getGeneration() {
    return FAILURE;
  }

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
