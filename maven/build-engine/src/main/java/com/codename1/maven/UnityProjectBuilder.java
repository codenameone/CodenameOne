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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/// Builds the Unity project of an application module (`src/main/unity`, laid
/// out like any Unity project: `Assets/` and `ProjectSettings/`) into what
/// every Codename One target already knows how to build. Shared by the Maven
/// goal `compile-unity` and the Gradle task.
///
/// Four steps, each a child process so the build tool's own JVM never loads
/// the translator:
///
/// 1. the .NET SDK compiles `Assets/**/*.cs` to one assembly, against the
///    reference assemblies of the runtime (never against Unity's own);
/// 2. the CIL translator turns that assembly into class files;
/// 3. the scene compiler turns the scenes and prefabs into Java source
///    (`com.codename1.generated.unity.UnityAppImpl`), because a device build
///    has no reflection to instantiate them with at run time;
/// 4. the classes and the images the scenes draw are installed in the classes
///    directory, where the build tool's own `javac` then compiles the
///    generated source beside them.
///
/// Steps 1 to 3 are skipped while nothing they read has changed, so a project
/// that is only being run again does not need the .NET SDK at all. Nothing is
/// written outside the build directory: the SDK's first-run files and its
/// package cache are pointed into it unless the environment already names
/// them.
public class UnityProjectBuilder {

    /// The runtime translated scripts run against. An application depends on
    /// it like on any library, so every target ships it.
    public static final String RUNTIME_ARTIFACT = "codenameone-unity-compat";

    /// The translator and scene compiler; a build-time tool, never shipped.
    public static final String TOOL_ARTIFACT = "codenameone-cil-translator";

    /// The classifier under which [#RUNTIME_ARTIFACT] publishes the assemblies
    /// C# compiles against. A classifier and not resources of the runtime
    /// jar, because that jar is merged into the application and the
    /// assemblies are megabytes no device has a use for.
    public static final String REFERENCES_CLASSIFIER = "references";

    /// The package of the class the scene compiler generates.
    public static final String GENERATED_PACKAGE = "com.codename1.generated.unity";

    /// The class the scene compiler generates.
    public static final String APP_IMPL_CLASS = "UnityAppImpl";

    /// The lifecycle class a generated main class extends.
    public static final String APPLICATION_CLASS = "com.codename1.unitycompat.app.UnityApplication";

    /// The property both build tools read the `dotnet` executable from.
    public static final String DOTNET_PROPERTY = "cn1.unity.dotnet";

    /// The oldest .NET SDK accepted. The scripts are compiled as C# 9 for
    /// `netstandard2.1`; this is the oldest long-term-support SDK whose
    /// compiler does that.
    static final int MINIMUM_SDK_MAJOR = 6;

    private static final String NETSTANDARD = "netstandard.dll";
    private static final String ENGINE = "UnityEngine.dll";
    private static final String VALUES = "Codename1.UnityValues.dll";
    private static final String SCRIPTS = "Assembly-CSharp.dll";
    private static final String TRANSLATOR_MAIN = "com.codename1.cil.translate.Translator";
    private static final String SCENE_COMPILER_MAIN = "com.codename1.unity.scenecompiler.SceneCompiler";
    /// Bumped when what the steps write changes, so an upgraded plugin does
    /// not trust the staging directory an older one left.
    private static final String STATE_VERSION = "2";
    private final File unityDir;
    private final File javaOut;
    private final File classesOut;
    private final File workDir;
    private final File runtimeJar;
    private final File referencesJar;
    private final List<File> toolClasspath;
    private final String dotnetSetting;
    private final String mainPackage;
    private final String mainClass;
    private final List<File> sourceRoots;
    private final Log log;
    private Toolchain toolchain;
    /// Why an output that was already under the work directory is not being
    /// used, or null when there was none. Said along with a missing SDK: a
    /// build that was given compiled output and still asks for the SDK was
    /// given output of something else, and that is the fact to act on.
    private String staleOutput;

    /// The three external steps. An interface so the tests can count and fake
    /// them: the logic around them (what is skipped, what is installed, what
    /// is removed) must be testable on a machine with no .NET SDK.
    interface Toolchain {
        /// Compiles the generated C# project; its assembly lands in
        /// `bin/Release/netstandard2.1` beside it.
        void compileScripts(File csproj, File logFile) throws BuildException;

        /// Translates `assemblies` to class files under `out`. Returns what
        /// the translator printed.
        List<String> translate(File out, File runtimeClasses, List<File> references, List<File> assemblies,
                File logFile) throws BuildException;

        /// Compiles the project's scenes to `javaOut` and copies the images
        /// they draw to `resources`. Returns what the compiler printed.
        List<String> compileScenes(File project, File javaOut, File resources, List<File> references,
                List<File> assemblies, File logFile) throws BuildException;
    }

