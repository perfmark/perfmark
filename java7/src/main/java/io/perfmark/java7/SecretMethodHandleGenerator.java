/*
 * Copyright 2019 Carl Mastrangelo
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

package io.perfmark.java7;

import io.perfmark.impl.Generator;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;

final class SecretMethodHandleGenerator {

  // UsedReflectively
  public static final class MethodHandleGenerator extends Generator {
    private static final MutableCallSite currentGeneration =
        new MutableCallSite(MethodHandles.constant(long.class, 0));
    private static final MutableCallSite[] currentGenerations =
        new MutableCallSite[] {currentGeneration};
    private static final MethodHandle currentGenerationGetter = currentGeneration.dynamicInvoker();

    public MethodHandleGenerator() {}

    @Override
    public long getGeneration() {
      try {
        return (long) currentGenerationGetter.invoke();
      } catch (Throwable throwable) {
        return FAILURE;
      }
    }

    @Override
    public void setGeneration(long generation) {
      currentGeneration.setTarget(MethodHandles.constant(long.class, generation));
      MutableCallSite.syncAll(currentGenerations);
    }

    @Override
    public long costOfGetNanos() {
      // Method handles compile to constants, so this is effectively free.
      // JMH testing on a Skylake x86_64 processor shows the cost to be about 0.3ns.
      return 0;
    }

    @Override
    public long costOfSetNanos() {
      // based on JMH testing on a Skylake x86_64 processor.
      return 2_000_000;
    }
  }

  private SecretMethodHandleGenerator() {
    throw new AssertionError("nope");
  }
}
