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
package android.view;

import android.widget.FrameLayout;

import com.codename1.androidcompat.runtime.GroupPeer;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import com.codename1.ui.Image;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/// Work a view posts can be cancelled through any view, and a root view's
/// tree observer hears pre-draw and draw before the tree paints.
public class ViewCallbacksTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void removeCallbacksCancelsWhatPostQueued() {
        View v = new View(AndroidTestSupport.context());
        final List<String> log = new ArrayList<String>();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                log.add("ran");
            }
        };
        v.post(r);
        v.removeCallbacks(r);
        v.postDelayed(r, 1);
        v.removeCallbacks(r);
        MainThreadRule.drain();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        MainThreadRule.drain();
        assertEquals("cancelled callbacks still ran", "[]", log.toString());
    }

    @Test
    public void aRootPaintDispatchesPreDrawAndDraw() {
        FrameLayout root = new FrameLayout(AndroidTestSupport.context());
        final List<String> log = new ArrayList<String>();
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                log.add("preDraw");
                return true;
            }
        });
        root.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            @Override
            public void onDraw() {
                log.add("draw");
            }
        });
        Image img = Image.createImage(4, 4);
        ((GroupPeer) root.getPeer()).paint(img.getGraphics());
        assertEquals("[preDraw, draw]", log.toString());
    }
}
