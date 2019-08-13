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

package io.perfmark.traceviewer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.perfmark.tracewriter.TraceEventWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * This class converts from the Trace Event json data into a full HTML page. It includes the trace
 * viewer from Catapult, the Chromium trace UI.
 *
 * <p>This class is separate from {@link TraceEventWriter}, because it includes a fairly large HTML
 * chunk, and brings in a differently licenced piece of code.
 *
 * <p>This code is <strong>NOT</strong> API stable, and may be removed in the future, or changed
 * without notice.
 *
 * @since 0.17.0
 */
public final class TraceEventViewer {
  private static final Logger logger = Logger.getLogger(TraceEventViewer.class.getName());

  // Copied from trace2html.html in the Catapult tracing code.
  private static final String INLINE_TRACE_DATA =
      ""
          + "    const traces = [];\n"
          + "    const viewerDataScripts = Polymer.dom(document).querySelectorAll(\n"
          + "        '#viewer-data');\n"
          + "    for (let i = 0; i < viewerDataScripts.length; i++) {\n"
          + "      let text = Polymer.dom(viewerDataScripts[i]).textContent;\n"
          + "      // Trim leading newlines off the text. They happen during writing.\n"
          + "      while (text[0] === '\\n') {\n"
          + "        text = text.substring(1);\n"
          + "      }\n"
          + "      onResult(tr.b.Base64.atob(text));\n"
          + "      viewer.updateDocumentFavicon();\n"
          + "      viewer.globalMode = true;\n"
          + "      viewer.viewTitle = document.title;\n"
          + "      break;\n"
          + "    }\n";

  /**
   * A convenience function around {@link #writeTraceHtml(Writer)}. This writes the trace data to a
   * temporary file and logs the output location.
   *
   * @return the Path of the written file.
   * @throws IOException if it can't write to the destination.
   */
  @CanIgnoreReturnValue
  public static Path writeTraceHtml() throws IOException {
    Path path = Files.createTempFile("perfmark-trace-", ".html");
    try (OutputStream os = Files.newOutputStream(path, TRUNCATE_EXISTING);
        Writer w = new OutputStreamWriter(os, UTF_8)) {
      writeTraceHtml(w);
    }
    logger.log(Level.INFO, "Wrote PerfMark Trace file://{0}", new Object[] {path.toAbsolutePath()});
    return path;
  }

  /**
   * Writes all available trace data as a single HTML file into the given writer.
   *
   * @param writer The destination to write all HTML to.
   * @throws IOException if it can't write to the writer.
   */
  public static void writeTraceHtml(Writer writer) throws IOException {
    InputStream indexStream =
        TraceEventViewer.class.getResourceAsStream("third_party/catapult/index.html");
    if (indexStream == null) {
      throw new IOException("unable to find index.html");
    }
    String index = readAll(indexStream);

    InputStream traceViewerStream =
        TraceEventViewer.class.getResourceAsStream("third_party/catapult/trace_viewer_full.html");
    if (traceViewerStream == null) {
      throw new IOException("unable to find trace_viewer_full.html");
    }
    String traceViewer = trimTraceViewer(readAll(traceViewerStream));

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (OutputStreamWriter w = new OutputStreamWriter(baos, UTF_8)) {
      TraceEventWriter.writeTraceEvents(w);
    }
    byte[] traceData64 = Base64.getUrlEncoder().encode(baos.toByteArray());

    String indexWithTraceViewer =
        replaceIndexTraceImport(index, traceViewer, new String(traceData64, UTF_8));

    String fullIndex = replaceIndexTraceData(indexWithTraceViewer, INLINE_TRACE_DATA);
    writer.write(fullIndex);
  }

  /**
   * Replaces the normal {@code <link>} tag in index.html with a custom replacement, and optionally
   * the inlined Trace data as a base64 script. This is because the trace2html.html file imports the
   * data as a top level text/plain script.
   */
  private static String replaceIndexTraceImport(
      String index, String replacement, @Nullable String inlineTraceData64) {
    int start = index.indexOf("IO_PERFMARK_TRACE_IMPORT");
    if (start == -1) {
      throw new IllegalArgumentException("index doesn't contain IO_PERFMARK_TRACE_IMPORT");
    }
    int line0pos = index.lastIndexOf('\n', start);
    assert line0pos != -1;
    int line1pos = index.indexOf('\n', line0pos + 1);
    assert line1pos != -1;
    int line2pos = index.indexOf('\n', line1pos + 1);
    assert line2pos != -1;
    int line3pos = index.indexOf('\n', line2pos + 1);
    assert line3pos != -1;
    String inlineTraceData = "";
    if (inlineTraceData64 != null) {
      inlineTraceData =
          "\n<script id=\"viewer-data\" type=\"text/plain\">" + inlineTraceData64 + "</script>";
    }
    return index.substring(0, line0pos + 1)
        + replacement
        + inlineTraceData
        + index.substring(line3pos);
  }

  private static String replaceIndexTraceData(String index, String replacement) {
    int start = index.indexOf("IO_PERFMARK_TRACE_URL");
    if (start == -1) {
      throw new IllegalArgumentException("index doesn't contain IO_PERFMARK_TRACE_URL");
    }
    int line0pos = index.lastIndexOf('\n', start);
    assert line0pos != -1;
    int line1pos = index.indexOf('\n', line0pos + 1);
    assert line1pos != -1;
    int line2pos = index.indexOf('\n', line1pos + 1);
    assert line2pos != -1;
    int line3pos = index.indexOf('\n', line2pos + 1);
    assert line3pos != -1;
    return index.substring(0, line0pos + 1) + replacement + index.substring(line3pos);
  }

  private static String trimTraceViewer(String traceViewer) {
    int line0pos = traceViewer.indexOf('\n'); // <!DOCTYPE html>
    int line1pos = traceViewer.indexOf('\n', line0pos + 1); // <html>
    int line2pos = traceViewer.indexOf('\n', line1pos + 1); // <head i18n-values= ...
    int line3pos = traceViewer.indexOf('\n', line2pos + 1); // <meta http-equiv="Content-Type" ...

    int lastpos = traceViewer.lastIndexOf("</head>");

    return traceViewer.substring(line3pos + 1, lastpos);
  }

  private static String readAll(InputStream stream) throws IOException {
    int available = stream.available();
    byte[] data;
    if (available > 0) {
      data = new byte[available + 1];
    } else {
      data = new byte[4096];
    }
    int pos = 0;
    while (true) {
      int read = stream.read(data, pos, data.length - pos);
      if (read == -1) {
        break;
      } else {
        pos += read;
      }
      if (pos == data.length) {
        data = Arrays.copyOf(data, data.length + (data.length >> 2));
      }
    }
    return new String(data, 0, pos, UTF_8);
  }

  private TraceEventViewer() {
    throw new AssertionError("nope");
  }
}
