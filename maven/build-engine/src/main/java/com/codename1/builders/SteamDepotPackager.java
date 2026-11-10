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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

/// Lays a native Linux build out as a Steam depot, ready for SteamPipe.
///
/// ```
/// steam/
///   content/              the depot: uploaded as is
///     <Main>              the executable
///     launch.sh           the launch option to configure in Steamworks
///     libsteam_api.so     from the Steamworks SDK, when one was supplied
///     lib*.so             the libraries the build already bundles (libcn1ble)
///     steam_appid.txt     for running locally; excluded from the upload
///   scripts/
///     app_build_<app>.vdf    steamcmd +run_app_build scripts/app_build_<app>.vdf
///     depot_build_<depot>.vdf
///   output/               SteamPipe's build cache and logs
/// ```
///
/// Steamworks is loaded by the application at run time (`cn1_linux_steam.c`),
/// so a depot packaged with no SDK still runs; it just has no `GameServices`.
/// Nothing from the SDK is compiled in, and only its redistributable library is
/// copied.
public final class SteamDepotPackager {
    private SteamDepotPackager() {
    }

    /// What the packager needs, gathered by the builder.
    public static final class Input {
        File resultDir;
        File executable;
        String appId;
        String depotId;
        String branch;
        String description;
        boolean fullscreen;
        String arch;
        File sdkDir;

        Input(File resultDir, File executable, String appId) {
            this.resultDir = resultDir;
            this.executable = executable;
            this.appId = appId;
        }
    }

    /// The depot app id: the hint, else nothing (packaging refuses).
    static String appIdOf(String hint) {
        return hint == null ? null : hint.trim();
    }

    /// `steam.depotId`, defaulting to the app id + 1, the depot Steamworks
    /// creates alongside a new app.
    static String depotIdOf(String appId, String hint) {
        if (hint != null && hint.trim().length() > 0) {
            return hint.trim();
        }
        // Steam ids are 32-bit; anything else is rejected by packageDepot with the hint's name.
        if (appId == null || !appId.matches("[0-9]{1,10}")) {
            return appId;
        }
        return String.valueOf(Long.parseLong(appId) + 1);
    }

    /// Builds the depot tree under `resultDir/steam` and zips it to
    /// `resultDir/<Main>-steam.zip`, keeping the executable bits. Returns the zip.
    static File packageDepot(Input in, Logger log) throws BuildException {
        if (in.appId == null || !in.appId.matches("[0-9]{1,10}")) {
            throw new BuildException("The steam target needs the steam.appId build hint "
                    + "(the numeric app id from the Steamworks partner site); it is '" + in.appId + "'");
        }
        String depot = depotIdOf(in.appId, in.depotId);
        if (!depot.matches("[0-9]{1,10}")) {
            throw new BuildException("steam.depotId must be numeric; it is '" + depot + "'");
        }
        File steam = new File(in.resultDir, "steam");
        if (in.branch != null && ("public".equalsIgnoreCase(in.branch.trim())
                || "default".equalsIgnoreCase(in.branch.trim()))) {
            throw new BuildException("steam.branch cannot be the default branch: Valve sets it live only "
                    + "from the Steamworks site. Leave steam.branch empty, or name a beta branch.");
        }
        // A rebuild must not ship what the last one left behind (a library
        // since removed, a libsteam_api.so from an SDK no longer configured).
        deleteTree(steam);
        File content = new File(steam, "content");
        File scripts = new File(steam, "scripts");
        File output = new File(steam, "output");
        content.mkdirs();
        scripts.mkdirs();
        output.mkdirs();
        String main = in.executable.getName();
        // Spliced into launch.sh unquoted-safe only for plain file names.
        if (!main.matches("[A-Za-z0-9_.-]+")) {
            throw new BuildException("The executable name cannot go in launch.sh: " + main);
        }
        try {
            copy(in.executable, new File(content, main));
            new File(content, main).setExecutable(true, false);
            File[] siblings = in.resultDir.listFiles();
            if (siblings != null) {
                for (File f : siblings) {
                    if (f.isFile() && f.getName().endsWith(".so")) {
                        copy(f, new File(content, f.getName()));
                    }
                }
            }
            File steamLib = steamApiLibrary(in.sdkDir, in.arch);
            if (steamLib != null) {
                copy(steamLib, new File(content, "libsteam_api.so"));
                log.log("Steam: bundled " + steamLib.getAbsolutePath());
            } else {
                log.log("Steam: no Steamworks SDK supplied (steam.sdkPath / STEAMWORKS_SDK); the depot is "
                        + "packaged without libsteam_api.so and GameServices will report unavailable.");
            }
            write(new File(content, "launch.sh"), launchScript(main, in.appId, in.fullscreen));
            new File(content, "launch.sh").setExecutable(true, false);
            write(new File(content, "steam_appid.txt"), in.appId + "\n");
            write(new File(scripts, "depot_build_" + depot + ".vdf"), depotVdf(depot));
            write(new File(scripts, "app_build_" + in.appId + ".vdf"),
                    appVdf(in.appId, depot, in.description, in.branch));
            File zip = new File(in.resultDir, main + "-steam.zip");
            zipTree(steam, zip, main);
            log.log("Steam depot: " + content.getAbsolutePath() + " (app " + in.appId + ", depot " + depot + ")");
            return zip;
        } catch (IOException ex) {
            throw new BuildException("Failed to package the Steam depot", ex);
        }
    }

