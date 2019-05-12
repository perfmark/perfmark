package io.perfmark.impl;

import javax.annotation.Nullable;

final class SecretMarkerFactory {

  public static final class MarkerFactory {
    public static Marker createMarker(String taskName, @Nullable StackTraceElement location) {
      return new Marker(taskName, location);
    }

    private MarkerFactory() {
      throw new AssertionError("nope");
    }
  }

  private SecretMarkerFactory() {
    throw new AssertionError("nope");
  }
}
