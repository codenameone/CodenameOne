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
import com.codenameone.examples.bazaar.api.CardPaymentDto;
import com.codenameone.examples.bazaar.api.CardReaderDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.CardPayment;
import com.codenameone.examples.bazaar.domain.CardReader;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.pay.CardTerminal;
import com.codenameone.examples.bazaar.repository.CardPaymentRepository;
import com.codenameone.examples.bazaar.repository.CardReaderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Card readers at the registers and the payments pushed to them.
///
/// The flow is Stripe Terminal's server-driven one. An owner or manager
/// pairs a reader with the code it displays and places it at a register. To
/// take a card the register asks this server for a payment of the amount
/// due; the server opens a `card_present` intent at the processor and hands
/// it to that register's reader, which asks for the card. The register then
/// polls [#payment] (each poll asks the processor), and a webhook from the
/// processor ([#readerEvent]) settles the same row sooner. A SUCCEEDED
/// payment pays a sale when the sale's CARD tender names its id
/// ([#claim]), once.
@Component
public class CardReaders {
    public static final String ACTIVE = "ACTIVE";
    public static final String REMOVED = "REMOVED";

    private final StoreAccess access;
    private final Pos pos;
    private final StoreRepository stores;
    private final CardReaderRepository readers;
    private final CardPaymentRepository payments;
    private final CardTerminal terminal;

    public CardReaders(StoreAccess access, Pos pos, StoreRepository stores,
            CardReaderRepository readers, CardPaymentRepository payments, CardTerminal terminal) {
        this.access = access;
        this.pos = pos;
        this.stores = stores;
        this.readers = readers;
        this.payments = payments;
        this.terminal = terminal;
    }

    // -------------------------------------------------------------- readers

