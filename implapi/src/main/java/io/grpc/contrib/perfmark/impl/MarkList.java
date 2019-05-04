package io.grpc.contrib.perfmark.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MarkList {

  private final List<Mark> marks;
  private final long startNanoTime;
  private final long threadId;
  private final String threadName;

  MarkList(
      List<Mark> marks,
      long startNanoTime,
      String threadName,
      long threadId) {
    this.marks = Collections.unmodifiableList(new ArrayList<Mark>(marks));
    this.startNanoTime = startNanoTime;
    if (threadName == null) {
      throw new NullPointerException("threadName");
    }
    this.threadName = threadName;
    this.threadId = threadId;
  }

  public static MarkList create(
      List<Mark> marks, long startNanoTime, String threadName, long threadId) {
    return new MarkList(marks, startNanoTime, threadName, threadId);
  }

  public List<Mark> getMarks() {
    return marks;
  }

  public long getThreadId() {
    return threadId;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MarkList)) {
      return false;
    }
    MarkList that = (MarkList) obj;
    return Mark.equal(this.marks, that.marks)
        && this.startNanoTime == that.startNanoTime
        && this.threadId == that.threadId
        && Mark.equal(this.threadName, that.threadName);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {marks, startNanoTime, threadId, threadName});
  }

  @Override
  public String toString() {
    return "MarkList{"
        + "marks=" + marks + ", "
        + "startNanoTime=" + startNanoTime + ", "
        + "threadId=" + threadId + ", "
        + "threadName=" + threadName + "}";
  }
}