    /// Logs the `warning:` and `note:` lines of a tool's output, and
    /// answers how many warnings there were.
    private int report(List<String> lines) {
        int warnings = 0;
        for (String line : lines) {
            if (line.startsWith("warning:")) {
                log.warn("src/main/unity: " + line.substring("warning:".length()).trim());
                warnings++;
            } else if (line.startsWith("note:")) {
                log.info("src/main/unity: " + line.substring("note:".length()).trim());
            }
        }
        return warnings;
    }

    /// @param unityDir      `src/main/unity`
    /// @param javaOut       generated sources (`target/generated-sources/unity`)
    /// @param classesOut    where the translated classes and the images go (the classes directory)
    /// @param buildDir      `target`; everything else is written under its `unity` directory
    /// @param runtimeJar    the `codenameone-unity-compat` jar
    /// @param referencesJar its `references` classifier
    /// @param toolClasspath the `codenameone-cil-translator` jar and its dependencies
    /// @param dotnetSetting the `dotnet` executable or its directory, or null to look for it
    /// @param mainPackage   `codename1.packageName`, or null
    /// @param mainClass     `codename1.mainName`, or null
    /// @param sourceRoots   the project's own source roots, to see whether the main class exists
    public UnityProjectBuilder(File unityDir, File javaOut, File classesOut, File buildDir, File runtimeJar,
                               File referencesJar, List<File> toolClasspath, String dotnetSetting,
                               String mainPackage, String mainClass, List<File> sourceRoots, Log log) {
        this.unityDir = unityDir;
        this.javaOut = javaOut;
        this.classesOut = classesOut;
        this.workDir = new File(buildDir, "unity");
        this.runtimeJar = runtimeJar;
        this.referencesJar = referencesJar;
        this.toolClasspath = toolClasspath == null ? new ArrayList<File>() : new ArrayList<File>(toolClasspath);
        this.dotnetSetting = dotnetSetting;
        this.mainPackage = mainPackage;
        this.mainClass = mainClass;
        this.sourceRoots = sourceRoots == null ? new ArrayList<File>() : new ArrayList<File>(sourceRoots);
        this.log = log;
    }

    /// A Unity project has both directories. `Assets` alone is not enough to
    /// go by: the scenes to build, the physics settings and the input axes
    /// are all read from `ProjectSettings`.
    public static boolean isUnityProject(File unityDir) {
        return unityDir != null && new File(unityDir, "Assets").isDirectory()
                && new File(unityDir, "ProjectSettings").isDirectory();
    }

    void setToolchain(Toolchain toolchain) {
        this.toolchain = toolchain;
    }

    /// Builds the project unless nothing changed since the last run. Returns
    /// false when there is no Unity project.
    public boolean run() throws BuildException {
        if (!isUnityProject(unityDir)) {
            if (unityDir != null && unityDir.isDirectory()) {
                log.warn(unityDir + " is not a Unity project (it needs both Assets and ProjectSettings); ignored");
            }
            return false;
        }
        if (runtimeJar == null || !runtimeJar.isFile()) {
            throw new BuildException("src/main/unity exists but the " + RUNTIME_ARTIFACT
                    + " dependency is missing; add it to the common module, or run cn1:import-unity-project,"
                    + " which adds it.");
        }
        if (referencesJar == null || !referencesJar.isFile()) {
            throw new BuildException("The reference assemblies of " + RUNTIME_ARTIFACT + " (its '"
                    + REFERENCES_CLASSIFIER + "' classifier) could not be resolved; the C# scripts in"
                    + " src/main/unity cannot be compiled without them.");
        }
        File state = new File(workDir, "state.txt");
        File staged = new File(workDir, "app");
        File resources = new File(workDir, "resources");
        File runtimeClasses = new File(workDir, "runtime");
        File appImpl = new File(javaOut, GENERATED_PACKAGE.replace('.', File.separatorChar) + File.separator
                + APP_IMPL_CLASS + ".java");
        String digest = digest();
        String stored = read(state);
        if (digest.equals(stored) && staged.isDirectory() && appImpl.isFile()) {
            log.debug("Unity project unchanged; not compiled again");
        } else {
            staleOutput = staleOutput(state, stored, staged, appImpl);
            if (staleOutput != null) {
                log.info(staleOutput);
            }
            // Deleted first: a step that fails must not leave the next build
            // believing the staging directory matches the sources.
            deleteQuietly(state);
            compile(staged, resources, runtimeClasses);
            if (!appImpl.isFile()) {
                throw new BuildException("The scene compiler wrote no " + appImpl + " (log: "
                        + new File(workDir, "scene-compiler.log") + ")");
            }
            write(state, digest);
        }
        install(staged, resources, runtimeClasses);
        writeMainClass();
        return true;
    }

