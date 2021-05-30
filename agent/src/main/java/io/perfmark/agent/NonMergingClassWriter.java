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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * This class exists to avoid the runtime lookup of classes when doing flow analysis.   When
 * computing frames, the default implementation needs to merge types together, which involves
 * finding their common super class.  This makes the transformer accidentally load a bunch of
 * classes before they can be transformed.  To avoid the possibility of this happening this
 * class writer intentionally fails any such attempts. This ensures flags like {@link
 * ClassWriter#COMPUTE_FRAMES} don't trigger merge logic.
 */
final class NonMergingClassWriter extends ClassWriter {
  NonMergingClassWriter(ClassReader classReader, int flags) {
    super(classReader, flags);
  }

  @Override
  protected String getCommonSuperClass(String type1, String type2) {
    throw new UnsupportedOperationException("avoiding reflective lookup of classes");
  }
}
