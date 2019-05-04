package io.grpc.contrib.perfmark;

import static io.grpc.contrib.perfmark.impl.Generator.GEN_OFFSET;

import com.google.errorprone.annotations.CompileTimeConstant;
import io.grpc.contrib.perfmark.impl.Generator;
import io.grpc.contrib.perfmark.impl.Marker;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PerfMark {
  private static final long INCREMENT = 1L << GEN_OFFSET;

  private static final Generator generator;
  private static final Logger logger;

  static {
    Generator gen = null;
    Queue<Throwable> failures = new ArrayDeque<Throwable>();
    try {
      Class<? extends Generator> clz =
          Class.forName("io.grpc.contrib.perfmark.java7.SecretMethodHandleGenerator$MethodHandleGenerator")
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
            Class.forName("io.grpc.contrib.perfmark.java9.PackageAccess$VarHandleGenerator")
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
      try {
        Class<? extends Generator> clz =
            Class.forName("io.grpc.contrib.perfmark.java6.PackageAccess$VolatileGenerator")
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
      gen = new NoopGenerator();
    }
    generator = gen;
    // Logger creation must happen after the generator is set, incase the logger is instrumented.
    logger = Logger.getLogger(PerfMark.class.getName());
    Level level = gen instanceof NoopGenerator ? Level.WARNING : Level.INFO;
    for (Throwable failure : failures) {
      logger.log(level, "PerfMark init failure", failure);
    }
    logger.log(level, "Using {0}", new Object[] {gen.getClass()});
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
    if (actualGeneration == Generator.FAILURE) {
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

  static final class PackageAccess {
    private PackageAccess() {
      throw new AssertionError("nope");
    }

    public static final class Public {

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

      private Public() {
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

  static void stopTaskNonConstant(String taskName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.stopAnyways(gen, taskName, tag);
  }

  public static void stopTaskNonConstant(String taskName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.stopAnyways(gen, taskName);
  }

  private PerfMark() {}

  static long getGen() {
    return generator.getGeneration();
  }

  static boolean isEnabled(long gen) {
    return ((gen >>> GEN_OFFSET) & 0x1L) != 0L;
  }
}
