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
package com.codename1.builders;

import java.util.List;

/**
 * Carries the {@code nativeVerify} build hint into the forked translator JVM.
 *
 * <p>Every ParparVM translation checks that each {@code native} method has a C
 * implementation with the name and prototype the generated code will call, and
 * fails the build when one does not (see {@code NativeSignatureVerifier}). The
 * per-symbol escape hatch is {@code cn1-native-verify-ignore.txt} beside the
 * native sources, which is where a native provided by a prebuilt {@code .a} or
 * {@code .framework} belongs. This hint is the blunt one, for a build that has to
 * go out before the real fix: {@code nativeVerify=warn} reports and continues,
 * {@code off} skips the pass entirely.</p>
 *
 * <p>It has to travel as a {@code -D} on the forked JVM's command line: the
 * translator runs in its own process and inherits none of the builder's system
 * properties.</p>
 */
final class NativeVerifyOption {
    /** Build hint name, accepted unprefixed and per-platform. */
    static final String HINT = "nativeVerify";

    /**
     * Appends {@code -Dparparvm.nativeVerify=...} to a forked translator command
     * when the build asks for anything other than the strict default.
     *
     * @param jvmArgs the JVM argument list, before {@code -jar}
     * @param request the build request to read the hint from
     * @param platformPrefix e.g. {@code "ios"}, so {@code ios.nativeVerify} works
     *        alongside the unprefixed form
     */
    static void addTo(List<String> jvmArgs, BuildRequest request, String platformPrefix) {
        if (request == null) {
            return;
        }
        String value = request.getArg(platformPrefix + "." + HINT, null);
        if (value == null) {
            value = request.getArg(HINT, null);
        }
        if (value == null || value.trim().length() == 0 || "strict".equalsIgnoreCase(value.trim())) {
            return;
        }
        jvmArgs.add("-Dparparvm.nativeVerify=" + value.trim());
    }

    private NativeVerifyOption() {
    }
}
