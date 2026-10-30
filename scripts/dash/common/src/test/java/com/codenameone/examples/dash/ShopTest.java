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
package com.codenameone.examples.dash;

import com.codenameone.examples.dash.net.Session;

/// Buying gems through the store and spending them in the game's shop. In
/// the simulator the store purchase completes at once with a simulated
/// receipt, which the test server's receipt validator accepts.
public class ShopTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        final int gems = Session.user().gems();
        click("shopButton");
        waitForForm("shop");
        until(() -> has("buy-gems-80"), 20000, "the shop items");
        shot("shop");
        click("buy-gems-80");
        until(() -> Session.user().gems() == gems + 80, 20000, "the gems to arrive");
        waitForForm("shop");
        until(() -> has("buy-arena-frost"), 20000, "the shop items");
        click("buy-arena-frost");
        until(() -> Session.user().owned().contains("arena-frost"), 20000, "the arena to be owned");
        return true;
    }
}
