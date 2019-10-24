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

package io.perfmark.util;

import io.perfmark.PerfMark;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkUtilTest {

  @Test
  public void record() throws Exception {
    PerfMark.tracer().setEnabled(true);
    PerfMarkUtil.recordTask(
        "hi", PerfMark.tracer().createTag("hello"), () -> LockSupport.parkNanos(this, 100));

    try (PerfMarkUtil.TaskRecorder tr = PerfMarkUtil.recordTask("yes")) {}

    List<MarkList> data = Storage.read();
    System.out.println(data);
  }
}
