package io.perfmark;

import static org.assertj.core.api.Assertions.assertThat;

import io.perfmark.impl.Generator;
import io.perfmark.impl.MarkList;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Vector;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
    Logger logger = Logger.getLogger(PerfMark.class.getName());
    Filter oldFilter = logger.getFilter();
    // This causes a cycle in case PerfMark tries to log during init.
    // Also, it silences initial nagging about missing generators.
    logger.setFilter(new Filter() {
      @Override
      public boolean isLoggable(LogRecord record) {
        PerfMark.startTask("isLoggable");
        try {
          return false;
        } finally {
          PerfMark.stopTask("isLoggable");
        }
      }
    });
    // Try to get PerfMark to accidentally log that it is enabled.  We are careful to not
    // accidentally cause class initialization early here, as START_ENABLED_PROPERTY is a
    // constant.
    String oldProperty = System.getProperty(PerfMark.START_ENABLED_PROPERTY);
    System.setProperty(PerfMark.START_ENABLED_PROPERTY, "true");
    try {
      Class.forName(PerfMark.class.getName());
    } finally {
      if (oldProperty == null) {
        System.clearProperty(PerfMark.START_ENABLED_PROPERTY);
      } else {
        System.setProperty(PerfMark.START_ENABLED_PROPERTY, oldProperty);
      }
      logger.setFilter(oldFilter);
    }
  }

  @Test
  public void getLoadable_usesFallBackAddresses() {
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

    List<Generator> res = PerfMark.getLoadable(
        errors, Generator.class, Arrays.asList(FakeGenerator.class.getName()), cl);
    assertThat(errors).isEmpty();
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(FakeGenerator.class);
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

    List<Generator> res = PerfMark.getLoadable(
        errors, Generator.class, Arrays.asList(FakeGenerator.class.getName()), cl);

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0)).isInstanceOf(ServiceConfigurationError.class);
    assertThat(errors.get(0).getCause()).isSameAs(expected);
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(FakeGenerator.class);
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

    List<Generator> res = PerfMark.getLoadable(
        errors, Generator.class, Arrays.asList(FakeGenerator.class.getName()), cl);

    assertThat(errors).hasSize(1);
    assertThat(errors.get(0)).isInstanceOf(ServiceConfigurationError.class);
    assertThat(errors.get(0).getMessage()).contains("provider-class name");
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(FakeGenerator.class);
  }

  @Test
  public void getLoadable_readsFromServiceLoader() {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<Generator> res = PerfMark.getLoadable(
        errors, Generator.class, Collections.emptyList(), PerfMarkTest.class.getClassLoader());

    assertThat(errors).isEmpty();
    assertThat(res).isNotEmpty();
    assertThat(res.get(0)).isInstanceOf(FakeGenerator.class);
  }

  @Test
  public void getLoadable_readsFromServiceLoader_dontDoubleRead() {
    List<Throwable> errors = new ArrayList<Throwable>();
    List<Generator> res = PerfMark.getLoadable(
        errors,
        Generator.class,
        Arrays.asList(FakeGenerator.class.getName()),
        PerfMarkTest.class.getClassLoader());

    assertThat(errors).isEmpty();
    assertThat(res).hasSize(1);
    assertThat(res.get(0)).isInstanceOf(FakeGenerator.class);
  }

  public static final class FakeGenerator extends Generator {

    long generation;

    @Override
    public void setGeneration(long generation) {
      this.generation = generation;
    }

    @Override
    public long getGeneration() {
      return generation;
    }
  }
}
