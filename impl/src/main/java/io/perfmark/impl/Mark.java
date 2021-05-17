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

import java.util.Arrays;
import javax.annotation.Nullable;

public final class Mark {
  // TODO: make sure these match the values in Impl
  public static final String NO_TAG_NAME = "";
  public static final long NO_TAG_ID = Long.MIN_VALUE;
  public static final long NO_LINK_ID = Long.MIN_VALUE;

  public static final long NO_NANOTIME = 0;

  private static final long N0 = 0;
  private static final String S0 = null;

  private final long generation;

  private final long n1;
  private final long n2;
  private final long n3;

  @Nullable private final String s1;
  @Nullable private final String s2;
  @Nullable private final String s3;

  private final Operation operation;

  public static Mark taskStart(long generation, long nanoTime, String name) {
    return new Mark(nanoTime, N0, N0, name, S0, S0, generation, Operation.TASK_START_N1S1);
  }

  public static Mark taskStart(long generation, long nanoTime, String name, String subName) {
    return new Mark(nanoTime, N0, N0, name, subName, S0, generation, Operation.TASK_START_N1S2);
  }

  public static Mark taskEnd(long generation, long nanoTime) {
    return new Mark(nanoTime, N0, N0, S0, S0, S0, generation, Operation.TASK_END_N1S0);
  }

  public static Mark taskEnd(long generation, long nanoTime, String name) {
    return new Mark(nanoTime, N0, N0, name, S0, S0, generation, Operation.TASK_END_N1S1);
  }

  public static Mark taskEnd(long generation, long nanoTime, String name, String subName) {
    return new Mark(nanoTime, N0, N0, name, subName, S0, generation, Operation.TASK_END_N1S2);
  }

  public static Mark event(long generation, long nanoTime, String name) {
    return new Mark(nanoTime, N0, N0, name, S0, S0, generation, Operation.EVENT_N1S1);
  }

  public static Mark event(long generation, long nanoTime, String name, String subName) {
    return new Mark(nanoTime, N0, N0, name, subName, S0, generation, Operation.EVENT_N1S2);
  }

  public static Mark event(
      long generation, long nanoTime, String taskName, String tagName, long tagId) {
    return new Mark(
        nanoTime, tagId, N0, taskName, tagName, S0, generation, Operation.EVENT_N2S2);
  }

  public static Mark event(
      long generation,
      long nanoTime,
      String taskName,
      String subTaskName,
      String tagName,
      long tagId) {
    return new Mark(
        nanoTime, tagId, N0, taskName, subTaskName, tagName, generation, Operation.EVENT_N2S3);
  }

  public static Mark tag(long generation, String tagName, long tagId) {
    return new Mark(tagId, N0, N0, tagName, S0, S0, generation, Operation.TAG_N1S1);
  }

  public static Mark tag(long generation, long tagId) {
    return new Mark(tagId, N0, N0, S0, S0, S0, generation, Operation.TAG_N1S0);
  }

  public static Mark tag(long generation, String tagName) {
    return new Mark(N0, N0, N0, tagName, S0, S0, generation, Operation.TAG_N0S1);
  }

  public static Mark keyedTag(long generation, String tagName, String value) {
    return new Mark(N0, N0, N0, tagName, value, S0, generation, Operation.TAG_KEYED_N0S2);
  }

  public static Mark keyedTag(long generation, String tagName, long value) {
    return new Mark(value, N0, N0, tagName, S0, S0, generation, Operation.TAG_KEYED_N1S1);
  }

  public static Mark keyedTag(long generation, String tagName, long value0, long value1) {
    return new Mark(value0, value1, N0, tagName, S0, S0, generation, Operation.TAG_KEYED_N2S1);
  }

  public static Mark link(long generation, long linkId) {
    return new Mark(linkId, N0, N0, S0, S0, S0, generation, Operation.LINK);
  }

