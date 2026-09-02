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
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /// Runs the wizard's Auto Setup against a project whose settings carry `extraSettings`,
    /// and hands back the service it ran through.
    private MockSigningService runAutoSetup(String extraSettings) throws Exception {
        return runAutoSetup(extraSettings, false);
    }

    /// `macRecordFirst` puts the macOS registration of the identifier ahead of the iOS one
    /// in what the service reports, which Apple is free to do.
    private MockSigningService runAutoSetup(String extraSettings, boolean macRecordFirst) throws Exception {
        Path dir = Files.createTempDirectory("cn1-cw-push");
        Path settings = dir.resolve("codenameone_settings.properties");
        Files.write(settings, ("codename1.packageName=" + EXISTING_BUNDLE + "\n"
                + "codename1.displayName=My App\n" + extraSettings).getBytes(StandardCharsets.UTF_8));
        Path binding = dir.resolve("binding.properties");
        Files.write(binding, ("projectDir=" + dir + "\nsettings=" + settings + "\noutputDir=" + dir + "\n")
                .getBytes(StandardCharsets.UTF_8));
        System.setProperty(ProjectIO.INPUT_PROPERTY, binding.toString());

        final MockSigningService service = new MockSigningService();
        if (macRecordFirst) {
            service.moveBundleToFront(MAC_BUNDLE_APPLE_ID);
        }
        final CertificateWizard[] app = new CertificateWizard[1];
        try {
            onEdt(new Runnable() {
                public void run() {
                    CertificateWizard.setServiceForTesting(service);
                    app[0] = new CertificateWizard();
                    app[0].runApp();
                }
            });
            onEdt(new Runnable() {
                public void run() {
                    fire(app[0].getForm(), "btn.autoSetup");
                }
            });
            // The mock answers every call on the caller's thread, so the whole chain has
            // run by the time the action returns. One more turn of the EDT for anything
            // the run queued behind it.
            onEdt(new Runnable() {
                public void run() {
                }
            });
        } finally {
            System.clearProperty(ProjectIO.INPUT_PROPERTY);
            CertificateWizard.setServiceForTesting(null);
        }
        return service;
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
