// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//
// Codename One modifications Copyright (c) 2026, Codename One and/or its
// affiliates, distributed under the license above.
//
// This file is a rewrite of ASM's org.objectweb.asm.ClassReader for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes a class file and reports its contents to a {@link ClassVisitor}.
 *
 * <p>Within a method, one {@link Label} is created per distinct bytecode offset that
 * something refers to -- a branch or switch target, the bounds and handler of an
 * exception range, a line-number or local-variable boundary, an uninitialized
 * {@code NEW} or a stack-map frame -- and each is visited just before the
 * instruction at its offset (or after the last one, for the end of the code). The
 * translator turns every visited label into an IR label, so that set is part of
 * its output and changing it changes generated code.
 *
 * <p>Stack-map frames are read only to place their labels; nothing else in the
 * translator consumes them, so they are never reported.
 */
public class ClassReader {
    /** Do not read method bodies. */
    public static final int SKIP_CODE = 1;
    /** Do not read source file, line numbers, local variables or parameter names. */
    public static final int SKIP_DEBUG = 2;
    /** Do not read stack-map frames, which also means no labels at frame offsets. */
    public static final int SKIP_FRAMES = 4;
    /** Accepted for symmetry with the frame-skipping flag; frames are never reported. */
    public static final int EXPAND_FRAMES = 8;

    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_DYNAMIC = 17;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;
    private static final int CONSTANT_MODULE = 19;
    private static final int CONSTANT_PACKAGE = 20;

    private static final int ITEM_UNINITIALIZED = 8;

    private final byte[] b;
    /** Offset of each constant pool entry's tag byte, indexed by constant pool index. */
    private final int[] cpOffsets;
    private final String[] utf8Cache;
    /** Offset of the access_flags item that follows the constant pool. */
    private final int header;
    /** Offset of each BootstrapMethods entry, read lazily. */
    private int[] bootstrapOffsets;

    public ClassReader(byte[] classFile) {
        this.b = classFile;
        if (readInt(0) != 0xCAFEBABE) {
            throw new IllegalArgumentException("Not a class file");
        }
        int count = readUnsignedShort(8);
        cpOffsets = new int[count];
        utf8Cache = new String[count];
        int offset = 10;
        for (int i = 1; i < count; i++) {
            cpOffsets[i] = offset;
            int size;
            switch (b[offset]) {
                case CONSTANT_FIELDREF:
                case CONSTANT_METHODREF:
                case CONSTANT_INTERFACE_METHODREF:
                case CONSTANT_INTEGER:
                case CONSTANT_FLOAT:
                case CONSTANT_NAME_AND_TYPE:
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKE_DYNAMIC:
                    size = 5;
                    break;
                case CONSTANT_LONG:
                case CONSTANT_DOUBLE:
                    size = 9;
                    i++;
                    break;
                case CONSTANT_UTF8:
                    size = 3 + readUnsignedShort(offset + 1);
                    break;
                case CONSTANT_METHOD_HANDLE:
                    size = 4;
                    break;
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                case CONSTANT_METHOD_TYPE:
                case CONSTANT_MODULE:
                case CONSTANT_PACKAGE:
                    size = 3;
                    break;
                default:
                    throw new IllegalArgumentException("Bad constant pool tag " + b[offset] + " at " + offset);
            }
            offset += size;
        }
        header = offset;
    }

    public ClassReader(InputStream in) throws IOException {
        this(readFully(in));
    }

    private static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** The internal name of the class this file declares. */
    public String getClassName() {
        return readClass(header + 2);
    }

    // ------------------------------------------------------------------ primitives

    private int readUnsignedByte(int offset) {
        return b[offset] & 0xFF;
    }

    private int readUnsignedShort(int offset) {
        return ((b[offset] & 0xFF) << 8) | (b[offset + 1] & 0xFF);
    }

    private short readShort(int offset) {
        return (short) readUnsignedShort(offset);
    }

    private int readInt(int offset) {
        return ((b[offset] & 0xFF) << 24) | ((b[offset + 1] & 0xFF) << 16)
                | ((b[offset + 2] & 0xFF) << 8) | (b[offset + 3] & 0xFF);
    }

