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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.events.ActionEvent;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `loadDataWithBaseURL` names its page by `historyUrl`, as Android does. The
/// parameter used to be dropped, so the page was reported by its base URL,
/// also after navigating away and back to it.
public class WebViewHistoryUrlTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final String BASE = "https://example.com/app/";
    private static final String HISTORY = "https://example.com/history";

    @Test
    public void thePageIsReportedByItsHistoryUrl() {
        WebView w = new WebView(AndroidTestSupport.context());
        final List<String> finished = new ArrayList<String>();
        w.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                finished.add(url);
            }
        });
        w.loadDataWithBaseURL(BASE, "<p>hi</p>", "text/html", "utf-8", HISTORY);
        assertEquals(HISTORY, w.getUrl());

        BrowserComponent b = w.getBrowserComponent();
        b.fireWebEvent(BrowserComponent.onLoad, new ActionEvent(BASE));
        assertEquals(HISTORY, w.getUrl());

        // Away to another page and back to the generated one.
        b.fireWebEvent(BrowserComponent.onLoad, new ActionEvent("https://example.com/other"));
        assertEquals("https://example.com/other", w.getUrl());
        b.fireWebEvent(BrowserComponent.onLoad, new ActionEvent(BASE));
        assertEquals(HISTORY, w.getUrl());
        assertEquals("[" + HISTORY + ", https://example.com/other, " + HISTORY + "]", finished.toString());
    }

    @Test
    public void withoutAHistoryUrlTheBaseIsReported() {
        WebView w = new WebView(AndroidTestSupport.context());
        w.loadDataWithBaseURL(BASE, "<p>hi</p>", "text/html", "utf-8", null);
        assertEquals(BASE, w.getUrl());
    }
}
