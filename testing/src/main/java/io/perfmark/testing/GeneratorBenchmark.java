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
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class GeneratorBenchmark {

  private Generator generator;

  @Setup(Level.Trial)
  public void setUp() {
    generator = getGenerator();
    generator.setGeneration(Generator.FAILURE);
  }

  protected Generator getGenerator() {
    throw new UnsupportedOperationException();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void ifEnabled() {
    if (isEnabled(getGeneration())) {
      Blackhole.consumeCPU(1000);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public long getGeneration() {
    return generator.getGeneration();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public long getAndSetAndGetGeneration() {
    long oldGeneration = generator.getGeneration();
    generator.setGeneration(oldGeneration + 1);
    return generator.getGeneration();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @GroupThreads(3)
  public long racyGetAndSetAndGetGeneration() {
    long oldGeneration = generator.getGeneration();
    generator.setGeneration(oldGeneration + 1);
    return generator.getGeneration();
  }

  protected static boolean isEnabled(long gen) {
    return ((gen >>> Generator.GEN_OFFSET) & 0x1L) != 0L;
  }
}
