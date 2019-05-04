package io.grpc.contrib.perfmark;

import com.google.errorprone.annotations.CompileTimeConstant;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PerfMark {
  static final int GEN_OFFSET = 8;
  static final long FAILURE = (-2L) << GEN_OFFSET;
  private static final long INCREMENT = 1L << GEN_OFFSET;

  private static final Generator generator;
  private static final Logger logger;

  static {
    Generator gen = null;
    Queue<Throwable> failures = new ArrayDeque<>();
    try {
      Class<? extends Generator> clz =
          Class.forName("io.grpc.contrib.perfmark.MethodHandleGenerator")
              .asSubclass(Generator.class);
      gen = clz.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      // May happen if MethodHandleGenerator was removed from the jar.
      failures.add(e);
    } catch (NoClassDefFoundError e) {
      // May happen if MethodHandles are not available, such as on Java 6.
      failures.add(e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    if (gen == null) {
      try {
        Class<? extends Generator> clz =
            Class.forName("io.grpc.contrib.perfmark.VarHandleGenerator")
                .asSubclass(Generator.class);
        gen = clz.getDeclaredConstructor().newInstance();
      } catch (ClassNotFoundException e) {
        // May happen if VarHandleGenerator was removed from the jar.
        failures.add(e);
      } catch (NoClassDefFoundError e) {
        // May happen if VarHandles are not available, such as on Java 7.
        failures.add(e);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    if (gen == null) {
      gen = new VolatileGenerator();
    }
    generator = gen;
    // Logger creation must happen after the generator is set, incase the logger is instrumented.
    logger = Logger.getLogger(PerfMark.class.getName());
    for (Throwable failure : failures) {
      logger.log(Level.FINE, "PerfMark init failure", failure);
    }
    logger.info("Using the " + gen);
  }

  private static long actualGeneration;

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
    if (logger.isLoggable(Level.FINE)) {
      logger.fine((value ? "Enabling" : "Disabling") + " PerfMark recorder");
    }
    generator.setGeneration(actualGeneration += INCREMENT);
  }

  // For Testing
  static synchronized long getActualGeneration() {
    return actualGeneration;
  }

  public static void startTask(@CompileTimeConstant String taskName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.startAnyways(gen, taskName, tag);
  }

  public static void startTask(@CompileTimeConstant String taskName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.startAnyways(gen, taskName);
  }

  public static void stopTask(@CompileTimeConstant String taskName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.stopAnyways(gen, taskName, tag);
  }

  public static void stopTask(@CompileTimeConstant String taskName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.stopAnyways(gen, taskName);
  }

  static final class Package {
    private Package() {
      throw new AssertionError("nope");
    }

    public static final class Access {

      public static void startTask(Marker marker, Tag tag) {
        final long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.startAnyways(gen, marker, tag);
      }

      public static void startTask(Marker marker) {
        final long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.startAnyways(gen, marker);
      }

      public static void stopTask(Marker marker, Tag tag) {
        final long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.stopAnyways(gen, marker, tag);
      }

      public static void stopTask(Marker marker) {
        final long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.stopAnyways(gen, marker);
      }

      public static Link link(Marker marker) {
        long gen = getGen();
        if (!isEnabled(gen)) {
          return Link.NONE;
        }
        long inboundLinkId = Link.linkIdAlloc.incrementAndGet();
        PerfMarkStorage.linkAnyways(gen, inboundLinkId, marker);
        return new Link(inboundLinkId);
      }

      public static void link(long linkId, Marker marker) {
        long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.linkAnyways(gen, -linkId, marker);
      }

      private Access() {
        throw new AssertionError("nope");
      }
    }
  }

  public static PerfMarkCloseable record(@CompileTimeConstant String taskName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return PerfMarkCloseable.NOOP;
    }
    PerfMarkStorage.startAnyways(gen, taskName, tag);
    return new PerfMarkCloseable.TaskTagAutoCloseable(taskName, tag);
  }

  public static PerfMarkCloseable record(@CompileTimeConstant String taskName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return PerfMarkCloseable.NOOP;
    }
    PerfMarkStorage.startAnyways(gen, taskName);
    return new PerfMarkCloseable.TaskAutoCloseable(taskName);
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

  public static Tag createTag(String name, long id) {
    if (!isEnabled(getGen())) {
      return Tag.NO_TAG;
    } else {
      return new Tag(name, id);
    }
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

  static void link(long linkId) {
    long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.linkAnyways(gen, -linkId, Marker.NONE);
  }

  private PerfMark() {}

  static long getGen() {
    return generator.getGeneration();
  }

  static boolean isEnabled(long gen) {
    return ((gen >>> GEN_OFFSET) & 0x1L) != 0L;
  }
}
