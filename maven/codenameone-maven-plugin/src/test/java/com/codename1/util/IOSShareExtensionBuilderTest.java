package com.codename1.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IOSShareExtensionBuilderTest {

    private IOSShareExtensionBuilder validBuilder() {
        return new IOSShareExtensionBuilder()
                .setExtensionName("MyShareExtension")
                .setDisplayName("Share to MyApp")
                .setHostBundleId("com.example.myapp")
                .setAppGroupId("group.com.example.myapp.shared")
                .acceptText(true)
                .acceptURLs(true)
                .acceptImages(true);
    }

    @Test
    public void buildFileMap_producesExpectedFiles() {
        Map<String, byte[]> files = validBuilder().buildFileMap();
        Set<String> names = new HashSet<>(files.keySet());
        assertTrue(names.contains("Info.plist"), "Info.plist must be present");
        assertTrue(names.contains("MyShareExtension.entitlements"),
                "entitlements file must be named after the extension");
        assertTrue(names.contains("ShareViewController.swift"),
                "Swift controller must be present");
        assertTrue(names.contains("buildSettings.properties"),
                "buildSettings.properties must be present");
    }

    @Test
    public void infoPlist_includesNSExtensionAndActivationRules() {
        String plist = new String(
                validBuilder().buildFileMap().get("Info.plist"), StandardCharsets.UTF_8);
        assertTrue(plist.contains("com.apple.share-services"),
                "share-services point identifier missing");
        assertTrue(plist.contains("ShareViewController"),
                "principal class must reference ShareViewController");
        assertTrue(plist.contains("NSExtensionActivationSupportsText"),
                "text activation rule missing");
        assertTrue(plist.contains("NSExtensionActivationSupportsWebURLWithMaxCount"),
                "url activation rule missing");
        assertTrue(plist.contains("NSExtensionActivationSupportsImageWithMaxCount"),
                "image activation rule missing");
        assertTrue(plist.contains("Share to MyApp"),
                "display name missing from plist");
    }

    @Test
    public void entitlements_includesAppGroup() {
        String ent = new String(
                validBuilder().buildFileMap().get("MyShareExtension.entitlements"),
                StandardCharsets.UTF_8);
        assertTrue(ent.contains("com.apple.security.application-groups"),
                "application-groups key missing");
        assertTrue(ent.contains("group.com.example.myapp.shared"),
                "app group id missing");
    }

    @Test
    public void swiftSource_writesPayloadToAppGroupUserDefaults() {
        String swift = new String(
                validBuilder().buildFileMap().get("ShareViewController.swift"),
                StandardCharsets.UTF_8);
        assertTrue(swift.contains("SLComposeServiceViewController"),
                "must subclass SLComposeServiceViewController");
        assertTrue(swift.contains("UserDefaults(suiteName:"),
                "must use suiteName-based UserDefaults");
        assertTrue(swift.contains("group.com.example.myapp.shared"),
                "must reference configured app group");
        assertTrue(swift.contains(IOSShareExtensionBuilder.PAYLOAD_KEY),
                "must reference the payload key");
        assertTrue(swift.contains("completeRequest"),
                "must complete the extension request");
    }

    @Test
    public void buildSettings_referencesInfoAndEntitlements() {
        String props = new String(
                validBuilder().buildFileMap().get("buildSettings.properties"),
                StandardCharsets.UTF_8);
        assertTrue(props.contains("INFOPLIST_FILE=MyShareExtension/Info.plist"),
                "INFOPLIST_FILE must point to the extension's Info.plist");
        assertTrue(props.contains("CODE_SIGN_ENTITLEMENTS=MyShareExtension/MyShareExtension.entitlements"),
                "CODE_SIGN_ENTITLEMENTS must point to the extension's entitlements");
    }

    @Test
    public void writeTo_writesAllFilesToDirectory(@TempDir Path tmp) throws Exception {
        File outDir = new File(tmp.toFile(), "MyShareExtension");
        Map<String, byte[]> files = validBuilder().writeTo(outDir);
        assertEquals(4, files.size());
        for (String name : files.keySet()) {
            File f = new File(outDir, name);
            assertTrue(f.exists(), name + " must exist on disk");
            assertTrue(f.length() > 0, name + " must not be empty");
        }
    }

    @Test
    public void writeAppext_producesReadableZipArchive(@TempDir Path tmp) throws Exception {
        File zip = new File(tmp.toFile(), "MyShareExtension.ios.appext");
        Map<String, byte[]> files = validBuilder().writeAppext(zip);
        assertTrue(zip.exists());
        assertTrue(zip.length() > 0);

        Set<String> seen = new HashSet<>();
        try (FileInputStream fis = new FileInputStream(zip);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                seen.add(entry.getName());
            }
        }
        assertEquals(files.keySet(), seen,
                "zip entries must match the file map exactly");
    }

    @Test
    public void validation_rejectsMissingHostBundleId() {
        IOSShareExtensionBuilder b = new IOSShareExtensionBuilder()
                .setExtensionName("X")
                .setAppGroupId("group.x")
                .acceptText(true);
        assertThrows(IllegalStateException.class, b::buildFileMap);
    }

    @Test
    public void validation_rejectsAppGroupMissingGroupPrefix() {
        IOSShareExtensionBuilder b = validBuilder().setAppGroupId("com.example.bogus");
        assertThrows(IllegalStateException.class, b::buildFileMap);
    }

    @Test
    public void validation_rejectsAllAcceptanceRulesDisabled() {
        IOSShareExtensionBuilder b = validBuilder()
                .acceptText(false).acceptURLs(false).acceptImages(false);
        assertThrows(IllegalStateException.class, b::buildFileMap);
    }

    @Test
    public void validation_rejectsNonIdentifierExtensionName() {
        IOSShareExtensionBuilder b = validBuilder().setExtensionName("My Share!");
        assertThrows(IllegalStateException.class, b::buildFileMap);
    }
}
