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

import android.util.TypedValue;
import android.view.MenuItem;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// A menu item's `android:onClick` is routed to the build-generated
/// dispatcher when the item is selected. It used to be ignored, so selecting
/// the item did nothing. The runtime's own tests run without the generated
/// dispatcher, so the handler is reported missing -- which proves the
/// attribute reached it.
public class MenuXmlOnClickTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final XmlNode[] NONE = new XmlNode[0];

    private static XmlNode menu(XmlNode item) {
        return new XmlNode("menu", 1, new int[0], new byte[0], new String[0], new ResValue[0], null,
                new XmlNode[] {item});
    }

    @Test
    public void onClickAttributeDispatchesThroughTheBuild() {
        XmlNode item = new XmlNode("item", 2, new int[] {android.R.attr.onClick},
                new byte[] {(byte) XmlNode.NS_ANDROID}, new String[] {"onClick"},
                new ResValue[] {new ResValue(TypedValue.TYPE_STRING, 0, "picked")}, null, NONE);
        MenuImpl m = new MenuImpl(AndroidTestSupport.context());
        MenuImpl.inflate(AndroidTestSupport.context(), menu(item), m);
        MenuItem mi = m.getItem(0);
        try {
            ((MenuImpl.Item) mi).invoke();
            fail("the onClick handler was not looked up");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("picked(MenuItem)"));
        }
    }

    @Test
    public void itemWithoutOnClickIsUnhandled() {
        XmlNode item = new XmlNode("item", 2, new int[0], new byte[0], new String[0], new ResValue[0], null, NONE);
        MenuImpl m = new MenuImpl(AndroidTestSupport.context());
        MenuImpl.inflate(AndroidTestSupport.context(), menu(item), m);
        assertEquals(1, m.size());
        assertFalse(((MenuImpl.Item) m.getItem(0)).invoke());
    }
}