    private long readLong(int offset) {
        long high = readInt(offset);
        long low = readInt(offset + 4) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    /** The CONSTANT_Utf8 whose index is stored at {@code offset}, or null for index 0. */
    private String readUTF8(int offset) {
        int index = readUnsignedShort(offset);
        if (index == 0) {
            return null;
        }
        return utf8(index);
    }

    private String utf8(int index) {
        String s = utf8Cache[index];
        if (s != null) {
            return s;
        }
        int offset = cpOffsets[index];
        int length = readUnsignedShort(offset + 1);
        s = decodeModifiedUtf8(offset + 3, length);
        utf8Cache[index] = s;
        return s;
    }

    private String decodeModifiedUtf8(int start, int length) {
        char[] chars = new char[length];
        int count = 0;
        int i = start;
        int end = start + length;
        while (i < end) {
            int c = b[i++] & 0xFF;
            if ((c & 0x80) == 0) {
                chars[count++] = (char) c;
            } else if ((c & 0xE0) == 0xC0) {
                chars[count++] = (char) (((c & 0x1F) << 6) | (b[i++] & 0x3F));
            } else {
                chars[count++] = (char) (((c & 0x0F) << 12) | ((b[i++] & 0x3F) << 6) | (b[i++] & 0x3F));
            }
        }
        return new String(chars, 0, count);
    }

    /** The internal name of the CONSTANT_Class whose index is stored at {@code offset}. */
    private String readClass(int offset) {
        int index = readUnsignedShort(offset);
        if (index == 0) {
            return null;
        }
        return utf8(readUnsignedShort(cpOffsets[index] + 1));
    }

    /** The loadable constant at a constant pool index, in its reported form. */
    private Object readConst(int index) {
        int offset = cpOffsets[index];
        switch (b[offset]) {
            case CONSTANT_INTEGER:
                return Integer.valueOf(readInt(offset + 1));
            case CONSTANT_FLOAT:
                return Float.valueOf(Float.intBitsToFloat(readInt(offset + 1)));
            case CONSTANT_LONG:
                return Long.valueOf(readLong(offset + 1));
            case CONSTANT_DOUBLE:
                return Double.valueOf(Double.longBitsToDouble(readLong(offset + 1)));
            case CONSTANT_CLASS:
                return Type.getObjectType(utf8(readUnsignedShort(offset + 1)));
            case CONSTANT_STRING:
                return utf8(readUnsignedShort(offset + 1));
            case CONSTANT_METHOD_TYPE:
                return Type.getMethodType(utf8(readUnsignedShort(offset + 1)));
            case CONSTANT_METHOD_HANDLE:
                return readHandle(index);
            case CONSTANT_DYNAMIC:
                return readConstantDynamic(index);
            default:
                throw new IllegalArgumentException("Not a loadable constant: tag " + b[offset]);
        }
    }

    private Handle readHandle(int index) {
        int offset = cpOffsets[index];
        int kind = readUnsignedByte(offset + 1);
        int memberOffset = cpOffsets[readUnsignedShort(offset + 2)];
        String owner = readClass(memberOffset + 1);
        int nameAndType = cpOffsets[readUnsignedShort(memberOffset + 3)];
        String name = readUTF8(nameAndType + 1);
        String desc = readUTF8(nameAndType + 3);
        boolean itf = b[memberOffset] == CONSTANT_INTERFACE_METHODREF;
        return new Handle(kind, owner, name, desc, itf);
    }

    private ConstantDynamic readConstantDynamic(int index) {
        int offset = cpOffsets[index];
        int bootstrap = bootstrapOffset(readUnsignedShort(offset + 1));
        int nameAndType = cpOffsets[readUnsignedShort(offset + 3)];
        String name = readUTF8(nameAndType + 1);
        String desc = readUTF8(nameAndType + 3);
        Handle handle = readHandle(readUnsignedShort(bootstrap));
        Object[] args = readBootstrapArguments(bootstrap);
        return new ConstantDynamic(name, desc, handle, args);
    }

    private Object[] readBootstrapArguments(int bootstrap) {
        int count = readUnsignedShort(bootstrap + 2);
        Object[] args = new Object[count];
        for (int i = 0; i < count; i++) {
            args[i] = readConst(readUnsignedShort(bootstrap + 4 + 2 * i));
        }
        return args;
    }

    private int bootstrapOffset(int bootstrapIndex) {
        if (bootstrapOffsets == null) {
            int attrs = skipMembers(skipMembers(header + 8 + 2 * readUnsignedShort(header + 6)));
            int count = readUnsignedShort(attrs);
            int offset = attrs + 2;
            for (int i = 0; i < count; i++) {
                String attrName = readUTF8(offset);
                int length = readInt(offset + 2);
                if ("BootstrapMethods".equals(attrName)) {
                    int n = readUnsignedShort(offset + 6);
                    bootstrapOffsets = new int[n];
                    int entry = offset + 8;
                    for (int j = 0; j < n; j++) {
                        bootstrapOffsets[j] = entry;
                        entry += 4 + 2 * readUnsignedShort(entry + 2);
                    }
                }
                offset += 6 + length;
            }
            if (bootstrapOffsets == null) {
                throw new IllegalArgumentException("Missing BootstrapMethods attribute");
            }
        }
        return bootstrapOffsets[bootstrapIndex];
    }

    /** Skips a fields_count/fields or methods_count/methods table, returning the offset after it. */
    private int skipMembers(int offset) {
        int count = readUnsignedShort(offset);
        offset += 2;
        for (int i = 0; i < count; i++) {
            offset = skipAttributes(offset + 6);
        }
        return offset;
    }

    private int skipAttributes(int offset) {
        int count = readUnsignedShort(offset);
        offset += 2;
        for (int i = 0; i < count; i++) {
            offset += 6 + readInt(offset + 2);
        }
        return offset;
    }

    // ------------------------------------------------------------------ class

    public void accept(ClassVisitor visitor, int flags) {
        int access = readUnsignedShort(header);
        String name = readClass(header + 2);
        String superName = readClass(header + 4);
        int interfaceCount = readUnsignedShort(header + 6);
        String[] interfaces = new String[interfaceCount];
        for (int i = 0; i < interfaceCount; i++) {
            interfaces[i] = readClass(header + 8 + 2 * i);
        }
        int fieldsOffset = header + 8 + 2 * interfaceCount;
        int methodsOffset = skipMembers(fieldsOffset);
        int attrsOffset = skipMembers(methodsOffset);

        String signature = null;
        String sourceFile = null;
        String sourceDebug = null;
        int innerClasses = 0;
        int enclosingMethod = 0;
        int visibleAnnotations = 0;
        int invisibleAnnotations = 0;
        int attrCount = readUnsignedShort(attrsOffset);
        int offset = attrsOffset + 2;
        for (int i = 0; i < attrCount; i++) {
            String attrName = readUTF8(offset);
            int length = readInt(offset + 2);
            int content = offset + 6;
            if ("SourceFile".equals(attrName)) {
                sourceFile = readUTF8(content);
            } else if ("SourceDebugExtension".equals(attrName)) {
                sourceDebug = decodeModifiedUtf8(content, length);
            } else if ("InnerClasses".equals(attrName)) {
                innerClasses = content;
            } else if ("EnclosingMethod".equals(attrName)) {
                enclosingMethod = content;
            } else if ("Signature".equals(attrName)) {
                signature = readUTF8(content);
            } else if ("RuntimeVisibleAnnotations".equals(attrName)) {
                visibleAnnotations = content;
            } else if ("RuntimeInvisibleAnnotations".equals(attrName)) {
                invisibleAnnotations = content;
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC;
            } else if ("Record".equals(attrName)) {
                access |= Opcodes.ACC_RECORD;
            }
            offset = content + length;
        }

        visitor.visit(readInt(4), access, name, signature, superName, interfaces);
        if ((flags & SKIP_DEBUG) == 0 && (sourceFile != null || sourceDebug != null)) {
            visitor.visitSource(sourceFile, sourceDebug);
        }
        if (enclosingMethod != 0) {
            String owner = readClass(enclosingMethod);
            int nameAndType = readUnsignedShort(enclosingMethod + 2);
            String methodName = null;
            String methodDesc = null;
            if (nameAndType != 0) {
                methodName = readUTF8(cpOffsets[nameAndType] + 1);
                methodDesc = readUTF8(cpOffsets[nameAndType] + 3);
            }
            visitor.visitOuterClass(owner, methodName, methodDesc);
        }
        readAnnotations(visibleAnnotations, true, visitor, null, null);
        readAnnotations(invisibleAnnotations, false, visitor, null, null);
        if (innerClasses != 0) {
            int count = readUnsignedShort(innerClasses);
            int entry = innerClasses + 2;
            for (int i = 0; i < count; i++) {
                visitor.visitInnerClass(readClass(entry), readClass(entry + 2), readUTF8(entry + 4),
                        readUnsignedShort(entry + 6));
                entry += 8;
            }
        }

        int fieldCount = readUnsignedShort(fieldsOffset);
        offset = fieldsOffset + 2;
        for (int i = 0; i < fieldCount; i++) {
            offset = readField(visitor, offset);
        }
        int methodCount = readUnsignedShort(methodsOffset);
        offset = methodsOffset + 2;
        for (int i = 0; i < methodCount; i++) {
            offset = readMethod(visitor, offset, flags);
        }
        visitor.visitEnd();
    }

    // ------------------------------------------------------------------ annotations

    /** Reads a RuntimeXxxAnnotations attribute body into the owner's visitor. */
    private void readAnnotations(int offset, boolean visible, ClassVisitor cv, FieldVisitor fv, MethodVisitor mv) {
        if (offset == 0) {
            return;
        }
        int count = readUnsignedShort(offset);
        offset += 2;
        for (int i = 0; i < count; i++) {
            String desc = readUTF8(offset);
            AnnotationVisitor av;
            if (cv != null) {
                av = cv.visitAnnotation(desc, visible);
            } else if (fv != null) {
                av = fv.visitAnnotation(desc, visible);
            } else {
                av = mv.visitAnnotation(desc, visible);
            }
            offset = readElementValuePairs(av, offset + 2, true);
        }
    }

    private void readParameterAnnotations(int offset, boolean visible, MethodVisitor mv) {
        if (offset == 0) {
            return;
        }
        int parameters = readUnsignedByte(offset);
        offset++;
        for (int p = 0; p < parameters; p++) {
            int count = readUnsignedShort(offset);
            offset += 2;
            for (int i = 0; i < count; i++) {
                String desc = readUTF8(offset);
                AnnotationVisitor av = mv.visitParameterAnnotation(p, desc, visible);
                offset = readElementValuePairs(av, offset + 2, true);
            }
        }
    }

    /**
     * Reads {@code num_element_value_pairs} pairs (when {@code named}) or values, then
     * ends the visitor. Returns the offset after them. A null visitor skips them.
     */
    private int readElementValuePairs(AnnotationVisitor av, int offset, boolean named) {
        int count = readUnsignedShort(offset);
        offset += 2;
        for (int i = 0; i < count; i++) {
            String name = null;
            if (named) {
                name = readUTF8(offset);
                offset += 2;
            }
            offset = readElementValue(av, offset, name);
        }
        if (av != null) {
            av.visitEnd();
        }
        return offset;
    }

    private int readElementValue(AnnotationVisitor av, int offset, String name) {
        int tag = b[offset] & 0xFF;
        offset++;
        if (av == null) {
            return skipElementValue(tag, offset);
        }
        switch (tag) {
            case 'B':
                av.visit(name, Byte.valueOf((byte) readInt(cpOffsets[readUnsignedShort(offset)] + 1)));
                return offset + 2;
            case 'C':
                av.visit(name, Character.valueOf((char) readInt(cpOffsets[readUnsignedShort(offset)] + 1)));
                return offset + 2;
            case 'S':
                av.visit(name, Short.valueOf((short) readInt(cpOffsets[readUnsignedShort(offset)] + 1)));
                return offset + 2;
            case 'Z':
                av.visit(name, readInt(cpOffsets[readUnsignedShort(offset)] + 1) == 0 ? Boolean.FALSE : Boolean.TRUE);
                return offset + 2;
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                av.visit(name, readConst(readUnsignedShort(offset)));
                return offset + 2;
            case 's':
                av.visit(name, readUTF8(offset));
                return offset + 2;
            case 'e':
                av.visitEnum(name, readUTF8(offset), readUTF8(offset + 2));
                return offset + 4;
            case 'c':
                av.visit(name, Type.getType(readUTF8(offset)));
                return offset + 2;
            case '@':
                return readElementValuePairs(av.visitAnnotation(name, readUTF8(offset)), offset + 2, true);
            case '[':
                return readArrayValue(av, offset, name);
            default:
                throw new IllegalArgumentException("Bad element value tag " + tag);
        }
    }

    private int readArrayValue(AnnotationVisitor av, int offset, String name) {
        int count = readUnsignedShort(offset);
        if (count == 0) {
            return readElementValuePairs(av.visitArray(name), offset, false);
        }
        int elementTag = b[offset + 2] & 0xFF;
        int item = offset + 2;
        switch (elementTag) {
            case 'B': {
                byte[] values = new byte[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = (byte) readInt(cpOffsets[readUnsignedShort(item + 1)] + 1);
                }
                av.visit(name, values);
                return item;
            }
            case 'Z': {
                boolean[] values = new boolean[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = readInt(cpOffsets[readUnsignedShort(item + 1)] + 1) != 0;
                }
                av.visit(name, values);
                return item;
            }
            case 'S': {
                short[] values = new short[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = (short) readInt(cpOffsets[readUnsignedShort(item + 1)] + 1);
                }
                av.visit(name, values);
                return item;
            }
            case 'C': {
                char[] values = new char[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = (char) readInt(cpOffsets[readUnsignedShort(item + 1)] + 1);
                }
                av.visit(name, values);
                return item;
            }
            case 'I': {
                int[] values = new int[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = readInt(cpOffsets[readUnsignedShort(item + 1)] + 1);
                }
                av.visit(name, values);
                return item;
            }
            case 'J': {
                long[] values = new long[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = readLong(cpOffsets[readUnsignedShort(item + 1)] + 1);
                }
                av.visit(name, values);
                return item;
            }
            case 'F': {
                float[] values = new float[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = Float.intBitsToFloat(readInt(cpOffsets[readUnsignedShort(item + 1)] + 1));
                }
                av.visit(name, values);
                return item;
            }
            case 'D': {
                double[] values = new double[count];
                for (int i = 0; i < count; i++, item += 3) {
                    values[i] = Double.longBitsToDouble(readLong(cpOffsets[readUnsignedShort(item + 1)] + 1));
                }
                av.visit(name, values);
                return item;
            }
            default:
                return readElementValuePairs(av.visitArray(name), offset, false);
        }
    }

    private int skipElementValue(int tag, int offset) {
        switch (tag) {
            case 'e':
                return offset + 4;
            case '@':
                return readElementValuePairs(null, offset + 2, true);
            case '[':
                return readElementValuePairs(null, offset, false);
            default:
                return offset + 2;
        }
    }

    // ------------------------------------------------------------------ fields

    private int readField(ClassVisitor visitor, int offset) {
        int access = readUnsignedShort(offset);
        String name = readUTF8(offset + 2);
        String desc = readUTF8(offset + 4);
        String signature = null;
        Object value = null;
        int visibleAnnotations = 0;
        int invisibleAnnotations = 0;
        int attrCount = readUnsignedShort(offset + 6);
        offset += 8;
        for (int i = 0; i < attrCount; i++) {
            String attrName = readUTF8(offset);
            int length = readInt(offset + 2);
            int content = offset + 6;
            if ("ConstantValue".equals(attrName)) {
                int index = readUnsignedShort(content);
                value = index == 0 ? null : readConst(index);
            } else if ("Signature".equals(attrName)) {
                signature = readUTF8(content);
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC;
            } else if ("RuntimeVisibleAnnotations".equals(attrName)) {
                visibleAnnotations = content;
            } else if ("RuntimeInvisibleAnnotations".equals(attrName)) {
                invisibleAnnotations = content;
            }
            offset = content + length;
        }
        FieldVisitor fv = visitor.visitField(access, name, desc, signature, value);
        if (fv != null) {
            readAnnotations(visibleAnnotations, true, null, fv, null);
            readAnnotations(invisibleAnnotations, false, null, fv, null);
            fv.visitEnd();
        }
        return offset;
    }

    // ------------------------------------------------------------------ methods

    private int readMethod(ClassVisitor visitor, int offset, int flags) {
        int access = readUnsignedShort(offset);
        String name = readUTF8(offset + 2);
        String desc = readUTF8(offset + 4);
        String signature = null;
        String[] exceptions = null;
        int code = 0;
        int methodParameters = 0;
        int annotationDefault = 0;
        int visibleAnnotations = 0;
        int invisibleAnnotations = 0;
        int visibleParameterAnnotations = 0;
        int invisibleParameterAnnotations = 0;
        int attrCount = readUnsignedShort(offset + 6);
        offset += 8;
        for (int i = 0; i < attrCount; i++) {
            String attrName = readUTF8(offset);
            int length = readInt(offset + 2);
            int content = offset + 6;
            if ("Code".equals(attrName)) {
                if ((flags & SKIP_CODE) == 0) {
                    code = content;
                }
            } else if ("Exceptions".equals(attrName)) {
                int count = readUnsignedShort(content);
                exceptions = new String[count];
                for (int j = 0; j < count; j++) {
                    exceptions[j] = readClass(content + 2 + 2 * j);
                }
            } else if ("Signature".equals(attrName)) {
                signature = readUTF8(content);
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC;
            } else if ("MethodParameters".equals(attrName)) {
                if ((flags & SKIP_DEBUG) == 0) {
                    methodParameters = content;
                }
            } else if ("AnnotationDefault".equals(attrName)) {
                annotationDefault = content;
            } else if ("RuntimeVisibleAnnotations".equals(attrName)) {
                visibleAnnotations = content;
            } else if ("RuntimeInvisibleAnnotations".equals(attrName)) {
                invisibleAnnotations = content;
            } else if ("RuntimeVisibleParameterAnnotations".equals(attrName)) {
                visibleParameterAnnotations = content;
            } else if ("RuntimeInvisibleParameterAnnotations".equals(attrName)) {
                invisibleParameterAnnotations = content;
            }
            offset = content + length;
        }
        MethodVisitor mv = visitor.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return offset;
        }
        if (methodParameters != 0) {
            int count = readUnsignedByte(methodParameters);
            int entry = methodParameters + 1;
            for (int i = 0; i < count; i++) {
                mv.visitParameter(readUTF8(entry), readUnsignedShort(entry + 2));
                entry += 4;
            }
        }
        if (annotationDefault != 0) {
            AnnotationVisitor av = mv.visitAnnotationDefault();
            readElementValue(av, annotationDefault, null);
            if (av != null) {
                av.visitEnd();
            }
        }
        readAnnotations(visibleAnnotations, true, null, null, mv);
        readAnnotations(invisibleAnnotations, false, null, null, mv);
        readParameterAnnotations(visibleParameterAnnotations, true, mv);
        readParameterAnnotations(invisibleParameterAnnotations, false, mv);
        if (code != 0) {
            mv.visitCode();
            readCode(mv, code, flags);
        }
        mv.visitEnd();
        return offset;
    }

