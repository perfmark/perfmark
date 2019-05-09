package io.perfmark.java9;

import io.perfmark.Generator;
import io.perfmark.MarkHolder;
import io.perfmark.MarkHolderProvider;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import javax.annotation.Nullable;

final class PackageAccess {

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
  }

  public static final class VarHandleMarkHolderProvider extends MarkHolderProvider {

    @Nullable
    @Override
    public Throwable unavailabilityCause() {
      try {
        MarkHolder holder = new VarHandleMarkHolder();
        holder.start(1, "bogus", 0);
        holder.stop(1, "bogus", 0);
        int size = holder.read(true).size();
        assert size == 2;
        return null;
      } catch (Throwable t) {
        return t;
      }
    }

    @Override
    public MarkHolder create() {
      return new VarHandleMarkHolder();
    }
  }

  private PackageAccess() {
    throw new AssertionError("nope");
  }
}
