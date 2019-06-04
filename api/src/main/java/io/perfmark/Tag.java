package io.perfmark;

import javax.annotation.Nullable;

/** Tag is a dynamic, runtime created identifier (such as an RPC id). */
public final class Tag {
  @Nullable final String tagName;
  final long tagId;

  Tag(@Nullable String tagName, long tagId) {
    // tagName should be non-null, but checking is expensive
    this.tagName = tagName;
    this.tagId = tagId;
  }
}
