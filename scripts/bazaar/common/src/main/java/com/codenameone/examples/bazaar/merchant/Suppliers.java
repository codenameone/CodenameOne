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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.SupplierDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;

/// The people the store buys stock from.
final class Suppliers extends Container {
    private final Container list = new Container(BoxLayout.y());

    Suppliers(InventoryPane pane) {
        super(new BorderLayout());
        setName("suppliers");
        Container page = MUi.page("suppliers-page");
        page.add(MUi.buttons(Ui.primary(Lang.tr("Add supplier"), "supplier-add", e -> edit(null))));
        list.setUIID("MeCard");
        page.add(list);
        add(BorderLayout.CENTER, page);
        reload();
    }

    private void reload() {
        Api.inventory().suppliers(MerchantState.storeId(), Net.to(found -> {
            list.removeAll();
            for (final SupplierDto s : found) {
                Container tap = Ui.tap(Ui.row(FontImage.MATERIAL_FACTORY, s.name(),
                        (s.contact() == null ? "" : s.contact()) + (s.email() == null ? ""
                        : Ui.DOT + s.email()), null), "supplier-" + s.id(), e -> edit(s));
                RowMenus.attach(tap, Command.create(Lang.tr("Delete"), null, e -> Api.inventory()
                        .deleteSupplier(MerchantState.storeId(), s.id(), Net.to(x -> reload(), Ui::fail))));
                list.add(tap);
            }
            if (found.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_FACTORY, "No suppliers",
                        "Add the people you buy stock from."));
            }
            Ui.relayout(this);
        }, Ui::fail));
    }

    private void edit(SupplierDto s) {
        Container body = new Container(BoxLayout.y());
        TextField name = MUi.text(body, "Name", "supplier-name", s == null ? "" : s.name());
        TextField contact = MUi.text(body, "Contact", "supplier-contact", s == null ? "" : s.contact());
        TextField email = MUi.text(body, "Email", "supplier-email", s == null ? "" : s.email());
        TextField phone = MUi.text(body, "Phone", "supplier-phone", s == null ? "" : s.phone());
        TextField address = MUi.text(body, "Address", "supplier-address", s == null ? "" : s.address());
        TextField note = MUi.text(body, "Note", "supplier-note", s == null ? "" : s.note());
        Command ok = Command.create(Lang.tr("Save"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Supplier"), body, cancel, ok) != ok
                || name.getText().trim().length() == 0) {
            return;
        }
        Api.inventory().saveSupplier(MerchantState.storeId(), new SupplierDto(s == null ? null : s.id(),
                name.getText().trim(), contact.getText(), email.getText(), phone.getText(),
                address.getText(), note.getText()), Net.to(x -> reload(), Ui::fail));
    }
}
