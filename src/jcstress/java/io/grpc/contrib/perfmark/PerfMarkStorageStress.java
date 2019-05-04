package io.grpc.contrib.perfmark;

import java.util.List;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LL_Result;

/**
 * Simulates the PerfMarkStorage racy reader.
 */
@JCStressTest
@Outcome(id = "0, 3", expect = Expect.ACCEPTABLE, desc = "0 Writes")
@Outcome(id = "65536, 1", expect = Expect.ACCEPTABLE, desc = "1 Writes")
@Outcome(id = "131072, 2", expect = Expect.ACCEPTABLE, desc = "2 Writes")
@Outcome(id = "262144, 4", expect = Expect.ACCEPTABLE, desc = "3 Writes")
@Outcome(id = "524288, 8", expect = Expect.ACCEPTABLE, desc = "4 writes")
@Outcome(id = "1048576, 16", expect = Expect.ACCEPTABLE, desc = "5 writes")
@Outcome(id = "-2, -2", expect = Expect.FORBIDDEN, desc = "Wrong Type")
@State
@Description("Simulates the PerfMarkStorage reader.")
public class PerfMarkStorageStress {
  private static final int OFFSET;
  static {
    OFFSET = 16;
    assert PerfMark.GEN_OFFSET <= OFFSET;
  }

  private static final Thread noThread = new Thread();

  private static final PerfMarkStorage.MarkHolder holder = new PerfMarkStorage.MarkHolder(noThread);

  public static volatile boolean spoiler;
  @Actor
  public void writer() {
    holder.link(1 << OFFSET, 1, Marker.NONE);
    if (spoiler) return;
    holder.link(2 << OFFSET, 2, Marker.NONE);
    if (spoiler) return;
    holder.link(4 << OFFSET, 4, Marker.NONE);
    if (spoiler) return;
    holder.link(8 << OFFSET, 8, Marker.NONE);
    if (spoiler) return;
    holder.link(16 << OFFSET, 16, Marker.NONE);
  }

  @Actor
  public void reader(LL_Result r) {
    MarkList markList = holder.read();
    List<MarkList.Mark> marks = markList.getMarks();
    if (marks.isEmpty()) {
      r.r1 = 0;
      r.r2 = 3;
      return;
    }

    for (int i = 0; i < marks.size(); i++) {
      MarkList.Mark mark = marks.get(i);
      if (mark.getOperation() != MarkList.Mark.Operation.LINK) {
        r.r1 = -2;
        r.r2 = -2;
        return;
      } else if (mark.getMarker() != Marker.NONE) {
        r.r1 = -2;
        r.r2 = -2;
        return;
      } else if (mark.getGeneration() >>> OFFSET != mark.getTagId()) {
        r.r1 = -3;
        r.r2 = -3;
        return;
      } else {
        r.r1 = mark.getGeneration();
        r.r2 = mark.getTagId();
      }
    }
  }
}
