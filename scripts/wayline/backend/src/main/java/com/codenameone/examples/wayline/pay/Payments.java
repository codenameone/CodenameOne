/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.pay;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.account.ProfileRepository;
import com.codenameone.examples.wayline.api.CardDto;
import com.codenameone.examples.wayline.api.PaymentConfigDto;
import com.codenameone.examples.wayline.api.PaymentMethodDto;
import com.codenameone.examples.wayline.api.PaymentSetupDto;
import com.codenameone.examples.wayline.api.ReceiptDto;
import com.codenameone.examples.wayline.domain.Payment;
import com.codenameone.examples.wayline.domain.PaymentCustomer;
import com.codenameone.examples.wayline.domain.PaymentMethod;
import com.codenameone.examples.wayline.domain.PaymentSetup;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.live.LiveHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The ways a rider can pay, and what each ride came to.
///
/// A ride is paid for when it completes: the fare and the service fee, charged
/// to the method the ride was asked for with. A tip is a second charge, made
/// when the rider leaves one. A ride paid in cash is recorded and nothing is
/// charged. What the driver earns from all this is `driver/Earnings`' business.
///
/// Everything here that takes a `username` acts on that user's own methods and
/// rides and no one else's; the endpoints pass the signed-in user.
///
/// The processor is somewhere else, and a call to it can take seconds, so none
/// is made with a transaction open. Whatever talks to it is three steps: a
/// transaction that reads, or claims, what the call needs and commits; the
/// call; and a transaction that records what came of it, or gives the claim
/// back.
@Component
public class Payments {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";
    /// The id and the label of the method every account has.
    public static final String CASH = "cash";
    public static final String CASH_LABEL = "Cash";
    private static final int MAX_CARDS = 8;
    /// A setup not finished within this long is abandoned.
    private static final long SETUP_MILLIS = 3600000L;
    private static final long MAX_TIP_CENTS = 20000L;

    private final PaymentRepository repository;
    private final ProfileRepository profiles;
    private final PaymentProvider provider;
    private final LiveHub live;

    public Payments(PaymentRepository repository, ProfileRepository profiles,
            PaymentProvider provider, LiveHub live) {
        this.repository = repository;
        this.profiles = profiles;
        this.provider = provider;
        this.live = live;
    }

    public PaymentConfigDto config(String currency) {
        PaymentConfigDto dto = new PaymentConfigDto();
        dto.provider = provider.name();
        dto.currency = currency;
        return dto;
    }

    // --------------------------------------------------------------- methods

    /// The saved cards, oldest first, then cash. Exactly one of them is the
    /// default: a card when one is marked, cash otherwise.
    @Transactional(readOnly = true)
    public List<PaymentMethodDto> methods(String username) throws IOException {
        List<PaymentMethod> rows = repository.cards(username);
        List<PaymentMethodDto> out = new ArrayList<PaymentMethodDto>();
        boolean marked = false;
        for (int iter = 0; iter < rows.size(); iter++) {
            PaymentMethodDto dto = card(rows.get(iter));
            marked |= dto.isDefault;
            out.add(dto);
        }
        out.add(cash(!marked));
        return out;
    }

    /// The id of the user's default method.
    @Transactional(readOnly = true)
    public String defaultMethod(String username) throws IOException {
        PaymentMethod row = repository.defaultCard(username);
        return row == null ? CASH : row.id;
    }

    /// Begins adding a card.
    public PaymentSetupDto startSetup(String username, String displayName) throws IOException {
        String customer = customerForSetup(username);
        String id = Ids.next();
        PaymentProvider.Setup setup;
        try {
            if (customer == null) {
                customer = keepCustomer(username, provider.createCustomer(username, displayName));
            }
            setup = provider.startCardSetup(customer, id);
        } catch (IOException failed) {
            throw unavailable("card setup", failed);
        }
        keepSetup(username, id, setup.reference);
        PaymentSetupDto dto = new PaymentSetupDto();
        dto.id = id;
        dto.hosted = setup.hosted;
        dto.url = setup.url;
        return dto;
    }

    /// The rider's reference at the processor, or null when they have none
    /// yet. Answers 409 for an account that has all the cards it may.
    @Transactional(readOnly = true)
    private String customerForSetup(String username) {
        if (repository.countCards(username) >= MAX_CARDS) {
            throw new ResponseStatusException(409, "Remove a card before adding another");
        }
        PaymentCustomer row = repository.customer(username);
        return row == null ? null : row.reference;
    }

