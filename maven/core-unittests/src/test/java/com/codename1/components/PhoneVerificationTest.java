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
package com.codename1.components;

import com.codename1.components.PhoneVerification.Response;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link PhoneVerification}: the two stages, the one-shot response
 * handed to the application, and what happens to an answer that arrives after
 * the user has moved on.
 */
class PhoneVerificationTest extends UITestBase {

    /** Records what it was asked and answers only when the test says so. */
    private static final class RecordingSender implements PhoneVerification.CodeSender {
        final List<String> numbers = new ArrayList<String>();
        Response pending;

        public void sendCode(String e164Number, Response response) {
            numbers.add(e164Number);
            pending = response;
        }
    }

    private static final class RecordingVerifier implements PhoneVerification.CodeVerifier {
        final List<String> codes = new ArrayList<String>();
        String number;
        Response pending;

        public void verifyCode(String e164Number, String code, Response response) {
            number = e164Number;
            codes.add(code);
            pending = response;
        }
    }

    private static PhoneVerification withNumber(String iso, String national) {
        PhoneVerification v = new PhoneVerification();
        v.getPhoneNumberField().setCountry(PhoneNumberField.findCountry(iso));
        v.getPhoneNumberField().getNumberField().setText(national);
        return v;
    }

    // ---- the number's shape ------------------------------------------

    @FormTest
    void plausibilityFollowsTheShapeOfE164() {
        assertTrue(PhoneVerification.isPlausibleE164("+972501234567"));
        assertTrue(PhoneVerification.isPlausibleE164("+12345"));
        assertFalse(PhoneVerification.isPlausibleE164("+1234"), "four digits is too short");
        assertFalse(PhoneVerification.isPlausibleE164("+1234567890123456"), "sixteen digits is too long");
        assertFalse(PhoneVerification.isPlausibleE164("972501234567"), "no leading plus");
        assertFalse(PhoneVerification.isPlausibleE164("+97250-123456"), "not all digits");
        assertFalse(PhoneVerification.isPlausibleE164("+01234567"),
                "E.164 reserves zero, so no calling code starts with one");
        assertFalse(PhoneVerification.isPlausibleE164(null));
    }

    // ---- stage one ----------------------------------------------------

    @FormTest
    void startsOnTheNumberStage() {
        assertFalse(new PhoneVerification().isCodeStage());
    }

    @FormTest
    void anImplausibleNumberIsNeverSentToTheServer() {
        RecordingSender sender = new RecordingSender();
        PhoneVerification v = withNumber("IL", "1");
        v.setCodeSender(sender);
        v.requestCode(v.getPhoneNumberField().getE164());
        assertTrue(sender.numbers.isEmpty());
        assertFalse(v.isCodeStage());
    }

    @FormTest
    void aMissingSenderIsReportedRatherThanThrown() {
        PhoneVerification v = withNumber("IL", "501234567");
        v.requestCode(v.getPhoneNumberField().getE164());
        assertFalse(v.isCodeStage());
    }

    @FormTest
    void acceptedNumberMovesToTheCodeStage() {
        RecordingSender sender = new RecordingSender();
        PhoneVerification v = withNumber("IL", "501234567");
        v.setCodeSender(sender);
        v.requestCode(v.getPhoneNumberField().getE164());
        assertEquals(1, sender.numbers.size());
        assertEquals("+972501234567", sender.numbers.get(0));
        assertFalse(v.isCodeStage(), "the stage only turns once the server has answered");
        sender.pending.succeeded();
        flushSerialCalls();
        assertTrue(v.isCodeStage());
        assertEquals("+972501234567", v.getPhoneNumber());
    }

    @FormTest
    void aSecondTapWhileTheServerIsThinkingSendsNothing() {
        RecordingSender sender = new RecordingSender();
        PhoneVerification v = withNumber("IL", "501234567");
        v.setCodeSender(sender);
        v.requestCode(v.getPhoneNumberField().getE164());
        v.requestCode(v.getPhoneNumberField().getE164());
        assertEquals(1, sender.numbers.size(), "one tap, one message");
    }

