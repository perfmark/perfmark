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

package io.perfmark.java6;

import io.perfmark.impl.MarkRecorderProvider;
import io.perfmark.impl.MarkRecorder;
import io.perfmark.impl.Storage;

final class SecretSynchronizedMarkRecorderProvider {

  public static final class SynchronizedMarkRecorderProvider extends MarkRecorderProvider {

    // Used reflectively
    public SynchronizedMarkRecorderProvider() {}

    @Override
    public MarkRecorder createMarkRecorder(long markRecorderId) {
      SynchronizedMarkRecorder markRecorder = new SynchronizedMarkRecorder(markRecorderId);
      Storage.registerMarkHolder(markRecorder.markHolder);
      return markRecorder;
    }
  }

  private SecretSynchronizedMarkRecorderProvider() {
    throw new AssertionError("nope");
  }
}
