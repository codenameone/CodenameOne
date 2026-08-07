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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds code that relies on a failing cast throwing ClassCastException.
 *
 * <p>ParparVM's CHECKCAST is unchecked: {@code BC_CHECKCAST} expands to nothing
 * and the optimizer drops the instruction, so a bad cast hands the wrong pointer
 * to the next instruction rather than throwing. Making it throw would put a class
 * check on every cast in every app; the rule instead is that our own code must not
 * depend on the exception. Test with {@code instanceof} and branch:</p>
 *
 * <pre>
 *   // relies on ClassCastException -- the handler never runs on iOS, and the
 *   // cast target's fields get read out of a foreign object instead (issue #5531)
 *   try { return Double.parseDouble((String) o); } catch (Exception e) { return def; }
 *
 *   // correct everywhere
 *   if (o instanceof Number) { return ((Number) o).doubleValue(); }
 * </pre>
 *
 * <h2>What is reported</h2>
 * <p>A CHECKCAST inside a try range whose handler catches ClassCastException or
 * one of its supertypes: that handler is what would absorb the failure on the
 * simulator and on Android, so the method behaves differently on iOS.</p>
 *
 * <p>Note the rule is about the CAST, not about the handler. A
 * {@code catch (ClassCastException)} with no cast in its try is NOT reported --
 * ParparVM delivers an explicitly thrown ClassCastException perfectly well (only
 * the implicit one a failed CHECKCAST would raise is missing), so such a handler
 * is still live. {@code java.util.AbstractSet.equals} is exactly that case: its
 * handler is there for a foreign {@code containsAll} that throws, not for a cast
 * of its own.</p>
 *
 * <p>{@code finally} blocks and handlers for unrelated exception types are never
 * reported -- neither can absorb a failed cast.</p>
 *
 * <p>Findings are held against a baseline file rather than fixed wholesale: many
 * are casts that merely sit inside a broad defensive guard and cannot actually
 * fail. The gate is a ratchet -- new code must not add entries.</p>
 *
 * <pre>
 *   java -cp ByteCodeTranslator.jar com.codename1.tools.translator.CastSemanticsVerifier \
 *       [--baseline FILE] [--write-baseline FILE] path [path ...]
 * </pre>
 */
public class CastSemanticsVerifier {
    /** Handlers that would catch a ClassCastException. */
    private static final Set<String> CATCHES_CAST_FAILURE = new HashSet<String>(Arrays.asList(
            "java/lang/ClassCastException",
            "java/lang/RuntimeException",
            "java/lang/Exception",
            "java/lang/Throwable"));

    private static final String CLASS_CAST_EXCEPTION = "java/lang/ClassCastException";

    public static class Violation {
        public final String className;
        public final String methodName;
        public final String methodDesc;
        public final int line;
        public final String castTo;
        public final String caughtType;

        Violation(String className, String methodName, String methodDesc, int line,
                  String castTo, String caughtType) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.line = line;
            this.castTo = castTo;
            this.caughtType = caughtType;
        }

        /**
         * Baseline key: {@code com/example/Foo#bar(I)V}. Deliberately method
         * granularity -- line numbers move with unrelated edits and would make the
         * baseline churn on every commit.
         */
        public String key() {
            return className + "#" + methodName + methodDesc;
        }

        public String describe() {
            return className.replace('/', '.') + "." + methodName
                    + (line > 0 ? " (line " + line + ")" : "")
                    + ": cast to " + castTo.replace('/', '.')
                    + " would be swallowed by catch(" + caughtType.replace('/', '.') + ")";
        }

