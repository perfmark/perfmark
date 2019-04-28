package io.grpc.contrib.perfmark;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;


public class PerfMarkBenchmark {

  @State(Scope.Benchmark)
  public static class ASpanHolderBenchmark {
    private final PerfMarkStorage.SpanHolder spanHolder = new PerfMarkStorage.SpanHolder();

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void start() {
      spanHolder.start(1, "hi", null, 0, Marker.NONE, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void startNoTag() {
      spanHolder.startNoTag(1, "hi", Marker.NONE, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void stop() {
      spanHolder.stop(1, "hi", null, 0, Marker.NONE, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void stopNoTag() {
      spanHolder.stopNoTag(1, "hi", Marker.NONE, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void link() {
      spanHolder.link(1, 9999, Marker.NONE);
    }
  }

  @State(Scope.Benchmark)
  public static class EnabledBenchmark {
    @Param({"false", "true"})
    public boolean enabled;

    @Setup
    public void setup() {
      PerfMark.setEnabled(enabled);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void startStop() {
      PerfMark.startTask("hi");
      PerfMark.stopTask();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void startStopClosable() {
      try (PerfMarkCloseable pc = PerfMark.record("hi")) {
      }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Tag createTag() {
      return PerfMark.createTag(1);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void link() {
      Link link = PerfMark.link();
      link.link();
    }
  }
}
