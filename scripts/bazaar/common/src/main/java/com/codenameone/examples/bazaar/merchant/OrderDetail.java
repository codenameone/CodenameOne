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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PayLinkDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.api.OrderEventDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.OrderNoteDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.RefundDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// One order, for the merchant: its lines and totals, the customer, a
/// timeline of what happened to it, notes and tags, and what can be done to
/// it -- mark paid, refund, cancel, and for a draft send the invoice or turn it
/// into an order. Two columns on a wide screen, one on a phone.
final class OrderDetail extends Container {
    private OrderDto order;
    private final Runnable changed;

    OrderDetail(OrderDto order, Runnable changed) {
        super(new BorderLayout());
        this.changed = changed;
        setName("order-detail");
        show(order);
    }

    private void show(OrderDto o) {
        order = o;
        removeAll();
        Container page = MUi.page("order-page");
        Container head = new Container(new BorderLayout());
        head.setUIID("MeCard");
        Container badges = new Container(BoxLayout.x());
        badges.add(MUi.badge(o.status()));
        badges.add(MUi.badge(o.financial()));
        badges.add(MUi.badge(o.fulfilment()));
        badges.add(Ui.badge(Lang.tr(Words.of(o.channel())), "SvBadge"));
        head.add(BorderLayout.NORTH, Ui.plain(Lang.tr("Order #{0}", String.valueOf(o.number()))
                + Ui.DOT + MUi.when(o.createdAt()), "SvHeading"));
        head.add(BorderLayout.CENTER, badges);
        Container south = new Container(BoxLayout.y());
        south.add(actions(o));
        south.add(documents(o));
        head.add(BorderLayout.SOUTH, south);
        page.add(head);

        Container columns = new Container(Layouts.wide() ? new GridLayout(1, 2) : BoxLayout.y());
        Container left = new Container(BoxLayout.y());
        Container right = new Container(BoxLayout.y());
        columns.add(left);
        columns.add(right);
        page.add(columns);

        Ui.section(left, "Items");
        Container lines = new Container(BoxLayout.y());
        lines.setUIID("MeCard");
        for (int iter = 0; o.lines() != null && iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            Container row = new Container(new BorderLayout());
            row.setUIID("MeListRow");
            row.add(BorderLayout.WEST, ProductArt.label(l.productId(), l.title(), null, l.photoId(),
                    com.codename1.ui.CN.convertToPixels(9f), com.codename1.ui.CN.convertToPixels(9f),
                    "MeThumb"));
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(l.title(), "SvRowTitle"));
            String detail = (l.variantTitle() == null || "Default".equals(l.variantTitle())
                    ? "" : l.variantTitle() + Ui.DOT) + (l.sku() == null ? "" : l.sku());
            words.add(Ui.plain(detail, "SvRowDetail"));
            if (l.refundedQty() > 0 || l.fulfilledQty() > 0) {
                words.add(Ui.plain(Lang.tr("{0} fulfilled, {1} refunded",
                        String.valueOf(l.fulfilledQty()), String.valueOf(l.refundedQty())),
                        "SvRowDetail"));
            }
            row.add(BorderLayout.CENTER, words);
            row.add(BorderLayout.EAST, Ui.plain(l.quantity() + " x "
                    + MerchantState.money(l.priceCents()), "SvRowValue"));
            lines.add(row);
        }
        left.add(lines);
        Container totals = new Container(BoxLayout.y());
        totals.setUIID("MeCard");
        totals.add(Ui.fact(Lang.tr("Subtotal"), MerchantState.money(o.subtotalCents())));
        if (o.discountCents() > 0) {
            totals.add(Ui.fact(Lang.tr("Discount") + (o.discountCode() == null ? ""
                    : " (" + o.discountCode() + ")"), "-" + MerchantState.money(o.discountCents())));
        }
        totals.add(Ui.fact(Lang.tr("Shipping"), MerchantState.money(o.shippingCents())));
        totals.add(Ui.fact(Lang.tr("Tax"), MerchantState.money(o.taxCents())));
        totals.add(Ui.fact(Lang.tr("Total"), MerchantState.money(o.totalCents()), "SvFactTotal"));
        if (o.tipCents() > 0) {
            totals.add(Ui.fact(Lang.tr("Tip"), MerchantState.money(o.tipCents())));
        }
        if (o.fields() != null) {
            for (int iter = 0; iter < o.fields().size(); iter++) {
                totals.add(Ui.fact(o.fields().get(iter).label(), o.fields().get(iter).value()));
            }
        }
        if (o.refundedCents() > 0) {
            totals.add(Ui.fact(Lang.tr("Refunded"), "-" + MerchantState.money(o.refundedCents())));
        }
        left.add(totals);

