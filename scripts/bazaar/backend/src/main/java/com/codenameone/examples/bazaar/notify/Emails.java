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
package com.codenameone.examples.bazaar.notify;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.image.PngCodec;
import com.codename1.backend.image.RasterImage;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.qrcode.QrCode;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Refund;
import com.codenameone.examples.bazaar.domain.ReturnRequest;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.RefundRepository;
import com.codenameone.examples.bazaar.repository.ReturnRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The mail a store sends: branded HTML with a plain-text twin, in the store's
/// accent colour and with its logo, and the PDF the occasion calls for.
///
/// Everything goes through [MailSender], so it leaves over the transport
/// `cn1.mail.url` names (SMTP, SES, the log or memory; see
/// `application.properties`), is written to the outbox inside a transaction
/// and sent after it commits, and is retried when the mail server cannot be
/// reached.
///
/// No method throws: a mail that cannot be built or queued is logged, and the
/// order, refund or sale it reports stands.
@Component
public class Emails {
    /// The accent of a store that has not picked one: the brand's indigo.
    static final int DEFAULT_ACCENT = 0x4f46e5;

    private final MailSender mail;
    private final StoreRepository stores;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final CustomerRepository customers;
    private final RefundRepository refunds;
    private final StoreStaffRepository staff;
    private final FulfillmentRepository fulfillments;
    private final ReturnRequestRepository returns;
    private final ProductRepository products;
    private final SiteUrls urls;
    private final String from;

    /// Field-injected: the documents reach the fulfilment service, which
    /// reaches the order service, which tells this class what happened, so a
    /// constructor dependency would be a cycle.
    @Autowired
    private Documents documents;

    public Emails(MailSender mail, StoreRepository stores, StoreOrderRepository orders,
            OrderLineRepository lines, CustomerRepository customers, RefundRepository refunds,
            StoreStaffRepository staff, FulfillmentRepository fulfillments,
            ReturnRequestRepository returns, ProductRepository products, SiteUrls urls,
            @Value("${bazaar.mail.from:Bazaar <no-reply@bazaar.example>}") String from) {
        this.mail = mail;
        this.stores = stores;
        this.orders = orders;
        this.lines = lines;
        this.customers = customers;
        this.refunds = refunds;
        this.staff = staff;
        this.fulfillments = fulfillments;
        this.returns = returns;
        this.products = products;
        this.urls = urls;
        this.from = from.trim();
    }

    // ------------------------------------------------------------ the mails

