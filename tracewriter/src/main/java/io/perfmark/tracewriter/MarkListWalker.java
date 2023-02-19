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

  static final String UNKNOWN_TASK_NAME = "(unknown)";

  // TODO: make sure the generations dont have any timestamp overlap
  final void walk(List<? extends MarkList> markLists, long nowNanoTime) {
    Map<Long, List<MarkList>> generationToMarkLists = groupMarkListsByGeneration(markLists);
    for (Map.Entry<Long, List<MarkList>> entry : generationToMarkLists.entrySet()) {
      enterGeneration(entry.getKey());
      for (MarkList markList : entry.getValue()) {
        enterMarkList(
            markList.getThreadName(), markList.getThreadId(), markList.getMarkRecorderId());
        Deque<Mark> fakeStarts = new ArrayDeque<>();
        Deque<Mark> fakeEnds = new ArrayDeque<>();
        Set<Mark> unmatchedPairMarks =
            Collections.newSetFromMap(new IdentityHashMap<Mark, Boolean>());
        createFakes(fakeStarts, fakeEnds, unmatchedPairMarks, markList, nowNanoTime);
        for (Mark mark : fakeStarts) {
          onTaskStart(mark, true, false);
        }
        for (Mark mark : markList) {
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

  protected void enterMarkList(String threadName, long threadId, long markRecorderId) {}

  protected void exitMarkList() {}

  private void onRealMark(Mark mark, Collection<Mark> unmatchedPairMarks) {
    switch (mark.getOperation().getOpType()) {
      case TASK_START:
        onTaskStart(mark, false, unmatchedPairMarks.contains(mark));
        return;
      case TASK_END:
        onTaskEnd(mark, unmatchedPairMarks.contains(mark), false);
        return;
      case TAG:
        onAttachTag(mark);
        return;
      case EVENT:
        onEvent(mark);
        return;
      case LINK:
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

  protected void onAttachTag(Mark mark) {}

  private static Map<Long, List<MarkList>> groupMarkListsByGeneration(
      List<? extends MarkList> markLists) {
    Map<Long, List<MarkList>> generationToMarkLists = new TreeMap<>();
    for (MarkList markList : markLists) {
      if (markList.isEmpty()) {
        continue;
      }
      Map<Long, List<Mark>> generationToMarks = new TreeMap<>();
      for (Mark mark : markList) {
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
      switch (mark.getOperation().getOpType()) {
        case TASK_START:
          unmatchedMarks.addLast(mark);
          continue loop;
        case TASK_END:
          if (!unmatchedMarks.isEmpty()) {
            // TODO: maybe double check the tags and task names match
            unmatchedMarks.removeLast();
          } else {
            fakeStarts.addFirst(createFakeStart(mark, nanoTimeBounds[0]));
            unmatchedPairMarks.add(mark);
          }
          continue loop;
        case EVENT:
        case LINK:
        case TAG:
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
    switch (mark.getOperation().getOpType()) {
      case TASK_START:
      case TASK_END:
      case EVENT:
        if (mark.getNanoTime() - nanoTimeBounds[0] < 0) {
          nanoTimeBounds[0] = mark.getNanoTime();
        }
        if (mark.getNanoTime() - nanoTimeBounds[1] > 0) {
          nanoTimeBounds[1] = mark.getNanoTime();
        }
        return;
      case LINK:
      case TAG:
        return;
      case NONE:
        break;
    }
    throw new AssertionError();
  }

  private static Mark createFakeEnd(Mark start, long lastNanoTime) {
    switch (start.getOperation()) {
      case TASK_START_N1S1:
        return Mark.taskEnd(start.getGeneration(), lastNanoTime, start.getTaskName());
      case TASK_START_N1S2:
        return Mark.taskEnd(
            start.getGeneration(), lastNanoTime, start.getTaskName(), start.getSubTaskName());
      case TASK_END_N1S0:
      case TASK_END_N1S1:
      case TASK_END_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case EVENT_N2S2:
      case EVENT_N2S3:
      case LINK:
      case TAG_N0S1:
      case TAG_KEYED_N0S2:
      case TAG_KEYED_N2S1:
      case TAG_KEYED_N1S1:
      case TAG_N1S0:
      case TAG_N1S1:
      case NONE:
        break;
    }
    throw new AssertionError(start.getOperation());
  }

  private static Mark createFakeStart(Mark end, long firstNanoTime) {
    switch (end.getOperation()) {
      case TASK_END_N1S0:
        return Mark.taskStart(end.getGeneration(), firstNanoTime, UNKNOWN_TASK_NAME);
      case TASK_END_N1S1:
        return Mark.taskStart(end.getGeneration(), firstNanoTime, end.getTaskName());
      case TASK_END_N1S2:
        return Mark.taskStart(
            end.getGeneration(), firstNanoTime, end.getTaskName(), end.getSubTaskName());
      case NONE:
      case TASK_START_N1S1:
      case TASK_START_N1S2:
      case EVENT_N1S1:
      case EVENT_N1S2:
      case EVENT_N2S2:
      case EVENT_N2S3:
      case LINK:
      case TAG_N0S1:
      case TAG_KEYED_N0S2:
      case TAG_KEYED_N2S1:
      case TAG_KEYED_N1S1:
      case TAG_N1S0:
      case TAG_N1S1:
        break;
    }
    throw new AssertionError(end.getOperation());
  }
}
