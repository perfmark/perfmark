/*
 * Copyright 2019 Google LLC
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

package io.perfmark.java9;

import io.perfmark.impl.Generator;
import io.perfmark.impl.Mark;PerfMarkTransformerTest
import java.util.List;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

/** Simulates the PerfMarkStorage racy reader. */
@JCStressTest
@Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "0 Writes")
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "1 Write")
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "2 Writes")
@Outcome(id = "3", expect = Expect.ACCEPTABLE, desc = "3 Writes")
@Outcome(id = "4", expect = Expect.ACCEPTABLE, desc = "4 Writes")
@Outcome(id = "5", expect = Expect.ACCEPTABLE, desc = "5 Writes")
@Outcome(id = "6", expect = Expect.ACCEPTABLE, desc = "6 Writes")
@Outcome(id = "7", expect = Expect.ACCEPTABLE, desc = "7 Writes")
@Outcome(id = "8", expect = Expect.ACCEPTABLE, desc = "8 Writes")
@Outcome(id = "9", expect = Expect.ACCEPTABLE, desc = "9 Writes")
@Outcome(id = "10", expect = Expect.ACCEPTABLE, desc = "10 Writes")
@Outcome(id = "11", expect = Expect.ACCEPTABLE, desc = "11 Writes")
@Outcome(id = "12", expect = Expect.ACCEPTABLE, desc = "12 Writes")
@Outcome(id = "13", expect = Expect.ACCEPTABLE, desc = "13 Writes")
@Outcome(id = "14", expect = Expect.ACCEPTABLE, desc = "14 Writes")
@Outcome(id = "15", expect = Expect.ACCEPTABLE, desc = "15 Writes")
@Outcome(id = "16", expect = Expect.ACCEPTABLE, desc = "16 Writes")
@Outcome(id = "17", expect = Expect.ACCEPTABLE, desc = "17 Writes")
@Outcome(id = "18", expect = Expect.ACCEPTABLE, desc = "18 Writes")
@Outcome(id = "19", expect = Expect.ACCEPTABLE, desc = "19 Writes")
@Outcome(id = "20", expect = Expect.ACCEPTABLE, desc = "20 Writes")
@Outcome(id = "21", expect = Expect.ACCEPTABLE, desc = "21 Writes")
@Outcome(id = "22", expect = Expect.ACCEPTABLE, desc = "22 Writes")
@Outcome(id = "23", expect = Expect.ACCEPTABLE, desc = "23 Writes")
@Outcome(id = "24", expect = Expect.ACCEPTABLE, desc = "24 Writes")
@Outcome(id = "25", expect = Expect.ACCEPTABLE, desc = "25 Writes")
@Outcome(id = "26", expect = Expect.ACCEPTABLE, desc = "26 Writes")
@Outcome(id = "27", expect = Expect.ACCEPTABLE, desc = "27 Writes")
@Outcome(id = "28", expect = Expect.ACCEPTABLE, desc = "28 Writes")
@Outcome(id = "29", expect = Expect.ACCEPTABLE, desc = "29 Writes")
@Outcome(id = "30", expect = Expect.ACCEPTABLE, desc = "30 Writes")
@Outcome(id = "31", expect = Expect.ACCEPTABLE, desc = "31 Writes")
@Outcome(id = "32", expect = Expect.ACCEPTABLE, desc = "32 Writes")
@Outcome(id = "-1", expect = Expect.FORBIDDEN, desc = "Wrong Type")
@Outcome(id = "-2", expect = Expect.FORBIDDEN, desc = "Wrong Marker")
@Outcome(id = "-3", expect = Expect.FORBIDDEN, desc = "Wrong ID")
@State
@Description("Simulates the PerfMarkStorage reader.")
public class PerfMarkStorageStress {
  private static final int OFFSET;
  private static final int SIZE = 8;

  static {
    OFFSET = 31;
    assert Generator.GEN_OFFSET <= OFFSET;
  }

  private final VarHandleMarkHolder holder = new VarHandleMarkHolder(SIZE);

  @Actor
  public void writer() {
    for (long i = 0; i < SIZE * 4; i++) {
      holder.link(i << OFFSET, i);
    }
  }

  @Actor
  @SuppressWarnings("ReferenceEquality")
  public void reader(L_Result r) {
    List<Mark> marks = holder.read(true);
    int ret = marks.size();
    for (int i = 0; i < marks.size(); i++) {
      Mark mark = marks.get(i);
      if (mark.getOperation() != Mark.Operation.LINK) {
        ret = -1;
        break;
      } else if (mark.getGeneration() >>> OFFSET != mark.getLinkId()) {
        ret = -3;
        break;
      } else {
        // keep going
      }
    }
    r.r1 = ret;
  }
}
