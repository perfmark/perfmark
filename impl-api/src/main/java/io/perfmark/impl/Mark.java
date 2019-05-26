package io.perfmark.impl;

import java.util.Arrays;
import javax.annotation.Nullable;

public final class Mark {
  public static final long NO_LINK_ID = 0;
  public static final long NO_TAG_ID = Long.MIN_VALUE;
  public static final String NO_TAG_NAME = null;
  public static final long NO_NANOTIME = 0;

  @Nullable private final String taskName;
  @Nullable private final Marker marker;
  @Nullable private final String tagName;
  private final long tagId;
  private final long linkId;
  private final long nanoTime;
  private final long generation;
  private final Operation operation;

  public static Mark create(
      String taskName,
      @Nullable String tagName,
      long tagId,
      long nanoTime,
      long generation,
      Operation operation) {
    return new Mark(taskName, tagName, tagId, nanoTime, generation, operation);
  }

  public static Mark create(
      Marker marker,
      @Nullable String tagName,
      long tagId,
      long nanoTime,
      long generation,
      Operation operation) {
    return new Mark(marker, tagName, tagId, nanoTime, generation, operation);
  }

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
        case TASK_END: // fall-through
        case EVENT:
          this.tagName = tagName;
          this.tagId = tagId;
          break tagCheck;
        case TASK_NOTAG_START: // fall-through
        case TASK_NOTAG_END: // fall-through
        case EVENT_NOTAG: // fall-through
        case LINK:
          this.tagName = NO_TAG_NAME;
          this.tagId = NO_TAG_ID;
          break tagCheck;
        case NONE:
          // fall-through
      }
      throw new AssertionError(String.valueOf(operation));
    }
    if (operation == Operation.LINK) {
      this.nanoTime = NO_NANOTIME;
      this.linkId = tagId;
    } else {
      this.nanoTime = nanoTime;
      this.linkId = NO_LINK_ID;
    }
    this.generation = generation;
  }

  public enum Operation {
    NONE,
    TASK_START,
    TASK_NOTAG_START,
    TASK_END,
    TASK_NOTAG_END,
    EVENT,
    EVENT_NOTAG,
    LINK,
    ;

    private static final Operation[] values = Operation.values();
    static {
      assert values.length <= (1 << Generator.GEN_OFFSET);
    }

    public static Operation valueOf(int ordinal) {
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

  @Nullable
  public String getTagName() {
    return tagName;
  }

  public long getTagId() {
    return tagId;
  }

  @Nullable
  public Marker getMarker() {
    return marker;
  }

  @Nullable
  public String getTaskName() {
    return taskName;
  }

  public long getLinkId() {
    return linkId;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Mark)) {
      return false;
    }
    Mark that = (Mark) obj;
    return equal(this.taskName, that.taskName)
        && equal(this.marker, that.marker)
        && equal(this.tagName, that.tagName)
        && this.tagId == that.tagId
        && this.linkId == that.linkId
        && this.nanoTime == that.nanoTime
        && this.operation == that.operation;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(
        new Object[] {taskName, tagName, tagId, linkId, marker, nanoTime, operation});
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

  static <T> boolean equal(T a, T b) {
    return a == b || a.equals(b);
  }
}
