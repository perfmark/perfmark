/*
 * Copyright 2026 Carl Mastrangelo
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CompletableFuture;

@RunWith(JUnit4.class)
public class ThreadRefInfoTest {
    @Test
    public void getName() {
        ThreadRefInfo threadRefInfo = new ThreadRefInfo(ThreadRef.newRef(null));

        String name = threadRefInfo.getName();
        Assert.assertEquals(Thread.currentThread().getName(), name);

        Thread.currentThread().setName(name + "1");

        Assert.assertEquals(Thread.currentThread().getName(), name + "1");
    }

    @Test
    public void getName_deadThread() throws Exception {
        CompletableFuture<ThreadRefInfo> future = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            future.complete(new ThreadRefInfo(ThreadRef.newRef(null)));
        });
        thread.start();
        thread.join();

        String name = future.get().getName();
        Assert.assertEquals(thread.getName(), name);

        thread.setName(name + "1");
        // Should not change after exit.
        Assert.assertEquals(name, future.get().getName());

        future.get().clearThreadNameRef();

        Assert.assertTrue(future.get().getName().contains("GCed"));
    }
}
