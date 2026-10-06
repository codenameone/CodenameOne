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
package com.codename1.initializr.model;

import com.codename1.components.ToastBar;
import com.codename1.initializr.WebsiteThemeNative;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.system.NativeLookup;
import com.codename1.util.StringUtil;
import net.sf.zipme.CRC32;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;
import net.sf.zipme.ZipOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.codename1.ui.CN.*;

public class GeneratorModel {
    private static final String CN1_PLUGIN_VERSION = "7.0.274";
    /// Whether the Codename One Gradle plugin is published at [CN1_PLUGIN_VERSION].
    /// A Gradle download resolves the plugin by that version, so until a release
    /// carrying the plugin is what the initializr generates against, a Gradle
    /// project could not build at all -- and the UI does not offer Gradle.
    /// `update-cn1-version.sh` sets this with the version, from whether the
    /// plugin's marker exists in the repository.
    static final boolean GRADLE_PLUGIN_PUBLISHED = true;

    /// Whether the initializr offers Gradle projects; see [GRADLE_PLUGIN_PUBLISHED].
    public static boolean isGradleOffered() {
        return GRADLE_PLUGIN_PUBLISHED;
    }

    /// The first codenameone-maven-plugin that builds an app without its platform
    /// modules (common/pom.xml binds its compile-javase-natives and hosted-platform
    /// goals) and runs a backend that is the whole project. A Maven download made
    /// against an older plugin is the full multi-module layout it always was, so
    /// the new layouts light up when update-cn1-version.sh moves
    /// [CN1_PLUGIN_VERSION] to this release, with no other change.
    static final String MAVEN_LAYOUTS_SINCE = "7.0.275";

    /// Whether a Maven project gets the layout choices -- the minimal app, the
    /// optional backend module, a backend-only project -- at [CN1_PLUGIN_VERSION].
    public static boolean isMavenLayoutChoiceOffered() {
        return isVersionAtLeast(CN1_PLUGIN_VERSION, MAVEN_LAYOUTS_SINCE);
    }

    /// The plugin version a download is generated against.
    static String cn1PluginVersion() {
        return CN1_PLUGIN_VERSION;
    }

