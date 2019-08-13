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
import io.perfmark.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
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

    @SuppressWarnings("unused") // used reflectively
    public static void startTask(String taskName, Tag tag, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName, marker, unpackTagName(tag), unpackTagId(tag));
    }

    @SuppressWarnings("unused") // used reflectively
    public static void startTask(String taskName, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.startAnyways(gen, taskName, marker);
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

    @SuppressWarnings("unused") // used reflectively
    public static void stopTask(String taskName, Tag tag, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName, marker, unpackTagName(tag), unpackTagId(tag));
    }

    @SuppressWarnings("unused") // used reflectively
    public static void stopTask(String taskName, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.stopAnyways(gen, taskName, marker);
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

    public static void event(String eventName, Tag tag, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName, marker, unpackTagName(tag), unpackTagId(tag));
    }

    public static void event(String eventName, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.eventAnyways(gen, eventName, marker);
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

    public static Link linkOut(Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return NO_LINK;
      }
      long linkId = linkIdAlloc.getAndIncrement();
      Storage.linkAnyways(gen, linkId, marker);
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

    @SuppressWarnings("unused") // Used Reflectively.
    public static void linkIn(Link link, Marker marker) {
      final long gen = getGen();
      if (!isEnabled(gen)) {
        return;
      }
      Storage.linkAnyways(gen, -unpackLinkId(link), marker);
    }

    private static long getGen() {
      return generator.getGeneration();
    }

    private static boolean isEnabled(long gen) {
      return ((gen >>> Generator.GEN_OFFSET) & 0x1L) != 0L;
    }
  }
}
