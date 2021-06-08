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

import io.perfmark.impl.MarkHolder;
import io.perfmark.impl.MarkHolderProvider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class SecretHiddenClassMarkHolderProvider {
  public static final class HiddenClassMarkHolderProvider extends MarkHolderProvider {
    private static final int DEFAULT_SIZE = 32768;

    private final int[] maxEventsOffsets;
    private final int[] maxEventsMaskOffsets;

    private final byte[] markHolderClassData;

    public HiddenClassMarkHolderProvider() {
      try (InputStream classData = getClass().getResourceAsStream("HiddenClassVarHandleMarkHolder.class")) {
        markHolderClassData = classData.readAllBytes();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      ByteBuffer expectedMaxEvents = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
      expectedMaxEvents.putInt(HiddenClassVarHandleMarkHolder.MAX_EVENTS);
      byte[] maxEvents = expectedMaxEvents.array();
      maxEventsOffsets = findOffsets(markHolderClassData, maxEvents);
      if (maxEventsOffsets.length != 2) {
        throw new RuntimeException("hop");
      }

      ByteBuffer expectedMaxEventsMask = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      expectedMaxEventsMask.putLong(HiddenClassVarHandleMarkHolder.MAX_EVENTS_MASK);
      byte[] maxEventsMax = expectedMaxEventsMask.array();
      maxEventsMaskOffsets = findOffsets(markHolderClassData, maxEventsMax);
      if (maxEventsMaskOffsets.length != 1) {
        throw new RuntimeException("skip");
      }

      replaceSize(markHolderClassData, DEFAULT_SIZE);
    }

    @Override
    public MarkHolder create(long markHolderId) {
      return create(markHolderId, DEFAULT_SIZE);
    }

    MarkHolder create(long markHolderId, int size) {
      final byte[] classData;
      if (size != DEFAULT_SIZE) {
        classData = Arrays.copyOf(markHolderClassData, markHolderClassData.length);
        replaceSize(classData, size);
      } else {
        classData = markHolderClassData;
      }
      try {
        Class<?> clz = MethodHandles.lookup().defineHiddenClass(classData, true).lookupClass();
        return clz.asSubclass(MarkHolder.class).getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }

    private void replaceSize(byte[] haystack, int size) {
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
  }

  private SecretHiddenClassMarkHolderProvider() {}
}
