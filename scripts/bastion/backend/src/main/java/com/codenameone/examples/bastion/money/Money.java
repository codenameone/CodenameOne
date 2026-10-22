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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.Card;
import com.codenameone.examples.bastion.domain.CryptoAddress;
import com.codenameone.examples.bastion.domain.Device;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Payee;
import com.codenameone.examples.bastion.domain.Transfer;
import com.codenameone.examples.bastion.exchange.Markets;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.repository.JournalEntryRepository;
import com.codenameone.examples.bastion.repository.ProfileRepository;
import com.codenameone.examples.bastion.repository.TransferRepository;

import java.io.IOException;
import java.util.Arrays;

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
///    new payee during its cooling-off period -- fiat in its own units, crypto
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
    private final long perTransfer;
    private final long daily;
    private final long stepUp;
    private final long coolOffMillis;
    private final long coolOffLimit;
    private final boolean enforceDevices;
    private final Markets markets;
    private final long cryptoPerMove;
    private final long cryptoDaily;

    public Money(Ledger ledger, Audit audit, TransferRepository transfers,
            JournalEntryRepository entries, ProfileRepository profiles, Markets markets,
            @Value("${bastion.crypto.limit.per-withdrawal:1000000}") String cryptoPerMove,
            @Value("${bastion.crypto.limit.daily:2000000}") String cryptoDaily,
            @Value("${bastion.limit.per-transfer:500000}") String perTransfer,
            @Value("${bastion.limit.daily:1000000}") String daily,
            @Value("${bastion.stepup.threshold:100000}") String stepUp,
            @Value("${bastion.payee.cooloff-hours:24}") String coolOffHours,
            @Value("${bastion.payee.cooloff-limit:10000}") String coolOffLimit,
            @Value("${bastion.device.policy.enforce:true}") String enforceDevices) {
        this.ledger = ledger;
        this.audit = audit;
        this.transfers = transfers;
        this.entries = entries;
        this.profiles = profiles;
        // In ledger units of a fiat asset: cents. 5,000.00 a movement, 10,000.00
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
        long value = "FIAT".equals(asset.getKind()) ? amount
                : markets.value(asset.getCode(), Math.max(amount, 0L), "USD");
        return value > stepUp;
    }

    /// Refuses a device that may not send money away, when the policy says so.
    public void requireTrusted(Device device) {
        if (enforceDevices && !Devices.trusted(device)) {
            throw new ResponseStatusException(403, "This device is not attested, or is rooted: "
                    + "it can move money only between your own accounts");
        }
    }

    /// Moves `amount` between two of the caller's accounts.
    public ReceiptDto internal(String owner, String key, LedgerAccount from, LedgerAccount to,
            long amount, String memo) throws IOException {
        if (from.getId().equals(to.getId())) {
            throw new ResponseStatusException(400, "Choose two different accounts");
        }
        // A move is one asset in and the same out; changing one into another
        // is a trade, at a price, on the exchange.
        if (!from.getAsset().equals(to.getAsset())) {
            throw new ResponseStatusException(400,
                    "Both accounts must hold the same currency. Use the exchange to convert.");
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

    /// Coins out of the bank to an allow-listed address: into SYS_CHAIN,
    /// standing for the blockchain transaction a real exchange would
    /// broadcast. The caller has checked the address is the owner's, for this
    /// asset, and past its cooling-off.
    public ReceiptDto withdrawCrypto(String owner, String key, LedgerAccount from,
            CryptoAddress address, long amount) throws IOException {
        LedgerAccount chain = ledger.system(from.getAsset(), AccountKinds.SYS_CHAIN);
        return move(new Move(owner, key, "CRYPTO_OUT", from, chain, address.getId(), amount,
                true, "to " + address.getAddress(), null));
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
            throw new ResponseStatusException(400, "The amount must be more than zero");
        }
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
                throw new ResponseStatusException(422,
                        "That Idempotency-Key was already used for a different request");
            }
            return receipt(first);
        }
        if (m.outgoing) {
            // Behind the customer's own row: two payments at once would
            // otherwise each count the day's total without the other and
            // together pass a limit neither may.
            profiles.hold(m.owner);
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
        if (m.amount > perTransfer) {
            throw new ResponseStatusException(409, "That is more than one payment may move");
        }
        Long sent = transfers.sentSince(m.owner, asset.getCode(), now - DAY);
        if ((sent == null ? 0L : sent.longValue()) + m.amount > daily) {
            throw new ResponseStatusException(409, "That would pass your limit for the day");
        }
        if (m.payee != null && now < m.payee.getCreatedAt() + coolOffMillis) {
            Long paid = transfers.throughSince(m.payee.getId(), m.payee.getCreatedAt());
            if ((paid == null ? 0L : paid.longValue()) + m.amount > coolOffLimit) {
                throw new ResponseStatusException(409, "A new payee can receive only "
                        + coolOffLimit + " until their cooling-off period ends");
            }
        }
        if (m.card != null) {
            Long spent = transfers.throughSince(m.card.getId(), now - DAY);
            if ((spent == null ? 0L : spent.longValue()) + m.amount > m.card.getDailyLimit()) {
                throw new ResponseStatusException(409, "That is over the card's daily limit");
            }
        }
    }

    private void checkCryptoLimits(Move m, Asset asset) throws IOException {
        long now = System.currentTimeMillis();
        long value = markets.value(asset.getCode(), m.amount, "USD");
        if (value > cryptoPerMove) {
            throw new ResponseStatusException(409, "That is more than one crypto movement may move");
        }
        long sent = value;
        for (Asset coin : ledger.assets()) {
            if (!"FIAT".equals(coin.getKind())) {
                Long moved = transfers.sentSince(m.owner, coin.getCode(), now - DAY);
                if (moved != null && moved.longValue() > 0) {
                    sent = saturatedAdd(sent, markets.value(coin.getCode(), moved.longValue(),
                            "USD"));
                }
            }
        }
        if (sent > cryptoDaily) {
            throw new ResponseStatusException(409, "That would pass your crypto limit for the day");
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
                t.getAsset(), t.getAmount(), t.getMemo(), t.getCreatedAt());
    }

    /// What one call asked to move.
    private static final class Move {
        final String owner;
        final String key;
        final String kind;
        final LedgerAccount from;
        final LedgerAccount to;
        final String via;
        final long amount;
        final boolean outgoing;
        final String memo;
        final Payee payee;
        Card card;

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
