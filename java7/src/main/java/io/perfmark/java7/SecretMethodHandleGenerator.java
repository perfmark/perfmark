package io.perfmark.java7;

import io.perfmark.impl.Generator;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;

final class SecretMethodHandleGenerator {

  // UsedReflectively
  public static final class MethodHandleGenerator extends Generator {
    private static final MutableCallSite currentGeneration =
        new MutableCallSite(MethodHandles.constant(long.class, 0));
    private static final MutableCallSite[] currentGenerations =
        new MutableCallSite[] {currentGeneration};
    private static final MethodHandle currentGenerationGetter = currentGeneration.dynamicInvoker();

    public MethodHandleGenerator() {}

    @Override
    public long getGeneration() {
      try {
        return (long) currentGenerationGetter.invoke();
      } catch (Throwable throwable) {
        return FAILURE;
      }
    }

    @Override
    public void setGeneration(long generation) {
      currentGeneration.setTarget(MethodHandles.constant(long.class, generation));
      MutableCallSite.syncAll(currentGenerations);
    }

    @Override
    public long costOfGetNanos() {
      // Method handles compile to constants, so this is effectively free.
      // JMH testing on a Skylake x86_64 processor shows the cost to be about 0.3ns.
      return 0;
    }

    @Override
    public long costOfSetNanos() {
      // based on JMH testing on a Skylake x86_64 processor.
      return 2_000_000;
    }
  }

  private SecretMethodHandleGenerator() {
    throw new AssertionError("nope");
  }
}
