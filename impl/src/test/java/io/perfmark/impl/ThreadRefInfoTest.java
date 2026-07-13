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
