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
import io.perfmark.impl.GlobalMarkRecorder;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.MarkRecorder;
import io.perfmark.impl.MarkRecorderRef;
import io.perfmark.testing.MarkHolderTest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HiddenClassVarHandleMarkRecorderTest extends MarkHolderTest {

  private final long gen = 1L << Generator.GEN_OFFSET;

  @Before
  public void setUp() {
    try {
      Reflect15.HiddenClassVarHandleGlobalMarkRecorder.setLocalMarkHolder(
          Loader.getHiddenClass(Loader.DEFAULT_SIZE)
              .getDeclaredConstructor(MarkRecorderRef.class)
              .newInstance(MarkRecorderRef.newRef()));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected MarkHolder getMarkHolder() {
    return Reflect15.HiddenClassVarHandleGlobalMarkRecorder.getLocalMarkHolder();
  }

  @Override
  protected GlobalMarkRecorder getMarkRecorder() {
    return new Reflect15.HiddenClassVarHandleGlobalMarkRecorder();
  }
/*
  @Test
  @Ignore// TODO(carl-mastrangelo): reenable this
  public void read_getsAllButLastIfNotWriter() {
    MarkRecorder mr = getMarkRecorder();
    MarkHolder mh = getMarkHolder();
    int events = mh.maxMarks() - 1;
    for (int i = 0; i < events; i++) {
      mr.start(gen, "task", 3);
    }

    List<MarkList> marks = mh.read();
    assertEquals(events - 1, marks.size());
  }

  @Test
  @Ignore // TODO(carl-mastrangelo): reenable this
  public void read_getsAllIfNotWriterButNoWrap() {
    MarkRecorder mr = getMarkRecorder();
    MarkHolder mh = getMarkHolder();

    int events = mh.maxMarks() - 2;
    for (int i = 0; i < events; i++) {
      mr.start(gen, "task", 3);
    }

    List<MarkList> marks = mh.read();
    assertEquals(events, marks.size());
  }*/
}
