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

package io.perfmark.java9;

import io.perfmark.impl.Generator;
import io.perfmark.impl.MarkRecorder;
import io.perfmark.impl.MarkRecorderProvider;
import io.perfmark.impl.Storage;

final class SecretVarHandleMarkRecorderProvider {

  public static final class VarHandleMarkRecorderProvider extends MarkRecorderProvider {

    public VarHandleMarkRecorderProvider() {
      // Do some basic operations to see if it works.
      VarHandleMarkRecorder recorder = new VarHandleMarkRecorder(12345);
      recorder.start(1 << Generator.GEN_OFFSET, "bogus", 0);
      recorder.stop(1 << Generator.GEN_OFFSET, "bogus", 0);
      int size = recorder.markHolder.read().get(0).size();
      if (size != 2) {
        throw new AssertionError("Wrong size " + size);
      }
    }

    @Override
    public MarkRecorder createMarkRecorder(long markHolderId) {
      VarHandleMarkRecorder markRecorder = new VarHandleMarkRecorder(markHolderId);
      Storage.registerMarkHolder(markRecorder.markHolder);
      return markRecorder;
    }
  }

  private SecretVarHandleMarkRecorderProvider() {
    throw new AssertionError("nope");
  }
}
