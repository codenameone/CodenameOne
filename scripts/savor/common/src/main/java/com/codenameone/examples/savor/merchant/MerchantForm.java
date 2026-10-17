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
package com.codenameone.examples.savor.merchant;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.savor.ui.Menu;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Ui;

/// The restaurant's side of the app: the orders coming in, and the menu they
/// are ordered from.
///
/// Which restaurant is the server's to say. An account is a merchant because it
/// is on a restaurant's staff, and every call this screen makes is answered for
/// that restaurant alone.
public class MerchantForm implements Nav.Home {
    private final Form form = Ui.home("Restaurant");
    private final Menu menu = new Menu(form, Nav.MERCHANT);
    private final Container page = Ui.page();

    public MerchantForm() {
        menu.item(FontImage.MATERIAL_RECEIPT_LONG, "Orders", "home", () -> { });
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        bar.add(BorderLayout.CENTER, FlowLayout.encloseLeftMiddle(
                Ui.label("Your restaurant", "SvHeading")));
        page.setSafeArea(false);
        Container screen = new Container(new BorderLayout());
        screen.setUIID("SvForm");
        screen.setSafeArea(true);
        screen.add(BorderLayout.NORTH, bar);
        screen.add(BorderLayout.CENTER, page);
        form.add(screen);
        menu.install();
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        page.removeAll();
        page.add(Ui.empty(FontImage.MATERIAL_STOREFRONT, "No orders yet",
                "New orders appear here as customers place them."));
        form.revalidate();
    }
}
