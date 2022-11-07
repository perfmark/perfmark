/*
 * Copyright 2022 Carl Mastrangelo
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

final class MostlyThreadLocalMarkHolder extends LocalMarkHolder {

  private static final MostlyThreadLocal localMarkHolder = new MostlyThreadLocal();

  MostlyThreadLocalMarkHolder() {}

  @Override
  public MarkHolder acquire() {
    return localMarkHolder.get();
  }

  @Override
  public void release(MarkHolder markHolder) {}

  @Override
  public void clear() {
    localMarkHolder.remove();
  }
}