    /// Whether `version` is `min` or newer, comparing up to four numeric parts and
    /// stopping at the first character that is neither a digit nor a dot, so
    /// `8.0-SNAPSHOT` reads as 8.0. Written by hand: the Codename One runtime this
    /// runs on has no version parser, and split() would mean a regex.
    static boolean isVersionAtLeast(String version, String min) {
        int[] a = parseVersion(version);
        int[] b = parseVersion(min);
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i];
            }
        }
        return true;
    }

    private static int[] parseVersion(String version) {
        int[] out = new int[4];
        if (version == null) {
            return out;
        }
        int part = 0;
        int current = 0;
        for (int i = 0; i < version.length() && part < out.length; i++) {
            char c = version.charAt(i);
            if (c >= '0' && c <= '9') {
                current = current * 10 + (c - '0');
            } else if (c == '.') {
                out[part++] = current;
                current = 0;
            } else {
                break;
            }
        }
        if (part < out.length) {
            out[part] = current;
        }
        return out;
    }
    /// The Kotlin version a Gradle Kotlin project builds with, for both the Kotlin
    /// Gradle plugin and kotlin-stdlib (the plugin adds its own stdlib anyway, so
    /// the two cannot usefully differ).
    ///
    /// Deliberately NOT kotlin-pom.xml's 1.6.0. The Kotlin Gradle plugin is tied
    /// to the Gradle it runs in, and 1.6.0 cannot run in the Gradle 9 the shared
    /// wrapper pins: it fails at configuration with NoClassDefFoundError
    /// org/gradle/util/WrapUtil, and before that it registers a build listener the
    /// configuration cache (on in the template's gradle.properties) rejects. 2.2.10
    /// was verified to compile the Kotlin template with the com.codenameone plugin
    /// on that wrapper. The Maven template's kotlin-maven-plugin has no such tie.
    ///
    /// `GradleConversion.KOTLIN_VERSION` in maven/build-engine writes the same
    /// line into a converted Kotlin project; bump the two together.
    static final String KOTLIN_VERSION = "2.2.10";
    /// The Gradle project template, zipped at build time (common/pom.xml,
    /// package-gradle-template) from maven/build-engine's
    /// com/codename1/project/templates/gradle -- the directory the Maven and
    /// Gradle plugins write projects from, so every generator emits the same files.
    private static final String GRADLE_TEMPLATE_ZIP = "/gradle.zip";
    private static final String PREVIEW_BUTTON_SELECTOR =
            "Button, InitializrLiveButtonDarkClean, "
                    + "InitializrLiveButtonLightTealRound, InitializrLiveButtonLightTealSquare, "
                    + "InitializrLiveButtonDarkTealRound, InitializrLiveButtonDarkTealSquare, "
                    + "InitializrLiveButtonLightBlueRound, InitializrLiveButtonLightBlueSquare, "
                    + "InitializrLiveButtonDarkBlueRound, InitializrLiveButtonDarkBlueSquare, "
                    + "InitializrLiveButtonLightOrangeRound, InitializrLiveButtonLightOrangeSquare, "
                    + "InitializrLiveButtonDarkOrangeRound, InitializrLiveButtonDarkOrangeSquare";
    private static final String PREVIEW_BUTTON_PRESSED_SELECTOR =
            "Button.pressed, InitializrLiveButtonDarkClean.pressed, "
                    + "InitializrLiveButtonLightTealRound.pressed, InitializrLiveButtonLightTealSquare.pressed, "
                    + "InitializrLiveButtonDarkTealRound.pressed, InitializrLiveButtonDarkTealSquare.pressed, "
                    + "InitializrLiveButtonLightBlueRound.pressed, InitializrLiveButtonLightBlueSquare.pressed, "
                    + "InitializrLiveButtonDarkBlueRound.pressed, InitializrLiveButtonDarkBlueSquare.pressed, "
                    + "InitializrLiveButtonLightOrangeRound.pressed, InitializrLiveButtonLightOrangeSquare.pressed, "
                    + "InitializrLiveButtonDarkOrangeRound.pressed, InitializrLiveButtonDarkOrangeSquare.pressed";
    private static final String GENERATED_GITIGNORE =
            "**/target/\n" +
            ".idea/\n" +
            "*.iml\n" +
            ".DS_Store\n" +
            "Thumbs.db\n";

    private static final String AGENT_SKILL_TARGET_PREFIX = ".agent-skills/codename-one/";
    private static final String CLAUDE_SKILL_STUB_PATH = ".claude/skills/codename-one/SKILL.md";
    // The AGENTS.md pointer and the Claude Code stub are FILES rather than string
    // constants because maven/cn1app-archetype stages the very same two files into
    // archetype-resources/ (see its pom). A project generated from the archetype and one
    // downloaded from the Initializr have to hand an agent the same layout, and the only
    // way to guarantee that is one copy on disk.
    //
    // They sit flat at the root of src/main/resources for the same reason skill/ is
    // repackaged into skill.zip: Codename One's classloader rejects nested directories
    // under src/main/resources at runtime.
    //
    // Neither is stored under the name it is published as. AGENTS.md is a name agents
    // look for on their own, so a file called that here would be read as instructions
    // for THIS repository -- and it says things like "run the simulator with
    // mvn -pl common cn1:run", which is true of a generated app and false of the
    // Codename One tree. The Claude stub already had to be renamed on staging because
    // its published name is SKILL.md; this one is renamed for the opposite reason.
    private static final String CLAUDE_SKILL_STUB_RESOURCE = "/agent-skill-claude-stub.md";
    private static final String AGENTS_MD_RESOURCE = "/agent-skill-agents-md.md";
    // The Gradle variant is a second file rather than placeholders in the first:
    // maven/cn1app-archetype stages agent-skill-agents-md.md verbatim, so a token
    // there would reach every archetype-generated Maven project unrendered.
    private static final String AGENTS_MD_GRADLE_RESOURCE = "/agent-skill-agents-md-gradle.md";
    // Both shared with maven/cn1app-archetype, which stages them from this directory.
    private static final String HOSTED_PROFILES_RESOURCE = "/common-hosted-platform-profiles.xml";
    private static final String BACKEND_ONLY_POM_RESOURCE = "/backend-only-pom.xml";

    private final IDE ide;
    private final Template template;
    private final String appName;
    private final String packageName;
    private final ProjectOptions options;
    private final String pluginVersion;

    GeneratorModel(IDE ide, Template template, String appName, String packageName, ProjectOptions options) {
        this(ide, template, appName, packageName, options, CN1_PLUGIN_VERSION);
    }

    private GeneratorModel(IDE ide, Template template, String appName, String packageName, ProjectOptions options,
                           String pluginVersion) {
        this.ide = ide;
        this.template = template;
        this.appName = appName;
        this.packageName = packageName;
        this.options = options == null ? ProjectOptions.defaults() : options;
        this.pluginVersion = pluginVersion;
    }

    /// A model that generates against `pluginVersion` rather than [CN1_PLUGIN_VERSION]:
    /// how the tests and fixtures see both sides of [MAVEN_LAYOUTS_SINCE] whatever
    /// release the initializr is on.
    static GeneratorModel createForPluginVersion(IDE ide, Template template, String appName, String packageName,
                                                 ProjectOptions options, String pluginVersion) {
        return new GeneratorModel(ide, template, appName, packageName, options, pluginVersion);
    }

    /// Whether this Maven download gets the layouts of [MAVEN_LAYOUTS_SINCE].
    /// `content` without the lines that contain `marker`.
    static String removeLinesContaining(String content, String marker) {
        StringBuilder out = new StringBuilder(content.length());
        int start = 0;
        while (start < content.length()) {
            int end = content.indexOf('\n', start);
            int next = end < 0 ? content.length() : end + 1;
            String line = content.substring(start, next);
            if (line.indexOf(marker) < 0) {
                out.append(line);
            }
            start = next;
        }
        return out.toString();
    }

    private boolean mavenLayoutsEnabled() {
        return !options.isGradle() && isVersionAtLeast(pluginVersion, MAVEN_LAYOUTS_SINCE);
    }

    /// A Maven backend-only project: the server alone, at the root.
    private boolean isMavenBackendOnly() {
        return mavenLayoutsEnabled() && options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY;
    }

    /// Whether the Maven app carries its platform modules (javase/, android/, ...).
    private boolean includesPlatformModules() {
        return !mavenLayoutsEnabled() || options.allPlatformModules;
    }

    /// Whether the Maven app carries the backend/ module.
    private boolean includesBackendModule() {
        return !mavenLayoutsEnabled() || options.projectType == ProjectOptions.ProjectType.APP_WITH_BACKEND;
    }

    public static GeneratorModel create(IDE ide, Template template, String appName, String packageName) {
        return new GeneratorModel(ide, template, appName, packageName, ProjectOptions.defaults());
    }

    public static GeneratorModel create(IDE ide, Template template, String appName, String packageName, ProjectOptions options) {
        return new GeneratorModel(ide, template, appName, packageName, options);
    }

    /// Builds the project zip and hands it to the browser (or, off the web, to the
    /// platform). Returns true once the download was handed over, which is when
    /// the Initializr shows its next-steps panel; false after an error, which has
    /// already been reported to the user.
    public boolean generate() {
        cleanupGeneratedZips();
        String fileName = toLowerCaseInvariant(appName) + ".zip";

        // Collect the project's entries (read source/template/cn1lib bytes).
        Map<String, byte[]> entries;
        try {
            entries = collectProjectEntries();
        } catch (IOException ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Couldn't build the project: " + describeError(ex));
            return false;
        }

        // Build the project zip in memory. This runs on every platform,
        // including the JavaScript port: the in-Java zip is fast there now that
        // the translator no longer wraps synchronous natives (arraycopy/CRC) in
        // cooperative generators and resolves inherited interface static fields
        // correctly. Then hand the bytes to the platform downloader; falls
        // through to the storage + execute() path when unsupported.
        byte[] bytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            writeEntriesToZip(bos, entries);
            bytes = bos.toByteArray();
        } catch (IOException ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Couldn't build the project: " + describeError(ex));
            return false;
        }
        if (downloadWebsiteProject(fileName, bytes, packageName, templateId())) {
            return true;
        }
        if (downloadBytesAsFile(fileName, bytes)) {
            return true;
        }

        // Fallback (non-JS platforms): write the bytes to storage and hand the
        // file path to execute(). The IndexedDB entry sticks around, so a prior
        // cleanupGeneratedZips() keeps generations from accumulating multi-MB
        // records. Retry once after a fresh cleanup before giving up.
        String filePath = getAppHomePath() + fileName;
        try {
            writeBytesToStorage(filePath, bytes);
        } catch (IOException firstErr) {
            cleanupGeneratedZips();
            try {
                writeBytesToStorage(filePath, bytes);
            } catch (IOException retryErr) {
                Log.e(retryErr);
                ToastBar.showErrorMessage(
                        "Couldn't generate the project: " + describeError(retryErr)
                                + ". If your browser storage is full, clear site data for "
                                + "this page and try again.");
                return false;
            }
        }
        execute(filePath);
        return true;
    }

    /** Use the website bridge so the metric is emitted only after its download handler succeeds. */
    private static boolean downloadWebsiteProject(String fileName, byte[] bytes,
                                                  String packageName, String template) {
        WebsiteThemeNative nativeBridge = NativeLookup.create(WebsiteThemeNative.class);
        if (nativeBridge == null || !nativeBridge.isSupported()) {
            return false;
        }
        try {
            String dataUrl = "data:application/octet-stream;base64,"
                    + com.codename1.util.Base64.encodeNoNewline(bytes);
            return nativeBridge.downloadProject(fileName, dataUrl,
                    packageName == null ? "" : packageName, template);
        } catch (Throwable t) {
            return false;
        }
    }

    /// The template identifier the website download beacon reports, e.g.
    /// "barebones" or "kotlin": the enum constant, lower-cased.
    public String templateId() {
        return template == null ? "" : toLowerCaseInvariant(template.name());
    }

    /// The IDE identifier the "email me these steps" request reports, e.g.
    /// "intellij" or "vs_code": the enum constant, lower-cased.
    /// The IDE as BuildCloud's "email me these steps" endpoint names it. It
    /// accepts a closed set (intellij, netbeans, eclipse, vscode) and treats
    /// anything else as "no IDE", so VS_CODE must not become "vs_code".
    public String ideId() {
        if (ide == null) {
            return "";
        }
        return ide == IDE.VS_CODE ? "vscode" : toLowerCaseInvariant(ide.name());
    }

    public String getPackageName() {
        return packageName;
    }

    // ---------- launcher build-progress reporting ----------

    /// Where the generated launchers report a build's start and end. The
    /// launchers let CN1_EVENTS_URL override it (for tests); the README names it.
    static final String LAUNCHER_EVENTS_URL = "https://cloud.codenameone.com/api/v2/funnel/initializr-event";
    // The reporting code the launchers gain. Kept as resources rather than Java
    // strings so the bash and cmd read as what they are; flat at the root of
    // src/main/resources because the Codename One classloader rejects nested
    // resource directories.
    private static final String LAUNCHER_TELEMETRY_SH = "/launcher-telemetry-sh.txt";
    private static final String LAUNCHER_TELEMETRY_BAT_START = "/launcher-telemetry-bat-start.txt";
    private static final String LAUNCHER_TELEMETRY_BAT_FINISH = "/launcher-telemetry-bat-finish.txt";
    // The archetype launcher lines the reporting hooks onto. common.zip's
    // build.sh/build.bat are the archetype's (scripts/sync-initializr-launchers.py
    // holds them byte-identical), so a change there that moves these lines turns
    // the reporting off rather than shipping a broken launcher; the matrix test
    // fails on that, so it does not go unnoticed.
    private static final String SH_MVNW_LINE = "MVNW=\"./mvnw\"\n";
    private static final String BAT_GOTO_LINE = "\ngoto %CMD%\n";
    private static final String BAT_FINISH_BLOCK = ":finish\nset \"CN1_EXIT_CODE=%errorlevel%\"\npopd\n"
            + "exit /b %CN1_EXIT_CODE%\n";

    /// The anonymous id the launchers report: the lower-case hex SHA-256 of the
    /// lower-cased package name. The website's download beacon sends the same
    /// hash and BuildCloud computes it for every cloud build, which is how a
    /// download, its launcher runs and its first cloud build are joined without
    /// the package name leaving the machine.
    String projectId() {
        return Sha256.hex(toLowerCaseInvariant(packageName == null ? "" : packageName));
    }

    /// Initializr downloads -- and only those; the archetype's launchers are not
    /// touched -- get build.sh/build.bat that report each build's start and how it
    /// ended (see launcher-telemetry-sh.txt for exactly what is sent and the
    /// opt-out). Without it the funnel goes dark between "downloaded a project"
    /// and "first cloud build", which is exactly where new developers stall.
    private void addLauncherTelemetry(Map<String, byte[]> entries) throws IOException {
        byte[] sh = entries.get("build.sh");
        if (sh != null) {
            entries.put("build.sh", withShellTelemetry(StringUtil.newString(sh)).getBytes("UTF-8"));
        }
        byte[] bat = entries.get("build.bat");
        if (bat != null) {
            entries.put("build.bat", withBatchTelemetry(StringUtil.newString(bat)).getBytes("UTF-8"));
        }
    }

    /// build.sh with every Maven run routed through cn1_mvnw, which runs ./mvnw
    /// unchanged and reports around it. Every target calls "$MVNW", so swapping
    /// that one variable covers them all.
    String withShellTelemetry(String script) throws IOException {
        int at = script.indexOf(SH_MVNW_LINE);
        if (at < 0) {
            return script;
        }
        return script.substring(0, at) + "MVNW=\"cn1_mvnw\"\n"
                + fillTelemetryTokens(readResourceToString(LAUNCHER_TELEMETRY_SH))
                + script.substring(at + SH_MVNW_LINE.length());
    }

    /// build.bat with the launch report just before it jumps to the target, and
    /// the exit report in its :finish block, which every target falls through to.
    String withBatchTelemetry(String script) throws IOException {
        int jump = script.indexOf(BAT_GOTO_LINE);
        int finish = script.lastIndexOf(BAT_FINISH_BLOCK);
        if (jump < 0 || finish < jump || finish + BAT_FINISH_BLOCK.length() != script.length()) {
            return script;
        }
        return script.substring(0, jump + 1)
                + fillTelemetryTokens(readResourceToString(LAUNCHER_TELEMETRY_BAT_START))
                + script.substring(jump + 1, finish)
                + fillTelemetryTokens(readResourceToString(LAUNCHER_TELEMETRY_BAT_FINISH));
    }

    private String fillTelemetryTokens(String text) {
        text = StringUtil.replaceAll(text, "__CN1_PROJECT_ID__", projectId());
        // The oldest JDK this project builds with: a Java 8 project still builds
        // on JDK 8, so only an older one is "too old" for it.
        return StringUtil.replaceAll(text, "__CN1_MIN_JAVA__",
                options.javaVersion == ProjectOptions.JavaVersion.JAVA_17 ? "17" : "8");
    }

    // ---------- next steps shown after the download ----------

    /// What the Initializr's post-download panel tells the developer to do next,
    /// worded per IDE and build tool. Kept here, beside the README that says the
    /// same thing at more length, so the two give the same commands.
    public static final class NextSteps {
        /// Short numbered steps: extract, open in the IDE, run the first build.
        public final String[] steps;
        /// The commands for the last step on macOS/Linux and on Windows, one per line.
        public final String unixCommands;
        public final String windowsCommands;
        /// Whether that first build is a cloud build, which asks for an account.
        /// False for a backend-only project, which has no client to build.
        public final boolean cloudBuild;

        NextSteps(String[] steps, String unixCommands, String windowsCommands, boolean cloudBuild) {
            this.steps = steps;
            this.unixCommands = unixCommands;
            this.windowsCommands = windowsCommands;
            this.cloudBuild = cloudBuild;
        }
    }

    /// Which of nextSteps()' four sets of steps this project gets, as BuildCloud's
    /// "email me these steps" endpoint names them: maven, gradle, maven-backend or
    /// gradle-backend. Sent with the request so the email carries the same
    /// commands -- and the same account line, absent for a backend-only project
    /// -- as the panel; BuildCloud picks from a fixed set, never from text sent here.
    public String buildKind() {
        boolean gradle = options.isGradle();
        boolean backendOnly = gradle ? options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY
                : isMavenBackendOnly();
        return (gradle ? "gradle" : "maven") + (backendOnly ? "-backend" : "");
    }

    public NextSteps nextSteps() {
        boolean gradle = options.isGradle();
        boolean backendOnly = gradle ? options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY
                : isMavenBackendOnly();
        String extract = "Extract the whole " + toLowerCaseInvariant(appName) + ".zip into a folder.";
        String open = openInIdeStep(gradle);
        if (backendOnly) {
            return new NextSteps(new String[] {extract, open, "Start the server from a terminal in that folder:"},
                    gradle ? "./gradlew runBackend" : "./mvnw cn1:backend",
                    gradle ? ".\\gradlew.bat runBackend" : ".\\mvnw.cmd cn1:backend", false);
        }
        if (gradle) {
            return new NextSteps(new String[] {extract, open,
                    "Run your first cloud build from a terminal in that folder:"},
                    "./gradlew buildJavascript", ".\\gradlew.bat buildJavascript", true);
        }
        // The same commands as the generated README's Getting Started section.
        return new NextSteps(new String[] {extract, open,
                "From a terminal in that folder, check Maven sees your JDK, then run your first cloud build:"},
                "./mvnw -v\n./build.sh javascript_cloud", ".\\mvnw.cmd -v\n.\\build.bat javascript_cloud", true);
    }

    private String openInIdeStep(boolean gradle) {
        String kind = gradle ? "Gradle" : "Maven";
        if (ide == IDE.INTELLIJ) {
            return "In IntelliJ IDEA, choose File > Open and pick the folder; it imports the " + kind + " build.";
        }
        if (ide == IDE.ECLIPSE) {
            return "In Eclipse, choose File > Import > " + kind + " > Existing " + kind
                    + (gradle ? " Project" : " Projects") + " and pick the folder.";
        }
        if (ide == IDE.NETBEANS) {
            return "In NetBeans, choose File > Open Project and pick the folder.";
        }
        return "In VS Code, choose File > Open Folder and install the Java and " + kind
                + " extensions it suggests.";
    }

    private static String describeError(Throwable ex) {
        String detail = ex.getMessage();
        return (detail == null || detail.length() == 0) ? ex.getClass().getName() : detail;
    }

    private void writeBytesToStorage(String filePath, byte[] bytes) throws IOException {
        try (OutputStream fos = openFileOutputStream(filePath)) {
            fos.write(bytes);
        }
    }

    public static void cleanupGeneratedZips() {
        try {
            String home = getAppHomePath();
            String[] files = listFiles(home);
            if (files == null) {
                return;
            }
            for (String file : files) {
                if (file != null && file.endsWith(".zip")) {
                    try {
                        delete(home + file);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    void writeProjectZip(OutputStream outputStream) throws IOException {
        writeEntriesToZip(outputStream, collectProjectEntries());
    }

    /// Reads every entry (IDE scaffold, common files, template sources, cn1libs,
    /// localization, generated README/.gitignore/skills) into an ordered map of
    /// path -> bytes. This is the I/O phase, kept separate from the zip assembly.
    Map<String, byte[]> collectProjectEntries() throws IOException {
        validateOptions();
        if (options.isGradle()) {
            return collectGradleProjectEntries();
        }
        if (isMavenBackendOnly()) {
            return collectMavenBackendOnlyEntries();
        }
        Map<String, byte[]> mergedEntries = new LinkedHashMap<String, byte[]>();

        copyZipEntriesToMap(ide.ZIP, mergedEntries, ZipEntryType.IDE);
        copyZipEntriesToMap("/common.zip", mergedEntries, ZipEntryType.COMMON_ARCHIVE);
        copySingleTextEntryToMap(".gitignore", GENERATED_GITIGNORE, mergedEntries, ZipEntryType.COMMON);
        copySingleTextEntryToMap("README.md", buildReadmeMarkdown(), mergedEntries, ZipEntryType.COMMON);
        if (options.javaVersion == ProjectOptions.JavaVersion.JAVA_17) {
            addAgentSkillEntries(mergedEntries);
        }
        copySingleTextEntryToMap("common/pom.xml", readResourceToString(template.POM_XML), mergedEntries, ZipEntryType.TEMPLATE_POM);
        if (template.CN1LIB_ZIP != null) {
            copyZipEntriesToMap(template.CN1LIB_ZIP, mergedEntries, ZipEntryType.TEMPLATE_CN1LIB);
        }
        copyZipEntriesToMap(template.CSS, mergedEntries, ZipEntryType.TEMPLATE_CSS);
        copyZipEntriesToMap(template.SOURCE_ZIP, mergedEntries, ZipEntryType.TEMPLATE_SOURCE);
        addLocalizationEntries(mergedEntries);
        addLauncherTelemetry(mergedEntries);
        validateGeneratedPomCoordinates(mergedEntries);
        return mergedEntries;
    }

    /// The Gradle form of [collectProjectEntries()]: one project at the root, laid
    /// out the way the `com.codenameone` Gradle plugin reads it. The build files
    /// come from the shared template in gradle.zip; the application's files are the
    /// same common.zip and template entries a Maven download gets, moved out of
    /// `common/`.
    private Map<String, byte[]> collectGradleProjectEntries() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        Map<String, byte[]> scaffold = readZipResource(GRADLE_TEMPLATE_ZIP);
        boolean backendOnly = options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY;

        addGradleIdeEntries(entries);
        putGradleText(entries, "settings.gradle.kts", gradleTemplate(scaffold, "settings.gradle.kts.txt", ""));
        putGradleText(entries, "gradle.properties", gradleTemplate(scaffold, "gradle.properties.txt", ""));
        putGradleText(entries, ".gitignore", gradleTemplate(scaffold, "gitignore.txt", ""));
        // The wrapper (gradlew, gradlew.bat, gradle/wrapper/*) is every template file
        // that is not a .txt template, copied byte for byte.
        for (Map.Entry<String, byte[]> e : scaffold.entrySet()) {
            if (!e.getKey().endsWith(".txt")) {
                entries.put(e.getKey(), e.getValue());
            }
        }

        if (backendOnly) {
            // No client: the backend is the root project. No settings file, no CSS and
            // no icon -- the plugin tells a backend from an app by the absence of
            // codenameone_settings.properties and the presence of application.properties.
            addGradleBackendEntries(entries, scaffold, "", "");
        } else {
            putGradleText(entries, "build.gradle.kts",
                    gradleAppBuildScript(gradleTemplate(scaffold, "app/build.gradle.kts.txt", "")));
            copyZipEntriesToMap("/common.zip", entries, ZipEntryType.COMMON_ARCHIVE);
            copyZipEntriesToMap(template.CSS, entries, ZipEntryType.TEMPLATE_CSS);
            copyZipEntriesToMap(template.SOURCE_ZIP, entries, ZipEntryType.TEMPLATE_SOURCE);
            addLocalizationEntries(entries);
            if (options.projectType == ProjectOptions.ProjectType.APP_WITH_BACKEND) {
                // What `./gradlew addBackend` writes, plus the backend's own build script
                // so there is an obvious place for its dependencies.
                addGradleBackendEntries(entries, scaffold, "backend/", ":backend:");
            }
        }
        copySingleTextEntryToMap("README.md", buildGradleReadmeMarkdown(), entries, ZipEntryType.COMMON);
        if (!backendOnly) {
            // The skill teaches Codename One UI authoring; a project with no client
            // would only be told to run a simulator it does not have.
            addAgentSkillEntries(entries);
        }
        validateGradleProject(entries);
        return entries;
    }

    /// A Maven backend-only project: one module, the server, at the root. The pom is
    /// backend-only-pom.xml; the server's files are the same templates the Gradle
    /// generators use (gradle.zip), with Maven's commands; the wrapper comes from
    /// common.zip. No launchers and no agent skill -- both are about the app.
    private Map<String, byte[]> collectMavenBackendOnlyEntries() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        Map<String, byte[]> scaffold = readZipResource(GRADLE_TEMPLATE_ZIP);
        addMavenBackendIdeEntries(entries);
        copyZipEntriesToMap("/common.zip", entries, ZipEntryType.COMMON_ARCHIVE);
        copySingleTextEntryToMap(".gitignore", GENERATED_GITIGNORE, entries, ZipEntryType.COMMON);
        copySingleTextEntryToMap("pom.xml", readResourceToString(BACKEND_ONLY_POM_RESOURCE), entries,
                ZipEntryType.COMMON);
        copySingleTextEntryToMap("application.properties",
                mavenBackendTemplate(scaffold, "backend/application.properties.txt"), entries, ZipEntryType.COMMON);
        copySingleTextEntryToMap("application-dev.properties",
                mavenBackendTemplate(scaffold, "backend/application-dev.properties.txt"), entries,
                ZipEntryType.COMMON);
        String sourceDir = "src/main/java/" + packageName.replace('.', '/') + "/";
        copySingleTextEntryToMap(sourceDir + "Api.java", mavenBackendTemplate(scaffold, "backend/Api.java.txt"),
                entries, ZipEntryType.COMMON);
        copySingleTextEntryToMap(sourceDir + "Greeter.java",
                mavenBackendTemplate(scaffold, "backend/Greeter.java.txt"), entries, ZipEntryType.COMMON);
        // The same @BackendTest samples every other backend layout gets.
        String testDir = "src/test/java/" + packageName.replace('.', '/') + "/";
        copySingleTextEntryToMap(testDir + "ApiTest.java",
                mavenBackendTemplate(scaffold, "backend/ApiTest.java.txt"), entries, ZipEntryType.COMMON);
        copySingleTextEntryToMap(testDir + "ServedApiTest.java",
                mavenBackendTemplate(scaffold, "backend/ServedApiTest.java.txt"), entries, ZipEntryType.COMMON);
        copySingleTextEntryToMap("README.md", buildMavenBackendReadmeMarkdown(), entries, ZipEntryType.COMMON);
        validateGeneratedPomCoordinates(entries);
        return entries;
    }

    /// A backend template from gradle.zip with the Gradle commands it names put the
    /// way Maven spells them, and its package filled in.
    private String mavenBackendTemplate(Map<String, byte[]> scaffold, String name) throws IOException {
        byte[] data = scaffold.get(name);
        if (data == null) {
            throw new IOException("Missing backend template " + name);
        }
        String text = StringUtil.newString(data);
        text = StringUtil.replaceAll(text, "./gradlew __BACKEND__runBackend", "./mvnw cn1:backend");
        text = StringUtil.replaceAll(text, "./gradlew __BACKEND__backendPackage", "./mvnw cn1:backend-package");
        text = StringUtil.replaceAll(text, "./gradlew __BACKEND__backendTest",
                "./mvnw test -Dcn1.backend.compiledTests=true");
        text = StringUtil.replaceAll(text, "./gradlew __BACKEND__test", "./mvnw test");
        text = StringUtil.replaceAll(text, "under `runBackend`", "under `cn1:backend`");
        text = StringUtil.replaceAll(text, "${package}", packageName);
        return text;
    }

    private void addGradleBackendEntries(Map<String, byte[]> entries, Map<String, byte[]> scaffold, String dir,
                                         String taskPrefix) throws IOException {
        putGradleText(entries, dir + "build.gradle.kts",
                gradleTemplate(scaffold, "backend/build.gradle.kts.txt", taskPrefix));
        putGradleText(entries, dir + "application.properties",
                gradleTemplate(scaffold, "backend/application.properties.txt", taskPrefix));
        putGradleText(entries, dir + "application-dev.properties",
                gradleTemplate(scaffold, "backend/application-dev.properties.txt", taskPrefix));
        putGradleText(entries, dir + "src/main/java/" + packageName.replace('.', '/') + "/Api.java",
                gradleTemplate(scaffold, "backend/Api.java.txt", taskPrefix));
        putGradleText(entries, dir + "src/main/java/" + packageName.replace('.', '/') + "/Greeter.java",
                gradleTemplate(scaffold, "backend/Greeter.java.txt", taskPrefix));
        putGradleText(entries, dir + "src/test/java/" + packageName.replace('.', '/') + "/ApiTest.java",
                gradleTemplate(scaffold, "backend/ApiTest.java.txt", taskPrefix));
        putGradleText(entries, dir + "src/test/java/" + packageName.replace('.', '/') + "/ServedApiTest.java",
                gradleTemplate(scaffold, "backend/ServedApiTest.java.txt", taskPrefix));
    }

    private void putGradleText(Map<String, byte[]> entries, String path, String content) throws IOException {
        copySingleTextEntryToMap(path, content, entries, ZipEntryType.COMMON);
    }

    /// A template from gradle.zip with its tokens filled in, exactly as
    /// GradleProjectTemplate fills them for the Maven and Gradle plugins.
    ///
    /// @param taskPrefix how the backend's tasks are addressed from the root:
    ///        `:backend:` for a subproject, empty for a backend-only project
    private String gradleTemplate(Map<String, byte[]> scaffold, String name, String taskPrefix) throws IOException {
        byte[] data = scaffold.get(name);
        if (data == null) {
            throw new IOException("Missing Gradle project template " + name);
        }
        String text = StringUtil.newString(data);
        text = StringUtil.replaceAll(text, "__CN1_VERSION__", pluginVersion);
        // The same name the Maven reactor gets as its artifactId (cn1app.name).
        text = StringUtil.replaceAll(text, "__PROJECT_NAME__", toLowerCaseInvariant(appName));
        text = StringUtil.replaceAll(text, "__BACKEND__", taskPrefix);
        text = StringUtil.replaceAll(text, "${package}", packageName);
        return text;
    }

    /// The application's build.gradle.kts: the template, plus what the chosen
    /// template's pom adds -- the Kotlin plugin for a Kotlin project, and the
    /// template's dependencies (cn1libs by coordinates, libraries).
    String gradleAppBuildScript(String script) {
        String[] dependencies = template.GRADLE_DEPENDENCIES;
        if (dependencies.length > 0) {
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < dependencies.length; i++) {
                lines.append("    ").append(dependencies[i]).append('\n');
            }
            script = insertAfter(script, "dependencies {\n", lines.toString() + "\n");
        }
        if (template.IS_KOTLIN) {
            // plugins {} has to be the first statement of a Kotlin build script, so it
            // goes after the leading comment and before dependencies {}.
            String plugins = "plugins {\n"
                    + "    kotlin(\"jvm\") version \"" + KOTLIN_VERSION + "\"\n"
                    + "}\n\n";
            int deps = script.indexOf("dependencies {");
            script = deps < 0 ? plugins + script : script.substring(0, deps) + plugins + script.substring(deps);
        }
        return script;
    }

    private static String insertAfter(String text, String marker, String insertion) {
        int pos = text.indexOf(marker);
        if (pos < 0) {
            return text + "\ndependencies {\n" + insertion + "}\n";
        }
        int at = pos + marker.length();
        return text.substring(0, at) + insertion + text.substring(at);
    }

    /// The runtime guard for a Gradle download, in the spirit of
    /// [validateGeneratedPomCoordinates(Map)]: gradle.zip is assembled from a
    /// directory other generators share, so a token the initializr does not know
    /// about, or a Maven file that slipped through the mapping, fails closed here
    /// instead of reaching users.
    void validateGradleProject(Map<String, byte[]> entries) throws IOException {
        String settings = entries.get("settings.gradle.kts") == null ? null
                : StringUtil.newString(entries.get("settings.gradle.kts"));
        if (settings == null || settings.indexOf("id(\"com.codenameone\") version \"" + pluginVersion + "\"") < 0
                || settings.indexOf("rootProject.name = \"" + toLowerCaseInvariant(appName) + "\"") < 0) {
            throw new IOException("Refusing to generate project: settings.gradle.kts does not apply the "
                    + "Codename One plugin " + pluginVersion + " to " + toLowerCaseInvariant(appName));
        }
        if (entries.get("build.gradle.kts") == null || entries.get("gradlew") == null) {
            throw new IOException("Refusing to generate project: the Gradle build script or wrapper is missing");
        }
        String[] tokens = {"__CN1_VERSION__", "__PROJECT_NAME__", "__BACKEND__", "${package}"};
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String path = e.getKey();
            if (path.startsWith("common/") || path.endsWith("pom.xml") || path.startsWith(".mvn/")
                    || path.startsWith("mvnw")) {
                throw new IOException("Refusing to generate project: Maven file " + path + " in a Gradle project");
            }
            if (path.endsWith(".kts") || path.endsWith(".properties") || path.endsWith(".java")
                    || path.endsWith(".md") || path.endsWith(".xml") || path.endsWith(".json")) {
                String text = StringUtil.newString(e.getValue());
                for (int i = 0; i < tokens.length; i++) {
                    if (text.indexOf(tokens[i]) >= 0) {
                        throw new IOException("Refusing to generate project: " + path
                                + " still contains the template token " + tokens[i]);
                    }
                }
            }
        }
    }

    private Map<String, byte[]> readZipResource(String zipResource) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        InputStream in = getResourceAsStream(zipResource);
        if (in == null) {
            throw new IOException("Missing resource " + zipResource);
        }
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    // Defensive normalization: ant may produce backslashes on Windows.
                    out.put(StringUtil.replaceAll(entry.getName(), "\\", "/"), readToBytesNoClose(zis));
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
        return out;
    }

    /// Refuses a combination of options that has no project to generate. The UI never
    /// offers these; the check is here so that no caller (tests, fixtures, a future UI)
    /// can produce a download that fails on its first build.
    void validateOptions() throws IOException {
        if (options.isGradle()) {
            if (options.javaVersion != ProjectOptions.JavaVersion.JAVA_17) {
                throw new IOException("Gradle projects target Java 17. Choose Java 17, or the Maven build "
                        + "for a project that must target Java 8.");
            }
            if (options.projectType != ProjectOptions.ProjectType.BACKEND_ONLY && !template.supportsGradle()) {
                throw new IOException(template.GRADLE_UNSUPPORTED_REASON + " Choose the Maven build for this template.");
            }
            return;
        }
        if (options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY) {
            if (!mavenLayoutsEnabled()) {
                throw new IOException("A backend-only Maven project needs Codename One " + MAVEN_LAYOUTS_SINCE
                        + " or newer; generate it for Gradle. Every Maven project already carries the backend "
                        + "module; build it with -Dcodename1.platform=backend.");
            }
            if (options.allPlatformModules) {
                throw new IOException("A backend-only project has no platform modules; leave "
                        + "\"Include all platform modules\" off.");
            }
        }
    }

    /// Refuses to publish a generated download when one of the embedded module POMs
    /// still points at the Initializr application itself, or otherwise drifts from
    /// the generated root project's Maven coordinates. This is intentionally a
    /// runtime guard in addition to the tests: common.zip is a committed binary
    /// artifact, so a bad manual rebuild must fail closed instead of reaching users.
    void validateGeneratedPomCoordinates(Map<String, byte[]> entries) throws IOException {
        String rootArtifactId = toLowerCaseInvariant(appName);
        String version = "1.0-SNAPSHOT";

        String rootPom = normalizedPom(entries, "pom.xml");
        requirePomFragment(
                "pom.xml",
                rootPom,
                "</modelVersion><groupId>" + packageName + "</groupId>"
                        + "<artifactId>" + rootArtifactId + "</artifactId>"
                        + "<version>" + version + "</version>",
                "root project coordinates " + packageName + ":" + rootArtifactId + ":" + version
        );
        requirePomFragment(
                "pom.xml",
                rootPom,
                "<cn1app.name>" + rootArtifactId + "</cn1app.name>",
                "cn1app.name " + rootArtifactId
        );

        if (isMavenBackendOnly()) {
            validateBackendOnlyPom(entries, rootPom);
            return;
        }

        validateModulePomCoordinates(entries, "common", rootArtifactId + "-common", false, version);

        for (int i = 0; i < PLATFORM_MODULES.length; i++) {
            String platform = PLATFORM_MODULES[i];
            if (includesPlatformModules()) {
                validateModulePomCoordinates(entries, platform, rootArtifactId + "-" + platform, true, version);
            } else {
                requireAbsentModule(entries, rootPom, platform);
            }
        }
        // The backend module is checked for coordinates like the rest, but NOT for a
        // dependency on the generated common module. It must not have one: common is
        // compiled against codenameone-core, and a server has no display. Requiring it
        // here would enforce exactly the mistake the module's own comment warns against.
        if (includesBackendModule()) {
            validateModulePomCoordinates(entries, "backend", rootArtifactId + "-backend", false, version);
        } else {
            requireAbsentModule(entries, rootPom, "backend");
        }

        if (mavenLayoutsEnabled()) {
            String commonPom = normalizedPom(entries, "common/pom.xml");
            requirePomFragment("common/pom.xml", commonPom, "<goal>compile-javase-natives</goal>",
                    "hosted platform profiles");
            if (commonPom.indexOf("<id>simulator</id>") != commonPom.lastIndexOf("<id>simulator</id>")) {
                throw new IOException("Refusing to generate project: common/pom.xml declares the simulator "
                        + "profile twice");
            }
            if (!includesPlatformModules() && rootPom.indexOf("<activeByDefault>") >= 0) {
                throw new IOException("Refusing to generate project: pom.xml activates a javase module "
                        + "the project does not have");
            }
        }
    }

    /// An optional module that was left out: no files of it in the download, and
    /// the root pom adds it only once its pom exists.
    private void requireAbsentModule(Map<String, byte[]> entries, String rootPom, String module) throws IOException {
        for (String path : entries.keySet()) {
            if (path.startsWith(module + "/")) {
                throw new IOException("Refusing to generate project: " + path + " belongs to the " + module
                        + " module this project does not have");
            }
        }
        if (rootPom.indexOf("<module>" + module + "</module>") >= 0) {
            requirePomFragment("pom.xml", rootPom, "<exists>${basedir}/" + module + "/pom.xml</exists>",
                    "guard that adds the " + module + " module only when it exists");
        }
    }

    /// A backend-only project is one module: no parent, the backend runtime, the
    /// annotation processing that generates its entry point, and nothing of an app.
    private void validateBackendOnlyPom(Map<String, byte[]> entries, String rootPom) throws IOException {
        if (rootPom.indexOf("<parent>") >= 0) {
            throw new IOException("Refusing to generate project: a backend-only pom.xml has a parent");
        }
        requirePomFragment("pom.xml", rootPom, "<artifactId>codenameone-backend</artifactId>",
                "dependency on codenameone-backend");
        requirePomFragment("pom.xml", rootPom, "<goal>process-annotations</goal>", "process-annotations goal");
        if (rootPom.indexOf("-common</artifactId>") >= 0 || rootPom.indexOf("<artifactId>codenameone-core</artifactId>") >= 0) {
            throw new IOException("Refusing to generate project: a backend-only pom.xml depends on the app");
        }
        for (String path : entries.keySet()) {
            if (path.startsWith("common/") || (path.endsWith("/pom.xml") && !"pom.xml".equals(path))) {
                throw new IOException("Refusing to generate project: " + path + " in a backend-only project");
            }
        }
    }

    private void validateModulePomCoordinates(
            Map<String, byte[]> entries,
            String module,
            String moduleArtifactId,
            boolean requireCommonDependency,
            String version
    ) throws IOException {
        String path = module + "/pom.xml";
        String pom = normalizedPom(entries, path);
        String expectedCoordinates =
                "</modelVersion><parent>"
                        + "<groupId>" + packageName + "</groupId>"
                        + "<artifactId>" + toLowerCaseInvariant(appName) + "</artifactId>"
                        + "<version>" + version + "</version>"
                        + "</parent>"
                        + "<groupId>" + packageName + "</groupId>"
                        + "<artifactId>" + moduleArtifactId + "</artifactId>"
                        + "<version>" + version + "</version>";
        requirePomFragment(path, pom, expectedCoordinates,
                "module coordinates " + packageName + ":" + moduleArtifactId + ":" + version);

        if (requireCommonDependency) {
            requirePomFragment(
                    path,
                    pom,
                    "<dependency><groupId>${project.groupId}</groupId>"
                            + "<artifactId>${cn1app.name}-common</artifactId>"
                            + "<version>${project.version}</version>",
                    "dependency on the generated common module"
            );
        }
    }

    private String normalizedPom(Map<String, byte[]> entries, String path) throws IOException {
        byte[] data = entries.get(path);
        if (data == null) {
            throw new IOException("Refusing to generate project: missing " + path);
        }
        String content = StringUtil.newString(data);
        StringBuilder out = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!Character.isWhitespace(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void requirePomFragment(String path, String pom, String expected, String description) throws IOException {
        if (pom.indexOf(expected) < 0) {
            throw new IOException("Refusing to generate project: " + path
                    + " does not declare the expected " + description);
        }
    }

    static String toLowerCaseInvariant(String value) {
        // The CN1 runtime does not expose String.toLowerCase(Locale), so apply
        // the locale-independent Character mapping one code unit at a time.
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            out.append(Character.toLowerCase(value.charAt(i)));
        }
        return out.toString();
    }

    /// Writes the collected entries as a STORED (uncompressed) zip. STORED rather
    /// than DEFLATED keeps the byte work minimal (CRC + copy, no compression
    /// engine); the size cost is irrelevant for a one-off scaffold download.
    /// Used on every platform including the JavaScript port, where it is fast now
    /// that the translator no longer wraps synchronous natives in generators and
    /// resolves inherited interface static fields correctly.
    void writeEntriesToZip(OutputStream outputStream, Map<String, byte[]> mergedEntries) throws IOException {
        try (OutputStream target = outputStream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(buffer)) {
                zos.setMethod(ZipOutputStream.STORED);
                for (Map.Entry<String, byte[]> fileEntry : mergedEntries.entrySet()) {
                    byte[] data = fileEntry.getValue();
                    ZipEntry zipEntry = new ZipEntry(fileEntry.getKey());
                    zipEntry.setMethod(ZipOutputStream.STORED);
                    zipEntry.setSize(data.length);
                    zipEntry.setCompressedSize(data.length);
                    CRC32 crc = new CRC32();
                    crc.update(data);
                    zipEntry.setCrc(crc.getValue());
                    zos.putNextEntry(zipEntry);
                    zos.write(data);
                    zos.closeEntry();
                }
            }
            byte[] archive = buffer.toByteArray();
            ProjectZipPermissions.apply(archive);
            target.write(archive);
        }
    }


    private void addAgentSkillEntries(Map<String, byte[]> mergedEntries) throws IOException {
        // Ship the Codename One authoring skill inside every generated project under a
        // vendor-neutral path so any AI agent (Claude Code, Cursor, others) can pick it up.
        // The skill markdown lives in source form under src/main/resources/skill/** and is
        // repackaged into skill.zip at build time (CN1 classloader rejects nested
        // directories under resources). ASCII-only Markdown so no encoding surprises end
        // up in the project tree.
        try (ZipInputStream zis = new ZipInputStream(getResourceAsStream("/skill.zip"))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    byte[] sourceData = readToBytesNoClose(zis);
                    String relative = entry.getName();
                    // Defensive normalization: ant may produce backslashes on Windows.
                    relative = StringUtil.replaceAll(relative, "\\", "/");
                    String targetPath = AGENT_SKILL_TARGET_PREFIX + relative;
                    byte[] targetData = applyDataReplacements(targetPath, sourceData);
                    mergedEntries.put(targetPath, targetData);
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
        // Top-level AGENTS.md so agents that follow the (emerging) AGENTS.md convention
        // discover the skill without having to know our directory layout.
        copySingleTextEntryToMap("AGENTS.md",
                readResourceToString(options.isGradle() ? AGENTS_MD_GRADLE_RESOURCE : AGENTS_MD_RESOURCE),
                mergedEntries, ZipEntryType.COMMON);
        // Claude Code stub. Frontmatter so the skill shows up in /skills, body redirects
        // to the canonical vendor-neutral content.
        copySingleTextEntryToMap(CLAUDE_SKILL_STUB_PATH, readResourceToString(CLAUDE_SKILL_STUB_RESOURCE),
                mergedEntries, ZipEntryType.COMMON);
    }

    private void addLocalizationEntries(Map<String, byte[]> mergedEntries) throws IOException {
        if (!isBareTemplate() || !options.includeLocalizationBundles) {
            return;
        }
        // The Codename One Maven plugin's CSS compiler scans src/main/l10n (or src/main/i18n)
        // for *.properties bundles and bakes them into theme.res. If the bundles are placed
        // anywhere else (e.g. src/main/resources) they are NOT baked into the resource file
        // and Resources.getGlobalResources().getL10N("messages", lang) returns null at runtime.
        copySingleTextEntryToMap(
                appDir() + "src/main/l10n/messages.properties",
                readResourceToString("/messages.properties"),
                mergedEntries,
                ZipEntryType.COMMON
        );
        for (ProjectOptions.PreviewLanguage language : ProjectOptions.PreviewLanguage.values()) {
            if (language == ProjectOptions.PreviewLanguage.ENGLISH) {
                continue;
            }
            copySingleTextEntryToMap(
                    appDir() + "src/main/l10n/messages_" + language.bundleSuffix + ".properties",
                    readResourceToString("/messages_" + language.bundleSuffix + ".properties"),
                    mergedEntries,
                    ZipEntryType.COMMON
            );
        }
    }

    private void copyZipEntriesToMap(String zipResource, Map<String, byte[]> mergedEntries, ZipEntryType zipType) throws IOException {
        try(ZipInputStream zis = new ZipInputStream(getResourceAsStream(zipResource))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    // The win/ module is the native win32 target, shipped for every Java
                    // version. (The retired UWP module that once lived here used to be
                    // stripped for Java 17 -- that strip is gone now that win/ is win32.)
                    copyEntryToMap(entry.getName(), readToBytesNoClose(zis), mergedEntries, zipType);
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
    }

    private void copyEntryToMap(String sourceName, byte[] sourceData, Map<String, byte[]> mergedEntries, ZipEntryType zipType) throws IOException {
        String targetName = mapTargetPath(sourceName, zipType);
        if (targetName == null) {
            return;
        }
        byte[] targetData = applyDataReplacements(targetName, sourceData);
        mergedEntries.put(targetName, targetData);
    }

    private void copySingleTextEntryToMap(String targetPath, String content, Map<String, byte[]> mergedEntries, ZipEntryType zipType) throws IOException {
        byte[] sourceData = content.getBytes("UTF-8");
        copyEntryToMap(targetPath, sourceData, mergedEntries, zipType);
    }

    /// Where an entry lands in the generated project, or null to leave it out.
    private String mapTargetPath(String sourcePath, ZipEntryType zipType) {
        String targetPath = sourcePath;
        if (zipType == ZipEntryType.COMMON_ARCHIVE && options.isGradle()) {
            // A Gradle project is one project at the root: what common.zip keeps in the
            // Maven common/ module moves up a level, and everything else in it -- the
            // reactor and platform module poms, mvnw, .mvn, build/run launchers, the
            // Maven backend module -- is Maven's and has no Gradle counterpart.
            if (!sourcePath.startsWith("common/") || sourcePath.endsWith("/pom.xml")
                    || "common/pom.xml".equals(sourcePath)) {
                return null;
            }
            targetPath = sourcePath.substring("common/".length());
        } else if (zipType == ZipEntryType.COMMON_ARCHIVE && mavenLayoutsEnabled()) {
            String mapped = mapMavenLayoutPath(sourcePath);
            if (mapped == null) {
                return null;
            }
            targetPath = mapped;
        } else if (zipType == ZipEntryType.TEMPLATE_CSS) {
            targetPath = appDir() + "src/main/css/" + sourcePath;
        } else if (zipType == ZipEntryType.TEMPLATE_SOURCE) {
            if (sourcePath.startsWith("java/")) {
                targetPath = appDir() + "src/main/java/" + sourcePath.substring("java/".length());
            } else if (sourcePath.startsWith("kotlin/")) {
                targetPath = appDir() + "src/main/kotlin/" + sourcePath.substring("kotlin/".length());
            } else if (sourcePath.startsWith("resources/")) {
                targetPath = appDir() + "src/main/resources/" + sourcePath.substring("resources/".length());
            } else if (sourcePath.startsWith("rad/")) {
                targetPath = appDir() + "src/main/rad/" + sourcePath.substring("rad/".length());
            } else {
                targetPath = appDir() + "src/main/" + sourcePath;
            }
        }
        return applyPathReplacements(targetPath);
    }

    /// The platform modules common.zip carries, each optional from [MAVEN_LAYOUTS_SINCE].
    private static final String[] PLATFORM_MODULES = {"android", "ios", "javase", "javascript", "linux", "win"};

    /// Where a common.zip entry goes in a Maven download with the layout choices, or
    /// null to leave it out: a backend-only project keeps the Maven wrapper alone; an
    /// app drops the platform modules unless every one was asked for, and the backend
    /// module unless it was asked for. The packaged desktop app's native theme moves
    /// from the javase module to common, where the desktop goals read it when there
    /// is no javase module.
    private String mapMavenLayoutPath(String sourcePath) {
        if (isMavenBackendOnly()) {
            return "mvnw".equals(sourcePath) || "mvnw.cmd".equals(sourcePath) || sourcePath.startsWith(".mvn/")
                    ? sourcePath : null;
        }
        if (!includesPlatformModules()) {
            if ("javase/src/desktop/resources/NativeTheme.res".equals(sourcePath)) {
                return "common/src/desktop/resources/NativeTheme.res";
            }
            for (int i = 0; i < PLATFORM_MODULES.length; i++) {
                if (sourcePath.startsWith(PLATFORM_MODULES[i] + "/")) {
                    return null;
                }
            }
        }
        if (!includesBackendModule() && sourcePath.startsWith("backend/")) {
            return null;
        }
        return sourcePath;
    }

    /// The modules the root pom adds through a profile of the same id.
    private static final String[] PROFILE_MODULES = {"javascript", "ios", "win", "linux", "backend", "android", "javase"};

    /// The root pom with every module profile also requiring the module's pom, so a
    /// module that is not there is simply not in the reactor and `common` builds
    /// that platform instead. Maven needs both the property and the file. Without a
    /// javase module the javase profile also loses activeByDefault, which Maven
    /// honours even when the profile's own conditions fail.
    static String guardModuleProfiles(String pom, boolean withoutJavase) {
        int profiles = pom.indexOf("<profiles>");
        if (profiles < 0) {
            return pom;
        }
        for (int i = 0; i < PROFILE_MODULES.length; i++) {
            String module = PROFILE_MODULES[i];
            int id = pom.indexOf("<id>" + module + "</id>", profiles);
            if (id < 0) {
                continue;
            }
            int activationEnd = pom.indexOf("</activation>", id);
            int property = pom.indexOf("</property>", id);
            if (activationEnd < 0 || property < 0 || property > activationEnd) {
                continue;
            }
            String guard = "<exists>${basedir}/" + module + "/pom.xml</exists>";
            if (pom.substring(id, activationEnd).indexOf(guard) >= 0) {
                continue;
            }
            int lineStart = pom.lastIndexOf('\n', property) + 1;
            String indent = pom.substring(lineStart, property);
            // common.zip's root pom indents by two.
            String step = "  ";
            int at = property + "</property>".length();
            pom = pom.substring(0, at)
                    + "\n" + indent + "<file>\n" + indent + step + guard + "\n" + indent + "</file>"
                    + pom.substring(at);
        }
        if (withoutJavase) {
            String tag = "<activeByDefault>true</activeByDefault>";
            int pos = pom.indexOf(tag);
            if (pos >= 0) {
                int lineStart = pom.lastIndexOf('\n', pos);
                pom = pom.substring(0, lineStart < 0 ? pos : lineStart) + pom.substring(pos + tag.length());
            }
        }
        return pom;
    }

    /// common/pom.xml with the profiles that let common build any platform the
    /// project has no module for (common-hosted-platform-profiles.xml, the file the
    /// archetype injects too). They replace the template's own `simulator` profile,
    /// which the shared file carries in full.
    private static String injectHostedPlatformProfiles(String pom) throws IOException {
        String fragment = readResourceToString(HOSTED_PROFILES_RESOURCE);
        int id = pom.indexOf("<id>simulator</id>");
        int start = id < 0 ? -1 : pom.lastIndexOf("<profile>", id);
        int end = id < 0 ? -1 : pom.indexOf("</profile>", id);
        if (start < 0 || end < 0) {
            throw new IOException("Refusing to generate project: common/pom.xml has no simulator profile "
                    + "to replace with the hosted platform profiles");
        }
        int lineStart = pom.lastIndexOf('\n', start) + 1;
        int lineEnd = pom.indexOf('\n', end);
        lineEnd = lineEnd < 0 ? pom.length() : lineEnd + 1;
        return pom.substring(0, lineStart) + fragment + pom.substring(lineEnd);
    }

    /// The directory the application's own files live in: the `common/` module of
    /// a Maven project, the root of a Gradle one.
    private String appDir() {
        return options.isGradle() ? "" : "common/";
    }

    private String applyPathReplacements(String path) {
        String packagePath = packageName.replace('.', '/');
        String sourcePackagePath = template.SOURCE_PACKAGE.replace('.', '/');

        String replaced = path;
        replaced = StringUtil.replaceAll(replaced, "com/example/myapp", packagePath);
        replaced = StringUtil.replaceAll(replaced, sourcePackagePath, packagePath);
        replaced = StringUtil.replaceAll(replaced, "MyAppName", appName);
        replaced = StringUtil.replaceAll(replaced, template.SOURCE_MAIN_CLASS, appName);
        replaced = StringUtil.replaceAll(replaced, "myappname", toLowerCaseInvariant(appName));
        return replaced;
    }

    private byte[] applyDataReplacements(String targetPath, byte[] sourceData) throws IOException {
        if (!isTextFile(targetPath)) {
            return sourceData;
        }

        String content = StringUtil.newString(sourceData);
        content = StringUtil.replaceAll(content, "com.example.myapp", packageName);
        content = StringUtil.replaceAll(content, template.SOURCE_PACKAGE, packageName);
        content = StringUtil.replaceAll(content, "MyAppName", appName);
        content = StringUtil.replaceAll(content, template.SOURCE_MAIN_CLASS, appName);
        content = StringUtil.replaceAll(content, "myappname", toLowerCaseInvariant(appName));
        if ((appDir() + "codenameone_settings.properties").equals(targetPath)) {
            content = replaceProperty(content, "codename1.kotlin", String.valueOf(template.IS_KOTLIN));
            content = applyJavaVersionSettings(content);
        }
        if (options.includeLocalizationBundles && isBareTemplate()) {
            content = injectLocalizationBootstrap(targetPath, content);
        }
        if (isBareTemplate() && (appDir() + "src/main/css/theme.css").equals(targetPath)) {
            content = ensureDefaultLargeTextScale(content);
            content += buildThemeCss();
        }
        if ("common/pom.xml".equals(targetPath)) {
            content = applyJavaVersionToPom(content);
        }
        if (".idea/misc.xml".equals(targetPath)) {
            content = normalizeIntellijMiscXml(content);
        }
        if (!includesPlatformModules()
                && (".idea/compiler.xml".equals(targetPath) || ".idea/encodings.xml".equals(targetPath))) {
            // The IntelliJ files name the javase module, which this download does not have.
            content = removeLinesContaining(content, "-javase\"");
            content = removeLinesContaining(content, "$PROJECT_DIR$/javase/");
        }
        if (".idea/workspace.xml".equals(targetPath)) {
            content = applySimulatorJvmExportToIdeaWorkspace(content);
        }
        if ("pom.xml".equals(targetPath)) {
            content = replaceTagValue(content, "cn1.plugin.version", pluginVersion);
            content = replaceTagValue(content, "cn1.version", pluginVersion);
            if (mavenLayoutsEnabled() && !isMavenBackendOnly()) {
                content = guardModuleProfiles(content, !includesPlatformModules());
            }
        }
        if ("common/pom.xml".equals(targetPath) && mavenLayoutsEnabled()) {
            content = injectHostedPlatformProfiles(content);
        }
        if ("android/pom.xml".equals(targetPath) || "ios/pom.xml".equals(targetPath) || "javascript/pom.xml".equals(targetPath)) {
            content = hardenPlatformModulePomAgainstDoubleJarAttach(content);
        }
        if ("javase/pom.xml".equals(targetPath)) {
            content = normalizeJavasePom(content);
        }
        return content.getBytes("UTF-8");
    }



    private String applyJavaVersionSettings(String content) {
        if (options.javaVersion == ProjectOptions.JavaVersion.JAVA_17) {
            content = replaceProperty(content, "codename1.arg.java.version", "17");
        }
        return content;
    }

    private String applyJavaVersionToPom(String content) {
        if (options.javaVersion != ProjectOptions.JavaVersion.JAVA_17) {
            return content;
        }
        content = StringUtil.replaceAll(content, "<source>1.8</source>", "<source>17</source>");
        content = StringUtil.replaceAll(content, "<target>1.8</target>", "<target>17</target>");
        return content;
    }

    private String normalizeIntellijMiscXml(String content) {
        String languageLevel = options.javaVersion == ProjectOptions.JavaVersion.JAVA_17 ? "JDK_17" : "JDK_1_8";
        content = removeXmlAttribute(content, "project-jdk-name");
        content = removeXmlAttribute(content, "project-jdk-type");
        content = setXmlAttribute(content, "languageLevel", languageLevel);
        return content;
    }

    private static String removeXmlAttribute(String xml, String attributeName) {
        String pattern = attributeName + "=\"";
        int pos = xml.indexOf(pattern);
        if (pos < 0) {
            return xml;
        }
        int valueStart = pos + pattern.length();
        int valueEnd = xml.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return xml;
        }
        int removeStart = pos;
        while (removeStart > 0 && xml.charAt(removeStart - 1) == ' ') {
            removeStart--;
        }
        return xml.substring(0, removeStart) + xml.substring(valueEnd + 1);
    }

    private static String setXmlAttribute(String xml, String attributeName, String value) {
        String pattern = attributeName + "=\"";
        int pos = xml.indexOf(pattern);
        if (pos < 0) {
            return xml;
        }
        int valueStart = pos + pattern.length();
        int valueEnd = xml.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return xml;
        }
        return xml.substring(0, valueStart) + value + xml.substring(valueEnd);
    }

    private static String applySimulatorJvmExportToIdeaWorkspace(String content) {
        String configHeader = "<configuration name=\"Run in Simulator\"";
        int start = content.indexOf(configHeader);
        if (start < 0) {
            return content;
        }
        int end = content.indexOf("</configuration>", start);
        if (end < 0) {
            return content;
        }
        String segment = content.substring(start, end);
        String exportArg = "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED";
        if (segment.indexOf(exportArg) >= 0) {
            return content;
        }
        segment = StringUtil.replaceAll(segment, "<option name=\"vmOptions\" value=\"\" />",
                "<option name=\"vmOptions\" value=\"" + exportArg + "\" />");
        return content.substring(0, start) + segment + content.substring(end);
    }

    private static String hardenPlatformModulePomAgainstDoubleJarAttach(String pom) {
        String pluginBlock =
                "<plugin>\n" +
                "                <groupId>org.apache.maven.plugins</groupId>\n" +
                "                <artifactId>maven-jar-plugin</artifactId>\n" +
                "                <version>3.4.1</version>\n" +
                "                <executions>\n" +
                "                    <execution>\n" +
                "                        <id>default-jar</id>\n" +
                "                        <phase>none</phase>\n" +
                "                    </execution>\n" +
                "                </executions>\n" +
                "            </plugin>\n";
        if (pom.indexOf("<artifactId>maven-jar-plugin</artifactId>") >= 0) {
            if (pom.indexOf("<id>default-jar</id>") >= 0 && pom.indexOf("<phase>none</phase>") >= 0) {
                return pom;
            }
            int pluginsTag = pom.indexOf("<plugins>\n");
            if (pluginsTag >= 0) {
                int firstPluginStart = pom.indexOf("<plugin>", pluginsTag);
                if (firstPluginStart >= 0) {
                    return pom.substring(0, firstPluginStart) + pluginBlock + pom.substring(firstPluginStart);
                }
            }
            return pom;
        }
        return StringUtil.replaceAll(pom,
                "<plugins>\n",
                "<plugins>\n" + pluginBlock);
    }

    private String injectLocalizationBootstrap(String targetPath, String content) {
        String javaMainPath = appDir() + "src/main/java/" + packageName.replace('.', '/') + "/" + appName + ".java";
        String kotlinMainPath = appDir() + "src/main/kotlin/" + packageName.replace('.', '/') + "/" + appName + ".kt";
        if (javaMainPath.equals(targetPath)) {
            return injectJavaLocalizationBootstrap(content);
        }
        if (kotlinMainPath.equals(targetPath)) {
            return injectKotlinLocalizationBootstrap(content);
        }
        return content;
    }

    private String injectJavaLocalizationBootstrap(String content) {
        if (content.indexOf("setBundle(") >= 0) {
            return content;
        }
        content = StringUtil.replaceAll(content, "import static com.codename1.ui.CN.*;\n", "import static com.codename1.ui.CN.*;\nimport com.codename1.l10n.L10NManager;\nimport com.codename1.ui.plaf.UIManager;\nimport java.util.Hashtable;\n");
        String method = "\n    @Override\n"
                + "    public void init(Object context) {\n"
                + "        super.init(context);\n"
                + "        String language = L10NManager.getInstance().getLanguage();\n"
                + "        Resources global = Resources.getGlobalResources();\n"
                + "        Hashtable<String, String> bundle = global == null ? null : global.getL10N(\"messages\", language);\n"
                + "        if (bundle == null && global != null) {\n"
                + "            bundle = global.getL10N(\"messages\", \"\");\n"
                + "        }\n"
                + "        if (bundle != null) {\n"
                + "            UIManager.getInstance().setBundle(bundle);\n"
                + "        }\n"
                + "    }\n\n";
        int firstBrace = content.indexOf('{');
        if (firstBrace > -1) {
            return content.substring(0, firstBrace + 1) + method + content.substring(firstBrace + 1);
        }
        return content;
    }

    private String injectKotlinLocalizationBootstrap(String content) {
        if (content.indexOf("setBundle(") >= 0) {
            return content;
        }
        content = StringUtil.replaceAll(content, "import com.codename1.system.Lifecycle\n", "import com.codename1.system.Lifecycle\nimport com.codename1.l10n.L10NManager\nimport com.codename1.ui.plaf.UIManager\nimport com.codename1.ui.util.Resources\nimport java.util.Hashtable\n");
        String method = "\n    override fun init(context: Any?) {\n"
                + "        super.init(context)\n"
                + "        val language = L10NManager.getInstance().language\n"
                + "        val global = Resources.getGlobalResources()\n"
                + "        var bundle: Hashtable<String, String>? = global?.getL10N(\"messages\", language)\n"
                + "        if (bundle == null) {\n"
                + "            bundle = global?.getL10N(\"messages\", \"\")\n"
                + "        }\n"
                + "        if (bundle != null) {\n"
                + "            UIManager.getInstance().setBundle(bundle)\n"
                + "        }\n"
                + "    }\n\n";
        int firstBrace = content.indexOf('{');
        if (firstBrace > -1) {
            return content.substring(0, firstBrace + 1) + method + content.substring(firstBrace + 1);
        }
        return content;
    }

    private static String replaceTagValue(String xml, String tagName, String value) {
        String open = "<" + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        if (start < 0) {
            return xml;
        }
        int valueStart = start + open.length();
        int end = xml.indexOf(close, valueStart);
        if (end < 0) {
            return xml;
        }
        return xml.substring(0, valueStart) + value + xml.substring(end);
    }

    private static String normalizeJavasePom(String pom) {
        pom = removeDependencyBlock(pom, "com.codenameone", "codenameone-core", "provided");
        pom = removeDependencyBlock(pom, "com.codenameone", "codenameone-javase", "provided");
        return pom;
    }

    private static String removeDependencyBlock(String xml, String groupId, String artifactId, String scope) {
        String block =
                "<dependency>\n" +
                "          <groupId>" + groupId + "</groupId>\n" +
                "          <artifactId>" + artifactId + "</artifactId>\n" +
                "          <scope>" + scope + "</scope>\n" +
                "      </dependency>\n";
        return StringUtil.replaceAll(xml, block, "");
    }

    private static String replaceProperty(String content, String key, String value) {
        String linePrefix = key + "=";
        int start = content.indexOf(linePrefix);
        if (start < 0) {
            return content + "\n" + linePrefix + value;
        }
        int end = content.indexOf('\n', start);
        if (end < 0) {
            return content.substring(0, start) + linePrefix + value;
        }
        return content.substring(0, start) + linePrefix + value + content.substring(end);
    }

    private boolean isBareTemplate() {
        return template == Template.BAREBONES || template == Template.KOTLIN;
    }

    private static String ensureDefaultLargeTextScale(String css) {
        if (css.indexOf("useLargerTextScaleBool") >= 0) {
            return css;
        }
        int constantsStart = css.indexOf("#Constants");
        if (constantsStart < 0) {
            return css + "\n\n#Constants {\n    useLargerTextScaleBool: true;\n}\n";
        }
        int blockStart = css.indexOf('{', constantsStart);
        if (blockStart < 0) {
            return css;
        }
        int blockEnd = css.indexOf('}', blockStart);
        if (blockEnd < 0) {
            return css;
        }
        return css.substring(0, blockEnd)
                + "    useLargerTextScaleBool: true;\n"
                + css.substring(blockEnd);
    }

    private String buildReadmeMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("# Codename One Project\n\n");
        if (includesPlatformModules()) {
            out.append("This is a multi-module Maven project for a Codename One app.\n");
        } else {
            out.append("This is a Maven project for a Codename One app. The app lives in `common/`, and every ")
                    .append("platform is built from it: there is no per-platform module until you add one.\n");
        }
        out.append("You can write the app in Java and/or Kotlin, and build for Android, iOS, desktop, and web.\n\n")
                .append("## Getting Started\n\n");

        out.append("Use JDK ").append(options.javaVersion == ProjectOptions.JavaVersion.JAVA_17 ? "17" : "8")
                .append(" or newer for this project's selected Java level. Configure your IDE's project SDK ")
                .append("and Maven runner, and set JAVA_HOME for terminal builds. An IDE's bundled runtime ")
                .append("does not necessarily configure either.\n\n")
                .append("Extract the entire ZIP, then open the root pom.xml as a Maven project. ")
                .append("Use a terminal so errors remain visible.\n\n")
                .append("Windows PowerShell or Command Prompt:\n\n```\n.\\mvnw.cmd -v\n.\\build.bat javascript_cloud\n```\n\n")
                .append("macOS/Linux:\n\n```\n./mvnw -v\n./build.sh javascript_cloud\n```\n\n")
                .append("Check that Maven reports the required JDK. ")
                .append("The first cloud build opens your browser and asks you to sign in or create a free ")
                .append("Codename One account; local builds need no account. ")
                .append("The first build downloads dependencies and can take several minutes. ")
                .append("If a download fails, check your connection or Maven proxy settings and retry.\n\n")
                .append("The javascript command builds locally; javascript_cloud submits a hosted build. ")
                .append("With no target, build makes a local JAR and run starts the local simulator. ")
                .append("These local operations do not create a cloud build.\n\n");

        appendIdeSection(out);
        appendBuildProgressReportingSection(out);

        if (!includesPlatformModules()) {
            out.append("## Native Code\n\n")
                    .append("Native interface implementations go in a directory per platform beside `common/`, ")
                    .append("for example `android/src/main/java` or `ios/src/main/objectivec`. ")
                    .append("`mvn cn1:generate-native-interfaces` creates them, and the build picks them up from there.\n\n");
        }
        if (mavenLayoutsEnabled() && includesBackendModule()) {
            out.append("## Backend\n\n")
                    .append("`backend/` is the app's server. Run it with ")
                    .append("`./mvnw -pl backend -Dcodename1.platform=backend cn1:backend` and package it as a native ")
                    .append("binary with `./mvnw -pl backend -Dcodename1.platform=backend cn1:backend-package`.\n\n");
        }

        if (template.USES_CODERAD) {
            out.append("### Additional Eclipse Steps for CodeRAD Projects\n\n")
                    .append("CodeRAD uses annotation processing, so Eclipse needs two extra settings:\n\n")
                    .append("1. Add `org.eclipse.m2e.apt.mode=jdt_apt` to `./common/.settings/org.eclipse.m2e.apt.prefs`\n")
                    .append("2. Add `target/generated-sources/rad-views` to `.classpath`\n\n")
                    .append("More details:\n")
                    .append("https://github.com/codenameone/CodenameOne/issues/3724\n\n");
        }

        out.append("## Signing\n\n")
                .append("Use the Certificate Wizard to configure Apple signing assets, Android keystores, and desktop signing settings:\n\n")
                .append("```\n")
                .append("mvn cn1:certificatewizard\n")
                .append("```\n\n")
                .append("Generated IDE projects include a Certificate Wizard action under their tools/favorites area.\n\n")
                .append("## Help and Support\n\n")
                .append("- Codename One website: https://www.codenameone.com\n")
                .append("- Codename One GitHub: https://github.com/codenameone/CodenameOne\n");
        return out.toString();
    }

    /// Says what the launchers' build-progress reporting sends and how to turn it
    /// off, where a developer reads before running anything. Only Maven app
    /// downloads have the reporting launchers (see [addLauncherTelemetry(Map)]).
    private void appendBuildProgressReportingSection(StringBuilder out) {
        out.append("## Build progress reporting\n\n")
                .append("`build.sh` and `build.bat` tell Codename One when a build starts and how it ended, ")
                .append("so we can see where first builds get stuck. They send only a one-way hash of the ")
                .append("package name (never the name itself), the build target, the OS family, the Java ")
                .append("version, the exit code, a one-word failure reason and the duration, to ")
                .append(LAUNCHER_EVENTS_URL).append(". Never your code, paths, user name or build output.\n\n")
                .append("To opt out, set `CN1_TELEMETRY` to `0` in the environment the build runs in:\n\n")
                .append("- macOS/Linux: `export CN1_TELEMETRY=0`\n")
                .append("- Windows PowerShell: `$Env:CN1_TELEMETRY = \"0\"`\n")
                .append("- Windows Command Prompt: `set CN1_TELEMETRY=0`\n\n");
    }

    private void appendIdeSection(StringBuilder out) {
        if (ide == IDE.INTELLIJ) {
            out.append("## IntelliJ Users\n\n")
                    .append("This project should work in IntelliJ out of the box.\n")
                    .append("You usually don't need to copy or tweak any project files.\n\n");
            return;
        }
        if (ide == IDE.ECLIPSE) {
            out.append("## Eclipse Users\n\n")
                    .append("The `tools/eclipse` folder includes `.launch` files that add common Maven goals to Eclipse.\n\n")
                    .append("After the Maven import, choose File > Import > Run/Debug > Launch Configurations, select tools/eclipse, and import the launch files before using Run > Run Configurations.\n\n");
            return;
        }
        if (ide == IDE.NETBEANS) {
            out.append("## NetBeans Users\n\n")
                    .append(includesPlatformModules()
                            ? "This is a standard multi-module Maven project generated from an archetype.\n\n"
                            : "This is a standard Maven project; open the root pom.xml.\n\n");
            return;
        }
        out.append("## VS Code Users\n\n")
                .append("Open the project folder in VS Code and make sure Java + Maven extensions are installed.\n\n");
    }

    /// The goals a Maven backend-only project's IDE files offer, as {label, goal}.
    private static final String[][] MAVEN_BACKEND_GOALS = {
            {"Run Backend", "cn1:backend"},
            {"Package Backend", "cn1:backend-package"},
            {"Update Codename One", "cn1:update"}
    };

    /// IDE files for a Maven backend-only project: every IDE imports the pom on its
    /// own, so what is left is a button per goal worth one. The IDE zips are not
    /// used -- their run configurations are the app's simulator and builds.
    private void addMavenBackendIdeEntries(Map<String, byte[]> entries) throws IOException {
        if (ide == IDE.INTELLIJ) {
            for (int i = 0; i < MAVEN_BACKEND_GOALS.length; i++) {
                copySingleTextEntryToMap(".idea/runConfigurations/" + fileNameFor(MAVEN_BACKEND_GOALS[i][0]) + ".xml",
                        intellijMavenRunConfiguration(MAVEN_BACKEND_GOALS[i][0], MAVEN_BACKEND_GOALS[i][1]),
                        entries, ZipEntryType.COMMON);
            }
            return;
        }
        if (ide == IDE.VS_CODE) {
            copySingleTextEntryToMap(".vscode/tasks.json", vscodeTasks(MAVEN_BACKEND_GOALS, "./mvnw", ".\\\\mvnw.cmd"),
                    entries, ZipEntryType.COMMON);
            copySingleTextEntryToMap(".vscode/extensions.json",
                    "{\n  \"recommendations\": [\n"
                            + "    \"vscjava.vscode-java-pack\",\n"
                            + "    \"vscjava.vscode-maven\"\n"
                            + "  ]\n}\n", entries, ZipEntryType.COMMON);
        }
    }

    private static String intellijMavenRunConfiguration(String name, String goal) {
        return "<component name=\"ProjectRunConfigurationManager\">\n"
                + "  <configuration default=\"false\" name=\"" + name + "\" type=\"MavenRunConfiguration\" "
                + "factoryName=\"Maven\">\n"
                + "    <MavenSettings>\n"
                + "      <option name=\"myGeneralSettings\" />\n"
                + "      <option name=\"myRunnerSettings\" />\n"
                + "      <option name=\"myRunnerParameters\">\n"
                + "        <MavenRunnerParameters>\n"
                + "          <option name=\"profiles\">\n"
                + "            <set />\n"
                + "          </option>\n"
                + "          <option name=\"goals\">\n"
                + "            <list>\n"
                + "              <option value=\"" + goal + "\" />\n"
                + "              <option value=\"-e\" />\n"
                + "            </list>\n"
                + "          </option>\n"
                + "          <option name=\"pomFileName\" value=\"pom.xml\" />\n"
                + "          <option name=\"profilesMap\">\n"
                + "            <map />\n"
                + "          </option>\n"
                + "          <option name=\"resolveToWorkspace\" value=\"false\" />\n"
                + "          <option name=\"workingDirPath\" value=\"$PROJECT_DIR$\" />\n"
                + "        </MavenRunnerParameters>\n"
                + "      </option>\n"
                + "    </MavenSettings>\n"
                + "    <method v=\"2\" />\n"
                + "  </configuration>\n"
                + "</component>\n";
    }

    /// A VS Code tasks.json running each {label, argument} through `command`.
    private static String vscodeTasks(String[][] tasks, String command, String windowsCommand) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"version\": \"2.0.0\",\n  \"tasks\": [\n");
        for (int i = 0; i < tasks.length; i++) {
            json.append("    {\n")
                    .append("      \"label\": \"").append(tasks[i][0]).append("\",\n")
                    .append("      \"type\": \"shell\",\n")
                    .append("      \"command\": \"").append(command).append("\",\n")
                    .append("      \"windows\": { \"command\": \"").append(windowsCommand).append("\" },\n")
                    .append("      \"args\": [\"").append(tasks[i][1]).append("\"],\n")
                    .append("      \"problemMatcher\": []\n")
                    .append("    }").append(i + 1 < tasks.length ? "," : "").append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    private String buildMavenBackendReadmeMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("# Codename One Backend\n\n")
                .append("This is a Maven project for a Codename One backend: a server written in Java, ")
                .append("run on the JVM while you develop and packaged as a single native binary.\n\n")
                .append("## Getting Started\n\n")
                .append("Use JDK 17 or newer, and set JAVA_HOME for terminal builds. ")
                .append("Extract the entire ZIP, then open the root pom.xml as a Maven project.\n\n")
                .append("macOS/Linux:\n\n```\nCN1_PROFILE=dev ./mvnw cn1:backend\n```\n\n")
                .append("Windows PowerShell:\n\n```\n$env:CN1_PROFILE=\"dev\"; .\\mvnw.cmd cn1:backend\n```\n\n")
                .append("The first build downloads the dependencies and can take several minutes. ")
                .append("If a download fails, check your connection or Maven proxy settings and retry.\n\n")
                .append("## Goals\n\n")
                .append("- `./mvnw cn1:backend` runs the server on this JVM; it starts in seconds.\n")
                .append("- `CN1_PROFILE=dev ./mvnw cn1:backend` also reads `application-dev.properties` ")
                .append("(an in-memory database).\n")
                .append("- `./mvnw cn1:backend-package` builds a single native binary.\n\n")
                .append("Routes are the annotated methods in `src/main/java/")
                .append(packageName.replace('.', '/')).append("/Api.java`. Settings live in ")
                .append("`application.properties` beside the pom.\n\n");
        if (ide == IDE.ECLIPSE) {
            out.append("## Eclipse Users\n\n")
                    .append("Choose File > Import > Maven > Existing Maven Projects and select this folder. ")
                    .append("Run the goals above from Run As > Maven build.\n\n");
        } else if (ide == IDE.NETBEANS) {
            out.append("## NetBeans Users\n\n")
                    .append("Open this folder as a Maven project and run the goals above from Run Maven > Goals.\n\n");
        } else if (ide == IDE.INTELLIJ) {
            out.append("## IntelliJ Users\n\n")
                    .append("Open this folder; the Run Backend and Package Backend configurations run the goals above.\n\n");
        } else {
            out.append("## VS Code Users\n\n")
                    .append("Open this folder; Terminal > Run Task offers the goals above.\n\n");
        }
        out.append("## Help and Support\n\n")
                .append("- Codename One website: https://www.codenameone.com\n")
                .append("- Codename One GitHub: https://github.com/codenameone/CodenameOne\n");
        return out.toString();
    }

    /// The tasks a Gradle project's IDE files offer, as {label, task path}.
    private String[][] gradleIdeTasks() {
        if (options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY) {
            return new String[][] {
                    {"Run Backend", "runBackend"},
                    {"Package Backend", "backendPackage"},
                    {"Update Codename One", "cn1Update"}
            };
        }
        String[][] app = new String[][] {
                {"Run in Simulator", "run"},
                {"Debug in Simulator", "debug"},
                {"Android Build", "buildAndroid"},
                {"iOS Debug Build", "buildIos"},
                {"iOS Release Build", "buildIosRelease"},
                {"JavaScript Build", "buildJavascript"},
                {"JavaScript Local Build", "buildJavascriptLocal"},
                {"Certificate Wizard", "certificateWizard"},
                {"Codename One Settings", "settings"},
                {"Update Codename One", "cn1Update"}
        };
        if (options.projectType != ProjectOptions.ProjectType.APP_WITH_BACKEND) {
            return app;
        }
        String[][] out = new String[app.length + 1][];
        System.arraycopy(app, 0, out, 0, app.length);
        out[app.length] = new String[] {"Run Backend", ":backend:runBackend"};
        return out;
    }

    /// IDE files for a Gradle project. They are generated here rather than kept as
    /// more resource zips: every IDE imports a Gradle build on its own, so all that
    /// is left to ship is the list of tasks worth a button, and that list depends
    /// on the project type. Eclipse (Buildship) and NetBeans need nothing at all --
    /// their README section says how to open the project.
    private void addGradleIdeEntries(Map<String, byte[]> entries) throws IOException {
        String[][] tasks = gradleIdeTasks();
        if (ide == IDE.INTELLIJ) {
            // gradle.xml links the build, so opening the folder imports it rather than
            // treating .idea/ as a plain IntelliJ project with no build attached.
            putGradleText(entries, ".idea/gradle.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<project version=\"4\">\n"
                            + "  <component name=\"GradleSettings\">\n"
                            + "    <option name=\"linkedExternalProjectsSettings\">\n"
                            + "      <GradleProjectSettings>\n"
                            + "        <option name=\"externalProjectPath\" value=\"$PROJECT_DIR$\" />\n"
                            + "        <option name=\"modules\">\n"
                            + "          <set>\n"
                            + "            <option value=\"$PROJECT_DIR$\" />\n"
                            + (options.projectType == ProjectOptions.ProjectType.APP_WITH_BACKEND
                                    ? "            <option value=\"$PROJECT_DIR$/backend\" />\n" : "")
                            + "          </set>\n"
                            + "        </option>\n"
                            + "      </GradleProjectSettings>\n"
                            + "    </option>\n"
                            + "  </component>\n"
                            + "</project>\n");
            for (int i = 0; i < tasks.length; i++) {
                putGradleText(entries, ".idea/runConfigurations/" + fileNameFor(tasks[i][0]) + ".xml",
                        intellijGradleRunConfiguration(tasks[i][0], tasks[i][1]));
            }
            return;
        }
        if (ide == IDE.VS_CODE) {
            StringBuilder json = new StringBuilder();
            json.append("{\n  \"version\": \"2.0.0\",\n  \"tasks\": [\n");
            for (int i = 0; i < tasks.length; i++) {
                json.append("    {\n")
                        .append("      \"label\": \"").append(tasks[i][0]).append("\",\n")
                        .append("      \"type\": \"shell\",\n")
                        .append("      \"command\": \"./gradlew\",\n")
                        .append("      \"windows\": { \"command\": \".\\\\gradlew.bat\" },\n")
                        .append("      \"args\": [\"").append(tasks[i][1]).append("\"],\n")
                        .append("      \"problemMatcher\": []\n")
                        .append("    }").append(i + 1 < tasks.length ? "," : "").append('\n');
            }
            json.append("  ]\n}\n");
            putGradleText(entries, ".vscode/tasks.json", json.toString());
            putGradleText(entries, ".vscode/extensions.json",
                    "{\n  \"recommendations\": [\n"
                            + "    \"vscjava.vscode-java-pack\",\n"
                            + "    \"vscjava.vscode-gradle\"\n"
                            + "  ]\n}\n");
        }
    }

    private static String intellijGradleRunConfiguration(String name, String task) {
        return "<component name=\"ProjectRunConfigurationManager\">\n"
                + "  <configuration default=\"false\" name=\"" + name + "\" type=\"GradleRunConfiguration\" "
                + "factoryName=\"Gradle\">\n"
                + "    <ExternalSystemSettings>\n"
                + "      <option name=\"executionName\" />\n"
                + "      <option name=\"externalProjectPath\" value=\"$PROJECT_DIR$\" />\n"
                + "      <option name=\"externalSystemIdString\" value=\"GRADLE\" />\n"
                + "      <option name=\"scriptParameters\" value=\"\" />\n"
                + "      <option name=\"taskDescriptions\">\n"
                + "        <list />\n"
                + "      </option>\n"
                + "      <option name=\"taskNames\">\n"
                + "        <list>\n"
                + "          <option value=\"" + task + "\" />\n"
                + "        </list>\n"
                + "      </option>\n"
                + "      <option name=\"vmOptions\" />\n"
                + "    </ExternalSystemSettings>\n"
                + "    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>\n"
                + "    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>\n"
                + "    <DebugAllEnabled>false</DebugAllEnabled>\n"
                + "    <RunAsTest>false</RunAsTest>\n"
                + "    <method v=\"2\" />\n"
                + "  </configuration>\n"
                + "</component>\n";
    }

    private static String fileNameFor(String label) {
        StringBuilder out = new StringBuilder(label.length());
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            boolean keep = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            out.append(keep ? c : '_');
        }
        return out.toString();
    }

    private String buildGradleReadmeMarkdown() {
        boolean backendOnly = options.projectType == ProjectOptions.ProjectType.BACKEND_ONLY;
        StringBuilder out = new StringBuilder();
        out.append("# Codename One Project\n\n");
        if (backendOnly) {
            out.append("This is a Gradle project for a Codename One backend: a server written in Java, ")
                    .append("run on the JVM while you develop and packaged as a single native binary.\n\n");
        } else {
            out.append("This is a Gradle project for a Codename One app. ")
                    .append("You can write the app in Java and/or Kotlin, and build for Android, iOS, desktop, and web.\n\n");
        }
        out.append("The only Codename One line the build needs is the `com.codenameone` plugin in ")
                .append("`settings.gradle.kts`. Bump its version there, or run `./gradlew cn1Update`.\n\n")
                .append("## Getting Started\n\n")
                .append("Use JDK 17 or newer, and set JAVA_HOME for terminal builds. Gradle projects target Java 17. ")
                .append("Extract the entire ZIP, then open the folder in your IDE as a Gradle project.\n\n");
        String first = backendOnly ? "runBackend" : "run";
        out.append("macOS/Linux:\n\n```\n./gradlew ").append(first).append("\n```\n\n")
                .append("Windows PowerShell or Command Prompt:\n\n```\n.\\gradlew.bat ").append(first).append("\n```\n\n")
                .append("The first build downloads Gradle and the dependencies and can take several minutes. ")
                .append("If a download fails, check your connection or Gradle proxy settings and retry.\n\n");

        if (backendOnly) {
            out.append("## Tasks\n\n")
                    .append("- `./gradlew runBackend` runs the server on this JVM; it starts in seconds.\n")
                    .append("- `CN1_PROFILE=dev ./gradlew runBackend` also reads `application-dev.properties` ")
                    .append("(an in-memory database).\n")
                    .append("- `./gradlew backendPackage` builds a single native binary.\n")
                    .append("- `./gradlew tasks` lists everything else.\n\n")
                    .append("Routes are the annotated methods in `src/main/java/")
                    .append(packageName.replace('.', '/')).append("/Api.java`. Settings live in ")
                    .append("`application.properties` beside the build files.\n\n");
        } else {
            out.append("## Tasks\n\n")
                    .append("- `./gradlew run` runs the app in the simulator; `./gradlew debug` waits for a debugger on port 5005.\n")
                    .append("- `./gradlew buildAndroid`, `./gradlew buildIos` and `./gradlew buildIosRelease` send ")
                    .append("device builds to the build server.\n")
                    .append("- `./gradlew buildJavascriptLocal` builds the web app locally; `./gradlew buildJavascript` ")
                    .append("submits a hosted build.\n")
                    .append("- `./gradlew buildAndroidGradleProject` and `./gradlew buildIosXcodeProject` generate native ")
                    .append("IDE projects locally.\n")
                    .append("- `./gradlew buildMacDesktop` and `./gradlew buildWindowsDesktop` build desktop apps.\n")
                    .append("- `./gradlew settings` opens Codename One Settings; `./gradlew tasks` lists everything else.\n\n")
                    .append("Complete the browser login when a build asks for it. Local tasks such as `run` and ")
                    .append("`buildJavascriptLocal` do not create a cloud build.\n\n")
                    .append("## Libraries\n\n")
                    .append("Declare Codename One libraries (cn1libs) by their Maven coordinates in `build.gradle.kts`:\n\n")
                    .append("```\ndependencies {\n    cn1lib(\"com.codenameone:googlemaps-lib:1.0\")\n}\n```\n\n")
                    .append("Legacy `.cn1lib` files are not supported by the Gradle build.\n\n");
            if (options.projectType == ProjectOptions.ProjectType.APP_WITH_BACKEND) {
                out.append("## Backend\n\n")
                        .append("`backend/` is the app's server. Run it with `./gradlew :backend:runBackend` and ")
                        .append("package it with `./gradlew :backend:backendPackage`.\n\n");
            } else {
                out.append("## Backend\n\n")
                        .append("`./gradlew addBackend` adds a `backend/` server project to this build.\n\n");
            }
        }

        appendGradleIdeSection(out);

        if (!backendOnly) {
            out.append("## Signing\n\n")
                    .append("Use the Certificate Wizard to configure Apple signing assets, Android keystores, ")
                    .append("and desktop signing settings:\n\n")
                    .append("```\n./gradlew certificateWizard\n```\n\n");
        }
        out.append("## Help and Support\n\n")
                .append("- Codename One website: https://www.codenameone.com\n")
                .append("- Codename One GitHub: https://github.com/codenameone/CodenameOne\n");
        return out.toString();
    }

    private void appendGradleIdeSection(StringBuilder out) {
        if (ide == IDE.INTELLIJ) {
            out.append("## IntelliJ Users\n\n")
                    .append("Open the project folder; IntelliJ imports the Gradle build. The Run menu lists ")
                    .append("configurations for the common tasks, and the Gradle tool window has the rest.\n\n");
            return;
        }
        if (ide == IDE.ECLIPSE) {
            out.append("## Eclipse Users\n\n")
                    .append("Choose File > Import > Gradle > Existing Gradle Project and select this folder. ")
                    .append("Eclipse's Gradle support (Buildship) imports the build; run tasks from the ")
                    .append("Gradle Tasks view.\n\n");
            return;
        }
        if (ide == IDE.NETBEANS) {
            out.append("## NetBeans Users\n\n")
                    .append("Choose File > Open Project and select this folder. NetBeans opens Gradle builds ")
                    .append("directly; run tasks from the project's Navigator or context menu.\n\n");
            return;
        }
        out.append("## VS Code Users\n\n")
                .append("Install the recommended extensions (Extension Pack for Java, Gradle for Java), then use ")
                .append("Terminal > Run Task for the common tasks.\n\n");
    }

    public static String buildThemeOverrides(ProjectOptions options) {
        ProjectOptions effective = options == null ? ProjectOptions.defaults() : options;
        String customCss = normalizeCustomCss(effective.customThemeCss);
        boolean hasCustomCss = customCss.length() > 0;
        if (isDefaultBarebonesOptions(effective) && !hasCustomCss) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (!isDefaultBarebonesOptions(effective)) {
            out.append("\n\n/* Initializr Theme Overrides */\n");
        }

        if (effective.themeMode == ProjectOptions.ThemeMode.DARK) {
            out.append("Form {\n")
                    .append("    background-color: #0f172a;\n")
                    .append("    color: #e2e8f0;\n")
                    .append("}\n")
                    .append("Toolbar {\n")
                    .append("    background-color: #0f172a;\n")
                    .append("    border: none;\n")
                    .append("}\n")
                    .append("Title, TitleCommand, Command, OverflowCommand {\n")
                    .append("    color: #e2e8f0;\n")
                    .append("}\n")
                    .append("DialogBody, DialogTitle {\n")
                    .append("    color: #e2e8f0;\n")
                    .append("}\n");

            if (effective.accent == ProjectOptions.Accent.DEFAULT) {
                out.append("Button {\n")
                        .append("    color: #e2e8f0;\n")
                        .append("    background-color: #1f2937;\n")
                        .append("    border: 1px solid #475569;\n")
                        .append("}\n")
                        .append("Button.pressed {\n")
                        .append("    color: #e2e8f0;\n")
                        .append("    background-color: #334155;\n")
                        .append("    border: 1px solid #64748b;\n")
                        .append("}\n");
                appendCustomCss(out, customCss);
                return out.toString();
            }
        } else if (effective.accent == ProjectOptions.Accent.DEFAULT) {
            // Light + Clean intentionally inherits template defaults (rounded ignored) unless custom CSS is provided.
            out.setLength(0);
            appendCustomCss(out, customCss);
            return out.toString();
        }

        int accent = resolveAccentColor(effective);
        int accentPressed = darkenColor(accent, 0.22f);
        String buttonRadius = effective.roundedButtons ? "3mm" : "0";
        out.append("Button {\n")
                .append("    background-color: ").append(toCssColor(accent)).append(";\n")
                .append("    color: #ffffff;\n")
                .append("    border: 1px solid ").append(toCssColor(accent)).append(";\n")
                .append("    border-radius: ").append(buttonRadius).append(";\n")
                .append("}\n")
                .append("Button.pressed {\n")
                .append("    background-color: ").append(toCssColor(accentPressed)).append(";\n")
                .append("    border: 1px solid ").append(toCssColor(accentPressed)).append(";\n")
                .append("    color: #ffffff;\n")
                .append("    border-radius: ").append(buttonRadius).append(";\n")
                .append("}\n");
        appendCustomCss(out, customCss);
        return out.toString();
    }

    private static String normalizeCustomCss(String css) {
        if (css == null) {
            return "";
        }
        String trimmed = css.trim();
        if (trimmed.length() == 0) {
            return "";
        }
        return normalizeCustomCssForCompiler(trimmed);
    }

    public static String normalizeCustomCssForCompiler(String css) {
        String out = css;
        out = expandPreviewButtonAliases(out);
        out = replaceKnownNamedColors(out);
        out = addAlignFallback(out);
        return out;
    }

    private static String expandPreviewButtonAliases(String css) {
        String out = css;
        out = StringUtil.replaceAll(out, "Button.pressed {", PREVIEW_BUTTON_PRESSED_SELECTOR + " {");
        out = StringUtil.replaceAll(out, "Button.pressed{", PREVIEW_BUTTON_PRESSED_SELECTOR + "{");
        out = StringUtil.replaceAll(out, "Button {", PREVIEW_BUTTON_SELECTOR + " {");
        out = StringUtil.replaceAll(out, "Button{", PREVIEW_BUTTON_SELECTOR + "{");
        return out;
    }

    private static String replaceKnownNamedColors(String css) {
        String out = css;
        out = replaceCssColorValue(out, "pink", "#ffc0cb");
        out = replaceCssColorValue(out, "orange", "#ffa500");
        out = replaceCssColorValue(out, "purple", "#800080");
        out = replaceCssColorValue(out, "yellow", "#ffff00");
        out = replaceCssColorValue(out, "gray", "#808080");
        out = replaceCssColorValue(out, "grey", "#808080");
        return out;
    }

    private static String replaceCssColorValue(String css, String namedColor, String hexColor) {
        StringBuilder out = new StringBuilder();
        int from = 0;
        while (from < css.length()) {
            int colon = css.indexOf(':', from);
            if (colon < 0) {
                out.append(css.substring(from));
                break;
            }
            out.append(css.substring(from, colon + 1));
            int valueStart = colon + 1;
            while (valueStart < css.length() && Character.isWhitespace(css.charAt(valueStart))) {
                valueStart++;
            }
            int valueEnd = valueStart + namedColor.length();
            if (matchesIgnoreCase(css, valueStart, namedColor)) {
                int semiPos = valueEnd;
                while (semiPos < css.length() && Character.isWhitespace(css.charAt(semiPos))) {
                    semiPos++;
                }
                if (semiPos < css.length() && css.charAt(semiPos) == ';') {
                    out.append(css.substring(colon + 1, valueStart));
                    out.append(hexColor);
                    out.append(css.substring(valueEnd, semiPos + 1));
                    from = semiPos + 1;
                    continue;
                }
            }
            from = colon + 1;
        }
        return out.toString();
    }

    private static boolean matchesIgnoreCase(String text, int start, String token) {
        if (start < 0 || start + token.length() > text.length()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char a = Character.toLowerCase(text.charAt(start + i));
            char b = Character.toLowerCase(token.charAt(i));
            if (a != b) {
                return false;
            }
        }
        return true;
    }

    private static String addAlignFallback(String css) {
        String out = css;
        int searchFrom = 0;
        while (searchFrom < out.length()) {
            int idx = indexOfIgnoreCase(out, "text-align", searchFrom);
            if (idx < 0) {
                break;
            }
            int colon = out.indexOf(':', idx);
            if (colon < 0) {
                break;
            }
            int semi = out.indexOf(';', colon);
            if (semi < 0) {
                break;
            }
            String value = out.substring(colon + 1, semi).trim();
            String fallback = "\n    align: " + value + ";";
            out = out.substring(0, semi + 1) + fallback + out.substring(semi + 1);
            searchFrom = semi + fallback.length() + 1;
        }
        return out;
    }

    private static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        String lowerText = text.toLowerCase();
        return lowerText.indexOf(needle.toLowerCase(), fromIndex);
    }

    private static void appendCustomCss(StringBuilder out, String customCss) {
        if (customCss.length() == 0) {
            return;
        }
        out.append("\n/* Initializr Appended Custom CSS */\n")
                .append(customCss)
                .append('\n');
    }

    private static boolean isDefaultBarebonesOptions(ProjectOptions options) {
        return options.themeMode == ProjectOptions.ThemeMode.LIGHT
                && options.accent == ProjectOptions.Accent.DEFAULT;
    }

    private static int resolveAccentColor(ProjectOptions options) {
        if (options.accent == ProjectOptions.Accent.DEFAULT) {
            return 0x0f766e;
        }
        if (options.accent == ProjectOptions.Accent.BLUE) {
            return 0x1d4ed8;
        }
        if (options.accent == ProjectOptions.Accent.ORANGE) {
            return 0xea580c;
        }
        return 0x0f766e;
    }


    private String buildThemeCss() {
        return buildThemeOverrides(options);
    }

    private static String toCssColor(int color) {
        String hex = Integer.toHexString(color & 0xffffff);
        while (hex.length() < 6) {
            hex = "0" + hex;
        }
        return "#" + hex;
    }

    private static int darkenColor(int color, float ratio) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        r = Math.max(0, (int)(r * (1f - ratio)));
        g = Math.max(0, (int)(g * (1f - ratio)));
        b = Math.max(0, (int)(b * (1f - ratio)));
        return (r << 16) | (g << 8) | b;
    }

    private static boolean isTextFile(String path) {
        return path.endsWith(".xml")
                || path.endsWith(".properties")
                || path.endsWith(".java")
                || path.endsWith(".kt")
                || path.endsWith(".json")
                || path.endsWith(".launch")
                || path.endsWith(".css")
                || path.endsWith(".xsd")
                || path.endsWith(".md")
                || path.endsWith(".adoc")
                || path.endsWith(".bat")
                || path.endsWith(".cmd")
                || path.endsWith(".sh")
                || path.endsWith(".kts")
                || "mvnw".equals(path);
    }

    private static String readResourceToString(String resourcePath) throws IOException {
        try (InputStream inputStream = getResourceAsStream(resourcePath)) {
            return readToStringNoClose(inputStream);
        }
    }

    private static String readToStringNoClose(InputStream is) throws IOException {
        return StringUtil.newString(readToBytesNoClose(is));
    }

    private static byte[] readToBytesNoClose(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Util.copyNoClose(is, bos, 8192);
        bos.close();
        return bos.toByteArray();
    }
}
