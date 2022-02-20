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

package io.perfmark;

import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.MustBeClosed;
import java.lang.reflect.Method;

/**
 * PerfMark is a very low overhead tracing library. To use PerfMark, annotate the code that needs to
 * be traced using the start and stop methods. For example:
 *
 * <pre>{@code
 * PerfMark.startTask("parseMessage");
 * try {
 *   message = parse(bytes);
 * } finally {
 *   PerfMark.stopTask("parseMessage");
 * }
 * }</pre>
 *
 * <p>When PerfMark is enabled, these tracing calls will record the start and stop times of the
 * given task. When disabled, PerfMark disables these tracing calls, resulting in no additional
 * tracing overhead. PerfMark can be enabled or disabled using the {@link #setEnabled(boolean)}. By
 * default, PerfMark starts off disabled. PerfMark can be automatically enabled by setting the
 * System property {@code io.perfmark.PerfMark.startEnabled} to true.
 *
 * <p>Tasks represent the span of work done by some code, starting and stopping in the same thread.
 * Each task is started using one of the {@code startTask} methods, and ended using one of
 * {@code stopTask} methods.  Each start must have a corresponding stop.    While not required,
 * it is good practice for the start and stop calls have matching arguments for clarity.  Tasks
 * form a "tree", with each child task starting after the parent has started, and stopping before
 * the parent has stopped. The most recently started (and not yet stopped) task is used by the
 * tagging and linking commands described below.
 *
 * <p>Tags are metadata about the task.  Each {@code Tag} contains a String and/or a long that
 * describes the task, such as an RPC name, or request ID.  When PerfMark is disabled, the Tag
 * objects are not created, avoiding overhead.  Tags are useful for keeping track of metadata
 * about a task(s) that doesn't change frequently, or needs to be applied at multiple layers.
 * In addition to Tag objects, named-tags can be added to the current task using the
 * {@code attachTag} methods.  These allow including key-value like metadata with the task.
 *
 * <p>Links allow the code to represent relationships between different threads.  When one thread
 * initiates work for another thread (such as a callback), Links express the control flow.  For
 * example:
 *
 * <pre>{@code
 * PerfMark.startTask("handleMessage");
 * try {
 *   Link link = PerfMark.linkOut();
 *   message = parse(bytes);
 *   executor.execute(() -> {
 *     PerfMark.startTask("processMessage");
 *     try {
 *       PerfMark.linkIn(link);
 *       handle(message);
 *     } finally {
 *       PerfMark.stopTask("processMessage");
 *     }
 *   });
 * } finally {
 *   PerfMark.stopTask("handleMessage");
 * }
 * }</pre>
 *
 * <p>Links are created inside the scope of the current task and are linked into the scope of
 * another task.  PerfMark will represent the causal relationship between these two tasks.  Links
 * have a many-many relationship, and can be reused.  Like Tasks and Tags, when PerfMark is
 * disabled, the Links returned are no-op implementations.
 *
 * <p>Events are a special kind of Task, which do not have a duration.  In effect, they only have
 * a single timestamp the represents a particular occurrence.  Events are slightly more efficient
 * than tasks while PerfMark is enabled, but cannot be used with Links or named-tags.
 *
 * @author Carl Mastrangelo
 */
public final class PerfMark {

