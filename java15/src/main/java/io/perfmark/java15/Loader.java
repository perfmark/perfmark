/*
 * Copyright 2023 Carl Mastrangelo
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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

final class Loader {

  static final int DEFAULT_SIZE = 32768;

  private static final int[] maxEventsOffsets;
  private static final int[] maxEventsMaskOffsets;
  private static final byte[] markHolderClassData;

  static {
    byte[] classData;
    try (var is = Loader.class.getResourceAsStream("HiddenClassVarHandleMarkHolder.class")) {
      classData = is.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ByteBuffer expectedMaxEvents = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
    expectedMaxEvents.putInt(HiddenClassVarHandleMarkHolder.MAX_EVENTS);
    byte[] maxEvents = expectedMaxEvents.array();
    int[] maxOffsets = findOffsets(classData, maxEvents);
    if (maxOffsets.length != 2) {
      throw new RuntimeException("hop");
    }

    ByteBuffer expectedMaxEventsMask = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
    expectedMaxEventsMask.putLong(HiddenClassVarHandleMarkHolder.MAX_EVENTS_MASK);
    byte[] maxEventsMax = expectedMaxEventsMask.array();
    int[] maskOffsets = findOffsets(classData, maxEventsMax);
    if (maskOffsets.length != 1) {
      throw new RuntimeException("skip");
    }

    replaceSize(classData, DEFAULT_SIZE, maxOffsets, maskOffsets);
    maxEventsOffsets = maxOffsets;
    maxEventsMaskOffsets = maskOffsets;
    markHolderClassData = classData;
  }

  static Class<? extends MarkHolderRecorder> getHiddenClass(int size)
      throws IllegalAccessException {
    byte[] classData;
    if (size != DEFAULT_SIZE) {
      classData = markHolderClassData.clone();
      replaceSize(classData, size, maxEventsOffsets, maxEventsMaskOffsets);
    } else {
      classData = markHolderClassData;
    }
    return MethodHandles.lookup()
        .defineHiddenClass(classData, true)
        .lookupClass()
        .asSubclass(MarkHolderRecorder.class);
  }

  private static void replaceSize(
      byte[] haystack, int size, int[] maxEventsOffsets, int[] maxEventsMaskOffsets) {
    ByteBuffer buf = ByteBuffer.wrap(haystack).order(ByteOrder.BIG_ENDIAN);
    for (int off : maxEventsOffsets) {
      buf.putInt(off, size);
    }
    for (int off : maxEventsMaskOffsets) {
      buf.putLong(off, size - 1);
    }
  }

  private static int[] findOffsets(byte[] haystack, byte[] needle) {
    int[] matches = new int[0];
    outer: for (int i = 0; i < haystack.length - needle.length; i++) {
      for (int k = 0; k < needle.length; k++) {
        if (haystack[i + k] != needle[k]) {
          continue outer;
        }
      }
      matches = Arrays.copyOf(matches, matches.length + 1);
      matches[matches.length - 1] = i;
    }
    return matches;
  }

  private Loader() {}
}
