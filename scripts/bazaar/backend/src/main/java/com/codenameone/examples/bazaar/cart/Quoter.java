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
package com.codenameone.examples.bazaar.cart;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.CartLine;
import com.codenameone.examples.bazaar.domain.CollectionProduct;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.GiftCard;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Preorder;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.geo.Geo;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.pricing.TaxCalculator;
import com.codenameone.examples.bazaar.repository.CartLineRepository;
import com.codenameone.examples.bazaar.repository.CollectionProductRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.DiscountUseRepository;
import com.codenameone.examples.bazaar.repository.GiftCardRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.PreorderRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ShippingRateRepository;
import com.codenameone.examples.bazaar.repository.ShippingZoneRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.TaxRateRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import com.codenameone.examples.bazaar.loyalty.Memberships;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Prices a cart from the catalog as it is now: the cart, checkout and draft
/// orders all go through here, so what the shopper is shown is what they pay.
///
/// The calculators in `pricing` do the arithmetic; this class only reads what
/// they need. Free shipping is weighed against the cheapest delivery for the
/// cart before discounts, and price-tiered rates are then chosen on the
/// subtotal after them.
@Component
public class Quoter {
    public static final String ACTIVE = "ACTIVE";

    private final StoreRepository stores;
    private final CartLineRepository cartLines;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final CollectionProductRepository collections;
    private final InventoryLevelRepository levels;
    private final LocationRepository locations;
    private final PreorderRepository preorders;
    private final DataSource db;
    private final DiscountRepository discounts;
    private final DiscountUseRepository uses;
    private final TaxRateRepository taxes;
    private final ShippingZoneRepository zones;
    private final ShippingRateRepository rates;
    private final GiftCardRepository giftCards;
    private final Memberships memberships;
    private final com.codenameone.examples.bazaar.taxes.TaxExemptions exemptions;
    private final com.codenameone.examples.bazaar.market.Markets markets;
    private final com.codenameone.examples.bazaar.carrier.CarrierRates carrierRates;

    public Quoter(StoreRepository stores, CartLineRepository cartLines, VariantRepository variants,
            ProductRepository products, CollectionProductRepository collections,
            InventoryLevelRepository levels, LocationRepository locations,
            DiscountRepository discounts, DiscountUseRepository uses, TaxRateRepository taxes,
            ShippingZoneRepository zones, ShippingRateRepository rates,
            GiftCardRepository giftCards, Memberships memberships,
            com.codenameone.examples.bazaar.taxes.TaxExemptions exemptions,
            com.codenameone.examples.bazaar.market.Markets markets,
            PreorderRepository preorders, DataSource db,
            com.codenameone.examples.bazaar.carrier.CarrierRates carrierRates) {
        this.carrierRates = carrierRates;
        this.preorders = preorders;
        this.db = db;
        this.stores = stores;
        this.cartLines = cartLines;
        this.variants = variants;
        this.products = products;
        this.collections = collections;
        this.levels = levels;
        this.locations = locations;
        this.discounts = discounts;
        this.uses = uses;
        this.taxes = taxes;
        this.zones = zones;
        this.rates = rates;
        this.giftCards = giftCards;
        this.memberships = memberships;
        this.exemptions = exemptions;
        this.markets = markets;
    }

    /// One wanted variant and quantity, from a cart line or a draft.
    public static final class Want {
        public final String lineId;
        public final String variantId;
        public final int quantity;

        public Want(String lineId, String variantId, int quantity) {
            this.lineId = lineId;
            this.variantId = variantId;
            this.quantity = quantity;
        }
    }

    /// Prices a saved cart.
    ///
    /// @param address where it goes, or null for no shipping and tax yet
    /// @param username the shopper, or null for a guest
    @Transactional(readOnly = true)
    public Quote price(Cart cart, AddressDto address, String rateId, String username)
            throws IOException {
        List<CartLine> rows = cartLines.findByCartId(cart.getId());
        List<Want> wants = new ArrayList<Want>();
        for (int iter = 0; iter < rows.size(); iter++) {
            CartLine row = rows.get(iter);
            wants.add(new Want(row.getId(), row.getVariantId(), row.getQuantity()));
        }
        return price(cart, wants, cart.getDiscountCode(), cart.getGiftCardCode(), address, rateId,
                username);
    }

