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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

/**
 * This class records the "header" portion of the method visitor.
 */
class MethodVisitorRecorder extends MethodVisitor {

  private final int VISIT_PARAMETER = 1;
  private final int VISIT_ANNOTATION_DEFAULT = 2;

  private final int ANNOTATION_VISIT = 3;
  private final int ANNOTATION_VISIT_ENUM = 4;
  private final int ANNOTATION_VISIT_ANNOTATION = 5;
  private final int ANNOTATION_VISIT_ARRAY = 6;
  private final int ANNOTATION_VISIT_END = 7;

  private final int VISIT_ANNOTATION = 8;
  private final int VISIT_ANNOTABLE_PARAMETER_COUNT = 9;
  private final int VISIT_PARAMETER_ANNOTATION = 10;
  private final int VISIT_TYPE_ANNOTATION = 11;
  private final int VISIT_ATTRIBUTE = 12;

  private final AnnotationVisitorRecorder annotationVisitorRecorder = new AnnotationVisitorRecorder(null);

  private int opsWidx;
  private int intsWidx;
  private int stringsWidx;
  private int objectsWidx;
  private int booleansWidx;

  private int[] ops = new int[0];
  private String[] strings = new String[0];
  private int[] ints = new int[0];
  private Object[] objects = new Object[0];
  private boolean[] booleans = new boolean[0];

  int firstLine = -1;
  int lastLine = -1;

  MethodVisitorRecorder(MethodVisitor delegate) {
    // Have to pin to a specific version, since the method invocations may be different in a later release
    super(Opcodes.ASM9, delegate);
  }

  /*
   * Docs for Method Visitor Say:
   *
   * A visitor to visit a Java method. The methods of this class must be called in the following order:
   * ( visitParameter )*
   * [ visitAnnotationDefault ]
   * ( visitAnnotation | visitAnnotableParameterCount | visitParameterAnnotation visitTypeAnnotation | visitAttribute )*
   * [ visitCode (
   *     visitFrame | visit<i>X</i>Insn | visitLabel | visitInsnAnnotation | visitTryCatchBlock
   *         | visitTryCatchAnnotation | visitLocalVariable | visitLocalVariableAnnotation | visitLineNumber )*
   *     visitMaxs ]
   * visitEnd.
   *
   *
   * In addition, the visit<i>X</i>Insn and visitLabel methods must be called in the sequential order of the bytecode
   * instructions of the visited code, visitInsnAnnotation must be called after the annotated instruction,
   *  visitTryCatchBlock must be called before the labels passed as arguments have been visited,
   * visitTryCatchBlockAnnotation must be called after the corresponding try catch block has been visited, and the
   * visitLocalVariable, visitLocalVariableAnnotation and visitLineNumber methods must be called after the labels passed
   * as arguments have been visited.
   */

  @Override
  public final void visitParameter(String name, int access) {
    addOp(VISIT_PARAMETER);
    addString(name);
    addInt(access);
    super.visitParameter(name, access);
  }

  @Override
  public final AnnotationVisitor visitAnnotationDefault() {
    addOp(VISIT_ANNOTATION_DEFAULT);
    if (mv == null) {
      return annotationVisitorRecorder;
    }
    return new AnnotationVisitorRecorder(super.visitAnnotationDefault());
  }

