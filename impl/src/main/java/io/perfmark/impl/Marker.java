package io.perfmark.impl;

import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Marker {

  public static final Marker NONE = new Marker(null);

  private final @Nullable StackTraceElement location;

  Marker(@Nullable StackTraceElement location) {
    this.location = location;
  }

  @Override
  public String toString() {
    return "Marker{" + location + "}";
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {location});
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Marker)) {
      return false;
    }
    Marker other = (Marker) obj;
    return Arrays.equals(new Object[] {location}, new Object[] {other.location});
  }
}
