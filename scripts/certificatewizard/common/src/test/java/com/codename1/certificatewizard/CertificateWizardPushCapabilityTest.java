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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
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
    void aMacPushFailureNamesTheMacHint() throws Exception {
        // The two platforms declare push through different settings. A Mac-only project
        // told to look at ios.includePush is being pointed at something it never set, while
        // the profiles it just got cannot satisfy the build it did ask for.
        final MockSigningService service = new MockSigningService();
        service.failPushCapability("Apple rejected the request.");
        final CertificateWizard[] app = launchBound(service, "com.example.universal",
                "codename1.arg.macos.entitlements.apsEnvironment=production\n");
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.autoSetup");
            }
        });

        settle();
        Component banner = find(app[0].getForm(), "page.message");
        assertNotNull(banner, "a failed capability change has to be reported");
        String text = ((SpanLabel) banner).getText();
        assertTrue(text.contains("macos.entitlements.apsEnvironment"),
                "the hint that asked for push is the one to name: " + text);
        assertTrue(text.indexOf("ios.includePush") < 0,
                "and not the one this project never set: " + text);
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

        settle();
        Component banner = find(app[0].getForm(), "page.message");
        assertNotNull(banner, "the outcome has to be on screen");
        String text = ((SpanLabel) banner).getText();
        assertTrue(text.contains("automatic setup reissues them"),
                "the profiles this button did not reissue have to be named: " + text);
    }

    @Test
    void aSecondRunStillRetiresAProfileTheFirstRunFailedToDelete() throws Exception {
        // The reissue guard records a profile before the delete is attempted, so a delete
        // that failed leaves the id behind. Carried into the next run it reads as "already
        // handled": the run skips the delete and installs the invalid profile it was
        // supposed to replace, leaving the project signed with assets that cannot sign.
        final MockSigningService service = new MockSigningService();
        service.invalidateProfilesFor("com.example.myapp");
        service.failProfileDeletion("Apple rejected the request.");
        final CertificateWizard[] app = launchBound(service, "");
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.autoSetup");
            }
        });
        List<Long> firstRun = service.deletedProfileIds();
        assertTrue(!firstRun.isEmpty(), "the first run has to try the delete");
        Long refused = firstRun.get(0);

        service.failProfileDeletion(null);
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.autoSetup");
            }
        });

        // The SAME profile, not merely another delete: a run retires several invalid
        // profiles, so a count says nothing about whether this one was tried again.
        List<Long> secondRun = service.deletedProfileIds().subList(firstRun.size(),
                service.deletedProfileIds().size());
        assertTrue(secondRun.contains(refused),
                "the retry has to attempt the profile it could not delete, got " + secondRun
                        + " after " + firstRun);
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
        settle();
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
    @Test
    void anAccountHoldingOnlyIosIdentifiersStillOffersToRegisterAMacOne() throws Exception {
        // The account has App IDs, just none a Mac profile can use, and this project's
        // identifier is not one of them. Deciding the remedy on whether the account holds
        // ANY bundle IDs left this case with an explanation and nothing to act on: the Mac
        // profile could not be created, and no screen in the wizard registered a Mac App ID.
        final MockSigningService service = new MockSigningService();
        service.removeBundlesForPlatform("MAC_OS");
        service.removeBundlesForPlatform("UNIVERSAL");
        final CertificateWizard[] app = launchBound(service, "com.example.brandnew", "");

        openMacProfileDialog(app);

        assertNotNull(find(app[0].getForm(), "btn.profileNeedsBundle"),
                "an unregistered identifier has to be offered for registration");
    }

    @Test
    void otherPeoplesMacIdentifiersAreNotARemedyForThisProjectHavingNone() throws Exception {
        // The account holds Mac App IDs -- for other apps. The account-wide list is
        // therefore not empty, and testing THAT skipped the whole remedy: the dialog listed
        // other people's apps, offered no way to register this project's own Mac App ID,
        // and the Bundle IDs page registers iOS identifiers only. What matters is whether
        // this PROJECT has one the profile type can use.
        final MockSigningService service = new MockSigningService();
        final CertificateWizard[] app = launchBound(service, "com.example.brandnew", "");

        openMacProfileDialog(app);

        assertNotNull(find(app[0].getForm(), "pick.bundle.BID_MACONLY"),
                "the account's other Mac App IDs are still listed");
        assertNotNull(find(app[0].getForm(), "btn.profileNeedsBundle"),
                "and this project is still offered a Mac App ID of its own");
    }

    @Test
    void appGroupsOnAMacRegistrationLookTheBundleUpOnItsOwnPlatform() throws Exception {
        // Registering from the Mac profile flow creates a MAC_OS App ID, and the App Groups
        // follow-up then looked the identifier back up as IOS. It found nothing and told the
        // user the bundle it had just created could not be found.
        final MockSigningService service = new MockSigningService();
        service.removeBundlesForPlatform("MAC_OS");
        service.removeBundlesForPlatform("UNIVERSAL");
        final CertificateWizard[] app = launchBound(service, "com.example.brandnew", "");

        openMacProfileDialog(app);
        // Checked from here rather than inside the event thread: fire() asserts on a
        // missing component, and an assertion thrown in there is handled as an application
        // error instead of failing the test -- the run hangs rather than saying what is
        // wrong.
        assertNotNull(find(app[0].getForm(), "btn.profileNeedsBundle"),
                "the registration action has to be offered before this can test it");
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.profileNeedsBundle");
            }
        });
        onEdt(new Runnable() {
            public void run() {
                Component id = find(app[0].getForm(), "modal.bundle.identifier");
                assertNotNull(id, "the registration dialog has to be on screen");
                ((TextField) id).setText("com.example.brandnew");
                Component groups = find(app[0].getForm(), "modal.bundle.appGroups");
                assertNotNull(groups, "with the App Groups option on it");
                ((CheckBox) groups).setSelected(true);
                fire(app[0].getForm(), "modal.bundle.submit");
            }
        });
        settle();

        Component banner = find(app[0].getForm(), "page.message");
        String text = banner == null ? "" : ((SpanLabel) banner).getText();
        assertTrue(text.indexOf("could not be found after refresh") < 0,
                "the bundle it just registered has to be found again: " + text);
    }

    /// Opens the new-profile dialog and switches it to a Mac profile type, which is what
    /// narrows the bundle list to the App IDs macOS can use.
    private void openMacProfileDialog(final CertificateWizard[] app) throws Exception {
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "nav.profiles");
            }
        });
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "btn.newProfile");
            }
        });
        // Settled before touching the dialog: its show animation queues container
        // mutations, so the type buttons are not reachable while it is still running and
        // the type silently stays on iOS.
        settle();
        onEdt(new Runnable() {
            public void run() {
                fire(app[0].getForm(), "pick.type.mac_app_store");
            }
        });
        settle();
    }

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

    /// Waits until the event thread has nothing animating, because container mutations are
    /// QUEUED while the animation manager is busy: a page rebuilt under a toast reads back
    /// as though the rebuild never happened. Bounded, so a test that never settles fails on
    /// its own assertion rather than hanging.
    private static void settle() throws Exception {
        for (int i = 0; i < 60; i++) {
            final boolean[] busy = new boolean[1];
            onEdt(new Runnable() {
                public void run() {
                    Form current = Display.getInstance().getCurrent();
                    busy[0] = current != null && current.getAnimationManager().isAnimating();
                }
            });
            if (!busy[0]) {
                // A few more turns, so anything the last animated frame queued has been
                // applied before the tree is read.
                for (int t = 0; t < 3; t++) {
                    onEdt(new Runnable() {
                        public void run() {
                        }
                    });
                }
                return;
            }
            Thread.sleep(100);
        }
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
