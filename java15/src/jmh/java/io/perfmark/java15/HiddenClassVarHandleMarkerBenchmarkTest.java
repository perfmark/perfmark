/*
 * Copyright 2021 Carl Mastrangelo
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

package io.perfmark.java15;

import io.perfmark.impl.MarkRecorder;
import io.perfmark.testing.MarkHolderRecorder;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@RunWith(JUnit4.class)
public class HiddenClassVarHandleMarkerBenchmarkTest {
  @Test
  public void markHolderBenchmark() throws Exception {
    Options options = new OptionsBuilder()
        .include(SecretHiddenClassMarkRecorderBenchmark.class.getCanonicalName())
        .addProfiler("cl")
        .measurementIterations(10)
        .warmupIterations(10)
        .forks(1)
        .warmupTime(TimeValue.seconds(1))
        .measurementTime(TimeValue.seconds(1))
        .shouldFailOnError(true)
        // This is necessary to run in the IDE, otherwise it would inherit the VM args.
        .jvmArgs(
            "-da",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:HeapDumpPath=/tmp/oom.hprof")
        .build();

    new Runner(options).run();
  }

  @State(Scope.Thread)
  public static class SecretHiddenClassMarkRecorderBenchmark extends MarkHolderRecorder {
    @Override
    public MarkRecorder getMarkRecorder() {
      return new SecretMarkRecorder.HiddenClassMarkRecorder();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Class<?> allocationBenchmark() throws IllegalAccessException {
      // TODO(carl-mastrangelo): fix this once the global mark recorder refactor is done.
      return Loader.getHiddenClass(Loader.DEFAULT_SIZE);
    }
  }
}
