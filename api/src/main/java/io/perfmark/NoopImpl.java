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

package io.perfmark;

import javax.annotation.Nullable;

final class NoopImpl extends Impl {
  NoopImpl() {
    super("I'm pretty sure I know what I'm doing");
  }

  @Override
  protected void setEnabled(boolean value) {}

  @Override
  protected void startTask(String taskName, @Nullable String tagName, long tagId) {}

  @Override
  protected void startTask(String taskName) {}

  @Override
  protected void event(String eventName, @Nullable String tagName, long tagId) {}

  @Override
  protected void event(String eventName) {}

  @Override
  protected void stopTask(String taskName, @Nullable String tagName, long tagId) {}

  @Override
  protected void stopTask(String taskName) {}

  @Override
  protected boolean shouldCreateTag() {
    return false;
  }

  @Override
  protected long linkAndGetId() {
    return NO_LINK_ID;
  }

  @Override
  protected void link(long linkId) {}
}
