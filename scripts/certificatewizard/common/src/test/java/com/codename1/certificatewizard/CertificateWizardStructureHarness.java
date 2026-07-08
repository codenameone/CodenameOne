package com.codename1.certificatewizard;

import com.codename1.certificatewizard.api.MockSigningService;
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
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                runChecks(app[0]);
            }
        });
        System.out.println("[CertificateWizardStructure] failures=" + fail.size());
        for (String f : fail) {
            System.out.println("  FAIL: " + f);
        }
        System.out.println("[CertificateWizardStructure] RESULT " + (fail.isEmpty() ? "OK" : "FAIL"));
        System.exit(fail.isEmpty() ? 0 : 1);
    }

    private static void runChecks(CertificateWizard app) {
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
        check(find(form, "modal.profile.submit") != null, "profile modal create action present");
        check(find(form, "pick.bundle.BID_A1") != null, "profile modal bundle choice present");
        check(find(form, "pick.cert.1") != null, "profile modal certificate choice present");
        fire(form, "pick.bundle.BID_A1");
        fire(form, "pick.cert.1");
        fire(form, "modal.cancel");
        form = app.getForm();
        fire(form, "nav.apns");
        check(find(form, "btn.addApns") != null, "APNs add action present");
        fire(form, "nav.mac");
        check(find(form, "btn.macAppStore") != null, "Mac App Store setup action present");
        check(find(form, "btn.macDeveloperId") != null, "Mac Developer ID setup action present");
        check(find(form, "btn.installMacCert.3") != null, "Mac certificate install action present");
        check(find(form, "btn.installMacProfile.3") != null, "Mac profile install action present");
        fire(form, "nav.android");
        check(find(form, "btn.androidGenerate") != null, "Android keystore generation action present");
        check(find(form, "field.androidAlias") != null, "Android alias field present");
        check(find(form, "field.androidDname") == null, "Android raw distinguished-name field removed");
        check(find(form, "field.androidCommonName") != null, "Android common name field present");
        check(find(form, "field.androidOrganization") != null, "Android organization field present");
        TextField org = (TextField)find(form, "field.androidOrganization");
        check(org != null && "MyCompany".equals(org.getText()), "Android organization placeholder avoids Codename One");
        check(find(form, "field.androidCountry") != null, "Android country field present");
        fire(form, "nav.windows");
        check(find(form, "btn.windowsDocs") != null, "Windows signing docs action present");
        fire(form, "nav.maintenance");
        check(find(form, "btn.clearSigningData") != null, "clear signing data action present");
        fire(form, "btn.clearSigningData");
        check(find(form, "modal.confirm") != null, "clear signing data requires confirmation");
        Component destructiveText = findText(form, "This deletes all cached cloud signing data for this Codename One account. It cannot be undone. Continue?");
        check(destructiveText != null, "clear signing data warning explains irreversible operation");
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
        Component edtError = findText(app.getForm(), "Synthetic EDT failure");
        check(edtError != null, "EDT errors render inline");
        check(edtError instanceof Label && ((Label)edtError).isTextSelectionEnabled(), "EDT error text is selectable");
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
