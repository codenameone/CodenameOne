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

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.Crypto;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.CardDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Card;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.repository.CardRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Virtual cards: one per checking account, frozen and unfrozen by the
/// customer, each with a daily spending limit of its own under the
/// customer's overall one.
///
/// The card number is simulated. A real issuer's processor holds the number
/// in its PCI-scoped vault and the bank keeps a token and the last four
/// digits, which is all this table has: there is no full number here to
/// steal.
@Component
public class Cards {
    /// 500.00 a day until the customer says otherwise.
    static final long DEFAULT_LIMIT = 50000L;
    static final long MAX_LIMIT = 500000L;

    private final CardRepository cards;
    private final Audit audit;

    public Cards(CardRepository cards, Audit audit) {
        this.cards = cards;
        this.audit = audit;
    }

    @Transactional(rollbackFor = Exception.class)
    public CardDto issue(String owner, LedgerAccount account) throws IOException {
        if (!AccountKinds.CHECKING.equals(account.getKind())) {
            throw Refused.with(400, "cards-issued-checking-accounts", "Cards are issued on checking accounts");
        }
        if (cards.findFirstByAccountId(account.getId()) != null) {
            throw Refused.with(409, "account-already-has-card", "That account already has a card");
        }
        byte[] random = Crypto.randomBytes(2);
        int digits = (((random[0] & 0xff) << 8) | (random[1] & 0xff)) % 10000;
        String last4 = Integer.toString(10000 + digits).substring(1);
        Card card = new Card();
        card.setId(Ids.next());
        card.setOwner(owner);
        card.setAccountId(account.getId());
        card.setLast4(last4);
        card.setDailyLimit(DEFAULT_LIMIT);
        card.setCreatedAt(System.currentTimeMillis());
        cards.insert(card);
        audit.append(owner, "CARD_ISSUED", card.getId(), "account " + account.getId()
                + " ending " + last4);
        return dto(card);
    }

    @Transactional(readOnly = true)
    public List<CardDto> list(String owner) throws IOException {
        List<CardDto> out = new ArrayList<CardDto>();
        for (Card c : cards.findByOwnerOrderByCreatedAtAsc(owner)) {
            out.add(dto(c));
        }
        return out;
    }

    /// `owner`'s card `id`, or 404 whether it is missing or someone else's.
    @Transactional(readOnly = true)
    public Card owned(String owner, String id) throws IOException {
        Card card = id == null ? null : cards.findById(id);
        if (card == null || !card.getOwner().equals(owner)) {
            throw Refused.with(404, "no-such-card", "No such card");
        }
        return card;
    }

    /// Freezing needs no signature: it only ever stops money, and a customer
    /// who has lost a phone must be able to do it from anywhere.
    @Transactional(rollbackFor = Exception.class)
    public CardDto freeze(String owner, String id, boolean frozen) throws IOException {
        Card card = owned(owner, id);
        cards.setFrozen(id, owner, frozen);
        card.setFrozen(frozen);
        audit.append(owner, frozen ? "CARD_FROZEN" : "CARD_UNFROZEN", id, "");
        return dto(card);
    }

    @Transactional(rollbackFor = Exception.class)
    public CardDto limit(String owner, String id, long limit) throws IOException {
        if (limit < 0 || limit > MAX_LIMIT) {
            throw Refused.with(400, "card-limit-range", "A card's limit is 0 to {0}", MAX_LIMIT);
        }
        Card card = owned(owner, id);
        cards.setLimit(id, owner, limit);
        audit.append(owner, "CARD_LIMIT", id, card.getDailyLimit() + " -> " + limit);
        card.setDailyLimit(limit);
        return dto(card);
    }

    static CardDto dto(Card c) {
        return new CardDto(c.getId(), c.getAccountId(), c.getLast4(), c.isFrozen(),
                c.getDailyLimit(), c.getCreatedAt());
    }
}
