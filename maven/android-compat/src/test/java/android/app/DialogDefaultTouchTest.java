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
package android.app;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
public class DialogDefaultTouchTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void plainDialogRequiresOptInButCancelableAlertOptsIn() {
        android.content.Context c = AndroidTestSupport.context();
        Dialog plain = new Dialog(c); plain.setContentView(new android.widget.TextView(c)); plain.show();
        try { assertFalse(plain.host().isDisposeWhenPointerOutOfBounds()); }
        finally { plain.dismiss(); }
        AlertDialog alert = new AlertDialog.Builder(c).setMessage("message").create(); alert.show();
        try { assertTrue(alert.host().isDisposeWhenPointerOutOfBounds()); }
        finally { alert.dismiss(); }
        alert = new AlertDialog.Builder(c).setMessage("message").setCancelable(false).create(); alert.show();
        try { assertFalse(alert.host().isDisposeWhenPointerOutOfBounds()); }
        finally { alert.dismiss(); }
    }
}
