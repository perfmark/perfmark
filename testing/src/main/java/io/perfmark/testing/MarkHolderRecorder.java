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

package io.perfmark.testing;

import io.perfmark.impl.Generator;
import io.perfmark.impl.MarkRecorder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class MarkHolderRecorder {

  private static final long gen = 1 << Generator.GEN_OFFSET;
  private static final String taskName = "hiya";

  public static final List<String> ASM_FLAGS = List.of(
      "-XX:+UnlockDiagnosticVMOptions",
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
      "-XX:PrintAssemblyOptions=syntax",
      "-XX:PrintAssemblyOptions=intel");

  protected MarkRecorder markRecorder;

  private String tagName = "tag";
  private long tagId = 0xf0f0;
  private long nanoTime = 0xf1f1;
  private long linkId = 0xf2f2;

  public MarkRecorder getMarkRecorder() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Setup
  public final void setUp() {
    markRecorder = getMarkRecorder();
  }

  @Param({"G1"})
  GarbageCollector GC;

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_name_tag() {
    markRecorder.start(gen, taskName, tagName, tagId, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_name_noTag() {
    markRecorder.start(gen, taskName, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_name_subname() {
    markRecorder.start(gen, taskName, taskName, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_name_tag() {
    markRecorder.stop(gen, taskName, tagName, tagId, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_name_noTag() {
    markRecorder.stop(gen, taskName, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_name_subname() {
    markRecorder.stop(gen, taskName, tagName, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void link() {
    markRecorder.link(gen, linkId);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_name_tag() {
    markRecorder.event(gen, taskName, tagName, tagId, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_name_noTag() {
    markRecorder.event(gen, taskName, nanoTime);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_name_subname() {
    markRecorder.event(gen, taskName, taskName, nanoTime);
  }
}
