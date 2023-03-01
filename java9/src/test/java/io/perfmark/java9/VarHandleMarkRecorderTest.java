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

package io.perfmark.java9;

import static org.junit.Assert.assertEquals;

import io.perfmark.impl.Generator;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.MarkRecorder;
import io.perfmark.impl.MarkRecorderRef;
import io.perfmark.testing.MarkHolderTest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VarHandleMarkRecorderTest extends MarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;

  private final VarHandleMarkRecorder recorder =
      new VarHandleMarkRecorder(MarkRecorderRef.newRef(), 16384);
  private final MarkHolder markHolder = recorder.markHolder;

  @Override
  protected MarkHolder getMarkHolder() {
    return markHolder;
  }

  @Override
  public MarkRecorder getMarkRecorder() {
    return recorder;
  }

  @Test
  public void read_getsAllButLastIfNotWriter() {
    MarkRecorder mr = getMarkRecorder();
    int events = markHolder.maxMarks() - 1;
    for (int i = 0; i < events; i++) {
      mr.start(gen, "task", 3);
    }

    List<MarkList> markLists = markHolder.read();
    assertEquals(markLists.size(), 1);
    assertEquals(events, markLists.get(0).size());
  }

  @Test
  public void read_getsAllIfNotWriterButNoWrap() {
    MarkRecorder mr = getMarkRecorder();

    int events = markHolder.maxMarks() - 2;
    for (int i = 0; i < events; i++) {
      mr.start(gen, "task", 3);
    }

    List<MarkList> markLists = markHolder.read();
    assertEquals(markLists.size(), 1);
    assertEquals(events, markLists.get(0).size());
  }
}
