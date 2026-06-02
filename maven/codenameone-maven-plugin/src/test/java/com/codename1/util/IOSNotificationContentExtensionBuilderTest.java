package com.codename1.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class IOSNotificationContentExtensionBuilderTest {

    private IOSNotificationContentExtensionBuilder validBuilder() {
        return new IOSNotificationContentExtensionBuilder()
                .setExtensionName("MyNotifContent")
                .setDisplayName("Custom Notification")
                .setCategory("cn1-ln-reminder")
                .setAppGroupId("group.com.example.myapp.shared");
    }

    @Test
    void buildFileMapProducesExpectedFiles() {
        Map<String, byte[]> files = validBuilder().buildFileMap();
        Set<String> names = new HashSet<String>(files.keySet());
        assertTrue(names.contains("Info.plist"));
        assertTrue(names.contains("NotificationViewController.swift"));
        assertTrue(names.contains("buildSettings.properties"));
        assertTrue(names.contains("MyNotifContent.entitlements"));
    }

    @Test
    void infoPlistDeclaresContentExtensionPointAndCategory() {
        Map<String, byte[]> files = validBuilder().buildFileMap();
        String plist = new String(files.get("Info.plist"), StandardCharsets.UTF_8);
        assertTrue(plist.contains("com.apple.usernotifications.content"));
        assertTrue(plist.contains("<key>UNNotificationExtensionCategory</key>"));
        assertTrue(plist.contains("cn1-ln-reminder"));
        assertTrue(plist.contains("NotificationViewController"));
    }

    @Test
    void entitlementsOmittedWithoutAppGroup() {
        Map<String, byte[]> files = new IOSNotificationContentExtensionBuilder()
                .setExtensionName("NoGroup")
                .setCategory("c")
                .buildFileMap();
        assertFalse(files.containsKey("NoGroup.entitlements"));
        String settings = new String(files.get("buildSettings.properties"), StandardCharsets.UTF_8);
        assertFalse(settings.contains("CODE_SIGN_ENTITLEMENTS"));
    }

    @Test
    void invalidExtensionNameRejected() {
        assertThrows(IllegalStateException.class, () ->
                new IOSNotificationContentExtensionBuilder().setExtensionName("bad name").setCategory("c").buildFileMap());
    }

    @Test
    void appGroupMustStartWithGroupPrefix() {
        assertThrows(IllegalStateException.class, () ->
                new IOSNotificationContentExtensionBuilder()
                        .setExtensionName("Ext").setCategory("c").setAppGroupId("com.bad").buildFileMap());
    }

    @Test
    void writeAppextProducesRootEntries(@TempDir Path tmp) throws Exception {
        File appext = tmp.resolve("MyNotifContent.ios.appext").toFile();
        validBuilder().writeAppext(appext);
        assertTrue(appext.exists());
        Set<String> entries = new HashSet<String>();
        ZipInputStream zis = new ZipInputStream(new FileInputStream(appext));
        try {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                entries.add(e.getName());
            }
        } finally {
            zis.close();
        }
        assertTrue(entries.contains("Info.plist"));
        assertTrue(entries.contains("NotificationViewController.swift"));
    }

    @Test
    void outputIsDeterministic() {
        Map<String, byte[]> a = validBuilder().buildFileMap();
        Map<String, byte[]> b = validBuilder().buildFileMap();
        assertArrayEquals(a.get("Info.plist"), b.get("Info.plist"));
        assertArrayEquals(a.get("NotificationViewController.swift"), b.get("NotificationViewController.swift"));
    }
}
