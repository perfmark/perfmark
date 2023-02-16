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

package io.perfmark.tracewriter;

import com.google.gson.annotations.SerializedName;
import io.perfmark.impl.Mark;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@CheckReturnValue
final class TraceEvent implements Cloneable {

  private TraceEvent() {}

  static final TraceEvent EVENT = new TraceEvent();

  @SerializedName("ph")
  @SuppressWarnings("unused")
  String phase;

  @SerializedName("name")
  @SuppressWarnings("unused")
  String name;

  @Nullable
  @SerializedName("cat")
  @SuppressWarnings("unused")
  String categories;

  @Nullable
  @SerializedName("ts")
  @SuppressWarnings("unused")
  Double traceClockMicros;

  @Nullable
  @SerializedName("pid")
  @SuppressWarnings("unused")
  Long pid;

  @SerializedName("tid")
  @Nullable
  @SuppressWarnings("unused")
  Long tid;

  @Nullable
  @SerializedName("id")
  @SuppressWarnings("unused")
  Long id;

  @Nullable
  @SerializedName("args")
  @SuppressWarnings("unused")
  TagMap args = null;

  @Nullable
  @SerializedName("cname")
  @SuppressWarnings("unused")
  String colorName = null;

  TraceEvent name(String name) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    TraceEvent other = clone();
    other.name = name;
    return other;
  }

  TraceEvent categories(String... categories) {
    if (categories == null) {
      throw new NullPointerException("categories");
    }
    return categories(Arrays.asList(categories));
  }

  TraceEvent categories(List<String> categories) {
    if (categories == null) {
      throw new NullPointerException("categories");
    }
    TraceEvent other = clone();
    if (!categories.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      ListIterator<String> it = categories.listIterator();
      sb.append(it.next());
      while (it.hasNext()) {
        String next = it.next();
        if (next == null) {
          throw new NullPointerException("next null at " + (it.nextIndex() - 1));
        }
        sb.append(',').append(next);
      }
      other.categories = sb.toString();
    } else {
      other.categories = null;
    }
    return other;
  }

  strictfp TraceEvent traceClockNanos(long traceClockNanos) {
    TraceEvent other = clone();
    other.traceClockMicros = traceClockNanos / 1000.0;
    return other;
  }

  TraceEvent phase(String phase) {
    if (phase == null) {
      throw new NullPointerException("phase");
    }
    TraceEvent other = clone();
    other.phase = phase;
    return other;
  }

  TraceEvent tid(long tid) {
    TraceEvent other = clone();
    other.tid = tid;
    return other;
  }

  TraceEvent pid(long pid) {
    TraceEvent other = clone();
    other.pid = pid;
    return other;
  }

  TraceEvent id(long id) {
    TraceEvent other = clone();
    other.id = id;
    return other;
  }

  /**
   * Note This should only be used for tags, as the map size is used to determine the arg names in
   * TraceEventWriter. This will overwrite any existing args.
   *
   * @param tagMap the args to use.
   * @return this
   */
  TraceEvent args(TagMap tagMap) {
    if (tagMap == null) {
      throw new NullPointerException("tagMap");
    }
    TraceEvent other = clone();
    other.args = tagMap;
    return other;
  }

  TagMap args() {
    if (args == null) {
      return TagMap.EMPTY;
    } else {
      return args;
    }
  }

  @Override
  protected TraceEvent clone() {
    try {
      return (TraceEvent) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  static final class TagMap extends AbstractMap<String, Object> {

    static final TagMap EMPTY =
        new TagMap(Collections.<Entry<String, ?>>emptyList(), Collections.emptyList());

    private final List<Entry<String, ?>> keyedValues;
    private final List<?> unkeyedValues;

    private TagMap(List<Entry<String, ?>> keyedValues, List<?> unkeyedValues) {
      this.keyedValues = keyedValues;
      this.unkeyedValues = unkeyedValues;
    }

    TagMap withUnkeyed(@Nullable String tagName, long tagId) {
      List<Object> unkeyedValues = null;
      if (tagName != null && !Mark.NO_TAG_NAME.equals(tagName)) {
        unkeyedValues = new ArrayList<>(this.unkeyedValues);
        unkeyedValues.add(tagName);
      }
      if (tagId != Mark.NO_TAG_ID) {
        unkeyedValues = unkeyedValues != null ? unkeyedValues : new ArrayList<>(this.unkeyedValues);
        unkeyedValues.add(tagId);
      }
      if (unkeyedValues != null) {
        return new TagMap(keyedValues, Collections.unmodifiableList(unkeyedValues));
      } else {
        return new TagMap(keyedValues, this.unkeyedValues);
      }
    }

    TagMap withKeyed(@Nullable String tagName, Object tagValue) {
      List<Entry<String, ?>> keyedValues = new ArrayList<>(this.keyedValues);
      keyedValues.add(new SimpleImmutableEntry<>(String.valueOf(tagName), tagValue));
      return new TagMap(Collections.unmodifiableList(keyedValues), unkeyedValues);
    }

    TagMap withKeyed(@Nullable String tagName, long tagValue0, long tagValue1) {
      List<Entry<String, ?>> keyedValues = new ArrayList<>(this.keyedValues);
      keyedValues.add(
          new SimpleImmutableEntry<>(String.valueOf(tagName), tagValue0 + ":" + tagValue1));
      return new TagMap(Collections.unmodifiableList(keyedValues), unkeyedValues);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      List<Entry<String, ?>> pairs = new ArrayList<>(keyedValues.size() + unkeyedValues.size());
      pairs.addAll(keyedValues);
      for (Object value : unkeyedValues) {
        if (value instanceof Long) {
          pairs.add(new SimpleImmutableEntry<>("id", value));
        } else if (value instanceof String) {
          pairs.add(new SimpleImmutableEntry<>("tag", value));
        } else {
          pairs.add(new SimpleImmutableEntry<>("tag", String.valueOf(value)));
        }
      }

      Map<String, Object> ret = new LinkedHashMap<>();
      addEntry:
      for (Entry<String, ?> kv : pairs) {
        String name = kv.getKey();
        Object value = kv.getValue();
        String derivedName = name;
        int usages = 0;
        while (true) {
          if (!ret.containsKey(derivedName)) {
            ret.put(derivedName, value);
            continue addEntry;
          }
          if (ret.get(derivedName).equals(value)) {
            continue addEntry;
          }
          usages++;
          derivedName = name + " (" + usages + ')';
        }
      }
      return Collections.unmodifiableSet(ret.entrySet());
    }
  }
}
