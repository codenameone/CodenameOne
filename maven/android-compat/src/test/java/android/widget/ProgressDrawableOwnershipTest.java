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
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

import android.graphics.drawable.ColorDrawable;
public class ProgressDrawableOwnershipTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void replacingEitherDrawableDetachesThePreviousOwner() {
        ProgressBar bar = new ProgressBar(AndroidTestSupport.context());
        ColorDrawable first = new ColorDrawable(0xff000000);
        ColorDrawable next = new ColorDrawable(0xffffffff);
        bar.setProgressDrawable(first);
        bar.setProgressDrawable(next);
        assertNull(first.getCallback());
        assertSame(bar, next.getCallback());
        bar.setIndeterminateDrawable(first);
        bar.setIndeterminateDrawable(null);
        assertNull(first.getCallback());
        bar.setProgressDrawable(null);
        assertNull(next.getCallback());
    }
    @Test public void drawableStillUsedByOtherSlotKeepsItsCallback() {
        ProgressBar bar = new ProgressBar(AndroidTestSupport.context());
        ColorDrawable shared = new ColorDrawable(0xff000000);
        bar.setProgressDrawable(shared);
        bar.setIndeterminateDrawable(shared);
        bar.setProgressDrawable(null);
        assertSame(bar, shared.getCallback());
        bar.setIndeterminateDrawable(null);
        assertNull(shared.getCallback());
    }
}
