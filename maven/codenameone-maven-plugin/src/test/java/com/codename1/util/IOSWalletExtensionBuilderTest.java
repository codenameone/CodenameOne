package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IOSWalletExtensionBuilderTest {

    private IOSWalletExtensionBuilder validBuilder() {
        return new IOSWalletExtensionBuilder()
                .setAppGroupId("group.com.example.myapp.wallet")
                .setIssuerEndpoint("https://issuer.example.com/provision")
                .setAuthEndpoint("https://issuer.example.com/login");
    }

    private static String text(Map<String, byte[]> files, String name) {
        byte[] data = files.get(name);
        assertTrue(data != null, name + " must be present in file map");
        return new String(data, StandardCharsets.UTF_8);
    }

    @Test
    public void nonUIFileMap_producesExpectedFiles() {
        Map<String, byte[]> files = validBuilder().buildNonUIFileMap();
        assertTrue(files.containsKey("Info.plist"));
        assertTrue(files.containsKey("WalletNonUIExtension.entitlements"),
                "entitlements file must be named after the extension");
        assertTrue(files.containsKey("CN1WalletExtensionHandler.h"));
        assertTrue(files.containsKey("CN1WalletExtensionHandler.m"));
        assertEquals(4, files.size());
    }

    @Test
    public void uiFileMap_producesExpectedFiles() {
        Map<String, byte[]> files = validBuilder().buildUIFileMap();
        assertTrue(files.containsKey("Info.plist"));
        assertTrue(files.containsKey("WalletUIExtension.entitlements"));
        assertTrue(files.containsKey("CN1WalletAuthViewController.h"));
        assertTrue(files.containsKey("CN1WalletAuthViewController.m"));
        assertEquals(4, files.size());
    }

    @Test
    public void nonUIInfoPlist_hasExactExtensionPointAndPrincipalClass() {
        String plist = text(validBuilder().buildNonUIFileMap(), "Info.plist");
        // Whitespace inside NSExtension string values breaks the extension;
        // assert the exact single-line form.
        assertTrue(plist.contains("<string>com.apple.PassKit.issuer-provisioning</string>"),
                "exact non-UI extension point identifier missing");
        assertFalse(plist.contains("<string>com.apple.PassKit.issuer-provisioning </string>"));
        assertTrue(plist.contains("<string>CN1WalletExtensionHandler</string>"),
                "principal class missing");
        assertTrue(plist.contains("<key>CN1WalletAppGroup</key>"));
        assertTrue(plist.contains("<string>group.com.example.myapp.wallet</string>"));
        assertTrue(plist.contains("<key>CN1WalletIssuerEndpoint</key>"));
        assertTrue(plist.contains("<string>https://issuer.example.com/provision</string>"));
    }

    @Test
    public void uiInfoPlist_hasExactExtensionPointAndPrincipalClass() {
        String plist = text(validBuilder().buildUIFileMap(), "Info.plist");
        assertTrue(plist.contains("<string>com.apple.PassKit.issuer-provisioning.authorization</string>"),
                "exact UI extension point identifier missing");
        assertTrue(plist.contains("<string>CN1WalletAuthViewController</string>"));
        assertTrue(plist.contains("<key>CN1WalletAuthEndpoint</key>"));
        assertTrue(plist.contains("<string>https://issuer.example.com/login</string>"));
    }

    @Test
    public void entitlements_includePaymentPassProvisioningAndAppGroup() {
        String ent = text(validBuilder().buildNonUIFileMap(), "WalletNonUIExtension.entitlements");
        assertTrue(ent.contains("<key>com.apple.developer.payment-pass-provisioning</key>"));
        assertTrue(ent.contains("<true/>"));
        assertTrue(ent.contains("<string>group.com.example.myapp.wallet</string>"));
        assertFalse(ent.contains("application-identifier"),
                "application-identifier must be omitted unless a prefix is set");
    }

    @Test
    public void entitlements_includeApplicationIdentifierWhenPrefixSet() {
        String ent = text(validBuilder()
                .setApplicationIdentifierPrefix("TEAM123.com.example.myapp")
                .buildNonUIFileMap(), "WalletNonUIExtension.entitlements");
        assertTrue(ent.contains("<key>application-identifier</key>"));
        assertTrue(ent.contains("<string>TEAM123.com.example.myapp.WalletNonUIExtension</string>"));
    }

    @Test
    public void nonUISource_containsAllMarkers() {
        String source = text(validBuilder().buildNonUIFileMap(), "CN1WalletExtensionHandler.m");
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_NONUI_IMPORTS));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_STATUS));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_PASS_ENTRIES));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_REMOTE_PASS_ENTRIES));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_GENERATE_REQUEST));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_GENERATE_RESPONSE));
    }

    @Test
    public void uiSource_containsAllMarkers() {
        String source = text(validBuilder().buildUIFileMap(), "CN1WalletAuthViewController.m");
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_UI_IMPORTS));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_UI_VIEWDIDLOAD));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_UI_AUTH_REQUEST));
        assertTrue(source.contains(IOSWalletExtensionBuilder.MARKER_UI_AUTH_RESPONSE));
    }

    @Test
    public void injection_insertsCodeAndPreservesMarker() {
        String snippet = "NSLog(@\"custom status hook\");";
        String source = text(validBuilder()
                .setInjection(IOSWalletExtensionBuilder.MARKER_STATUS, snippet)
                .buildNonUIFileMap(), "CN1WalletExtensionHandler.m");
        int snippetIdx = source.indexOf(snippet);
        int markerIdx = source.indexOf(IOSWalletExtensionBuilder.MARKER_STATUS);
        assertTrue(snippetIdx >= 0, "injected snippet missing");
        assertTrue(markerIdx > snippetIdx, "marker must be preserved after the injected code");
    }

    @Test
    public void injection_unknownMarkerRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> validBuilder().setInjection("//NOT_A_MARKER", "code"));
    }

    @Test
    public void validation_rejectsMissingAppGroupPrefix() {
        assertThrows(IllegalStateException.class, () -> new IOSWalletExtensionBuilder()
                .setAppGroupId("com.example.nogroup")
                .setIssuerEndpoint("https://x.example.com")
                .buildNonUIFileMap());
    }

    @Test
    public void validation_rejectsMissingIssuerEndpoint() {
        assertThrows(IllegalStateException.class, () -> new IOSWalletExtensionBuilder()
                .setAppGroupId("group.com.example.myapp")
                .buildNonUIFileMap());
    }

    @Test
    public void validation_rejectsMissingAuthEndpointForUI() {
        assertThrows(IllegalStateException.class, () -> new IOSWalletExtensionBuilder()
                .setAppGroupId("group.com.example.myapp")
                .setIssuerEndpoint("https://x.example.com")
                .buildUIFileMap());
    }

    @Test
    public void validation_rejectsBadExtensionName() {
        assertThrows(IllegalStateException.class, () -> validBuilder()
                .setNonUIExtensionName("Bad Name!")
                .buildNonUIFileMap());
    }

    @Test
    public void customNames_flowIntoFileMapAndBundleArtifacts() {
        Map<String, byte[]> files = validBuilder()
                .setNonUIExtensionName("MyWalletExt")
                .buildNonUIFileMap();
        assertTrue(files.containsKey("MyWalletExt.entitlements"));
        String plist = text(files, "Info.plist");
        assertTrue(plist.contains("<string>MyWalletExt</string>"), "display name should use the custom name");
    }
}