    /// What to say about compiled output that is present and cannot be used,
    /// or null when the work directory holds none: a first build has nothing
    /// to explain.
    private String staleOutput(File state, String stored, File staged, File appImpl) {
        if (stored == null) {
            return null;
        }
        if (!staged.isDirectory() || !appImpl.isFile()) {
            return state + " is there, but " + (staged.isDirectory() ? appImpl : staged) + " is not: the"
                    + " compiled output of " + unityDir + " is incomplete and has to be made again.";
        }
        return "The compiled output under " + workDir + " does not match these sources or jars: " + state
                + " records other content than " + unityDir + ", the " + RUNTIME_ARTIFACT + " jar, its '"
                + REFERENCES_CLASSIFIER + "' jar and the " + TOOL_ARTIFACT + " class path have now. Output"
                + " compiled elsewhere is good only with the exact project files and jars it was compiled"
                + " from, so the project has to be compiled again.";
    }

    private void compile(File staged, File resources, File runtimeClasses) throws BuildException {
        if (toolClasspath.isEmpty() && toolchain == null) {
            throw new BuildException("The " + TOOL_ARTIFACT + " tool could not be resolved; the Unity project in"
                    + " src/main/unity cannot be translated without it.");
        }
        Toolchain tools = toolchain != null ? toolchain : new ForkedToolchain();
        File refs = new File(workDir, "ref");
        File cs = new File(workDir, "cs");
        delete(refs);
        delete(cs);
        delete(staged);
        delete(resources);
        delete(runtimeClasses);
        // The scene compiler's output is the only thing of ours in javaOut
        // that a changed project can make stale.
        delete(new File(javaOut, GENERATED_PACKAGE.replace('.', File.separatorChar)));
        mkdirs(refs);
        mkdirs(cs);
        mkdirs(staged);
        mkdirs(resources);
        mkdirs(javaOut);
        try {
            extract(referencesJar, refs, ".dll");
            extract(runtimeJar, runtimeClasses, ".class");
        } catch (IOException e) {
            throw new BuildException("Cannot unpack the Unity runtime: " + e.getMessage(), e);
        }
        File netstandard = new File(refs, NETSTANDARD);
        File engine = new File(refs, ENGINE);
        File values = new File(refs, VALUES);
        for (File f : new File[] {netstandard, engine, values}) {
            if (!f.isFile()) {
                throw new BuildException(referencesJar + " has no " + f.getName()
                        + "; it is not the reference assemblies of " + RUNTIME_ARTIFACT);
            }
        }
        File csproj = new File(cs, "Assembly-CSharp.csproj");
        write(csproj, csproj(new File(unityDir, "Assets"), netstandard, engine, values));

        log.info("Compiling the C# scripts of " + unityDir);
        tools.compileScripts(csproj, new File(workDir, "dotnet.log"));
        File scripts = new File(cs, "bin/Release/netstandard2.1/" + SCRIPTS);
        if (!scripts.isFile()) {
            throw new BuildException("The C# build produced no " + scripts + " (log: "
                    + new File(workDir, "dotnet.log") + ")");
        }
        List<File> references = new ArrayList<File>();
        references.add(engine);
        references.add(netstandard);
        // The value types are translated along with the scripts, not only
        // referenced: the translator keeps the names of what it translates and
        // maps what it merely references onto the hand-written runtime.
        List<File> assemblies = new ArrayList<File>();
        assemblies.add(values);
        assemblies.add(scripts);
        // Both tools say what they could not carry over, a line each; a
        // build that kept those to its log files would be a game that
        // differs from the one in Unity with nothing to say why.
        int warnings = report(tools.translate(staged, runtimeClasses, references, assemblies,
                new File(workDir, "translate.log")));
        List<File> sceneAssemblies = new ArrayList<File>();
        sceneAssemblies.add(scripts);
        sceneAssemblies.add(values);
        warnings += report(tools.compileScenes(unityDir, javaOut, resources, references, sceneAssemblies,
                new File(workDir, "scene-compiler.log")));
        log.info("Compiled the Unity project" + (warnings == 0 ? "" : " with " + warnings
                + " warning(s): what they name will not behave as it does in Unity"));
    }

