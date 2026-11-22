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

import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CardDto;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// A processor that moves no money. What the sample runs on until it is given
/// a real one's key, and what every test but that provider's own runs against.
///
/// It behaves like the real thing where the app can tell the difference: a
/// card has to look like a card, and the well-known test numbers do what they
/// do at a real processor's test mode.
///
/// - `4242 4242 4242 4242` is accepted, as is any other number that passes the
///   check digit.
/// - `4000 0000 0000 0002` is declined when it is added.
/// - `4000 0000 0000 0341` is accepted and then declined when it is charged,
///   which is how a payment that fails when an order is captured is tried out.
/// - `4000 0000 0000 0119` is accepted and then cannot be charged at all: the
///   processor fails without saying whether it took the money, which is how
///   a payment nobody can vouch for is tried out.
/// - `4000 0000 0000 7726` is charged like any other, and a refund of it is
///   pending when it is made and has succeeded when it is next asked about,
///   which is how a refund the bank has not answered yet is tried out.
///
/// A refund that is pending and then fails has no card number: a card whose
/// token starts `sim_pm_lost_` does that, and only a test makes one.
///
/// The card number is read here, in this method, and goes no further: the
/// brand and the last four digits are all that is returned or stored.
public class SimulatedPayments implements PaymentProvider {
    static final String DECLINED_NUMBER = "4000000000000002";
    static final String FAILS_LATER_NUMBER = "4000000000000341";
    static final String FAULTY_NUMBER = "4000000000000119";
    static final String SLOW_REFUND_NUMBER = "4000000000007726";
    private static final String FAILING_TOKEN = "sim_pm_fail_";
    private static final String FAULTY_TOKEN = "sim_pm_fault_";
    private static final String SLOW_REFUND_TOKEN = "sim_pm_slow_";
    private static final String LOST_REFUND_TOKEN = "sim_pm_lost_";
    private static final String SLOW_REFUND_CHARGE = "sim_ch_slow_";
    private static final String LOST_REFUND_CHARGE = "sim_ch_lost_";
    private static final String FAILING_HOLD = "sim_hold_fail_";
    private static final String FAULTY_HOLD = "sim_hold_fault_";
    private static final String REFUND = "sim_re_";

    @Override
    public String name() {
        return "simulated";
    }

    @Override
    public String createCustomer(String username, String displayName) throws IOException {
        return "sim_cus_" + Ids.next();
    }

    @Override
    public Setup startCardSetup(String customer, String setupId) {
        Setup setup = new Setup();
        setup.reference = "sim_seti_" + setupId;
        return setup;
    }

    @Override
    public Card finishCardSetup(String customer, String reference, CardDto typed)
            throws IOException {
        if (typed == null) {
            throw new PaymentDeclinedException("Enter the card's details");
        }
        String number = digits(typed.number());
        if (number.length() < 12 || number.length() > 19 || !luhn(number)) {
            throw new PaymentDeclinedException("That is not a card number");
        }
        int year = typed.expYear() < 100 ? typed.expYear() + 2000 : typed.expYear();
        String today = Days.iso(System.currentTimeMillis());
        int thisYear = Integer.parseInt(today.substring(0, 4));
        int thisMonth = Integer.parseInt(today.substring(5, 7));
        if (typed.expMonth() < 1 || typed.expMonth() > 12 || year > thisYear + 25
                || year < thisYear || (year == thisYear && typed.expMonth() < thisMonth)) {
            throw new PaymentDeclinedException("That card has expired");
        }
        String cvc = typed.cvc() == null ? "" : typed.cvc().trim();
        if (cvc.length() < 3 || cvc.length() > 4 || digits(cvc).length() != cvc.length()) {
            throw new PaymentDeclinedException("Check the card's security code");
        }
        if (DECLINED_NUMBER.equals(number)) {
            throw new PaymentDeclinedException("Your card was declined");
        }
        Card card = new Card();
        card.brand = brand(number);
        card.last4 = number.substring(number.length() - 4);
        card.expMonth = typed.expMonth();
        card.expYear = year;
        card.token = (FAILS_LATER_NUMBER.equals(number) ? FAILING_TOKEN
                : FAULTY_NUMBER.equals(number) ? FAULTY_TOKEN
                : SLOW_REFUND_NUMBER.equals(number) ? SLOW_REFUND_TOKEN : "sim_pm_") + Ids.next();
        return card;
    }

    @Override
    public void detachCard(String customer, String token) throws IOException {
        // Nothing is kept of a card here; its token is all there is, and the
        // server deletes that.
    }

    @Override
    public String charge(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        if (token.startsWith(FAILING_TOKEN)) {
            throw new PaymentDeclinedException("Your card was declined");
        }
        if (token.startsWith(FAULTY_TOKEN)) {
            throw new IOException("The simulated processor did not answer");
        }
        // A charge says how its refund is to go, as the card did: the refund
        // has only the charge to go by.
        return (token.startsWith(SLOW_REFUND_TOKEN) ? SLOW_REFUND_CHARGE
                : token.startsWith(LOST_REFUND_TOKEN) ? LOST_REFUND_CHARGE : "sim_ch_") + key;
    }

    /// A hold is a charge that has not been taken yet, and its reference
    /// refunds the way the charge's would. A card that fails when charged is
    /// held without complaint and fails when the hold is taken, as a hold that
    /// lapsed while the order was on its way does: that is the failure an
    /// order meets after checkout, and the one the tests have to reach.
    @Override
    public String authorize(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        if (token.startsWith(FAILING_TOKEN)) {
            return FAILING_HOLD + key;
        }
        if (token.startsWith(FAULTY_TOKEN)) {
            return FAULTY_HOLD + key;
        }
        return charge(key, customer, token, amountCents, currency, description);
    }

