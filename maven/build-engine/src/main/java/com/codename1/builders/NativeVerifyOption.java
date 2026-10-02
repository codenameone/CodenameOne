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
 * <p>A ParparVM translation can check that each {@code native} method has a C
 * implementation with the name and prototype the generated code will call (see
 * {@code NativeSignatureVerifier}). It is <b>off unless asked</b>, because making
 * that failure hard changes the outcome of app builds that succeed today. This
 * hint turns it on for one build: {@code nativeVerify=strict} fails on a bad
 * native, {@code warn} reports and continues, {@code off} (the default) skips the
 * pass entirely.</p>
 *
 * <p>The hint has to travel as a {@code -D} on the forked JVM's command line: the
 * translator runs in its own process and inherits none of the builder's system
 * properties. Codename One's own CI does not use the hint at all -- it sets the
 * {@code CN1_NATIVE_VERIFY} environment variable once for the job, which every
 * forked process inherits for free.</p>
 */
final class NativeVerifyOption {
    /** Build hint name, accepted unprefixed and per-platform. */
    static final String HINT = "nativeVerify";

    /**
     * Appends {@code -Dparparvm.nativeVerify=...} to a forked translator command
     * when the build sets the hint. An unset hint adds nothing, leaving the
     * translator on its default (and leaving {@code CN1_NATIVE_VERIFY} to speak
     * for CI, since a {@code -D} would override it).
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
        if (value == null || value.trim().length() == 0) {
            return;
        }
        jvmArgs.add("-Dparparvm.nativeVerify=" + value.trim());
    }

    private NativeVerifyOption() {
    }
}
