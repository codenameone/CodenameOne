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

import com.codename1.util.IOSCallDirectoryExtensionBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated iOS Call Directory extension.
 *
 * <p>Without this target the directory API compiled, linked, and then failed
 * every {@code setEntries} with "No App Group is configured" -- the extension
 * that reads the numbers runs in another process and simply was not there.</p>
 */
public class CallDirectoryExtensionTest {

    private static Map<String, byte[]> files() {
        return IOSCallDirectoryExtensionBuilder.buildFileMap(
                "com.example.app", "group.com.example.app.cn1call",
                "Example", "1.2", "34");
    }

    private static String text(Map<String, byte[]> files, String name) {
        byte[] b = files.get(name);
        if (b == null) {
            return null;
        }
        try {
            return new String(b, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void theExtensionCarriesEverythingATargetNeeds() {
        Map<String, byte[]> f = files();
        assertTrue(f.containsKey("CN1CallDirectoryHandler.m"));
        assertTrue(f.containsKey("CN1CallDirectoryHandler.h"));
        assertTrue(f.containsKey("Info.plist"));
        assertTrue(f.containsKey("CN1CallDirectory.entitlements"));
    }

    @Test
    public void theInfoPlistNamesTheCallDirectoryExtensionPoint() {
        // The exact string is what tells iOS this is a call directory rather
        // than some other extension kind; a wrong one makes the extension
        // build, sign, embed and never launch.
        //
        // The value is Xcode's, read from its own template rather than from
        // memory -- Platforms/iPhoneOS.platform/Developer/Library/Xcode/
        // Templates/Project Templates/iOS/Application Extension/Call
        // Directory Extension.xctemplate/TemplateInfo.plist. This assertion
        // previously pinned com.apple.identitylookup.call-directory, which
        // is not an extension point at all: IdentityLookup owns
        // message-filter and classification-ui, and nothing in the toolchain
        // mentions an identitylookup call-directory.
        String plist = text(files(), "Info.plist");
        assertTrue(plist.contains(
                "<string>com.apple.callkit.call-directory</string>"));
        assertTrue(plist.contains("<string>CN1CallDirectoryHandler</string>"));
        assertTrue(plist.contains("<string>XPC!</string>"));
    }

    @Test
    public void theVersionsMatchTheHostApp() {
        // An embedded extension whose marketing or build version differs from
        // its containing app fails archive validation.
        String plist = text(files(), "Info.plist");
        assertTrue(plist.contains("<string>1.2</string>"));
        assertTrue(plist.contains("<string>34</string>"));
    }

    @Test
    public void bothSidesShareTheSameAppGroup() {
        // The whole mechanism: the app writes the file into this group and
        // the extension reads it from the same one.
        String ent = text(files(), "CN1CallDirectory.entitlements");
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(ent.contains("group.com.example.app.cn1call"));
        assertTrue(handler.contains("group.com.example.app.cn1call"));
        assertTrue(ent.contains("com.apple.security.application-groups"));
    }

    @Test
    public void theHandlerHandlesAnIncrementalReload() {
        // Adding the whole list again during an incremental request is an
        // error, and no changelog is kept -- so it must be turned back into a
        // full reload rather than ignored.
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(handler.contains("isIncremental"));
        assertTrue(handler.contains("removeAllIdentificationEntries"));
        assertTrue(handler.contains("removeAllBlockingEntries"));
    }

    @Test
    public void theHandlerRefusesRowsThatWouldBreakTheOrdering() {
        // iOS rejects the whole list when entries are out of order, naming no
        // row, so a bad row is skipped rather than allowed to poison it.
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(handler.contains("number <= previous"));
        assertTrue(handler.contains("addBlockingEntryWithNextSequentialPhoneNumber"));
        assertTrue(handler.contains("addIdentificationEntryWithNextSequentialPhoneNumber"));
    }

    @Test
    public void anAbsentDataFileCompletesRatherThanFails() {
        // Failing the request makes iOS disable the extension; completing
        // with no entries is the correct answer before setEntries has run.
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(handler.contains("data == nil"));
        assertTrue(handler.contains("completeRequestWithCompletionHandler"));
    }

    @Test
    public void blockedAndLabelledNumbersAreEmittedInSeparatePasses() {
        // One interleaved pass emitted blocking(N) immediately followed by
        // identification(N) for a row that is both blocked and labelled.
        // Whether those two share one sequence cursor is precisely what
        // CallKit does not document -- the headers carry no prose at all, and
        // Xcode's own template says only "Numbers must be provided in
        // numerically ascending order", stated once per kind. Guessing wrong
        // costs a directory that silently never loads, which no build catches
        // and only a device shows. Two passes are correct under either
        // reading, and a second scan of an mmapped buffer is nearly free.
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(handler.contains("cn1cdScan(context, data, YES);"));
        assertTrue(handler.contains("cn1cdScan(context, data, NO);"));
        int block = handler.indexOf(
                "addBlockingEntryWithNextSequentialPhoneNumber");
        int ident = handler.indexOf(
                "addIdentificationEntryWithNextSequentialPhoneNumber");
        assertTrue(block > 0 && ident > block,
                "the two kinds must not share a scan");
        // Each pass keeps its own ascending cursor, so a number present in
        // both lists is not dropped by the other pass's "goes backwards"
        // guard.
        assertEquals(2, count(handler, "previous = number;"),
                "each pass advances only its own cursor");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0;
                i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    public void theListIsMappedRatherThanReadIntoMemory() {
        // A production blocklist runs to six figures and the extension has a
        // tight memory budget; reading it into an NSString is how the
        // extension gets killed and the reload silently fails.
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(handler.contains("NSDataReadingMappedIfSafe"),
                "the directory must be memory-mapped, not read");
        assertFalse(handler.contains("stringWithContentsOfURL"),
                "reading the whole file into a string is what this avoids");
        assertTrue(handler.contains("@autoreleasepool"));
    }

    @Test
    public void theCommonRowAllocatesNothing() {
        // The number is parsed from the raw bytes; only a row that carries a
        // label builds an NSString.
        String handler = text(files(), "CN1CallDirectoryHandler.m");
        assertTrue(handler.contains("number * 10 + (bytes[cursor] - '0')"));
        assertTrue(handler.contains("initWithBytes:bytes + labelStart"));
    }

    @Test
    public void theBundleIdIsDerivedFromTheHost() {
        assertEquals("com.example.app.calldirectory",
                IOSCallDirectoryExtensionBuilder.bundleId("com.example.app"));
        assertEquals("group.com.example.app.cn1call",
                IOSCallDirectoryExtensionBuilder.defaultAppGroup("com.example.app"));
    }

    @Test
    public void theExtensionShipsNoSwiftRuntime() {
        // An extension is memory-capped and CallKit has an Objective-C
        // interface, so unlike the Matter extension there is no reason to
        // embed the Swift standard library.
        for (String name : files().keySet()) {
            assertFalse(name.endsWith(".swift"),
                    "the call directory extension is Objective-C: " + name);
        }
    }
}
