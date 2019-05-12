package io.perfmark;

import static io.perfmark.impl.Generator.GEN_OFFSET;

import com.google.errorprone.annotations.CompileTimeConstant;
import io.perfmark.impl.Generator;
import io.perfmark.impl.Marker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PerfMark {
  private static final long INCREMENT = 1L << GEN_OFFSET;
  static final String START_ENABLED_PROPERTY = "io.perfmark.PerfMark.startEnabled";
  static final List<String> FALLBACK_GENERATORS =
      Collections.unmodifiableList(Arrays.asList(
          "io.perfmark.java7.SecretMethodHandleGenerator$MethodHandleGenerator",
          "io.perfmark.java9.SecretVarHandleGenerator$VarHandleGenerator",
          "io.perfmark.java6.SecretVolatileGenerator$VolatileGenerator"));

  private static final Generator generator;
  private static final Logger logger;

  private static long actualGeneration;

  static {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<Generator> generators =
        getLoadable(errors, Generator.class, FALLBACK_GENERATORS, PerfMark.class.getClassLoader());
    Level level;
    if (generators.isEmpty()) {
      generator = new NoopGenerator();
      level = Level.WARNING;
    } else {
      generator = generators.get(0);
      level = Level.FINE;
    }
    boolean startEnabled = false;
    try {
      startEnabled =
          Boolean.parseBoolean(System.getProperty(START_ENABLED_PROPERTY, "false"));
    } catch (RuntimeException e) {
      errors.add(e);
    } catch (Error e) {
      errors.add(e);
    }
    boolean success = setEnabledQuiet(startEnabled);
    logger = Logger.getLogger(PerfMark.class.getName());
    logger.log(level, "Using {0}", new Object[] {generator.getClass()});
    logEnabledChange(startEnabled, success);
    for (Throwable error : errors) {
      logger.log(level, "Error encountered loading generator", error);
    }
  }

  // @VisibleForTesting
  static <T> List<T> getLoadable(
      List<? super Throwable> errors,
      Class<T> clz,
      List<? extends String> fallbackClassNames,
      ClassLoader cl) {
    Map<Class<? extends T>, T> loadables = new LinkedHashMap<Class<? extends T>, T>();
    List<Throwable> serviceLoaderErrors = new ArrayList<Throwable>();
    try {
      ServiceLoader<T> loader = ServiceLoader.load(clz, cl);
      Iterator<T> it = loader.iterator();
      for (int i = 0; i < 10 && serviceLoaderErrors.size() < 10; i++) {
        try {
          if (it.hasNext()) {
            T next = it.next();
            Class<? extends T> subClz = next.getClass().asSubclass(clz);
            if (!loadables.containsKey(subClz)) {
              loadables.put(subClz, next);
            }
          } else {
            break;
          }
        } catch (ServiceConfigurationError sce) {
          if (!serviceLoaderErrors.isEmpty()) {
            Throwable last = serviceLoaderErrors.get(serviceLoaderErrors.size() - 1);
            if (errorsEqual(sce, last)) {
              continue;
            }
          }
          serviceLoaderErrors.add(sce);
        }
      }
    } catch (ServiceConfigurationError sce) {
      serviceLoaderErrors.add(sce);
    } finally {
      errors.addAll(serviceLoaderErrors);
    }
    for (String fallbackClassName : fallbackClassNames) {
      try {
        Class<?> fallbackClz = Class.forName(fallbackClassName, true, cl);
        if (!loadables.containsKey(fallbackClz)) {
          Class<? extends T> subClz = fallbackClz.asSubclass(clz);
          loadables.put(subClz, subClz.getDeclaredConstructor().newInstance());
        }
      } catch (Throwable t) {
        errors.add(t);
      }
    }
    return Collections.unmodifiableList(new ArrayList<T>(loadables.values()));
  }

  /**
   * Turns on or off PerfMark recording.  Don't call this method too frequently; while neither on
   * nor off have very high overhead, transitioning between the two may be slow.
   */
  public static synchronized void setEnabled(boolean value) {
    logEnabledChange(value, setEnabledQuiet(value));
  }

  private static synchronized void logEnabledChange(boolean value, boolean success) {
    if (success && logger.isLoggable(Level.FINE)) {
      logger.fine((value ? "Enabling" : "Disabling") + " PerfMark recorder");
    }
  }

  /**
   * Returns true if sucessfully changed.
   */
  private static synchronized boolean setEnabledQuiet(boolean value) {
    if (isEnabled(actualGeneration) == value) {
      return false;
    }
    if (actualGeneration == Generator.FAILURE) {
      return false;
    }
    generator.setGeneration(actualGeneration += INCREMENT);
    return true;
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

  @SuppressWarnings("ReferenceEquality") // No Java 8 yet
  private static boolean errorsEqual(Throwable t1, Throwable t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }
    if (t1.getClass() == t2.getClass()) {
      String msg1 = t1.getMessage();
      String msg2 = t2.getMessage();
      if (msg1 == msg2 || (msg1 != null && msg1.equals(msg2))) {
        if (Arrays.equals(t1.getStackTrace(), t2.getStackTrace())) {
          return errorsEqual(t1.getCause(), t2.getCause());
        }
      }
    }
    return false;
  }
}