    /// The C# project that compiles the scripts where they are. Its
    /// references are the three assemblies and nothing else: with the
    /// implicit framework reference left on, the SDK would download the
    /// `netstandard` targeting pack from NuGet on every clean build, and fail
    /// offline.
    static String csproj(File assets, File netstandard, File engine, File values) {
        String a = msbuild(assets.getAbsolutePath());
        return "<Project Sdk=\"Microsoft.NET.Sdk\">\n"
                + "  <PropertyGroup>\n"
                + "    <OutputType>Library</OutputType>\n"
                + "    <TargetFramework>netstandard2.1</TargetFramework>\n"
                + "    <LangVersion>9.0</LangVersion>\n"
                + "    <Nullable>disable</Nullable>\n"
                + "    <ImplicitUsings>disable</ImplicitUsings>\n"
                + "    <AssemblyName>Assembly-CSharp</AssemblyName>\n"
                + "    <Deterministic>true</Deterministic>\n"
                + "    <DebugType>portable</DebugType>\n"
                + "    <Optimize>true</Optimize>\n"
                + "    <GenerateAssemblyInfo>false</GenerateAssemblyInfo>\n"
                + "    <EnableDefaultCompileItems>false</EnableDefaultCompileItems>\n"
                + "    <DisableImplicitFrameworkReferences>true</DisableImplicitFrameworkReferences>\n"
                + "    <NoWarn>$(NoWarn);CS0649;CS0414;CS0169;CS0108;CS0114;CS0618</NoWarn>\n"
                + "  </PropertyGroup>\n"
                + "  <ItemGroup>\n"
                // Editor scripts are not part of a player build.
                + "    <Compile Include=\"" + a + "/**/*.cs\" Exclude=\"" + a + "/**/Editor/**/*.cs\" />\n"
                + reference("netstandard", netstandard)
                + reference("UnityEngine", engine)
                + reference("Codename1.UnityValues", values)
                + "  </ItemGroup>\n"
                + "</Project>\n";
    }

    private static String reference(String name, File assembly) {
        return "    <Reference Include=\"" + name + "\">\n"
                + "      <HintPath>" + msbuild(assembly.getAbsolutePath()) + "</HintPath>\n"
                + "      <Private>false</Private>\n"
                + "    </Reference>\n";
    }

