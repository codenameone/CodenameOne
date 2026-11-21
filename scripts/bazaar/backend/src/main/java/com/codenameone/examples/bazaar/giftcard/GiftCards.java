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
package com.codenameone.examples.bazaar.giftcard;

import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.GiftCardBalanceDto;
import com.codenameone.examples.bazaar.api.GiftCardDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quoter;
import com.codenameone.examples.bazaar.domain.GiftCard;
import com.codenameone.examples.bazaar.domain.GiftCardTx;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.GiftCardRepository;
import com.codenameone.examples.bazaar.repository.GiftCardTxRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Gift cards: issued by a store's managers, spent at checkout, topped back up
/// by a refund or a cancelled order. Every change of balance is a
/// [GiftCardTx] row beside it.
///
/// A code is 16 characters from an alphabet with no 0/O or 1/I, drawn from
/// the platform's secure random. 32 letters make 80 bits, and each byte
/// picks one with `& 31`, which is exactly uniform because 256 is a multiple
/// of 32. The balance lookup by code is rate limited ([SecurityConfig]) so it
/// cannot be used to guess codes.
@Component
public class GiftCards {
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int CODE_LENGTH = 16;
    /// The most a card may be issued for: $10,000 in cents.
    public static final long MAX_CENTS = 1000000L;

    private final GiftCardRepository cards;
    private final GiftCardTxRepository txs;
    private final StoreRepository stores;
    private final StoreAccess access;
    private final Emails emails;

    public GiftCards(GiftCardRepository cards, GiftCardTxRepository txs, StoreRepository stores,
            StoreAccess access, Emails emails) {
        this.emails = emails;
        this.cards = cards;
        this.txs = txs;
        this.stores = stores;
        this.access = access;
    }

    /// A fresh random code.
    public static String newCode() throws IOException {
        byte[] random = Crypto.randomBytes(CODE_LENGTH);
        StringBuilder out = new StringBuilder(CODE_LENGTH);
        for (int iter = 0; iter < CODE_LENGTH; iter++) {
            out.append(ALPHABET.charAt(random[iter] & 31));
        }
        return out.toString();
    }

    @Transactional(readOnly = true)
    public List<GiftCardDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<GiftCard> rows = cards.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<GiftCardDto> out = new ArrayList<GiftCardDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), false));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public GiftCardDto issue(String username, String storeId, GiftCardDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null || wanted.initialCents() <= 0 || wanted.initialCents() > MAX_CENTS) {
            throw new ResponseStatusException(400, "A gift card is worth between 1 cent and 10,000");
        }
        GiftCard card = new GiftCard();
        card.setId(Ids.next());
        card.setStoreId(storeId);
        String code = newCode();
        // 80 random bits never collide in practice; the unique key would
        // refuse one that did.
        card.setCode(code);
        card.setLastFour(code.substring(CODE_LENGTH - 4));
        card.setInitialCents(wanted.initialCents());
        card.setBalanceCents(wanted.initialCents());
        card.setCustomerId(Codes.cut(wanted.customerId(), 32));
        card.setNote(Codes.cut(wanted.note(), 500));
        card.setExpiresAt(wanted.expiresAt() > 0 ? Instant.ofEpochMilli(wanted.expiresAt())
                : Instant.EPOCH);
        card.setActive(true);
        card.setCreatedAt(Instant.now());
        cards.insert(card);
        record(card.getId(), "", wanted.initialCents(), "Issued by " + username);
        Store owner = stores.findById(storeId);
        emails.giftCard(storeId, card.getCustomerId(), code, wanted.initialCents(),
                owner == null ? "" : owner.getCurrency());
        return dto(card, true);
    }

    @Transactional(rollbackFor = IOException.class)
    public GiftCardDto disable(String username, String storeId, String cardId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        GiftCard card = cardId == null ? null : cards.findById(cardId);
        if (card == null || !storeId.equals(card.getStoreId())) {
            throw new ResponseStatusException(404, "No such gift card");
        }
        card.setActive(false);
        cards.save(card);
        return dto(card, false);
    }

    /// The balance of the card with this code. A wrong code is a 404 that
    /// says nothing more.
    @Transactional(readOnly = true)
    public GiftCardBalanceDto balance(String code) throws IOException {
        String clean = Codes.normalize(code);
        GiftCard card = clean.length() == CODE_LENGTH ? cards.findByCode(clean) : null;
        if (card == null) {
            throw new ResponseStatusException(404, "No such gift card");
        }
        Store store = stores.findById(card.getStoreId());
        return new GiftCardBalanceDto(card.getLastFour(), card.getBalanceCents(),
                store == null ? "" : store.getCurrency(),
                Quoter.usable(card, card.getStoreId(), Instant.now()),
                card.getExpiresAt() == null ? 0 : card.getExpiresAt().toEpochMilli());
    }

    /// Takes `amount` off a card for an order. Answers false, changing
    /// nothing, when the card is no longer active or has less.
    @Transactional(rollbackFor = IOException.class)
    public boolean redeem(String cardId, String orderId, long amount) throws IOException {
        if (amount <= 0) {
            return true;
        }
        if (cards.debitIf(cardId, amount) != 1) {
            return false;
        }
        record(cardId, orderId, -amount, "Spent");
        return true;
    }

    /// Puts `amount` back on a card: an order refunded or that did not go
    /// through.
    @Transactional(rollbackFor = IOException.class)
    public void credit(String cardId, String orderId, long amount, String note)
            throws IOException {
        if (amount <= 0) {
            return;
        }
        cards.credit(cardId, amount);
        record(cardId, orderId, amount, note);
    }

    /// What gift cards paid toward an order, less what went back to them.
    @Transactional(readOnly = true)
    public long spentOn(String orderId) throws IOException {
        List<GiftCardTx> rows = txs.findByOrderId(orderId);
        long sum = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            sum -= rows.get(iter).getDeltaCents();
        }
        return sum;
    }

    /// The card an order was paid with, or null.
    @Transactional(readOnly = true)
    public String cardOf(String orderId) throws IOException {
        List<GiftCardTx> rows = txs.findByOrderId(orderId);
        return rows.isEmpty() ? null : rows.get(0).getGiftCardId();
    }

    private void record(String cardId, String orderId, long delta, String note)
            throws IOException {
        GiftCardTx tx = new GiftCardTx();
        tx.setId(Ids.next());
        tx.setGiftCardId(cardId);
        tx.setOrderId(orderId == null ? "" : orderId);
        tx.setDeltaCents(delta);
        tx.setNote(Codes.cut(note, 200));
        tx.setCreatedAt(Instant.now());
        txs.insert(tx);
    }

    static GiftCardDto dto(GiftCard card, boolean withCode) {
        return new GiftCardDto(card.getId(), withCode ? card.getCode() : null, card.getLastFour(),
                card.getInitialCents(), card.getBalanceCents(), card.isActive(), card.getNote(),
                card.getExpiresAt() == null ? 0 : card.getExpiresAt().toEpochMilli(),
                card.getCustomerId(), card.getCreatedAt() == null ? 0
                        : card.getCreatedAt().toEpochMilli());
    }
}
