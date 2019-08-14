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

import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Marker {

  public static final Marker NONE = new Marker("(notask)", null);

  private final String taskName;
  private final @Nullable StackTraceElement location;

  Marker(String taskName, @Nullable StackTraceElement location) {
    if (taskName == null) {
      throw new NullPointerException("taskName");
    }
    this.taskName = taskName;
    this.location = location;
  }

  public String getTaskName() {
    return taskName;
  }

  @Override
  public String toString() {
    return "Marker{" + taskName + "," + location + "}";
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {taskName, location});
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Marker)) {
      return false;
    }
    Marker other = (Marker) obj;
    return Arrays.equals(
        new Object[] {taskName, location}, new Object[] {other.taskName, other.location});
  }
}
