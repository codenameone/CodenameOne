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
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
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
    private static final String HOISTED_FIELD_PREFIX = "zqL$";
    static final String DECODER_DESC = "(Ljava/lang/String;)Ljava/lang/String;";

    /** {@code StringConcatFactory} recipe markers: an ordinary argument slot and a constant slot. */
    private static final char TAG_ARG = '\u0001';
    private static final char TAG_CONST = '\u0002';

    /** Synthesized <clinit> initializer-chunk helpers, kept clear of any real member. */
    private static final String INIT_HELPER_PREFIX = "zqCI$";
    /**
     * Cut a generated <clinit> into helper methods once a chunk grows past this many ENCODED bytes, so
     * a class with thousands of hoisted/encrypted constants never exceeds the JVM's 65535-byte method
     * limit. Kept well under MethodSize.SAFE_LIMIT so each helper fits.
     */
    private static final int MAX_CLINIT_CHUNK_BYTES = 48000;
    /** Widest encoding of one INVOKESTATIC helper call prepended to <clinit>. */
    private static final int CLINIT_CALL_BYTES = 5;
    /** Widest encoding of the per-access decoder INVOKESTATIC inserted after an LDC. */
    private static final int DECODER_CALL_BYTES = 5;
    /** Target ceiling for the constant-pool item count (the hard JVM limit is 65535), with margin. */
    private static final int SAFE_POOL_ITEMS = 60000;
    /**
     * Conservative constant-pool entries each hoisted literal adds -- a Utf8 for the field name, a
     * NameAndType, a Fieldref, and the ciphertext Utf8 + String -- so the total can be bounded before
     * hoisting rather than discovering the overflow only when ASM writes the class.
     */
    private static final int POOL_ITEMS_PER_HOIST = 6;

    private final boolean encryptAllStrings;
    private final int seed;
    private final ClassLoader hierarchy;
    private final java.util.Set<String> constantValues;
    private int encryptedCount;
    private int concatLiteralCount;
    private int legacyInterfaceConstantCount;
    private int oversizedLiteralCount;
    private int condyLiteralCount;
    private int clinitFullLiteralCount;
    private int methodFullLiteralCount;
    private int annotationLiteralCount;
    /** The input class's constant-pool item count, so hoisting can stay under the 65535-entry limit. */
    private int poolBaseItems;

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

    /**
     * The number of {@code invokedynamic} string-concatenation sites carrying plaintext literal text
     * that this transform did NOT encrypt. javac from JDK 9 on compiles {@code "a" + b} to an
     * {@code invokedynamic} bound to {@link java.lang.invoke.StringConcatFactory}, storing the literal
     * fragments in the bootstrap recipe/constant arguments rather than as {@code LDC} instructions.
     * The engine encrypts {@code LDC} and {@code ConstantValue} channels; a recipe cannot be rewritten
     * to a decode call because {@code StringConcatFactory} interprets it at link time, so those
     * fragments would ship in cleartext. They are counted and reported rather than silently left,
     * so a build compiled that way is not believed fully string-encrypted. Compiling with
     * {@code -XDstringConcat=inline} (or an older {@code -target}) emits {@code StringBuilder} the
     * engine encrypts.
     */
    public int getConcatLiteralCount() {
        return concatLiteralCount;
    }

    /**
     * The number of {@code static final String} constants on pre-Java-8 interfaces that this transform
     * left in plaintext. Such an interface cannot host a {@code <clinit>} or the decoder (both need
     * class-file version 52), so its own {@code ConstantValue} attribute cannot be moved to a decode
     * call. Every <em>read</em> of the constant elsewhere was inlined by javac to an {@code LDC} and is
     * encrypted there, so the value is hidden at each use; only the declaring interface's constant pool
     * still carries it. Counted and reported rather than shipped silently.
     */
    public int getLegacyInterfaceConstantCount() {
        return legacyInterfaceConstantCount;
    }

    /**
     * The number of distinct string literals this transform would have encrypted but left in plaintext
     * because their ciphertext could overflow the 65535-byte constant-pool limit (a valid ASCII literal
     * longer than 21,845 characters can widen to a 3-byte-per-char modified-UTF-8 constant). A large
     * embedded credential, JSON document or encoded blob therefore stays readable; counted and reported
     * so an {@code strings:all} build is not believed to have encrypted everything.
     */
    public int getOversizedLiteralCount() {
        return oversizedLiteralCount;
    }

    /**
     * The number of {@code LDC ConstantDynamic} sites carrying a String among their bootstrap arguments
     * that this transform did NOT encrypt. Java 11+ bytecode can materialize a constant through a
     * {@code constant-dynamic} whose bootstrap arguments hold plaintext; those live neither in a direct
     * {@code LDC "..."} nor in a field {@code ConstantValue}, and the condy is resolved at link time, so
     * rewriting it to a decode call is unsafe. Counted and reported rather than shipped unremarked.
     */
    public int getCondyLiteralCount() {
        return condyLiteralCount;
    }

    /**
     * The number of literals left plaintext because the class's {@code <clinit>} is already so close to
     * the 65,535-byte method limit that even the split path's helper calls would not fit. Extremely
     * rare (a class whose static initializer is itself near the limit), but reported rather than
     * aborting the build so a valid input class is never rejected.
     */
    public int getClinitFullLiteralCount() {
        return clinitFullLiteralCount;
    }

    /**
     * The number of literals left plaintext because encrypting them per access would have pushed the
     * enclosing method past the 65,535-byte limit. Only the interface path decodes per access (a class's
     * literals are hoisted to {@code <clinit>}, which does not grow the method body), so this is rare;
     * reported rather than aborting the build on a valid input class.
     */
    public int getMethodFullLiteralCount() {
        return methodFullLiteralCount;
    }

    /**
     * The number of distinct string values the current mode would encrypt that are stored in annotation
     * element values or annotation defaults. javac keeps those in the annotation metadata, not as an
     * {@code LDC} or a field {@code ConstantValue}, so no encryption channel reaches them; they stay
     * readable. Counted and reported so an {@code strings:all} build is not believed to have encrypted
     * every string. (Codename One has no runtime reflection to read an annotation value back, so this is
     * a disclosure note, not a correctness risk.)
     */
    public int getAnnotationLiteralCount() {
        return annotationLiteralCount;
    }


    /** Encrypts {@code classBytes}, returning the transformed bytes (or the input if nothing changed). */
    public byte[] transform(byte[] classBytes) {
        ClassNode cn = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(cn, ClassReader.SKIP_FRAMES);
        // The input's current constant-pool item count; hoisting must not grow the pool past 65535.
        poolBaseItems = reader.getItemCount();

        boolean isInterface = (cn.access & Opcodes.ACC_INTERFACE) != 0;
        // The decoder is a concrete static method, and (for interface constants) it is invoked from
        // <clinit>. Static/private methods and <clinit> in an interface are only valid from class-file
        // version 52 (Java 8). A pre-Java-8 interface therefore cannot host the decoder, and such an
        // interface has no default/static method bodies to hold LDC literals anyway, so skip it whole
        // rather than emit a class that fails verification.
        if (isInterface && (cn.version & 0xFFFF) < Opcodes.V1_8) {
            // A pre-Java-8 interface cannot host a <clinit>/decoder, so its own static-final String
            // ConstantValue attributes cannot be moved to a decode call and stay plaintext. Count the
            // ones we would otherwise have encrypted so the engine reports the exclusion instead of
            // silently shipping them; javac already inlined (and this pass encrypts) every read site.
            if (cn.fields != null) {
                for (FieldNode f : cn.fields) {
                    if ((f.access & Opcodes.ACC_STATIC) != 0 && (f.access & Opcodes.ACC_FINAL) != 0
                            && f.value instanceof String && shouldEncryptLiteral((String) f.value)) {
                        legacyInterfaceConstantCount++;
                    }
                }
            }
            return classBytes;
        }

        // Pick a decoder name that does not collide with an existing member, so a class is NEVER
        // skipped for a name clash. Skipping would leave that class's literals in plaintext while an
        // equal literal in another class was encrypted+interned; on ParparVM, whose intern pool does
        // not contain the compile-time literals, the two would then fail a valid literal '==' compare.
        // Never skipping keeps encryption applied by-value across the whole jar, so all occurrences of
        // a value are decoded through the shared intern pool and stay reference-equal.
        String decoderName = resolveDecoderName(cn);

        // Count invokedynamic string-concatenation literals we cannot encrypt (see
        // getConcatLiteralCount). Done before the mutating passes so it is independent of them; the
        // engine turns a non-zero total into a build warning so plaintext concat fragments are never
        // silently shipped.
        concatLiteralCount += countConcatLiterals(cn);
        condyLiteralCount += countCondyLiterals(cn);
        annotationLiteralCount += countAnnotationStrings(cn);
        // Count the distinct literals that would be encrypted but are too large to (their ciphertext
        // could overflow the constant pool), so the engine can report the exclusion rather than let an
        // strings:all build claim it encrypted everything.
        oversizedLiteralCount += countOversizedLiterals(cn);

        int base = keyBase(cn.name);
        boolean changed = false;

        // Channel 1: LDC string literals in method bodies. In "all" mode every literal is
        // encrypted; in "constants" mode only literals whose value was declared as a
        // static-final String constant somewhere in the jar -- which is exactly the set javac
        // inlined at these read sites -- so ordinary incidental literals are left alone.
        //
        // For a normal class each distinct literal is hoisted to a synthetic static field decoded
        // ONCE in <clinit>, and its LDC sites become a GETSTATIC of that field, so a literal in a hot
        // loop is not re-decoded (and re-interned -- an O(n) scan on ParparVM) on every iteration. An
        // interface can't host that (its fields are public), so there the literal is decoded per
        // access; interface method bodies are rare and not hot loops.
        if (cn.methods != null) {
            if (isInterface) {
                changed |= encryptAllMethodsPerAccess(cn, base, decoderName, true);
            } else {
                changed |= hoistMethodLiterals(cn, base, decoderName);
            }
        }

        // Channel 2: static final String ConstantValue attributes (both modes), including
        // interfaces. A Java 8 interface may carry a <clinit> for non-constant field initialization,
        // so an interface constant's plaintext can be moved to a decoder call there just as a class
        // field's is -- otherwise "String TOKEN = \"secret\"" would still leak the plaintext.
        changed |= encryptStaticFinalStrings(cn, base, isInterface, decoderName);

        if (!changed) {
            return classBytes;
        }

        addDecoder(cn, base, isInterface, decoderName);

        ClassWriter cw = new FrameClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, hierarchy);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Encrypts every method-body literal in {@code cn} PER ACCESS (an {@code LDC} ciphertext + a decoder
     * {@code INVOKESTATIC}), rather than hoisting distinct values to fields. Used for interfaces (whose
     * fields are public) and as the fallback when hoisting a class would overflow the constant pool or
     * its {@code <clinit>} is already full: per-access adds no per-value field, so it does not grow the
     * pool, and it keeps EVERY occurrence of a value encrypted and interned -- preserving a valid
     * cross-class literal {@code ==} on ParparVM instead of leaving some copies plaintext.
     */
    private boolean encryptAllMethodsPerAccess(ClassNode cn, int base, String decoderName,
                                               boolean isInterface) {
        boolean changed = false;
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null || decoderName.equals(mn.name)) {
                    continue;
                }
                changed |= encryptMethodLiterals(cn, mn, base, isInterface, decoderName);
            }
        }
        return changed;
    }

    private boolean encryptMethodLiterals(ClassNode cn, MethodNode mn, int base, boolean isInterface,
                                          String decoderName) {
        boolean changed = false;
        // Each rewrite inserts an INVOKESTATIC, growing the method. A method already near the limit
        // cannot take unbounded rewrites, so track the running size and stop (leaving the remaining
        // literals plaintext, reported) before the method would overflow, rather than aborting the build.
        int currentBytes = MethodSize.estimateBytes(mn.instructions);
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof String && shouldEncryptLiteral((String) ldc.cst)) {
                    if (currentBytes + DECODER_CALL_BYTES > MethodSize.SAFE_LIMIT) {
                        methodFullLiteralCount++;
                    } else {
                        String plain = (String) ldc.cst;
                        // shouldEncrypt already rejected any value whose ciphertext could overflow the
                        // constant pool, using a class-independent bound, so the encode result fits.
                        ldc.cst = encode(plain, base);
                        // The itf flag must be true when the decoder lives in an interface, or the JVM
                        // writes a Methodref instead of an InterfaceMethodref and throws
                        // IncompatibleClassChangeError at run time.
                        mn.instructions.insert(ldc, new MethodInsnNode(
                                Opcodes.INVOKESTATIC, cn.name, decoderName, DECODER_DESC, isInterface));
                        currentBytes += DECODER_CALL_BYTES;
                        encryptedCount++;
                        changed = true;
                    }
                }
            }
            insn = next;
        }
        return changed;
    }

    /**
     * Counts the {@code invokedynamic} string-concatenation sites in {@code cn} that carry plaintext
     * the engine cannot reach. A {@code makeConcatWithConstants} recipe embeds the literal fragments of
     * {@code "a" + b} directly (any character other than the U+0001 argument marker and the
     * U+0002 constant marker), and additional constant fragments arrive as String bootstrap
     * arguments after the recipe. Either form leaves cleartext in the constant pool that no
     * {@code LDC}/{@code ConstantValue} pass touches. Counts the site once when it bears any literal
     * text, so the reported number tracks concat sites rather than characters.
     */
    private static int countConcatLiterals(ClassNode cn) {
        if (cn.methods == null) {
            return 0;
        }
        int count = 0;
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof InvokeDynamicInsnNode)) {
                    continue;
                }
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                Handle bsm = indy.bsm;
                if (bsm == null
                        || !"java/lang/invoke/StringConcatFactory".equals(bsm.getOwner())
                        || !"makeConcatWithConstants".equals(bsm.getName())) {
                    continue;
                }
                if (concatSiteHasLiteral(indy.bsmArgs)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * True when a {@code makeConcatWithConstants} site carries any plaintext: a recipe (the first
     * bootstrap argument) with a character that is neither the U+0001 argument marker nor the
     * U+0002 constant marker, or any String constant among the later bootstrap arguments.
     */
    private static boolean concatSiteHasLiteral(Object[] bsmArgs) {
        if (bsmArgs == null || bsmArgs.length == 0) {
            return false;
        }
        if (bsmArgs[0] instanceof String) {
            String recipe = (String) bsmArgs[0];
            for (int i = 0; i < recipe.length(); i++) {
                char c = recipe.charAt(i);
                if (c != TAG_ARG && c != TAG_CONST) {
                    return true;
                }
            }
        }
        for (int i = 1; i < bsmArgs.length; i++) {
            if (bsmArgs[i] instanceof String) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the {@code LDC ConstantDynamic} sites in {@code cn} whose bootstrap arguments include a
     * String -- Java 11+ can carry plaintext through a constant-dynamic (e.g. an enum switch map or an
     * explicit-condy compiler), which no {@code LDC}/{@code ConstantValue} pass reaches. Counts the site
     * once when it bears any String argument.
     */
    private static int countCondyLiterals(ClassNode cn) {
        if (cn.methods == null) {
            return 0;
        }
        int count = 0;
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof ConstantDynamic) {
                    if (condyHasStringArgument((ConstantDynamic) ((LdcInsnNode) insn).cst)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** True when a constant-dynamic carries a String among its bootstrap arguments, nested ones too. */
    private static boolean condyHasStringArgument(ConstantDynamic condy) {
        return condyHasStringArgument(condy, 0);
    }

    private static boolean condyHasStringArgument(ConstantDynamic condy, int depth) {
        // A constant-dynamic bootstrap argument can itself be a ConstantDynamic whose arguments hold the
        // plaintext, so recurse. The depth guard is a backstop against a pathological/cyclic pool.
        if (depth > 16) {
            return false;
        }
        for (int i = 0, n = condy.getBootstrapMethodArgumentCount(); i < n; i++) {
            Object arg = condy.getBootstrapMethodArgument(i);
            if (arg instanceof String) {
                return true;
            }
            if (arg instanceof ConstantDynamic
                    && condyHasStringArgument((ConstantDynamic) arg, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the distinct string values the current mode would encrypt that live in annotation element
     * values or defaults (see {@link #getAnnotationLiteralCount()}). Walks class, field, method and
     * parameter annotations plus method annotation defaults, recursing into nested annotations and array
     * values; skips enum references ({@code String[]}) and {@code Type}, which are not string literals.
     */
    private int countAnnotationStrings(ClassNode cn) {
        java.util.Set<String> found = new java.util.HashSet<String>();
        collectAnnotations(cn.visibleAnnotations, found);
        collectAnnotations(cn.invisibleAnnotations, found);
        if (cn.fields != null) {
            for (FieldNode f : cn.fields) {
                collectAnnotations(f.visibleAnnotations, found);
                collectAnnotations(f.invisibleAnnotations, found);
            }
        }
        if (cn.methods != null) {
            for (MethodNode m : cn.methods) {
                collectAnnotations(m.visibleAnnotations, found);
                collectAnnotations(m.invisibleAnnotations, found);
                collectParameterAnnotations(m.visibleParameterAnnotations, found);
                collectParameterAnnotations(m.invisibleParameterAnnotations, found);
                collectAnnotationValue(m.annotationDefault, found);
            }
        }
        return found.size();
    }

    private void collectAnnotations(java.util.List<AnnotationNode> list, java.util.Set<String> out) {
        if (list == null) {
            return;
        }
        for (AnnotationNode an : list) {
            collectAnnotationNode(an, out);
        }
    }

    private void collectParameterAnnotations(java.util.List<AnnotationNode>[] params,
                                             java.util.Set<String> out) {
        if (params == null) {
            return;
        }
        for (java.util.List<AnnotationNode> list : params) {
            collectAnnotations(list, out);
        }
    }

    private void collectAnnotationNode(AnnotationNode an, java.util.Set<String> out) {
        if (an == null || an.values == null) {
            return;
        }
        // values is a flat [name, value, name, value, ...] list; only the values can hold strings.
        for (int i = 1; i < an.values.size(); i += 2) {
            collectAnnotationValue(an.values.get(i), out);
        }
    }

    private void collectAnnotationValue(Object value, java.util.Set<String> out) {
        if (value instanceof String) {
            if (modeSelectsLiteral((String) value)) {
                out.add((String) value);
            }
        } else if (value instanceof AnnotationNode) {
            collectAnnotationNode((AnnotationNode) value, out);
        } else if (value instanceof java.util.List) {
            for (Object e : (java.util.List<?>) value) {
                collectAnnotationValue(e, out);
            }
        }
        // A String[] is an enum reference {descriptor, constant} and a Type is a class literal -- neither
        // is a user string literal, so both are left uncounted.
    }

    /**
     * Counts the distinct literals in {@code cn} that this transform would encrypt but skips because
     * their ciphertext could overflow the constant pool (see {@link #getOversizedLiteralCount()}).
     * Covers both channels: method-body {@code LDC}s selected by the current mode, and
     * {@code static final String} {@code ConstantValue}s (encrypted regardless of mode). Distinct by
     * value within the class, mirroring how encryption dedups.
     */
    private int countOversizedLiterals(ClassNode cn) {
        java.util.Set<String> skipped = new java.util.HashSet<String>();
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                        String v = (String) ((LdcInsnNode) insn).cst;
                        if (isOversized(v) && modeSelectsLiteral(v)) {
                            skipped.add(v);
                        }
                    }
                }
            }
        }
        if (cn.fields != null) {
            for (FieldNode fn : cn.fields) {
                if ((fn.access & Opcodes.ACC_STATIC) != 0 && fn.value instanceof String) {
                    String v = (String) fn.value;
                    // encryptStaticFinalStrings uses shouldEncrypt (mode-independent), so any oversized
                    // static-final String would be skipped.
                    if (v.length() > 2 && isOversized(v)) {
                        skipped.add(v);
                    }
                }
            }
        }
        return skipped.size();
    }

    /** True when {@code s}'s worst-case ciphertext would overflow the 65535-byte constant-pool limit. */
    private static boolean isOversized(String s) {
        return s != null && (long) s.length() * 3 > 65535;
    }

    /** True when the current mode would select {@code s} for method-literal encryption (size aside). */
    private boolean modeSelectsLiteral(String s) {
        if (s == null || s.length() <= 2) {
            return false;
        }
        return encryptAllStrings || (constantValues != null && constantValues.contains(s));
    }

    /**
     * Hoists each distinct encryptable method-body literal in {@code cn} to a synthetic static field
     * decoded once in {@code <clinit>}, and rewrites its LDC sites to a GETSTATIC of that field. So a
     * literal read in a loop pays the decode + intern cost once at class load instead of on every
     * access. Fields are private and synthetic; the decoded value is interned, so all sites of one
     * value -- and equal values in other classes -- stay reference-equal.
     */
    private boolean hoistMethodLiterals(ClassNode cn, int base, String decoderName) {
        // 1. Collect the distinct values, in first-seen order, each mapped to a field name that does
        //    NOT collide with an existing member (an input class may already declare a zqL$N field;
        //    on Android the engine doesn't rename first, so this is reachable and a duplicate field
        //    would make the class fail to load).
        java.util.Set<String> taken = new java.util.HashSet<String>();
        if (cn.fields != null) {
            for (FieldNode f : cn.fields) {
                taken.add(f.name);
            }
        }
        java.util.LinkedHashMap<String, String> valueToField = new java.util.LinkedHashMap<String, String>();
        int counter = 0;
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || decoderName.equals(mn.name)) {
                continue;
            }
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                    String v = (String) ((LdcInsnNode) insn).cst;
                    if (shouldEncryptLiteral(v) && !valueToField.containsKey(v)) {
                        String fname;
                        do {
                            fname = HOISTED_FIELD_PREFIX + counter;
                            counter++;
                        } while (taken.contains(fname));
                        taken.add(fname);
                        valueToField.put(v, fname);
                    }
                }
            }
        }
        if (valueToField.isEmpty()) {
            return false;
        }
        // Bound hoisting by constant-pool growth: each hoisted literal adds several pool entries (its
        // field name Utf8, a NameAndType, a Fieldref, the ciphertext Utf8 + String), so a class with
        // tens of thousands of distinct literals could exceed the JVM's 65535-entry limit and make ASM
        // throw ClassTooLargeException. When hoisting them all would overflow, fall back to per-access
        // encryption for the whole class: it adds no per-value field, so it does not grow the pool, and
        // -- crucially -- it keeps EVERY occurrence encrypted and interned rather than leaving some
        // plaintext, so a value hoisted in one class still compares == to its per-access copy here.
        int budget = (SAFE_POOL_ITEMS - poolBaseItems) / POOL_ITEMS_PER_HOIST;
        if (budget < 0) {
            budget = 0;
        }
        if (valueToField.size() > budget) {
            return encryptAllMethodsPerAccess(cn, base, decoderName, false);
        }
        // 2. Build the initializer BEFORE mutating anything.
        InsnList init = new InsnList();
        for (java.util.Map.Entry<String, String> e : valueToField.entrySet()) {
            init.add(new LdcInsnNode(encode(e.getKey(), base)));
            init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, decoderName, DECODER_DESC, false));
            init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, e.getValue(), "Ljava/lang/String;"));
        }
        // If the class's <clinit> is already so full it cannot hold the decode init, fall back to
        // per-access here too (no <clinit> growth), for the same reason: keep every occurrence encrypted
        // and interned rather than leaving these method-body literals plaintext.
        if (!clinitCanAccept(cn, init)) {
            return encryptAllMethodsPerAccess(cn, base, decoderName, false);
        }
        // 3. Commit. Replace each LDC of a hoisted value with a GETSTATIC of its field in the ORIGINAL
        //    method bodies; the standalone init is inserted into <clinit> only afterwards, so its own
        //    ciphertext LDCs are never rescanned -- otherwise a ciphertext that happens to equal another
        //    hoisted plaintext (the XOR encoding is involutive) would be rewritten into a read of a
        //    not-yet-assigned field and pass null to the decoder.
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || decoderName.equals(mn.name)) {
                continue;
            }
            AbstractInsnNode insn = mn.instructions.getFirst();
            while (insn != null) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                    String field = valueToField.get((String) ((LdcInsnNode) insn).cst);
                    if (field != null) {
                        mn.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, cn.name, field,
                                "Ljava/lang/String;"));
                    }
                }
                insn = next;
            }
        }
        // Add a field per value; the init decodes each once in <clinit>, prepended so it runs before
        // the original body (a hoisted value used within <clinit> reads the already-initialized field).
        if (cn.fields == null) {
            cn.fields = new java.util.ArrayList<FieldNode>();
        }
        for (String field : valueToField.values()) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    field, "Ljava/lang/String;", null, null));
            encryptedCount++;
        }
        // hoistMethodLiterals runs only for a non-interface (interfaces decode per access), so the
        // helper split, if it triggers, emits ordinary private static helpers.
        prependToClinit(cn, init, false);
        return true;
    }

    private boolean encryptStaticFinalStrings(ClassNode cn, int base, boolean isInterface,
                                              String decoderName) {
        if (cn.fields == null) {
            return false;
        }
        // Build the initializer and remember which fields to strip WITHOUT mutating yet, so a class
        // whose <clinit> cannot accommodate the init keeps its constants (plaintext, reported) rather
        // than being left with fields whose ConstantValue was stripped but never re-initialized.
        InsnList init = new InsnList();
        java.util.List<FieldNode> toStrip = new java.util.ArrayList<FieldNode>();
        for (FieldNode fn : cn.fields) {
            boolean isStatic = (fn.access & Opcodes.ACC_STATIC) != 0;
            if (isStatic && fn.value instanceof String && shouldEncrypt((String) fn.value)) {
                String plain = (String) fn.value;
                // shouldEncrypt already rejected any value whose ciphertext could overflow the
                // constant pool (class-independent bound), so the encode result fits.
                init.add(new LdcInsnNode(encode(plain, base)));
                // itf=true when the decoder lives in an interface, else the JVM emits a Methodref
                // instead of an InterfaceMethodref and throws IncompatibleClassChangeError.
                init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, decoderName, DECODER_DESC, isInterface));
                init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, fn.name, fn.desc));
                toStrip.add(fn);
            }
        }
        if (toStrip.isEmpty()) {
            return false;
        }
        if (!clinitCanAccept(cn, init)) {
            clinitFullLiteralCount += toStrip.size();
            return false;
        }
        // Commit: strip each ConstantValue so the plaintext leaves the class file entirely (the slot
        // ParparVM would otherwise dump into the C constant pool), and decode it once in <clinit>.
        for (FieldNode fn : toStrip) {
            fn.value = null;
            encryptedCount++;
        }
        prependToClinit(cn, init, isInterface);
        return true;
    }

    /**
     * Whether {@code s} fits a single constant-pool entry: the class-file format stores a String
     * constant as modified UTF-8 with a 16-bit (65535-byte) length prefix. The XOR cipher can turn
     * an ASCII character into a value up to {@code 0xFFFF} (three modified-UTF-8 bytes), so an
     * originally-valid literal can encrypt into an over-long one; such literals are left in plaintext.
     */
    static boolean fitsConstantPool(String s) {
        long bytes = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i) & 0xFFFF;
            // Modified UTF-8: 0x0001..0x007F -> 1 byte; 0x0000 and 0x0080..0x07FF -> 2 bytes;
            // 0x0800..0xFFFF -> 3 bytes.
            if (c >= 0x0001 && c <= 0x007F) {
                bytes += 1;
            } else if (c == 0x0000 || c <= 0x07FF) {
                bytes += 2;
            } else {
                bytes += 3;
            }
            if (bytes > 65535) {
                return false;
            }
        }
        return bytes <= 65535;
    }

    private void prependToClinit(ClassNode cn, InsnList init, boolean isInterface) {
        // A generated class can hoist/encrypt thousands of constants; emitting every init unit into a
        // single <clinit> can blow past the 65535-byte method limit (ASM throws MethodTooLargeException
        // even though every input method was valid). When the initializer is large, split it across
        // synthetic helper methods and have <clinit> call them in order. Each init unit ends with
        // PUTSTATIC (stack empty), so cutting after a PUTSTATIC keeps every chunk verifiable.
        //
        // Measure the COMBINED ENCODED bytes -- the class may already carry a large <clinit>, so even a
        // small new initializer inserted directly could push the existing method over the limit, and a
        // node count cannot prove the direct-insert path fits. When the total is under the bound, insert
        // directly; otherwise split so <clinit> only gains a few calls. Callers pre-check clinitCanAccept
        // before mutating, so the split is only reached when the calls themselves fit.
        int existingBytes = MethodSize.estimateBytes(findClinit(cn));
        if (existingBytes + MethodSize.estimateBytes(init) <= MethodSize.SAFE_LIMIT) {
            insertIntoClinit(cn, init);
            return;
        }
        java.util.Set<String> taken = new java.util.HashSet<String>();
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                taken.add(mn.name);
            }
        }
        int access = (isInterface ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE)
                | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        InsnList calls = new InsnList();
        InsnList chunk = new InsnList();
        int chunkBytes = 0;
        int helperCounter = 0;
        AbstractInsnNode insn = init.getFirst();
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            init.remove(insn);
            chunkBytes += MethodSize.estimateBytes(insn);
            chunk.add(insn);
            boolean atBoundary = insn.getOpcode() == Opcodes.PUTSTATIC && chunkBytes >= MAX_CLINIT_CHUNK_BYTES;
            if (atBoundary || next == null) {
                String hname;
                do {
                    hname = INIT_HELPER_PREFIX + helperCounter;
                    helperCounter++;
                } while (taken.contains(hname));
                taken.add(hname);
                MethodNode helper = new MethodNode(Opcodes.ASM9, access, hname, "()V", null, null);
                helper.instructions = new InsnList();
                helper.instructions.add(chunk);
                helper.instructions.add(new InsnNode(Opcodes.RETURN));
                cn.methods.add(helper);
                // itf=true when the helper lives in an interface, or the JVM emits a Methodref instead
                // of an InterfaceMethodref and throws IncompatibleClassChangeError at run time.
                calls.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, hname, "()V", isInterface));
                chunk = new InsnList();
                chunkBytes = 0;
            }
            insn = next;
        }
        insertIntoClinit(cn, calls);
    }

    /**
     * Whether {@code prependToClinit} can add {@code init} without pushing {@code <clinit>} past the
     * method limit. A direct insert must fit under {@link MethodSize#SAFE_LIMIT}; otherwise the split
     * moves {@code init} into helper methods and {@code <clinit>} only gains one call per chunk, so the
     * check is whether the existing initializer plus those calls fits. Returns false only in the extreme
     * case that even the calls do not fit -- then the caller leaves the literals plaintext rather than
     * mutating a class it cannot finish.
     */
    private boolean clinitCanAccept(ClassNode cn, InsnList init) {
        int existingBytes = MethodSize.estimateBytes(findClinit(cn));
        int initBytes = MethodSize.estimateBytes(init);
        if (existingBytes + initBytes <= MethodSize.SAFE_LIMIT) {
            return true;
        }
        int chunks = (initBytes + MAX_CLINIT_CHUNK_BYTES - 1) / MAX_CLINIT_CHUNK_BYTES;
        if (chunks < 1) {
            chunks = 1;
        }
        return existingBytes + chunks * CLINIT_CALL_BYTES <= MethodSize.SAFE_LIMIT;
    }

    /** The class's existing {@code <clinit>}, or {@code null} if it has none. */
    private static MethodNode findClinit(ClassNode cn) {
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if ("<clinit>".equals(mn.name) && "()V".equals(mn.desc)) {
                    return mn;
                }
            }
        }
        return null;
    }

    private void insertIntoClinit(ClassNode cn, InsnList init) {
        MethodNode clinit = findClinit(cn);
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

    private void addDecoder(ClassNode cn, int base, boolean isInterface, String decoderName) {
        // A Java 8 interface may only have public static methods (private statics are 9+), so the
        // decoder is public there; in a class it stays private.
        int access = (isInterface ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE)
                | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        MethodNode m = new MethodNode(Opcodes.ASM9, access, decoderName, DECODER_DESC, null, null);
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
        // return new String(c).intern();  -- intern so a decoded literal is the canonical String,
        // preserving reference (==) equality that Java guarantees for string literals and constants.
        in.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        in.add(new InsnNode(Opcodes.DUP));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));
        in.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        in.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "intern",
                "()Ljava/lang/String;", false));
        in.add(new InsnNode(Opcodes.ARETURN));
        if (cn.methods == null) {
            cn.methods = new java.util.ArrayList<MethodNode>();
        }
        cn.methods.add(m);
    }

    /**
     * A decoder method name for {@code cn} that collides with no existing member. Starts from the
     * base name and lengthens the {@code $} suffix until unused, so a class is never skipped for a
     * clash (which would leave its literals in plaintext and break cross-class literal {@code ==}).
     */
    private String resolveDecoderName(ClassNode cn) {
        String name = DECODER_NAME;
        while (memberExists(cn, name)) {
            name = name + "$";
        }
        return name;
    }

    private boolean memberExists(ClassNode cn, String name) {
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                // Same descriptor would be an outright clash; a differently-typed method of the same
                // name is legal, but the decoder is also referenced by name from <clinit>, so keep it
                // simple and avoid the name entirely.
                if (name.equals(mn.name)) {
                    return true;
                }
            }
        }
        if (cn.fields != null) {
            for (FieldNode fn : cn.fields) {
                if (name.equals(fn.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Strings too short to be worth the decoder overhead, or trivially empty, are left alone. */
    private boolean shouldEncrypt(String s) {
        if (s == null || s.length() <= 2) {
            return false;
        }
        // Skip a literal whose ciphertext could overflow the 65535-byte constant-pool limit and make
        // ASM throw. The XOR key varies per class, so decide from the plaintext's WORST case -- every
        // character widening to a 3-byte modified-UTF-8 character. That bound is class-INDEPENDENT, so
        // the same value is skipped in every class rather than encrypted in one and left plaintext in
        // another, which would break a valid cross-class literal == on ParparVM's deduplicated pool.
        if ((long) s.length() * 3 > 65535) {
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

    /** Package-visible so a test can construct a value whose ciphertext equals another value. */
    int keyBase(String internalName) {
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
