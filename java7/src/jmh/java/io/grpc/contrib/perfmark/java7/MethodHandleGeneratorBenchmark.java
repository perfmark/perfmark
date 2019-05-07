package io.grpc.contrib.perfmark.java7;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class MethodHandleGeneratorBenchmark {

  public static final SecretMethodHandleGenerator.MethodHandleGenerator generator =
      new SecretMethodHandleGenerator.MethodHandleGenerator();

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
}