    /// Keeps the reference the processor made for a rider on first use, and
    /// returns the one the account has: another request may have kept its own
    /// while this one was asking for it.
    @Transactional
    private String keepCustomer(String username, String reference) {
        PaymentCustomer row = repository.customer(username);
        if (row == null) {
            row = new PaymentCustomer();
            row.username = username;
            row.reference = reference;
            repository.add(row);
        }
        return row.reference;
    }

    @Transactional
    private void keepSetup(String username, String id, String reference) {
        PaymentSetup row = new PaymentSetup();
        row.id = id;
        row.username = username;
        row.reference = reference;
        row.createdAt = System.currentTimeMillis();
        row.completed = false;
        repository.add(row);
    }

    /// Finishes adding a card and returns it. The first card an account saves
    /// becomes its default: someone who adds a card means to pay with it.
    /// Answers 402 when the card was refused, with the reason.
    public PaymentMethodDto completeSetup(String username, String setupId, CardDto typed)
            throws IOException {
        String[] references = openSetup(username, setupId);
        PaymentProvider.Card card;
        try {
            card = provider.finishCardSetup(references[0], references[1], typed);
        } catch (PaymentDeclinedException declined) {
            throw new ResponseStatusException(402, declined.getMessage());
        } catch (IOException failed) {
            throw unavailable("card setup", failed);
        }
        return keepCard(username, setupId, Ids.next(), card);
    }

    /// What the processor calls the rider and a setup of theirs that is still
    /// open, in that order. Answers 404 for a setup that is not.
    @Transactional(readOnly = true)
    private String[] openSetup(String username, String setupId) {
        PaymentSetup setup = repository.setup(username, setupId);
        if (setup == null || setup.completed
                || setup.createdAt < System.currentTimeMillis() - SETUP_MILLIS) {
            throw new ResponseStatusException(404, "No such card setup");
        }
        PaymentCustomer customer = repository.customer(username);
        return new String[] {customer == null ? "" : customer.reference, setup.reference};
    }

    /// Closes the setup and saves its card, together or not at all.
    @Transactional
    private PaymentMethodDto keepCard(String username, String setupId, String id,
            PaymentProvider.Card card) {
        // Claimed with a conditional update, so a request sent twice saves the
        // card once.
        if (!repository.claimSetup(setupId)) {
            throw new ResponseStatusException(404, "No such card setup");
        }
        boolean first = repository.defaultCard(username) == null;
        // Cards are listed in the order they were added, and two added in the
        // same millisecond would have no order: the later is stamped after
        // the one before it.
        Long newest = repository.newestCardAt(username);
        long after = newest == null ? 0L : newest.longValue() + 1L;
        PaymentMethod row = new PaymentMethod();
        row.id = id;
        row.username = username;
        row.brand = cut(card.brand, 24);
        row.last4 = cut(card.last4, 4);
        row.expMonth = card.expMonth;
        row.expYear = card.expYear;
        row.token = card.token;
        row.preferred = first;
        row.createdAt = Math.max(System.currentTimeMillis(), after);
        repository.add(row);
        return card(row);
    }

    /// Makes a method the one rides are paid with unless they say otherwise.
    @Transactional
    public PaymentMethodDto makeDefault(String username, String id) throws IOException {
        PaymentMethod chosen = CASH.equals(id) ? null : owned(username, id);
        // The cards are few, and each is written only if its mark changed.
        List<PaymentMethod> rows = repository.cards(username);
        for (int iter = 0; iter < rows.size(); iter++) {
            PaymentMethod row = rows.get(iter);
            row.preferred = chosen != null && row.id.equals(chosen.id);
        }
        if (chosen == null) {
            return cash(true);
        }
        chosen.preferred = true;
        return card(chosen);
    }

    /// Forgets a card. Cash cannot be removed, and neither can the card a ride
    /// under way is to be paid with.
    @Transactional
    public PaymentMethodDto remove(String username, String id) throws IOException {
        if (CASH.equals(id)) {
            throw new ResponseStatusException(400, "Cash is always available");
        }
        PaymentMethod row = owned(username, id);
        PaymentMethodDto dto = card(row);
        if (repository.paysForRideUnderWay(username, id)) {
            throw new ResponseStatusException(409, "That card is paying for a ride under way");
        }
        repository.remove(row);
        dto.isDefault = false;
        return dto;
    }