        Ui.section(right, "Customer");
        Container customer = new Container(BoxLayout.y());
        customer.setUIID("MeCard");
        customer.add(Ui.plain(OrdersPane.customer(o), "SvRowTitle"));
        if (o.email() != null) {
            customer.add(Ui.plain(o.email(), "SvRowDetail"));
        }
        if (o.address() != null) {
            customer.add(Ui.text(o.address().line1() + ", " + o.address().city() + " "
                    + o.address().postal() + ", " + o.address().country(), "SvRowDetail"));
        }
        if (o.shippingTitle() != null) {
            customer.add(Ui.plain(o.shippingTitle(), "SvRowDetail"));
        }
        right.add(customer);

        Ui.section(right, "Notes and tags");
        Container notes = new Container(BoxLayout.y());
        notes.setUIID("MeCard");
        final TextArea note = new TextArea(o.note() == null ? "" : o.note(), 3, 30);
        note.setUIID("SvField");
        note.setName("order-note");
        notes.add(note);
        final TextField tags = MUi.text(notes, "Tags", "order-tags", o.tags());
        notes.add(MUi.cardButtons(Ui.secondary(Lang.tr("Save note"), "order-note-save", e ->
                Api.orders().note(MerchantState.storeId(), o.id(),
                        new OrderNoteDto(note.getText(), tags.getText()), Net.to(saved -> {
                            Ui.say("Saved");
                            show(saved);
                        }, Ui::fail)))));
        right.add(notes);

