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
package com.codenameone.examples.bastion.money;

import java.time.Instant;
import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.chain.Chain;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.Card;
import com.codenameone.examples.bastion.domain.CryptoAddress;
import com.codenameone.examples.bastion.domain.Device;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Market;
import com.codenameone.examples.bastion.domain.Payee;
import com.codenameone.examples.bastion.domain.Trade;
import com.codenameone.examples.bastion.domain.Transfer;
import com.codenameone.examples.bastion.exchange.Markets;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.repository.CardRepository;
import com.codenameone.examples.bastion.repository.JournalEntryRepository;
import com.codenameone.examples.bastion.repository.ProfileRepository;
import com.codenameone.examples.bastion.repository.TradeRepository;
import com.codenameone.examples.bastion.repository.TransferRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/// Every movement of a customer's money: between their own accounts, to a
/// payee, out of the bank, onto a card, and (on a development server) in.
///
/// Each movement is one transaction holding the ledger entry, the [Transfer]
/// row the limits are counted from, and the audit event: all three or none.
/// What is checked before it, in order:
///
/// 1. **Ownership.** The source account is the caller's, or 404.
/// 2. **The signature** ([RequestSigning]) when the movement needs one: above
///    `bastion.stepup.threshold`, and always for money leaving the caller --
///    to a payee or out of the bank -- while `bastion.device.policy.enforce`
///    is on, since that is how the server knows which device asked.
/// 3. **The device** is trusted ([Devices]) for money leaving the caller.
/// 4. **The limits** ([#checkLimits]): per movement, per 24 hours, and per
///    new payee during its cooling-off period -- fiat valued in USD at
///    `bastion.limit.fx` and summed across currencies (the card limit in
///    the card's own units), crypto
///    valued in USD at the index price.
///
/// A retry of a movement already made skips 2 to 4's counting -- the money
/// moved, and counting it again would refuse a retry of a payment that was
/// within its limits -- and answers the first receipt.
@Component
public class Money {
    static final long DAY = 24L * 3600L * 1000L;

    private final Ledger ledger;
    private final Audit audit;
    private final TransferRepository transfers;
    private final JournalEntryRepository entries;
    private final ProfileRepository profiles;
    private final CardRepository cards;
    private final long perTransfer;
    private final long daily;
    private final long stepUp;
    private final long coolOffMillis;
    private final long coolOffLimit;
    private final boolean enforceDevices;
    private final Markets markets;
    private final long cryptoPerMove;
    private final long cryptoDaily;
    private final Map<String, Long> fxBps;
    private final TradeRepository trades;
    private final Chain chain;

    public Money(Ledger ledger, Audit audit, TransferRepository transfers,
            JournalEntryRepository entries, ProfileRepository profiles, Markets markets,
            CardRepository cards, TradeRepository trades, Chain chain,
            @Value("${bastion.crypto.limit.per-withdrawal:1000000}") String cryptoPerMove,
            @Value("${bastion.crypto.limit.daily:2000000}") String cryptoDaily,
            @Value("${bastion.limit.per-transfer:500000}") String perTransfer,
            @Value("${bastion.limit.daily:1000000}") String daily,
            @Value("${bastion.stepup.threshold:100000}") String stepUp,
            @Value("${bastion.payee.cooloff-hours:24}") String coolOffHours,
            @Value("${bastion.payee.cooloff-limit:10000}") String coolOffLimit,
            @Value("${bastion.device.policy.enforce:true}") String enforceDevices,
            @Value("${bastion.limit.fx:EUR=11000}") String fx) {
        this.ledger = ledger;
        this.trades = trades;
        this.chain = chain;
        this.audit = audit;
        this.transfers = transfers;
        this.entries = entries;
        this.profiles = profiles;
        this.cards = cards;
        // In USD cents, every fiat currency valued in USD: 5,000.00 a movement, 10,000.00
        // a day, a signature above 1,000.00, 100.00 to a payee for its first
        // day.
        this.perTransfer = Long.parseLong(perTransfer.trim());
        this.daily = Long.parseLong(daily.trim());
        this.stepUp = Long.parseLong(stepUp.trim());
        this.coolOffMillis = Long.parseLong(coolOffHours.trim()) * 3600L * 1000L;
        this.coolOffLimit = Long.parseLong(coolOffLimit.trim());
        this.enforceDevices = "true".equalsIgnoreCase(enforceDevices.trim());
        this.markets = markets;
        // In USD cents at the index price, across every coin together:
        // 10,000.00 a movement, 20,000.00 a day.
        this.cryptoPerMove = Long.parseLong(cryptoPerMove.trim());
        this.cryptoDaily = Long.parseLong(cryptoDaily.trim());
        this.fxBps = parseFx(fx);
    }

