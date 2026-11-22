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
package com.codenameone.examples.bazaar.pos;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.Section;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.Shell;
import com.codenameone.examples.bazaar.ui.Ui;

/// Puts the point of sale into the POS mode's sections -- Register (sell),
/// Shift, Returns and Outbox -- and gives the merchant side's "Point of
/// sale" section a way in.
public final class PosSections {
    public static final String SHIFT = "pos-shift";
    public static final String RETURNS = "pos-returns";
    public static final String OUTBOX = "pos-outbox";
    public static final String PARKED = "pos-parked";
    public static final String LAYAWAYS = "pos-layaways";
    public static final String EDITS = "pos-order-edit";
    public static final String PICKUPS = "pos-pickups";
    public static final String SHIP = "pos-ship";
    public static final String DEVICES = "pos-devices";

    private static boolean installed;

    private PosSections() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        Sections.content(Nav.POS, Sections.REGISTER, PosHome::create);
        Sections.register(Nav.POS, new Section(SHIFT, "Shift", FontImage.MATERIAL_SCHEDULE,
                ShiftPane::create));
        Sections.register(Nav.POS, new Section(RETURNS, "Returns",
                FontImage.MATERIAL_ASSIGNMENT_RETURN, ReturnsPane::create));
        Sections.register(Nav.POS, new Section(PARKED, "Parked", FontImage.MATERIAL_PAUSE_CIRCLE,
                ParkedPane::create));
        Sections.register(Nav.POS, new Section(LAYAWAYS, "Layaways", FontImage.MATERIAL_SAVINGS,
                LayawayPane::create));
        Sections.register(Nav.POS, new Section(EDITS, "Edit order", FontImage.MATERIAL_EDIT_NOTE,
                OrderEditPane::create));
        Sections.register(Nav.POS, new Section(PICKUPS, "Pickups", FontImage.MATERIAL_STOREFRONT,
                PickupPane::create));
        Sections.register(Nav.POS, new Section(SHIP, "Ship to customer",
                FontImage.MATERIAL_LOCAL_SHIPPING, ShipPane::create));
        Sections.register(Nav.POS, new Section(DEVICES, "Devices", FontImage.MATERIAL_PRINT,
                DevicesPane::create));
        Sections.register(Nav.POS, new Section(OUTBOX, "Outbox", FontImage.MATERIAL_CLOUD_UPLOAD,
                OutboxPane::create));
        Sections.content(Nav.MERCHANT, Sections.POS, PosSections::launcher);
    }

    /// Opens section `id` of the POS mode when it is on screen.
    static void show(String id) {
        Shell shell = Shell.current();
        if (shell != null && Nav.POS.equals(shell.mode())) {
            shell.show(id);
        }
    }

    /// The merchant side's entry: what the register is and a button into it.
    private static Component launcher() {
        Container page = Ui.page();
        page.setName("pos-launcher");
        page.add(Ui.empty(FontImage.MATERIAL_POINT_OF_SALE, "Point of sale",
                "Ring up sales at the counter, even with the network down."));
        if (PosState.hasRegister()) {
            page.add(Ui.fact("Register", PosState.storeName() + Ui.DOT + PosState.registerName()));
        }
        page.add(Ui.primary("Open the register", "pos-open", e -> Nav.switchTo(Nav.POS)));
        Container wrap = new Container(new BorderLayout());
        wrap.add(BorderLayout.CENTER, page);
        return wrap;
    }
}
