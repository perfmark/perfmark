/*
 * Copyright 2023 Carl Mastrangelo
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

import io.perfmark.impl.GlobalMarkRecorder;
import io.perfmark.impl.MarkRecorderRef;
import io.perfmark.impl.Storage;

final class SecretSynchronizedGlobalMarkRecorder {
  public static final class SynchronizedGlobalMarkRecorder extends GlobalMarkRecorder {

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

    private static final LocalHolder localMarkHolder = new LocalHolder();

    // Used reflectively
    public SynchronizedGlobalMarkRecorder() {}

    @Override
    public void start(long gen, String taskName) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeTs(gen + START_N1S1_OP, taskName);
      }
    }

    @Override
    public void start(long gen, String taskName, String subTaskName) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeTss(gen + START_N1S2_OP, taskName, subTaskName);
      }
    }

    @Override
    public void start(long gen, String taskName, String tagName, long tagId) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeTs(gen + START_N1S1_OP, taskName);
        holder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
      }
    }

    @Override
    public void startAt(long gen, String taskName, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + START_N1S1_OP, nanoTime, taskName);
      }
    }

    @Override
    public void startAt(long gen, String taskName, String subTaskName, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNss(gen + START_N1S2_OP, nanoTime, taskName, subTaskName);
      }
    }

    @Override
    public void startAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + START_N1S1_OP, nanoTime, taskName);
        holder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
      }
    }

    @Override
    public void stop(long gen) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeN(gen + STOP_N1S0_OP, nanoTime);
      }
    }

    @Override
    public void stop(long gen, String taskName) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
      }
    }

    @Override
    public void stop(long gen, String taskName, String subTaskName) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNss(gen + STOP_N1S2_OP, nanoTime, taskName, subTaskName);
      }
    }

    @Override
    public void stop(long gen, String taskName, String tagName, long tagId) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
        holder.writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
      }
    }

    @Override
    public void stopAt(long gen, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeN(gen + STOP_N1S0_OP, nanoTime);
      }
    }

    @Override
    public void stopAt(long gen, String taskName, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
      }
    }

    @Override
    public void stopAt(long gen, String taskName, String subTaskName, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNss(gen + STOP_N1S2_OP, nanoTime, taskName, subTaskName);
      }
    }

    @Override
    public void stopAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
        holder.writeNs(gen + STOP_N1S1_OP, nanoTime, taskName);
      }
    }

    @Override
    public void event(long gen, String eventName) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + EVENT_N1S1_OP, nanoTime, eventName);
      }
    }

    @Override
    public void event(long gen, String eventName, String subEventName) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNss(gen + EVENT_N1S2_OP, nanoTime, eventName, subEventName);
      }
    }

    @Override
    public void event(long gen, String eventName, String tagName, long tagId) {
      long nanoTime = System.nanoTime();
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNnss(gen + EVENT_N2S2_OP, nanoTime, tagId, eventName, tagName);
      }
    }

    @Override
    public void eventAt(long gen, String eventName, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + EVENT_N1S1_OP, nanoTime, eventName);
      }
    }

    @Override
    public void eventAt(long gen, String eventName, String subEventName, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNss(gen + EVENT_N1S2_OP, nanoTime, eventName, subEventName);
      }
    }

    @Override
    public void eventAt(long gen, String eventName, String tagName, long tagId, long nanoTime) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNnss(gen + EVENT_N2S2_OP, nanoTime, tagId, eventName, tagName);
      }
    }

    @Override
    public void link(long gen, long linkId) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeN(gen + LINK_OP, linkId);
      }
    }

    @Override
    public void attachTag(long gen, String tagName, long tagId) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + TAG_N1S1_OP, tagId, tagName);
      }
    }

    @Override
    public void attachKeyedTag(long gen, String name, long value0) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNs(gen + TAG_KEYED_N1S1_OP, value0, name);
      }
    }

    @Override
    public void attachKeyedTag(long gen, String name, String value) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeSs(gen + TAG_KEYED_N0S2_OP, name, value);
      }
    }

    @Override
    public void attachKeyedTag(long gen, String name, long value0, long value1) {
      SynchronizedMarkHolder holder = localMarkHolder.get();
      synchronized (holder) {
        holder.writeNns(gen + TAG_KEYED_N2S1_OP, value0, value1, name);
      }
    }

    // VisibleForTesting
    static SynchronizedMarkHolder getLocalMarkHolder() {
      return localMarkHolder.getNoInit();
    }

    // VisibleForTesting
    static void clearLocalMarkHolder() {
      localMarkHolder.remove();
    }

    // VisibleForTesting
    static void setLocalMarkHolder(SynchronizedMarkHolder holder) {
      localMarkHolder.set(holder);
    }

    private static final class LocalHolder extends ThreadLocal<SynchronizedMarkHolder> {
      @Override
      protected SynchronizedMarkHolder initialValue() {
        SynchronizedMarkHolder holder =
            new SynchronizedMarkHolder(32768, MarkRecorderRef.newRef());
        Storage.registerMarkHolder(holder);
        return holder;
      }

      // VisibleForTesting
      SynchronizedMarkHolder getNoInit() {
        return super.get();
      }

      LocalHolder() {}
    }
  }

  private SecretSynchronizedGlobalMarkRecorder() {}
}
