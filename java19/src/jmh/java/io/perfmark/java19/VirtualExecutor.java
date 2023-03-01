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

package io.perfmark.java19;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class VirtualExecutor implements ExecutorService {

  static final String THREAD_LOCALS_DISABLED_PROP = "io.perfmark.java19.threadLocalsDisabled";

  private final BlockingQueue<Runnable> runnables;
  private final ExecutorService delegate;

  public VirtualExecutor(int maxThread, String prefix) {
    boolean threadLocalsAllowed = !Boolean.getBoolean(THREAD_LOCALS_DISABLED_PROP);
    this.runnables = new LinkedBlockingQueue<>();
    this.delegate =
        Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name(prefix, 1).allowSetThreadLocals(threadLocalsAllowed).factory());
    for (int i = 0; i < maxThread; i++) {
      delegate.execute(new Worker());
    }
  }

  @Override
  public void shutdown() {
    delegate.shutdownNow();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    var future = new FutureTask<>(task);
    execute(future);
    return future;
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    var future = new FutureTask<>(task, result);
    execute(future);
    return future;
  }

  @Override
  public Future<?> submit(Runnable task) {
    var future = new FutureTask<>(task, null);
    execute(future);
    return future;
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public void execute(Runnable command) {
    try {
      runnables.put(command);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private final class Worker implements Runnable {

    @Override
    public void run() {
      while (true) {
        Runnable run;
        try {
          run = runnables.take();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        run.run();
      }
    }
  }
}
