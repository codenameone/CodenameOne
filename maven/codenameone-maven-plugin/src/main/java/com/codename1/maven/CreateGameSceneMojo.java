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
package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Scaffolds a new game scene for the {@code com.codename1.gaming} engine, mirroring how
 * {@code create-gui-form} scaffolds a {@code .gui} + {@code .java} pair.
 *
 * <p>It writes two files into the project:</p>
 * <ul>
 *   <li>{@code src/main/resources/games/<Name>.game} - an empty level of the chosen
 *       mode ({@code 2d} / {@code 3d} / {@code board}), in the JSON format that
 *       {@code com.codename1.gaming.level.GameLevel} reads, and</li>
 *   <li>{@code src/main/java/<pkg>/<Name>.java} - a thin companion that extends
 *       {@code GameSceneView}, loads that resource and exposes an {@code onUpdate} hook.</li>
 * </ul>
 *
 * <p>Then open it visually with {@code mvn cn1:gamebuilder}. The goal only applies to
 * Java 17 Codename One projects.</p>
 *
 * <pre>mvn cn1:create-game-scene -DclassName=com.example.Level1 -Dmode=2d</pre>
 */
@Mojo(name = "create-game-scene")
public class CreateGameSceneMojo extends AbstractCN1Mojo {

    @Parameter(property = "className", required = true)
    private String className;

    @Parameter(property = "mode", required = false, defaultValue = "2d")
    private String mode;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (getCN1ProjectDir() == null) {
            return;
        }
        try {
            if (!getCN1ProjectDir().getCanonicalFile().equals(project.getBasedir().getCanonicalFile())) {
                return;
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to resolve project dir", ex);
        }
        requireJava17();

        String fqn = className.trim();
        String pkg = fqn.contains(".") ? fqn.substring(0, fqn.lastIndexOf('.')) : "";
        String simple = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
        String normMode = normalizeMode(mode);
        String resourcePath = "/games/" + simple + ".game";

        File base = getCN1ProjectDir();
        File gameFile = new File(base, "src/main/resources/games/" + simple + ".game");
        File javaFile = new File(base, "src/main/java/" + fqn.replace(".", File.separator) + ".java");

        try {
            if (gameFile.exists() || javaFile.exists()) {
                throw new MojoFailureException("Scene " + fqn + " already exists; not overwriting.");
            }
            FileUtils.write(gameFile, emptyLevelJson(normMode), StandardCharsets.UTF_8);
            FileUtils.write(javaFile, companionJava(pkg, simple, resourcePath), StandardCharsets.UTF_8);
            getLog().info("Created game scene " + fqn + " (" + normMode + ")");
            getLog().info("  data:      " + gameFile);
            getLog().info("  companion: " + javaFile);
            getLog().info("Edit it visually with: mvn cn1:gamebuilder");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write scene files", ex);
        }
    }

    private void requireJava17() throws MojoFailureException, MojoExecutionException {
        String v;
        try {
            v = getProjectProperties().getProperty("codename1.arg.java.version",
                    getProjectProperties().getProperty("codename1.java.version", "8"));
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read codenameone_settings.properties", ex);
        }
        if (!"17".equals(v.trim())) {
            throw new MojoFailureException("The game builder requires a Java 17 Codename One project "
                    + "(codename1.arg.java.version=17). This project reports '" + v + "'.");
        }
    }

    private static String normalizeMode(String m) {
        String s = m == null ? "2d" : m.trim().toLowerCase();
        if (s.equals("3d") || s.equals("board")) {
            return s;
        }
        return "2d";
    }

    private static String emptyLevelJson(String mode) {
        if (mode.equals("3d")) {
            return "{\"mode\":\"3d\",\"assetPack\":\"kit3d\",\"cols\":16,\"rows\":16,\"tileSize\":1,"
                    + "\"camera\":{\"eye\":[0,8,14],\"target\":[0,0,0],\"fov\":60,\"near\":0.1,\"far\":500},"
                    + "\"lights\":[{\"dir\":[0.4,-1,0.3],\"color\":-3408,\"ambient\":-13948354}],"
                    + "\"terrain\":{\"cols\":16,\"rows\":16,\"cellSize\":1,\"heights\":[]},"
                    + "\"layers\":[{\"name\":\"Models\",\"kind\":\"model\",\"visible\":true,\"locked\":false,\"band\":0}],"
                    + "\"elements\":[]}";
        }
        if (mode.equals("board")) {
            return "{\"mode\":\"board\",\"assetPack\":\"board\",\"cols\":10,\"rows\":10,\"tileSize\":64,"
                    + "\"layers\":[{\"name\":\"Board\",\"kind\":\"tile\",\"visible\":true,\"locked\":false,\"band\":0},"
                    + "{\"name\":\"Pieces\",\"kind\":\"entity\",\"visible\":true,\"locked\":false,\"band\":2}],"
                    + "\"elements\":[]}";
        }
        return "{\"mode\":\"2d\",\"assetPack\":\"platformer\",\"cols\":26,\"rows\":16,\"tileSize\":32,"
                + "\"layers\":[{\"name\":\"Background\",\"kind\":\"tile\",\"visible\":true,\"locked\":false,\"band\":0},"
                + "{\"name\":\"Terrain\",\"kind\":\"tile\",\"visible\":true,\"locked\":false,\"band\":1},"
                + "{\"name\":\"Items\",\"kind\":\"entity\",\"visible\":true,\"locked\":false,\"band\":2},"
                + "{\"name\":\"Actors\",\"kind\":\"entity\",\"visible\":true,\"locked\":false,\"band\":3}],"
                + "\"elements\":[]}";
    }

    private static String companionJava(String pkg, String cls, String res) {
        String header = pkg.isEmpty() ? "" : "package " + pkg + ";\n\n";
        return header
                + "import com.codename1.gaming.level.AssetCatalog;\n"
                + "import com.codename1.gaming.level.GameLevel;\n"
                + "import com.codename1.gaming.level.GameSceneView;\n"
                + "import com.codename1.ui.Display;\n\n"
                + "/** Generated by cn1:create-game-scene. Level data: " + res + " */\n"
                + "public class " + cls + " extends GameSceneView {\n"
                + "    public " + cls + "(AssetCatalog catalog) {\n"
                + "        super(loadLevel(), catalog);\n"
                + "    }\n\n"
                + "    //-- GAMEBUILDER GENERATED - DO NOT EDIT BELOW\n"
                + "    private static GameLevel loadLevel() {\n"
                + "        try {\n"
                + "            return GameLevel.load(Display.getInstance().getResourceAsStream("
                + cls + ".class, \"" + res + "\"));\n"
                + "        } catch (java.io.IOException err) {\n"
                + "            throw new RuntimeException(\"failed to load level " + res + "\", err);\n"
                + "        }\n"
                + "    }\n"
                + "    //-- GAMEBUILDER GENERATED - DO NOT EDIT ABOVE\n\n"
                + "    @Override\n"
                + "    protected void onUpdate(double deltaSeconds) {\n"
                + "        // TODO: your per-frame game logic\n"
                + "    }\n"
                + "}\n";
    }
}
