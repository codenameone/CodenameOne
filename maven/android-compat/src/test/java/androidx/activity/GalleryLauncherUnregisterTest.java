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
package androidx.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.net.Uri;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

/// A `GetContent` launcher goes to the platform gallery. Unregistering it
/// while the gallery is open means its callback is no longer called, as on
/// Android, rather than answered by the listener the launch captured.
public class GalleryLauncherUnregisterTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void reset() {
        HeadlessImplementation.gallery = null;
    }

    @Test
    public void anUnregisteredLauncherHearsNothing() {
        HeadlessImplementation.install();
        final int[] calls = new int[1];
        ComponentActivity a = new ComponentActivity();
        ActivityResultCallback<Uri> cb = new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                calls[0]++;
            }
        };
        ActivityResultLauncher<String> launcher = a.registerForActivityResult(
                new ActivityResultContracts.GetContent(), cb);
        launcher.launch("image/*");
        ActionListener picker = HeadlessImplementation.gallery;
        assertNotNull("the launch did not open the gallery", picker);
        picker.actionPerformed(new ActionEvent("file:///a.png"));
        assertEquals("a registered launcher hears the result", 1, calls[0]);

        HeadlessImplementation.gallery = null;
        launcher.launch("image/*");
        picker = HeadlessImplementation.gallery;
        assertNotNull(picker);
        launcher.unregister();
        picker.actionPerformed(new ActionEvent("file:///b.png"));
        assertEquals("an unregistered launcher heard the result", 1, calls[0]);
    }
}
