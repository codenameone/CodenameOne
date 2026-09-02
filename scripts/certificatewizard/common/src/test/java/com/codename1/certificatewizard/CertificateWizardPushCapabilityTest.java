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
import com.codename1.certificatewizard.project.ProjectIO;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What automatic setup does about push, driven end to end against the mock account.
///
/// The capability used to be turned on for every App ID the wizard registered, which is
/// issue #5657: a project that never asked for push got it, and so did every profile
/// issued from that App ID. Reading `ios.includePush` instead leaves the other half open,
/// because registration was the ONLY place the capability was ever set -- a project that
/// turns the hint on later reuses the App ID it already has, and nothing would enable it
/// while the builder starts stamping an aps-environment entitlement on the app.
class CertificateWizardPushCapabilityTest {
    /// The mock account's own App ID, so every run here takes the reuse path.
    private static final String EXISTING_BUNDLE = "com.example.myapp";
    private static final String EXISTING_BUNDLE_APPLE_ID = "BID_A1";
    /// The macOS registration of that same identifier, which the account also holds.
    private static final String MAC_BUNDLE_APPLE_ID = "BID_MAC";

    @BeforeAll
    static void initDisplay() {
        if (Display.getInstance() == null || !Display.isInitialized()) {
            Display.init(null);
        }
    }

    @Test
    void aProjectThatAddsPushLaterGetsItOnTheAppIdItAlreadyHas() throws Exception {
        MockSigningService service = runAutoSetup("codename1.arg.ios.includePush=true\n");

        assertTrue(service.pushEnabledOn().contains(EXISTING_BUNDLE_APPLE_ID),
                "the run has to assert the capability on the App ID it reused, got "
                        + service.pushEnabledOn());
    }

    @Test
    void aProjectThatNeverAskedForPushIsLeftAlone() throws Exception {
        MockSigningService service = runAutoSetup("");

        assertTrue(service.pushEnabledOn().isEmpty(),
                "nothing asked for push, so nothing may turn it on: " + service.pushEnabledOn());
    }

    @Test
    void theHintIsReadTheWayTheBuilderReadsIt() throws Exception {
        // IPhoneBuilder normalises anything that is not a trimmed case-insensitive "true"
        // to false. A wizard that read it more loosely would provision a capability the
        // build does not stamp, which is the same over-provisioning by another route.
        assertTrue(runAutoSetup("codename1.arg.ios.includePush= TRUE \n").pushEnabledOn()
                .contains(EXISTING_BUNDLE_APPLE_ID));
        assertFalse(runAutoSetup("codename1.arg.ios.includePush=yes\n").pushEnabledOn()
                .contains(EXISTING_BUNDLE_APPLE_ID));
    }

    @Test
    void pushGoesOnTheIosAppIdEvenWhenTheMacRecordIsListedFirst() throws Exception {
        // One identifier, two App ID records -- iOS and macOS -- which is what an account
        // that also ships a Mac build holds, and Apple returns them in no documented order.
        // The capability has to land on the record the iOS profiles are issued from.
        MockSigningService service = runAutoSetup("codename1.arg.ios.includePush=true\n", true);

        assertTrue(service.pushEnabledOn().contains(EXISTING_BUNDLE_APPLE_ID),
                "push belongs on the iOS App ID, got " + service.pushEnabledOn());
        assertFalse(service.pushEnabledOn().contains(MAC_BUNDLE_APPLE_ID),
                "and not on the macOS one that merely came back first");
    }

    @Test
    void aVoipProjectGetsPushWithoutHavingSetTheHint() throws Exception {
        // The builder turns an absent ios.includePush into true for a VoIP app, so an App
        // ID without the capability means a build that stamps a push entitlement nothing
        // grants. The project declaring the background mode is the part of that the wizard
        // can read without guessing at compiled classes.
        MockSigningService service = runAutoSetup("codename1.arg.ios.background_modes=audio,voip\n");

        assertTrue(service.pushEnabledOn().contains(EXISTING_BUNDLE_APPLE_ID),
                "a declared VoIP project needs the capability, got " + service.pushEnabledOn());
    }

    @Test
    void aMacPushProjectGetsTheCapabilityOnItsMacAppId() throws Exception {
        // The mock account holds both records of this identifier, and the run reaches the
        // Mac half. The Mac build declares the APNs entitlement from its own hint, so the
        // Mac App ID is the one that has to grant it -- the iOS hint says nothing here.
        MockSigningService service =
                runAutoSetup("codename1.arg.macos.entitlements.apsEnvironment=production\n");

        assertTrue(service.pushEnabledOn().contains(MAC_BUNDLE_APPLE_ID),
                "the Mac App ID needs the capability its own build declares, got "
                        + service.pushEnabledOn());
        assertFalse(service.pushEnabledOn().contains(EXISTING_BUNDLE_APPLE_ID),
                "and the iOS App ID does not, because nothing asked for iOS push");
    }

    @Test
    void anIosPushProjectDoesNotTouchTheMacAppId() throws Exception {
        MockSigningService service = runAutoSetup("codename1.arg.ios.includePush=true\n");

        assertFalse(service.pushEnabledOn().contains(MAC_BUNDLE_APPLE_ID),
                "an iOS-only push project must not provision push on its Mac App ID, got "
                        + service.pushEnabledOn());
    }

