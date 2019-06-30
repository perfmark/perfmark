package io.perfmark.impl;

public final class Internal {

  public static Marker createMarker() {
    return new Marker(null);
  }

  public static StackTraceElement getElement(Marker marker) {
    return marker.location;
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
