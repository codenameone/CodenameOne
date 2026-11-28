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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The StoreKit 2 bridge is a Swift class the port reaches by name, through an
/// Objective-C protocol. Nothing compiles the two against each other: a
/// selector the protocol declares that the Swift class does not export
/// compiles, links, and throws "unrecognized selector" on the device the
/// first time a purchase is made. So the protocol is read here and every one
/// of its selectors is required of the Swift source.
public class StoreKit2InjectorTest {

    private static BuildRequest request(String... pairs) {
        BuildRequest r = new BuildRequest();
        for (int i = 0; i < pairs.length; i += 2) {
            r.putArgument(pairs[i], pairs[i + 1]);
        }
        return r;
    }

    private static String bridgeSource() throws Exception {
        InputStream is = StoreKit2InjectorTest.class.getResourceAsStream(
                "/com/codename1/builders/storekit/CN1StoreKit2.swifts");
        assertNotNull(is, "the bridge is not packaged with the builder");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = is.read(b)) > 0) {
                out.write(b, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            is.close();
        }
    }

    @Test
    public void storeKit2IsOnUnlessTurnedOff() {
        assertTrue(StoreKit2Injector.enabled(request(), false));
        assertTrue(StoreKit2Injector.enabled(request("ios.storeKit2", "true"), false));
        assertFalse(StoreKit2Injector.enabled(request("ios.storeKit2", " FALSE "), false));
        assertTrue(StoreKit2Injector.enabled(request(), true));
    }

    @Test
    public void aMacBuildFallsBackToTheIosHint() {
        assertFalse(StoreKit2Injector.enabled(request("ios.storeKit2", "false"), true));
        assertTrue(StoreKit2Injector.enabled(request("ios.storeKit2", "false", "macos.storeKit2", "true"), true));
        assertFalse(StoreKit2Injector.enabled(request("macos.storeKit2", "false"), true));
        // The iOS build never reads the Mac hint.
        assertTrue(StoreKit2Injector.enabled(request("macos.storeKit2", "false"), false));
    }

    @Test
    public void theSwiftClassExportsEverySelectorTheProtocolDeclares() throws Exception {
        String swift = bridgeSource();
        assertTrue(swift.contains("@objc(CN1StoreKit2)"),
                "CN1StoreKit.m finds the class as CN1StoreKit2");
        File header = new File("../../Ports/iOSPort/nativeSources/CodenameOne_GLViewController.h");
        String h = new String(Files.readAllBytes(header.toPath()), StandardCharsets.UTF_8);
        int start = h.indexOf("@protocol CN1StoreKit2Bridge");
        assertTrue(start >= 0, "protocol not found in " + header);
        String protocol = h.substring(start, h.indexOf("@end", start));
        Matcher m = Pattern.compile("^- \\([^)]*\\)(\\w+)(.*);$", Pattern.MULTILINE).matcher(protocol);
        int count = 0;
        while (m.find()) {
            count++;
            String first = m.group(1);
            // Further keywords of the selector: "fetchProducts:(..)skus timeoutMs:(..)".
            Matcher k = Pattern.compile("\\s(\\w+):").matcher(m.group(2));
            StringBuilder labels = new StringBuilder();
            while (k.find()) {
                labels.append("[^)]*\\b").append(k.group(1)).append(":");
            }
            // Swift exports func first(_ a: T, label: U) as first:label: -- an
            // unlabelled first argument, every later one labelled.
            boolean takesArgs = m.group(2).startsWith(":");
            String re = "@objc func " + first + "\\(" + (takesArgs ? "_ \\w+:" : "\\)") + labels;
            assertTrue(Pattern.compile(re).matcher(swift).find(),
                    "the Swift bridge exports no selector for '" + m.group(0).trim() + "'");
        }
        assertTrue(count >= 8, "read only " + count + " selectors from the protocol");
    }

    @Test
    public void theBridgeCompilesToNothingOnWatchOs() throws Exception {
        String swift = bridgeSource();
        int guard = swift.indexOf("#if !os(watchOS)");
        assertTrue(guard >= 0 && guard < swift.indexOf("import StoreKit"),
                "the watch target may take the file and has no Transaction below watchOS 8");
        assertTrue(swift.trim().endsWith("#endif"));
    }
}
