package io.grpc.contrib.perfmark;

import com.google.errorprone.annotations.CompileTimeConstant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckReturnValue;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;

public final class PerfMark {
  static final int GEN_OFFSET = 8;
  static final long INCREMENT = 1L << GEN_OFFSET;
  static final long FAILURE = (-2L) << GEN_OFFSET;

  private static final Generation generation;

  static {
    Generation gen = null;
    Throwable failure = null;
    try {
      gen = MethodHandleGeneration.INSTANCE;
    } catch (Throwable t) {
      failure = t;
      gen = VolatileGeneration.INSTANCE;
    } finally {
      generation = gen;
      if (failure != null) {
        Logger.getLogger(PerfMark.class.getName())
            .log(Level.SEVERE, "PerfMark init failure", failure);
      }
    }
  }

  private static long actualGeneration;

  abstract static class Generation {
    protected abstract void setGeneration(long generation);

    protected abstract long getGeneration();
  }

  private static final class MethodHandleGeneration extends Generation {
    private static final MutableCallSite currentGeneration =
        new MutableCallSite(MethodHandles.constant(long.class, 0));
    private static final MutableCallSite[] currentGenerations =
        new MutableCallSite[] {currentGeneration};
    private static final MethodHandle currentGenerationGetter = currentGeneration.dynamicInvoker();

    static final MethodHandleGeneration INSTANCE = new MethodHandleGeneration();

    private MethodHandleGeneration() {}

    @Override
    protected long getGeneration() {
      try {
        return (long) currentGenerationGetter.invoke();
      } catch (Throwable throwable) {
        return FAILURE;
      }
    }

    @Override
    protected void setGeneration(long generation) {
      currentGeneration.setTarget(MethodHandles.constant(long.class, generation));
      MutableCallSite.syncAll(currentGenerations);
    }
  }

  private static final class VolatileGeneration extends Generation {
    private static volatile long gen;

    static final VolatileGeneration INSTANCE = new VolatileGeneration();

    private VolatileGeneration() {}

    @Override
    protected long getGeneration() {
      return gen;
    }

    @Override
    protected void setGeneration(long generation) {
      gen = generation;
    }
  }



  /**
   * Turns on or off PerfMark recording.  Don't call this method too frequently; while neither on
   * nor off have very high overhead, transitioning between the two may be slow.
   */
  public static synchronized void setEnabled(boolean value) {
    if (isEnabled(actualGeneration) == value) {
      return;
    }
    if (actualGeneration == FAILURE) {
      return;
    }
    generation.setGeneration(actualGeneration += INCREMENT);
  }

  // For Testing
  static synchronized long getActualGeneration() {
    return actualGeneration;
  }

  public static void startTask(@CompileTimeConstant String taskName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.startAnyways(gen, taskName, Tag.NO_TAG, Marker.NONE);
  }

  public static void startTask(@CompileTimeConstant String taskName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.startAnyways(gen, taskName, tag, Marker.NONE);
  }

  public static void stopTask() {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.stopAnyways(gen, Marker.NONE);
  }

  /**
   * A link connects between two tasks that start asynchronously.  When {@link #link()} is
   * called, an association between the most recently started task and a yet to be named
   * task on another thread, is created.  Links are a one-to-many relationship.  A single
   * started task can have multiple associated tasks on other threads.
   */
  public static Link link() {
    long gen = getGen();
    if (!isEnabled(gen)) {
      return Link.NONE;
    }
    long inboundLinkId = Link.linkIdAlloc.incrementAndGet();
    PerfMarkStorage.linkAnyways(gen, inboundLinkId, Marker.NONE);
    return new Link(inboundLinkId);
  }

  @CheckReturnValue
  public static PerfMarkCloseable record(@CompileTimeConstant String taskName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return PerfMarkCloseable.NOOP;
    }
    PerfMarkStorage.startAnyways(gen, taskName, Tag.NO_TAG, Marker.NONE);
    return PerfMarkCloseable.MARKING;
  }

  @CheckReturnValue
  public static PerfMarkCloseable record(@CompileTimeConstant String taskName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return PerfMarkCloseable.NOOP;
    }
    PerfMarkStorage.startAnyways(gen, taskName, tag, Marker.NONE);
    return PerfMarkCloseable.MARKING;
  }

  public static Tag createTag() {
    return Tag.NO_TAG;
  }

  public static Tag createTag(long id) {
    if (!isEnabled(getGen())) {
      return Tag.NO_TAG;
    } else {
      return new Tag(id);
    }
  }

  public static Tag createTag(String name) {
    if (!isEnabled(getGen())) {
      return Tag.NO_TAG;
    } else {
      return new Tag(name);
    }
  }

  public static Tag createTag(long id, String name) {
    if (!isEnabled(getGen())) {
      return Tag.NO_TAG;
    } else {
      return new Tag(id, name);
    }
  }

  static void link(long linkId) {
    long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.linkAnyways(gen, -linkId, Marker.NONE);
  }

  private PerfMark() {}

  private static long getGen() {
    return generation.getGeneration();
  }

  private static boolean isEnabled(long gen) {
    return ((gen >>> GEN_OFFSET) & 0x1L) != 0L;
  }
}
