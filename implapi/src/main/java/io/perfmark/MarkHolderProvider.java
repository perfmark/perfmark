package io.perfmark;

import javax.annotation.Nullable;

public abstract class MarkHolderProvider {

  public MarkHolderProvider() {}

  @Nullable
  public abstract Throwable unavailabilityCause();

  public abstract MarkHolder create();
}
