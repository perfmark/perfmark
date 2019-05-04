package io.grpc.contrib.perfmark.java9;

import io.grpc.contrib.perfmark.impl.Marker;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class VarHandleMarkHolderBenchmark {
  public final Marker MARKER = Marker.NONE;

  public final VarHandleMarkHolder markHolder = new VarHandleMarkHolder();

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_name_tag() {
    markHolder.start(1, "hi", "tag", 2, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_name_noTag() {
    markHolder.start(1, "hi", 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_marker_tag() {
    markHolder.start(1, MARKER, "tag", 2, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_marker_noTag() {
    markHolder.start(1, MARKER, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_name_tag() {
    markHolder.stop(1, "hi", "tag", 2, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_name_noTag() {
    markHolder.stop(1, "hi", 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_marker_tag() {
    markHolder.stop(1, MARKER, "tag", 2, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_marker_noTag() {
    markHolder.stop(1, MARKER, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void link() {
    markHolder.link(1, 9999, MARKER);
  }
}
