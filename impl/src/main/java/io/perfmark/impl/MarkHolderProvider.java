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

public abstract class MarkHolderProvider {

  protected MarkHolderProvider() {}

  /**
   * To be removed in 0.26.0
   *
   * @return the new MarkHolder for the current thread.
   */
  @Deprecated
  public MarkHolder create() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new MarkHolder.  Mark holders are always mutated by the thread that created them, (e.g. THIS thread),
   * but may be read by other threads.
   *
   * @param markHolderId the Unique ID associated with the Mark Holder.   This exists as a work around to Java's
   *                     thread ID, which does not guarantee they will not be reused.
   * @return the new MarkHolder for the current thread.
   * @since 0.24.0
   */
  public MarkHolder create(long markHolderId) {
    return create();
  }
}
