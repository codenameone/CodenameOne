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
package com.codenameone.examples.bastion.exchange;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.NewOrderDto;
import com.codenameone.examples.bastion.api.OrderDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Market;
import com.codenameone.examples.bastion.domain.Trade;
import com.codenameone.examples.bastion.domain.TradeOrder;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.money.Money;
import com.codenameone.examples.bastion.repository.MarketRepository;
import com.codenameone.examples.bastion.repository.TradeOrderRepository;
import com.codenameone.examples.bastion.repository.TradeRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The matching engine: orders in, trades out, every one of them a balanced
/// entry in the same [Ledger] the bank's accounts live in.
///
/// **Holds.** An order commits its funds before it can match. A buy moves the
/// most it can cost -- `ceil(limit * quantity / 10^decimals)` plus the most
/// fee that could be charged on it -- from the owner's CHECKING account in the
/// quote asset into their HOLD account; a sell moves the coins from the WALLET
/// into HOLD. A trade then pays out of the holds, so a customer can never
/// trade with money a payment spent a moment ago, and two orders cannot both
/// spend the same dollar: the ledger's conditional update decides.
///
/// **Matching** runs inside the placing transaction, behind the market's row
/// ([MarketRepository#hold]), so one market matches one order at a time and
/// the book a match reads is the book it changes. Price-time priority: the
/// best price first, and at one price the order with the lower `seq`, a
/// counter on the market row. A trade happens at the *resting* order's price,
/// so an aggressive order gets any better price the book offers and the
/// unspent part of its hold is released when it finishes.
///
/// **Each trade is one journal entry across both assets**:
///
/// ```
/// buyer  HOLD(quote)    -paid (+fee, when the buyer is the taker)
/// seller CHECKING(quote) +got (-fee, when the seller is the taker)
/// SYS_FEES(quote)        +fee + (paid - got)
/// seller HOLD(base)      -quantity
/// buyer  WALLET(base)    +quantity
/// ```
///
/// **Rounding, in the bank's favour and once per order.** `price * quantity`
/// is rarely a whole number of cents. Each order keeps the exact running sum
/// of `price * quantity` over its fills (`notional`), and the quote it has
/// paid or received so far is that sum divided once: rounded *up* for the
/// buyer, *down* for the seller. A fill moves the difference between the
/// rounded totals before and after it, so an order filled in a hundred pieces
/// pays exactly what it would have paid in one, never a hundred roundings.
/// Whatever the two sides' roundings leave between them goes to SYS_FEES; it
/// is at most a cent per order. The taker fee is rounded up the same way, on
/// the taker's running total.
///
/// **Self-trade prevention.** An order never trades with its own owner: the
/// older, resting order is cancelled and its hold released. Without it a
/// customer could print any price they like on an empty market for free, and
/// wash-trade volume.
///
/// **Market orders** never rest. A market buy names the quote it may spend,
/// fee included; it holds exactly that, buys whole lots while the next lot
/// is affordable, and releases the rest -- so it never leaves a hold behind.
///
/// Every order, cancel and trade is audited, and the audit events are written
/// last, after every balance: the transaction then takes its locks in the
/// order a payment does (balances, then audit_head) and cannot deadlock one.
@Component
public class Exchange {
    public static final String BUY = "BUY";
    public static final String SELL = "SELL";
    public static final String LIMIT = "LIMIT";
    public static final String MARKET = "MARKET";
    public static final String OPEN = "OPEN";
    public static final String FILLED = "FILLED";
    public static final String CANCELLED = "CANCELLED";

    /// A bound on lots in one order, so `price * quantity` stays far inside a
    /// `long` for any price a market here will see.
    static final long MAX_LOTS = 1000000000L;

    private final Ledger ledger;
    private final Audit audit;
    private final Markets markets;
    private final MarketRepository marketRows;
    private final TradeOrderRepository orders;
    private final TradeRepository trades;
    private final long feeBps;
    private final long collarBps;
    private final Money money;

