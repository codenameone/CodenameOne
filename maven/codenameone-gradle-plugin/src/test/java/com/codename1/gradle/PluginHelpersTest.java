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
package com.codename1.gradle;

import com.codename1.build.BuildArtifact;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PluginHelpersTest {
    @Test
    void cn1UpdateRewritesOnlyThePluginVersion() {
        String kts = "pluginManagement { repositories { gradlePluginPortal() } }\n"
                + "plugins {\n    id(\"com.codenameone\") version \"8.0.1\"\n    kotlin(\"jvm\") version \"2.2.10\"\n}\n";
        String updated = UpdateSupport.withPluginVersion(kts, "8.0.2");
        assertEquals(kts.replace("\"8.0.1\"", "\"8.0.2\""), updated);

        String groovy = "plugins {\n    id 'com.codenameone' version '8.0.1'\n}\n";
        assertEquals(groovy.replace("8.0.1", "9.0"), UpdateSupport.withPluginVersion(groovy, "9.0"));

        assertNull(UpdateSupport.withPluginVersion("plugins { id(\"com.codenameone\") }\n", "1.0"),
                "a script that declares no version is left to the user");
        assertNull(UpdateSupport.withPluginVersion("plugins { id(\"com.codenameone.other\") version \"1\" }", "2"));
    }

    @Test
    void theLatestReleaseComesFromTheMarkerMetadata() {
        String metadata = "<metadata><versioning><latest>8.1-SNAPSHOT</latest><release>8.0.2</release>"
                + "</versioning></metadata>";
        assertEquals("8.0.2", UpdateSupport.releaseOf(metadata));
        assertEquals("8.1", UpdateSupport.releaseOf("<metadata><latest> 8.1 </latest></metadata>"));
        assertNull(UpdateSupport.releaseOf("<metadata/>"));
    }

    @Test
    void aResolvedDependencyRoundTripsThroughATaskInput() {
        BuildArtifact a = GradleProjectHost.decode("com.acme|maps-common|1.2|cn1css|zip|compile|/r/a|b.zip");
        assertEquals("com.acme", a.getGroupId());
        assertEquals("maps-common", a.getArtifactId());
        assertEquals("1.2", a.getVersion());
        assertEquals("cn1css", a.getClassifier());
        assertEquals("zip", a.getType());
        assertEquals("compile", a.getScope());
        assertEquals(new File("/r/a|b.zip"), a.getFile(), "the path is the last field and may hold anything");

        assertNull(GradleProjectHost.decode("com.acme|x|1||jar|compile|/r/x.jar").getClassifier());
        assertNull(GradleProjectHost.decode("too|few|fields"));
        assertNull(GradleProjectHost.decode(null));
    }

    @Test
    void backendArgumentsSplitOnWhitespace() {
        assertEquals(Arrays.asList("-Xmx1g", "-Dx=y"), BackendSupport.split("  -Xmx1g \t -Dx=y "));
        assertEquals(Collections.emptyList(), BackendSupport.split("   "));
    }

    /// Cached native outputs are rebuilt when a build hint changes, however it
    /// arrives; the recorded fingerprint never holds a hint's value in the clear.
    @Test
    void theBuildHintFingerprintTracksEveryHint() {
        Properties a = new Properties();
        a.setProperty("codename1.arg.android.xpermissions", "CAMERA");
        a.setProperty("codename1.arg.ios.certificatePassword", "s3cret");
        Properties reordered = new Properties();
        reordered.setProperty("codename1.arg.ios.certificatePassword", "s3cret");
        reordered.setProperty("codename1.arg.android.xpermissions", "CAMERA");
        Properties changed = new Properties();
        changed.putAll(a);
        changed.setProperty("codename1.arg.android.xpermissions", "CAMERA,NFC");

        String fingerprint = GradleProjectHost.hintsFingerprint(a);
        assertEquals(fingerprint, GradleProjectHost.hintsFingerprint(reordered));
        org.junit.jupiter.api.Assertions.assertNotEquals(fingerprint, GradleProjectHost.hintsFingerprint(changed));
        org.junit.jupiter.api.Assertions.assertFalse(fingerprint.contains("s3cret"));
    }

    /// codenameone { mainClass } overrides the settings file's two keys.
    @Test
    void theMainClassExtensionSplitsIntoPackageAndName() {
        java.util.Map<String, String> p = ProjectSupport.mainClassProperties("com.acme.app.OtherApp");
        assertEquals("com.acme.app", p.get("codename1.packageName"));
        assertEquals("OtherApp", p.get("codename1.mainName"));
        assertEquals(Collections.emptyMap(), ProjectSupport.mainClassProperties(""));
        assertEquals("", ProjectSupport.mainClassProperties("Bare").get("codename1.packageName"));
    }

    @Test
    void eachPlatformHasItsOwnCn1libConfiguration() {
        assertEquals("cn1libIos", Cn1libs.configurationName("ios"));
        assertEquals("cn1libJavascript", Cn1libs.configurationName("javascript"));
    }
}