    /** One label per offset; lines are attached in LineNumberTable order. */
    private static final class Labels {
        final Label[] byOffset;
        int[][] lines;

        Labels(int codeLength) {
            byOffset = new Label[codeLength + 1];
        }

        Label get(int offset) {
            Label l = byOffset[offset];
            if (l == null) {
                l = new Label();
                l.offset = offset;
                byOffset[offset] = l;
            }
            return l;
        }

        void addLine(int offset, int line) {
            get(offset);
            // A zero line number before any other at this offset reads as "no line"
            // and is dropped; one after a real line is kept.
            if (line == 0 && (lines == null || lines[offset] == null)) {
                return;
            }
            if (lines == null) {
                lines = new int[byOffset.length][];
            }
            int[] existing = lines[offset];
            if (existing == null) {
                lines[offset] = new int[]{line};
            } else {
                int[] grown = new int[existing.length + 1];
                System.arraycopy(existing, 0, grown, 0, existing.length);
                grown[existing.length] = line;
                lines[offset] = grown;
            }
        }
    }

    private void readCode(MethodVisitor mv, int offset, int flags) {
        int maxStack = readUnsignedShort(offset);
        int maxLocals = readUnsignedShort(offset + 2);
        int codeLength = readInt(offset + 4);
        int codeStart = offset + 8;
        int codeEnd = codeStart + codeLength;
        Labels labels = new Labels(codeLength);

        // Pass 1: branch and switch targets.
        int pc = codeStart;
        while (pc < codeEnd) {
            int bc = pc - codeStart;
            int op = b[pc] & 0xFF;
            switch (op) {
                case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
                case Opcodes.IFGT: case Opcodes.IFLE: case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE: case Opcodes.GOTO: case Opcodes.JSR:
                case Opcodes.IFNULL: case Opcodes.IFNONNULL:
                    labels.get(bc + readShort(pc + 1));
                    break;
                case 200: // GOTO_W
                case 201: // JSR_W
                    labels.get(bc + readInt(pc + 1));
                    break;
                case Opcodes.TABLESWITCH: {
                    int p = pc + 4 - (bc & 3);
                    labels.get(bc + readInt(p));
                    int low = readInt(p + 4);
                    int high = readInt(p + 8);
                    for (int i = 0; i <= high - low; i++) {
                        labels.get(bc + readInt(p + 12 + 4 * i));
                    }
                    break;
                }
                case Opcodes.LOOKUPSWITCH: {
                    int p = pc + 4 - (bc & 3);
                    labels.get(bc + readInt(p));
                    int pairs = readInt(p + 4);
                    for (int i = 0; i < pairs; i++) {
                        labels.get(bc + readInt(p + 12 + 8 * i));
                    }
                    break;
                }
                default:
                    break;
            }
            pc += instructionLength(pc, codeStart);
        }

        // Exception table: visited now, ahead of every instruction.
        int exceptionTable = codeEnd;
        int handlerCount = readUnsignedShort(exceptionTable);
        int entry = exceptionTable + 2;
        for (int i = 0; i < handlerCount; i++) {
            Label start = labels.get(readUnsignedShort(entry));
            Label end = labels.get(readUnsignedShort(entry + 2));
            Label handler = labels.get(readUnsignedShort(entry + 4));
            int catchType = readUnsignedShort(entry + 6);
            mv.visitTryCatchBlock(start, end, handler, catchType == 0 ? null : readClass(entry + 6));
            entry += 8;
        }

        // Code attributes: debug labels, stack map position.
        int localVariableTable = 0;
        int localVariableTypeTable = 0;
        int stackMap = 0;
        int stackMapEnd = 0;
        int attrCount = readUnsignedShort(entry);
        int attr = entry + 2;
        for (int i = 0; i < attrCount; i++) {
            String attrName = readUTF8(attr);
            int length = readInt(attr + 2);
            int content = attr + 6;
            if ("LocalVariableTable".equals(attrName)) {
                if ((flags & SKIP_DEBUG) == 0) {
                    localVariableTable = content;
                    int count = readUnsignedShort(content);
                    int e = content + 2;
                    for (int j = 0; j < count; j++) {
                        int startPc = readUnsignedShort(e);
                        labels.get(startPc);
                        labels.get(startPc + readUnsignedShort(e + 2));
                        e += 10;
                    }
                }
            } else if ("LocalVariableTypeTable".equals(attrName)) {
                localVariableTypeTable = content;
            } else if ("LineNumberTable".equals(attrName)) {
                if ((flags & SKIP_DEBUG) == 0) {
                    int count = readUnsignedShort(content);
                    int e = content + 2;
                    for (int j = 0; j < count; j++) {
                        labels.addLine(readUnsignedShort(e), readUnsignedShort(e + 2));
                        e += 4;
                    }
                }
            } else if ("StackMapTable".equals(attrName)) {
                if ((flags & SKIP_FRAMES) == 0) {
                    stackMap = content + 2;
                    stackMapEnd = content + length;
                }
            } else if ("RuntimeVisibleTypeAnnotations".equals(attrName)
                    || "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                labelTypeAnnotationRanges(content, labels);
            }
            attr = content + length;
        }

        // An uninitialized-type entry names the offset of its NEW, which needs a label.
        if (stackMap != 0) {
            for (int p = stackMap; p < stackMapEnd - 2; p++) {
                if (b[p] == ITEM_UNINITIALIZED) {
                    int target = readUnsignedShort(p + 1);
                    if (target < codeLength && (b[codeStart + target] & 0xFF) == Opcodes.NEW) {
                        labels.get(target);
                    }
                }
            }
        }

        // Pass 2: visit. Frames are consumed one ahead of the code, as each frame's
        // offset is only known once the previous one has been read, and each read
        // places a label at the frame it describes.
        int[] frameOffset = {-1};
        boolean debug = (flags & SKIP_DEBUG) == 0;
        pc = codeStart;
        while (pc < codeEnd) {
            int bc = pc - codeStart;
            Label label = labels.byOffset[bc];
            if (label != null) {
                mv.visitLabel(label);
                if (debug && labels.lines != null && labels.lines[bc] != null) {
                    for (int line : labels.lines[bc]) {
                        mv.visitLineNumber(line, label);
                    }
                }
            }
            while (stackMap != 0 && (frameOffset[0] == bc || frameOffset[0] == -1)) {
                if (stackMap < stackMapEnd) {
                    stackMap = skipFrame(stackMap, frameOffset, labels);
                } else {
                    stackMap = 0;
                }
            }
            visitInstruction(mv, pc, codeStart, labels);
            pc += instructionLength(pc, codeStart);
        }
        Label endLabel = labels.byOffset[codeLength];
        if (endLabel != null) {
            mv.visitLabel(endLabel);
        }

        if (localVariableTable != 0 && debug) {
            int count = readUnsignedShort(localVariableTable);
            int e = localVariableTable + 2;
            for (int j = 0; j < count; j++) {
                int startPc = readUnsignedShort(e);
                int length = readUnsignedShort(e + 2);
                String name = readUTF8(e + 4);
                String desc = readUTF8(e + 6);
                int index = readUnsignedShort(e + 8);
                String signature = null;
                if (localVariableTypeTable != 0) {
                    int typeCount = readUnsignedShort(localVariableTypeTable);
                    int t = localVariableTypeTable + 2;
                    for (int k = 0; k < typeCount; k++) {
                        if (readUnsignedShort(t) == startPc && readUnsignedShort(t + 8) == index) {
                            signature = readUTF8(t + 6);
                            break;
                        }
                        t += 10;
                    }
                }
                mv.visitLocalVariable(name, desc, signature, labels.get(startPc), labels.get(startPc + length), index);
                e += 10;
            }
        }
        mv.visitMaxs(maxStack, maxLocals);
    }

