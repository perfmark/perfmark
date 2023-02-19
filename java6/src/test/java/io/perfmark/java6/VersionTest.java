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

package io.perfmark.java6;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VersionTest {

  private static final short JAVA_VERSION_6 = 50;

  @Test
  public void blah() throws Exception {
    Class<?> clz = SecretSynchronizedMarkRecorderProvider.class;
    try (InputStream stream =
        clz.getClassLoader().getResourceAsStream(clz.getName().replace('.', '/') + ".class")) {
      byte[] data = stream.readAllBytes();
      ByteBuffer buf = ByteBuffer.wrap(data);
      // Discard magic int
      buf.getInt();
      short major = buf.getShort();
      short minor = buf.getShort();
      assertEquals(0, major);
      assertEquals(JAVA_VERSION_6, minor);
    }
  }
}
