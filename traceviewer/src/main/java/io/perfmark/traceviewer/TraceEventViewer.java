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

package io.perfmark.traceviewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TraceEventViewer {

  public static void writeTraceHtml(OutputStream stream) throws IOException {
    InputStream tvf =
        TraceEventViewer.class.getResourceAsStream("third_party/catapult/trace_viewer_full.html");
    if (tvf == null) {
      throw new IOException("unable to find trace_viewer_full.html");
    }
  }

  private TraceEventViewer() {
    throw new AssertionError("nope");
  }
}