    /** Local-variable type-annotation targets name code ranges, whose bounds get labels. */
    private void labelTypeAnnotationRanges(int offset, Labels labels) {
        int count = readUnsignedShort(offset);
        offset += 2;
        for (int i = 0; i < count; i++) {
            int targetType = b[offset] & 0xFF;
            switch (targetType) {
                case 0x00: case 0x01: case 0x16:
                    offset += 2;
                    break;
                case 0x10: case 0x11: case 0x12: case 0x17:
                    offset += 3;
                    break;
                case 0x13: case 0x14: case 0x15:
                    offset += 1;
                    break;
                case 0x40: case 0x41: {
                    int tableLength = readUnsignedShort(offset + 1);
                    offset += 3;
                    for (int j = 0; j < tableLength; j++) {
                        int startPc = readUnsignedShort(offset);
                        labels.get(startPc);
                        labels.get(startPc + readUnsignedShort(offset + 2));
                        offset += 6;
                    }
                    break;
                }
                case 0x42:
                    offset += 3;
                    break;
                case 0x43: case 0x44: case 0x45: case 0x46:
                    offset += 3;
                    break;
                case 0x47: case 0x48: case 0x49: case 0x4A: case 0x4B:
                    offset += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Bad type annotation target " + targetType);
            }
            int pathLength = b[offset] & 0xFF;
            offset += 1 + 2 * pathLength;
            offset = readElementValuePairs(null, offset + 2, true);
        }
    }

