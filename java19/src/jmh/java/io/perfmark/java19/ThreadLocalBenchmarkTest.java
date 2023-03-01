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

package io.perfmark.java19;

import io.perfmark.impl.ConcurrentThreadLocal;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

@RunWith(JUnit4.class)
public class ThreadLocalBenchmarkTest {
  @Test
  public void localBenchMark_allowed() throws Exception {
    localBenchMark(true);
  }

  @Test
  public void localBenchMark_disallowed() throws Exception {
    localBenchMark(false);
  }

  private void localBenchMark(boolean allowed) throws RunnerException {
    Options options =
        new OptionsBuilder()
            .include(ThreadLocalBenchMark.class.getCanonicalName())
            .measurementIterations(5)
            .warmupIterations(10)
            .forks(1)
            .verbosity(VerboseMode.EXTRA)
            .warmupTime(TimeValue.seconds(1))
            .measurementTime(TimeValue.seconds(1))
            .shouldFailOnError(true)
            .param("allowed", Boolean.toString(allowed))
            .jvmArgs(
                "-da",
                // "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=localhost:5005",
                "-D" + VirtualExecutor.THREAD_LOCALS_DISABLED_PROP + "=" + !allowed,
                "--enable-preview",
                "-Djmh.executor=CUSTOM",
                "-Djmh.executor.class=" + VirtualExecutor.class.getName())
            .build();

    new Runner(options).run();
  }

  @State(Scope.Benchmark)
  public static class ThreadLocalBenchMark {
    private static long countN(int n) {
      long sum = 0;
      do {
        sum += n;
      } while (--n > 0);
      return sum;
    }

    private static class CountingThreadLocal extends ThreadLocal<Long> {
      @Override
      protected Long initialValue() {
        return countN(256);
      }
    }

    private static final class CountingGetSetCatchThreadLocal extends ThreadLocal<Long> {
      @Override
      public Long get() {
        Long value = super.get();
        if (value == null) {
          value = countN(256);
          try {
            set(value);
          } catch (UnsupportedOperationException e) {
          }
        }
        return value;
      }
    }

    private static final ThreadLocal<Long> staticTLInitialValue = new CountingThreadLocal();

    private final ThreadLocal<Long> memberTLInitialValue = new CountingThreadLocal();

    private static final ThreadLocal<Long> staticTLGetSet = new CountingGetSetCatchThreadLocal();

    private final ThreadLocal<Long> memberTLGetSet = new CountingGetSetCatchThreadLocal();

    private static final ConcurrentThreadLocal<Long> staticConcLocal =
        new ConcurrentThreadLocal<>() {
          @Override
          protected Long initialValue() {
            return countN(256);
          }
        };

    private final ConcurrentThreadLocal<Long> memberConcLocal =
        new ConcurrentThreadLocal<>() {
          @Override
          protected Long initialValue() {
            return countN(256);
          }
        };

    @Param({"true", "false"})
    public boolean allowed;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Long staticTLInitialValue() {
      return staticTLInitialValue.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Long memberTLInitialValue() {
      return memberTLInitialValue.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Long astaticTLGetSet() {
      return staticTLGetSet.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Long memberTLGetSet() {
      return memberTLGetSet.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Long getStaticConcOverride() {
      return staticConcLocal.get();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Long getMemberConcOverride() {
      return memberConcLocal.get();
    }
  }
}
