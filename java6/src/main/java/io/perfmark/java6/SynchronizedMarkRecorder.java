/*
 * Copyright 2022 Google LLC
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

package io.perfmark.java6;

import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkRecorder;
import java.lang.ref.WeakReference;

final class SynchronizedMarkRecorder extends MarkRecorder {

  private static final long START_N1S1_OP = 1; // Mark.Operation.TASK_START_N1S1.ordinal();
  private static final long START_N1S2_OP = 2; // Mark.Operation.TASK_START_N1S2.ordinal();
  private static final long STOP_N1S0_OP = 3; // Mark.Operation.TASK_END_N1S0.ordinal();
  private static final long STOP_N1S1_OP = 4; // Mark.Operation.TASK_END_N1S1.ordinal();
  private static final long STOP_N1S2_OP = 5; // Mark.Operation.TASK_END_N1S2.ordinal();
  private static final long EVENT_N1S1_OP = 6; // Mark.Operation.EVENT_N1S1.ordinal();
  private static final long EVENT_N1S2_OP = 7; // Mark.Operation.EVENT_N1S2.ordinal();
  private static final long EVENT_N2S2_OP = 8; // Mark.Operation.EVENT_N2S2.ordinal();
  private static final long LINK_OP = 10; // Mark.Operation.LINK.ordinal();
  private static final long TAG_N1S1_OP = 13; // Mark.Operation.TAG_N1S1.ordinal();
  private static final long TAG_KEYED_N0S2_OP = 16; // Mark.Operation.TAG_KEYED_N0S2.ordinal();
  private static final long TAG_KEYED_N1S1_OP = 14; // Mark.Operation.TAG_KEYED_N1S1.ordinal();
  private static final long TAG_KEYED_N2S1_OP = 15; // Mark.Operation.TAG_KEYED_N2S1.ordinal();

  static {
    assert START_N1S1_OP == Mark.Operation.TASK_START_N1S1.ordinal();
    assert START_N1S2_OP == Mark.Operation.TASK_START_N1S2.ordinal();
    assert STOP_N1S0_OP == Mark.Operation.TASK_END_N1S0.ordinal();
    assert STOP_N1S1_OP == Mark.Operation.TASK_END_N1S1.ordinal();
    assert STOP_N1S2_OP == Mark.Operation.TASK_END_N1S2.ordinal();
    assert EVENT_N1S1_OP == Mark.Operation.EVENT_N1S1.ordinal();
    assert EVENT_N1S2_OP == Mark.Operation.EVENT_N1S2.ordinal();
    assert EVENT_N2S2_OP == Mark.Operation.EVENT_N2S2.ordinal();
    assert LINK_OP == Mark.Operation.LINK.ordinal();
    assert TAG_N1S1_OP == Mark.Operation.TAG_N1S1.ordinal();
    assert TAG_KEYED_N0S2_OP == Mark.Operation.TAG_KEYED_N0S2.ordinal();
    assert TAG_KEYED_N1S1_OP == Mark.Operation.TAG_KEYED_N1S1.ordinal();
    assert TAG_KEYED_N2S1_OP == Mark.Operation.TAG_KEYED_N2S1.ordinal();
  }

  final SynchronizedMarkHolder markHolder;

  SynchronizedMarkRecorder(long markRecorderId, WeakReference<Thread> creatingThread) {
    this.markHolder = new SynchronizedMarkHolder(32768, markRecorderId, creatingThread);
  }

  @Override
  public void start(
      long gen, String taskName, String tagName, long tagId, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + START_N1S1_OP, nanoTime, taskName);
      markHolder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
    }
  }

  @Override
  public void start(long gen, String taskName, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + START_N1S1_OP, nanoTime, taskName);
    }
  }

  @Override
  public void start(long gen, String taskName, String subTaskName, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNss(gen + START_N1S2_OP, nanoTime, taskName, subTaskName);
    }
  }

  @Override
  public void link(long gen, long linkId) {
    synchronized (markHolder) {
      markHolder.writeN(gen + LINK_OP, linkId);
    }
  }

  @Override
  public void stop(long gen, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeN(gen + STOP_N1S0_OP, nanoTime);
    }
  }

  @Override
  public void stop(long gen, String taskName, String tagName, long tagId, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
      markHolder.writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
    }
  }

  @Override
  public void stop(long gen, String taskName, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
    }
  }

  @Override
  public void stop(long gen, String taskName, String subTaskName, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNss(gen + STOP_N1S2_OP, nanoTime, taskName, subTaskName);
    }
  }

  @Override
  public void event(long gen, String eventName, String tagName, long tagId, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNnss(gen + EVENT_N2S2_OP, nanoTime, tagId, eventName, tagName);
    }
  }

  @Override
  public void event(long gen, String eventName, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + EVENT_N1S1_OP, nanoTime, eventName);
    }
  }

  @Override
  public void event(long gen, String eventName, String subEventName, long nanoTime) {
    synchronized (markHolder) {
      markHolder.writeNss(gen + EVENT_N1S2_OP, nanoTime, eventName, subEventName);
    }
  }

  @Override
  public void attachTag(long gen, String tagName, long tagId) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
    }
  }

  @Override
  public void attachKeyedTag(long gen, String name, long value0) {
    synchronized (markHolder) {
      markHolder.writeNs(gen + TAG_KEYED_N1S1_OP, value0, name);
    }
  }

  @Override
  public void attachKeyedTag(long gen, String name, String value) {
    synchronized (markHolder) {
      markHolder.writeSs(gen + TAG_KEYED_N0S2_OP, name, value);
    }
  }

  @Override
  public void attachKeyedTag(long gen, String name, long value0, long value1) {
    synchronized (markHolder) {
      markHolder.writeNns(gen + TAG_KEYED_N2S1_OP, value0, value1, name);
    }
  }
}
