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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosShipQuoteDto;
import com.codenameone.examples.bazaar.api.PosShipSaleDto;
import com.codenameone.examples.bazaar.api.PosShipOptionDto;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosSaleResultDto;
import com.codenameone.examples.bazaar.api.PosSyncDto;
import com.codenameone.examples.bazaar.api.PosSyncResultDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.cart.Quoter;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.CardPayment;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.DiscountUse;
import com.codenameone.examples.bazaar.domain.GiftCard;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.PosApproval;
import com.codenameone.examples.bazaar.domain.PosPolicy;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.Shift;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Tender;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.pricing.TaxCalculator;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.DiscountUseRepository;
import com.codenameone.examples.bazaar.repository.GiftCardRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ShiftRepository;
import com.codenameone.examples.bazaar.repository.ShippingRateRepository;
import com.codenameone.examples.bazaar.repository.ShippingZoneRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.TaxRateRepository;
import com.codenameone.examples.bazaar.repository.TenderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.events.StoreEvents;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/// Sales a register uploads, possibly long after it made them offline.
///
/// Each sale names itself with a `clientId` the register made, which becomes
/// the order's unique `client_id`: a batch sent twice (the answer was lost,
/// the register retried) finds the orders the first one wrote and answers
/// DUPLICATE, and stock moves once. A sale is never refused for want of
/// stock -- the goods already left with the customer -- so on hand may go
/// below zero; such a sale is answered `oversold` and an audit event is kept.
///
/// The sale is priced again here with the same [DiscountEngine] and
/// [TaxCalculator] the online checkout uses, at the prices the register sold
/// at, and taxed where the register is. Tenders are taken gift cards first,
/// then store credit, then the card, and cash covers the rest with change.
/// Everything a sale writes -- the order, its lines and tenders, the gift
/// card and store credit debits, the card payment, stock and the shift's
/// totals -- is one transaction: a sale is written whole or not at all.
@Component
public class PosSales {
    public static final String OK = "OK";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String ERROR = "ERROR";
    /// A sale refused only for want of a manager's approval: the register
    /// can ask for one and send the same sale again.
    public static final String APPROVAL = "APPROVAL";
    /// Sales in one upload.
    static final int MAX_BATCH = 200;
    static final int MAX_LINES = 100;

    private final Pos pos;
    private final StoreAccess access;
    private final StoreRepository stores;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final TaxRateRepository taxes;
    private final DiscountRepository discounts;
    private final DiscountUseRepository uses;
    private final CustomerRepository customers;
    private final GiftCardRepository giftCardRows;
    private final StoreOrderRepository orderRows;
    private final OrderLineRepository lineRows;
    private final TenderRepository tenders;
    private final ShiftRepository shifts;
    private final AuditEventRepository audits;
    private final Quoter quoter;
    private final Orders orders;
    private final Inventory inventory;
    private final GiftCards giftCards;
    private final Payments payments;
    private final StoreEvents storeEvents;
    private final PosTools tools;
    private final CardReaders readers;
    private final ShippingZoneRepository zones;
    private final ShippingRateRepository rates;
    private final LocationRepository locations;

    public PosSales(Pos pos, StoreAccess access, StoreRepository stores,
            VariantRepository variants, ProductRepository products, TaxRateRepository taxes,
            DiscountRepository discounts, DiscountUseRepository uses,
            CustomerRepository customers, GiftCardRepository giftCardRows,
            StoreOrderRepository orderRows, OrderLineRepository lineRows,
            TenderRepository tenders, ShiftRepository shifts, AuditEventRepository audits,
            Quoter quoter, Orders orders, Inventory inventory, GiftCards giftCards,
            Payments payments, StoreEvents storeEvents, PosTools tools, CardReaders readers,
            ShippingZoneRepository zones, ShippingRateRepository rates,
            LocationRepository locations) {
        this.pos = pos;
        this.access = access;
        this.stores = stores;
        this.variants = variants;
        this.products = products;
        this.taxes = taxes;
        this.discounts = discounts;
        this.uses = uses;
        this.customers = customers;
        this.giftCardRows = giftCardRows;
        this.orderRows = orderRows;
        this.lineRows = lineRows;
        this.tenders = tenders;
        this.shifts = shifts;
        this.audits = audits;
        this.quoter = quoter;
        this.orders = orders;
        this.inventory = inventory;
        this.giftCards = giftCards;
        this.payments = payments;
        this.storeEvents = storeEvents;
        this.tools = tools;
        this.readers = readers;
        this.zones = zones;
        this.rates = rates;
        this.locations = locations;
    }

