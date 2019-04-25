package io.grpc.contrib.perfmark;

import java.util.Arrays;

public final class Mark {
  private final String taskName;
  private final String tagName;
  private final long tagId;
  private final Marker marker;
  private final long nanoTime;
  private final long generation;
  private final Operation operation;

  Mark(
      String taskName,
      String tagName,
      long tagId,
      Marker marker,
      long nanoTime,
      long generation,
      Operation operation) {
    this.operation = checkNotNull(operation, "operation");
    this.taskName = taskName;
    if (operation != Operation.TASK_END && operation != Operation.LINK) {
      checkNotNull(taskName, "taskName");
    }
    this.tagName = tagName;
    this.tagId = tagId;
    this.marker = checkNotNull(marker, "marker");
    this.nanoTime = nanoTime;
    this.generation = generation;
  }

  public enum Operation {
    NONE,
    TASK_START,
    TASK_END,
    LINK,
    ;

    private static final Operation[] values = values();
    static {
      assert values.length <= (1 << PerfMark.GEN_OFFSET);
    }

    static Operation valueOf(int ordinal) {
      return values[ordinal];
    }
  }

  long getNanoTime() {
    return nanoTime;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Mark)) {
      return false;
    }
    Mark that = (Mark) obj;
    return equal(this.taskName, that.taskName)
        && equal(this.tagName, that.tagName)
        && equal(this.tagId, that.tagId)
        && equal(this.marker, that.marker)
        && this.nanoTime == that.nanoTime
        && this.operation == that.operation;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{taskName, tagName, tagId, marker, nanoTime, operation});
  }

  @Override
  public String toString() {
    return "Mark{"
        + "taskName=" + taskName + ", "
        + "tagName=" + tagName + ", "
        + "tagId=" + tagId + ", "
        + "marker=" + marker + ", "
        + "nanoTime=" + nanoTime + ", "
        + "generation=" + generation + ", "
        + "operation=" + operation + "}";
  }

  private static <T> T checkNotNull(T t, String name) {
    if (t == null) {
      throw new NullPointerException(name + " is null");
    }
    return t;
  }

  private static <T> boolean equal(T a, T b) {
    return a == b || a != null && a.equals(b);
  }
}
