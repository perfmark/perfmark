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

package io.perfmark.impl;

public class GlobalMarkRecorder {

  protected GlobalMarkRecorder() {}

  protected void start(long gen, String taskName, String tagName, long tagId) {
    startAt(gen, taskName, tagName, tagId, System.nanoTime());
  }

  protected void start(long gen, String taskName) {
    startAt(gen, taskName, System.nanoTime());
  }

  protected void start(long gen, String taskName, String subTaskName) {
    startAt(gen, taskName, subTaskName, System.nanoTime());
  }

  protected void startAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

  protected void startAt(long gen, String taskName, long nanoTime) {}

  protected void startAt(long gen, String taskName, String subTaskName, long nanoTime) {}

  protected void link(long gen, long linkId) {}

  protected void stop(long gen) {
    stopAt(gen, System.nanoTime());
  }

  protected void stop(long gen, String taskName, String tagName, long tagId) {
    stopAt(gen, taskName, tagName, tagId, System.nanoTime());
  }

  protected void stop(long gen, String taskName) {
    stopAt(gen, taskName, System.nanoTime());
  }

  protected void stop(long gen, String taskName, String subTaskName) {
    stopAt(gen, taskName, subTaskName, System.nanoTime());
  }

  protected void stopAt(long gen, long nanoTime) {}

  protected void stopAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

  protected void stopAt(long gen, String taskName, long nanoTime) {}

  protected void stopAt(long gen, String taskName, String subTaskName, long nanoTime) {}

  protected void event(long gen, String eventName, String tagName, long tagId) {
    eventAt(gen, eventName, tagName, tagId);
  }

  protected void event(long gen, String eventName) {
    eventAt(gen, eventName, System.nanoTime());
  }

  protected void event(long gen, String eventName, String subEventName) {
    eventAt(gen, eventName, subEventName, System.nanoTime());
  }

  protected void eventAt(long gen, String eventName, String tagName, long tagId, long nanoTime) {}

  protected void eventAt(long gen, String eventName, long nanoTime) {}

  protected void eventAt(long gen, String eventName, String subEventName, long nanoTime) {}

  protected void attachTag(long gen, String tagName, long tagId) {}

  protected void attachKeyedTag(long gen, String name, String value) {}

  protected void attachKeyedTag(long gen, String name, long value0) {}

  protected void attachKeyedTag(long gen, String name, long value0, long value1) {}
}