    /// The method a ride is asked for with: the one named, or the default when
    /// none is. Answers 400 for a method that is not the rider's.
    ///
    /// @return the method's id and its label, in that order
    @Transactional(readOnly = true)
    public String[] choose(String username, String wanted) throws IOException {
        String id = wanted == null || wanted.trim().length() == 0 ? defaultMethod(username)
                : wanted.trim();
        if (CASH.equals(id)) {
            return new String[] {CASH, CASH_LABEL};
        }
        PaymentMethod row = repository.card(username, id);
        if (row == null) {
            throw new ResponseStatusException(400, "Choose a way to pay");
        }
        return new String[] {id, label(row)};
    }

    /// Everything of a user's that this class keeps, for an account being closed.
    @Transactional
    public void forget(String username) throws IOException {
        repository.removeCards(username);
        repository.removeSetups(username);
        repository.removeCustomer(username);
    }

    // --------------------------------------------------------------- charges

    /// Settles a ride that has just completed and returns [#PAID] or [#FAILED].
    /// A ride is settled once: its payment is keyed by the ride, and a ride
    /// completes once. Called with no transaction open, since the card is
    /// charged here; what came of the charge is then written in one.
    public String settle(Ride ride) throws IOException {
        long amount = ride.fareCents + ride.feeCents;
        String status = PAID;
        String reference = CASH;
        if (!CASH.equals(ride.methodId)) {
            try {
                reference = charge("ride-" + ride.id, ride.rider, ride.methodId, amount,
                        ride.currency, "Wayline ride");
            } catch (IOException declined) {
                // The ride and the amount, never the card.
                System.err.println("payment for ride " + ride.id + " failed: "
                        + declined.getMessage());
                status = FAILED;
                reference = "";
            }
        }
        keepPayment(ride.id, amount, ride.currency, status, reference);
        return status;
    }

    @Transactional
    private void keepPayment(String rideId, long amount, String currency, String status,
            String reference) {
        Payment row = new Payment();
        row.rideId = rideId;
        row.amountCents = amount;
        row.currency = currency;
        row.status = status;
        row.providerRef = reference;
        row.createdAt = System.currentTimeMillis();
        repository.add(row);
    }

    /// Tips the driver of a completed ride, once. Charged to the method the
    /// ride was paid with; a cash ride's tip is recorded as given in cash.
    public ReceiptDto tip(String rider, String rideId, long amountCents) throws IOException {
        Ride ride = claimTip(rider, rideId, amountCents);
        if (!CASH.equals(ride.methodId)) {
            String reference;
            try {
                reference = charge("tip-" + rideId, rider, ride.methodId, amountCents,
                        ride.currency, "Wayline tip");
            } catch (IOException declined) {
                releaseTip(rideId);
                if (declined instanceof PaymentDeclinedException) {
                    throw new ResponseStatusException(402, declined.getMessage());
                }
                throw unavailable("tip", declined);
            }
            keepTip(rideId, reference);
        }
        // The driver's earnings changed; their app reads them again.
        live.rideChanged(rideId, ride.state, rider, ride.driver, "");
        return receipt(rider, false, rideId);
    }

    /// Sets the tip on the rider's ride and returns the ride as it was just
    /// before. Committed before the card is charged.
    @Transactional
    private Ride claimTip(String rider, String rideId, long amountCents) {
        Ride ride = repository.ride(rideId);
        if (ride == null || !rider.equals(ride.rider)) {
            throw new ResponseStatusException(404, "No such ride");
        }
        if (amountCents < 1L || amountCents > MAX_TIP_CENTS) {
            throw new ResponseStatusException(400, "That is not an amount for a tip");
        }
        // Claimed before the charge: of two requests, one changes the row and
        // only that one charges.
        if (!repository.claimTip(rideId, rider, amountCents)) {
            throw new ResponseStatusException(409, "That ride cannot be tipped now");
        }
        return ride;
    }

    @Transactional
    private void releaseTip(String rideId) {
        repository.releaseTip(rideId);
    }

    @Transactional
    private void keepTip(String rideId, String reference) {
        Payment payment = repository.payment(rideId);
        if (payment != null) {
            payment.tipRef = reference;
        }
    }

