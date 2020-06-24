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

package io.perfmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class EnabledBenchmark {
  private final Tag TAG = new Tag("tag", 2);

  @Param({"true", "false"})
  public boolean enabled;

  final String there = "there";

  @Setup
  public void setup() {
    PerfMark.setEnabled(enabled);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void startStop() {
    PerfMark.startTask("hi", TAG);
    PerfMark.stopTask("hi", TAG);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public Tag createTag() {
    return PerfMark.createTag("tag", 2);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void link() {
    Link link = PerfMark.linkOut();
    PerfMark.linkIn(link);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event() {
    PerfMark.event("hi", TAG);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_ss() {
    PerfMark.attachTag("hi", "there");
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_sn() {
    PerfMark.attachTag("hi", 934);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_snn() {
    PerfMark.attachTag("hi", 934, 5);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_ss_methodRef() {
    PerfMark.attachTag("hi", this, EnabledBenchmark::getStringValue);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_ss_ctor() {
    PerfMark.attachTag("hi", there, String::new);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_ss_globalRef() {
    PerfMark.attachTag("hi", this, ignore -> this.there);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void attachKeyedTag_ss_localRef() {
    String bar = there;
    PerfMark.attachTag("hi", this, ignore -> bar);
  }

  static String getStringValue(EnabledBenchmark self) {
    return self.there;
  }
}
