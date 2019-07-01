package io.perfmark.agent;

import java.lang.instrument.ClassFileTransformer;
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

final class PerfMarkTransformer implements ClassFileTransformer {
  private static final String MARKER_FACTORY = "io/perfmark/impl/SecretMarkerFactory$MarkerFactory";

  private static final String SRC_OWNER = "io/perfmark/PerfMark";
  private static final String DST_OWNER = "io/perfmark/impl/SecretPerfMarkImpl$PerfMarkImpl";
  private static final Map<List<String>, List<String>> REWRITE_MAP = buildMap();

  private static Map<List<String>, List<String>> buildMap() {
    Map<List<String>, List<String>> map = new HashMap<>();
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "startTask", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(
            DST_OWNER,
            "startTask",
            "(Ljava/lang/String;Lio/perfmark/Tag;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "stopTask", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(
            DST_OWNER,
            "stopTask",
            "(Ljava/lang/String;Lio/perfmark/Tag;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "event", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(
            DST_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/Tag;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "linkOut", "()Lio/perfmark/Link;"),
        Arrays.asList(DST_OWNER, "linkOut", "(Lio/perfmark/impl/Marker;)Lio/perfmark/Link;"));
    map.put(
        Arrays.asList(SRC_OWNER, "linkIn", "(Lio/perfmark/Link;)V"),
        Arrays.asList(DST_OWNER, "linkIn", "(Lio/perfmark/Link;Lio/perfmark/impl/Marker;)V"));
    return Collections.unmodifiableMap(map);
  }

  @Override
  public byte[] transform(
      ClassLoader loader,
      final String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    ClassReader cr = new ClassReader(classfileBuffer);
    PerfMarkClassReader perfMarkReader = new PerfMarkClassReader(Opcodes.ASM7, className);
    cr.accept(perfMarkReader, ClassReader.SKIP_FRAMES);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cr.accept(
        new PerfMarkMethodRewriter(perfMarkReader, className, cw),
        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return cw.toByteArray();
  }

  static final class PerfMarkClassReader extends ClassVisitor {
    final String className;

    String fileName;
    boolean clinitSeen;
    List<StackTraceElement> matches = new ArrayList<>();

    PerfMarkClassReader(int api, String className) {
      super(api);
      this.className = className;
      this.fileName = deriveFileName(className);
    }

    int api() {
      return api;
    }

    @Override
    public void visitSource(String sourceFileName, String debug) {
      this.fileName = sourceFileName;
      super.visitSource(sourceFileName, debug);
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      clinitSeen = clinitSeen || name.equals("<clinit>");
      return new PerfMarkMethodVisitor(
          super.visitMethod(access, name, descriptor, signature, exceptions), name);
    }

    private final class PerfMarkMethodVisitor extends MethodVisitor {
      private final String methodName;

      private int lineNumber = -1;

      PerfMarkMethodVisitor(MethodVisitor methodVisitor, String methodName) {
        super(PerfMarkClassReader.this.api, methodVisitor);
        this.methodName = methodName;
      }

      @Override
      public void visitLineNumber(int line, Label start) {
        this.lineNumber = line;
        super.visitLineNumber(line, start);
      }

      @Override
      public void visitMethodInsn(
          int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (opcode == Opcodes.INVOKESTATIC && owner.equals(SRC_OWNER)) {
          if (REWRITE_MAP.containsKey(Arrays.asList(owner, name, descriptor))) {
            matches.add(new StackTraceElement(className, methodName, fileName, lineNumber));
          }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }
    }

    static String deriveFileName(String className) {
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
      return fileName;
    }
  }

  static final class PerfMarkMethodRewriter extends ClassVisitor {
    int fieldId;
    final PerfMarkClassReader perfmarkClassReader;
    final String className;

    PerfMarkMethodRewriter(
        PerfMarkClassReader perfmarkClassReader, String className, ClassWriter cw) {
      super(perfmarkClassReader.api(), cw);
      this.perfmarkClassReader = perfmarkClassReader;
      this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      return new PerfMarkMethodVisitor(
          perfmarkClassReader.api(),
          super.visitMethod(access, name, descriptor, signature, exceptions),
          name.equals("<clinit>"));
    }

    @Override
    public void visitEnd() {
      if (!perfmarkClassReader.clinitSeen && !perfmarkClassReader.matches.isEmpty()) {
        MethodVisitor mv =
            visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "<clinit>", "()V", null, new String[0]);
        mv.visitAnnotationDefault();
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(Integer.MAX_VALUE, Integer.MAX_VALUE);
        mv.visitEnd();
      }
      super.visitEnd();
    }

    private final class PerfMarkMethodVisitor extends MethodVisitor {
      private static final String FIELD_PREFIX = "IO_PERFMARK_MARKER->";
      private final boolean isClinit;

      PerfMarkMethodVisitor(int api, MethodVisitor methodVisitor, boolean isClinit) {
        super(api, methodVisitor);
        this.isClinit = isClinit;
      }

      @Override
      public void visitCode() {
        if (isClinit) {
          for (int i = 0; i < perfmarkClassReader.matches.size(); i++) {
            StackTraceElement match = perfmarkClassReader.matches.get(i);
            visitTypeInsn(Opcodes.NEW, "java/lang/StackTraceElement");
            visitInsn(Opcodes.DUP);
            visitLdcInsn(match.getClassName());
            visitLdcInsn(match.getMethodName());
            visitLdcInsn(match.getFileName());
            visitLdcInsn(match.getLineNumber());
            super.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StackTraceElement",
                "<init>",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V",
                false);
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                MARKER_FACTORY,
                "createMarker",
                "(Ljava/lang/StackTraceElement;)Lio/perfmark/impl/Marker;",
                false);
            String fieldName = FIELD_PREFIX + i;
            visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                fieldName,
                "Lio/perfmark/impl/Marker;",
                null,
                null);
            visitFieldInsn(
                Opcodes.PUTSTATIC,
                className.replace('.', '/'),
                fieldName,
                "Lio/perfmark/impl/Marker;");
          }
        }
        super.visitCode();
      }

      @Override
      public void visitMethodInsn(
          int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (opcode == Opcodes.INVOKESTATIC && owner.equals(SRC_OWNER)) {
          List<String> dest = REWRITE_MAP.get(Arrays.asList(owner, name, descriptor));
          if (dest != null) {
            assert dest.size() == 3;
            owner = dest.get(0);
            name = dest.get(1);
            descriptor = dest.get(2);
            int id = fieldId++;
            String fieldName = FIELD_PREFIX + id;
            visitFieldInsn(
                Opcodes.GETSTATIC,
                className.replace('.', '/'),
                fieldName,
                "Lio/perfmark/impl/Marker;");
          }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }
    }
  }

  PerfMarkTransformer() {}
}
