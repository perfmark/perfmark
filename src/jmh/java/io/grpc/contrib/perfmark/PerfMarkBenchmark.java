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
import org.openjdk.jmh.infra.Blackhole;


public class PerfMarkBenchmark {

  static final Marker MARKER = Marker.create("task");
  static final Tag TAG = new Tag("tag", 2);

  @State(Scope.Benchmark)
  public static class ASpanHolderBenchmark {

    private final PerfMarkStorage.SpanHolder spanHolder = new PerfMarkStorage.SpanHolder();

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void start_name_tag() {
      spanHolder.start(1, "hi", "tag", 2, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void start_name_noTag() {
      spanHolder.start(1, "hi", 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void start_marker_tag() {
      spanHolder.start(1, MARKER, "tag", 2, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void start_marker_noTag() {
      spanHolder.start(1, MARKER, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void stop_name_tag() {
      spanHolder.stop(1, "hi", "tag", 2, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void stop_name_noTag() {
      spanHolder.stop(1, "hi", 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void stop_marker_tag() {
      spanHolder.stop(1, MARKER, "tag", 2, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void stop_marker_noTag() {
      spanHolder.stop(1, MARKER, 1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void link() {
      spanHolder.link(1, 9999, MARKER);
    }
  }

  @State(Scope.Benchmark)
  public static class EnabledBenchmark {
    @Param({"true", "false"})
    public boolean enabled;

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
    public void startStopClosable() {
      try (PerfMarkCloseable pc = PerfMark.record("hi", TAG)) {
      }
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
      Link link = PerfMark.link();
      link.link();
    }
  }
}
