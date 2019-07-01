package io.perfmark.java9;

import io.perfmark.Link;
import io.perfmark.PerfMark;
import io.perfmark.Tag;
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
    final class Fibonacci extends RecursiveTask<Long> {

      private final long input;
      private final Link link;

      Fibonacci(long input, Link link) {
        this.input = input;
        this.link = link;
      }

      @Override
      protected Long compute() {
        Tag tag = PerfMark.createTag(input);
        PerfMark.startTask("compute", tag);
        PerfMark.linkIn(link);
        try {
          if (input >= 20) {
            Link link2 = PerfMark.linkOut();
            ForkJoinTask<Long> task1 = new Fibonacci(input - 1, link2).fork();
            Fibonacci task2 = new Fibonacci(input - 2, link2);
            return task2.compute() + task1.join();
          } else {
            return computeUnboxed(input);
          }
        } finally {
          PerfMark.stopTask("compute", tag);
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
    Link link = PerfMark.linkOut();
    ForkJoinTask<Long> task = new Fibonacci(30, link);
    fjp.execute(task);
    PerfMark.stopTask("calc");
    Long res;
    try {
      res = task.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.err.println(res);

    fjp.shutdown();
  }
}