    /** Skips one StackMapTable entry, advancing the frame offset and labelling it. */
    private int skipFrame(int offset, int[] frameOffset, Labels labels) {
        int type = b[offset] & 0xFF;
        offset++;
        int delta;
        if (type < 64) {
            delta = type;
        } else if (type < 128) {
            delta = type - 64;
            offset = skipVerificationType(offset);
        } else if (type == 247) {
            delta = readUnsignedShort(offset);
            offset = skipVerificationType(offset + 2);
        } else if (type >= 248 && type <= 251) {
            delta = readUnsignedShort(offset);
            offset += 2;
        } else if (type >= 252 && type <= 254) {
            delta = readUnsignedShort(offset);
            offset += 2;
            for (int i = 0; i < type - 251; i++) {
                offset = skipVerificationType(offset);
            }
        } else if (type == 255) {
            delta = readUnsignedShort(offset);
            int locals = readUnsignedShort(offset + 2);
            offset += 4;
            for (int i = 0; i < locals; i++) {
                offset = skipVerificationType(offset);
            }
            int stack = readUnsignedShort(offset);
            offset += 2;
            for (int i = 0; i < stack; i++) {
                offset = skipVerificationType(offset);
            }
        } else {
            throw new IllegalArgumentException("Bad stack map frame type " + type);
        }
        frameOffset[0] += delta + 1;
        labels.get(frameOffset[0]);
        return offset;
    }

