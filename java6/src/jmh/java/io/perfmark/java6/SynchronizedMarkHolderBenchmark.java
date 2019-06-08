package io.perfmark.java6;

import io.perfmark.impl.Marker;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class SynchronizedMarkHolderBenchmark {

  public final Marker MARKER = Marker.NONE;

  public final SynchronizedMarkHolder markHolder = new SynchronizedMarkHolder(16384);

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
    markHolder.start(1, "hi", MARKER, "tag", 2, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void start_marker_noTag() {
    markHolder.start(1, "hi", MARKER, 1234);
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
    markHolder.stop(1, "hi", MARKER, "tag", 2, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void stop_marker_noTag() {
    markHolder.stop(1, "hi", MARKER, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void link() {
    markHolder.link(1, 9999);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void link_marker() {
    markHolder.link(1, 9999, MARKER);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_name_tag() {
    markHolder.event(1, "hi", "tag", 2, 8, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_name_noTag() {
    markHolder.event(1, "hi", 8, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_marker_tag() {
    markHolder.event(1, "hi", Marker.NONE, "tag", 2, 8, 1234);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event_marker_noTag() {
    markHolder.event(1, "hi", Marker.NONE, 8, 1234);
  }
}
