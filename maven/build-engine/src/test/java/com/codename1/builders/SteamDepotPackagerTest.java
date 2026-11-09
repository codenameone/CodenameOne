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

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteamDepotPackagerTest {
    @TempDir
    File tmp;

    private SteamDepotPackager.Input input(String appId) throws Exception {
        File result = new File(tmp, "result");
        result.mkdirs();
        File exe = new File(result, "MyGame");
        Files.write(exe.toPath(), new byte[]{0x7f, 'E', 'L', 'F'});
        Files.write(new File(result, "libcn1ble.so").toPath(), new byte[]{1});
        SteamDepotPackager.Input in = new SteamDepotPackager.Input(result, exe, appId);
        in.description = "My \"Game\" 1.0";
        in.fullscreen = true;
        in.arch = "x64";
        return in;
    }

    @Test
    void laysOutDepotAndScripts() throws Exception {
        SteamDepotPackager.Input in = input("480");
        File sdk = new File(tmp, "sdk/redistributable_bin/linux64");
        sdk.mkdirs();
        Files.write(new File(sdk, "libsteam_api.so").toPath(), new byte[]{2});
        in.sdkDir = new File(tmp, "sdk");
        in.branch = "beta";
        File zip = SteamDepotPackager.packageDepot(in, m -> { });

        File content = new File(in.resultDir, "steam/content");
        assertTrue(new File(content, "MyGame").canExecute());
        assertTrue(new File(content, "launch.sh").canExecute());
        assertTrue(new File(content, "libsteam_api.so").isFile());
        assertTrue(new File(content, "libcn1ble.so").isFile());
        assertEquals("480\n", read(new File(content, "steam_appid.txt")));
        String launch = read(new File(content, "launch.sh"));
        assertTrue(launch.contains("CN1_STEAM_APPID=480"));
        assertTrue(launch.contains("CN1_FULLSCREEN=1"));

        String app = read(new File(in.resultDir, "steam/scripts/app_build_480.vdf"));
        assertTrue(app.contains("\"SetLive\" \"beta\""));
        assertTrue(app.contains("\"481\" \"depot_build_481.vdf\""));
        assertTrue(app.contains("My \\\"Game\\\" 1.0"));
        String depot = read(new File(in.resultDir, "steam/scripts/depot_build_481.vdf"));
        assertTrue(depot.contains("\"FileExclusion\" \"steam_appid.txt\""));

        Map<String, Integer> modes = new HashMap<>();
        try (ZipFile zf = new ZipFile(zip)) {
            Enumeration<ZipEntry> e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                modes.put(ze.getName(), ze.getUnixMode());
            }
        }
        assertEquals(0100755, (int) modes.get("steam/content/MyGame"));
        assertEquals(0100755, (int) modes.get("steam/content/launch.sh"));
        assertEquals(0100644, (int) modes.get("steam/content/steam_appid.txt"));
    }

    @Test
    void defaultBranchIsNeverSetLive() throws Exception {
        SteamDepotPackager.Input in = input("480");
        in.depotId = "999";
        SteamDepotPackager.packageDepot(in, m -> { });
        String app = read(new File(in.resultDir, "steam/scripts/app_build_480.vdf"));
        assertFalse(app.contains("SetLive"));
        assertNotNull(new File(in.resultDir, "steam/scripts/depot_build_999.vdf"));
        assertFalse(new File(in.resultDir, "steam/content/libsteam_api.so").exists());
    }

    @Test
    void refusesMissingAppId() throws Exception {
        assertThrows(BuildException.class, () -> SteamDepotPackager.packageDepot(input(null), m -> { }));
        assertThrows(BuildException.class, () -> SteamDepotPackager.packageDepot(input("abc"), m -> { }));
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
