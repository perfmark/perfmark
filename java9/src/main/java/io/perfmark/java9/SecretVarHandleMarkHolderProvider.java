package io.perfmark.java9;

import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;

final class SecretVarHandleMarkHolderProvider {

  public static final class VarHandleMarkHolderProvider extends MarkHolderProvider {

    public VarHandleMarkHolderProvider() {
      // Do some basic operations to see if it works.
      MarkHolder holder = create();
      holder.start(1, "bogus", 0);
      holder.stop(1, "bogus", 0);
      int size = holder.read(true).size();
      if (size != 2) {
        throw new AssertionError("Wrong size " + size);
      }
    }

    @Override
    public MarkHolder create() {
      return new VarHandleMarkHolder();
    }
  }

  private SecretVarHandleMarkHolderProvider() {
    throw new AssertionError("nope");
  }
}
