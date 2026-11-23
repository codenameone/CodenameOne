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
package com.codenameone.examples.bazaar.pay;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CardDto;
import com.codenameone.examples.bazaar.api.PaymentConfigDto;
import com.codenameone.examples.bazaar.api.PaymentMethodDto;
import com.codenameone.examples.bazaar.api.PaymentSetupDto;
import com.codenameone.examples.bazaar.api.ReceiptDto;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.domain.PaymentCustomer;
import com.codenameone.examples.bazaar.domain.PaymentMethod;
import com.codenameone.examples.bazaar.domain.PaymentSetup;
import com.codenameone.examples.bazaar.repository.PaymentCustomerRepository;
import com.codenameone.examples.bazaar.repository.PaymentMethodRepository;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.PaymentSetupRepository;
import com.codenameone.examples.bazaar.repository.ProfileRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The ways a customer can pay, and the money for each order.
///
/// An order's total is held on the card at checkout ([#authorize]), taken when
/// the order ships or is handed over ([#capture]), let go when it is cancelled
/// ([#voidOrder]) and given back, in part or whole, by [#refund]. A payment is
/// keyed by the order's id alone; this class knows nothing else of orders.
/// An order paid in cash is recorded and nothing is charged.
///
/// Everything here that takes a `username` acts on that user's own methods and
/// payments and no one else's; the endpoints pass the signed-in user.
///
/// The processor is somewhere else, and a call to it can take seconds, so none
/// is made with a transaction open. Whatever talks to it is three steps: a
/// transaction that claims the payment (a conditional status change, so two
/// callers never both act), the call, and a transaction that records what came
/// of it, or gives the claim back.
@Component
public class Payments {
    /// Money held on the card and not yet taken.
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String PAID = "PAID";
    /// A hold that was let go: the order was cancelled before it was charged.
    public static final String VOIDED = "VOIDED";
    public static final String FAILED = "FAILED";
    public static final String PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
    public static final String REFUNDED = "REFUNDED";
    /// Refunded at the processor, which has not yet said that the money is
    /// back on the card. See [#settleRefunds].
    public static final String REFUND_PENDING = "REFUND_PENDING";
    /// A refund the processor took and then failed or canceled.
    public static final String REFUND_FAILED = "REFUND_FAILED";
    private static final String CAPTURING = "CAPTURING";
    private static final String VOIDING = "VOIDING";
    private static final String REFUNDING = "REFUNDING";

    /// How long a refund may stay claimed before [#resumeRefunds] finishes it.
    static final long STALE_REFUND_MILLIS = 10L * 60L * 1000L;
    /// The id and the label of the method every account has.
    public static final String CASH = "cash";
    public static final String CASH_LABEL = "Cash";
    private static final int MAX_CARDS = 8;
    /// A setup not finished within this long is abandoned.
    private static final long SETUP_MILLIS = 3600000L;
    /// How many setups an account may have open at once.
    private static final int MAX_OPEN_SETUPS = 5;

    private final PaymentMethodRepository cards;
    private final PaymentSetupRepository setups;
    private final PaymentCustomerRepository customers;
    private final PaymentRepository payments;
    private final ProfileRepository profiles;
    private final PaymentProvider provider;

