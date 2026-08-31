/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.UITimer;

/// The two stages of verifying that a user holds a phone number: enter the
/// number, then enter the code that arrives by SMS.
///
/// The application supplies both server calls. This component owns everything
/// around them: the number entry, the code entry, the wait before a resend is
/// offered, the way back to a mistyped number, and the errors either call
/// reports.
///
/// #### Example
///
/// ```java
/// PhoneVerification verify = new PhoneVerification();
/// verify.setCodeSender((number, response) -> myServer.sendSms(number, response));
/// verify.setCodeVerifier((number, code, response) -> myServer.check(number, code, response));
/// verify.addVerifiedListener(e -> showMainScreen());
/// form.add(verify);
/// ```
///
/// A sender is handed the number and a `Response`, and calls exactly one of
/// `Response#succeeded()` or `Response#failed(String)` when its server answers
/// -- from any thread. Until then the button it came from stays disabled, so a
/// second tap cannot send a second message.
///
/// #### The code is offered by the platform
///
/// The code field is an `OtpField`, so it carries the one-time-code hint and
/// the platform offers the arriving code on the keyboard or through autofill.
/// Nothing here reads messages, and no messaging permission is involved.
///
/// #### Styling
///
/// The component uses the UIID "PhoneVerification", its explanatory lines
/// "PhoneVerificationText", its error line "PhoneVerificationError" and its
/// buttons "PhoneVerificationButton" -- except the resend and change-number
/// buttons, which use "PhoneVerificationLink".
public class PhoneVerification extends Container {

    /// The application's answer to one request. Exactly one of its methods
    /// takes effect, and later calls are ignored rather than rejected: a server
    /// wrapper that answers twice on a retry is a nuisance, not a reason to
    /// leave the screen stuck.
    ///
    /// Either method may be called from any thread.
    public static final class Response {

        private final PhoneVerification owner;
        private final int generation;
        private final boolean sending;
        private boolean answered;

        Response(PhoneVerification owner, int generation, boolean sending) {
            this.owner = owner;
            this.generation = generation;
            this.sending = sending;
        }

        /// The request succeeded: the message was sent, or the code was
        /// correct.
        public void succeeded() {
            deliver(true, null);
        }

        /// The request failed.
        ///
        /// #### Parameters
        ///
        /// - `message`: what to show the user; a default is shown when null
        public void failed(String message) {
            deliver(false, message);
        }

