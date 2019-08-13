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

import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;

final class SecretVarHandleMarkHolderProvider {

  public static final class VarHandleMarkHolderProvider extends MarkHolderProvider {

    public VarHandleMarkHolderProvider() {
      // Do some basic operations to see if it works.
      MarkHolder holder = create();
      holder.start(1, "bogus", 0);
      holder.stop(1, "bogus", 0);
      int size = holder.read(false).size();
      if (size != 2) {
        throw new AssertionError("Wrong size " + size);
      }
    }

    @Override
    public MarkHolder create() {
      return new VarHandleMarkHolder();
    }
  }

  private SecretVarHandleMarkHolderProvider() {
    throw new AssertionError("nope");
  }
}
