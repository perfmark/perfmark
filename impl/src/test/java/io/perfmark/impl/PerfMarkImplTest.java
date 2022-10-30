/*
 * Copyright 2022 Google LLC
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

package io.perfmark.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkImplTest {

  @Test
  public void nextGeneration_enable() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(0, 3*4096);
    assertEquals((3L<<10) + (1<<8), gen);
  }

  @Test
  public void nextGeneration_disable() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(1 << Generator.GEN_OFFSET, 3*4096);
    assertEquals((3L<<10), gen);
  }

  @Test
  public void nextGeneration_newStampEnabled() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(
        (3<< (Generator.GEN_OFFSET + 2))
            + (1 << Generator.GEN_OFFSET),
        3*4096);
    assertEquals((3L<<10), gen);
  }

  @Test
  public void nextGeneration_newStampDisabled() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(
        (3<< (Generator.GEN_OFFSET + 2)),
        3*4096);
    assertEquals((4L<<10) + (1<<Generator.GEN_OFFSET), gen);
  }

  @Test
  public void nextGeneration_maxNanos() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(1 << Generator.GEN_OFFSET, -1);
    assertNotEquals(Generator.FAILURE, gen);
  }

  @Test
  public void nextGeneration_noOverflowOnDisable() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(0x3FFF_FFFF_FFFF_FD00L, -1);
    assertNotEquals(Generator.FAILURE, gen);
  }

  @Test
  public void nextGeneration_overflowOnEnable() {
    var gen = SecretPerfMarkImpl.PerfMarkImpl.nextGeneration(0x3FFF_FFFF_FFFF_FC00L, -1);
    assertEquals(Generator.FAILURE, gen);
  }
}