  @Override
  public final AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    addOp(VISIT_ANNOTATION);
    addString(descriptor);
    addBoolean(visible);
    if (mv == null) {
      return annotationVisitorRecorder;
    }
    return new AnnotationVisitorRecorder(super.visitAnnotation(descriptor, visible));
  }

  @Override
  public final void visitAnnotableParameterCount(int parameterCount, boolean visible) {
    addOp(VISIT_ANNOTABLE_PARAMETER_COUNT);
    addInt(parameterCount);
    addBoolean(visible);
    super.visitAnnotableParameterCount(parameterCount, visible);
  }

  @Override
  public final AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
    addOp(VISIT_PARAMETER_ANNOTATION);
    addInt(parameter);
    addString(descriptor);
    addBoolean(visible);
    if (mv == null) {
      return annotationVisitorRecorder;
    }
    return new AnnotationVisitorRecorder(super.visitParameterAnnotation(parameter, descriptor, visible));
  }

  @Override
  public final AnnotationVisitor visitTypeAnnotation(
      int typeRef, TypePath typePath, String descriptor, boolean visible) {
    addOp(VISIT_TYPE_ANNOTATION);
    addInt(typeRef);
    addObject(typePath);
    addString(descriptor);
    addBoolean(visible);
    if (mv == null) {
      return annotationVisitorRecorder;
    }
    return new AnnotationVisitorRecorder(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
  }

  @Override
  public final void visitAttribute(Attribute attribute) {
    addOp(VISIT_ATTRIBUTE);
    addObject(attribute);
    super.visitAttribute(attribute);
  }

  @Override
  public final void visitLineNumber(int line, Label start) {
    if (firstLine == -1) {
      firstLine = line;
    }
    lastLine = line;
    super.visitLineNumber(line, start);
  }

  final void replay() {
    if (mv != null) {
      replay(mv);
    }
  }

  final void replay(MethodVisitor delegate) {
    if (delegate == null) {
      return;
    }
    int stringsRidx = 0;
    int intsRidx = 0;
    int objectsRidx = 0;
    int booleansRidx = 0;
    int annoWidx = 0;
    AnnotationVisitor[] visitorStack = new AnnotationVisitor[0];
    for (int opsRidx = 0; opsRidx < opsWidx; opsRidx++) {
      int op = ops[opsRidx];
      switch (op) {
        case VISIT_PARAMETER: {
          String name = getString(stringsRidx++);
          int access = getInt(intsRidx++);
          delegate.visitParameter(name, access);
          break;
        }
        case VISIT_ANNOTATION_DEFAULT: {
          AnnotationVisitor visitor = delegate.visitAnnotationDefault();
          visitorStack = addAnnotationVisitor(visitorStack, annoWidx++, visitor);
          break;
        }
        case ANNOTATION_VISIT: {
          AnnotationVisitor currentVisitor = visitorStack[annoWidx - 1];
          String name = getString(stringsRidx++);
          Object value = getObject(objectsRidx++);
          if (currentVisitor != null) {
            currentVisitor.visit(name, value);
          }
          break;
        }
        case ANNOTATION_VISIT_ENUM: {
          AnnotationVisitor currentVisitor = visitorStack[annoWidx - 1];
          String name = getString(stringsRidx++);
          String descriptor = getString(stringsRidx++);
          String value = getString(stringsRidx++);
          if (currentVisitor != null) {
            currentVisitor.visitEnum(name, descriptor, value);
          }
          break;
        }
        case ANNOTATION_VISIT_ANNOTATION: {
          AnnotationVisitor currentVisitor = visitorStack[annoWidx - 1];
          String name = getString(stringsRidx++);
          String descriptor = getString(stringsRidx++);
          AnnotationVisitor newVisitor = null;
          if (currentVisitor != null) {
            newVisitor = currentVisitor.visitAnnotation(name, descriptor);
          }
          visitorStack = addAnnotationVisitor(visitorStack, annoWidx++, newVisitor);
          break;
        }
        case ANNOTATION_VISIT_ARRAY: {
          AnnotationVisitor currentVisitor = visitorStack[annoWidx - 1];
          String name = getString(stringsRidx++);
          AnnotationVisitor newVisitor = null;
          if (currentVisitor != null) {
            newVisitor = currentVisitor.visitArray(name);
          }
          visitorStack = addAnnotationVisitor(visitorStack, annoWidx++, newVisitor);
          break;
        }
        case ANNOTATION_VISIT_END: {
          AnnotationVisitor currentVisitor = visitorStack[annoWidx - 1];
          visitorStack[--annoWidx] = null;
          if (currentVisitor != null) {
            currentVisitor.visitEnd();
          }
          break;
        }
        case VISIT_ANNOTATION: {
          String descriptor = getString(stringsRidx++);
          boolean visible = getBoolean(booleansRidx++);
          AnnotationVisitor newVisitor = delegate.visitAnnotation(descriptor, visible);
          visitorStack = addAnnotationVisitor(visitorStack, annoWidx++, newVisitor);
          break;
        }
        case VISIT_ANNOTABLE_PARAMETER_COUNT: {
          int parameterCount = getInt(intsRidx++);
          boolean visible = getBoolean(booleansRidx++);
          delegate.visitAnnotableParameterCount(parameterCount, visible);
          break;
        }
        case VISIT_PARAMETER_ANNOTATION: {
          int parameter = getInt(intsRidx++);
          String descriptor = getString(stringsRidx++);
          boolean visible = getBoolean(booleansRidx++);
          AnnotationVisitor newVisitor = delegate.visitParameterAnnotation(parameter, descriptor, visible);
          visitorStack = addAnnotationVisitor(visitorStack, annoWidx++, newVisitor);
          break;
        }
        case VISIT_TYPE_ANNOTATION: {
          // (int typeRef, TypePath typePath, String descriptor, boolean visible)
          int typeRef = getInt(intsRidx++);
          TypePath typePath = (TypePath) getObject(objectsRidx++);
          String descriptor = getString(stringsRidx++);
          boolean visible = getBoolean(booleansRidx++);
          AnnotationVisitor newVisitor = delegate.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
          visitorStack = addAnnotationVisitor(visitorStack, annoWidx++, newVisitor);
          break;
        }
        case VISIT_ATTRIBUTE: {
          Attribute attribute = (Attribute) getObject(objectsRidx++);
          delegate.visitAttribute(attribute);
          break;
        }
        default:
          throw new AssertionError("Bad op " + op);
      }
    }
  }

  private void addOp(int op) {
    ops = addInt(ops, opsWidx++, op);
  }

  private void addInt(int value) {
    ints = addInt(ints, intsWidx++, value);
  }

  private static int[] addInt(int[] dest, int pos, int value) {
    if (dest.length == pos){
      int[] newDest = new int[1 + dest.length * 2];
      System.arraycopy(dest, 0, newDest, 0, dest.length);
      dest = newDest;
    }
    dest[pos] = value;
    return dest;
  }

  private void addString(String value) {
    strings = addString(strings, stringsWidx++, value);
  }

  private static String[] addString(String[] dest, int pos, String value) {
    if (dest.length == pos){
      String[] newDest = new String[1 + dest.length * 2];
      System.arraycopy(dest, 0, newDest, 0, dest.length);
      dest = newDest;
    }
    dest[pos] = value;
    return dest;
  }

  private void addObject(Object value) {
    objects = addObject(objects, objectsWidx++, value);
  }

  private static Object[] addObject(Object[] dest, int pos, Object value) {
    if (dest.length == pos){
      Object[] newDest = new Object[1 + dest.length * 2];
      System.arraycopy(dest, 0, newDest, 0, dest.length);
      dest = newDest;
    }
    dest[pos] = value;
    return dest;
  }

  private void addBoolean(boolean value) {
    booleans = addBoolean(booleans, booleansWidx++, value);
  }

  private static boolean[] addBoolean(boolean[] dest, int pos, boolean value) {
    if (dest.length == pos){
      boolean[] newDest = new boolean[1 + dest.length * 2];
      System.arraycopy(dest, 0, newDest, 0, dest.length);
      dest = newDest;
    }
    dest[pos] = value;
    return dest;
  }

  private static AnnotationVisitor[] addAnnotationVisitor(AnnotationVisitor[] dest, int pos, AnnotationVisitor value) {
    if (dest.length == pos){
      AnnotationVisitor[] newDest = new AnnotationVisitor[1 + dest.length * 2];
      System.arraycopy(dest, 0, newDest, 0, dest.length);
      dest = newDest;
    }
    dest[pos] = value;
    return dest;
  }

  private int getInt(int ridx) {
    assert ridx < intsWidx;
    return ints[ridx];
  }

  private String getString(int ridx) {
    assert ridx < stringsWidx;
    return strings[ridx];
  }

  private Object getObject(int ridx) {
    assert ridx < objectsWidx;
    return objects[ridx];
  }

  private boolean getBoolean(int ridx) {
    assert ridx < booleansWidx;
    return booleans[ridx];
  }

  private final class AnnotationVisitorRecorder extends AnnotationVisitor {

    AnnotationVisitorRecorder(AnnotationVisitor delegate) {
      super(MethodVisitorRecorder.this.api, delegate);
    }

    /*
     * Docs for AnnotationVisitor say
     *
     * A visitor to visit a Java annotation. The methods of this class must be called in the following order:
     * ( visit | visitEnum | visitAnnotation | visitArray )* visitEnd.
     */

    @Override
    public void visit(String name, Object value) {
      addOp(ANNOTATION_VISIT);
      addString(name);
      addObject(value);
      super.visit(name, value);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
      addOp(ANNOTATION_VISIT_ENUM);
      addString(name);
      addString(descriptor);
      addString(value);
      super.visitEnum(name, descriptor, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      addOp(ANNOTATION_VISIT_ANNOTATION);
      addString(name);
      addString(descriptor);
      if (av == null) {
        return this;
      }
      return new AnnotationVisitorRecorder(super.visitAnnotation(name, descriptor));
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      addOp(ANNOTATION_VISIT_ARRAY);
      addString(name);
      if (av == null) {
        return this;
      }
      return new AnnotationVisitorRecorder(super.visitArray(name));
    }

    @Override
    public void visitEnd() {
      addOp(ANNOTATION_VISIT_END);
      super.visitEnd();
    }
  }
}
