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

    /**
     * Misaligned and null-page candidates are rejected before the syscall.
     *
     * <p>Misalignment here means bit 1, not bit 0: an odd reference is a
     * tagged int by design, so it is a value rather than a bad pointer.</p>
     */
    @Test
    void obviouslyImpossiblePointersAreRejected() throws Exception {
        assertEquals("rejected", probe("MISALIGNED"));
        assertEquals("rejected", probe("NULL_PAGE"));
        assertEquals("rejected", probe("NULL"));
    }

    /**
     * A boxed {@code Integer} is a tagged value, not an address.
     *
     * <p>{@code Integer.valueOf()} returns {@code (v << 1) | 1} on every
     * 64-bit target, which is the shipping iOS shape. Those references are
     * deliberately odd, so the alignment rejection that guards the header read
     * would discard every one of them — boxed integers in locals, fields,
     * object arrays, invocation arguments and receivers would all read as
     * null. They are recognised before any read is attempted.</p>
     */
    @Test
    void aTaggedIntegerIsAcceptedWithoutBeingDereferenced() throws Exception {
        assertEquals("accepted", probe("TAGGED_INT"));
    }

    /** And it reports Integer's class, so the IDE can name and expand it. */
    @Test
    void aTaggedIntegerReportsIntegersClass() throws Exception {
        assertEquals("java.lang.Integer", probe("TAGGED_INT_CLASS"));
    }

    /** Its value comes from the tag, since there is no field to read. */
    @Test
    void aTaggedIntegerCarriesItsValueInTheReference() throws Exception {
        assertEquals("42", probe("TAGGED_INT_VALUE"));
        assertEquals("-7", probe("TAGGED_INT_NEGATIVE"));
    }

    /**
     * An objectID the IDE holds across a resume is refused, not read.
     *
     * <p>A registered class word proves shape, not liveness: full-page BiBOP
     * reclamation resets the page cursor without clearing class words, and a
     * legacy object reaches {@code free()} with its header intact. So a
     * reclaimed allocation still passes the class check. Requiring that a wire
     * id be one the debugger issued since the last resume refuses the stale
     * one instead of reading through it.</p>
     */
    @Test
    void aWireIdFromAnEarlierSuspensionIsRefused() throws Exception {
        assertEquals("accepted", probe("WIRE_ID_ISSUED"));
        assertEquals("rejected", probe("WIRE_ID_AFTER_RESUME"));
    }

    /** An id never handed out at all is refused, however well-formed. */
    @Test
    void aWireIdTheDebuggerNeverIssuedIsRefused() throws Exception {
        assertEquals("rejected", probe("WIRE_ID_NEVER_ISSUED"));
    }

    /**
     * Tagged ints are exempt: they carry their value in the reference, so
     * there is no allocation that could have been reclaimed.
     */
    @Test
    void aTaggedIntNeedsNoIssueRecord() throws Exception {
        assertEquals("accepted", probe("WIRE_ID_TAGGED"));
    }

    /**
     * The issued-id table grows rather than silently dropping records.
     *
     * <p>A fixed table stopped recording once full, but callers still sent
     * those references to the IDE — so past the cap every displayed reference
     * became one the IDE could see and never expand. Inspecting a large object
     * array reaches that in a single suspension.</p>
     */
    @Test
    void theIssuedTableGrowsBeyondItsInitialCapacity() throws Exception {
        assertEquals("all-recorded", probe("ISSUE_MANY"));
    }

    /** And every one of them still resolves afterwards. */
    @Test
    void everyIssuedIdRemainsResolvableAfterGrowth() throws Exception {
        assertEquals("all-resolvable", probe("ISSUE_MANY_RESOLVE"));
    }

    /**
     * Resuming one thread invalidates its ids and only its ids.
     *
     * <p>Both directions are failures. Clearing globally cut the ground from
     * under a thread that was still stopped and being inspected, so its
     * locals started reporting unavailable; clearing nothing left the resumed
     * thread's ids accepted after its objects could be collected, which is a
     * read of reclaimed storage rather than a display glitch.</p>
     */
    @Test
    void aPerThreadResumeDropsOnlyThatThreadsIds() throws Exception {
        assertEquals("gone", probe("OWNER_RESUMED_IS_DROPPED"));
        assertEquals("kept", probe("OWNER_PARKED_IS_KEPT"));
    }

    /** A reference reached through an object inherits that object's owner. */
    @Test
    void referencesReachedThroughAnObjectInheritItsOwner() throws Exception {
        assertEquals("gone", probe("OWNER_INHERITED_IS_DROPPED"));
    }

    /** Ids tied to no suspension survive a per-thread resume, but not a full one. */
    @Test
    void unownedIdsSurviveAPerThreadResumeOnly() throws Exception {
        assertEquals("kept", probe("OWNER_UNOWNED_SURVIVES_THREAD_RESUME"));
        assertEquals("gone", probe("OWNER_UNOWNED_DROPPED_BY_FULL_RESUME"));
    }

    /**
     * A reference two parked threads both expose survives one of them
     * resuming.
     *
     * <p>Storing a single owner per reference meant the second thread to
     * expose a shared object — a singleton in both their locals, say — had its
     * claim discarded, so resuming the first dropped the id while the second
     * was still parked and inspecting through it.</p>
     */
    @Test
    void aReferenceSharedByTwoThreadsOutlivesTheFirstResume() throws Exception {
        assertEquals("kept", probe("SHARED_SURVIVES_FIRST_RESUME"));
        assertEquals("gone", probe("SHARED_DROPPED_BY_LAST_RESUME"));
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
                "NULL", "NO_CLASS_WORD", "TAGGED_INT", "TAGGED_INT_CLASS",
                "TAGGED_INT_VALUE", "WIRE_ID_ISSUED", "WIRE_ID_AFTER_RESUME",
                "WIRE_ID_NEVER_ISSUED", "WIRE_ID_TAGGED", "ISSUE_MANY",
                "ISSUE_MANY_RESOLVE", "OWNER_RESUMED_IS_DROPPED",
                "OWNER_PARKED_IS_KEPT", "OWNER_INHERITED_IS_DROPPED",
                "OWNER_UNOWNED_SURVIVES_THREAD_RESUME",
                "OWNER_UNOWNED_DROPPED_BY_FULL_RESUME",
                "SHARED_SURVIVES_FIRST_RESUME", "SHARED_DROPPED_BY_LAST_RESUME");
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
            "    } else if (strncmp(which, \"SHARED_\", 7) == 0) {\n" +
            "        /* One reference exposed by both parked threads. */\n" +
            "        JAVA_OBJECT shared = (JAVA_OBJECT)(uintptr_t)0xC000;\n" +
            "        cn1_debugger_note_issued_for(shared, 7);\n" +
            "        cn1_debugger_note_issued_for(shared, 9);\n" +
            "        cn1_debugger_forget_issued_for(7);\n" +
            "        if (strcmp(which, \"SHARED_DROPPED_BY_LAST_RESUME\") == 0) {\n" +
            "            cn1_debugger_forget_issued_for(9);\n" +
            "        }\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(shared) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strncmp(which, \"OWNER_\", 6) == 0) {\n" +
            "        /* Two parked threads, 7 and 9, each with a reference. */\n" +
            "        JAVA_OBJECT ofSeven = (JAVA_OBJECT)(uintptr_t)0x8000;\n" +
            "        JAVA_OBJECT ofNine  = (JAVA_OBJECT)(uintptr_t)0x9000;\n" +
            "        JAVA_OBJECT reached = (JAVA_OBJECT)(uintptr_t)0xA000;\n" +
            "        JAVA_OBJECT unowned = (JAVA_OBJECT)(uintptr_t)0xB000;\n" +
            "        cn1_debugger_note_issued_for(ofSeven, 7);\n" +
            "        cn1_debugger_note_issued_for(ofNine, 9);\n" +
            "        cn1_debugger_note_issued_for(reached, cn1_debugger_owner_of(ofSeven));\n" +
            "        cn1_debugger_note_issued_for(unowned, 0);\n" +
            "        if (strcmp(which, \"OWNER_UNOWNED_DROPPED_BY_FULL_RESUME\") == 0) {\n" +
            "            cn1_debugger_forget_issued();\n" +
            "            printf(\"%s\\n\", cn1_debugger_was_issued(unowned) ? \"kept\" : \"gone\");\n" +
            "            return 0;\n" +
            "        }\n" +
            "        cn1_debugger_forget_issued_for(7);   /* thread 7 resumes */\n" +
            "        JAVA_OBJECT probe = NULL;\n" +
            "        if (strcmp(which, \"OWNER_RESUMED_IS_DROPPED\") == 0) probe = ofSeven;\n" +
            "        else if (strcmp(which, \"OWNER_PARKED_IS_KEPT\") == 0) probe = ofNine;\n" +
            "        else if (strcmp(which, \"OWNER_INHERITED_IS_DROPPED\") == 0) probe = reached;\n" +
            "        else probe = unowned;\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(probe) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"ISSUE_MANY\") == 0) {\n" +
            "        /* Past the initial 4096 capacity, as a big array would. */\n" +
            "        for (int i = 1; i <= 9000; i++) {\n" +
            "            if (!cn1_debugger_note_issued((JAVA_OBJECT)(uintptr_t)(i * 8))) {\n" +
            "                printf(\"dropped-at-%d\\n\", i);\n" +
            "                return 0;\n" +
            "            }\n" +
            "        }\n" +
            "        printf(\"all-recorded\\n\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"ISSUE_MANY_RESOLVE\") == 0) {\n" +
            "        for (int i = 1; i <= 9000; i++) {\n" +
            "            cn1_debugger_note_issued((JAVA_OBJECT)(uintptr_t)(i * 8));\n" +
            "        }\n" +
            "        for (int i = 1; i <= 9000; i++) {\n" +
            "            if (!cn1_debugger_was_issued((JAVA_OBJECT)(uintptr_t)(i * 8))) {\n" +
            "                printf(\"lost-%d\\n\", i);\n" +
            "                return 0;\n" +
            "            }\n" +
            "        }\n" +
            "        printf(\"all-resolvable\\n\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"WIRE_ID_ISSUED\") == 0) {\n" +
            "        object.__codenameOneParentClsReference = &registeredClass;\n" +
            "        cn1_debugger_note_issued(&object);\n" +
            "        printf(\"%s\\n\", cn1_debugger_class_of_wire_id(&object)\n" +
            "                ? \"accepted\" : \"rejected\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"WIRE_ID_AFTER_RESUME\") == 0) {\n" +
            "        object.__codenameOneParentClsReference = &registeredClass;\n" +
            "        cn1_debugger_note_issued(&object);\n" +
            "        cn1_debugger_forget_issued();   /* the app ran on */\n" +
            "        printf(\"%s\\n\", cn1_debugger_class_of_wire_id(&object)\n" +
            "                ? \"accepted\" : \"rejected\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"WIRE_ID_NEVER_ISSUED\") == 0) {\n" +
            "        object.__codenameOneParentClsReference = &registeredClass;\n" +
            "        printf(\"%s\\n\", cn1_debugger_class_of_wire_id(&object)\n" +
            "                ? \"accepted\" : \"rejected\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"WIRE_ID_TAGGED\") == 0) {\n" +
            "        printf(\"%s\\n\", cn1_debugger_class_of_wire_id(CN1_TAG_INT(42))\n" +
            "                ? \"accepted\" : \"rejected\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"TAGGED_INT\") == 0) {\n" +
            "        candidate = CN1_TAG_INT(42);\n" +
            "    } else if (strcmp(which, \"TAGGED_INT_CLASS\") == 0) {\n" +
            "        struct clazz* c = cn1_debugger_class_of(CN1_TAG_INT(42));\n" +
            "        printf(\"%s\\n\", c == &class__java_lang_Integer\n" +
            "                ? \"java.lang.Integer\" : \"other\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"TAGGED_INT_VALUE\") == 0) {\n" +
            "        printf(\"%d\\n\", cn1_debugger_tagged_int_value(CN1_TAG_INT(42)));\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"TAGGED_INT_NEGATIVE\") == 0) {\n" +
            "        printf(\"%d\\n\", cn1_debugger_tagged_int_value(CN1_TAG_INT(-7)));\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"MISALIGNED\") == 0) {\n" +
            "        candidate = (JAVA_OBJECT)(((uintptr_t)&object) | 2);\n" +
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
