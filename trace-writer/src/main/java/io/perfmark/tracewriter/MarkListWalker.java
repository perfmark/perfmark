package io.perfmark.tracewriter;

import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class MarkListWalker {
  MarkListWalker() {}

  // TODO: make sure the generations dont have any timestamp overlap
  final void walk(List<MarkList> markLists) {
    Map<Long, List<MarkList>> generationToMarkLists = groupMarkListsByGeneration(markLists);
    for (Map.Entry<Long, List<MarkList>> entry : generationToMarkLists.entrySet()) {
      enterGeneration(entry.getKey());
      for (MarkList markList : entry.getValue()) {
        enterMarkList(markList.getThreadName(), markList.getThreadId(), markList.getMarkListId());
        Deque<Mark> fakeStarts = new ArrayDeque<>();
        Deque<Mark> fakeEnds = new ArrayDeque<>();
        Set<Mark> unmatchedPairMarks =
            Collections.newSetFromMap(new IdentityHashMap<Mark, Boolean>());
        createFakes(fakeStarts, fakeEnds, unmatchedPairMarks, markList.getMarks());
        for (Mark mark : fakeStarts) {
          onTaskStart(mark, true, false);
        }
        for (Mark mark : markList.getMarks()) {
          onRealMark(mark, unmatchedPairMarks);
        }
        for (Mark mark : fakeEnds) {
          onTaskEnd(mark, false, true);
        }
        exitMarkList();
      }
      exitGeneration();
    }
  }

  protected void enterGeneration(long generation) {}

  protected void exitGeneration() {}

  protected void enterMarkList(String threadName, long threadId, long markListId) {}

  protected void exitMarkList() {}

  private void onRealMark(Mark mark, Collection<Mark> unmatchedPairMarks) {
    switch (mark.getOperation()) {
      case NONE:
        break;
      case TASK_START:
      case TASK_NOTAG_START:
        onTaskStart(mark, false, unmatchedPairMarks.contains(mark));
        return;
      case TASK_END:
      case TASK_NOTAG_END:
        onTaskEnd(mark, unmatchedPairMarks.contains(mark), false);
        return;
      case LINK:
        onLink(mark);
        return;
    }
    throw new AssertionError();
  }

  protected void onTaskStart(Mark mark, boolean unmatchedStart, boolean unmatchedEnd) {}

  protected void onTaskEnd(Mark mark, boolean unmatchedStart, boolean unmatchedEnd) {}

  protected void onLink(Mark mark) {}

  private static Map<Long, List<MarkList>> groupMarkListsByGeneration(List<MarkList> markLists) {
    Map<Long, List<MarkList>> generationToMarkLists = new TreeMap<>();
    for (MarkList markList : markLists) {
      List<Mark> marks = markList.getMarks();
      if (marks.isEmpty()) {
        continue;
      }
      Map<Long, List<Mark>> generationToMarks = new TreeMap<>();
      for (Mark mark : marks) {
        List<Mark> groupedMarks = generationToMarks.get(mark.getGeneration());
        if (groupedMarks == null) {
          generationToMarks.put(mark.getGeneration(), groupedMarks = new ArrayList<>());
        }
        groupedMarks.add(mark);
      }
      // note: marklists without any marks are lost here, since they have no generation.
      for (Map.Entry<Long, List<Mark>> entry : generationToMarks.entrySet()) {
        List<MarkList> groupedMarkLists = generationToMarkLists.get(entry.getKey());
        if (groupedMarkLists == null) {
          generationToMarkLists.put(entry.getKey(), groupedMarkLists = new ArrayList<>());
        }
        groupedMarkLists.add(markList.toBuilder().setMarks(entry.getValue()).build());
      }
    }
    // TODO: make a defensive copy of this and the sublists
    return generationToMarkLists;
  }

  private static void createFakes(
      Deque<? super Mark> fakeStarts,
      Deque<? super Mark> fakeEnds,
      Set<? super Mark> unmatchedPairMarks,
      List<Mark> marks) {
    final Deque<Mark> unmatchedMarks = new ArrayDeque<>();
    long[] nanoTimeBounds = new long[2]; // first, last
    nanoTimeBounds[0] = System.nanoTime(); // forces each subsequent overwrite to succeed.

    loop: for (Mark mark : marks) {
      setNanoTimeBounds(nanoTimeBounds, mark);
      switch (mark.getOperation()) {
        case TASK_START:
        case TASK_NOTAG_START:
          unmatchedMarks.addLast(mark);
          continue loop;
        case TASK_END:
        case TASK_NOTAG_END:
          if (!unmatchedMarks.isEmpty()) {
            // TODO: maybe double check the tags and task names match
            unmatchedMarks.removeLast();
          } else {
            fakeStarts.addFirst(createFakeStart(mark, nanoTimeBounds[0]));
            unmatchedPairMarks.add(mark);
          }
          continue loop;
        case LINK:
          continue loop;
        case NONE:
          break;
      }
      throw new AssertionError();
    }
    for (Mark unmatchedMark : unmatchedMarks) {
      fakeEnds.addFirst(createFakeEnd(unmatchedMark, nanoTimeBounds[1]));
      unmatchedPairMarks.add(unmatchedMark);
    }
    unmatchedMarks.clear();
  }

  private static void setNanoTimeBounds(long[] nanoTimeBounds, Mark mark) {
    switch (mark.getOperation()) {
      case TASK_NOTAG_START:
      case TASK_START:
      case TASK_NOTAG_END:
      case TASK_END:
        if (mark.getNanoTime() - nanoTimeBounds[0] < 0) {
          nanoTimeBounds[0] = mark.getNanoTime();
        }
        nanoTimeBounds[1] = mark.getNanoTime();
        return;
      case LINK:
        return;
      case NONE:
        break;
    }
    throw new AssertionError();
  }

  private static Mark createFakeEnd(Mark start, long lastNanoTime) {
    if (start.getMarker() != null) {
      switch (start.getOperation()) {
        case TASK_START:
          return Mark.create(
              start.getMarker(),
              start.getTagName(),
              start.getTagId(),
              lastNanoTime,
              start.getGeneration(),
              Mark.Operation.TASK_END);
        case TASK_NOTAG_START:
          return Mark.create(
              start.getMarker(),
              Mark.NO_TAG_NAME,
              Mark.NO_TAG_ID,
              lastNanoTime,
              start.getGeneration(),
              Mark.Operation.TASK_NOTAG_END);
        case LINK:
        case NONE:
        case TASK_END:
        case TASK_NOTAG_END:
          break;
      }
      throw new AssertionError();
    } else {
      String taskName = String.valueOf(start.getTaskName()); // Uses "null" if null.
      switch (start.getOperation()) {
        case TASK_START:
          return Mark.create(
              taskName,
              start.getTagName(),
              start.getTagId(),
              lastNanoTime,
              start.getGeneration(),
              Mark.Operation.TASK_END);
        case TASK_NOTAG_START:
          return Mark.create(
              taskName,
              Mark.NO_TAG_NAME,
              Mark.NO_TAG_ID,
              lastNanoTime,
              start.getGeneration(),
              Mark.Operation.TASK_NOTAG_END);
        case LINK:
        case NONE:
        case TASK_END:
        case TASK_NOTAG_END:
          break;
      }
      throw new AssertionError();
    }
  }

  private static Mark createFakeStart(Mark end, long firstNanoTime) {
    if (end.getMarker() != null) {
      switch (end.getOperation()) {
        case TASK_END:
          return Mark.create(
              end.getMarker(),
              end.getTagName(),
              end.getTagId(),
              firstNanoTime,
              end.getGeneration(),
              Mark.Operation.TASK_START);
        case TASK_NOTAG_END:
          return Mark.create(
              end.getMarker(),
              Mark.NO_TAG_NAME,
              Mark.NO_TAG_ID,
              firstNanoTime,
              end.getGeneration(),
              Mark.Operation.TASK_NOTAG_START);
        case LINK:
        case NONE:
        case TASK_START:
        case TASK_NOTAG_START:
          break;
      }
      throw new AssertionError();
    } else {
      String taskName = String.valueOf(end.getTaskName()); // Uses "null" if null.
      switch (end.getOperation()) {
        case TASK_END:
          return Mark.create(
              taskName,
              end.getTagName(),
              end.getTagId(),
              firstNanoTime,
              end.getGeneration(),
              Mark.Operation.TASK_START);
        case TASK_NOTAG_END:
          return Mark.create(
              taskName,
              Mark.NO_TAG_NAME,
              Mark.NO_TAG_ID,
              firstNanoTime,
              end.getGeneration(),
              Mark.Operation.TASK_NOTAG_START);
        case LINK:
        case NONE:
        case TASK_START:
        case TASK_NOTAG_START:
          break;
      }
      throw new AssertionError();
    }
  }
}
