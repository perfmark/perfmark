package io.grpc.contrib.perfmark;

import javax.annotation.Nullable;

/**
 * Tag is a dynamic, runtime created identifier (such as an RPC id).
 */
public final class Tag {
  static final String NO_TAG_NAME = null;
  static final long NO_TAG_ID = 0;
  static final Tag NO_TAG = new Tag();

  @Nullable final String tagName;
  final long tagId;

  private Tag() {
    this.tagName = NO_TAG_NAME;
    this.tagId = NO_TAG_ID;
  }

  Tag(long tagId) {
    this.tagName = NO_TAG_NAME;
    this.tagId = tagId;
  }

  Tag(String tagName) {
    if (tagName == null) {
      throw new NullPointerException("bad tag name");
    }
    this.tagName = tagName;
    this.tagId = NO_TAG_ID;
  }

  Tag(String tagName, long tagId) {
    if (tagName == null) {
      throw new NullPointerException("bad tag name");
    }
    this.tagName = tagName;
    this.tagId = tagId;
  }
}
