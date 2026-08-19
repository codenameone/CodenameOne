/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.tools.translator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns compiled classes into a {@code .cn1ip} bundle for the on-device
 * interpreter.
 *
 * <p>The transform runs here, on the developer's machine, rather than on the
 * device. The device then never parses a class file, never decodes a constant
 * pool and never resolves a label: the bundle hands it flat int arrays whose
 * operands are already indices into its own tables. That keeps the shipped app
 * free of a class-file reader (smaller, and less to get wrong on a platform
 * with no reflection) and moves every cost that can be paid once to the side
 * that can afford it.</p>
 *
 * <p>What it does not do is reinterpret the bytecode. Opcodes are preserved
 * as-is, so the interpreter's behaviour can be checked against a real JVM
 * running the same source -- which is what the conformance tests do.</p>
 *
 * @author Shai Almog
 */
public class InterpBundleWriter {
    private static final int MAGIC = 0x434E3149;
    private static final int VERSION = 3;

    private static final int EXTERN_CLASS = 0;
    private static final int EXTERN_METHOD = 1;
    private static final int EXTERN_FIELD = 2;

    private static final int LDC_INT = 0;
    private static final int LDC_LONG = 1;
    private static final int LDC_FLOAT = 2;
    private static final int LDC_DOUBLE = 3;
    private static final int LDC_STRING = 4;
    private static final int LDC_CLASS = 5;

    private final Map<String, Integer> stringPool = new LinkedHashMap<String, Integer>();
    private final List<String> strings = new ArrayList<String>();

    private final Map<String, Integer> externPool = new LinkedHashMap<String, Integer>();
    private final List<int[]> externs = new ArrayList<int[]>();

    private final List<ClassNode> interpreted = new ArrayList<ClassNode>();
    private final Map<String, String> sources = new LinkedHashMap<String, String>();

    /// The program's own resources -- theme.res, CSS, images -- keyed by the
    /// path an application loads them with, e.g. `/theme.res`.
    private final Map<String, byte[]> resources = new LinkedHashMap<String, byte[]>();
    private String mainClass;

    /** Names of the classes carried in this bundle, i.e. the interpreted set. */
    private final java.util.Set<String> interpretedNames = new java.util.HashSet<String>();

    /**
     * Adds a compiled class to the bundle. Everything it references that is not
     * also added becomes an extern -- a symbol expected to exist in the host
     * app.
     */
    public void addClass(byte[] classFile) {
        ClassNode cn = new ClassNode();
        new ClassReader(classFile).accept(cn, ClassReader.SKIP_FRAMES);
        interpreted.add(cn);
        interpretedNames.add(cn.name);
    }

