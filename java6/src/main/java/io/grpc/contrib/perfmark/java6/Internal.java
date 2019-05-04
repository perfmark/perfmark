package io.grpc.contrib.perfmark.java6;

import io.grpc.contrib.perfmark.impl.Generator;

public final class Internal {

  public static Generator createVolatileGenerator() {
    return new SecretVolatileGenerator.VolatileGenerator();
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
