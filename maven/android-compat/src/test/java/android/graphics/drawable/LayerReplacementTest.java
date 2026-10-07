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
package android.graphics.drawable;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import org.junit.Test;
import static org.junit.Assert.*;

public class LayerReplacementTest {
    @Test public void replacementInheritsContainerProperties() {
        ColorDrawable old = new ColorDrawable(0xff000000);
        LayerDrawable layers = new LayerDrawable(new Drawable[] {old});
        layers.setId(0, 7);
        layers.setBounds(0, 0, 80, 40);
        layers.setLayerInset(0, 2, 3, 4, 5);
        int[] state = {android.R.attr.state_pressed};
        layers.setState(state);
        layers.setLevel(6500);
        layers.setVisible(false, false);
        layers.setAlpha(100);
        layers.setLayoutDirection(1);
        layers.setAutoMirrored(true);
        ColorFilter filter = new PorterDuffColorFilter(0xff00ff00, PorterDuff.Mode.SRC_IN);
        layers.setColorFilter(filter);
        ColorDrawable replacement = new ColorDrawable(0xffffffff);
        assertTrue(layers.setDrawableByLayerId(7, replacement));
        assertEquals(6500, replacement.getLevel());
        assertFalse(replacement.isVisible());
        assertEquals(100, replacement.getAlpha());
        assertEquals(1, replacement.getLayoutDirection());
        assertTrue(replacement.isAutoMirrored());
        assertSame(filter, replacement.getColorFilter());
        assertArrayEquals(state, replacement.getState());
        assertEquals(new Rect(2, 3, 76, 35), replacement.getBounds());
        assertNull(old.getCallback());
        assertSame(layers, replacement.getCallback());
    }
}
