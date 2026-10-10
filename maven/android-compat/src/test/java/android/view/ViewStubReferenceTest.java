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
import android.content.Context;
import android.widget.FrameLayout;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
public class ViewStubReferenceTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void detachedInflationIsHeldWeaklyAndLiveVisibilityStillForwards() throws Exception {
        final Context c = AndroidTestSupport.context();
        FrameLayout parent = new FrameLayout(c);
        ViewStub stub = new ViewStub(c,1);
        stub.setLayoutInflater(new LayoutInflater(c) {
            @Override public LayoutInflater cloneInContext(Context context) { return this; }
            @Override public View inflate(int resource, ViewGroup root, boolean attach) { return new View(c); }
        });
        parent.addView(stub);
        View inflated = stub.inflate(); stub.setVisibility(View.INVISIBLE);
        assertEquals(View.INVISIBLE,inflated.getVisibility()); parent.removeView(inflated);
        java.lang.reflect.Field f = ViewStub.class.getDeclaredField("mInflated"); f.setAccessible(true);
        assertTrue("stub must not retain the detached subtree", f.get(stub) instanceof java.lang.ref.WeakReference);
        java.lang.ref.WeakReference<?> ref = (java.lang.ref.WeakReference<?>)f.get(stub);
        assertSame(inflated,ref.get()); ref.clear();
        try { stub.setVisibility(View.VISIBLE); fail("collected target"); }
        catch (IllegalStateException expected) { }
    }
    @Test public void unsupportedInflaterPolicyFailsBeforeInflation() {
        LayoutInflater inflater = LayoutInflater.from(AndroidTestSupport.context()).cloneInContext(AndroidTestSupport.context());
        inflater.setFilter(null);
        try {
            inflater.setFilter(new LayoutInflater.Filter() {
                @Override public boolean onLoadClass(Class clazz) { return false; }
            });
            fail("an unsupported class policy must not be silently ignored");
        } catch (UnsupportedOperationException expected) { }
        assertNull(inflater.getFilter());
    }

}
