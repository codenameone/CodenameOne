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
package android.os;

import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// `Bundle.EMPTY` is one shared instance, so writing to it throws, as on
/// Android, instead of leaking the value to every other holder.
public class BundleEmptyImmutableTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void writingToEmptyThrows() {
        try {
            Bundle.EMPTY.putString("leak", "x");
            fail("EMPTY accepted a value");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("EMPTY"));
        }
        Bundle other = new Bundle();
        other.putInt("n", 1);
        try {
            Bundle.EMPTY.putAll(other);
            fail("EMPTY accepted putAll");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("EMPTY"));
        }
        Bundle.EMPTY.remove("leak");
        Bundle.EMPTY.clear();
        assertTrue(Bundle.EMPTY.isEmpty());
        Bundle copy = new Bundle(Bundle.EMPTY);
        copy.putString("k", "v");
        assertEquals("v", copy.getString("k"));
        assertTrue(Bundle.EMPTY.isEmpty());
    }
}
