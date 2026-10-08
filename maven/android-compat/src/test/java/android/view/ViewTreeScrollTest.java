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
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViewTreeScrollTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void descendantAndRootScrollNotifyTheSharedObserverOnlyOnChanges() {
        FrameLayout root = new FrameLayout(AndroidTestSupport.context());
        FrameLayout nested = new FrameLayout(AndroidTestSupport.context());
        View child = new View(AndroidTestSupport.context());
        root.addView(nested);
        nested.addView(child);
        final int[] changes = {0};
        child.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override public void onScrollChanged() { changes[0]++; }
        });
        child.scrollTo(3, 4);
        assertEquals(1, changes[0]);
        child.scrollTo(3, 4);
        assertEquals(1, changes[0]);
        nested.scrollBy(0, 10);
        root.scrollTo(5, 0);
        assertEquals(3, changes[0]);
    }
}
