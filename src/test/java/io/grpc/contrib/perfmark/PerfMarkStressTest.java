package io.grpc.contrib.perfmark;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkStressTest {

  @Test
  public void fibonacci() {
    ForkJoinPool fjp = new ForkJoinPool(8);
    final class FibTask extends RecursiveTask<Long> {

      private final long input;
      private final Link link;

      FibTask(long input, Link link) {
        this.input = input;
        this.link = link;
      }

      @Override
      protected Long compute() {
        Tag tag = PerfMark.createTag(input);
        PerfMark.startTask("compute", tag);
        link.link();
        try {
          if (input >= 12) {
            Link link2 = PerfMark.link();
            ForkJoinTask<Long> task1 = new FibTask(input - 1, link2).fork();
            FibTask task2 = new FibTask(input - 2, link2);
            return task2.compute() + task1.join();
          } else {
            return computeUnboxed(input);
          }
        } finally {
          PerfMark.stopTask();
        }
      }

      private long computeUnboxed(long n) {
        if (n <= 1) {
          return n;
        }
        return computeUnboxed(n - 1) + computeUnboxed(n - 2);
      }
    }
    PerfMark.setEnabled(true);
    PerfMark.startTask("calc");
    Link link = PerfMark.link();
    Long res = fjp.invoke(new FibTask(49, link));
    PerfMark.stopTask();
    fjp.shutdown();
    System.err.println(res);
  }
}
