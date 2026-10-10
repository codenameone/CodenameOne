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
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The build of `src/main/unity`, with the three external steps faked: what
/// is under test is everything around them -- when they run, what reaches the
/// classes directory, what leaves it -- and that has to be testable on a
/// machine with no .NET SDK. The steps themselves run for real in
/// `maven/integration-tests/unity-compat-project-test.sh`.
public class UnityProjectBuilderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File unity;
    private File target;
    private File classes;
    private File generated;
    private File sources;
    private File runtimeJar;
    private File referencesJar;
    private FakeToolchain tools;
    private final List<String> warned = new ArrayList<String>();

    /// Stands in for the SDK, the translator and the scene compiler, writing
    /// the files the real ones would.
    private static final class FakeToolchain implements UnityProjectBuilder.Toolchain {
        int compiled;
        int translated;
        int scenes;
        boolean failTranslation;
        List<String> classNames = new ArrayList<String>(Arrays.asList("global/Player", "global/Shot",
                "UnityEngine/Vector2"));
        String csproj;
        File runtimeClasses;

        public void compileScripts(File csprojFile, File logFile) throws BuildException {
            compiled++;
            try {
                csproj = new String(Files.readAllBytes(csprojFile.toPath()), StandardCharsets.UTF_8);
                File dll = new File(csprojFile.getParentFile(), "bin/Release/netstandard2.1/Assembly-CSharp.dll");
                dll.getParentFile().mkdirs();
                Files.write(dll.toPath(), new byte[] {'M', 'Z'});
            } catch (IOException e) {
                throw new BuildException(e.getMessage(), e);
            }
        }

        public List<String> translate(File out, File runtime, List<File> references, List<File> assemblies,
                File logFile) throws BuildException {
            translated++;
            runtimeClasses = runtime;
            if (failTranslation) {
                throw new BuildException("translation failed");
            }
            try {
                for (String name : classNames) {
                    File f = new File(out, name + ".class");
                    f.getParentFile().mkdirs();
                    Files.write(f.toPath(), name.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new BuildException(e.getMessage(), e);
            }
            return Arrays.asList("3 classes", "warning: Player.OnGUI (Player.cs:7): never called");
        }

        public List<String> compileScenes(File project, File javaOut, File resources, List<File> references,
                List<File> assemblies, File logFile) throws BuildException {
            scenes++;
            try {
                File impl = new File(javaOut, "com/codename1/generated/unity/UnityAppImpl.java");
                impl.getParentFile().mkdirs();
                Files.write(impl.toPath(), "package com.codename1.generated.unity;\n".getBytes(StandardCharsets.UTF_8));
                resources.mkdirs();
                Files.write(new File(resources, "sheet.png").toPath(), new byte[] {1, 2, 3});
            } catch (IOException e) {
                throw new BuildException(e.getMessage(), e);
            }
            return Arrays.asList("note: mouse axes read as zero", "warning: a particle system was left out",
                    "wrote UnityAppImpl.java");
        }
    }

    @Before
    public void project() throws Exception {
        File root = tmp.newFolder("app");
        unity = new File(root, "src/main/unity");
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player {}");
        write(new File(unity, "Assets/Scenes/Main.unity"), "%YAML 1.1");
        write(new File(unity, "ProjectSettings/EditorBuildSettings.asset"), "scenes");
        target = new File(root, "target");
        classes = new File(target, "classes");
        generated = new File(target, "generated-sources/unity");
        sources = new File(root, "src/main/java");
        runtimeJar = zip(new File(root, "codenameone-unity-compat.jar"), "UnityEngine/Vector2.class",
                "com/codename1/unitycompat/unityengine/UnityRuntime.class", "META-INF/MANIFEST.MF");
        referencesJar = zip(new File(root, "codenameone-unity-compat-references.jar"), "netstandard.dll",
                "UnityEngine.dll", "Codename1.UnityValues.dll");
        tools = new FakeToolchain();
    }

    private UnityProjectBuilder builder() {
        return builder(runtimeJar, referencesJar);
    }

    private UnityProjectBuilder builder(File runtime, File references) {
        UnityProjectBuilder b = new UnityProjectBuilder(unity, generated, classes, target, runtime, references,
                Collections.<File>emptyList(), null, "com.acme.game", "MyGame", Collections.singletonList(sources),
                new SystemStreamLog() {
                    @Override
                    public void warn(CharSequence content) {
                        warned.add(content.toString());
                        super.warn(content);
                    }
                });
        b.setToolchain(tools);
        return b;
    }

    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static File zip(File f, String... entries) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            for (String e : entries) {
                out.putNextEntry(new ZipEntry(e));
                out.write(e.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return f;
    }

    @Test
    public void aProjectWithoutUnitySourcesIsLeftAlone() throws Exception {
        File empty = tmp.newFolder("plain");
        assertFalse(UnityProjectBuilder.isUnityProject(new File(empty, "src/main/unity")));
        assertFalse(UnityProjectBuilder.isUnityProject(null));
        unity = new File(empty, "src/main/unity");
        // Not even the dependency is asked for: a project that is not a Unity
        // project must build exactly as it did before the goal existed.
        assertFalse(builder(null, null).run());
        assertEquals(0, tools.compiled);
        assertFalse(target.exists());
    }

    /// `Assets` alone is some other directory of that name: the scenes to
    /// build are listed in `ProjectSettings`.
    @Test
    public void assetsAloneAreNotAProject() throws Exception {
        File dir = tmp.newFolder("half");
        assertTrue(new File(dir, "Assets").mkdirs());
        assertFalse(UnityProjectBuilder.isUnityProject(dir));
        unity = dir;
        assertFalse(builder().run());
        assertEquals(0, tools.compiled);
    }

    @Test
    public void installsClassesImagesSceneSourceAndMainClass() throws Exception {
        assertTrue(builder().run());
        assertTrue(new File(classes, "global/Player.class").isFile());
        assertTrue(new File(classes, "global/Shot.class").isFile());
        assertTrue(new File(classes, "sheet.png").isFile());
        assertTrue(new File(generated, "com/codename1/generated/unity/UnityAppImpl.java").isFile());
        String main = new String(Files.readAllBytes(new File(generated, "com/acme/game/MyGame.java").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(main, main.contains("public class MyGame extends com.codename1.unitycompat.app.UnityApplication"));
        assertTrue(main, main.contains("com.codename1.generated.unity.UnityAppImpl.install();"));
        // The translator checks the scripts against the runtime's classes,
        // which it reads from a directory: the jar was unpacked for it.
        assertTrue(new File(tools.runtimeClasses, "com/codename1/unitycompat/unityengine/UnityRuntime.class").isFile());
        assertFalse("only classes are unpacked", new File(tools.runtimeClasses, "META-INF/MANIFEST.MF").exists());
    }

    /// What the translator could not carry over reaches the build's own
    /// output beside what the scene compiler could not, and not only a log
    /// file under `target`.
    @Test
    public void warningsOfBothToolsAreReported() throws Exception {
        assertTrue(builder().run());
        assertEquals(Arrays.asList("src/main/unity: Player.OnGUI (Player.cs:7): never called",
                "src/main/unity: a particle system was left out"), warned);
    }

    /// The value types are translated with the scripts, and the runtime jar
    /// already ships them: the application must not carry a second copy.
    @Test
    public void classesTheRuntimeShipsAreNotInstalledAgain() throws Exception {
        assertTrue(builder().run());
        assertFalse(new File(classes, "UnityEngine/Vector2.class").exists());
    }

    @Test
    public void anUnchangedProjectIsNotCompiledAgain() throws Exception {
        assertTrue(builder().run());
        assertTrue(builder().run());
        assertEquals(1, tools.compiled);
        assertEquals(1, tools.translated);
        assertEquals(1, tools.scenes);
        // A changed script is.
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player { int lives; }");
        assertTrue(builder().run());
        assertEquals(2, tools.compiled);
        // And so is a changed setting, which only the scene compiler reads.
        write(new File(unity, "ProjectSettings/EditorBuildSettings.asset"), "scenes: two");
        assertTrue(builder().run());
        assertEquals(3, tools.scenes);
    }

    /// An edit that keeps a file's length and its modification time -- a
    /// checkout that restores times, two saves inside one tick of the file
    /// system's clock -- is still an edit: text is compared by content.
    @Test
    public void anEditOfTheSameLengthAndTimeCompilesAgain() throws Exception {
        File script = new File(unity, "Assets/Scripts/Player.cs");
        File scene = new File(unity, "Assets/Scenes/Main.unity");
        write(script, "class Player { int speed = 1; }");
        assertTrue(builder().run());
        long length = script.length();
        long time = script.lastModified();
        write(script, "class Player { int speed = 2; }");
        assertTrue(script.setLastModified(time));
        assertEquals(length, script.length());
        assertEquals(time, script.lastModified());
        assertTrue(builder().run());
        assertEquals(2, tools.compiled);
        // A scene, which only the scene compiler reads, likewise.
        time = scene.lastModified();
        write(scene, "%YAML 1.2");
        assertTrue(scene.setLastModified(time));
        assertTrue(builder().run());
        assertEquals(3, tools.scenes);
        // And nothing changed is still nothing to do.
        assertTrue(builder().run());
        assertEquals(3, tools.scenes);
    }

    /// The staging directory is made where the .NET SDK is and may be used
    /// somewhere else: another checkout, at another path, with the times a
    /// checkout gives its files and jars that were packed again. Nothing of
    /// that is the project, so the build there has nothing to compile.
    @Test
    public void aCopyElsewhereWithNewTimesIsNotCompiledAgain() throws Exception {
        write(new File(unity, "Assets/Sprites/ship.png"), "pixels");
        assertTrue(builder().run());
        File elsewhere = new File(tmp.newFolder("another"), "deeper/checkout");
        long later = System.currentTimeMillis() + 86400000L;
        copy(unity, new File(elsewhere, "src/main/unity"), later);
        copy(target, new File(elsewhere, "target"), later);
        // The same entries, packed again under other names: a jar's own
        // bytes differ by the times in it.
        File runtime = zip(new File(elsewhere, "runtime-of-another-version.jar"), "META-INF/MANIFEST.MF",
                "com/codename1/unitycompat/unityengine/UnityRuntime.class", "UnityEngine/Vector2.class");
        File references = zip(new File(elsewhere, "references.jar"), "netstandard.dll", "UnityEngine.dll",
                "Codename1.UnityValues.dll");
        assertTrue(runtime.setLastModified(later));
        File classesThere = new File(elsewhere, "target/classes");
        assertTrue(new File(classesThere, "global/Player.class").delete());
        UnityProjectBuilder there = new UnityProjectBuilder(new File(elsewhere, "src/main/unity"),
                new File(elsewhere, "target/generated-sources/unity"), classesThere, new File(elsewhere, "target"),
                runtime, references, Collections.<File>emptyList(), null, "com.acme.game", "MyGame",
                Collections.singletonList(new File(elsewhere, "src/main/java")), new SystemStreamLog());
        there.setToolchain(tools);
        assertTrue(there.run());
        assertEquals(1, tools.compiled);
        assertEquals(1, tools.translated);
        assertEquals(1, tools.scenes);
        assertTrue(new File(classesThere, "global/Player.class").isFile());
        // And content is still what decides: a picture of the same length
        // and time, with other pixels in it.
        File picture = new File(elsewhere, "src/main/unity/Assets/Sprites/ship.png");
        write(picture, "PIXELS");
        assertTrue(picture.setLastModified(later));
        assertTrue(there.run());
        assertEquals(2, tools.scenes);
    }

    private static void copy(File from, File to, long time) throws IOException {
        File[] children = from.listFiles();
        if (children == null) {
            to.getParentFile().mkdirs();
            Files.copy(from.toPath(), to.toPath());
            assertTrue(to.setLastModified(time));
            return;
        }
        for (File c : children) {
            copy(c, new File(to, c.getName()), time);
        }
    }

    /// A new runtime translates differently and checks against different
    /// classes: the staging directory of the old one cannot be trusted.
    @Test
    public void aNewRuntimeJarCompilesAgain() throws Exception {
        assertTrue(builder().run());
        zip(runtimeJar, "UnityEngine/Vector2.class", "UnityEngine/Vector3.class");
        assertTrue(builder().run());
        assertEquals(2, tools.translated);
    }

    /// The classes directory is the build tool's to clean; cleaning it alone
    /// must not need the .NET SDK to get the game back.
    @Test
    public void aCleanedClassesDirectoryIsRefilledWithoutCompiling() throws Exception {
        assertTrue(builder().run());
        assertTrue(new File(classes, "global/Player.class").delete());
        assertTrue(new File(classes, "sheet.png").delete());
        assertTrue(builder().run());
        assertEquals(1, tools.compiled);
        assertTrue(new File(classes, "global/Player.class").isFile());
        assertTrue(new File(classes, "sheet.png").isFile());
    }

    @Test
    public void aScriptDeletedFromTheProjectLeavesTheClasses() throws Exception {
        assertTrue(builder().run());
        write(new File(classes, "com/acme/game/Own.class"), "the application's own class");
        tools.classNames.remove("global/Shot");
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player { /* no shots */ }");
        assertTrue(builder().run());
        assertFalse(new File(classes, "global/Shot.class").exists());
        assertTrue(new File(classes, "global/Player.class").isFile());
        assertTrue("only what this build installed is removed", new File(classes, "com/acme/game/Own.class").isFile());
    }

    /// A build that fails half way has replaced part of the staging
    /// directory; the next one must not skip on the strength of the old state.
    @Test
    public void aFailedBuildIsNotRememberedAsDone() throws Exception {
        assertTrue(builder().run());
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player { broken }");
        tools.failTranslation = true;
        try {
            builder().run();
            fail("the translation failure was swallowed");
        } catch (BuildException expected) {
            assertEquals("translation failed", expected.getMessage());
        }
        tools.failTranslation = false;
        assertTrue(builder().run());
        assertEquals(3, tools.compiled);
        assertTrue(new File(classes, "global/Player.class").isFile());
    }

    @Test
    public void theApplicationsOwnMainClassRetiresTheGeneratedOne() throws Exception {
        assertTrue(builder().run());
        File generatedMain = new File(generated, "com/acme/game/MyGame.java");
        assertTrue(generatedMain.isFile());
        File own = new File(sources, "com/acme/game/MyGame.java");
        write(own, "package com.acme.game; public class MyGame {}");
        assertTrue(builder().run());
        assertFalse("javac would see two MyGame classes", generatedMain.exists());
        // Deleted again, the generated one comes back, with nothing compiled.
        assertTrue(own.delete());
        assertTrue(builder().run());
        assertTrue(generatedMain.isFile());
        assertEquals(1, tools.compiled);
    }

    @Test
    public void aMissingRuntimeDependencyIsNamed() throws Exception {
        try {
            builder(null, referencesJar).run();
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("codenameone-unity-compat dependency is missing"));
            assertTrue(e.getMessage(), e.getMessage().contains("cn1:import-unity-project"));
        }
        try {
            builder(runtimeJar, null).run();
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'references' classifier"));
        }
    }

    @Test
    public void aReferencesJarWithoutTheAssembliesIsRejected() throws Exception {
        zip(referencesJar, "netstandard.dll");
        try {
            builder().run();
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("has no UnityEngine.dll"));
        }
        assertEquals("nothing was compiled against half the references", 0, tools.compiled);
    }

    /// The generated project references the three assemblies and nothing
    /// else, so the SDK has nothing to download; and it compiles the scripts
    /// where they are, without the editor's.
    @Test
    public void theCSharpProjectIsSelfContained() throws Exception {
        assertTrue(builder().run());
        String p = tools.csproj;
        assertTrue(p, p.contains("<DisableImplicitFrameworkReferences>true</DisableImplicitFrameworkReferences>"));
        assertTrue(p, p.contains("<TargetFramework>netstandard2.1</TargetFramework>"));
        assertTrue(p, p.contains("/Assets/**/*.cs\" Exclude=\""));
        assertTrue(p, p.contains("/Assets/**/Editor/**/*.cs\""));
        for (String name : new String[] {"netstandard", "UnityEngine", "Codename1.UnityValues"}) {
            assertTrue(p, p.contains("<Reference Include=\"" + name + "\">"));
            assertTrue(p, p.contains("/unity/ref/" + name + ".dll</HintPath>"));
        }
        assertFalse("no package may be needed", p.contains("PackageReference"));
    }

    /// A path is data to MSBuild only when its special characters are
    /// encoded; unencoded, `$(x)` in a directory name is a property reference.
    @Test
    public void pathsAreLiteralToMsbuild() {
        assertEquals("/Users/a &amp; b/50%25/%24(x)/it%27s%3B%40", UnityProjectBuilder.msbuild("/Users/a & b/50%/$(x)/it's;@"));
        assertEquals("C:/games/My Game", UnityProjectBuilder.msbuild("C:\\games\\My Game"));
    }

    @Test
    public void findsDotnetWhereItIsNamedThenDotnetRootThenThePath() throws Exception {
        String exe = UnityProjectBuilder.isWindows() ? "dotnet.exe" : "dotnet";
        File named = new File(tmp.newFolder("named"), exe);
        File rooted = new File(tmp.newFolder("rooted"), exe);
        File onPath = new File(tmp.newFolder("onpath"), exe);
        for (File f : new File[] {named, rooted, onPath}) {
            write(f, "");
        }
        Map<String, String> env = new HashMap<String, String>();
        env.put("DOTNET_ROOT", rooted.getParent());
        env.put("PATH", tmp.getRoot() + File.pathSeparator + onPath.getParent());
        assertEquals(named, UnityProjectBuilder.locateDotnet(named.getPath(), env));
        assertEquals("the directory it is in names it too", named,
                UnityProjectBuilder.locateDotnet(named.getParent(), env));
        assertEquals(rooted, UnityProjectBuilder.locateDotnet(null, env));
        assertEquals(rooted, UnityProjectBuilder.locateDotnet("  ", env));
        env.remove("DOTNET_ROOT");
        assertEquals(onPath, UnityProjectBuilder.locateDotnet(null, env));
        assertEquals("a bare command is looked for on the PATH", onPath,
                UnityProjectBuilder.locateDotnet(exe, env));
    }

    @Test
    public void aMissingSdkSaysWhereItLookedAndWhatToDo() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("PATH", tmp.getRoot().getPath());
        try {
            UnityProjectBuilder.locateDotnet(null, env);
            fail();
        } catch (BuildException e) {
            String m = e.getMessage();
            assertTrue(m, m.contains(".NET SDK, which was not found"));
            assertTrue(m, m.contains("DOTNET_ROOT environment variable (not set)"));
            assertTrue(m, m.contains("-Dcn1.unity.dotnet=/path/to/dotnet"));
            assertTrue(m, m.contains("https://dotnet.microsoft.com/download"));
        }
        // A setting that names nothing is an error, not a reason to fall back:
        // the developer said which SDK to use.
        try {
            UnityProjectBuilder.locateDotnet(new File(tmp.getRoot(), "no/such/dotnet").getPath(), env);
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("cn1.unity.dotnet names "));
        }
    }

    /// A build that reads its project from a configured directory names that
    /// directory, not the default one it does not have.
    @Test
    public void aMissingSdkNamesTheProjectThatNeedsIt() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("PATH", tmp.getRoot().getPath());
        File elsewhere = new File(tmp.getRoot(), "samples/gallery");
        try {
            UnityProjectBuilder.locateDotnet(null, env, elsewhere);
            fail();
        } catch (BuildException e) {
            String m = e.getMessage();
            assertTrue(m, m.startsWith(elsewhere.getPath() + " holds a Unity project"));
            assertFalse(m, m.contains("src/main/unity"));
        }
    }

    /// Output compiled on another machine is used only with the exact jars
    /// and project files it was compiled from. A build that has such output
    /// and still needs the SDK says that the output does not match, since
    /// installing an SDK is not what its owner meant to do.
    @Test
    public void outputOfOtherInputsIsNamedWhenTheSdkIsMissing() throws Exception {
        builder().run();
        File state = new File(target, "unity/state.txt");
        assertTrue(state.isFile());
        write(new File(unity, "Assets/Later.cs"), "class Later {}");
        File noSdk = new File(tmp.getRoot(), "no/such/dotnet");
        UnityProjectBuilder real = new UnityProjectBuilder(unity, generated, classes, target, runtimeJar,
                referencesJar, Collections.singletonList(runtimeJar), noSdk.getPath(), "com.acme.game", "MyGame",
                Collections.singletonList(sources), new SystemStreamLog());
        try {
            real.run();
            fail();
        } catch (BuildException e) {
            String m = e.getMessage();
            assertTrue(m, m.startsWith("cn1.unity.dotnet names "));
            assertTrue(m, m.contains("does not match these sources or jars"));
            assertTrue(m, m.contains(state.getPath()));
            assertTrue(m, m.contains(unity.getPath()));
        }
        // The first build of a project has no output to explain.
        assertFalse(state.exists());
        try {
            real.run();
            fail();
        } catch (BuildException e) {
            assertFalse(e.getMessage(), e.getMessage().contains("does not match"));
        }
    }

    /// A builder with a tool class path, whose INFO lines are kept.
    private UnityProjectBuilder told(File tool, final List<String> said) {
        UnityProjectBuilder b = new UnityProjectBuilder(unity, generated, classes, target, runtimeJar, referencesJar,
                Collections.singletonList(tool), null, "com.acme.game", "MyGame", Collections.singletonList(sources),
                new SystemStreamLog() {
                    @Override
                    public void info(CharSequence content) {
                        said.add(content.toString());
                    }
                });
        b.setToolchain(tools);
        return b;
    }

    private static String mismatch(List<String> said) {
        for (String line : said) {
            if (line.contains("does not match these sources or jars")) {
                return line;
            }
        }
        return null;
    }

    /// Output compiled on another machine that is refused here has to say
    /// which of its four inputs is not the one it was compiled from. The
    /// first time it happened the log said only that one of them was, from a
    /// runner nobody could look at afterwards.
    @Test
    public void aMismatchNamesThePartAndTheFirstEntryThatDiffer() throws Exception {
        File tool = zip(new File(tmp.getRoot(), "tool.jar"), "com/acme/Translator.class", "org/asm/Reader.class");
        List<String> said = new ArrayList<String>();
        assertTrue(told(tool, said).run());
        assertEquals(said.toString(), null, mismatch(said));
        // Nothing changed: nothing is said, and nothing is compiled.
        assertTrue(told(tool, said).run());
        assertEquals(said.toString(), null, mismatch(said));
        assertEquals(1, tools.compiled);

        // The project: one file edited, one added.
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player { int lives; }");
        write(new File(unity, "Assets/Later.cs"), "class Later {}");
        assertTrue(told(tool, said).run());
        String m = mismatch(said);
        assertTrue(m, m.contains("What differs: the project files ("));
        assertTrue(m, m.contains("; 2 of 4 entries differ, the first: Assets/Later.cs is there now and is not"
                + " recorded)."));
        assertFalse(m, m.contains("; the codenameone-"));
        assertFalse(m, m.contains("; the 'references' jar"));

        // A file that is gone, and one whose content is another.
        said.clear();
        assertTrue(new File(unity, "Assets/Later.cs").delete());
        assertTrue(told(tool, said).run());
        m = mismatch(said);
        assertTrue(m, m.contains("1 of 4 entries differ, the first: Assets/Later.cs is recorded and is not there"
                + " now)."));
        said.clear();
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player { int score; }");
        assertTrue(told(tool, said).run());
        m = mismatch(said);
        assertTrue(m, m.contains("1 of 3 entries differ, the first: Assets/Scripts/Player.cs has other content)."));

        // The runtime jar.
        said.clear();
        zip(runtimeJar, "UnityEngine/Vector2.class", "UnityEngine/Vector3.class",
                "com/codename1/unitycompat/unityengine/UnityRuntime.class", "META-INF/MANIFEST.MF");
        assertTrue(told(tool, said).run());
        m = mismatch(said);
        assertTrue(m, m.contains("What differs: the codenameone-unity-compat jar ("));
        assertTrue(m, m.contains("1 of 3 entries differ, the first: UnityEngine/Vector3.class is there now and is"
                + " not recorded)."));
        assertFalse(m, m.contains("the project files"));

        // The references.
        said.clear();
        zip(referencesJar, "netstandard.dll", "UnityEngine.dll", "Codename1.UnityValues.dll", "Other.dll");
        assertTrue(told(tool, said).run());
        m = mismatch(said);
        assertTrue(m, m.contains("What differs: the 'references' jar ("));
        assertTrue(m, m.contains("the first: Other.dll is there now and is not recorded)."));

        // The tool, whose jars are one class path: the same classes from
        // another jar of another name are the same content.
        said.clear();
        File renamed = zip(new File(tmp.getRoot(), "tool-2.jar"), "com/acme/Translator.class",
                "org/asm/Reader.class");
        assertTrue(told(renamed, said).run());
        assertEquals(said.toString(), null, mismatch(said));
        File other = zip(new File(tmp.getRoot(), "tool-3.jar"), "com/acme/Translator.class");
        assertTrue(told(other, said).run());
        m = mismatch(said);
        assertTrue(m, m.contains("What differs: the codenameone-cil-translator class path ("));
        assertTrue(m, m.contains("1 of 2 entries differ, the first: org/asm/Reader.class is recorded and is not"
                + " there now)."));

        // Two parts at once are both named, in the order they are listed.
        said.clear();
        write(new File(unity, "Assets/Scripts/Player.cs"), "class Player {}");
        assertTrue(told(tool, said).run());
        m = mismatch(said);
        assertTrue(m, m.indexOf("What differs: the project files (") > 0);
        assertTrue(m, m.indexOf("; the codenameone-cil-translator class path (")
                > m.indexOf("What differs: the project files ("));
    }

    /// The first line of the state file is the digest and the only line a
    /// build is skipped on; what follows is read back as it was written.
    @Test
    public void theStateFileRecordsThePartsUnderTheDigest() throws Exception {
        assertTrue(builder().run());
        File state = new File(target, "unity/state.txt");
        String text = new String(Files.readAllBytes(state.toPath()), StandardCharsets.UTF_8);
        UnityProjectBuilder.State recorded = UnityProjectBuilder.State.parse(text);
        assertEquals(text, text.substring(0, text.indexOf('\n')), recorded.digest);
        assertEquals(64, recorded.digest.length());
        assertEquals(UnityProjectBuilder.STATE_VERSION, recorded.version);
        assertEquals(Arrays.asList("project", "runtime", "references", "tool"),
                new ArrayList<String>(recorded.parts.keySet()));
        // Three files of the project, two classes of the runtime (META-INF
        // is not content), three assemblies, and no tool in this fixture.
        assertEquals(text, 8, recorded.entries.size());
        assertEquals(text, recorded.text());
        assertEquals(recorded.digest, UnityProjectBuilder.State.of(recorded.entries).digest);
        // Line endings a checkout or an archive rewrote do not make it
        // another record.
        UnityProjectBuilder.State crlf = UnityProjectBuilder.State.parse(text.replace("\n", "\r\n"));
        assertEquals(recorded.digest, crlf.digest);
        assertEquals(recorded.entries, crlf.entries);
        assertEquals(recorded.parts, crlf.parts);
        assertEquals(null, UnityProjectBuilder.State.parse(null));
    }

    /// A state file with nothing under its digest -- an older build's, or one
    /// that was overwritten -- is refused as any other, and says that it has
    /// nothing to compare.
    @Test
    public void aStateFileOfADigestAloneSaysItCannotNameThePart() throws Exception {
        assertTrue(builder().run());
        File state = new File(target, "unity/state.txt");
        write(state, "stale\n");
        List<String> said = new ArrayList<String>();
        assertTrue(told(runtimeJar, said).run());
        String m = mismatch(said);
        assertTrue(m, m.contains("Which of them differs cannot be said: the file records a digest (stale, now "));
        assertEquals(2, tools.compiled);
        // An empty file is not the digest of anything, either.
        write(state, "");
        said.clear();
        assertTrue(told(runtimeJar, said).run());
        assertTrue(said.toString(), mismatch(said).contains("records a digest (none, now "));
        assertEquals(3, tools.compiled);
    }

    /// The same content under another version of the record: the digest
    /// covers the version, and no part is blamed for it.
    @Test
    public void aRecordOfAnotherVersionWithTheSameContentBlamesNoPart() throws Exception {
        UnityProjectBuilder.State now = UnityProjectBuilder.State.of(Arrays.asList(
                "Assets/A.cs=00", "runtime!a/B.class:1:ff"));
        UnityProjectBuilder.State old = UnityProjectBuilder.State.parse(
                now.text().replace("version=" + UnityProjectBuilder.STATE_VERSION, "version=0")
                        .replace(now.digest, "0123"));
        String said = UnityProjectBuilder.differences(old, now);
        assertEquals("All four are the same content: the file was written as version 0 of this record and the"
                + " build writes version " + UnityProjectBuilder.STATE_VERSION + ".", said);
        // And a part the file gives a digest for, without its entries.
        UnityProjectBuilder.State bare = UnityProjectBuilder.State.parse("0123\nversion=3\npart.project=aa\n"
                + "part.runtime=" + now.parts.get("runtime") + "\npart.references=" + now.parts.get("references")
                + "\npart.tool=" + now.parts.get("tool") + "\nruntime!a/B.class:1:ff\n");
        said = UnityProjectBuilder.differences(bare, now);
        assertTrue(said, said.startsWith("What differs: the project files (aa recorded, "));
        assertTrue(said, said.endsWith("; the file lists none of its 1 entries)."));
    }

    @Test
    public void readsTheSdkVersion() {
        assertEquals(10, UnityProjectBuilder.sdkMajor(Arrays.asList("10.0.401")));
        assertEquals(8, UnityProjectBuilder.sdkMajor(Arrays.asList("", "Welcome to .NET!", "  8.0.100-rc.1  ")));
        assertEquals(-1, UnityProjectBuilder.sdkMajor(Arrays.asList("No .NET SDKs were found.", "Download: x")));
        assertEquals(-1, UnityProjectBuilder.sdkMajor(Collections.<String>emptyList()));
    }

    /// The real toolchain against a stand-in `dotnet`: an SDK too old for the
    /// scripts, and a runtime with no SDK at all, are both refused before
    /// anything is compiled, each with what to install.
    @Test
    public void refusesAnSdkThatIsTooOldOrIsNoSdk() throws Exception {
        Assume.assumeFalse("the stand-in is a shell script", UnityProjectBuilder.isWindows());
        File old = script("old", "echo 3.1.426");
        File runtimeOnly = script("runtime", "echo 'No .NET SDKs were found.'; exit 145");
        assertRefused(old, "is the .NET SDK 3.1.426, which is too old");
        assertRefused(runtimeOnly, "A .NET runtime alone cannot compile C#");
    }

    private File script(String dir, String body) throws IOException {
        File f = new File(tmp.newFolder(dir), "dotnet");
        write(f, "#!/bin/sh\n" + body + "\n");
        assertTrue(f.setExecutable(true));
        return f;
    }

    private void assertRefused(File dotnet, String expected) {
        UnityProjectBuilder b = new UnityProjectBuilder(unity, generated, classes, target, runtimeJar, referencesJar,
                Collections.singletonList(runtimeJar), dotnet.getPath(), null, null, null, new SystemStreamLog());
        try {
            b.run();
            fail("built with " + dotnet);
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
            assertTrue(e.getMessage(), e.getMessage().contains("Install the .NET SDK"));
        }
    }
}
