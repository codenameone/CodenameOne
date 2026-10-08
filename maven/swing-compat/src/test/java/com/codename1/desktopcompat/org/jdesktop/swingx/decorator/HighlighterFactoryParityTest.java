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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// The striping colors are those of SwingX, read from the real class.
public class HighlighterFactoryParityTest {

    @Test
    public void colorConstantsMatchSwingX() throws Exception {
        Class<?> real = Class.forName("org.jdesktop.swingx.decorator.HighlighterFactory");
        int checked = 0;
        Field[] ours = HighlighterFactory.class.getFields();
        for (int i = 0; i < ours.length; i++) {
            Field f = ours[i];
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Color.class) {
                continue;
            }
            Object theirs = real.getField(f.getName()).get(null);
            int rgb = ((Integer) theirs.getClass().getMethod("getRGB").invoke(theirs)).intValue();
            assertEquals(f.getName(), Integer.toHexString(rgb), Integer.toHexString(((Color) f.get(null)).getRGB()));
            checked++;
        }
        assertTrue(checked >= 8);
    }
}