    /// A path as MSBuild reads it literally, inside XML: its own special
    /// characters percent-encoded, then XML's escaped.
    static String msbuild(String path) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            switch (c) {
                case '%':
                    b.append("%25");
                    break;
                case '$':
                    b.append("%24");
                    break;
                case '@':
                    b.append("%40");
                    break;
                case ';':
                    b.append("%3B");
                    break;
                case '\'':
                    b.append("%27");
                    break;
                case '*':
                    b.append("%2A");
                    break;
                case '?':
                    b.append("%3F");
                    break;
                case '&':
                    b.append("&amp;");
                    break;
                case '<':
                    b.append("&lt;");
                    break;
                case '>':
                    b.append("&gt;");
                    break;
                case '"':
                    b.append("&quot;");
                    break;
                case '\\':
                    b.append('/');
                    break;
                default:
                    b.append(c);
                    break;
            }
        }
        return b.toString();
    }

    /// Copies what the steps staged into the classes directory, and removes
    /// what an earlier run installed and this one did not: a script deleted
    /// from the project must not stay behind as a class.
    ///
    /// This runs on every build, also when nothing was compiled: the classes
    /// directory is the build tool's, and a clean of it alone must not lose
    /// the game.
    private void install(File staged, File resources, File runtimeClasses) throws BuildException {
        File record = new File(workDir, "installed.txt");
        Set<String> previous = new LinkedHashSet<String>();
        String recorded = read(record);
        if (recorded != null) {
            for (String line : recorded.split("\n")) {
                if (line.length() > 0) {
                    previous.add(line);
                }
            }
        }
        Set<String> installed = new TreeSet<String>();
        List<String> files = new ArrayList<String>();
        collect(staged, "", files);
        int classes = 0;
        try {
            for (String rel : files) {
                // The runtime jar already ships the value types, translated
                // from the same assembly. A second copy in the application
                // would be one more definition of the class for each target's
                // packaging to pick between.
                if (new File(runtimeClasses, rel).isFile()) {
                    continue;
                }
                copyIfChanged(new File(staged, rel), new File(classesOut, rel));
                installed.add(rel);
                classes++;
            }
            files.clear();
            collect(resources, "", files);
            for (String rel : files) {
                copyIfChanged(new File(resources, rel), new File(classesOut, rel));
                installed.add(rel);
            }
        } catch (IOException e) {
            throw new BuildException("Cannot install the Unity project into " + classesOut + ": " + e.getMessage(), e);
        }
        for (String rel : previous) {
            if (!installed.contains(rel)) {
                deleteQuietly(new File(classesOut, rel));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String rel : installed) {
            sb.append(rel).append('\n');
        }
        write(record, sb.toString());
        log.debug("Installed " + classes + " translated classes and " + files.size() + " images in " + classesOut);
    }

    private static void copyIfChanged(File from, File to) throws IOException {
        if (to.isFile() && to.length() == from.length() && to.lastModified() >= from.lastModified()) {
            return;
        }
        File parent = to.getParentFile();
        if (parent != null) {
            mkdirs(parent);
        }
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private File generatedMainClass() {
        return new File(javaOut, mainPackage.replace('.', File.separatorChar) + File.separator + mainClass + ".java");
    }

    /// True when the application has its own main class, in which case the
    /// generated one is deleted: left in the generated sources next to the
    /// developer's class, javac would see two definitions until a clean build.
    private boolean removeGeneratedMainClassIfWritten() {
        String rel = mainPackage.replace('.', File.separatorChar) + File.separator + mainClass;
        for (File root : sourceRoots) {
            if (root.equals(javaOut)) {
                continue;
            }
            if (new File(root, rel + ".java").isFile() || new File(root, rel + ".kt").isFile()) {
                File generated = generatedMainClass();
                if (generated.isFile() && !generated.delete()) {
                    log.warn("Cannot delete the generated " + generated + "; the application now has its own");
                }
                return true;
            }
        }
        return false;
    }

    /// Generates the Codename One main class when the project does not have
    /// one, so a Unity project builds without any Codename One code of its
    /// own. Checked on every run and not only when the project changed: a
    /// main class written or deleted since the last build must be noticed.
    private void writeMainClass() throws BuildException {
        if (mainPackage == null || mainClass == null || mainPackage.length() == 0 || mainClass.length() == 0) {
            return;
        }
        if (removeGeneratedMainClassIfWritten()) {
            return;
        }
        File out = generatedMainClass();
        String src = mainClassSource(mainPackage, mainClass,
                "// Generated by the Codename One Unity build: the application's Codename One\n"
                + "// entry point. Write this class yourself to customize startup.\n", "");
        if (src.equals(read(out))) {
            return;
        }
        write(out, src);
    }

    /// The source of a main class that runs the project's first scene. The
    /// subclass names the generated class itself, as source: the runtime jar
    /// was compiled before the project existed and has no other way to reach
    /// it without reflection.
    static String mainClassSource(String pkg, String cls, String fileComment, String classComment) {
        return fileComment
                + "package " + pkg + ";\n\n"
                + classComment
                + "public class " + cls + " extends " + APPLICATION_CLASS + " {\n"
                + "    @Override\n"
                + "    protected void installProject() {\n"
                + "        " + GENERATED_PACKAGE + "." + APP_IMPL_CLASS + ".install();\n"
                + "    }\n"
                + "}\n";
    }

    /// What the build is skipped on the strength of: the content of
    /// everything the steps read, and nothing of where or when.
    ///
    /// The staging directory may be made on one machine and used on
    /// another -- compiled once where the .NET SDK is, then handed with its
    /// state file to the jobs that package each target -- and a fresh
    /// checkout there has its own path and its own modification times. So
    /// neither is in here. Every file of `Assets` and `ProjectSettings` is
    /// its path below the project, with `/` between the names, and a
    /// SHA-256 of its bytes; the lines are sorted as `String` compares
    /// them, which no locale changes. A length and a time would also miss
    /// an edit of the same length whose time was kept, as a checkout that
    /// restores times makes of `speed = 1` to `speed = 2`.
    ///
    /// That reads the whole project on every build, also the ones that
    /// change nothing. Measured: SHA-256 through 256 MB took 1.1 to 1.6
    /// seconds on JDK 8 and on JDK 17 here, about 200 MB a second, so a
    /// gigabyte of pictures and sound is some five seconds; the sample
    /// projects under `scripts/unity-compat-samples`, 0.2 MB between
    /// them, take under ten milliseconds.
    ///
    /// The runtime, the references and the tool are in it by what their
    /// jars hold -- see [#stamp] -- and by the role they have here, never
    /// by a file name, which has a version and a directory in it.
    private String digest() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1 << 16];
            List<String> entries = new ArrayList<String>();
            collectStamped(new File(unityDir, "Assets"), "Assets/", entries, md, buffer);
            collectStamped(new File(unityDir, "ProjectSettings"), "ProjectSettings/", entries, md, buffer);
            stamp(runtimeJar, "runtime!", entries, md, buffer);
            stamp(referencesJar, "references!", entries, md, buffer);
            for (File f : toolClasspath) {
                // One label for all of them: the order of a class path and
                // how many jars the classes came in are not content.
                stamp(f, "tool!", entries, md, buffer);
            }
            Collections.sort(entries);
            md.reset();
            md.update(STATE_VERSION.getBytes(StandardCharsets.UTF_8));
            for (String e : entries) {
                md.update((byte) '\n');
                md.update(e.getBytes(StandardCharsets.UTF_8));
            }
            StringBuilder sb = new StringBuilder();
            hex(sb, md.digest());
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // No digest, no skipping: an empty string never equals a stored one.
            return "";
        } catch (IOException e) {
            // Nor for a file that cannot be read: the build that follows
            // says which and why.
            return "";
        }
    }

    /// The lines of one jar, or of a directory of classes standing in for
    /// one. A jar is read by its directory: each entry's name, length and
    /// CRC-32, which is of the entry's bytes as they are unpacked. The file
    /// itself is not hashed, since the same classes packed twice are two
    /// different files -- every entry carries the time it was packed at.
    /// `META-INF` is left out for the same reason: the manifest names the
    /// JDK and the machine that packed it, and no step reads anything
    /// there.
    private static void stamp(File f, String label, List<String> out, MessageDigest md, byte[] buffer)
            throws IOException {
        if (f.isDirectory()) {
            collectStamped(f, label, out, md, buffer);
            return;
        }
        try (ZipFile zip = new ZipFile(f)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.isDirectory() && !e.getName().startsWith("META-INF/")) {
                    out.add(label + e.getName() + ":" + e.getSize() + ":" + Long.toHexString(e.getCrc()));
                }
            }
        } catch (ZipException e) {
            // Not a jar: a file of some other kind on the class path, by
            // its bytes and without its name.
            md.reset();
            try (InputStream in = Files.newInputStream(f.toPath())) {
                int n = in.read(buffer);
                while (n >= 0) {
                    md.update(buffer, 0, n);
                    n = in.read(buffer);
                }
            }
            StringBuilder line = new StringBuilder(label).append('=');
            hex(line, md.digest());
            out.add(line.toString());
        }
    }

    /// One line for each file below a directory: its path from `prefix`
    /// on, and a SHA-256 of its bytes.
    private static void collectStamped(File dir, String prefix, List<String> out, MessageDigest md, byte[] buffer)
            throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                collectStamped(f, prefix + name + "/", out, md, buffer);
                continue;
            }
            md.reset();
            try (InputStream in = Files.newInputStream(f.toPath())) {
                int n = in.read(buffer);
                while (n >= 0) {
                    md.update(buffer, 0, n);
                    n = in.read(buffer);
                }
            }
            StringBuilder line = new StringBuilder(prefix).append(name).append('=');
            hex(line, md.digest());
            out.add(line.toString());
        }
    }

    private static void hex(StringBuilder sb, byte[] bytes) {
        for (byte b : bytes) {
            sb.append(Integer.toHexString((b & 0xff) | 0x100).substring(1));
        }
    }

    private static void collect(File dir, String prefix, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, prefix + f.getName() + "/", out);
            } else {
                out.add(prefix + f.getName());
            }
        }
    }

    /// Unpacks the entries of `jar` whose names end in `suffix`.
    private static void extract(File jar, File into, String suffix) throws IOException {
        String root = into.getCanonicalPath() + File.separator;
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().endsWith(suffix)) {
                    continue;
                }
                File out = new File(into, e.getName());
                // An entry named with ".." would be written outside the
                // directory; no jar of ours has one.
                if (!out.getCanonicalPath().startsWith(root)) {
                    throw new IOException(jar + " has an entry outside its root: " + e.getName());
                }
                File parent = out.getParentFile();
                if (parent != null) {
                    mkdirs(parent);
                }
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void mkdirs(File dir) {
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("Cannot create " + dir);
        }
    }

    private static void delete(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                delete(c);
            }
        }
        deleteQuietly(f);
    }

    private static void deleteQuietly(File f) {
        try {
            Files.deleteIfExists(f.toPath());
        } catch (IOException e) {
            // Whatever is in the way is reported by the step that then fails
            // to write here, with the path.
            return;
        }
    }

    /// The text of a file, or null when it cannot be read.
    private static String read(File f) {
        if (!f.isFile()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static void write(File f, String text) throws BuildException {
        try {
            File parent = f.getParentFile();
            if (parent != null) {
                mkdirs(parent);
            }
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BuildException("Cannot write " + f + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------- the SDK

    static boolean isWindows() {
        return File.separatorChar == '\\';
    }

    /// The `dotnet` executable: the explicit setting, then `DOTNET_ROOT`, then
    /// the `PATH`. The setting may name the executable, the directory it is
    /// in, or a bare command to look for on the `PATH`.
    ///
    /// @param setting the value of [#DOTNET_PROPERTY], or null
    /// @param env     the environment to read `DOTNET_ROOT` and `PATH` from
    static File locateDotnet(String setting, Map<String, String> env) throws BuildException {
        return locateDotnet(setting, env, null);
    }

    /// As [#locateDotnet(String, Map)], naming the project that needs the
    /// SDK in the failure: a build may be configured to read its Unity
    /// project from anywhere, and `src/main/unity` is then a directory that
    /// does not exist.
    ///
    /// @param project the Unity project, or null when it is not known
    static File locateDotnet(String setting, Map<String, String> env, File project) throws BuildException {
        String exe = isWindows() ? "dotnet.exe" : "dotnet";
        String path = env.get("PATH");
        if (path == null) {
            // Windows spells it Path, and its environment is case insensitive
            // to everything but a Java map.
            path = env.get("Path");
        }
        if (setting != null && setting.trim().length() > 0) {
            File f = new File(setting.trim());
            if (f.isDirectory() && new File(f, exe).isFile()) {
                return new File(f, exe);
            }
            if (f.isFile()) {
                return f;
            }
            File found = f.getParent() == null ? onPath(f.getName(), path) : null;
            if (found != null) {
                return found;
            }
            throw new BuildException(DOTNET_PROPERTY + " names " + setting + ", which is neither the dotnet"
                    + " executable nor the directory it is in. " + installHint());
        }
        String root = env.get("DOTNET_ROOT");
        if (root != null && root.length() > 0 && new File(root, exe).isFile()) {
            return new File(root, exe);
        }
        File found = onPath(exe, path);
        if (found != null) {
            return found;
        }
        throw new BuildException((project == null ? "src/main/unity" : project.getPath())
                + " holds a Unity project, and compiling its C# scripts needs the"
                + " .NET SDK, which was not found. Looked at the " + DOTNET_PROPERTY + " property (not set), the"
                + " DOTNET_ROOT environment variable (" + (root == null || root.length() == 0 ? "not set" : root)
                + ") and the PATH. " + installHint());
    }

    private static String installHint() {
        return "Install the .NET SDK " + MINIMUM_SDK_MAJOR + " or newer from https://dotnet.microsoft.com/download"
                + " and either put dotnet on the PATH or pass -D" + DOTNET_PROPERTY + "=/path/to/dotnet.";
    }

    private static File onPath(String name, String path) {
        if (path == null) {
            return null;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (dir.length() == 0) {
                continue;
            }
            File f = new File(dir, name);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /// The major version in what `dotnet --version` printed, or -1. The
    /// version is the first line that starts with a number; an SDK that has
    /// something to say first (a first-run notice) says it on other lines.
    static int sdkMajor(List<String> output) {
        for (String line : output) {
            String t = line.trim();
            int dot = t.indexOf('.');
            if (dot <= 0) {
                continue;
            }
            int major = 0;
            boolean digits = true;
            for (int i = 0; i < dot; i++) {
                char c = t.charAt(i);
                if (c < '0' || c > '9' || i > 4) {
                    digits = false;
                    break;
                }
                major = major * 10 + (c - '0');
            }
            if (digits) {
                return major;
            }
        }
        return -1;
    }

    /// The forked implementation: the .NET CLI and this JVM's own `java`.
    private final class ForkedToolchain implements Toolchain {
        private File dotnet() throws BuildException {
            File dotnet;
            try {
                dotnet = locateDotnet(dotnetSetting, System.getenv(), unityDir);
            } catch (BuildException e) {
                if (staleOutput == null) {
                    throw e;
                }
                throw new BuildException(e.getMessage() + "\n" + staleOutput, e);
            }
            File logFile = new File(workDir, "dotnet-version.log");
            List<String> cmd = new ArrayList<String>();
            cmd.add(dotnet.getAbsolutePath());
            cmd.add("--version");
            int exit = exec(cmd, logFile, true);
            List<String> out = lines(logFile);
            int major = sdkMajor(out);
            if (exit != 0 || major < 0) {
                // The host without an SDK answers --version with an error:
                // a runtime-only installation is the usual way to get here.
                throw new BuildException(dotnet + " did not report an SDK version (exit code " + exit + "): "
                        + tail(out, 5) + "\nA .NET runtime alone cannot compile C#. " + installHint());
            }
            if (major < MINIMUM_SDK_MAJOR) {
                throw new BuildException(dotnet + " is the .NET SDK " + out.get(0).trim() + ", which is too old to"
                        + " compile the scripts in src/main/unity. " + installHint());
            }
            return dotnet;
        }

        public void compileScripts(File csproj, File logFile) throws BuildException {
            List<String> cmd = new ArrayList<String>();
            cmd.add(dotnet().getAbsolutePath());
            cmd.add("build");
            cmd.add("-c");
            cmd.add("Release");
            cmd.add("--nologo");
            // No build servers left running behind a build that has ended:
            // they would hold the build directory open, on Windows against
            // the next clean.
            cmd.add("-nodeReuse:false");
            cmd.add("-p:UseSharedCompilation=false");
            cmd.add(csproj.getAbsolutePath());
            if (exec(cmd, logFile, true) != 0) {
                List<String> out = lines(logFile);
                List<String> errors = new ArrayList<String>();
                for (String line : out) {
                    if (line.contains(": error ") && !errors.contains(line.trim())) {
                        errors.add(line.trim());
                    }
                }
                throw new BuildException("The C# scripts in src/main/unity/Assets did not compile:\n"
                        + (errors.isEmpty() ? tail(out, 40) : tail(errors, 40)) + "\n(full log: " + logFile + ")\n"
                        + "A script that compiles in Unity and not here uses a part of the UnityEngine API the"
                        + " compatibility runtime does not have.");
            }
        }

        public List<String> translate(File out, File runtimeClasses, List<File> references, List<File> assemblies,
                File logFile) throws BuildException {
            List<String> cmd = java(TRANSLATOR_MAIN);
            cmd.add("--out");
            cmd.add(out.getAbsolutePath());
            cmd.add("--runtime");
            cmd.add(runtimeClasses.getAbsolutePath());
            for (File r : references) {
                cmd.add("--ref");
                cmd.add(r.getAbsolutePath());
            }
            for (File a : assemblies) {
                cmd.add(a.getAbsolutePath());
            }
            if (exec(cmd, logFile, false) != 0) {
                throw new BuildException("The C# scripts in src/main/unity could not be translated:\n"
                        + tail(lines(logFile), 40) + "\n(full log: " + logFile + ")");
            }
            return lines(logFile);
        }

        public List<String> compileScenes(File project, File sceneJavaOut, File resources, List<File> references,
                List<File> assemblies, File logFile) throws BuildException {
            List<String> cmd = java(SCENE_COMPILER_MAIN);
            cmd.add("--project");
            cmd.add(project.getAbsolutePath());
            cmd.add("--out");
            cmd.add(sceneJavaOut.getAbsolutePath());
            cmd.add("--resources");
            cmd.add(resources.getAbsolutePath());
            for (File r : references) {
                cmd.add("--ref");
                cmd.add(r.getAbsolutePath());
            }
            for (File a : assemblies) {
                cmd.add(a.getAbsolutePath());
            }
            int exit = exec(cmd, logFile, false);
            List<String> out = lines(logFile);
            if (exit != 0) {
                throw new BuildException("The scenes in src/main/unity could not be compiled:\n" + tail(out, 40)
                        + "\n(full log: " + logFile + ")");
            }
            return out;
        }

        private List<String> java(String mainClassName) {
            List<String> cmd = new ArrayList<String>();
            cmd.add(new File(new File(System.getProperty("java.home"), "bin"), isWindows() ? "java.exe" : "java")
                    .getAbsolutePath());
            // The scene compiler reads images with ImageIO; without this a
            // desktop JVM shows an icon in the dock for the length of a build.
            cmd.add("-Djava.awt.headless=true");
            cmd.add("-cp");
            StringBuilder cp = new StringBuilder();
            for (File f : toolClasspath) {
                if (cp.length() > 0) {
                    cp.append(File.pathSeparatorChar);
                }
                cp.append(f.getAbsolutePath());
            }
            cmd.add(cp.toString());
            cmd.add(mainClassName);
            return cmd;
        }

        /// Runs a command to its end, its output in `logFile`.
        private int exec(List<String> cmd, File logFile, boolean sdk) throws BuildException {
            mkdirs(workDir);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            // Not the project directory: a global.json there, written for the
            // Unity editor's own tooling, would pick the SDK for us.
            pb.directory(workDir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);
            if (sdk) {
                Map<String, String> env = pb.environment();
                env.put("DOTNET_CLI_TELEMETRY_OPTOUT", "1");
                env.put("DOTNET_NOLOGO", "1");
                env.put("DOTNET_CLI_USE_MSBUILD_SERVER", "0");
                env.put("MSBUILDDISABLENODEREUSE", "1");
                // Left alone when set: a CI machine that points these at a
                // cache of its own has said where they belong.
                if (!env.containsKey("DOTNET_CLI_HOME")) {
                    env.put("DOTNET_CLI_HOME", new File(workDir, "dotnet-home").getAbsolutePath());
                }
                if (!env.containsKey("NUGET_PACKAGES")) {
                    env.put("NUGET_PACKAGES", new File(workDir, "nuget-packages").getAbsolutePath());
                }
            }
            log.debug("Running " + cmd);
            try {
                return pb.start().waitFor();
            } catch (IOException e) {
                throw new BuildException("Cannot run " + cmd.get(0) + ": " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BuildException("Interrupted while running " + cmd.get(0), e);
            }
        }
    }

    private static List<String> lines(File f) {
        String text = read(f);
        List<String> out = new ArrayList<String>();
        if (text != null) {
            for (String line : text.split("\n")) {
                out.add(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
            }
        }
        return out;
    }

    private static String tail(List<String> lines, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, lines.size() - count); i < lines.size(); i++) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }
}
