/*
 * Copyright 2019 Carl Mastrangelo
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

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Marker {

  public static final Marker NONE = new Marker(null);

  @Nullable final StackTraceElement location;

  Marker(@Nullable StackTraceElement location) {
    this.location = location;
  }

  @Override
  public String toString() {
    return "Marker{" + location + "}";
  }

  @Override
  public int hashCode() {
    return location == null ? 31 : location.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Marker)) {
      return false;
    }
    Marker other = (Marker) obj;
    if (this.location == null) {
      return other.location == null;
    }
    return this.location.equals(other.location);
  }
}
