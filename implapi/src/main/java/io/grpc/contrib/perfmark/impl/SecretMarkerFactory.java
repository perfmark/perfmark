package io.grpc.contrib.perfmark.impl;

import javax.annotation.Nullable;

final class SecretMarkerFactory {

  public static Marker createMarker(String taskName, @Nullable StackTraceElement location) {
    return new Marker(taskName, location);
  }

  private SecretMarkerFactory() {
    throw new AssertionError("nope");
  }
}
