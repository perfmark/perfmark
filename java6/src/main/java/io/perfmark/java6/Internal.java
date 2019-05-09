package io.perfmark.java6;

import io.perfmark.impl.Generator;

public final class Internal {

  public static Generator createVolatileGenerator() {
    return new SecretVolatileGenerator.VolatileGenerator();
  }

  private Internal() {
    throw new AssertionError("nope");
  }
}
