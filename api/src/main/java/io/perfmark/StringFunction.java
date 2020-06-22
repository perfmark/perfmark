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

/**
 * This interface is equivalent to {@code java.util.function.Function}. It is here as a
 * compatibility shim to make PerfMark compatible with Java 6. This will likely be removed if
 * PerfMark picks Java 8 as the minimum support version. See {@link PerfMark} for expected usage.
 *
 * @since 0.22.0
 * @param <T> The type to turn into a String.
 */
public interface StringFunction<T> {

  /**
   * Takes the given argument and produces a String.
   *
   * @since 0.22.0
   * @param t the subject to Stringify
   * @return the String
   */
  String apply(T t);
}