    @FormTest
    void aRefusedNumberStaysOnTheNumberStageAndReportsIt() {
        RecordingSender sender = new RecordingSender();
        AtomicInteger failed = new AtomicInteger();
        PhoneVerification v = withNumber("IL", "501234567");
        v.setCodeSender(sender);
        v.addFailedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                failed.incrementAndGet();
            }
        });
        v.requestCode(v.getPhoneNumberField().getE164());
        sender.pending.failed("no route to that number");
        flushSerialCalls();
        assertFalse(v.isCodeStage());
        assertEquals(1, failed.get());
    }

    // ---- stage two ------------------------------------------------------

    @FormTest
    void aFilledCodeIsVerifiedWithoutPressingAnything() {
        RecordingVerifier verifier = new RecordingVerifier();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123456");
        assertEquals(1, verifier.codes.size(), "filling the last box is the submit");
        assertEquals("123456", verifier.codes.get(0));
        assertEquals("+972501234567", verifier.number);
    }

    @FormTest
    void anAcceptedCodeIsReportedToTheApplication() {
        RecordingVerifier verifier = new RecordingVerifier();
        AtomicInteger verified = new AtomicInteger();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.addVerifiedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                verified.incrementAndGet();
            }
        });
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123456");
        assertEquals(0, verified.get(), "not before the server answers");
        verifier.pending.succeeded();
        flushSerialCalls();
        assertEquals(1, verified.get());
    }

    @FormTest
    void aRejectedCodeIsClearedForAnotherTry() {
        RecordingVerifier verifier = new RecordingVerifier();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123456");
        verifier.pending.failed(null);
        flushSerialCalls();
        assertEquals("", v.getOtpField().getText());
        assertTrue(v.isCodeStage(), "a wrong code does not send the user back to the number");
    }

    @FormTest
    void anIncompleteCodeIsNeverSubmitted() {
        RecordingVerifier verifier = new RecordingVerifier();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123");
        v.submitCode();
        assertTrue(verifier.codes.isEmpty());
    }

    @FormTest
    void changingTheNumberGoesBackAndDropsTheCode() {
        PhoneVerification v = new PhoneVerification();
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("1234");
        v.showNumberStage();
        assertFalse(v.isCodeStage());
        assertEquals("", v.getOtpField().getText());
    }

    // ---- the response contract -------------------------------------------

    @FormTest
    void aSecondAnswerToTheSameRequestIsIgnored() {
        RecordingVerifier verifier = new RecordingVerifier();
        AtomicInteger verified = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.addVerifiedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                verified.incrementAndGet();
            }
        });
        v.addFailedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                failed.incrementAndGet();
            }
        });
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123456");
        Response r = verifier.pending;
        r.succeeded();
        r.failed("changed my mind");
        r.succeeded();
        flushSerialCalls();
        assertEquals(1, verified.get());
        assertEquals(0, failed.get());
    }

    @FormTest
    void anAnswerToASupersededRequestIsDropped() {
        RecordingSender sender = new RecordingSender();
        PhoneVerification v = withNumber("IL", "501234567");
        v.setCodeSender(sender);
        v.requestCode(v.getPhoneNumberField().getE164());
        Response stale = sender.pending;

        // the user gave up waiting, corrected the number and sent again
        v.showNumberStage();
        v.getPhoneNumberField().getNumberField().setText("501111111");
        v.requestCode(v.getPhoneNumberField().getE164());

        stale.succeeded();
        flushSerialCalls();
        assertFalse(v.isCodeStage(), "the first server's answer must not move a screen it no longer owns");

        sender.pending.succeeded();
        flushSerialCalls();
        assertTrue(v.isCodeStage());
        assertEquals("+972501111111", v.getPhoneNumber());
    }

    @FormTest
    void anAnswerArrivingAfterTheUserBackedOutIsDropped() {
        RecordingVerifier verifier = new RecordingVerifier();
        AtomicInteger verified = new AtomicInteger();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.addVerifiedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                verified.incrementAndGet();
            }
        });
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123456");

        // the user gave up waiting and went back to fix the number
        v.showNumberStage();

        verifier.pending.succeeded();
        flushSerialCalls();
        assertEquals(0, verified.get(),
                "a number must not be reported verified on a screen the user left");
        assertFalse(v.isCodeStage());
    }

    @FormTest
    void aResendAnswerArrivingAfterTheUserBackedOutDoesNotReopenTheCodeStage() {
        RecordingSender sender = new RecordingSender();
        PhoneVerification v = withNumber("IL", "501234567");
        v.setCodeSender(sender);
        v.setResendDelay(0);
        v.showCodeStage("+972501234567");
        v.requestCode("+972501234567");

        v.showNumberStage();

        sender.pending.succeeded();
        flushSerialCalls();
        assertFalse(v.isCodeStage(),
                "a resend the user walked away from must not drag them back");
    }

    @FormTest
    void anAnswerStillArrivesAfterTheComponentLeavesTheForm() {
        // An application that shows a progress screen over the wait deinitializes this
        // component while the server is still thinking. Dropping the answer then would
        // lose a verification the server did give, and leave the user with nothing --
        // which is worse than a listener firing a moment after they moved on. Going
        // BACK to the number stage is the case that retires a request, and it is a
        // different thing: there the user said they were done waiting.
        RecordingVerifier verifier = new RecordingVerifier();
        AtomicInteger verified = new AtomicInteger();
        PhoneVerification v = new PhoneVerification();
        v.setCodeVerifier(verifier);
        v.addVerifiedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                verified.incrementAndGet();
            }
        });
        Form f = new Form("t", BoxLayout.y());
        f.add(v);
        f.show();
        v.showCodeStage("+972501234567");
        v.getOtpField().setText("123456");
        assertEquals(1, verifier.codes.size());

        f.removeComponent(v);

        verifier.pending.succeeded();
        flushSerialCalls();
        assertEquals(1, verified.get(),
                "the answer the server gave must still reach the application");
    }

    // ---- resend ------------------------------------------------------------

    @FormTest
    void resendIsHeldBackUntilTheDelayHasPassed() {
        PhoneVerification v = new PhoneVerification();
        assertEquals(60, v.getResendDelay());
        v.showCodeStage("+972501234567");
        assertFalse(v.getResendButton().isEnabled(), "a resend offered at once invites a second SMS");
    }

    @FormTest
    void startingAtTheCodeStageBeforeThereIsAFormStillOpensTheKeyboard() {
        // the documented way to start at the second stage builds the component,
        // calls showCodeStage and only then adds it to a form; requestFocus does
        // nothing until there is one
        PhoneVerification v = new PhoneVerification();
        v.showCodeStage("+972501234567");

        Form f = new Form("t", BoxLayout.y());
        f.add(v);
        f.show();
        flushSerialCalls();

        assertTrue(v.getOtpField().getInputField().hasFocus(),
                "the code field should be ready to type into");
    }

    @FormTest
    void shorteningTheResendDelayAppliesToTheWaitAlreadyRunning() {
        PhoneVerification v = new PhoneVerification();
        v.showCodeStage("+972501234567");
        assertFalse(v.getResendButton().isEnabled());

        v.setResendDelay(0);
        assertTrue(v.getResendButton().isEnabled(),
                "setting the delay to zero offers the resend now, not at the next stage");
    }

    @FormTest
    void lengtheningTheResendDelayLeavesTheWaitAlreadyRunningAlone() {
        // extending a wait somebody is already serving is not something a setter
        // should do behind their back
        PhoneVerification v = new PhoneVerification();
        v.setResendDelay(5);
        v.showCodeStage("+972501234567");
        v.setResendDelay(600);
        assertEquals(600, v.getResendDelay());
        v.setResendDelay(0);
        assertTrue(v.getResendButton().isEnabled());
    }

    @FormTest
    void aZeroDelayOffersResendImmediately() {
        PhoneVerification v = new PhoneVerification();
        v.setResendDelay(0);
        v.showCodeStage("+972501234567");
        assertTrue(v.getResendButton().isEnabled());
    }

    @FormTest
    void aNegativeDelayIsTreatedAsNone() {
        PhoneVerification v = new PhoneVerification();
        v.setResendDelay(-5);
        assertEquals(0, v.getResendDelay());
    }

    // ---- the code field carries the hint ------------------------------------

    @FormTest
    void theCodeFieldIsTheOneThatCanReceiveTheSms() {
        PhoneVerification v = new PhoneVerification();
        assertNotEquals(0,
                v.getOtpField().getInputField().getConstraint() & com.codename1.ui.TextArea.ONE_TIME_CODE);
    }

    @FormTest
    void theCodeLengthIsConfigurable() {
        assertEquals(4, new PhoneVerification(4).getOtpField().getLength());
    }
}
