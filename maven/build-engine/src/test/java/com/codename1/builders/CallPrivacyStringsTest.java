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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The call privacy strings reach the plist, and only for apps that own calls.
 *
 * <p>Both halves were wrong at once, in opposite directions, which is why
 * this is one test rather than two. The injection sat inside the
 * {@code usesCallSession || usesCallVoip || usesCallDirectory} branch, so a
 * directory-only app -- one that labels or blocks somebody else's caller and
 * never touches AVAudioSession -- was given a voice-capture disclosure, and
 * would have been REFUSED for setting that description to false. And it wrote
 * the value with {@code putArgument}, while the sweep that copies
 * {@code ios.NS*UsageDescription} arguments into {@code
 * privacyUsageDescriptions} runs earlier in the same method and the plist is
 * rendered from that map alone -- so the value never shipped, and the safety
 * net for an app whose only call usage is inside a cn1lib (which sets
 * usesCallSession but produces no PlatformFeatureCatalog hit, so the
 * catalog's own entry does not fire either) did nothing at all.</p>
 *
 * <p>Source text rather than behaviour, as {@link HealthScannerParityTest}
 * explains: the region lives in the middle of a build method with no seam to
 * call. Crude and load-bearing beats absent.</p>
 */
public class CallPrivacyStringsTest {

    /** The call block of the iOS builder, from its plist flag to the VPN one. */
    private static String callBlock() throws Exception {
        File f = new File(
                "src/main/java/com/codename1/builders/IPhoneBuilder.java");
        assertTrue(f.exists(), "builder source must be readable: "
                + f.getAbsolutePath());
        String src = new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
        int from = src.indexOf("callPlistWanted = true;");
        assertTrue(from > 0, "the call block starts at callPlistWanted");
        int to = src.indexOf("// VPN configuration management.", from);
        assertTrue(to > from, "the call block ends where the VPN one begins");
        return src.substring(from, to);
    }

    @Test
    public void privacyStringsAreGatedOnOwningACall() throws Exception {
        String block = callBlock();
        int mic = block.indexOf("NSMicrophoneUsageDescription");
        int camera = block.indexOf("NSCameraUsageDescription");
        assertTrue(mic > 0 && camera > 0,
                "both purpose strings are decided in this block");
        for (int at : new int[] {mic, camera}) {
            // The nearest preceding condition, which is the one that decides
            // whether the string is written. usesCallDirectory must not
            // appear in it: a caller-ID app has no media of any kind.
            int guard = block.lastIndexOf("if (", at);
            assertTrue(guard >= 0, "each purpose string sits under a guard");
            String condition = block.substring(guard, at);
            assertTrue(condition.contains("usesCallSession")
                            && condition.contains("usesCallVoip"),
                    "owning a call is what earns a media disclosure: "
                            + condition);
            assertTrue(!condition.contains("usesCallDirectory"),
                    "a directory-only app never reaches media: " + condition);
        }
    }

    @Test
    public void privacyStringsGoWhereThePlistIsRenderedFrom() throws Exception {
        String block = callBlock();
        for (String key : new String[] {"NSMicrophoneUsageDescription",
                "NSCameraUsageDescription"}) {
            boolean placed = false;
            int at = block.indexOf("privacyUsageDescriptions.put(");
            while (at >= 0 && !placed) {
                int stop = Math.min(block.length(), at + 160);
                placed = block.substring(at, stop).contains(key);
                at = block.indexOf("privacyUsageDescriptions.put(", at + 1);
            }
            assertTrue(placed, key + " has to be placed in the map the plist"
                    + " is rendered from, not in an argument");
        }
        // And NOT through an argument. The sweep that would have copied it
        // into that map has already run by the time this block executes, so a
        // putArgument here is a value nothing ever reads.
        assertTrue(!block.contains(
                        "putArgument(\"ios.NSMicrophoneUsageDescription\"")
                        && !block.contains(
                                "putArgument(\"ios.NSCameraUsageDescription\""),
                "an argument set this late is never swept into the map");
    }
}
