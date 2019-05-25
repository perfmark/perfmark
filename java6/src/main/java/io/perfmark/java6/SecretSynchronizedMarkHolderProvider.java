package io.perfmark.java6;

import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;

final class SecretSynchronizedMarkHolderProvider {

  public static final class SynchronizedMarkHolderProvider extends MarkHolderProvider {

    public SynchronizedMarkHolderProvider() {}

    @Override
    public MarkHolder create() {
      return new SynchronizedMarkHolder();
    }
  }

  private SecretSynchronizedMarkHolderProvider() {
    throw new AssertionError("nope");
  }
}
