/*
 * Copyright 2019 Carl Mastrangelo
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

/** Tag is a dynamic, runtime created identifier (such as an RPC id). */
public final class Tag {
  @Nullable final String tagName;
  final long tagId;

  Tag(@Nullable String tagName, long tagId) {
    // tagName should be non-null, but checking is expensive
    this.tagName = tagName;
    this.tagId = tagId;
  }
}
