package io.perfmark.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
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

  private static final String SRC_OWNER = "io/perfmark/PerfMark";
  private static final String DST_OWNER = "io/perfmark/impl/SecretPerfMarkImpl$PerfMarkImpl";
  private static final Map<? extends List<String>, ? extends List<String>> REWRITE_MAP = buildMap();

  private static final Map<List<String>, List<String>> buildMap() {
    Map<List<String>, List<String>> map = new HashMap<>();
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(
            DST_OWNER,
            "startTask",
            "(Ljava/lang/String;Lio/perfmark/Tag;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "startTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "startTask", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(
            DST_OWNER,
            "stopTask",
            "(Ljava/lang/String;Lio/perfmark/Tag;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "stopTask", "(Ljava/lang/String;)V"),
        Arrays.asList(DST_OWNER, "stopTask", "(Ljava/lang/String;Lio/perfmark/impl/Marker;)V"));
    map.put(
        Arrays.asList(SRC_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/Tag;)V"),
        Arrays.asList(
            DST_OWNER, "event", "(Ljava/lang/String;Lio/perfmark/Tag;Lio/perfmark/impl/Marker;)V"));
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

    PerfMarkClassVisitor(String className, ClassWriter cw) {
      super(Opcodes.ASM7, cw);
      this.className = className;
    }

    private static String deriveFileName(String className) {
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
      return new PerfMarkMethodVisitor(
          className,
          name,
          fileName,
          Opcodes.ASM7,
          super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    private static final class PerfMarkMethodVisitor extends MethodVisitor {
      private final String className;
      private final String methodName;
      private final String fileName;

      private int line = -1;

      PerfMarkMethodVisitor(
          String className,
          String methodName,
          String fileName,
          int api,
          MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
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
            // markers.add(new StackTraceElement(className, methodName, fileName, line));
          }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }
    }
  }

  PerfMarkTransformer() {}
}
