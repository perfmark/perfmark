package io.perfmark.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
  private static final Map<? extends List<String>, ? extends List<String>> REWRITE_MAP = buildMap();

  private static final Map<List<String>, List<String>> buildMap() {
    Map<List<String>, List<String>> map = new HashMap<>();
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "startTask", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "stopTask", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "event", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
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
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cr.accept(new PerfMarkClassVisitor(className, cw), 0);
    return cw.toByteArray();
  }

  static final class PerfMarkClassVisitor extends ClassVisitor {
    private final String className;
    private String fileName;
    private boolean fileNameChecked;
    int fieldId;
    Map<Integer, StackTraceElement> callSites = new TreeMap<>();
    boolean clinitSeen;

    PerfMarkClassVisitor(String className, ClassWriter cw) {
      super(Opcodes.ASM7, cw);
      this.className = className;
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

    @Override
    public void visitSource(String sourceFileName, String debug) {
      this.fileName = sourceFileName;
      this.fileNameChecked = true;
      super.visitSource(fileName, debug);
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String descriptor, String signature, String[] exceptions) {
      if (!fileNameChecked) {
        fileName = deriveFileName(className);
        fileNameChecked = true;
      }
      clinitSeen |= name.equals("<clinit>");

      return new PerfMarkMethodVisitor(
          className,
          name,
          fileName,
          Opcodes.ASM7,
          this,
          super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    @Override
    public void visitEnd() {
      if (!clinitSeen) {
        MethodVisitor mv =
            visitMethod(
                Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "<clinit>", "()V", null, new String[0]);
        mv.visitAnnotationDefault();
        mv.visitCode();
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv.visitInsn(Opcodes.RETURN);
      }
      {
        MethodVisitor mv =
            super.visitMethod(
                Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "__io_perfmark_clinit", "()V", null, new String[0]);
        mv.visitAnnotationDefault();
        mv.visitCode();
        for (Map.Entry<Integer, StackTraceElement> entry : callSites.entrySet()) {
          mv.visitTypeInsn(Opcodes.NEW, "java/lang/StackTraceElement");
          mv.visitInsn(Opcodes.DUP);
          mv.visitLdcInsn(entry.getValue().getClassName());
          mv.visitLdcInsn(entry.getValue().getMethodName());
          mv.visitLdcInsn(entry.getValue().getFileName());
          mv.visitLdcInsn(entry.getValue().getLineNumber());
          mv.visitMethodInsn(
              Opcodes.INVOKESPECIAL,
              "java/lang/StackTraceElement",
              "<init>",
              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V",
              false);
          mv.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              MARKER_FACTORY,
              "createMarker",
              "(Ljava/lang/StackTraceElement;)Lio/perfmark/impl/Marker;",
              false);
          String fieldName = "IO_PERFMARK_MARKER<" + entry.getKey() + '>';
          mv.visitFieldInsn(
              Opcodes.PUTSTATIC, className.replace('.', '/'), fieldName, "Lio/perfmark/impl/Marker;");
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }


      super.visitEnd();
    }

    private static final class PerfMarkMethodVisitor extends MethodVisitor {
      private final PerfMarkClassVisitor visitor;
      private final String className;
      private final String methodName;
      private final String fileName;
      private final boolean isClinit;

      private int line = -1;

      PerfMarkMethodVisitor(
          String className,
          String methodName,
          String fileName,
          int api,
          PerfMarkClassVisitor visitor,
          MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        this.visitor = visitor;
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.isClinit = methodName.equals("<clinit>");
      }

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
            if (isClinit) {
              visitTypeInsn(Opcodes.NEW, "java/lang/StackTraceElement");
              visitInsn(Opcodes.DUP);
              visitLdcInsn(className);
              visitLdcInsn(methodName);
              visitLdcInsn(fileName);
              visitLdcInsn(line);
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
            } else {
              int fieldId = visitor.fieldId++;
              visitor.callSites.put(
                  fieldId, new StackTraceElement(className, methodName, fileName, line));
              String fieldName = "IO_PERFMARK_MARKER<" + fieldId + '>';
              visitor.visitField(
                  Opcodes.ACC_PRIVATE  | Opcodes.ACC_STATIC,
                  fieldName,
                  "Lio/perfmark/impl/Marker;",
                  null,
                  null);
              visitFieldInsn(
                  Opcodes.GETSTATIC,
                  className.replace('.', '/'),
                  fieldName,
                  "Lio/perfmark/impl/Marker;");
            }
            assert dest.size() == 3 : "Wrong size for dest" + dest;
            owner = dest.get(0);
            name = dest.get(1);
            descriptor = dest.get(2);
          }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }

      @Override
      public void visitEnd() {
        if (isClinit) {
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              className.replace('.', '/'),
              "__io_perfmark_clinit",
              "()V",
              false);
        }
        super.visitEnd();
      }
    }
  }

  PerfMarkTransformer() {}
}