        Ui.section(right, "Timeline");
        Container timeline = new Container(BoxLayout.y());
        timeline.setUIID("MeCard");
        timeline.setName("order-timeline");
        List<OrderEventDto> events = o.events() == null ? new ArrayList<OrderEventDto>() : o.events();
        for (int iter = events.size() - 1; iter >= 0; iter--) {
            OrderEventDto ev = events.get(iter);
            Container row = new Container(new BorderLayout());
            row.setUIID("MeTimelineRow");
            Label dot = new Label("", "MeTimelineDot");
            row.add(BorderLayout.WEST, dot);
            Container words = new Container(BoxLayout.y());
            words.add(Ui.text(ev.message(), "SvText"));
            words.add(Ui.plain(MUi.when(ev.createdAt()) + (ev.username() == null ? ""
                    : Ui.DOT + ev.username()), "SvRowDetail"));
            row.add(BorderLayout.CENTER, words);
            timeline.add(row);
        }
        right.add(timeline);
        add(BorderLayout.CENTER, page);
        Ui.relayout(this);
    }

    private Container actions(final OrderDto o) {
        Container row = new Container(new com.codename1.ui.layouts.FlowLayout());
        boolean cancelled = "CANCELLED".equals(o.status());
        if ("DRAFT".equals(o.channel()) && "OPEN".equals(o.status())) {
            row.add(Ui.primary(Lang.tr("Send invoice"), "draft-invoice", e ->
                    Api.orders().sendInvoice(MerchantState.storeId(), o.id(), Net.to(sent -> {
                        Ui.say("Invoice sent");
                        updated(sent);
                    }, Ui::fail))));
            row.add(Ui.secondary(Lang.tr("Create order"), "draft-convert", e ->
                    Api.orders().convertDraft(MerchantState.storeId(), o.id(), Net.to(this::updated,
                            Ui::fail))));
        }
        if (!cancelled && "DRAFT".equals(o.channel())
                && ("OPEN".equals(o.status()) || "PENDING".equals(o.financial()))) {
            row.add(Ui.secondary(Lang.tr("Send pay link"), "draft-pay-link", e ->
                    Api.orderTools().payLink(MerchantState.storeId(), o.id(),
                            new PayLinkRequestDto(0), Net.to(OrderDetail::payLinkSent, Ui::fail))));
        }
        if (!cancelled && ("PENDING".equals(o.financial()) || "AUTHORIZED".equals(o.financial()))) {
            row.add(Ui.secondary(Lang.tr("Mark as paid"), "order-paid", e ->
                    Api.orders().markPaid(MerchantState.storeId(), o.id(), Net.to(this::updated,
                            Ui::fail))));
        }
        if (!cancelled && o.totalCents() > o.refundedCents()
                && ("PAID".equals(o.financial()) || "PARTIALLY_REFUNDED".equals(o.financial()))) {
            row.add(Ui.secondary(Lang.tr("Refund"), "order-refund", e -> refund()));
        }
        if (!cancelled && !"FULFILLED".equals(o.fulfilment())) {
            row.add(Ui.danger(Lang.tr("Cancel order"), "order-cancel", e -> cancel(o, () -> reload())));
        }
        return row;
    }

    /// Shows a pay link the server made: the address, when it expires and
    /// whether the customer was emailed; the address can be copied.
    private static void payLinkSent(final PayLinkDto link) {
        Container body = new Container(com.codename1.ui.layouts.BoxLayout.y());
        body.add(Ui.text(link.url(), "SvText"));
        body.add(Ui.plain(Lang.tr("Expires {0}", MUi.when(link.expiresAt())), "SvRowDetail"));
        body.add(Ui.plain(Lang.tr(link.emailed() ? "Emailed to the customer"
                : "Not emailed; share the link yourself"), "SvRowDetail"));
        Command copy = Command.create(Lang.tr("Copy link"), null, null);
        Command close = Command.create(Lang.tr("OK"), null, null);
        if (Dialog.show(Lang.tr("Pay link"), body, close, copy) == copy) {
            com.codename1.ui.Display.getInstance().copyToClipboard(link.url());
            Ui.say("Link copied");
        }
    }

    /// The order's documents, rendered by the server and previewed here.
    private Container documents(final OrderDto o) {
        Container row = new Container(new com.codename1.ui.layouts.FlowLayout());
        row.setName("order-documents");
        row.add(Ui.secondary(Lang.tr("Invoice"), "order-invoice", e -> Pdfs.open("Invoice",
                "invoice-" + o.number() + ".pdf", path(o, "invoice.pdf"))));
        row.add(Ui.secondary(Lang.tr("Packing slip"), "order-slip", e -> Pdfs.open("Packing slip",
                "packing-slip-" + o.number() + ".pdf", path(o, "packing-slip.pdf"))));
        if (o.refundedCents() > 0) {
            row.add(Ui.secondary(Lang.tr("Credit notes"), "order-credit", e ->
                    Api.orders().refunds(MerchantState.storeId(), o.id(), Net.to(list -> {
                        if (list.isEmpty()) {
                            Ui.say("No refunds yet");
                            return;
                        }
                        Command[] items = new Command[list.size()];
                        for (int iter = 0; iter < list.size(); iter++) {
                            final RefundDto r = list.get(iter);
                            items[iter] = Command.create(MerchantState.money(r.amountCents())
                                    + Ui.DOT + MUi.when(r.createdAt()), null, ev -> Pdfs.open(
                                    "Credit note", "credit-note-" + o.number() + ".pdf",
                                    "/api/merchant/stores/" + MerchantState.storeId() + "/refunds/"
                                    + r.id() + "/credit-note.pdf"));
                        }
                        com.codename1.ui.ContextMenu.show(row, row.getWidth() / 2, 0, items);
                    }, Ui::fail))));
        }
        return row;
    }

    private static String path(OrderDto o, String file) {
        return "/api/merchant/stores/" + MerchantState.storeId() + "/orders/" + o.id() + "/" + file;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        DesktopActions.set(DesktopActions.PRINT, () -> Pdfs.print("invoice-" + order.number()
                + ".pdf", path(order, "invoice.pdf")));
    }

    @Override
    protected void deinitialize() {
        DesktopActions.clear(DesktopActions.PRINT);
        super.deinitialize();
    }

    private void reload() {
        Api.orders().detail(MerchantState.storeId(), order.id(), Net.to(this::updated, Ui::fail));
    }

    private void updated(OrderDto o) {
        show(o);
        if (changed != null) {
            changed.run();
        }
    }

    /// The order a cancel is in flight for, so a second tap while it is on
    /// the way does not send another.
    private static String cancelling;

    /// True while a refund is on its way; a second submit is refused until it
    /// is answered.
    private boolean refunding;

    static void cancel(final OrderDto o, final Runnable done) {
        if (o.id().equals(cancelling)) {
            return;
        }
        if (!MUi.confirm("Cancel order", "The stock is released and any payment is voided "
                + "or refunded.", "Cancel order")) {
            return;
        }
        cancelling = o.id();
        Api.orders().cancel(MerchantState.storeId(), o.id(), Net.to(cancelled -> {
            cancelling = null;
            Ui.say("Order cancelled");
            done.run();
        }, (status, message) -> {
            cancelling = null;
            Ui.fail(status, message);
        }));
    }

    /// The refund dialog: how many of each line, whether they go back on the
    /// shelf, how much shipping, and why -- with the amount it comes to.
    void refund() {
        if (refunding) {
            return;
        }
        final OrderDto o = order;
        final boolean inclusive = MerchantState.store() != null
                && MerchantState.store().taxInclusive();
        final Map<String, Integer> quantities = new HashMap<String, Integer>();
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        final Label total = new Label("", "SvTotal");
        total.setName("refund-total");
        final TextField[] shippingField = new TextField[1];
        final Runnable recalc = () -> {
            long ship = shippingField[0] == null ? 0 : MUi.cents(shippingField[0]);
            total.setText(Lang.tr("Refund {0}", MerchantState.money(
                    RefundMath.total(o, quantities, ship, inclusive))));
            total.getParent().revalidate();
        };
        for (int iter = 0; o.lines() != null && iter < o.lines().size(); iter++) {
            final OrderLineDto l = o.lines().get(iter);
            int left = RefundMath.refundable(l);
            if (left == 0) {
                continue;
            }
            Container row = new Container(new BorderLayout());
            row.setUIID("MeListRow");
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(l.title(), "SvRowTitle"));
            words.add(Ui.plain(Lang.tr("{0} of {1} left", String.valueOf(left),
                    String.valueOf(l.quantity())), "SvRowDetail"));
            row.add(BorderLayout.CENTER, words);
            final TextField qty = new TextField("0", "0", 3, TextArea.NUMERIC);
            qty.setUIID("SvField");
            qty.setName("refund-qty-" + iter);
            qty.addDataChangedListener((t, i) -> {
                quantities.put(l.id(), Integer.valueOf(MUi.integer(qty)));
                recalc.run();
            });
            row.add(BorderLayout.EAST, qty);
            body.add(row);
        }
        final CheckBox restock = new CheckBox(Lang.tr("Put the items back in stock"));
        restock.setSelected(true);
        restock.setName("refund-restock");
        body.add(restock);
        shippingField[0] = MUi.money(body, Lang.tr("Shipping to refund (up to {0})",
                MerchantState.money(o.shippingCents())), "refund-shipping", 0);
        shippingField[0].addDataChangedListener((t, i) -> recalc.run());
        final TextField reason = Ui.field(body, Lang.tr("Reason"), "refund-reason", "");
        body.add(total);
        recalc.run();
        Command ok = Command.create(Lang.tr("Refund"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Refund order #{0}", String.valueOf(o.number())), body, cancel, ok)
                != ok) {
            return;
        }
        List<RefundLineDto> lines = new ArrayList<RefundLineDto>();
        for (Map.Entry<String, Integer> e : quantities.entrySet()) {
            if (e.getValue().intValue() > 0) {
                lines.add(new RefundLineDto(e.getKey(), e.getValue().intValue()));
            }
        }
        long ship = MUi.cents(shippingField[0]);
        if (RefundMath.total(o, quantities, ship, inclusive) <= 0) {
            Ui.say("Nothing to refund");
            return;
        }
        refunding = true;
        Api.orders().refund(MerchantState.storeId(), o.id(), new RefundRequestDto(lines,
                Math.min(ship, o.shippingCents()), restock.isSelected(), reason.getText()),
                Net.to(refunded -> {
                    refunding = false;
                    Ui.say("Refunded");
                    updated(refunded);
                }, (status, message) -> {
                    refunding = false;
                    Ui.fail(status, message);
                }));
    }
}