    private int skipVerificationType(int offset) {
        int tag = b[offset] & 0xFF;
        return tag == 7 || tag == 8 ? offset + 3 : offset + 1;
    }

    private int instructionLength(int pc, int codeStart) {
        int op = b[pc] & 0xFF;
        switch (op) {
            case Opcodes.BIPUSH: case Opcodes.LDC: case Opcodes.ILOAD: case Opcodes.LLOAD:
            case Opcodes.FLOAD: case Opcodes.DLOAD: case Opcodes.ALOAD: case Opcodes.ISTORE:
            case Opcodes.LSTORE: case Opcodes.FSTORE: case Opcodes.DSTORE: case Opcodes.ASTORE:
            case Opcodes.RET: case Opcodes.NEWARRAY:
                return 2;
            case Opcodes.SIPUSH: case 19: case 20: case Opcodes.IINC:
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
            case Opcodes.IFGT: case Opcodes.IFLE: case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE: case Opcodes.GOTO: case Opcodes.JSR:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
            case Opcodes.GETSTATIC: case Opcodes.PUTSTATIC: case Opcodes.GETFIELD: case Opcodes.PUTFIELD:
            case Opcodes.INVOKEVIRTUAL: case Opcodes.INVOKESPECIAL: case Opcodes.INVOKESTATIC:
            case Opcodes.NEW: case Opcodes.ANEWARRAY: case Opcodes.CHECKCAST: case Opcodes.INSTANCEOF:
                return 3;
            case Opcodes.MULTIANEWARRAY:
                return 4;
            case Opcodes.INVOKEINTERFACE: case Opcodes.INVOKEDYNAMIC: case 200: case 201:
                return 5;
            case 196: // WIDE
                return (b[pc + 1] & 0xFF) == Opcodes.IINC ? 6 : 4;
            case Opcodes.TABLESWITCH: {
                int bc = pc - codeStart;
                int p = pc + 4 - (bc & 3);
                int low = readInt(p + 4);
                int high = readInt(p + 8);
                return p + 12 + 4 * (high - low + 1) - pc;
            }
            case Opcodes.LOOKUPSWITCH: {
                int bc = pc - codeStart;
                int p = pc + 4 - (bc & 3);
                return p + 8 + 8 * readInt(p + 4) - pc;
            }
            default:
                if (op > 201) {
                    throw new IllegalArgumentException("Bad opcode " + op);
                }
                return 1;
        }
    }

