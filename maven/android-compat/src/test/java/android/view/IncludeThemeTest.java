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

import android.widget.LinearLayout;

import com.codename1.androidcompat.runtime.ResValue;
import com.codename1.androidcompat.runtime.XmlNode;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// An `android:theme` on an `<include>` themes the included root, as on
/// Android. It used to be ignored, so the included tree resolved its styles
/// from the parent's theme.
public class IncludeThemeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final XmlNode[] NONE = new XmlNode[0];

    @Test
    public void includeThemeWrapsTheIncludedRoot() {
        int theme = android.R.style.Theme_Material;
        XmlNode include = new XmlNode("include", 2,
                new int[] {0, android.R.attr.theme},
                new byte[] {(byte) XmlNode.NS_NONE, (byte) XmlNode.NS_ANDROID},
                new String[] {"layout", "theme"},
                new ResValue[] {
                    new ResValue(android.util.TypedValue.TYPE_REFERENCE, android.R.layout.simple_list_item_1, null),
                    new ResValue(android.util.TypedValue.TYPE_REFERENCE, theme, null)
                }, null, NONE);
        XmlNode root = new XmlNode("LinearLayout", 1, new int[0], new byte[0], new String[0], new ResValue[0],
                null, new XmlNode[] {include});
        LayoutInflater inflater = LayoutInflater.from(AndroidTestSupport.context());
        View inflated = inflater.inflate(root, null, false);
        assertTrue(inflated instanceof LinearLayout);
        View included = ((LinearLayout) inflated).getChildAt(0);
        assertTrue("the included root's context is themed: " + included.getContext(),
                included.getContext() instanceof ContextThemeWrapper);
        assertEquals(theme, ((ContextThemeWrapper) included.getContext()).getThemeResId());
    }
}
