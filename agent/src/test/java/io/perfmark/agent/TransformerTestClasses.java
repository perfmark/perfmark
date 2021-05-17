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
import java.util.concurrent.Executor;

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
    SomeRecord {
      PerfMark.attachTag("hi", hi);
    }
  }

  final class ClzCtorLambda implements Executor {
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
