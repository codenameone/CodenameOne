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
package com.codenameone.examples.bazaar.shop;

import com.codename1.appkit.Net;
import com.codename1.appkit.Skeleton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.SwipeableContainer;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CartLineDto;
import com.codenameone.examples.bazaar.api.CodeDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.SignInGate;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The cart of the current store: its lines (swipe one away, or change how
/// many), a discount code and a gift card, and the summary as the server
/// priced it. Lines on the left and the summary on the right on a wide
/// screen.
final class CartPane extends ShopPane {
    private final boolean wide = Layouts.wide();
    private boolean busy;

    CartPane() {
        super(new BorderLayout());
        setName("shop-cart");
        add(BorderLayout.CENTER, new Skeleton(5, "SvSkeleton"));
        reload();
    }

    @Override
    void reload() {
        if (ShopState.store() == null) {
            draw();
            return;
        }
        ShopState.loadCart(this::draw);
    }

    @Override
    void cartChanged() {
        draw();
    }

    private void draw() {
        removeAll();
        CartDto cart = ShopState.cart();
        if (ShopState.store() == null || cart == null || cart.lines() == null
                || cart.lines().isEmpty()) {
            Container page = Ui.page();
            page.add(Ui.empty(FontImage.MATERIAL_SHOPPING_CART, "Your cart is empty",
                    "Things you add to your cart show up here."));
            page.add(FlowLayout.encloseCenter(Ui.primary("Start shopping", "start-shopping",
                    e -> ShopSections.show(Sections.HOME))));
            add(BorderLayout.CENTER, page);
            Ui.relayout(this);
            return;
        }
        Container lines = new Container(BoxLayout.y());
        lines.setName("cart-lines");
        if (cart.problems() != null) {
            for (String problem : cart.problems()) {
                lines.add(Ui.callout(FontImage.MATERIAL_WARNING, "Check your cart", problem));
            }
        }
        for (CartLineDto line : cart.lines()) {
            lines.add(line(line));
        }
        Container summary = summary(cart);
        if (wide) {
            Container split = new Container(new GridLayout(1, 2));
            Container left = Ui.page();
            left.add(Ui.label(Lang.tr("Cart ({0})", String.valueOf(ShopState.units())), "SvTitle"));
            left.add(lines);
            Container right = Ui.page();
            right.add(summary);
            split.add(left).add(right);
            add(BorderLayout.CENTER, split);
        } else {
            Container page = Ui.page();
            // Every store has a cart of its own: say whose this is.
            page.add(Ui.label(Lang.tr("Cart ({0})", String.valueOf(ShopState.units())), "SvTitle"));
            Label from = Ui.plain(Lang.tr("From {0}", ShopState.store().name()), "SvMuted");
            from.setName("cart-store");
            page.add(from);
            page.add(lines);
            page.add(summary);
            Ui.pull(page, this::reload);
            add(BorderLayout.CENTER, page);
        }
        Ui.relayout(this);
    }

    private Component line(final CartLineDto line) {
        Container row = new Container(new BorderLayout());
        row.setUIID("SvCartLine");
        int side = CN.convertToPixels(16);
        Label art = ProductArt.label(line.productId(), line.title(), line.title(),
                line.photoId(), side, side, "ShopThumb");
        row.add(BorderLayout.WEST, art);
        Container words = new Container(BoxLayout.y());
        words.add(Ui.plain(line.title(), "SvRowTitle"));
        if (line.variantTitle() != null && line.variantTitle().length() > 0
                && !"Default".equalsIgnoreCase(line.variantTitle())) {
            words.add(Ui.plain(line.variantTitle(), "SvRowDetail"));
        }
        if (!line.available()) {
            words.add(Ui.plain(line.availableQty() > 0
                    ? Lang.tr("Only {0} left", String.valueOf(line.availableQty()))
                    : Lang.tr("Sold out"), "ShopStockOut"));
        }
        Container stepper = new Container(BoxLayout.x());
        stepper.setUIID("SvStepper");
        Button less = new Button("", "ShopStepButton");
        less.setName("line-less-" + line.id());
        FontImage.setMaterialIcon(less, line.quantity() <= 1 ? FontImage.MATERIAL_DELETE
                : FontImage.MATERIAL_REMOVE, 2.8f);
        Button more = new Button("", "ShopStepButton");
        more.setName("line-more-" + line.id());
        FontImage.setMaterialIcon(more, FontImage.MATERIAL_ADD, 2.8f);
        less.addActionListener(e -> change(line, line.quantity() - 1));
        more.addActionListener(e -> change(line, line.quantity() + 1));
        stepper.add(less).add(new Label(String.valueOf(line.quantity()), "SvQuantity")).add(more);
        words.add(FlowLayout.encloseIn(stepper));
        row.add(BorderLayout.CENTER, words);
        Container prices = new Container(BoxLayout.y());
        prices.add(Ui.plain(ShopState.money(line.priceCents() * line.quantity()), "SvRowValue"));
        if (line.discountCents() > 0) {
            prices.add(Ui.plain(ShopState.money(-line.discountCents()), "SvDiscount"));
        }
        row.add(BorderLayout.EAST, prices);
        RowMenus.attach(row, new Command[] {
            Command.create(Lang.tr("Remove"), null, e -> remove(line))
        });
        if (CN.isDesktop()) {
            // A desktop has no swipe: the menu above, and a delete key on the
            // stepper.
            return row;
        }
        Button delete = Ui.danger("Remove", "line-remove-" + line.id(), e -> remove(line));
        FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE, 3.2f);
        SwipeableContainer swipe = new SwipeableContainer(null, delete, row);
        swipe.setName("line-" + line.id());
        return swipe;
    }

    private void change(CartLineDto line, int quantity) {
        if (quantity <= 0) {
            remove(line);
            return;
        }
        if (busy || ShopState.store() == null) {
            return;
        }
        busy = true;
        ShopUi.tap();
        Api.cart().update(ShopState.store().id(), line.id(), ShopState.token(),
                new CartLineChangeDto(line.variantId(), quantity), Net.to(cart -> {
                    busy = false;
                    ShopState.setCart(cart);
                }, (status, message) -> {
                    busy = false;
                    Ui.fail(status, message);
                }));
    }

    private void remove(CartLineDto line) {
        if (ShopState.store() == null) {
            return;
        }
        ShopUi.tap();
        Api.cart().remove(ShopState.store().id(), line.id(), ShopState.token(),
                Net.to(ShopState::setCart, Ui::fail));
    }

    private Container summary(final CartDto cart) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("ShopSummary");
        box.setName("cart-summary");
        codes(box, cart);
        Ui.section(box, "Summary");
        rows(box, cart);
        box.add(Ui.primary("Checkout", "checkout", e -> checkout()));
        return box;
    }

    /// The checkout is the first place a guest is asked to sign in. The cart
    /// they filled is merged into the account's, and the checkout opens on it.
    static void checkout() {
        SignInGate.require(CN.getCurrentForm(), "Sign in to check out",
                "Your cart comes with you. Next time your address and card are already "
                + "filled in, so checking out takes a tap.",
                () -> ShopState.loadCart(CheckoutForm::show));
    }

    /// The summary lines of `cart`, as [Estimate] decides them.
    static void rows(Container box, CartDto cart) {
        List<Estimate.Row> rows = Estimate.rows(cart);
        for (Estimate.Row row : rows) {
            String caption = Lang.tr(row.caption);
            if (row.detail != null && row.detail.length() > 0) {
                caption += " (" + row.detail + ")";
            }
            String value = row.kind == Estimate.NOTE ? Lang.tr(row.note) : ShopState.money(row.cents);
            String uiid = row.kind == Estimate.TOTAL ? "SvTotal"
                    : row.kind == Estimate.SAVING ? "SvDiscount" : "SvFactValue";
            box.add(Ui.fact(caption, value, uiid));
        }
    }

    private void codes(Container box, final CartDto cart) {
        final String storeId = cart.storeId();
        if (cart.discountCode() != null && cart.discountCode().length() > 0) {
            box.add(applied(Lang.tr("Code {0}", cart.discountCode()), "discount-remove",
                    () -> Api.cart().removeDiscount(storeId, ShopState.token(),
                            Net.to(ShopState::setCart, Ui::fail))));
        } else {
            box.add(codeField("Discount code", "discount", (code, done) ->
                    Api.cart().applyDiscount(storeId, ShopState.token(), new CodeDto(code),
                            Net.to(c -> {
                                done.run();
                                ShopState.setCart(c);
                                Ui.say("Discount applied");
                            }, (status, message) -> {
                                done.run();
                                Ui.fail(status, message);
                            }))));
        }
        if (cart.giftCardCode() != null && cart.giftCardCode().length() > 0) {
            box.add(applied(Lang.tr("Gift card {0}", cart.giftCardCode()), "giftcard-remove",
                    () -> Api.cart().removeGiftCard(storeId, ShopState.token(),
                            Net.to(ShopState::setCart, Ui::fail))));
        } else {
            box.add(codeField("Gift card", "giftcard", (code, done) ->
                    Api.cart().applyGiftCard(storeId, ShopState.token(), new CodeDto(code),
                            Net.to(c -> {
                                done.run();
                                ShopState.setCart(c);
                                Ui.say("Gift card applied");
                            }, (status, message) -> {
                                done.run();
                                Ui.fail(status, message);
                            }))));
        }
    }

    private interface Apply {
        void apply(String code, Runnable done);
    }

    private static Container codeField(String caption, String name, final Apply apply) {
        Container row = new Container(new BorderLayout());
        row.setUIID("ShopCodeRow");
        final TextField field = new TextField("", Lang.tr(caption), 16, TextField.ANY);
        field.setUIID("SvField");
        field.setName(name + "-code");
        final Button go = Ui.secondary("Apply", name + "-apply", null);
        Runnable submit = () -> {
            String code = field.getText().trim();
            if (code.length() == 0) {
                return;
            }
            go.setEnabled(false);
            apply.apply(code, () -> go.setEnabled(true));
        };
        go.addActionListener(e -> submit.run());
        // Enter applies it.
        field.setDoneListener(e -> submit.run());
        row.add(BorderLayout.CENTER, field);
        row.add(BorderLayout.EAST, go);
        return row;
    }

    private static Container applied(String text, String name, final Runnable remove) {
        Container row = new Container(new BorderLayout());
        row.setUIID("ShopCodeRow");
        Label chip = Ui.chip(text, FontImage.MATERIAL_LOCAL_OFFER);
        row.add(BorderLayout.CENTER, FlowLayout.encloseIn(chip));
        Button x = new Button("", "ShopIconButton");
        x.setName(name);
        FontImage.setMaterialIcon(x, FontImage.MATERIAL_CLOSE, 3f);
        x.addActionListener(e -> remove.run());
        row.add(BorderLayout.EAST, x);
        return row;
    }
}
