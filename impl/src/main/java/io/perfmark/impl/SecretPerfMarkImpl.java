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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class SecretPerfMarkImpl {

  public static final class PerfMarkImpl extends Impl {
    static final String NO_TAG_NAME = Impl.NO_TAG_NAME;
    static final Long NO_TAG_ID = Impl.NO_TAG_ID;
    static final Long NO_LINK_ID = Impl.NO_LINK_ID;
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

    public PerfMarkImpl() {
      super("I'm pretty sure I know what I'm doing");
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

    /** Returns true if sucessfully changed. */
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
    protected void startTask(String taskName, @Nullable String tagName, long tagId) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName, tagName, tagId);
    }

    @Override
    protected void startTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName);
    }

    public static void startTask(
        String taskName, @Nullable String tagName, long tagId, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName, tagName, tagId);
    }

    public static void startTask(String taskName, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName);
    }

    @Override
    protected void stopTask(String taskName, @Nullable String tagName, long tagId) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName, tagName, tagId);
    }

    @Override
    protected void stopTask(String taskName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName);
    }

    public static void stopTask(
        String taskName, @Nullable String tagName, long tagId, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName, tagName, tagId);
    }

    public static void stopTask(String taskName, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName);
    }

    @Override
    protected void event(String eventName, @Nullable String tagName, long tagId) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName, tagName, tagId);
    }

    @Override
    protected void event(String eventName) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName);
    }

    public static void event(
        String eventName, @Nullable String tagName, long tagId, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName, tagName, tagId);
    }

    public static void event(String eventName, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName);
    }

    @Override
    protected boolean shouldCreateTag() {
      return isEnabled(getGen());
    }

    @Override
    protected long linkAndGetId() {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return NO_LINK_ID;
      }
      long linkId = linkIdAlloc.getAndIncrement();
      Storage.linkAnyways(gen, linkId, Marker.NONE);
      return linkId;
    }

    public static long linkAndGetId(Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return NO_LINK_ID;
      }
      long linkId = linkIdAlloc.getAndIncrement();
      Storage.linkAnyways(gen, linkId, marker);
      return linkId;
    }

    @Override
    protected void link(long linkId) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.linkAnyways(gen, -linkId, Marker.NONE);
    }

    public static void link(long linkId, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.linkAnyways(gen, -linkId, marker);
    }

    private static long getGen() {
      return generator.getGeneration();
    }

    private static boolean isEnabled(long gen) {
      return ((gen >>> Generator.GEN_OFFSET) & 0x1L) != 0L;
    }
  }
}
