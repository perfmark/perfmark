package io.perfmark.util;

import io.perfmark.PerfMark;
import io.perfmark.Tag;
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
public class EnabledUtilBenchmark {
  private Tag tag;

  @Param({"true", "false"})
  public boolean enabled;

  @Setup
  public void setup() {
    PerfMark.setEnabled(enabled);
    tag = PerfMark.createTag("tag", 2);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void autoCloseable() {
    try (PerfMarkUtil.TaskRecorder recorder = PerfMarkUtil.recordTask("hi", tag)) {}
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void recordRunnable() {
    PerfMarkUtil.recordTask("hi", tag, () -> {});
  }
}
