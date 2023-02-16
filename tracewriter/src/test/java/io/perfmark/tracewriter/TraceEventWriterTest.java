/*
 * Copyright 2023 Google LLC
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

package io.perfmark.tracewriter;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkList;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceEventWriterTest {
  @Test
  public void writeAndParse() throws Exception {
    List<MarkList> markLists = List.of(MarkList.newBuilder()
            .setMarkListId(9)
            .setThreadId(99)
            .setThreadName("Billy")
            .setMarks(
                List.of(
                    Mark.taskStart(1, 2345, "allbadd<>\\//\"'"),
                    Mark.keyedTag(1, "hello", "world"),
                    Mark.taskEnd(1, 2347)))
        .build());
    TestTraceEvent event1 = new TestTraceEvent();
    event1.name = "thread_name";
    event1.phase = "M";
    event1.pid = 100L;
    event1.tid = 99L;
    event1.args = Map.of("name", "Billy", "markListId", 9);
    TestTraceEvent event2 = new TestTraceEvent();
    event2.phase = "B";
    event2.name = "allbadd<>\\//\"'";
    event2.traceClockMicros = 1.111;
    event2.tid = 99L;
    event2.pid = 100L;
    event2.args = Map.of("hello", "world");
    TestTraceEvent event3 = new TestTraceEvent();
    event3.phase = "E";
    event3.traceClockMicros = 1.113;
    event3.tid = 99L;
    event3.pid = 100L;
    //event2.
    Map<String, List<TestTraceEvent>> expected = Map.of("traceEvents", List.of(event1, event2, event3));

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (var osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
      TraceEventWriter.writeTraceEvents(osw, markLists, 1234, 5678, 100);
    }
    Map<String, List<TestTraceEvent>> map =
        new ObjectMapper().readValue(baos.toByteArray(), new TypeReference<>() {});

    assertEquals(expected, map);
  }

  public static final class TestTraceEvent {

    // This is not part of the official spec.
    public Long markListId;

    @SuppressWarnings("unused")
    @JsonProperty("ph")
    public String phase;

    @JsonProperty("name")
    @SuppressWarnings("unused")
    public String name;

    @JsonProperty("cat")
    @SuppressWarnings("unused")
    public String categories;

    @JsonProperty("ts")
    @SuppressWarnings("unused")
    public Double traceClockMicros;

    @JsonProperty("pid")
    @SuppressWarnings("unused")
    public Long pid;

    @JsonProperty("tid")
    @SuppressWarnings("unused")
    public Long tid;

    @JsonProperty("id")
    @SuppressWarnings("unused")
    public Long id;

    @JsonProperty("args")
    @SuppressWarnings("unused")
    public Map<String, ?> args;

    @JsonProperty("cname")
    @SuppressWarnings("unused")
    public String colorName;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TestTraceEvent that = (TestTraceEvent) o;
      return Objects.equals(phase, that.phase)
          && Objects.equals(name, that.name)
          && Objects.equals(categories, that.categories)
          && Objects.equals(traceClockMicros, that.traceClockMicros)
          && Objects.equals(pid, that.pid)
          && Objects.equals(tid, that.tid)
          && Objects.equals(id, that.id)
          && Objects.equals(args, that.args)
          && Objects.equals(colorName, that.colorName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(phase, name, categories, traceClockMicros, pid, tid, id, colorName);
    }

    @Override
    public String toString() {
      return "TestTraceEvent{" +
          ", phase='" + phase + '\'' +
          ", name='" + name + '\'' +
          ", categories='" + categories + '\'' +
          ", traceClockMicros=" + traceClockMicros +
          ", pid=" + pid +
          ", tid=" + tid +
          ", id=" + id +
          ", args=" + args +
          ", colorName='" + colorName + '\'' +
          '}';
    }
  }
}
