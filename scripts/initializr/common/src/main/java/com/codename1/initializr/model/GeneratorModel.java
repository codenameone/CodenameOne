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
import com.codename1.io.Log;
import com.codename1.io.Util;
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
    private static final String CN1_PLUGIN_VERSION = "7.0.258";
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
    private static final String CLAUDE_SKILL_STUB_BODY =
            "---\n"
            + "name: codename-one\n"
            + "description: Build and modify Codename One cross-platform mobile apps (Java 17, Maven, ParparVM/Android/iOS/JavaScript). Use when the project contains a `common/codenameone_settings.properties`, depends on `com.codenameone:codenameone-core`, edits CSS files under `common/src/main/css/`, calls `cn1:run`, `cn1:test`, `cn1:build`, references `com.codename1.ui.*` / `com.codename1.testing.*`, or when the user asks to build a UI, write screen tests, generate screenshots, or compare to Swing/HTML/Android.\n"
            + "metadata:\n"
            + "  type: skill\n"
            + "---\n"
            + "\n"
            + "# Codename One — App and UI Authoring Skill (Claude Code stub)\n"
            + "\n"
            + "This file exists so Claude Code can index the Codename One authoring skill.\n"
            + "The actual skill content is **vendor-neutral** and lives in this repository at:\n"
            + "\n"
            + "- `.agent-skills/codename-one/SKILL.md` — top-level cheat sheet\n"
            + "- `.agent-skills/codename-one/references/*.md` — deep-dive references\n"
            + "- `.agent-skills/codename-one/tools/` — runnable Java 17 utilities (`isApiSupported`, `isCssValid`, ...)\n"
            + "\n"
            + "**Read `.agent-skills/codename-one/SKILL.md` next.** All the guidance you need to\n"
            + "build, style, test, debug, and port to Codename One is in that directory.\n";
    private static final String AGENTS_MD_BODY =
            "# AGENTS.md\n"
            + "\n"
            + "This project is a Codename One cross-platform mobile app (Java 17 / Maven /\n"
            + "ParparVM-iOS / Android / JavaScript / desktop). A vendor-neutral authoring skill\n"
            + "is bundled in this repository for any AI agent:\n"
            + "\n"
            + "- **Start here:** `.agent-skills/codename-one/SKILL.md`\n"
            + "- **Topical references:** `.agent-skills/codename-one/references/`\n"
            + "- **Runnable utilities (Java 17 single-file source mode):** `.agent-skills/codename-one/tools/`\n"
            + "\n"
            + "Tool integrations (Claude Code, Cursor, etc.) may also pick this skill up via\n"
            + "their own conventions; the canonical source of truth is `.agent-skills/`.\n"
            + "\n"
            + "## Quick orientation for an agent\n"
            + "\n"
            + "- App source lives in `common/src/main/java/`.\n"
            + "- Theme/styling lives in `common/src/main/css/theme.css` (Codename One CSS — a\n"
            + "  deliberate subset, see `.agent-skills/codename-one/references/css.md`).\n"
            + "- Run the simulator with `mvn -pl common cn1:run`.\n"
            + "- Run tests with `mvn -pl common cn1:test` (on Linux CI use `xvfb-run -a`).\n"
            + "- Native cloud builds use `mvn -pl <ios|android|javascript|javase> package -Dcodename1.platform=... -Dcodename1.buildTarget=...`.\n"
            + "\n"
            + "When in doubt, open `.agent-skills/codename-one/SKILL.md` and follow the\n"
            + "reference table at the bottom.\n";

    private final IDE ide;
    private final Template template;
    private final String appName;
    private final String packageName;
    private final ProjectOptions options;

    GeneratorModel(IDE ide, Template template, String appName, String packageName, ProjectOptions options) {
        this.ide = ide;
        this.template = template;
        this.appName = appName;
        this.packageName = packageName;
        this.options = options == null ? ProjectOptions.defaults() : options;
    }

    public static GeneratorModel create(IDE ide, Template template, String appName, String packageName) {
        return new GeneratorModel(ide, template, appName, packageName, ProjectOptions.defaults());
    }

    public static GeneratorModel create(IDE ide, Template template, String appName, String packageName, ProjectOptions options) {
        return new GeneratorModel(ide, template, appName, packageName, options);
    }

    public void generate() {
        cleanupGeneratedZips();
        String fileName = appName.toLowerCase() + ".zip";

        // Collect the project's entries (read source/template/cn1lib bytes).
        Map<String, byte[]> entries;
        try {
            entries = collectProjectEntries();
        } catch (IOException ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Couldn't build the project: " + describeError(ex));
            return;
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
            return;
        }
        if (downloadBytesAsFile(fileName, bytes)) {
            return;
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
                return;
            }
        }
        execute(filePath);
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
        Map<String, byte[]> mergedEntries = new LinkedHashMap<String, byte[]>();

        copyZipEntriesToMap(ide.ZIP, mergedEntries, ZipEntryType.IDE);
        copyZipEntriesToMap("/common.zip", mergedEntries, ZipEntryType.COMMON);
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
        validateGeneratedPomCoordinates(mergedEntries);
        return mergedEntries;
    }

    /// Refuses to publish a generated download when one of the embedded module POMs
    /// still points at the Initializr application itself, or otherwise drifts from
    /// the generated root project's Maven coordinates. This is intentionally a
    /// runtime guard in addition to the tests: common.zip is a committed binary
    /// artifact, so a bad manual rebuild must fail closed instead of reaching users.
    void validateGeneratedPomCoordinates(Map<String, byte[]> entries) throws IOException {
        String rootArtifactId = appName.toLowerCase();
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

        validateModulePomCoordinates(entries, "common", rootArtifactId + "-common", false, version);

        String[] platforms = new String[] {"android", "ios", "javase", "javascript", "linux", "win"};
        for (int i = 0; i < platforms.length; i++) {
            String platform = platforms[i];
            validateModulePomCoordinates(entries, platform, rootArtifactId + "-" + platform, true, version);
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
                        + "<artifactId>" + appName.toLowerCase() + "</artifactId>"
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

    /// Writes the collected entries as a STORED (uncompressed) zip. STORED rather
    /// than DEFLATED keeps the byte work minimal (CRC + copy, no compression
    /// engine); the size cost is irrelevant for a one-off scaffold download.
    /// Used on every platform including the JavaScript port, where it is fast now
    /// that the translator no longer wraps synchronous natives in generators and
    /// resolves inherited interface static fields correctly.
    void writeEntriesToZip(OutputStream outputStream, Map<String, byte[]> mergedEntries) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
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
        copySingleTextEntryToMap("AGENTS.md", AGENTS_MD_BODY, mergedEntries, ZipEntryType.COMMON);
        // Claude Code stub. Frontmatter so the skill shows up in /skills, body redirects
        // to the canonical vendor-neutral content.
        copySingleTextEntryToMap(CLAUDE_SKILL_STUB_PATH, CLAUDE_SKILL_STUB_BODY,
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
                "common/src/main/l10n/messages.properties",
                readResourceToString("/messages.properties"),
                mergedEntries,
                ZipEntryType.COMMON
        );
        for (ProjectOptions.PreviewLanguage language : ProjectOptions.PreviewLanguage.values()) {
            if (language == ProjectOptions.PreviewLanguage.ENGLISH) {
                continue;
            }
            copySingleTextEntryToMap(
                    "common/src/main/l10n/messages_" + language.bundleSuffix + ".properties",
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
        byte[] targetData = applyDataReplacements(targetName, sourceData);
        mergedEntries.put(targetName, targetData);
    }

    private void copySingleTextEntryToMap(String targetPath, String content, Map<String, byte[]> mergedEntries, ZipEntryType zipType) throws IOException {
        byte[] sourceData = content.getBytes("UTF-8");
        copyEntryToMap(targetPath, sourceData, mergedEntries, zipType);
    }

    private String mapTargetPath(String sourcePath, ZipEntryType zipType) {
        String targetPath = sourcePath;
        if (zipType == ZipEntryType.TEMPLATE_CSS) {
            targetPath = "common/src/main/css/" + sourcePath;
        } else if (zipType == ZipEntryType.TEMPLATE_SOURCE) {
            if (sourcePath.startsWith("java/")) {
                targetPath = "common/src/main/java/" + sourcePath.substring("java/".length());
            } else if (sourcePath.startsWith("kotlin/")) {
                targetPath = "common/src/main/kotlin/" + sourcePath.substring("kotlin/".length());
            } else if (sourcePath.startsWith("resources/")) {
                targetPath = "common/src/main/resources/" + sourcePath.substring("resources/".length());
            } else if (sourcePath.startsWith("rad/")) {
                targetPath = "common/src/main/rad/" + sourcePath.substring("rad/".length());
            } else {
                targetPath = "common/src/main/" + sourcePath;
            }
        }
        return applyPathReplacements(targetPath);
    }

    private String applyPathReplacements(String path) {
        String packagePath = packageName.replace('.', '/');
        String sourcePackagePath = template.SOURCE_PACKAGE.replace('.', '/');

        String replaced = path;
        replaced = StringUtil.replaceAll(replaced, "com/example/myapp", packagePath);
        replaced = StringUtil.replaceAll(replaced, sourcePackagePath, packagePath);
        replaced = StringUtil.replaceAll(replaced, "MyAppName", appName);
        replaced = StringUtil.replaceAll(replaced, template.SOURCE_MAIN_CLASS, appName);
        replaced = StringUtil.replaceAll(replaced, "myappname", appName.toLowerCase());
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
        content = StringUtil.replaceAll(content, "myappname", appName.toLowerCase());
        if ("common/codenameone_settings.properties".equals(targetPath)) {
            content = replaceProperty(content, "codename1.kotlin", String.valueOf(template.IS_KOTLIN));
            content = applyJavaVersionSettings(content);
        }
        if (options.includeLocalizationBundles && isBareTemplate()) {
            content = injectLocalizationBootstrap(targetPath, content);
        }
        if (isBareTemplate() && "common/src/main/css/theme.css".equals(targetPath)) {
            content = ensureDefaultLargeTextScale(content);
            content += buildThemeCss();
        }
        if ("common/pom.xml".equals(targetPath)) {
            content = applyJavaVersionToPom(content);
        }
        if (".idea/misc.xml".equals(targetPath)) {
            content = normalizeIntellijMiscXml(content);
        }
        if (".idea/workspace.xml".equals(targetPath)) {
            content = applySimulatorJvmExportToIdeaWorkspace(content);
        }
        if ("pom.xml".equals(targetPath)) {
            content = replaceTagValue(content, "cn1.plugin.version", CN1_PLUGIN_VERSION);
            content = replaceTagValue(content, "cn1.version", CN1_PLUGIN_VERSION);
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
        String javaMainPath = "common/src/main/java/" + packageName.replace('.', '/') + "/" + appName + ".java";
        String kotlinMainPath = "common/src/main/kotlin/" + packageName.replace('.', '/') + "/" + appName + ".kt";
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
        out.append("# Codename One Project\n\n")
                .append("This is a multi-module Maven project for a Codename One app.\n")
                .append("You can write the app in Java and/or Kotlin, and build for Android, iOS, desktop, and web.\n\n")
                .append("## Getting Started\n\n");

        if (template.IS_KOTLIN) {
            out.append("You selected the Kotlin template. Start here:\n")
                    .append("https://shannah.github.io/cn1app-archetype-kotlin-template/getting-started.html\n\n");
        } else {
            out.append("You selected a Java template. Start here:\n")
                    .append("https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html\n\n");
        }

        appendIdeSection(out);

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
                    .append("After importing this project into Eclipse, import those launch files.\n\n");
            return;
        }
        if (ide == IDE.NETBEANS) {
            out.append("## NetBeans Users\n\n")
                    .append("This is a standard multi-module Maven project generated from an archetype.\n\n");
            return;
        }
        out.append("## VS Code Users\n\n")
                .append("Open the project folder in VS Code and make sure Java + Maven extensions are installed.\n\n");
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
