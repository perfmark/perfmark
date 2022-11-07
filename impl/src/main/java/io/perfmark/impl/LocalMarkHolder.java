/*
 * Copyright 2022 Carl Mastrangelo
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
 * A local MarkHolder is a class that gets the "current" MarkHolder based on context.  For example, a thread local
 * MarkHolder could use this class to pull the local MarkHolder from the threadlocal variable.  Other implementations
 * are possible as well.
 */
public abstract class LocalMarkHolder {

  /**
   * Removes the local markholder storage.
   */
  public abstract void clear();

  /**
   * Get's the current MarkHolder for mutation.  Only called from a tracing thread.
   */
  public abstract MarkHolder acquire();

  /**
   * Releases the MarkHolder from being written too.  Usually called very shortly after {@link #acquire()}.
   * This method is meant to be overridden and should not be called from subclasses.
   */
  public void release(MarkHolder markHolder) {}
}
