/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gamebuilder.project;

import com.codename1.gamebuilder.editor.CompanionCodeGen;
import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.AssetPack;
import com.codename1.gaming.level.GameLevel;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Reads and writes a project's game scenes through `com.codename1.io.FileSystemStorage`
/// (the Codename One file API, so it stays inside the device subset and works on the
/// desktop editor). The binding path is handed to the forked editor by the
/// {@code cn1:gamebuilder} goal via the {@code gamebuilder.input} system property.
public final class ProjectIO {
    public static final String INPUT_PROPERTY = "gamebuilder.input";

    private ProjectIO() {
    }

    /// Loads the project binding the Maven goal wrote, or null when running standalone
    /// (no binding property / unreadable file).
    public static ProjectBinding loadBinding() {
        String path = System.getProperty(INPUT_PROPERTY);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        InputStream in = null;
        try {
            String url = fsUrl(path);
            if (!fs.exists(url)) {
                return null;
            }
            in = fs.openInputStream(url);
            ProjectBinding b = ProjectBinding.parse(Util.readToString(in, "UTF-8"));
            return b.isValid() ? b : null;
        } catch (IOException e) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    /// The scene names (without the {@code .game} extension) found in the games dir.
    public static List<String> listScenes(ProjectBinding binding) {
        List<String> out = new ArrayList<>();
        FileSystemStorage fs = FileSystemStorage.getInstance();
        try {
            if (binding == null || !fs.isDirectory(fsUrl(binding.gamesDir()))) {
                return out;
            }
            String[] names = fs.listFiles(fsUrl(binding.gamesDir()));
            for (int i = 0; i < names.length; i++) {
                String n = names[i];
                if (n.endsWith(".game")) {
                    out.add(n.substring(0, n.length() - ".game".length()));
                }
            }
        } catch (IOException e) {
            // return whatever we have
        }
        return out;
    }

    public static GameLevel loadScene(ProjectBinding binding, String sceneName) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = join(fsUrl(binding.gamesDir()), sceneName + ".game");
        InputStream in = null;
        try {
            in = fs.openInputStream(path);
            return GameLevel.load(in);
        } finally {
            Util.cleanup(in);
        }
    }

    /// Saves the level JSON to {@code <gamesDir>/<scene>.game}, and writes the companion
    /// {@code <scene>.java} into the source tree if it does not exist yet (existing
    /// companions are left alone so hand-written game logic is preserved).
    public static void saveScene(ProjectBinding binding, EditorModel model, String packageName)
            throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String scene = model.getSceneName();
        String gamesUrl = fsUrl(binding.gamesDir());
        mkdirs(fs, gamesUrl);
        writeString(fs, join(gamesUrl, scene + ".game"), CompanionCodeGen.gameData(model));

