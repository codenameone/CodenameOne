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
package com.codenameone.examples.pulse.pay;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.api.CardDto;
import com.codenameone.examples.pulse.api.PaymentConfigDto;
import com.codenameone.examples.pulse.api.PaymentMethodDto;
import com.codenameone.examples.pulse.api.PaymentSetupDto;
import com.codenameone.examples.pulse.domain.PaymentCustomer;
import com.codenameone.examples.pulse.domain.PaymentMethod;
import com.codenameone.examples.pulse.domain.PaymentSetup;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.PaymentCustomerRepository;
import com.codenameone.examples.pulse.repository.PaymentMethodRepository;
import com.codenameone.examples.pulse.repository.PaymentSetupRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The ways a member can pay: their saved cards, and cash at the desk.
///
/// This is the wallet and nothing more. What is owed, and when it is charged,
/// is `billing/Billing`'s business; it calls [#charge] with an invoice as the
/// key, so a charge asked for twice is made once.
///
/// Everything here that takes a `username` acts on that user's own methods and
/// no one else's; the endpoints pass the signed-in user.
///
/// The processor is somewhere else, and a call to it can take seconds, so none
/// is made with a transaction open. Whatever talks to it is three steps: a
/// transaction that reads, or claims, what the call needs and commits; the
/// call; and a transaction that records what came of it, or gives the claim
/// back.
@Component
public class Payments {
    /// The id and the label of the method every account has.
    public static final String CASH = "cash";
    /// An invoice claimed for a charge whose outcome is not yet recorded.
    public static final String CHARGING = "CHARGING";
    public static final String CASH_LABEL = "Cash";
    private static final int MAX_CARDS = 8;
    /// A setup not finished within this long is abandoned.
    private static final long SETUP_MILLIS = 3600000L;
    /// How many setups an account may have open at once. Each is a session
    /// made at the processor and a row here; someone adding a card has one,
    /// or two after giving up on the first.
    private static final int MAX_OPEN_SETUPS = 5;

    private final PaymentMethodRepository cards;
    private final PaymentSetupRepository setups;
    private final PaymentCustomerRepository customers;
    private final InvoiceRepository invoices;
    private final ProfileRepository profiles;
    private final PaymentProvider provider;

    public Payments(PaymentMethodRepository cards, PaymentSetupRepository setups,
            PaymentCustomerRepository customers, InvoiceRepository invoices,
            ProfileRepository profiles, PaymentProvider provider) {
        this.cards = cards;
        this.setups = setups;
        this.customers = customers;
        this.invoices = invoices;
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
    @Scheduled(fixedDelay = 600000, initialDelay = 60000, lock = "pulse-card-setups")
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

    /// Makes a method the one deliveries are paid with unless they say otherwise.
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
    /// neither can a card a charge is under way on.
    ///
    /// The card is detached at the processor before its row is deleted, and
    /// inside the transaction that deletes it, as [#forget] deletes a
    /// customer: the token is the only way back to the card, so a processor
    /// that cannot be reached fails the removal and the card stays, to be
    /// removed again. Detaching a card that is already detached succeeds, so
    /// a removal whose commit failed after the processor answered finishes
    /// the second time. The account's row is held across the request, which
    /// holds back only that member's own payments, and removing a card is
    /// rare enough for that.
    @Transactional
    public PaymentMethodDto remove(String username, String id) throws IOException {
        if (CASH.equals(id)) {
            throw new ResponseStatusException(400, "Cash is always available");
        }
        // Billing holds the same row as it claims an invoice for a charge, so
        // a charge and a removal sent together take turns: either the charge
        // is there to be seen below, or it no longer finds the card.
        profiles.hold(username);
        PaymentMethod row = owned(username, id);
        // What is answered is the card as it was, no longer anyone's default.
        PaymentMethodDto dto = card(row, false);
        // A charge whose answer never came is asked about again by the card's
        // token; with the card gone the question could not be put.
        if (invoices.existsByAccountAndMethodIdAndStatus(username, id, CHARGING)) {
            throw new ResponseStatusException(409,
                    "That card is being charged. Try again in a few minutes");
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

    /// The method a payment is made with: the one named, or the default when
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
    /// closed, here and at the processor. Answers 409 while a charge of theirs
    /// is under way; see [#remove].
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
        if (invoices.existsByAccountAndStatus(username, CHARGING)) {
            throw new ResponseStatusException(409,
                    "A payment is under way. Try again in a few minutes");
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

    // ------------------------------------------------------------- internals

    /// Charges one of a member's saved cards and returns the processor's
    /// reference. `key` makes the charge idempotent at the processor: asked
    /// again with the same key, it answers the first charge and takes nothing
    /// more. What the charge needs is read in a transaction that has ended by
    /// the time the processor is called; call it with none open.
    ///
    /// @throws PaymentDeclinedException when the card was refused or is gone
    public String charge(String key, String username, String methodId, long amountCents,
            String currency, String description) throws IOException {
        String[] references = chargeable(username, methodId);
        if (references == null) {
            throw new PaymentDeclinedException("The card is no longer on the account");
        }
        return cut(provider.charge(key, references[0], references[1], amountCents, currency,
                description), 64);
    }

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
