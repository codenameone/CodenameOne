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

import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/// `intent.resolveActivity(pm) != null` is how Android code checks an intent
/// can run before starting it. An implicit intent has no component, and
/// answering null for it suppressed every browser, dialer and share intent.
public class IntentResolveActivityTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void implicitIntentsTheRuntimeHandlesResolve() {
        Context c = AndroidTestSupport.context();
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.codenameone.com"));
        assertNotNull(view.resolveActivity(c.getPackageManager()));
        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:123"));
        assertNotNull(dial.resolveActivity(c.getPackageManager()));
    }

    @Test
    public void explicitIntentsResolveToTheirActivityAndUnknownActionsToNothing() {
        Context c = AndroidTestSupport.context();
        Intent explicit = new Intent(c, AndroidTestSupport.TestActivity.class);
        assertEquals(AndroidTestSupport.TestActivity.class.getName(),
                explicit.resolveActivity(c.getPackageManager()).getClassName());
        assertNull(new Intent("com.example.NO_SUCH_ACTION").resolveActivity(c.getPackageManager()));
    }
}
