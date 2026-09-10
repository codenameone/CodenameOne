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
package com.codename1.builders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdMobIosLinkSettingsTest {
    @TempDir Path temp;

    private byte[] template() throws Exception {
        return Files.readAllBytes(Paths.get("../../vm/ByteCodeTranslator/src/template/template.xcodeproj/project.pbxproj"));
    }

    @Test
    void ordinaryAndUnrelatedPodProjectsRemainByteForByteUnchanged() throws Exception {
        byte[] original = template();
        assertFalse(new String(original, StandardCharsets.UTF_8).contains("/usr/lib/swift"));
        Path project = temp.resolve("project.pbxproj");
        for (String pods : new String[]{null, "", "Alamofire", "Google-Mobile-Ads-SDK-Other ~> 13.0"}) {
            Files.write(project, original);
            AdMobIosLinkSettings.apply(project.toFile(), pods);
            assertArrayEquals(original, Files.readAllBytes(project), String.valueOf(pods));
        }
    }

    @Test
    void adMobGetsPathsInBothConfigurationsAndPreservesExistingPaths() throws Exception {
        Path project = temp.resolve("project.pbxproj");
        for (String pods : new String[]{"Google-Mobile-Ads-SDK ~> 13.0",
                "OtherPod, Google-Mobile-Ads-SDK ~> 13.0; ThirdPod", "Google-Mobile-Ads-SDK"}) {
            Files.write(project, template());
            AdMobIosLinkSettings.apply(project.toFile(), pods);
            String result = new String(Files.readAllBytes(project), StandardCharsets.UTF_8);
            assertEquals(2, result.split("XcodeDefault.xctoolchain/usr/lib/swift", -1).length - 1);
            assertEquals(2, result.split("/usr/lib/swift\",", -1).length - 1);
            assertTrue(result.contains("$(PROJECT_DIR)/template-src"));
            assertTrue(result.contains("$(inherited)"));
            byte[] first = Files.readAllBytes(project);
            AdMobIosLinkSettings.apply(project.toFile(), pods);
            assertArrayEquals(first, Files.readAllBytes(project));
        }
    }
}
