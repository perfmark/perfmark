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

import java.util.Collection;
import java.util.List;

/**
 * A MarkHolder records Marks for later retrieval.
 */
public abstract class MarkHolder {

  public static final int NO_MAX_MARKS = -1;

  /**
   * Attempts to remove all Marks for the calling thread.
   *
   */
  public void resetForThread() {
    // noop
  }

  /**
   * Attempts to remove all Marks in this mark holder.
   */
  public void resetForAll() {
    // noop
  }

  public abstract List<MarkList> read();

  public void read(Collection<? super MarkList> destination) {
    destination.addAll(read());
  }

  public int maxMarks() {
    return NO_MAX_MARKS;
  }

  protected MarkHolder() {}
}
