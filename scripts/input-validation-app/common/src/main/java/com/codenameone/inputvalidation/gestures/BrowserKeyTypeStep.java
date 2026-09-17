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
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/** Hardware typing into a native browser peer, without a CN1 native editor (#5740). */
public final class BrowserKeyTypeStep implements GestureStep {
    @Override
    public String name() {
        return "browserkeytype";
    }

    @Override
    public void install(Container target, Callback callback) {
        BrowserComponent browser = new BrowserComponent();
        final boolean[] fired = {false};
        browser.addWebEventListener(BrowserComponent.onLoad, event -> {
            browser.addJSCallback("document.getElementById('input').oninput = function() {"
                    + "callback.onSuccess(this.value); };", value -> CN.callSerially(() -> {
                String text = value.toString();
                if (!fired[0] && text.contains(KeyTypeStep.EXPECTED_TEXT)) {
                    fired[0] = true;
                    callback.onDetected("text=" + text);
                }
            }));
        });
        // Fill the peer so the driver's screen-center tap lands in the HTML input.
        // No JavaScript writes its value: only a real input event can pass the step.
        browser.setPage("<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<style>html,body{margin:0;width:100%;height:100%}"
                + "input{box-sizing:border-box;width:100%;height:100%;font-size:24px}</style>"
                + "</head><body><input id='input' aria-label='Browser keyboard input'"
                + " autocapitalize='off' autocorrect='off' autocomplete='off' spellcheck='false'>"
                + "</body></html>", null);
        target.add(BorderLayout.CENTER, browser);
    }
}
