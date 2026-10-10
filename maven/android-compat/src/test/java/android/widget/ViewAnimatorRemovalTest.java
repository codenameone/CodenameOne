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
package android.widget;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViewAnimatorRemovalTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    private ViewAnimator animator() {
        ViewAnimator result = new ViewAnimator(AndroidTestSupport.context());
        for (int i = 0; i < 5; i++) result.addView(new View(result.getContext()));
        result.setDisplayedChild(3);
        return result;
    }
    @Test public void removingEarlierChildKeepsTheCurrentView() {
        ViewAnimator animator = animator();
        View current = animator.getCurrentView();
        animator.removeViewAt(0);
        assertSame(current, animator.getCurrentView());
        assertEquals(2, animator.getDisplayedChild());
        assertEquals(View.VISIBLE, current.getVisibility());
        animator.showNext();
        assertEquals(View.GONE, current.getVisibility());
        assertEquals(3, animator.getDisplayedChild());
    }
    @Test public void batchRemovalAdjustsIndexAndHandlesRemovingCurrentView() {
        ViewAnimator animator = animator();
        View current = animator.getCurrentView();
        animator.removeViews(0, 2);
        assertSame(current, animator.getCurrentView());
        assertEquals(1, animator.getDisplayedChild());
        animator.removeViews(1, 2);
        assertEquals(0, animator.getDisplayedChild());
        assertEquals(View.VISIBLE, animator.getCurrentView().getVisibility());
        animator.removeViews(0, 1);
        assertNull(animator.getCurrentView());
        assertEquals(0, animator.getDisplayedChild());
    }
}
