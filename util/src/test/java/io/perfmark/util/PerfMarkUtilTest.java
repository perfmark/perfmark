package io.perfmark.util;

import io.perfmark.PerfMark;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PerfMarkUtilTest {

  @Test
  public void record() throws Exception {
    PerfMark.setEnabled(true);
    PerfMarkUtil.recordTask(
        "hi", PerfMark.createTag("hello"), () -> LockSupport.parkNanos(this, 100));

    try (PerfMarkUtil.TaskRecorder tr = PerfMarkUtil.recordTask("yes")) {}

    List<MarkList> data = Storage.read();
    System.out.println(data);
  }
}
