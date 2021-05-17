/*
 * Copyright 2021 Google LLC
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

package io.perfmark.agent;

import io.perfmark.Link;
import io.perfmark.PerfMark;
import io.perfmark.Tag;
import io.perfmark.TaskCloseable;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

final class TransformerTestClasses {

  static final class ClzAutoRecord {
    public ClzAutoRecord() {
      recordMe();
    }

    void recordMe() {
      // seemingly nothing here.
    }
  }

  record SomeRecord(int hi) {
    public SomeRecord {
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
    }
  }

  static final class ClzCtorLambda implements Executor {
    public ClzCtorLambda() {
      execute(
          () -> {
            PerfMark.startTask("task");
            PerfMark.stopTask("task");
          });
    }

    @Override
    public void execute(Runnable command) {
      command.run();
    }
  }

  static final class ClzWithClinit {
    static {
      Tag tag = PerfMark.createTag("tag", 1);
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
      PerfMark.startTask("task", tag);
      PerfMark.stopTask("task", tag);
    }
  }

  static final class ClzWithInit {
    {
      Tag tag = PerfMark.createTag("tag", 1);
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
      PerfMark.startTask("task", tag);
      PerfMark.stopTask("task", tag);
    }
  }

  static final class ClzWithCtor {
    public ClzWithCtor() {
      Tag tag = PerfMark.createTag("tag", 1);
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
      PerfMark.startTask("task", tag);
      PerfMark.stopTask("task", tag);
    }
  }

  static final class ClzWithLinks {
    public ClzWithLinks() {
      PerfMark.startTask("task");
      Link link = PerfMark.linkOut();
      PerfMark.linkIn(link);
      PerfMark.stopTask("task");
    }
  }

  static final class ClzWithCloseable {
    public ClzWithCloseable() {
      try (TaskCloseable discard = PerfMark.traceTask("task")) {}
    }
  }

  static final class ClzWithWrongCloseable {
    public ClzWithWrongCloseable() throws IOException {
      try (Closeable discard = PerfMark.traceTask("task")) {}
    }
  }

  public interface InterfaceWithDefaults {
    default void record() {
      Tag tag = PerfMark.createTag("tag", 1);
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
      PerfMark.startTask("task", tag);
      PerfMark.stopTask("task", tag);
    }
  }

  static final class Bar implements InterfaceWithDefaults {
    public Bar() {
      record();
    }
  }

  static final class ClzWithMethodRefs {
    public ClzWithMethodRefs() {
      execute(PerfMark::startTask);
      execute(PerfMark::stopTask);
    }

    void execute(Consumer<String> method) {
      method.accept("task");
    }
  }

  private TransformerTestClasses() {}
}

final class ClzFooter {
  {
    Tag tag = PerfMark.createTag("tag", 1);
    PerfMark.startTask("task");
    PerfMark.stopTask("task");
    PerfMark.startTask("task", tag);
    PerfMark.stopTask("task", tag);
  }
}