    /// Records a batch; one result per sale, in the order sent. A sale that
    /// fails is answered ERROR with the reason and leaves nothing behind; the
    /// rest of the batch goes on.
    public PosSyncResultDto sync(String username, String storeId, String registerId,
            PosSyncDto batch) throws IOException {
        Register register = check(username, storeId, registerId);
        if (batch == null || batch.sales() == null) {
            throw new ResponseStatusException(400, "Send the sales");
        }
        if (batch.sales().size() > MAX_BATCH) {
            throw new ResponseStatusException(400, "At most " + MAX_BATCH + " sales at once");
        }
        List<PosSaleResultDto> out = new ArrayList<PosSaleResultDto>();
        for (int iter = 0; iter < batch.sales().size(); iter++) {
            PosSaleDto sale = batch.sales().get(iter);
            String clientId = sale == null ? "" : sale.clientId();
            try {
                out.add(sale(username, storeId, register, sale));
            } catch (ResponseStatusException refused) {
                out.add(new PosSaleResultDto(clientId, approvalRefusal(refused) ? APPROVAL : ERROR,
                        "", 0, 0, 0, false, refused.getMessage()));
            }
        }
        return new PosSyncResultDto(out);
    }

    @Transactional(readOnly = true)
    private Register check(String username, String storeId, String registerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return pos.register(storeId, registerId);
    }

    /// The `client_id` a register's sale id becomes: per store, so two
    /// stores' registers never meet, and of a fixed length whatever the
    /// register sent.
    static String clientId(String storeId, String clientId) throws IOException {
        return "p" + Ids.sha256(storeId + "\n" + clientId).substring(0, 63);
    }

    /// A sale's tenders, sorted out and checked before anything is written.
    private static final class Plan {
        GiftCard giftCard;
        long giftCents;
        Customer customer;
        long creditCents;
        long cardCents;
        String cardReference = "";
        long cashTendered;
        long cashCents;
        long changeCents;
        boolean oversold;
        /// What the card, gift card and credit paid past this server's total.
        long overCents;
        /// What the tenders fell short of this server's total, within tolerance().
        long shortCents;
        final List<String> oversoldSkus = new ArrayList<String>();
        /// Who made the sale, and the manager approval it spends (null for none).
        StoreStaff seller;
        PosApproval approval;
        long tipCents;
        /// The payment the register's paired reader took, when the card
        /// tender names one ([CardReaders]); null for a standalone terminal.
        CardPayment readerPayment;
        /// Where a sale shipped to the customer goes, or null for one handed over.
        AddressDto shipTo;
    }

    /// How far a register's total may fall short of this server's before the
    /// sale is refused: the larger of one currency unit (100 minor units) and
    /// 1% of the total. A stale tax rate or a rounding rule applied per line
    /// rather than per sale moves a total by cents, not by more than a
    /// percent; anything larger is a sale that was not paid for.
    public static long tolerance(long totalCents) {
        return Math.max(100L, Math.max(0L, totalCents) / 100L);
    }

    private PosSaleResultDto sale(String username, String storeId, Register register,
            PosSaleDto sale) throws IOException {
        return sale(username, storeId, register, sale, null, null);
    }

