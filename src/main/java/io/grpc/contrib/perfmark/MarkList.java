package io.grpc.contrib.perfmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class MarkList {
  static final long NO_NANOTIME = 0;

  private final List<Mark> marks;
  private final long startNanoTime;
  private final long threadId;
  private final String threadName;

  MarkList(List<Mark> marks, long startNanoTime, Thread thread) {
    this.marks = Collections.unmodifiableList(new ArrayList<>(marks));
    this.startNanoTime = startNanoTime;
    this.threadId = thread != null ? thread.getId() : -1;
    this.threadName = thread != null ? thread.getName() : "(null)";
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
    return equal(this.marks, that.marks)
        && this.startNanoTime == that.startNanoTime
        && this.threadId == that.threadId
        && equal(this.threadName, that.threadName);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{marks, startNanoTime, threadId, threadName});
  }

  @Override
  public String toString() {
    return "MarkList{"
        + "marks=" + marks + ", "
        + "startNanoTime=" + startNanoTime + ", "
        + "threadId=" + threadId + ", "
        + "threadName=" + threadName + "}";
  }

  public static final class Mark {
    @Nullable private final String taskName;
    @Nullable private final Marker marker;

    private final String tagName;
    private final long tagId;
    private final long linkId;
    private final long nanoTime;
    private final long generation;
    private final Operation operation;

    Mark(
        Object taskNameOrMarker,
        @Nullable String tagName,
        long tagId,
        long nanoTime,
        long generation,
        Operation operation) {
      this.operation = checkNotNull(operation, "operation");
      if (operation == Operation.NONE) {
        throw new IllegalArgumentException("bad operation");
      }
      if (taskNameOrMarker instanceof Marker) {
        this.marker = (Marker) taskNameOrMarker;
        this.taskName = null;
      } else if (taskNameOrMarker instanceof String) {
        this.marker = null;
        this.taskName = (String) taskNameOrMarker;
      } else {
        throw new IllegalArgumentException("wrong marker type " + taskNameOrMarker);
      }
      tagCheck: {
        switch (operation) {
          case TASK_START: // fall-through
          case TASK_END:
            this.tagName = tagName;
            this.tagId = tagId;
            break tagCheck;
          case TASK_NOTAG_START: // fall-through
          case TASK_NOTAG_END: // fall-through
          case LINK:
            this.tagName = Tag.NO_TAG_NAME;
            this.tagId = Tag.NO_TAG_ID;
            break tagCheck;
        }
        throw new AssertionError(String.valueOf(operation));
      }
      if (operation == Operation.LINK) {
        this.nanoTime = NO_NANOTIME;
        this.linkId = tagId;
      } else {
        this.nanoTime = nanoTime;
        this.linkId = Link.NO_LINK_ID;
      }
      this.generation = generation;
    }

    public enum Operation {
      NONE,
      TASK_START,
      TASK_NOTAG_START,
      TASK_END,
      TASK_NOTAG_END,
      LINK,
      ;

      private static final Operation[] values = Operation.values();
      static {
        assert values.length <= (1 << PerfMark.GEN_OFFSET);
      }

      static Operation valueOf(int ordinal) {
        return values[ordinal];
      }
    }

    public long getNanoTime() {
      return nanoTime;
    }

    public long getGeneration() {
      return generation;
    }

    public Operation getOperation() {
      return operation;
    }

    public long getTagId() {
      return tagId;
    }

    Marker getMarker() {
      return marker;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Mark)) {
        return false;
      }
      Mark that = (Mark) obj;
      return equal(this.taskName, that.taskName)
          && equal(this.tagName, that.tagName)
          && equal(this.tagId, that.tagId)
          && equal(this.linkId, that.linkId)
          && equal(this.marker, that.marker)
          && this.nanoTime == that.nanoTime
          && this.operation == that.operation;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(
          new Object[]{taskName, tagName, tagId, linkId, marker, nanoTime, operation});
    }

    @Override
    public String toString() {
      return "Mark{"
          + "taskName=" + taskName + ", "
          + "tagName=" + tagName + ", "
          + "tagId=" + tagId + ", "
          + "linkId=" + linkId + ", "
          + "marker=" + marker + ", "
          + "nanoTime=" + nanoTime + ", "
          + "generation=" + generation + ", "
          + "operation=" + operation + "}";
    }

    private static <T> T checkNotNull(T t, String name) {
      if (t == null) {
        throw new NullPointerException(name + " is null");
      }
      return t;
    }
  }

  static <T> boolean equal(T a, T b) {
    return a == b || a.equals(b);
  }

  static boolean equal(long a, long b) {
    return a == b;
  }
}