    /// Prices `wants` at the cart's store.
    @Transactional(readOnly = true)
    public Quote price(Cart cart, List<Want> wants, String code, String giftCode,
            AddressDto address, String rateId, String username) throws IOException {
        Quote quote = new Quote();
        quote.cart = cart;
        Store store = stores.findById(cart.getStoreId());
        quote.store = store;
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < wants.size(); iter++) {
            ids.add(wants.get(iter).variantId);
        }
        Map<String, Variant> byId = new HashMap<String, Variant>();
        Map<String, Product> productById = new HashMap<String, Product>();
        if (!ids.isEmpty()) {
            List<Variant> found = variants.findByIdIn(ids);
            List<String> productIds = new ArrayList<String>();
            for (int iter = 0; iter < found.size(); iter++) {
                byId.put(found.get(iter).getId(), found.get(iter));
                productIds.add(found.get(iter).getProductId());
            }
            List<Product> ps = products.findByIdIn(productIds);
            for (int iter = 0; iter < ps.size(); iter++) {
                productById.put(ps.get(iter).getId(), ps.get(iter));
            }
        }
        Map<String, Location> locationById = new HashMap<String, Location>();
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(cart.getStoreId());
        for (int iter = 0; iter < all.size(); iter++) {
            locationById.put(all.get(iter).getId(), all.get(iter));
        }
        Map<String, Integer> availability = availability(ids, locationById);
        for (int iter = 0; iter < wants.size(); iter++) {
            Want want = wants.get(iter);
            Variant variant = byId.get(want.variantId);
            Product product = variant == null ? null : productById.get(variant.getProductId());
            if (variant == null || product == null || !cart.getStoreId().equals(variant.getStoreId())
                    || !ACTIVE.equals(product.getStatus())) {
                quote.problems.add("An item in the cart is no longer sold");
                continue;
            }
            Quote.Line line = new Quote.Line();
            line.cartLineId = want.lineId;
            line.variant = variant;
            line.product = product;
            line.price = new PriceLine(want.lineId, product.getId(), variant.getId(),
                    variant.getPriceCents(), want.quantity, variant.getWeightGrams(),
                    variant.isTaxable());
            List<CollectionProduct> in = collections.findByProductId(product.getId());
            for (int c = 0; c < in.size(); c++) {
                line.price.collectionIds.add(in.get(c).getCollectionId());
            }
            if (!variant.isTrackInventory() || variant.isAllowBackorder()) {
                line.availableQty = Integer.MAX_VALUE;
            } else {
                Integer a = availability.get(variant.getId());
                line.availableQty = a == null ? 0 : a.intValue();
                if (line.availableQty < want.quantity) {
                    // Short of stock: a variant on pre-order sells the whole
                    // line ahead of stock instead, and reserves nothing.
                    int ahead = preorderAllowance(variant.getId());
                    if (ahead >= want.quantity) {
                        line.preorder = true;
                        line.availableQty = ahead;
                    }
                }
            }
            if (!line.available()) {
                quote.problems.add(line.availableQty <= 0
                        ? product.getTitle() + " is out of stock"
                        : "Only " + line.availableQty + " of " + product.getTitle() + " left");
            }
            quote.lines.add(line);
        }
        List<PriceLine> priceLines = quote.priceLines();
        quote.subtotalCents = PriceLine.subtotal(priceLines);
        boolean hasAddress = address != null && address.country() != null
                && address.country().trim().length() > 0;
        String country = hasAddress ? address.country().trim() : null;
        boolean hasPoint = hasAddress && Geo.isPoint(address.lat(), address.lng());
        List<ShippingRate> storeRates = rates.findByStoreIdOrderByPositionAsc(cart.getStoreId());
        int weight = PriceLine.weight(priceLines);
        long cheapest = 0;
        ShippingCalculator.CarrierQuotes live = hasAddress
                ? carrierQuotes(cart, all, address) : null;
        if (hasAddress) {
            List<ShippingCalculator.Option> before = ShippingCalculator.options(
                    zones.findByStoreIdOrderByPositionAsc(cart.getStoreId()), storeRates,
                    locationById, country, hasPoint, hasPoint ? address.lat() : 0,
                    hasPoint ? address.lng() : 0, quote.subtotalCents, weight, false, live);
            cheapest = cheapestDelivery(before);
        }
        // Discounts.
        Instant now = Instant.now();
        List<Discount> automatic = discounts.findByStoreIdAndAutomatic(cart.getStoreId(), true);
        Discount typed = null;
        if (code != null && code.length() > 0) {
            typed = byCode(cart.getStoreId(), code);
        }
        Set<String> used = new HashSet<String>();
        if (username != null) {
            List<Discount> check = new ArrayList<Discount>(automatic);
            if (typed != null) {
                check.add(typed);
            }
            for (int iter = 0; iter < check.size(); iter++) {
                Discount d = check.get(iter);
                if (d.isOncePerCustomer() && uses.countByDiscountIdAndUsername(d.getId(), username) > 0) {
                    used.add(d.getId());
                }
            }
        }
        // Member prices, free shipping and early access follow the shopper's
        // loyalty tier and clubs (Memberships).
        Set<String> member = memberships.of(cart.getStoreId(), username, now);
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line line = quote.lines.get(iter);
            String locked = memberships.restriction(cart.getStoreId(), line.product.getId(),
                    new HashSet<String>(line.price.collectionIds), line.product.getTitle(), member, now);
            if (locked != null) {
                quote.problems.add(locked);
            }
        }
        quote.discounts = DiscountEngine.apply(priceLines, automatic, typed, code, now, used,
                cheapest, member);
        if (quote.discounts.codeProblem != null) {
            quote.problems.add(quote.discounts.codeProblem);
        }
        long afterDiscounts = quote.subtotalCents - quote.discountCents();
        if (hasAddress) {
            quote.options = ShippingCalculator.options(
                    zones.findByStoreIdOrderByPositionAsc(cart.getStoreId()), storeRates,
                    locationById, country, hasPoint, hasPoint ? address.lat() : 0,
                    hasPoint ? address.lng() : 0, afterDiscounts, weight,
                    quote.discounts.freeShipping, live);
            quote.shipping = ShippingCalculator.find(quote.options, rateId);
            if (quote.shipping == null) {
                if (rateId != null && rateId.length() > 0) {
                    quote.problems.add("That delivery option is not available for this address");
                }
                quote.shipping = cheapest(quote.options);
            }
            if (quote.options.isEmpty()) {
                quote.problems.add("The store does not deliver to that address");
            }
        }
        quote.shippingCents = quote.shipping == null ? 0 : quote.shipping.priceCents;
        quote.tax = TaxCalculator.apply(priceLines, quote.shippingCents,
                hasAddress ? taxes.findByStoreId(cart.getStoreId()) : null, country,
                hasAddress ? address.region() : null, store.isTaxInclusive());
        // A signed-in shopper's tax exemption covering the address takes the
        // tax off. Added tax is simply not added; in a store whose prices
        // include it, the tax inside the prices (lines and shipping) comes
        // off the total as a reduction, so the exempt buyer pays the net.
        if (hasAddress && username != null && quote.tax.totalCents() > 0) {
            String cert = exemptions.certificateFor(cart.getStoreId(), username, country,
                    address.region(), now);
            if (cert != null) {
                quote.exemptCertificate = cert;
                if (store.isTaxInclusive()) {
                    quote.exemptCents = quote.tax.totalCents();
                }
                for (int iter = 0; iter < priceLines.size(); iter++) {
                    priceLines.get(iter).taxCents = 0;
                }
                quote.tax = TaxCalculator.apply(priceLines, quote.shippingCents, null, country,
                        address.region(), store.isTaxInclusive());
            }
        }
        if (hasAddress) {
            List<String[]> dutiable = new ArrayList<String[]>();
            for (int iter = 0; iter < priceLines.size(); iter++) {
                PriceLine pl = priceLines.get(iter);
                dutiable.add(new String[] {pl.productId, Long.toString(pl.netCents())});
            }
            quote.dutyCents = markets.dutyEstimate(cart.getStoreId(), country, dutiable);
        }
        quote.totalCents = Math.max(0, afterDiscounts + quote.shippingCents
                + quote.tax.addedCents() - quote.exemptCents);
        if (giftCode != null && giftCode.length() > 0) {
            GiftCard card = giftCards.findByCode(giftCode);
            if (usable(card, cart.getStoreId(), now)) {
                quote.giftCard = card;
                quote.giftCardCents = Math.min(card.getBalanceCents(), quote.totalCents);
            } else {
                quote.problems.add("That gift card cannot be used");
            }
        }
        return quote;
    }

    /// Whether a gift card can pay at a store now.
    public static boolean usable(GiftCard card, String storeId, Instant now) {
        return card != null && card.isActive() && storeId.equals(card.getStoreId())
                && card.getBalanceCents() > 0
                && (card.getExpiresAt() == null || card.getExpiresAt().toEpochMilli() <= 0
                    || now.isBefore(card.getExpiresAt()));
    }

    /// The store's discount with this code, ignoring ASCII case, or null.
    public Discount byCode(String storeId, String code) throws IOException {
        String wanted = Codes.normalize(code);
        List<Discount> found = discounts.findByStoreIdAndCode(storeId, wanted);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        List<Discount> every = discounts.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = 0; iter < every.size(); iter++) {
            if (wanted.equalsIgnoreCase(every.get(iter).getCode())) {
                return every.get(iter);
            }
        }
        return null;
    }

    /// Prices CARRIER rates through [CarrierRates]: from the store's first
    /// active location that fulfils online orders (the configured origin's
    /// city otherwise) to `address`, cached per cart and address. The
    /// carrier is asked at most once per quote and only when the address's
    /// zone has a CARRIER rate.
    private ShippingCalculator.CarrierQuotes carrierQuotes(Cart cart, List<Location> all,
            AddressDto address) {
        Location origin = null;
        for (int iter = 0; iter < all.size() && origin == null; iter++) {
            Location l = all.get(iter);
            if (l.isActive() && l.isFulfilsOnline()) {
                origin = l;
            }
        }
        final com.codenameone.examples.bazaar.carrier.CarrierProvider.Place from = origin == null
                ? new com.codenameone.examples.bazaar.carrier.CarrierProvider.Place("", "", "", 0, 0)
                : new com.codenameone.examples.bazaar.carrier.CarrierProvider.Place(
                    origin.getName(), origin.getAddress(), origin.getName(), origin.getLat(),
                    origin.getLng());
        final com.codenameone.examples.bazaar.carrier.CarrierProvider.Place to =
                new com.codenameone.examples.bazaar.carrier.CarrierProvider.Place(
                    address.name(), address.line1(), address.city(),
                    Geo.isPoint(address.lat(), address.lng()) ? address.lat() : 0,
                    Geo.isPoint(address.lat(), address.lng()) ? address.lng() : 0,
                    address.postal(), address.region(), address.country());
        final String cartKey = cart.getId() == null || cart.getId().length() == 0
                ? cart.getStoreId() : cart.getId();
        return (rate, weightGrams) -> carrierRates.quotes(cartKey, weightGrams, from, to);
    }

    private static long cheapestDelivery(List<ShippingCalculator.Option> options) {
        long best = -1;
        for (int iter = 0; iter < options.size(); iter++) {
            ShippingCalculator.Option o = options.get(iter);
            if (ShippingCalculator.PICKUP.equals(o.rate.getKind())) {
                continue;
            }
            if (best < 0 || o.priceCents < best) {
                best = o.priceCents;
            }
        }
        return Math.max(0, best);
    }

    private static ShippingCalculator.Option cheapest(List<ShippingCalculator.Option> options) {
        ShippingCalculator.Option best = null;
        for (int iter = 0; iter < options.size(); iter++) {
            if (best == null || options.get(iter).priceCents < best.priceCents) {
                best = options.get(iter);
            }
        }
        return best;
    }

    /// Units of a variant that may still be sold ahead of stock: the
    /// pre-order allowance ([com.codenameone.examples.bazaar.alerts.StockAlerts#preorderable])
    /// less the pre-ordered units already sold and not yet shipped. Zero when
    /// the variant is not on pre-order.
    @Transactional(readOnly = true)
    public int preorderAllowance(String variantId) throws IOException {
        Preorder p = variantId == null ? null : preorders.findByVariantId(variantId);
        int cap = com.codenameone.examples.bazaar.alerts.StockAlerts.preorderable(p,
                System.currentTimeMillis());
        if (cap <= 0) {
            return 0;
        }
        Map sold = db.queryOne("SELECT COALESCE(SUM(l.quantity - l.fulfilled_qty), 0) AS n "
                + "FROM order_line l JOIN store_order o ON o.id = l.order_id WHERE l.variant_id = ? "
                + "AND l.preorder = 1 AND l.quantity > l.fulfilled_qty AND o.status <> 'CANCELLED'",
                new Object[] {variantId});
        long n = sold == null ? 0 : com.codenameone.examples.bazaar.Rows.number(sold.get("n"));
        return (int) Math.max(0, cap - n);
    }

    /// Units available per variant across the active locations that fulfil
    /// online orders.
    private Map<String, Integer> availability(List<String> variantIds,
            Map<String, Location> locationById) throws IOException {
        Map<String, Integer> out = new HashMap<String, Integer>();
        if (variantIds.isEmpty()) {
            return out;
        }
        List<InventoryLevel> found = levels.findByVariantIdIn(variantIds);
        for (int iter = 0; iter < found.size(); iter++) {
            InventoryLevel level = found.get(iter);
            Location at = locationById.get(level.getLocationId());
            if (at == null || !at.isActive() || !at.isFulfilsOnline()) {
                continue;
            }
            int a = Math.max(0, level.getOnHand() - level.getCommitted());
            Integer had = out.get(level.getVariantId());
            out.put(level.getVariantId(), Integer.valueOf((had == null ? 0 : had.intValue()) + a));
        }
        return out;
    }
}
