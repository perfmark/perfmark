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

public abstract class MarkRecorder {

  protected MarkRecorder() {}

  public abstract void start(long gen, String taskName, String tagName, long tagId, long nanoTime);

  public abstract void start(long gen, String taskName, long nanoTime);

  public abstract void start(long gen, String taskName, String subTaskName, long nanoTime);

  public abstract void link(long gen, long linkId);

  public abstract void stop(long gen, long nanoTime);

  public abstract void stop(long gen, String taskName, String tagName, long tagId, long nanoTime);

  public abstract void stop(long gen, String taskName, long nanoTime);

  public abstract void stop(long gen, String taskName, String subTaskName, long nanoTime);

  public abstract void event(long gen, String eventName, String tagName, long tagId, long nanoTime);

  public abstract void event(long gen, String eventName, long nanoTime);

  public abstract void event(long gen, String eventName, String subEventName, long nanoTime);

  public abstract void attachTag(long gen, String tagName, long tagId);

  public abstract void attachKeyedTag(long gen, String name, String value);

  public abstract void attachKeyedTag(long gen, String name, long value0);

  public abstract void attachKeyedTag(long gen, String name, long value0, long value1);
}
