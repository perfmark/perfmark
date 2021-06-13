/*
 * Copyright 2021 Carl Mastrangelo
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

package io.perfmark.testing;

import java.util.List;

public enum GarbageCollector {
  G1("-XX:+UseG1GC"),
  ZGC("-XX:+UseZGC"),
  SHENANDOAH("-XX:+UseShenandoahGC"),
  SERIAL("-XX:+UseSerialGC"),
  PARALLEL("-XX:+UseParallelGC"),
  EPSILON("-XX:+UnlockExperimentalVMOptions", "-XX:+UseEpsilonGC"),
  CMS("-XX:+UseConcMarkSweepGC"),
  ;

  private final List<String> jvmArgs;

  GarbageCollector(String ... jvmArgs) {
    this.jvmArgs = List.of(jvmArgs);
  }

  public List<String> jvmArgs() {
    return jvmArgs;
  }
}
