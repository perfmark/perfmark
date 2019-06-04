package io.perfmark;

/**
 * Internal class for accessing package protected methods of the PerfMark API. If you need to use
 * this class, please file an issue on GitHub first with your use case.
 */
public final class Internal {

  public static Tag createTag(String name, long tagId) {
    return new Tag(name, tagId);
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