    @Transactional(readOnly = true)
    public List<CardReaderDto> readers(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<CardReader> rows = readers.findByStoreId(storeId);
        List<CardReaderDto> out = new ArrayList<CardReaderDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (ACTIVE.equals(rows.get(iter).getStatus())) {
                out.add(dto(rows.get(iter)));
            }
        }
        return out;
    }

    /// Pairs a reader and places it at a register; the register's previous
    /// reader, if any, is taken off it.
    public CardReaderDto addReader(String username, String storeId, CardReaderDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null || wanted.registrationCode() == null
                || wanted.registrationCode().trim().length() == 0) {
            throw new ResponseStatusException(400, "Enter the code the reader shows");
        }
        Register register = registerOf(storeId, wanted.registerId());
        String label = wanted.label() == null || wanted.label().trim().length() == 0
                ? register.getName() : wanted.label().trim();
        CardTerminal.Reader paired;
        try {
            paired = terminal.register(wanted.registrationCode().trim(), Codes.cut(label, 80));
        } catch (IOException refused) {
            throw new ResponseStatusException(400, refused.getMessage());
        }
        return keepReader(storeId, register.getId(), paired, label);
    }

    @Transactional
    private CardReaderDto keepReader(String storeId, String registerId, CardTerminal.Reader paired,
            String label) throws IOException {
        List<CardReader> there = readers.findByStoreIdAndRegisterIdAndStatus(storeId, registerId,
                ACTIVE);
        for (int iter = 0; iter < there.size(); iter++) {
            CardReader old = there.get(iter);
            old.setStatus(REMOVED);
            readers.save(old);
        }
        CardReader r = new CardReader();
        r.setId(Ids.next());
        r.setStoreId(storeId);
        r.setRegisterId(registerId);
        r.setProvider(terminal.name());
        r.setProviderRef(Codes.cut(paired.reference, 64));
        r.setLabel(Codes.cut(label, 80));
        r.setSerial(Codes.cut(paired.serial, 64));
        r.setStatus(ACTIVE);
        r.setCreatedAt(System.currentTimeMillis());
        readers.insert(r);
        return dto(r);
    }

    public CardReaderDto removeReader(String username, String storeId, String readerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        CardReader r = readerIn(storeId, readerId);
        if (ACTIVE.equals(r.getStatus())) {
            try {
                terminal.remove(r.getProviderRef());
            } catch (IOException unreachable) {
                // Taken out of use here all the same; the processor's record
                // pairs nothing without this row.
                System.err.println("[terminal] reader " + r.getId() + " not unpaired: "
                        + unreachable.getMessage());
            }
            r.setStatus(REMOVED);
            saveReader(r);
        }
        return dto(r);
    }

    @Transactional
    private void saveReader(CardReader r) {
        readers.save(r);
    }

    // ------------------------------------------------------------- payments

    /// Pushes a payment of `amountCents` to the register's reader. The
    /// register's `clientId` names it: asked again with the same one (an
    /// answer lost on the way back), it answers the payment already made.
    public CardPaymentDto start(String username, String storeId, String registerId,
            CardPaymentDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register register = registerOf(storeId, registerId);
        if (wanted == null || wanted.amountCents() <= 0) {
            throw new ResponseStatusException(400, "That is not an amount");
        }
        String clientId = wanted.clientId() == null ? "" : wanted.clientId().trim();
        if (clientId.length() == 0 || clientId.length() > 64) {
            throw new ResponseStatusException(400, "A card payment needs the register's id for it");
        }
        CardPayment again = existing(storeId, clientId);
        if (again != null && !CardTerminal.FAILED.equals(again.getStatus())
                && !CardTerminal.CANCELED.equals(again.getStatus())) {
            return dto(again);
        }
        CardReader reader = readerAt(storeId, register.getId());
        if (reader == null) {
            throw new ResponseStatusException(409, "No card reader at this register");
        }
        Store store = stores.findById(storeId);
        String id = again != null ? again.getId() : Ids.next();
        String intent;
        try {
            // Keyed by the row and the attempt, so a retry after a decline is
            // a new intent and a lost answer is not a second one.
            intent = terminal.createIntent(id + "-" + System.currentTimeMillis(),
                    wanted.amountCents(), store.getCurrency(), "Sale at " + register.getName());
            terminal.process(reader.getProviderRef(), intent);
        } catch (IOException refused) {
            throw new ResponseStatusException(502, "The card reader could not be reached: "
                    + refused.getMessage());
        }
        CardPayment p = again != null ? again : new CardPayment();
        p.setId(id);
        p.setStoreId(storeId);
        p.setRegisterId(register.getId());
        p.setReaderId(reader.getId());
        p.setClientId(clientId);
        p.setIntentRef(Codes.cut(intent, 64));
        p.setAmountCents(wanted.amountCents());
        p.setCurrency(store.getCurrency());
        p.setStatus(CardTerminal.PENDING);
        p.setMessage("");
        p.setCardLabel("");
        p.setOrderId("");
        long now = System.currentTimeMillis();
        if (again == null) {
            p.setCreatedAt(now);
        }
        p.setUpdatedAt(now);
        keep(p, again == null);
        return dto(p);
    }

    @Transactional
    private void keep(CardPayment p, boolean insert) {
        if (insert) {
            payments.insert(p);
        } else {
            payments.save(p);
        }
    }

    @Transactional(readOnly = true)
    private CardPayment existing(String storeId, String clientId) {
        return payments.findByStoreIdAndClientId(storeId, clientId);
    }

    /// The payment as it stands, asking the processor while it is PENDING.
    public CardPaymentDto payment(String username, String storeId, String paymentId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(refresh(paymentIn(storeId, paymentId)));
    }

    public CardPaymentDto cancel(String username, String storeId, String paymentId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        CardPayment p = refresh(paymentIn(storeId, paymentId));
        if (CardTerminal.PENDING.equals(p.getStatus())) {
            CardReader reader = readers.findById(p.getReaderId());
            try {
                terminal.cancel(reader == null ? "" : reader.getProviderRef(), p.getIntentRef());
            } catch (IOException unreachable) {
                throw new ResponseStatusException(502, "The card reader could not be reached");
            }
            p.setStatus(CardTerminal.CANCELED);
            p.setMessage("Cancelled at the register");
            p.setUpdatedAt(System.currentTimeMillis());
            keep(p, false);
        }
        return dto(p);
    }

    /// A processor event about a reader (`terminal.reader.action_succeeded`
    /// or `..._failed`). It only names the reader: what happened is asked of
    /// the processor, so a forged event can at worst make the server look.
    public void readerEvent(String readerRef) throws IOException {
        if (readerRef == null || readerRef.length() == 0) {
            return;
        }
        List<CardReader> rs = readers.findByProviderRef(readerRef);
        for (int iter = 0; iter < rs.size(); iter++) {
            List<CardPayment> waiting = payments.findByReaderIdAndStatus(rs.get(iter).getId(),
                    CardTerminal.PENDING);
            for (int p = 0; p < waiting.size(); p++) {
                refresh(waiting.get(p));
            }
        }
    }

    private CardPayment refresh(CardPayment p) throws IOException {
        if (!CardTerminal.PENDING.equals(p.getStatus())) {
            return p;
        }
        CardReader reader = readers.findById(p.getReaderId());
        CardTerminal.Outcome now;
        try {
            now = terminal.status(reader == null ? "" : reader.getProviderRef(), p.getIntentRef());
        } catch (IOException unreachable) {
            // Still pending as far as anyone knows; the register asks again.
            return p;
        }
        if (!CardTerminal.PENDING.equals(now.status)) {
            p.setStatus(now.status);
            p.setMessage(Codes.cut(now.message, 200));
            p.setCardLabel(Codes.cut(now.cardLabel, 40));
            p.setUpdatedAt(System.currentTimeMillis());
            keep(p, false);
        }
        return p;
    }

    // ---------------------------------------------------------------- sales

    /// The succeeded payment a sale's CARD tender names, checked against the
    /// store and the amount, or null when `reference` names no payment of
    /// this server (a card taken on a standalone terminal).
    @Transactional(readOnly = true)
    CardPayment forSale(String storeId, String reference, long amountCents) {
        if (reference == null || reference.length() == 0) {
            return null;
        }
        CardPayment p = payments.findById(reference);
        if (p == null || !storeId.equals(p.getStoreId())) {
            return null;
        }
        if (!CardTerminal.SUCCEEDED.equals(p.getStatus())) {
            throw new ResponseStatusException(409, "The card reader has not taken that payment");
        }
        if (p.getAmountCents() != amountCents) {
            throw new ResponseStatusException(400, "The card tender is not what the reader took");
        }
        if (p.getOrderId().length() > 0) {
            throw new ResponseStatusException(409, "That card payment paid another sale");
        }
        return p;
    }

    /// Spends the payment on `orderId`, inside the sale's transaction; false
    /// when another sale got it first.
    boolean claim(String paymentId, String orderId) {
        return payments.claim(paymentId, orderId, CardTerminal.SUCCEEDED, "") == 1;
    }

    // -------------------------------------------------------------- helpers

    @Transactional(readOnly = true)
    private Register registerOf(String storeId, String registerId) {
        return pos.register(storeId, registerId);
    }

    @Transactional(readOnly = true)
    private CardReader readerAt(String storeId, String registerId) {
        List<CardReader> rs = readers.findByStoreIdAndRegisterIdAndStatus(storeId, registerId,
                ACTIVE);
        return rs.isEmpty() ? null : rs.get(0);
    }

    @Transactional(readOnly = true)
    private CardReader readerIn(String storeId, String readerId) {
        CardReader r = readerId == null ? null : readers.findById(readerId);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such reader");
        }
        return r;
    }

    @Transactional(readOnly = true)
    private CardPayment paymentIn(String storeId, String paymentId) {
        CardPayment p = paymentId == null ? null : payments.findById(paymentId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such card payment");
        }
        return p;
    }

    private static CardReaderDto dto(CardReader r) {
        return new CardReaderDto(r.getId(), r.getLabel(), r.getRegisterId(), "", r.getSerial(),
                r.getProvider(), r.getStatus());
    }

    private static CardPaymentDto dto(CardPayment p) {
        return new CardPaymentDto(p.getId(), p.getRegisterId(), p.getReaderId(), p.getClientId(),
                p.getAmountCents(), p.getCurrency(), p.getStatus(), p.getMessage(),
                p.getCardLabel());
    }
}
