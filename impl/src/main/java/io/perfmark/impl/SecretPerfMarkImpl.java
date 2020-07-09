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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class SecretPerfMarkImpl {

  public static final class PerfMarkImpl extends Impl {
    private static final Tag NO_TAG = packTag(Mark.NO_TAG_NAME, Mark.NO_TAG_ID);
    private static final Link NO_LINK = packLink(Mark.NO_LINK_ID);
    private static final long INCREMENT = 1L << Generator.GEN_OFFSET;

    private static final String START_ENABLED_PROPERTY = "io.perfmark.PerfMark.startEnabled";

    private static final AtomicLong linkIdAlloc = new AtomicLong(1);
    private static final Generator generator;
    private static final Logger logger;

    private static long actualGeneration;

    static {
      List<Generator> generators = new ArrayList<Generator>();
      List<Throwable> fines = new ArrayList<Throwable>();
      List<Throwable> warnings = new ArrayList<Throwable>();
      Class<?> clz = null;
      try {
        clz = Class.forName("io.perfmark.java7.SecretMethodHandleGenerator$MethodHandleGenerator");
      } catch (ClassNotFoundException e) {
        fines.add(e);
      } catch (Throwable t) {
        warnings.add(t);
      }
      if (clz != null) {
        try {
          generators.add(clz.asSubclass(Generator.class).getConstructor().newInstance());
        } catch (Throwable t) {
          warnings.add(t);
        }
        clz = null;
      }
      try {
        clz = Class.forName("io.perfmark.java9.SecretVarHandleGenerator$VarHandleGenerator");
      } catch (ClassNotFoundException e) {
        fines.add(e);
      } catch (Throwable t) {
        warnings.add(t);
      }
      if (clz != null) {
        try {
          generators.add(clz.asSubclass(Generator.class).getConstructor().newInstance());
        } catch (Throwable t) {
          warnings.add(t);
        }
        clz = null;
      }
      try {
        clz = Class.forName("io.perfmark.java6.SecretVolatileGenerator$VolatileGenerator");
      } catch (ClassNotFoundException e) {
        fines.add(e);
      } catch (Throwable t) {
        warnings.add(t);
      }
      if (clz != null) {
        try {
          generators.add(clz.asSubclass(Generator.class).getConstructor().newInstance());
        } catch (Throwable t) {
          warnings.add(t);
        }
        clz = null;
      }

      if (!generators.isEmpty()) {
        generator = generators.get(0);
      } else {
        generator = new NoopGenerator();
      }
      boolean startEnabled = false;
      try {
        startEnabled = Boolean.parseBoolean(System.getProperty(START_ENABLED_PROPERTY, "false"));
      } catch (Throwable t) {
        warnings.add(t);
      }
      boolean success = setEnabledQuiet(startEnabled);
      logger = Logger.getLogger(PerfMarkImpl.class.getName());
      logger.log(Level.FINE, "Using {0}", new Object[] {generator.getClass().getName()});

      for (Throwable error : warnings) {
        logger.log(Level.WARNING, "Error loading MarkHolderProvider", error);
      }
      for (Throwable error : fines) {
        logger.log(Level.FINE, "Error loading MarkHolderProvider", error);
      }
      logEnabledChange(startEnabled, success);
    }

    public PerfMarkImpl(Tag key) {
      super(key);
    }

    @Override
    protected synchronized void setEnabled(boolean value) {
      logEnabledChange(value, setEnabledQuiet(value));
    }

    private static synchronized void logEnabledChange(boolean value, boolean success) {
      if (success && logger.isLoggable(Level.FINE)) {
        logger.fine((value ? "Enabling" : "Disabling") + " PerfMark recorder");
      }
    }

    /** Returns true if successfully changed. */
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

    @Override
    protected void startTask(String taskName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void startTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName);
    }

    @Override
    protected void startTask(String taskName, String subTaskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName, subTaskName);
    }

    @Override
    protected <T> void startTask(T taskNameObject, StringFunction<? super T> stringFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      String taskName = deriveTaskValue(taskNameObject, stringFunction);
      Storage.startAnyways(gen, taskName);
    }

    @Override
    protected void stopTask() {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen);
    }

    @Override
    protected void stopTask(String taskName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void stopTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName);
    }

    @Override
    protected void stopTask(String taskName, String subTaskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName, subTaskName);
    }

    @Override
    protected void event(String eventName, Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void event(String eventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName);
    }

    @Override
    protected void event(String eventName, String subEventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName, subEventName);
    }

    @Override
    protected void attachTag(Tag tag) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachTagAnyways(gen, unpackTagName(tag), unpackTagId(tag));
    }

    @Override
    protected void attachTag(String tagName, String tagValue) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachKeyedTagAnyways(gen, tagName, tagValue);
    }

    @Override
    protected <T> void attachTag(
        String tagName, T tagObject, StringFunction<? super T> stringFunction) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      String tagValue = deriveTagValue(tagName, tagObject, stringFunction);
      Storage.attachKeyedTagAnyways(gen, tagName, tagValue);
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
      try {
        if (logger.isLoggable(Level.WARNING)) {
          LogRecord lr =
              new LogRecord(
                  Level.WARNING,
                  "PerfMark.attachTag ignored: tagName={0}, tagObject={1}, stringFunction={2}");
          lr.setParameters(new Object[] {tagName, tagObject, stringFunction});
          lr.setThrown(cause);
          logger.log(lr);
        }
      } catch (Throwable t) {
        // Need to be careful here.  It's possible that the Exception thrown may itself throw
        // while trying to convert it to a String.  Instead, only pass the class name, which is
        // safer than passing the whole object.
        logger.log(
            Level.WARNING,
            "PerfMark.attachTag failed for {0}: {1}",
            new Object[] {tagName, t.getClass()});
      }
    }

    static <T> void handleTaskNameFailure(
        T taskNameObject, StringFunction<? super T> stringFunction, Throwable cause) {
      try {
        if (logger.isLoggable(Level.WARNING)) {
          LogRecord lr =
              new LogRecord(
                  Level.WARNING, "PerfMark.startTask ignored: taskObject={0}, stringFunction={1}");
          lr.setParameters(new Object[] {taskNameObject, stringFunction});
          lr.setThrown(cause);
          logger.log(lr);
        }
      } catch (Throwable t) {
        // Need to be careful here.  It's possible that the Exception thrown may itself throw
        // while trying to convert it to a String.  Instead, only pass the class name, which is
        // safer than passing the whole object.
        logger.log(Level.WARNING, "PerfMark.startTask failed for {0}", new Object[] {t.getClass()});
      }
    }

    @Override
    protected void attachTag(String tagName, long tagValue) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachKeyedTagAnyways(gen, tagName, tagValue);
    }

    @Override
    protected void attachTag(String tagName, long tagValue0, long tagValue1) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.attachKeyedTagAnyways(gen, tagName, tagValue0, tagValue1);
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
      Storage.linkAnyways(gen, linkId);
      return packLink(linkId);
    }

    @Override
    protected void linkIn(Link link) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.linkAnyways(gen, -unpackLinkId(link));
    }

    private static long getGen() {
      return generator.getGeneration();
    }

    private static boolean isEnabled(long gen) {
      return ((gen >>> Generator.GEN_OFFSET) & 0x1L) != 0L;
    }
  }
}
