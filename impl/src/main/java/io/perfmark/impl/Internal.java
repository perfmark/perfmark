package io.perfmark.impl;

public final class Internal {

  public static Marker createMarker() {
    return new Marker(null);
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
