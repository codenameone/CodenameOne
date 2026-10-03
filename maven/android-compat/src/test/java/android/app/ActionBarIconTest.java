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

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The framework action bar draws a menu item's icon as it is. It recolored
/// every icon with the title color, so the gallery's accent star came out white.
public class ActionBarIconTest {

    /// Records whether it was ever drawn under a color filter.
    static final class RecordingIcon extends Drawable {
        ColorFilter filter;
        boolean drawn;
        boolean drawnFiltered;

        @Override
        public void draw(Canvas canvas) {
            drawn = true;
            drawnFiltered |= filter != null;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            filter = colorFilter;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return 24;
        }

        @Override
        public int getIntrinsicHeight() {
            return 24;
        }
    }

    @Test
    public void anActionIconIsDrawnWithoutARecoloring() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final RecordingIcon icon = new RecordingIcon();
        AndroidTestSupport.TestActivity.optionsMenu = new AndroidTestSupport.OptionsMenu() {
            @Override
            public void fill(Menu menu) {
                menu.add(0, 1, 0, "Star").setIcon(icon).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        };
        // An application's forms get a Toolbar from its theme's
        // globalToobarBool constant; the headless display has no theme.
        final boolean globalToolbar = com.codename1.ui.Toolbar.isGlobalToolbar();
        com.codename1.ui.Toolbar.setGlobalToolbar(true);
        try {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    ActivityThread.finishAllActivities();
                }
            });
        } finally {
            AndroidTestSupport.TestActivity.optionsMenu = null;
            com.codename1.ui.Toolbar.setGlobalToolbar(globalToolbar);
        }
        assertTrue("the action icon was never drawn", icon.drawn);
        assertFalse("the action icon was drawn recolored", icon.drawnFiltered);
    }
}
