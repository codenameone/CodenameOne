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
package com.codenameone.examples.wayline.pay;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.CardDto;
import com.codenameone.examples.wayline.api.PaymentConfigDto;
import com.codenameone.examples.wayline.api.PaymentMethodDto;
import com.codenameone.examples.wayline.api.PaymentSetupDto;
import com.codenameone.examples.wayline.api.ReceiptDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.Payment;
import com.codenameone.examples.wayline.domain.PaymentCustomer;
import com.codenameone.examples.wayline.domain.PaymentMethod;
import com.codenameone.examples.wayline.domain.PaymentSetup;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.repository.PaymentCustomerRepository;
import com.codenameone.examples.wayline.repository.PaymentMethodRepository;
import com.codenameone.examples.wayline.repository.PaymentRepository;
import com.codenameone.examples.wayline.repository.PaymentSetupRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;
import com.codenameone.examples.wayline.repository.RideRepository;

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
    /// How long a completed ride's charge goes on being asked for. Short of
    /// the day a processor remembers the key a charge was first sent under.
    public static final long RETRY_MILLIS = 23L * 60L * 60L * 1000L;
    public static final String PAID = "PAID";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";
    /// Refunded at the processor, which has not yet said that the money is
    /// back on the card. See [#settleRefunds].
    public static final String REFUND_PENDING = "REFUND_PENDING";
    /// A refund the processor took and then failed or canceled: the rider has
    /// not had the money back.
    public static final String REFUND_FAILED = "REFUND_FAILED";
    /// The id and the label of the method every account has.
    public static final String CASH = "cash";
    public static final String CASH_LABEL = "Cash";
    private static final int MAX_CARDS = 8;
    /// A setup not finished within this long is abandoned.
    private static final long SETUP_MILLIS = 3600000L;
    /// How many setups an account may have open at once. Each is a session
    /// made at the processor and a row here; someone adding a card has one,
    /// or two after giving up on the first.
    private static final int MAX_OPEN_SETUPS = 5;
    private static final long MAX_TIP_CENTS = 20000L;
    /// The states of a ride that has not ended.
    private static final RideState[] UNDER_WAY = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS};

    private final PaymentMethodRepository cards;
    private final PaymentSetupRepository setups;
    private final PaymentCustomerRepository customers;
    private final PaymentRepository payments;
    private final RideRepository rides;
    private final ProfileRepository profiles;
    private final PaymentProvider provider;
    private final LiveHub live;

    public Payments(PaymentMethodRepository cards, PaymentSetupRepository setups,
            PaymentCustomerRepository customers, PaymentRepository payments,
            RideRepository rides, ProfileRepository profiles, PaymentProvider provider,
            LiveHub live) {
        this.cards = cards;
        this.setups = setups;
        this.customers = customers;
        this.payments = payments;
        this.rides = rides;
        this.profiles = profiles;
        this.provider = provider;
        this.live = live;
    }

    public PaymentConfigDto config(String currency) {
        return new PaymentConfigDto(provider.name(), currency);
    }

    // --------------------------------------------------------------- methods

    /// The saved cards, oldest first, then cash. Exactly one of them is the
    /// default: a card when one is marked, cash otherwise.
    @Transactional(readOnly = true)
    public List<PaymentMethodDto> methods(String username) throws IOException {
        List<PaymentMethod> rows = cards.findByUsernameOrderByCreatedAtAscIdAsc(username);
        List<PaymentMethodDto> out = new ArrayList<PaymentMethodDto>();
        boolean marked = false;
        for (int iter = 0; iter < rows.size(); iter++) {
            PaymentMethodDto dto = card(rows.get(iter));
            marked |= dto.isDefault();
            out.add(dto);
        }
        out.add(cash(!marked));
        return out;
    }

    /// The id of the user's default method.
    @Transactional(readOnly = true)
    public String defaultMethod(String username) throws IOException {
        PaymentMethod row = cards.findFirstByUsernameAndPreferredTrue(username);
        return row == null ? CASH : row.getId();
    }

    /// Begins adding a card.
    public PaymentSetupDto startSetup(String username, String displayName) throws IOException {
        String customer = customerForSetup(username);
        String id = Ids.next();
        PaymentProvider.Setup setup;
        try {
            if (customer == null) {
                String created = provider.createCustomer(username, displayName);
                customer = keepCustomer(username, created);
                if (!created.equals(customer)) {
                    // Not kept: the account was closed while the processor was
                    // asked (its clean-up found no customer to delete, so this
                    // one is ours to delete), or another request kept its own
                    // first. Either way the record made here would otherwise
                    // stay at the processor with the rider's name on it.
                    forgetQuietly(created);
                }
                if (customer == null) {
                    throw new ResponseStatusException(404, "No such account");
                }
            }
            setup = provider.startCardSetup(customer, id);
        } catch (IOException failed) {
            throw unavailable("card setup", failed);
        }
        keepSetup(username, id, setup.reference);
        return new PaymentSetupDto(id, setup.hosted, setup.url);
    }

    /// The rider's reference at the processor, or null when they have none
    /// yet. Answers 409 for an account that has all the cards it may, and
    /// 429 for one with as many setups open as it may have.
    @Transactional(readOnly = true)
    private String customerForSetup(String username) {
        if (cards.countByUsername(username) >= MAX_CARDS) {
            throw new ResponseStatusException(409, "Remove a card before adding another");
        }
        // Refused before the processor is asked: every setup is a session
        // made there, so a loop of these would otherwise make sessions
        // without end in the service's name.
        refuseTooManySetups(username);
        PaymentCustomer row = customers.findById(username);
        return row == null ? null : row.getReference();
    }

    /// Answers 429 when the account already has [#MAX_OPEN_SETUPS] setups
    /// that are neither finished nor abandoned.
    private void refuseTooManySetups(String username) {
        if (setups.countByUsernameAndCompletedFalseAndCreatedAtGreaterThan(username,
                System.currentTimeMillis() - SETUP_MILLIS) >= MAX_OPEN_SETUPS) {
            throw new ResponseStatusException(429,
                    "Too many cards are being added. Finish one, or try again later");
        }
    }

    /// Deletes the setups nobody can use any more: the finished ones, whose
    /// card is saved, and the abandoned ones. Without it every setup ever
    /// started would stay, one row for each tap of "Add card".
    ///
    /// An abandoned setup is kept for twice the time it can be finished in.
    /// One finished just inside the hour is still at the processor when the
    /// hour passes, and its row is what saves the card when the answer comes.
    @Scheduled(fixedDelay = 600000, initialDelay = 60000, lock = "wayline-card-setups")
    @Transactional
    public void sweepSetups() throws IOException {
        setups.deleteByCompletedTrue();
        setups.deleteByCreatedAtLessThan(System.currentTimeMillis() - 2L * SETUP_MILLIS);
    }

    /// Keeps the reference the processor made for a rider on first use, and
    /// returns the one the account has: another request may have kept its own
    /// while this one was asking for it. Null when the account is gone.
    @Transactional
    private String keepCustomer(String username, String reference) {
        // Two first cards started together both find no row. Taking turns on
        // the account lets the second see the first's row and use it, where
        // it would otherwise insert a second and fail on the key. With no
        // profile to hold the account was closed meanwhile, and its clean-up
        // has already run: a row kept now would never be deleted.
        if (profiles.hold(username) == 0) {
            return null;
        }
        PaymentCustomer row = customers.findById(username);
        if (row == null) {
            row = new PaymentCustomer();
            row.setUsername(username);
            row.setReference(reference);
            customers.insert(row);
        }
        return row.getReference();
    }

    @Transactional
    private void keepSetup(String username, String id, String reference) {
        // As in keepCustomer: an account closed while the processor was asked
        // gets no row its clean-up has already missed.
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        // Counted again with the account held: several requests sent together
        // all pass the count made before the processor was asked.
        //
        // The session a refusal here leaves at the processor is not cleaned
        // up, on purpose. Its address is never answered, so nobody can open
        // it; a card entered there anyway could not be saved, because saving
        // claims this row and there is none; and the processor expires an
        // unfinished session by itself. Only the requests already past the
        // first count when the limit was reached can leave one, so a loop
        // of them still makes no more than a handful.
        refuseTooManySetups(username);
        PaymentSetup row = new PaymentSetup();
        row.setId(id);
        row.setUsername(username);
        row.setReference(reference);
        row.setCreatedAt(System.currentTimeMillis());
        row.setCompleted(false);
        setups.insert(row);
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
        try {
            return keepCard(username, setupId, Ids.next(), card);
        } catch (ResponseStatusException refused) {
            // The processor attached the card when the setup finished. Refused
            // here by the card limit -- two setups finishing together for the
            // last slot, or a slot filled meanwhile -- it has no row, so the
            // rider has nothing to remove it through: it is detached here
            // instead. keepCard has rolled back by now, so this runs outside
            // its transaction and nothing undoes it.
            //
            // Undone after the fact rather than a slot reserved before the
            // processor is asked: a reservation is one more row with a life of
            // its own, given back when the processor declines, times out or
            // the server stops between the two, and swept when nothing gives
            // it back. A refusal is the rare case, and this costs only it.
            //
            // Only the limit's 409. The 404 is a setup claimed by a request
            // sent twice, whose other copy may have saved this very card.
            if (refused.getStatus() == 409) {
                discard(username, references[0], card);
            }
            throw refused;
        }
    }

    /// Detaches a card the processor attached that this server did not keep.
    /// A failure is logged and not answered: the rider's answer is the refusal
    /// that brought us here, and the card is no worse off than before.
    private void discard(String username, String customer, PaymentProvider.Card card) {
        try {
            provider.detachCard(customer, card.token);
        } catch (IOException failed) {
            System.err.println("a card refused for " + username + " is still attached at the "
                    + "processor, as " + card.token + ": " + failed.getMessage());
        }
    }

    /// What the processor calls the rider and a setup of theirs that is still
    /// open, in that order. Answers 404 for a setup that is not.
    @Transactional(readOnly = true)
    private String[] openSetup(String username, String setupId) {
        PaymentSetup setup = setupId == null || setupId.length() == 0 || setupId.length() > 32
                ? null : setups.findFirstByIdAndUsername(setupId, username);
        if (setup == null || setup.isCompleted()
                || setup.getCreatedAt() < System.currentTimeMillis() - SETUP_MILLIS) {
            throw new ResponseStatusException(404, "No such card setup");
        }
        PaymentCustomer customer = customers.findById(username);
        return new String[] {customer == null ? "" : customer.getReference(), setup.getReference()};
    }

    /// Closes the setup and saves its card, together or not at all.
    @Transactional
    private PaymentMethodDto keepCard(String username, String setupId, String id,
            PaymentProvider.Card card) {
        // Claimed with a conditional update, so a request sent twice saves the
        // card once.
        if (setups.claim(setupId) != 1) {
            throw new ResponseStatusException(404, "No such card setup");
        }
        // Counted again here, where the card is saved: starting a setup checks
        // the limit too, but several can be started while there is room for
        // one. The account's row at the processor is written first, so two
        // setups finishing together take turns and the second counts the
        // first one's card. Refusing rolls the claim back with everything else.
        customers.hold(username);
        if (cards.countByUsername(username) >= MAX_CARDS) {
            throw new ResponseStatusException(409, "Remove a card before adding another");
        }
        boolean first = cards.findFirstByUsernameAndPreferredTrue(username) == null;
        // Cards are listed in the order they were added, and two added in the
        // same millisecond would have no order: the later is stamped after
        // the one before it.
        Long newest = cards.newestAt(username);
        long after = newest == null ? 0L : newest.longValue() + 1L;
        PaymentMethod row = new PaymentMethod();
        row.setId(id);
        row.setUsername(username);
        row.setBrand(cut(card.brand, 24));
        row.setLast4(cut(card.last4, 4));
        row.setExpMonth(card.expMonth);
        row.setExpYear(card.expYear);
        row.setToken(card.token);
        row.setPreferred(first);
        row.setCreatedAt(Math.max(System.currentTimeMillis(), after));
        cards.insert(row);
        return card(row);
    }

    /// Makes a method the one rides are paid with unless they say otherwise.
    @Transactional
    public PaymentMethodDto makeDefault(String username, String id) throws IOException {
        // One at a time for an account: two choices made together would each
        // clear the cards it had read and mark its own, and leave two marked.
        profiles.hold(username);
        PaymentMethod chosen = CASH.equals(id) ? null : owned(username, id);
        // The cards are few, and each is written only if its mark changed.
        List<PaymentMethod> rows = cards.findByUsernameOrderByCreatedAtAscIdAsc(username);
        for (int iter = 0; iter < rows.size(); iter++) {
            PaymentMethod row = rows.get(iter);
            row.setPreferred(chosen != null && row.getId().equals(chosen.getId()));
        }
        if (chosen == null) {
            return cash(true);
        }
        chosen.setPreferred(true);
        return card(chosen);
    }

    /// Forgets a card, here and at the processor. Cash cannot be removed, and
    /// neither can the card a ride under way is to be paid with.
    ///
    /// The card is detached at the processor before its row is deleted, and
    /// inside the transaction that deletes it, as [#forget] deletes a
    /// customer: the token is the only way back to the card, so a processor
    /// that cannot be reached fails the removal and the card stays, to be
    /// removed again. Detaching a card that is already detached succeeds, so
    /// a removal whose commit failed after the processor answered finishes
    /// the second time. The account's row is held across the request, which
    /// holds back only that rider's own ride requests, and removing a card is
    /// rare enough for that.
    @Transactional
    public PaymentMethodDto remove(String username, String id) throws IOException {
        if (CASH.equals(id)) {
            throw new ResponseStatusException(400, "Cash is always available");
        }
        // Asking for a ride holds the same row before it picks its card. So a
        // ride and a removal sent together take turns: either the ride is
        // there to be seen below, or it no longer finds the card.
        profiles.hold(username);
        PaymentMethod row = owned(username, id);
        // What is answered is the card as it was, no longer anyone's default.
        PaymentMethodDto dto = card(row, false);
        if (rides.existsByRiderAndMethodIdAndStateIn(username, id, UNDER_WAY)) {
            throw new ResponseStatusException(409, "That card is paying for a ride under way");
        }
        // A ride that ended with nobody able to say whether its card was
        // charged is asked about again, by the card's token. With the card
        // gone the question could not be put: the ride would be written down
        // as unpaid, whether or not the first attempt took the money.
        if (awaitsCharge(username, id, retriedSince())) {
            throw new ResponseStatusException(409,
                    "That card is still paying for a ride. Try again in a few minutes");
        }
        PaymentCustomer customer = customers.findById(username);
        if (customer != null && customer.getReference().length() > 0) {
            try {
                provider.detachCard(customer.getReference(), row.getToken());
            } catch (IOException failed) {
                // A runtime exception, so the transaction rolls back with it.
                throw unavailable("card removal", failed);
            }
        }
        cards.deleteById(row.getId());
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
        PaymentMethod row = card(username, id);
        if (row == null) {
            throw new ResponseStatusException(400, "Choose a way to pay");
        }
        return new String[] {id, label(row)};
    }

    /// Everything of a user's that this class keeps, for an account being
    /// closed, here and at the processor. Answers 409 while a ride of theirs
    /// is still to be charged; see [#remove].
    ///
    /// The processor is asked last, and inside the transaction that deletes
    /// the rest. It holds the rider's address, their name and their cards, and
    /// the reference deleted here is the only way back to them: forgotten
    /// first, a request that then failed would leave that record with nothing
    /// pointing at it. So a processor that cannot be reached fails the closure
    /// whole and the account stays as it was, to be closed again. The other
    /// way round -- the processor has deleted and the commit then fails -- is
    /// mended by the same second attempt, since a customer already gone counts
    /// as deleted. A closure is rare enough to wait on a request with a row
    /// held, which nothing else in this class does.
    @Transactional(rollbackFor = IOException.class)
    public void forget(String username) throws IOException {
        // Only charges still being retried hold a closure up. One left
        // pending past the retry window has already been handed to a person
        // to settle against the processor's own records, which keep the
        // charge and the card used after the customer is deleted here.
        // Counting it would refuse the closure for good over something the
        // rider can do nothing about.
        if (awaitsCharge(username, null, retriedSince())) {
            throw new ResponseStatusException(409,
                    "A ride is still being paid for. Try again in a few minutes");
        }
        PaymentCustomer customer = customers.findById(username);
        String reference = customer == null ? "" : customer.getReference();
        cards.deleteByUsername(username);
        setups.deleteByUsername(username);
        customers.deleteByUsername(username);
        if (reference != null && reference.length() > 0) {
            provider.forgetCustomer(reference);
        }
    }

    private static long retriedSince() {
        return System.currentTimeMillis() - RETRY_MILLIS;
    }

    // --------------------------------------------------------------- charges

    /// Settles a ride that has just completed and returns [#PAID], [#FAILED],
    /// or [#PENDING] when nobody can say yet.
    /// A ride is settled once: its payment is keyed by the ride, and a ride
    /// completes once. Called with no transaction open, since the card is
    /// charged here; what came of the charge is then written in one.
    ///
    /// Safe to ask for again, which is how a ride whose settlement was cut
    /// short is finished later: the charge carries the ride as its key, so
    /// the processor answers a second request with the first one's charge, and
    /// a payment already written is the answer without asking anyone.
    ///
    /// Only a card the processor refused is a payment that failed. Any other
    /// failure -- the processor not reached, its answer lost on the way back --
    /// says nothing about whether the card was charged, so nothing is written
    /// and the ride stays pending for `Rides.settleLeftBehind` to ask again
    /// under the same key. Written down as failed, a charge that did go through
    /// would never be looked for, and the rider would have paid for a ride
    /// that says they did not.
    public String settle(Ride ride) throws IOException {
        Payment kept = payment(ride.getId());
        if (kept != null) {
            return kept.getStatus();
        }
        long amount = ride.getFareCents() + ride.getFeeCents();
        String status = PAID;
        String reference = CASH;
        if (!CASH.equals(ride.getMethodId())) {
            try {
                reference = charge("ride-" + ride.getId(), ride.getRider(), ride.getMethodId(), amount,
                        ride.getCurrency(), "Wayline ride");
            } catch (PaymentDeclinedException declined) {
                // The ride and the amount, never the card.
                System.err.println("payment for ride " + ride.getId() + " failed: "
                        + declined.getMessage());
                status = FAILED;
                reference = "";
            } catch (IOException unknown) {
                System.err.println("payment for ride " + ride.getId() + " is not known yet: "
                        + unknown.getMessage());
                return PENDING;
            }
        }
        keepPayment(ride.getId(), amount, ride.getCurrency(), status, reference);
        return status;
    }

    @Transactional
    private void keepPayment(String rideId, long amount, String currency, String status,
            String reference) {
        if (payments.findById(rideId) != null) {
            // Written while this request was at the processor.
            return;
        }
        Payment row = new Payment();
        row.setRideId(rideId);
        row.setAmountCents(amount);
        row.setCurrency(currency);
        row.setStatus(status);
        row.setProviderRef(reference);
        row.setCreatedAt(System.currentTimeMillis());
        payments.insert(row);
    }

    /// Tips the driver of a completed ride, once. Charged to the method the
    /// ride was paid with; a cash ride's tip is recorded as given in cash.
    ///
    /// The tip is taken off the ride again only when the card was refused. A
    /// charge that failed any other way may have gone through, so the tip
    /// stays on the ride with no charge recorded for it: the rider is told to
    /// try again, and trying again with the same amount asks the processor
    /// under the same key, which answers with the first charge if there was
    /// one. Until then the ride cannot be refunded -- see [#claimRefund].
    public ReceiptDto tip(String rider, String rideId, long amountCents) throws IOException {
        Ride ride = claimTip(rider, rideId, amountCents);
        if (!CASH.equals(ride.getMethodId())) {
            String reference;
            try {
                reference = charge("tip-" + rideId, rider, ride.getMethodId(), amountCents,
                        ride.getCurrency(), "Wayline tip");
            } catch (PaymentDeclinedException declined) {
                releaseTip(rideId);
                throw new ResponseStatusException(402, declined.getMessage());
            } catch (IOException unknown) {
                throw unavailable("tip", unknown);
            }
            keepTip(rideId, reference);
        }
        // The driver's earnings changed; their app reads them again.
        live.rideChanged(rideId, ride.getState(), rider, ride.getDriver(), "");
        return receipt(rider, false, rideId);
    }

    /// Sets the tip on the rider's ride and returns the ride as it was just
    /// before. Committed before the card is charged.
    @Transactional
    private Ride claimTip(String rider, String rideId, long amountCents) {
        Ride ride = ride(rideId);
        if (ride == null || !rider.equals(ride.getRider())) {
            throw new ResponseStatusException(404, "No such ride");
        }
        if (amountCents < 1L || amountCents > MAX_TIP_CENTS) {
            throw new ResponseStatusException(400, "That is not an amount for a tip");
        }
        // Not for a driver who has closed the account: the tip would be
        // credited to a name nobody can sign in as or cash out. A closure can
        // still land while this tip's charge is at the processor, since the
        // balance counts a tip only once its charge is recorded; the driver
        // would have to close in that second or two, just after cashing out.
        // Holding closures until every tip in flight settles would need a
        // state of its own, and one that never settles would hold the
        // account open for good.
        String driver = ride.getDriver();
        if (driver == null || driver.length() == 0 || profiles.findById(driver) == null) {
            throw new ResponseStatusException(409, "That driver can no longer be tipped");
        }
        // Claimed before the charge: of two requests, one changes the row and
        // only that one charges.
        if (rides.claimTip(rideId, rider, amountCents) != 1 && !tipLeftUnknown(ride, amountCents)) {
            throw new ResponseStatusException(409, "That ride cannot be tipped now");
        }
        return ride;
    }

    /// Whether the ride already carries this very tip with no charge recorded
    /// for it: one whose charge was asked for and never answered. Asking again
    /// is safe only for the same amount, the key being the ride's.
    private boolean tipLeftUnknown(Ride ride, long amountCents) {
        if (ride.getTipCents() != amountCents || CASH.equals(ride.getMethodId())
                || !PAID.equals(ride.getPaymentStatus())) {
            return false;
        }
        Payment payment = payments.findById(ride.getId());
        return payment != null && (payment.getTipRef() == null || payment.getTipRef().length() == 0);
    }

    @Transactional
    private void releaseTip(String rideId) {
        rides.releaseTip(rideId);
    }

    @Transactional
    private void keepTip(String rideId, String reference) {
        Payment payment = payments.findById(rideId);
        if (payment != null) {
            payment.setTipRef(reference);
        }
    }

    /// Returns what a ride cost to the card that paid for it, tip included.
    /// For an admin. A cash ride has nothing to return, and a ride is refunded
    /// once.
    public ReceiptDto refund(String rideId, String reason) throws IOException {
        Ride ride = claimRefund(rideId);
        Payment payment = payment(rideId);
        String why = cut(reason == null ? "" : reason.trim(), 255);
        PaymentProvider.Refund fare;
        PaymentProvider.Refund tip = null;
        try {
            fare = provider.refund(payment.getProviderRef(), payment.getAmountCents());
        } catch (IOException failed) {
            releaseRefund(rideId);
            throw unavailable("refund", failed);
        }
        if (payment.getTipRef().length() > 0) {
            try {
                tip = provider.refund(payment.getTipRef(), ride.getTipCents());
            } catch (IOException failed) {
                // The fare is back with the rider already. Paid again, the
                // ride would show that money as paid and its driver could cash
                // it out; so the fare's refund is kept and the payment marked
                // failed, which is what it is until a person with the
                // processor's records settles the tip -- see settleRefunds.
                keepPartialRefund(rideId, fare, why);
                throw unavailable("tip refund", failed);
            }
        }
        keepRefund(rideId, fare, tip, why);
        live.rideChanged(rideId, ride.getState(), ride.getRider(), ride.getDriver(), "");
        return receipt("", true, rideId);
    }

    /// Asks the processor how each refund it took and had not finished came
    /// out, and records the ones that have an answer.
    ///
    /// A processor can take a refund and only say later whether the bank
    /// returned the money; Stripe answers such a refund `pending`, and it can
    /// still end `failed` or `canceled`. Recorded as refunded at once, a
    /// refund that then failed would show the rider and the admin money that
    /// never came back, and nothing would look at it again. So it is kept as
    /// [#REFUND_PENDING] and asked about here until it ends: [#REFUNDED] once
    /// the fare's refund and the tip's have both succeeded, [#REFUND_FAILED]
    /// as soon as either has not. A failed refund is not tried again from
    /// here. Part of the money may have gone back, and the processor may
    /// answer a repeat with the refund that failed, so what to do about it is
    /// for a person with the processor's records.
    ///
    /// Not a transaction itself, as `Rides.settleLeftBehind` is not: the
    /// processor is asked with none open, each refund is recorded in one of
    /// its own, and what went wrong with the first refund that could not be
    /// asked about is reported once the rest have been.
    @Scheduled(fixedDelay = 60000, initialDelay = 45000, lock = "wayline-refunds")
    public void settleRefunds() throws IOException {
        List<Payment> pending = refundsPending();
        Exception failed = null;
        for (int iter = 0; iter < pending.size(); iter++) {
            Payment payment = pending.get(iter);
            try {
                String outcome = refundOutcome(payment);
                Ride ride = outcome == null ? null : endRefund(payment.getRideId(), outcome);
                if (ride != null) {
                    live.rideChanged(ride.getId(), ride.getState(), ride.getRider(), ride.getDriver(), "");
                }
            } catch (IOException err) {
                failed = failed == null ? err : failed;
            } catch (RuntimeException err) {
                failed = failed == null ? err : failed;
            }
        }
        if (failed instanceof IOException) {
            throw (IOException) failed;
        }
        if (failed instanceof RuntimeException) {
            throw (RuntimeException) failed;
        }
    }

    /// How a pending refund came out: [#REFUNDED], [#REFUND_FAILED], or null
    /// while the processor still does not know.
    private String refundOutcome(Payment payment) throws IOException {
        String fare = provider.refundStatus(payment.getRefundRef());
        String tip = payment.getTipRefundRef().length() == 0 ? PaymentProvider.REFUND_SUCCEEDED
                : provider.refundStatus(payment.getTipRefundRef());
        if (PaymentProvider.REFUND_FAILED.equals(fare)
                || PaymentProvider.REFUND_FAILED.equals(tip)) {
            return REFUND_FAILED;
        }
        if (PaymentProvider.REFUND_SUCCEEDED.equals(fare)
                && PaymentProvider.REFUND_SUCCEEDED.equals(tip)) {
            return REFUNDED;
        }
        return null;
    }

    @Transactional(readOnly = true)
    private List<Payment> refundsPending() {
        return payments.findByStatusOrderByCreatedAtAsc(REFUND_PENDING, new PageRequest(0, 50));
    }

    /// Records how a refund ended and returns its ride, or null when another
    /// pass recorded it first.
    @Transactional
    private Ride endRefund(String rideId, String outcome) {
        if (payments.endRefund(rideId, outcome,
                REFUNDED.equals(outcome) ? System.currentTimeMillis() : 0L) != 1) {
            return null;
        }
        rides.paymentStatus(rideId, outcome);
        return ride(rideId);
    }

    /// Marks the ride refunded and returns it as it was just before. Committed
    /// before the processor is asked for the money: of two requests, one
    /// changes the row and only that one refunds.
    ///
    /// The update comes first and the ride is read after it. A tip is claimed
    /// by an update of the same row, so the two take turns, and the ride read
    /// here says whether a tip got in before this did. One that did and has no
    /// charge recorded yet is still at the processor: refunding now would
    /// return the fare, miss the tip, and leave a tip charged to a refunded
    /// ride. The refund is refused until the tip has settled either way, and
    /// a tip asked for after this commits finds the ride no longer paid.
    ///
    /// A tip whose charge never reports back keeps its ride from being
    /// refunded here. That is deliberate: nobody knows whether that card was
    /// charged, and it takes a person and the processor's records to say.
    @Transactional
    private Ride claimRefund(String rideId) {
        boolean claimed = rides.claimRefund(rideId) == 1;
        Ride ride = ride(rideId);
        if (ride == null) {
            throw new ResponseStatusException(404, "No such ride");
        }
        if (CASH.equals(ride.getMethodId())) {
            throw new ResponseStatusException(409, "A ride paid in cash has nothing to refund");
        }
        if (REFUND_PENDING.equals(ride.getPaymentStatus())) {
            throw new ResponseStatusException(409,
                    "That ride's refund is still with the payment processor");
        }
        if (REFUND_FAILED.equals(ride.getPaymentStatus())) {
            throw new ResponseStatusException(409,
                    "That ride's refund failed. Check it with the payment processor");
        }
        if (!claimed && !refundUnfinished(ride)) {
            throw new ResponseStatusException(409, "That ride has no payment to refund");
        }
        if (ride.getTipCents() > 0L) {
            Payment payment = payments.findById(rideId);
            if (payment == null || payment.getTipRef() == null || payment.getTipRef().length() == 0) {
                throw new ResponseStatusException(409,
                        "A tip on that ride is still being charged. Try again in a moment");
            }
        }
        return ride;
    }

    /// Whether `ride` was marked refunded by a request that never recorded
    /// the refund: the server stopped between asking the processor and
    /// writing the answer down, so neither [#keepRefund] nor [#releaseRefund]
    /// ran. Such a ride would otherwise be refunded in name only and refused
    /// for good. It is refunded again instead, which is safe because a refund
    /// is keyed by the charge it returns: the processor answers a repeat with
    /// the refund it already made, and makes one if it never did.
    ///
    /// It cannot tell such a ride from one whose refund is still at the
    /// processor, so it also lets a second request through while the first
    /// is under way. That is safe for the same reason, and [#releaseRefund]
    /// and [#keepRefund] keep the ride agreeing with the payment however the
    /// two end.
    private boolean refundUnfinished(Ride ride) {
        if (!REFUNDED.equals(ride.getPaymentStatus())) {
            return false;
        }
        Payment payment = payments.findById(ride.getId());
        return payment != null && !REFUNDED.equals(payment.getStatus());
    }

    @Transactional(readOnly = true)
    private Payment payment(String rideId) {
        return payments.findById(rideId);
    }

    /// Gives back the claim of a refund the processor did not take: the ride
    /// is paid again -- unless another request has recorded a refund of it
    /// meanwhile.
    ///
    /// Two refunds can overlap: the second is let through by
    /// [#refundUnfinished] while the first is at the processor, which is
    /// what lets a refund cut short be asked for again. Should one of them
    /// record a refund and the other then fail, putting the ride back to paid
    /// would leave a refunded payment on a ride its driver is paid for, and
    /// the fare would be cashed out after it went back to the rider. So the
    /// payment decides: the ride is paid again only while its payment is.
    ///
    /// The ride's row is written first, as in [#keepRefund], so the two take
    /// turns on it: whichever comes second sees what the first wrote.
    @Transactional
    private void releaseRefund(String rideId) {
        if (rides.releaseRefund(rideId) != 1) {
            // No longer marked refunded: a refund was recorded as pending.
            return;
        }
        Payment payment = payments.findById(rideId);
        if (payment != null && !PAID.equals(payment.getStatus())) {
            rides.paymentStatus(rideId, payment.getStatus());
        }
    }

    /// Records the refunds the processor took. Refunded when both are known
    /// to have returned the money; otherwise the payment and its ride are
    /// [#REFUND_PENDING], for [#settleRefunds] to finish.
    ///
    /// The ride is written as well, and first, although its claim already
    /// marked it refunded: an overlapping request that failed may have put it
    /// back to paid in the meantime. See [#releaseRefund].
    @Transactional
    private void keepRefund(String rideId, PaymentProvider.Refund fare,
            PaymentProvider.Refund tip, String reason) {
        boolean pending = fare.pending || (tip != null && tip.pending);
        rides.paymentStatus(rideId, pending ? REFUND_PENDING : REFUNDED);
        Payment payment = payments.findById(rideId);
        payment.setStatus(pending ? REFUND_PENDING : REFUNDED);
        payment.setRefundRef(cut(fare.reference, 64));
        payment.setTipRefundRef(tip == null ? "" : cut(tip.reference, 64));
        payment.setRefundReason(reason);
        payment.setRefundedAt(pending ? 0L : System.currentTimeMillis());
    }

    /// Records a refund of the fare whose tip could not be refunded: the ride
    /// and its payment are [#REFUND_FAILED], with the fare's refund kept.
    @Transactional
    private void keepPartialRefund(String rideId, PaymentProvider.Refund fare, String reason) {
        rides.paymentStatus(rideId, REFUND_FAILED);
        Payment payment = payments.findById(rideId);
        payment.setStatus(REFUND_FAILED);
        payment.setRefundRef(cut(fare.reference, 64));
        payment.setTipRefundRef("");
        payment.setRefundReason(reason);
        payment.setRefundedAt(0L);
    }

    // -------------------------------------------------------------- receipts

    /// A rider's receipts, newest first: one for every ride that completed.
    @Transactional(readOnly = true)
    public List<ReceiptDto> receipts(String rider) throws IOException {
        return receipts(rides.findByRiderAndStateOrderByRequestedAtDesc(rider, RideState.COMPLETED,
                new PageRequest(0, 50)), false);
    }

    /// Every receipt, newest first, for the admin.
    @Transactional(readOnly = true)
    public List<ReceiptDto> allReceipts() throws IOException {
        return receipts(rides.findByStateOrderByRequestedAtDesc(RideState.COMPLETED,
                new PageRequest(0, 200)), true);
    }

    /// The receipt of one ride: what it will cost while it is under way, what
    /// it cost once it completed. Answers 404 for a ride that is not the
    /// viewer's, and for one that ended without completing, which cost nothing.
    @Transactional(readOnly = true)
    public ReceiptDto receipt(String viewer, boolean admin, String rideId) throws IOException {
        Ride ride = ride(rideId);
        RideState state = ride == null ? RideState.NONE : ride.getState();
        if (ride == null || !(admin || viewer.equals(ride.getRider()))
                || RideState.CANCELLED_BY_RIDER == state || RideState.CANCELLED_BY_DRIVER == state
                || RideState.CANCELLED_BY_ADMIN == state || RideState.NO_DRIVERS == state) {
            throw new ResponseStatusException(404, "No such receipt");
        }
        Payment payment = payments.findById(rideId);
        return receipt(ride, payment == null ? 0L : payment.getCreatedAt(), admin,
                new HashMap<String, Profile>());
    }

    /// The rides' receipts. When each was paid is on its payment and not on
    /// the ride, so the payments of all of them are read at once and matched
    /// to their rides here.
    private List<ReceiptDto> receipts(List<Ride> completed, boolean admin) {
        Map<String, Long> paidAt = new HashMap<String, Long>();
        if (completed.size() > 0) {
            List<String> ids = new ArrayList<String>();
            for (int iter = 0; iter < completed.size(); iter++) {
                ids.add(completed.get(iter).getId());
            }
            List<Payment> rows = payments.findByRideIdIn(ids);
            for (int iter = 0; iter < rows.size(); iter++) {
                Payment payment = rows.get(iter);
                paidAt.put(payment.getRideId(), Long.valueOf(payment.getCreatedAt()));
            }
        }
        Map<String, Profile> drivers = new HashMap<String, Profile>();
        List<ReceiptDto> out = new ArrayList<ReceiptDto>();
        for (int iter = 0; iter < completed.size(); iter++) {
            Ride ride = completed.get(iter);
            Long at = paidAt.get(ride.getId());
            out.add(receipt(ride, at == null ? 0L : at.longValue(), admin, drivers));
        }
        return out;
    }

    private ReceiptDto receipt(Ride ride, long paidAt, boolean admin,
            Map<String, Profile> drivers) {
        long fare = ride.getFareCents();
        long distanceFare = ride.getDistanceCents();
        long timeFare = ride.getTimeCents();
        // Whatever of the fare the other two parts do not account for, which
        // for a ride asked for before the parts were kept is all of it.
        long baseFare = fare - distanceFare - timeFare;
        long serviceFee = ride.getFeeCents();
        long tip = ride.getTipCents();
        long discount = 0L;
        String paid = ride.getPaymentStatus();
        boolean cash = CASH.equals(ride.getMethodId());
        String status;
        if (PAID.equals(paid)) {
            status = cash ? "cash" : "paid";
        } else if (FAILED.equals(paid)) {
            status = "failed";
        } else if (REFUNDED.equals(paid)) {
            status = "refunded";
        } else if (REFUND_PENDING.equals(paid)) {
            status = "refund_pending";
        } else if (REFUND_FAILED.equals(paid)) {
            status = "refund_failed";
        } else {
            status = "pending";
        }
        // Every refund state follows a payment that was made.
        long paidOn = PAID.equals(paid) || (paid != null && paid.startsWith("REFUND"))
                ? paidAt : 0L;
        String driverName = "";
        String vehicle = "";
        String driver = ride.getDriver();
        if (driver.length() > 0 && RideState.OFFERED != ride.getState()) {
            Profile who = drivers.get(driver);
            if (who == null && !drivers.containsKey(driver)) {
                who = profiles.findById(driver);
                drivers.put(driver, who);
            }
            if (who != null) {
                driverName = who.getDisplayName();
                vehicle = who.getVehicle();
            }
        }
        return new ReceiptDto(ride.getId(), ride.getCurrency(), baseFare, distanceFare,
                timeFare, serviceFee, tip, discount, fare + serviceFee + tip - discount,
                ride.getMethodLabel(), status, paidOn, ride.getPickupAddress(),
                ride.getDropoffAddress(), ride.getDistanceMeters(), ride.getDurationSeconds(),
                driverName, vehicle, admin ? ride.getRider() : "");
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
        PaymentMethod method = card(rider, methodId);
        PaymentCustomer customer = customers.findById(rider);
        if (method == null || customer == null) {
            return null;
        }
        return new String[] {customer.getReference(), method.getToken()};
    }

    private PaymentMethod owned(String username, String id) {
        PaymentMethod row = card(username, id);
        if (row == null) {
            // Someone else's card is told apart from no card by nobody.
            throw new ResponseStatusException(404, "No such payment method");
        }
        return row;
    }

    private static PaymentMethodDto card(PaymentMethod row) {
        return card(row, row.isPreferred());
    }

    private static PaymentMethodDto card(PaymentMethod row, boolean isDefault) {
        return new PaymentMethodDto(row.getId(), "card", row.getBrand(), row.getLast4(),
                row.getExpMonth(), row.getExpYear(), isDefault, label(row));
    }

    private static PaymentMethodDto cash(boolean isDefault) {
        return new PaymentMethodDto(CASH, "cash", "", "", 0, 0, isDefault, CASH_LABEL);
    }

    private static String label(PaymentMethod method) {
        return method.getBrand() + " " + method.getLast4();
    }

    private static String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }

    /// Deletes a customer record this server made and did not keep. A failure
    /// is logged rather than answered: the request has its own answer, and
    /// the record holds no card.
    private void forgetQuietly(String reference) {
        try {
            provider.forgetCustomer(reference);
        } catch (IOException failed) {
            System.err.println("could not delete an unused payment customer: "
                    + failed.getMessage());
        }
    }

    /// A fault at the processor, as the app is told of it. What went wrong goes
    /// to the log; it describes the request, never a card or a key.
    private static ResponseStatusException unavailable(String what, IOException failed) {
        System.err.println("payment provider failed during " + what + ": " + failed.getMessage());
        return new ResponseStatusException(502, "Payments are unavailable right now");
    }

    /// A ride, or null when there is none. An id that cannot be one is not
    /// looked for.
    private Ride ride(String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return rides.findById(id);
    }

    /// One of an account's cards, or null when the id is not a card of theirs.
    private PaymentMethod card(String username, String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return cards.findFirstByIdAndUsername(id, username);
    }

    /// Whether a completed ride of the rider's is still to be charged to this
    /// method -- or to any card, for a null `methodId` -- and will be asked
    /// about again: its payment is pending and it ended after `since`.
    ///
    /// A card tip left unknown counts too, with no time limit: see [#tip].
    /// Its retry charges the ride's card under the same key, so with the card
    /// gone a tip that may have been charged would be cleared as refused.
    private boolean awaitsCharge(String rider, String methodId, long since) {
        List<Ride> tipped = rides.findByRiderAndPaymentStatusAndTipCentsGreaterThan(rider, PAID,
                0L);
        for (Ride ride : tipped) {
            if ((methodId == null || methodId.equals(ride.getMethodId()))
                    && tipLeftUnknown(ride, ride.getTipCents())) {
                return true;
            }
        }
        if (methodId == null) {
            return rides.existsByRiderAndStateAndPaymentStatusAndUpdatedAtGreaterThanAndMethodIdNot(
                    rider, RideState.COMPLETED, PENDING, since, CASH);
        }
        return rides.existsByRiderAndStateAndPaymentStatusAndUpdatedAtGreaterThanAndMethodId(
                rider, RideState.COMPLETED, PENDING, since, methodId);
    }
}
