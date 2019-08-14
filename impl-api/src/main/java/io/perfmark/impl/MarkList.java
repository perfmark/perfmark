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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public final class MarkList {

  public static Builder newBuilder() {
    return new Builder();
  }

  private final List<Mark> marks;
  private final long threadId;
  private final long markListId;
  private final String threadName;

  MarkList(Builder builder) {
    if (builder.marks == null) {
      throw new NullPointerException("marks");
    }
    this.marks = builder.marks;
    if (builder.threadName == null) {
      throw new NullPointerException("threadName");
    }
    this.threadName = builder.threadName;
    this.threadId = builder.threadId;
    this.markListId = builder.markListId;
  }

  /**
   * Returns the Marks associated with a given list.
   *
   * @return the marks, in the order they were recorded.
   */
  public List<Mark> getMarks() {
    return marks;
  }

  /**
   * Gets the Thread name of the thread that recorded the Marks.
   *
   * @return the Thread name.
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * Thread IDs can be recycled, so this is not unique.
   *
   * @return the id of the thread, as returned by {@link Thread#getId()}.
   */
  public long getThreadId() {
    return threadId;
  }

  /**
   * The globally unique ID for this Mark list.  Unlike {@link #getThreadId()}, this value is
   * never recycled.
   *
   * @return the id of this list.
   */
  public long getMarkListId() {
    return markListId;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MarkList)) {
      return false;
    }
    MarkList that = (MarkList) obj;
    return Mark.equal(this.marks, that.marks)
        && this.threadId == that.threadId
        && this.markListId == that.markListId
        && Mark.equal(this.threadName, that.threadName);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {marks, threadId, markListId, threadName});
  }

  @Override
  public String toString() {
    return "MarkList{"
        + "marks=" + marks + ", "
        + "threadId=" + threadId + ", "
        + "markListId=" + markListId + ", "
        + "threadName=" + threadName + "}";
  }

  public Builder toBuilder() {
    Builder builder = newBuilder();
    builder.marks = marks;
    return builder.setThreadName(threadName).setThreadId(threadId).setMarkListId(markListId);
  }

  public static final class Builder {

    List<Mark> marks;
    String threadName;
    long threadId;
    long markListId;

    public MarkList build() {
      return new MarkList(this);
    }

    /**
     * Sets the marks for this MarkList builder.
     *
     * @throws NullPointerException if any element in this list is {@code null}.
     * @param marks the marks to set.
     * @return this
     */
    public Builder setMarks(List<Mark> marks) {
      if (marks == null) {
        throw new NullPointerException("marks");
      }
      ArrayList<Mark> copy = new ArrayList<Mark>(marks.size());
      ListIterator<Mark> it = marks.listIterator();
      while (it.hasNext()) {
        Mark mark = it.next();
        if (mark == null) {
          throw new NullPointerException("mark is null at pos " + (it.nextIndex() - 1));
        }
        copy.add(mark);
      }
      this.marks = Collections.unmodifiableList(copy);
      return this;
    }

    /**
     * Sets the thread name for this MarkList builder.
     *
     * @param threadName the Thread Name
     * @return this
     */
    public Builder setThreadName(String threadName) {
      this.threadName = threadName;
      return this;
    }

    /**
     * Sets the thread ID for this MarkList builder.
     *
     * @param threadId the Thread Id
     * @return this
     */
    public Builder setThreadId(long threadId) {
      this.threadId = threadId;
      return this;
    }

    /**
     * Sets the mark list ID for this MarkList builder.
     *
     * @param markListId the mark list ID
     * @return this
     */
    public Builder setMarkListId(long markListId) {
      this.markListId = markListId;
      return this;
    }
  }
}