    public Exchange(Ledger ledger, Audit audit, Markets markets, MarketRepository marketRows,
            TradeOrderRepository orders, TradeRepository trades, Money money,
            @Value("${bastion.exchange.taker-fee-bps:20}") String feeBps,
            @Value("${bastion.exchange.collar-bps:1000}") String collarBps) {
        this.money = money;
        // How far from the index price an order may trade, in basis points:
        // 1000 is 10%. A limit price outside it is refused, and a market
        // order stops walking the book at its edge -- so a thief with a
        // signed-in session who sells into their own resting order at a
        // silly price, or a fat finger, loses at most this much a fill.
        this.collarBps = Long.parseLong(collarBps.trim());
        if (this.collarBps <= 0 || this.collarBps >= 10000) {
            throw new IllegalArgumentException("bastion.exchange.collar-bps must be 1 to 9999");
        }
        this.ledger = ledger;
        this.audit = audit;
        this.markets = markets;
        this.marketRows = marketRows;
        this.orders = orders;
        this.trades = trades;
        // Basis points of the quote a taker pays: 20 is 0.2%. Makers pay none,
        // which is what pays a market maker to keep a book.
        this.feeBps = Long.parseLong(feeBps.trim());
        if (this.feeBps < 0 || this.feeBps >= 10000) {
            throw new IllegalArgumentException("bastion.exchange.taker-fee-bps must be 0 to 9999");
        }
    }

    /// Places `request` for `owner`, outside the customer limits: the market
    /// maker's own quotes, and the tests of the engine itself.
    public OrderDto place(String owner, String key, NewOrderDto request) throws IOException {
        return place(owner, key, request, false);
    }

    /// Places `request` for `owner`. A retry with the same key answers the
    /// order its first attempt placed. With `limited`, a customer's order:
    /// its worst-case worth in USD is held to the per-payment and daily
    /// limits ([Money#checkTrade]) along with the day's payments and fills.
    public OrderDto place(String owner, String key, NewOrderDto request, boolean limited)
            throws IOException {
        if (key == null || key.length() < 16 || key.length() > 80) {
            throw Refused.with(400, "send-idempotency-key-16-80", "Send an Idempotency-Key of 16 to 80 characters");
        }
        if (request == null) {
            throw Refused.with(400, "send-order", "Send the order");
        }
        try {
            return placeOnce(owner, key, request, limited);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException | RuntimeException failed) {
            // Two copies of one order at once: the second lost on the
            // (owner, client_key) index and rolled back whole. Asked again it
            // finds the first.
            if (existing(owner, key) != null) {
                return placeOnce(owner, key, request, limited);
            }
            throw failed;
        }
    }

    @Transactional(readOnly = true)
    private TradeOrder existing(String owner, String key) throws IOException {
        return orders.findFirstByOwnerAndClientKey(owner, key);
    }

