package io.grpc.contrib.perfmark;

public final class Internal {

  public static Tag createTag(String name, long tagId) {
    return new Tag(name, tagId);
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
