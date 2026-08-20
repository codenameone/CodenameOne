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
package com.codename1.certificatewizard;

import com.codename1.certificatewizard.api.MockSigningService;
import com.codename1.certificatewizard.api.SigningError;
import com.codename1.certificatewizard.api.SigningService;
import com.codename1.certificatewizard.api.SigningState;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.util.OnComplete;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Sync with Apple" is the button the outage report came in on. These drive it
 * for real and assert that whatever the signing service said reaches the
 * screen -- the old code showed "cloud signing service failed (HTTP 502). Try
 * again later." no matter what the body held.
 */
class CertificateWizardErrorBannerTest {

    private static final String REVOKED_KEY =
            "Apple rejected your App Store Connect API key. It has most likely been revoked. Create a new "
            + "key under Users and Access > Integrations > App Store Connect API, then save it in the wizard.";
    private static final String APPLE_DOWN =
            "Codename One could not reach Apple's App Store Connect API. That is usually a brief outage on "
            + "Apple's side -- wait a few minutes and try again.";

    @BeforeAll
    static void initDisplay() {
        if (Display.getInstance() == null || !Display.isInitialized()) {
            Display.init(null);
        }
    }

    @Test
    void aRejectedKeyReachesTheBannerWithAWayToFixIt() throws Exception {
        CertificateWizard app = launch(SigningError.from(409, REVOKED_KEY, null));

        onEdt(() -> fire(app.getForm(), "btn.reconcile"));

        Form form = app.getForm();
        assertTrue(bannerText(form).contains("revoked"), bannerText(form));
        assertTrue(bannerText(form).contains("Users and Access"), bannerText(form));
        Button action = (Button) find(form, "page.message.action");
        assertNotNull(action, "a credential failure should offer a way to the key page");
        assertTrue(action.getText().contains("ASC API Key"), action.getText());
    }

    @Test
    void theKeyActionNavigatesToTheCredentialPage() throws Exception {
        CertificateWizard app = launch(SigningError.from(409, REVOKED_KEY, null));
        onEdt(() -> fire(app.getForm(), "btn.reconcile"));

        onEdt(() -> fire(app.getForm(), "page.message.action"));

        assertTrue(app.getSection() == CertificateWizard.Section.CREDENTIAL,
                "expected the credential page, got " + app.getSection());
    }

    @Test
    void anOutageKeepsTheServersExplanationAndOffersARetry() throws Exception {
        CertificateWizard app = launch(SigningError.from(502, APPLE_DOWN, null));

        onEdt(() -> fire(app.getForm(), "btn.reconcile"));

        Form form = app.getForm();
        String banner = bannerText(form);
        assertTrue(banner.contains("outage on Apple's side"), banner);
        assertTrue(banner.indexOf("Try again later.") < 0, "the old catch-all message is gone: " + banner);
        Button action = (Button) find(form, "page.message.action");
        assertNotNull(action, "a transient failure should offer a retry");
        assertTrue(action.getText().contains("Try again"), action.getText());
    }

    @Test
    void applesOwnRejectionNeedsNoButton() throws Exception {
        String apple = "Apple rejected the request: You already have a current Distribution certificate.";
        CertificateWizard app = launch(SigningError.from(422, apple, null));

        onEdt(() -> fire(app.getForm(), "btn.reconcile"));

        Form form = app.getForm();
        assertTrue(bannerText(form).contains("already have a current Distribution certificate"),
                bannerText(form));
        assertNull(find(form, "page.message.action"),
                "nothing to retry and nothing to fix in the key -- no button");
    }

    /** Long sentences have to wrap; a plain Label would clip them. */
    @Test
    void theBannerWrapsAndStaysCopyable() throws Exception {
        CertificateWizard app = launch(SigningError.from(409, REVOKED_KEY, null));

        onEdt(() -> fire(app.getForm(), "btn.reconcile"));

        Component banner = find(app.getForm(), "page.message");
        assertTrue(banner instanceof SpanLabel, "the banner must wrap, not clip");
        assertTrue(((SpanLabel) banner).getTextComponent().isTextSelectionEnabled(),
                "a developer has to be able to copy the error out");
    }

