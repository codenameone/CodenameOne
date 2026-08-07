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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The debugger must never be able to crash the app it is debugging.
 *
 * <p>Every object reference the on-device debugger handles is untrusted. The
 * IDE echoes back objectIDs it was handed earlier, and a local slot can hold
 * whatever the frame left there on a branch that never ran — a stale
 * reference, or the bytes of an unrelated primitive. Before
 * {@code cn1_debugger_class_of} existed, each of those was dereferenced
 * directly, and the process died mid-session with the {@code signal 11}
 * reported in issue #5333.</p>
 *
 * <p>These compile the real validation unit against the real {@code struct
 * clazz} — a one-line stub stands in for the generated class-id header — and
 * run it, so what is under test is the shipped policy rather than a
 * restatement of it. The unit is macOS-only: it rests on {@code
 * vm_read_overwrite} to probe an address without taking a signal, and iOS
 * debugging is a macOS activity anyway.</p>
 */
@EnabledOnOs(OS.MAC)
class DebuggerObjectValidationTest {

    @Test
    void aRegisteredObjectIsAccepted() throws Exception {
        assertEquals("accepted", probe("REGISTERED"));
    }

    /**
     * Array classes are synthesised per dimension and the primitive ones live
     * in the runtime, so none of them run the registration constructor. They
     * are still verifiable through the component type they name — and they had
     * better be, or the debugger would report every array as unavailable.
     */
    @Test
    void anArrayIsAcceptedThroughItsComponentType() throws Exception {
        assertEquals("accepted", probe("ARRAY"));
    }

    /** The exact-identity check: claiming a registered id is not enough. */
    @Test
    void aClazzThatIsNotTheRegisteredOneForItsIdIsRejected() throws Exception {
        assertEquals("rejected", probe("IMPOSTOR"));
    }

    /**
     * The shape that produced the crash: a slot holding a four-byte {@code
     * int}, read as an eight-byte reference. The resulting pointer is
     * arbitrary, and the old code dereferenced it to test for
     * {@code java.lang.String}.
     */
    @Test
    void anIntSlotReadAsAReferenceIsRejectedRatherThanDereferenced() throws Exception {
        assertEquals("rejected", probe("INT_AS_REFERENCE"));
    }

    /** An address in no mapping at all must come back as a rejection, not a signal. */
    @Test
    void anUnmappedAddressIsRejectedWithoutFaulting() throws Exception {
        assertEquals("rejected", probe("WILD_POINTER"));
    }

    /** Misaligned and null-page candidates are rejected before the syscall. */
    @Test
    void obviouslyImpossiblePointersAreRejected() throws Exception {
        assertEquals("rejected", probe("MISALIGNED"));
        assertEquals("rejected", probe("NULL_PAGE"));
        assertEquals("rejected", probe("NULL"));
    }

    /** A zeroed object has no class word to check. */
    @Test
    void anObjectWithNoClassWordIsRejected() throws Exception {
        assertEquals("rejected", probe("NO_CLASS_WORD"));
    }

    /**
     * The whole point is that none of the above take the process down: a
     * rejection is a return value, not a crash.
     */
    @Test
    void noCandidateEverCrashesTheProcess() throws Exception {
        List<String> cases = Arrays.asList("REGISTERED", "ARRAY", "IMPOSTOR",
                "INT_AS_REFERENCE", "WILD_POINTER", "MISALIGNED", "NULL_PAGE",
                "NULL", "NO_CLASS_WORD");
        for (String candidate : cases) {
            NativeDebuggerHarness.Result result = harness().run(candidate);
            assertEquals(0, result.exitCode,
                    "validating " + candidate + " should return, not die: " + result.output);
        }
    }

    private static NativeDebuggerHarness harness;

    private static synchronized NativeDebuggerHarness harness() throws Exception {
        if (harness == null) {
            harness = NativeDebuggerHarness.compile("validate", DRIVER);
        }
        return harness;
    }

    private String probe(String candidate) throws Exception {
        return harness().verdict(candidate);
    }

