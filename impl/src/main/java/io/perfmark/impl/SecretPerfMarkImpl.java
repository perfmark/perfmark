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

package io.perfmark.impl;

import io.perfmark.Impl;
import io.perfmark.Link;
import io.perfmark.StringFunction;
import io.perfmark.Tag;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class SecretPerfMarkImpl {

  public static final class PerfMarkImpl extends Impl {
    private static final int ENABLED_BIT_SPACE = 2;
    private static final int GEN_TIMESTAMP_SPACE = 54;
    private static final long MAX_MIBROS = (1L << GEN_TIMESTAMP_SPACE) - 1;
    private static final Tag NO_TAG = packTag(Mark.NO_TAG_NAME, Mark.NO_TAG_ID);
    private static final Link NO_LINK = packLink(Mark.NO_LINK_ID);
    private static final long INCREMENT = 1L << Generator.GEN_OFFSET;

    private static final AtomicLong linkIdAlloc = new AtomicLong(1);
    private static final Generator generator;

    // May be null if debugging is disabled.
    private static final Object logger;

    /**
     * This is the generation of the recorded tasks.  The bottom 8 bits [0-7] are reserved for opcode packing.
     * Bit 9 [8] is used for detecting if PerfMark is enabled or not.  Bit 10 [9] is unused.  Bits 11-64 [10-647]
     * are used for storing the time since Perfmark Was last / enabled or disabled.  The units are in nanoseconds/1024,
     * or (inaccurately) called mibros (like micros, but power of 2 based).
     */
    private static long actualGeneration;

    static {
      // Avoid using asserts here, because it triggers a class load of the outer SecretPerfMarkImpl.
      // See https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.10
      // assert ENABLED_BIT_SPACE + Generator.GEN_OFFSET + GEN_TIMESTAMP_SPACE <= 64;
      Generator gen = null;
      Throwable[] problems = new Throwable[4];
      // Avoid using a for-loop for this code, as it makes it easier for tools like Proguard to rewrite.
      try {
        Class<?> clz = Class.forName("io.perfmark.java7.SecretMethodHandleGenerator$MethodHandleGenerator");
        gen = clz.asSubclass(Generator.class).getConstructor().newInstance();
      } catch (Throwable t) {
        problems[0] = t;
      }
      if (gen == null) {
        try {
          Class<?> clz = Class.forName("io.perfmark.java9.SecretVarHandleGenerator$VarHandleGenerator");
          gen = clz.asSubclass(Generator.class).getConstructor().newInstance();
        } catch (Throwable t) {
          problems[1] = t;
        }
      }
      if (gen == null) {
        try {
          Class<?> clz = Class.forName("io.perfmark.java6.SecretVolatileGenerator$VolatileGenerator");
          gen = clz.asSubclass(Generator.class).getConstructor().newInstance();
        } catch (Throwable t) {
          problems[2] = t;
        }
      }
      if (gen != null) {
        generator = gen;
      } else {
        // This magic incantation avoids loading the NoopGenerator class.   When PerfMarkImpl is
        // being verified, the JVM needs to load NoopGenerator to see that it actually is a
        // Generator.  By doing a cast here, Java pushes the verification to when this branch is
        // actually taken, which is uncommon.  Avoid reflectively loading the class, which may
        // make binary shrinkers drop the NoopGenerator class.
        generator = (Generator) (Object) new NoopGenerator();
      }

      boolean startEnabled = false;
      boolean startEnabledSuccess = false;
      try {
        if ((startEnabled = Boolean.getBoolean("io.perfmark.PerfMark.startEnabled"))) {
          startEnabledSuccess = setEnabledQuiet(startEnabled, Generator.INIT_NANO_TIME);
        }
      } catch (Throwable t) {
        problems[3] = t;
      }

      Object log = null;
      try {
        if (Boolean.getBoolean("io.perfmark.PerfMark.debug")) {
          Logger localLogger = Logger.getLogger(PerfMarkImpl.class.getName());
          log = localLogger;
          for (Throwable problem : problems) {
            if (problem == null) {
              continue;
            }
            localLogger.log(Level.FINE, "Error loading Generator", problem);
          }
          localLogger.log(Level.FINE, "Using {0}", new Object[] {generator.getClass().getName()});
          logEnabledChange(startEnabled, startEnabledSuccess);
        }
      } catch (Throwable t) {
        // ignore
      }
      logger = log;
    }

    public PerfMarkImpl(Tag key) {
      super(key);
    }

    @Override
    protected synchronized void setEnabled(boolean value) {
      boolean changed = setEnabledQuiet(value, System.nanoTime());
      logEnabledChange(value, changed);
    }

    @Override
    protected synchronized boolean setEnabled(boolean value, boolean overload) {
      boolean changed = setEnabledQuiet(value, System.nanoTime());
      logEnabledChange(value, changed);
      return changed;
    }

    private static synchronized void logEnabledChange(boolean value, boolean success) {
      if (success && logger != null) {
        Logger localLogger = (Logger) logger;
        if (localLogger.isLoggable(Level.FINE)) {
          localLogger.fine((value ? "Enabling" : "Disabling") + " PerfMark recorder");
        }
      }
    }

    /** Returns true if successfully changed. */
    private static synchronized boolean setEnabledQuiet(boolean value, long now) {
      if (isEnabled(actualGeneration) == value) {
        return false;
      }
      if (actualGeneration == Generator.FAILURE) {
        return false;
      }
      long nanoDiff = now - Generator.INIT_NANO_TIME;
      generator.setGeneration(actualGeneration = nextGeneration(actualGeneration, nanoDiff));
      return true;
    }

    // VisibleForTesting
    static long nextGeneration(final long currentGeneration, final long nanosSinceInit) {
      // currentGeneration != Generator.FAILURE;
      long currentMibros = mibrosFromGeneration(currentGeneration);
      long mibrosSinceInit = Math.min(mibrosFromNanos(nanosSinceInit), MAX_MIBROS); // 54bits
      boolean nextEnabled = !isEnabled(currentGeneration);
      long nextMibros;
      if (mibrosSinceInit > currentMibros) {
        nextMibros = mibrosSinceInit;
      } else {
        nextMibros = currentMibros + (nextEnabled ? 1 : 0);
      }
      if (nextMibros > MAX_MIBROS || nextMibros < 0) {
        return Generator.FAILURE;
      }
      long enabledMask = nextEnabled ? INCREMENT : 0;
      long mibroMask = (nextMibros << (Generator.GEN_OFFSET + ENABLED_BIT_SPACE));
      // (enabledMask & mibroMask) == 0;
      return mibroMask | enabledMask;
    }

    private static long mibrosFromGeneration(long currentGeneration) {
      if (currentGeneration == Generator.FAILURE) {
        throw new IllegalArgumentException();
      }
      return currentGeneration >>> (Generator.GEN_OFFSET + ENABLED_BIT_SPACE);
    }

    private static long mibrosFromNanos(long nanos) {
      long remainder = ((1L<<(64 - GEN_TIMESTAMP_SPACE)) - 1) & nanos;
      return (nanos >>> (64 - GEN_TIMESTAMP_SPACE))
          + (remainder >= (1L<<(64 - GEN_TIMESTAMP_SPACE - 1)) ? 1 : 0);
    }

    @Override
    protected void startTask(String taskName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyway(gen, taskName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void startTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyway(gen, taskName);
    }

    @Override
    protected void startTask(String taskName, String subTaskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyway(gen, taskName, subTaskName);
    }

    @Override
    protected <T> void startTask(T taskNameObject, StringFunction<? super T> stringFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      String taskName = deriveTaskValue(taskNameObject, stringFunction);
      Storage.startAnyway(gen, taskName);
    }

    @Override
    protected void stopTask() {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyway(gen);
    }

    @Override
    protected void stopTask(String taskName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyway(gen, taskName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void stopTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyway(gen, taskName);
    }

    @Override
    protected void stopTask(String taskName, String subTaskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyway(gen, taskName, subTaskName);
    }

    @Override
    protected void event(String eventName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyway(gen, eventName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void event(String eventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyway(gen, eventName);
    }

    @Override
    protected void event(String eventName, String subEventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyway(gen, eventName, subEventName);
    }

    @Override
    protected void attachTag(Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachTagAnyway(gen, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void attachTag(String tagName, String tagValue) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachKeyedTagAnyway(gen, tagName, tagValue);
    }

    @Override
    protected <T> void attachTag(
        String tagName, T tagObject, StringFunction<? super T> stringFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      String tagValue = deriveTagValue(tagName, tagObject, stringFunction);
      Storage.attachKeyedTagAnyway(gen, tagName, tagValue);
    }

    static <T> String deriveTagValue(
        String tagName, T tagObject, StringFunction<? super T> stringFunction) {
      try {
        return stringFunction.apply(tagObject);
      } catch (Throwable t) {
        handleTagValueFailure(tagName, tagObject, stringFunction, t);
        return "PerfMarkTagError:" + t.getClass().getName();
      }
    }

    static <T> String deriveTaskValue(T taskNameObject, StringFunction<? super T> stringFunction) {
      try {
        return stringFunction.apply(taskNameObject);
      } catch (Throwable t) {
        handleTaskNameFailure(taskNameObject, stringFunction, t);
        return "PerfMarkTaskError:" + t.getClass().getName();
      }
    }

    static <T> void handleTagValueFailure(
        String tagName, T tagObject, StringFunction<? super T> stringFunction, Throwable cause) {
      if (logger == null) {
        return;
      }
      Logger localLogger = (Logger) logger;
      try {
        if (localLogger.isLoggable(Level.FINE)) {
          LogRecord lr =
              new LogRecord(
                  Level.FINE,
                  "PerfMark.attachTag failed: tagName={0}, tagObject={1}, stringFunction={2}");
          lr.setParameters(new Object[] {tagName, tagObject, stringFunction});
          lr.setThrown(cause);
          localLogger.log(lr);
        }
      } catch (Throwable t) {
        // Need to be careful here.  It's possible that the Exception thrown may itself throw
        // while trying to convert it to a String.  Instead, only pass the class name, which is
        // safer than passing the whole object.
        localLogger.log(
            Level.FINE,
            "PerfMark.attachTag failed for {0}: {1}",
            new Object[] {tagName, t.getClass()});
      }
    }

    static <T> void handleTaskNameFailure(
        T taskNameObject, StringFunction<? super T> stringFunction, Throwable cause) {
      if (logger == null) {
        return;
      }
      Logger localLogger = (Logger) logger;
      try {
        if (localLogger.isLoggable(Level.FINE)) {
          LogRecord lr =
              new LogRecord(
                  Level.FINE, "PerfMark.startTask failed: taskObject={0}, stringFunction={1}");
          lr.setParameters(new Object[] {taskNameObject, stringFunction});
          lr.setThrown(cause);
          localLogger.log(lr);
        }
      } catch (Throwable t) {
        // Need to be careful here.  It's possible that the Exception thrown may itself throw
        // while trying to convert it to a String.  Instead, only pass the class name, which is
        // safer than passing the whole object.
        localLogger.log(Level.FINE, "PerfMark.startTask failed for {0}", new Object[] {t.getClass()});
      }
    }

    @Override
    protected void attachTag(String tagName, long tagValue) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachKeyedTagAnyway(gen, tagName, tagValue);
    }

    @Override
    protected void attachTag(String tagName, long tagValue0, long tagValue1) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachKeyedTagAnyway(gen, tagName, tagValue0, tagValue1);
    }

    @Override
    protected Tag createTag(@Nullable String tagName, long tagId) {
      if (!isEnabled(getGen())) {
        return NO_TAG;
      }
      return packTag(tagName, tagId);
    }

    @Override
    protected Link linkOut() {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return NO_LINK;
      }
      long linkId = linkIdAlloc.getAndIncrement();
      Storage.linkAnyway(gen, linkId);
      return packLink(linkId);
    }

    @Override
    protected void linkIn(Link link) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.linkAnyway(gen, -unpackLinkId(link));
    }

    private static long getGen() {
      return generator.getGeneration();
    }

    private static boolean isEnabled(long gen) {
      return ((gen >>> Generator.GEN_OFFSET) & 0x1L) != 0L;
    }
  }

  private SecretPerfMarkImpl() {}
}
