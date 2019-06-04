package io.perfmark.agent;

import io.perfmark.PerfMark;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@SuppressWarnings("CatchAndPrintStackTrace") // I don't care for debugging.
public final class Main {

  private static final String MARKER_FACTORY = "io/perfmark/impl/SecretMarkerFactory$MarkerFactory";

  private static final String SRC_OWNER = "io/perfmark/PerfMark";
  private static final String DST_OWNER = "io/perfmark/PerfMark$PackageAccess$Public";
  private static final Map<? extends List<String>, ? extends List<String>> REWRITE_MAP = buildMap();

  public static final class MySelf {

    {
      PerfMark.startTask("hi4");
    }

    static {
      PerfMark.startTask("hi2");
      System.err.println("I BRAN");
    }

    public static void main(String[] args) throws Exception {
      System.err.println("I BRAN");
      PerfMark.startTask("hi");

      class Babs {
        {
          PerfMark.startTask("hi9");

          System.err.println("AQUI");
          Runnable r =
              () -> {
                PerfMark.startTask("hi6");
                System.err.println("ALLI");
              };
          r.run();
        }
      }
      new Babs();

      for (Field a : Babs.class.getDeclaredFields()) {
        a.setAccessible(true);
        System.err.print("Field " + a);
        System.err.println(a.get(null) + "");
      }

      System.err.println("SO FAR AWAY");
    }
  }

  private static final Map<List<String>, List<String>> buildMap() {
    Map<List<String>, List<String>> map = new HashMap<>();
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(DST_OWNER, "startTask", "(Lio/perfmark/impl/Marker;Lio/perfmark/Tag;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "startTask", "(Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(DST_OWNER, "stopTask", "(Lio/perfmark/impl/Marker;Lio/perfmark/Tag;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "stopTask", "(Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(DST_OWNER, "event", "(Lio/perfmark/impl/Marker;Lio/perfmark/Tag;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "event", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "event", "(Lio/perfmark/impl/Marker;)V"));
    return Collections.unmodifiableMap(map);
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(new Transformer());
  }

  private static final class Transformer implements ClassFileTransformer {
    @Override
    public byte[] transform(
        ClassLoader loader,
        final String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer) {
      ClassReader cr = new ClassReader(classfileBuffer);
      final List<StackTraceElement> markers = new ArrayList<>();

      cr.accept(
          new ClassVisitor(Opcodes.ASM7) {

            @Override
            public MethodVisitor visitMethod(
                int access,
                final String methodName,
                String descriptor,
                String signature,
                String[] exceptions) {
              MethodVisitor mv =
                  super.visitMethod(access, methodName, descriptor, signature, exceptions);
              return new MethodVisitor(Opcodes.ASM7, mv) {
                int line;

                @Override
                public void visitLineNumber(int line, Label start) {
                  this.line = line;
                  super.visitLineNumber(line, start);
                }

                @Override
                public void visitMethodInsn(
                    int opcode, String owner, String name, String descriptor, boolean isInterface) {
                  if (SRC_OWNER.equals(owner)) {
                    List<String> dest = REWRITE_MAP.get(Arrays.asList(SRC_OWNER, name, descriptor));
                    if (dest != null) {
                      String clzName = className.replace('/', '.');
                      int dollar = clzName.indexOf('$');
                      String fileName;
                      if (dollar == -1) {
                        fileName = clzName;
                      } else {
                        fileName = clzName.substring(0, dollar);
                      }
                      if (!fileName.isEmpty()) {
                        int dot = fileName.lastIndexOf('.');
                        if (dot != -1) {
                          fileName = fileName.substring(dot + 1);
                        }
                      }
                      // TODO: this is broken for private top level classes.
                      if (!fileName.isEmpty()) {
                        fileName += ".java";
                      } else {
                        fileName = null;
                      }
                      markers.add(new StackTraceElement(clzName, methodName, fileName, line));
                    }
                  }
                  super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
              };
            }
          },
          0);

      ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv =
          new ClassVisitor(Opcodes.ASM7, cw) {
            private boolean markerFieldsReady;
            private boolean clinitVisited;

            private int markerIndex;

            @Override
            public void visitEnd() {
              if (!clinitVisited) {
                clinitVisited = true;
                MethodVisitor mv =
                    visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, new String[0]);
                mv.visitAnnotationDefault();
                mv.visitCode();
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(Integer.MAX_VALUE, Integer.MAX_VALUE);
                mv.visitEnd();
              }
              // todo: validate all constants used
              super.visitEnd();
            }

            @Override
            public MethodVisitor visitMethod(
                int access,
                final String name,
                String descriptor,
                String signature,
                String[] exceptions) {

              if (!markerFieldsReady) {
                markerFieldsReady = true;
                for (int i = 0; i < markers.size(); i++) {
                  String fieldName = "IO_PERFMARK_MARKER<" + i + '>';
                  visitField(
                      Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
                      fieldName,
                      "Lio/perfmark/impl/Marker;",
                      null,
                      null);
                }
              }

              final MethodVisitor mv =
                  super.visitMethod(access, name, descriptor, signature, exceptions);
              return new MethodVisitor(Opcodes.ASM7, mv) {

                @Override
                public void visitCode() {
                  if (name.equals("<clinit>")) {
                    clinitVisited = true;
                    for (int i = 0; i < markers.size(); i++) {
                      visitLdcInsn(
                          markers.get(i).getClassName() + '.' + markers.get(i).getMethodName());
                      visitTypeInsn(Opcodes.NEW, "java/lang/StackTraceElement");
                      visitInsn(Opcodes.DUP);
                      visitLdcInsn(markers.get(i).getClassName());
                      visitLdcInsn(markers.get(i).getMethodName());
                      visitLdcInsn(markers.get(i).getFileName());
                      visitLdcInsn(markers.get(i).getLineNumber());
                      super.visitMethodInsn(
                          Opcodes.INVOKESPECIAL,
                          "java/lang/StackTraceElement",
                          "<init>",
                          "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V",
                          false);
                      String fieldName = "IO_PERFMARK_MARKER<" + i + '>';
                      super.visitMethodInsn(
                          Opcodes.INVOKESTATIC,
                          MARKER_FACTORY,
                          "createMarker",
                          "(Ljava/lang/String;Ljava/lang/StackTraceElement;)Lio/perfmark/impl/Marker;",
                          false);
                      visitFieldInsn(
                          Opcodes.PUTSTATIC, className, fieldName, "Lio/perfmark/impl/Marker;");
                    }
                  }
                  super.visitCode();
                }

                @Override
                public void visitMethodInsn(
                    int opcode, String owner, String name, String descriptor, boolean isInterface) {
                  if (SRC_OWNER.equals(owner)) {
                    List<String> dest = REWRITE_MAP.get(Arrays.asList(SRC_OWNER, name, descriptor));
                    if (dest != null) {
                      int i = markerIndex++;
                      String fieldName = "IO_PERFMARK_MARKER<" + i + '>';

                      visitInsn(Opcodes.POP);
                      visitFieldInsn(
                          Opcodes.GETSTATIC, className, fieldName, "Lio/perfmark/impl/Marker;");

                      assert dest.size() == 3 : "Wrong size for dest" + dest;
                      owner = dest.get(0);
                      name = dest.get(1);
                      descriptor = dest.get(2);
                    }
                  }
                  super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
              };
            }
          };

      cr.accept(cv, 0);
      byte[] bb = cw.toByteArray();
      if (className.contains("perfmark")) {
        try (OutputStream os = new FileOutputStream(new File("/tmp/clz.class"))) {
          os.write(bb);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      return cw.toByteArray();
    }
  }
}
