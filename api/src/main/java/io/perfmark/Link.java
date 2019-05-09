package io.perfmark;

import io.perfmark.impl.Mark;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A link represents a linkage between asynchronous tasks.
 */
public final class Link {
  static final Link NONE = new Link(Mark.NO_LINK_ID);

  static final AtomicLong linkIdAlloc = new AtomicLong();

  private final long id;

  Link(long linkId) {
    this.id = linkId;
  }

  public void link() {
    PerfMark.link(id);
  }

  // For Testing
  long getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Link(" + id + ")";
  }

  /**
   * Equality on Links is not well defined, since the created tag can depend on if PerfMark was
   * enabled at the time of creation.
   */
  @Override
  @Deprecated // Don't use equality on links
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
