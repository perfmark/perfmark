package io.perfmark;

public final class Internal {

  public static Marker createMarker(String taskName) {
    return new Marker(taskName, null);
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
