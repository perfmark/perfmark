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

package io.perfmark.java15;

import io.perfmark.impl.MarkHolder;

abstract class MarkHolderRecorder extends MarkHolder {

  abstract void startAt(long gen, String taskName, String tagName, long tagId, long nanoTime);

  abstract void startAt(long gen, String taskName, long nanoTime);

  abstract void startAt(long gen, String taskName, String subTaskName, long nanoTime);

  abstract void link(long gen, long linkId);

  abstract void stopAt(long gen, long nanoTime);

  abstract void stopAt(long gen, String taskName, String tagName, long tagId, long nanoTime);

  abstract void stopAt(long gen, String taskName, long nanoTime);
  abstract void stopAt(long gen, String taskName, String subTaskName, long nanoTime);

  abstract void eventAt(long gen, String eventName, String tagName, long tagId, long nanoTime);

  abstract void eventAt(long gen, String eventName, long nanoTime);

  abstract void eventAt(long gen, String eventName, String subEventName, long nanoTime);

  abstract void attachTag(long gen, String tagName, long tagId);

  abstract void attachKeyedTag(long gen, String name, long value);

  abstract void attachKeyedTag(long gen, String name, long value0, long value1);

  abstract void attachKeyedTag(long gen, String name, String value);
}
