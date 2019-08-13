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
  final void walk(List<? extends MarkList> markLists, long nowNanoTime) {
    Map<Long, List<MarkList>> generationToMarkLists = groupMarkListsByGeneration(markLists);
    for (Map.Entry<Long, List<MarkList>> entry : generationToMarkLists.entrySet()) {
      enterGeneration(entry.getKey());
      for (MarkList markList : entry.getValue()) {
        enterMarkList(markList.getThreadName(), markList.getThreadId(), markList.getMarkListId());
        Deque<Mark> fakeStarts = new ArrayDeque<>();
        Deque<Mark> fakeEnds = new ArrayDeque<>();
        Set<Mark> unmatchedPairMarks =
            Collections.newSetFromMap(new IdentityHashMap<Mark, Boolean>());
        createFakes(fakeStarts, fakeEnds, unmatchedPairMarks, markList.getMarks(), nowNanoTime);
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
      case TASK_START:
      case TASK_START_T:
      case TASK_START_M:
      case TASK_START_TM:
        onTaskStart(mark, false, unmatchedPairMarks.contains(mark));
        return;
      case TASK_END:
      case TASK_END_T:
      case TASK_END_M:
      case TASK_END_TM:
        onTaskEnd(mark, unmatchedPairMarks.contains(mark), false);
        return;
      case EVENT:
      case EVENT_T:
      case EVENT_M:
      case EVENT_TM:
        onEvent(mark);
        return;
      case LINK:
      case LINK_M:
        onLink(mark);
        return;
      case NONE:
        break;
    }
    throw new AssertionError();
  }

  protected void onTaskStart(Mark mark, boolean unmatchedStart, boolean unmatchedEnd) {}

  protected void onTaskEnd(Mark mark, boolean unmatchedStart, boolean unmatchedEnd) {}

  protected void onLink(Mark mark) {}

  protected void onEvent(Mark mark) {}

  private static Map<Long, List<MarkList>> groupMarkListsByGeneration(
      List<? extends MarkList> markLists) {
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
      List<Mark> marks,
      long nowNanoTime) {
    final Deque<Mark> unmatchedMarks = new ArrayDeque<>();
    long[] nanoTimeBounds = new long[2]; // first, last
    nanoTimeBounds[0] = nowNanoTime; // forces each subsequent overwrite to succeed.
    nanoTimeBounds[1] = nowNanoTime;

    loop:
    for (Mark mark : marks) {
      setNanoTimeBounds(nanoTimeBounds, mark);
      switch (mark.getOperation()) {
        case TASK_START:
        case TASK_START_T:
        case TASK_START_M:
        case TASK_START_TM:
          unmatchedMarks.addLast(mark);
          continue loop;
        case TASK_END:
        case TASK_END_T:
        case TASK_END_M:
        case TASK_END_TM:
          if (!unmatchedMarks.isEmpty()) {
            // TODO: maybe double check the tags and task names match
            unmatchedMarks.removeLast();
          } else {
            fakeStarts.addFirst(createFakeStart(mark, nanoTimeBounds[0]));
            unmatchedPairMarks.add(mark);
          }
          continue loop;
        case EVENT:
        case EVENT_T:
        case EVENT_M:
        case EVENT_TM:
        case LINK:
        case LINK_M:
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
      case TASK_START:
      case TASK_START_T:
      case TASK_START_M:
      case TASK_START_TM:
      case TASK_END:
      case TASK_END_T:
      case TASK_END_M:
      case TASK_END_TM:
      case EVENT:
      case EVENT_T:
      case EVENT_M:
      case EVENT_TM:
        if (mark.getNanoTime() - nanoTimeBounds[0] < 0) {
          nanoTimeBounds[0] = mark.getNanoTime();
        }
        if (mark.getNanoTime() - nanoTimeBounds[1] > 0) {
          nanoTimeBounds[1] = mark.getNanoTime();
        }
        return;
      case LINK:
      case LINK_M:
        return;
      case NONE:
        break;
    }
    throw new AssertionError();
  }

  private static Mark createFakeEnd(Mark start, long lastNanoTime) {
    Mark.Operation op;
    out:
    {
      switch (start.getOperation()) {
        case TASK_START:
          op = Mark.Operation.TASK_END;
          break out;
        case TASK_START_T:
          op = Mark.Operation.TASK_END_T;
          break out;
        case TASK_START_M:
          op = Mark.Operation.TASK_END_M;
          break out;
        case TASK_START_TM:
          op = Mark.Operation.TASK_END_TM;
          break out;
        default:
          break;
      }
      throw new AssertionError();
    }
    return Mark.create(
        String.valueOf(start.getTaskName()),
        start.getMarker(),
        start.getTagName(),
        start.getTagId(),
        lastNanoTime,
        start.getGeneration(),
        op);
  }

  private static Mark createFakeStart(Mark end, long firstNanoTime) {
    Mark.Operation op;
    out:
    {
      switch (end.getOperation()) {
        case TASK_END:
          op = Mark.Operation.TASK_START;
          break out;
        case TASK_END_T:
          op = Mark.Operation.TASK_START_T;
          break out;
        case TASK_END_M:
          op = Mark.Operation.TASK_START_M;
          break out;
        case TASK_END_TM:
          op = Mark.Operation.TASK_START_TM;
          break out;
        default:
          break;
      }
      throw new AssertionError();
    }
    return Mark.create(
        String.valueOf(end.getTaskName()),
        end.getMarker(),
        end.getTagName(),
        end.getTagId(),
        firstNanoTime,
        end.getGeneration(),
        op);
  }
}
