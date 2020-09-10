/*
 * Copyright 2020 Google LLC
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

import java.io.Closeable;

/**
 * TaskCloseable is a helper class to simplify the closing of PerfMark tasks. It should be used in a
 * try-with-resources block so that PerfMark tasks are recorded even in the event of exceptions.
 *
 * <p>Implementation note: This would normally implement {@code AutoCloseable}, but that is not
 * available in Java 6. A future version of PerfMark may implement the parent interface instead.
 *
 * <p>This class is <strong>NOT API STABLE</strong>.
 *
 * @since 0.23.0
 */
public final class TaskCloseable implements Closeable {

  static final TaskCloseable INSTANCE = new TaskCloseable();

  @Override
  public void close() {
    PerfMark.stopTask();
  }

  private TaskCloseable() {}
}
