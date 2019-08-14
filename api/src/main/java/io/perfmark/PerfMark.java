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

import com.google.errorprone.annotations.CompileTimeConstant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PerfMark can be automatically enabled by setting the System property {@code
 * io.perfmark.PerfMark.startEnabled} to true.
 */
public final class PerfMark {
  private static final Tag NO_TAG = new Tag(Impl.NO_TAG_NAME, Impl.NO_TAG_ID);
  private static final Link NO_LINK = new Link(Impl.NO_LINK_ID);
  private static final Impl impl;

  static {
    Impl instance = null;
    Level level = Level.WARNING;
    Throwable err = null;
    Class<?> clz = null;
    try {
      clz = Class.forName("io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
    } catch (ClassNotFoundException e) {
      level = Level.FINE;
      err = e;
    } catch (Throwable t) {
      err = t;
    }
    if (clz != null) {
      try {
        instance = clz.asSubclass(Impl.class).getConstructor().newInstance();
      } catch (Throwable t) {
        err = t;
      }
    }
    if (instance != null) {
      impl = instance;
    } else {
      impl = new Impl();
    }
    if (err != null) {
      Logger.getLogger(PerfMark.class.getName()).log(level, "Error during PerfMark.<clinit>", err);
    }
  }

  /**
   * Turns on or off PerfMark recording. Don't call this method too frequently; while neither on nor
   * off have very high overhead, transitioning between the two may be slow.
   *
   * @param value {@code true} to enable PerfMark recording, or {@code false} to disable it.
   */
  public static void setEnabled(boolean value) {
    impl.setEnabled(value);
  }

  public static void startTask(@CompileTimeConstant String taskName, Tag tag) {
    impl.startTask(taskName, tag.tagName, tag.tagId);
  }

  public static void startTask(@CompileTimeConstant String taskName) {
    impl.startTask(taskName);
  }

  public static void event(@CompileTimeConstant String eventName, Tag tag) {
    impl.event(eventName, tag.tagName, tag.tagId);
  }

  public static void event(@CompileTimeConstant String eventName) {
    impl.event(eventName);
  }

  public static void stopTask(@CompileTimeConstant String taskName, Tag tag) {
    impl.stopTask(taskName, tag.tagName, tag.tagId);
  }

  public static void stopTask(@CompileTimeConstant String taskName) {
    impl.stopTask(taskName);
  }

  public static Tag createTag() {
    return NO_TAG;
  }

  public static Tag createTag(long id) {
    if (!impl.shouldCreateTag()) {
      return NO_TAG;
    } else {
      return new Tag(Impl.NO_TAG_NAME, id);
    }
  }

  public static Tag createTag(String name) {
    if (!impl.shouldCreateTag()) {
      return NO_TAG;
    } else {
      return new Tag(name, Impl.NO_TAG_ID);
    }
  }

  public static Tag createTag(String name, long id) {
    if (!impl.shouldCreateTag()) {
      return NO_TAG;
    } else {
      return new Tag(name, id);
    }
  }

  /**
   * A link connects between two tasks that start asynchronously. When {@link #link()} is called, an
   * association between the most recently started task and a yet to be named task on another
   * thread, is created. Links are a one-to-many relationship. A single started task can have
   * multiple associated tasks on other threads.
   *
   * @return A Link to be used in other tasks.
   */
  public static Link link() {
    long linkId = impl.linkAndGetId();
    if (linkId == Impl.NO_LINK_ID) {
      return NO_LINK;
    } else {
      return new Link(linkId);
    }
  }

  static void link(long linkId) {
    impl.link(linkId);
  }

  private PerfMark() {}
}