    /// Returns what a ride cost to the card that paid for it, tip included.
    /// For an admin. A cash ride has nothing to return, and a ride is refunded
    /// once.
    public ReceiptDto refund(String rideId, String reason) throws IOException {
        Ride ride = claimRefund(rideId);
        Payment payment = payment(rideId);
        String reference;
        try {
            reference = provider.refund(payment.providerRef, payment.amountCents);
            if (payment.tipRef.length() > 0) {
                provider.refund(payment.tipRef, ride.tipCents);
            }
        } catch (IOException failed) {
            releaseRefund(rideId);
            throw unavailable("refund", failed);
        }
        keepRefund(rideId, cut(reference, 64), cut(reason == null ? "" : reason.trim(), 255));
        live.rideChanged(rideId, ride.state, ride.rider, ride.driver, "");
        return receipt("", true, rideId);
    }

    /// Marks the ride refunded and returns it as it was just before. Committed
    /// before the processor is asked for the money: of two requests, one
    /// changes the row and only that one refunds.
    @Transactional
    private Ride claimRefund(String rideId) {
        Ride ride = repository.ride(rideId);
        if (ride == null) {
            throw new ResponseStatusException(404, "No such ride");
        }
        if (CASH.equals(ride.methodId)) {
            throw new ResponseStatusException(409, "A ride paid in cash has nothing to refund");
        }
        if (!repository.claimRefund(rideId)) {
            throw new ResponseStatusException(409, "That ride has no payment to refund");
        }
        return ride;
    }

    @Transactional(readOnly = true)
    private Payment payment(String rideId) {
        return repository.payment(rideId);
    }

    @Transactional
    private void releaseRefund(String rideId) {
        repository.releaseRefund(rideId);
    }

    @Transactional
    private void keepRefund(String rideId, String reference, String reason) {
        Payment payment = repository.payment(rideId);
        payment.status = REFUNDED;
        payment.refundRef = reference;
        payment.refundReason = reason;
        payment.refundedAt = System.currentTimeMillis();
    }

    // -------------------------------------------------------------- receipts

    /// A rider's receipts, newest first: one for every ride that completed.
    @Transactional(readOnly = true)
    public List<ReceiptDto> receipts(String rider) throws IOException {
        return receipts(repository.completed(rider, 50), false);
    }

    /// Every receipt, newest first, for the admin.
    @Transactional(readOnly = true)
    public List<ReceiptDto> allReceipts() throws IOException {
        return receipts(repository.completed(200), true);
    }

    /// The receipt of one ride: what it will cost while it is under way, what
    /// it cost once it completed. Answers 404 for a ride that is not the
    /// viewer's, and for one that ended without completing, which cost nothing.
    @Transactional(readOnly = true)
    public ReceiptDto receipt(String viewer, boolean admin, String rideId) throws IOException {
        Ride ride = repository.ride(rideId);
        String state = ride == null ? "" : ride.state;
        if (ride == null || !(admin || viewer.equals(ride.rider))
                || state.startsWith("CANCELLED") || "NO_DRIVERS".equals(state)) {
            throw new ResponseStatusException(404, "No such receipt");
        }
        Payment payment = repository.payment(rideId);
        return receipt(ride, payment == null ? 0L : payment.createdAt, admin,
                new HashMap<String, Profile>());
    }

    /// The rides' receipts. When each was paid is on its payment and not on
    /// the ride, so the payments of all of them are read at once and matched
    /// to their rides here.
    private List<ReceiptDto> receipts(List<Ride> rides, boolean admin) {
        Map<String, Long> paidAt = new HashMap<String, Long>();
        if (rides.size() > 0) {
            List<String> ids = new ArrayList<String>();
            for (int iter = 0; iter < rides.size(); iter++) {
                ids.add(rides.get(iter).id);
            }
            List<Payment> payments = repository.payments(ids);
            for (int iter = 0; iter < payments.size(); iter++) {
                Payment payment = payments.get(iter);
                paidAt.put(payment.rideId, Long.valueOf(payment.createdAt));
            }
        }
        Map<String, Profile> drivers = new HashMap<String, Profile>();
        List<ReceiptDto> out = new ArrayList<ReceiptDto>();
        for (int iter = 0; iter < rides.size(); iter++) {
            Ride ride = rides.get(iter);
            Long at = paidAt.get(ride.id);
            out.add(receipt(ride, at == null ? 0L : at.longValue(), admin, drivers));
        }
        return out;
    }

