package io.perfmark;

import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Marker {

  public static final Marker NONE = new Marker("(notask)", null);

  private final String taskName;
  private final @Nullable StackTraceElement location;

  Marker(String taskName, @Nullable StackTraceElement location) {
    if (taskName == null) {
      throw new NullPointerException("taskName");
    }
    this.taskName = taskName;
    this.location = location;
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
