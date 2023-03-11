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

package io.perfmark.java6;

import io.perfmark.impl.GlobalMarkRecorder;
import io.perfmark.impl.Mark;
import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkRecorder;
import io.perfmark.impl.MarkRecorderRef;
import io.perfmark.java6.SecretSynchronizedGlobalMarkRecorder.SynchronizedGlobalMarkRecorder;
import io.perfmark.testing.MarkHolderTest;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SynchronizedMarkHolderTest extends MarkHolderTest  {

  @Before
  public void setUp() {
    SynchronizedGlobalMarkRecorder.setLocalMarkHolder(
        new SynchronizedMarkHolder(32768, MarkRecorderRef.newRef()));
  }

  @After
  public void tearDown() {
    SynchronizedGlobalMarkRecorder.clearLocalMarkHolder();
  }

  @Override
  protected GlobalMarkRecorder getMarkRecorder() {
    return new SecretSynchronizedGlobalMarkRecorder.SynchronizedGlobalMarkRecorder();
  }

  @Override
  protected MarkHolder getMarkHolder() {
    return SynchronizedGlobalMarkRecorder.getLocalMarkHolder();
  }
}
