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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every path that pushes a call frame must first clear that frame's debugger
 * side-channel.
 *
 * <p>{@code callStackFrameInfo} and {@code callStackLocalsAddresses} are
 * written only by methods that carry a locals side-table, and only after the
 * frame is pushed. Native, eliminated and barebone methods never write them.
 * Nothing clears them, so without {@code CN1_DEBUG_FRAME_ENTER} such a frame
 * inherits whatever the previous occupant of that call depth left — including
 * a locals-address array that points into a C frame which has already
 * returned. The debugger then reports the wrong method for the frame and reads
 * freed stack memory as object references, which is one of the ways a
 * breakpoint took the app down in issue #5333.</p>
 *
 * <p>The property is structural: it holds for the push sites that exist today,
 * and the thing worth defending is that a <em>new</em> one cannot be added
 * without the clear. Asserted over the runtime sources because the arrays only
 * exist in an on-device-debug build, which is not a configuration these tests
 * can compile and run.</p>
 */
class DebugFramePushCoverageTest {

    /** Runtime sources that own a call-frame push. */
    private static final String[] RUNTIME_SOURCES = {
        "cn1_globals.h",
        "cn1_globals.m",
        "nativeMethods.m",
    };

    private static final String PUSH = "threadStateData->callStackOffset++";
    private static final String CLEAR = "CN1_DEBUG_FRAME_ENTER(threadStateData)";

    @Test
    void everyFramePushClearsTheDebugSideChannelFirst() throws Exception {
        List<String> offenders = new ArrayList<>();
        int pushes = 0;

        for (String source : RUNTIME_SOURCES) {
            Path file = runtimeSource(source);
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.contains(PUSH) || isCommentedOut(lines, i)) {
                    continue;
                }
                pushes++;
                if (!precededByClear(lines, i)) {
                    offenders.add(source + ":" + (i + 1) + "  " + line.trim());
                }
            }
        }

        assertTrue(pushes > 0,
                "found no call-frame pushes at all — this test is looking in the wrong place");
        assertTrue(offenders.isEmpty(),
                "these frame pushes do not clear the debugger side-channel first, so the new"
                        + " frame inherits the previous occupant's locals-address array:\n  "
                        + String.join("\n  ", offenders));
    }

    /**
     * The clear is a no-op in release builds, so it must sit outside any
     * {@code CN1_ON_DEVICE_DEBUG} guard at the push site — otherwise a release
     * build and a debug build push frames differently for no reason.
     */
    @Test
    void theClearIsUnconditionalAtThePushSite() throws Exception {
        String globals = readAll(runtimeSource("cn1_globals.h"));

        assertTrue(globals.contains("#define CN1_DEBUG_FRAME_ENTER(threadStateData) \\"),
                "the debug build must define the clear");
        assertTrue(globals.contains("#define CN1_DEBUG_FRAME_ENTER(threadStateData)\n"),
                "the release build must define it as a no-op, so push sites need no #ifdef");
    }

    /**
     * Every debugger hook the translator emits calls has a weak no-op.
     *
     * <p>Generated code calls these from each class's constructor, and it is
     * compiled in targets that do not link the debugger runtime — the watchOS
     * slice does not get {@code cn1_debugger_objects.c} at all. Without a weak
     * default in {@code cn1_globals.m}, which every target does compile, those
     * targets fail to link. The compile failure that surfaced this named
     * {@code cn1_debugger_register_class} in the watch sources.</p>
     */
    @Test
    void everyGeneratedDebuggerHookHasAWeakDefault() throws Exception {
        String globalsHeader = readAll(runtimeSource("cn1_globals.h"));
        String globalsImpl = readAll(runtimeSource("cn1_globals.m"));

        for (String hook : new String[] {
                "cn1_debugger_check",
                "cn1_debugger_register_class",
                "cn1_debugger_mark_issued_roots" }) {
            assertTrue(globalsHeader.contains("extern void " + hook),
                    hook + " must be declared where generated code can see it,"
                            + " not only in the iOS port's own header");
            assertTrue(globalsImpl.contains("__attribute__((weak)) void " + hook),
                    hook + " must have a weak no-op so a target without the"
                            + " debugger runtime still links");
        }
    }

    /** The clear only makes sense before the increment it protects. */
    @Test
    void theClearTargetsTheFrameBeingPushedNotTheOneBelow() throws Exception {
        String globals = readAll(runtimeSource("cn1_globals.h"));
        int at = globals.indexOf("#define CN1_DEBUG_FRAME_ENTER(threadStateData) \\");
        assertTrue(at >= 0, "the clear macro should be defined in cn1_globals.h");
        String body = globals.substring(at, globals.indexOf("\n\n", at));

        assertTrue(body.contains("callStackFrameInfo[threadStateData->callStackOffset] = 0")
                        && body.contains("callStackLocalsAddresses[threadStateData->callStackOffset] = 0"),
                "both side-channel slots must be cleared, was:\n" + body);
        assertFalse(body.contains("callStackOffset - 1"),
                "the clear runs before the increment, so it must not index the caller's"
                        + " frame, was:\n" + body);
    }

    private static String readAll(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path runtimeSource(String name) {
        Path path = Paths.get("..", "ByteCodeTranslator", "src", name).normalize().toAbsolutePath();
        assertTrue(Files.exists(path), "expected runtime source at " + path);
        return path;
    }

    /**
     * The header keeps a commented-out {@code ENTERING_CODENAME_ONE_METHOD}
     * macro that also increments the offset. It emits no code, so it is not a
     * push site.
     */
    private static boolean isCommentedOut(List<String> lines, int index) {
        for (int i = index; i >= 0; i--) {
            String line = lines.get(i);
            if (line.contains("*/")) return false;
            if (line.contains("/*")) return true;
        }
        return false;
    }

    /**
     * Whether the clear appears in the few lines immediately above the push,
     * with nothing but the frame's own bookkeeping between them.
     */
    private static boolean precededByClear(List<String> lines, int index) {
        for (int i = index - 1; i >= 0 && i >= index - 4; i--) {
            String line = lines.get(i).trim();
            if (line.contains(CLEAR)) return true;
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (!line.contains("threadStateData->")) return false;
        }
        return false;
    }
}
