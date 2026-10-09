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

import android.util.SparseArray;

import com.codename1.androidcompat.testing.MainThreadRule;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/// `deepCopy()` shares no mutable container with the original; it used to
/// be the shallow copy constructor, so editing a nested bundle, list or array
/// through the copy edited the original.
public class BundleDeepCopyTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void nestedContainersAreCopied() {
        Bundle inner = new Bundle();
        inner.putString("k", "orig");
        ArrayList<String> list = new ArrayList<String>();
        list.add("orig");
        Bundle listed = new Bundle();
        listed.putInt("n", 1);
        ArrayList<Bundle> bundles = new ArrayList<Bundle>();
        bundles.add(listed);
        Bundle b = new Bundle();
        b.putBundle("inner", inner);
        b.putStringArrayList("list", list);
        b.putParcelableArrayList("bundles", bundles);
        b.putIntArray("ints", new int[] {1, 2});
        b.putStringArray("strings", new String[] {"orig"});
        b.putString("plain", "shared");

        Bundle copy = b.deepCopy();
        copy.getBundle("inner").putString("k", "changed");
        copy.getStringArrayList("list").set(0, "changed");
        copy.<Bundle>getParcelableArrayList("bundles").get(0).putInt("n", 2);
        copy.getIntArray("ints")[0] = 9;
        copy.getStringArray("strings")[0] = "changed";

        assertEquals("orig", inner.getString("k"));
        assertEquals("orig", list.get(0));
        assertEquals(1, listed.getInt("n"));
        assertArrayEquals(new int[] {1, 2}, b.getIntArray("ints"));
        assertArrayEquals(new String[] {"orig"}, b.getStringArray("strings"));
        assertSame(b.getString("plain"), copy.getString("plain"));
    }

    @Test
    public void sparseParcelableContainersAndNestedBundlesAreCopied() {
        Bundle nested = new Bundle();
        nested.putInt("value", 1);
        Parcelable ordinary = new Parcelable() {
            public int describeContents() { return 0; }
            public void writeToParcel(Parcel dest, int flags) { }
        };
        SparseArray<Parcelable> values = new SparseArray<Parcelable>();
        values.put(3, nested);
        values.put(8, ordinary);
        values.put(10, null);
        Bundle original = new Bundle();
        original.putSparseParcelableArray("values", values);

        SparseArray<Parcelable> copy = original.deepCopy().getSparseParcelableArray("values");
        assertNotSame(values, copy);
        assertNotSame(nested, copy.get(3));
        assertSame(ordinary, copy.get(8));
        assertEquals(3, copy.size());
        assertEquals(10, copy.keyAt(2));
        assertNull(copy.get(10));
        ((Bundle) copy.get(3)).putInt("value", 2);
        copy.remove(8);
        copy.put(20, ordinary);
        copy.put(10, ordinary);
        assertEquals(1, nested.getInt("value"));
        assertSame(ordinary, values.get(8));
        assertNull(values.get(20));
        assertNull(values.get(10));
        assertEquals(3, values.size());
    }
}
