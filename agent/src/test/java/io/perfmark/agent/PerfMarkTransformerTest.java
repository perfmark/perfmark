package io.perfmark.agent;

import static org.junit.Assert.assertEquals;

import io.perfmark.PerfMark;
import io.perfmark.impl.MarkList;
import io.perfmark.impl.Storage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import io.perfmark.agent.PerfMarkTransformer.PerfMarkClassVisitor;

@RunWith(JUnit4.class)
public class PerfMarkTransformerTest {

  @Test
  public void deriveFileName() {
    String file = PerfMarkClassVisitor.deriveFileName("io/perfmark/Foo");

    assertEquals("Foo.java", file);
  }

  @Test
  public void deriveFileName_innerClass() {
    String file = PerfMarkClassVisitor.deriveFileName("io/perfmark/Foo$Bar");

    assertEquals("Foo.java", file);
  }

  static final class FooClinit {
    static {
      PerfMark.startTask("hi");
      PerfMark.stopTask("hi");
    }
  }

  static final class Foo {
    {
      PerfMark.startTask("hi");
      PerfMark.stopTask("hi");
    }
  }

  @Test
  public void transform_ctor() throws Exception {
    PerfMark.setEnabled(true);
    Storage.resetForTest();

    class FooLocal {
      {
        PerfMark.startTask("hi");
        PerfMark.stopTask("hi");
      }
    }

    Class<?> clz = transformAndLoad(Foo.class);
    Constructor<?> ctor = clz.getDeclaredConstructor();
    ctor.setAccessible(true);
    ctor.newInstance();
    List<MarkList> markLists = Storage.read();
    System.out.println(markLists);

  }

  private static byte[] getBytes(Class<?> clz) throws IOException {
    String className = clz.getName().replace('.', '/') + ".class";
    return clz.getClassLoader().getResourceAsStream(className).readAllBytes();
  }

  private static final class TestClassLoader extends ClassLoader {
    TestClassLoader() {
      super(PerfMarkTransformerTest.class.getClassLoader());
    }

    Class<?> defineClass(String name, byte[] data) {
      return defineClass(name, data, 0, data.length);
    }
  }

  private static Class<?> transformAndLoad(Class<?> clz) throws IOException {
    String name = clz.getName();
    TestClassLoader cl = new TestClassLoader();
    byte[] bytes = getBytes(clz);
    byte[] newBytes =
        new PerfMarkTransformer().transform(
            cl, name, clz, /* protectionDomain= */ null, bytes);
    return cl.defineClass(name, newBytes);
  }
}
