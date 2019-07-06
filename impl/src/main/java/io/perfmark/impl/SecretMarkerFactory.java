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

final class SecretMarkerFactory {

  public static final class MarkerFactory {
    public static Marker createMarker(@Nullable StackTraceElement location) {
      return new Marker(location);
    }

    private MarkerFactory() {
      throw new AssertionError("nope");
    }
  }

  private SecretMarkerFactory() {
    throw new AssertionError("nope");
  }
}
