package io.perfmark;

/**
 * A link represents a linkage between asynchronous tasks. A link is created inside of a started
 * task. The resulting {@link Link} object can then be passed to other asynchronous tasks to
 * associate them with the original task.
 *
 * <p>A source task may have multiple outbound links pointing to other tasks. However, calling
 * {@link Link#link()} only work if it is the first such call. Subsequent calls to {@code link()}
 * have no effect.
 */
public final class Link {

  final long linkId;

  Link(long linkId) {
    this.linkId = linkId;
  }

  /**
   * Associate this link with the most recently started task. There may be at most one inbound
   * linkage per task: the first call to {@code link()} decides which outbound task is the origin.
   */
  public void link() {
    PerfMark.link(linkId);
  }
}