    @Override
    public void capture(String reference, long amountCents) throws IOException {
        // Otherwise nothing to take: the simulated card holds no money.
        if (reference.startsWith(FAILING_HOLD)) {
            throw new PaymentDeclinedException("Your card was declined");
        }
        if (reference.startsWith(FAULTY_HOLD)) {
            throw new IOException("The simulated processor did not answer");
        }
    }

    @Override
    public void release(String reference) {
        // Nothing to let go.
    }

    @Override
    public void forgetCustomer(String customer) {
        // Nothing is kept of one here.
    }

    @Override
    public Refund refund(String chargeReference, long amountCents) throws IOException {
        // Named after the charge, as a real processor's refund is keyed by
        // it: asking twice is one refund, which is what lets a refund whose
        // answer was lost be asked for again.
        Refund refund = new Refund();
        refund.reference = REFUND + chargeReference;
        refund.pending = chargeReference.startsWith(SLOW_REFUND_CHARGE)
                || chargeReference.startsWith(LOST_REFUND_CHARGE);
        return refund;
    }

    @Override
    public String refundStatus(String refundReference) throws IOException {
        return refundReference.startsWith(REFUND + LOST_REFUND_CHARGE) ? REFUND_FAILED
                : REFUND_SUCCEEDED;
    }

    // ------------------------------------------------------------- intents

    /// The prefix of a simulated intent's reference.
    static final String INTENT = "sim_pi_";

    /// The intents opened since the server started, by reference. Nothing
    /// survives a restart, as nothing of a simulated card does.
    private final Map<String, Intent> intents = new HashMap<String, Intent>();

    @Override
    public synchronized Intent createIntent(String key, long amountCents, String currency,
            String methodType, boolean manualCapture, String orderId) {
        String reference = INTENT + key;
        Intent had = intents.get(reference);
        if (had != null) {
            // Keyed: asking twice opens one intent.
            return copy(had);
        }
        Intent intent = new Intent();
        intent.reference = reference;
        intent.clientSecret = reference + "_secret";
        intent.amountCents = amountCents;
        intent.currency = currency;
        intent.methodType = methodType;
        intent.status = INTENT_OPEN;
        intents.put(reference, intent);
        return copy(intent);
    }

    @Override
    public synchronized void updateIntent(String reference, long amountCents) throws IOException {
        Intent intent = known(reference);
        if (!INTENT_OPEN.equals(intent.status)) {
            throw new IOException("The intent was already confirmed");
        }
        intent.amountCents = amountCents;
    }

    @Override
    public synchronized Intent intent(String reference) throws IOException {
        return copy(known(reference));
    }

    /// What the shopper does in the simulated wallet sheet or bank page: pays
    /// (`approve`) or gives up. A card or wallet payment is then held; a
    /// method that is not a card is processing until [#settle] says how the
    /// bank answered, as a debit or a pay-later check does.
    public synchronized void complete(String reference, boolean approve) throws IOException {
        Intent intent = known(reference);
        if (!INTENT_OPEN.equals(intent.status)) {
            return;
        }
        if (!approve) {
            intent.status = INTENT_FAILED;
        } else {
            intent.status = "card".equals(intent.methodType) ? INTENT_HELD : INTENT_PROCESSING;
        }
    }

    /// The bank's late answer for a processing intent.
    public synchronized void settle(String reference, boolean ok) throws IOException {
        Intent intent = known(reference);
        if (INTENT_PROCESSING.equals(intent.status) || INTENT_OPEN.equals(intent.status)) {
            intent.status = ok ? INTENT_SUCCEEDED : INTENT_FAILED;
        }
    }

    private Intent known(String reference) throws IOException {
        Intent intent = reference == null ? null : intents.get(reference);
        if (intent == null) {
            throw new IOException("No such intent");
        }
        return intent;
    }

    private static Intent copy(Intent from) {
        Intent out = new Intent();
        out.reference = from.reference;
        out.clientSecret = from.clientSecret;
        out.status = from.status;
        out.amountCents = from.amountCents;
        out.currency = from.currency;
        out.methodType = from.methodType;
        return out;
    }

    /// The digits of what was typed, with the spaces and dashes people put in a
    /// card number taken out. Anything else in it leaves it too short to pass.
    private static String digits(String value) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; value != null && iter < value.length(); iter++) {
            char c = value.charAt(iter);
            if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c != ' ' && c != '-') {
                return "";
            }
        }
        return out.toString();
    }

    /// The check digit every card number carries, which catches a mistyped one.
    private static boolean luhn(String number) {
        int sum = 0;
        boolean doubled = false;
        for (int iter = number.length() - 1; iter >= 0; iter--) {
            int digit = number.charAt(iter) - '0';
            if (doubled) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubled = !doubled;
        }
        return sum % 10 == 0;
    }

    private static String brand(String number) {
        if (number.startsWith("4")) {
            return "Visa";
        }
        if (number.startsWith("34") || number.startsWith("37")) {
            return "Amex";
        }
        if (number.startsWith("6011") || number.startsWith("65")) {
            return "Discover";
        }
        int two = Integer.parseInt(number.substring(0, 2));
        if ((two >= 51 && two <= 55) || (two >= 22 && two <= 27)) {
            return "Mastercard";
        }
        return "Card";
    }
}
