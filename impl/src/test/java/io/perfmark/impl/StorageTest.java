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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.truth.Truth;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StorageTest {

  @Test
  public void threadsCleanedUp() throws Exception {
    Storage.resetForAll();
    Storage.registerMarkHolder(new MarkHolder() {
      @Override
      public List<MarkList> read() {
        return List.of(MarkList.newBuilder().setMarkRecorderId(1).setMarks(List.of()).setThreadName("name").build());
      }

      @Override
      public void resetForAll() {
        Storage.unregisterMarkHolder(this);
      }
    });
    List<MarkList> firstRead = Storage.read();
    assertEquals(1, firstRead.size());
    Storage.resetForAll();

    for (int i = 10; i < 5000; i += i / 2) {
      System.gc();
      System.runFinalization();
      List<MarkList> secondRead = Storage.read();
      if (secondRead.size() != 0) {
        Thread.sleep(i);
      } else{
        return;
      }
    }
    throw new AssertionError("Didn't clean up");
  }
  
  @Test
  public void customMarkHolderImpl() throws Exception {
    Class<?> clz = runWithProperty(
        System.getProperties(),
        "io.perfmark.PerfMark.markRecorderProvider",
        TestMarkRecorderProvider.class.getName(),
        () -> Class.forName(Storage.class.getName(), true, new TestClassLoader(getClass().getClassLoader())));

    Field field = clz.getDeclaredField("markRecorderProvider");
    field.setAccessible(true);
    Object value = field.get(null);
    assertNotNull(value);
    // Can't do instanceof, since class loaders are different.
    assertEquals(TestMarkRecorderProvider.class.getName(), value.getClass().getName());
  }
  @Test
  public void logEnabled() throws Exception {
    ClassLoader loader = new TestClassLoader(getClass().getClassLoader());
    List<LogRecord> logs = new ArrayList<>();
    Filter filter = record -> {
      logs.add(record);
      return true;
    };
    Logger logger = Logger.getLogger(Storage.class.getName());
    Level oldLevel = logger.getLevel();
    Filter oldFilter = logger.getFilter();
    logger.setLevel(Level.ALL);
    logger.setFilter(filter);
    try {
      runWithProperty(System.getProperties(), "io.perfmark.PerfMark.debug", "true", () -> {
        // Force Initialization.
        Class.forName(Storage.class.getName(), true, loader);
        return null;
      });
    } finally{
      logger.setFilter(oldFilter);
      logger.setLevel(oldLevel);
    }

    // This depends on the classpath being set up correctly.
    Truth.assertThat(logs).hasSize(3);
    Truth.assertThat(logs.get(0).getMessage()).contains("Error loading MarkRecorderProvider");
    Truth.assertThat(logs.get(0).getThrown()).hasMessageThat()
        .contains("io.perfmark.java9.SecretVarHandleMarkRecorderProvider$VarHandleMarkRecorderProvider");
    Truth.assertThat(logs.get(1).getMessage()).contains("Error loading MarkRecorderProvider");
    Truth.assertThat(logs.get(1).getThrown()).hasMessageThat()
        .contains("io.perfmark.java6.SecretSynchronizedMarkRecorderProvider$SynchronizedMarkRecorderProvider");
    Truth.assertThat(new SimpleFormatter().format(logs.get(2)))
        .contains("Using io.perfmark.impl.NoopMarkRecorderProvider");
  }

  public static final class TestMarkRecorderProvider extends MarkRecorderProvider {
    @Override
    public MarkRecorder createMarkRecorder(MarkRecorderRef ref) {
      throw new AssertionError();
    }
  }

  private static class TestClassLoader extends ClassLoader {

    private final List<String> classesToDrop;

    TestClassLoader(ClassLoader parent, String ... classesToDrop) {
      super(parent);
      this.classesToDrop = Arrays.asList(classesToDrop);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (classesToDrop.contains(name)) {
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

  @CanIgnoreReturnValue
  private static <T> T runWithProperty(Properties properties, String name, String value, Callable<T> runnable)
      throws Exception {
    if (properties.containsKey(name)) {
      String oldProp;
      oldProp = properties.getProperty(name);
      try {
        System.setProperty(name, value);
        return runnable.call();
      } finally{
        properties.setProperty(name, oldProp);
      }
    } else {
      try {
        System.setProperty(name, value);
        return runnable.call();
      } finally{
        properties.remove(name);
      }
    }
  }

}
