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
 * A local MarkRecorder is a class that gets the "current" MarkRecorder based on context.  For example, a thread local
 * MarkRecorder could use this class to pull the local MarkRecorder from a threadlocal variable.  Other
 * implementations are possible as well.
 */
public interface LocalMarkRecorder {
  /**
   * Gets or creates a MarkHolder.
   * @return a non {@code null} MarkRecorder.
   */
  MarkRecorder get();

  /**
   * Removes the local Mark Holder
   */
  void remove();
}
