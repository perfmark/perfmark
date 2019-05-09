package io.perfmark;

import io.perfmark.impl.Mark;
import javax.annotation.Nullable;

/**
 * Tag is a dynamic, runtime created identifier (such as an RPC id).
 */
public final class Tag {
  static final Tag NO_TAG = new Tag(Mark.NO_TAG_ID);

  @Nullable final String tagName;
  final long tagId;

  Tag(long tagId) {
    this.tagName = Mark.NO_TAG_NAME;
    this.tagId = tagId;
  }

  Tag(String tagName) {
    this(tagName, Mark.NO_TAG_ID);
  }

  Tag(String tagName, long tagId) {
    if (tagName == null) {
      throw new NullPointerException("bad tag name");
    }
    this.tagName = tagName;
    this.tagId = tagId;
  }

  @Override
  public String toString() {
    return "Tag(" + tagName + ", " + tagId + ')';
  }

  /**
   * Equality on Tags is not well defined, since the created tag can depend on if PerfMark was
   * enabled at the time of creation.
   */
  @Override
  @Deprecated // Don't use equality on tags
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
