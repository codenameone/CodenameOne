/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.papercut;

import com.codenameone.examples.papercut.net.Session;

/// The demo player buys a paper for the quill with ink, uses it, and buys
/// ink coins through the simulated store.
public class ShopTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        click("signIn");
        waitForForm("signin");
        signIn(PLAYER);
        waitForForm("home");
        click("shop");
        waitForForm("shop");
        until(() -> has("buy-quill-kraft") || has("equip-quill-kraft"), 15000, "the shop");
        shot("shop");
        if (has("buy-quill-kraft")) {
            click("buy-quill-kraft");
            until(() -> has("equip-quill-kraft"), 15000, "the kraft paper to be owned");
        }
        click("equip-quill-kraft");
        until(() -> "quill-kraft".equals(Session.user().quill()), 15000, "the kraft paper in use");
        click("buy-coins-small");
        waitFor(1500);
        shot("shop-after");
        return true;
    }
}
