package io.perfmark.impl;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Marker {

  public static final Marker NONE = new Marker(null);

  @Nullable final StackTraceElement location;

  Marker(@Nullable StackTraceElement location) {
    this.location = location;
  }

  @Override
  public String toString() {
    return "Marker{" + location + "}";
  }

  @Override
  public int hashCode() {
    return location == null ? 31 : location.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Marker)) {
      return false;
    }
    Marker other = (Marker) obj;
    if (this.location == null) {
      return other.location == null;
    }
    return this.location.equals(other.location);
  }
}
