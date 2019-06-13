package io.perfmark.java6;

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
public class VolatileGeneratorBenchmark {
  public static final SecretVolatileGenerator.VolatileGenerator generator =
      new SecretVolatileGenerator.VolatileGenerator();

  @Setup(Level.Trial)
  public void setUp() {
    generator.setGeneration(Generator.FAILURE);
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

  private static boolean isEnabled(long gen) {
    return ((gen >>> Generator.GEN_OFFSET) & 0x1L) != 0L;
  }
}
