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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.AnonymousAuthenticationToken;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContextHolder;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CardDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.PaymentMethodDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The HTML checkout: one page of progressive sections -- contact and
/// address, delivery, codes, payment -- over the visitor's session cart, the
/// same `Cart` row the cart page fills.
///
/// Every post carries the whole form, so nothing of the checkout is kept on
/// the server between them except the cart itself: a refresh of the rates
/// (htmx when there is JavaScript, the "Update delivery" button when there is
/// not), a discount or gift card applied to the cart, and the order placed.
/// Placing goes through [Checkouts#place], the app's own path, with the
/// form's idempotency key, so a second submit of the same page answers the
/// order the first one placed.
///
/// **Guests.** An order needs a name to key the idempotency key, the store's
/// customer record and once-per-customer codes by. A guest's is
/// `guest:<cart id>`: unique, never shown, and no account can have it (an
/// account's name is an email address, which has no colon before an `@`).
/// The cart id is the session cart's, so a guest who orders twice from one
/// session is one customer. A guest pays with a card typed for this one order
/// ([Payments#checkCard]); nothing of it is saved.
///
/// **Confirmation links.** An order's page is open to the account that placed
/// it, and to whoever holds its link token ([#linkToken]): a digest of the
/// order's id and its idempotency `client_id`, two values that never leave the
/// server, so the token cannot be worked out from anything a page shows and
/// needs neither a column nor a key of its own.
@Component
public class CheckoutPages {
    @Autowired
    private com.codenameone.examples.bazaar.catalog.Downloads downloads;
    @Autowired
    private com.codenameone.examples.bazaar.checkout.CheckoutSettings checkoutSettings;
    @Autowired
    private com.codenameone.examples.bazaar.pay.PaymentEvents paymentEvents;
    @Autowired
    private SiteUrls siteUrls;
    @Autowired
    private ExpressButton expressButton;
    @Autowired
    private com.codenameone.examples.bazaar.repository.PaymentRepository paymentRows;
    static final String GUEST = "guest:";
    /// The countries the form offers: ISO code, name.
    static final String[][] COUNTRIES = {
        {"US", "United States"}, {"CA", "Canada"}, {"GB", "United Kingdom"},
        {"DE", "Germany"}, {"FR", "France"}, {"NL", "Netherlands"}, {"ES", "Spain"},
        {"IT", "Italy"}, {"IE", "Ireland"}, {"AU", "Australia"}, {"IL", "Israel"},
    };

    @com.codename1.backend.annotations.Autowired
    private Backorders backorders;
    private final Stores stores;
    private final Carts carts;
    private final Checkouts checkouts;
    private final Payments payments;
    private final CustomerAddressRepository addresses;
    private final StoreOrderRepository orderRows;
    private final FulfillmentRepository fulfillments;
    private final Orders orders;
    private final Documents documents;
    private final StorefrontPages pages;
    private final com.codenameone.examples.bazaar.trade.Trade trade;
    private final com.codenameone.examples.bazaar.web.security.BotGuard bots;
    private final com.codenameone.examples.bazaar.market.Markets markets;
    private final com.codenameone.examples.bazaar.taxes.TaxExemptions exemptions;

    public CheckoutPages(Stores stores, Carts carts, Checkouts checkouts, Payments payments,
            CustomerAddressRepository addresses, StoreOrderRepository orderRows,
            FulfillmentRepository fulfillments, Orders orders, Documents documents,
            StorefrontPages pages, com.codenameone.examples.bazaar.trade.Trade trade,
            com.codenameone.examples.bazaar.web.security.BotGuard bots,
            com.codenameone.examples.bazaar.market.Markets markets,
            com.codenameone.examples.bazaar.taxes.TaxExemptions exemptions) {
        this.trade = trade;
        this.bots = bots;
        this.markets = markets;
        this.exemptions = exemptions;
        this.stores = stores;
        this.carts = carts;
        this.checkouts = checkouts;
        this.payments = payments;
        this.addresses = addresses;
        this.orderRows = orderRows;
        this.fulfillments = fulfillments;
        this.orders = orders;
        this.documents = documents;
        this.pages = pages;
    }

