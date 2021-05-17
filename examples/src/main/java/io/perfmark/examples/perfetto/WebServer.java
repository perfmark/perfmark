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

package io.perfmark.examples.perfetto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.perfmark.PerfMark;
import io.perfmark.TaskCloseable;
import io.perfmark.tracewriter.TraceEventWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * This class shows how to set up a basic HTTP server that can display PerfMark traces in the
 * browser. The code here has some minor error cases ignored for the sake of brevity.
 */
public final class WebServer {

  public static void main(String[] args) throws IOException, InterruptedException {
    PerfMark.setEnabled(true);

    HttpServer res = HttpServer.create(new InetSocketAddress("localhost", 0), 5);

    res.createContext("/", new IndexHandler());
    res.createContext("/trace.json", new JsonHandler());
    res.start();
    try {
      InetSocketAddress listen = res.getAddress();
      System.err.println("Listening at http://localhost:" + listen.getPort() + '/');

      while (true) {
        Thread.sleep(10);
      }
    } finally {
      res.stop(5);
    }
  }

  private static final class IndexHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, 0);
      try (TaskCloseable ignored = PerfMark.traceTask("IndexHandler.handle");
          InputStream is = getClass().getResourceAsStream("index.html");
          OutputStream os = exchange.getResponseBody(); ) {
        byte[] data = new byte[is.available()];
        int total = is.read(data);
        if (total != data.length) {
          throw new IOException("didn't read");
        }
        os.write(data);
        os.flush();
      }
    }
  }

  private static final class JsonHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, 0);
      try (TaskCloseable ignored = PerfMark.traceTask("JsonHandler.handle");
          OutputStream os = exchange.getResponseBody();
          OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
        TraceEventWriter.writeTraceEvents(osw);
        osw.flush();
      }
    }
  }
}
