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

import com.codename1.certificatewizard.api.CloudSigningService;
import com.codename1.certificatewizard.api.MockSigningService;
import com.codename1.certificatewizard.api.SigningService;
import com.codename1.certificatewizard.api.SigningState;
import com.codename1.certificatewizard.api.WizardDecisions;
import com.codename1.certificatewizard.project.ProjectBinding;
import com.codename1.certificatewizard.project.ProjectIO;
import com.codename1.certificatewizard.project.AndroidKeystoreProvider;
import com.codename1.certificatewizard.project.SigningAssetInstaller;
import com.codename1.components.InteractionDialog;
import com.codename1.components.ToastBar;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.UIManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CertificateWizard extends Lifecycle {
    public enum Section { OVERVIEW, CREDENTIAL, CERTIFICATES, BUNDLES, DEVICES, PROFILES, APNS, MAC, ANDROID, WINDOWS, MAINTENANCE }

    private static final String ASC_API_KEYS_URL = "https://appstoreconnect.apple.com/access/integrations/api";
    private static final String PREF_DARK_MODE = "certificatewizard.darkMode";
    private static final String PREF_FONT_DELTA = "certificatewizard.fontDeltaPx";
    private static final String PREF_CREDENTIAL_CONFIGURED = "certificatewizard.credentialConfigured";
    private static final String PREF_CREDENTIAL_KEY_ID = "certificatewizard.credentialKeyId";
    private static final String PREF_CREDENTIAL_ISSUER_ID = "certificatewizard.credentialIssuerId";
    private static final String PROFILE_DEVELOPMENT = "IOS_APP_DEVELOPMENT";
    private static final String PROFILE_APP_STORE = "IOS_APP_STORE";
    private static final String PROFILE_MAC_STORE = "MAC_APP_STORE";
    private static final String PROFILE_MAC_DIRECT = "MAC_APP_DIRECT";

    private static SigningService injectedService;
    private static AndroidKeystoreProvider androidKeystoreProvider;
    private static CertificateWizard activeWizard;
    private static boolean edtErrorHandlerInstalled;
    private static boolean networkErrorHandlerInstalled;

    private SigningService service;
    private SigningState state = SigningState.empty();
    private Section section = Section.OVERVIEW;
    private Form form;
    private Container page;
    private ProjectBinding binding;
    private String userEmail = "";
    private String token = "";
    private String latestCertificatePath = "";
    private String latestCertificatePassword = "";
    private String latestProfilePath = "";
    private boolean latestAssetsDebug;
    private boolean darkMode;
    private int fontDeltaPx;
    private boolean assumedCredentialConfigured;
    private float fontPinchAccumulator = 1f;
    private final String[] tableFilters = new String[Section.values().length];
    private final int[] tableSortColumns = new int[Section.values().length];
    private final boolean[] tableSortAscending = new boolean[Section.values().length];
    private Container messageHost;
    private String pageMessage = "";
    private boolean pageMessageWarn;

    public static void setServiceForTesting(SigningService s) {
        injectedService = s;
    }

    public static void setAndroidKeystoreProvider(AndroidKeystoreProvider provider) {
        androidKeystoreProvider = provider;
    }

    public static void adjustActiveFontSizeForDesktopShortcut(int deltaPx) {
        CertificateWizard wizard = activeWizard;
        if (wizard != null) {
            CN.callSerially(() -> wizard.adjustFontSize(deltaPx));
        }
    }

    @Override
    public void runApp() {
        installEdtErrorHandler();
        initTableState();
        Toolbar.setGlobalToolbar(true);
        darkMode = Preferences.get(PREF_DARK_MODE, Boolean.TRUE.equals(CN.isDarkMode()));
        fontDeltaPx = Preferences.get(PREF_FONT_DELTA, 0);
        CN.setDarkMode(Boolean.valueOf(darkMode));
        binding = ProjectIO.loadBinding();
        userEmail = firstNonEmpty(System.getProperty("certificatewizard.user"),
                binding == null ? null : binding.user(), "Not signed in");
        token = firstNonEmpty(System.getProperty("certificatewizard.token"), binding == null ? null : binding.token(), "");
        seedCredentialState();

        if (injectedService != null) {
            service = injectedService;
        } else if ("true".equals(firstNonEmpty(System.getProperty("certificatewizard.mock"), null, "false"))) {
            service = new MockSigningService();
        } else {
            String baseUrl = firstNonEmpty(System.getProperty("certificatewizard.baseUrl"),
                    binding == null ? null : binding.baseUrl(), CloudSigningService.DEFAULT_BASE_URL);
            String out = binding == null ? null : binding.outputDir();
            service = new CloudSigningService(baseUrl, token, out);
        }

        form = new Form("Codename One Certificate Wizard", new BorderLayout()) {
            @Override
            public void keyPressed(int keyCode) {
                if (handleFontShortcut(keyCode)) {
                    return;
                }
                super.keyPressed(keyCode);
            }

            @Override
            protected boolean pinch(float scale) {
                handleFontPinch(scale);
                return true;
            }

            @Override
            protected void pinchReleased(int x, int y) {
                fontPinchAccumulator = 1f;
                super.pinchReleased(x, y);
            }
        };
        form.setUIID(uiid("CWForm"));
        form.getToolbar().setUIID(uiid("CWChrome"));
        form.getTextSelection().setEnabled(true);
        installMenuCommands();
        buildShell();
        form.show();
        reload();
    }

    private void seedCredentialState() {
        boolean configured = Preferences.get(PREF_CREDENTIAL_CONFIGURED, token != null && token.length() > 0);
        if (!configured) {
            return;
        }
        assumedCredentialConfigured = true;
        String keyId = Preferences.get(PREF_CREDENTIAL_KEY_ID, "Stored");
        String issuerId = Preferences.get(PREF_CREDENTIAL_ISSUER_ID, "");
        state = new SigningState(new SigningState.Credential(true, keyId, issuerId), null, null, null, null, null, null);
    }

    private void initTableState() {
        for (int i = 0; i < tableSortAscending.length; i++) {
            tableSortAscending[i] = true;
        }
    }

    private void installEdtErrorHandler() {
        activeWizard = this;
        if (edtErrorHandlerInstalled) {
            return;
        }
        edtErrorHandlerInstalled = true;
        networkErrorHandlerInstalled = true;
        CN.addNetworkErrorListener(evt -> {
            evt.consume();
            CertificateWizard wizard = activeWizard;
            if (wizard != null) {
                if (CN.isEdt()) {
                    wizard.handleNetworkError(evt);
                } else {
                    CN.callSerially(() -> wizard.handleNetworkError(evt));
                }
            }
        });
        CN.addEdtErrorHandler(evt -> {
            evt.consume();
            Throwable err = evt.getSource() instanceof Throwable ? (Throwable)evt.getSource() : null;
            if (err != null) {
                Log.e(err);
            }
            CertificateWizard wizard = activeWizard;
            if (wizard != null) {
                wizard.showUnhandledEdtError(err);
            }
        });
    }

    void showUnhandledEdtError(Throwable err) {
        String message = "An internal application error occurred.";
        if (err != null) {
            message = friendlyMessage(err);
        }
        showPageMessage(message, true);
    }

    public Form getForm() {
        return form;
    }

    public SigningState getState() {
        return state;
    }

    public Section getSection() {
        return section;
    }

    static boolean isEdtErrorHandlerInstalledForTesting() {
        return edtErrorHandlerInstalled;
    }

    static boolean isNetworkErrorHandlerInstalledForTesting() {
        return networkErrorHandlerInstalled;
    }

    @Override
    protected void handleNetworkError(NetworkEvent err) {
        err.consume();
        if (err.getError() != null) {
            Log.e(err.getError());
        }
        String message = err.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = err.getError() == null ? null : err.getError().getMessage();
        }
        if (message == null || message.trim().isEmpty()) {
            message = "Network request failed";
        }
        if (err.getResponseCode() > 0) {
            message = "Codename One cloud request failed (HTTP " + err.getResponseCode() + "): " + message;
        }
        showPageMessage(message, true);
    }

    public void refreshUI() {
        buildShell();
    }

    private void buildShell() {
        form.removeAll();
        form.setUIID(uiid("CWForm"));
        form.getToolbar().setUIID(uiid("CWChrome"));
        form.add(BorderLayout.NORTH, topBar());
        form.add(BorderLayout.WEST, sidebar());
        page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID(uiid("CWPage"));
        form.add(BorderLayout.CENTER, page);
        renderPage();
        applyFontScale(form);
        form.revalidate();
    }

    private Container topBar() {
        Container bar = new Container(new BorderLayout());
        bar.setUIID(uiid("CWChrome"));
        Container left = new Container(new FlowLayout(Component.LEFT));
        Label logo = new Label("Codename One");
        logo.setUIID(uiid("CWLogo"));
        logo.setTextSelectionEnabled(true);
        Label title = new Label("Signing");
        title.setUIID(uiid("CWTitle"));
        title.setTextSelectionEnabled(true);
        left.add(logo).add(title);
        Container right = new Container(new FlowLayout(Component.RIGHT));
        right.setUIID(uiid("CWToolbarActions"));
        Button status = new Button(state.credential.configured() ? "ASC API Key configured" : "ASC API Key missing");
        status.setUIID(uiid(state.credential.configured() ? "CWStatus" : "CWStatusOff"));
        status.setName("pill.credential");
        status.addActionListener(e -> go(Section.CREDENTIAL));
        Label email = new Label(userEmail);
        email.setUIID(uiid("CWEmail"));
        email.setTextSelectionEnabled(true);
        Button refresh = iconButton(FontImage.MATERIAL_REFRESH, "Refresh", "btn.refresh");
        refresh.addActionListener(e -> reload());
        Button dark = iconButton(darkMode ? FontImage.MATERIAL_WB_SUNNY : FontImage.MATERIAL_BRIGHTNESS_3,
                darkMode ? "Switch to light mode" : "Switch to dark mode", "toggle.darkMode");
        dark.setUIID(uiid("CWDarkToggle"));
        dark.addActionListener(e -> setDarkMode(!darkMode));
        right.add(status);
        if (state.credential.configured()) {
            Button auto = button("Auto Setup", "btn.autoSetup", "CWStatusOff");
            FontImage.setMaterialIcon(auto, FontImage.MATERIAL_BOLT, 2.6f);
            auto.addActionListener(e -> autoSetupCurrentProject());
            right.add(auto);
        }
        right.add(email).add(dark).add(refresh);
        bar.add(BorderLayout.WEST, left);
        bar.add(BorderLayout.EAST, right);
        return bar;
    }

    private Container sidebar() {
        Container side = new Container(BoxLayout.y());
        side.setUIID(uiid("CWSidebar"));
        label(side, "WORKSPACE", "CWNavLabel");
        nav(side, Section.OVERVIEW, FontImage.MATERIAL_DASHBOARD, "Overview", null);
        nav(side, Section.CREDENTIAL, FontImage.MATERIAL_VPN_KEY, "ASC API Key", null);
        label(side, "APPLE SIGNING", "CWNavLabel");
        nav(side, Section.CERTIFICATES, FontImage.MATERIAL_CARD_MEMBERSHIP, "Certificates", state.certificates.size());
        nav(side, Section.BUNDLES, FontImage.MATERIAL_APPS, "Bundle IDs", state.bundleIds.size());
        nav(side, Section.DEVICES, FontImage.MATERIAL_PHONE_IPHONE, "Devices", state.devices.size());
        nav(side, Section.PROFILES, FontImage.MATERIAL_DESCRIPTION, "Profiles", state.profiles.size());
        label(side, "PUSH", "CWNavLabel");
        nav(side, Section.APNS, FontImage.MATERIAL_NOTIFICATIONS, "APNs Keys", state.apnsKeys.size());
        label(side, "OTHER PLATFORMS", "CWNavLabel");
        nav(side, Section.MAC, FontImage.MATERIAL_LAPTOP_MAC, "Mac", null);
        nav(side, Section.ANDROID, FontImage.MATERIAL_ANDROID, "Android", null);
        nav(side, Section.WINDOWS, FontImage.MATERIAL_DESKTOP_WINDOWS, "Windows", null);
        label(side, "MAINTENANCE", "CWNavLabel");
        nav(side, Section.MAINTENANCE, FontImage.MATERIAL_DELETE_SWEEP, "Clear Signing Data", null);
        return side;
    }

    private void nav(Container side, Section target, char icon, String text, Integer count) {
        Button b = new Button(count == null ? text : text + "  " + count);
        b.setName("nav." + target.name().toLowerCase());
        b.setUIID(uiid(section == target ? "CWNavSelected" : "CWNav"));
        FontImage.setMaterialIcon(b, icon, 3.2f);
        b.addActionListener(e -> go(target));
        side.add(b);
    }

    private void renderPage() {
        page.removeAll();
        switch (section) {
            case CREDENTIAL -> credentialPage();
            case CERTIFICATES -> certificatesPage();
            case BUNDLES -> bundlesPage();
            case DEVICES -> devicesPage();
            case PROFILES -> profilesPage();
            case APNS -> apnsPage();
            case MAC -> macPage();
            case ANDROID -> androidPage();
            case WINDOWS -> windowsPage();
            case MAINTENANCE -> maintenancePage();
            default -> overviewPage();
        }
    }

    private void overviewPage() {
        pageHead("Overview", "Manage signing assets for Apple, Android and desktop builds from the current project.");
        if (!state.credential.configured()) {
            Container b = new Container(new BorderLayout());
            b.setUIID(uiid("CWBanner"));
            Container text = new Container(BoxLayout.y());
            label(text, "Connect your App Store Connect API key to begin", "CWCardTitle");
            Label copy = new Label("Certificates, bundle IDs, devices and profiles all talk to Apple through this key.");
            copy.setUIID(uiid("CWCardMeta"));
            copy.setTextSelectionEnabled(true);
            text.add(copy);
            b.add(BorderLayout.CENTER, text);
            Button connect = primary("Connect key", "btn.connectKey");
            connect.addActionListener(e -> go(Section.CREDENTIAL));
            b.add(BorderLayout.EAST, connect);
            page.add(b);
        } else if (state.expiringCount(System.currentTimeMillis()) > 0) {
            banner(state.expiringCount(System.currentTimeMillis()) + " signing asset(s) expire within 30 days.", true);
        }
        label(page, "SETUP STATUS", "CWNavLabel");
        Container setup = new Container(new GridLayout(1, 3));
        setupCard(setup, "ASC API Key", state.credential.configured() ? "Connected" : "Not configured",
                state.credential.configured() ? state.credential.keyId() : "-");
        setupCard(setup, "Apple distribution certificate",
                state.certificates.isEmpty() ? "None active" : "Ready",
                state.certificates.isEmpty() ? "-" : state.certificates.get(0).displayName());
        setupCard(setup, "App Store profile",
                state.profiles.isEmpty() ? "None yet" : "Ready",
                state.profiles.isEmpty() ? "-" : state.profiles.get(0).name());
        page.add(setup);
        label(page, "YOUR ASSETS", "CWNavLabel");
        Container metrics = new Container(new GridLayout(1, 4));
        metric(metrics, "" + state.certificates.size(), "Certificates", () -> go(Section.CERTIFICATES));
        metric(metrics, "" + state.bundleIds.size(), "Bundle IDs", () -> go(Section.BUNDLES));
        metric(metrics, "" + state.devices.size(), "Devices", () -> go(Section.DEVICES));
        metric(metrics, "" + state.profiles.size(), "Profiles", () -> go(Section.PROFILES));
        page.add(metrics);
        Button profile = primary("New profile", "btn.newProfile");
        profile.addActionListener(e -> newProfileDialog());
        Button cert = outline("Generate certificate", "btn.generateCert");
        cert.addActionListener(e -> certificateDialog());
        Button sync = outline("Sync with Apple", "btn.reconcile");
        sync.addActionListener(e -> service.reconcile(r -> afterMutation(r, "Synced with Apple")));
        page.add(actionRow(Component.LEFT, profile, cert, sync));
    }

    private void setupCard(Container parent, String title, String meta, String value) {
        Container c = card();
        label(c, title, "CWCardTitle");
        label(c, meta, "CWCardMeta");
        label(c, value, "CWCellMain");
        parent.add(c);
    }

    private void credentialPage() {
        pageHead("ASC API Key", "The .p8 key is stored by the cloud service and is never returned by the API.");
        Container instructions = card();
        label(instructions, "Create an App Store Connect API key", "CWCardTitle");
        label(instructions, "1. Open Users and Access > Integrations > App Store Connect API.", "CWCardMeta");
        label(instructions, "2. If prompted, request API access and accept Apple's terms.", "CWCardMeta");
        label(instructions, "3. On Team Keys, generate a key with Admin access.", "CWCardMeta");
        label(instructions, "4. Copy the Issuer ID and Key ID, then download the .p8 file. Apple only allows one download.", "CWCardMeta");
        Button openAsc = primary("Create .p8 key in App Store Connect", "btn.openAscApiKeys");
        openAsc.addActionListener(e -> openAscApiKeys());
        instructions.add(openAsc);
        page.add(instructions);

        Container card = card();
        if (state.credential.configured()) {
            row(card, "Key ID", state.credential.keyId(), null);
            row(card, "Issuer ID", state.credential.issuerId(), null);
        } else {
            label(card, "No App Store Connect API key stored.", "CWCardMeta");
        }
        TextField key = field("Key ID", "ABCD1234EF");
        key.setName("field.ascKeyId");
        TextField issuer = field("Issuer ID", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
        issuer.setName("field.ascIssuerId");
        TextArea p8 = new TextArea("");
        p8.setHint("-----BEGIN PRIVATE KEY-----");
        prepareP8TextArea(p8, 6);
        p8.setName("field.ascP8");
        label(card, "Key ID", "CWFieldLabel");
        label(card, "Shown in the generated key row in App Store Connect.", "CWCardMeta");
        card.add(key);
        label(card, "Issuer ID", "CWFieldLabel");
        label(card, "Shown above the Team Keys table on the App Store Connect API page.", "CWCardMeta");
        card.add(issuer);
        label(card, "Private key .p8", "CWFieldLabel");
        label(card, "Import the downloaded .p8 file or paste its full contents, including BEGIN and END PRIVATE KEY.", "CWCardMeta");
        Button importP8 = outline("Import .p8 file", "btn.importAscP8");
        importP8.addActionListener(e -> importP8(p8));
        card.add(importP8);
        card.add(p8);
        Button save = primary("Store key", "btn.saveCredential");
        save.addActionListener(e -> service.saveCredential(key.getText(), issuer.getText(), p8.getText(),
                r -> {
                    if (r.ok) {
                        state = new SigningState(new SigningState.Credential(true, key.getText(), issuer.getText()),
                                state.certificates, state.bundleIds, state.devices, state.profiles, state.apnsKeys,
                                state.appGroups);
                        storeCredentialState();
                    }
                    afterMutation(r, "Credential stored");
                }));
        Button del = danger("Delete key", "btn.deleteCredential");
        del.addActionListener(e -> confirm("Delete API key", "Remove the stored App Store Connect API key from the cloud service?",
                "Delete", () -> service.deleteCredential(r -> {
                    if (r.ok) {
                        state = new SigningState(new SigningState.Credential(false, null, null),
                                state.certificates, state.bundleIds, state.devices, state.profiles, state.apnsKeys,
                                state.appGroups);
                        storeCredentialState();
                    }
                    afterMutation(r, "Credential deleted");
                })));
        card.add(actionRow(Component.LEFT, save, del));
        page.add(card);
    }

    private void installMenuCommands() {
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Refresh", Command.DESKTOP_MENU_FILE, 'R', () -> reload()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Auto Setup", Command.DESKTOP_MENU_FILE, 'A', () -> autoSetupCurrentProject()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("ASC API Key", Command.DESKTOP_MENU_VIEW, 'K', () -> go(Section.CREDENTIAL)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Overview", Command.DESKTOP_MENU_VIEW, '1', () -> go(Section.OVERVIEW)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Certificates", Command.DESKTOP_MENU_VIEW, '2', () -> go(Section.CERTIFICATES)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Profiles", Command.DESKTOP_MENU_VIEW, '3', () -> go(Section.PROFILES)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Mac Signing", Command.DESKTOP_MENU_VIEW, '4', () -> go(Section.MAC)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Android Signing", Command.DESKTOP_MENU_VIEW, '5', () -> go(Section.ANDROID)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Windows Signing", Command.DESKTOP_MENU_VIEW, '6', () -> go(Section.WINDOWS)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Clear Signing Data", Command.DESKTOP_MENU_FILE, 'L', () -> go(Section.MAINTENANCE)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Toggle Dark Mode", Command.DESKTOP_MENU_VIEW, 'D', () -> setDarkMode(!darkMode)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Increase Font Size", Command.DESKTOP_MENU_VIEW, '+', () -> adjustFontSize(2)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Decrease Font Size", Command.DESKTOP_MENU_VIEW, '-', () -> adjustFontSize(-2)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Reset Font Size", Command.DESKTOP_MENU_VIEW, '0', () -> resetFontSize()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Open App Store Connect API Keys", Command.DESKTOP_MENU_HELP, 'O', () -> openAscApiKeys()));
    }

    private Command menuCommand(String name, String menu, char shortcut, Runnable action) {
        Command cmd = new Command(name) {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                action.run();
            }
        };
        cmd.setDesktopMenu(menu);
        cmd.setDesktopShortcut(shortcut);
        return cmd;
    }

    private void openAscApiKeys() {
        Display.getInstance().execute(ASC_API_KEYS_URL);
    }

    private void importP8(TextArea target) {
        CN.openFileChooser(e -> {
            if (e == null || e.getSource() == null) {
                return;
            }
            String path = (String)e.getSource();
            InputStream in = null;
            try {
                in = FileSystemStorage.getInstance().openInputStream(path);
                String contents = Util.readToString(in, "UTF-8").trim();
                if (contents.length() == 0) {
                    ToastBar.showErrorMessage("The selected .p8 file is empty");
                    return;
                }
                target.setText(contents);
                target.repaint();
                form.revalidate();
                ToastBar.showMessage("Loaded .p8 file", FontImage.MATERIAL_CHECK);
            } catch (Exception ex) {
                Log.e(ex);
                ToastBar.showErrorMessage("Failed to read .p8 file: " + ex.getMessage());
            } finally {
                Util.cleanup(in);
            }
        }, "p8");
    }

    private String friendlyMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null || t.getMessage().trim().isEmpty() ? t.getClass().getSimpleName() : t.getMessage().trim();
    }

    private void setDarkMode(boolean dark) {
        if (darkMode == dark) {
            return;
        }
        darkMode = dark;
        CN.setDarkMode(Boolean.valueOf(darkMode));
        Preferences.set(PREF_DARK_MODE, darkMode);
        UIManager.getInstance().refreshTheme();
        buildShell();
        form.refreshTheme(false);
        applyFontScale(form);
        form.revalidate();
    }

    private void adjustFontSize(int deltaPx) {
        fontDeltaPx += deltaPx;
        if (fontDeltaPx > 8) {
            fontDeltaPx = 8;
        } else if (fontDeltaPx < -4) {
            fontDeltaPx = -4;
        }
        Preferences.set(PREF_FONT_DELTA, fontDeltaPx);
        buildShell();
        form.revalidate();
    }

    private boolean handleFontShortcut(int keyCode) {
        Display display = Display.getInstance();
        if (!display.isControlKeyDown() && !display.isMetaKeyDown()) {
            return false;
        }
        if (keyCode == '+' || keyCode == '=') {
            adjustFontSize(2);
            return true;
        }
        if (keyCode == '-' || keyCode == '_') {
            adjustFontSize(-2);
            return true;
        }
        return false;
    }

    private void handleFontPinch(float scale) {
        if (scale <= 0) {
            return;
        }
        fontPinchAccumulator *= scale;
        while (fontPinchAccumulator >= 1.14f) {
            adjustFontSize(2);
            fontPinchAccumulator /= 1.14f;
        }
        while (fontPinchAccumulator <= 0.88f) {
            adjustFontSize(-2);
            fontPinchAccumulator /= 0.88f;
        }
    }

    private void resetFontSize() {
        fontDeltaPx = 0;
        Preferences.set(PREF_FONT_DELTA, fontDeltaPx);
        buildShell();
        form.revalidate();
    }

    private void applyFontScale(Component c) {
        if (fontDeltaPx == 0 || c == null) {
            return;
        }
        applyScaledFont(c);
        if (c instanceof TextArea) {
            applyScaledFont(((TextArea)c).getHintLabel());
        }
        if (c instanceof Container) {
            Container cnt = (Container)c;
            for (int iter = 0; iter < cnt.getComponentCount(); iter++) {
                applyFontScale(cnt.getComponentAt(iter));
            }
        }
    }

    private void applyScaledFont(Component c) {
        if (c == null) {
            return;
        }
        float baseMm = baseFontMm(c.getUIID());
        int size = Display.getInstance().convertToPixels(baseMm) + fontDeltaPx;
        if (size < 8) {
            size = 8;
        }
        boolean bold = isBoldUiid(c.getUIID());
        Font font = nativeFont(bold ? CN.NATIVE_MAIN_BOLD : CN.NATIVE_MAIN_REGULAR, size, bold ? Font.STYLE_BOLD : Font.STYLE_PLAIN);
        if (font != null) {
            c.getAllStyles().setFont(font);
        }
    }

    private Font nativeFont(String nativeName, int sizePx, int style) {
        try {
            Font base = Font.createTrueTypeFont(nativeName, nativeName);
            if (base != null) {
                return base.derive(sizePx, style);
            }
        } catch (Exception ex) {
            Log.e(ex);
        }
        Font fallback = Font.getDefaultFont();
        if (fallback != null && fallback.isTTFNativeFont()) {
            try {
                return fallback.derive(sizePx, style);
            } catch (Exception ex) {
                Log.e(ex);
            }
        }
        return null;
    }

    private float baseFontMm(String uiid) {
        String id = stripDark(uiid);
        if ("CWMetricNumber".equals(id)) {
            return 5.2f;
        }
        if ("CWPageTitle".equals(id)) {
            return 4.5f;
        }
        if ("CWModalTitle".equals(id)) {
            return 3.4f;
        }
        if ("CWLogo".equals(id)) {
            return 3.1f;
        }
        if ("CWTitle".equals(id)) {
            return 3.0f;
        }
        if ("CWCardTitle".equals(id)) {
            return 2.8f;
        }
        if ("CWNav".equals(id) || "CWNavSelected".equals(id) || "CWCellMain".equals(id)) {
            return 2.65f;
        }
        if ("CWSub".equals(id)) {
            return 2.55f;
        }
        if ("CWStatus".equals(id) || "CWStatusOff".equals(id) || "CWEmail".equals(id)
                || "CWPrimary".equals(id) || "CWAccent".equals(id) || "CWOutline".equals(id)
                || "CWDanger".equals(id) || "CWDarkToggle".equals(id)) {
            return 2.45f;
        }
        if ("CWCardMeta".equals(id) || "CWMetricLabel".equals(id) || "CWFieldLabel".equals(id)
                || "CWSegment".equals(id) || "CWSegmentSelected".equals(id)) {
            return 2.35f;
        }
        if ("CWCellSub".equals(id)) {
            return 2.25f;
        }
        if ("CWPillOk".equals(id) || "CWPillWarn".equals(id) || "CWPillBad".equals(id)
                || "CWPillMuted".equals(id)) {
            return 2.2f;
        }
        if ("CWNavLabel".equals(id) || "CWTableHeader".equals(id)) {
            return 2.1f;
        }
        return 2.75f;
    }

    private boolean isBoldUiid(String uiid) {
        String id = stripDark(uiid);
        return id.indexOf("Title") > -1 || id.indexOf("Logo") > -1 || id.indexOf("Status") > -1
                || id.indexOf("Selected") > -1 || id.indexOf("Primary") > -1 || id.indexOf("Accent") > -1
                || id.indexOf("Outline") > -1 || id.indexOf("Danger") > -1 || id.indexOf("Pill") > -1
                || id.indexOf("Segment") > -1 || id.indexOf("CellMain") > -1
                || "CWNavLabel".equals(id) || "CWTableHeader".equals(id) || "CWMetricNumber".equals(id)
                || "CWDarkToggle".equals(id);
    }

    private String stripDark(String uiid) {
        if (uiid != null && uiid.startsWith("DarkCW")) {
            return uiid.substring(4);
        }
        return uiid == null ? "" : uiid;
    }

    private void prepareP8TextArea(TextArea p8, int rows) {
        p8.setRows(rows);
        p8.setGrowByContent(false);
        p8.setTextSelectionEnabled(true);
        p8.getHintLabel().setUIID(uiid("CWFieldHint"));
        p8.setUIID(uiid("CWField"));
    }

    private void certificatesPage() {
        pageHead("Certificates", "Generate, sync, install, and revoke iOS signing certificates.");
        Button add = primary("Generate", "btn.generateCert");
        add.addActionListener(e -> certificateDialog());
        Button sync = outline("Sync with Apple", "btn.reconcile");
        sync.addActionListener(e -> service.reconcile(r -> afterMutation(r, "Synced with Apple")));
        page.add(actionRow(Component.LEFT, add, sync));
        Container table = card();
        Container body = tableBody(Section.CERTIFICATES);
        table.add(filterRow(Section.CERTIFICATES, body));
        populateTableBody(Section.CERTIFICATES, body);
        table.add(body);
        page.add(table);
    }

    private void bundlesPage() {
        pageHead("Bundle IDs", "Register app identifiers and enable capabilities.");
        Button add = primary("Register bundle ID", "btn.addBundle");
        add.addActionListener(e -> bundleDialog(null, null));
        page.add(actionRow(Component.LEFT, add));
        Container table = card();
        Container body = tableBody(Section.BUNDLES);
        table.add(filterRow(Section.BUNDLES, body));
        populateTableBody(Section.BUNDLES, body);
        table.add(body);
        page.add(table);
    }

    private void devicesPage() {
        pageHead("Devices", "Register devices for development and ad-hoc provisioning profiles.");
        Button add = primary("Register device", "btn.addDevice");
        add.addActionListener(e -> deviceDialog());
        page.add(actionRow(Component.LEFT, add));
        Container table = card();
        Container body = tableBody(Section.DEVICES);
        table.add(filterRow(Section.DEVICES, body));
        populateTableBody(Section.DEVICES, body);
        table.add(body);
        page.add(table);
    }

    private void profilesPage() {
        pageHead("Provisioning Profiles", "Create and install fresh .mobileprovision files.");
        Button add = primary("New profile", "btn.newProfile");
        add.addActionListener(e -> newProfileDialog());
        page.add(actionRow(Component.LEFT, add));
        Container table = card();
        Container body = tableBody(Section.PROFILES);
        table.add(filterRow(Section.PROFILES, body));
        populateTableBody(Section.PROFILES, body);
        table.add(body);
        page.add(table);
    }

    private void apnsPage() {
        pageHead("APNs Auth Keys", "Store token-based push notification keys for later use.");
        Button add = primary("Add key", "btn.addApns");
        add.addActionListener(e -> apnsDialog());
        page.add(actionRow(Component.LEFT, add));
        Container table = card();
        Container body = tableBody(Section.APNS);
        table.add(filterRow(Section.APNS, body));
        populateTableBody(Section.APNS, body);
        table.add(body);
        page.add(table);
    }

    private void macPage() {
        pageHead("Mac Signing", "Create Apple signing assets for Mac App Store, Developer ID and notarized builds.");
        Container info = card();
        label(info, "Mac native builds use Apple signing assets that are separate from iOS certificates.", "CWCardMeta");
        label(info, "Use Developer ID for direct distribution and Mac App Distribution/Installer for the Mac App Store.", "CWCardMeta");
        page.add(info);
        Button appStore = primary("Generate Mac App Store assets", "btn.macAppStore");
        appStore.addActionListener(e -> autoSetupMacProject(PROFILE_MAC_STORE));
        Button direct = outline("Generate Developer ID assets", "btn.macDeveloperId");
        direct.addActionListener(e -> autoSetupMacProject(PROFILE_MAC_DIRECT));
        Button cert = outline("Generate Mac certificate", "btn.macCert");
        cert.addActionListener(e -> certificateDialog());
        page.add(actionRow(Component.LEFT, appStore, direct, cert));

        Container certTable = card();
        label(certTable, "Mac certificates", "CWCardTitle");
        for (SigningState.Certificate c : state.certificates) {
            if (isMacCertificate(c.certificateType())) {
                macAssetRow(certTable, typeLabel(c.certificateType()),
                        firstNonEmpty(c.displayName(), c.appleCertId(), "-"),
                        "btn.installMacCert." + c.id(), () -> installCertificate(c));
            }
        }
        page.add(certTable);

        Container profileTable = card();
        label(profileTable, "Mac profiles", "CWCardTitle");
        for (SigningState.Profile p : state.profiles) {
            if (isMacProfile(p.profileType())) {
                macAssetRow(profileTable, profileTypeLabel(p.profileType()),
                        firstNonEmpty(p.name(), p.appleProfileId(), "-"),
                        "btn.installMacProfile." + p.id(), () -> installProfile(p));
            }
        }
        page.add(profileTable);

        if (binding != null && binding.settings() != null && !binding.settings().trim().isEmpty()) {
            Container current = card();
            label(current, "Current Mac project settings", "CWCardTitle");
            row(current, "Certificate", readSetting(binding.settings(), "codename1.mac.certificate"), null);
            row(current, "Provisioning profile", readSetting(binding.settings(), "codename1.mac.provision"), null);
            row(current, "Distribution", readSetting(binding.settings(), "codename1.arg.macNative.distribution"), null);
            page.add(current);
        }
    }

    private void androidPage() {
        pageHead("Android Signing", "Generate a local Android keystore and install it into the current project.");
        if (!canInstallIntoProject()) {
            return;
        }
        ProjectDefaults defaults = projectDefaults();
        TextField alias = field("Alias", "androidKey");
        alias.setName("field.androidAlias");
        alias.setText("androidKey");
        TextField password = field("Password", "at least 6 characters");
        password.setName("field.androidPassword");
        TextField commonName = field("Common name", androidCommonName(defaults));
        commonName.setName("field.androidCommonName");
        commonName.setText(androidCommonName(defaults));
        TextField orgUnit = field("Organizational unit", "Development");
        orgUnit.setName("field.androidOrgUnit");
        orgUnit.setText("Development");
        TextField organization = field("Organization", "MyCompany");
        organization.setName("field.androidOrganization");
        organization.setText("MyCompany");
        TextField locality = field("City/locality", "MyCity");
        locality.setName("field.androidLocality");
        locality.setText("MyCity");
        TextField stateName = field("State/province", "MyState");
        stateName.setName("field.androidState");
        stateName.setText("MyState");
        TextField country = field("Country code", "US");
        country.setName("field.androidCountry");
        country.setText("US");
        Container c = card();
        label(c, "Android uses a self-signed keystore. Back up the generated file and password.", "CWCardMeta");
        label(c, "Alias", "CWFieldLabel");
        c.add(alias);
        label(c, "Password", "CWFieldLabel");
        c.add(password);
        label(c, "Certificate details", "CWFieldLabel");
        c.add(commonName);
        c.add(twoColumn(orgUnit, organization));
        c.add(twoColumn(locality, stateName));
        c.add(country);
        Button generate = primary("Generate and install", "btn.androidGenerate");
        generate.addActionListener(e -> generateAndroidKeystore(alias.getText(), password.getText(),
                androidDistinguishedName(commonName.getText(), orgUnit.getText(), organization.getText(),
                        locality.getText(), stateName.getText(), country.getText())));
        c.add(actionRow(Component.LEFT, generate));
        page.add(c);
        Container current = card();
        label(current, "Current project settings", "CWCardTitle");
        row(current, "Keystore", readSetting(binding.settings(), "codename1.android.keystore"), null);
        row(current, "Alias", readSetting(binding.settings(), "codename1.android.keystoreAlias"), null);
        page.add(current);
    }

    private void windowsPage() {
        pageHead("Windows Signing", "Install a PKCS#12 Authenticode certificate for native Windows builds.");
        Container c = card();
        label(c, "Windows certificates are issued by a code-signing certificate authority.", "CWCardMeta");
        label(c, "Recommended routes include Azure Trusted Signing, DigiCert KeyLocker, GlobalSign GCC, or another OV/EV code-signing CA.", "CWCardMeta");
        label(c, "If you already have a .pfx/.p12 file, configure these project properties:", "CWCardMeta");
        row(c, "codename1.windows.signing.certificate", readSetting(binding == null ? null : binding.settings(),
                "codename1.windows.signing.certificate"), null);
        row(c, "codename1.windows.signing.password", readSetting(binding == null ? null : binding.settings(),
                "codename1.windows.signing.password"), null);
        row(c, "codename1.windows.signing.timestamp", firstNonEmpty(readSetting(binding == null ? null : binding.settings(),
                "codename1.windows.signing.timestamp"), "http://timestamp.digicert.com"), null);
        Button docs = primary("Open Windows signing docs", "btn.windowsDocs");
        docs.addActionListener(e -> Display.getInstance().execute("https://www.codenameone.com/manual/advanced-topics.html#_windows_native_port_only"));
        Button certs = outline("Open Azure Trusted Signing", "btn.windowsAzure");
        certs.addActionListener(e -> Display.getInstance().execute("https://azure.microsoft.com/products/trusted-signing"));
        c.add(actionRow(Component.LEFT, docs, certs));
        page.add(c);
    }

    private void maintenancePage() {
        pageHead("Clear Signing Data", "Privacy tool for deleting cached cloud signing data.");
        Container c = card();
        label(c, "This is not recommended for normal signing setup or troubleshooting. Use it only when you need to remove cached signing data from the Codename One cloud service for privacy reasons.", "CWCardMeta");
        label(c, "It clears cloud-side Apple signing cache for this account, including stored API keys, generated certificate private keys, profiles, APNs keys and notarization data.", "CWCardMeta");
        label(c, "It does not delete assets from Apple, but future cloud builds will need signing data to be regenerated or re-imported.", "CWCardMeta");
        Button clear = danger("Clear cloud signing data", "btn.clearSigningData");
        clear.addActionListener(e -> confirm("Permanently clear signing data",
                "This privacy tool deletes all cached cloud signing data for this Codename One account. It is not recommended for normal setup and cannot be undone. Continue?",
                "Clear signing data", () -> service.clearSigningData(r -> {
                    if (r.ok) {
                        state = SigningState.empty();
                        storeCredentialState();
                    }
                    afterMutation(r, "Signing data cleared");
                })));
        c.add(actionRow(Component.LEFT, clear));
        page.add(c);
    }

    private void certificateDialog() {
        InteractionDialog d = modal("Generate certificate");
        final String[] type = {"IOS_DISTRIBUTION"};
        label(d, "Certificate type", "CWFieldLabel");
        Button dist = segment("iOS Distribution", true);
        Button dev = segment("iOS Development", false);
        Button macStore = segment("Mac App Store", false);
        Button developerId = segment("Developer ID", false);
        Button installer = segment("Mac Installer", false);
        Button[] typeButtons = {dist, dev, macStore, developerId, installer};
        String[] typeValues = {"IOS_DISTRIBUTION", "IOS_DEVELOPMENT", "MAC_APP_DISTRIBUTION",
                "DEVELOPER_ID_APPLICATION", "MAC_INSTALLER_DISTRIBUTION"};
        for (int i = 0; i < typeButtons.length; i++) {
            final int typeIndex = i;
            typeButtons[i].addActionListener(e -> {
                type[0] = typeValues[typeIndex];
                updateSegmentButtons(typeButtons, typeValues, type[0]);
                d.revalidate();
            });
        }
        TextField name = field("Display name", "App Store Distribution");
        d.add(actionRow(Component.LEFT, dist, dev, macStore));
        d.add(actionRow(Component.LEFT, developerId, installer));
        label(d, "Display name", "CWFieldLabel");
        d.add(name);
        Button gen = primary("Generate", "modal.generateCert.submit");
        gen.addActionListener(e -> { d.dispose(); service.createCertificate(type[0], name.getText(), r -> afterMutation(r, "Certificate generated")); });
        addDialogActions(d, gen);
        showModal(d);
    }

    private void p12Dialog(SigningState.Certificate c) {
        InteractionDialog d = modal("Download .p12");
        label(d, "Choose a new password for this downloaded .p12 file. Save it with the file.", "CWCardMeta");
        label(d, "This is not the App Store Connect Issuer ID and it does not come from Apple.", "CWCardMeta");
        label(d, "P12 password", "CWFieldLabel");
        TextField pass = field("P12 password", "New .p12 password");
        pass.setName("modal.p12.password");
        d.add(pass);
        Button dl = primary("Download .p12", "modal.p12.submit");
        dl.addActionListener(e -> {
            d.dispose();
            service.downloadP12(c.id(), pass.getText(), safeFileName(c.displayName()) + ".p12",
                    x -> afterCertificateDownload(x, c, pass.getText()));
        });
        addDialogActions(d, dl);
        showModal(d);
    }

    private void bundleDialog(String initialIdentifier, String initialName) {
        InteractionDialog d = modal("Register bundle ID");
        TextField id = field("Identifier", "com.example.app");
        TextField name = field("Name", "My App");
        if (initialIdentifier != null) {
            id.setText(initialIdentifier);
        }
        if (initialName != null) {
            name.setText(initialName);
        }
        CheckBox push = new CheckBox("Enable Push Notifications");
        push.setUIID(uiid("CWFieldLabel"));
        CheckBox appGroups = new CheckBox("Enable App Groups (widgets / live activities)");
        appGroups.setUIID(uiid("CWFieldLabel"));
        d.add(id).add(name).add(push).add(appGroups);
        Button save = primary("Register", "modal.bundle.submit");
        save.addActionListener(e -> {
            d.dispose();
            final String bundleIdentifier = id.getText();
            final boolean withGroups = appGroups.isSelected();
            service.createBundleId(bundleIdentifier, name.getText(), push.isSelected(), r -> {
                if (!r.ok) {
                    showPageMessage(r.message, true);
                    return;
                }
                if (withGroups) {
                    enableAppGroupsForBundle(bundleIdentifier);
                } else {
                    afterMutation(r, "Bundle ID registered");
                }
            });
        });
        addDialogActions(d, save);
        showModal(d);
    }

    private void deviceDialog() {
        InteractionDialog d = modal("Register device");
        TextField name = field("Device name", "QA iPhone");
        TextField udid = field("UDID", "00008120-000A1C3E0C68201E");
        d.add(name).add(udid);
        Button save = primary("Register", "modal.device.submit");
        save.addActionListener(e -> { d.dispose(); service.registerDevice(name.getText(), udid.getText(), r -> afterMutation(r, "Device registered")); });
        addDialogActions(d, save);
        showModal(d);
    }

    private void apnsDialog() {
        InteractionDialog d = modal("Add APNs auth key");
        TextField name = field("Display name", "Production APNs");
        TextField key = field("Key ID", "A1B2C3D4E5");
        TextField team = field("Team ID", "9WQ7X2K4LM");
        TextArea p8 = new TextArea("");
        p8.setHint("-----BEGIN PRIVATE KEY-----");
        prepareP8TextArea(p8, 5);
        d.add(name).add(key).add(team);
        label(d, "Auth key .p8", "CWFieldLabel");
        Button importP8 = outline("Import .p8 file", "modal.apns.importP8");
        importP8.addActionListener(e -> importP8(p8));
        d.add(importP8);
        d.add(p8);
        Button save = primary("Store key", "modal.apns.submit");
        save.addActionListener(e -> { d.dispose(); service.saveApnsKey(key.getText(), team.getText(), p8.getText(), name.getText(), r -> afterMutation(r, "APNs key stored")); });
        addDialogActions(d, save);
        showModal(d);
    }

    private void newProfileDialog() {
        InteractionDialog d = modalFrame("New provisioning profile");
        Container content = new Container(BoxLayout.y());
        content.setScrollableY(true);
        content.setUIID(uiid("CWDialogContent"));
        d.add(BorderLayout.CENTER, content);

        final String[] profileType = {null};
        final String[] bundleId = {null};
        final String[] certificateId = {null};
        final String[] profileName = {projectDefaults().appName + " App Store"};
        final List<String> certs = new ArrayList<String>();
        final List<String> devs = new ArrayList<String>();
        final Button[] createRef = new Button[1];

        Button store = segment("iOS App Store", true);
        Button adhoc = segment("iOS Ad Hoc", false);
        Button dev = segment("iOS Development", false);
        Button macStore = segment("Mac App Store", false);
        Button macDirect = segment("Mac Direct", false);
        Button macDev = segment("Mac Development", false);
        Button[] typeButtons = {store, adhoc, dev, macStore, macDirect, macDev};
        String[] typeValues = {"IOS_APP_STORE", "IOS_APP_ADHOC", "IOS_APP_DEVELOPMENT",
                "MAC_APP_STORE", "MAC_APP_DIRECT", "MAC_APP_DEVELOPMENT"};
        label(content, "Profile type", "CWFieldLabel");
        content.add(actionRow(Component.LEFT, store, adhoc, dev));
        content.add(actionRow(Component.LEFT, macStore, macDirect, macDev));

        label(content, "Bundle ID", "CWFieldLabel");
        List<Button> bundleButtons = new ArrayList<Button>();
        for (SigningState.BundleId b : state.bundleIds) {
            Button pick = choice(b.identifier(), b.name(), false);
            pick.setName("pick.bundle." + b.id());
            bundleButtons.add(pick);
            pick.addActionListener(e -> {
                bundleId[0] = b.id().equals(bundleId[0]) ? null : b.id();
                updateChoiceButtons(bundleButtons, bundleId[0]);
                updateCreateProfileButton(createRef[0], profileType[0], bundleId[0], certs, devs, profileName[0]);
            });
            content.add(pick);
        }
        if (state.bundleIds.isEmpty()) {
            Button createBundle = outline("Register bundle ID first", "btn.profileNeedsBundle");
            createBundle.addActionListener(e -> { d.dispose(); bundleDialog(null, null); });
            content.add(createBundle);
        }

        label(content, "Certificate", "CWFieldLabel");
        List<Button> certButtons = new ArrayList<Button>();
        for (SigningState.Certificate c : state.certificates) {
            Button pick = choice(c.displayName(), typeLabel(c.certificateType()), false);
            pick.setName("pick.cert." + c.id());
            certButtons.add(pick);
            pick.addActionListener(e -> {
                certs.clear();
                String id = String.valueOf(c.id());
                if (!id.equals(certificateId[0])) {
                    certificateId[0] = id;
                    certs.add(c.appleCertId());
                } else {
                    certificateId[0] = null;
                }
                updateChoiceButtons(certButtons, certificateId[0]);
                updateCreateProfileButton(createRef[0], profileType[0], bundleId[0], certs, devs, profileName[0]);
            });
            content.add(pick);
        }
        label(content, "Devices", "CWFieldLabel");
        for (SigningState.Device device : state.devices) {
            CheckBox cb = new CheckBox(device.name());
            cb.setName("pick.device." + device.id());
            cb.setUIID(uiid("CWFieldLabel"));
            cb.addActionListener(e -> {
                if (cb.isSelected()) {
                    if (!devs.contains(device.id())) {
                        devs.add(device.id());
                    }
                } else {
                    devs.remove(device.id());
                }
                updateCreateProfileButton(createRef[0], profileType[0], bundleId[0], certs, devs, profileName[0]);
            });
            content.add(cb);
        }
        TextField name = field("Profile name", "My App App Store");
        name.setText(profileName[0]);
        content.add(name);
        Button create = primary("Create", "modal.profile.submit");
        createRef[0] = create;
        name.addDataChangedListener((type, index) -> {
            profileName[0] = name.getText();
            updateCreateProfileButton(create, profileType[0], bundleId[0], certs, devs, profileName[0]);
        });
        for (int i = 0; i < typeButtons.length; i++) {
            final int typeIndex = i;
            typeButtons[i].setUIID(uiid("CWSegment"));
            typeButtons[i].addActionListener(e -> {
                profileType[0] = typeValues[typeIndex].equals(profileType[0]) ? null : typeValues[typeIndex];
                updateSegmentButtons(typeButtons, typeValues, profileType[0]);
                updateCreateProfileButton(create, profileType[0], bundleId[0], certs, devs, name.getText());
                d.revalidate();
            });
        }
        create.addActionListener(e -> {
            if (!WizardDecisions.canCreateProfile(profileType[0], bundleId[0], certs, devs, name.getText())) {
                return;
            }
            d.dispose();
            service.createProfile(name.getText(), profileType[0], bundleId[0], certs, devs,
                    r -> afterMutation(r, "Profile created"));
        });
        updateCreateProfileButton(create, profileType[0], bundleId[0], certs, devs, name.getText());
        addDialogFooter(d, create);
        showLargeModal(d);
    }

    private void autoSetupCurrentProject() {
        ProjectDefaults defaults = projectDefaults();
        autoSetupProject(defaults.bundleId, defaults.appName);
    }

    private void autoSetupProject(String bundleIdentifier, String appName) {
        if (!state.credential.configured()) {
            showPageMessage("Connect an App Store Connect API key before generating signing assets.", true);
            go(Section.CREDENTIAL);
            return;
        }
        if (!canInstallIntoProject()) {
            return;
        }
        SigningState.BundleId existing = findBundleByIdentifier(bundleIdentifier);
        if (existing == null) {
            showPageMessage("Creating Bundle ID " + bundleIdentifier + "...", false);
            service.createBundleId(bundleIdentifier, appName, true, r -> {
                if (!r.ok) {
                    showPageMessage(r.message, true);
                    return;
                }
                refreshForAutoSetup(() -> autoSetupDefaultProfiles(bundleIdentifier, appName));
            });
        } else {
            autoSetupDefaultProfiles(bundleIdentifier, appName);
        }
    }

    private void autoSetupDefaultProfiles(String bundleIdentifier, String appName) {
        autoSetupCertificate(bundleIdentifier, appName, PROFILE_DEVELOPMENT,
                () -> autoSetupCertificate(bundleIdentifier, appName, PROFILE_APP_STORE,
                        () -> autoSetupMacProject(PROFILE_MAC_STORE,
                                () -> autoSetupMacProject(PROFILE_MAC_DIRECT,
                                        () -> autoSetupWidgetExtension(bundleIdentifier, appName,
                                                () -> showPageMessage("Automatic signing setup completed.", false))))));
    }

    private void autoSetupMacProject(String profileType) {
        autoSetupMacProject(profileType, () -> showPageMessage("Mac signing setup completed.", false));
    }

    private void autoSetupMacProject(String profileType, Runnable next) {
        ProjectDefaults defaults = projectDefaults();
        if (!state.credential.configured()) {
            showPageMessage("Connect an App Store Connect API key before generating Mac signing assets.", true);
            go(Section.CREDENTIAL);
            return;
        }
        if (!canInstallIntoProject()) {
            return;
        }
        SigningState.BundleId existing = findBundleByIdentifier(defaults.bundleId, "MAC_OS");
        if (existing == null) {
            showPageMessage("Creating Mac Bundle ID " + defaults.bundleId + "...", false);
            service.createBundleId(defaults.bundleId, defaults.appName, "MAC_OS", true, r -> {
                if (!r.ok) {
                    showPageMessage(r.message, true);
                    return;
                }
                refreshForAutoSetup(() -> autoSetupCertificate(defaults.bundleId, defaults.appName,
                        profileType, next));
            });
        } else {
            autoSetupCertificate(defaults.bundleId, defaults.appName, profileType, next);
        }
    }

    private void enableAppGroupsForBundle(String bundleIdentifier) {
        ProjectDefaults defaults = projectDefaults();
        String groupId = resolveAppGroupIdentifier(defaults);
        String groupName = defaults.appName + " Shared";
        showPageMessage("Enabling App Groups for " + bundleIdentifier + "...", false);
        findOrCreateAppGroup(groupId, groupName, group -> refreshForAutoSetup(() -> {
            SigningState.BundleId bundle = findBundleByIdentifier(bundleIdentifier, "IOS");
            if (bundle == null) {
                showPageMessage("Bundle ID was created but could not be found after refresh.", true);
                return;
            }
            List<String> ids = new ArrayList<String>();
            ids.add(group.id());
            service.enableAppGroupCapability(bundle.id(), ids, rr -> {
                if (!rr.ok) {
                    showPageMessage(rr.message, true);
                    return;
                }
                clearPageMessage();
                ToastBar.showMessage("App Groups enabled", FontImage.MATERIAL_CHECK);
                reload();
            });
        }));
    }

    private String resolveAppGroupIdentifier(ProjectDefaults defaults) {
        String fromManifest = binding == null ? null
                : surfacesAppGroup(ProjectIO.readSurfacesManifest(binding.projectDir()));
        return firstNonEmpty(fromManifest, WizardDecisions.defaultAppGroup(defaults.packageName));
    }

    private static String surfacesAppGroup(ProjectIO.SurfacesManifest manifest) {
        return manifest == null ? null : manifest.appGroup();
    }

    /// Reuses an App Group already known to the current state, otherwise creates it.
    private void findOrCreateAppGroup(String identifier, String name,
                                      com.codename1.util.OnComplete<SigningState.AppGroup> onGroup) {
        for (SigningState.AppGroup g : state.appGroups) {
            if (g.identifier() != null && g.identifier().equals(identifier)) {
                onGroup.completed(g);
                return;
            }
        }
        service.createAppGroup(identifier, name, r -> {
            if (!r.ok || r.value == null) {
                showPageMessage(r.ok ? "Server returned no App Group" : r.message, true);
                return;
            }
            onGroup.completed(r.value);
        });
    }

    private void autoSetupWidgetExtension(String bundleIdentifier, String appName, Runnable next) {
        if (binding == null || ProjectIO.readSurfacesManifest(binding.projectDir()) == null) {
            next.run();
            return;
        }
        ProjectDefaults defaults = projectDefaults();
        final String groupId = resolveAppGroupIdentifier(defaults);
        final String extIdentifier = WizardDecisions.widgetExtensionBundleId(defaults.packageName);
        if (extIdentifier == null || groupId == null) {
            next.run();
            return;
        }
        showPageMessage("Setting up widget extension signing...", false);
        findOrCreateAppGroup(groupId, appName + " Shared", group -> refreshForAutoSetup(() ->
                autoSetupWidgetExtensionAfterGroup(bundleIdentifier, appName, defaults, groupId, extIdentifier,
                        group, next)));
    }

    private void autoSetupWidgetExtensionAfterGroup(String bundleIdentifier, String appName,
            ProjectDefaults defaults, String groupId, String extIdentifier, SigningState.AppGroup group,
            Runnable next) {
        SigningState.BundleId mainBundle = findBundleByIdentifier(bundleIdentifier, "IOS");
        if (mainBundle == null) {
            showPageMessage("Main bundle ID could not be found after refresh.", true);
            return;
        }
        List<String> groupIds = new ArrayList<String>();
        groupIds.add(group.id());
        service.enableAppGroupCapability(mainBundle.id(), groupIds, r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                return;
            }
            ensureWidgetExtensionBundle(appName, defaults, extIdentifier, groupIds, next);
        });
    }

    private void ensureWidgetExtensionBundle(String appName, ProjectDefaults defaults, String extIdentifier,
            List<String> groupIds, Runnable next) {
        SigningState.BundleId ext = findBundleByIdentifier(extIdentifier, "IOS");
        if (ext != null) {
            enableWidgetExtensionGroupAndProfile(appName, defaults, extIdentifier, groupIds, next);
            return;
        }
        showPageMessage("Creating widget extension bundle ID " + extIdentifier + "...", false);
        service.createBundleId(extIdentifier, appName + " Widgets", true, r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                return;
            }
            refreshForAutoSetup(() ->
                    enableWidgetExtensionGroupAndProfile(appName, defaults, extIdentifier, groupIds, next));
        });
    }

    private void enableWidgetExtensionGroupAndProfile(String appName, ProjectDefaults defaults,
            String extIdentifier, List<String> groupIds, Runnable next) {
        SigningState.BundleId ext = findBundleByIdentifier(extIdentifier, "IOS");
        if (ext == null) {
            showPageMessage("Widget extension bundle ID could not be found after refresh.", true);
            return;
        }
        service.enableAppGroupCapability(ext.id(), groupIds, r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                return;
            }
            createWidgetExtensionProfile(appName, defaults, extIdentifier, next);
        });
    }

    private void createWidgetExtensionProfile(String appName, ProjectDefaults defaults, String extIdentifier,
            Runnable next) {
        // The extension needs a distribution profile for release builds and a development
        // profile for debug device builds, mirroring codename1.ios.release.provision /
        // codename1.ios.debug.provision on the app itself. The App Store profile is
        // required; the development profile is skipped gracefully when no development
        // certificate or registered device is available.
        ensureWidgetExtensionProfile(appName, extIdentifier, PROFILE_APP_STORE, releasePath ->
                ensureWidgetExtensionProfile(appName, extIdentifier, PROFILE_DEVELOPMENT, debugPath ->
                        installWidgetExtensionSigning(defaults, releasePath, debugPath, next)));
    }

    private void ensureWidgetExtensionProfile(String appName, String extIdentifier, String profileType,
            com.codename1.util.OnComplete<String> onPath) {
        SigningState.Profile existing = findProfile(extIdentifier, profileType);
        if (existing != null) {
            downloadWidgetExtensionProfile(existing, profileType, onPath);
            return;
        }
        SigningState.BundleId ext = findBundleByIdentifier(extIdentifier, "IOS");
        List<SigningState.Certificate> compatible = WizardDecisions.compatibleCertificates(state, profileType);
        boolean development = PROFILE_DEVELOPMENT.equals(profileType);
        if (ext == null || compatible.isEmpty()) {
            if (development) {
                onPath.completed(null);
                return;
            }
            showPageMessage("No distribution certificate was available for the widget extension profile.", true);
            return;
        }
        List<String> certs = new ArrayList<String>();
        certs.add(compatible.get(0).appleCertId());
        List<String> devices = deviceIdsFor(profileType);
        if (!WizardDecisions.canCreateProfile(profileType, ext.id(), certs, devices, appName)) {
            showPageMessage("Skipped the widget extension development profile: register a device to create development signing assets.", true);
            onPath.completed(null);
            return;
        }
        String profileName = appName + " Widgets " + (development ? "Development" : "App Store");
        showPageMessage("Creating widget extension provisioning profile " + profileName + "...", false);
        service.createProfile(profileName, profileType, ext.id(), certs, devices, r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                // A failed development profile shouldn't drop the App Store profile that
                // was already downloaded -- continue and install what we have.
                if (development) {
                    onPath.completed(null);
                }
                return;
            }
            refreshForAutoSetup(() -> {
                SigningState.Profile created = findProfile(extIdentifier, profileType);
                if (created == null) {
                    showPageMessage("Widget extension profile was created but could not be found after refresh.", true);
                    if (development) {
                        onPath.completed(null);
                    }
                    return;
                }
                downloadWidgetExtensionProfile(created, profileType, onPath);
            });
        });
    }

    private void downloadWidgetExtensionProfile(SigningState.Profile profile, String profileType,
            com.codename1.util.OnComplete<String> onPath) {
        String fileName = PROFILE_DEVELOPMENT.equals(profileType)
                ? "CN1Widgets_Development.mobileprovision" : "CN1Widgets.mobileprovision";
        service.downloadProfile(profile.id(), fileName, r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                if (PROFILE_DEVELOPMENT.equals(profileType)) {
                    onPath.completed(null);
                }
                return;
            }
            onPath.completed(r.value);
        });
    }

    private void installWidgetExtensionSigning(ProjectDefaults defaults, String releasePath, String debugPath,
            Runnable next) {
        try {
            String groupId = resolveAppGroupIdentifier(defaults);
            SigningAssetInstaller.applyWidgetExtensionSigning(binding.settings(), groupId, releasePath, debugPath);
            clearPageMessage();
            ToastBar.showMessage("Widget extension signing installed", FontImage.MATERIAL_CHECK);
            if (next != null) {
                next.run();
            }
        } catch (Exception ex) {
            Log.e(ex);
            showPageMessage("Failed to update widget extension settings: " + friendlyMessage(ex), true);
        }
    }

    private void generateAndroidKeystore(String alias, String password, String dname) {
        if (!canInstallIntoProject()) {
            return;
        }
        String keystore = defaultAndroidKeystorePath();
        showPageMessage("Generating Android keystore " + keystore + "...", false);
        new Thread(() -> {
            try {
                generateAndroidKeystoreWithJavaSE(keystore, alias, password, dname);
                SigningAssetInstaller.applyAndroidKeystore(binding.settings(), keystore, alias, password);
                CN.callSerially(() -> {
                    clearPageMessage();
                    ToastBar.showMessage("Android keystore installed", FontImage.MATERIAL_CHECK);
                    buildShell();
                });
            } catch (Exception ex) {
                Log.e(ex);
                CN.callSerially(() -> showPageMessage("Failed to generate Android keystore: "
                        + friendlyMessage(ex), true));
            }
        }).start();
    }

    private String defaultAndroidKeystorePath() {
        if (androidKeystoreProvider != null) {
            return androidKeystoreProvider.defaultKeystore(binding.projectDir());
        }
        return binding.projectDir() + "/androidCerts/KeyChain.ks";
    }

    private void generateAndroidKeystoreWithJavaSE(String keystore, String alias, String password, String dname)
            throws Exception {
        if (androidKeystoreProvider == null) {
            throw new IllegalStateException("Android keystore generation is available only in the JavaSE wizard.");
        }
        androidKeystoreProvider.generate(keystore, alias, password, dname);
    }

    private void autoSetupCertificate(String bundleIdentifier, String appName, String profileType, Runnable next) {
        List<SigningState.Certificate> compatible = WizardDecisions.compatibleCertificates(state, profileType);
        if (compatible.isEmpty()) {
            String type = WizardDecisions.requiredCertificateType(profileType);
            showPageMessage("Generating " + typeLabel(type).toLowerCase() + " certificate...", false);
            service.createCertificate(type, appName + " " + typeLabel(type), r -> {
                if (!r.ok) {
                    showPageMessage(r.message, true);
                    return;
                }
                refreshForAutoSetup(() -> autoSetupProfile(bundleIdentifier, appName, profileType, next));
            });
        } else {
            autoSetupProfile(bundleIdentifier, appName, profileType, next);
        }
    }

    private void autoSetupProfile(String bundleIdentifier, String appName, String profileType, Runnable next) {
        SigningState.BundleId bundle = findBundleByIdentifier(bundleIdentifier, platformForProfile(profileType));
        if (bundle == null) {
            showPageMessage("Bundle ID was created but could not be found after refresh.", true);
            return;
        }
        List<SigningState.Certificate> compatible = WizardDecisions.compatibleCertificates(state, profileType);
        if (compatible.isEmpty()) {
            showPageMessage("No compatible certificate was available after generation.", true);
            return;
        }
        List<String> certs = new ArrayList<String>();
        certs.add(compatible.get(0).appleCertId());
        List<String> devices = deviceIdsFor(profileType);
        if (!WizardDecisions.canCreateProfile(profileType, bundle.id(), certs, devices, appName)) {
            showPageMessage("Skipped " + profileTypeLabel(profileType) + ": register a device to create development signing assets.", true);
            next.run();
            return;
        }
        SigningState.Profile existing = findProfile(bundleIdentifier, profileType);
        if (existing != null) {
            installPair(compatible.get(0), existing, profileTypeLabel(profileType) + " signing assets installed",
                    next);
            return;
        }
        String profileName = appName + " " + profileTypeLabel(profileType);
        showPageMessage("Creating provisioning profile " + profileName + "...", false);
        service.createProfile(profileName, profileType, bundle.id(), certs, devices, r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                return;
            }
            refreshForAutoSetup(() -> {
                SigningState.Profile created = findProfile(bundleIdentifier, profileType);
                if (created == null) {
                    showPageMessage("Profile was created but could not be found after refresh.", true);
                    return;
                }
                List<SigningState.Certificate> refreshedCerts = WizardDecisions.compatibleCertificates(state, profileType);
                SigningState.Certificate cert = refreshedCerts.isEmpty() ? compatible.get(0) : refreshedCerts.get(0);
                installPair(cert, created, profileTypeLabel(profileType) + " signing assets installed", next);
            });
        });
    }

    private void refreshForAutoSetup(Runnable next) {
        service.refresh(r -> {
            if (!r.ok) {
                showPageMessage(r.message, true);
                return;
            }
            state = r.value == null ? SigningState.empty() : r.value;
            buildShell();
            next.run();
        });
    }

    private SigningState.BundleId findBundleByIdentifier(String identifier) {
        return findBundleByIdentifier(identifier, null);
    }

    private SigningState.BundleId findBundleByIdentifier(String identifier, String platform) {
        for (SigningState.BundleId b : state.bundleIds) {
            if (identifier != null && identifier.equals(b.identifier())
                    && (platform == null || platform.equals(b.platform()))) {
                return b;
            }
        }
        return null;
    }

    private String platformForProfile(String profileType) {
        return isMacProfile(profileType) ? "MAC_OS" : "IOS";
    }

    private SigningState.Profile findProfile(String bundleIdentifier, String profileType) {
        for (SigningState.Profile p : state.profiles) {
            if (profileType != null && profileType.equals(p.profileType())
                    && bundleIdentifier != null && bundleIdentifier.equals(p.bundleId())) {
                return p;
            }
        }
        return null;
    }

    private List<String> deviceIdsFor(String profileType) {
        List<String> out = new ArrayList<String>();
        if (!WizardDecisions.profileRequiresDevices(profileType)) {
            return out;
        }
        for (SigningState.Device d : state.devices) {
            if ("ENABLED".equals(d.status()) || "ACTIVE".equals(d.status())) {
                out.add(d.id());
            }
        }
        return out;
    }

    private ProjectDefaults projectDefaults() {
        String settings = binding == null ? null : binding.settings();
        String packageName = readSetting(settings, "codename1.packageName");
        String iosAppId = readSetting(settings, "codename1.ios.appid");
        String appName = firstNonEmpty(readSetting(settings, "codename1.displayName"),
                readSetting(settings, "codename1.mainName"), "Codename One App");
        String bundleId = firstNonEmpty(packageName, stripTeamPrefix(iosAppId), "com.example.app");
        return new ProjectDefaults(appName, bundleId, firstNonEmpty(packageName, bundleId));
    }

    private String readSetting(String settingsPath, String key) {
        if (settingsPath == null || settingsPath.length() == 0 || key == null) {
            return null;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        InputStream in = null;
        try {
            String url = ProjectIO.fsUrl(settingsPath);
            if (!fs.exists(url)) {
                return null;
            }
            in = fs.openInputStream(url);
            String text = Util.readToString(in, "UTF-8");
            String[] lines = text.replace("\r\n", "\n").split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                if (key.equals(trimmed.substring(0, eq).trim())) {
                    return trimmed.substring(eq + 1).trim();
                }
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            Util.cleanup(in);
        }
        return null;
    }

    private String stripTeamPrefix(String iosAppId) {
        if (iosAppId == null || iosAppId.length() == 0) {
            return null;
        }
        int dot = iosAppId.indexOf('.');
        if (dot > 0 && iosAppId.substring(0, dot).length() <= 12) {
            return iosAppId.substring(dot + 1);
        }
        return iosAppId;
    }

    private void reload() {
        final boolean wasAssumedCredentialConfigured = assumedCredentialConfigured;
        service.refresh(r -> {
            if (r.ok) {
                SigningState refreshed = r.value == null ? SigningState.empty() : r.value;
                if (shouldPreserveCachedCredentialState(token, wasAssumedCredentialConfigured, refreshed)) {
                    clearPageMessage();
                    buildShell();
                    showPageMessage("No Codename One login token was provided, so the wizard is showing the cached App Store Connect API key status.", true);
                    return;
                }
                state = preserveCachedCredentialDetails(state, refreshed);
                assumedCredentialConfigured = false;
                storeCredentialState();
                clearPageMessage();
                buildShell();
                if (wasAssumedCredentialConfigured && !state.credential.configured()) {
                    showPageMessage("The stored App Store Connect API key is no longer configured on the server. Store a new key to continue.", true);
                }
            } else {
                showPageMessage(r.message, true);
            }
        });
    }

    static boolean shouldPreserveCachedCredentialState(String token, boolean wasAssumedCredentialConfigured,
                                                       SigningState refreshed) {
        return (token == null || token.trim().isEmpty())
                && wasAssumedCredentialConfigured
                && (refreshed == null || !refreshed.credential.configured());
    }

    static SigningState preserveCachedCredentialDetails(SigningState current, SigningState refreshed) {
        if (refreshed == null || !refreshed.credential.configured() || current == null || current.credential == null) {
            return refreshed == null ? SigningState.empty() : refreshed;
        }
        String keyId = firstNonEmpty(refreshed.credential.keyId(), current.credential.keyId(), "");
        String issuerId = firstNonEmpty(refreshed.credential.issuerId(), current.credential.issuerId(), "");
        if (keyId.equals(firstNonEmpty(refreshed.credential.keyId(), "")) &&
                issuerId.equals(firstNonEmpty(refreshed.credential.issuerId(), ""))) {
            return refreshed;
        }
        SigningState.Credential credential = new SigningState.Credential(true, keyId, issuerId);
        return new SigningState(credential, refreshed.certificates, refreshed.bundleIds, refreshed.devices,
                refreshed.profiles, refreshed.apnsKeys, refreshed.appGroups);
    }

    private void storeCredentialState() {
        Preferences.set(PREF_CREDENTIAL_CONFIGURED, state.credential.configured());
        Preferences.set(PREF_CREDENTIAL_KEY_ID, state.credential.keyId() == null ? "" : state.credential.keyId());
        Preferences.set(PREF_CREDENTIAL_ISSUER_ID, state.credential.issuerId() == null ? "" : state.credential.issuerId());
    }

    private void afterMutation(SigningService.Result<Void> r, String okMessage) {
        if (!r.ok) {
            showPageMessage(r.message, true);
            return;
        }
        clearPageMessage();
        ToastBar.showMessage(okMessage, FontImage.MATERIAL_CHECK);
        reload();
    }

    private void afterDownload(SigningService.Result<String> r) {
        if (r.ok) {
            clearPageMessage();
            ToastBar.showMessage("Saved " + r.value, FontImage.MATERIAL_FILE_DOWNLOAD);
        } else {
            showPageMessage(r.message, true);
        }
    }

    private void afterCertificateDownload(SigningService.Result<String> r, SigningState.Certificate c, String password) {
        afterDownload(r);
        if (!r.ok) {
            return;
        }
        latestCertificatePath = r.value;
        latestCertificatePassword = password == null ? "" : password;
        latestAssetsDebug = "IOS_DEVELOPMENT".equals(c.certificateType());
        offerInstall();
    }

    private void afterProfileDownload(SigningService.Result<String> r, SigningState.Profile p) {
        afterDownload(r);
        if (!r.ok) {
            return;
        }
        latestProfilePath = r.value;
        latestAssetsDebug = "IOS_APP_DEVELOPMENT".equals(p.profileType());
        offerInstall();
    }

    private void installCertificate(SigningState.Certificate cert) {
        if (!canInstallIntoProject()) {
            return;
        }
        SigningState.Profile profile = profileForCertificate(cert);
        if (profile == null) {
            showPageMessage("Create a compatible provisioning profile before installing this certificate.", true);
            return;
        }
        installPair(cert, profile, "Installed " + cert.displayName());
    }

    private void installProfile(SigningState.Profile profile) {
        if (!canInstallIntoProject()) {
            return;
        }
        SigningState.Certificate cert = certificateForProfile(profile);
        if (cert == null) {
            showPageMessage("Create a compatible certificate before installing this provisioning profile.", true);
            return;
        }
        installPair(cert, profile, "Installed " + profile.name());
    }

    private boolean canInstallIntoProject() {
        if (binding == null || binding.settings() == null || binding.settings().trim().isEmpty()) {
            showPageMessage("Open the wizard from a Codename One project to install signing assets.", true);
            return false;
        }
        return true;
    }

    private void installPair(SigningState.Certificate cert, SigningState.Profile profile, String okMessage) {
        installPair(cert, profile, okMessage, null);
    }

    private void installPair(SigningState.Certificate cert, SigningState.Profile profile, String okMessage, Runnable next) {
        final boolean mac = isMacCertificate(cert.certificateType()) || isMacProfile(profile.profileType());
        final boolean debug = isDevelopmentCertificate(cert.certificateType()) || isDevelopmentProfile(profile.profileType());
        final String password = generatedP12Password(cert);
        final String p12Name = safeFileName(cert.displayName()) + ".p12";
        final String profileName = safeFileName(profile.name()) + (mac ? ".provisionprofile" : ".mobileprovision");
        service.downloadP12(cert.id(), password, p12Name, p12 -> {
            if (!p12.ok) {
                showPageMessage(p12.message, true);
                return;
            }
            service.downloadProfile(profile.id(), profileName, prof -> {
                if (!prof.ok) {
                    showPageMessage(prof.message, true);
                    return;
                }
                if (mac) {
                    installMacSigningAssets(profile.profileType(), p12.value, password, prof.value, okMessage);
                } else {
                    installSigningAssets(debug, p12.value, password, prof.value, okMessage);
                }
                if (next != null) {
                    next.run();
                }
            });
        });
    }

    private void offerInstall() {
        if (binding == null || binding.settings() == null || binding.settings().trim().isEmpty()
                || latestCertificatePath.length() == 0 || latestProfilePath.length() == 0) {
            return;
        }
        InteractionDialog d = modal("Install signing assets");
        label(d, "Apply the downloaded certificate and profile to this project.", "CWCardMeta");
        Button recommended = primary(latestAssetsDebug ? "Install debug" : "Install release", "modal.install.recommended");
        Button alternate = outline(latestAssetsDebug ? "Install release" : "Install debug", "modal.install.alternate");
        Button later = outline("Later", "modal.install.later");
        recommended.addActionListener(e -> {
            d.dispose();
            installSigningAssets(latestAssetsDebug);
        });
        alternate.addActionListener(e -> {
            d.dispose();
            installSigningAssets(!latestAssetsDebug);
        });
        later.addActionListener(e -> d.dispose());
        addDialogActions(d, later, alternate, recommended);
        showModal(d);
    }

    private void installSigningAssets(boolean debug) {
        installSigningAssets(debug, latestCertificatePath, latestCertificatePassword, latestProfilePath,
                "Project signing settings updated");
    }

    private void installSigningAssets(boolean debug, String certificatePath, String password,
                                      String profilePath, String okMessage) {
        try {
            if (debug) {
                SigningAssetInstaller.applyDebugCertificate(binding.settings(), certificatePath,
                        password, profilePath);
            } else {
                SigningAssetInstaller.applyReleaseCertificate(binding.settings(), certificatePath,
                        password, profilePath);
            }
            clearPageMessage();
            ToastBar.showMessage(okMessage, FontImage.MATERIAL_CHECK);
        } catch (Exception ex) {
            Log.e(ex);
            showPageMessage("Failed to update project settings: " + friendlyMessage(ex), true);
        }
    }

    private void installMacSigningAssets(String profileType, String certificatePath, String password,
                                         String profilePath, String okMessage) {
        try {
            SigningAssetInstaller.applyMacCertificate(binding.settings(), certificatePath, password, profilePath,
                    "MAC_APP_DIRECT".equals(profileType) || "MAC_CATALYST_APP_DIRECT".equals(profileType)
                            ? "developerID" : "appStore");
            clearPageMessage();
            ToastBar.showMessage(okMessage, FontImage.MATERIAL_CHECK);
        } catch (Exception ex) {
            Log.e(ex);
            showPageMessage("Failed to update Mac signing settings: " + friendlyMessage(ex), true);
        }
    }

    private void go(Section s) {
        section = s;
        clearPageMessage();
        buildShell();
    }

    private void pageHead(String title, String sub) {
        label(page, title, "CWPageTitle");
        label(page, sub, "CWSub");
        messageHost = new Container(BoxLayout.y());
        page.add(messageHost);
        renderPageMessage();
    }

    private void showPageMessage(String message, boolean warn) {
        pageMessage = message == null || message.trim().isEmpty() ? "The request failed. Try again later." : message;
        pageMessageWarn = warn;
        renderPageMessage();
    }

    private void clearPageMessage() {
        pageMessage = "";
        renderPageMessage();
    }

    private void renderPageMessage() {
        if (messageHost == null) {
            return;
        }
        messageHost.removeAll();
        if (pageMessage != null && pageMessage.length() > 0) {
            Label l = new Label(pageMessage);
            l.setUIID(uiid(pageMessageWarn ? "CWBannerWarn" : "CWBanner"));
            l.setTextSelectionEnabled(true);
            messageHost.add(l);
        }
        messageHost.revalidate();
    }

    private void banner(String text, boolean warn) {
        Label l = new Label(text);
        l.setUIID(uiid(warn ? "CWBannerWarn" : "CWBanner"));
        l.setTextSelectionEnabled(true);
        page.add(l);
    }

    private void confirm(String title, String message, String actionText, Runnable action) {
        InteractionDialog d = modal(title);
        label(d, message, "CWCardMeta");
        Button cancel = outline("Cancel", "modal.cancel");
        cancel.addActionListener(e -> d.dispose());
        Button run = danger(actionText, "modal.confirm");
        run.addActionListener(e -> {
            d.dispose();
            action.run();
        });
        addDialogActions(d, cancel, run);
        showModal(d);
    }

    private void certificateDetails(SigningState.Certificate c) {
        InteractionDialog d = detailsDialog("Certificate details");
        detailRow(d, "Name", c.displayName());
        detailRow(d, "Type", typeLabel(c.certificateType()));
        detailRow(d, "Apple ID", c.appleCertId());
        detailRow(d, "Serial", c.serialNumber());
        detailRow(d, "Status", c.status());
        detailRow(d, "Expires", expiry(c.expiresAt()));
        detailRow(d, "Private key", c.privateKeyPresent() ? "Available" : "Missing");
        addCloseAction(d);
        showModal(d);
    }

    private void bundleDetails(SigningState.BundleId b) {
        InteractionDialog d = detailsDialog("Bundle ID details");
        detailRow(d, "Identifier", b.identifier());
        detailRow(d, "Name", b.name());
        detailRow(d, "Apple ID", b.id());
        detailRow(d, "Platform", b.platform());
        detailRow(d, "Push", b.pushEnabled() ? "Enabled" : "Off");
        addCloseAction(d);
        showModal(d);
    }

    private void deviceDetails(SigningState.Device device) {
        InteractionDialog d = detailsDialog("Device details");
        detailRow(d, "Name", device.name());
        detailRow(d, "UDID", device.udid());
        detailRow(d, "Apple ID", device.id());
        detailRow(d, "Platform", device.platform());
        detailRow(d, "Status", device.status());
        addCloseAction(d);
        showModal(d);
    }

    private void profileDetails(SigningState.Profile p) {
        InteractionDialog d = detailsDialog("Profile details");
        detailRow(d, "Name", p.name());
        detailRow(d, "Type", profileTypeLabel(p.profileType()));
        detailRow(d, "Bundle ID", p.bundleId());
        detailRow(d, "Apple ID", p.id() == null ? "" : String.valueOf(p.id()));
        detailRow(d, "Profile ID", p.appleProfileId());
        detailRow(d, "UUID", p.uuid());
        detailRow(d, "Status", p.status());
        detailRow(d, "Expires", expiry(p.expiresAt()));
        addCloseAction(d);
        showModal(d);
    }

    private void apnsDetails(SigningState.ApnsKey k) {
        InteractionDialog d = detailsDialog("APNs key details");
        detailRow(d, "Name", k.displayName());
        detailRow(d, "Key ID", k.keyId());
        detailRow(d, "Team ID", k.teamId());
        detailRow(d, "Added", expiry(k.createdAt()));
        addCloseAction(d);
        showModal(d);
    }

    private InteractionDialog detailsDialog(String title) {
        return modal(title);
    }

    private void detailRow(Container c, String k, String v) {
        label(c, k, "CWFieldLabel");
        label(c, v == null || v.length() == 0 ? "-" : v, "CWCellMain");
    }

    private void addCloseAction(InteractionDialog d) {
        Button close = primary("Close", "modal.close");
        close.addActionListener(e -> d.dispose());
        addDialogActions(d, close);
    }

    private Container card() {
        Container c = new Container(BoxLayout.y());
        c.setUIID(uiid("CWCard"));
        return c;
    }

    private InteractionDialog modal(String title) {
        InteractionDialog d = new InteractionDialog(title);
        d.setLayout(BoxLayout.y());
        d.setUIID(uiid("CWModal"));
        d.getTitleComponent().setUIID(uiid("CWModalTitle"));
        d.setScrollableY(true);
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setAnimateShow(true);
        d.setRepositionAnimation(false);
        return d;
    }

    private InteractionDialog modalFrame(String title) {
        InteractionDialog d = new InteractionDialog(title);
        d.setLayout(new BorderLayout());
        d.setUIID(uiid("CWModal"));
        d.getTitleComponent().setUIID(uiid("CWModalTitle"));
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setAnimateShow(true);
        d.setRepositionAnimation(false);
        return d;
    }

    private void addDialogActions(InteractionDialog d, Button primaryAction) {
        Button cancel = outline("Cancel", "modal.cancel");
        cancel.addActionListener(e -> d.dispose());
        addDialogActions(d, cancel, primaryAction);
    }

    private void addDialogActions(InteractionDialog d, Button first, Button second) {
        Container actions = actionRow(Component.RIGHT, first, second);
        actions.setUIID(uiid("CWDialogActions"));
        d.add(actions);
    }

    private void addDialogActions(InteractionDialog d, Button first, Button second, Button third) {
        Container actions = actionRow(Component.RIGHT, first, second, third);
        actions.setUIID(uiid("CWDialogActions"));
        d.add(actions);
    }

    private void addDialogFooter(InteractionDialog d, Button primaryAction) {
        Button cancel = outline("Cancel", "modal.cancel");
        cancel.addActionListener(e -> d.dispose());
        Container actions = actionRow(Component.RIGHT, cancel, primaryAction);
        actions.setUIID(uiid("CWDialogActions"));
        d.add(BorderLayout.SOUTH, actions);
    }

    private void showModal(InteractionDialog d) {
        Display display = Display.getInstance();
        Form current = display.getCurrent();
        int w = current == null || current.getWidth() <= 0 ? display.getDisplayWidth() : current.getWidth();
        int h = current == null || current.getHeight() <= 0 ? display.getDisplayHeight() : current.getHeight();
        int min = display.convertToPixels(4);
        int preferredW = Math.min(w - min * 2, Math.max(display.convertToPixels(68), w / 3));
        applyFontScale(d);
        int naturalH = d.getPreferredH();
        int preferredH = Math.min(h - min * 2, Math.max(display.convertToPixels(28),
                naturalH <= 0 ? display.convertToPixels(44) : naturalH + display.convertToPixels(3)));
        d.setPreferredW(preferredW);
        d.setPreferredH(preferredH);
        int left = Math.max(min, (w - preferredW) / 2);
        int right = Math.max(min, w - preferredW - left);
        int top = Math.max(min, (h - preferredH) / 2);
        int bottom = Math.max(min, h - preferredH - top);
        d.show(top, bottom, left, right);
    }

    private void showLargeModal(InteractionDialog d) {
        Display display = Display.getInstance();
        Form current = display.getCurrent();
        int w = current == null || current.getWidth() <= 0 ? display.getDisplayWidth() : current.getWidth();
        int h = current == null || current.getHeight() <= 0 ? display.getDisplayHeight() : current.getHeight();
        int min = display.convertToPixels(4);
        int preferredW = Math.min(w - min * 2, Math.max(display.convertToPixels(86), w * 2 / 5));
        applyFontScale(d);
        int preferredH = Math.min(h - min * 2, Math.max(display.convertToPixels(64), h * 3 / 4));
        d.setPreferredW(preferredW);
        d.setPreferredH(preferredH);
        int left = Math.max(min, (w - preferredW) / 2);
        int right = Math.max(min, w - preferredW - left);
        int top = Math.max(min, (h - preferredH) / 2);
        int bottom = Math.max(min, h - preferredH - top);
        d.show(top, bottom, left, right);
    }

    private void metric(Container parent, String number, String label, Runnable action) {
        Container box = new Container(BoxLayout.y());
        box.setUIID(uiid("CWMetric"));
        Label n = new Label(number);
        n.setUIID(uiid("CWMetricNumber"));
        n.setTextSelectionEnabled(true);
        Label l = new Label(label);
        l.setUIID(uiid("CWMetricLabel"));
        l.setTextSelectionEnabled(true);
        box.add(n).add(l);
        box.addPointerReleasedListener(e -> action.run());
        parent.add(box);
    }

    private Container actionRow(int align, Button... buttons) {
        Container outer = new Container(new FlowLayout(align));
        outer.setUIID(uiid("CWActionRow"));
        Container grid = new Container(new GridLayout(1, buttons.length));
        grid.setUIID(uiid("CWActionGrid"));
        for (Button b : buttons) {
            grid.add(b);
        }
        outer.add(grid);
        return outer;
    }

    private Container filterRow(Section tableSection, Container body) {
        Container outer = new Container(new BorderLayout());
        outer.setUIID(uiid("CWFilterRow"));
        Container fieldWrap = new Container(new BorderLayout());
        fieldWrap.setUIID(uiid("CWFilterWrap"));
        TextField filter = new TextField(tableFilter(tableSection));
        filter.setHint("Filter");
        filter.setName("filter." + tableSection.name().toLowerCase());
        filter.getHintLabel().setUIID(uiid("CWFieldHint"));
        filter.setTextSelectionEnabled(true);
        filter.setUIID(uiid("CWFilterField"));
        Button clear = new Button("");
        clear.setName("filter.clear." + tableSection.name().toLowerCase());
        clear.setUIID(uiid("CWFilterClear"));
        clear.setTooltip("Clear filter");
        FontImage.setMaterialIcon(clear, FontImage.MATERIAL_CLOSE, 2.4f);
        clear.setHidden(tableFilter(tableSection).length() == 0);
        clear.setVisible(tableFilter(tableSection).length() > 0);
        filter.addDataChangedListener((type, index) -> {
            tableFilters[tableSection.ordinal()] = filter.getText() == null ? "" : filter.getText().trim();
            boolean hasText = tableFilter(tableSection).length() > 0;
            clear.setHidden(!hasText);
            clear.setVisible(hasText);
            refreshTableBody(tableSection, body);
            fieldWrap.revalidate();
        });
        clear.addActionListener(e -> {
            tableFilters[tableSection.ordinal()] = "";
            filter.setText("");
            refreshTableBody(tableSection, body);
        });
        fieldWrap.add(BorderLayout.CENTER, filter);
        fieldWrap.add(BorderLayout.EAST, clear);
        outer.add(BorderLayout.CENTER, fieldWrap);
        return outer;
    }

    private Container tableBody(Section tableSection) {
        Container body = new Container(BoxLayout.y());
        body.setName("table." + tableSection.name().toLowerCase());
        body.setUIID(uiid("CWTableBody"));
        return body;
    }

    private void refreshTableBody(Section tableSection, Container body) {
        populateTableBody(tableSection, body);
        applyFontScale(body);
        body.revalidate();
    }

    private void populateTableBody(Section tableSection, Container body) {
        body.removeAll();
        switch (tableSection) {
            case CERTIFICATES -> populateCertificateTable(body);
            case BUNDLES -> populateBundleTable(body);
            case DEVICES -> populateDeviceTable(body);
            case PROFILES -> populateProfileTable(body);
            case APNS -> populateApnsTable(body);
            default -> {
            }
        }
    }

    private void populateCertificateTable(Container body) {
        tableHeader(body, Section.CERTIFICATES, "Certificate", "Type", "Expires", "Status", "");
        for (SigningState.Certificate c : certificateRows()) {
            Container r = tableRow();
            r.add(detailCell(cell(c.displayName(), c.serialNumber()), () -> certificateDetails(c)));
            r.add(detailCell(pill(typeLabel(c.certificateType()), c.certificateType().contains("DISTRIBUTION") ? "CWPillOk" : "CWPillMuted"), () -> certificateDetails(c)));
            r.add(detailCell(cell(expiry(c.expiresAt()), ""), () -> certificateDetails(c)));
            r.add(detailCell(pill(c.status(), statusUIID(c.status())), () -> certificateDetails(c)));
            Button dl = outline("Install", "btn.installCert." + c.id());
            dl.addActionListener(e -> installCertificate(c));
            Button revoke = danger("Revoke", "btn.revokeCert." + c.id());
            revoke.addActionListener(e -> confirm("Revoke certificate", "Revoke " + c.displayName() + "? This cannot be undone.",
                    "Revoke", () -> service.revokeCertificate(c.id(), x -> afterMutation(x, "Certificate revoked"))));
            r.add(tableCell(actionRow(Component.RIGHT, dl, revoke)));
            body.add(r);
        }
    }

    private void populateBundleTable(Container body) {
        tableHeader(body, Section.BUNDLES, "Identifier", "Name", "Platform", "Push", "");
        for (SigningState.BundleId b : bundleRows()) {
            Container r = tableRow();
            r.add(detailCell(cell(b.identifier(), b.id()), () -> bundleDetails(b)));
            r.add(detailCell(cell(b.name(), ""), () -> bundleDetails(b)));
            r.add(detailCell(pill(b.platform(), "CWPillMuted"), () -> bundleDetails(b)));
            r.add(detailCell(pill(b.pushEnabled() ? "Enabled" : "Off", b.pushEnabled() ? "CWPillOk" : "CWPillMuted"), () -> bundleDetails(b)));
            r.add(tableCell(blankCell()));
            body.add(r);
        }
    }

    private void populateDeviceTable(Container body) {
        tableHeader(body, Section.DEVICES, "Device", "UDID", "Platform", "Status", "");
        for (SigningState.Device d : deviceRows()) {
            Container r = tableRow();
            r.add(detailCell(cell(d.name(), d.id()), () -> deviceDetails(d)));
            r.add(detailCell(cell(d.udid(), ""), () -> deviceDetails(d)));
            r.add(detailCell(pill(d.platform(), "CWPillMuted"), () -> deviceDetails(d)));
            r.add(detailCell(pill(d.status(), "CWPillOk"), () -> deviceDetails(d)));
            r.add(tableCell(blankCell()));
            body.add(r);
        }
    }

    private void populateProfileTable(Container body) {
        tableHeader(body, Section.PROFILES, "Profile", "Type", "Bundle ID", "Expires", "");
        for (SigningState.Profile p : profileRows()) {
            Container r = tableRow();
            r.add(detailCell(cell(p.name(), p.appleProfileId()), () -> profileDetails(p)));
            r.add(detailCell(pill(profileTypeLabel(p.profileType()), "IOS_APP_STORE".equals(p.profileType()) ? "CWPillOk" : "CWPillWarn"), () -> profileDetails(p)));
            r.add(detailCell(cell(p.bundleId(), ""), () -> profileDetails(p)));
            r.add(detailCell(cell(expiry(p.expiresAt()), p.status()), () -> profileDetails(p)));
            Button dl = outline("Install", "btn.installProfile." + p.id());
            dl.addActionListener(e -> installProfile(p));
            Button del = danger("Delete", "btn.deleteProfile." + p.id());
            del.addActionListener(e -> confirm("Delete profile", "Delete " + p.name() + "? This cannot be undone.",
                    "Delete", () -> service.deleteProfile(p.id(), x -> afterMutation(x, "Profile deleted"))));
            r.add(tableCell(actionRow(Component.RIGHT, dl, del)));
            body.add(r);
        }
    }

    private void populateApnsTable(Container body) {
        tableHeader(body, Section.APNS, "Name", "Key ID", "Team ID", "Added", "");
        for (SigningState.ApnsKey k : apnsRows()) {
            Container r = tableRow();
            r.add(detailCell(cell(k.displayName(), ""), () -> apnsDetails(k)));
            r.add(detailCell(cell(k.keyId(), ""), () -> apnsDetails(k)));
            r.add(detailCell(cell(k.teamId(), ""), () -> apnsDetails(k)));
            r.add(detailCell(cell(expiry(k.createdAt()), ""), () -> apnsDetails(k)));
            Button del = danger("Delete", "btn.deleteApns." + k.keyId());
            del.addActionListener(e -> confirm("Delete APNs key", "Delete APNs key " + k.keyId() + "? This cannot be undone.",
                    "Delete", () -> service.deleteApnsKey(k.keyId(), x -> afterMutation(x, "APNs key deleted"))));
            r.add(tableCell(actionRow(Component.RIGHT, del)));
            body.add(r);
        }
    }

    private Container tableRow() {
        Container r = new Container(new GridLayout(1, 5));
        r.setUIID(uiid("CWRow"));
        return r;
    }

    private Container tableCell(Component cmp) {
        Container out = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        out.setUIID(uiid("CWTableCell"));
        out.add(cmp);
        return out;
    }

    private Container detailCell(Component cmp, Runnable details) {
        Container out = tableCell(cmp);
        addDetailListener(out, details);
        return out;
    }

    private void addDetailListener(Component cmp, Runnable details) {
        cmp.addPointerReleasedListener(e -> details.run());
        if (cmp instanceof Container) {
            Container cnt = (Container)cmp;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                addDetailListener(cnt.getComponentAt(i), details);
            }
        }
    }

    private Label blankCell() {
        Label l = new Label("");
        l.setUIID(uiid("CWCellSub"));
        l.setTextSelectionEnabled(true);
        return l;
    }

    private void tableHeader(Container t, Section tableSection, String a, String b, String c, String d, String e) {
        Container row = new Container(new GridLayout(1, 5));
        row.add(header(tableSection, 0, a)).add(header(tableSection, 1, b))
                .add(header(tableSection, 2, c)).add(header(tableSection, 3, d))
                .add(header(tableSection, 4, e));
        t.add(row);
    }

    private Button header(Section tableSection, int column, String text) {
        Button b = new Button(headerText(tableSection, column, text));
        b.setUIID(uiid("CWTableHeader"));
        b.setName("sort." + tableSection.name().toLowerCase() + "." + column);
        b.setAlignment(Component.LEFT);
        b.getAllStyles().setAlignment(Component.LEFT);
        if (text == null || text.length() == 0) {
            b.setEnabled(false);
            return b;
        }
        b.addActionListener(e -> {
            int idx = tableSection.ordinal();
            if (tableSortColumns[idx] == column) {
                tableSortAscending[idx] = !tableSortAscending[idx];
            } else {
                tableSortColumns[idx] = column;
                tableSortAscending[idx] = true;
            }
            rerenderPage();
        });
        return b;
    }

    private String headerText(Section tableSection, int column, String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        int idx = tableSection.ordinal();
        if (tableSortColumns[idx] == column) {
            return text + (tableSortAscending[idx] ? " ^" : " v");
        }
        return text;
    }

    private void rerenderPage() {
        renderPage();
        applyFontScale(page);
        page.revalidate();
    }

    private Container cell(String main, String sub) {
        Container c = new Container(BoxLayout.y());
        Label m = new Label(main == null ? "" : main);
        m.setUIID(uiid("CWCellMain"));
        m.setTextSelectionEnabled(true);
        Label s = new Label(sub == null ? "" : sub);
        s.setUIID(uiid("CWCellSub"));
        s.setTextSelectionEnabled(true);
        c.add(m).add(s);
        return c;
    }

    private Label pill(String text, String uiid) {
        Label l = new Label(text == null ? "" : text);
        l.setUIID(uiid(uiid));
        l.setAlignment(Component.CENTER);
        l.setVerticalAlignment(Component.CENTER);
        l.setTextSelectionEnabled(true);
        return l;
    }

    private TextField field(String label, String hint) {
        TextField f = new TextField();
        f.setHint(hint);
        f.getHintLabel().setUIID(uiid("CWFieldHint"));
        f.setTextSelectionEnabled(true);
        f.setUIID(uiid("CWField"));
        return f;
    }

    private Button primary(String text, String name) {
        return button(text, name, "CWPrimary");
    }

    private Button outline(String text, String name) {
        return button(text, name, "CWOutline");
    }

    private Button danger(String text, String name) {
        return button(text, name, "CWDanger");
    }

    private Button choice(String title, String desc, boolean selected) {
        Button b = new Button(title + "\n" + desc);
        b.setUIID(uiid(selected ? "CWChoiceSelected" : "CWChoice"));
        return b;
    }

    private Button segment(String text, boolean selected) {
        return button(text, null, selected ? "CWSegmentSelected" : "CWSegment");
    }

    private void updateSegmentButtons(Button[] buttons, String[] values, String selectedValue) {
        for (int i = 0; i < buttons.length; i++) {
            setScaledUIID(buttons[i], values[i].equals(selectedValue) ? "CWSegmentSelected" : "CWSegment");
        }
    }

    private void updateChoiceButtons(List<Button> buttons, String selectedId) {
        for (Button b : buttons) {
            String name = b.getName();
            boolean selected = selectedId != null && name != null && name.endsWith("." + selectedId);
            setScaledUIID(b, selected ? "CWChoiceSelected" : "CWChoice");
        }
    }

    private void updateCreateProfileButton(Button create, String profileType, String bundleId, List<String> certs,
                                           List<String> devs, String name) {
        if (create == null) {
            return;
        }
        boolean enabled = WizardDecisions.canCreateProfile(profileType, bundleId, certs, devs, name);
        create.setEnabled(enabled);
        setScaledUIID(create, enabled ? "CWPrimary" : "CWDisabled");
    }

    private void setScaledUIID(Component c, String id) {
        c.setUIID(uiid(id));
        applyFontScale(c);
    }

    private Button iconButton(char icon, String tooltip, String name) {
        Button b = new Button("");
        b.setUIID(uiid("CWToolbarButton"));
        b.setName(name);
        b.setTooltip(tooltip);
        b.setAlignment(Component.CENTER);
        b.getAllStyles().setAlignment(Component.CENTER);
        FontImage.setMaterialIcon(b, icon, 3.2f);
        return b;
    }

    private Button button(String text, String name, String uiid) {
        Button b = new Button(text);
        b.setUIID(uiid(uiid));
        b.setName(name);
        b.setAlignment(Component.CENTER);
        b.getAllStyles().setAlignment(Component.CENTER);
        return b;
    }

    private void label(Container c, String text, String uiid) {
        Label l = new Label(text);
        l.setUIID(uiid(uiid));
        l.setTextSelectionEnabled(true);
        c.add(l);
    }

    private void row(Container c, String k, String v, String name) {
        Label l = new Label(k + ": " + (v == null ? "" : v));
        l.setUIID(uiid("CWCardMeta"));
        l.setTextSelectionEnabled(true);
        l.setName(name);
        c.add(l);
    }

    private void macAssetRow(Container c, String title, String detail, String buttonName, Runnable action) {
        Container r = new Container(new BorderLayout());
        r.setUIID(uiid("CWCardRow"));
        Label l = new Label(title + ": " + (detail == null ? "" : detail));
        l.setUIID(uiid("CWCardMeta"));
        l.setTextSelectionEnabled(true);
        r.add(BorderLayout.CENTER, l);
        Button install = outline("Install", buttonName);
        install.addActionListener(e -> action.run());
        r.add(BorderLayout.EAST, install);
        c.add(r);
    }

    private Container twoColumn(Component left, Component right) {
        Container out = new Container(new GridLayout(1, 2));
        out.add(left);
        out.add(right);
        return out;
    }

    private String androidCommonName(ProjectDefaults defaults) {
        String appName = defaults == null ? null : defaults.appName;
        if (appName == null || appName.trim().isEmpty() || appName.toLowerCase().contains("codename one")) {
            return "My App";
        }
        return appName.trim();
    }

    private String androidDistinguishedName(String commonName, String orgUnit, String organization,
                                            String locality, String stateName, String country) {
        return "CN=" + dnPart(firstNonEmpty(commonName, "My App"))
                + ", OU=" + dnPart(firstNonEmpty(orgUnit, "Development"))
                + ", O=" + dnPart(firstNonEmpty(organization, "MyCompany"))
                + ", L=" + dnPart(firstNonEmpty(locality, "MyCity"))
                + ", ST=" + dnPart(firstNonEmpty(stateName, "MyState"))
                + ", C=" + dnPart(countryCode(country));
    }

    private String countryCode(String country) {
        String clean = firstNonEmpty(country, "US").trim().toUpperCase();
        if (clean.length() != 2) {
            return "US";
        }
        return clean;
    }

    private String dnPart(String value) {
        return firstNonEmpty(value, "Unknown").replace(',', ' ').trim();
    }

    private String uiid(String id) {
        return darkMode ? "Dark" + id : id;
    }

    private static final class ProjectDefaults {
        final String appName;
        final String bundleId;
        final String packageName;

        ProjectDefaults(String appName, String bundleId, String packageName) {
            this.appName = appName == null || appName.trim().isEmpty() ? "Codename One App" : appName.trim();
            this.bundleId = bundleId == null || bundleId.trim().isEmpty() ? "com.example.app" : bundleId.trim();
            this.packageName = packageName == null || packageName.trim().isEmpty()
                    ? this.bundleId : packageName.trim();
        }
    }

    private String expiry(Long millis) {
        if (millis == null) {
            return "Unknown";
        }
        long days = (millis.longValue() - System.currentTimeMillis()) / 86400000L;
        if (days < 0) {
            return "Expired";
        }
        if (days == 0) {
            return "Today";
        }
        return days + " days";
    }

    private String statusUIID(String status) {
        if ("ACTIVE".equals(status)) {
            return "CWPillOk";
        }
        if ("REVOKED".equals(status) || "EXPIRED".equals(status)) {
            return "CWPillBad";
        }
        return "CWPillMuted";
    }

    private String typeLabel(String raw) {
        return raw == null ? "" : raw.replace("IOS_", "").replace("MAC_", "MAC ")
                .replace("DEVELOPER_ID_", "DEVELOPER ID ").replace('_', ' ');
    }

    private String profileTypeLabel(String raw) {
        return raw == null ? "" : raw.replace("IOS_APP_", "").replace("MAC_CATALYST_APP_", "MAC CATALYST ")
                .replace("MAC_APP_", "MAC ").replace('_', ' ');
    }

    private String tableFilter(Section tableSection) {
        String v = tableFilters[tableSection.ordinal()];
        return v == null ? "" : v;
    }

    private List<SigningState.Certificate> certificateRows() {
        List<SigningState.Certificate> out = new ArrayList<SigningState.Certificate>();
        String filter = tableFilter(Section.CERTIFICATES);
        for (SigningState.Certificate c : state.certificates) {
            if (matches(filter, c.displayName(), c.serialNumber(), typeLabel(c.certificateType()),
                    expiry(c.expiresAt()), c.status())) {
                out.add(c);
            }
        }
        Collections.sort(out, new Comparator<SigningState.Certificate>() {
            public int compare(SigningState.Certificate a, SigningState.Certificate b) {
                int col = tableSortColumns[Section.CERTIFICATES.ordinal()];
                int v;
                switch (col) {
                    case 1 -> v = cmp(typeLabel(a.certificateType()), typeLabel(b.certificateType()));
                    case 2 -> v = cmpLong(a.expiresAt(), b.expiresAt());
                    case 3 -> v = cmp(a.status(), b.status());
                    default -> v = cmp(a.displayName(), b.displayName());
                }
                return sortDir(Section.CERTIFICATES, v);
            }
        });
        return out;
    }

    private List<SigningState.BundleId> bundleRows() {
        List<SigningState.BundleId> out = new ArrayList<SigningState.BundleId>();
        String filter = tableFilter(Section.BUNDLES);
        for (SigningState.BundleId b : state.bundleIds) {
            if (matches(filter, b.identifier(), b.id(), b.name(), b.platform(), b.pushEnabled() ? "Enabled" : "Off")) {
                out.add(b);
            }
        }
        Collections.sort(out, new Comparator<SigningState.BundleId>() {
            public int compare(SigningState.BundleId a, SigningState.BundleId b) {
                int col = tableSortColumns[Section.BUNDLES.ordinal()];
                int v;
                switch (col) {
                    case 1 -> v = cmp(a.name(), b.name());
                    case 2 -> v = cmp(a.platform(), b.platform());
                    case 3 -> v = cmp(a.pushEnabled() ? "Enabled" : "Off", b.pushEnabled() ? "Enabled" : "Off");
                    default -> v = cmp(a.identifier(), b.identifier());
                }
                return sortDir(Section.BUNDLES, v);
            }
        });
        return out;
    }

    private List<SigningState.Device> deviceRows() {
        List<SigningState.Device> out = new ArrayList<SigningState.Device>();
        String filter = tableFilter(Section.DEVICES);
        for (SigningState.Device d : state.devices) {
            if (matches(filter, d.name(), d.id(), d.udid(), d.platform(), d.status())) {
                out.add(d);
            }
        }
        Collections.sort(out, new Comparator<SigningState.Device>() {
            public int compare(SigningState.Device a, SigningState.Device b) {
                int col = tableSortColumns[Section.DEVICES.ordinal()];
                int v;
                switch (col) {
                    case 1 -> v = cmp(a.udid(), b.udid());
                    case 2 -> v = cmp(a.platform(), b.platform());
                    case 3 -> v = cmp(a.status(), b.status());
                    default -> v = cmp(a.name(), b.name());
                }
                return sortDir(Section.DEVICES, v);
            }
        });
        return out;
    }

    private List<SigningState.Profile> profileRows() {
        List<SigningState.Profile> out = new ArrayList<SigningState.Profile>();
        String filter = tableFilter(Section.PROFILES);
        for (SigningState.Profile p : state.profiles) {
            if (matches(filter, p.name(), p.appleProfileId(), profileTypeLabel(p.profileType()),
                    p.bundleId(), expiry(p.expiresAt()), p.status())) {
                out.add(p);
            }
        }
        Collections.sort(out, new Comparator<SigningState.Profile>() {
            public int compare(SigningState.Profile a, SigningState.Profile b) {
                int col = tableSortColumns[Section.PROFILES.ordinal()];
                int v;
                switch (col) {
                    case 1 -> v = cmp(profileTypeLabel(a.profileType()), profileTypeLabel(b.profileType()));
                    case 2 -> v = cmp(a.bundleId(), b.bundleId());
                    case 3 -> v = cmpLong(a.expiresAt(), b.expiresAt());
                    default -> v = cmp(a.name(), b.name());
                }
                return sortDir(Section.PROFILES, v);
            }
        });
        return out;
    }

    private List<SigningState.ApnsKey> apnsRows() {
        List<SigningState.ApnsKey> out = new ArrayList<SigningState.ApnsKey>();
        String filter = tableFilter(Section.APNS);
        for (SigningState.ApnsKey k : state.apnsKeys) {
            if (matches(filter, k.displayName(), k.keyId(), k.teamId(), expiry(k.createdAt()))) {
                out.add(k);
            }
        }
        Collections.sort(out, new Comparator<SigningState.ApnsKey>() {
            public int compare(SigningState.ApnsKey a, SigningState.ApnsKey b) {
                int col = tableSortColumns[Section.APNS.ordinal()];
                int v;
                switch (col) {
                    case 1 -> v = cmp(a.keyId(), b.keyId());
                    case 2 -> v = cmp(a.teamId(), b.teamId());
                    case 3 -> v = cmpLong(a.createdAt(), b.createdAt());
                    default -> v = cmp(a.displayName(), b.displayName());
                }
                return sortDir(Section.APNS, v);
            }
        });
        return out;
    }

    private boolean matches(String filter, String... values) {
        if (filter == null || filter.trim().length() == 0) {
            return true;
        }
        String f = filter.trim().toLowerCase();
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && values[i].toLowerCase().indexOf(f) >= 0) {
                return true;
            }
        }
        return false;
    }

    private int sortDir(Section s, int value) {
        return tableSortAscending[s.ordinal()] ? value : -value;
    }

    private int cmp(String a, String b) {
        String aa = a == null ? "" : a.toLowerCase();
        String bb = b == null ? "" : b.toLowerCase();
        return aa.compareTo(bb);
    }

    private int cmpLong(Long a, Long b) {
        long aa = a == null ? Long.MAX_VALUE : a.longValue();
        long bb = b == null ? Long.MAX_VALUE : b.longValue();
        if (aa == bb) {
            return 0;
        }
        return aa < bb ? -1 : 1;
    }

    private SigningState.Profile profileForCertificate(SigningState.Certificate cert) {
        SigningState.Profile fallback = null;
        boolean development = isDevelopmentCertificate(cert.certificateType());
        for (SigningState.Profile p : state.profiles) {
            if (!"ACTIVE".equals(p.status())) {
                continue;
            }
            if (cert.certificateType() != null
                    && cert.certificateType().equals(WizardDecisions.requiredCertificateType(p.profileType()))) {
                return p;
            }
            if (development && isDevelopmentProfile(p.profileType())) {
                return p;
            }
            if (!development && isDistributionProfile(p.profileType())) {
                if ("IOS_APP_STORE".equals(p.profileType())) {
                    return p;
                }
                if (fallback == null) {
                    fallback = p;
                }
            }
        }
        return fallback;
    }

    private SigningState.Certificate certificateForProfile(SigningState.Profile profile) {
        String required = WizardDecisions.requiredCertificateType(profile.profileType());
        boolean development = isDevelopmentProfile(profile.profileType());
        for (SigningState.Certificate c : state.certificates) {
            if (!"ACTIVE".equals(c.status()) || !c.privateKeyPresent()) {
                continue;
            }
            if (required.equals(c.certificateType())) {
                return c;
            }
            if (development == isDevelopmentCertificate(c.certificateType())) {
                return c;
            }
        }
        return null;
    }

    private boolean isDevelopmentCertificate(String type) {
        return "IOS_DEVELOPMENT".equals(type) || "MAC_APP_DEVELOPMENT".equals(type)
                || "DEVELOPMENT".equals(type);
    }

    private boolean isDevelopmentProfile(String type) {
        return "IOS_APP_DEVELOPMENT".equals(type) || "MAC_APP_DEVELOPMENT".equals(type)
                || "MAC_CATALYST_APP_DEVELOPMENT".equals(type);
    }

    private boolean isDistributionProfile(String type) {
        return "IOS_APP_STORE".equals(type) || "IOS_APP_ADHOC".equals(type)
                || "MAC_APP_STORE".equals(type) || "MAC_APP_DIRECT".equals(type)
                || "MAC_CATALYST_APP_STORE".equals(type) || "MAC_CATALYST_APP_DIRECT".equals(type);
    }

    private boolean isMacCertificate(String type) {
        return type != null && (type.startsWith("MAC_") || type.startsWith("DEVELOPER_ID_"));
    }

    private boolean isMacProfile(String type) {
        return type != null && (type.startsWith("MAC_APP_") || type.startsWith("MAC_CATALYST_APP_"));
    }

    private String generatedP12Password(SigningState.Certificate cert) {
        long id = cert.id() == null ? 0 : cert.id().longValue();
        return "cn1-" + System.currentTimeMillis() + "-" + id;
    }

    private String safeFileName(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "ios-signing-asset";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(isSafeFileChar(c) ? c : '-');
        }
        return out.toString();
    }

    private boolean isSafeFileChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_';
    }

    private static String firstNonEmpty(String a, String b, String c) {
        if (a != null && !a.trim().isEmpty()) {
            return a;
        }
        if (b != null && !b.trim().isEmpty()) {
            return b;
        }
        return c;
    }

    private static String firstNonEmpty(String a, String b) {
        return firstNonEmpty(a, b, "");
    }
}
