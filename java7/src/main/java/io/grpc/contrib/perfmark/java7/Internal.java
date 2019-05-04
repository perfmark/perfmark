package io.grpc.contrib.perfmark.java7;

import io.grpc.contrib.perfmark.impl.Generator;

public final class Internal {

  public static Generator createMethodHandleGenerator() {
    return new SecretMethodHandleGenerator.MethodHandleGenerator();
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
