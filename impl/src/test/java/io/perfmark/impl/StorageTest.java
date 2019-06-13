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
