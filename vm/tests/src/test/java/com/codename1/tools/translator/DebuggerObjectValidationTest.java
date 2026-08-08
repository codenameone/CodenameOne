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

    /**
     * Every outstanding id is a GC root, so its object cannot be collected.
     *
     * <p>Membership in the issued set only decides which ids are <em>accepted</em>.
     * Whether the object behind an accepted id is still allocated is a separate
     * question, and a class word survives reclamation — so without rooting, an
     * accepted id could still name freed memory and the IDE's next field read
     * would touch it. Marking the set from the collector's root pass is what
     * turns the membership check into a guarantee.</p>
     */
    @Test
    void everyOutstandingIdIsMarkedAsAGcRoot() throws Exception {
        assertEquals("marked", probe("ROOTS_MARK_ISSUED"));
    }

    /** Including references the table only holds after it has grown. */
    @Test
    void rootsSurviveTableGrowth() throws Exception {
        assertEquals("marked", probe("ROOTS_MARK_AFTER_GROWTH"));
    }

    /** A released id stops being a root, so the object can be collected again. */
    @Test
    void aReleasedIdIsNoLongerARoot() throws Exception {
        assertEquals("not-marked", probe("ROOTS_RELEASED_NOT_MARKED"));
    }

    /** A tagged int is a value, not an allocation, so it is never marked. */
    @Test
    void taggedIntsAreNotMarkedAsRoots() throws Exception {
        assertEquals("not-marked", probe("ROOTS_TAGGED_NOT_MARKED"));
    }

    /**
     * A reference reached through a shared object inherits every owner.
     *
     * <p>Taking only the parent's first owner meant resuming that thread
     * deleted the nested ids while a second thread was still parked and able
     * to reach the same tree, so an object the IDE had already expanded went
     * unavailable underneath it.</p>
     */
    @Test
    void derivedReferencesInheritEveryOwnerOfTheirParent() throws Exception {
        assertEquals("kept", probe("DERIVED_INHERITS_ALL_OWNERS"));
    }

    /**
     * An id claimed both by a suspension and by the thread list survives that
     * suspension ending.
     *
     * <p>A java.lang.Thread object can be exposed first through a stopped
     * thread's locals and later by the thread list, which is tied to no
     * suspension. Recording only the first claim meant resuming that thread
     * rejected an id the live thread list was still advertising.</p>
     */
    @Test
    void anUnownedClaimSurvivesThreadInvalidation() throws Exception {
        assertEquals("kept", probe("UNOWNED_CLAIM_SURVIVES"));
    }

    /**
     * A thread-list refresh releases the objects the previous one advertised.
     *
     * <p>Every {@code java.lang.Thread} the list returns is rooted so its id
     * stays usable. Released only on a full resume, an IDE polling the thread
     * list on a running app would pin every thread object it ever saw — and
     * whatever those threads retain — for the session.</p>
     */
    @Test
    void aThreadListRefreshReleasesTheObjectsTheLastOneAdvertised() throws Exception {
        assertEquals("gone", probe("THREAD_LIST_REFRESH_RELEASES"));
    }

    /** But not one a suspended thread is also holding. */
    @Test
    void aRefreshKeepsObjectsASuspensionStillOwns() throws Exception {
        assertEquals("kept", probe("THREAD_LIST_REFRESH_KEEPS_OWNED"));
    }

    /**
     * Nor anything reached through a Thread object the refresh re-advertises.
     *
     * <p>A refresh re-issues only the top-level {@code Thread} objects, so a
     * field the IDE had expanded off one carries no claim of its own. Clearing
     * the old claims before recording the new ones dropped every such id, and
     * the object stayed reachable from a live thread the whole time -- every
     * later string, field or array request naming it was refused for no reason
     * the developer could see.</p>
     */
    @Test
    void aRefreshKeepsWhatHangsOffAThreadItStillAdvertises() throws Exception {
        assertEquals("kept", probe("REFRESH_DESCENDANT_KEPT"));
    }

    /**
     * An object reached through two threads survives while either still is.
     *
     * <p>Recording only the most recent parent meant the second expansion
     * overwrote the first, so when that thread went away the id went with it —
     * while the IDE was still showing the same id under the thread that
     * remained.</p>
     */
    @Test
    void aDescendantSharedByTwoThreadsSurvivesWhileEitherDoes() throws Exception {
        assertEquals("kept", probe("REFRESH_SHARED_DESCENDANT"));
    }

    /** Reached transitively: a field of a field is no less reachable. */
    @Test
    void aRefreshKeepsDescendantsSeveralHopsDown() throws Exception {
        assertEquals("kept", probe("REFRESH_DESCENDANT_DEEP"));
    }

    /**
     * Once the thread itself stops being advertised, what hung off it goes
     * too -- otherwise the fix would just be a leak by another name.
     */
    @Test
    void aRefreshReleasesDescendantsOfAThreadThatIsGone() throws Exception {
        assertEquals("gone", probe("REFRESH_DESCENDANT_THREAD_GONE"));
    }

    /**
     * A resumed thread stops owning an entry even when the thread list also
     * claims it.
     *
     * <p>Deciding survival before removing the owner left the resumed thread
     * in the owner set, so the next thread-list refresh kept the entry alive
     * on the strength of an owner that had long since resumed — and once that
     * thread died, the object and everything it reached stayed rooted for the
     * session.</p>
     */
    @Test
    void aResumedOwnerIsRemovedEvenFromAnEntryTheThreadListClaims() throws Exception {
        assertEquals("gone", probe("STALE_OWNER_IS_REMOVED"));
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
                "SHARED_SURVIVES_FIRST_RESUME", "SHARED_DROPPED_BY_LAST_RESUME",
                "ROOTS_MARK_ISSUED", "ROOTS_MARK_AFTER_GROWTH",
                "ROOTS_RELEASED_NOT_MARKED", "ROOTS_TAGGED_NOT_MARKED",
                "DERIVED_INHERITS_ALL_OWNERS", "UNOWNED_CLAIM_SURVIVES",
                "THREAD_LIST_REFRESH_RELEASES", "THREAD_LIST_REFRESH_KEEPS_OWNED",
                "STALE_OWNER_IS_REMOVED");
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
            "    } else if (strncmp(which, \"ROOTS_\", 6) == 0) {\n" +
            "        extern JAVA_OBJECT cn1MarkRootTarget;\n" +
            "        extern int cn1MarkRootTargetSeen;\n" +
            "        extern int cn1MarkedRootCount;\n" +
            "        JAVA_OBJECT tracked = (JAVA_OBJECT)(uintptr_t)0xD000;\n" +
            "        if (strcmp(which, \"ROOTS_TAGGED_NOT_MARKED\") == 0) {\n" +
            "            tracked = CN1_TAG_INT(42);\n" +
            "            cn1_debugger_note_issued_for(tracked, 7);\n" +
            "        } else if (strcmp(which, \"ROOTS_MARK_AFTER_GROWTH\") == 0) {\n" +
            "            /* Force the table past its initial capacity first. */\n" +
            "            for (int i = 1; i <= 9000; i++) {\n" +
            "                cn1_debugger_note_issued_for((JAVA_OBJECT)(uintptr_t)(i * 8), 7);\n" +
            "            }\n" +
            "            cn1_debugger_note_issued_for(tracked, 7);\n" +
            "        } else {\n" +
            "            cn1_debugger_note_issued_for(tracked, 7);\n" +
            "            if (strcmp(which, \"ROOTS_RELEASED_NOT_MARKED\") == 0) {\n" +
            "                cn1_debugger_forget_issued_for(7);   /* thread resumes */\n" +
            "            }\n" +
            "        }\n" +
            "        cn1MarkRootTarget = tracked;\n" +
            "        cn1MarkRootTargetSeen = 0;\n" +
            "        cn1MarkedRootCount = 0;\n" +
            "        cn1_debugger_mark_issued_roots(NULL);\n" +
            "        printf(\"%s\\n\", cn1MarkRootTargetSeen ? \"marked\" : \"not-marked\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"STALE_OWNER_IS_REMOVED\") == 0) {\n" +
            "        /* Claimed by a suspension and by the thread list; the\n" +
            "         * thread resumes, then a later refresh drops the list\n" +
            "         * claim. Nothing holds it after that. */\n" +
            "        JAVA_OBJECT both = (JAVA_OBJECT)(uintptr_t)0x12000;\n" +
            "        cn1_debugger_note_issued_for(both, 7);\n" +
            "        cn1_debugger_note_issued(both);\n" +
            "        cn1_debugger_forget_issued_for(7);        /* thread resumes */\n" +
            "        cn1_debugger_begin_thread_list();         /* later refresh */\n" +
            "        cn1_debugger_end_thread_list();\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(both) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strncmp(which, \"THREAD_LIST_REFRESH_\", 20) == 0) {\n" +
            "        /* A thread object the list advertised, and one a stopped\n" +
            "         * thread also holds; then the next refresh comes round. */\n" +
            "        JAVA_OBJECT dead = (JAVA_OBJECT)(uintptr_t)0x11000;\n" +
            "        JAVA_OBJECT alsoOwned = (JAVA_OBJECT)(uintptr_t)0x11008;\n" +
            "        cn1_debugger_note_issued(dead);\n" +
            "        cn1_debugger_note_issued_for(alsoOwned, 7);\n" +
            "        cn1_debugger_note_issued(alsoOwned);\n" +
            "        cn1_debugger_begin_thread_list();\n" +
            "        cn1_debugger_end_thread_list();\n" +
            "        JAVA_OBJECT probe =\n" +
            "            strcmp(which, \"THREAD_LIST_REFRESH_KEEPS_OWNED\") == 0 ? alsoOwned : dead;\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(probe) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strncmp(which, \"REFRESH_DESCENDANT_\", 19) == 0) {\n" +
            "        /* The IDE expands a field of a live Thread object, then the\n" +
            "         * thread list refreshes and re-advertises that same Thread. */\n" +
            "        JAVA_OBJECT threadObj = (JAVA_OBJECT)(uintptr_t)0x13000;\n" +
            "        JAVA_OBJECT child = (JAVA_OBJECT)(uintptr_t)0x13008;\n" +
            "        JAVA_OBJECT grandchild = (JAVA_OBJECT)(uintptr_t)0x13010;\n" +
            "        cn1_debugger_begin_thread_list();\n" +
            "        cn1_debugger_note_issued(threadObj);\n" +
            "        cn1_debugger_end_thread_list();\n" +
            "        cn1_debugger_note_issued_inheriting(child, threadObj);\n" +
            "        cn1_debugger_note_issued_inheriting(grandchild, child);\n" +
            "        cn1_debugger_begin_thread_list();\n" +
            "        if (strcmp(which, \"REFRESH_DESCENDANT_THREAD_GONE\") != 0) {\n" +
            "            cn1_debugger_note_issued(threadObj);   /* still alive */\n" +
            "        }\n" +
            "        cn1_debugger_end_thread_list();\n" +
            "        JAVA_OBJECT probe =\n" +
            "            strcmp(which, \"REFRESH_DESCENDANT_DEEP\") == 0 ? grandchild : child;\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(probe) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"REFRESH_SHARED_DESCENDANT\") == 0) {\n" +
            "        /* One object reached through two threads' graphs; only\n" +
            "         * one of those threads is still advertised afterwards. */\n" +
            "        JAVA_OBJECT threadA = (JAVA_OBJECT)(uintptr_t)0x14000;\n" +
            "        JAVA_OBJECT threadB = (JAVA_OBJECT)(uintptr_t)0x14008;\n" +
            "        JAVA_OBJECT shared = (JAVA_OBJECT)(uintptr_t)0x14010;\n" +
            "        cn1_debugger_begin_thread_list();\n" +
            "        cn1_debugger_note_issued(threadA);\n" +
            "        cn1_debugger_note_issued(threadB);\n" +
            "        cn1_debugger_end_thread_list();\n" +
            "        cn1_debugger_note_issued_inheriting(shared, threadA);\n" +
            "        cn1_debugger_note_issued_inheriting(shared, threadB);\n" +
            "        cn1_debugger_begin_thread_list();\n" +
            "        cn1_debugger_note_issued(threadA);   /* B is gone */\n" +
            "        cn1_debugger_end_thread_list();\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(shared) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"DERIVED_INHERITS_ALL_OWNERS\") == 0) {\n" +
            "        /* A parent both parked threads expose, and a child reached\n" +
            "         * through it; thread 7 then resumes. */\n" +
            "        JAVA_OBJECT parent = (JAVA_OBJECT)(uintptr_t)0xE000;\n" +
            "        JAVA_OBJECT child  = (JAVA_OBJECT)(uintptr_t)0xE008;\n" +
            "        cn1_debugger_note_issued_for(parent, 7);\n" +
            "        cn1_debugger_note_issued_for(parent, 9);\n" +
            "        cn1_debugger_note_issued_inheriting(child, parent);\n" +
            "        cn1_debugger_forget_issued_for(7);\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(child) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
            "    } else if (strcmp(which, \"UNOWNED_CLAIM_SURVIVES\") == 0) {\n" +
            "        /* Exposed through a stopped thread, then by the thread list. */\n" +
            "        JAVA_OBJECT threadObj = (JAVA_OBJECT)(uintptr_t)0xF000;\n" +
            "        cn1_debugger_note_issued_for(threadObj, 7);\n" +
            "        cn1_debugger_note_issued_for(threadObj, 0);\n" +
            "        cn1_debugger_forget_issued_for(7);\n" +
            "        printf(\"%s\\n\", cn1_debugger_was_issued(threadObj) ? \"kept\" : \"gone\");\n" +
            "        return 0;\n" +
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