    /// The signed-in account, or null for a guest.
    static String signedIn() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || who instanceof AnonymousAuthenticationToken || !who.isAuthenticated()
                || who.getName() == null || who.getName().length() == 0) {
            return null;
        }
        return who.getName();
    }

    // ------------------------------------------------------------- the page

    /// The first visit: an empty form, or the default saved address of a
    /// signed-in shopper, and a fresh idempotency key.
    public void show(String handle, Model model, HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        CheckoutForm form = new CheckoutForm();
        String me = signedIn();
        if (me != null) {
            if (me.indexOf('@') > 0) {
                form.email = me;
            }
            List<CustomerAddress> mine = addresses.findByUsername(me);
            for (int iter = 0; iter < mine.size(); iter++) {
                if (mine.get(iter).isIsDefault()) {
                    form.addressId = mine.get(iter).getId();
                }
            }
        }
        render(store, form, new CheckoutView(), model, request);
    }

    /// A post that only changes what is shown: the address or the delivery
    /// changed, so the rates and totals are worked out again.
    public void refresh(String handle, CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        render(stores.activeStore(handle), clean(form), new CheckoutView(), model, request);
    }

    /// Applies or removes a discount code or a gift card on the session cart.
    /// A refusal (an unknown code, an empty card) is shown beside the field.
    public void codes(String handle, CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        CheckoutForm f = clean(form);
        CheckoutView view = new CheckoutView();
        String token = StorefrontPages.token(request, store, false);
        if (token != null) {
            try {
                if ("apply-discount".equals(f.codeAction)) {
                    if (f.discountCode.length() == 0) {
                        view.codeError = "Enter a discount code";
                    } else {
                        List<String> before = carts.get(store.getId(), null, token).problems();
                        CartDto after = carts.applyDiscount(store.getId(), null, token,
                                f.discountCode);
                        // A code the store does not know, or one that does not
                        // apply, is kept on the cart with a problem; here it is
                        // told beside the field and taken off again, so the
                        // order is not refused for it later.
                        for (int iter = 0; after.problems() != null
                                && iter < after.problems().size(); iter++) {
                            String p = after.problems().get(iter);
                            if (before == null || !before.contains(p)) {
                                view.codeError = p;
                            }
                        }
                        if (view.codeError.length() > 0) {
                            carts.removeDiscount(store.getId(), null, token);
                        }
                    }
                } else if ("remove-discount".equals(f.codeAction)) {
                    carts.removeDiscount(store.getId(), null, token);
                } else if ("apply-gift".equals(f.codeAction)) {
                    if (f.giftCardCode.length() == 0) {
                        view.codeError = "Enter a gift card code";
                    } else {
                        carts.applyGiftCard(store.getId(), null, token, f.giftCardCode);
                    }
                } else if ("remove-gift".equals(f.codeAction)) {
                    carts.removeGiftCard(store.getId(), null, token);
                } else if (("use-credit".equals(f.codeAction) || "stop-credit".equals(f.codeAction))
                        && signedIn() != null) {
                    carts.useStoreCredit(store.getId(), null, token,
                            "use-credit".equals(f.codeAction));
                }
            } catch (ResponseStatusException refused) {
                if (refused.getStatus() == 429) {
                    throw refused;
                }
                view.codeError = reason(refused, "That code cannot be used");
            }
        }
        render(store, f, view, model, request);
    }

    /// Places the order. Answers the confirmation page's address, or null
    /// when the page was drawn again with what has to change.
    public String place(String handle, CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        CheckoutForm f = clean(form);
        String me = signedIn();
        if (StorefrontPages.token(request, store, false) != null) {
            // A second submit of the page (a double click, a reload of the
            // post) finds the order the first one placed, though the cart it
            // came from is empty now.
            String payer = me != null ? me : GUEST + cart(store, request).getId();
            OrderDto before = checkouts.placedBefore(payer, f.idempotencyKey);
            if (before != null) {
                return SiteUrls.order(store.getHandle(), before.number(),
                        linkToken(orderRows.findById(before.id())));
            }
        }
        CheckoutView view = new CheckoutView();
        CartDto dto = render(store, f, view, model, request);
        if (view.empty || dto == null) {
            return null;
        }
        if (f.email.indexOf('@') <= 0 || f.email.indexOf('@') == f.email.length() - 1) {
            view.emailError = "Enter the email address the receipt goes to";
        }
        boolean pickup = false;
        for (int iter = 0; iter < view.rates.size(); iter++) {
            CheckoutView.Option o = view.rates.get(iter);
            if (o.chosen && o.detail.startsWith("Pick up")) {
                pickup = true;
            }
        }
        if (country(f.country) == null) {
            view.countryError = "Choose a country";
        }
        if (!pickup) {
            if (f.name.length() == 0) {
                view.nameError = "Enter the name the order goes to";
            }
            if (f.line1.length() == 0) {
                view.line1Error = "Enter the street address";
            }
            if (f.city.length() == 0) {
                view.cityError = "Enter the city";
            }
        }
        if (view.countryError.length() == 0 && dto.shippingRateId() == null
                || view.countryError.length() == 0 && dto.shippingRateId().length() == 0) {
            view.rateError = "The store does not deliver to that address";
        }
        // A payment method other than a card (pay later, a bank) is finished
        // on the order page after placing; no card is asked for.
        boolean later = dto.dueCents() > 0 && f.payType.length() > 0
                && !com.codenameone.examples.bazaar.pay.PaymentTypes.CARD.equals(f.payType);
        boolean card = !later && dto.dueCents() > 0 && (me == null || f.methodId.length() == 0);
        CardDto typed = null;
        if (card) {
            typed = typedCard(f, view);
        }
        if (!view.problems.isEmpty() || anyFieldError(view)) {
            view.invalid = true;
            return null;
        }
        try {
            String payer = me != null ? me : GUEST + cart(store, request).getId();
            Payments.OneOffCard oneOff = typed == null ? null : payments.checkCard(payer, typed);
            AddressDto address = address(f);
            OrderDto placed = checkouts.place(payer, store.getId(), null,
                    new PlaceOrderDto(address, dto.shippingRateId(),
                        card || later ? null : f.methodId, f.email, f.note, f.idempotencyKey,
                        tipChoice(f.tip), answers(store, f)),
                    cart(store, request), oneOff,
                    markets.pricer(store, StorefrontMarkets.cookie(request)),
                    later ? com.codenameone.examples.bazaar.checkout.Checkouts.Via.method(f.payType)
                        : null);
            StoreOrder row = orderRows.findById(placed.id());
            return SiteUrls.order(store.getHandle(), placed.number(), linkToken(row));
        } catch (ResponseStatusException refused) {
            if (refused.getStatus() == 429 || refused.getStatus() == 404) {
                throw refused;
            }
            String why = reason(refused, "The order could not be placed");
            if (refused.getStatus() == 402) {
                view.cardError = why;
            } else {
                view.problems.add(why);
            }
            view.invalid = true;
            // The cart may have changed under the refusal (a line sold out):
            // draw it as it is now.
            CheckoutView again = new CheckoutView();
            again.problems = view.problems;
            again.cardError = view.cardError;
            again.invalid = true;
            render(store, f, again, model, request);
            return null;
        }
    }

    /// Places the cart on the buyer company's net terms ([Trade#placeOnTerms]):
    /// at the company's prices, held to its quantity rules, unpaid with a due
    /// date. Answers the order page, or null when the page was drawn again
    /// with the reason it was refused (a quantity rule, stock).
    public String terms(String handle, CheckoutForm form, Model model, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        CheckoutForm f = clean(form);
        String me = signedIn();
        if (me == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        String token = StorefrontPages.token(request, store, false);
        CartDto dto = token == null ? null : carts.get(store.getId(), null, token);
        List<com.codenameone.examples.bazaar.api.CartLineChangeDto> lines =
                new ArrayList<com.codenameone.examples.bazaar.api.CartLineChangeDto>();
        for (int iter = 0; dto != null && dto.lines() != null && iter < dto.lines().size();
                iter++) {
            lines.add(new com.codenameone.examples.bazaar.api.CartLineChangeDto(
                    dto.lines().get(iter).variantId(), dto.lines().get(iter).quantity()));
        }
        com.codenameone.examples.bazaar.trade.LineOrders.Buyer ship =
                new com.codenameone.examples.bazaar.trade.LineOrders.Buyer();
        ship.email = f.email.indexOf('@') > 0 ? f.email : me.indexOf('@') > 0 ? me : "";
        ship.shipName = f.name;
        ship.shipLine1 = f.line1;
        ship.shipCity = f.city;
        ship.shipRegion = f.region;
        ship.shipPostal = f.postal;
        ship.shipCountry = f.country;
        if (f.addressId.length() > 0 && f.line1.length() == 0) {
            List<CustomerAddress> mine = addresses.findByUsername(me);
            for (int iter = 0; iter < mine.size(); iter++) {
                CustomerAddress a = mine.get(iter);
                if (a.getId().equals(f.addressId)) {
                    ship.shipName = a.getName();
                    ship.shipLine1 = a.getLine1();
                    ship.shipCity = a.getCity();
                    ship.shipRegion = a.getRegion();
                    ship.shipPostal = a.getPostal();
                    ship.shipCountry = a.getCountry();
                }
            }
        }
        StoreOrder placed;
        try {
            placed = trade.placeOnTerms(me, store.getId(), lines, ship, "terms-"
                    + f.idempotencyKey);
        } catch (ResponseStatusException refused) {
            int st = refused.getStatus();
            if (st == 401 || st == 403 || st == 404 || st == 429) {
                throw refused;
            }
            CheckoutView again = new CheckoutView();
            again.problems.add(reason(refused, "The order could not be placed on terms"));
            render(store, f, again, model, request);
            return null;
        }
        if (dto != null) {
            carts.clear(dto.id());
        }
        return SiteUrls.order(store.getHandle(), placed.getNumber(), linkToken(placed));
    }

    private Cart cart(Store store, HttpServer.Request request) throws IOException {
        String token = StorefrontPages.token(request, store, false);
        Cart cart = carts.cart(store.getId(), null, token);
        StorefrontPages.remember(request, store, cart.getToken());
        return cart;
    }

    private static boolean anyFieldError(CheckoutView v) {
        return (v.emailError + v.nameError + v.line1Error + v.cityError + v.countryError
                + v.rateError + v.cardError).length() > 0;
    }

    /// The typed card, or null with the reason set on the view.
    static CardDto typedCard(CheckoutForm f, CheckoutView view) {
        String exp = f.cardExpiry.replace(" ", "");
        int slash = exp.indexOf('/');
        int month = slash < 0 ? -1 : digits(exp.substring(0, slash));
        int year = slash < 0 ? -1 : digits(exp.substring(slash + 1));
        if (f.cardNumber.length() == 0) {
            view.cardError = "Enter the card number";
        } else if (month < 1 || month > 12 || year < 0) {
            view.cardError = "Enter the expiry date as MM/YY";
        } else if (f.cardCvc.length() < 3) {
            view.cardError = "Enter the card's security code";
        } else {
            return new CardDto(f.cardNumber, month, year, f.cardCvc, f.name);
        }
        return null;
    }

    private static int digits(String text) {
        if (text.length() == 0 || text.length() > 4) {
            return -1;
        }
        int out = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    /// A whole positive number of at most nine digits, else -1.
    private static int positive(String text) {
        if (text.length() > 9) {
            return -1;
        }
        int out = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return text.length() == 0 ? -1 : out;
    }

    private static String reason(ResponseStatusException e, String fallback) {
        return e.getReason() == null || e.getReason().length() == 0 ? fallback : e.getReason();
    }

    /// Fills the view from the form and the cart priced for its address, and
    /// adds both to the model. Answers the priced cart, or null with no cart.
    private CartDto render(Store store, CheckoutForm f, CheckoutView view, Model model,
            HttpServer.Request request) throws IOException {
        String me = signedIn();
        String base = SiteUrls.checkout(store.getHandle());
        view.action = base;
        view.codesAction = base + "/codes";
        view.placeAction = base + "/place";
        view.fillToken = bots.issue();
        if (me != null && trade.buyerStores(me).contains(store.getId())) {
            com.codenameone.examples.bazaar.trade.Trade.BuyerTerms t = trade.myTerms(me,
                    store.getId());
            if (t.termsDays > 0) {
                view.netTerms = true;
                view.termsAction = base + "/terms";
                view.termsLabel = "Buy for " + t.companyName + " on net " + t.termsDays
                        + ": your company's prices and quantity rules apply, and the invoice "
                        + "is due " + t.termsDays + " days after the order.";
            }
        }
        view.cartUrl = "/s/" + SiteUrls.encode(store.getHandle()) + "/cart";
        view.signedIn = me != null;
        if (f.idempotencyKey.length() < 16 || f.idempotencyKey.length() > 64) {
            f.idempotencyKey = Ids.next();
        }
        view.key = f.idempotencyKey;
        if (me != null) {
            useSavedAddress(me, f, view);
        }
        if (f.country.length() == 0) {
            f.country = "US";
        }
        view.email = f.email;
        view.name = f.name;
        view.line1 = f.line1;
        view.line2 = f.line2;
        view.city = f.city;
        view.region = f.region;
        view.postal = f.postal;
        view.phone = f.phone;
        view.note = f.note;
        for (int iter = 0; iter < COUNTRIES.length; iter++) {
            view.countries.add(new CheckoutView.Option(COUNTRIES[iter][0], COUNTRIES[iter][1], "",
                    COUNTRIES[iter][0].equals(f.country)));
        }

        String token = StorefrontPages.token(request, store, false);
        CartDto dto = null;
        if (token != null) {
            AddressDto address = country(f.country) == null ? null : address(f);
            dto = carts.estimateFor(store.getId(), token,
                    new EstimateDto(address, f.rateId.length() == 0 ? null : f.rateId), me);
            if (f.rateId.length() > 0 && !offered(dto, f.rateId)) {
                // Another country offers other rates: take the cheapest
                // rather than complain about a choice the page no longer shows.
                dto = carts.estimateFor(store.getId(), token, new EstimateDto(address, null), me);
            }
            String delivery = f.rateId.length() == 0 ? cheapestDelivery(dto) : null;
            if (delivery != null && !delivery.equals(dto.shippingRateId())) {
                // Unasked, the price picks the cheapest option, which is a free
                // pickup where the store has one. A shopper who typed an
                // address means it to be delivered: start from the cheapest
                // delivery, and let them choose the pickup.
                dto = carts.estimateFor(store.getId(), token, new EstimateDto(address, delivery),
                        me);
            }
        }
        String currency = store.getCurrency();
        if (dto != null && !dto.lines().isEmpty()) {
            view.empty = false;
            Map<String, String> handles = pages.handles(dto.lines());
            String locale = pages.contentLocale(store, base, request);
            for (int iter = 0; iter < dto.lines().size(); iter++) {
                CartLineDto l = dto.lines().get(iter);
                CartLineView lv = new CartLineView();
                lv.id = l.id();
                pages.lineNames(lv, store, l.productId(), l.title(), l.variantTitle(), locale);

                String ph = handles.get(l.productId());
                lv.url = ph == null ? "" : StorefrontPages.productUrl(store.getHandle(), ph);
                lv.quantity = l.quantity();
                lv.unit = Money.format(l.priceCents(), currency);
                lv.total = Money.format(l.priceCents() * l.quantity(), currency);
                lv.available = l.available();
                lv.backordered = !l.preorder() && backorders.wouldBackorder(l.variantId(),
                        l.quantity());
                view.lines.add(lv);
            }
            List<ShippingOptionDto> options = dto.shippingOptions();
            for (int iter = 0; options != null && iter < options.size(); iter++) {
                ShippingOptionDto o = options.get(iter);
                String eta = ShippingCalculator.PICKUP.equals(o.kind()) ? "Pick up in store"
                        : o.etaDaysMax() > 0 ? o.etaDaysMin() + "-" + o.etaDaysMax()
                            + " business days" : "";
                view.rates.add(new CheckoutView.Option(o.rateId(), o.name()
                        + " - " + (o.priceCents() == 0 ? "Free" : Money.format(o.priceCents(),
                            currency)), eta, o.rateId().equals(dto.shippingRateId())));
            }
            view.subtotal = Money.format(dto.subtotalCents(), currency);
            view.discount = dto.discountCents() > 0 ? "-" + Money.format(dto.discountCents(),
                    currency) : "";
            view.shipping = dto.shippingRateId() == null || dto.shippingRateId().length() == 0
                    ? "Enter an address" : dto.shippingCents() == 0 ? "Free"
                    : Money.format(dto.shippingCents(), currency);
            view.tax = Money.format(dto.taxCents(), currency);
            view.taxInclusive = dto.taxInclusive();
            view.total = Money.format(dto.totalCents(), currency);
            view.giftCard = dto.giftCardCents() > 0 ? "-" + Money.format(dto.giftCardCents(),
                    currency) : "";
            view.due = Money.format(dto.dueCents(), currency);
            view.storeCredit = dto.storeCreditCents() > 0 ? "-" + Money.format(
                    dto.storeCreditCents(), currency) : "";
            view.creditAvailable = dto.creditAvailableCents() > 0
                    ? Money.format(dto.creditAvailableCents(), currency) : "";
            view.usingCredit = dto.useStoreCredit() && dto.storeCreditCents() > 0;
            if (f.country.length() == 2) {
                List<String[]> dutiable = new java.util.ArrayList<String[]>();
                for (int iter = 0; iter < dto.lines().size(); iter++) {
                    CartLineDto l = dto.lines().get(iter);
                    dutiable.add(new String[] {l.productId(),
                        Long.toString(l.priceCents() * l.quantity())});
                }
                long duty = markets.dutyEstimate(store.getId(), f.country, dutiable);
                view.duty = duty > 0 ? Money.format(duty, currency) : "";
                String cert = me == null ? null : exemptions
                        .certificateFor(store.getId(), me, f.country, f.region,
                                java.time.Instant.now());
                view.exemption = cert == null ? "" : "Tax exemption certificate " + cert
                        + " applies to this order.";
            }
            String browsed = StorefrontMarkets.cookie(request);
            com.codenameone.examples.bazaar.market.Markets.Pricer shown =
                    browsed == null ? null : markets.pricer(store, browsed);
            if (shown != null && !shown.currency().equals(currency)) {
                // Charged in the shopper's currency at the rate the prices were
                // shown in (Checkouts.place); worked out in the store's.
                view.marketNote = "This order is charged in " + shown.currency() + ": "
                        + Money.format(com.codenameone.examples.bazaar.checkout.Checkouts.atRate(
                            dto.dueCents(), shown.rateMicros()), shown.currency())
                        + " for " + Money.format(dto.dueCents(), currency) + ".";
            }
            view.nothingDue = dto.dueCents() <= 0;
            view.discountCode = dto.discountCode() == null ? "" : dto.discountCode();
            view.giftCardCode = dto.giftCardCode() == null || dto.giftCardCode().length() < 4
                    ? (dto.giftCardCode() == null ? "" : dto.giftCardCode())
                    : "ending " + dto.giftCardCode().substring(dto.giftCardCode().length() - 4);
            List<String> problems = dto.problems();
            for (int iter = 0; problems != null && iter < problems.size(); iter++) {
                if (!view.problems.contains(problems.get(iter))) {
                    view.problems.add(problems.get(iter));
                }
            }
        }
        view.typedCards = payments.takesTypedCards();
        if (me != null) {
            List<PaymentMethodDto> methods = payments.methods(me);
            for (int iter = 0; iter < methods.size(); iter++) {
                PaymentMethodDto m = methods.get(iter);
                boolean chosen = m.id().equals(f.methodId);
                view.methods.add(new CheckoutView.Option(m.id(), m.label(), "", chosen));
                if (chosen) {
                    view.typedChosen = false;
                }
            }
        }
        extras(store, f, view, dto);
        view.invalid = view.invalid || anyFieldError(view);
        PageMeta meta = pages.meta(store, request, base);
        if (!view.empty && !view.nothingDue) {
            expressButton.decorate(meta, store, f.country, dto == null ? 0 : dto.dueCents());
        }
        meta.title = meta.t.get("checkout") + " - " + store.getName();
        if (!view.tips.isEmpty()) {
            view.tips.get(0).label = meta.t.get("tip.none");
        }
        meta.noindex = true;
        // The field errors were written in English; show them in the page's language.
        view.emailError = StoreText.translate(meta.t, view.emailError);
        view.nameError = StoreText.translate(meta.t, view.nameError);
        view.line1Error = StoreText.translate(meta.t, view.line1Error);
        view.cityError = StoreText.translate(meta.t, view.cityError);
        view.countryError = StoreText.translate(meta.t, view.countryError);
        view.rateError = StoreText.translate(meta.t, view.rateError);
        view.cardError = StoreText.translate(meta.t, view.cardError);
        view.codeError = StoreText.translate(meta.t, view.codeError);
        // Payment types: "Card" and the generic ones translate; brand names
        // (Klarna, iDEAL) have no row and stay as they are.
        for (int iter = 0; iter < view.payTypes.size(); iter++) {
            CheckoutView.Option o = view.payTypes.get(iter);
            o.label = StoreText.translate(meta.t, o.label);
        }
        model.addAttribute("page", meta).addAttribute("checkout", view);
        return dto;
    }

    /// The cheapest option that is not a pickup, or null when there is none.
    private static String cheapestDelivery(CartDto dto) {
        List<ShippingOptionDto> options = dto.shippingOptions();
        ShippingOptionDto best = null;
        for (int iter = 0; options != null && iter < options.size(); iter++) {
            ShippingOptionDto o = options.get(iter);
            if (!ShippingCalculator.PICKUP.equals(o.kind())
                    && (best == null || o.priceCents() < best.priceCents())) {
                best = o;
            }
        }
        return best == null ? null : best.rateId();
    }

    private static boolean offered(CartDto dto, String rateId) {
        List<ShippingOptionDto> options = dto.shippingOptions();
        for (int iter = 0; options != null && iter < options.size(); iter++) {
            if (rateId.equals(options.get(iter).rateId())) {
                return true;
            }
        }
        return false;
    }

    /// Copies the chosen saved address into the form, and lists them all.
    /// Only the shopper's own: an id of somebody else's is ignored.
    private void useSavedAddress(String me, CheckoutForm f, CheckoutView view) {
        List<CustomerAddress> mine = addresses.findByUsername(me);
        for (int iter = 0; iter < mine.size(); iter++) {
            CustomerAddress a = mine.get(iter);
            boolean chosen = a.getId().equals(f.addressId);
            view.addresses.add(new CheckoutView.Option(a.getId(), a.getName() + ", " + a.getLine1()
                    + ", " + a.getCity() + " " + a.getCountry(), "", chosen));
            if (chosen) {
                f.name = a.getName();
                f.line1 = a.getLine1();
                f.line2 = a.getLine2();
                f.city = a.getCity();
                f.region = a.getRegion();
                f.postal = a.getPostal();
                f.country = a.getCountry();
                f.phone = a.getPhone();
            }
        }
    }

    private static AddressDto address(CheckoutForm f) {
        return new AddressDto(f.name, f.line1, f.line2, f.city, f.region, f.postal, f.country,
                f.phone, 0, 0);
    }

    /// The ISO code if it is one the form offers, else null.
    static String country(String code) {
        for (int iter = 0; iter < COUNTRIES.length; iter++) {
            if (COUNTRIES[iter][0].equals(code)) {
                return code;
            }
        }
        return null;
    }

    /// Trims every field and cuts it to a length no real value needs, so a
    /// megabyte typed into a field is not carried into every page drawn.
    /// The tip presets, the store's checkout fields, the payment method types
    /// for this currency and country.
    private void extras(Store store, CheckoutForm f, CheckoutView view, CartDto dto)
            throws IOException {
        com.codenameone.examples.bazaar.api.CheckoutSettingsDto s =
                checkoutSettings.of(store.getId());
        String currency = store.getCurrency();
        if (dto != null && s.tipsEnabled()) {
            int chosen = tipChoice(f.tip);
            view.tips.add(new CheckoutView.Option("0", "No tip", "", chosen == 0));
            List<com.codenameone.examples.bazaar.api.TipOptionDto> on =
                    com.codenameone.examples.bazaar.checkout.CheckoutSettings.tipOptions(s,
                        dto.subtotalCents() - dto.discountCents(), currency);
            for (int iter = 0; iter < on.size(); iter++) {
                com.codenameone.examples.bazaar.api.TipOptionDto t = on.get(iter);
                view.tips.add(new CheckoutView.Option(String.valueOf(t.choice()), t.label(),
                        Money.format(t.amountCents(), currency), chosen == t.choice()));
            }
        }
        for (int iter = 0; iter < s.fields().size(); iter++) {
            com.codenameone.examples.bazaar.api.CheckoutFieldDto d = s.fields().get(iter);
            CheckoutView.Field fv = new CheckoutView.Field();
            fv.name = "f" + iter;
            fv.label = d.label();
            fv.kind = d.kind();
            fv.text = com.codenameone.examples.bazaar.checkout.CheckoutSettings.TEXT.equals(fv.kind);
            fv.checkbox = com.codenameone.examples.bazaar.checkout.CheckoutSettings.CHECKBOX
                    .equals(fv.kind);
            fv.select = com.codenameone.examples.bazaar.checkout.CheckoutSettings.SELECT
                    .equals(fv.kind);
            fv.required = d.required();
            fv.value = f.field(iter);
            fv.checked = fv.value.length() > 0;
            List<String> choices = com.codenameone.examples.bazaar.checkout.CheckoutSettings
                    .options(d.options());
            for (int c = 0; c < choices.size(); c++) {
                fv.choices.add(new CheckoutView.Option(choices.get(c), choices.get(c), "",
                        choices.get(c).equals(fv.value)));
            }
            view.fields.add(fv);
        }
        if (dto != null && dto.dueCents() > 0) {
            List<com.codenameone.examples.bazaar.api.PaymentTypeDto> types =
                    com.codenameone.examples.bazaar.pay.PaymentTypes.offered(s.methods(),
                        currency, f.country);
            if (types.size() > 1) {
                for (int iter = 0; iter < types.size(); iter++) {
                    com.codenameone.examples.bazaar.api.PaymentTypeDto t = types.get(iter);
                    boolean card = com.codenameone.examples.bazaar.pay.PaymentTypes.CARD
                            .equals(t.type());
                    view.payTypes.add(new CheckoutView.Option(t.type(), t.label(), "",
                            card ? f.payType.length() == 0 || f.payType.equals(t.type())
                                : f.payType.equals(t.type())));
                }
            }
        }
    }

    /// The tip preset a form chose, 0 for none or anything unreadable.
    static int tipChoice(String tip) {
        if (tip == null || tip.length() != 1 || tip.charAt(0) < '0' || tip.charAt(0) > '9') {
            return 0;
        }
        return tip.charAt(0) - '0';
    }

    /// The form's answers to the store's fields, in the settings' order.
    private List<com.codenameone.examples.bazaar.api.FieldAnswerDto> answers(Store store,
            CheckoutForm f) throws IOException {
        List<com.codenameone.examples.bazaar.api.CheckoutFieldDto> fields =
                checkoutSettings.of(store.getId()).fields();
        List<com.codenameone.examples.bazaar.api.FieldAnswerDto> out =
                new java.util.ArrayList<com.codenameone.examples.bazaar.api.FieldAnswerDto>();
        for (int iter = 0; iter < fields.size(); iter++) {
            out.add(new com.codenameone.examples.bazaar.api.FieldAnswerDto(fields.get(iter).id(),
                    f.field(iter)));
        }
        return out;
    }

    /// A payment pending order's two simulated outcomes (the simulated
    /// processor only): `approve` pays it, `decline` fails it, each through
    /// the same settling as the processor's webhook.
    public String simulatePayment(String handle, String number, String token, String outcome)
            throws IOException {
        StoreOrder row = visible(handle, number, token);
        if (!(payments.provider()
                instanceof com.codenameone.examples.bazaar.pay.SimulatedPayments)) {
            throw new ResponseStatusException(404, "Not found");
        }
        com.codenameone.examples.bazaar.pay.SimulatedPayments sim =
                (com.codenameone.examples.bazaar.pay.SimulatedPayments) payments.provider();
        com.codenameone.examples.bazaar.domain.Payment p = paymentRow(row.getId());
        if (p != null && Orders.PENDING.equals(row.getFinancial())) {
            boolean ok = "approve".equals(outcome);
            sim.complete(p.getProviderRef(), ok);
            if (ok) {
                sim.settle(p.getProviderRef(), true);
            }
            paymentEvents.settle(row.getId());
        }
        return SiteUrls.order(handle, row.getNumber(), linkToken(row));
    }

    private com.codenameone.examples.bazaar.domain.Payment paymentRow(String orderId)
            throws IOException {
        return paymentRows.findById(orderId);
    }

    static CheckoutForm clean(CheckoutForm in) {
        CheckoutForm f = in == null ? new CheckoutForm() : in;
        f.email = cut(f.email, 190);
        f.addressId = cut(f.addressId, 64);
        f.name = cut(f.name, 120);
        f.line1 = cut(f.line1, 200);
        f.line2 = cut(f.line2, 200);
        f.city = cut(f.city, 120);
        f.region = cut(f.region, 80);
        f.postal = cut(f.postal, 20);
        f.country = upper(cut(f.country, 2));
        f.phone = cut(f.phone, 32);
        f.rateId = cut(f.rateId, 64);
        f.note = cut(f.note, 1000);
        f.codeAction = cut(f.codeAction, 32);
        f.discountCode = cut(f.discountCode, 64);
        f.giftCardCode = cut(f.giftCardCode, 64);
        f.methodId = cut(f.methodId, 64);
        f.cardNumber = cut(f.cardNumber, 32);
        f.cardExpiry = cut(f.cardExpiry, 8);
        f.cardCvc = cut(f.cardCvc, 4);
        f.idempotencyKey = cut(f.idempotencyKey, 64);
        f.tip = cut(f.tip, 2);
        f.payType = cut(f.payType, 32);
        f.f0 = cut(f.f0, 500);
        f.f1 = cut(f.f1, 500);
        f.f2 = cut(f.f2, 500);
        f.f3 = cut(f.f3, 500);
        f.f4 = cut(f.f4, 500);
        f.f5 = cut(f.f5, 500);
        f.f6 = cut(f.f6, 500);
        f.f7 = cut(f.f7, 500);
        f.f8 = cut(f.f8, 500);
        f.f9 = cut(f.f9, 500);
        return f;
    }

    /// ASCII upper case: a country code is ASCII by definition, and a
    /// locale-sensitive fold would turn `i` into a dotted capital on a
    /// Turkish server.
    private static String upper(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            out.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return out.toString();
    }

    private static String cut(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    // --------------------------------------------------------- confirmation

    /// The token that opens an order's page and invoice without an account.
    static String linkToken(StoreOrder order) throws IOException {
        return Ids.sha256("bazaar-order-link\n" + order.getId() + "\n" + order.getClientId())
                .substring(0, 32);
    }

    /// The order `number` of the store, when the caller may see it: the
    /// account that placed it, or anybody with its link token. Answers 404
    /// otherwise -- the same for a wrong token as for no such order, since
    /// order numbers are sequential and would otherwise be confirmed one by
    /// one.
    StoreOrder visible(String handle, String number, String token) throws IOException {
        Store store = stores.activeStore(handle);
        int n = number == null ? -1 : positive(number);
        StoreOrder order = n <= 0 ? null : orderRows.findByStoreIdAndNumber(store.getId(), n);
        if (order == null || !Orders.ONLINE.equals(order.getChannel())) {
            throw new ResponseStatusException(404, "No such order");
        }
        String me = signedIn();
        boolean mine = me != null && me.equals(order.getUsername());
        boolean held = token != null && token.length() > 0 && MessageDigest.isEqual(
                linkToken(order).getBytes("UTF-8"), token.getBytes("UTF-8"));
        if (!mine && !held) {
            throw new ResponseStatusException(404, "No such order");
        }
        return order;
    }

    public void confirmation(String handle, String number, String token, Model model,
            HttpServer.Request request) throws IOException {
        StoreOrder row = visible(handle, number, token);
        Store store = stores.activeStore(handle);
        OrderDto o = orders.detailOf(row.getId());
        String currency = o.currency();
        OrderView view = new OrderView();
        view.number = o.number();
        view.email = o.email();
        view.status = describe(o);
        view.placed = Days.iso(o.createdAt());
        AddressDto a = o.address();
        if (a != null) {
            StringBuilder to = new StringBuilder();
            append(to, a.name());
            append(to, a.line1());
            append(to, a.line2());
            append(to, (a.city() + " " + a.region() + " " + a.postal()).trim());
            append(to, a.country());
            view.shipTo = to.toString();
        }
        view.shippingTitle = o.shippingTitle() == null ? "" : o.shippingTitle();
        Map<String, Integer> late = backorders.ofOrder(row.getId());
        // The lines in the language the order was placed in, as its mail.
        String locale = pages.orderLocale(store, row.getLanguage());
        for (int iter = 0; iter < o.lines().size(); iter++) {
            OrderLineDto l = o.lines().get(iter);
            CartLineView lv = new CartLineView();
            lv.id = l.id();
            pages.lineNames(lv, store, l.productId(), l.title(), l.variantTitle(), locale);
            lv.quantity = l.quantity();
            lv.unit = Money.format(l.priceCents(), currency);
            lv.total = Money.format(l.priceCents() * l.quantity(), currency);
            lv.backordered = late.containsKey(l.id());
            view.lines.add(lv);
        }
        view.subtotal = Money.format(o.subtotalCents(), currency);
        view.discount = o.discountCents() > 0 ? "-" + Money.format(o.discountCents(), currency)
                : "";
        view.shipping = Money.format(o.shippingCents(), currency);
        view.tax = Money.format(o.taxCents(), currency);
        view.total = Money.format(o.totalCents(), currency);
        view.tip = o.tipCents() > 0 ? Money.format(o.tipCents(), currency) : "";
        for (int iter = 0; o.fields() != null && iter < o.fields().size(); iter++) {
            view.fields.add(new CheckoutView.Option(o.fields().get(iter).value(),
                    o.fields().get(iter).label(), "", false));
        }
        String t = linkToken(row);
        com.codenameone.examples.bazaar.domain.Payment pay = paymentRow(row.getId());
        if (Orders.PENDING.equals(o.financial()) && Orders.OPEN.equals(o.status()) && pay != null
                && com.codenameone.examples.bazaar.pay.Payments.PROCESSING.equals(pay.getStatus())) {
            view.payPending = true;
            String self = "/s/" + SiteUrls.encode(store.getHandle()) + "/orders/" + o.number();
            if (payments.provider()
                    instanceof com.codenameone.examples.bazaar.pay.SimulatedPayments) {
                view.paySimulated = true;
                view.payAction = self + "/pay?t=" + t;
            } else {
                com.codenameone.examples.bazaar.pay.PaymentProvider.Intent intent =
                        payments.openIntent(row.getId());
                if (intent != null && com.codenameone.examples.bazaar.pay.PaymentProvider
                        .INTENT_OPEN.equals(intent.status)) {
                    view.payKey = payments.provider().publicKey();
                    view.paySecret = intent.clientSecret;
                    view.payReturn = siteUrls.absolute(request, self + "?t=" + t);
                    com.codenameone.examples.bazaar.web.security.WebHeaders.allowStripe();
                }
            }
        }
        view.invoiceUrl = "/s/" + SiteUrls.encode(store.getHandle()) + "/orders/" + o.number()
                + "/invoice.pdf?t=" + t;
        view.storeUrl = StorefrontPages.storeUrl(store.getHandle());
        view.downloads = DownloadView.of(downloads.forOrder(row.getId()));
        List<Fulfillment> shipped = fulfillments.findByOrderId(row.getId());
        for (int iter = 0; iter < shipped.size(); iter++) {
            Fulfillment f = shipped.get(iter);
            if (f.getTrackingNumber() != null && f.getTrackingNumber().length() > 0) {
                view.tracking.add(new LinkView(f.getCarrier() + " " + f.getTrackingNumber(),
                        "/track/" + SiteUrls.encode(f.getTrackingNumber()), false));
            }
        }
        PageMeta meta = pages.meta(store, request, view.storeUrl + "/orders/" + o.number());
        meta.title = meta.t.get("order") + " #" + o.number() + " - " + store.getName();
        meta.noindex = true;
        model.addAttribute("page", meta).addAttribute("order", view);
    }

    private static String describe(OrderDto o) {
        if (Orders.CANCELLED.equals(o.status())) {
            return "Cancelled";
        }
        if (Orders.FULFILLED.equals(o.fulfilment())) {
            return "Shipped";
        }
        if (Orders.PARTIAL.equals(o.fulfilment())) {
            return "Partly shipped";
        }
        return "Confirmed - we will email you when it ships";
    }

    private static void append(StringBuilder to, String part) {
        if (part != null && part.trim().length() > 0) {
            if (to.length() > 0) {
                to.append(", ");
            }
            to.append(part.trim());
        }
    }

    /// The invoice PDF of an order the caller may see.
    public HttpServer.Response invoice(String handle, String number, String token)
            throws IOException {
        StoreOrder row = visible(handle, number, token);
        return new HttpServer.Response(200, "application/pdf",
                documents.invoice(row.getStoreId(), row.getId()))
                .header("Content-Disposition", "inline; filename=\"invoice-" + row.getNumber()
                    + ".pdf\"")
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff");
    }
}
