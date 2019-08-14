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

import static org.assertj.core.api.Assertions.assertThat;

import io.perfmark.impl.Generator;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Vector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkStorageTest {

  @Test
  public void getLoadable_usesFallBackTypes() {
    List<Throwable> errors = new ArrayList<Throwable>();
    ClassLoader cl = new ClassLoader() {
      @Override
      @SuppressWarnings("JdkObsolete")
      public Enumeration<URL> getResources(String name) {
        return new Vector<URL>().elements();
      }

      @Override
      public URL getResource(String name) {
        return null;
      }
    };

    List<Generator> res = PerfMarkStorage.getLoadable(
        errors, Generator.class, Arrays.asList(PerfMarkTest.FakeGenerator.class.getName()), cl);
    assertThat(errors).isEmpty();
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(PerfMarkTest.FakeGenerator.class);
  }

  @Test
  public void getLoadable_reportsErrorsReadingList() {
    List<Throwable> errors = new ArrayList<Throwable>();
    final IOException expected = new IOException("expected");
    ClassLoader cl = new ClassLoader() {
      @Override
      public Enumeration<URL> getResources(String name) throws IOException {
        throw expected;
      }

      @Override
      public URL getResource(String name) {
        return null;
      }
    };

    List<Generator> res = PerfMarkStorage.getLoadable(
        errors, Generator.class, Arrays.asList(PerfMarkTest.FakeGenerator.class.getName()), cl);

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0)).isInstanceOf(ServiceConfigurationError.class);
    assertThat(errors.get(0).getCause()).isSameAs(expected);
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(PerfMarkTest.FakeGenerator.class);
  }


  @Test
  public void getLoadable_reportsErrorsOnMissingClass() {
    List<Throwable> errors = new ArrayList<Throwable>();
    ClassLoader cl = new ClassLoader() {
      @Override
      @SuppressWarnings("JdkObsolete")
      public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> serviceList = PerfMarkTest.class.getClassLoader().getResources(
            PerfMarkTest.class.getName().replace('.', '/') + ".class");
        return serviceList;
      }

      @Override
      public URL getResource(String name) {
        return null;
      }
    };

    List<Generator> res = PerfMarkStorage.getLoadable(
        errors, Generator.class, Arrays.asList(PerfMarkTest.FakeGenerator.class.getName()), cl);

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0)).isInstanceOf(ServiceConfigurationError.class);
    assertThat(errors.get(0).getMessage()).contains("provider-class name");
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(PerfMarkTest.FakeGenerator.class);
  }

  @Test
  public void getLoadable_readsFromServiceLoader() {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<Generator> res = PerfMarkStorage.getLoadable(
        errors,
        Generator.class,
        Collections.<String>emptyList(),
        PerfMarkTest.class.getClassLoader());

    assertThat(errors).isEmpty();
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(PerfMarkTest.FakeGenerator.class);
  }

  @Test
  public void getLoadable_readsFromServiceLoader_dontDoubleRead() {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<Generator> res = PerfMarkStorage.getLoadable(
        errors,
        Generator.class,
        Arrays.asList(PerfMarkTest.FakeGenerator.class.getName()),
        PerfMarkTest.class.getClassLoader());

    assertThat(errors).isEmpty();
    assertThat(res).hasSize(1);
    assertThat(res.get(0)).isInstanceOf(PerfMarkTest.FakeGenerator.class);
  }
}
