package io.grpc.contrib.perfmark;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * This class let's PerfMark have fairly low overhead detection if it is enabled, with reasonable
 * time between enabled and other threads noticing.  Since this uses Java 9 APIs, it may not be
 * available.
 */
final class VarHandleGenerator extends Generator {

  private static final VarHandle genHandle;

  static {
    try {
      genHandle = MethodHandles.lookup().findVarHandle(VarHandleGenerator.class, "gen", long.class);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  VarHandleGenerator() {}

  private long gen;

  @Override
  protected void setGeneration(long generation) {
    genHandle.setOpaque(this, generation);
  }

  @Override
  protected long getGeneration() {
    return (long) genHandle.getOpaque(this);
  }
}
