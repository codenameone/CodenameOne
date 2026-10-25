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
package com.codename1.androidcompat.runtime;

import android.content.Intent;
import android.net.Uri;

import com.codename1.androidcompat.testing.AndroidTestSupport;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/// ACTION_SEND's file is in EXTRA_STREAM. It was ignored and the share still
/// reported success; a stream the runtime cannot open is now refused.
public class ShareStreamTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void fileStreamsNameTheirPathAndOthersAreRefused() {
        assertEquals("file:///data/report.pdf", AndroidRuntime.sharedFilePath(Uri.parse("file:///data/report.pdf")));
        assertNull(AndroidRuntime.sharedFilePath(Uri.parse("content://com.example.provider/report.pdf")));

        AndroidTestSupport.context();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://com.example.provider/report.pdf"));
        assertFalse(AndroidRuntime.getInstance().handleImplicitIntent(send));
    }
    @Test
    public void sandboxedFileStreamsKeepTheirAuthority() {
        assertEquals("file://home/files/my image.png",
                AndroidRuntime.sharedFilePath(Uri.parse("file://home/files/my%20image.png")));
        assertEquals("/files/my image.png",
                AndroidRuntime.sharedFilePath(Uri.parse("/files/my%20image.png")));
    }

}
