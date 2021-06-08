/*
 * Copyright 2021 Carl Mastrangelo
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

package io.perfmark.java15;

import static org.junit.Assert.assertEquals;

import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.testing.MarkHolderTest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HiddenClassVarHandleMarkHolderTest extends MarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;

  @Override
  protected MarkHolder getMarkHolder() {
    return new SecretHiddenClassMarkHolderProvider.HiddenClassMarkHolderProvider().create(1234);
  }

  @Test
  public void read_getsAllButLastIfNotWriter() {
    MarkHolder mh = getMarkHolder();
    int events = mh.maxMarks() - 1;
    for (int i = 0; i < events; i++) {
      mh.start(gen, "task", 3);
    }

    List<Mark> marks = mh.read(true);
    assertEquals(events - 1, marks.size());
  }

  @Test
  public void read_getsAllIfNotWriterButNoWrap() {
    MarkHolder mh = getMarkHolder();

    int events = mh.maxMarks() - 2;
    for (int i = 0; i < events; i++) {
      mh.start(gen, "task", 3);
    }

    List<Mark> marks = mh.read(true);
    assertEquals(events, marks.size());
  }
}