    private ReceiptDto receipt(Ride ride, long paidAt, boolean admin,
            Map<String, Profile> drivers) {
        ReceiptDto dto = new ReceiptDto();
        dto.rideId = ride.id;
        dto.currency = ride.currency;
        long fare = ride.fareCents;
        dto.distanceFare = ride.distanceCents;
        dto.timeFare = ride.timeCents;
        // Whatever of the fare the other two parts do not account for, which
        // for a ride asked for before the parts were kept is all of it.
        dto.baseFare = fare - dto.distanceFare - dto.timeFare;
        dto.serviceFee = ride.feeCents;
        dto.tip = ride.tipCents;
        dto.total = fare + dto.serviceFee + dto.tip - dto.discount;
        dto.methodLabel = ride.methodLabel;
        String paid = ride.paymentStatus;
        boolean cash = CASH.equals(ride.methodId);
        if (PAID.equals(paid)) {
            dto.status = cash ? "cash" : "paid";
        } else if (FAILED.equals(paid)) {
            dto.status = "failed";
        } else if (REFUNDED.equals(paid)) {
            dto.status = "refunded";
        } else {
            dto.status = "pending";
        }
        dto.paidAt = PAID.equals(paid) || REFUNDED.equals(paid) ? paidAt : 0L;
        dto.pickupName = ride.pickupAddress;
        dto.dropoffName = ride.dropoffAddress;
        dto.distanceMeters = ride.distanceMeters;
        dto.durationSeconds = ride.durationSeconds;
        dto.riderUsername = admin ? ride.rider : "";
        dto.driverName = "";
        dto.vehicle = "";
        String driver = ride.driver;
        if (driver.length() > 0 && !"OFFERED".equals(ride.state)) {
            Profile who = drivers.get(driver);
            if (who == null && !drivers.containsKey(driver)) {
                who = profiles.find(driver);
                drivers.put(driver, who);
            }
            if (who != null) {
                dto.driverName = who.displayName;
                dto.vehicle = who.vehicle;
            }
        }
        return dto;
    }

    // ------------------------------------------------------------- internals

    /// Charges one of a rider's saved cards. What the charge needs is read in
    /// a transaction that has ended by the time the processor is called.
    private String charge(String key, String rider, String methodId, long amountCents,
            String currency, String description) throws IOException {
        String[] references = chargeable(rider, methodId);
        if (references == null) {
            throw new PaymentDeclinedException("The card is no longer on the account");
        }
        return cut(provider.charge(key, references[0], references[1], amountCents, currency,
                description), 64);
    }

    /// What the processor calls the rider and one of their cards, in that
    /// order, or null when either is gone.
    @Transactional(readOnly = true)
    private String[] chargeable(String rider, String methodId) {
        PaymentMethod method = repository.card(rider, methodId);
        PaymentCustomer customer = repository.customer(rider);
        if (method == null || customer == null) {
            return null;
        }
        return new String[] {customer.reference, method.token};
    }

    private PaymentMethod owned(String username, String id) {
        PaymentMethod row = repository.card(username, id);
        if (row == null) {
            // Someone else's card is told apart from no card by nobody.
            throw new ResponseStatusException(404, "No such payment method");
        }
        return row;
    }

    private static PaymentMethodDto card(PaymentMethod row) {
        PaymentMethodDto dto = new PaymentMethodDto();
        dto.id = row.id;
        dto.kind = "card";
        dto.brand = row.brand;
        dto.last4 = row.last4;
        dto.expMonth = row.expMonth;
        dto.expYear = row.expYear;
        dto.isDefault = row.preferred;
        dto.label = label(row);
        return dto;
    }

    private static PaymentMethodDto cash(boolean isDefault) {
        PaymentMethodDto dto = new PaymentMethodDto();
        dto.id = CASH;
        dto.kind = "cash";
        dto.brand = "";
        dto.last4 = "";
        dto.isDefault = isDefault;
        dto.label = CASH_LABEL;
        return dto;
    }

    private static String label(PaymentMethod method) {
        return method.brand + " " + method.last4;
    }

    private static String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }

    /// A fault at the processor, as the app is told of it. What went wrong goes
    /// to the log; it describes the request, never a card or a key.
    private static ResponseStatusException unavailable(String what, IOException failed) {
        System.err.println("payment provider failed during " + what + ": " + failed.getMessage());
        return new ResponseStatusException(502, "Payments are unavailable right now");
    }
}