    /// One sale; `shipTo` and `rateId` make it a sale shipped to the customer.
    private PosSaleResultDto sale(String username, String storeId, Register register,
            PosSaleDto sale, AddressDto shipTo, String rateId) throws IOException {
        if (sale == null || sale.clientId() == null || sale.clientId().trim().length() == 0
                || sale.clientId().length() > 100) {
            throw new ResponseStatusException(400, "A sale needs its register's id for it");
        }
        final String clientId = clientId(storeId, sale.clientId().trim());
        StoreOrder again = existing(clientId);
        if (again != null) {
            return duplicate(sale, again);
        }
        final Quote quote = shipTo == null ? price(storeId, register.getTaxCountry(),
                register.getTaxRegion(), sale.lines(), sale.discountCode())
                : shipped(storeId, register, sale.lines(), sale.discountCode(), shipTo, rateId);
        final Shift shift = shiftFor(storeId, register, sale.shiftId());
        final Plan plan = plan(username, storeId, register, sale, quote);
        plan.shipTo = shipTo;
        final String staffName = plan.seller.getUsername();
        StoreOrder order = orders.newOrder(quote, Orders.POS,
                plan.customer == null ? "" : plan.customer.getUsername(),
                sale.email() != null && sale.email().trim().length() > 0 ? sale.email().trim()
                        : plan.customer == null ? "" : plan.customer.getEmail(),
                shipTo, clientId);
        // Shipped from the register's location, the sale stays open until it
        // is packed and shipped like an online order.
        order.setStatus(shipTo == null ? Orders.CLOSED : Orders.OPEN);
        order.setFinancial(Orders.PAID);
        order.setFulfilment(shipTo == null ? Orders.FULFILLED : Orders.UNFULFILLED);
        order.setCommissionBp(tools.rateFor(storeId, plan.seller.getUsername()));
        order.setLocationId(register.getLocationId());
        order.setRegisterId(register.getId());
        order.setShiftId(shift.getId());
        order.setStaffUsername(plan.seller.getUsername());
        order.setTipCents(plan.tipCents);
        order.setCustomerId(plan.customer == null ? "" : plan.customer.getId());
        order.setNote(Codes.cut(sale.note() == null ? "" : sale.note(), 1000));
        long now = System.currentTimeMillis();
        // When the register made the sale; a clock far ahead is not believed.
        Instant at = sale.createdAt() > 0 && sale.createdAt() <= now + 300000L
                ? Instant.ofEpochMilli(sale.createdAt()) : Instant.ofEpochMilli(now);
        order.setCreatedAt(at);
        if (shipTo == null) {
            order.setClosedAt(at);
        }
        final String[] where = new String[quote.lines.size()];
        for (int iter = 0; iter < where.length; iter++) {
            where[iter] = register.getLocationId();
        }
        final Register reg = register;
        StoreOrder placed = orders.insertNumbered(order, new Orders.Writer() {
            @Override
            public void write(StoreOrder o) throws IOException {
                writeSale(o, quote, plan, where, reg, shift, staffName);
            }
        });
        if (placed == null) {
            StoreOrder first = existing(clientId);
            if (first != null) {
                return duplicate(sale, first);
            }
            throw new ResponseStatusException(409, "The sale is being recorded; send it again");
        }
        orders.publish(placed);
        storeEvents.orderPlaced(placed.getId());
        return new PosSaleResultDto(sale.clientId(), OK, placed.getId(), placed.getNumber(),
                placed.getTotalCents(), plan.changeCents, plan.oversold,
                message(plan));
    }

    private static String message(Plan plan) {
        StringBuilder out = new StringBuilder();
        if (plan.oversold) {
            out.append("Sold more than the location had: ").append(join(plan.oversoldSkus));
        }
        if (plan.overCents > 0 || plan.shortCents > 0) {
            out.append(out.length() > 0 ? ". " : "").append("Total adjusted to the server's by ")
                    .append(plan.overCents > 0 ? plan.overCents : -plan.shortCents)
                    .append(" cents");
        }
        return out.toString();
    }

    private static PosSaleResultDto duplicate(PosSaleDto sale, StoreOrder order) {
        return new PosSaleResultDto(sale.clientId(), DUPLICATE, order.getId(), order.getNumber(),
                order.getTotalCents(), 0, false, "Already recorded");
    }

