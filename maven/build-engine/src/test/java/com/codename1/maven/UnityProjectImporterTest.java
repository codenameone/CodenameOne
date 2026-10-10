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
package com.codename1.maven;

import com.codename1.build.SystemStreamLog;
import com.codename1.builders.BuildException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UnityProjectImporterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File game;
    private File common;
    private File unity;

    @Before
    public void project() throws Exception {
        game = tmp.newFolder("MyGame");
        write(new File(game, "Assets/Scripts/Player.cs"), "class Player {}");
        write(new File(game, "Assets/Scripts/Player.cs.meta"), "guid: 1");
        write(new File(game, "Assets/Scenes/Main.unity"), "%YAML 1.1");
        write(new File(game, "Assets/Plugins/Fast.dll"), "MZ");
        write(new File(game, "ProjectSettings/ProjectSettings.asset"),
                "PlayerSettings:\n  companyName: Acme\n  productName: Rock Fall\n");
        // What the editor derives, and what version control keeps.
        write(new File(game, "Library/ScriptAssemblies/Assembly-CSharp.dll"), "MZ");
        write(new File(game, "Temp/lock"), "");
        write(new File(game, "obj/Debug/x.cache"), "");
        write(new File(game, ".git/HEAD"), "ref");
        write(new File(game, "Assets/.git/HEAD"), "a nested checkout");
        write(new File(game, "Assets/Scripts/obj/Debug/y.cache"), "");
        write(new File(game, "Packages/manifest.json"), "{}");
        common = tmp.newFolder("app", "common");
        unity = new File(common, "src/main/unity");
    }

    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private UnityProjectImporter.Result importIt() throws BuildException {
        return new UnityProjectImporter(new SystemStreamLog()).importProject(game, common, "com.acme.game", "MyGame");
    }

    @Test
    public void copiesTheProjectAndNothingTheEditorDerives() throws Exception {
        UnityProjectImporter.Result r = importIt();
        assertTrue(new File(unity, "Assets/Scripts/Player.cs").isFile());
        assertTrue("the .meta files carry the guids scenes refer to scripts by",
                new File(unity, "Assets/Scripts/Player.cs.meta").isFile());
        assertTrue(new File(unity, "Assets/Scenes/Main.unity").isFile());
        assertTrue(new File(unity, "ProjectSettings/ProjectSettings.asset").isFile());
        for (String never : new String[] {"Library", "Temp", "obj", ".git", "Packages", "Assets/.git",
            "Assets/Scripts/obj"}) {
            assertFalse(never + " was copied", new File(unity, never).exists());
        }
        assertEquals(5, r.copiedFiles);
        assertEquals(1, r.scripts);
        assertEquals(1, r.scenes);
        assertEquals("Rock Fall", r.productName);
        assertEquals(1, r.ignored.size());
        assertTrue(r.ignored.get(0), r.ignored.get(0).startsWith("Assets/Plugins/Fast.dll (compiled code"));
        assertTrue(UnityProjectBuilder.isUnityProject(unity));
    }

    @Test
    public void writesTheEntryPointAndKeepsTheApplicationsOwn() throws Exception {
        File main = new File(common, "src/main/java/com/acme/game/MyGame.java");
        write(main, "package com.acme.game; public class MyGame { /* the archetype's */ }");
        importIt();
        String src = read(main);
        assertTrue(src, src.startsWith("package com.acme.game;\n\n/// The Codename One entry point"));
        assertTrue(src, src.contains("public class MyGame extends com.codename1.unitycompat.app.UnityApplication {"));
        assertTrue(src, src.contains("com.codename1.generated.unity.UnityAppImpl.install();"));
        File backup = new File(main.getPath() + ".pre-unity-import");
        assertTrue(read(backup).contains("the archetype's"));
        // Importing twice keeps the backup of the application's own class;
        // the second run must not replace it with what the first one wrote.
        importIt();
        assertTrue(read(backup).contains("the archetype's"));
        // A class customized after the import is kept as it is.
        write(main, src + "// mine\n");
        importIt();
        assertTrue(read(main).endsWith("// mine\n"));
    }

    /// The copy mirrors the Unity project: what the project dropped goes,
    /// what changed is taken, and what the import never copied stays.
    @Test
    public void importingAgainMirrorsTheProject() throws Exception {
        importIt();
        File own = new File(unity, "Assets/Scripts/Added.cs");
        write(own, "class Added {}");
        assertTrue(new File(game, "Assets/Scenes/Main.unity").delete());
        write(new File(game, "Assets/Scripts/Player.cs"), "class Player { int lives; }");
        UnityProjectImporter.Result r = importIt();
        assertEquals(1, r.removedFiles);
        assertFalse(new File(unity, "Assets/Scenes/Main.unity").exists());
        assertTrue(read(new File(unity, "Assets/Scripts/Player.cs")).contains("lives"));
        assertTrue("a file the import did not copy is not its to remove", own.isFile());
    }

    /// The record of what was imported is a file in the project; a line
    /// edited into it must not name a file elsewhere for deletion.
    @Test
    public void theImportRecordCannotNameFilesOutsideTheCopy() throws Exception {
        importIt();
        File outside = new File(common, "src/main/java/Keep.java");
        write(outside, "class Keep {}");
        File record = new File(unity, UnityProjectImporter.IMPORT_RECORD);
        write(record, read(record) + "../java/Keep.java\nAssets/../../java/Keep.java\n");
        importIt();
        assertTrue(outside.isFile());
    }

    @Test
    public void refusesWhatIsNotAUnityProject() throws Exception {
        File notOne = tmp.newFolder("notes");
        try {
            new UnityProjectImporter(new SystemStreamLog()).importProject(notOne, common, "a", "B");
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("is not a Unity project"));
        }
        assertFalse("nothing is created for a project that was not imported", unity.exists());
    }

    @Test
    public void readsNoProductNameFromABinarySettingsFile() throws Exception {
        File settings = new File(game, "ProjectSettings/ProjectSettings.asset");
        Files.write(settings.toPath(), new byte[] {0, 1, 2, (byte) 0xff, 3});
        assertNull(UnityProjectImporter.productName(settings));
        assertNull(UnityProjectImporter.productName(new File(game, "ProjectSettings/None.asset")));
    }
}
