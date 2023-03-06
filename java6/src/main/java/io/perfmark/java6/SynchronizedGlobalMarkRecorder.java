package io.perfmark.java6;

import io.perfmark.impl.GlobalMarkRecorder;
import io.perfmark.impl.MarkRecorderRef;

public final class SynchronizedGlobalMarkRecorder extends GlobalMarkRecorder {

  private static final ThreadLocal<SynchronizedMarkRecorder> localMarkRecorder =
      new ThreadLocal<SynchronizedMarkRecorder>() {
        @Override
        protected SynchronizedMarkRecorder initialValue() {
          return new SynchronizedMarkRecorder(MarkRecorderRef.newRef());
        }
      };

  @Override
  protected void start(long gen, String taskName) {
    localMarkRecorder.get().start(gen, taskName, System.nanoTime());
  }

  @Override
  protected void start(long gen, String taskName, String subTaskName) {
    localMarkRecorder.get().start(gen, taskName, subTaskName, System.nanoTime());
  }

  @Override
  protected void start(long gen, String taskName, String tagName, long tagId) {
    localMarkRecorder.get().start(gen, taskName, tagName, tagId, System.nanoTime());
  }

  @Override
  protected void stop(long gen) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, nanoTime);
  }

  @Override
  protected void stop(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, taskName, nanoTime);
  }

  @Override
  protected void stop(long gen, String taskName, String subTaskName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, taskName, subTaskName, nanoTime);
  }

  @Override
  protected void stop(long gen, String taskName, String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().stop(gen, taskName, tagName, tagId, nanoTime);
  }

  @Override
  protected void event(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().event(gen, eventName, nanoTime);
  }

  @Override
  protected void event(long gen, String eventName, String subEventName) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().event(gen, eventName, subEventName, nanoTime);
  }

  @Override
  protected void event(long gen, String eventName, String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkRecorder.get().event(gen, eventName, tagName, tagId, nanoTime);
  }

  @Override
  protected void attachTag(long gen, String tagName, long tagId) {
    localMarkRecorder.get().attachTag(gen, tagName, tagId);
  }

  @Override
  protected void attachKeyedTag(long gen, String name, long value0) {
    localMarkRecorder.get().attachKeyedTag(gen, name, value0);
  }

  @Override
  protected void attachKeyedTag(long gen, String name, String value) {
    localMarkRecorder.get().attachKeyedTag(gen, name, value);
  }

  @Override
  protected void attachKeyedTag(long gen, String name, long value0, long value1) {
    localMarkRecorder.get().attachKeyedTag(gen, name, value0, value1);
  }
}
