/*
 * Copyright 2021 Carl Mastrangelo
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

package io.perfmark.apitesting;

import static org.junit.Assert.assertNotNull;

import com.google.common.truth.Truth;
import io.perfmark.PerfMark;
import io.perfmark.traceviewer.TraceEventViewer;
import io.perfmark.tracewriter.TraceEventWriter;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ArtifactTest {
  @Test
  public void hasPackage() throws Exception {
    List<String> reflectiveClasses =
        List.of(
            "io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl",
            "io.perfmark.java9.SecretMarkRecorder$VarHandleMarkRecorder",
            "io.perfmark.java6.SecretMarkRecorder$SynchronizedMarkRecorder",
            "io.perfmark.java7.SecretGenerator$MethodHandleGenerator"
            );
    for (String reflectiveClass : reflectiveClasses) {
      Class<?> clz = Class.forName(reflectiveClass, false, getClass().getClassLoader());
      checkPackage(clz.getPackage());
    }
    checkPackage(PerfMark.class.getPackage());
    checkPackage(TraceEventWriter.class.getPackage());
    checkPackage(TraceEventViewer.class.getPackage());
  }

  private static void checkPackage(Package pkg) {
    Truth.assertWithMessage(pkg.toString()).that(pkg.getImplementationTitle()).contains("PerfMark");
    Truth.assertWithMessage(pkg.toString()).that(pkg.toString()).contains("PerfMark");
    Truth.assertWithMessage(pkg.toString()).that(pkg.isSealed()).isFalse();

    String vers = pkg.getImplementationVersion();
    assertNotNull(vers);
    String[] path = vers.split("\\.", 3);
    Truth.assertThat(path).hasLength(3);
    Truth.assertThat(Long.parseLong(path[0])).isAtLeast(0);
    Truth.assertThat(Long.parseLong(path[1])).isAtLeast(1);
    Truth.assertThat(Long.parseLong(path[2].replace("-SNAPSHOT", ""))).isAtLeast(0);

    Truth.assertThat(pkg.getImplementationVendor()).isNotEmpty();
  }
}
