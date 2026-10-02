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
package com.codename1.maven;

import java.util.Arrays;
import java.util.List;

/**
 * Client-side hardening pre-flight (Check 1 of three). Runs before the build is
 * dispatched and catches the cases the server can never see: a local or
 * source-project target that cannot be hardened, an on-device-debug build that
 * must not be hardened, and an invalid {@code harden.level}. It fails loudly rather
 * than let a build silently ship unhardened when the developer asked for hardening.
 *
 * <p>Pure and side-effect free so it is trivially unit-testable; the mojo feeds it
 * the resolved hint values and acts on the {@link Result}.
 */
public final class HardeningPreflight {

    private static final List<String> LEVELS = Arrays.asList("off", "standard", "aggressive", "paranoid");

    /** The pre-flight decision. */
    public static final class Result {
        private final boolean failed;
        private final boolean forceOff;
        private final String message;

        private Result(boolean failed, boolean forceOff, String message) {
            this.failed = failed;
            this.forceOff = forceOff;
            this.message = message;
        }

        /** True when the build must be stopped. {@link #getMessage()} explains why. */
        public boolean isFailed() {
            return failed;
        }

        /** True when the build may proceed but hardening must be forced off (a warning applies). */
        public boolean isForceOff() {
            return forceOff;
        }

        /** The failure or warning message, or {@code null} when there is nothing to say. */
        public String getMessage() {
            return message;
        }

        static Result ok() {
            return new Result(false, false, null);
        }

        static Result fail(String m) {
            return new Result(true, false, m);
        }

        static Result forceOff(String m) {
            return new Result(false, true, m);
        }
    }

    private HardeningPreflight() {
    }

    /**
     * @param level                    the {@code harden.level} value (may be null / "off")
     * @param buildTarget              the resolved build target (e.g. {@code ios-device}, {@code local-javascript})
     * @param allowUnhardenedLocalBuild the {@code harden.allowUnhardenedLocalBuild} escape hatch
     * @param onDeviceDebug            whether this is an on-device-debug build
     */
    public static Result check(String level, String buildTarget,
                               boolean allowUnhardenedLocalBuild, boolean onDeviceDebug) {
        String normalized = level == null ? "off" : level.trim().toLowerCase();
        if (normalized.length() == 0) {
            normalized = "off";
        }
        if (!LEVELS.contains(normalized)) {
            return Result.fail("Invalid harden.level '" + level + "'. Valid values are: "
                    + "off, standard, aggressive, paranoid. The build was stopped rather than "
                    + "silently treating an unrecognized value as 'off'.");
        }
        if ("off".equals(normalized)) {
            return Result.ok();
        }
        if (onDeviceDebug) {
            return Result.fail("App hardening cannot be combined with an on-device-debug build: a "
                    + "debuggable, hardened binary is a contradiction. Remove harden.level or build "
                    + "a normal device target.");
        }
        if (isLocalOrSourceTarget(buildTarget)) {
            if (allowUnhardenedLocalBuild) {
                return Result.forceOff("App hardening runs on the Codename One build server; the "
                        + "target '" + buildTarget + "' is built locally, so this output is NOT "
                        + "hardened. Proceeding unhardened because "
                        + "harden.allowUnhardenedLocalBuild=true.");
            }
            return Result.fail("App hardening cannot run for the build target '" + buildTarget
                    + "'. Hardening runs on the Codename One build server, on the merged application "
                    + "jar, before translation -- a local or source-project build never reaches the "
                    + "server, so the project this produces would NOT be hardened. Build a cloud "
                    + "target (e.g. ios-device / android-device), set harden.level=off, or -- if you "
                    + "understand the output is unhardened -- set "
                    + "codename1.arg.harden.allowUnhardenedLocalBuild=true.");
        }
        return Result.ok();
    }

    /** True for the {@code *-source} and {@code local-*} targets, which never reach the build server. */
    public static boolean isLocalOrSourceTarget(String buildTarget) {
        if (buildTarget == null) {
            return false;
        }
        String t = buildTarget.trim().toLowerCase();
        return t.startsWith("local-") || t.endsWith("-source") || t.equals("mac-source")
                || t.equals("windows-source") || t.equals("ios-source") || t.equals("android-source");
    }
}
