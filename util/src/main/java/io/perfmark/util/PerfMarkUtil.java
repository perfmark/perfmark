package io.perfmark.util;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.MustBeClosed;
import io.perfmark.PerfMark;
import io.perfmark.Tag;
import java.util.function.Supplier;

public final class PerfMarkUtil {

  public interface CheckedRunnable<E extends Exception> {
    void run() throws E;
  }

  public static <T> T recordTaskResult(
      @CompileTimeConstant String taskName, Tag tag, Supplier<T> cmd) {
    PerfMark.startTask(taskName, tag);
    try {
      return cmd.get();
    } finally {
      PerfMark.stopTask(taskName, tag);
    }
  }

  public static <E extends Exception> void recordTask(
      @CompileTimeConstant String taskName, Tag tag, CheckedRunnable<E> cmd) throws E {
    PerfMark.startTask(taskName, tag);
    try {
      cmd.run();
    } finally {
      PerfMark.stopTask(taskName, tag);
    }
  }

  @MustBeClosed
  public static TaskRecorder recordTask(@CompileTimeConstant String taskName) {
    PerfMark.startTask(taskName);
    return () -> PerfMark.stopTask(taskName);
  }

  @MustBeClosed
  public static TaskRecorder recordTask(@CompileTimeConstant String taskName, Tag tag) {
    PerfMark.startTask(taskName, tag);
    return () -> PerfMark.stopTask(taskName, tag);
  }

  public interface TaskRecorder extends AutoCloseable {
    @Override
    void close();
  }

  private PerfMarkUtil() {}
}