  /**
   * Turns on or off PerfMark recording. Don't call this method too frequently; while neither on nor
   * off have very high overhead, transitioning between the two may be slow.
   *
   * @param value {@code true} to enable PerfMark recording, or {@code false} to disable it.
   */
  public static void setEnabled(boolean value) {
    impl.setEnabled(value);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a runtime-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * <p>The tag is a run-time identifier for the task. It represents the dynamic part of the task,
   * while the task name is the constant part of the task. While not always enforced, tags should
   * not be {@code null}.
   *
   * @param taskName the name of the task.
   * @param tag a user provided tag for the task.
   */
  public static void startTask(String taskName, Tag tag) {
    impl.startTask(taskName, tag);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a runtime-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * @param taskName the name of the task.
   */
  public static void startTask(String taskName) {
    impl.startTask(taskName);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a runtime-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * <p>This function has many more caveats than the {@link #startTask(String)} that accept a
   * string. See the docs at {@link #attachTag(String, Object, StringFunction)} for a list of risks
   * associated with passing a function.
   *
   * @param taskNameObject the name of the task.
   * @param taskNameFunction the function that will convert the taskNameObject to a taskName
   * @param <T> the object type to be stringified
   * @since 0.22.0
   */
  public static <T> void startTask(T taskNameObject, StringFunction<? super T> taskNameFunction) {
    impl.startTask(taskNameObject, taskNameFunction);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The names of
   * the task and subtask should be runtime-time constants, usually a string literal. Tasks with the
   * same name can be grouped together for analysis later, so avoid using too many unique task
   * names.
   *
   * @param taskName the name of the task.
   * @param subTaskName the name of the sub task
   * @since 0.20.0
   */
  public static void startTask(String taskName, String subTaskName) {
    impl.startTask(taskName, subTaskName);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a runtime-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * <p>The returned closeable is meant to be used in a try-with-resources block. Callers should not
   * allow the returned closeable to be used outside of the try block that initiated the call.
   * Unlike other closeables, it is not safe to call close() more than once.
   *
   * @param taskName the name of the task.
   * @return a closeable that must be closed at the end of the task
   * @since 0.23.0
   */
  @MustBeClosed
  public static TaskCloseable traceTask(String taskName) {
    impl.startTask(taskName);
    return TaskCloseable.INSTANCE;
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a runtime-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * <p>This function has many more caveats than the {@link #traceTask(String)} that accept a
   * string. See the docs at {@link #attachTag(String, Object, StringFunction)} for a list of risks
   * associated with passing a function.  Unlike other closeables, it is not safe to call close()
   * more than once.
   *
   * @param taskNameObject the name of the task.
   * @param taskNameFunction the function that will convert the taskNameObject to a taskName
   * @param <T> the object type to be stringified
   * @return a closeable that must be closed at the end of the task
   * @since 0.23.0
   */
  @MustBeClosed
  public static <T> TaskCloseable traceTask(
      T taskNameObject, StringFunction<? super T> taskNameFunction) {
    impl.startTask(taskNameObject, taskNameFunction);
    return TaskCloseable.INSTANCE;
  }

  /**
   * Marks an event. Events are logically both a task start and a task end. Events have no duration
   * associated. Events still represent the instant something occurs. If PerfMark is disabled, this
   * method is a no-op.
   *
   * <p>The tag is a run-time identifier for the event. It represents the dynamic part of the event,
   * while the event name is the constant part of the event. While not always enforced, tags should
   * not be {@code null}.
   *
   * @param eventName the name of the event.
   * @param tag a user provided tag for the event.
   */
  public static void event(String eventName, Tag tag) {
    impl.event(eventName, tag);
  }

  /**
   * Marks an event. Events are logically both a task start and a task end. Events have no duration
   * associated. Events still represent the instant something occurs. If PerfMark is disabled, this
   * method is a no-op.
   *
   * @param eventName the name of the event.
   */
  public static void event(String eventName) {
    impl.event(eventName);
  }

  /**
   * Marks an event. Events are logically both a task start and a task end. Events have no duration
   * associated. Events still represent the instant something occurs. If PerfMark is disabled, this
   * method is a no-op.
   *
   * @param eventName the name of the event.
   * @param subEventName the name of the sub event.
   * @since 0.20.0
   */
  public static void event(String eventName, String subEventName) {
    impl.event(eventName, subEventName);
  }

  /**
   * Marks the end of a task. If PerfMark is disabled, this method is a no-op.
   *
   * <p>It is important that {@link #stopTask} always be called after starting a task, even in case
   * of exceptions. Failing to do so may result in corrupted results.
   *
   * @since 0.22.0
   */
  public static void stopTask() {
    impl.stopTask();
  }

  /**
   * Marks the end of a task. If PerfMark is disabled, this method is a no-op. The task name and tag
   * should match the ones provided to the corresponding {@link #startTask(String, Tag)}, if
   * provided. If the task name or tag do not match, the implementation will prefer the starting
   * name and tag. The name and tag help identify the task if PerfMark is enabled mid way through
   * the task, or if the previous results have been overwritten. The name of the task should be a
   * runtime-time constant, usually a string literal. Consider using {@link #stopTask()} instead.
   *
   * <p>It is important that {@link #stopTask} always be called after starting a task, even in case
   * of exceptions. Failing to do so may result in corrupted results.
   *
   * @param taskName the name of the task being ended.
   * @param tag the tag of the task being ended.
   */
  public static void stopTask(String taskName, Tag tag) {
    impl.stopTask(taskName, tag);
  }

  /**
   * Marks the end of a task. If PerfMark is disabled, this method is a no-op. The task name should
   * match the ones provided to the corresponding {@link #startTask(String)}, if provided. If the
   * task name does not match, the implementation will prefer the starting name. The name helps
   * identify the task if PerfMark is enabled mid way through the task, or if the previous results
   * have been overwritten. The name of the task should be a runtime-time constant, usually a string
   * literal. Consider using {@link #stopTask()} instead.
   *
   * <p>It is important that {@link #stopTask} always be called after starting a task, even in case
   * of exceptions. Failing to do so may result in corrupted results.
   *
   * @param taskName the name of the task being ended.
   */
  public static void stopTask(String taskName) {
    impl.stopTask(taskName);
  }

  /**
   * Marks the end of a task. If PerfMark is disabled, this method is a no-op. The task name should
   * match the ones provided to the corresponding {@link #startTask(String, String)}, if provided.
   * If the task name does not match, the implementation will prefer the starting name. The name
   * helps identify the task if PerfMark is enabled mid way through the task, or if the previous
   * results have been overwritten. The name of the task should be a runtime-time constant, usually
   * a string literal. Consider using {@link #stopTask()} instead.
   *
   * <p>It is important that {@link #stopTask} always be called after starting a task, even in case
   * of exceptions. Failing to do so may result in corrupted results.
   *
   * @param taskName the name of the task being ended.
   * @param subTaskName the name of the sub task being ended.
   * @since 0.20.0
   */
  public static void stopTask(String taskName, String subTaskName) {
    impl.stopTask(taskName, subTaskName);
  }

  /**
   * Creates a tag with no name or numeric identifier. The returned instance is different based on
   * if PerfMark is enabled or not.
   *
   * <p>This method is seldomly useful; users should generally prefer to use the overloads of
   * methods that don't need a tag. An empty tag may be useful though when the tag of a group of
   * tasks may change over time.
   *
   * @return a Tag that has no name or id.
   */
  public static Tag createTag() {
    return Impl.NO_TAG;
  }

  /**
   * Creates a tag with no name. The returned instance is different based on if PerfMark is enabled
   * or not. The provided id does not have to be globally unique, but is instead meant to give
   * context to a task.
   *
   * @param id a user provided identifier for this Tag.
   * @return a Tag that has no name.
   */
  public static Tag createTag(long id) {
    return impl.createTag(Impl.NO_TAG_NAME, id);
  }

  /**
   * Creates a tag with no numeric identifier. The returned instance is different based on if
   * PerfMark is enabled or not. The provided name does not have to be globally unique, but is
   * instead meant to give context to a task.
   *
   * @param name a user provided name for this Tag.
   * @return a Tag that has no numeric identifier.
   */
  public static Tag createTag(String name) {
    return impl.createTag(name, Impl.NO_TAG_ID);
  }

  /**
   * Creates a tag with both a name and a numeric identifier. The returned instance is different
   * based on if PerfMark is enabled or not. Neither the provided name nor id has to be globally
   * unique, but are instead meant to give context to a task.
   *
   * @param id a user provided identifier for this Tag.
   * @param name a user provided name for this Tag.
   * @return a Tag that has both a name and id.
   */
  public static Tag createTag(String name, long id) {
    return impl.createTag(name, id);
  }

  /**
   * DO NOT CALL, no longer implemented. Use {@link #linkOut} instead.
   *
   * @return a no-op link that
   */
  @Deprecated
  @DoNotCall
  public static Link link() {
    return Impl.NO_LINK;
  }

  /**
   * A link connects between two tasks that start asynchronously. When {@link #linkOut()} is called,
   * an association between the most recently started task and a yet-to-be named task on another
   * thread, is created. Links are a one-to-many relationship. A single started task can have
   * multiple associated tasks on other threads.
   *
   * @since 0.17.0
   * @return A Link to be used in other tasks.
   */
  public static Link linkOut() {
    return impl.linkOut();
  }

  /**
   * Associate this link with the most recently started task. There may be at most one inbound
   * linkage per task: the first call to {@link #linkIn} decides which outbound task is the origin.
   *
   * @param link a link created inside of another task.
   * @since 0.17.0
   */
  public static void linkIn(Link link) {
    impl.linkIn(link);
  }

  /**
   * Attaches an additional tag to the current active task. The tag provided is independent of the
   * tag used with {@link #startTask(String, Tag)} and {@link #stopTask(String, Tag)}. Unlike the
   * two previous two task overloads, the tag provided to {@link #attachTag(Tag)} does not have to
   * match any other tags in use. This method is useful for when you have the tag information after
   * the task is started.
   *
   * <p>Here are some example usages:
   *
   * <p>Recording the amount of work done in a task:
   *
   * <pre>
   *   PerfMark.startTask("read");
   *   byte[] data = file.read();
   *   PerfMark.attachTag(PerfMark.createTag("bytes read", data.length));
   *   PerfMark.stopTask("read");
   * </pre>
   *
   * <p>Recording a tag which may be absent on an exception:
   *
   * <pre>
   *   Socket s;
   *   Tag remoteTag = PerfMark.createTag(remoteAddress.toString());
   *   PerfMark.startTask("connect", remoteTag);
   *   try {
   *     s = connect(remoteAddress);
   *     PerfMark.attachTag(PerfMark.createTag(s.getLocalAddress().toString());
   *   } finally {
   *     PerfMark.stopTask("connect", remoteTag);
   *   }
   * </pre>
   *
   * @since 0.18.0
   * @param tag the Tag to attach.
   */
  public static void attachTag(Tag tag) {
    impl.attachTag(tag);
  }

  /**
   * Attaches an additional keyed tag to the current active task. The tag provided is independent of
   * the tag used with {@code startTask} and {@code stopTask}. This tag operation is different than
   * {@link Tag} in that the tag value has an associated name (also called a key). The tag name and
   * value are attached to the most recently started task, and don't have to match any other tags.
   * This method is useful for when you have the tag information after the task is started.
   *
   * @param tagName The name of the value being attached
   * @param tagValue The value to attach to the current task.
   * @since 0.20.0
   */
  public static void attachTag(String tagName, String tagValue) {
    impl.attachTag(tagName, tagValue);
  }

  /**
   * Attaches an additional keyed tag to the current active task. The tag provided is independent of
   * the tag used with {@code startTask} and {@code stopTask}. This tag operation is different than
   * {@link Tag} in that the tag value has an associated name (also called a key). The tag name and
   * value are attached to the most recently started task, and don't have to match any other tags.
   * This method is useful for when you have the tag information after the task is started.
   *
   * @param tagName The name of the value being attached
   * @param tagValue The value to attach to the current task.
   * @since 0.20.0
   */
  public static void attachTag(String tagName, long tagValue) {
    impl.attachTag(tagName, tagValue);
  }

  /**
   * Attaches an additional keyed tag to the current active task. The tag provided is independent of
   * the tag used with {@code startTask} and {@code stopTask}. This tag operation is different than
   * {@link Tag} in that the tag values have an associated name (also called a key). The tag name
   * and values are attached to the most recently started task, and don't have to match any other
   * tags. This method is useful for when you have the tag information after the task is started.
   *
   * <p>This method may treat the given two longs as special. If the tag name contains the string
   * "uuid" (case insensitive), the value may be treated as a single 128 bit value. An example
   * usage:
   *
   * <pre>
   *   RPC rpc = ...
   *   PerfMark.startTask("sendRPC");
   *   try {
   *     UUID u = rpc.uuid();
   *     PerfMark.attachTag("rpc uuid", u.getMostSignificantBits(), u.getLeastSignificantBits());
   *     send(rpc);
   *   } finally {
   *     PerfMark.stopTask("sendRPC");
   *   }
   * </pre>
   *
   * @param tagName The name of the value being attached
   * @param tagValue0 The first value to attach to the current task.
   * @param tagValue1 The second value to attach to the current task.
   * @since 0.20.0
   */
  public static void attachTag(String tagName, long tagValue0, long tagValue1) {
    impl.attachTag(tagName, tagValue0, tagValue1);
  }

  /**
   * Attaches an additional keyed tag to the current active task. The tag provided is independent of
   * the tag used with {@code startTask} and {@code stopTask}. This tag operation is different than
   * {@link Tag} in that the tag value has an associated name (also called a key). The tag name and
   * value are attached to the most recently started task, and don't have to match any other tags.
   * This method is useful for when you have the tag information after the task is started.
   *
   * <p>Unlike {@link #attachTag(String, String)}, this defers constructing the tagValue String
   * until later, and avoids doing any work while PerfMark is disabled. Callers are expected to
   * provide a method handle that can consume the {@code tagObject}, and produce a tagValue. For
   * example:
   *
   * <pre>{@code
   * Response resp = client.makeCall(request);
   * PerfMark.attachTag("httpServerHeader", resp, r -> r.getHeaders().get("Server"));
   * }</pre>
   *
   * <p>Also unlike {@link #attachTag(String, String)}, this function is easier to misuse. Prefer
   * using the other attachTag methods unless you are confident you need this one. Be familiar with
   * following issues:
   *
   * <ul>
   *   <li>Callers should be careful to not capture the {@code tagObject}, and instead use the
   *       argument to {@code stringFunction}. This avoids a memory allocation and possibly holding
   *       the tagObject alive longer than necessary.
   *   <li>The {@code stringFunction} should be idempotent, have no side effects, and be safe to
   *       invoke from other threads. If the string function references state that may be changed,
   *       callers must synchronize access. The string function may be called multiple times for the
   *       same tag object. Additionally, if {@code attachTag()} is called with the same tag object
   *       and string function multiple times, PerfMark may invoke the function only once.
   *   <li>The tag object may kept alive longer than normal, and prevent garbage collection from
   *       reclaiming it. If the tag object retains a large amount of resources, this may appear as
   *       a memory leak. The risk of this memory increase will need to be balanced with the cost of
   *       eagerly constructing the tag value string. Additionally, if the string function is a
   *       capturing lambda (refers to local or global state), the function itself may appear as a
   *       leak.
   *   <li>If the stringFunction is {@code null}, or if it throws an exception when called, the tag
   *       value will not be attached. It is implementation defined if such problems are reported
   *       (e.g. logged). Note that exceptions are expensive compared to PerfMark calls, and thus
   *       may slow down tracing. If an exception is thrown, or if the stringFunction is {@code
   *       null}, PerfMark may invoke other methods on the tag object or string function, such as
   *       {@code toString()} and {@code getClass()}.
   * </ul>
   *
   * @param tagName The name of the value being attached
   * @param tagObject The tag object which will passed to the stringFunction.
   * @param stringFunction The function that will convert the object to
   * @param <T> the type of tag object to be stringified
   * @since 0.22.0
   */
  public static <T> void attachTag(
      String tagName, T tagObject, StringFunction<? super T> stringFunction) {
    impl.attachTag(tagName, tagObject, stringFunction);
  }

  private static final Impl impl;

  static {
    Impl instance = null;
    Throwable err = null;
    Class<?> clz = null;
    try {
      clz = Class.forName("io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
    } catch (Throwable t) {
      err = t;
    }
    if (clz != null) {
      try {
        instance = clz.asSubclass(Impl.class).getConstructor(Tag.class).newInstance(Impl.NO_TAG);
      } catch (Throwable t) {
        err = t;
      }
    }
    if (instance != null) {
      impl = instance;
    } else {
      impl = new Impl(Impl.NO_TAG);
    }
    if (err != null) {
      try {
        if (Boolean.getBoolean("io.perfmark.PerfMark.debug")) {
          // We need to be careful here, as it's easy to accidentally cause a class load.  Logger is loaded
          // reflectively to avoid accidentally pulling it in.
          // TODO(carl-mastrangelo): Maybe make this load SLF4J instead?
          Class<?> logClass = Class.forName("java.util.logging.Logger");
          Object logger = logClass.getMethod("getLogger", String.class).invoke(null, PerfMark.class.getName());
          Class<?> levelClass = Class.forName("java.util.logging.Level");
          Object level = levelClass.getField("FINE").get(null);
          Method logMethod = logClass.getMethod("log", levelClass, String.class, Throwable.class);
          logMethod.invoke(logger, level, "Error during PerfMark.<clinit>", err);
        }
      } catch (Throwable e) {
        // ignored.
      }
    }
  }

  private PerfMark() {}
}
