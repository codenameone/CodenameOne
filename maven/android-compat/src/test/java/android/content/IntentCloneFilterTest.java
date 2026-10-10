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

import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// `cloneFilter()` copies every field intent resolution reads; the package
/// and categories were dropped, so the clone could resolve elsewhere.
public class IntentCloneFilterTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void keepsPackageAndCategoriesButNotExtras() {
        Intent src = new Intent(Intent.ACTION_VIEW);
        src.setPackage("com.example.viewer");
        src.addCategory(Intent.CATEGORY_BROWSABLE);
        src.putExtra("k", "v");
        Intent clone = src.cloneFilter();
        assertEquals("com.example.viewer", clone.getPackage());
        assertTrue(clone.hasCategory(Intent.CATEGORY_BROWSABLE));
        assertNull(clone.getStringExtra("k"));

        // The clone's category set is its own.
        clone.addCategory(Intent.CATEGORY_DEFAULT);
        assertEquals(1, src.getCategories().size());
    }
}
