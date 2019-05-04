package io.grpc.contrib.perfmark;

import java.util.Arrays;
import javax.annotation.Nullable;

final class Marker {

  static final Marker NONE = new Marker("(notask)", null);

  private final String taskName;
  private final @Nullable StackTraceElement location;


  private Marker(String taskName, @Nullable StackTraceElement location) {
    if (taskName == null) {
      throw new NullPointerException("taskName");
    }
    this.taskName = taskName;
    this.location = location;
  }

  static Marker create(String taskName) {
    StackTraceElement[] st = new RuntimeException().fillInStackTrace().getStackTrace();
    if (st.length > 1) {
      return new Marker(taskName, st[1]);
    } else {
      return new Marker(taskName, null);
    }
  }

  public String getTaskName() {
    return taskName;
  }

  @Override
  public String toString() {
    return "Marker{" + taskName + "," + location + "}";
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {taskName, location});
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Marker)) {
      return false;
    }
    Marker other = (Marker) obj;
    return Arrays.equals(
        new Object[] {taskName, location}, new Object[] {other.taskName, other.location});

  }
}
