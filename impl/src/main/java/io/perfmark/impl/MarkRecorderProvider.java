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

import java.lang.ref.WeakReference;

/**
 * This class creates MarkRecorders.  Custom MarkRecorder providers can be set using
 * {@link Storage#MARK_RECORDER_PROVIDER_PROP "io.perfmark.PerfMark.markRecorderProvider"}.
 */
public abstract class MarkRecorderProvider {

  protected MarkRecorderProvider() {}

  /**
   * Creates a new MarkHolder.  Mark holders are always mutated by the thread that created them, (e.g. THIS thread),
   * but may be read by other threads.
   *
   * @return the new MarkHolder for the current thread.
   * @since 0.27.0
   */
  public abstract MarkRecorder createMarkRecorder(long markRecorderId, WeakReference<Thread> creatingThread);
}
