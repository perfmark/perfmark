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
import io.perfmark.impl.MarkRecorder;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.MarkRecorderRef;
import io.perfmark.java9.SecretMarkRecorder.VarHandleMarkRecorder;
import io.perfmark.testing.MarkHolderTest;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VarHandleMarkRecorderTest extends MarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;


  @Before
  public void setUp() {
    VarHandleMarkRecorder.setLocalMarkHolder(
        new VarHandleMarkHolder(MarkRecorderRef.newRef(), 32768));
  }

  @After
  public void tearDown() {
    VarHandleMarkRecorder.clearLocalMarkHolder();
  }

  @Override
  protected MarkRecorder getMarkRecorder() {
    return new VarHandleMarkRecorder();
  }

  @Override
  protected MarkHolder getMarkHolder() {
    return VarHandleMarkRecorder.getLocalMarkHolder();
  }

  @Test
  public void read_getsAllButLastIfNotWriter() {
    MarkRecorder mr = getMarkRecorder();
    int events = getMarkHolder().maxMarks() - 1;
    for (int i = 0; i < events; i++) {
      mr.startAt(gen, "task", 3);
    }

    List<MarkList> markLists = getMarkHolder().read();
    assertEquals(markLists.size(), 1);
    assertEquals(events, markLists.get(0).size());
  }

  @Test
  public void read_getsAllIfNotWriterButNoWrap() {
    MarkRecorder mr = getMarkRecorder();

    int events = getMarkHolder().maxMarks() - 2;
    for (int i = 0; i < events; i++) {
      mr.startAt(gen, "task", 3);
    }

    List<MarkList> markLists = getMarkHolder().read();
    assertEquals(markLists.size(), 1);
    assertEquals(events, markLists.get(0).size());
  }
}
