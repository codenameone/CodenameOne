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
package android.content;

import android.net.Uri;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// A sandboxed port's app home is `file://home/`; opening a resolver stream
/// for a file under it must address the whole storage path, authority
/// included, not just the URI's path (`/files/x`).
public class ContentResolverFileUriTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void fileUriKeepsItsAuthority() throws Exception {
        ContentResolver r = AndroidTestSupport.context().getContentResolver();
        Uri u = Uri.parse("file://home/files/note.txt");
        OutputStream out = r.openOutputStream(u);
        out.write(new byte[] {1, 2, 3});
        out.close();
        assertTrue(HeadlessImplementation.FILES.keySet().toString(),
                HeadlessImplementation.FILES.containsKey("file://home/files/note.txt"));
        InputStream in = r.openInputStream(u);
        byte[] b = new byte[3];
        assertEquals(3, in.read(b));
        assertArrayEquals(new byte[] {1, 2, 3}, b);
    }

    @Test
    public void localFileUriStaysTheSame() throws Exception {
        ContentResolver r = AndroidTestSupport.context().getContentResolver();
        OutputStream out = r.openOutputStream(Uri.parse("file:///data/report.txt"));
        out.write(7);
        out.close();
        assertTrue(HeadlessImplementation.FILES.keySet().toString(),
                HeadlessImplementation.FILES.containsKey("file:///data/report.txt"));
    }
}
