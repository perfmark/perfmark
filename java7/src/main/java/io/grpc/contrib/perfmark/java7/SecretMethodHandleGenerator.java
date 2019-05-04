package io.grpc.contrib.perfmark.java7;

import io.grpc.contrib.perfmark.impl.Generator;
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
  }

  private SecretMethodHandleGenerator() {
    throw new AssertionError("nope");
  }
}
