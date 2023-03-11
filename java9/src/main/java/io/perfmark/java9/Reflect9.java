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

package io.perfmark.java9;

import io.perfmark.impl.GlobalMarkRecorder;
import io.perfmark.impl.MarkRecorderRef;
import io.perfmark.impl.Storage;

final class Reflect9 {

  public static final class VarHandleGlobalMarkRecorder extends GlobalMarkRecorder {

    private static final LocalHolder localMarkHolder = new LocalHolder();

    // Used Reflectively
    public VarHandleGlobalMarkRecorder() {}

    @Override
    public void start(long gen, String taskName) {
      localMarkHolder.get().startAt(gen, taskName, System.nanoTime());
    }

    @Override
    public void start(long gen, String taskName, String tagName, long tagId) {
      localMarkHolder.get().startAt(gen, taskName, tagName, tagId, System.nanoTime());
    }

    @Override
    public void start(long gen, String taskName, String subTaskName) {
      localMarkHolder.get().startAt(gen, taskName, subTaskName, System.nanoTime());
    }

    @Override
    public void startAt(long gen, String taskName, long nanoTime) {
      localMarkHolder.get().startAt(gen, taskName, nanoTime);
    }

    @Override
    public void startAt(long gen, String taskName, String subTaskName, long nanoTime) {
      localMarkHolder.get().startAt(gen, taskName, subTaskName, nanoTime);
    }

    @Override
    public void startAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      localMarkHolder.get().startAt(gen, taskName, tagName, tagId, nanoTime);
    }

    @Override
    public void stop(long gen) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().stopAt(gen, nanoTime);
    }

    @Override
    public void stop(long gen, String taskName) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().stopAt(gen, taskName, nanoTime);
    }

    @Override
    public void stop(long gen, String taskName, String subTaskName) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().stopAt(gen, taskName, subTaskName, nanoTime);
    }

    @Override
    public void stop(long gen, String taskName, String tagName, long tagId) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().stopAt(gen, taskName, tagName, tagId, nanoTime);
    }

    @Override
    public void stopAt(long gen, long nanoTime) {
      localMarkHolder.get().stopAt(gen, nanoTime);
    }

    @Override
    public void stopAt(long gen, String taskName, long nanoTime) {
      localMarkHolder.get().stopAt(gen, taskName, nanoTime);
    }

    @Override
    public void stopAt(long gen, String taskName, String subTaskName, long nanoTime) {
      localMarkHolder.get().stopAt(gen, taskName, subTaskName, nanoTime);
    }

    @Override
    public void stopAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {
      localMarkHolder.get().stopAt(gen, taskName, tagName, tagId, nanoTime);
    }

    @Override
    public void link(long gen, long linkId) {
      localMarkHolder.get().link(gen, linkId);
    }

    @Override
    public void event(long gen, String eventName) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().eventAt(gen, eventName, nanoTime);
    }

    @Override
    public void event(long gen, String eventName, String subEventName) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().eventAt(gen, eventName, subEventName, nanoTime);
    }

    @Override
    public void event(long gen, String eventName, String tagName, long tagId) {
      long nanoTime = System.nanoTime();
      localMarkHolder.get().eventAt(gen, eventName, tagName, tagId, nanoTime);
    }

    @Override
    public void eventAt(long gen, String eventName, long nanoTime) {
      localMarkHolder.get().eventAt(gen, eventName, nanoTime);
    }

    @Override
    public void eventAt(long gen, String eventName, String subEventName, long nanoTime) {
      localMarkHolder.get().eventAt(gen, eventName, subEventName, nanoTime);
    }

    @Override
    public void eventAt(long gen, String eventName, String tagName, long tagId, long nanoTime) {
      localMarkHolder.get().eventAt(gen, eventName, tagName, tagId, nanoTime);
    }

    @Override
    public void attachTag(long gen, String tagName, long tagId) {
      localMarkHolder.get().attachTag(gen, tagName, tagId);
    }

    @Override
    public void attachKeyedTag(long gen, String name, String value) {
      localMarkHolder.get().attachKeyedTag(gen, name, value);
    }

    @Override
    public void attachKeyedTag(long gen, String name, long value0) {
      localMarkHolder.get().attachKeyedTag(gen, name, value0);
    }

    @Override
    public void attachKeyedTag(long gen, String name, long value0, long value1) {
      localMarkHolder.get().attachKeyedTag(gen, name, value0, value1);
    }

    // VisibleForTesting
    static VarHandleMarkHolder getLocalMarkHolder() {
      return localMarkHolder.getNoInit();
    }

    // VisibleForTesting
    static void clearLocalMarkHolder() {
      localMarkHolder.remove();
    }

    // VisibleForTesting
    static void setLocalMarkHolder(VarHandleMarkHolder holder) {
      localMarkHolder.set(holder);
    }

    private static final class LocalHolder extends ThreadLocal<VarHandleMarkHolder> {

      @Override
      protected VarHandleMarkHolder initialValue() {
        VarHandleMarkHolder holder =
            new VarHandleMarkHolder(MarkRecorderRef.newRef(), 32768);
        Storage.registerMarkHolder(holder);
        return holder;
      }

      // VisibleForTesting
      VarHandleMarkHolder getNoInit() {
        return super.get();
      }

      LocalHolder() {}
    }
  }

  private Reflect9() {}
}
