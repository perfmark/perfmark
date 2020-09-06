/*
 * Copyright 2020 Google LLC
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

package io.perfmark.traceviewer;

import io.perfmark.PerfMark;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceEventViewerTest {

  /** This is an example function to show how to use the recorder. */
  @Test
  @Ignore
  public void exampleRecorder() throws Exception {
    PerfMark.setEnabled(true);
    PerfMark.startTask(this, s -> s.getClass().getSimpleName() + ".this");
    PerfMark.stopTask();
    PerfMark.startTask(this, s -> s.getClass().getSimpleName() + ".that");
    PerfMark.stopTask();
    PerfMark.startTask(this, s -> s.getClass().getSimpleName() + "::hi");
    PerfMark.stopTask();
    PerfMark.startTask(this, s -> s.getClass().getSimpleName() + "::hey");
    PerfMark.stopTask();
    PerfMark.startTask(this, s -> s.getClass().getSimpleName() + "::hey");
    PerfMark.stopTask();
    PerfMark.setEnabled(false);
    TraceEventViewer.writeTraceHtml();
  }
}
