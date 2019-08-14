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

import io.perfmark.impl.Generator;

/**
 * Noop Generator for use when no other generator can be used.
 */
final class NoopGenerator extends Generator {

  NoopGenerator() {}

  @Override
  public void setGeneration(long generation) {}

  @Override
  public long getGeneration() {
    return FAILURE;
  }
}
