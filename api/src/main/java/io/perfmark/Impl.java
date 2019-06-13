package io.perfmark;

import javax.annotation.Nullable;

public class Impl {

  protected static final String NO_TAG_NAME = "";
  protected static final long NO_TAG_ID = Long.MIN_VALUE;
  /**
   * This value is current {@link Long#MIN_VALUE}, but it could also be {@code 0}. The invariant
   * {@code NO_LINK_ID == -NO_LINK_ID} must be maintained to work when PerfMark is disabled.
   */
  protected static final long NO_LINK_ID = Long.MIN_VALUE;

  /** The Noop implementation */
  protected Impl() {}

  protected void setEnabled(boolean value) {}

  protected void startTask(String taskName, Tag tag) {}

  protected void startTask(String taskName) {}

  protected void event(String eventName, Tag tag) {}

  protected void event(String eventName) {}

  protected void stopTask(String taskName, Tag tag) {}

  protected void stopTask(String taskName) {}

  protected boolean shouldCreateTag() {
    return false;
  }

  protected long linkAndGetId() {
    return NO_LINK_ID;
  }

  protected void link(long linkId) {}

  @Nullable
  protected static final String tagName(Tag tag) {
    return tag.tagName;
  }

  protected static final long tagId(Tag tag) {
    return tag.tagId;
  }
}
