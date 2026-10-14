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

package com.codename1.impl.ios;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class MacFileChooserRequestTest extends UITestBase {
    private static class NativeStub extends IOSNative {
        final ArrayList<Integer> requests = new ArrayList<Integer>();
        boolean fail;
        @Override void openFileChooserWithId(String accept, int requestId) {
            if (fail) { throw new IllegalStateException("presentation failed"); }
            requests.add(requestId);
        }
    }

    @FormTest
    void chooserCompletionsFollowRequestIdentityRatherThanPresentationOrder() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        IOSImplementation port = new IOSImplementation() {
            @Override public String getPlatformName() { return "mac"; }
        };
        NativeStub stub = new NativeStub();
        final ArrayList<String> delivered = new ArrayList<String>();
        try {
            IOSImplementation.nativeInstance = stub;
            IOSImplementation.instance = port;
            port.openFileChooser(e -> delivered.add("first:" + e.getSource()), ".cn1doc");
            port.openFileChooser(e -> delivered.add("second:" + e.getSource()), ".docx");
            assertNotEquals(stub.requests.get(0), stub.requests.get(1));
            IOSImplementation.macFileChooserResult("/tmp/second.docx", stub.requests.get(1));
            IOSImplementation.macFileChooserResult(null, stub.requests.get(0));
            IOSImplementation.macFileChooserResult("/tmp/duplicate", stub.requests.get(1));
            flushSerialCalls();
            assertEquals(java.util.Arrays.asList("second:file:/tmp/second.docx", "first:null"), delivered);
            stub.fail = true;
            assertThrows(IllegalStateException.class,
                    () -> port.openFileChooser(e -> fail("failed request must not receive another result"), "*"));
            stub.fail = false;
            port.openFileChooser(e -> delivered.add("third:" + e.getSource()), "*");
            IOSImplementation.macFileChooserResult("/tmp/third", stub.requests.get(2));
            flushSerialCalls();
            assertEquals("third:file:/tmp/third", delivered.get(2));
            assertEquals(3, delivered.size());
        } finally {
            for (Integer request : stub.requests) { IOSImplementation.macFileChooserResult(null, request); }
            flushSerialCalls();
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }
}
