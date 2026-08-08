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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The on-device debugger's view of a frame's locals must describe storage that
 * actually holds what it says it holds.
 *
 * <p>A JVM slot is reused across disjoint scopes, and the occupants need not
 * share a type: {@code int count} and {@code String label} can both live in
 * slot 2 of one method. ParparVM gives each its own storage — {@code
 * ilocals_2_} is a C auto, {@code locals[2].data.o} is a GC-scanned frame slot
 * — so a side-table with one address <em>per slot</em> could only ever name one
 * of them, while still listing a row for each. The mismatch made the runtime
 * read eight bytes of reference out of a four-byte {@code JAVA_INT} and then
 * dereference the result, which is the {@code signal 11} on a listener
 * breakpoint reported in issue #5333.</p>
 *
 * <p>What these pin is the invariant the fix rests on: one address per
 * <em>row</em>, so a row's type code and the storage its address points at can
 * never disagree. Asserted on the emitted C because the failure only shows up
 * on a device, under a debugger, at a breakpoint.</p>
 */
class OnDeviceDebugFrameTableTest {

    private static final String HOST = "com/example/DbgHost";

    /** Source lines the fixture's two disjoint scopes open and close on. */
    private static final int INT_SCOPE_LINE = 10;
    private static final int REF_SCOPE_LINE = 14;
    private static final int METHOD_END_LINE = 18;

    private boolean previousOnDeviceDebug;

    @BeforeEach
    void enableOnDeviceDebug() {
        Parser.cleanup();
        previousOnDeviceDebug = BytecodeMethod.onDeviceDebug;
        BytecodeMethod.onDeviceDebug = true;
    }

    @AfterEach
    void restoreOnDeviceDebug() {
        BytecodeMethod.onDeviceDebug = previousOnDeviceDebug;
        Parser.cleanup();
    }

