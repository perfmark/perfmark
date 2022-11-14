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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

final class NoopMarkRecorderProvider extends MarkRecorderProvider {
  NoopMarkRecorderProvider() {}

  @Override
  public MarkRecorder createMarkRecorder(long markRecorderId, WeakReference<Thread> creatingThread) {
    return new NoopMarkHolder();
  }

  private static final class NoopMarkHolder extends MarkRecorder {

    NoopMarkHolder() {}

    @Override
    public void start(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

    @Override
    public void start(long gen, String taskName, long nanoTime) {}

    @Override
    public void start(long gen, String taskName, String subTaskName, long nanoTime) {}

    @Override
    public void link(long gen, long linkId) {}

    @Override
    public void stop(long gen, long nanoTime) {}

    @Override
    public void stop(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

    @Override
    public void stop(long gen, String taskName, long nanoTime) {}

    @Override
    public void stop(long gen, String taskName, String subTaskName, long nanoTime) {}

    @Override
    public void event(long gen, String eventName, String tagName, long tagId, long nanoTime) {}

    @Override
    public void event(long gen, String eventName, long nanoTime) {}

    @Override
    public void event(long gen, String eventName, String subEventName, long nanoTime) {}

    @Override
    public void attachTag(long gen, String tagName, long tagId) {}

    @Override
    public void attachKeyedTag(long gen, String name, String value) {}

    @Override
    public void attachKeyedTag(long gen, String name, long value0) {}

    @Override
    public void attachKeyedTag(long gen, String name, long value0, long value1) {}
  }
}
