package io.perfmark.impl;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public final class RecordedMarks extends AbstractList<Mark> {
  private final List<Mark> marks;
  private final long markRecorderId;

  public RecordedMarks(List<Mark> marks, long markRecorderId) {
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
    this.markRecorderId = markRecorderId;
  }

  @Override
  public Mark get(int index) {
    return marks.get(index);
  }

  @Override
  public int size() {
    return marks.size();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RecordedMarks)) {
      return false;
    }
    RecordedMarks that = (RecordedMarks) obj;
    return Mark.equal(this.marks, that.marks)
        && this.markRecorderId == that.markRecorderId;
  }

  @Override
  public int hashCode() {
    return (int)(markRecorderId ^ (markRecorderId >>> 32));
  }

  @Override
  public String toString() {
    return "RecordedMarks{"
        + "marks="
        + marks
        + ", "
        + "markRecorderId="
        + markRecorderId
        + "}";
  }
}
