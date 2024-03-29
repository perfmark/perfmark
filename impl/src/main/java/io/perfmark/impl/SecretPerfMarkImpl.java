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
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
    private static final MarkRecorder markRecorder;

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
        Class<?> clz = Class.forName("io.perfmark.java7.SecretGenerator$MethodHandleGenerator");
        gen = clz.asSubclass(Generator.class).getConstructor().newInstance();
      } catch (Throwable t) {
        problems[0] = t;
      }
      if (gen == null) {
        try {
          Class<?> clz = Class.forName("io.perfmark.java9.SecretGenerator$VarHandleGenerator");
          gen = clz.asSubclass(Generator.class).getConstructor().newInstance();
        } catch (Throwable t) {
          problems[1] = t;
        }
      }
      if (gen == null) {
        try {
          Class<?> clz = Class.forName("io.perfmark.java6.SecretGenerator$VolatileGenerator");
          gen = clz.asSubclass(Generator.class).getConstructor().newInstance();
        } catch (Throwable t) {
          problems[2] = t;
        }
      }
      boolean isNoop;
      if (gen != null) {
        generator = gen;
        isNoop = false;
      } else {
        // This magic incantation avoids loading the NoopGenerator class.   When PerfMarkImpl is
        // being verified, the JVM needs to load NoopGenerator to see that it actually is a
        // Generator.  By doing a cast here, Java pushes the verification to when this branch is
        // actually taken, which is uncommon.  Avoid reflectively loading the class, which may
        // make binary shrinkers drop the NoopGenerator class.
        generator = new Generator();
        isNoop = true;
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
      problems[0] = null;
      problems[1] = null;
      problems[2] = null;
      problems[3] = null;

      MarkRecorder markRecorder0 = null;
      if (!isNoop) {
        try {
          Class<?> clz =
              Class.forName("io.perfmark.java9.SecretMarkRecorder$VarHandleMarkRecorder");
          markRecorder0 = clz.asSubclass(MarkRecorder.class).getConstructor().newInstance();
        } catch (Throwable t) {
          problems[0] = t;
        }
        if (markRecorder0 == null) {
          try {
            Class<?> clz =
                Class.forName("io.perfmark.java6.SecretMarkRecorder$SynchronizedMarkRecorder");
            markRecorder0 = clz.asSubclass(MarkRecorder.class).getConstructor().newInstance();
          } catch (Throwable t) {
            problems[1] = t;
          }
        }
      }
      if (markRecorder0 == null) {
        markRecorder0 = new MarkRecorder();
      }
      markRecorder = markRecorder0;
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
      markRecorder.start(gen, taskName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void startTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      markRecorder.start(gen, taskName);
    }

    @Override
    protected void startTask(String taskName, String subTaskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      markRecorder.start(gen, taskName, subTaskName);
    }

    /**
     * This method is needed to work with old version of perfmark-api.
     */
    protected <T> void startTask(T taskNameObject, StringFunction<? super T> stringFunction) {
      Function<? super T, String> function = stringFunction;
      startTask(taskNameObject, function);
    }

    @Override
    protected <T> void startTask(T taskNameObject, Function<? super T, String> stringFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      String taskName = deriveTaskValue(taskNameObject, stringFunction);
      markRecorder.start(gen, taskName);
    }

    @Override
    protected void stopTask() {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.stopAt(gen, nanoTime);
    }

    @Override
    protected void stopTask(String taskName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.stopAt(gen, taskName, unpackTagName(tag), unpackTagId(tag), nanoTime);
    }

    @Override
    protected void stopTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.stopAt(gen, taskName, nanoTime);
    }

    @Override
    protected void stopTask(String taskName, String subTaskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.stopAt(gen, taskName, subTaskName, nanoTime);
    }

    @Override
    protected void event(String eventName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.eventAt(gen, eventName, unpackTagName(tag), unpackTagId(tag), nanoTime);
    }

    @Override
    protected void event(String eventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.eventAt(gen, eventName, nanoTime);
    }

    @Override
    protected void event(String eventName, String subEventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long nanoTime = System.nanoTime();
      markRecorder.eventAt(gen, eventName, subEventName, nanoTime);
    }

    @Override
    protected void attachTag(Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      markRecorder.attachTag(gen, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void attachTag(String tagName, String tagValue) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      markRecorder.attachKeyedTag(gen, tagName, tagValue);
    }

    /**
     * This method is needed to work with old version of perfmark-api.
     */
    public <T> void attachTag(
        String tagName, T tagObject, StringFunction<? super T> stringFunction) {
      Function<? super T, ? extends String> function = stringFunction;
      attachTag(tagName, tagObject, function);
    }

    @Override
    protected <T> void attachTag(
        String tagName, T tagObject, Function<? super T, ? extends String> stringFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      String tagValue = deriveTagValue(tagName, tagObject, stringFunction);
      markRecorder.attachKeyedTag(gen, tagName, tagValue);
    }

    @Override
    protected <T> void attachTag(
        String tagName, T tagObject, ToIntFunction<? super T> intFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long tagValue = deriveTagValue(tagName, tagObject, intFunction);
      markRecorder.attachKeyedTag(gen, tagName, tagValue);
    }

    @Override
    protected <T> void attachTag(
        String tagName, T tagObject, ToLongFunction<? super T> longFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      long tagValue = deriveTagValue(tagName, tagObject, longFunction);
      markRecorder.attachKeyedTag(gen, tagName, tagValue);
    }

    static <T> String deriveTagValue(
        String tagName, T tagObject, Function<? super T, ? extends String> stringFunction) {
      try {
        return stringFunction.apply(tagObject);
      } catch (Throwable t) {
        handleTagValueFailure(tagName, tagObject, stringFunction, t);
        return "PerfMarkTagError:" + t.getClass().getName();
      }
    }

    static <T> long deriveTagValue(
        String tagName, T tagNameObject, ToIntFunction<? super T> intFunction) {
      try {
        // implicit cast
        return intFunction.applyAsInt(tagNameObject);
      } catch (Throwable t) {
        handleTagValueFailure(tagName, tagNameObject, intFunction, t);
        return Mark.NO_TAG_ID;
      }
    }

    static <T> long deriveTagValue(
        String tagName, T tagNameObject, ToLongFunction<? super T> longFunction) {
      try {
        return longFunction.applyAsLong(tagNameObject);
      } catch (Throwable t) {
        handleTagValueFailure(tagName, tagNameObject, longFunction, t);
        return Mark.NO_TAG_ID;
      }
    }

    static <T> String deriveTaskValue(T taskNameObject, Function<? super T, String> stringFunction) {
      try {
        return stringFunction.apply(taskNameObject);
      } catch (Throwable t) {
        handleTaskNameFailure(taskNameObject, stringFunction, t);
        return "PerfMarkTaskError:" + t.getClass().getName();
      }
    }

    static <T> void handleTagValueFailure(
        String tagName, T tagObject, Object stringFunction, Throwable cause) {
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
        T taskNameObject, Object function, Throwable cause) {
      if (logger == null) {
        return;
      }
      Logger localLogger = (Logger) logger;
      try {
        if (localLogger.isLoggable(Level.FINE)) {
          LogRecord lr =
              new LogRecord(
                  Level.FINE, "PerfMark.startTask failed: taskObject={0}, function={1}");
          lr.setParameters(new Object[] {taskNameObject, function});
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
      markRecorder.attachKeyedTag(gen, tagName, tagValue);
    }

    @Override
    protected void attachTag(String tagName, long tagValue0, long tagValue1) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      markRecorder.attachKeyedTag(gen, tagName, tagValue0, tagValue1);
    }

    @Override
    protected Tag createTag(String tagName, long tagId) {
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
      markRecorder.link(gen, linkId);
      return packLink(linkId);
    }

    @Override
    protected void linkIn(Link link) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      markRecorder.link(gen, -unpackLinkId(link));
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
