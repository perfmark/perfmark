package io.perfmark;

import javax.annotation.Nullable;

public abstract class Impl {

  protected static final String NO_TAG_NAME = null;
  protected static final long NO_TAG_ID = Long.MIN_VALUE;
  /**
   * This value is current {@link Long#MIN_VALUE}, but it could also be {@code 0}. The invariant
   * {@code NO_LINK_ID == -NO_LINK_ID} must be maintained to work when PerfMark is disabled.
   */
  protected static final long NO_LINK_ID = Long.MIN_VALUE;

  protected Impl(String password) {
    if (!"I'm pretty sure I know what I'm doing".equals(password)) {
      throw new AssertionError("You don't really know what you're doing.");
    }
  }

  protected abstract void setEnabled(boolean value);

  protected abstract void startTask(String taskName, @Nullable String tagName, long tagId);

  protected abstract void startTask(String taskName);

  protected abstract void event(String eventName, @Nullable String tagName, long tagId);

  protected abstract void event(String eventName);

  protected abstract void stopTask(String taskName, @Nullable String tagName, long tagId);

  protected abstract void stopTask(String taskName);

  protected abstract boolean shouldCreateTag();

  protected abstract long linkAndGetId();

  protected abstract void link(long linkId);
}
