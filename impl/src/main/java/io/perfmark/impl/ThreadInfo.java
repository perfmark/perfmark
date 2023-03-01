/*
 * Copyright 2023 Carl Mastrangelo
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
 * Represents info about a Thread that may or may not still be around.
 */
public abstract class ThreadInfo {
  /**
   * The most recent name of the thread.  Non-{@code null}.
   */
  public abstract String getName();

  /**
   * The most recent ID of the thread.
   */
  public abstract long getId();

  /**
   * Returns {@code true} if the thread has been GC'd or is terminated.
   */
  public abstract boolean isTerminated();

  public abstract boolean isCurrentThread();
}
