/*
 * Copyright 2021 Google LLC
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

package io.perfmark;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class PerfMarkBenchmark {
  private static final List<String> PERFASM_ARGS =
      List.of("-XX:+UnlockDiagnosticVMOptions",
      "-XX:+LogCompilation",
      "-XX:LogFile=/tmp/blah.txt",
      "-XX:+PrintAssembly",
      "-XX:+PrintInterpreter",
      "-XX:+PrintNMethods",
      "-XX:+PrintNativeNMethods",
      "-XX:+PrintSignatureHandlers",
      "-XX:+PrintAdapterHandlers",
      "-XX:+PrintStubCode",
      "-XX:+PrintCompilation",
      "-XX:+PrintInlining",
      "-XX:+TraceClassLoading",
      "-XX:PrintAssemblyOptions=syntax",
      "-XX:PrintAssemblyOptions=intel");

  @Test
  public void run() throws RunnerException {
    Options options = new OptionsBuilder()
        .addProfiler("cl")
        .include(ClassLoad.class.getName())
        // This is necessary to run in the IDE, otherwise it would inherit the VM args.
        .jvmArgs("-da", "-Dio.perfmark.PerfMark.debug=false", "-XX:+UseZGC")
        .build();

    new Runner(options).run();
  }

  @Test
  public void runAsm() throws RunnerException {
    Options options = new OptionsBuilder()
        .addProfiler("gc")

        .include(Pattern.quote(StartStop.class.getCanonicalName()) + ".*")
        //.jvmArgsPrepend(PERFASM_ARGS.toArray(new String[0]))
        .jvmArgs("-da", "-Dio.perfmark.PerfMark.debug=false", "-XX:+UseZGC", "-Djava.security.manager")
        .build();

    new Runner(options).run();
  }

  @State(Scope.Benchmark)
  public static class StartStop {
    @Setup
    public void setUp() {
      PerfMark.setEnabled(true);
    }

    @Benchmark
    @Fork(1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Measurement(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
    @Warmup(iterations = 1, time = 4, timeUnit = TimeUnit.SECONDS)
    public Object startTask() {
      return PerfMark.linkOut();
    }
  }


  @State(Scope.Benchmark)
  public static class ClassLoad {
    @Setup
    public void setUp()
    {
      /*
      Logger logger = Logger.getLogger("io.perfmark.PerfMark");
      logger.setLevel(Level.FINE);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.FINE);
      logger.addHandler(handler);
      logger.setFilter(new Filter() {
        @Override
        public boolean isLoggable(LogRecord record) {
          System.err.println(record.toString());
          return true;
        }
      });
      PerfMark.setEnabled(true);
      Reference.reachabilityFence(logger);
      */

    }

    @Benchmark
    @Fork(200)
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Measurement(iterations = 1)
    @Warmup(iterations = 0)
    public void startTask() {
      PerfMark.setEnabled(false);
      PerfMark.startTask("hi");
    }
  }
}
