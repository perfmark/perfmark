/*
 * Copyright 2023 Carl Mastrangelo
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

package io.perfmark.java19;

import static org.junit.Assert.assertEquals;

import io.perfmark.PerfMark;
import io.perfmark.impl.Storage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VirtualThreadTest {

  @Test
  @Ignore
  public void run() throws Exception {
    Thread.currentThread().isVirtual();

    var tf =
        Thread.ofVirtual()
            .name("bla")
            .factory();
    var exec = Executors.newThreadPerTaskExecutor(tf);
    PerfMark.setEnabled(true);

    System.err.println(java.lang.ProcessHandle.current().pid());
    int count = 10_000;
    var latch = new CountDownLatch(count);
    for (int i = 0; i < count; i++) {
      exec.execute(
          () -> {
            PerfMark.event("run");
            Runtime.getRuntime().gc();
            latch.countDown();
          });
    }

    exec.shutdown();
    latch.await(1, TimeUnit.MINUTES);

    var s = Storage.read();
    assertEquals(count, s.size());
  }
}
