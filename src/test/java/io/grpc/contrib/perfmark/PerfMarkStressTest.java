package io.grpc.contrib.perfmark;

import java.util.List;
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
        link.link();
        try {
          if (input >= 20) {
            Link link2 = PerfMark.link();
            ForkJoinTask<Long> task1 = new Fibonacci(input - 1, link2).fork();
            Fibonacci task2 = new Fibonacci(input - 2, link2);
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
    Long res = fjp.invoke(new Fibonacci(49, link));
    PerfMark.stopTask();
    for (MarkList markList : PerfMarkStorage.read()) {
      List<MarkList.Mark> marks = markList.getMarks();
      System.err.println("Thread " + markList.getThreadId() + " " + marks.size());
      for (int i = marks.size() - 1; i >= marks.size() - 100 && i >= 0; i--) {
        System.err.println(marks.get(i));
      }
    }
    fjp.shutdown();
  }
}
