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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// The lowest deployment target each Apple SDK will accept, asked of the SDK itself.
///
/// Apple raises these between Xcode releases, and a project under the floor does not warn --
/// it fails outright with "the range of supported deployment target versions is X to Y".
/// Measured across the two SDKs installed when this was written:
///
/// | platform | Xcode 26 | Xcode 27 |
/// |---|---|---|
/// | iOS      | 12.0  | 15.0 |
/// | tvOS     | 12.0  | 15.0 |
/// | watchOS  | 4.0   | 9.0  |
/// | macOS    | 10.13 | 12.0 |
///
/// Codename One's own defaults were iOS 14.0, tvOS 13.0 and macOS 10.15, so three of the four
/// slices stopped building the day Xcode 27 was selected -- an unmodified Hello World included.
/// Only watchOS, at 10.0, happened to sit above the new floor.
///
/// The answer is read from the SDK rather than kept as an Xcode-version table in this tree,
/// because a table has to be edited on exactly the release where nobody is looking for it. This
/// way the next raise costs nothing, and a build against an Xcode that has not been released
/// yet is already correct.
///
/// Every lookup returns null rather than throwing when there is no Mac, no Xcode, or no
/// readable SDKSettings, which leaves a build with whatever floor its hints and features asked
/// for -- the behaviour before any of this existed.
final class AppleSdkFloor {

    /// Two subprocesses per lookup, and the same SDK is asked about by several builders in one
    /// build. Keyed by developer directory as well as SDK, because a build can legitimately
    /// switch Xcode between runs in the same process.
    private static final Map<String, String> CACHE = new ConcurrentHashMap<String, String>();

    /// Stands in for a null answer, which ConcurrentHashMap will not store.
    private static final String NONE = "";

    private AppleSdkFloor() {
    }

    /// The minimum deployment target of `sdkName`, or null if it cannot be determined.
    ///
    /// `sdkName` is an xcrun SDK name -- `iphoneos`, `appletvos`, `watchos`, `macosx` -- and is
    /// also the key inside the SDK's own `SupportedTargets` dictionary.
    static String minimumDeploymentTarget(String sdkName, String xcrun, String developerDir) {
        String key = (developerDir == null ? "" : developerDir) + "|" + sdkName;
        String cached = CACHE.get(key);
        if (cached != null) {
            return NONE.equals(cached) ? null : cached;
        }
        String answer = lookup(sdkName, xcrun, developerDir);
        CACHE.put(key, answer == null ? NONE : answer);
        return answer;
    }

    private static String lookup(String sdkName, String xcrun, String developerDir) {
        try {
            String sdkPath = firstLine(developerDir,
                    xcrun == null ? "xcrun" : xcrun, "--sdk", sdkName, "--show-sdk-path");
            if (sdkPath == null) {
                return null;
            }
            File settings = new File(sdkPath, "SDKSettings.plist");
            if (!settings.isFile()) {
                return null;
            }
            // SDKSettings.plist is a binary plist. plutil prints the raw scalar, which is one
            // subprocess against this module growing a plist parser for a single string.
            String min = firstLine(developerDir, "/usr/bin/plutil", "-extract",
                    "SupportedTargets." + sdkName + ".MinimumDeploymentTarget", "raw",
                    settings.getAbsolutePath());
            if (min != null && min.length() > 0 && Character.isDigit(min.charAt(0))) {
                return min;
            }
        } catch (Exception noXcodeHere) {
            // Not a Mac, or no Xcode. The caller keeps the floor it already had.
        }
        return null;
    }

    /// First non-empty line of a command, run against a specific Xcode, or null.
    private static String firstLine(String developerDir, String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (developerDir != null && developerDir.length() > 0) {
            // Asking the system default can answer for a different installation than the one
            // this build uses, which is the whole problem being solved here.
            builder.environment().put("DEVELOPER_DIR", developerDir);
        }
        Process p = builder.redirectErrorStream(false).start();
        String line;
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        try {
            line = in.readLine();
        } finally {
            in.close();
        }
        if (p.waitFor() != 0 || line == null) {
            return null;
        }
        line = line.trim();
        return line.length() == 0 ? null : line;
    }

    /// `current` unless `floor` is higher, in which case `floor`.
    ///
    /// A null or unreadable floor leaves `current` alone, so a machine that cannot be asked
    /// behaves exactly as it did before.
    static String raiseTo(String current, String floor) {
        if (floor == null || floor.length() == 0) {
            return current;
        }
        if (current == null || current.length() == 0) {
            return floor;
        }
        return compare(current, floor) < 0 ? floor : current;
    }

    /// Numeric, component-wise version comparison: 10.9 is below 10.13, which a string
    /// comparison gets backwards, and that is exactly the shape of a macOS floor.
    static int compare(String a, String b) {
        String[] left = a.trim().split("\\.");
        String[] right = b.trim().split("\\.");
        int len = Math.max(left.length, right.length);
        for (int i = 0; i < len; i++) {
            int l = componentAt(left, i);
            int r = componentAt(right, i);
            if (l != r) {
                return l < r ? -1 : 1;
            }
        }
        return 0;
    }

    private static int componentAt(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException notANumber) {
            // A build string like "27.0.x" or a beta suffix: treat the component as zero
            // rather than failing, since the comparison only has to order floors.
            return 0;
        }
    }
}
