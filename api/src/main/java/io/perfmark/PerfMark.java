/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.perfmark;

import static io.perfmark.impl.Generator.GEN_OFFSET;

import com.google.errorprone.annotations.CompileTimeConstant;
import io.perfmark.impl.Generator;
import io.perfmark.impl.Marker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PerfMark {
  private static final long INCREMENT = 1L << GEN_OFFSET;
  static final String START_ENABLED_PROPERTY = "io.perfmark.PerfMark.startEnabled";
  static final List<? extends String> FALLBACK_GENERATORS =
      Collections.unmodifiableList(Arrays.asList(
          "io.perfmark.java7.SecretMethodHandleGenerator$MethodHandleGenerator",
          "io.perfmark.java9.SecretVarHandleGenerator$VarHandleGenerator",
          "io.perfmark.java6.SecretVolatileGenerator$VolatileGenerator"));

  private static final Generator generator;
  private static final Logger logger;

  private static long actualGeneration;

  static {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<Generator> generators = PerfMarkStorage.getLoadable(
        errors, Generator.class, FALLBACK_GENERATORS, PerfMark.class.getClassLoader());
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

  /**
   * Turns on or off PerfMark recording.  Don't call this method too frequently; while neither on
   * nor off have very high overhead, transitioning between the two may be slow.
   *
   * @param value {@code true} to enable PerfMark recording, or {@code false} to disable it.
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

  public static void event(@CompileTimeConstant String eventName, Tag tag) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.eventAnyways(gen, eventName, tag);
  }

  public static void event(@CompileTimeConstant String eventName) {
    final long gen = getGen();
    if (!isEnabled(gen)) {
      return;
    }
    PerfMarkStorage.eventAnyways(gen, eventName);
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

      public static void event(Marker marker, Tag tag) {
        final long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.eventAnyways(gen, marker, tag);
      }

      public static void event(Marker marker) {
        final long gen = getGen();
        if (!isEnabled(gen)) {
          return;
        }
        PerfMarkStorage.eventAnyways(gen, marker);
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
   *
   * @return A Link to be used in other tasks.
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

  static void stopTaskNonConstant(String taskName) {
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
