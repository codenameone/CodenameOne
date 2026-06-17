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
package com.codename1.gamebuilder;

import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gamebuilder.project.ProjectBinding;
import com.codename1.gamebuilder.project.ProjectIO;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.GameLevel;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/// End-to-end check of the project binding + file IO: boots CN1, points a binding at a
/// temp project dir, saves a scene, lists it back, reloads it, and verifies the companion
/// Java was written. Run with the harness idiom (see {@link ScreenshotHarness}).
public final class ProjectIOHarness {
    public static void main(String[] args) throws Exception {
        Display.init(null);

        File baseDir = new File("target/itest-project").getAbsoluteFile();
        deleteRecursive(baseDir);
        String games = new File(baseDir, "games").getAbsolutePath();
        String src = new File(baseDir, "src").getAbsolutePath();
        ProjectBinding binding = ProjectBinding.parse(
                "gamesDir=" + games + "\nsourceDir=" + src + "\npackageName=com.example.demo\n");

        EditorModel model = new EditorModel(StarterPacks.demoLevel(), StarterPacks.loadCatalog());
        model.setSceneName("Arena");

        // FileSystemStorage IO does not require the EDT; run it directly.
        ProjectIO.saveScene(binding, model, "com.example.demo");

        List<String> scenes = ProjectIO.listScenes(binding);
        GameLevel reloaded = ProjectIO.loadScene(binding, "Arena");
        File gameFile = new File(games, "Arena.game");
        File javaFile = new File(src, "com/example/demo/Arena.java");

        boolean sceneOk = scenes.contains("Arena")
                && reloaded.elements().size() == model.level().elements().size()
                && gameFile.isFile() && javaFile.isFile();

        // --- custom asset import persistence round-trip (#12) ---
        Image sprite = Image.createImage(24, 24, 0xff33cc66);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(sprite, bos, ImageIO.FORMAT_PNG, 1f);
        ProjectIO.saveCustomAsset(binding, "mysprite", bos.toByteArray());
        ProjectIO.saveCustomPack(binding, "{\"packs\":[{\"id\":\"custom\",\"name\":\"Custom\","
                + "\"assets\":[{\"id\":\"mysprite\",\"name\":\"My Sprite\",\"kind\":\"actor\","
                + "\"w\":24,\"h\":24,\"source\":\"assets/mysprite.png\"}]}]}");
        AssetCatalog fresh = StarterPacks.loadCatalog();
        ProjectIO.loadCustomPack(binding, fresh);
        File pngFile = new File(games, "assets/mysprite.png");
        boolean customOk = pngFile.isFile()
                && fresh.getPack("custom") != null
                && fresh.def("mysprite") != null
                && fresh.image("mysprite") != null;

        boolean ok = sceneOk && customOk;
        System.out.println("[ProjectIOHarness] scenes=" + scenes
                + " reloadedElements=" + reloaded.elements().size()
                + " gameFile=" + gameFile.length() + "b"
                + " javaFile=" + (javaFile.isFile() ? javaFile.length() + "b" : "MISSING")
                + " customAsset=" + (pngFile.isFile() ? pngFile.length() + "b" : "MISSING")
                + " customDef=" + (fresh.def("mysprite") != null) + " sceneOk=" + sceneOk + " customOk=" + customOk);
        System.out.println("[ProjectIOHarness] RESULT " + (ok ? "OK" : "FAIL"));
        System.exit(ok ? 0 : 1);
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteRecursive(k);
                }
            }
        }
        f.delete();
    }
}