    /// An online order was placed: its summary, with the invoice attached.
    public void orderConfirmation(String orderId) {
        try {
            send(orderConfirmationMail(orderId));
        } catch (IOException | RuntimeException failed) {
            log("order confirmation", orderId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail orderConfirmationMail(String orderId) throws IOException {
        StoreOrder o = orders.findById(orderId);
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        List<OrderLine> ls = lines.findByOrderId(orderId);
        Body b = new Body(s, "Thanks for your order");
        b.para("We have your order #" + o.getNumber() + " and are getting it ready.");
        b.lines(ls, o.getCurrency());
        b.total("Total", o.getTotalCents(), o.getCurrency());
        b.para("Your invoice is attached.");
        b.button("Track your order", track(o));
        return builder(s, o.getEmail(), "Order #" + o.getNumber() + " confirmed", b)
                .attach("invoice-" + o.getNumber() + ".pdf", "application/pdf",
                        documents.invoice(o.getStoreId(), orderId))
                .build();
    }

    /// A parcel left with its carrier, or with the store's driver.
    public void shipped(String fulfillmentId) {
        try {
            send(shippedMail(fulfillmentId));
        } catch (IOException | RuntimeException failed) {
            log("shipping confirmation", fulfillmentId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail shippedMail(String fulfillmentId) throws IOException {
        Fulfillment f = fulfillments.findById(fulfillmentId);
        StoreOrder o = f == null ? null : orders.findById(f.getOrderId());
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body b = new Body(s, "Your order is on its way");
        if (f.getTrackingNumber().length() > 0) {
            b.para("Order #" + o.getNumber() + " left with " + f.getCarrier()
                    + ". Tracking number " + f.getTrackingNumber() + ".");
        } else {
            b.para("Order #" + o.getNumber() + " is out with our own driver.");
        }
        b.button("Track your parcel", track(o));
        return builder(s, o.getEmail(), "Order #" + o.getNumber() + " has shipped", b).build();
    }

    /// A parcel arrived: the delivery notice and, beside it, a request to
    /// review what came.
    public void delivered(String fulfillmentId) {
        try {
            Mail[] both = deliveredMails(fulfillmentId);
            if (both != null) {
                send(both[0]);
                send(both[1]);
            }
        } catch (IOException | RuntimeException failed) {
            log("delivery notice", fulfillmentId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail[] deliveredMails(String fulfillmentId) throws IOException {
        Fulfillment f = fulfillments.findById(fulfillmentId);
        StoreOrder o = f == null ? null : orders.findById(f.getOrderId());
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body d = new Body(s, "Delivered");
        d.para("Order #" + o.getNumber() + " was delivered. We hope you love it.");
        d.button("See your order", track(o));
        Mail delivered = builder(s, o.getEmail(), "Order #" + o.getNumber() + " was delivered", d)
                .build();
        Body r = new Body(s, "How did we do?");
        r.para("A few words about what you bought help other shoppers choose.");
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        for (int iter = 0; iter < ls.size(); iter++) {
            Product p = products.findById(ls.get(iter).getProductId());
            if (p != null) {
                r.link("Review " + p.getTitle(), urls.absolute(null, "/s/" + enc(s.getHandle())
                        + "/products/" + enc(p.getHandle()) + "#reviews"));
            }
        }
        Mail review = builder(s, o.getEmail(), "Tell us about order #" + o.getNumber(), r)
                .build();
        return new Mail[] {delivered, review};
    }

    /// Money went back: the amount, with the credit note attached.
    public void refundIssued(String orderId) {
        try {
            send(refundMail(orderId));
        } catch (IOException | RuntimeException failed) {
            log("refund notice", orderId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail refundMail(String orderId) throws IOException {
        StoreOrder o = orders.findById(orderId);
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        List<Refund> rs = refunds.findByOrderId(orderId);
        Refund latest = null;
        for (int iter = 0; iter < rs.size(); iter++) {
            Refund r = rs.get(iter);
            if (latest == null || !r.getCreatedAt().isBefore(latest.getCreatedAt())) {
                latest = r;
            }
        }
        if (latest == null) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body b = new Body(s, "Your refund is on its way");
        b.total("Refunded", latest.getAmountCents(), o.getCurrency());
        b.para("It goes back to the way you paid for order #" + o.getNumber()
                + ". The credit note is attached.");
        return builder(s, o.getEmail(), "Refund for order #" + o.getNumber(), b)
                .attach("credit-note-" + latest.getId() + ".pdf", "application/pdf",
                        documents.creditNote(o.getStoreId(), latest.getId()))
                .build();
    }

    /// A return was approved: what to do next.
    public void returnApproved(String returnId) {
        try {
            send(returnMail(returnId));
        } catch (IOException | RuntimeException failed) {
            log("return approval", returnId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail returnMail(String returnId) throws IOException {
        ReturnRequest r = returns.findById(returnId);
        StoreOrder o = r == null ? null : orders.findById(r.getOrderId());
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body b = new Body(s, "Your return is approved");
        b.para("Send the items from order #" + o.getNumber() + " back to "
                + (s.getAddress().length() > 0 ? s.getAddress() : s.getName())
                + ". We refund you as soon as they arrive.");
        b.button("See your order", track(o));
        return builder(s, o.getEmail(), "Return approved for order #" + o.getNumber(), b)
                .build();
    }

    /// A receipt from the register, with the receipt PDF.
    public void posReceipt(String storeId, String orderId, String to) throws IOException {
        Mail m = posReceiptMail(storeId, orderId, to);
        if (m == null) {
            throw new IOException("Not an e-mail address: " + to);
        }
        send(m);
    }

    @Transactional(readOnly = true)
    private Mail posReceiptMail(String storeId, String orderId, String to) throws IOException {
        StoreOrder o = documents.order(storeId, orderId);
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        Body b = new Body(s, "Your receipt");
        b.lines(lines.findByOrderId(orderId), o.getCurrency());
        b.total("Total", o.getTotalCents(), o.getCurrency());
        b.para("Thanks for shopping with us. The receipt is attached.");
        return builder(s, to.trim(), "Your receipt from " + s.getName(), b)
                .attach("receipt-" + o.getNumber() + ".pdf", "application/pdf",
                        documents.receipt(storeId, orderId))
                .build();
    }

    /// A cart sat untouched: a reminder that opens it again.
    public void abandonedCart(Cart cart, String to) {
        try {
            send(cartMail(cart, to));
        } catch (IOException | RuntimeException failed) {
            log("cart reminder", cart.getId(), failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail cartMail(Cart cart, String to) throws IOException {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(cart.getStoreId());
        Body b = new Body(s, "You left something behind");
        b.para("Your cart at " + s.getName() + " is saved. Pick up where you left off.");
        b.button("Back to your cart", urls.appCart(null, s.getHandle(), cart.getToken()));
        return builder(s, to, "Your cart at " + s.getName() + " is waiting", b).build();
    }

    /// A gift card was issued to a customer: the code and a QR code to show
    /// at the till.
    public void giftCard(String storeId, String customerId, String code, long cents,
            String currency) {
        try {
            send(giftCardMail(storeId, customerId, code, cents, currency));
        } catch (IOException | RuntimeException failed) {
            log("gift card", customerId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail giftCardMail(String storeId, String customerId, String code, long cents,
            String currency) throws IOException {
        Customer c = customerId == null || customerId.length() == 0 ? null
                : customers.findById(customerId);
        if (c == null || !Mail.isAddress(c.getEmail())) {
            return null;
        }
        Store s = stores.findById(storeId);
        Body b = new Body(s, "A gift card for you");
        b.total("Worth", cents, currency);
        b.para("Use the code at checkout, or show the QR code at the till.");
        b.code(group(code));
        b.html.append("<p style=\"text-align:center\"><img src=\"cid:giftcard-qr\" width=\"180\" "
                + "height=\"180\" alt=\"QR code\"></p>");
        return builder(s, c.getEmail(), "Your " + s.getName() + " gift card", b)
                .inline("giftcard-qr", "image/png", qrPng(code))
                .build();
    }

    /// A variant fell to its reorder point: told to the store's owners and
    /// managers, and to the store's own address.
    public void lowStock(String storeId, String sku, int available, int reorderPoint) {
        try {
            send(lowStockMail(storeId, sku, available, reorderPoint));
        } catch (IOException | RuntimeException failed) {
            log("low stock alert", sku, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail lowStockMail(String storeId, String sku, int available, int reorderPoint)
            throws IOException {
        Store s = stores.findById(storeId);
        if (s == null) {
            return null;
        }
        List<String> to = new ArrayList<String>();
        List<StoreStaff> team = staff.findByStoreId(storeId);
        for (int iter = 0; iter < team.size(); iter++) {
            StoreStaff m = team.get(iter);
            boolean managing = "OWNER".equals(m.getRole()) || "MANAGER".equals(m.getRole());
            if (managing && Mail.isAddress(m.getUsername()) && !to.contains(m.getUsername())) {
                to.add(m.getUsername());
            }
        }
        if (Mail.isAddress(s.getEmail()) && !to.contains(s.getEmail())) {
            to.add(s.getEmail());
        }
        if (to.isEmpty()) {
            return null;
        }
        Body b = new Body(s, "Running low: " + sku);
        b.para(sku + " is down to " + available + " (reorder point " + reorderPoint + ").");
        b.button("Open inventory", urls.absolute(null, "/app/"));
        return builder(s, to.toArray(new String[to.size()]), "Low stock: " + sku, b).build();
    }

    /// The automation action `email.customer`: the merchant's own subject and
    /// message, in the store's frame.
    public void custom(String storeId, String to, String subject, String message)
            throws IOException {
        Mail m = customMail(storeId, to, subject, message);
        if (m == null) {
            throw new IOException("Not an e-mail address: " + to);
        }
        send(m);
    }

    @Transactional(readOnly = true)
    private Mail customMail(String storeId, String to, String subject, String message)
            throws IOException {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        String title = subject == null || subject.trim().length() == 0 ? "A note from "
                + s.getName() : subject.trim();
        Body b = new Body(s, title);
        b.para(message == null ? "" : message);
        b.button("Visit the store", urls.absolute(null, "/s/" + enc(s.getHandle())));
        return builder(s, to, title, b).build();
    }

    // ------------------------------------------------------------ marketing

    /// Tells a shopper that a variant they asked about is back. Answers
    /// false, sending nothing, for an address that is not one.
    public boolean backInStock(String storeId, String to, String title, String path)
            throws IOException {
        Mail m = backInStockMail(storeId, to, title, path);
        if (m == null) {
            return false;
        }
        send(m);
        return true;
    }

    @Transactional(readOnly = true)
    private Mail backInStockMail(String storeId, String to, String title, String path) {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        String subject = title + " is back in stock";
        Body b = new Body(s, subject);
        b.para("You asked us to tell you when " + title + " was back. It is, while it lasts.");
        b.button("Shop now", urls.absolute(null, path));
        b.para("This is the only message you will get about it.");
        return builder(s, to, subject, b).build();
    }

    /// One campaign message: the merchant's text, the campaign's tracked
    /// links, an open pixel and the unsubscribe link (also as a
    /// `List-Unsubscribe` header). `links` holds label and URL pairs.
    public boolean campaign(String storeId, String to, String subject, String message,
            List<String[]> links, String pixelUrl, String unsubscribeUrl) throws IOException {
        Mail m = campaignMail(storeId, to, subject, message, links, pixelUrl, unsubscribeUrl);
        if (m == null) {
            return false;
        }
        send(m);
        return true;
    }

    @Transactional(readOnly = true)
    private Mail campaignMail(String storeId, String to, String subject, String message,
            List<String[]> links, String pixelUrl, String unsubscribeUrl) {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        Body b = new Body(s, subject);
        b.para(message == null ? "" : message);
        for (int iter = 0; iter < links.size(); iter++) {
            if (iter == 0) {
                b.button(links.get(iter)[0], links.get(iter)[1]);
            } else {
                b.link(links.get(iter)[0], links.get(iter)[1]);
            }
        }
        b.link("Unsubscribe", unsubscribeUrl);
        b.pixel(pixelUrl);
        return builder(s, to, subject, b).header("List-Unsubscribe", "<" + unsubscribeUrl + ">")
                .build();
    }

    /// Asks a new subscriber to confirm the address they signed up with.
    public boolean confirmSignup(String storeId, String to, String confirmUrl) throws IOException {
        Mail m = confirmMail(storeId, to, confirmUrl);
        if (m == null) {
            return false;
        }
        send(m);
        return true;
    }

    @Transactional(readOnly = true)
    private Mail confirmMail(String storeId, String to, String confirmUrl) {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        String subject = "Confirm your subscription to " + s.getName();
        Body b = new Body(s, subject);
        b.para("Someone, hopefully you, signed this address up for news from " + s.getName()
                + ". Confirm it and we will start writing.");
        b.button("Confirm", confirmUrl);
        b.para("If it was not you, ignore this message and nothing more will come.");
        return builder(s, to, subject, b).build();
    }

    /// Sends a subscriber the code their signup earned.
    public boolean signupReward(String storeId, String to, String code) throws IOException {
        Mail m = rewardMail(storeId, to, code);
        if (m == null) {
            return false;
        }
        send(m);
        return true;
    }

    @Transactional(readOnly = true)
    private Mail rewardMail(String storeId, String to, String code) {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        String subject = "Your welcome code for " + s.getName();
        Body b = new Body(s, subject);
        b.para("Thanks for subscribing. Use this code at checkout:");
        b.code(code);
        b.button("Visit the store", urls.absolute(null, "/s/" + enc(s.getHandle())));
        return builder(s, to, subject, b).build();
    }

    // -------------------------------------------------------------- inside

    private void send(Mail m) throws IOException {
        if (m != null) {
            mail.send(m);
        }
    }

    private Mail.Builder builder(Store s, String to, String subject, Body b) {
        return builder(s, new String[] {to}, subject, b);
    }

    private Mail.Builder builder(Store s, String[] to, String subject, Body b) {
        Mail.Builder m = Mail.to(to).from(from).subject(subject).html(b.html()).text(b.text());
        if (s != null && Mail.isAddress(s.getEmail())) {
            m.replyTo(s.getEmail());
        }
        return m;
    }

    private String track(StoreOrder o) {
        return urls.absolute(null, "/track/" + o.getNumber());
    }

    private static String enc(String s) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            boolean plain = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_' || c == '.' || c == '~';
            if (plain) {
                out.append(c);
            } else {
                out.append('%').append("0123456789ABCDEF".charAt((c >> 4) & 0xf))
                        .append("0123456789ABCDEF".charAt(c & 0xf));
            }
        }
        return out.toString();
    }

    /// `ABCD EFGH ...`: a code read aloud or typed, in groups of four.
    static String group(String code) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < code.length(); iter++) {
            if (iter > 0 && iter % 4 == 0) {
                out.append(' ');
            }
            out.append(code.charAt(iter));
        }
        return out.toString();
    }

    /// The QR code of `text` as a PNG: six pixels a module, four modules of
    /// quiet zone.
    static byte[] qrPng(String text) {
        QrCode qr = QrCode.encode(text, QrCode.ECC_MEDIUM);
        int scale = 6;
        int quiet = 4;
        int n = qr.getSize();
        int side = (n + 2 * quiet) * scale;
        RasterImage img = new RasterImage(side, side);
        img.fill(0xffffffff);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (!qr.isDark(x, y)) {
                    continue;
                }
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        img.setRGB((x + quiet) * scale + dx, (y + quiet) * scale + dy, 0xff000000);
                    }
                }
            }
        }
        return PngCodec.encode(img);
    }

    static String money(long cents, String currency) {
        long abs = cents < 0 ? -cents : cents;
        String frac = Long.toString(abs % 100);
        String amount = (cents < 0 ? "-" : "") + (abs / 100) + "." + (frac.length() < 2 ? "0" : "")
                + frac;
        return currency == null || currency.length() == 0 || "USD".equals(currency) ? "$" + amount
                : amount + " " + currency;
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            switch (c) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '&': out.append("&amp;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    private static String hex(int rgb) {
        String h = Integer.toHexString(rgb & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return "#" + h;
    }

    private static void log(String what, String id, Throwable failed) {
        System.err.println("[mail] " + what + " for " + id + " was not sent: " + failed);
    }

    /// One mail's body, written as HTML (inline styles: mail clients drop
    /// style sheets) and as plain text at the same time.
    final class Body {
        final StringBuilder html = new StringBuilder();
        final StringBuilder text = new StringBuilder();
        private final Store store;
        private final String accent;

        Body(Store store, String heading) {
            this.store = store;
            this.accent = hex(store == null || store.getAccentColor() == 0 ? DEFAULT_ACCENT
                    : store.getAccentColor());
            html.append("<h1 style=\"font-size:22px;margin:0 0 16px;color:#0f172a\">")
                    .append(esc(heading)).append("</h1>");
            text.append(heading).append("\n\n");
        }

        void para(String s) {
            html.append("<p style=\"margin:0 0 14px;line-height:1.5\">").append(esc(s))
                    .append("</p>");
            text.append(s).append("\n\n");
        }

        void lines(List<OrderLine> ls, String currency) {
            html.append("<table width=\"100%\" cellpadding=\"6\" style=\"border-collapse:collapse;"
                    + "margin:0 0 8px\">");
            for (int iter = 0; iter < ls.size(); iter++) {
                OrderLine l = ls.get(iter);
                String name = l.getTitle() + (l.getVariantTitle().length() > 0 ? " - "
                        + l.getVariantTitle() : "");
                String price = money(l.getPriceCents() * l.getQuantity(), currency);
                html.append("<tr style=\"border-bottom:1px solid #e2e8f0\"><td>")
                        .append(l.getQuantity()).append(" x ").append(esc(name))
                        .append("</td><td align=\"right\">").append(esc(price))
                        .append("</td></tr>");
                text.append(l.getQuantity()).append(" x ").append(name).append("  ").append(price)
                        .append('\n');
            }
            html.append("</table>");
            text.append('\n');
        }

        void total(String label, long cents, String currency) {
            String m = money(cents, currency);
            html.append("<p style=\"margin:0 0 14px;font-size:18px\"><strong>").append(esc(label))
                    .append(": ").append(esc(m)).append("</strong></p>");
            text.append(label).append(": ").append(m).append("\n\n");
        }

        void code(String code) {
            html.append("<p style=\"text-align:center;font:bold 22px monospace;letter-spacing:2px;"
                    + "padding:12px;border:2px dashed ").append(accent).append("\">")
                    .append(esc(code)).append("</p>");
            text.append(code).append("\n\n");
        }

        void button(String label, String url) {
            html.append("<p style=\"margin:20px 0\"><a href=\"").append(esc(url))
                    .append("\" style=\"background:").append(accent)
                    .append(";color:#ffffff;padding:12px 20px;border-radius:8px;"
                            + "text-decoration:none;font-weight:bold\">")
                    .append(esc(label)).append("</a></p>");
            text.append(label).append(": ").append(url).append("\n\n");
        }

        /// A 1x1 image whose fetch records the open.
        void pixel(String url) {
            html.append("<img src=\"").append(esc(url))
                    .append("\" width=\"1\" height=\"1\" alt=\"\" style=\"display:block\">");
        }

        void link(String label, String url) {
            html.append("<p style=\"margin:0 0 8px\"><a href=\"").append(esc(url))
                    .append("\" style=\"color:").append(accent).append("\">").append(esc(label))
                    .append("</a></p>");
            text.append(label).append(": ").append(url).append('\n');
        }

        String html() {
            String name = store == null ? "Bazaar" : store.getName();
            StringBuilder page = new StringBuilder();
            page.append("<!doctype html><html><body style=\"margin:0;background:#f1f5f9;"
                    + "font-family:Helvetica,Arial,sans-serif;color:#334155\">")
                    .append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
                            + "<td align=\"center\" style=\"padding:24px 12px\">")
                    .append("<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" "
                            + "style=\"background:#ffffff;border-radius:12px;overflow:hidden\">")
                    .append("<tr><td style=\"background:").append(accent)
                    .append(";padding:18px 24px;color:#ffffff;font-size:20px;font-weight:bold\">");
            if (store != null && store.getLogoPhotoId().length() > 0) {
                page.append("<img src=\"").append(esc(urls.absolute(null, "/media/"
                        + store.getLogoPhotoId()))).append("\" height=\"32\" alt=\"\" "
                        + "style=\"vertical-align:middle;margin-right:10px;border-radius:6px\">");
            }
            page.append(esc(name)).append("</td></tr><tr><td style=\"padding:24px\">")
                    .append(html).append("</td></tr><tr><td style=\"padding:16px 24px;"
                            + "font-size:12px;color:#64748b;border-top:1px solid #e2e8f0\">")
                    .append(esc(name));
            if (store != null && store.getAddress().length() > 0) {
                page.append(" &middot; ").append(esc(store.getAddress()));
            }
            page.append("</td></tr></table></td></tr></table></body></html>");
            return page.toString();
        }

        String text() {
            String name = store == null ? "Bazaar" : store.getName();
            return text.toString() + "-- \n" + name
                    + (store != null && store.getAddress().length() > 0 ? ", " + store.getAddress()
                    : "") + "\n";
        }
    }
}
