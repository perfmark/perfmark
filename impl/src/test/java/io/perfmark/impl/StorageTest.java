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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StorageTest {

  @Test
  public void threadsCleanedUp() throws Exception {
    Storage.resetForTest();
    final CountDownLatch latch = new CountDownLatch(1);
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                Storage.resetForTest();
                Storage.linkAnyways(4096, 1234);
                latch.countDown();
              }
            })
        .start();

    for (int i = 10; i < 5000; i += i / 2) {
      latch.await(i, TimeUnit.MILLISECONDS);
      System.gc();
      System.runFinalization();
    }

    assertEquals(0, latch.getCount());
    List<MarkList> firstRead = Storage.read();
    assertEquals(1, firstRead.size());
    List<MarkList> secondRead = Storage.read();
    assertEquals(0, secondRead.size());
  }
}