        if (binding.sourceDir() != null) {
            String pkgPath = packageName == null || packageName.isEmpty()
                    ? "" : packageName.replace('.', '/');
            String dir = pkgPath.isEmpty() ? fsUrl(binding.sourceDir()) : join(fsUrl(binding.sourceDir()), pkgPath);
            mkdirs(fs, dir);
            String javaPath = join(dir, scene + ".java");
            if (!fs.exists(javaPath)) {
                String res = "/games/" + scene + ".game";
                writeString(fs, javaPath, CompanionCodeGen.companionJava(packageName, scene, res));
            }
        }
    }

    // ---- custom (imported) assets -------------------------------------------

    public static final String CUSTOM_PACK_ID = "custom";
    private static final String CUSTOM_PACK_FILE = "custompack.json";
    private static final String ASSETS_DIR = "assets";

    /// Writes imported artwork to {@code <gamesDir>/assets/<id>.png} so a custom asset
    /// survives save/reload and ships with the project's resources.
    public static void saveCustomAsset(ProjectBinding binding, String id, byte[] png) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = join(fsUrl(binding.gamesDir()), ASSETS_DIR);
        mkdirs(fs, dir);
        OutputStream out = null;
        try {
            out = fs.openOutputStream(join(dir, id + ".png"));
            out.write(png);
            out.flush();
        } finally {
            Util.cleanup(out);
        }
    }

    /// Persists the custom pack definition (a {@code {"packs":[..]}} document) to
    /// {@code <gamesDir>/custompack.json}.
    public static void saveCustomPack(ProjectBinding binding, String packsJson) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String gamesUrl = fsUrl(binding.gamesDir());
        mkdirs(fs, gamesUrl);
        writeString(fs, join(gamesUrl, CUSTOM_PACK_FILE), packsJson);
    }

    /// Merges the project's custom pack into the catalog (if present) and loads each
    /// imported image from {@code <gamesDir>/assets/<id>.png}. Safe to call when no
    /// custom pack exists yet (no-op). Returns the loaded custom pack, or null.
    public static AssetPack loadCustomPack(ProjectBinding binding, AssetCatalog catalog) {
        if (binding == null) {
            return null;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String packPath = join(fsUrl(binding.gamesDir()), CUSTOM_PACK_FILE);
        InputStream in = null;
        try {
            if (!fs.exists(packPath)) {
                return null;
            }
            in = fs.openInputStream(packPath);
            AssetCatalog tmp = AssetCatalog.load(in);
            AssetPack pack = tmp.getPack(CUSTOM_PACK_ID);
            if (pack == null) {
                return null;
            }
            catalog.addPack(pack);
            String assetsDir = join(fsUrl(binding.gamesDir()), ASSETS_DIR);
            for (AssetDef d : pack.assets()) {
                String imgPath = join(assetsDir, d.getId() + ".png");
                if (fs.exists(imgPath)) {
                    byte[] bytes = readAll(fs, imgPath);
                    if (bytes != null) {
                        catalog.setImage(d.getId(), Image.createImage(bytes, 0, bytes.length));
                    }
                }
            }
            return pack;
        } catch (IOException e) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    private static byte[] readAll(FileSystemStorage fs, String path) throws IOException {
        InputStream in = null;
        try {
            in = fs.openInputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            Util.cleanup(in);
        }
    }

    // ---- helpers -------------------------------------------------------------

    private static void writeString(FileSystemStorage fs, String path, String content) throws IOException {
        OutputStream out = null;
        try {
            out = fs.openOutputStream(path);
            out.write(content.getBytes("UTF-8"));
            out.flush();
        } finally {
            Util.cleanup(out);
        }
    }

    /// Recursively creates a directory and its parents (FileSystemStorage#mkdir only
    /// makes the leaf). Operates on `file:` URLs and stops at the URL scheme root.
    private static void mkdirs(FileSystemStorage fs, String url) {
        if (url == null) {
            return;
        }
        try {
            if (fs.isDirectory(url)) {
                return;
            }
        } catch (RuntimeException ignore) {
            // unfile() rejects paths above the scheme root; nothing to create there
        }
        int sep = url.lastIndexOf('/');
        if (sep > 7) {   // keep the "file://" scheme intact
            mkdirs(fs, url.substring(0, sep));
        }
        try {
            if (!fs.exists(url)) {
                fs.mkdir(url);
            }
        } catch (RuntimeException ignore) {
            // a parent above the project root we cannot/shouldn't create
        }
    }

    private static String join(String dir, String child) {
        if (dir.endsWith("/")) {
            return dir + child;
        }
        return dir + "/" + child;
    }

    /// Converts an OS path (as written into the binding by the Maven goal) to the
    /// `file:` URL `FileSystemStorage` requires. Pass-through if already a URL.
    private static String fsUrl(String osPath) {
        if (osPath == null || osPath.startsWith("file:")) {
            return osPath;
        }
        String s = osPath.replace('\\', '/');
        return s.startsWith("/") ? "file://" + s : "file:///" + s;
    }
}