  public Mark withTaskName(String name) {
    switch (operation) {
      case EVENT_N1S1:
      case TASK_END_N1S1:
      case TASK_START_N1S1:
        return new Mark(n1, n2, n3, name, s2, s3, generation, operation);
      case TASK_START_N1S2:
      case TASK_END_N1S0:
      case NONE:
      case TASK_END_N1S2:
      case EVENT_N1S2:
      case EVENT_N2S2:
      case EVENT_N2S3:
      case LINK:
      case TAG_N0S1:
      case TAG_N1S0:
      case TAG_N1S1:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
      case TAG_KEYED_N0S2:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError();
  }

  private Mark(
      long n1,
      long n2,
      long n3,
      @Nullable String s1,
      @Nullable String s2,
      @Nullable String s3,
      long generation,
      Operation operation) {
    if (operation == null) {
      throw new NullPointerException("operation");
    }
    this.operation = operation;
    this.generation = generation;

    if (operation == Operation.NONE) {
      throw new IllegalArgumentException("bad operation");
    }
    this.n1 = n1;
    this.n2 = n2;
    this.n3 = n3;

    this.s1 = s1;
    this.s2 = s2;
    this.s3 = s3;
  }

  public enum OperationType {
    NONE,
    TASK_START,
    TASK_END,
    EVENT,
    LINK,
    TAG,
    ;
  }

  public enum Operation {
    NONE(OperationType.NONE, 0, 0),
    /** startTask(String taskName) 1 long for nanoTime. */
    TASK_START_N1S1(OperationType.TASK_START, 1, 1),
    /** startTask(String name, String subTaskName) 1 long for nanoTime. */
    TASK_START_N1S2(OperationType.TASK_START, 1, 2),

    TASK_END_N1S0(OperationType.TASK_END, 1, 0),
    TASK_END_N1S1(OperationType.TASK_END, 1, 1),
    TASK_END_N1S2(OperationType.TASK_END, 1, 2),

    EVENT_N1S1(OperationType.EVENT, 1, 1),
    EVENT_N1S2(OperationType.EVENT, 1, 2),
    /** Tagged event, since attach tags can't apply to events */
    EVENT_N2S2(OperationType.EVENT, 2, 2),
    /** Tagged event, since attach tags can't apply to events */
    EVENT_N2S3(OperationType.EVENT, 2, 3),

    LINK(OperationType.LINK, 1, 0),

    /** An unkeyed tag that has a single string value. */
    TAG_N0S1(OperationType.TAG, 0, 1),
    /** An unkeyed tag that has a single numeric value. */
    TAG_N1S0(OperationType.TAG, 1, 0),
    /**
     * An unkeyed tag that has a string and numeric value. The values are unrelated to each other.
     */
    TAG_N1S1(OperationType.TAG, 1, 1),

    TAG_KEYED_N1S1(OperationType.TAG, 1, 1),

    TAG_KEYED_N2S1(OperationType.TAG, 2, 1),

    TAG_KEYED_N0S2(OperationType.TAG, 0, 2),
    ;

    private final OperationType opType;
    private final int longs;
    private final int strings;

    Operation(OperationType opType, int longs, int strings) {
      this.opType = opType;
      this.longs = longs;
      this.strings = strings;
      assert longs <= maxNumbers();
      assert strings <= maxStrings();
    }

    private static final Operation[] values = Operation.values();

    static {
      assert values.length <= (1 << Generator.GEN_OFFSET);
    }

    public OperationType getOpType() {
      return opType;
    }

    public int getNumbers() {
      return longs;
    }

    public int getStrings() {
      return strings;
    }

    public static int maxNumbers() {
      return 2;
    }

    public static int maxStrings() {
      return 3;
    }

    public static int maxMarkers() {
      return 1;
    }

    public static Operation valueOf(int code) {
      return values[code];
    }
  }

  public long getNanoTime() {
    switch (operation.opType) {
      case TASK_START:
      case TASK_END:
      case EVENT:
        return n1;
      case NONE:
      case LINK:
      case TAG:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation.opType);
  }

  public long getGeneration() {
    return generation;
  }

  public Operation getOperation() {
    return operation;
  }

  public String getTagStringValue() {
    switch (operation) {
      case TAG_N0S1:
      case TAG_N1S1:
        return s1;
      case TAG_KEYED_N0S2:
      case EVENT_N2S2:
        return s2;
      case EVENT_N2S3:
        return s3;
      case TAG_N1S0:
      case NONE:
      case TASK_START_N1S1:
      case TASK_START_N1S2:
      case TASK_END_N1S0:
      case TASK_END_N1S1:
      case TASK_END_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
      case LINK:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation.opType);
  }

  public long getTagFirstNumeric() {
    switch (operation) {
      case TAG_N1S0:
      case TAG_N1S1:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
        return n1;
      case EVENT_N2S2:
      case EVENT_N2S3:
        return n2;
      case TAG_N0S1:
      case TAG_KEYED_N0S2:
      case NONE:
      case TASK_START_N1S1:
      case TASK_START_N1S2:
      case TASK_END_N1S0:
      case TASK_END_N1S1:
      case TASK_END_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case LINK:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation.opType);
  }

  public long getTagSecondNumeric() {
    switch (operation) {
      case TAG_KEYED_N2S1:
        return n2;
      case TAG_N1S0:
      case TAG_N1S1:
      case TAG_KEYED_N1S1:
      case EVENT_N2S2:
      case EVENT_N2S3:
      case TAG_N0S1:
      case TAG_KEYED_N0S2:
      case NONE:
      case TASK_START_N1S1:
      case TASK_START_N1S2:
      case TASK_END_N1S0:
      case TASK_END_N1S1:
      case TASK_END_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case LINK:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation.opType);
  }

  public String getTagKey() {
    switch (operation) {
      case TAG_KEYED_N0S2:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
        return s1;
      case TAG_N1S1:
      case TAG_N0S1:
      case TAG_N1S0:
      case NONE:
      case TASK_START_N1S1:
      case TASK_START_N1S2:
      case TASK_END_N1S0:
      case TASK_END_N1S1:
      case TASK_END_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case EVENT_N2S2:
      case EVENT_N2S3:
      case LINK:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation.opType);
  }

  public String getTaskName() {
    switch (operation) {
      case TASK_START_N1S1:
      case TASK_START_N1S2:
      case TASK_END_N1S1:
      case TASK_END_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case EVENT_N2S2:
      case EVENT_N2S3:
        return s1;
      case NONE:
      case LINK:
      case TASK_END_N1S0:
      case TAG_N0S1:
      case TAG_N1S0:
      case TAG_N1S1:
      case TAG_KEYED_N0S2:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation);
  }

  public String getSubTaskName() {
    switch (operation) {
      case TASK_END_N1S2:
      case TASK_START_N1S2:
      case EVENT_N1S2:
      case EVENT_N2S3:
        return s2;
      case TASK_START_N1S1:
      case TASK_END_N1S0:
      case TASK_END_N1S1:
      case EVENT_N1S1:
      case EVENT_N2S2:
      case NONE:
      case LINK:
      case TAG_N0S1:
      case TAG_KEYED_N0S2:
      case TAG_KEYED_N1S1:
      case TAG_KEYED_N2S1:
      case TAG_N1S0:
      case TAG_N1S1:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation);
  }

  public long getLinkId() {
    switch (operation.opType) {
      case LINK:
        return n1;
      case TASK_START:
      case TASK_END:
      case EVENT:
      case NONE:
      case TAG:
        throw new UnsupportedOperationException();
    }
    throw new AssertionError(operation.opType);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Mark)) {
      return false;
    }
    Mark that = (Mark) obj;
    return equal(this.s1, that.s1)
        && equal(this.s2, that.s2)
        && equal(this.s3, that.s3)
        && this.n1 == that.n1
        && this.n2 == that.n2
        && this.n3 == that.n3
        && this.operation == that.operation
        && this.generation == that.generation;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {operation, s1, s2, s3, n1, n2, n3, generation});
  }

  @Override
  public String toString() {
    return "Mark{"
        + "operation="
        + operation
        + ", "
        + "s1="
        + s1
        + ", "
        + "s2="
        + s2
        + ", "
        + "s3="
        + s3
        + ", "
        + "n1="
        + n1
        + ", "
        + "n2="
        + n2
        + ", "
        + "n3="
        + n3
        + ", "
        + "generation="
        + generation
        + "}";
  }

  static <T> boolean equal(T a, T b) {
    return a == b || a.equals(b);
  }
}
