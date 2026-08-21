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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The surfaces Swift sources are compiled into a watchOS widget extension as well as an iOS
/// one, and several of the symbols they use do not exist on watchOS: the four system widget
/// families are `@available(watchOS, unavailable)`, and `UIColor.systemBackground`,
/// `UIColor(dynamicProvider:)` and `UIGraphicsImageRenderer` are all `API_UNAVAILABLE(watchos)`.
/// Naming any of them outside a platform guard fails the watch build.
///
/// The real proof is a watchOS compile, which the `build-ios-watch` CI job performs. This test
/// is the cheap half that also runs on a Linux leg with no Xcode: it reads the shipped
/// resources and checks that each forbidden symbol appears only inside a `#if !os(watchOS)`
/// region. It cannot prove the sources compile -- only that the specific mistakes that have
/// actually been made here have not been made again.
class SurfacesSwiftWatchPortabilityTest {

    private static final String ROOT = "/com/codename1/builders/surfaces/ios/";

    /// Symbols that must never be reachable when compiling for watchOS.
    ///
    /// The dynamic-provider entry is spelled with its closure parameter because a bare
    /// "UIColor {" also matches the trailing brace of `func cn1UIColor(...) -> UIColor {`,
    /// which is a perfectly portable declaration.
    private static final String[] IOS_ONLY_SYMBOLS = {
        ".systemSmall", ".systemMedium", ".systemLarge", ".systemExtraLarge",
        "UIColor.systemBackground", "UIColor { trait", "UIGraphicsImageRenderer"
    };

    private static final String[] SHARED_SOURCES = {
        "CN1DescriptorWidget.swift", "CN1SurfaceModel.swift",
        "CN1SurfaceRenderer.swift", "CN1WidgetProvider.swift"
    };

    private static String load(String name) throws IOException {
        InputStream in = SurfacesSwiftWatchPortabilityTest.class.getResourceAsStream(ROOT + name);
        if (in == null) {
            fail("missing surfaces Swift resource " + name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    /// True when the line sits inside a region the watch compiler never sees.
    ///
    /// Tracks the `#if` nesting rather than pattern-matching a single line, because the guard
    /// that matters is often several lines above the symbol and may be nested inside another.
    private static boolean[] excludedFromWatch(String source) {
        String[] lines = source.split("\n", -1);
        boolean[] excluded = new boolean[lines.length];
        // One entry per open #if: true when that block's ACTIVE branch is invisible to watchOS.
        Deque<Boolean> stack = new ArrayDeque<Boolean>();
        boolean hidden = false;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("#if ")) {
                boolean blockHidden = trimmed.contains("!os(watchOS)");
                stack.push(Boolean.valueOf(blockHidden));
                hidden = hidden || blockHidden;
            } else if (trimmed.equals("#else") && !stack.isEmpty()) {
                // The other branch of a `#if os(watchOS)` is equally invisible to the watch.
                boolean wasHidden = stack.pop().booleanValue();
                boolean nowHidden = !wasHidden && wasElseOfWatchOnly(lines, i);
                stack.push(Boolean.valueOf(nowHidden));
                hidden = anyTrue(stack);
            } else if (trimmed.equals("#endif") && !stack.isEmpty()) {
                stack.pop();
                hidden = anyTrue(stack);
            }
            excluded[i] = hidden;
        }
        return excluded;
    }

    /// Whether the `#else` at {@code idx} closes a `#if os(watchOS)` block, which makes the
    /// else-branch the non-watch one.
    private static boolean wasElseOfWatchOnly(String[] lines, int idx) {
        int depth = 0;
        for (int i = idx - 1; i >= 0; i--) {
            String trimmed = lines[i].trim();
            if (trimmed.equals("#endif")) {
                depth++;
            } else if (trimmed.startsWith("#if ")) {
                if (depth == 0) {
                    return trimmed.contains("os(watchOS)") && !trimmed.contains("!os(watchOS)");
                }
                depth--;
            }
        }
        return false;
    }

    private static boolean anyTrue(Deque<Boolean> stack) {
        for (Boolean b : stack) {
            if (b.booleanValue()) {
                return true;
            }
        }
        return false;
    }

    @Test
    void iosOnlySymbolsAreNeverReachableFromTheWatchSlice() throws IOException {
        StringBuilder problems = new StringBuilder();
        for (String name : SHARED_SOURCES) {
            String source = load(name);
            String[] lines = source.split("\n", -1);
            boolean[] excluded = excludedFromWatch(source);
            for (int i = 0; i < lines.length; i++) {
                if (excluded[i] || lines[i].trim().startsWith("//")) {
                    continue;
                }
                for (String symbol : IOS_ONLY_SYMBOLS) {
                    if (lines[i].contains(symbol)) {
                        problems.append(name).append(':').append(i + 1)
                                .append(" uses ").append(symbol)
                                .append(" outside a #if !os(watchOS) guard\n");
                    }
                }
            }
        }
        assertTrue(problems.length() == 0,
                "these are unavailable on watchOS and will fail the watch build:\n" + problems);
    }

    /// containerBackground(for:) is watchOS 10.0, and the watch extension's floor is exactly
    /// 10.0. Leaving the availability check as a bare `*` compiles today and would silently
    /// stop guarding if that floor were ever lowered.
    @Test
    void containerBackgroundNamesItsWatchAvailability() throws IOException {
        String source = load("CN1DescriptorWidget.swift");

        assertTrue(source.contains("#available(iOS 17.0, watchOS 10.0, *)"),
                "containerBackground must declare its watchOS availability explicitly");
    }
}
