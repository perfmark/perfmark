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

/**
 * A MarkRecorder records tracing events from {@link io.perfmark.PerfMark} calls.  Instances
 * of this class can be called concurrently by multiple threads.
 */
public class MarkRecorder {

  protected MarkRecorder() {}

  public void start(long gen, String taskName, String tagName, long tagId) {
    unimplemented();
  }

  public void start(long gen, String taskName) {
    unimplemented();
  }

  public void start(long gen, String taskName, String subTaskName) {
    unimplemented();
  }

  public void startAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {
    unimplemented();
  }

  public void startAt(long gen, String taskName, long nanoTime) {
    unimplemented();
  }

  public void startAt(long gen, String taskName, String subTaskName, long nanoTime) {
    unimplemented();
  }

  public void link(long gen, long linkId) {
    unimplemented();
  }

  public void stop(long gen) {
    unimplemented();
  }

  public void stop(long gen, String taskName, String tagName, long tagId) {
    unimplemented();
  }

  public void stop(long gen, String taskName) {
    unimplemented();
  }

  public void stop(long gen, String taskName, String subTaskName) {
    unimplemented();
  }

  public void stopAt(long gen, long nanoTime) {
    unimplemented();
  }

  public void stopAt(long gen, String taskName, String tagName, long tagId, long nanoTime) {
    unimplemented();
  }

  public void stopAt(long gen, String taskName, long nanoTime) {
    unimplemented();
  }

  public void stopAt(long gen, String taskName, String subTaskName, long nanoTime) {
    unimplemented();
  }

  public void event(long gen, String eventName, String tagName, long tagId) {
    unimplemented();
  }

  public void event(long gen, String eventName) {
    unimplemented();
  }

  public void event(long gen, String eventName, String subEventName) {
    unimplemented();
  }

  public void eventAt(long gen, String eventName, String tagName, long tagId, long nanoTime) {
    unimplemented();
  }

  public void eventAt(long gen, String eventName, long nanoTime) {
    unimplemented();
  }

  public void eventAt(long gen, String eventName, String subEventName, long nanoTime) {
    unimplemented();
  }

  public void attachTag(long gen, String tagName, long tagId) {
    unimplemented();
  }

  public void attachKeyedTag(long gen, String name, String value) {
    unimplemented();
  }

  public void attachKeyedTag(long gen, String name, long value0) {
    unimplemented();
  }

  public void attachKeyedTag(long gen, String name, long value0, long value1) {
    unimplemented();
  }

  /**
   * This method exists for subclasses to add custom behavior to unimplemented method calls.
   * All the other methods in this class invoke this method.
   */
  public void unimplemented() {}
}
