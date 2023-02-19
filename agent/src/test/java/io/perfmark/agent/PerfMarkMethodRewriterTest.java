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

package io.perfmark.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.truth.Truth;
import io.perfmark.PerfMark;
import io.perfmark.TaskCloseable;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@RunWith(JUnit4.class)
public class PerfMarkMethodRewriterTest {

  @Test
  public void getStackTraceElementCtor_present() {
    Constructor<StackTraceElement> ctor = PerfMarkMethodRewriter.getStackTraceElementCtor();
    assertNotNull(ctor);
  }

  @Test
  public void moduleElement() throws Exception {
    PerfMarkMethodRewriter rewriter =
        new PerfMarkMethodRewriter(
            "loadername", "modulename", "moduleversion", "classname", "methodname", "filename", () -> {}, null);
    rewriter.visitLineNumber(1234, new Label());
    StackTraceElement expected =
        new StackTraceElement("loadername", "modulename", "moduleversion", "classname", "methodname", "filename", 1234);

    StackTraceElement ste = rewriter.moduleElement();

    assertEquals(expected, ste);
  }

  @Test
  public void moduleElement_oldClass() throws Exception {
    PerfMarkMethodRewriter rewriter =
        new PerfMarkMethodRewriter(
            null, null, null, "classname", "methodname", null, () -> {}, null);
    rewriter.visitLineNumber(1234, new Label());
    StackTraceElement expected =
        new StackTraceElement("classname", "methodname", null, 1234);

    StackTraceElement ste = rewriter.moduleElement();

    assertEquals(expected, ste);
  }

  @Test
  public void rewriteClass_task() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForThread();
    String expectedValue =
        new StackTraceElement("loadername", "modulename", "moduleversion", "classname", "methodname", "filename", -1)
            .toString();
    // remove the trailing paren ) to make string match easier.
    expectedValue = expectedValue.substring(0, expectedValue.length() - 1);
    ClassReader reader = new ClassReader(ClzToRewrite.class.getName());
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(
          int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new PerfMarkMethodRewriter(
                "loadername", "modulename", "moduleversion", "classname", "methodname", "filename", () -> {}, delegate);
      }
    }, 0);
    Class<?> clz = MethodHandles.lookup().defineHiddenClass(writer.toByteArray(), false).lookupClass();

    clz.getMethod("task").invoke(null);

    MarkList out = Storage.readForTest();
    Truth.assertThat(out).hasSize(4);
    String start = out.get(1).getTagStringValue();
    Truth.assertThat(start).startsWith(expectedValue);
    String end = out.get(2).getTagStringValue();
    // Hack to check the line number changed.
    Truth.assertThat(start).isNotEqualTo(end);
  }

  @Test
  public void rewriteClass_closeable() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForThread();
    String expectedValue =
        new StackTraceElement("loadername", "modulename", "moduleversion", "classname", "methodname", "filename", -1)
            .toString();
    // remove the trailing paren ) to make string match easier.
    expectedValue = expectedValue.substring(0, expectedValue.length() - 1);
    ClassReader reader = new ClassReader(ClzToRewrite.class.getName());
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(
          int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new PerfMarkMethodRewriter(
            "loadername", "modulename", "moduleversion", "classname", "methodname", "filename", () -> {}, delegate);
      }
    }, 0);
    Class<?> clz = MethodHandles.lookup().defineHiddenClass(writer.toByteArray(), false).lookupClass();

    clz.getMethod("closeable").invoke(null);

    MarkList out = Storage.readForTest();
    Truth.assertThat(out).hasSize(4);
    String start = out.get(1).getTagStringValue();
    Truth.assertThat(start).startsWith(expectedValue);
    String end = out.get(2).getTagStringValue();
    // Hack to check the line number changed.
    Truth.assertThat(start).isNotEqualTo(end);
  }

  @SuppressWarnings("UnusedMethod")
  private static final class ClzToRewrite {
    public static void task() {
      // These two calls MUST happen on separate lines to ensure line reading works.
      PerfMark.startTask("task");
      PerfMark.stopTask("task");
    }

    public static void closeable() {
      try (TaskCloseable closeable = PerfMark.traceTask("task")) {
        // extra line to ensure line numbers are different.
      }
    }
  }
}
