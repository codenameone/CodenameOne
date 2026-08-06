/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.hardening;

import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Encrypts the string literals in a class so they are not readable in the shipped
 * binary, and are never present as plaintext in the ParparVM C constant pool.
 *
 * <p>Two channels are neutralized, which is the point often missed: the
 * {@code LDC "..."} literals in method bodies, and the {@code ConstantValue}
 * attribute of {@code static final String} fields. javac inlines a constant into
 * every reader as its own LDC (caught by the first channel), but the defining
 * field still carries the plaintext in its {@code ConstantValue} slot, which
 * ParparVM emits into the same C table -- so we also strip that attribute and move
 * the initialization into {@code <clinit>} as a decode call.
 *
 * <p>The decoder is synthesized into each class with a per-class key baked in, so
 * there is no single named framework method to hook. (Scattering, split keys and
 * inlining are further hardening layers the design calls for; a per-class keyed
 * decoder already removes the single-hook weakness and is what ships first.)
 */
public final class StringEncryptTransform {

    /** Synthesized per-class decoder; the {@code $} keeps it clear of any real app member. */
    static final String DECODER_NAME = "zqdec$";
    static final String DECODER_DESC = "(Ljava/lang/String;)Ljava/lang/String;";

    private final boolean encryptAllStrings;
    private final int seed;
    private final ClassLoader hierarchy;
    private final java.util.Set<String> constantValues;
    private int encryptedCount;

    public StringEncryptTransform(boolean encryptAllStrings, int seed) {
        this(encryptAllStrings, seed, null, null);
    }

    public StringEncryptTransform(boolean encryptAllStrings, int seed, ClassLoader hierarchy) {
        this(encryptAllStrings, seed, hierarchy, null);
    }

    /**
     * @param hierarchy      a classloader over the (renamed) input classes plus the library jars,
     *                       used for stack-map frame computation so it never loads types through the
     *                       engine's own classloader; may be {@code null} in tests
     * @param constantValues in "constants" mode ({@code encryptAllStrings == false}), the set of
     *                       string values that were declared as {@code static final String}
     *                       constants across the jar; only those literals (including javac's inlined
     *                       copies at every read site) are encrypted. Ignored in "all" mode. May be
     *                       {@code null}, in which case constants mode encrypts nothing extra.
     */
    public StringEncryptTransform(boolean encryptAllStrings, int seed, ClassLoader hierarchy,
                                  java.util.Set<String> constantValues) {
        this.encryptAllStrings = encryptAllStrings;
        this.seed = seed;
        this.hierarchy = hierarchy;
        this.constantValues = constantValues;
    }

