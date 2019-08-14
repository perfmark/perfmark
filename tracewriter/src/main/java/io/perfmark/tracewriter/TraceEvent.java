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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@CheckReturnValue
final class TraceEvent implements Cloneable {

  private TraceEvent() {}

  static final TraceEvent EVENT = new TraceEvent();

  @SerializedName("ph")
  @SuppressWarnings("unused")
  private String phase;

  @SerializedName("name")
  @SuppressWarnings("unused")
  private String name;

  @Nullable
  @SerializedName("cat")
  @SuppressWarnings("unused")
  private String categories;

  @Nullable
  @SerializedName("ts")
  @SuppressWarnings("unused")
  private Double traceClockMicros;

  @Nullable
  @SerializedName("pid")
  @SuppressWarnings("unused")
  private Long pid;

  @SerializedName("tid")
  @Nullable
  @SuppressWarnings("unused")
  private Long tid;

  @Nullable
  @SerializedName("id")
  @SuppressWarnings("unused")
  private Long id;

  @Nullable
  @SerializedName("args")
  @SuppressWarnings("unused")
  private Map<String, ?> args = null;

  @Nullable
  @SerializedName("cname")
  @SuppressWarnings("unused")
  private String colorName = null;

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

  TraceEvent args(Map<String, ?> args) {
    if (args == null) {
      throw new NullPointerException("args");
    }
    Map<String, Object> newArgs = new LinkedHashMap<>(args.size());
    for (Map.Entry<String, ?> arg : args.entrySet()) {
      if (arg.getKey() == null) {
        throw new NullPointerException("key");
      }
      if (arg.getValue() == null) {
        throw new NullPointerException("value");
      }
      newArgs.put(arg.getKey(), arg.getValue());
    }
    TraceEvent other = clone();
    if (!newArgs.isEmpty()) {
      other.args = Collections.unmodifiableMap(newArgs);
    } else {
      other.args = null;
    }
    return other;
  }

  TraceEvent arg(String argKey, Object argValue) {
    if (argKey == null) {
      throw new NullPointerException("argKey");
    }
    if (argValue == null) {
      throw new NullPointerException("argValue");
    }
    TraceEvent other = clone();
    if (args == null) {
      other.args = Collections.singletonMap(argKey, argValue);
    } else {
      Map<String, Object> newArgs = new LinkedHashMap<>(args);
      newArgs.put(argKey, argValue);
      other.args = Collections.unmodifiableMap(newArgs);
    }
    return other;
  }

  @Override
  protected TraceEvent clone() {
    try {
      return (TraceEvent) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
