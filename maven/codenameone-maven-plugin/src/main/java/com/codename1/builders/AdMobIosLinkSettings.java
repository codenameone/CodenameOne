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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Link settings needed by the Google Mobile Ads pod's prebuilt Swift objects. */
final class AdMobIosLinkSettings {
    private AdMobIosLinkSettings() {
    }

    static void apply(File project, String pods) throws IOException {
        boolean usesAdMob = false;
        if (pods != null) {
            for (String pod : pods.split("[,;]")) {
                if ("Google-Mobile-Ads-SDK".equals(pod.trim().split("\\s+", 2)[0])) {
                    usesAdMob = true;
                    break;
                }
            }
        }
        if (!usesAdMob) {
            return;
        }
        String contents = new String(Files.readAllBytes(project.toPath()), StandardCharsets.UTF_8);
        // TOOLCHAIN_DIR can select the separately installed Metal toolchain.
        String swiftPath = "$(DEVELOPER_DIR)/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$(PLATFORM_NAME)";
        if (contents.contains(swiftPath)) {
            return;
        }
        String anchor = "LIBRARY_SEARCH_PATHS = (";
        if (!contents.contains(anchor)) {
            throw new IOException("Cannot add AdMob link paths: generated project has no library search paths");
        }
        contents = contents.replace(anchor, anchor + "\n\t\t\t\t\t\"" + swiftPath
                + "\",\n\t\t\t\t\t\"$(SDKROOT)/usr/lib/swift\",");
        Files.write(project.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
}
