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

package io.perfmark.java7;

import io.perfmark.impl.Generator;
import io.perfmark.testing.GeneratorBenchmark;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

@RunWith(JUnit4.class)
public class MethodHandleGeneratorBenchmarkTest {

  @Test
  public void generatorBenchmark() throws Exception {
    Options options = new OptionsBuilder()
        .include(MethodHandleGeneratorBenchmark.class.getCanonicalName())
        .measurementIterations(5)
        .warmupIterations(10)
        .forks(1)
        .verbosity(VerboseMode.EXTRA)
        .warmupTime(TimeValue.seconds(1))
        .measurementTime(TimeValue.seconds(1))
        .shouldFailOnError(true)
        // This is necessary to run in the IDE, otherwise it would inherit the VM args.
        .jvmArgs("-da")
        .build();

    new Runner(options).run();
  }

  @State(Scope.Benchmark)
  public static class MethodHandleGeneratorBenchmark extends GeneratorBenchmark {
    @Override
    protected Generator getGenerator() {
      return new SecretGenerator.MethodHandleGenerator();
    }
  }
}
