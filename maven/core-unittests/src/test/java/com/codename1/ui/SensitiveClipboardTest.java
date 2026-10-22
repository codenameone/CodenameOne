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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

/// The sensitive-clipboard flags and the scheduled clear `ClipboardContent#setClearAfter(int)`
/// asks for.
class SensitiveClipboardTest extends UITestBase {

    private static String pasted() {
        ClipboardContent now = Display.getInstance().getClipboardContent();
        return now == null ? null : now.getText(ClipboardContent.MIME_TEXT);
    }

    private void waitPast(int millis) throws InterruptedException {
        long until = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < until) {
            flushSerialCalls();
            Thread.sleep(10);
        }
        flushSerialCalls();
    }

    @FormTest
    void flagsAreKept() {
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "123456")
                .setSensitive(true)
                .setClearAfter(30000);
        assertTrue(content.isSensitive());
        assertEquals(30000, content.getClearAfter());
        assertFalse(new ClipboardContent().isSensitive());
        assertEquals(0, new ClipboardContent().setClearAfter(-5).getClearAfter());
    }

    @FormTest
    void clearedWhenTheTimeIsUp() throws InterruptedException {
        Display.getInstance().copyToClipboard(new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "recovery-code").setSensitive(true)
                .setClearAfter(50));
        assertEquals("recovery-code", pasted());
        waitPast(400);
        assertNull(pasted(), "the sensitive copy was taken off the clipboard");
    }

    @FormTest
    void aLaterCopyIsNeverCleared() throws InterruptedException {
        Display.getInstance().copyToClipboard(new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "secret").setClearAfter(50));
        Display.getInstance().copyToClipboard("the user's own copy");
        waitPast(400);
        assertEquals("the user's own copy", pasted());
    }

    @FormTest
    void keptWithoutAClearTime() throws InterruptedException {
        Display.getInstance().copyToClipboard(new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "plain").setSensitive(true));
        waitPast(200);
        assertEquals("plain", pasted());
    }
}
