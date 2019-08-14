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

import io.perfmark.impl.Mark;
import javax.annotation.Nullable;

/**
 * Tag is a dynamic, runtime created identifier (such as an RPC id).
 */
public final class Tag {
  static final Tag NO_TAG = new Tag(Mark.NO_TAG_ID);

  @Nullable final String tagName;
  final long tagId;

  Tag(long tagId) {
    this.tagName = Mark.NO_TAG_NAME;
    this.tagId = tagId;
  }

  Tag(String tagName) {
    this(tagName, Mark.NO_TAG_ID);
  }

  Tag(String tagName, long tagId) {
    if (tagName == null) {
      throw new NullPointerException("bad tag name");
    }
    this.tagName = tagName;
    this.tagId = tagId;
  }

  @Override
  public String toString() {
    return "Tag(" + tagName + ", " + tagId + ')';
  }

  /**
   * Equality on Tags is not well defined, since the created tag can depend on if PerfMark was
   * enabled at the time of creation.
   */
  @Override
  @Deprecated // Don't use equality on tags
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