    @Test
    void aUniversalAppIdHasItsCapabilitiesSettledBeforeAnyProfileIsIssued() throws Exception {
        // One App ID serving both platforms, and only the Mac build asks for push. Enabling
        // it when the Mac stages start would change the capabilities of the very App ID the
        // iOS profiles had just been issued from, and Apple invalidates a profile issued
        // before a capability change -- the run would reissue its Mac profiles and finish
        // with the iOS pair installed and dead.
        final MockSigningService service = new MockSigningService();
        final CertificateWizard[] app = launchBound(service, "com.example.universal",
                "codename1.arg.macos.entitlements.apsEnvironment=production\n");
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.autoSetup");
            }
        });

        List<String> log = service.callLog();
        int enabled = log.indexOf("enablePush:BID_UNIV");
        int firstProfile = -1;
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).equals("createProfile:BID_UNIV")) {
                firstProfile = i;
                break;
            }
        }
        assertTrue(enabled >= 0, "push has to be enabled for a Mac build that declares it: " + log);
        assertTrue(firstProfile >= 0, "and profiles have to be issued: " + log);
        assertTrue(enabled < firstProfile,
                "every capability change has to precede the profiles that snapshot it: " + log);
    }

    @Test
    void standaloneMacSetupSaysWhatItDidNotReissue() throws Exception {
        // One App ID for both platforms, profiles already issued from it, and a button that
        // reissues one Mac profile type. Asserting the capability invalidates everything
        // else that App ID ever issued, and this run is not going to fix those.
        final MockSigningService service = new MockSigningService();
        final CertificateWizard[] app = launchBound(service, "com.example.universal",
                "codename1.arg.macos.entitlements.apsEnvironment=production\n");
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.autoSetup");
            }
        });
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "nav.mac");
            }
        });
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.macAppStore");
            }
        });

        Component banner = find(app[0].getForm(), "page.message");
        assertNotNull(banner, "the outcome has to be on screen");
        String text = ((SpanLabel) banner).getText();
        assertTrue(text.contains("automatic setup reissues them"),
                "the profiles this button did not reissue have to be named: " + text);
    }

    @Test
    void thePushCapabilityCanBeTurnedOnFromTheBundleIdsPage() throws Exception {
        // What a project whose push the builders DETECT rather than read from a hint is
        // left with: neither inference is reproducible in the wizard, so the remedy for a
        // build whose entitlement the App ID does not grant has to be reachable by hand.
        final MockSigningService service = new MockSigningService();
        final CertificateWizard[] app = launchBound(service, "");

        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "nav.bundles");
            }
        });
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.enablePush." + EXISTING_BUNDLE_APPLE_ID);
            }
        });

        assertTrue(service.pushEnabledOn().contains(EXISTING_BUNDLE_APPLE_ID),
                "the action has to reach the service, got " + service.pushEnabledOn());

        // And it has to say what the change did to the profiles. Apple invalidates the ones
        // issued before a capability change, so stopping at "enabled" would leave the
        // project installed against profiles that no longer sign -- the App ID fixed and
        // the same codesign failure on screen.
        Component banner = find(app[0].getForm(), "page.message");
        assertNotNull(banner, "the follow-up has to be on screen");
        assertTrue(((SpanLabel) banner).getText().contains("reissued"),
                ((SpanLabel) banner).getText());
        assertNotNull(find(app[0].getForm(), "page.message.action"),
                "and offer the flow that reissues them");
    }

    /// Runs the wizard's Auto Setup against a project whose settings carry `extraSettings`,
    /// and hands back the service it ran through.
    private MockSigningService runAutoSetup(String extraSettings) throws Exception {
        return runAutoSetup(extraSettings, false);
    }

    /// `macRecordFirst` puts the macOS registration of the identifier ahead of the iOS one
    /// in what the service reports, which Apple is free to do.
    private MockSigningService runAutoSetup(String extraSettings, boolean macRecordFirst) throws Exception {
        final MockSigningService service = new MockSigningService();
        if (macRecordFirst) {
            service.moveBundleToFront(MAC_BUNDLE_APPLE_ID);
        }
        final CertificateWizard[] app = launchBound(service, extraSettings);
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.autoSetup");
            }
        });
        // The mock answers every call on the caller's thread, so the whole chain has run
        // by the time the action returns. One more turn of the EDT for anything the run
        // queued behind it.
        onEdt(new Runnable() {
            public void run() {
            }
        });
        return service;
    }

    /// A wizard bound to a project carrying `extraSettings`, running against `service`.
    private CertificateWizard[] launchBound(final MockSigningService service, String extraSettings)
            throws Exception {
        return launchBound(service, EXISTING_BUNDLE, extraSettings);
    }

    private CertificateWizard[] launchBound(final MockSigningService service, String bundleIdentifier,
            String extraSettings) throws Exception {
        Path dir = Files.createTempDirectory("cn1-cw-push");
        Path settings = dir.resolve("codenameone_settings.properties");
        Files.write(settings, ("codename1.packageName=" + bundleIdentifier + "\n"
                + "codename1.displayName=My App\n" + extraSettings).getBytes(StandardCharsets.UTF_8));
        Path binding = dir.resolve("binding.properties");
        Files.write(binding, ("projectDir=" + dir + "\nsettings=" + settings + "\noutputDir=" + dir + "\n")
                .getBytes(StandardCharsets.UTF_8));
        System.setProperty(ProjectIO.INPUT_PROPERTY, binding.toString());
        final CertificateWizard[] app = new CertificateWizard[1];
        try {
            onEdt(new Runnable() {
                public void run() {
                    CertificateWizard.setServiceForTesting(service);
                    app[0] = new CertificateWizard();
                    app[0].runApp();
                }
            });
        } finally {
            System.clearProperty(ProjectIO.INPUT_PROPERTY);
            CertificateWizard.setServiceForTesting(null);
        }
        return app;
    }

    private static void onEdt(Runnable r) {
        Display.getInstance().callSeriallyAndWait(r);
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
}
