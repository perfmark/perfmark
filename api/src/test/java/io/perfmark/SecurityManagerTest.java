/*
 * Copyright 2022 Google LLC
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

package io.perfmark;

import com.google.common.truth.Truth;
import io.perfmark.impl.Mark;
import io.perfmark.impl.Storage;
import java.io.FilePermission;
import java.security.Permission;
import java.util.List;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings("removal") // Security Manager is soon to be removed, but it still works.
public class SecurityManagerTest {

  @Test
  public void worksWithSecurityManager_startEnabled_noDebug() throws Exception {
    ClassLoader loader = new PerfMarkTest.TestClassLoader(getClass().getClassLoader());

    SecurityManager oldMgr = System.getSecurityManager();
    HesitantSecurityManager newMgr =
        new HesitantSecurityManager() {
          @Override
          public void checkPermission(Permission perm) {
            if (perm instanceof PropertyPermission) {
              if (perm.getName().equals("*")) {
                return;
              }
              if (perm.getName().equals("io.perfmark.PerfMark.startEnabled")
                  && perm.getActions().equals("read")) {
                return;
              }
            }
            super.checkPermission(perm);
          }
        };

    Class<?> clz =
        PerfMarkTest.runWithProperty(
            System.getProperties(),
            "io.perfmark.PerfMark.startEnabled",
            "true",
            () -> {
              try {
                System.setSecurityManager(newMgr);
                Class<?> clz2 = Class.forName(PerfMark.class.getName(), true, loader);
                clz2.getMethod("event", String.class).invoke(null, "event");
                return clz2;
              } finally {
                newMgr.unload = true;
                System.setSecurityManager(oldMgr);
              }
            });

    Class<?> storageClass = Class.forName(Storage.class.getName(), true, clz.getClassLoader());
    List<Mark> marks = (List<Mark>) storageClass.getMethod("readForTest").invoke(null);
    Truth.assertThat(marks).hasSize(1);
  }

  @Test
  public void worksWithSecurityManager_noStartEnabled_debug() throws Exception {
    ClassLoader loader = new PerfMarkTest.TestClassLoader(getClass().getClassLoader());

    SecurityManager oldMgr = System.getSecurityManager();
    HesitantSecurityManager newMgr =
        new HesitantSecurityManager() {
          @Override
          public void checkPermission(Permission perm) {
            if (perm instanceof PropertyPermission) {
              if (perm.getName().equals("*")) {
                return;
              }
              if (perm.getName().equals("io.perfmark.PerfMark.debug")
                  && perm.getActions().contains("read")) {
                return;
              }
            }
            super.checkPermission(perm);
          }
        };

    // TODO check logging occurred.

    Class<?> clz =
        PerfMarkTest.runWithProperty(
            System.getProperties(),
            "io.perfmark.PerfMark.debug",
            "true",
            () -> {
              try {
                System.setSecurityManager(newMgr);
                Class<?> clz2 = Class.forName(PerfMark.class.getName(), true, loader);
                clz2.getMethod("setEnabled", boolean.class).invoke(null, true);
                clz2.getMethod("event", String.class).invoke(null, "event");
                return clz2;
              } finally {
                newMgr.unload = true;
                System.setSecurityManager(oldMgr);
              }
            });

    Class<?> storageClass = Class.forName(Storage.class.getName(), true, clz.getClassLoader());
    List<Mark> marks = (List<Mark>) storageClass.getMethod("readForTest").invoke(null);
    Truth.assertThat(marks).hasSize(1);
  }

  @Test
  public void worksWithSecurityManager_noStartEnabled_noDebug() throws Exception {
    ClassLoader loader = new PerfMarkTest.TestClassLoader(getClass().getClassLoader());

    SecurityManager oldMgr = System.getSecurityManager();
    HesitantSecurityManager newMgr = new HesitantSecurityManager();
    Class<?> clz;
    try {
      System.setSecurityManager(newMgr);
      clz = Class.forName(PerfMark.class.getName(), true, loader);
      clz.getMethod("setEnabled", boolean.class).invoke(null, true);
      clz.getMethod("event", String.class).invoke(null, "event");
    } finally {
      newMgr.unload = true;
      System.setSecurityManager(oldMgr);
    }

    Class<?> storageClass = Class.forName(Storage.class.getName(), true, clz.getClassLoader());
    List<Mark> marks = (List<Mark>) storageClass.getMethod("readForTest").invoke(null);
    Truth.assertThat(marks).hasSize(1);
  }

  private static class HesitantSecurityManager extends SecurityManager {
    boolean unload;

    @Override
    public void checkPermission(Permission perm) {
      if (unload && perm.getName().equals("setSecurityManager")) {
        return;
      }
      if (perm instanceof FilePermission) {
        FilePermission fp = (FilePermission) perm;
        if ("read".equals(fp.getActions())) {
          if (fp.getName().endsWith(".class") && fp.getName().contains("io/perfmark/")) {
            return;
          }
          if (fp.getName().endsWith(".jar") && fp.getName().contains("/perfmark/")) {
            return;
          }
        }
      }
      if (perm instanceof PropertyPermission) {
        if (perm.getName().equals("java.util.logging.manager")
            && perm.getActions().equals("read")) {
          return;
        }
      }
      for (StackTraceElement element : new Throwable().getStackTrace()) {
        if (element.getClassName().equals(PerfMarkTest.TestClassLoader.class.getName())) {
          if (perm.getName().equals("suppressAccessChecks")) {
            return;
          }
          if (perm.getName().equals("accessSystemModules")) {
            return;
          }
        }
        if (element.getClassName().equals("java.util.logging.Level")) {
          if (perm.getName().equals("suppressAccessChecks")) {
            return;
          }
          if (perm.getName().equals("accessSystemModules")) {
            return;
          }
        }
        if (element.getClassName().equals("java.util.logging.LogManager")) {
          if (perm.getName().equals("shutdownHooks")) {
            return;
          }
          if (perm.getName().equals("setContextClassLoader")) {
            return;
          }
          if (perm instanceof LoggingPermission && perm.getName().equals("control")) {
            return;
          }
        }
        if (element.getClassName().equals("java.util.logging.Logger")) {
          if (perm.getName().equals("sun.util.logging.disableCallerCheck")) {
            return;
          }
          if (perm.getName().equals("getClassLoader")) {
            return;
          }
        }
      }

      super.checkPermission(perm);
    }
  }
}
