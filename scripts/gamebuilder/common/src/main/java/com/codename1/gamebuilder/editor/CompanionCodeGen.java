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
package com.codename1.gamebuilder.editor;

import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;

import java.util.ArrayList;
import java.util.List;

/// Generates the two files the builder writes into a project for a scene, mirroring how
/// the GUI builder pairs a {@code .gui} data file with a {@code .java} companion:
///
/// - the {@code <Name>.game} JSON (the level data, from `GameLevel#toJson()`), and
/// - a thin {@code <Name>.java} that extends
///   `com.codename1.gaming.level.GameSceneView`, loads that resource at runtime and
///   exposes a behavior hook. The generated load wiring lives between DON'T EDIT
///   markers (the region the Maven goal's monitor refreshes); the {@code onUpdate}
///   body is yours to edit and is preserved across regeneration.
public final class CompanionCodeGen {
    /// Marker the Maven goal uses to find the regenerated region.
    public static final String GEN_BEGIN = "//-- GAMEBUILDER GENERATED - DO NOT EDIT BELOW";
    public static final String GEN_END = "//-- GAMEBUILDER GENERATED - DO NOT EDIT ABOVE";

    private CompanionCodeGen() {
    }

    /// The level JSON to write to {@code <Name>.game}.
    public static String gameData(EditorModel model) {
        return model.level().toJson();
    }

    /// The companion Java source for the scene with no level introspection (no generated
    /// fields). Prefer `#companionJava(String, String, String, GameLevel)` so the objects
    /// you named in the editor become wired fields.
    public static String companionJava(String packageName, String className, String gameResourcePath) {
        return companionJava(packageName, className, gameResourcePath, null);
    }

    /// The companion Java source for the scene. {@code gameResourcePath} is the runtime
    /// classpath path of the {@code .game} file -- the runtime uses a flat resource
    /// namespace, so this is {@code /Level1.game}, not {@code /games/Level1.game}.
    ///
    /// When {@code level} is supplied, every named object becomes a {@code protected Sprite}
    /// field initialized in {@code initScene()} via `GameSceneView#findByName(String)`, and
    /// a player carrying a {@code lives} property seeds `GameSceneView#setLives(int)` -- so
    /// the user's {@code onUpdate} starts from real variables instead of boilerplate.
    public static String companionJava(String packageName, String className, String gameResourcePath, GameLevel level) {
        boolean blank = packageName == null || packageName.trim().isEmpty();
        String pkg = blank ? "" : "package " + packageName + ";\n\n";

        StringBuilder fieldDecls = new StringBuilder();
        StringBuilder init = new StringBuilder();
        String livesField = null;
        if (level != null) {
            List<String> used = new ArrayList<>();
            List<String[]> named = new ArrayList<>();
            for (GameElement el : level.elements()) {
                String name = el.getName();
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                String field = toFieldName(name);
                if (field.isEmpty() || used.contains(field)) {
                    continue;
                }
                used.add(field);
                named.add(new String[]{field, name});
                if (livesField == null && el.hasProperty("lives")) {
                    livesField = field;
                }
            }
            for (String[] f : named) {
                fieldDecls.append("    /// The \"").append(f[1]).append("\" object you placed in the editor.\n");
                fieldDecls.append("    protected Sprite ").append(f[0]).append(";\n");
                init.append("        ").append(f[0]).append(" = findByName(\"").append(f[1]).append("\");\n");
            }
            if (livesField != null) {
                init.append("        if (").append(livesField).append(" != null) {\n");
                init.append("            setLives(elementOf(").append(livesField).append(").getInt(\"lives\", 3));\n");
                init.append("        }\n");
            }
        }
        if (init.length() == 0) {
            init.append("        // name objects in the editor's Inspector to get fields wired here\n");
        }
        // a 2D scene gets the built-in arcade behavior (gravity/run/jump/pickups) so the
        // generated game is immediately playable; remove it to drive everything yourself.
        String arcade = level != null && level.getMode() == GameLevel.Mode.TWO_D
                ? "\n        setArcadeBehavior(true);" : "";
        String spriteImport = fieldDecls.length() == 0 ? "" : "import com.codename1.gaming.Sprite;\n";

        String body = """
                {SPRITE_IMPORT}import com.codename1.gaming.level.AssetCatalog;
                import com.codename1.gaming.level.GameLevel;
                import com.codename1.gaming.level.GameSceneView;
                import com.codename1.ui.Display;

                /// Generated by the Codename One game builder. The level data lives in
                /// {RES} and is loaded at runtime; edit the level visually with
                /// `mvn cn1:gamebuilder`. Put your game logic in onUpdate.
                public class {CLS} extends GameSceneView {
                    public {CLS}(AssetCatalog catalog) {
                        super(loadLevel(), catalog);
                        initScene();{ARCADE}
                    }

                    {BEGIN}
                {FIELDS}    private static GameLevel loadLevel() {
                        try {
                            return GameLevel.load(Display.getInstance().getResourceAsStream({CLS}.class, "{RES}"));
                        } catch (java.io.IOException err) {
                            throw new RuntimeException("failed to load level {RES}", err);
                        }
                    }

                    /// Wires the objects you named in the editor to fields and seeds game
                    /// state. The editor refreshes this whenever you re-edit the level.
                    private void initScene() {
                {INIT}    }
                    {END}

                    @Override
                    protected void onUpdate(double deltaSeconds) {
                        // TODO: your per-frame game logic
                    }
                }
                """;
        return pkg + body
                .replace("{SPRITE_IMPORT}", spriteImport)
                .replace("{ARCADE}", arcade)
                .replace("{FIELDS}", fieldDecls.toString())
                .replace("{INIT}", init.toString())
                .replace("{RES}", gameResourcePath)
                .replace("{CLS}", className)
                .replace("{BEGIN}", GEN_BEGIN)
                .replace("{END}", GEN_END);
    }

    /// Turns an editor object name into a conventional Java field identifier: keeps
    /// ASCII letters/digits/underscore (no leading digit), and lower-cases the first
    /// letter. Returns an empty string when nothing usable remains.
    private static String toFieldName(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            boolean digit = c >= '0' && c <= '9';
            if (letter || c == '_' || (digit && b.length() > 0)) {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            return "";
        }
        char first = b.charAt(0);
        if (first >= 'A' && first <= 'Z') {
            b.setCharAt(0, (char) (first - 'A' + 'a'));
        }
        return b.toString();
    }
}