    private void visitInstruction(MethodVisitor mv, int pc, int codeStart, Labels labels) {
        int bc = pc - codeStart;
        int op = b[pc] & 0xFF;
        switch (op) {
            case Opcodes.BIPUSH:
                mv.visitIntInsn(op, b[pc + 1]);
                return;
            case Opcodes.SIPUSH:
                mv.visitIntInsn(op, readShort(pc + 1));
                return;
            case Opcodes.NEWARRAY:
                mv.visitIntInsn(op, b[pc + 1] & 0xFF);
                return;
            case Opcodes.LDC:
                mv.visitLdcInsn(readConst(b[pc + 1] & 0xFF));
                return;
            case 19: // LDC_W
            case 20: // LDC2_W
                mv.visitLdcInsn(readConst(readUnsignedShort(pc + 1)));
                return;
            case Opcodes.ILOAD: case Opcodes.LLOAD: case Opcodes.FLOAD: case Opcodes.DLOAD: case Opcodes.ALOAD:
            case Opcodes.ISTORE: case Opcodes.LSTORE: case Opcodes.FSTORE: case Opcodes.DSTORE: case Opcodes.ASTORE:
            case Opcodes.RET:
                mv.visitVarInsn(op, b[pc + 1] & 0xFF);
                return;
            case Opcodes.IINC:
                mv.visitIincInsn(b[pc + 1] & 0xFF, b[pc + 2]);
                return;
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
            case Opcodes.IFGT: case Opcodes.IFLE: case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE: case Opcodes.GOTO: case Opcodes.JSR:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
                mv.visitJumpInsn(op, labels.get(bc + readShort(pc + 1)));
                return;
            case 200: // GOTO_W
                mv.visitJumpInsn(Opcodes.GOTO, labels.get(bc + readInt(pc + 1)));
                return;
            case 201: // JSR_W
                mv.visitJumpInsn(Opcodes.JSR, labels.get(bc + readInt(pc + 1)));
                return;
            case Opcodes.TABLESWITCH: {
                int p = pc + 4 - (bc & 3);
                Label dflt = labels.get(bc + readInt(p));
                int low = readInt(p + 4);
                int high = readInt(p + 8);
                Label[] targets = new Label[high - low + 1];
                for (int i = 0; i < targets.length; i++) {
                    targets[i] = labels.get(bc + readInt(p + 12 + 4 * i));
                }
                mv.visitTableSwitchInsn(low, high, dflt, targets);
                return;
            }
            case Opcodes.LOOKUPSWITCH: {
                int p = pc + 4 - (bc & 3);
                Label dflt = labels.get(bc + readInt(p));
                int pairs = readInt(p + 4);
                int[] keys = new int[pairs];
                Label[] targets = new Label[pairs];
                for (int i = 0; i < pairs; i++) {
                    keys[i] = readInt(p + 8 + 8 * i);
                    targets[i] = labels.get(bc + readInt(p + 12 + 8 * i));
                }
                mv.visitLookupSwitchInsn(dflt, keys, targets);
                return;
            }
            case Opcodes.GETSTATIC: case Opcodes.PUTSTATIC: case Opcodes.GETFIELD: case Opcodes.PUTFIELD: {
                int ref = cpOffsets[readUnsignedShort(pc + 1)];
                int nameAndType = cpOffsets[readUnsignedShort(ref + 3)];
                mv.visitFieldInsn(op, readClass(ref + 1), readUTF8(nameAndType + 1), readUTF8(nameAndType + 3));
                return;
            }
            case Opcodes.INVOKEVIRTUAL: case Opcodes.INVOKESPECIAL: case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEINTERFACE: {
                int ref = cpOffsets[readUnsignedShort(pc + 1)];
                int nameAndType = cpOffsets[readUnsignedShort(ref + 3)];
                mv.visitMethodInsn(op, readClass(ref + 1), readUTF8(nameAndType + 1), readUTF8(nameAndType + 3),
                        b[ref] == CONSTANT_INTERFACE_METHODREF);
                return;
            }
            case Opcodes.INVOKEDYNAMIC: {
                int ref = cpOffsets[readUnsignedShort(pc + 1)];
                int bootstrap = bootstrapOffset(readUnsignedShort(ref + 1));
                int nameAndType = cpOffsets[readUnsignedShort(ref + 3)];
                Handle handle = readHandle(readUnsignedShort(bootstrap));
                mv.visitInvokeDynamicInsn(readUTF8(nameAndType + 1), readUTF8(nameAndType + 3), handle,
                        readBootstrapArguments(bootstrap));
                return;
            }
            case Opcodes.NEW: case Opcodes.ANEWARRAY: case Opcodes.CHECKCAST: case Opcodes.INSTANCEOF:
                mv.visitTypeInsn(op, readClass(pc + 1));
                return;
            case Opcodes.MULTIANEWARRAY:
                mv.visitMultiANewArrayInsn(readClass(pc + 1), b[pc + 3] & 0xFF);
                return;
            case 196: { // WIDE
                int wideOp = b[pc + 1] & 0xFF;
                if (wideOp == Opcodes.IINC) {
                    mv.visitIincInsn(readUnsignedShort(pc + 2), readShort(pc + 4));
                } else {
                    mv.visitVarInsn(wideOp, readUnsignedShort(pc + 2));
                }
                return;
            }
            default:
                if (op >= 26 && op <= 45) { // xLOAD_n
                    int rel = op - 26;
                    mv.visitVarInsn(Opcodes.ILOAD + (rel >> 2), rel & 3);
                } else if (op >= 59 && op <= 78) { // xSTORE_n
                    int rel = op - 59;
                    mv.visitVarInsn(Opcodes.ISTORE + (rel >> 2), rel & 3);
                } else {
                    mv.visitInsn(op);
                }
        }
    }
}
