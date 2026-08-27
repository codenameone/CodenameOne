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
package com.codename1.call;

import com.codename1.call.directory.CallDirectory;
import com.codename1.call.directory.DirectoryEntry;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.LocalCallBridge;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Publishing the call directory writes a real file and swaps it into place,
 * so it needs an initialized implementation -- LocalCallTest has none, which
 * is why this path had no coverage at all.
 */
public class CallDirectoryPublishTest extends UITestBase {

    private LocalCallBridge bridge;

    @BeforeEach
    public void installBridge() {
        bridge = new LocalCallBridge();
        CallRequests.resetForTest(bridge);
    }

    @AfterEach
    public void clearBridge() {
        TestCodenameOneImplementation impl =
                TestCodenameOneImplementation.getInstance();
        if (impl != null) {
            impl.setRenameDisabled(false);
        }
        CallRequests.resetForTest(null);
    }

    @Test
    public void aPublishThatCannotRenameFailsRatherThanEmptyingTheList() {
        DirectoryEntry[] blocked = new DirectoryEntry[]{
            new DirectoryEntry(14155551212L, "Nuisance", true)};
        // The happy path first, so the failure below cannot pass merely
        // because publishing never worked in this harness at all.
        assertEquals(Boolean.TRUE,
                CallAwait.value(CallDirectory.setEntries(blocked)));

        // Now a filesystem that cannot rename. The fallback deletes the LIVE
        // list before moving the replacement into place, so a failure there
        // leaves no directory at all -- and the second rename went unchecked,
        // so the path was returned as though publication had succeeded. The
        // bridge then acknowledged success and dropped its cache, after which
        // every screened call consulted an empty list and the numbers this
        // app had blocked were allowed straight through, silently, for as
        // long as the process lived.
        TestCodenameOneImplementation.getInstance().setRenameDisabled(true);
        CallAwait.assertFailedWith(CallError.DIRECTORY_FAILED,
                CallDirectory.setEntries(blocked));
    }
}