    @Transactional(rollbackFor = Exception.class)
    private OrderDto placeOnce(String owner, String key, NewOrderDto r, boolean limited)
            throws IOException {
        TradeOrder earlier = orders.findFirstByOwnerAndClientKey(owner, key);
        if (earlier != null) {
            if (!earlier.getMarket().equals(r.market()) || !earlier.getSide().equals(r.side())
                    || !earlier.getType().equals(r.type()) || earlier.getPrice() != r.price()
                    || earlier.getQuantity() != r.quantity() || earlier.getBudget() != r.budget()) {
                throw Refused.with(422, "idempotency-key-reused-order", "That Idempotency-Key was already used for a different order");
            }
            return dto(earlier);
        }
        Market m = lock(r.market());
        boolean buy = BUY.equals(r.side());
        if (!buy && !SELL.equals(r.side())) {
            throw Refused.with(400, "order-buy-sell", "An order is a BUY or a SELL");
        }
        boolean limit = LIMIT.equals(r.type());
        if (!limit && !MARKET.equals(r.type())) {
            throw Refused.with(400, "order-limit-market", "An order is LIMIT or MARKET");
        }
        Asset base = ledger.asset(m.getBase());
        long scale = Markets.scale(base);
        long held;
        if (limit || !buy) {
            if (r.quantity() <= 0 || r.quantity() % m.getLot() != 0
                    || r.quantity() / m.getLot() > MAX_LOTS) {
                throw Refused.with(400, "quantity-whole-number-unit-lots", "The quantity must be a whole number of {0}-unit lots", m.getLot());
            }
            if (limit && r.price() <= 0) {
                throw Refused.with(400, "limit-order-needs-price", "A limit order needs a price");
            }
            if (limit && (r.price() < low(m) || r.price() > high(m))) {
                throw Refused.with(400, "price-outside-collar",
                        "The price must be within {0}% of the index price", collarBps / 100);
            }
            if (!limit && (r.price() != 0 || r.budget() != 0)) {
                throw Refused.with(400, "market-sell-names-quantity", "A market sell names only a quantity");
            }
            if (limit && r.budget() != 0) {
                throw Refused.with(400, "limit-order-names-no-budget", "A limit order names no budget");
            }
            if (buy) {
                long most = Markets.ceilDiv(exact(r.price(), r.quantity()), scale);
                held = Markets.addExact(most, fee(most));
            } else {
                held = r.quantity();
            }
        } else {
            if (r.budget() <= 0 || r.price() != 0 || r.quantity() != 0) {
                throw Refused.with(400, "market-buy-names-budget", "A market buy names only a budget");
            }
            held = r.budget();
        }
        if (limited) {
            // The most the order can carry away, in USD: what a buy holds
            // (its quote, fee included), or a sell's coins at the index.
            money.holdCustomer(owner);
            money.checkTrade(owner, buy ? money.usdValue(m.getQuote(), held)
                    : money.usdValue(m.getBase(), held), openValue(owner));
        }

        long now = System.currentTimeMillis();
        marketRows.advance(m.getId());
        TradeOrder o = new TradeOrder();
        o.setId(Ids.next());
        o.setOwner(owner);
        o.setMarket(m.getId());
        o.setSide(r.side());
        o.setType(r.type());
        o.setPrice(r.price());
        o.setQuantity(r.quantity());
        o.setBudget(r.budget());
        o.setHeld(held);
        o.setStatus(OPEN);
        // The value read behind the lock, plus the one just added: a re-read
        // would answer the session's cached row, not the update.
        o.setSeq(m.getSeq() + 1);
        o.setClientKey(key);
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        // Inserted before anything moves: a concurrent copy of the request
        // fails on the key index here.
        orders.insert(o);

        String asset = buy ? m.getQuote() : m.getBase();
        LedgerAccount source = funding(owner, asset, buy);
        LedgerAccount hold = ensure(owner, asset, AccountKinds.HOLD);
        // Insufficient funds is a 409 from the ledger, and rolls the order
        // back with it: an order that cannot be paid for was never placed.
        ledger.post("hold:" + o.getId(), "ORDER_HOLD", owner, o.getMarket() + " " + o.getSide(),
                legs(source.getId(), -held, hold.getId(), held));

        List<String[]> events = new ArrayList<String[]>();
        events.add(new String[] {owner, "ORDER_PLACED", o.getId(), m.getId() + " " + o.getType()
                + " " + o.getSide() + " " + o.getQuantity() + " @ " + o.getPrice()
                + (o.getBudget() > 0 ? " budget " + o.getBudget() : "") + " held " + held});
        match(m, base, o, events);
        if (!limit || o.getFilled() == o.getQuantity()) {
            finish(m, o, events);
        }
        o.setUpdatedAt(System.currentTimeMillis());
        orders.save(o);
        record(events);
        return dto(o);
    }

    /// Cancels `owner`'s open order `id` and releases what it still holds.
    @Transactional(rollbackFor = Exception.class)
    public OrderDto cancel(String owner, String id) throws IOException {
        String market = id == null ? null : orders.marketOf(id, owner);
        if (market == null) {
            throw Refused.with(404, "no-such-order", "No such order");
        }
        Market m = lock(market);
        // The first read of the row, and behind the lock: it may have filled
        // while we waited. Loading it before the lock would keep that stale
        // copy in the session (PostgreSQL and MySQL let a fill commit in
        // between; SQLite serializes the writers and hid it).
        TradeOrder o = orders.findById(id);
        if (!OPEN.equals(o.getStatus())) {
            throw Refused.with(409, "order-already-status", "That order is already {0}", o.getStatus());
        }
        List<String[]> events = new ArrayList<String[]>();
        finish(m, o, events);
        o.setUpdatedAt(System.currentTimeMillis());
        orders.save(o);
        record(events);
        return dto(o);
    }

    /// Order `id`, whoever's it is; null when there is none. For the admin
    /// side and the tests, never answered to a customer as it is.
    @Transactional(readOnly = true)
    public TradeOrder order(String id) throws IOException {
        return id == null ? null : orders.findById(id);
    }

