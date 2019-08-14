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
import java.io.Closeable;

public abstract class PerfMarkCloseable implements Closeable {

  /**
   * {@link #close()} does not throw a checked exception.
   */
  @Override
  public abstract void close();

  static final PerfMarkCloseable NOOP = new NoopAutoCloseable();

  PerfMarkCloseable() {}

  private static final class NoopAutoCloseable extends PerfMarkCloseable {
    @Override
    public void close() {}

    NoopAutoCloseable() {}
  }

  static final class TaskTagAutoCloseable extends PerfMarkCloseable {
    private final String taskName;
    private final Tag tag;

    @Override
    public void close() {
      PerfMark.stopTaskNonConstant(taskName, tag);
    }

    TaskTagAutoCloseable(@CompileTimeConstant String taskName, Tag tag) {
      this.taskName = taskName;
      this.tag = tag;
    }
  }

  static final class TaskAutoCloseable extends PerfMarkCloseable {
    private final String taskName;

    @Override
    public void close() {
      PerfMark.stopTaskNonConstant(taskName);
    }

    TaskAutoCloseable(@CompileTimeConstant String taskName) {
      this.taskName = taskName;
    }
  }
}

