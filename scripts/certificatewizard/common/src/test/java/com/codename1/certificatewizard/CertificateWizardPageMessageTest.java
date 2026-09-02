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
import com.codename1.ui.Form;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which banner is on screen when a step finishes, which is the whole of issue #5652: the
/// wizard reports what it is doing and what went wrong with it through the same banner,
/// and automatic setup runs a dozen steps in a row.
///
/// A step that succeeded used to wipe the warning the step before it had just put up, so
/// the only trace of a skipped step was a message too quick to read. The opposite mistake
/// costs just as much: keeping every warning-styled message would leave a failed install
/// on screen next to the success toast of the retry that fixed it.
class CertificateWizardPageMessageTest {
    /// Registered for iOS only in the mock account, so automatic setup has to skip the Mac
    /// half of the run: Apple registers an identifier once, and adding macOS to it is a
    /// portal action the signing API does not offer.
    private static final String IOS_ONLY_BUNDLE = "com.example.watch";

    @BeforeAll
    static void initDisplay() {
        if (Display.getInstance() == null || !Display.isInitialized()) {
            Display.init(null);
        }
    }

    @Test
    void aStepAutomaticSetupSkippedIsStillOnScreenWhenTheRunEnds() throws Exception {
        CertificateWizard app = launch(IOS_ONLY_BUNDLE);

        onEdt(new Runnable() {
            public void run() {
                fire(app.getForm(), "btn.autoSetup");
            }
        });

        String banner = bannerText(app.getForm());
        assertTrue(banner.contains("Mac signing was skipped"),
                "the run has to end saying what it could not do, got: " + banner);
        assertTrue(banner.contains(IOS_ONLY_BUNDLE), banner);
    }

    @Test
    void anIdentifierRegisteredOnlyForMacStillRunsTheMacStages() throws Exception {
        // The account has this identifier registered for macOS and nothing else, which is
        // what running Mac setup first leaves behind. iOS cannot be added to it from here --
        // Apple registers an identifier once -- so the run has to say so and carry on with
        // the stages that do not need an iOS App ID, rather than stopping several steps
        // later to report a bundle ID it never tried to create.
        CertificateWizard app = launch("com.example.macapp");

        onEdt(new Runnable() {
            public void run() {
                fire(app.getForm(), "btn.autoSetup");
            }
        });

        String banner = bannerText(app.getForm());
        assertTrue(banner.contains("iOS signing was skipped"),
                "the run has to say what it could not do, got: " + banner);
        assertTrue(banner.contains("Automatic signing setup completed"),
                "and still finish the stages that can run, got: " + banner);
        assertTrue(banner.indexOf("could not be found after refresh") < 0,
                "and never report a bundle ID it did not try to create: " + banner);
    }

    @Test
    void aSuccessfulInstallClearsTheFailureBeforeIt() throws Exception {
        CertificateWizard app = launch("com.example.myapp");

        // A failure that has nothing to do with automatic setup -- the install buttons on
        // the certificates and profiles pages run the same code, and a retry that succeeds
        // must not leave the failure it replaced on screen.
        onEdt(new Runnable() {
            public void run() {
                app.showUnhandledEdtError(new RuntimeException("Synthetic install failure"));
            }
        });
        assertTrue(bannerText(app.getForm()).contains("Synthetic install failure"),
                "the failure has to be on screen for this to mean anything");

        onEdt(new Runnable() {
            public void run() {
                fire(app.getForm(), "nav.profiles");
            }
        });
        onEdt(new Runnable() {
            public void run() {
                app.showUnhandledEdtError(new RuntimeException("Synthetic install failure"));
            }
        });
        onEdt(new Runnable() {
            public void run() {
                fire(app.getForm(), "btn.installProfile.1");
            }
        });

        Component banner = find(app.getForm(), "page.message");
        assertTrue(banner == null || ((SpanLabel) banner).getText().length() == 0,
                "a successful install has to clear the failure before it, got: "
                        + (banner == null ? "" : ((SpanLabel) banner).getText()));
    }

    /// A wizard bound to a project whose package name is `bundleIdentifier`, running
    /// against the mock account.
    private CertificateWizard launch(String bundleIdentifier) throws Exception {
        Path dir = Files.createTempDirectory("cn1-cw-banner");
        Path settings = dir.resolve("codenameone_settings.properties");
        Files.write(settings, ("codename1.packageName=" + bundleIdentifier + "\n"
                + "codename1.displayName=My App\n").getBytes(StandardCharsets.UTF_8));
        Path binding = dir.resolve("binding.properties");
        Files.write(binding, ("projectDir=" + dir + "\nsettings=" + settings + "\noutputDir=" + dir + "\n")
                .getBytes(StandardCharsets.UTF_8));
        System.setProperty(ProjectIO.INPUT_PROPERTY, binding.toString());
        final CertificateWizard[] app = new CertificateWizard[1];
        try {
            onEdt(new Runnable() {
                public void run() {
                    CertificateWizard.setServiceForTesting(new MockSigningService());
                    app[0] = new CertificateWizard();
                    app[0].runApp();
                }
            });
        } finally {
            System.clearProperty(ProjectIO.INPUT_PROPERTY);
            CertificateWizard.setServiceForTesting(null);
        }
        return app[0];
    }

    private static void onEdt(Runnable r) {
        Display.getInstance().callSeriallyAndWait(r);
    }

    private static String bannerText(Form form) {
        Component c = find(form, "page.message");
        assertNotNull(c, "no banner on screen");
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
}