    // ---- harness ------------------------------------------------------------

    private static CertificateWizard launch(SigningError failure) throws Exception {
        final CertificateWizard[] app = new CertificateWizard[1];
        onEdt(() -> {
            CertificateWizard.setServiceForTesting(new FailingReconcile(failure));
            app[0] = new CertificateWizard();
            app[0].runApp();
        });
        return app[0];
    }

    private static void onEdt(Runnable r) {
        Display.getInstance().callSeriallyAndWait(r);
    }

    private static String bannerText(Form form) {
        Component c = find(form, "page.message");
        assertNotNull(c, "no error banner on screen");
        return ((SpanLabel) c).getText();
    }

    private static void fire(Container root, String name) {
        Component c = find(root, name);
        assertNotNull(c, "no component named " + name);
        ((Button) c).pressed();
        ((Button) c).released();
    }

    private static Component find(Container root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component out = find((Container) c, name);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    /** A mock that succeeds at everything except the call under test. */
    private static final class FailingReconcile implements SigningService {
        private final MockSigningService delegate = new MockSigningService();
        private final SigningError failure;

        FailingReconcile(SigningError failure) {
            this.failure = failure;
        }

        public void reconcile(OnComplete<Result<Void>> callback) {
            callback.completed(Result.<Void>fail(failure));
        }

        public void refresh(OnComplete<Result<SigningState>> callback) {
            delegate.refresh(callback);
        }

        public void saveCredential(String keyId, String issuerId, String p8, OnComplete<Result<Void>> cb) {
            delegate.saveCredential(keyId, issuerId, p8, cb);
        }

        public void deleteCredential(OnComplete<Result<Void>> cb) {
            delegate.deleteCredential(cb);
        }

        public void createCertificate(String type, String name, OnComplete<Result<Void>> cb) {
            delegate.createCertificate(type, name, cb);
        }

        public void revokeCertificate(Long id, OnComplete<Result<Void>> cb) {
            delegate.revokeCertificate(id, cb);
        }

        public void createBundleId(String id, String name, String platform, boolean push,
                OnComplete<Result<Void>> cb) {
            delegate.createBundleId(id, name, platform, push, cb);
        }

        public void createAppGroup(String id, String name, OnComplete<Result<SigningState.AppGroup>> cb) {
            delegate.createAppGroup(id, name, cb);
        }

        public void enableAppGroupCapability(String bundleId, List<String> groups, OnComplete<Result<Void>> cb) {
            delegate.enableAppGroupCapability(bundleId, groups, cb);
        }

        public void registerDevice(String name, String udid, OnComplete<Result<Void>> cb) {
            delegate.registerDevice(name, udid, cb);
        }

        public void createProfile(String name, String type, String bundleId, List<String> certs,
                List<String> devices, OnComplete<Result<Void>> cb) {
            delegate.createProfile(name, type, bundleId, certs, devices, cb);
        }

        public void deleteProfile(Long id, OnComplete<Result<Void>> cb) {
            delegate.deleteProfile(id, cb);
        }

        public void saveApnsKey(String keyId, String teamId, String p8, String name,
                OnComplete<Result<Void>> cb) {
            delegate.saveApnsKey(keyId, teamId, p8, name, cb);
        }

        public void deleteApnsKey(String keyId, OnComplete<Result<Void>> cb) {
            delegate.deleteApnsKey(keyId, cb);
        }

        public void clearSigningData(OnComplete<Result<Void>> cb) {
            delegate.clearSigningData(cb);
        }

        public void downloadP12(Long id, String password, String name, OnComplete<Result<String>> cb) {
            delegate.downloadP12(id, password, name, cb);
        }

        public void downloadProfile(Long id, String name, OnComplete<Result<String>> cb) {
            delegate.downloadProfile(id, name, cb);
        }
    }
}
