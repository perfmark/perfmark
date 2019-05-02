package io.grpc.contrib.perfmark;

import java.util.concurrent.atomic.AtomicLong;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Simulates the PerfMarkStorage racy reader.
 */
@JCStressTest
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Default outcome.")
@Outcome(id = "2", expect = Expect.ACCEPTABLE_INTERESTING, desc = "No data outcome.")
@Outcome(id = "3", expect = Expect.FORBIDDEN, desc = "race")
@State
public class PerfMarkStorageStress {

  private static final int SIZE = 128; // must be a power of two
  private static final long MOD = SIZE - 3;
  private static final long MASK = SIZE - 1;

  // Writer state
  private static final byte[] writerData = new byte[SIZE];
  private static final AtomicLong writerIdx = new AtomicLong();

  // Cached reader state
  private static final byte[] readerData = new byte[SIZE];

  @Actor
  public void writer() {
    long idx = writerIdx.get();
    int i = (int) (idx & MASK);
    // Make sure to write different data each time through.
    writerData[i] = (byte) (idx % MOD);
    writerIdx.lazySet(idx + 1);
  }

  @Actor
  public void reader(I_Result r) {
    long startIdx = writerIdx.get();
    System.arraycopy(writerData, 0, readerData, 0, SIZE);
    long endIdx = writerIdx.get();
    int copy = (int) Math.min(startIdx, SIZE);
    // Always drop the last value, as the writerData may have been modified without updating idx.
    long elementsToDrop = endIdx - startIdx + 1;
    int ret = 2;
    for (int k = 0; k < copy - elementsToDrop; k++) {
      long idx = startIdx - k - 1;
      int i = (int) (idx & MASK);
      if (readerData[i] == (byte)(idx % MOD)) {
        ret = 1;
      } else {
        ret = 3;
        break;
      }
    }
    r.r1 = ret;
  }
}