    /** Collects the values of {@code static final String} fields in {@code classBytes} into {@code out}. */
    public static void collectConstantValues(byte[] classBytes, final java.util.Set<String> out) {
        new ClassReader(classBytes).accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc,
                                                             String sig, Object value) {
                if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0
                        && value instanceof String) {
                    out.add((String) value);
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    public int getEncryptedCount() {
        return encryptedCount;
    }

    /** Encrypts {@code classBytes}, returning the transformed bytes (or the input if nothing changed). */
    public byte[] transform(byte[] classBytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, ClassReader.SKIP_FRAMES);

        // If the class already defines a member colliding with the decoder, leave it alone.
        if (hasDecoderCollision(cn)) {
            return classBytes;
        }
        boolean isInterface = (cn.access & Opcodes.ACC_INTERFACE) != 0;

        int base = keyBase(cn.name);
        boolean changed = false;

        // Channel 1: LDC string literals in method bodies. In "all" mode every literal is
        // encrypted; in "constants" mode only literals whose value was declared as a
        // static-final String constant somewhere in the jar -- which is exactly the set javac
        // inlined at these read sites -- so ordinary incidental literals are left alone.
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) {
                    continue;
                }
                if (DECODER_NAME.equals(mn.name)) {
                    continue;
                }
                changed |= encryptMethodLiterals(cn, mn, base);
            }
        }

        // Channel 2: static final String ConstantValue attributes (both modes). Skipped on
        // interfaces, whose fields are implicitly constant and have no rewritable init slot.
        if (!isInterface) {
            changed |= encryptStaticFinalStrings(cn, base);
        }

        if (!changed) {
            return classBytes;
        }

        addDecoder(cn, base, isInterface);

        ClassWriter cw = new FrameClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, hierarchy);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean encryptMethodLiterals(ClassNode cn, MethodNode mn, int base) {
        boolean changed = false;
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof String && shouldEncryptLiteral((String) ldc.cst)) {
                    String plain = (String) ldc.cst;
                    ldc.cst = encode(plain, base);
                    mn.instructions.insert(ldc, new MethodInsnNode(
                            Opcodes.INVOKESTATIC, cn.name, DECODER_NAME, DECODER_DESC, false));
                    encryptedCount++;
                    changed = true;
                }
            }
            insn = next;
        }
        return changed;
    }

    private boolean encryptStaticFinalStrings(ClassNode cn, int base) {
        if (cn.fields == null) {
            return false;
        }
        InsnList init = new InsnList();
        boolean changed = false;
        for (FieldNode fn : cn.fields) {
            boolean isStatic = (fn.access & Opcodes.ACC_STATIC) != 0;
            if (isStatic && fn.value instanceof String && shouldEncrypt((String) fn.value)) {
                String plain = (String) fn.value;
                // Strip the ConstantValue so the plaintext leaves the class file entirely
                // (this is the slot ParparVM would otherwise dump into the C constant pool).
                fn.value = null;
                init.add(new LdcInsnNode(encode(plain, base)));
                init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, DECODER_NAME, DECODER_DESC, false));
                init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, fn.name, fn.desc));
                encryptedCount++;
                changed = true;
            }
        }
        if (changed) {
            prependToClinit(cn, init);
        }
        return changed;
    }

    private void prependToClinit(ClassNode cn, InsnList init) {
        MethodNode clinit = null;
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if ("<clinit>".equals(mn.name) && "()V".equals(mn.desc)) {
                    clinit = mn;
                    break;
                }
            }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ASM9,
                    Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions = new InsnList();
            clinit.instructions.add(init);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(clinit);
        } else {
            clinit.instructions.insert(init);
        }
    }

    private void addDecoder(ClassNode cn, int base, boolean isInterface) {
        // A Java 8 interface may only have public static methods (private statics are 9+), so the
        // decoder is public there; in a class it stays private.
        int access = (isInterface ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE)
                | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        MethodNode m = new MethodNode(Opcodes.ASM9, access, DECODER_NAME, DECODER_DESC, null, null);
        InsnList in = m.instructions;
        // char[] c = s.toCharArray();  (local 1)
        in.add(new VarInsnNode(Opcodes.ALOAD, 0));
        in.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        in.add(new VarInsnNode(Opcodes.ASTORE, 1));
        // int i = 0;  (local 2)
        in.add(new InsnNode(Opcodes.ICONST_0));
        in.add(new VarInsnNode(Opcodes.ISTORE, 2));
        org.objectweb.asm.tree.LabelNode loop = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode end = new org.objectweb.asm.tree.LabelNode();
        in.add(loop);
        // if (i >= c.length) goto end;
        in.add(new VarInsnNode(Opcodes.ILOAD, 2));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));
        in.add(new InsnNode(Opcodes.ARRAYLENGTH));
        in.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ICMPGE, end));
        // c[i] = (char)(c[i] ^ ((base + i*31) & 0xFFFF));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));   // arrayref
        in.add(new VarInsnNode(Opcodes.ILOAD, 2));   // index
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));   // c
        in.add(new VarInsnNode(Opcodes.ILOAD, 2));   // i
        in.add(new InsnNode(Opcodes.CALOAD));        // c[i]
        in.add(new VarInsnNode(Opcodes.ILOAD, 2));   // i
        in.add(new IntInsnNode(Opcodes.BIPUSH, 31));
        in.add(new InsnNode(Opcodes.IMUL));          // i*31
        in.add(new LdcInsnNode(Integer.valueOf(base)));
        in.add(new InsnNode(Opcodes.IADD));          // base + i*31
        in.add(new LdcInsnNode(Integer.valueOf(0xFFFF)));
        in.add(new InsnNode(Opcodes.IAND));          // & 0xFFFF
        in.add(new InsnNode(Opcodes.IXOR));          // c[i] ^ key
        in.add(new InsnNode(Opcodes.I2C));
        in.add(new InsnNode(Opcodes.CASTORE));
        // i++;
        in.add(new org.objectweb.asm.tree.IincInsnNode(2, 1));
        in.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.GOTO, loop));
        in.add(end);
        // return new String(c);
        in.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        in.add(new InsnNode(Opcodes.DUP));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));
        in.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        in.add(new InsnNode(Opcodes.ARETURN));
        if (cn.methods == null) {
            cn.methods = new java.util.ArrayList<MethodNode>();
        }
        cn.methods.add(m);
    }

    private boolean hasDecoderCollision(ClassNode cn) {
        if (cn.methods == null) {
            return false;
        }
        for (MethodNode mn : cn.methods) {
            if (DECODER_NAME.equals(mn.name) && DECODER_DESC.equals(mn.desc)) {
                return true;
            }
        }
        return false;
    }

    /** Strings too short to be worth the decoder overhead, or trivially empty, are left alone. */
    private boolean shouldEncrypt(String s) {
        if (s == null || s.length() <= 2) {
            return false;
        }
        return true;
    }

    /**
     * A method-body literal is encrypted in "all" mode, or in "constants" mode only when its value
     * was declared as a static-final String constant somewhere in the jar (javac inlined those here).
     */
    private boolean shouldEncryptLiteral(String s) {
        if (!shouldEncrypt(s)) {
            return false;
        }
        if (encryptAllStrings) {
            return true;
        }
        return constantValues != null && constantValues.contains(s);
    }

    /** Encodes a string by XORing each char with a position-dependent key derived from {@code base}. */
    static String encode(String plain, int base) {
        char[] c = plain.toCharArray();
        for (int i = 0; i < c.length; i++) {
            int key = (base + i * 31) & 0xFFFF;
            c[i] = (char) (c[i] ^ key);
        }
        return new String(c);
    }

    /** Decodes; the inverse of {@link #encode}. Used by tests to mirror the synthesized decoder. */
    static String decode(String enc, int base) {
        return encode(enc, base);
    }

    private int keyBase(String internalName) {
        int h = seed;
        for (int i = 0; i < internalName.length(); i++) {
            h = h * 31 + internalName.charAt(i);
        }
        int base = h & 0xFFFF;
        // Avoid a zero key, which would leave one-char-per-position untouched at i==0.
        return base == 0 ? 0x2f : base;
    }

    /** True if a transformed method still holds a plaintext copy of {@code needle} as an LDC. */
    static boolean containsStringLiteral(byte[] classBytes, final String needle) {
        final boolean[] found = new boolean[1];
        new ClassReader(classBytes).accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (needle.equals(value)) {
                            found[0] = true;
                        }
                    }
                };
            }

            @Override
            public org.objectweb.asm.FieldVisitor visitField(int a, String n, String d, String s, Object value) {
                if (needle.equals(value)) {
                    found[0] = true;
                }
                return null;
            }
        }, 0);
        return found[0];
    }
}