    /**
     * Builds one candidate reference per invocation and prints "accepted" or
     * "rejected". Kept to a single candidate per process so that a candidate
     * which does crash is attributable, and shows up as a non-zero exit rather
     * than as a missing line of output.
     */
    private static final String DRIVER =
            "#include \"cn1_globals.h\"\n" +
            "#include \"cn1_debugger.h\"\n" +
            "#include <stdio.h>\n" +
            "#include <string.h>\n" +
            "\n" +
            "static struct clazz registeredClass;\n" +
            "static struct clazz arrayClass;\n" +
            "static struct clazz unregisteredClass;\n" +
            "\n" +
            "int main(int argc, char** argv) {\n" +
            "    if (argc < 2) { fprintf(stderr, \"usage: validate <candidate>\\n\"); return 2; }\n" +
            "    const char* which = argv[1];\n" +
            "\n" +
            "    registeredClass.classId = 5;\n" +
            "    cn1_debugger_register_class(5, &registeredClass);\n" +
            "\n" +
            "    /* An array clazz is never registered; it points at its component. */\n" +
            "    JAVA_BOOLEAN yes = 1;\n" +
            "    memcpy((void*)&arrayClass.isArray, &yes, sizeof(JAVA_BOOLEAN));\n" +
            "    arrayClass.classId = cn1_array_1_id_JAVA_INT;\n" +
            "    arrayClass.arrayType = &registeredClass;\n" +
            "\n" +
            "    /* Claims a registered id, but is not the clazz registered for it. */\n" +
            "    unregisteredClass.classId = 5;\n" +
            "\n" +
            "    struct JavaObjectPrototype object;\n" +
            "    memset(&object, 0, sizeof(object));\n" +
            "    JAVA_OBJECT candidate = JAVA_NULL;\n" +
            "\n" +
            "    if (strcmp(which, \"REGISTERED\") == 0) {\n" +
            "        object.__codenameOneParentClsReference = &registeredClass;\n" +
            "        candidate = &object;\n" +
            "    } else if (strcmp(which, \"ARRAY\") == 0) {\n" +
            "        object.__codenameOneParentClsReference = &arrayClass;\n" +
            "        candidate = &object;\n" +
            "    } else if (strcmp(which, \"IMPOSTOR\") == 0) {\n" +
            "        object.__codenameOneParentClsReference = &unregisteredClass;\n" +
            "        candidate = &object;\n" +
            "    } else if (strcmp(which, \"NO_CLASS_WORD\") == 0) {\n" +
            "        candidate = &object;\n" +
            "    } else if (strcmp(which, \"INT_AS_REFERENCE\") == 0) {\n" +
            "        /* The issue #5333 shape: eight bytes read over a four-byte local. */\n" +
            "        volatile JAVA_INT slot = 42;\n" +
            "        candidate = *(JAVA_OBJECT*)(void*)&slot;\n" +
            "    } else if (strcmp(which, \"WILD_POINTER\") == 0) {\n" +
            "        candidate = (JAVA_OBJECT)(uintptr_t)0xDEADBEEFDEAD0000ULL;\n" +
            "    } else if (strcmp(which, \"MISALIGNED\") == 0) {\n" +
            "        candidate = (JAVA_OBJECT)(((uintptr_t)&object) | 1);\n" +
            "    } else if (strcmp(which, \"NULL_PAGE\") == 0) {\n" +
            "        candidate = (JAVA_OBJECT)(uintptr_t)0x18;\n" +
            "    } else if (strcmp(which, \"NULL\") == 0) {\n" +
            "        candidate = JAVA_NULL;\n" +
            "    } else {\n" +
            "        fprintf(stderr, \"unknown candidate %s\\n\", which);\n" +
            "        return 2;\n" +
            "    }\n" +
            "\n" +
            "    printf(\"%s\\n\", cn1_debugger_is_valid_object(candidate) ? \"accepted\" : \"rejected\");\n" +
            "    return 0;\n" +
            "}\n";
}