        private void deliver(final boolean ok, final String message) {
            synchronized (this) {
                if (answered) {
                    return;
                }
                answered = true;
            }
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    owner.requestAnswered(generation, sending, ok, message);
                }
            });
        }
    }

    /// Asks the application's server to send a code to a number.
    public interface CodeSender {
        /// Sends the code.
        ///
        /// #### Parameters
        ///
        /// - `e164Number`: the number in E.164 form, e.g. "+972501234567"
        ///
        /// - `response`: answered when the server replies
        void sendCode(String e164Number, Response response);
    }

    /// Asks the application's server whether a code matches a number.
    public interface CodeVerifier {
        /// Verifies the code.
        ///
        /// #### Parameters
        ///
        /// - `e164Number`: the number the code was sent to
        ///
        /// - `code`: the code the user entered
        ///
        /// - `response`: answered when the server replies
        void verifyCode(String e164Number, String code, Response response);
    }

    private final PhoneNumberField phone = new PhoneNumberField();
    private final OtpField code;
    private final Label sentTo = new Label();
    private final Label error = new Label();
    private final Button send = new Button();
    private final Button verify = new Button();
    private final Button resend = new Button();
    private final Button changeNumber = new Button();
    private final Container numberStage = new Container(BoxLayout.y());
    private final Container codeStage = new Container(BoxLayout.y());
    private final EventDispatcher verifiedListeners = new EventDispatcher();
    private final EventDispatcher failedListeners = new EventDispatcher();

    private CodeSender codeSender;
    private CodeVerifier codeVerifier;
    private String number;
    private int resendDelay = 60;
    private int resendRemaining;
    private UITimer resendTimer;
    private int generation;
    private boolean busy;
    /// Set when the code stage was entered before there was a form to focus into --
    /// the documented way to start at the second stage does exactly that -- so the
    /// keyboard can be opened once the component is attached instead of not at all.
    private boolean focusCodeWhenAttached;

    /// Builds the flow with a six digit code.
    public PhoneVerification() {
        this(6);
    }

    /// Builds the flow with a code of the given length.
    ///
    /// #### Parameters
    ///
    /// - `codeLength`: the number of digits in the code
    public PhoneVerification(int codeLength) {
        super(BoxLayout.y());
        setUIID("PhoneVerification");
        code = new OtpField(codeLength);
        sentTo.setUIID("PhoneVerificationText");
        error.setUIID("PhoneVerificationError");
        error.setVisible(false);
        send.setUIID("PhoneVerificationButton");
        verify.setUIID("PhoneVerificationButton");
        resend.setUIID("PhoneVerificationLink");
        changeNumber.setUIID("PhoneVerificationLink");
        send.setText(localize("PhoneVerification.Send", "Send code"));
        verify.setText(localize("PhoneVerification.Verify", "Verify"));
        changeNumber.setText(localize("PhoneVerification.ChangeNumber", "Change number"));

        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                requestCode(phone.getE164());
            }
        });
        verify.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                submitCode();
            }
        });
        resend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                requestCode(number);
            }
        });
        changeNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showNumberStage();
            }
        });
        code.addCompleteListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // a complete code is the user saying they are done; asking them
                // to press a button as well is a step no verification screen has
                submitCode();
            }
        });

        numberStage.add(phone).add(send);
        codeStage.add(sentTo).add(code).add(verify).add(resend).add(changeNumber);
        add(error);
        showNumberStage();
    }

    private String localize(String key, String defaultValue) {
        return getUIManager().localize(key, defaultValue);
    }

    // ---- stages ----

    /// Returns to the first stage, with the number as it was left, and clears
    /// any code that was typed.
    public void showNumberStage() {
        abandonPendingRequest();
        stopResendTimer();
        code.setText("");
        replaceStage(numberStage);
        setError(null);
        setBusy(false);
    }

    /// Moves to the code stage for a number, as though the code had just been
    /// sent. Useful when the application sent the message itself rather than
    /// through `#setCodeSender(CodeSender)`.
    ///
    /// #### Parameters
    ///
    /// - `e164Number`: the number the code went to
    public void showCodeStage(String e164Number) {
        abandonPendingRequest();
        number = e164Number;
        phone.setE164(e164Number);
        sentTo.setText(localize("PhoneVerification.SentTo", "Code sent to") + " " + e164Number);
        code.setText("");
        replaceStage(codeStage);
        setError(null);
        setBusy(false);
        startResendTimer();
        focusCodeWhenAttached = getComponentForm() == null;
        if (!focusCodeWhenAttached) {
            code.startEditing();
        }
    }

    /// Retires whatever request is still out. A stage transition is the user
    /// saying the request they were waiting for no longer describes the screen:
    /// they backed out of a verification, or changed a number a code was being
    /// sent to. Without this the answer still matches the current generation
    /// when it lands, so a late success would report a number as verified after
    /// the user left that screen, or drag them back to a code stage they had
    /// just abandoned.
    ///
    /// Answers already delivered are unaffected -- this only invalidates one
    /// that has not arrived yet.
    private void abandonPendingRequest() {
        // busy is left to the caller: both transitions set it through setBusy so
        // the buttons follow, and a second owner of that flag would be one too many
        generation++;
    }

    private void replaceStage(Container stage) {
        if (getComponentCount() > 1 && getComponentAt(1) == stage) { //NOPMD CompareObjectsWithEquals
            return;
        }
        if (getComponentCount() > 1) {
            removeComponent(getComponentAt(1));
        }
        add(stage);
        if (isInitialized()) {
            animateLayout(150);
        }
    }

    /// True when the code stage is showing.
    public boolean isCodeStage() {
        return getComponentCount() > 1 && getComponentAt(1) == codeStage; //NOPMD CompareObjectsWithEquals
    }

    // ---- requests ----

    /// Sends a code to a number, moving to the code stage when the server
    /// accepts it. Called by the send and resend buttons; an application driving
    /// the flow from its own button calls it directly.
    ///
    /// #### Parameters
    ///
    /// - `e164Number`: the number to send to
    public void requestCode(String e164Number) {
        if (busy) {
            return;
        }
        if (!isPlausibleE164(e164Number)) {
            setError(localize("PhoneVerification.BadNumber", "Enter a valid phone number"));
            return;
        }
        if (codeSender == null) {
            setError(localize("PhoneVerification.NoSender", "No verification service is configured"));
            return;
        }
        number = e164Number;
        setError(null);
        setBusy(true);
        generation++;
        codeSender.sendCode(e164Number, new Response(this, generation, true));
    }

    /// The shape a number must have before a request is worth making: a "+",
    /// then between five and fifteen digits, which is what E.164 allows. It is
    /// not a check that the number exists -- that is the sending service's
    /// answer, and its refusal is shown to the user like any other failure.
    ///
    /// #### Parameters
    ///
    /// - `e164Number`: the number to check
    ///
    /// #### Returns
    ///
    /// true when the number is worth sending to
    public static boolean isPlausibleE164(String e164Number) {
        if (e164Number == null || !e164Number.startsWith("+")) {
            return false;
        }
        int digits = 0;
        for (int i = 1; i < e164Number.length(); i++) {
            char c = e164Number.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            digits++;
        }
        return digits >= 5 && digits <= 15;
    }

    /// Verifies the code currently entered. Called when the last box is filled
    /// and by the verify button.
    public void submitCode() {
        if (busy || !isCodeStage()) {
            return;
        }
        if (!code.isComplete()) {
            setError(localize("PhoneVerification.ShortCode", "Enter the whole code"));
            return;
        }
        if (codeVerifier == null) {
            setError(localize("PhoneVerification.NoVerifier", "No verification service is configured"));
            return;
        }
        setError(null);
        setBusy(true);
        generation++;
        codeVerifier.verifyCode(number, code.getText(), new Response(this, generation, false));
    }

    /// Delivered on the EDT by `Response`. A response from a request that has
    /// since been superseded -- the user went back and sent to another number
    /// while the first server call was still out -- is dropped, because acting
    /// on it would move a screen the user has already left.
    void requestAnswered(int forGeneration, boolean sending, boolean ok, String message) {
        if (forGeneration != generation) {
            return;
        }
        setBusy(false);
        if (ok) {
            if (sending) {
                showCodeStage(number);
            } else {
                fireVerified();
            }
            return;
        }
        setError(message != null ? message
                : sending ? localize("PhoneVerification.SendFailed", "The code could not be sent")
                        : localize("PhoneVerification.WrongCode", "That code is not correct"));
        if (!sending) {
            code.clear();
        }
        failedListeners.fireActionEvent(new ActionEvent(this));
    }

    private void fireVerified() {
        verifiedListeners.fireActionEvent(new ActionEvent(this));
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        send.setEnabled(!busy);
        verify.setEnabled(!busy);
        resend.setEnabled(!busy && resendRemaining <= 0);
    }

    private void setError(String message) {
        error.setText(message == null ? "" : message);
        error.setVisible(message != null);
        if (isInitialized()) {
            revalidateLater();
        }
    }

    // ---- resend countdown ----

    private void startResendTimer() {
        stopResendTimer();
        resendRemaining = resendDelay;
        updateResendLabel();
        // no form yet means the countdown starts when the component is added;
        // initComponent arms it then
        if (resendRemaining <= 0 || getComponentForm() == null) {
            return;
        }
        resendTimer = UITimer.timer(1000, true, getComponentForm(), new Runnable() {
            @Override
            public void run() {
                tickResend();
            }
        });
    }

    private void tickResend() {
        if (resendRemaining > 0) {
            resendRemaining--;
            updateResendLabel();
            if (resendRemaining <= 0) {
                stopResendTimer();
            }
        }
    }

    private void updateResendLabel() {
        if (resendRemaining > 0) {
            resend.setEnabled(false);
            resend.setText(localize("PhoneVerification.ResendIn", "Resend in")
                    + " " + resendRemaining);
        } else {
            resend.setEnabled(!busy);
            resend.setText(localize("PhoneVerification.Resend", "Resend code"));
        }
    }

    private void stopResendTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
    }

    @Override
    protected void deinitialize() {
        // a timer bound to a form that is going away would otherwise keep
        // ticking against a screen nobody is looking at
        stopResendTimer();
        super.deinitialize();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (focusCodeWhenAttached && isCodeStage()) {
            // Once, and only for a request that had nowhere to go. Focusing on every
            // initComponent would take focus back and reopen the keyboard each time the
            // user returned to this screen. Deferred a beat because the form settles its
            // own initial focus as it is shown, and the later of the two wins.
            focusCodeWhenAttached = false;
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    if (isCodeStage()) {
                        code.startEditing();
                    }
                }
            });
        }
        if (isCodeStage() && resendRemaining > 0 && resendTimer == null && getComponentForm() != null) {
            resendTimer = UITimer.timer(1000, true, getComponentForm(), new Runnable() {
                @Override
                public void run() {
                    tickResend();
                }
            });
        }
    }

    // ---- configuration ----

    /// Sets the server call that sends a code to a number.
    ///
    /// #### Parameters
    ///
    /// - `codeSender`: the sender
    public void setCodeSender(CodeSender codeSender) {
        this.codeSender = codeSender;
    }

    /// Sets the server call that checks a code.
    ///
    /// #### Parameters
    ///
    /// - `codeVerifier`: the verifier
    public void setCodeVerifier(CodeVerifier codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    /// The seconds the user waits before a resend is offered; 60 by default.
    public int getResendDelay() {
        return resendDelay;
    }

    /// Sets the seconds before a resend is offered. Zero offers it at once.
    ///
    /// #### Parameters
    ///
    /// - `seconds`: the delay
    public void setResendDelay(int seconds) {
        this.resendDelay = Math.max(0, seconds);
        if (resendRemaining > resendDelay) {
            // A countdown already running is shortened to the new delay, so setting zero
            // offers the resend now rather than at the next stage change. A LONGER delay
            // is not applied to it: extending a wait somebody is already serving is not
            // something a setter should do behind their back, and it takes effect on the
            // next code like any other change.
            resendRemaining = resendDelay;
            updateResendLabel();
            if (resendRemaining <= 0) {
                stopResendTimer();
            }
        }
    }

    /// The number the code was sent to, in E.164 form, or null before a code
    /// has been requested.
    public String getPhoneNumber() {
        return number;
    }

    /// The number entry field, exposed for theming and for narrowing the
    /// country list.
    public PhoneNumberField getPhoneNumberField() {
        return phone;
    }

    /// The code entry field, exposed for theming.
    public OtpField getOtpField() {
        return code;
    }

    /// The button that sends the first code, exposed for theming and for
    /// relabelling.
    public Button getSendButton() {
        return send;
    }

    /// The button that submits a typed code, exposed for theming. The code is
    /// also submitted as soon as the last box is filled.
    public Button getVerifyButton() {
        return verify;
    }

    /// The button that asks for another code, exposed for theming.
    public Button getResendButton() {
        return resend;
    }

    /// The button that returns to the number stage, exposed for theming.
    public Button getChangeNumberButton() {
        return changeNumber;
    }

    /// Adds a listener fired when a code is accepted.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addVerifiedListener(ActionListener l) {
        verifiedListeners.addListener(l);
    }

    /// Removes a previously-registered listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeVerifiedListener(ActionListener l) {
        verifiedListeners.removeListener(l);
    }

    /// Adds a listener fired when either server call reports a failure. The
    /// failure is already shown to the user; this is for an application that
    /// wants to count attempts or log them.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addFailedListener(ActionListener l) {
        failedListeners.addListener(l);
    }

    /// Removes a previously-registered listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeFailedListener(ActionListener l) {
        failedListeners.removeListener(l);
    }
}
