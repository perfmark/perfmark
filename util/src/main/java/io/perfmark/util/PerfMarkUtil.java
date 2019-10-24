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

package io.perfmark.util;

import com.google.errorprone.annotations.MustBeClosed;
import io.perfmark.PerfMark;
import io.perfmark.Tag;
import java.util.function.Supplier;

public final class PerfMarkUtil {

  public interface CheckedRunnable<E extends Exception> {
    void run() throws E;
  }

  public static <T> T recordTaskResult(String taskName, Tag tag, Supplier<T> cmd) {
    PerfMark.tracer().startTask(taskName, tag);
    try {
      return cmd.get();
    } finally {
      PerfMark.tracer().stopTask(taskName, tag);
    }
  }

  public static <E extends Exception> void recordTask(
      String taskName, Tag tag, CheckedRunnable<E> cmd) throws E {
    PerfMark.tracer().startTask(taskName, tag);
    try {
      cmd.run();
    } finally {
      PerfMark.tracer().stopTask(taskName, tag);
    }
  }

  @MustBeClosed
  public static TaskRecorder recordTask(String taskName) {
    PerfMark.tracer().startTask(taskName);
    return () -> PerfMark.tracer().stopTask(taskName);
  }

  @MustBeClosed
  public static TaskRecorder recordTask(String taskName, Tag tag) {
    PerfMark.tracer().startTask(taskName, tag);
    return () -> PerfMark.tracer().stopTask(taskName, tag);
  }

  public interface TaskRecorder extends AutoCloseable {
    @Override
    void close();
  }

  private PerfMarkUtil() {}
}
