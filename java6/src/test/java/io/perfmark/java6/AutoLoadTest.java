/*
 * Copyright 2021 Google LLC
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

package io.perfmark.java6;

import static org.junit.Assert.assertEquals;

import io.perfmark.PerfMark;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Checks that auto loading this provider works.
 */
@RunWith(JUnit4.class)
public class AutoLoadTest {
  @Test
  public void autoLoad() {
    Storage.resetForThread();
    PerfMark.setEnabled(true);
    PerfMark.startTask("hi");
    PerfMark.stopTask("hi");
    PerfMark.setEnabled(false);
    MarkList markList = Storage.readForTest();
    assertEquals(2, markList.size());
  }
}
