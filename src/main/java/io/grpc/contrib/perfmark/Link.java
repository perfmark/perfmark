package io.grpc.contrib.perfmark;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A link represents a linkage between asynchronous tasks.
 */
public final class Link {
  static final long NO_LINK_ID = 0;
  static final Link NONE = new Link(NO_LINK_ID);

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
    return "Link{id=" + id + "}";
  }
}