    /// `bastion.limit.fx`: `CODE=bps,...`, what one unit of a fiat currency
    /// is worth in USD, in basis points (`EUR=11000` is 1.1000 USD). The fiat
    /// limits are in USD cents, so each currency is valued at this rate first;
    /// a fiat asset not listed is valued at its index market, if it has one,
    /// and refused if not.
    static Map<String, Long> parseFx(String fx) {
        Map<String, Long> out = new HashMap<String, Long>();
        String text = fx == null ? "" : fx.trim();
        int start = 0;
        while (start < text.length()) {
            int comma = text.indexOf(',', start);
            String pair = (comma < 0 ? text.substring(start) : text.substring(start, comma)).trim();
            start = comma < 0 ? text.length() : comma + 1;
            int colon = pair.indexOf('=');
            if (colon <= 0) {
                if (pair.length() > 0) {
                    throw new IllegalArgumentException("bastion.limit.fx: " + pair);
                }
                continue;
            }
            long bps = Long.parseLong(pair.substring(colon + 1).trim());
            if (bps <= 0) {
                throw new IllegalArgumentException("bastion.limit.fx: " + pair);
            }
            out.put(pair.substring(0, colon).trim(), Long.valueOf(bps));
        }
        return out;
    }

    /// `amount` ledger units of fiat `code` in USD cents, rounded up. Every
    /// fiat asset here has two decimals, as USD does.
    long fiatValue(String code, long amount) throws IOException {
        if ("USD".equals(code) || amount == 0) {
            return amount;
        }
        Long bps = fxBps.get(code);
        if (bps == null) {
            return markets.value(code, amount, "USD");
        }
        long b = bps.longValue();
        if (amount > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        long scaled = amount * b;
        return scaled / 10000L + (scaled % 10000L > 0 ? 1L : 0L);
    }

    /// Whether [#usdValue] can value `asset`, answered without refusing.
    @Transactional(readOnly = true)
    public boolean hasUsdPrice(String asset) throws IOException {
        return "USD".equals(asset) || fxBps.containsKey(asset) || markets.hasPrice(asset, "USD");
    }

    /// `amount` ledger units of any asset in USD cents, rounded up: a fiat
    /// currency at its configured rate, a coin at its index price.
    public long usdValue(String asset, long amount) throws IOException {
        Asset a = ledger.asset(asset);
        return "FIAT".equals(a.getKind()) ? fiatValue(asset, Math.max(amount, 0L))
                : markets.value(asset, Math.max(amount, 0L), "USD");
    }

    /// What `owner` has traded since `since`, both sides, in USD cents: the
    /// quote each fill paid or received, valued like any movement.
    /// Counting the buy side too, and filled orders' holds with the open
    /// ones, is deliberately conservative: it can refuse a customer early,
    /// never let one past the daily limit. Telling "value leaving" apart
    /// per side would need a valuation of every coin, and an error there
    /// would fail open.
    private long tradedSince(String owner, long since) throws IOException {
        long sum = 0;
        for (Trade t : trades.since(owner, Instant.ofEpochMilli(since))) {
            Market market = markets.find(t.getMarket());
            if (market == null) {
                continue;
            }
            long quote = owner.equals(t.getBuyer()) ? t.getBuyerPaid() : t.getSellerGot();
            sum = saturatedAdd(sum, usdValue(market.getQuote(), quote));
        }
        return sum;
    }

    /// Takes the customer's row for an order's limit check, as a payment
    /// does: two orders at once then cannot each pass alone. Before the
    /// open orders are read for [#checkTrade].
    public void holdCustomer(String owner) throws IOException {
        profiles.hold(owner);
    }

    /// Holds a customer's order, worth at most `value` USD cents, to the
    /// limits a payment is held to: no more than one payment may move, and
    /// with `open` (what the customer's other open orders could still carry
    /// away, in USD) and the day's payments and trades together no more than
    /// the daily limit. Open orders count because a resting order is checked
    /// once, when placed, and may fill later in one go: many small ones
    /// would each pass alone. A trade is value leaving the customer as surely as a payment
    /// is -- a thief selling the account's coins into their own resting
    /// order, or buying from it -- and the price collar bounds only how bad
    /// each fill can be, not how many there are. Behind the customer's row
    /// ([#holdCustomer]), taken by the caller.
    ///
    /// Called inside the order's transaction, after the market lock: the
    /// order is then market, customer, balances, audit, and no path takes
    /// a customer's row before a market's.
    public void checkTrade(String owner, long value, long open) throws IOException {
        if (value > perTransfer) {
            throw Refused.with(409, "more-than-payment-may-move", "That is more than one payment may move");
        }
        long now = System.currentTimeMillis();
        long sent = saturatedAdd(saturatedAdd(value, open), tradedSince(owner, now - DAY));
        sent = saturatedAdd(sent, fiatSentSince(owner, now - DAY));
        if (sent > daily) {
            throw Refused.with(409, "would-pass-limit-day", "That would pass your limit for the day");
        }
    }

    /// What `owner` has sent in every fiat currency since `since`, in USD cents.
    private long fiatSentSince(String owner, long since) throws IOException {
        long sent = 0;
        for (Asset fiat : ledger.assets()) {
            if ("FIAT".equals(fiat.getKind())) {
                Long moved = transfers.sentSince(owner, fiat.getCode(), Instant.ofEpochMilli(since));
                if (moved != null && moved.longValue() > 0) {
                    sent = saturatedAdd(sent, fiatValue(fiat.getCode(), moved.longValue()));
                }
            }
        }
        return sent;
    }

    public long coolOffMillis() {
        return coolOffMillis;
    }

    public boolean enforcesDevices() {
        return enforceDevices;
    }

    /// Whether a movement of `amount` out of `from` that does or does not
    /// leave the caller must be signed. The threshold is in fiat units; a
    /// coin amount is valued in USD at the index price first, or 0.001 BTC
    /// and 1,000 BTC would be the same number of satoshi short of it.
    public boolean needsSignature(LedgerAccount from, long amount, boolean leaves)
            throws IOException {
        if (leaves && enforceDevices) {
            return true;
        }
        Asset asset = ledger.asset(from.getAsset());
        long value = "FIAT".equals(asset.getKind()) ? fiatValue(asset.getCode(), Math.max(amount, 0L))
                : markets.value(asset.getCode(), Math.max(amount, 0L), "USD");
        return value > stepUp;
    }

    /// Refuses a device that may not send money away, when the policy says so.
    public void requireTrusted(Device device) {
        if (enforceDevices && !Devices.trusted(device)) {
            throw Refused.with(403, "device-not-attested", "This device is not attested, or is rooted: it can move money only between your own accounts");
        }
    }

    /// Moves `amount` between two of the caller's accounts.
    public ReceiptDto internal(String owner, String key, LedgerAccount from, LedgerAccount to,
            long amount, String memo) throws IOException {
        if (from.getId().equals(to.getId())) {
            throw Refused.with(400, "choose-two-different-accounts", "Choose two different accounts");
        }
        // A move is one asset in and the same out; changing one into another
        // is a trade, at a price, on the exchange.
        if (!from.getAsset().equals(to.getAsset())) {
            throw Refused.with(400, "both-accounts-hold-same-currency", "Both accounts must hold the same currency. Use the exchange to convert.");
        }
        return move(new Move(owner, key, "TRANSFER", from, to, "", amount, false, memo, null));
    }

    /// Pays `payee`'s customer into their checking account in the same asset.
    public ReceiptDto pay(String owner, String key, LedgerAccount from, Payee payee,
            LedgerAccount to, long amount, String memo) throws IOException {
        return move(new Move(owner, key, "PAYMENT", from, to, payee.getId(), amount, true, memo,
                payee));
    }

    /// Money out of the bank: into SYS_CASH, standing for the wire or the
    /// blockchain transaction that would carry it on.
    public ReceiptDto withdraw(String owner, String key, LedgerAccount from, long amount,
            String memo) throws IOException {
        LedgerAccount cash = ledger.system(from.getAsset(), AccountKinds.SYS_CASH);
        return move(new Move(owner, key, "WITHDRAWAL", from, cash, "", amount, true, memo, null));
    }

    /// Coins out of the bank to an allow-listed address. The caller has
    /// checked the address is the owner's, for this asset, and past its
    /// cooling-off.
    ///
    /// On a real chain ([Chain#handles]) the coins go to SYS_CHAIN_TRANSIT
    /// and the transaction is queued in the same database transaction, so
    /// there is never a debit without a withdrawal to settle it nor the
    /// reverse; [Chain] signs, broadcasts and settles it. Otherwise they go
    /// straight to SYS_CHAIN, standing for the transaction a real exchange
    /// would broadcast.
    public ReceiptDto withdrawCrypto(String owner, String key, LedgerAccount from,
            CryptoAddress address, long amount) throws IOException {
        boolean real = chain.handles(from.getAsset());
        LedgerAccount to = ledger.system(from.getAsset(), real ? AccountKinds.SYS_CHAIN_TRANSIT
                : AccountKinds.SYS_CHAIN);
        Move m = new Move(owner, key, "CRYPTO_OUT", from, to, address.getId(), amount,
                true, "to " + address.getAddress(), null);
        if (real) {
            m.chainTo = address;
        }
        return move(m);
    }

    /// Simulated money in, from SYS_CASH. Not counted against any limit.
    public ReceiptDto deposit(String owner, String key, LedgerAccount to, long amount,
            String memo) throws IOException {
        LedgerAccount cash = ledger.system(to.getAsset(), AccountKinds.SYS_CASH);
        return move(new Move(owner, key, "DEPOSIT", cash, to, "", amount, false, memo, null));
    }

    /// A card authorization: refused when the card is frozen or over its own
    /// daily limit, then a movement like any other out of the account.
    public ReceiptDto spend(String owner, String key, Card card, LedgerAccount from, long amount,
            String merchant) throws IOException {
        LedgerAccount cash = ledger.system(from.getAsset(), AccountKinds.SYS_CASH);
        Move move = new Move(owner, key, "CARD", from, cash, card.getId(), amount, true,
                merchant, null);
        move.card = card;
        return move(move);
    }

    /// One movement. See the class comment; this is the half that retries.
    private ReceiptDto move(Move m) throws IOException {
        if (m.amount <= 0) {
            throw Refused.with(400, "amount-more-than-zero", "The amount must be more than zero");
        }
        m.key = Ledger.clientKey(m.owner, m.key);
        try {
            return moveOnce(m);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException | RuntimeException failed) {
            // Two copies of one request, sent together: both found no entry
            // with the key, and the second failed on its unique index when the
            // first committed. Its transaction rolled back whole. Asked again,
            // it finds the first's entry and answers that receipt.
            if (m.key != null && found(m.key)) {
                return moveOnce(m);
            }
            throw failed;
        }
    }

    @Transactional(readOnly = true)
    private boolean found(String key) throws IOException {
        return entries.findFirstByIdempotencyKey(key) != null;
    }

    @Transactional(rollbackFor = Exception.class)
    private ReceiptDto moveOnce(Move m) throws IOException {
        java.util.List<Leg> legs = Arrays.asList(new Leg(m.from.getId(), -m.amount),
                new Leg(m.to.getId(), m.amount));
        String memo = m.memo == null ? "" : m.memo;
        JournalEntry earlier = m.key == null ? null : entries.findFirstByIdempotencyKey(m.key);
        if (earlier != null) {
            // The ledger compares the request with the first one and refuses a
            // different payment under the same key; then the first receipt.
            JournalEntry same = ledger.post(m.key, m.kind, m.owner, memo, legs);
            Transfer first = transfers.findById(same.getId());
            if (first == null || !first.getOwner().equals(m.owner)) {
                throw Refused.with(422, "idempotency-key-reused", "That Idempotency-Key was already used for a different request");
            }
            return receipt(first);
        }
        if (m.outgoing) {
            // Behind the customer's own row: two payments at once would
            // otherwise each count the day's total without the other and
            // together pass a limit neither may.
            profiles.hold(m.owner);
            if (m.card != null) {
                // The card as it is now, behind its row: the copy the endpoint
                // checked was read before this transaction, and a freeze or a
                // lower limit committed in between must not be spent past.
                cards.hold(m.card.getId());
                Card now = cards.findById(m.card.getId());
                if (now == null || now.isFrozen()) {
                    throw Refused.with(409, "card-frozen", "That card is frozen");
                }
                m.card = now;
            }
            checkLimits(m);
        }
        JournalEntry entry = ledger.post(m.key, m.kind, m.owner, memo, legs);
        Transfer t = new Transfer();
        t.setId(entry.getId());
        t.setOwner(m.owner);
        t.setKind(m.kind);
        t.setFromAccount(m.from.getId());
        t.setToAccount(m.to.getId());
        t.setVia(m.via);
        t.setAsset(m.from.getAsset());
        t.setAmount(m.amount);
        t.setOutgoing(m.outgoing);
        t.setMemo(entry.getMemo());
        t.setCreatedAt(entry.getCreatedAt());
        transfers.insert(t);
        if (m.chainTo != null) {
            chain.queue(entry.getId(), m.owner, m.chainTo, m.amount);
        }
        audit.append(m.owner, m.kind, entry.getId(), m.amount + " " + t.getAsset() + " "
                + t.getFromAccount() + " -> " + t.getToAccount()
                + (m.via.length() > 0 ? " via " + m.via : ""));
        return receipt(t);
    }

    /// The limits. A fiat movement is counted in its own asset's units. A
    /// coin is not a cent, so a crypto movement is valued in USD at the index
    /// price ([Markets#value], rounded up) and counted against limits shared
    /// by every coin: someone draining an account takes the BTC and the ETH
    /// both, and one limit per coin would let them take twice. The index
    /// price is the bank's own feed, not the last trade, which two colluding
    /// customers could print low to slip a withdrawal under the limit.
    private void checkLimits(Move m) throws IOException {
        Asset asset = ledger.asset(m.from.getAsset());
        if (!"FIAT".equals(asset.getKind())) {
            checkCryptoLimits(m, asset);
            return;
        }
        long now = System.currentTimeMillis();
        // In USD cents across every fiat currency together, like the crypto
        // limits: one limit per currency would let a customer with a USD and
        // a EUR account move the day's limit twice.
        long value = fiatValue(asset.getCode(), m.amount);
        if (value > perTransfer) {
            throw Refused.with(409, "more-than-payment-may-move", "That is more than one payment may move");
        }
        // The day's trades count too: one daily limit for everything that
        // can carry value away, or a thief would get it once per channel.
        long sent = saturatedAdd(value, fiatSentSince(m.owner, now - DAY));
        sent = saturatedAdd(sent, tradedSince(m.owner, now - DAY));
        if (sent > daily) {
            throw Refused.with(409, "would-pass-limit-day", "That would pass your limit for the day");
        }
        if (m.payee != null && now < m.payee.getCreatedAt().toEpochMilli() + coolOffMillis) {
            // In USD cents, each currency valued by itself: a raw sum would
            // add euro cents to dollar cents, and the limit would mean a
            // different amount in every currency.
            long paid = value;
            for (Asset fiat : ledger.assets()) {
                if ("FIAT".equals(fiat.getKind())) {
                    Long through = transfers.throughSinceIn(m.payee.getId(), fiat.getCode(),
                            m.payee.getCreatedAt());
                    if (through != null && through.longValue() > 0) {
                        paid = saturatedAdd(paid, fiatValue(fiat.getCode(), through.longValue()));
                    }
                }
            }
            if (paid > coolOffLimit) {
                throw Refused.with(409, "new-payee-receive-until-their", "A new payee can receive only {0} until their cooling-off period ends", coolOffLimit);
            }
        }
        if (m.card != null) {
            Long spent = transfers.throughSince(m.card.getId(), Instant.ofEpochMilli(now - DAY));
            if ((spent == null ? 0L : spent.longValue()) + m.amount > m.card.getDailyLimit()) {
                throw Refused.with(409, "over-card-daily-limit", "That is over the card's daily limit");
            }
        }
    }

    private void checkCryptoLimits(Move m, Asset asset) throws IOException {
        long now = System.currentTimeMillis();
        long value = markets.value(asset.getCode(), m.amount, "USD");
        if (value > cryptoPerMove) {
            throw Refused.with(409, "more-than-crypto-movement-may", "That is more than one crypto movement may move");
        }
        long sent = value;
        for (Asset coin : ledger.assets()) {
            if (!"FIAT".equals(coin.getKind())) {
                Long moved = transfers.sentSince(m.owner, coin.getCode(), Instant.ofEpochMilli(now - DAY));
                if (moved != null && moved.longValue() > 0) {
                    sent = saturatedAdd(sent, markets.value(coin.getCode(), moved.longValue(),
                            "USD"));
                }
            }
        }
        if (sent > cryptoDaily) {
            throw Refused.with(409, "would-pass-crypto-limit-day", "That would pass your crypto limit for the day");
        }
        // No payee branch: a payee is paid into a fiat checking account, so
        // coins leave a customer only by withdrawal, to an allow-listed address
        // whose cooling-off is its own.
    }

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }

    static ReceiptDto receipt(Transfer t) {
        return new ReceiptDto(t.getId(), t.getKind(), t.getFromAccount(), t.getToAccount(),
                t.getAsset(), t.getAmount(), t.getMemo(), t.getCreatedAt().toEpochMilli());
    }

    /// What one call asked to move.
    private static final class Move {
        final String owner;
        String key;
        final String kind;
        final LedgerAccount from;
        final LedgerAccount to;
        final String via;
        final long amount;
        final boolean outgoing;
        final String memo;
        final Payee payee;
        Card card;
        /// Set for a withdrawal [Chain] carries to a real network.
        CryptoAddress chainTo;

        Move(String owner, String key, String kind, LedgerAccount from, LedgerAccount to,
                String via, long amount, boolean outgoing, String memo, Payee payee) {
            this.owner = owner;
            this.key = key;
            this.kind = kind;
            this.from = from;
            this.to = to;
            this.via = via;
            this.amount = amount;
            this.outgoing = outgoing;
            this.memo = memo;
            this.payee = payee;
        }
    }
}