    public Payments(PaymentMethodRepository cards, PaymentSetupRepository setups,
            PaymentCustomerRepository customers, PaymentRepository payments,
            ProfileRepository profiles, PaymentProvider provider) {
        this.cards = cards;
        this.setups = setups;
        this.customers = customers;
        this.payments = payments;
        this.profiles = profiles;
        this.provider = provider;
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
                    // stay at the processor with the customer's name on it.
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

    /// The customer's reference at the processor, or null when they have none
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
                Instant.now().minusMillis(SETUP_MILLIS)) >= MAX_OPEN_SETUPS) {
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
    @Scheduled(fixedDelay = 600000, initialDelay = 60000, lock = "bazaar-card-setups")
    @Transactional
    public void sweepSetups() throws IOException {
        setups.deleteByCompletedTrue();
        setups.deleteByCreatedAtLessThan(Instant.now().minusMillis(2L * SETUP_MILLIS));
    }

    /// Keeps the reference the processor made for a customer on first use, and
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
        row.setCreatedAt(Instant.now());
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
            // customer has nothing to remove it through: it is detached here
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
    /// A failure is logged and not answered: the customer's answer is the refusal
    /// that brought us here, and the card is no worse off than before.
    private void discard(String username, String customer, PaymentProvider.Card card) {
        try {
            provider.detachCard(customer, card.token);
        } catch (IOException failed) {
            System.err.println("a card refused for " + username + " is still attached at the "
                    + "processor, as " + card.token + ": " + failed.getMessage());
        }
    }

    /// What the processor calls the customer and a setup of theirs that is still
    /// open, in that order. Answers 404 for a setup that is not.
    @Transactional(readOnly = true)
    private String[] openSetup(String username, String setupId) {
        PaymentSetup setup = setupId == null || setupId.length() == 0 || setupId.length() > 32
                ? null : setups.findFirstByIdAndUsername(setupId, username);
        if (setup == null || setup.isCompleted()
                || setup.getCreatedAt().plusMillis(SETUP_MILLIS).isBefore(Instant.now())) {
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
        Instant newest = cards.newestAt(username);
        Instant now = Instant.now();
        Instant after = newest == null ? Instant.EPOCH : newest.plusMillis(1L);
        PaymentMethod row = new PaymentMethod();
        row.setId(id);
        row.setUsername(username);
        row.setBrand(cut(card.brand, 24));
        row.setLast4(cut(card.last4, 4));
        row.setExpMonth(card.expMonth);
        row.setExpYear(card.expYear);
        row.setToken(card.token);
        row.setPreferred(first);
        row.setCreatedAt(after.isAfter(now) ? after : now);
        cards.insert(row);
        return card(row);
    }

    /// Makes a method the one orders are paid with unless they say otherwise.
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
    /// neither can the card an order under way is to be paid with.
    ///
    /// The card is detached at the processor before its row is deleted, and
    /// inside the transaction that deletes it, as [#forget] deletes a
    /// customer: the token is the only way back to the card, so a processor
    /// that cannot be reached fails the removal and the card stays, to be
    /// removed again. Detaching a card that is already detached succeeds, so
    /// a removal whose commit failed after the processor answered finishes
    /// the second time. The account's row is held across the request, which
    /// holds back only that customer's own orders, and removing a card is
    /// rare enough for that.
    @Transactional
    public PaymentMethodDto remove(String username, String id) throws IOException {
        if (CASH.equals(id)) {
            throw new ResponseStatusException(400, "Cash is always available");
        }
        // Placing an order holds the same row before it picks its card. So an
        // order and a removal sent together take turns: either the order is
        // there to be seen below, or it no longer finds the card.
        profiles.hold(username);
        PaymentMethod row = owned(username, id);
        // What is answered is the card as it was, no longer anyone's default.
        PaymentMethodDto dto = card(row, false);
        if (payments.existsByUsernameAndMethodIdAndStatus(username, id, AUTHORIZED)) {
            throw new ResponseStatusException(409, "That card is paying for an order under way");
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

    /// The method an order is paid with: the one named, or the default when
    /// none is. Answers 400 for a method that is not the customer's.
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
    /// closed, here and at the processor. Answers 409 while an order of theirs
    /// still holds money on a card; see [#remove].
    ///
    /// The processor is asked last, and inside the transaction that deletes
    /// the rest. It holds the customer's address, their name and their cards, and
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
        // customer can do nothing about.
        if (payments.existsByUsernameAndStatus(username, AUTHORIZED)) {
            throw new ResponseStatusException(409,
                    "An order is still being paid for. Try again in a few minutes");
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

    // --------------------------------------------------------------- orders

    // tag::bazaar-authorize[]
    /// Holds an order's total on the customer's card as the order is placed,
    /// with no transaction open, and records the payment as [#AUTHORIZED].
    /// Asking again for the same order answers the hold already made.
    ///
    /// @return the hold's reference, or [#CASH] for an order paid in cash
    /// - `ResponseStatusException` 402: when the card was refused
    public String authorize(String orderId, String username, String methodId, long amountCents,
            String currency) throws IOException {
        if (amountCents < 0) {
            throw new ResponseStatusException(400, "That is not an amount");
        }
        Payment kept = payment(orderId);
        if (kept != null) {
            return kept.getProviderRef();
        }
        String[] chosen = choose(username, methodId);
        String reference = CASH;
        if (!CASH.equals(chosen[0])) {
            String[] references = chargeable(username, chosen[0]);
            if (references == null) {
                throw new ResponseStatusException(402, "The card is no longer on the account");
            }
            try {
                reference = cut(provider.authorize(authorizeKey(orderId, references[1], amountCents),
                        references[0], references[1],
                        amountCents, currency, "Bazaar order"), 64);
            } catch (PaymentDeclinedException declined) {
                throw new ResponseStatusException(402, declined.getMessage());
            } catch (IOException unknown) {
                throw unavailable("card check", unknown);
            }
        }
        keepPayment(orderId, username, chosen, amountCents, currency, reference);
        return reference;
    }
    // end::bazaar-authorize[]

    /// A card typed into the web checkout for this one order, checked with
    /// the processor and saved to no account: what a guest pays with. The
    /// number and security code go to the processor in [#checkCard] and no
    /// further; what comes back is its customer, its token, a brand and four
    /// digits.
    public static final class OneOffCard {
        public String customer = "";
        public String token = "";
        public String label = "";
    }

    /// Whether a typed card can be checked here. A processor whose setup is
    /// hosted (Stripe) takes the number on its own page and never from this
    /// server, so there the web checkout offers saved cards only.
    public boolean takesTypedCards() {
        return "simulated".equals(provider.name());
    }

    /// Checks a typed card with the processor before anything is reserved:
    /// a mistyped number, an expired card or a declined one is refused here
    /// with 402 and the reason, and checkout has nothing to undo.
    public OneOffCard checkCard(String payer, CardDto typed) throws IOException {
        if (!takesTypedCards()) {
            throw new ResponseStatusException(402, "Pay with a card saved in the app");
        }
        PaymentProvider.Card card;
        String customer;
        try {
            customer = provider.createCustomer(payer, "");
            PaymentProvider.Setup setup = provider.startCardSetup(customer, Ids.next());
            card = provider.finishCardSetup(customer, setup.reference, typed);
        } catch (PaymentDeclinedException declined) {
            throw new ResponseStatusException(402, declined.getMessage());
        } catch (IOException failed) {
            throw unavailable("card check", failed);
        }
        OneOffCard out = new OneOffCard();
        out.customer = customer;
        out.token = card.token;
        out.label = card.brand + " ending " + card.last4;
        return out;
    }

    /// The idempotency key of one hold: the order, the card and the amount. A
    /// retry after a lost answer with the same card is answered with the first
    /// hold; a retry with another card after a decline is a new attempt, never
    /// the cached decline (or Stripe's idempotency error for changed
    /// parameters) of the first.
    public static String authorizeKey(String orderId, String token, long amountCents) {
        return "order-" + orderId + "-" + Integer.toHexString(token == null ? 0 : token.hashCode())
                + "-" + amountCents;
    }

    /// [#authorize] for a [OneOffCard]: holds the amount on it and records the
    /// payment, keyed by the order as every payment is.
    public String authorizeCard(String orderId, String payer, OneOffCard card, long amountCents,
            String currency) throws IOException {
        Payment kept = payment(orderId);
        if (kept != null) {
            return kept.getProviderRef();
        }
        String reference;
        try {
            reference = cut(provider.authorize(authorizeKey(orderId, card.token, amountCents),
                    card.customer, card.token,
                    amountCents, currency, "Bazaar order"), 64);
        } catch (PaymentDeclinedException declined) {
            throw new ResponseStatusException(402, declined.getMessage());
        } catch (IOException unknown) {
            throw unavailable("card check", unknown);
        }
        keepPayment(orderId, payer, new String[] {"card", card.label}, amountCents, currency,
                reference);
        return reference;
    }

    // ------------------------------------------------- payments in the browser

    /// A payment the shopper is finishing with the processor (a bank debit, a
    /// pay-later check); the processor's webhook ends it in [#PAID] or
    /// [#FAILED]. Nothing is held that [#voidOrder] could let go.
    public static final String PROCESSING = "PROCESSING";

    /// The processor this server pays through.
    public PaymentProvider provider() {
        return provider;
    }

    /// Records the hold an express checkout's wallet already made (an intent
    /// the browser confirmed, checked by the caller) as the order's payment:
    /// from here it is captured, voided and refunded like any card hold.
    public String keepHeld(String orderId, String payer, String reference, long amountCents,
            String currency, String label) {
        Payment kept = payment(orderId);
        if (kept != null) {
            return kept.getProviderRef();
        }
        keepPayment(orderId, payer, new String[] {"wallet", label}, amountCents, currency,
                cut(reference, 64));
        return reference;
    }

    /// Opens a processor intent of `methodType` for an order just written,
    /// and records the payment as [#PROCESSING]: the order waits as payment
    /// pending until the webhook settles it ([#settleIntent]). Keyed by the
    /// order, so asking again opens no second intent.
    public PaymentProvider.Intent startIntent(String orderId, String payer, String methodType,
            long amountCents, String currency) throws IOException {
        PaymentProvider.Intent intent;
        try {
            intent = provider.createIntent("order-" + orderId, amountCents, currency, methodType,
                    false, orderId);
        } catch (PaymentDeclinedException declined) {
            throw new ResponseStatusException(402, declined.getMessage());
        } catch (IOException unknown) {
            throw unavailable("payment", unknown);
        }
        keepPayment(orderId, payer, new String[] {methodType, PaymentTypes.label(methodType)},
                amountCents, currency, cut(intent.reference, 64), PROCESSING);
        return intent;
    }

    /// What the order's intent is waiting for the browser to finish, or null
    /// when the order has no payment in progress: the client secret the pay
    /// page hands Stripe.js, asked of the processor each time and never kept.
    public PaymentProvider.Intent openIntent(String orderId) throws IOException {
        Payment p = payment(orderId);
        if (p == null || !PROCESSING.equals(p.getStatus())) {
            return null;
        }
        try {
            PaymentProvider.Intent intent = provider.intent(p.getProviderRef());
            if (intent.clientSecret.length() == 0) {
                // Stripe answers a retrieve with the secret as well; a
                // processor that does not is asked by the key again.
                intent = provider.createIntent("order-" + orderId, p.getAmountCents(),
                        p.getCurrency(), p.getMethodId(), false, orderId);
            }
            return intent;
        } catch (IOException unknown) {
            throw unavailable("payment", unknown);
        }
    }

    /// Settles an order's [#PROCESSING] payment from what the processor says
    /// of its intent now -- never from what an event or a browser claims.
    /// Answers [#PAID], [#FAILED], [#PROCESSING] while it still waits, or the
    /// payment's status when it was not processing (settled already: a
    /// repeated event changes nothing).
    public String settleIntent(String orderId) throws IOException {
        Payment p = payment(orderId);
        if (p == null) {
            return "";
        }
        if (!PROCESSING.equals(p.getStatus())) {
            return p.getStatus();
        }
        PaymentProvider.Intent intent;
        try {
            intent = provider.intent(p.getProviderRef());
        } catch (IOException unknown) {
            throw unavailable("payment", unknown);
        }
        if (PaymentProvider.INTENT_SUCCEEDED.equals(intent.status)
                || PaymentProvider.INTENT_HELD.equals(intent.status)) {
            if (intent.amountCents != p.getAmountCents()) {
                // Paid, but not what the order asks: never settle on it.
                System.err.println("intent for order " + orderId + " paid " + intent.amountCents
                        + ", the order asks " + p.getAmountCents());
                return PROCESSING;
            }
            if (PaymentProvider.INTENT_HELD.equals(intent.status)) {
                return claim(orderId, PROCESSING, AUTHORIZED) == null ? payment(orderId).getStatus()
                        : AUTHORIZED;
            }
            if (claim(orderId, PROCESSING, CAPTURING) == null) {
                return payment(orderId).getStatus();
            }
            finish(orderId, CAPTURING, PAID);
            return PAID;
        }
        if (PaymentProvider.INTENT_FAILED.equals(intent.status)) {
            return claim(orderId, PROCESSING, FAILED) == null ? payment(orderId).getStatus()
                    : FAILED;
        }
        return PROCESSING;
    }

    /// The prefix of a card payment taken on a register's card terminal. The
    /// terminal settled it with the processor itself, so nothing here asks
    /// the processor about it: capture, void and refund only move the row.
    /// A real terminal integration would carry the processor's own reference
    /// and refund through it.
    public static final String TERMINAL = "TERM-";

    /// Whether a payment's reference is one the processor knows.
    private static boolean atProcessor(String reference) {
        return !CASH.equals(reference) && (reference == null || !reference.startsWith(TERMINAL));
    }

    /// Records a card payment a register's terminal took for a counter sale,
    /// and captures it at once. Answers the reference kept, `TERM-` and the
    /// terminal's own.
    public String terminal(String orderId, String staff, long amountCents, String currency,
            String terminalReference) throws IOException {
        if (amountCents <= 0) {
            throw new ResponseStatusException(400, "That is not an amount");
        }
        String reference = cut(TERMINAL + (terminalReference == null ? "" : terminalReference), 64);
        keepPayment(orderId, staff, new String[] {"terminal", "Card terminal"}, amountCents,
                currency, reference);
        capture(orderId);
        return reference;
    }

    /// Records a card payment a register's paired reader took through the
    /// processor ([CardTerminal]) for a counter sale. The reader captured it
    /// already, so it is kept as PAID under the processor's own reference and
    /// a refund goes back through the processor like an online one.
    public String reader(String orderId, String staff, long amountCents, String currency,
            String intentRef, String cardLabel) throws IOException {
        if (amountCents <= 0 || intentRef == null || intentRef.length() == 0) {
            throw new ResponseStatusException(400, "That is not a card payment");
        }
        keepPayment(orderId, staff, new String[] {"reader", cardLabel == null
                || cardLabel.length() == 0 ? "Card reader" : cardLabel}, amountCents, currency,
                cut(intentRef, 64), PAID);
        return intentRef;
    }

    private void keepPayment(String orderId, String username, String[] method, long amount,
            String currency, String reference) {
        keepPayment(orderId, username, method, amount, currency, reference, AUTHORIZED);
    }

    @Transactional
    private void keepPayment(String orderId, String username, String[] method, long amount,
            String currency, String reference, String status) {
        if (payments.findById(orderId) != null) {
            // Written while this request was at the processor.
            return;
        }
        Payment row = new Payment();
        row.setOrderId(orderId);
        row.setUsername(username);
        row.setMethodId(method[0]);
        row.setMethodLabel(cut(method[1], 64));
        row.setAmountCents(amount);
        row.setCurrency(currency);
        row.setStatus(status);
        row.setProviderRef(reference);
        row.setCreatedAt(Instant.now());
        payments.insert(row);
    }

    /// Takes the money held for an order: when its first shipment leaves, or at
    /// once for a sale at the counter or a pickup. Safe to repeat; answers the
    /// payment's status, or an empty string when the order has no payment.
    public String capture(String orderId) throws IOException {
        Payment payment = claim(orderId, AUTHORIZED, CAPTURING);
        if (payment == null) {
            Payment kept = payment(orderId);
            return kept == null ? "" : kept.getStatus();
        }
        if (atProcessor(payment.getProviderRef())) {
            try {
                provider.capture(payment.getProviderRef(), payment.getAmountCents());
            } catch (PaymentDeclinedException declined) {
                System.err.println("capture for order " + orderId + " failed: " + declined.getMessage());
                finish(orderId, CAPTURING, FAILED);
                return FAILED;
            } catch (IOException unknown) {
                finish(orderId, CAPTURING, AUTHORIZED);
                throw unavailable("capture", unknown);
            }
        }
        finish(orderId, CAPTURING, PAID);
        return PAID;
    }

    /// Lets go of the money held for an order that was cancelled. Safe to
    /// repeat. A hold the processor could not be asked about is left for it to
    /// expire, as every card hold does within days.
    public String voidOrder(String orderId) throws IOException {
        Payment payment = claim(orderId, AUTHORIZED, VOIDING);
        if (payment == null) {
            Payment kept = payment(orderId);
            return kept == null ? "" : kept.getStatus();
        }
        if (atProcessor(payment.getProviderRef())) {
            try {
                provider.release(payment.getProviderRef());
            } catch (IOException failed) {
                System.err.println("the hold for order " + orderId + " was not released: "
                        + failed.getMessage());
            }
        }
        finish(orderId, VOIDING, VOIDED);
        return VOIDED;
    }

    /// Gives back part or all of what an order was charged. An `amountCents` of
    /// 0 or less refunds whatever is left. Answers the payment as a receipt.
    ///
    /// - `ResponseStatusException` 409: when the payment cannot be refunded now;
    ///   400 for more than is left
    public ReceiptDto refund(String orderId, long amountCents, String reason) throws IOException {
        Payment before = payment(orderId);
        if (before == null) {
            throw new ResponseStatusException(404, "No such payment");
        }
        String from = before.getStatus();
        if (!PAID.equals(from) && !PARTIALLY_REFUNDED.equals(from)) {
            throw new ResponseStatusException(409, "That payment cannot be refunded now");
        }
        long left = before.getAmountCents() - before.getRefundedCents();
        long amount = amountCents <= 0 ? left : amountCents;
        if (amount > left || amount <= 0) {
            throw new ResponseStatusException(400, "That is more than is left to refund");
        }
        Payment payment = claimRefund(orderId, from, amount);
        if (payment == null) {
            throw new ResponseStatusException(409, "That payment is changing; try again");
        }
        PaymentProvider.Refund done = null;
        if (atProcessor(payment.getProviderRef())) {
            try {
                done = provider.refund(payment.getProviderRef(), amount,
                        refundKey(payment.getProviderRef(), payment.getRefundedCents(), amount));
            } catch (IOException failed) {
                finish(orderId, REFUNDING, from);
                throw unavailable("refund", failed);
            }
        }
        return receipt(keepRefund(orderId, amount, reason, done), true);
    }

    /// The idempotency key of one refund intent: the charge, what had been
    /// refunded before it and what it asks for. A retry of a refund whose
    /// answer was lost (a timeout after the processor made it) names the same
    /// key and is answered with the first refund, never a second; a genuine
    /// further partial refund starts from a larger refunded total and gets a
    /// key of its own.
    public static String refundKey(String chargeReference, long refundedBefore, long amountCents) {
        return "refund-" + chargeReference + "-" + refundedBefore + "-" + amountCents;
    }

    @Transactional
    private Payment claimRefund(String orderId, String from, long amount) {
        if (payment(orderId) == null || payments.claimRefund(orderId, from, amount, Instant.now()) != 1) {
            return null;
        }
        return payments.findById(orderId);
    }

    /// Finishes refunds left REFUNDING for more than ten minutes -- the server
    /// stopped, or keeping the result failed, after the claim. Each is asked
    /// for again under its own key, so a refund the processor already made is
    /// answered, not repeated, and is then recorded as any refund is. One the
    /// processor refuses, or one claimed with no amount kept, goes back to
    /// where it was, for the merchant to ask again.
    @Scheduled(fixedDelay = 300000, initialDelay = 90000, lock = "bazaar-stale-refunds")
    public void resumeRefunds() {
        List<Payment> stale = staleRefunds(Instant.now().minusMillis(STALE_REFUND_MILLIS));
        for (int iter = 0; iter < stale.size(); iter++) {
            Payment row = stale.get(iter);
            String orderId = row.getOrderId();
            String from = row.getRefundedCents() > 0 ? PARTIALLY_REFUNDED : PAID;
            long amount = row.getPendingRefundCents();
            if (amount <= 0 || amount > row.getAmountCents() - row.getRefundedCents()) {
                finish(orderId, REFUNDING, from);
                continue;
            }
            PaymentProvider.Refund done = null;
            if (atProcessor(row.getProviderRef())) {
                try {
                    done = provider.refund(row.getProviderRef(), amount,
                            refundKey(row.getProviderRef(), row.getRefundedCents(), amount));
                } catch (IOException failed) {
                    System.err.println("the interrupted refund of order " + orderId
                            + " was not finished: " + failed.getMessage());
                    finish(orderId, REFUNDING, from);
                    continue;
                }
            }
            keepRefund(orderId, amount, row.getRefundReason(), done);
        }
    }

    @Transactional(readOnly = true)
    private List<Payment> staleRefunds(Instant before) {
        return payments.findByStatusAndRefundClaimedAtLessThan(REFUNDING, before);
    }

    @Transactional
    private Payment keepRefund(String orderId, long amount, String reason, PaymentProvider.Refund done) {
        Payment row = payments.findById(orderId);
        long refunded = row.getRefundedCents() + amount;
        row.setRefundedCents(refunded);
        row.setPendingRefundCents(0L);
        row.setRefundReason(cut(reason == null ? "" : reason, 255));
        row.setRefundedAt(Instant.now());
        if (done != null) {
            row.setRefundRef(cut(done.reference, 64));
        }
        if (done != null && done.pending) {
            row.setStatus(REFUND_PENDING);
        } else {
            row.setStatus(refunded >= row.getAmountCents() ? REFUNDED : PARTIALLY_REFUNDED);
        }
        return row;
    }

    /// Asks the processor about refunds it took and had not finished.
    @Scheduled(fixedDelay = 60000, initialDelay = 45000, lock = "bazaar-refunds")
    public void settleRefunds() throws IOException {
        List<Payment> pending = refundsPending();
        IOException failed = null;
        for (int iter = 0; iter < pending.size(); iter++) {
            Payment payment = pending.get(iter);
            try {
                String state = provider.refundStatus(payment.getRefundRef());
                String outcome = null;
                if (PaymentProvider.REFUND_FAILED.equals(state)) {
                    outcome = REFUND_FAILED;
                } else if (PaymentProvider.REFUND_SUCCEEDED.equals(state)) {
                    outcome = payment.getRefundedCents() >= payment.getAmountCents() ? REFUNDED
                            : PARTIALLY_REFUNDED;
                }
                if (outcome != null) {
                    endRefund(payment.getOrderId(), outcome);
                }
            } catch (IOException err) {
                failed = failed == null ? err : failed;
            }
        }
        if (failed != null) {
            throw failed;
        }
    }

    @Transactional(readOnly = true)
    private List<Payment> refundsPending() {
        return payments.findByStatusOrderByCreatedAtAsc(REFUND_PENDING, new PageRequest(0, 50));
    }

    @Transactional
    private void endRefund(String orderId, String outcome) {
        payments.endRefund(orderId, outcome, Instant.now());
    }

    /// Moves a payment from one status to another and answers it, or null when
    /// it was not in `from` (another caller has it, or it never was).
    @Transactional
    private Payment claim(String orderId, String from, String to) {
        if (payment(orderId) == null || payments.claim(orderId, from, to) != 1) {
            return null;
        }
        return payments.findById(orderId);
    }

    @Transactional
    private void finish(String orderId, String from, String to) {
        payments.claim(orderId, from, to);
        if (PAID.equals(to)) {
            Payment row = payments.findById(orderId);
            row.setPaidAt(Instant.now());
            }
    }

    @Transactional(readOnly = true)
    private Payment payment(String orderId) {
        if (orderId == null || orderId.length() == 0 || orderId.length() > 32) {
            return null;
        }
        return payments.findById(orderId);
    }

    // -------------------------------------------------------------- receipts

    /// A customer's own payments, newest first.
    @Transactional(readOnly = true)
    public List<ReceiptDto> receipts(String customer) throws IOException {
        return receipts(payments.findByUsernameOrderByCreatedAtDesc(customer), false);
    }

    /// Everyone's most recent payments, for an admin.
    @Transactional(readOnly = true)
    public List<ReceiptDto> allReceipts() throws IOException {
        return receipts(payments.newestFirst(new PageRequest(0, 200)), true);
    }

    /// One payment, to its payer or an admin; 404 to anyone else.
    @Transactional(readOnly = true)
    public ReceiptDto receipt(String viewer, boolean admin, String orderId) throws IOException {
        Payment row = payment(orderId);
        if (row == null || (!admin && !row.getUsername().equals(viewer))) {
            throw new ResponseStatusException(404, "No such receipt");
        }
        return receipt(row, admin);
    }

    private static List<ReceiptDto> receipts(List<Payment> rows, boolean admin) {
        List<ReceiptDto> out = new ArrayList<ReceiptDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(receipt(rows.get(iter), admin));
        }
        return out;
    }

    private static ReceiptDto receipt(Payment row, boolean admin) {
        return new ReceiptDto(row.getOrderId(), row.getCurrency(), row.getAmountCents(),
                row.getRefundedCents(), row.getMethodLabel(), row.getStatus(),
                row.getPaidAt().toEpochMilli(), admin ? row.getUsername() : "");
    }

    // ------------------------------------------------------------- internals

    /// What the processor calls the customer and one of their cards, in that
    /// order, or null when either is gone.
    @Transactional(readOnly = true)
    private String[] chargeable(String username, String methodId) {
        PaymentMethod method = card(username, methodId);
        PaymentCustomer customer = customers.findById(username);
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

    /// One of an account's cards, or null when the id is not a card of theirs.
    private PaymentMethod card(String username, String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return cards.findFirstByIdAndUsername(id, username);
    }
}
