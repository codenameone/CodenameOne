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
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.ArrayList;
import java.util.List;

/// Drives the real wizard against the mock signing service and asserts the structure of every
/// page and of the new-profile dialog. It needs a display, so nothing runs it automatically and
/// it drifts out of date silently -- run it by hand after touching the UI:
///
/// ```
/// source tools/env.sh
/// cd scripts/certificatewizard
/// JAVA_HOME="$JAVA17_HOME" mvn -q install -DskipTests -Pexecutable-jar \
///     -Dmaven.repo.local=$(git rev-parse --show-toplevel)/.m2-repo
/// cd javase
/// CP="target/test-classes:target/classes:$(ls target/libs/*.jar | tr '\n' ':')"
/// "$JAVA17_HOME/bin/java" -cp "$CP" \
///     com.codename1.certificatewizard.CertificateWizardStructureHarness
/// ```
///
/// It exits non-zero and lists every failed expectation.
public final class CertificateWizardStructureHarness {
    private static final List<String> fail = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        Display.init(null);
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    Resources r = Resources.openLayered("/theme");
                    String[] n = r.getThemeResourceNames();
                    if (n != null && n.length > 0) {
                        UIManager.getInstance().setThemeProps(r.getTheme(n[0]));
                    }
                } catch (Exception ignore) {
                }
            }
        });

        final CertificateWizard[] app = new CertificateWizard[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                CertificateWizard.setServiceForTesting(new MockSigningService());
                app[0] = new CertificateWizard();
                app[0].runApp();
            }
        });
        // One stage per serial call, with the EDT left to run in between. A Container mutated
        // while the AnimationManager is busy -- which it is for the whole of a dialog's show
        // animation -- has its removals and additions QUEUED, so a rebuild inside a freshly shown
        // dialog reads back as if nothing happened. Nothing here is a UI defect; the checks just
        // have to look after the frame that applies them.
        Runnable[] stages = {
            new Runnable() { public void run() { checkShell(app[0]); } },
            new Runnable() { public void run() { checkNewProfileDialog(app[0]); } },
            new Runnable() { public void run() { checkMacDevelopmentRemedy(app[0]); } },
            new Runnable() { public void run() { checkMacDevelopmentDeadEnd(app[0]); } },
            new Runnable() { public void run() { checkCertificateDialogOpensOnTheNeededType(app[0]); } },
            new Runnable() { public void run() { checkRemainingSections(app[0]); } },
            // Last, because it replaces the wizard: the checks above deliberately run with
            // no project binding, and this one needs one.
            new Runnable() { public void run() { app[0] = openProjectBoundWizard(); } },
            new Runnable() { public void run() { fire(app[0].getForm(), "btn.newProfile"); } },
            new Runnable() { public void run() { checkProjectScopedProfileDialog(app[0]); } },
        };
        for (Runnable stage : stages) {
            Display.getInstance().callSeriallyAndWait(stage);
            Thread.sleep(400);
        }
        System.out.println("[CertificateWizardStructure] failures=" + fail.size());
        for (String f : fail) {
            System.out.println("  FAIL: " + f);
        }
        System.out.println("[CertificateWizardStructure] RESULT " + (fail.isEmpty() ? "OK" : "FAIL"));
        System.exit(fail.isEmpty() ? 0 : 1);
    }

    private static void checkShell(CertificateWizard app) {
        Form form = app.getForm();
        check(app.getSection() == CertificateWizard.Section.OVERVIEW, "wizard starts on overview section");
        check(find(form, "btn.refresh") != null, "refresh button present");
        check(find(form, "toggle.darkMode") != null, "dark mode toggle present");
        Component autoSetup = find(form, "btn.autoSetup");
        check(autoSetup != null, "auto setup action present on overview");
        check(autoSetup != null && autoSetup.getUIID().contains("CWStatusOff"), "auto setup uses status pill styling");
        check(form.getTextSelection().isEnabled(), "form text selection enabled");
        check(find(form, "pill.credential") != null, "credential status pill present");
        String[] navs = {"overview", "credential", "certificates", "bundles", "devices", "profiles", "apns",
                "mac", "android", "windows", "maintenance"};
        for (String n : navs) {
            check(find(form, "nav." + n) != null, "nav." + n + " present");
        }
        check(app.getState().certificates.size() >= 2, "mock certificates loaded");
        check(app.getState().profiles.size() >= 2, "mock profiles loaded");

        fire(form, "nav.certificates");
        check(app.getSection() == CertificateWizard.Section.CERTIFICATES, "certificates navigation updates section");
        check(find(form, "btn.generateCert") != null, "generate certificate action present");
        check(find(form, "btn.reconcile") != null, "sync action present");
        check(find(form, "btn.installCert.1") != null, "certificate install action present");
        check(find(form, "filter.certificates") != null, "certificate filter field present");
        Component clearFilter = find(form, "filter.clear.certificates");
        check(clearFilter != null, "certificate filter clear icon present");
        check(!(clearFilter instanceof Button) || ((Button)clearFilter).getText() == null
                || ((Button)clearFilter).getText().length() == 0, "certificate filter clear uses icon only");
        check(find(form, "sort.certificates.0") != null, "certificate sort header present");
        Component certText = findText(form, "App Store Distribution");
        check(certText instanceof Label && ((Label)certText).isTextSelectionEnabled(),
                "certificate table values are selectable");
        setText(form, "filter.certificates", "Development");
        form = app.getForm();
        check(find(form, "btn.installCert.1") == null, "certificate filter hides non-matches");
        check(find(form, "btn.installCert.2") != null, "certificate filter keeps matches");
        fire(form, "filter.clear.certificates");
        form = app.getForm();
        check(find(form, "btn.installCert.1") != null, "certificate filter clears");
        fire(form, "sort.certificates.1");
        form = app.getForm();
        check(find(form, "sort.certificates.1") != null, "certificate sort header remains after sort");
        check(selected(form, "nav.certificates"), "certificates nav selected");

        fire(form, "nav.bundles");
        check(find(form, "btn.addBundle") != null, "bundle add action present");
        // The service reports no capabilities with the App ID listing, so the push column
        // says so instead of filling the silence in with "Off" (issue #5657).
        check(findTextContaining(form, "Unknown") != null,
                "an App ID whose capabilities the service did not report says push is unknown");
        // And the capability can be turned on from here. The builders also enable push for
        // what they DETECT -- a VoIP reference on iOS, a push registration call in the
        // macOS class scan -- and neither inference is reproducible in the wizard, so the
        // remedy for a build whose entitlement the App ID does not grant has to be one
        // click rather than a guess.
        check(find(form, "btn.enablePush.BID_A1") != null,
                "push can be enabled on an existing App ID");
        fire(form, "btn.addBundle");
        check(Display.getInstance().getCurrent() == form, "bundle modal overlays current form");
        check(find(form, "modal.cancel") != null, "bundle modal cancel action present");
        fire(form, "modal.cancel");
        form = app.getForm();
        fire(form, "nav.devices");
        check(find(form, "btn.addDevice") != null, "device add action present");
        fire(form, "nav.profiles");
        check(find(form, "btn.newProfile") != null, "new profile action present");
        check(find(form, "btn.autoSetup") != null, "auto setup action present on profiles");
        check(find(form, "btn.installProfile.1") != null, "profile install action present");
        check(selected(form, "nav.profiles"), "profiles nav selected");
        check(!selected(form, "nav.certificates"), "certificates nav no longer selected");
        fire(form, "btn.newProfile");
    }

    private static void checkNewProfileDialog(CertificateWizard app) {
        Form form = app.getForm();
        check(find(form, "modal.profile.submit") != null, "profile modal create action present");
        check(find(form, "pick.bundle.BID_A1") != null, "profile modal bundle choice present");
        check(find(form, "pick.cert.1") != null, "profile modal certificate choice present");
        // The dialog opens on App Store, and that has to be true of the model and not only of the
        // segment's styling -- the whole of issue #5636's "Create never enables" is this gap.
        check(selectedSegment(form, "pick.type.ios_app_store"), "App Store segment starts selected");
        check(find(form, "pick.cert.3") == null,
                "only certificates the selected profile type can use are offered");
        check(find(form, "pick.cert.5") != null,
                "a certificate with no stored private key is still offered: creating a profile "
                        + "sends its Apple ID, and only the later export needs the key");
        check(find(form, "pick.device.DEV_1") == null,
                "an App Store profile does not ask for devices");
        Component requirement = find(form, "modal.profile.requirement");
        check(requirement instanceof Label && ((Label)requirement).getText().length() > 0,
                "disabled create action explains what is missing");
        fire(form, "pick.bundle.BID_A1");
        fire(form, "pick.bundle.BID_A1");
        check(selectedChoice(form, "pick.bundle.BID_A1"),
                "re-picking the selected bundle keeps it selected");
        fire(form, "pick.cert.1");
        check(enabled(form, "modal.profile.submit"),
                "type, bundle, certificate and a name are enough to create an App Store profile");
        check(requirementText(form).length() == 0, "no requirement is reported once create is enabled");
        fire(form, "pick.type.ios_app_development");
        fire(form, "pick.type.ios_app_development");
        check(selectedSegment(form, "pick.type.ios_app_development"),
                "re-picking the selected profile type keeps it selected");
        check(find(form, "pick.cert.1") == null,
                "a distribution certificate is dropped when the type becomes development");
        check(find(form, "pick.cert.2") != null, "the development certificate is offered instead");
        check(find(form, "pick.device.DEV_1") != null, "a development profile asks for devices");
        check(find(form, "pick.device.DEV_3") == null, "a disabled device is not offered");
        fire(form, "pick.cert.2");
        check(!enabled(form, "modal.profile.submit"), "create waits for the devices it now needs");
        check(requirementText(form).contains("device"), "the missing devices are named");
        fire(form, "modal.profile.selectAllDevices");
        check(selectedCheckBox(form, "pick.device.DEV_1") && selectedCheckBox(form, "pick.device.DEV_2"),
                "select all checks every device");
        check(find(form, "pick.device.DEV_3") == null, "select all cannot reach a disabled device");
        check(enabled(form, "modal.profile.submit"), "create enables once devices are selected");
        fire(form, "modal.profile.clearDevices");
        check(!selectedCheckBox(form, "pick.device.DEV_1"), "clear unchecks every device");
        check(!enabled(form, "modal.profile.submit"), "create disables again with no devices");
        fire(form, "modal.cancel");
    }

    private static void checkMacDevelopmentRemedy(CertificateWizard app) {
        Form form = app.getForm();
        fire(form, "btn.newProfile");
    }

    /// The account has no Mac development certificate and no Mac device, so this profile type is
    /// the one whose empty states have to lead somewhere. The remedy used to open the certificate
    /// dialog on a type list that did not contain the required one.
    private static void checkMacDevelopmentDeadEnd(CertificateWizard app) {
        Form form = app.getForm();
        fire(form, "pick.type.mac_app_development");
        check(find(form, "pick.cert.3") == null, "a Mac App Store certificate is not a Mac development one");
        check(find(form, "btn.profileNeedsCert") != null, "the missing certificate is called out");
        // And the certificate chosen under the previous type went with it. With a bundle
        // picked, the certificate is the ONLY thing still missing -- so if one left behind
        // by the type change were still in the request, canCreateProfile would see a
        // non-empty list of certificate IDs and enable Create with no certificate row
        // selected, sending Apple one that cannot sign this profile type.
        fire(form, "pick.bundle.BID_MAC");
        check(selectedChoice(form, "pick.bundle.BID_MAC"), "the Mac bundle ID is selectable here");
        check(!enabled(form, "modal.profile.submit"),
                "no certificate can sign this type, so create stays disabled");
        check(requirementText(form).contains("certificate"),
                "and the missing certificate is what it asks for: " + requirementText(form));
        check(find(form, "pick.device.DEV_1") == null, "an iPhone is not offered for a Mac profile");
        // and says so, rather than the section simply not being drawn -- which is the reading
        // that would make the check above pass for the wrong reason
        check(findTextContaining(form, "No enabled Mac devices") != null,
                "the empty device list names the platform it wanted");
        fire(form, "btn.profileNeedsCert");
    }

    private static void checkCertificateDialogOpensOnTheNeededType(CertificateWizard app) {
        Form form = app.getForm();
        check(find(form, "modal.generateCert.submit") != null, "the remedy opens the certificate dialog");
        check(selectedSegment(form, "pick.certType.mac_app_development"),
                "and opens it on the type the profile actually needs");
        String suggested = text(form, "field.certName");
        check(suggested.contains("MAC APP DEVELOPMENT"),
                "the suggested name describes that type, not the dialog's old default");
        fire(form, "pick.certType.ios_development");
        check(!text(form, "field.certName").equals(suggested),
                "and follows the type when it changes, instead of labelling one kind of "
                        + "certificate with the name of another");
        setText(form, "field.certName", "My own name");
        fire(form, "pick.certType.ios_distribution");
        check("My own name".equals(text(form, "field.certName")),
                "but stops following once the user has written their own");
        fire(form, "modal.cancel");
    }

    private static void checkRemainingSections(CertificateWizard app) {
        Form form = app.getForm();
        fire(form, "nav.apns");
        check(find(form, "btn.addApns") != null, "APNs add action present");
        fire(form, "nav.mac");
        check(find(form, "btn.macAppStore") != null, "Mac App Store setup action present");
        check(find(form, "btn.macDeveloperId") != null, "Mac Developer ID setup action present");
        check(find(form, "btn.installMacCert.3") != null, "Mac certificate install action present");
        check(find(form, "btn.installMacProfile.3") != null, "Mac profile install action present");
        fire(form, "nav.android");
        // Generating a keystore writes it into a project's settings, so the form is only offered
        // once the wizard knows which project that is. This harness runs without a binding, which
        // is the case that has to explain itself rather than show an inert form.
        check(find(form, "field.androidAlias") == null,
                "Android keystore form is withheld without a project binding");
        check(find(form, "field.androidDname") == null, "Android raw distinguished-name field removed");
        check(messageText(form).length() > 0, "Android page explains why it cannot install");
        fire(form, "nav.windows");
        check(find(form, "btn.windowsDocs") != null, "Windows signing docs action present");
        fire(form, "nav.maintenance");
        check(find(form, "btn.clearSigningData") != null, "clear signing data action present");
        fire(form, "btn.clearSigningData");
        check(find(form, "modal.confirm") != null, "clear signing data requires confirmation");
        check(findTextContaining(form, "cannot be undone") != null,
                "clear signing data warning explains irreversible operation");
        fire(form, "modal.cancel");
        form = app.getForm();
        fire(form, "nav.credential");
        check(find(form, "btn.saveCredential") != null, "credential save action present");
        check(find(form, "btn.deleteCredential") != null, "credential delete action present");
        check(find(form, "btn.openAscApiKeys") != null, "App Store Connect API key link present");
        check(find(form, "btn.importAscP8") != null, ".p8 import action present");
        check(find(form, "field.ascKeyId") != null, "ASC Key ID field present");
        check(find(form, "field.ascIssuerId") != null, "ASC Issuer ID field present");
        check(find(form, "field.ascP8") != null, "ASC .p8 field present");
        check(hasCommand(form, "Refresh"), "native menu Refresh command present");
        check(hasCommand(form, "Auto Setup"), "native menu Auto Setup command present");
        check(hasCommand(form, "ASC API Key"), "native menu API key command present");
        check(hasCommand(form, "Mac Signing"), "native menu Mac command present");
        check(hasCommand(form, "Android Signing"), "native menu Android command present");
        check(hasCommand(form, "Windows Signing"), "native menu Windows command present");
        check(hasCommand(form, "Clear Signing Data"), "native menu clear signing data command present");
        check(hasCommand(form, "Toggle Dark Mode"), "native menu dark mode command present");
        check(hasCommand(form, "Open App Store Connect API Keys"), "native menu help link command present");
        check(CertificateWizard.isEdtErrorHandlerInstalledForTesting(), "EDT error handler installed");
        check(CertificateWizard.isNetworkErrorHandlerInstalledForTesting(), "network error handler installed");

        runCommand(form, "Reset Font Size");
        form = app.getForm();
        Component save = find(form, "btn.saveCredential");
        int before = fontSize(save);
        runCommand(form, "Increase Font Size");
        form = app.getForm();
        save = find(form, "btn.saveCredential");
        int after = fontSize(save);
        check(after > before, "Increase Font Size command grows button font");
        check(save.getUnselectedStyle().getAlignment() == Component.CENTER, "button text is centered");
        fire(form, "nav.certificates");
        form = app.getForm();
        Component install = find(form, "btn.installCert.1");
        check(fontSize(install) > before, "increased font size survives navigation");
        runCommand(form, "Reset Font Size");

        app.showUnhandledEdtError(new RuntimeException("Synthetic EDT failure"));
        form = app.getForm();
        check(messageText(form).contains("Synthetic EDT failure"), "EDT errors render inline");
        Component edtError = find(form, "page.message");
        check(edtError instanceof SpanLabel && ((SpanLabel)edtError).isTextSelectionEnabled(),
                "EDT error text is selectable");
    }

    /// A second wizard, opened the way the IDE opens it: bound to a project whose package
    /// name is one of the mock account's App IDs. Everything above runs unbound, which is
    /// the case where the wizard knows nothing about the project and has to offer
    /// everything.
    private static CertificateWizard openProjectBoundWizard() {
        try {
            java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"),
                    "cn1-certificatewizard-harness");
            dir.mkdirs();
            java.io.File settings = new java.io.File(dir, "codenameone_settings.properties");
            write(settings, "codename1.packageName=com.example.myapp\n"
                    + "codename1.displayName=My App\n");
            java.io.File binding = new java.io.File(dir, "binding.properties");
            write(binding, "projectDir=" + dir.getAbsolutePath() + "\n"
                    + "settings=" + settings.getAbsolutePath() + "\n");
            System.setProperty("certificatewizard.input", binding.getAbsolutePath());
        } catch (Exception ex) {
            check(false, "the harness could not write a project binding: " + ex);
        }
        CertificateWizard.setServiceForTesting(new MockSigningService());
        CertificateWizard bound = new CertificateWizard();
        bound.runApp();
        return bound;
    }

    private static void write(java.io.File f, String content) throws java.io.IOException {
        java.io.OutputStream out = new java.io.FileOutputStream(f);
        try {
            out.write(content.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /// The new-profile dialog for a project the wizard knows the bundle ID of.
    ///
    /// The account holds three App IDs and only two of them belong to this project, which
    /// used to be a list to search rather than a choice to make (issue #5654). The
    /// project's own App ID and the single certificate that can sign an App Store profile
    /// start chosen, so the dialog opens ready to create rather than opening disabled.
    private static void checkProjectScopedProfileDialog(CertificateWizard app) {
        Form form = app.getForm();
        check(find(form, "modal.profile.submit") != null, "profile modal opens for a bound project");
        check(find(form, "pick.bundle.BID_A1") != null, "the project's own bundle ID is offered");
        check(find(form, "pick.bundle.BID_B2") == null,
                "an unrelated bundle ID is not offered by default");
        check(selectedChoice(form, "pick.bundle.BID_A1"),
                "the project's bundle ID starts selected");
        check(selectedChoice(form, "pick.cert.1"),
                "the one certificate that can sign this profile type starts selected");
        check(enabled(form, "modal.profile.submit"),
                "so the dialog opens ready to create rather than disabled");
        Component showAll = find(form, "btn.profileShowAllBundles");
        check(showAll != null, "the rest of the account is one click away");
        fire(form, "btn.profileShowAllBundles");
        check(find(form, "pick.bundle.BID_B2") != null, "and shows every bundle ID when asked");
        check(selectedChoice(form, "pick.bundle.BID_A1"),
                "widening the list does not lose the selection");
        check(find(form, "btn.profileShowProjectBundles") != null, "and the narrowing is reversible");
        // Narrowing the list again has to take the selection with it. A hidden bundle left
        // selected is a choice nothing on screen accounts for, and Create submitted it.
        fire(form, "pick.bundle.BID_B2");
        check(selectedChoice(form, "pick.bundle.BID_B2"), "an unrelated bundle can still be chosen");
        fire(form, "btn.profileShowProjectBundles");
        check(find(form, "pick.bundle.BID_B2") == null, "narrowing hides the unrelated bundle");
        check(selectedChoice(form, "pick.bundle.BID_A1"),
                "and moves the selection back to a row that is on screen");
        // The account holds the same identifier registered for both platforms. A default
        // chosen platform-neutrally picked the iOS one and kept it when the type became a
        // Mac one, so Create sent Apple an App ID that cannot carry the profile.
        fire(form, "pick.type.mac_app_store");
        check(selectedChoice(form, "pick.bundle.BID_MAC"),
                "a Mac profile type selects the macOS registration of the identifier");
        // Not merely deselected: an App ID registered for iOS cannot carry a Mac profile,
        // and while the row was on screen it could be picked outright -- nothing refused
        // the selection and Create submitted it.
        check(find(form, "pick.bundle.BID_A1") == null,
                "and stops offering the iOS one at all");
        check(find(form, "pick.bundle.BID_B2") == null,
                "no iOS App ID is offered for a Mac profile, this project's or anyone's");
        fire(form, "pick.type.ios_app_store");
        check(selectedChoice(form, "pick.bundle.BID_A1"),
                "and back again when the type returns to iOS");
        fire(form, "modal.cancel");
    }

    private static boolean selectedSegment(Container root, String name) {
        Component c = find(root, name);
        return c != null && c.getUIID() != null && c.getUIID().endsWith("CWSegmentSelected");
    }

    private static boolean selectedChoice(Container root, String name) {
        Component c = find(root, name);
        return c != null && c.getUIID() != null && c.getUIID().endsWith("CWChoiceSelected");
    }

    private static boolean selectedCheckBox(Container root, String name) {
        Component c = find(root, name);
        return c instanceof com.codename1.ui.CheckBox && ((com.codename1.ui.CheckBox)c).isSelected();
    }

    private static boolean enabled(Container root, String name) {
        Component c = find(root, name);
        return c != null && c.isEnabled();
    }

    private static String requirementText(Container root) {
        Component c = find(root, "modal.profile.requirement");
        return c instanceof Label ? ((Label)c).getText() : "MISSING";
    }

    private static boolean hasCommand(Form form, String commandName) {
        return findCommand(form, commandName) != null;
    }

    private static Command findCommand(Form form, String commandName) {
        java.util.Vector commands = form.getToolbar().getAllNativeMenuCommands();
        for (int i = 0; i < commands.size(); i++) {
            Object item = commands.elementAt(i);
            if (item instanceof Command && commandName.equals(((Command)item).getCommandName())) {
                return (Command)item;
            }
        }
        return null;
    }

    private static void runCommand(Form form, String commandName) {
        Command cmd = findCommand(form, commandName);
        if (cmd == null) {
            fail.add("command " + commandName + " present");
            return;
        }
        cmd.actionPerformed(new ActionEvent(cmd));
    }

    private static int fontSize(Component c) {
        if (c == null) {
            fail.add("component present for font check");
            return -1;
        }
        Font f = c.getUnselectedStyle().getFont();
        if (f == null) {
            fail.add("component font present");
            return -1;
        }
        return f.getPixelSize() > 0 ? (int)f.getPixelSize() : f.getHeight();
    }

    private static boolean selected(Container root, String name) {
        Component c = find(root, name);
        return c != null && c.getUIID() != null && c.getUIID().endsWith("CWNavSelected");
    }

    private static void fire(Container root, String name) {
        Component c = find(root, name);
        if (c instanceof Button) {
            ((Button)c).released();
        } else {
            fail.add("cannot fire " + name);
        }
    }

    private static String text(Container root, String name) {
        Component c = find(root, name);
        if (c instanceof TextField) {
            String t = ((TextField)c).getText();
            return t == null ? "" : t;
        }
        fail.add("no text field named " + name);
        return "";
    }

    private static void setText(Container root, String name, String value) {
        Component c = find(root, name);
        if (c instanceof TextField) {
            ((TextField)c).setText(value);
        } else {
            fail.add("cannot set text " + name);
        }
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
                Component out = find((Container)c, name);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    /// The inline page banner. It is a SpanLabel so a whole sentence can wrap, which findText --
    /// which only ever matches a Label -- cannot see.
    private static String messageText(Container root) {
        Component c = find(root, "page.message");
        if (c instanceof SpanLabel) {
            String t = ((SpanLabel)c).getText();
            return t == null ? "" : t;
        }
        return "";
    }

    private static Component findTextContaining(Container root, String text) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Label && ((Label)c).getText() != null && ((Label)c).getText().contains(text)) {
                return c;
            }
            if (c instanceof SpanLabel && ((SpanLabel)c).getText() != null
                    && ((SpanLabel)c).getText().contains(text)) {
                return c;
            }
            if (c instanceof Container) {
                Component out = findTextContaining((Container)c, text);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    private static Component findText(Container root, String text) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Label && text.equals(((Label)c).getText())) {
                return c;
            }
            if (c instanceof Container) {
                Component out = findText((Container)c, text);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            fail.add(msg);
        }
    }
}
