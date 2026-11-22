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
import com.codenameone.examples.bazaar.web.StoreText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private com.codenameone.examples.bazaar.catalog.Downloads downloads;
    @Autowired
    private Documents documents;

    /// The language each mail is written in. Field-injected like the
    /// documents, so the constructor stays as it was.
    @Autowired
    private ShopperLanguage language;
    @Autowired
    private Templates templates;
    /// Product names in the shopper's language, for the order lines.
    @Autowired
    private com.codenameone.examples.bazaar.market.ProductText productText;

    @Autowired
    private com.codenameone.examples.bazaar.checkout.CheckoutSettings checkoutSettings;

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
        Body b = new Body(s, language.of(o), "mail.order.title");
        b.para(b.f("mail.order.body", o.getNumber()));
        b.lines(ls, o.getCurrency());
        b.total(b.f("total"), o.getTotalCents(), o.getCurrency());
        if (o.getTipCents() > 0) {
            b.total(b.f("tip.line"), o.getTipCents(), o.getCurrency());
        }
        List<com.codenameone.examples.bazaar.api.OrderFieldDto> answers =
                checkoutSettings.answersOf(orderId);
        for (int iter = 0; iter < answers.size(); iter++) {
            b.para(answers.get(iter).label() + ": " + answers.get(iter).value());
        }
        downloadsBlock(b, orderId);
        b.para(b.f("mail.invoice"));
        b.button(b.f("mail.track"), track(o));
        return builder(s, o.getEmail(), b.f("mail.order.subject", o.getNumber()), b)
                .attach("invoice-" + o.getNumber() + ".pdf", "application/pdf",
                        documents.invoice(o.getStoreId(), orderId))
                .build();
    }

    /// The order's download links and licence keys, when it has any.
    private void downloadsBlock(Body b, String orderId) throws IOException {
        List<com.codenameone.examples.bazaar.api.DownloadGrantDto> gs =
                downloads.forOrder(orderId);
        if (gs.isEmpty()) {
            return;
        }
        b.heading(b.f("mail.downloads.heading"));
        for (int iter = 0; iter < gs.size(); iter++) {
            com.codenameone.examples.bazaar.api.DownloadGrantDto g = gs.get(iter);
            if (g.url().length() > 0) {
                b.link(g.title(), g.url());
            }
            for (int k = 0; k < g.licenseKeys().size(); k++) {
                b.para(b.f("mail.downloads.key", g.licenseKeys().get(k)));
            }
        }
        b.para(b.f("mail.downloads.limits", Integer.valueOf(gs.get(0).maxDownloads())));
    }

    /// The download links of an order whose payment came after its
    /// confirmation, or of a register sale. True when a mail went out.
    public boolean downloads(String orderId) {
        try {
            StoreOrder o = orders.findById(orderId);
            if (o == null || !Mail.isAddress(o.getEmail())) {
                return false;
            }
            Store s = stores.findById(o.getStoreId());
            Body b = new Body(s, language.of(o), "mail.downloads.title");
            b.para(b.f("mail.downloads.body", o.getNumber()));
            downloadsBlock(b, orderId);
            send(builder(s, o.getEmail(), b.f("mail.downloads.subject", o.getNumber()), b)
                    .build());
            return true;
        } catch (IOException | RuntimeException failed) {
            log("downloads", orderId, failed);
            return false;
        }
    }

    /// A checkout link for a draft or an unpaid order: the lines, the total
    /// and a button that pays it. True when a mail went out.
    public boolean payLink(String orderId, String url, long expiresAt) {
        try {
            Mail m = payLinkMail(orderId, url, expiresAt);
            send(m);
            return m != null;
        } catch (IOException | RuntimeException failed) {
            log("checkout link", orderId, failed);
            return false;
        }
    }

    @Transactional(readOnly = true)
    private Mail payLinkMail(String orderId, String url, long expiresAt) throws IOException {
        StoreOrder o = orders.findById(orderId);
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body b = new Body(s, language.of(o), "mail.paylink.title");
        b.para(b.f("mail.paylink.body", s.getName(), o.getNumber()));
        b.lines(lines.findByOrderId(orderId), o.getCurrency());
        b.total(b.f("total"), o.getTotalCents(), o.getCurrency());
        b.button(b.f("mail.paylink.button"), url);
        b.para(b.f("mail.paylink.expires", day(expiresAt)));
        return builder(s, o.getEmail(), b.f("mail.paylink.subject", o.getNumber(), s.getName()),
                b).build();
    }

    /// `yyyy-mm-dd` of an instant, in UTC.
    static String day(long millis) {
        return java.time.Instant.ofEpochMilli(millis).toString().substring(0, 10);
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
        Body b = new Body(s, language.of(o), "mail.shipped.title");
        if (f.getTrackingNumber().length() > 0) {
            b.para(b.f("mail.shipped.carrier", o.getNumber(), f.getCarrier(),
                    f.getTrackingNumber()));
        } else {
            b.para(b.f("mail.shipped.driver", o.getNumber()));
        }
        b.button(b.f("mail.shipped.button"), track(o));
        return builder(s, o.getEmail(), b.f("mail.shipped.subject", o.getNumber()), b).build();
    }

    /// An order is ready to collect at `location`.
    public void pickupReady(String fulfillmentId, String location) {
        try {
            send(pickupMail(fulfillmentId, location));
        } catch (IOException | RuntimeException failed) {
            log("pickup notice", fulfillmentId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail pickupMail(String fulfillmentId, String location) throws IOException {
        Fulfillment f = fulfillments.findById(fulfillmentId);
        StoreOrder o = f == null ? null : orders.findById(f.getOrderId());
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body b = new Body(s, language.of(o), "mail.pickup.title");
        b.para(b.f("mail.pickup.body", o.getNumber(), location));
        b.lines(lines.findByOrderId(o.getId()), o.getCurrency());
        b.button(b.f("mail.seeOrder"), track(o));
        return builder(s, o.getEmail(), b.f("mail.pickup.subject", o.getNumber(), location), b)
                .build();
    }

    /// A parcel arrived: the delivery notice, and a review request scheduled
    /// for some days later ([com.codenameone.examples.bazaar.domain.ReviewRequest]),
    /// when the shopper has had time to use what came. The request is mailed
    /// by [com.codenameone.examples.bazaar.marketing.MarketingMails].
    public void delivered(String fulfillmentId) {
        try {
            send(deliveredMail(fulfillmentId));
        } catch (IOException | RuntimeException failed) {
            log("delivery notice", fulfillmentId, failed);
        }
        try {
            scheduleReview(fulfillmentId, System.currentTimeMillis());
        } catch (IOException | RuntimeException failed) {
            log("review request", fulfillmentId, failed);
        }
    }

    @Transactional(readOnly = true)
    private Mail deliveredMail(String fulfillmentId) throws IOException {
        Fulfillment f = fulfillments.findById(fulfillmentId);
        StoreOrder o = f == null ? null : orders.findById(f.getOrderId());
        if (o == null || !Mail.isAddress(o.getEmail())) {
            return null;
        }
        Store s = stores.findById(o.getStoreId());
        Body d = new Body(s, language.of(o), "mail.delivered.title");
        d.para(d.f("mail.delivered.body", o.getNumber()));
        d.button(d.f("mail.seeOrder"), track(o));
        return builder(s, o.getEmail(), d.f("mail.delivered.subject", o.getNumber()), d).build();
    }

    /// Days after delivery the review request goes out.
    @Value("${bazaar.reviews.requestDays:7}")
    private int reviewDelayDays = 7;
    @Autowired
    private com.codenameone.examples.bazaar.repository.ReviewRequestRepository reviewRequests;

    /// Schedules the review request for the order whose fulfilment
    /// `fulfillmentId` was delivered at `now`: one per order, however many
    /// parcels it came in. Public so that tests can choose the time.
    @Transactional(rollbackFor = IOException.class)
    public void scheduleReview(String fulfillmentId, long now) throws IOException {
        Fulfillment f = fulfillments.findById(fulfillmentId);
        StoreOrder o = f == null ? null : orders.findById(f.getOrderId());
        if (o == null || !Mail.isAddress(o.getEmail())
                || reviewRequests.findByOrderId(o.getId()) != null) {
            return;
        }
        com.codenameone.examples.bazaar.domain.ReviewRequest r =
                new com.codenameone.examples.bazaar.domain.ReviewRequest();
        r.setId(com.codenameone.examples.bazaar.Ids.next());
        r.setStoreId(o.getStoreId());
        r.setOrderId(o.getId());
        r.setUsername(o.getUsername() == null ? "" : o.getUsername());
        r.setEmail(o.getEmail());
        r.setDueAt(java.time.Instant.ofEpochMilli(now + reviewDelayDays * 86400000L));
        r.setCreatedAt(java.time.Instant.ofEpochMilli(now));
        reviewRequests.insert(r);
    }

    /// The review request: one signed link that opens a page to rate every
    /// product of the order without signing in. Answers whether it went.
    public boolean reviewRequest(com.codenameone.examples.bazaar.domain.ReviewRequest r,
            String link) {
        try {
            Mail m = reviewMail(r, link);
            send(m);
            return m != null;
        } catch (IOException | RuntimeException failed) {
            log("review request", r.getId(), failed);
            return false;
        }
    }

    @Transactional(readOnly = true)
    private Mail reviewMail(com.codenameone.examples.bazaar.domain.ReviewRequest r, String link)
            throws IOException {
        StoreOrder o = orders.findById(r.getOrderId());
        if (o == null || !Mail.isAddress(r.getEmail())) {
            return null;
        }
        Store s = stores.findById(r.getStoreId());
        Body b = new Body(s, language.of(o), "mail.review.title");
        b.para(b.f("mail.review.body"));
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        for (int iter = 0; iter < ls.size(); iter++) {
            Product p = products.findById(ls.get(iter).getProductId());
            if (p != null) {
                b.para(p.getTitle());
            }
        }
        b.button(b.f("mail.review.button"), link);
        return builder(s, r.getEmail(), b.f("mail.review.subject", o.getNumber()), b).build();
    }

    /// The browse abandonment mail: the product the shopper looked at, a link
    /// back to it, and an unsubscribe link (also the `List-Unsubscribe`
    /// header). Answers whether it went.
    public boolean browsed(String storeId, String to, String lang, Product p,
            String unsubscribeUrl) {
        try {
            Mail m = browseMail(storeId, to, lang, p, unsubscribeUrl);
            send(m);
            return m != null;
        } catch (IOException | RuntimeException failed) {
            log("browse reminder", p.getId(), failed);
            return false;
        }
    }

    @Transactional(readOnly = true)
    private Mail browseMail(String storeId, String to, String lang, Product p,
            String unsubscribeUrl) throws IOException {
        if (!Mail.isAddress(to)) {
            return null;
        }
        Store s = stores.findById(storeId);
        Body b = new Body(s, lang, "mail.browse.title");
        b.para(b.f("mail.browse.body", p.getTitle(), s.getName()));
        b.button(b.f("mail.browse.button"), urls.absolute(null, "/s/" + enc(s.getHandle())
                + "/products/" + enc(p.getHandle())));
        b.link(b.f("mail.browse.unsubscribe"), unsubscribeUrl);
        return builder(s, to, b.f("mail.browse.subject", p.getTitle()), b)
                .header("List-Unsubscribe", "<" + unsubscribeUrl + ">").build();
    }

    /// A gift card for someone else, mailed to them on its delivery date with
    /// the sender's message. Answers whether it went.
    public boolean giftCardTo(com.codenameone.examples.bazaar.domain.GiftCard card) {
        try {
            Mail m = giftRecipientMail(card);
            send(m);
            return m != null;
        } catch (IOException | RuntimeException failed) {
            log("gift card delivery", card.getId(), failed);
            return false;
        }
    }

    @Transactional(readOnly = true)
    private Mail giftRecipientMail(com.codenameone.examples.bazaar.domain.GiftCard card)
            throws IOException {
        if (!Mail.isAddress(card.getRecipientEmail())) {
            return null;
        }
        Store s = stores.findById(card.getStoreId());
        Body b = new Body(s, language.shopper(card.getStoreId(), null, card.getRecipientEmail()),
                "mail.gift.title");
        if (card.getRecipientName().length() > 0) {
            b.para(b.f("mail.gift.hello", card.getRecipientName()));
        }
        if (card.getMessage().length() > 0) {
            b.para(card.getMessage());
        }
        b.total(b.f("mail.gift.worth"), card.getBalanceCents(), s.getCurrency());
        b.para(b.f("mail.gift.body"));
        b.code(group(card.getCode()));
        b.html.append("<p style=\"text-align:center\"><img src=\"cid:giftcard-qr\" width=\"180\" "
                + "height=\"180\" alt=\"QR code\"></p>");
        return builder(s, card.getRecipientEmail(), b.f("mail.gift.subject", s.getName()), b)
                .inline("giftcard-qr", "image/png", qrPng(card.getCode()))
                .build();
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
        Body b = new Body(s, language.of(o), "mail.refund.title");
        b.total(b.f("mail.refunded"), latest.getAmountCents(), o.getCurrency());
        b.para(b.f("mail.refund.body", o.getNumber()));
        return builder(s, o.getEmail(), b.f("mail.refund.subject", o.getNumber()), b)
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
        Body b = new Body(s, language.of(o), "mail.return.title");
        b.para(b.f("mail.return.body", o.getNumber(),
                s.getAddress().length() > 0 ? s.getAddress() : s.getName()));
        boolean label = r.getReturnTracking().length() > 0;
        if (label) {
            b.para(b.f("mail.returnLabel.body", r.getReturnCarrier(), r.getReturnTracking()));
        }
        b.button(b.f("mail.seeOrder"), track(o));
        Mail.Builder m = builder(s, o.getEmail(), b.f("mail.return.subject", o.getNumber()), b);
        if (label) {
            m.attach("return-label-" + o.getNumber() + ".pdf", "application/pdf",
                    documents.returnLabel(o.getStoreId(), returnId));
        }
        return m.build();
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
        Body b = new Body(s, language.of(o), "mail.receipt.title");
        b.lines(lines.findByOrderId(orderId), o.getCurrency());
        b.total(b.f("total"), o.getTotalCents(), o.getCurrency());
        b.para(b.f("mail.receipt.body"));
        return builder(s, to.trim(), b.f("mail.receipt.subject", s.getName()), b)
                .attach("receipt-" + o.getNumber() + ".pdf", "application/pdf",
                        documents.receipt(storeId, orderId))
                .build();
    }

    /// A cart sat untouched: a reminder that opens it again, in the language
    /// the cart was filled in.
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
        Body b = new Body(s, language.of(cart), "mail.cart.title");
        b.para(b.f("mail.cart.body", s.getName()));
        b.button(b.f("mail.cart.button"), urls.absolute(null,
                SiteUrls.recoverCart(s.getHandle(), cart.getToken())));
        b.para(b.f("mail.cart.app", urls.appCart(null, s.getHandle(), cart.getToken())));
        return builder(s, to, b.f("mail.cart.subject", s.getName()), b).build();
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
        Body b = new Body(s, language.shopper(storeId, c.getUsername(), c.getEmail()),
                "mail.gift.title");
        b.total(b.f("mail.gift.worth"), cents, currency);
        b.para(b.f("mail.gift.body"));
        b.code(group(code));
        b.html.append("<p style=\"text-align:center\"><img src=\"cid:giftcard-qr\" width=\"180\" "
                + "height=\"180\" alt=\"QR code\"></p>");
        return builder(s, c.getEmail(), b.f("mail.gift.subject", s.getName()), b)
                .inline("giftcard-qr", "image/png", qrPng(code))
                .build();
    }

    /// A variant fell to its reorder point: told to the store's owners and
    /// managers, and to the store's own address. Each member reads it in
    /// their account's language ([ShopperLanguage#staff]); the store's
    /// address, which is nobody's account, in English. One mail per
    /// language, so a failure to send one does not stop the others.
    public void lowStock(String storeId, String sku, int available, int reorderPoint) {
        List<Mail> ms;
        try {
            ms = lowStockMails(storeId, sku, available, reorderPoint);
        } catch (IOException | RuntimeException failed) {
            log("low stock alert", sku, failed);
            return;
        }
        for (int iter = 0; iter < ms.size(); iter++) {
            try {
                send(ms.get(iter));
            } catch (IOException | RuntimeException failed) {
                log("low stock alert", sku, failed);
            }
        }
    }

    @Transactional(readOnly = true)
    private List<Mail> lowStockMails(String storeId, String sku, int available, int reorderPoint)
            throws IOException {
        List<Mail> out = new ArrayList<Mail>();
        Store s = stores.findById(storeId);
        if (s == null) {
            return out;
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
        // Group the addresses by language, keeping the order they came in.
        List<String> langs = new ArrayList<String>();
        List<List<String>> groups = new ArrayList<List<String>>();
        for (int iter = 0; iter < to.size(); iter++) {
            String address = to.get(iter);
            boolean member = !address.equals(s.getEmail()) || isStaff(team, address);
            String lang = member ? language.staff(address) : "en";
            int at = langs.indexOf(lang);
            if (at < 0) {
                langs.add(lang);
                groups.add(new ArrayList<String>());
                at = langs.size() - 1;
            }
            groups.get(at).add(address);
        }
        for (int iter = 0; iter < langs.size(); iter++) {
            Body b = new Body(s, langs.get(iter), null);
            b.heading(b.f("mail.low.heading", sku));
            b.para(b.f("mail.low.body", sku, available, reorderPoint));
            b.button(b.f("mail.low.button"), urls.absolute(null, "/app/"));
            List<String> group = groups.get(iter);
            out.add(builder(s, group.toArray(new String[group.size()]),
                    b.f("mail.low.subject", sku), b).build());
        }
        return out;
    }

    private static boolean isStaff(List<StoreStaff> team, String address) {
        for (int iter = 0; iter < team.size(); iter++) {
            if (address.equals(team.get(iter).getUsername())) {
                return true;
            }
        }
        return false;
    }

    /// The automation action `email.customer` and a journey's e-mail step:
    /// the merchant's own subject and message, in the store's frame. The
    /// caller has already picked the merchant's translation for the
    /// shopper's language, if there is one; the frame around it (the default
    /// subject, the button) follows the shopper's language.
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
        Body b = new Body(s, language.shopper(storeId, null, to), null);
        String title = subject == null || subject.trim().length() == 0
                ? b.f("mail.note", s.getName()) : subject.trim();
        b.heading(title);
        b.para(message == null ? "" : message);
        b.button(b.f("mail.visit"), urls.absolute(null, "/s/" + enc(s.getHandle())));
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
        Body b = new Body(s, language.shopper(storeId, null, to), null);
        String subject = b.f("mail.stock.subject", title);
        b.heading(subject);
        b.para(b.f("mail.stock.body", title));
        b.button(b.f("mail.stock.button"), urls.absolute(null, path));
        b.para(b.f("mail.stock.once"));
        return builder(s, to, subject, b).build();
    }

    /// One campaign message: the merchant's text, the campaign's tracked
    /// links, an open pixel and the unsubscribe link (also as a
    /// `List-Unsubscribe` header). `links` holds label and URL pairs.
    ///
    /// The subject and body are the merchant's translation for the
    /// subscriber's language when there is one (`Campaigns.variant`), else
    /// the original; the frame (the unsubscribe link, the page direction)
    /// follows the subscriber's language.
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
        Body b = new Body(s, language.shopper(storeId, null, to), null);
        b.heading(subject);
        b.para(message == null ? "" : message);
        for (int iter = 0; iter < links.size(); iter++) {
            if (iter == 0) {
                b.button(links.get(iter)[0], links.get(iter)[1]);
            } else {
                b.link(links.get(iter)[0], links.get(iter)[1]);
            }
        }
        b.link(b.f("mail.unsubscribe"), unsubscribeUrl);
        b.pixel(pixelUrl);
        return builder(s, to, subject, b).header("List-Unsubscribe", "<" + unsubscribeUrl + ">")
                .build();
    }

    /// Asks a new subscriber to confirm the address they signed up with, in
    /// the language of the page they signed up on.
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
        Body b = new Body(s, here(storeId, to), null);
        String subject = b.f("mail.confirm.subject", s.getName());
        b.heading(subject);
        b.para(b.f("mail.confirm.body", s.getName()));
        b.button(b.f("mail.confirm.button"), confirmUrl);
        b.para(b.f("mail.confirm.ignore"));
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
        Body b = new Body(s, here(storeId, to), null);
        String subject = b.f("mail.reward.subject", s.getName());
        b.heading(subject);
        b.para(b.f("mail.reward.body"));
        b.code(code);
        b.button(b.f("mail.visit"), urls.absolute(null, "/s/" + enc(s.getHandle())));
        return builder(s, to, subject, b).build();
    }

    /// The language of the request being served when it says one (a signup
    /// form posted from a storefront page), else the shopper's.
    private String here(String storeId, String to) {
        String now = ShopperLanguage.fromRequest();
        return now.length() > 0 ? now : language.shopper(storeId, null, to);
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
        private final String lang;
        private final Map<String, String> t;

        /// A body in `lang` ([StoreText#LANGUAGES]), headed by the text of
        /// `headingKey`, or with no heading yet when it is null.
        Body(Store store, String lang, String headingKey) {
            this.store = store;
            this.lang = StoreText.supported(lang) ? lang : "en";
            this.t = StoreText.table(this.lang);
            this.accent = hex(store == null || store.getAccentColor() == 0 ? DEFAULT_ACCENT
                    : store.getAccentColor());
            if (headingKey != null) {
                heading(f(headingKey));
            }
        }

        /// The text of `key` in this mail's language, with `args` filled in.
        String f(String key, Object... args) {
            if (overrides == null) {
                overrides = templates == null || store == null ? java.util.Collections
                        .<String, String>emptyMap() : templates.overrides(store.getId(), lang);
            }
            String own = overrides.get(key);
            return own == null ? StoreText.format(t, key, args) : StoreText.fill(own, args);
        }

        /// The store's own wording of this mail's texts, read on first use.
        private Map<String, String> overrides;

        void heading(String heading) {
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
            // Product names in the mail's language where the store translated
            // them; the line's recorded names otherwise.
            String locale = null;
            try {
                locale = store == null || productText == null ? null
                        : productText.locale(store.getId(), lang);
            } catch (java.io.IOException unreadable) {
                locale = null;
            }
            for (int iter = 0; iter < ls.size(); iter++) {
                OrderLine l = ls.get(iter);
                String[] names = {l.getTitle(), l.getVariantTitle()};
                if (locale != null) {
                    try {
                        names = productText.line(store.getId(), l.getProductId(), l.getTitle(),
                                l.getVariantTitle(), locale);
                    } catch (java.io.IOException unreadable) {
                        names = new String[] {l.getTitle(), l.getVariantTitle()};
                    }
                }
                String name = names[0] + (names[1] != null && names[1].length() > 0 ? " - "
                        + names[1] : "");
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
            // Hebrew reads right to left: the whole page turns, the table
            // cells and the text alignment with it.
            String dir = StoreText.rtl(lang) ? "rtl" : "ltr";
            page.append("<!doctype html><html lang=\"").append(lang).append("\" dir=\"")
                    .append(dir).append("\"><body dir=\"").append(dir)
                    .append("\" style=\"margin:0;background:#f1f5f9;"
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
