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
package android.webkit;

import android.content.Context;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `canGoBackOrForward` answers only what the browser can confirm, which is
/// one entry in each direction. It used to answer any distance from whether
/// a single entry existed, enabling controls for positions that may not.
public class WebViewHistoryDistanceTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// A web view with one page behind it and one ahead.
    static final class OneEachWay extends WebView {
        OneEachWay(Context c) {
            super(c);
        }

        @Override
        public boolean canGoBack() {
            return true;
        }

        @Override
        public boolean canGoForward() {
            return true;
        }
    }

    @Test
    public void onlyASingleStepCanBeConfirmed() {
        WebView w = new OneEachWay(AndroidTestSupport.context());
        assertTrue(w.canGoBackOrForward(0));
        assertTrue(w.canGoBackOrForward(-1));
        assertTrue(w.canGoBackOrForward(1));
        assertFalse(w.canGoBackOrForward(-2));
        assertFalse(w.canGoBackOrForward(2));
    }
}
