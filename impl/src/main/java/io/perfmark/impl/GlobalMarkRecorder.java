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

  public void start(long gen, String taskName, String tagName, long tagId) {}

  public void start(long gen, String taskName) {}

  public void start(long gen, String taskName, String subTaskName) {}

  public void startAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

  public void startAt(long gen, String taskName, long nanoTime) {}

  public void startAt(long gen, String taskName, String subTaskName, long nanoTime) {}

  public void link(long gen, long linkId) {}

  public void stop(long gen) {}

  public void stop(long gen, String taskName, String tagName, long tagId) {}

  public void stop(long gen, String taskName) {}

  public void stop(long gen, String taskName, String subTaskName) {}

  public void stopAt(long gen, long nanoTime) {}

  public void stopAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {}

  public void stopAt(long gen, String taskName, long nanoTime) {}

  public void stopAt(long gen, String taskName, String subTaskName, long nanoTime) {}

  public void event(long gen, String eventName, String tagName, long tagId) {}

  public void event(long gen, String eventName) {}

  public void event(long gen, String eventName, String subEventName) {}

  public void eventAt(long gen, String eventName, String tagName, long tagId, long nanoTime) {}

  public void eventAt(long gen, String eventName, long nanoTime) {}

  public void eventAt(long gen, String eventName, String subEventName, long nanoTime) {}

  public void attachTag(long gen, String tagName, long tagId) {}

  public void attachKeyedTag(long gen, String name, String value) {}

  public void attachKeyedTag(long gen, String name, long value0) {}

  public void attachKeyedTag(long gen, String name, long value0, long value1) {}
}