    /**
     * The two occupants of a reused slot each get their own row, and each row
     * points at the storage its own type lives in.
     */
    @Test
    void aSlotSharedByAnIntAndAReferenceYieldsOneRowPerType() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        assertEquals(Arrays.asList("I", "L"), typeCodesForSlot(table, 2),
                "both occupants of slot 2 should be described, was:\n" + table);
        assertEquals("&ilocals_2_", addressForRow(table, 2, 'I'),
                "the int row must point at the int auto, was:\n" + table);
        assertEquals("&locals[2].data.o", addressForRow(table, 2, 'L'),
                "the reference row must point at the GC-scanned frame slot, was:\n" + table);
    }

    /**
     * The generic form of the same statement, over every row: a reference row
     * never points at a primitive auto and vice versa. This is the property
     * that stops the runtime dereferencing an int.
     */
    @Test
    void everyRowPointsAtStorageOfItsOwnKind() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        assertEquals(table.rows.size(), table.addresses.size(),
                "the address array must have exactly one entry per row, was:\n" + table);
        for (int i = 0; i < table.rows.size(); i++) {
            Row row = table.rows.get(i);
            String address = table.addresses.get(i);
            if (row.isReference()) {
                assertTrue(address.startsWith("&locals["),
                        "reference row " + row + " points at primitive storage " + address
                                + ", was:\n" + table);
            } else {
                assertTrue(address.startsWith("&" + row.qualifier() + "locals_"),
                        "primitive row " + row + " points at " + address
                                + ", which is not its own storage, was:\n" + table);
                assertFalse(address.startsWith("&locals["),
                        "primitive row " + row + " points at the object slot, was:\n" + table);
            }
        }
    }

    /**
     * The address for a row names the slot the row declares, not some other
     * slot that happens to share its type.
     */
    @Test
    void everyRowAddressesItsOwnSlot() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        for (int i = 0; i < table.rows.size(); i++) {
            Row row = table.rows.get(i);
            String expected = row.isReference()
                    ? "&locals[" + row.slot + "].data.o"
                    : "&" + row.qualifier() + "locals_" + row.slot + "_";
            assertEquals(expected, table.addresses.get(i),
                    "row " + row + " should address slot " + row.slot + ", was:\n" + table);
        }
    }

    /**
     * Two translations of the same input emit the same table.
     *
     * <p>The rows come from a {@code HashSet}, whose iteration order varies
     * between runs. Taken in that order the emitted table differed build to
     * build, which is both a reproducibility problem and the reason the old
     * per-slot table picked an arbitrary one of a reused slot's two types.</p>
     */
    @Test
    void theEmittedTableIsTheSameOnEveryTranslation() throws Exception {
        String first = frameTableOf(translateHost(), "handler").toString();
        Parser.cleanup();
        String second = frameTableOf(translateHost(), "handler").toString();

        assertEquals(first, second, "the locals side-table must not depend on hash order");
    }

    /**
     * A frame with nothing to describe still publishes its frame info.
     *
     * <p>The pointer is what tells the debugger which method a frame belongs
     * to. Skipping the publication for a method with no locals left the frame
     * holding whatever the previous occupant of that call depth wrote, so the
     * IDE was shown an unrelated method — and, worse, that method's locals
     * address array, which points into a C frame that has already returned.</p>
     */
    @Test
    void aMethodWithNoLocalsStillPublishesItsFrameInfo() throws Exception {
        String body = cFunctionBody(translateHost(), "_ping__");

        assertTrue(body.contains("callStackFrameInfo[threadStateData->callStackOffset - 1] = &__cn1_finfo_"),
                "a locals-free frame must still identify its method, was:\n" + body);
        assertTrue(body.contains("callStackLocalsAddresses[threadStateData->callStackOffset - 1] = 0;"),
                "and must clear the locals pointer rather than leave it stale, was:\n" + body);
    }

    /** The declared count and the emitted rows agree. */
    @Test
    void theDeclaredRowCountMatchesTheRowsEmitted() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        assertEquals(table.rows.size(), table.declaredRowCount,
                "varTableCount must match the side-table it describes, was:\n" + table);
    }

    /**
     * The two occupants of the reused slot report disjoint scopes, in source
     * order.
     *
     * <p>This is what lets the runtime show one of them at a time. Reporting
     * both as live — which is what an always-live table amounts to — means the
     * variable the code has not reached yet is displayed alongside the one it
     * has, reading storage that belongs to the other scope.</p>
     */
    @Test
    void theTwoOccupantsOfAReusedSlotReportDisjointScopes() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");
        Row intRow = rowFor(table, 2, 'I');
        Row refRow = rowFor(table, 2, 'L');

        assertEquals(INT_SCOPE_LINE, intRow.startLine, "int scope opens, was:\n" + table);
        assertEquals(REF_SCOPE_LINE, intRow.endLine, "int scope closes, was:\n" + table);
        assertEquals(REF_SCOPE_LINE, refRow.startLine, "reference scope opens, was:\n" + table);
        assertTrue(refRow.endLine == 0 || refRow.endLine > REF_SCOPE_LINE,
                "reference scope should stay open past its declaration, was:\n" + table);
        assertTrue(intRow.endLine <= refRow.startLine,
                "the two scopes must not overlap, was:\n" + table);
    }

    /** Stated as the runtime asks it: which local is live at this line. */
    @Test
    void onlyOneOccupantOfAReusedSlotIsLiveAtAnyLine() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");
        Row intRow = rowFor(table, 2, 'I');
        Row refRow = rowFor(table, 2, 'L');

        assertTrue(intRow.liveAt(INT_SCOPE_LINE), "the int is live where it is declared");
        assertFalse(refRow.liveAt(INT_SCOPE_LINE),
                "the reference must be hidden before its declaration, was:\n" + table);

        assertTrue(refRow.liveAt(REF_SCOPE_LINE), "the reference is live where it is declared");
        assertFalse(intRow.liveAt(REF_SCOPE_LINE),
                "the int must be hidden once its scope closes, was:\n" + table);
    }

    /**
     * A local the class file gives a name and a scope keeps them.
     *
     * <p>A store opcode synthesises a placeholder for the slot before the
     * class file's own entry is visited, and the two collide in the set that
     * holds them. The placeholder used to win, which cost both the name and
     * the scope — the debugger showed "v2", live for the whole method.</p>
     */
    @Test
    void aDeclaredLocalKeepsItsScopeDespiteTheStoreSynthesisedPlaceholder() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        assertTrue(rowFor(table, 2, 'I').startLine > 0,
                "the declared int should carry its scope, not the placeholder's, was:\n" + table);
        assertTrue(rowFor(table, 3, 'I').startLine > 0,
                "the same holds for a slot that is never reused, was:\n" + table);
    }

    /** Args and the receiver are live for the whole method. */
    @Test
    void theReceiverAndArgumentsAreLiveFromTheFirstLine() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        for (int slot : new int[] { 0, 1 }) {
            Row row = rowFor(table, slot, 'L');
            assertTrue(row.liveAt(INT_SCOPE_LINE) && row.liveAt(REF_SCOPE_LINE),
                    "slot " + slot + " should be live throughout, was:\n" + table);
        }
    }

    /**
     * A scope that runs to the end of the method stays live on the last line.
     *
     * <p>javac closes those scopes with a label placed after the final
     * instruction, which no line number follows. Resolving it to the last line
     * seen would make the exclusive end land <em>on</em> that line and hide
     * every method-wide local — {@code this} and the parameters included — at
     * exactly the breakpoint a developer is most likely to set, the closing
     * brace or the return.</p>
     */
    @Test
    void methodWideLocalsStayLiveOnTheFinalLine() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");

        for (int slot : new int[] { 0, 1, 4 }) {
            Row row = rowFor(table, slot, 'L');
            assertEquals(0, row.endLine,
                    "slot " + slot + " runs to the end of the method, so its scope"
                            + " must be open-ended, was:\n" + table);
            assertTrue(row.liveAt(METHOD_END_LINE),
                    "slot " + slot + " must still be visible on the last line, was:\n" + table);
        }
    }

    /** A scope that genuinely closes mid-method still ends where it should. */
    @Test
    void aScopeThatClosesMidMethodIsStillBounded() throws Exception {
        FrameTable table = frameTableOf(translateHost(), "handler");
        Row count = rowFor(table, 2, 'I');

        assertEquals(REF_SCOPE_LINE, count.endLine,
                "the int's scope closes where the reference's opens, was:\n" + table);
        assertFalse(count.liveAt(METHOD_END_LINE),
                "and it must not reappear on the final line, was:\n" + table);
    }

    /**
     * The device's frame table and the symbol table the IDE reads describe the
     * same local the same way.
     *
     * <p>They are produced at different times — the frame side-table during
     * code generation, the symbol table after every class has been generated —
     * and {@code optimize()} rewrites the instruction list in between, which
     * is what a scope is resolved against. The optimizer does preserve the
     * label and line-number instructions that walk depends on, so this holds
     * today either way; it is pinned because nothing else states that, and the
     * two disagreeing would not fail loudly. The IDE would ask for a slot the
     * device considers out of scope and the local would simply be missing from
     * the variables view.</p>
     */
    @Test
    void scopesAreResolvedBeforeTheOptimizerRewritesTheInstructionList() throws Exception {
        Parser.parse(writeHostClass().toFile());
        ByteCodeClass objectClass =
                new ByteCodeClass("java_lang_Object", "java/lang/Object");
        ByteCodeClass host = Parser.getClassObject("com_example_DbgHost");
        host.setBaseClassObject(objectClass);
        host.setBaseInterfacesObject(Collections.<ByteCodeClass>emptyList());
        host.updateAllDependencies();

        BytecodeMethod handler = null;
        for (BytecodeMethod m : host.getMethods()) {
            if ("handler".equals(m.getMethodName())) {
                handler = m;
            }
        }
        assertNotNull(handler, "the fixture should carry a handler method");

        List<int[]> beforeGeneration = handler.debugVarScopes(handler.debugVarEntries());
        // generateCCode runs optimize(), which rewrites the instruction list
        // the scopes would otherwise be resolved against.
        host.generateCCode(Arrays.asList(objectClass, host));
        List<int[]> afterGeneration = handler.debugVarScopes(handler.debugVarEntries());

        assertEquals(beforeGeneration.size(), afterGeneration.size());
        for (int i = 0; i < beforeGeneration.size(); i++) {
            assertArrayEquals(beforeGeneration.get(i), afterGeneration.get(i),
                    "scope " + i + " changed across code generation: "
                            + Arrays.toString(beforeGeneration.get(i)) + " -> "
                            + Arrays.toString(afterGeneration.get(i)));
        }
        // And they are real scopes, not a table of always-live pairs that
        // would agree trivially.
        boolean anyScoped = false;
        for (int[] scope : afterGeneration) {
            anyScoped |= scope[0] > 0;
        }
        assertTrue(anyScoped, "the fixture should produce at least one scoped local");
    }

    // ---- parsing the generated C ------------------------------------------

    private static final class Row {
        final int startLine;
        final int endLine;
        final int slot;
        final char typeCode;

        Row(int startLine, int endLine, int slot, char typeCode) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.slot = slot;
            this.typeCode = typeCode;
        }

        /** Whether this local is reported as live at the given source line. */
        boolean liveAt(int line) {
            if (startLine <= 0) return true;
            if (line < startLine) return false;
            return endLine <= 0 || line < endLine;
        }

        boolean isReference() {
            return typeCode == 'L' || typeCode == '[';
        }

        /** ParparVM's storage qualifier: byte/short/char/boolean collapse onto 'i'. */
        char qualifier() {
            switch (typeCode) {
                case 'J': return 'l';
                case 'F': return 'f';
                case 'D': return 'd';
                case 'L': case '[': return 'o';
                default: return 'i';
            }
        }

        @Override public String toString() {
            return "{slot=" + slot + ", type='" + typeCode + "', lines="
                    + startLine + ".." + (endLine == 0 ? "end" : String.valueOf(endLine)) + "}";
        }
    }

    private static final class FrameTable {
        final List<Row> rows = new ArrayList<>();
        final List<String> addresses = new ArrayList<>();
        int declaredRowCount;

        @Override public String toString() {
            StringBuilder b = new StringBuilder("frame table:\n");
            for (int i = 0; i < Math.max(rows.size(), addresses.size()); i++) {
                b.append("  #").append(i).append(' ')
                 .append(i < rows.size() ? rows.get(i).toString() : "<no row>")
                 .append(" -> ")
                 .append(i < addresses.size() ? addresses.get(i) : "<no address>")
                 .append('\n');
            }
            b.append("  declaredRowCount=").append(declaredRowCount).append('\n');
            return b.toString();
        }
    }

    private static final Pattern VAR_ROW =
            Pattern.compile("\\{\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*'(.)'\\s*\\}");
    private static final Pattern FINFO =
            Pattern.compile("struct cn1_frame_info __cn1_finfo_\\w+ = \\{\\s*"
                    + "(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+),\\s*(\\d+)");

    /**
     * Pulls the variable side-table, its declared row count and the matching
     * {@code __cn1_local_addrs} initialiser out of the generated C for the
     * first method whose C name contains {@code methodName}.
     */
    private FrameTable frameTableOf(String code, String methodName) {
        int anchor = code.indexOf("_" + methodName + "__");
        assertTrue(anchor >= 0, "no generated function for " + methodName + ":\n" + code);
        String identifier = methodIdentifierAt(code, anchor);

        FrameTable table = new FrameTable();

        String varsMarker = "static const struct cn1_var_entry __cn1_vars_" + identifier + "[] = {";
        int varsAt = code.indexOf(varsMarker);
        assertTrue(varsAt >= 0, "no variable side-table for " + methodName + ":\n" + code);
        int varsEnd = code.indexOf("};", varsAt);
        Matcher rowMatcher = VAR_ROW.matcher(code.substring(varsAt + varsMarker.length(), varsEnd));
        while (rowMatcher.find()) {
            table.rows.add(new Row(Integer.parseInt(rowMatcher.group(1)),
                    Integer.parseInt(rowMatcher.group(2)),
                    Integer.parseInt(rowMatcher.group(3)),
                    rowMatcher.group(4).charAt(0)));
        }

        String finfoMarker = "struct cn1_frame_info __cn1_finfo_" + identifier + " = {";
        int finfoAt = code.indexOf(finfoMarker);
        assertTrue(finfoAt >= 0, "no frame info for " + methodName + ":\n" + code);
        Matcher finfoMatcher = FINFO.matcher(code.substring(finfoAt - 20 < 0 ? 0 : finfoAt - 20));
        assertTrue(finfoMatcher.find(), "unparseable frame info for " + methodName + ":\n" + code);
        table.declaredRowCount = Integer.parseInt(finfoMatcher.group(4));

        String addrsMarker = "void* __cn1_local_addrs[";
        int addrsAt = code.indexOf(addrsMarker, varsEnd);
        assertTrue(addrsAt >= 0, "no locals address array for " + methodName + ":\n" + code);
        int open = code.indexOf('{', addrsAt);
        int close = code.indexOf('}', open);
        for (String entry : code.substring(open + 1, close).split(",(?![^\\[]*\\])")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                table.addresses.add(trimmed);
            }
        }
        return table;
    }

    /** The translator's per-method identifier, read back off the emitted symbol. */
    private String methodIdentifierAt(String code, int anchor) {
        Matcher m = Pattern.compile("__cn1_finfo_(\\w*" + "DbgHost_" + "\\w*)")
                .matcher(code.substring(anchor > 400 ? anchor - 400 : 0,
                        Math.min(code.length(), anchor + 400)));
        assertTrue(m.find(), "could not locate the frame-info symbol near the method:\n" + code);
        return m.group(1);
    }

    private List<String> typeCodesForSlot(FrameTable table, int slot) {
        List<String> out = new ArrayList<>();
        for (Row r : table.rows) {
            if (r.slot == slot) out.add(String.valueOf(r.typeCode));
        }
        Collections.sort(out);
        return out;
    }

    private Row rowFor(FrameTable table, int slot, char typeCode) {
        for (Row r : table.rows) {
            if (r.slot == slot && r.typeCode == typeCode) return r;
        }
        throw new AssertionError("no row for slot " + slot + " type '" + typeCode + "':\n" + table);
    }

    private String addressForRow(FrameTable table, int slot, char typeCode) {
        for (int i = 0; i < table.rows.size(); i++) {
            Row r = table.rows.get(i);
            if (r.slot == slot && r.typeCode == typeCode) return table.addresses.get(i);
        }
        return null;
    }

    /** The text of the first generated C function whose name contains {@code marker}. */
    private String cFunctionBody(String code, String marker) {
        int nameAt = code.indexOf(marker);
        assertTrue(nameAt >= 0, "generated code has no function matching " + marker + ":\n" + code);
        int start = code.indexOf('{', nameAt);
        int end = code.indexOf("\n}", start + 1);
        assertTrue(start >= 0 && end > start, "could not delimit the body of " + marker);
        return code.substring(start, end);
    }

    // ---- the class under translation ---------------------------------------

    private String translateHost() throws Exception {
        Parser.parse(writeHostClass().toFile());

        ByteCodeClass objectClass =
                new ByteCodeClass("java_lang_Object", "java/lang/Object");
        ByteCodeClass host = Parser.getClassObject("com_example_DbgHost");
        host.setBaseClassObject(objectClass);
        host.setBaseInterfacesObject(Collections.<ByteCodeClass>emptyList());
        host.updateAllDependencies();

        List<ByteCodeClass> classes = Arrays.asList(objectClass, host);
        return host.generateCCode(classes);
    }

    /**
     * {@code handler(Object)} is the shape of an event listener: slot 2 holds
     * an {@code int} for the first half of the method and a {@code String} for
     * the second, exactly as javac lays out
     *
     * <pre>
     *   void handler(Object ev) {
     *       { int count = 1; int copy = count; }
     *       { String label = null; Object held = label; }
     *   }
     * </pre>
     *
     * {@code ping()} is the contrast case: a frame with no locals at all that
     * still makes a call, so it is not eligible for the barebone form and does
     * push a frame.
     */
    private Path writeHostClass() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                HOST, null, "java/lang/Object", null);

        MethodVisitor probe = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "probe", "(I)V", null, null);
        probe.visitCode();
        probe.visitInsn(Opcodes.RETURN);
        probe.visitMaxs(1, 1);
        probe.visitEnd();

        MethodVisitor handler = cw.visitMethod(
                Opcodes.ACC_PUBLIC, "handler", "(Ljava/lang/Object;)V", null, null);
        Label start = new Label();
        Label midpoint = new Label();
        Label end = new Label();
        handler.visitCode();
        handler.visitLabel(start);
        handler.visitLineNumber(INT_SCOPE_LINE, start);
        handler.visitInsn(Opcodes.ICONST_1);
        handler.visitVarInsn(Opcodes.ISTORE, 2);        // int count
        handler.visitVarInsn(Opcodes.ILOAD, 2);
        handler.visitVarInsn(Opcodes.ISTORE, 3);        // int copy
        handler.visitLabel(midpoint);                   // count goes out of scope
        handler.visitLineNumber(REF_SCOPE_LINE, midpoint);
        handler.visitInsn(Opcodes.ACONST_NULL);
        handler.visitVarInsn(Opcodes.ASTORE, 2);        // String label — same slot
        handler.visitVarInsn(Opcodes.ALOAD, 2);
        handler.visitVarInsn(Opcodes.ASTORE, 4);        // Object held
        Label lastStatement = new Label();
        handler.visitLabel(lastStatement);
        handler.visitLineNumber(METHOD_END_LINE, lastStatement);
        handler.visitInsn(Opcodes.RETURN);
        // The scope-closing label goes after the last instruction with no line
        // number of its own, which is what javac emits.
        handler.visitLabel(end);
        handler.visitLocalVariable("this", "Lcom/example/DbgHost;", null, start, end, 0);
        handler.visitLocalVariable("ev", "Ljava/lang/Object;", null, start, end, 1);
        handler.visitLocalVariable("count", "I", null, start, midpoint, 2);
        handler.visitLocalVariable("copy", "I", null, start, end, 3);
        handler.visitLocalVariable("label", "Ljava/lang/String;", null, midpoint, end, 2);
        handler.visitLocalVariable("held", "Ljava/lang/Object;", null, midpoint, end, 4);
        handler.visitMaxs(2, 5);
        handler.visitEnd();

        MethodVisitor ping = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "ping", "()V", null, null);
        ping.visitCode();
        ping.visitInsn(Opcodes.ICONST_0);
        ping.visitMethodInsn(Opcodes.INVOKESTATIC, HOST, "probe", "(I)V", false);
        ping.visitInsn(Opcodes.RETURN);
        ping.visitMaxs(1, 0);
        ping.visitEnd();

        cw.visitEnd();
        return writeClassFile(HOST, cw);
    }

    private Path writeClassFile(String internalName, ClassWriter cw) throws Exception {
        Path dir = Files.createTempDirectory("cn1-dbg-frame-table");
        Path file = dir.resolve(internalName.substring(internalName.lastIndexOf('/') + 1) + ".class");
        Files.write(file, cw.toByteArray());
        return file;
    }
}
