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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The runtime reports a local only on the lines it is actually in scope for.
 *
 * <p>Locals used to be reported unconditionally, which for a slot two disjoint
 * scopes share means both occupants appear at every breakpoint — and the one
 * the code has not reached displays whatever the other scope left in its own
 * storage. The declaring scope is the only thing that tells them apart.</p>
 *
 * <p>Exercises the shipped predicate through {@link NativeDebuggerHarness}
 * rather than restating it. macOS only, like the rest of that unit.</p>
 */
@EnabledOnOs(OS.MAC)
class DebuggerLocalScopeTest {

    /** A local declared at line 10 in a block closing at 14 covers 10 to 13. */
    @Test
    void aScopedLocalIsLiveFromItsDeclarationUpToButNotIncludingTheClose() throws Exception {
        assertEquals("hidden", inScope(10, 14, 9));
        assertEquals("visible", inScope(10, 14, 10));
        assertEquals("visible", inScope(10, 14, 13));
        assertEquals("hidden", inScope(10, 14, 14));
        assertEquals("hidden", inScope(10, 14, 20));
    }

    /**
     * The case the whole thing is for: at any one line exactly one occupant of
     * a reused slot is reported.
     */
    @Test
    void exactlyOneOccupantOfAReusedSlotIsVisibleAtEachLine() throws Exception {
        // int count: lines 10-13. String label: line 14 to the end.
        assertEquals("visible", inScope(10, 14, 11));
        assertEquals("hidden", inScope(14, 0, 11));

        assertEquals("hidden", inScope(10, 14, 15));
        assertEquals("visible", inScope(14, 0, 15));
    }

    /** A zero end means the scope runs to the end of the method. */
    @Test
    void anOpenEndedScopeStaysLiveForTheRestOfTheMethod() throws Exception {
        assertEquals("hidden", inScope(14, 0, 13));
        assertEquals("visible", inScope(14, 0, 14));
        assertEquals("visible", inScope(14, 0, 9999));
    }

    /**
     * A local with no scope at all — no entry in the class file, or one the
     * translator synthesised from a store opcode — must stay visible. Hiding
     * it would be a regression on the previous behaviour for every such local.
     */
    @Test
    void anUnscopedLocalIsAlwaysVisible() throws Exception {
        assertEquals("visible", inScope(0, 0, 1));
        assertEquals("visible", inScope(0, 0, 9999));
    }

    /**
     * Before the frame has recorded a line there is nothing to compare
     * against, so everything shows rather than nothing — an over-full locals
     * view beats an empty one.
     */
    @Test
    void everythingIsVisibleWhenTheFrameHasNoLineYet() throws Exception {
        assertEquals("visible", inScope(10, 14, 0));
    }

    private String inScope(int startLine, int endLine, int atLine) throws Exception {
        return harness().verdict(String.valueOf(startLine),
                String.valueOf(endLine), String.valueOf(atLine));
    }

    private static NativeDebuggerHarness harness;

    private static synchronized NativeDebuggerHarness harness() throws Exception {
        if (harness == null) {
            harness = NativeDebuggerHarness.compile("scope", DRIVER);
        }
        return harness;
    }

    private static final String DRIVER =
            "#include \"cn1_globals.h\"\n" +
            "#include \"cn1_debugger.h\"\n" +
            "#include <stdio.h>\n" +
            "#include <stdlib.h>\n" +
            "\n" +
            "int main(int argc, char** argv) {\n" +
            "    if (argc < 4) {\n" +
            "        fprintf(stderr, \"usage: scope <startLine> <endLine> <atLine>\\n\");\n" +
            "        return 2;\n" +
            "    }\n" +
            "    struct cn1_var_entry v;\n" +
            "    v.startLine = atoi(argv[1]);\n" +
            "    v.endLine = atoi(argv[2]);\n" +
            "    v.slot = 2;\n" +
            "    v.typeCode = 'I';\n" +
            "    printf(\"%s\\n\", cn1_debugger_var_in_scope(&v, atoi(argv[3])) ? \"visible\" : \"hidden\");\n" +
            "    return 0;\n" +
            "}\n";
}
