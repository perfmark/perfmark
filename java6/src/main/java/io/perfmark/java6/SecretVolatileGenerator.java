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

import io.perfmark.impl.Generator;

final class SecretVolatileGenerator {

  // @UsedReflectively
  public static final class VolatileGenerator extends Generator {

    // @UsedReflectively
    public VolatileGenerator() {}

    private volatile long gen;

    @Override
    public void setGeneration(long generation) {
      gen = generation;
    }

    @Override
    public long getGeneration() {
      return gen;
    }

    @Override
    public long costOfGetNanos() {
      return 3;
    }

    @Override
    public long costOfSetNanos() {
      return 10;
    }
  }

  private SecretVolatileGenerator() {
    throw new AssertionError("nope");
  }
}