        @Override
        public String toString() {
            return describe();
        }
    }

    /** Analyzes one class file's bytes. */
    public static List<Violation> analyze(byte[] classBytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, ClassReader.SKIP_FRAMES);

        List<Violation> violations = new ArrayList<Violation>();
        if (cn.methods == null) {
            return violations;
        }
        for (MethodNode method : cn.methods) {
            if (method.instructions == null || method.tryCatchBlocks == null
                    || method.tryCatchBlocks.isEmpty()) {
                continue;
            }
            collectAbsorbedCasts(cn, method, violations);
        }
        return violations;
    }

    private static void collectAbsorbedCasts(ClassNode cn, MethodNode method, List<Violation> violations) {
        // guards[slot] -> the types proven for that local right here, by an
        // `if (local instanceof T)` the walk is currently inside
        InstanceOfGuards guards = new InstanceOfGuards(method);
        int line = -1;
        int index = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;
                insn = insn.getNext(), index++) {
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode) insn).line;
                continue;
            }
            guards.advance(insn, index);
            if (insn.getOpcode() != Opcodes.CHECKCAST) {
                continue;
            }
            String castTo = ((TypeInsnNode) insn).desc;
            // `if (o instanceof T) { ... (T) o ... }` is the fix this gate asks
            // for; javac still emits the CHECKCAST, and reporting it would make the
            // gate punish its own remedy
            if (guards.proves(previousRealInstruction(insn), castTo)) {
                continue;
            }
            String caught = coveringHandler(method, index);
            if (caught != null) {
                violations.add(new Violation(cn.name, method.name, method.desc,
                        line, castTo, caught));
            }
        }
    }

    /** The nearest preceding instruction that is not a label, line or frame marker. */
    private static AbstractInsnNode previousRealInstruction(AbstractInsnNode from) {
        for (AbstractInsnNode iter = from.getPrevious(); iter != null; iter = iter.getPrevious()) {
            if (iter.getOpcode() >= 0) {
                return iter;
            }
        }
        return null;
    }

    /**
     * Tracks which locals javac has already proven the type of, so a cast the
     * author guarded is not reported.
     *
     * <p>Recognises the one shape javac emits for {@code if (x instanceof T)} on a
     * local: {@code ALOAD n; INSTANCEOF T; IFEQ end} -- the proof holds from there
     * until {@code end}, and is dropped if the local is reassigned or if control
     * can re-enter the region at a branch target. {@code IFNE} (the negated form)
     * is not recognised, and neither is a proof about a field or an expression;
     * anything unrecognised simply keeps the cast reportable, so being incomplete
     * here costs noise rather than correctness.</p>
     */
    private static final class InstanceOfGuards {
        private final MethodNode method;
        /** slot -> proven type, valid while the walk index is below endIndex. */
        private final List<int[]> ranges = new ArrayList<int[]>();
        private final List<String> types = new ArrayList<String>();

        InstanceOfGuards(MethodNode method) {
            this.method = method;
        }

        /** Updates the live proofs for the instruction about to be examined. */
        void advance(AbstractInsnNode insn, int index) {
            for (int iter = ranges.size() - 1; iter >= 0; iter--) {
                if (index >= ranges.get(iter)[1]) {
                    ranges.remove(iter);
                    types.remove(iter);
                }
            }
            int opcode = insn.getOpcode();
            if (opcode == Opcodes.ASTORE) {
                dropSlot(((org.objectweb.asm.tree.VarInsnNode) insn).var);
                return;
            }
            if (opcode != Opcodes.IFEQ) {
                return;
            }
            // ... INSTANCEOF T ; IFEQ end   <- the false branch skips the guarded block
            AbstractInsnNode instanceOf = previousRealInstruction(insn);
            if (instanceOf == null || instanceOf.getOpcode() != Opcodes.INSTANCEOF) {
                return;
            }
            AbstractInsnNode load = previousRealInstruction(instanceOf);
            if (load == null || load.getOpcode() != Opcodes.ALOAD) {
                return;
            }
            int end = method.instructions.indexOf(((org.objectweb.asm.tree.JumpInsnNode) insn).label);
            if (end <= index) {
                return; // backwards branch; not the if-shape we recognise
            }
            ranges.add(new int[]{((org.objectweb.asm.tree.VarInsnNode) load).var, end});
            types.add(((TypeInsnNode) instanceOf).desc);
        }

        private void dropSlot(int slot) {
            for (int iter = ranges.size() - 1; iter >= 0; iter--) {
                if (ranges.get(iter)[0] == slot) {
                    ranges.remove(iter);
                    types.remove(iter);
                }
            }
        }

        /** True when {@code load} reads a local already proven to be {@code type}. */
        boolean proves(AbstractInsnNode load, String type) {
            if (load == null || load.getOpcode() != Opcodes.ALOAD) {
                return false;
            }
            int slot = ((org.objectweb.asm.tree.VarInsnNode) load).var;
            for (int iter = 0; iter < ranges.size(); iter++) {
                if (ranges.get(iter)[0] == slot && types.get(iter).equals(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * The type of a handler that would catch a cast failure at {@code index}, or
     * null when none would. ClassCastException wins when several cover the range,
     * so the report names the most specific one.
     */
    private static String coveringHandler(MethodNode method, int index) {
        String caught = null;
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            if (block.type == null || !CATCHES_CAST_FAILURE.contains(block.type)) {
                continue;
            }
            int start = method.instructions.indexOf(block.start);
            int end = method.instructions.indexOf(block.end);
            if (index < start || index >= end) {
                continue;
            }
            if (caught == null || CLASS_CAST_EXCEPTION.equals(block.type)) {
                caught = block.type;
            }
        }
        return caught;
    }

    /** Analyzes every class under a directory, inside a jar, or a single class file. */
    public static List<Violation> analyzeTree(File root) throws IOException {
        List<Violation> violations = new ArrayList<Violation>();
        if (root.isDirectory()) {
            collectFromDirectory(root, violations);
        } else if (root.getName().endsWith(".jar") || root.getName().endsWith(".zip")) {
            collectFromArchive(root, violations);
        } else if (root.getName().endsWith(".class")) {
            InputStream in = new FileInputStream(root);
            try {
                violations.addAll(analyze(readAll(in)));
            } finally {
                in.close();
            }
        }
        Collections.sort(violations, new Comparator<Violation>() {
            @Override
            public int compare(Violation a, Violation b) {
                int byKey = a.key().compareTo(b.key());
                if (byKey != 0) {
                    return byKey;
                }
                return a.line < b.line ? -1 : (a.line > b.line ? 1 : 0);
            }
        });
        return violations;
    }

    private static void collectFromDirectory(File dir, List<Violation> violations) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        // sorted so the report does not depend on filesystem order
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                collectFromDirectory(child, violations);
            } else if (child.getName().endsWith(".class")) {
                InputStream in = new FileInputStream(child);
                try {
                    violations.addAll(analyze(readAll(in)));
                } finally {
                    in.close();
                }
            }
        }
    }

    private static void collectFromArchive(File archive, List<Violation> violations) throws IOException {
        ZipFile zip = new ZipFile(archive);
        try {
            List<String> names = new ArrayList<String>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    names.add(entry.getName());
                }
            }
            Collections.sort(names);
            for (String name : names) {
                InputStream in = zip.getInputStream(zip.getEntry(name));
                try {
                    violations.addAll(analyze(readAll(in)));
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Reads the baseline. Format is {@code class#method(desc)|note}; blank lines
     * and {@code #} comments are ignored. Only the key is significant -- the note
     * is there so a human reading the file can tell what the entry is about.
     */
    public static Set<String> readBaseline(File file) throws IOException {
        Set<String> keys = new HashSet<String>();
        if (!file.exists()) {
            return keys;
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                int bar = line.indexOf('|');
                keys.add((bar < 0 ? line : line.substring(0, bar)).trim());
            }
        } finally {
            reader.close();
        }
        return keys;
    }

    private static void writeBaseline(File file, List<Violation> violations) throws IOException {
        Set<String> lines = new TreeSet<String>();
        for (Violation violation : violations) {
            lines.add(violation.key() + "|cast to " + violation.castTo.replace('/', '.')
                    + " inside catch(" + violation.caughtType.replace('/', '.') + ")");
        }
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            out.println("# Casts that a broad catch handler would swallow, as of the day this");
            out.println("# gate was added. ParparVM never throws ClassCastException (see");
            out.println("# CastSemanticsVerifier), so on iOS these run on rather than take the");
            out.println("# handler. Most are casts that merely sit inside a defensive guard and");
            out.println("# cannot actually fail; the rest are latent bugs.");
            out.println("#");
            out.println("# This is a ratchet, not an allow-list: new code must not add entries.");
            out.println("# Delete an entry when the method stops relying on the exception.");
            out.println("# Regenerate with scripts/check-cast-semantics.sh --write-baseline.");
            out.println("#");
            out.println("# Format: <class>#<method><descriptor>|<note>");
            out.println();
            for (String line : lines) {
                out.println(line);
            }
        } finally {
            out.close();
        }
    }

    public static void main(String[] args) throws IOException {
        File baselineFile = null;
        File writeBaselineFile = null;
        List<File> roots = new ArrayList<File>();
        for (int iter = 0; iter < args.length; iter++) {
            if ("--baseline".equals(args[iter]) && iter + 1 < args.length) {
                baselineFile = new File(args[++iter]);
            } else if ("--write-baseline".equals(args[iter]) && iter + 1 < args.length) {
                writeBaselineFile = new File(args[++iter]);
            } else {
                roots.add(new File(args[iter]));
            }
        }
        if (roots.isEmpty()) {
            System.err.println("usage: CastSemanticsVerifier [--baseline FILE] "
                    + "[--write-baseline FILE] path [path ...]");
            System.exit(2);
        }

        List<Violation> all = new ArrayList<Violation>();
        for (File root : roots) {
            if (!root.exists()) {
                System.err.println("CastSemanticsVerifier: no such path: " + root);
                System.exit(2);
            }
            all.addAll(analyzeTree(root));
        }

        if (writeBaselineFile != null) {
            writeBaseline(writeBaselineFile, all);
            System.out.println("CastSemanticsVerifier: wrote " + all.size()
                    + " entries to " + writeBaselineFile);
        }

        Set<String> baseline = baselineFile == null
                ? Collections.<String>emptySet() : readBaseline(baselineFile);

        // LinkedHashSet: several casts in one method collapse to one line, and the
        // report keeps bytecode order
        Set<String> introduced = new LinkedHashSet<String>();
        for (Violation violation : all) {
            if (!baseline.contains(violation.key())) {
                introduced.add(violation.describe());
            }
        }

        if (introduced.isEmpty()) {
            System.out.println("CastSemanticsVerifier: no new reliance on ClassCastException ("
                    + all.size() + " baselined).");
            return;
        }
        for (String description : introduced) {
            System.err.println("  " + description);
        }
        System.err.println();
        System.err.println("CastSemanticsVerifier: " + introduced.size()
                + " cast(s) rely on ClassCastException, which ParparVM does not throw for a"
                + " failed cast -- the handler would not run on iOS.");
        System.err.println("Test the type with instanceof and branch, rather than catching.");
        System.exit(1);
    }
}