    /** Adds a class from a {@code .class} file. */
    public void addClassFile(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            addClass(bos.toByteArray());
        } finally {
            in.close();
        }
    }

    /**
     * Adds a source file. The runtime refuses to load a bundle whose
     * interpreted classes are not all covered by sources -- the App Store's
     * allowance for downloaded educational code is conditional on the user
     * being able to see and edit what runs, so this is enforced rather than
     * documented.
     */
    public void addSource(String fileName, String text) {
        sources.put(fileName, text);
    }

    /**
     * Adds a resource under the path an application would load it by.
     *
     * <p>Without these a pushed program wears the runtime host's theme, which
     * is the wrong application's design and looks like a bug in yours.</p>
     */
    public void addResource(String path, byte[] data) {
        resources.put(path.startsWith("/") ? path : "/" + path, data);
    }

    /**
     * Adds every file that is not a {@code .java} under a directory, keyed by
     * its path relative to that directory.
     */
    public void addResourceTree(File dir) throws IOException {
        addResourceTree(dir, dir);
    }

    private void addResourceTree(File root, File dir) throws IOException {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File f : kids) {
            if (f.isDirectory()) {
                addResourceTree(root, f);
            } else if (!f.getName().endsWith(".java")) {
                String rel = f.getAbsolutePath()
                        .substring(root.getAbsolutePath().length())
                        .replace(File.separatorChar, '/');
                addResource(rel, Files.readAllBytes(f.toPath()));
            }
        }
    }

    /**
     * Adds every {@code .java} under a directory, recursively.
     *
     * <p>Keyed by package rather than by file name. A bare name collides the
     * moment a project has two {@code Util.java} in different packages, and the
     * loser is simply absent -- which the runtime reports as "bundle is missing
     * the source file Util.java" for a file that was right there. Since the
     * source requirement is what lets the app run pushed code at all, losing one
     * silently is not an option.</p>
     */
    public void addSourceTree(File dir) throws IOException {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File f : kids) {
            if (f.isDirectory()) {
                addSourceTree(f);
            } else if (f.getName().endsWith(".java")) {
                String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                addSource(sourceKey(packageOf(text), f.getName()), text);
            }
        }
    }

    /**
     * The bundle key for a source file: {@code com/foo/Util.java}, or the bare
     * name in the default package. Matches how a class's own internal name and
     * its {@code SourceFile} attribute combine, which is how the reader looks
     * one up.
     */
    public static String sourceKey(String packageName, String fileName) {
        if (packageName == null || packageName.length() == 0) {
            return fileName;
        }
        return packageName.replace('.', '/') + "/" + fileName;
    }

    /**
     * The declared package of a source file, read from the file rather than
     * inferred from its path -- a source root is not always the package root.
     */
    public static String packageOf(String text) {
        // Tokens, not line starts. `/* license */ package com.example;` is one
        // line of perfectly ordinary Java, and reading it as the default
        // package stored the source under a key the runtime never looks up --
        // so the push was refused for missing source that had been supplied.
        String code = stripComments(text);
        int i = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '{') {
                break;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                while (i < code.length() && Character.isJavaIdentifierPart(code.charAt(i))) {
                    i++;
                }
                String token = code.substring(start, i);
                if ("package".equals(token)) {
                    int end = code.indexOf(';', i);
                    return end < 0 ? "" : code.substring(i, end).replaceAll("\\s", "");
                }
                if ("import".equals(token) || "class".equals(token)
                        || "interface".equals(token) || "enum".equals(token)) {
                    // Past anything a package declaration may precede.
                    break;
                }
                continue;
            }
            i++;
        }
        return "";
    }

    /**
     * The source with comments and literals blanked out.
     *
     * <p>Blanked rather than removed, so nothing shifts: only the scan above
     * uses this, and it cares about what a token is, not where it sits.</p>
     */
    private static String stripComments(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < text.length()
                        && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(i + 2, text.length());
                out.append(' ');
            } else if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < text.length() && text.charAt(i) != quote) {
                    if (text.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** The class whose {@code main} the runtime should enter. */
    public void setMainClass(String internalName) {
        mainClass = internalName;
    }

    /** Writes the bundle. */
    public void write(OutputStream rawOut) throws IOException {
        // Lambdas become real classes before anything is encoded. Neither
        // target can spin one at run time, and the ahead-of-time pass that
        // handles this for a compiled application never sees a pushed bundle.
        for (ClassNode lambda : InterpLambdaDesugar.desugar(interpreted)) {
            interpreted.add(lambda);
            interpretedNames.add(lambda.name);
        }

        // Bodies are encoded first: doing so interns every string and extern
        // they mention, so the pools are complete before they are written.
        ByteArrayOutputStream classesBuf = new ByteArrayOutputStream();
        DataOutputStream cb = new DataOutputStream(classesBuf);
        cb.writeInt(interpreted.size());
        for (ClassNode cn : interpreted) {
            writeClass(cb, cn);
        }
        cb.flush();

        DataOutputStream out = new DataOutputStream(rawOut);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeUTF(mainClass == null ? "" : mainClass);

        out.writeInt(strings.size());
        for (String s : strings) {
            out.writeUTF(s);
        }

        out.writeInt(externs.size());
        for (int[] e : externs) {
            out.writeInt(e[0]);
            out.writeInt(e[1]);
            out.writeInt(e[2]);
            out.writeInt(e[3]);
        }

        classesBuf.writeTo(out);

        out.writeInt(sources.size());
        for (Map.Entry<String, String> e : sources.entrySet()) {
            out.writeUTF(e.getKey());
            // Stored uncompressed: ParparVM's java.util subset has no
            // java.util.zip, so the device could not inflate it. Sources are
            // small and the transport is a local socket, so nothing is lost.
            byte[] utf8 = e.getValue().getBytes(StandardCharsets.UTF_8);
            out.writeInt(utf8.length);
            out.write(utf8);
        }

        out.writeInt(resources.size());
        for (Map.Entry<String, byte[]> e : resources.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeInt(e.getValue().length);
            out.write(e.getValue());
        }
        out.flush();
    }


    private int intern(String s) {
        Integer existing = stringPool.get(s);
        if (existing != null) {
            return existing.intValue();
        }
        int idx = strings.size();
        strings.add(s);
        stringPool.put(s, Integer.valueOf(idx));
        return idx;
    }

    private int externClass(String internalName) {
        return extern(EXTERN_CLASS, internalName, "", "");
    }

    private int extern(int kind, String owner, String name, String desc) {
        String key = kind + "|" + owner + "|" + name + "|" + desc;
        Integer existing = externPool.get(key);
        if (existing != null) {
            return existing.intValue();
        }
        int idx = externs.size();
        externs.add(new int[]{kind, intern(owner), intern(name), intern(desc)});
        externPool.put(key, Integer.valueOf(idx));
        return idx;
    }

    /**
     * The simple name javac recorded for a class, or "" when it has none.
     *
     * <p>An anonymous class answers "" -- it has a simple name and that name is
     * empty, which is what {@code Class.getSimpleName()} reports. A class with
     * no InnerClasses entry naming itself is top-level and answers null: its
     * simple name is the last segment of its binary name, which the runtime
     * works out rather than paying for a string.</p>
     */
    private static String simpleNameOf(ClassNode cn) {
        if (cn.innerClasses == null) {
            return null;
        }
        for (Object o : cn.innerClasses) {
            org.objectweb.asm.tree.InnerClassNode icn = (org.objectweb.asm.tree.InnerClassNode) o;
            if (cn.name.equals(icn.name)) {
                // An entry with no name is an anonymous class: it has a simple
                // name, and that name is empty.
                return icn.innerName == null ? "" : icn.innerName;
            }
        }
        return null;
    }

    private void writeClass(DataOutputStream out, ClassNode cn) throws IOException {
        out.writeInt(intern(cn.name));
        out.writeInt(cn.access);
        out.writeUTF(cn.sourceFile == null ? "" : cn.sourceFile);
        // The simple name, from the InnerClasses attribute rather than from the
        // shape of the binary name. `Outer$1` is anonymous and has none,
        // `Outer$1Local` is a local class called Local, and both a nested class
        // and a top-level one may carry a `$` in their own identifier -- so
        // splitting the name cannot tell them apart, and only the attribute
        // knows.
        //
        // The flag says whether javac recorded an entry for this class at all,
        // which is what separates an anonymous class (an entry with no name,
        // whose simple name really is empty) from a top-level one (no entry,
        // whose simple name is the last segment of its binary name -- `$` and
        // all, as `Price$USD` is entitled to be called).
        String recorded = simpleNameOf(cn);
        out.writeBoolean(recorded != null);
        out.writeUTF(recorded == null ? "" : recorded);

        // A supertype is either interpreted (named, resolved after load) or an
        // extern. java/lang/Object is always an extern -- it is the host's.
        String superName = cn.superName == null ? "java/lang/Object" : cn.superName;
        boolean superInterpreted = interpretedNames.contains(superName);
        out.writeBoolean(superInterpreted);
        out.writeInt(superInterpreted ? intern(superName) : externClass(superName));

        List<String> interpIfaces = new ArrayList<String>();
        List<Integer> hostIfaces = new ArrayList<Integer>();
        if (cn.interfaces != null) {
            for (String i : cn.interfaces) {
                if (interpretedNames.contains(i)) {
                    interpIfaces.add(i);
                } else {
                    hostIfaces.add(Integer.valueOf(externClass(i)));
                }
            }
        }
        out.writeInt(interpIfaces.size());
        for (String i : interpIfaces) {
            out.writeInt(intern(i));
        }
        out.writeInt(hostIfaces.size());
        for (Integer i : hostIfaces) {
            out.writeInt(i.intValue());
        }

        List<FieldNode> instanceFields = new ArrayList<FieldNode>();
        List<FieldNode> staticFields = new ArrayList<FieldNode>();
        for (FieldNode fn : cn.fields) {
            if ((fn.access & Opcodes.ACC_STATIC) != 0) {
                staticFields.add(fn);
            } else {
                instanceFields.add(fn);
            }
        }
        out.writeInt(instanceFields.size());
        for (FieldNode fn : instanceFields) {
            out.writeInt(intern(fn.name));
            out.writeInt(intern(fn.desc));
        }
        out.writeInt(staticFields.size());
        for (FieldNode fn : staticFields) {
            out.writeInt(intern(fn.name));
            out.writeInt(intern(fn.desc));
        }

        out.writeInt(cn.methods.size());
        for (MethodNode mn : cn.methods) {
            writeMethod(out, mn);
        }
    }

    private void writeMethod(DataOutputStream out, MethodNode mn) throws IOException {
        out.writeInt(intern(mn.name));
        out.writeInt(intern(mn.desc));
        out.writeInt(mn.access);
        out.writeInt(mn.maxStack);
        out.writeInt(mn.maxLocals);

        if (mn.instructions == null || mn.instructions.size() == 0) {
            out.writeInt(0);   // instruction count
            out.writeInt(0);   // code length
            out.writeInt(0);   // exception entries
            out.writeInt(0);   // line entries
            return;
        }

        Encoded enc = encode(mn);

        out.writeInt(enc.instructionOffsets.size());
        for (Integer off : enc.instructionOffsets) {
            out.writeInt(off.intValue());
        }
        out.writeInt(enc.code.size());
        for (Integer c : enc.code) {
            out.writeInt(c.intValue());
        }
        out.writeInt(enc.exceptions.size() / 4);
        for (Integer e : enc.exceptions) {
            out.writeInt(e.intValue());
        }
        out.writeInt(enc.lines.size() / 2);
        for (Integer l : enc.lines) {
            out.writeInt(l.intValue());
        }
    }

    private static final class Encoded {
        final List<Integer> code = new ArrayList<Integer>();
        final List<Integer> instructionOffsets = new ArrayList<Integer>();
        final List<Integer> exceptions = new ArrayList<Integer>();
        final List<Integer> lines = new ArrayList<Integer>();
    }

    private Encoded encode(MethodNode mn) {
        Encoded enc = new Encoded();

        // Pass 1: assign an instruction index to every real instruction, so a
        // label can be resolved to the index of the next one. Labels, frames
        // and line markers are not instructions and do not get an index.
        Map<LabelNode, Integer> labelToIndex = new HashMap<LabelNode, Integer>();
        List<AbstractInsnNode> real = new ArrayList<AbstractInsnNode>();
        List<LabelNode> pendingLabels = new ArrayList<LabelNode>();
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LabelNode) {
                pendingLabels.add((LabelNode) insn);
                continue;
            }
            if (insn instanceof FrameNode || insn instanceof LineNumberNode) {
                continue;
            }
            for (LabelNode ln : pendingLabels) {
                labelToIndex.put(ln, Integer.valueOf(real.size()));
            }
            pendingLabels.clear();
            real.add(insn);
        }
        // Labels at the very end (an exception range's exclusive end, most
        // often) resolve one past the last instruction.
        for (LabelNode ln : pendingLabels) {
            labelToIndex.put(ln, Integer.valueOf(real.size()));
        }

        // Line numbers, recorded against the index of the instruction they
        // precede.
        int idx = 0;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                LineNumberNode lnn = (LineNumberNode) insn;
                Integer at = labelToIndex.get(lnn.start);
                if (at != null) {
                    enc.lines.add(at);
                    enc.lines.add(Integer.valueOf(lnn.line));
                }
            }
        }

        // Pass 2: emit.
        for (idx = 0; idx < real.size(); idx++) {
            enc.instructionOffsets.add(Integer.valueOf(enc.code.size()));
            emit(enc, real.get(idx), labelToIndex);
        }

        if (mn.tryCatchBlocks != null) {
            for (Object o : mn.tryCatchBlocks) {
                TryCatchBlockNode tc = (TryCatchBlockNode) o;
                Integer start = labelToIndex.get(tc.start);
                Integer end = labelToIndex.get(tc.end);
                Integer handler = labelToIndex.get(tc.handler);
                if (start == null || end == null || handler == null) {
                    continue;
                }
                enc.exceptions.add(start);
                enc.exceptions.add(end);
                enc.exceptions.add(handler);
                enc.exceptions.add(Integer.valueOf(tc.type == null ? -1 : externClass(tc.type)));
            }
        }
        return enc;
    }

    private void emit(Encoded enc, AbstractInsnNode insn, Map<LabelNode, Integer> labels) {
        int op = insn.getOpcode();
        switch (insn.getType()) {
            case AbstractInsnNode.INSN:
                enc.code.add(Integer.valueOf(op));
                break;
            case AbstractInsnNode.INT_INSN: {
                IntInsnNode i = (IntInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(i.operand));
                break;
            }
            case AbstractInsnNode.VAR_INSN: {
                VarInsnNode v = (VarInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(v.var));
                break;
            }
            case AbstractInsnNode.TYPE_INSN: {
                TypeInsnNode t = (TypeInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(externClass(t.desc)));
                break;
            }
            case AbstractInsnNode.FIELD_INSN: {
                FieldInsnNode f = (FieldInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(extern(EXTERN_FIELD, f.owner, f.name, f.desc)));
                break;
            }
            case AbstractInsnNode.METHOD_INSN: {
                MethodInsnNode m = (MethodInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(extern(EXTERN_METHOD, m.owner, m.name, m.desc)));
                break;
            }
            case AbstractInsnNode.JUMP_INSN: {
                JumpInsnNode j = (JumpInsnNode) insn;
                Integer target = labels.get(j.label);
                enc.code.add(Integer.valueOf(op));
                enc.code.add(target == null ? Integer.valueOf(-1) : target);
                break;
            }
            case AbstractInsnNode.LDC_INSN: {
                LdcInsnNode l = (LdcInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                emitLdc(enc, l.cst);
                break;
            }
            case AbstractInsnNode.IINC_INSN: {
                IincInsnNode i = (IincInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(i.var));
                enc.code.add(Integer.valueOf(i.incr));
                break;
            }
            case AbstractInsnNode.TABLESWITCH_INSN: {
                TableSwitchInsnNode t = (TableSwitchInsnNode) insn;
                // length, min, max, default, targets...
                List<Integer> body = new ArrayList<Integer>();
                body.add(Integer.valueOf(t.min));
                body.add(Integer.valueOf(t.max));
                body.add(indexOf(labels, t.dflt));
                for (Object lbl : t.labels) {
                    body.add(indexOf(labels, (LabelNode) lbl));
                }
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(body.size()));
                enc.code.addAll(body);
                break;
            }
            case AbstractInsnNode.LOOKUPSWITCH_INSN: {
                LookupSwitchInsnNode l = (LookupSwitchInsnNode) insn;
                // length, default, count, (key, target)...
                List<Integer> body = new ArrayList<Integer>();
                body.add(indexOf(labels, l.dflt));
                body.add(Integer.valueOf(l.keys.size()));
                for (int i = 0; i < l.keys.size(); i++) {
                    body.add((Integer) l.keys.get(i));
                    body.add(indexOf(labels, (LabelNode) l.labels.get(i)));
                }
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(body.size()));
                enc.code.addAll(body);
                break;
            }
            case AbstractInsnNode.MULTIANEWARRAY_INSN: {
                MultiANewArrayInsnNode m = (MultiANewArrayInsnNode) insn;
                enc.code.add(Integer.valueOf(op));
                enc.code.add(Integer.valueOf(externClass(m.desc)));
                enc.code.add(Integer.valueOf(m.dims));
                break;
            }
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
                // Lambdas and method references are gone by now --
                // InterpLambdaDesugar rewrote them into real classes before
                // encoding started. Anything still here is a bootstrap this
                // build does not implement, and there is no runtime
                // invokedynamic to fall back on, so say which one it is.
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                throw new IllegalStateException(
                        "invokedynamic against " + indy.bsm.getOwner() + "." + indy.bsm.getName()
                                + " reached the bundle writer, and neither target has a runtime "
                                + "invokedynamic. String concatenation is handled by compiling "
                                + "pushed code with -XDstringConcat=inline; lambdas and method "
                                + "references are desugared by InterpLambdaDesugar. This is "
                                + "neither.");
            }
            default:
                throw new IllegalStateException("unhandled instruction type " + insn.getType()
                        + " (opcode " + op + ")");
        }
    }

    private static Integer indexOf(Map<LabelNode, Integer> labels, LabelNode ln) {
        Integer i = labels.get(ln);
        return i == null ? Integer.valueOf(-1) : i;
    }

    private void emitLdc(Encoded enc, Object cst) {
        if (cst instanceof Integer) {
            enc.code.add(Integer.valueOf(LDC_INT));
            enc.code.add((Integer) cst);
        } else if (cst instanceof Long) {
            // Encoded as a string so the int-array code stream stays uniform;
            // the runtime parses it once at load, not per execution.
            enc.code.add(Integer.valueOf(LDC_LONG));
            enc.code.add(Integer.valueOf(intern(cst.toString())));
        } else if (cst instanceof Float) {
            enc.code.add(Integer.valueOf(LDC_FLOAT));
            enc.code.add(Integer.valueOf(Float.floatToIntBits(((Float) cst).floatValue())));
        } else if (cst instanceof Double) {
            enc.code.add(Integer.valueOf(LDC_DOUBLE));
            enc.code.add(Integer.valueOf(intern(cst.toString())));
        } else if (cst instanceof String) {
            enc.code.add(Integer.valueOf(LDC_STRING));
            enc.code.add(Integer.valueOf(intern((String) cst)));
        } else if (cst instanceof Type) {
            enc.code.add(Integer.valueOf(LDC_CLASS));
            enc.code.add(Integer.valueOf(externClass(((Type) cst).getInternalName())));
        } else {
            throw new IllegalStateException("unsupported constant " + cst.getClass().getName());
        }
    }
}
