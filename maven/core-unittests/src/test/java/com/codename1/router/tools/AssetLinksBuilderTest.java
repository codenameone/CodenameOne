package com.codename1.router.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssetLinksBuilderTest {

    @Test
    void singleAppEntry() {
        String json = new AssetLinksBuilder()
                .addApp("com.example.app", "AB:CD:EF")
                .build();
        assertTrue(json.contains("\"com.example.app\""));
        assertTrue(json.contains("\"AB:CD:EF\""));
        assertTrue(json.contains("delegate_permission/common.handle_all_urls"));
    }

    @Test
    void additionalFingerprintAttachesToLastApp() {
        String json = new AssetLinksBuilder()
                .addApp("com.example.app", "AAA")
                .addFingerprint("BBB")
                .build();
        // both fingerprints should appear in the same array
        int aaa = json.indexOf("\"AAA\"");
        int bbb = json.indexOf("\"BBB\"");
        assertTrue(aaa > 0 && bbb > 0 && bbb > aaa);
    }

    @Test
    void addAppRequiresFingerprint() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new AssetLinksBuilder().addApp("p", ""); }
        });
    }

    @Test
    void addFingerprintBeforeAppThrows() {
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new AssetLinksBuilder().addFingerprint("AAA"); }
        });
    }
}
