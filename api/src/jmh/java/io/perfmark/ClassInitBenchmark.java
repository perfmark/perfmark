/*
 * Copyright 2021 Google LLC
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@Fork(1000)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
public class ClassInitBenchmark {

  private ClassLoader loader;

  @Setup
  public void setup() {
    loader =
        new TestClassLoader(
            getClass().getClassLoader(), "io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
  }

  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public Object forName_noinit() throws Exception {
    return Class.forName(PerfMark.class.getName(), false, loader);
  }

  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public Object forName_init() throws Exception {
    return Class.forName(PerfMark.class.getName(), true, loader);
  }

  private static class TestClassLoader extends ClassLoader {

    private final List<String> classesToDrop;

    TestClassLoader(ClassLoader parent, String... classesToDrop) {
      super(parent);
      this.classesToDrop = Arrays.asList(classesToDrop);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (classesToDrop.contains(name)) {
        // Throw an exception, just like the real one would.
        throw new ClassNotFoundException();
      }
      if (!name.startsWith("io.perfmark.")) {
        return super.loadClass(name, resolve);
      }
      try (InputStream is = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
        if (is == null) {
          throw new ClassNotFoundException(name);
        }
        byte[] data = is.readAllBytes();
        Class<?> clz = defineClass(name, data, 0, data.length);
        if (resolve) {
          resolveClass(clz);
        }
        return clz;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
