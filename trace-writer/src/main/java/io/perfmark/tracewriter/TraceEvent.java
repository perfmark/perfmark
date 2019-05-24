package io.perfmark.tracewriter;

import com.google.gson.annotations.SerializedName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

class TraceEvent {

  @SerializedName("ph")
  final String phase;

  @SerializedName("name")
  final String name;

  @Nullable
  @SerializedName("cat")
  final String categories;

  @Nullable
  @SerializedName("ts")
  final Double traceClockMicros;

  @Nullable
  @SerializedName("pid")
  final Long pid;

  @SerializedName("tid")
  final Long tid;

  @Nullable
  @SerializedName("id")
  protected Long id;

  @Nullable
  @SerializedName("args")
  final Map<String, ?> args = null;

  @Nullable
  @SerializedName("cname")
  final String colorName = null;

  TraceEvent(
      String name,
      List<String> categories,
      String phase,
      @Nullable Long nanoTime,
      @Nullable Long pid,
      long tid) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    this.name = name;
    if (categories == null) {
      throw new NullPointerException("categories");
    }
    if (!categories.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      Iterator<String> it = categories.iterator();
      sb.append(it.next());
      while (it.hasNext()) {
        sb.append(',').append(it.next());
      }
      this.categories = sb.toString();
    } else {
      this.categories = null;
    }
    if (phase == null) {
      throw new NullPointerException("phase");
    }
    this.phase = phase;
    if (nanoTime != null) {
      this.traceClockMicros = nanoTime / 1000.0;
    } else {
      this.traceClockMicros = null;
    }
    this.pid = pid;
    this.tid = tid;
  }
}
