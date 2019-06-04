package io.perfmark;

import javax.annotation.Nullable;

final class NoopImpl extends Impl {
  NoopImpl() {
    super("I'm pretty sure I know what I'm doing");
  }

  @Override
  protected void setEnabled(boolean value) {}

  @Override
  protected void startTask(String taskName, @Nullable String tagName, long tagId) {}

  @Override
  protected void startTask(String taskName) {}

  @Override
  protected void event(String eventName, @Nullable String tagName, long tagId) {}

  @Override
  protected void event(String eventName) {}

  @Override
  protected void stopTask(String taskName, @Nullable String tagName, long tagId) {}

  @Override
  protected void stopTask(String taskName) {}

  @Override
  protected boolean shouldCreateTag() {
    return false;
  }

  @Override
  protected long linkAndGetId() {
    return NO_LINK_ID;
  }

  @Override
  protected void link(long linkId) {}
}