    /// `owner`'s open orders, newest first.
    @Transactional(readOnly = true)
    public List<TradeOrder> open(String owner) throws IOException {
        return orders.findByOwnerAndStatusOrderByCreatedAtDesc(owner, OPEN);
    }

    /// What `owner`'s open orders on `side` hold in all: what their HOLD
    /// account in that side's asset must contain.
    @Transactional(readOnly = true)
    public long held(String owner, String side) throws IOException {
        Long sum = orders.held(owner, OPEN, side);
        return sum == null ? 0L : sum.longValue();
    }

    /// Walks the other side of the book while `taker` still crosses it.
    private void match(Market m, Asset base, TradeOrder taker, List<String[]> events)
            throws IOException {
        boolean buy = BUY.equals(taker.getSide());
        boolean limit = LIMIT.equals(taker.getType());
        // A market order walks only as far as the collar: its worst case is
        // the edge, as a limit order's is its price.
        List<TradeOrder> book = buy
                ? orders.asks(m.getId(), SELL, OPEN, limit ? taker.getPrice() : high(m))
                : orders.bids(m.getId(), BUY, OPEN, limit ? taker.getPrice() : low(m));
        long scale = Markets.scale(base);
        for (TradeOrder resting : book) {
            if (taker.getQuantity() > 0 && taker.getFilled() == taker.getQuantity()) {
                break;
            }
            if (resting.getOwner().equals(taker.getOwner())) {
                events.add(new String[] {resting.getOwner(), "SELF_TRADE_PREVENTED",
                        resting.getId(), "cancelled by " + taker.getId()});
                finish(m, resting, events);
                resting.setUpdatedAt(System.currentTimeMillis());
                orders.save(resting);
                continue;
            }
            long available = resting.getQuantity() - resting.getFilled();
            long quantity;
            if (taker.getQuantity() > 0) {
                quantity = Math.min(available, taker.getQuantity() - taker.getFilled());
            } else {
                quantity = affordable(taker, resting.getPrice(), available, m.getLot(), scale);
                if (quantity == 0) {
                    // Not one more lot at this price, so none at a worse one.
                    break;
                }
            }
            TradeOrder buyOrder = buy ? taker : resting;
            TradeOrder sellOrder = buy ? resting : taker;
            trade(m, scale, buyOrder, sellOrder, resting.getPrice(), quantity, taker, events);
            if (resting.getFilled() == resting.getQuantity()) {
                finish(m, resting, events);
            }
            resting.setUpdatedAt(System.currentTimeMillis());
            orders.save(resting);
        }
    }