    /// The SDK's redistributable library for the architecture, or null.
    static File steamApiLibrary(File sdkDir, String arch) {
        if (sdkDir == null) {
            return null;
        }
        String sub = "arm64".equals(arch) ? "linuxarm64" : "linux64";
        String[] candidates = {
            "redistributable_bin/" + sub + "/libsteam_api.so",
            "sdk/redistributable_bin/" + sub + "/libsteam_api.so",
        };
        for (String c : candidates) {
            File f = new File(sdkDir, c);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    static String launchScript(String main, String appId, boolean fullscreen) {
        return "#!/bin/sh\n"
                + "# The launch option to configure in Steamworks (Installation > General).\n"
                + "HERE=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n"
                + "export LD_LIBRARY_PATH=\"$HERE${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}\"\n"
                + "export CN1_STEAM_APPID=" + appId + "\n"
                + (fullscreen ? "export CN1_FULLSCREEN=1\n" : "")
                + "exec \"$HERE/" + main + "\" \"$@\"\n";
    }

    static String depotVdf(String depot) {
        return "\"DepotBuild\"\n{\n"
                + "\t\"DepotID\" \"" + depot + "\"\n"
                + "\t\"FileMapping\"\n\t{\n"
                + "\t\t\"LocalPath\" \"*\"\n"
                + "\t\t\"DepotPath\" \".\"\n"
                + "\t\t\"Recursive\" \"1\"\n"
                + "\t}\n"
                + "\t\"FileExclusion\" \"steam_appid.txt\"\n"
                + "\t\"FileExclusion\" \"*.debug\"\n"
                + "}\n";
    }

    static String appVdf(String appId, String depot, String description, String branch) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"AppBuild\"\n{\n");
        sb.append("\t\"AppID\" \"").append(appId).append("\"\n");
        sb.append("\t\"Desc\" \"").append(vdfEscape(description == null ? "" : description)).append("\"\n");
        sb.append("\t\"ContentRoot\" \"../content/\"\n");
        sb.append("\t\"BuildOutput\" \"../output/\"\n");
        if (branch != null && branch.trim().length() > 0) {
            // Valve refuses SetLive on the default branch from an upload.
            sb.append("\t\"SetLive\" \"").append(vdfEscape(branch.trim())).append("\"\n");
        }
        sb.append("\t\"Depots\"\n\t{\n");
        sb.append("\t\t\"").append(depot).append("\" \"depot_build_").append(depot).append(".vdf\"\n");
        sb.append("\t}\n}\n");
        return sb.toString();
    }

    static String vdfEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n' || c == '\r') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Zips the tree with Unix modes, so the executable and launch.sh stay
    /// executable wherever the archive is unpacked for upload.
    private static void zipTree(File root, File zip, String main) throws IOException {
        try (OutputStream fo = new FileOutputStream(zip); ZipOutputStream zo = new ZipOutputStream(fo)) {
            addTree(zo, root, "steam/", main);
        }
    }

    private static void addTree(ZipOutputStream zo, File dir, String prefix, String main) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files);
        for (File f : files) {
            String name = prefix + f.getName();
            if (f.isDirectory()) {
                ZipEntry e = new ZipEntry(name + "/");
                e.setUnixMode(040755);
                zo.putNextEntry(e);
                zo.closeEntry();
                addTree(zo, f, name + "/", main);
                continue;
            }
            ZipEntry e = new ZipEntry(name);
            boolean exec = f.getName().equals(main) || f.getName().equals("launch.sh");
            e.setUnixMode(exec ? 0100755 : 0100644);
            zo.putNextEntry(e);
            try (InputStream is = new FileInputStream(f)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = is.read(buf)) > 0) {
                    zo.write(buf, 0, n);
                }
            }
            zo.closeEntry();
        }
    }

    private static void copy(File from, File to) throws IOException {
        java.nio.file.Files.copy(from.toPath(), to.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void write(File f, String text) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(text);
        }
    }

    /// The builder's log, so this class needs no Executor.
    interface Logger {
        void log(String message);
    }

    /** Deletes without following links: a link inside is removed, never what it points at. */
    private static void deleteTree(File f) {
        if (!java.nio.file.Files.isSymbolicLink(f.toPath())) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteTree(k);
                }
            }
        }
        f.delete();
    }
}
