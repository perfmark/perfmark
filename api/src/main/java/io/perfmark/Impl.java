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

public class Impl {
  static final String NO_TAG_NAME = "";
  static final long NO_TAG_ID = Long.MIN_VALUE;
  /**
   * This value is current {@link Long#MIN_VALUE}, but it could also be {@code 0}. The invariant
   * {@code NO_LINK_ID == -NO_LINK_ID} must be maintained to work when PerfMark is disabled.
   */
  private static final long NO_LINK_ID = Long.MIN_VALUE;

  static final Tag NO_TAG = new Tag(Impl.NO_TAG_NAME, Impl.NO_TAG_ID);
  static final Link NO_LINK = new Link(Impl.NO_LINK_ID);

  /** The Noop implementation */
  protected Impl(Tag key) {
    if (key != NO_TAG) {
      throw new AssertionError("nope");
    }
  }

  protected void setEnabled(boolean value) {}

  protected void startTask(String taskName, Tag tag) {}

  protected void startTask(String taskName) {}

  protected void startTask(String taskName, String subTaskName) {}

  protected void event(String eventName, Tag tag) {}

  protected void event(String eventName) {}

  protected void event(String eventName, String subEventName) {}

  protected void stopTask(String taskName, Tag tag) {}

  protected void stopTask(String taskName) {}

  protected void stopTask(String taskName, String subTaskName) {}

  protected Link linkOut() {
    return NO_LINK;
  }

  protected void linkIn(Link link) {}

  protected void attachTag(Tag tag) {}

  protected Tag createTag(@Nullable String tagName, long tagId) {
    return NO_TAG;
  }

  @Nullable
  protected static String unpackTagName(Tag tag) {
    return tag.tagName;
  }

  protected static long unpackTagId(Tag tag) {
    return tag.tagId;
  }

  protected static long unpackLinkId(Link link) {
    return link.linkId;
  }

  protected static Tag packTag(@Nullable String tagName, long tagId) {
    return new Tag(tagName, tagId);
  }

  protected static Link packLink(long linkId) {
    return new Link(linkId);
  }
}