    /// The most whole lots, up to `available`, a market buy can still pay
    /// for at `price`, fee included. A binary search: cost only grows with
    /// quantity.
    private long affordable(TradeOrder o, long price, long available, long lot, long scale) {
        long low = 0;
        long high = available / lot;
        while (low < high) {
            long mid = low + (high - low + 1) / 2;
            if (cost(o, price, mid * lot, scale) <= o.getBudget()) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low * lot;
    }

    /// What a taker buy has paid in all, fee included, once `quantity` more
    /// at `price` is added; [Long#MAX_VALUE] when that does not fit a long.
    private long cost(TradeOrder o, long price, long quantity, long scale) {
        try {
            long quote = Markets.ceilDiv(Markets.addExact(o.getNotional(),
                    Markets.multiplyExact(price, quantity)), scale);
            return Markets.addExact(quote, fee(quote));
        } catch (ArithmeticException tooLarge) {
            return Long.MAX_VALUE;
        }
    }

    /// One fill: one journal entry. See the class comment for the legs and
    /// the rounding.
    private void trade(Market m, long scale, TradeOrder buy, TradeOrder sell, long price,
            long quantity, TradeOrder taker, List<String[]> events) throws IOException {
        long product = exact(price, quantity);
        long buyNotional = Markets.addExact(buy.getNotional(), product);
        long sellNotional = Markets.addExact(sell.getNotional(), product);
        long paidTotal = Markets.ceilDiv(buyNotional, scale);
        long gotTotal = sellNotional / scale;
        long paid = paidTotal - buy.getSettled();
        long got = gotTotal - sell.getSettled();
        // The taker's fee on its running total. A taker fills only while it
        // is being placed, before it rests, so its running total is all taker
        // volume; once resting it is a maker and pays nothing more.
        boolean buyerTakes = taker == buy;
        long feeNow = fee(buyerTakes ? paidTotal : gotTotal) - taker.getFee();
        long buyerOut = paid + (buyerTakes ? feeNow : 0L);
        long sellerIn = got - (buyerTakes ? 0L : feeNow);
        long toFees = buyerOut - sellerIn;

        LedgerAccount buyerHold = ensure(buy.getOwner(), m.getQuote(), AccountKinds.HOLD);
        LedgerAccount sellerHold = ensure(sell.getOwner(), m.getBase(), AccountKinds.HOLD);
        LedgerAccount sellerCash = ensure(sell.getOwner(), m.getQuote(), AccountKinds.CHECKING);
        LedgerAccount buyerWallet = ensure(buy.getOwner(), m.getBase(), AccountKinds.WALLET);
        LedgerAccount fees = ledger.system(m.getQuote(), AccountKinds.SYS_FEES);
        Map<String, Long> sums = new LinkedHashMap<String, Long>();
        add(sums, buyerHold.getId(), -buyerOut);
        add(sums, sellerCash.getId(), sellerIn);
        add(sums, fees.getId(), toFees);
        add(sums, sellerHold.getId(), -quantity);
        add(sums, buyerWallet.getId(), quantity);
        List<Leg> legs = new ArrayList<Leg>();
        for (Map.Entry<String, Long> e : sums.entrySet()) {
            // A leg of zero is refused by the ledger, and a cent of rounding
            // can make the fee leg, or both quote legs, exactly that.
            if (e.getValue().longValue() != 0L) {
                legs.add(new Leg(e.getKey(), e.getValue().longValue()));
            }
        }
        String id = Ids.next();
        String entry = ledger.post("trade:" + id, "TRADE", taker.getOwner(),
                m.getId() + " " + quantity + " @ " + price, legs).getId();

        buy.setFilled(buy.getFilled() + quantity);
        buy.setNotional(buyNotional);
        buy.setSettled(paidTotal);
        buy.setHeld(buy.getHeld() - buyerOut);
        sell.setFilled(sell.getFilled() + quantity);
        sell.setNotional(sellNotional);
        sell.setSettled(gotTotal);
        sell.setHeld(sell.getHeld() - quantity);
        taker.setFee(taker.getFee() + feeNow);
        if (buy.getHeld() < 0 || sell.getHeld() < 0) {
            // The hold is computed to cover the worst case; this would be a
            // bug in that computation, and the ledger would already have
            // refused the entry had the HOLD account itself run dry.
            throw new IllegalStateException("An order spent more than it held");
        }

        Trade t = new Trade();
        t.setId(id);
        t.setMarket(m.getId());
        t.setBuyOrder(buy.getId());
        t.setSellOrder(sell.getId());
        t.setBuyer(buy.getOwner());
        t.setSeller(sell.getOwner());
        t.setTakerSide(taker.getSide());
        t.setPrice(price);
        t.setQuantity(quantity);
        t.setBuyerPaid(buyerOut);
        t.setSellerGot(sellerIn);
        t.setFee(feeNow);
        t.setEntryId(entry);
        t.setCreatedAt(System.currentTimeMillis());
        trades.insert(t);
        marketRows.traded(m.getId(), price);
        String detail = m.getId() + " " + quantity + " @ " + price + " buy " + buy.getId()
                + " sell " + sell.getId() + " fee " + feeNow;
        events.add(new String[] {buy.getOwner(), "TRADE", id, detail});
        events.add(new String[] {sell.getOwner(), "TRADE", id, detail});
    }

    /// Ends an order -- filled, cancelled, or a market order done -- and
    /// moves whatever it still holds back where it came from.
    private void finish(Market m, TradeOrder o, List<String[]> events) throws IOException {
        boolean buy = BUY.equals(o.getSide());
        boolean complete = o.getQuantity() > 0 ? o.getFilled() == o.getQuantity()
                : o.getFilled() > 0;
        boolean cancelled = OPEN.equals(o.getStatus()) && LIMIT.equals(o.getType()) && !complete;
        if (o.getHeld() > 0) {
            String asset = buy ? m.getQuote() : m.getBase();
            LedgerAccount hold = ensure(o.getOwner(), asset, AccountKinds.HOLD);
            LedgerAccount back = funding(o.getOwner(), asset, buy);
            ledger.post("release:" + o.getId(), "ORDER_RELEASE", o.getOwner(), o.getMarket(),
                    legs(hold.getId(), -o.getHeld(), back.getId(), o.getHeld()));
            o.setHeld(0);
        }
        o.setStatus(complete ? FILLED : CANCELLED);
        if (cancelled || (!complete && MARKET.equals(o.getType()))) {
            events.add(new String[] {o.getOwner(), "ORDER_CANCELLED", o.getId(),
                    o.getMarket() + " filled " + o.getFilled() + " of " + o.getQuantity()});
        }
    }

    /// What `owner`'s open orders hold, in USD: each one's hold valued in its
    /// own asset (a buy's quote, a sell's coins). The whole hold, even of a
    /// partly filled order, so it can only over-count.
    private long openValue(String owner) throws IOException {
        long sum = 0;
        for (TradeOrder o : orders.findByOwnerAndStatusOrderByCreatedAtDesc(owner, OPEN)) {
            Market om = markets.get(o.getMarket());
            long v = money.usdValue(BUY.equals(o.getSide()) ? om.getQuote() : om.getBase(),
                    o.getHeld());
            sum = sum + v < sum ? Long.MAX_VALUE : sum + v;
        }
        return sum;
    }

    /// The lowest price the collar lets `m` trade at.
    private long low(Market m) {
        return Math.max(1L, m.getIndexPrice() - band(m));
    }

    /// The highest price the collar lets `m` trade at.
    private long high(Market m) {
        return Markets.addExact(m.getIndexPrice(), band(m));
    }

    private long band(Market m) {
        return Markets.multiplyExact(m.getIndexPrice(), collarBps) / 10000L;
    }

    private Market lock(String id) throws IOException {
        if (id == null || marketRows.hold(id) != 1) {
            throw Refused.with(404, "no-such-market", "No such market");
        }
        return markets.get(id);
    }

    /// Where an order's funds come from and return to: the owner's first
    /// CHECKING account in a fiat asset, their first WALLET in a coin.
    private LedgerAccount funding(String owner, String asset, boolean fiatSide)
            throws IOException {
        String kind = fiatSide ? AccountKinds.CHECKING : AccountKinds.WALLET;
        LedgerAccount found = first(owner, asset, kind);
        if (found == null) {
            throw fiatSide
                    ? Refused.with(409, "open-checking-first", "Open a {0} checking account first", asset)
                    : Refused.with(409, "open-wallet-first", "Open a {0} wallet first", asset);
        }
        return found;
    }

    private LedgerAccount ensure(String owner, String asset, String kind) throws IOException {
        LedgerAccount found = first(owner, asset, kind);
        return found != null ? found : ledger.open(owner, asset, kind, kind);
    }

    private LedgerAccount first(String owner, String asset, String kind) throws IOException {
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            if (a.getAsset().equals(asset) && a.getKind().equals(kind)) {
                return a;
            }
        }
        return null;
    }

