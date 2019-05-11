package io.perfmark.java9;

import io.perfmark.impl.Generator;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

final class SecretVarHandleGenerator {

  /**
   * This class let's PerfMark have fairly low overhead detection if it is enabled, with reasonable
   * time between enabled and other threads noticing.  Since this uses Java 9 APIs, it may not be
   * available.
   */
  public static final class VarHandleGenerator extends Generator {

    private static final VarHandle GEN;

    static {
      try {
        GEN = MethodHandles.lookup().findVarHandle(VarHandleGenerator.class, "gen", long.class);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public VarHandleGenerator() {}

    @SuppressWarnings("unused")
    private long gen;

    @Override
    public void setGeneration(long generation) {
      GEN.setOpaque(this, generation);
    }

    @Override
    public long getGeneration() {
      return (long) GEN.getOpaque(this);
    }

    @Override
    public long costOfSetNanos() {
      return 5;
    }

    @Override
    public long costOfGetNanos() {
      return 2;
    }
  }

  private SecretVarHandleGenerator() {
    throw new AssertionError("nope");
  }
}
