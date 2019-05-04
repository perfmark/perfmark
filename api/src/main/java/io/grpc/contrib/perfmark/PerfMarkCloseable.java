package io.grpc.contrib.perfmark;

import com.google.errorprone.annotations.CompileTimeConstant;
import java.io.Closeable;

public abstract class PerfMarkCloseable implements Closeable {

  @Override
  public abstract void close();

  static final PerfMarkCloseable NOOP = new NoopAutoCloseable();

  PerfMarkCloseable() {}

  private static final class NoopAutoCloseable extends PerfMarkCloseable {
    @Override
    public void close() {}

    NoopAutoCloseable() {}
  }

  static final class TaskTagAutoCloseable extends PerfMarkCloseable {
    private final String taskName;
    private final Tag tag;

    @Override
    public void close() {
      PerfMark.stopTaskNonConstant(taskName, tag);
    }

    TaskTagAutoCloseable(@CompileTimeConstant String taskName, Tag tag) {
      this.taskName = taskName;
      this.tag = tag;
    }
  }

  static final class TaskAutoCloseable extends PerfMarkCloseable {
    private final String taskName;

    @Override
    public void close() {
      PerfMark.stopTaskNonConstant(taskName);
    }

    TaskAutoCloseable(@CompileTimeConstant String taskName) {
      this.taskName = taskName;
    }
  }
}

