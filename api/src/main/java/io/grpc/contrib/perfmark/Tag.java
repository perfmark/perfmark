package io.grpc.contrib.perfmark;

import io.grpc.contrib.perfmark.impl.Mark;
import javax.annotation.Nullable;

/**
 * Tag is a dynamic, runtime created identifier (such as an RPC id).
 */
public final class Tag {
  static final Tag NO_TAG = new Tag();

  @Nullable final String tagName;
  final long tagId;

  private Tag() {
    this.tagName = Mark.NO_TAG_NAME;
    this.tagId = Mark.NO_TAG_ID;
  }

  Tag(long tagId) {
    this.tagName = Mark.NO_TAG_NAME;
    this.tagId = tagId;
  }

  Tag(String tagName) {
    if (tagName == null) {
      throw new NullPointerException("bad tag name");
    }
    this.tagName = tagName;
    this.tagId = Mark.NO_TAG_ID;
  }

  Tag(String tagName, long tagId) {
    if (tagName == null) {
      throw new NullPointerException("bad tag name");
    }
    this.tagName = tagName;
    this.tagId = tagId;
  }
}