    private static String join(List<String> parts) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < parts.size(); iter++) {
            out.append(iter == 0 ? "" : ", ").append(parts.get(iter));
        }
        return out.toString();
    }

    @Transactional(readOnly = true)
    private StoreOrder existing(String clientId) {
        return orderRows.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    private Shift shiftFor(String storeId, Register register, String shiftId) {
        if (shiftId == null || shiftId.length() == 0) {
            throw new ResponseStatusException(400, "The sale names no shift");
        }
        Shift shift = pos.shiftOf(storeId, shiftId);
        if (!register.getId().equals(shift.getRegisterId())) {
            throw new ResponseStatusException(400, "That shift is another register's");
        }
        // A closed shift still takes a sale it made before it closed: the
        // register may have been offline at closing time. Its totals move, and
        // the Z report printed again shows them.
        return shift;
    }

    /// Prices the sale: the register's prices (the catalog's when it sent
    /// none), the store's discounts, the register's tax, no shipping.
    @Transactional(readOnly = true)
    public Quote price(String storeId, String taxCountry, String taxRegion, List<PosLineDto> wanted,
            String discountCode) throws IOException {
        return price(storeId, taxCountry, taxRegion, wanted, discountCode, null);
    }

    /// [#price], with `shipping` added to the total and taxed as the store
    /// taxes shipping.
    @Transactional(readOnly = true)
    Quote price(String storeId, String taxCountry, String taxRegion, List<PosLineDto> wanted,
            String discountCode, ShippingCalculator.Option shipping) throws IOException {
        if (wanted == null || wanted.isEmpty()) {
            throw new ResponseStatusException(400, "The sale has no lines");
        }
        if (wanted.size() > MAX_LINES) {
            throw new ResponseStatusException(400, "Too many lines in one sale");
        }
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < wanted.size(); iter++) {
            PosLineDto l = wanted.get(iter);
            if (l == null || l.quantity() <= 0 || l.quantity() > 10000 || l.unitCents() < 0) {
                throw new ResponseStatusException(400, "A line of the sale is not an item and a quantity");
            }
            if (custom(l)) {
                if (l.title() == null || l.title().trim().length() == 0 || l.unitCents() <= 0) {
                    throw new ResponseStatusException(400, "A custom item needs a title and a price");
                }
            } else {
                ids.add(l.variantId());
            }
        }
        Map<String, Variant> byId = new HashMap<String, Variant>();
        List<Variant> found = ids.isEmpty() ? new ArrayList<Variant>() : variants.findByIdIn(ids);
        List<String> productIds = new ArrayList<String>();
        for (int iter = 0; iter < found.size(); iter++) {
            byId.put(found.get(iter).getId(), found.get(iter));
            productIds.add(found.get(iter).getProductId());
        }
        Map<String, Product> productById = new HashMap<String, Product>();
        List<Product> ps = productIds.isEmpty() ? new ArrayList<Product>()
                : products.findByIdIn(productIds);
        for (int iter = 0; iter < ps.size(); iter++) {
            productById.put(ps.get(iter).getId(), ps.get(iter));
        }
        Quote quote = new Quote();
        Store store = stores.findById(storeId);
        quote.store = store;
        for (int iter = 0; iter < wanted.size(); iter++) {
            PosLineDto l = wanted.get(iter);
            Variant v = custom(l) ? customVariant(storeId, l) : byId.get(l.variantId());
            Product p = v == null ? null : custom(l) ? customProduct(l)
                    : productById.get(v.getProductId());
            // Sold even when archived since: the register sold what it had.
            if (v == null || p == null || !storeId.equals(v.getStoreId())) {
                throw new ResponseStatusException(400, "An item of the sale is not the store's");
            }
            Quote.Line line = new Quote.Line();
            line.cartLineId = Integer.toString(iter);
            line.variant = v;
            line.product = p;
            line.price = new PriceLine(line.cartLineId, p.getId(), v.getId(),
                    l.unitCents() > 0 ? l.unitCents() : v.getPriceCents(), l.quantity(),
                    v.getWeightGrams(), v.isTaxable());
            line.availableQty = Integer.MAX_VALUE;
            quote.lines.add(line);
        }
        List<PriceLine> priceLines = quote.priceLines();
        quote.subtotalCents = PriceLine.subtotal(priceLines);
        String code = discountCode == null ? "" : discountCode.trim();
        Discount typed = code.length() > 0 ? quoter.byCode(storeId, code) : null;
        quote.discounts = DiscountEngine.apply(priceLines,
                discounts.findByStoreIdAndAutomatic(storeId, true), typed,
                code.length() > 0 ? code : null, Instant.now(), new HashSet<String>(), 0);
        quote.shipping = shipping;
        quote.shippingCents = shipping == null ? 0 : shipping.priceCents;
        String country = taxCountry == null ? "" : taxCountry;
        quote.tax = TaxCalculator.apply(priceLines, quote.shippingCents,
                country.length() == 0 ? null : taxes.findByStoreId(storeId),
                country.length() == 0 ? null : country, taxRegion,
                store.isTaxInclusive());
        quote.totalCents = quote.subtotalCents - quote.discountCents() + quote.shippingCents
                + quote.tax.addedCents();
        return quote;
    }

    // --------------------------------------------------------- ship to customer

    /// The ways the lines can be sent to `address`, from the store's own
    /// zones and rates; never a pickup, which is the counter itself.
    @Transactional(readOnly = true)
    public List<PosShipOptionDto> shipQuote(String username, String storeId, String registerId,
            PosShipQuoteDto ask) throws IOException {
        Register register = check(username, storeId, registerId);
        if (ask == null) {
            throw new ResponseStatusException(400, "Send the lines and the address");
        }
        Quote goods = price(storeId, register.getTaxCountry(), register.getTaxRegion(),
                ask.lines(), "");
        List<ShippingCalculator.Option> options = shipOptions(storeId, goods, ask.address());
        List<PosShipOptionDto> out = new ArrayList<PosShipOptionDto>();
        for (int iter = 0; iter < options.size(); iter++) {
            ShippingCalculator.Option o = options.get(iter);
            Quote whole = price(storeId, ask.address().country().trim(), ask.address().region(),
                    ask.lines(), "", o);
            out.add(new PosShipOptionDto(o.id, o.name, o.priceCents, whole.tax.addedCents(),
                    whole.totalCents, o.etaDaysMin, o.etaDaysMax));
        }
        return out;
    }

    /// Records a sale shipped to the customer: paid at the counter, open
    /// until the register's location packs and ships it.
    public PosSaleResultDto shipSale(String username, String storeId, String registerId,
            PosShipSaleDto in) throws IOException {
        Register register = check(username, storeId, registerId);
        if (in == null) {
            throw new ResponseStatusException(400, "Send the sale");
        }
        PosSaleDto sale = new PosSaleDto(in.clientId(), in.shiftId(), in.customerId(), in.email(),
                "", in.note(), System.currentTimeMillis(), in.lines(), in.tenders(),
                in.staffUsername(), 0, in.approvalId());
        try {
            return sale(username, storeId, register, sale, in.address(), in.shippingRateId());
        } catch (ResponseStatusException refused) {
            return new PosSaleResultDto(in.clientId(), approvalRefusal(refused) ? APPROVAL : ERROR,
                    "", 0, 0, 0, false, refused.getMessage());
        }
    }

    /// The quote of a sale shipped to `address` by rate `rateId`: taxed
    /// where it goes, with the shipping in the total.
    private Quote shipped(String storeId, Register register, List<PosLineDto> lines,
            String discountCode, AddressDto address, String rateId) throws IOException {
        if (address == null || blank(address.name()) || blank(address.line1())
                || blank(address.city()) || blank(address.country())) {
            throw new ResponseStatusException(400, "A shipped sale needs a name, street, city "
                    + "and country");
        }
        Quote goods = price(storeId, register.getTaxCountry(), register.getTaxRegion(), lines,
                discountCode);
        ShippingCalculator.Option chosen = ShippingCalculator.find(
                shipOptions(storeId, goods, address), rateId);
        if (chosen == null) {
            throw new ResponseStatusException(400, "That delivery option is not available for "
                    + "this address");
        }
        return price(storeId, address.country().trim(), address.region(), lines, discountCode,
                chosen);
    }

    @Transactional(readOnly = true)
    private List<ShippingCalculator.Option> shipOptions(String storeId, Quote goods,
            AddressDto address) {
        List<ShippingCalculator.Option> out = new ArrayList<ShippingCalculator.Option>();
        if (address == null || blank(address.country())) {
            return out;
        }
        Map<String, Location> byId = new HashMap<String, Location>();
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            byId.put(all.get(iter).getId(), all.get(iter));
        }
        boolean point = address.lat() != 0 || address.lng() != 0;
        List<ShippingCalculator.Option> options = ShippingCalculator.options(
                zones.findByStoreIdOrderByPositionAsc(storeId),
                rates.findByStoreIdOrderByPositionAsc(storeId), byId, address.country().trim(),
                point, address.lat(), address.lng(), goods.subtotalCents - goods.discountCents(),
                PriceLine.weight(goods.priceLines()), goods.discounts != null
                    && goods.discounts.freeShipping, null);
        for (int iter = 0; iter < options.size(); iter++) {
            if (!ShippingCalculator.PICKUP.equals(options.get(iter).rate.getKind())) {
                out.add(options.get(iter));
            }
        }
        return out;
    }

    private static boolean blank(String text) {
        return text == null || text.trim().length() == 0;
    }

    /// Whether a line is a custom sale item: a title and a price, no SKU.
    public static boolean custom(PosLineDto l) {
        return l.variantId() == null || l.variantId().length() == 0;
    }

    /// The stand-in variant a custom item is priced and written with: no id,
    /// so nothing is stocked, reserved or restocked for it.
    private static Variant customVariant(String storeId, PosLineDto l) {
        Variant v = new Variant();
        v.setId("");
        v.setStoreId(storeId);
        v.setProductId("");
        v.setTitle("");
        v.setSku("");
        v.setPriceCents(l.unitCents());
        v.setTaxable(l.taxable());
        v.setTrackInventory(false);
        return v;
    }

    private static Product customProduct(PosLineDto l) {
        Product p = new Product();
        p.setId("");
        p.setTitle(Codes.cut(l.title().trim(), 200));
        return p;
    }

    /// Sorts the tenders out and checks they pay the total, before anything
    /// is written.
    @Transactional(readOnly = true)
    private Plan plan(String username, String storeId, Register register, PosSaleDto sale,
            Quote quote)
            throws IOException {
        Plan plan = new Plan();
        plan.seller = tools.seller(storeId, sale.staffUsername(), username);
        if (sale.tipCents() < 0) {
            throw new ResponseStatusException(400, "A tip cannot be negative");
        }
        plan.tipCents = sale.tipCents();
        plan.approval = approvalFor(storeId, plan.seller, sale, quote);
        if (sale.customerId() != null && sale.customerId().length() > 0) {
            plan.customer = customers.findById(sale.customerId());
            if (plan.customer == null || !storeId.equals(plan.customer.getStoreId())) {
                throw new ResponseStatusException(400, "No such customer");
            }
        }
        List<PosTenderDto> ts = sale.tenders() == null ? new ArrayList<PosTenderDto>()
                : sale.tenders();
        // A tip rides on the card: it is paid, but it is not part of the total.
        long due = quote.totalCents + plan.tipCents;
        for (int iter = 0; iter < ts.size(); iter++) {
            PosTenderDto t = ts.get(iter);
            if (t == null || t.kind() == null || t.amountCents() <= 0) {
                throw new ResponseStatusException(400, "A tender needs a kind and an amount");
            }
            String kind = t.kind();
            if ("CASH".equalsIgnoreCase(kind)) {
                plan.cashTendered += t.amountCents();
            } else if ("CARD".equalsIgnoreCase(kind)) {
                if (plan.cardCents > 0) {
                    throw new ResponseStatusException(400, "One card payment per sale");
                }
                plan.cardCents = t.amountCents();
                plan.cardReference = t.reference() == null ? "" : Codes.cut(t.reference(), 40);
                plan.readerPayment = readers.forSale(storeId, plan.cardReference,
                        plan.cardCents);
            } else if ("GIFT_CARD".equalsIgnoreCase(kind)) {
                if (plan.giftCard != null) {
                    throw new ResponseStatusException(400, "One gift card per sale");
                }
                String clean = Codes.normalize(t.reference());
                GiftCard card = clean.length() == 0 ? null : giftCardRows.findByCode(clean);
                if (!Quoter.usable(card, storeId, Instant.now())
                        || card.getBalanceCents() < t.amountCents()) {
                    throw new ResponseStatusException(409, "The gift card cannot pay that much");
                }
                plan.giftCard = card;
                plan.giftCents = t.amountCents();
            } else if ("STORE_CREDIT".equalsIgnoreCase(kind)) {
                if (plan.customer == null) {
                    throw new ResponseStatusException(400, "Store credit needs the customer");
                }
                plan.creditCents += t.amountCents();
                if (plan.creditCents > plan.customer.getStoreCreditCents()) {
                    throw new ResponseStatusException(409, "The customer has less store credit");
                }
            } else {
                throw new ResponseStatusException(400, "A tender is CASH, CARD, GIFT_CARD or STORE_CREDIT");
            }
        }
        // The register priced the sale with its own copy of the tax rates and
        // discounts, possibly offline and possibly stale, and the card was
        // charged that total on the terminal before this server saw it. The
        // sale happened: a card, gift card or credit paying more than this
        // server's total is recorded as an over-tender, never refused, and a
        // shortfall within tolerance() is recorded as a rounding difference.
        // Only a shortfall past the tolerance is refused -- that sale was
        // genuinely not paid for.
        // tag::bazaar-pos-sync[]
        if (plan.tipCents > 0 && plan.cardCents < plan.tipCents) {
            throw new ResponseStatusException(400, "A tip is taken on the card");
        }
        long other = plan.giftCents + plan.creditCents + plan.cardCents;
        long rest = Math.max(0, due - other);
        plan.overCents = Math.max(0, other - due);
        plan.cashCents = Math.min(rest, plan.cashTendered);
        plan.changeCents = plan.cashTendered - plan.cashCents;
        long shortCents = rest - plan.cashCents;
        if (shortCents > tolerance(due)) {
            throw new ResponseStatusException(400, "The tenders do not cover the total of "
                    + due + " cents");
        }
        plan.shortCents = shortCents;
        // end::bazaar-pos-sync[]
        // Units sold that the location does not have: the sale stands.
        Map<String, Integer> sold = new HashMap<String, Integer>();
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line l = quote.lines.get(iter);
            Integer before = sold.get(l.variant.getId());
            sold.put(l.variant.getId(), Integer.valueOf((before == null ? 0 : before.intValue())
                    + l.price.quantity));
        }
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Variant v = quote.lines.get(iter).variant;
            Integer qty = sold.remove(v.getId());
            if (qty != null && v.isTrackInventory()
                    && inventory.available(v.getId(), register.getLocationId()) < qty.intValue()) {
                plan.oversold = true;
                plan.oversoldSkus.add(v.getSku().length() > 0 ? v.getSku() : v.getId());
            }
        }
        return plan;
    }

    /// The approval a sale needs and names, or null when it needs none. A
    /// sale needs one, unless its seller is an owner or a manager, when the
    /// store's policy says so: a hand-changed price under
    /// `approvePriceOverrides`, or more off the list price than
    /// `discountApprovalBp` (hand-changed prices, the sale-wide percent or
    /// amount spread over the lines, and discounts together).
    private PosApproval approvalFor(String storeId, StoreStaff seller, PosSaleDto sale,
            Quote quote) {
        if (PosTools.approvesItself(seller)) {
            return null;
        }
        PosPolicy policy = tools.policyOf(storeId);
        long list = 0;
        long byHand = 0;
        boolean overridden = false;
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line l = quote.lines.get(iter);
            PosLineDto sent = sale.lines().get(iter);
            long catalog = custom(sent) ? l.price.unitCents : l.variant.getPriceCents();
            list += catalog * l.price.quantity;
            // Every line sold under its list price counts towards the
            // threshold, flagged or not: a percent or an amount off the
            // whole sale reaches the server only as lower unit prices on
            // lines the register never flagged, and ignoring them let a
            // clerk give any discount without a manager. The flag still
            // decides `approvePriceOverrides`, which is about typed prices.
            byHand += Math.max(0, (catalog - l.price.unitCents) * l.price.quantity);
            if (sent.overridden()) {
                overridden = true;
            }
        }
        boolean needs = overridden && policy.isApprovePriceOverrides();
        long off = byHand + quote.discountCents();
        long bp = policy.getDiscountApprovalBp();
        if (bp > 0 && list > 0 && off * 10000L > bp * list) {
            needs = true;
        }
        if (!needs) {
            return null;
        }
        try {
            return tools.requireApproval(storeId, sale.approvalId(), PosTools.DISCOUNT,
                    PosTools.PRICE_OVERRIDE);
        } catch (ResponseStatusException refused) {
            throw new ApprovalRefused(refused.getStatus(), refused.getMessage());
        }
    }

    /// Whether `refused` says the sale lacks a usable approval, and nothing else.
    static boolean approvalRefusal(ResponseStatusException refused) {
        return refused instanceof ApprovalRefused;
    }

    /// The refusal of a sale whose discounts need an approval it does not
    /// carry (or carries spent or for something else). Kept apart from every
    /// other refusal so the upload answers [#APPROVAL] for it.
    static final class ApprovalRefused extends ResponseStatusException {
        private static final long serialVersionUID = 1L;

        ApprovalRefused(int status, String reason) {
            super(status, reason);
        }
    }

    /// Everything a sale writes, inside the transaction that inserts its order.
    private void writeSale(StoreOrder o, Quote quote, Plan plan, String[] where,
            Register register, Shift shift, String staffName) throws IOException {
        String storeId = o.getStoreId();
        orders.insertLines(o, quote, where);
        List<OrderLine> written = lineRows.findByOrderId(o.getId());
        for (int iter = 0; plan.shipTo == null && iter < written.size(); iter++) {
            OrderLine line = written.get(iter);
            line.setFulfilledQty(line.getQuantity());
            lineRows.save(line);
        }
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line l = quote.lines.get(iter);
            if (l.variant.getId().length() == 0) {
                continue;
            }
            if (plan.shipTo != null) {
                // Held at the register's location for packing, as an online
                // order's stock is; shipping it takes it off the shelf.
                if (inventory.reserveCounted(storeId, l.variant.getId(), register.getLocationId(),
                        l.price.quantity, o.getId(), staffName) < 0) {
                    throw new ResponseStatusException(409, "Not enough " + l.product.getTitle()
                            + " at this location to ship");
                }
                continue;
            }
            inventory.recordPosSale(storeId, l.variant.getId(), register.getLocationId(),
                    l.price.quantity, o.getId(), staffName);
        }
        if (quote.discounts != null) {
            for (int iter = 0; iter < quote.discounts.applied.size(); iter++) {
                Discount d = quote.discounts.applied.get(iter).discount;
                // Counted, never refused: the customer already had the discount.
                discounts.useIf(d.getId());
                DiscountUse use = new DiscountUse();
                use.setId(Ids.next());
                use.setDiscountId(d.getId());
                use.setCustomerId(plan.customer == null ? "" : plan.customer.getId());
                use.setUsername(o.getUsername());
                use.setOrderId(o.getId());
                use.setCreatedAt(Instant.now());
                uses.insert(use);
            }
        }
        if (plan.approval != null) {
            tools.use(storeId, plan.approval.getId(), o.getId(), staffName);
            orders.event(o.getId(), "APPROVED", plan.approval.getKind() + " approved by "
                    + plan.approval.getApprover(), staffName);
        }
        if (plan.giftCard != null) {
            if (!giftCards.redeem(plan.giftCard.getId(), o.getId(), plan.giftCents)) {
                throw new ResponseStatusException(409, "The gift card's balance changed");
            }
            tender(o.getId(), "GIFT_CARD", plan.giftCents, 0, plan.giftCard.getLastFour());
        }
        if (plan.creditCents > 0) {
            if (customers.debitCreditIf(plan.customer.getId(), plan.creditCents) != 1) {
                throw new ResponseStatusException(409, "The customer's store credit changed");
            }
            tender(o.getId(), "STORE_CREDIT", plan.creditCents, 0, "");
        }
        if (plan.readerPayment != null) {
            if (!readers.claim(plan.readerPayment.getId(), o.getId())) {
                throw new ResponseStatusException(409, "That card payment paid another sale");
            }
            String reference = payments.reader(o.getId(), staffName, plan.cardCents,
                    o.getCurrency(), plan.readerPayment.getIntentRef(),
                    plan.readerPayment.getCardLabel());
            tender(o.getId(), "CARD", plan.cardCents, 0, reference);
        } else if (plan.cardCents > 0) {
            String reference = payments.terminal(o.getId(), staffName, plan.cardCents,
                    o.getCurrency(), plan.cardReference);
            tender(o.getId(), "CARD", plan.cardCents, 0, reference);
        }
        if (plan.cashTendered > 0) {
            tender(o.getId(), "CASH", plan.cashCents, plan.changeCents, "");
        }
        if (plan.customer != null) {
            Customer c = customers.findById(plan.customer.getId());
            c.setOrdersCount(c.getOrdersCount() + 1);
            c.setSpentCents(c.getSpentCents() + o.getTotalCents());
            c.setUpdatedAt(Instant.now());
            customers.save(c);
        }
        if (plan.oversold) {
            AuditEvent e = new AuditEvent();
            e.setId(Ids.next());
            e.setStoreId(storeId);
            e.setUsername(staffName);
            e.setAction("POS_OVERSOLD");
            e.setSubject(o.getId());
            e.setDetail(Codes.cut("Order #" + o.getNumber() + " at " + register.getName()
                    + " sold more than on hand: " + join(plan.oversoldSkus), 500));
            e.setCreatedAt(Instant.now());
            audits.insert(e);
        }
        shifts.addTotals(shift.getId(), plan.cashCents, plan.cardCents,
                plan.giftCents + plan.creditCents, 0, plan.cashCents, 1);
        orders.event(o.getId(), "PLACED", "Sold at " + register.getName()
                + (plan.shipTo == null ? "" : ", to ship to the customer"), staffName);
        if (plan.overCents > 0) {
            orders.event(o.getId(), plan.overCents > tolerance(o.getTotalCents())
                    ? "OVER_TENDERED" : "TOTAL_ADJUSTED",
                    "The register took " + plan.overCents + " cents more than the server total of "
                    + o.getTotalCents() + " cents; the order keeps the server total", staffName);
        }
        if (plan.shortCents > 0) {
            orders.event(o.getId(), "TOTAL_ADJUSTED", "The register took " + plan.shortCents
                    + " cents less than the server total of " + o.getTotalCents()
                    + " cents; within the rounding tolerance", staffName);
        }
    }

    private void tender(String orderId, String kind, long amount, long change, String reference)
            throws IOException {
        Tender t = new Tender();
        t.setId(Ids.next());
        t.setOrderId(orderId);
        t.setKind(kind);
        t.setAmountCents(amount);
        t.setChangeCents(change);
        t.setReference(Codes.cut(reference, 64));
        t.setPaymentId("CARD".equals(kind) ? orderId : "");
        t.setCreatedAt(Instant.now());
        tenders.insert(t);
    }
}
