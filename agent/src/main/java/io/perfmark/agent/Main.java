package io.perfmark.agent;

import java.lang.instrument.Instrumentation;

public final class Main {

  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(new PerfMarkTransformer());
  }

  private Main() {
    throw new AssertionError("nope");
  }
}
