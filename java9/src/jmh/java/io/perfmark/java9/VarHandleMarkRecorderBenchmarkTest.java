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

package io.perfmark.java9;


import io.perfmark.impl.MarkRecorder;
import io.perfmark.testing.GarbageCollector;
import io.perfmark.testing.MarkHolderRecorder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@RunWith(Parameterized.class)
public class VarHandleMarkRecorderBenchmarkTest {

  @Parameterized.Parameter(0)
  public GarbageCollector gc;

  @Parameterized.Parameters
  public static Collection<Object[]> args() {
    List<Object[]> cases = new ArrayList<>();
    for (GarbageCollector gc : GarbageCollector.values()) {
      if (gc != GarbageCollector.CMS) {
        continue;
      }
      cases.add(List.of(gc).toArray(new Object[0]));
    }

    return List.copyOf(cases);
  }

  @Test
  public void markHolderBenchmark() throws Exception {
    List<String> jvmArgs = new ArrayList<>();
    jvmArgs.add("-da");
    jvmArgs.addAll(gc.jvmArgs());
    Options options = new OptionsBuilder()
        .include(VarHandleMarkHolderBenchmark.class.getCanonicalName())
        .measurementIterations(10)
        .warmupIterations(10)
        .forks(1)
        .warmupTime(TimeValue.seconds(1))
        .measurementTime(TimeValue.seconds(1))
        .param("GC", gc.name())
        .shouldFailOnError(true)
        // This is necessary to run in the IDE, otherwise it would inherit the VM args.
        .jvmArgs(jvmArgs.toArray(new String[0]))
        .build();

    new Runner(options).run();
  }

  @State(Scope.Thread)
  public static class VarHandleMarkHolderBenchmark extends MarkHolderRecorder {
    @Override
    public MarkRecorder getMarkRecorder() {
      return new SecretMarkRecorder.VarHandleMarkRecorder();
    }
  }
}
