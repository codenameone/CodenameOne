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
import com.codename1.appkit.Skeleton;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnReceiveDto;
import com.codenameone.examples.bazaar.api.ReturnRefundDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The returns queue: what shoppers asked to send back, and each request's
/// next step -- approve or decline it, receive the parcel (back on the shelf
/// or not), and refund it.
final class ReturnsQueue extends Container {
    ReturnsQueue() {
        super(new BorderLayout());
        setName("returns-queue");
        add(BorderLayout.CENTER, new Skeleton(4, "SvSkeleton"));
        reload();
    }

    private void reload() {
        Api.returns().list(MerchantState.storeId(), Net.to(this::paint, Ui::fail));
    }

    private void paint(List<ReturnDto> list) {
        removeAll();
        Container page = MUi.page("returns-page");
        if (list.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_ASSIGNMENT_RETURN, "No returns", null));
        }
        for (int iter = 0; iter < list.size(); iter++) {
            page.add(row(list.get(iter)));
        }
        add(BorderLayout.CENTER, page);
        Ui.relayout(this);
    }

    private Container row(final ReturnDto r) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("MeCard");
        box.setName("return-" + r.orderNumber());
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.WEST, Ui.plain(Lang.tr("Order #{0}", String.valueOf(r.orderNumber()))
                + Ui.DOT + (r.username() == null ? "" : r.username()), "SvRowTitle"));
        top.add(BorderLayout.EAST, MUi.badge(r.status()));
        box.add(top);
        if (r.reason() != null && r.reason().length() > 0) {
            box.add(Ui.text(r.reason() + (r.note() == null || r.note().length() == 0 ? ""
                    : Ui.DOT + r.note()), "SvRowDetail"));
        }
        for (int iter = 0; r.lines() != null && iter < r.lines().size(); iter++) {
            FulfilLineDto l = r.lines().get(iter);
            box.add(Ui.plain(l.quantity() + " x " + l.title(), "SvRowDetail"));
        }
        if (r.refundedCents() > 0) {
            box.add(Ui.fact(Lang.tr("Refunded"), MerchantState.money(r.refundedCents())));
        }
        box.add(Ui.plain(MUi.when(r.createdAt()), "SvRowDetail"));
        Container buttons = MUi.buttons();
        String id = r.id();
        String store = MerchantState.storeId();
        if ("REQUESTED".equals(r.status())) {
            buttons.add(Ui.primary(Lang.tr("Approve"), "return-approve", e ->
                    Api.returns().approve(store, id, Net.to(x -> reload(), Ui::fail))));
            buttons.add(Ui.danger(Lang.tr("Decline"), "return-decline", e ->
                    Api.returns().decline(store, id, Net.to(x -> reload(), Ui::fail))));
        }
        if ("APPROVED".equals(r.status())) {
            buttons.add(Ui.primary(Lang.tr("Receive"), "return-receive", e -> receive(r)));
        }
        if ("RECEIVED".equals(r.status())) {
            buttons.add(Ui.primary(Lang.tr("Refund"), "return-refund", e -> refund(r)));
        }
        box.add(buttons);
        return box;
    }

    private void receive(ReturnDto r) {
        CheckBox restock = new CheckBox(Lang.tr("Put the items back in stock"));
        restock.setSelected(true);
        restock.setName("return-restock");
        Container body = BoxLayout.encloseY(restock);
        Command ok = Command.create(Lang.tr("Receive"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Receive return"), body, cancel, ok) != ok) {
            return;
        }
        Api.returns().receive(MerchantState.storeId(), r.id(),
                new ReturnReceiveDto(restock.isSelected()), Net.to(x -> reload(), Ui::fail));
    }

    private void refund(ReturnDto r) {
        Container body = new Container(BoxLayout.y());
        TextField shipping = MUi.money(body, Lang.tr("Shipping to refund"), "return-shipping", 0);
        Command ok = Command.create(Lang.tr("Refund"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Refund return"), body, cancel, ok) != ok) {
            return;
        }
        Api.returns().refund(MerchantState.storeId(), r.id(),
                new ReturnRefundDto(Math.max(0, MUi.cents(shipping))), Net.to(x -> {
                    Ui.say("Refunded");
                    reload();
                }, Ui::fail));
    }
}