    private void record(List<String[]> events) throws IOException {
        for (String[] e : events) {
            audit.append(e[0], e[1], e[2], e[3]);
        }
    }

    /// The taker fee on `quote`, rounded up.
    long fee(long quote) {
        return Markets.ceilDiv(Markets.multiplyExact(quote, feeBps), 10000L);
    }

    private static long exact(long a, long b) {
        try {
            return Markets.multiplyExact(a, b);
        } catch (ArithmeticException tooLarge) {
            throw Refused.with(400, "order-too-large", "That order is too large");
        }
    }

    private static void add(Map<String, Long> sums, String account, long amount) {
        Long was = sums.get(account);
        sums.put(account, Long.valueOf(Markets.addExact(was == null ? 0L : was.longValue(),
                amount)));
    }

    private static List<Leg> legs(String a, long amountA, String b, long amountB) {
        List<Leg> legs = new ArrayList<Leg>();
        legs.add(new Leg(a, amountA));
        legs.add(new Leg(b, amountB));
        return legs;
    }

    public static OrderDto dto(TradeOrder o) {
        return new OrderDto(o.getId(), o.getMarket(), o.getSide(), o.getType(), o.getPrice(),
                o.getQuantity(), o.getBudget(), o.getFilled(), o.getSettled(), o.getFee(),
                o.getStatus(), o.getCreatedAt());
    }
}
