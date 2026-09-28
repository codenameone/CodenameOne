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

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import com.codename1.util.StringUtil;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GeneratorModelMatrixTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        // Run the targeted regression checks first so they don't get masked when an
        // earlier broad assertion in validateCombination(...) fails first.
        validateClaudeSkillBundled();
        validateJava17DefaultRegressionFixes();
        validateLegacyJava8Generation();
        validateCoordinateGuardRejectsBrokenArtifacts();
        validateLocaleIndependentArtifactIds();
        validateGradleRefusesUnsupportedCombinations();
        validateMavenIgnoresAppProjectType();
        validateGradleTemplateDependencies();
        validateAgentsMdMatchesBuildTool();
        validateGradleLocalization();
        for (Template template : Template.values()) {
            if (!template.supportsGradle()) {
                continue;
            }
            for (ProjectOptions.ProjectType type : ProjectOptions.ProjectType.values()) {
                for (IDE ide : IDE.values()) {
                    validateGradleCombination(template, type, ide);
                }
            }
        }
        for (Template template : Template.values()) {
            for (IDE ide : IDE.values()) {
                validateCombination(template, ide);
            }
        }
        validateAppendedCustomCssGeneration();
        validateCustomCssWithoutPresetOverrides();
        validateThemeCssCompilesForAllVariants();
        return true;
    }



    private static ProjectOptions gradleOptions(ProjectOptions.ProjectType type) {
        return ProjectOptions.defaults().withBuild(ProjectOptions.BuildTool.GRADLE, type);
    }

    /// Every Gradle download, per template, project type and IDE: a single project
    /// at the root with the shared template's build files, and nothing of Maven's.
    private void validateGradleCombination(Template template, ProjectOptions.ProjectType type, IDE ide) throws Exception {
        String mainClassName = "Grd" + template.ordinal() + type.ordinal() + ide.ordinal() + "App";
        String packageName = "com.acme.g" + template.ordinal() + ".t" + type.ordinal() + ".i" + ide.ordinal();
        String packagePath = packageName.replace('.', '/');
        String label = template + "/" + type + "/" + ide + ": ";
        byte[] zipData = createProjectZip(ide, template, mainClassName, packageName, gradleOptions(type));
        Map<String, byte[]> entries = readZipEntries(zipData);

        for (String path : entries.keySet()) {
            assertFalse(path.startsWith("common/"), label + "Gradle projects have no common/ module: " + path);
            assertFalse(path.endsWith("pom.xml"), label + "Gradle projects carry no pom: " + path);
            assertFalse(path.startsWith("mvnw") || path.startsWith(".mvn/"), label + "Maven wrapper leaked: " + path);
            assertFalse(path.equals("build.sh") || path.equals("run.sh") || path.equals("build.bat")
                    || path.equals("run.bat"), label + "Maven launcher leaked: " + path);
            String[] mavenModules = {"android/", "ios/", "javase/", "javascript/", "linux/", "win/", "cn1libs/"};
            for (int i = 0; i < mavenModules.length; i++) {
                assertFalse(path.startsWith(mavenModules[i]), label + "Maven module leaked: " + path);
            }
            // Native directories (src/<platform>/<lang>) are created on demand by
            // generateNativeInterfaces, never shipped empty or pre-populated.
            if (path.startsWith("src/") || path.startsWith("backend/src/")) {
                String rel = path.startsWith("backend/") ? path.substring("backend/".length()) : path;
                assertTrue(rel.startsWith("src/main/"), label + "unexpected source set: " + path);
            }
            assertFalse(path.indexOf("com/example/myapp") >= 0, label + "Unrefactored placeholder path found: " + path);
        }

        assertNotNull(entries.get("gradlew"), label + "missing gradlew");
        assertNotNull(entries.get("gradlew.bat"), label + "missing gradlew.bat");
        assertNotNull(entries.get("gradle/wrapper/gradle-wrapper.jar"), label + "missing wrapper jar");
        assertNotNull(entries.get("gradle/wrapper/gradle-wrapper.properties"), label + "missing wrapper properties");
        assertTrue(unixMode(zipData, "gradlew") == 0100755, label + "gradlew must extract executable");
        assertTrue(unixMode(zipData, "gradlew.bat") == 0100644, label + "gradlew.bat is not a Unix executable");

        String settings = getText(entries, "settings.gradle.kts");
        assertContains(settings, "id(\"com.codenameone\") version \"7.0.273\"",
                label + "settings.gradle.kts should apply the Codename One plugin at the generated version");
        assertContains(settings, "rootProject.name = \"" + GeneratorModel.toLowerCaseInvariant(mainClassName) + "\"",
                label + "settings.gradle.kts should name the project like the Maven artifactId");
        assertContains(getText(entries, "gradle.properties"), "org.gradle.configuration-cache=true",
                label + "gradle.properties should come from the shared template");
        String gitIgnore = getText(entries, ".gitignore");
        assertContains(gitIgnore, ".gradle/", label + ".gitignore should ignore the Gradle cache");
        assertContains(gitIgnore, "build/", label + ".gitignore should ignore build outputs");
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            if (e.getKey().endsWith(".kts") || e.getKey().endsWith(".properties") || e.getKey().endsWith(".java")) {
                String text = StringUtil.newString(e.getValue());
                assertFalse(text.indexOf("__CN1_VERSION__") >= 0 || text.indexOf("__PROJECT_NAME__") >= 0
                        || text.indexOf("__BACKEND__") >= 0 || text.indexOf("${package}") >= 0,
                        label + "unrendered template token in " + e.getKey());
            }
        }

        String readme = getText(entries, "README.md");
        assertContains(readme, "./gradlew", label + "README should document Gradle commands");
        assertFalse(readme.indexOf("mvn") >= 0, label + "Gradle README must not mention Maven commands");
        assertFalse(readme.indexOf("build.sh") >= 0, label + "Gradle README must not mention the Maven launchers");

        String apiPath;
        if (type == ProjectOptions.ProjectType.BACKEND_ONLY) {
            assertNull(entries.get("codenameone_settings.properties"),
                    label + "a backend has no Codename One app settings");
            assertNull(entries.get("icon.png"), label + "a backend has no icon");
            assertNull(entries.get("src/main/css/theme.css"), label + "a backend has no theme");
            assertNull(entries.get("AGENTS.md"), label + "the UI authoring skill is not shipped with a backend");
            assertNotNull(entries.get("application.properties"), label + "backend settings belong at the root");
            assertNotNull(entries.get("application-dev.properties"), label + "backend dev profile belongs at the root");
            assertContains(getText(entries, "build.gradle.kts"), "runBackend",
                    label + "a backend-only root build script is the backend template");
            apiPath = "src/main/java/" + packagePath + "/Api.java";
            String api = getText(entries, apiPath);
            assertContains(api, "./gradlew runBackend", label + "backend-only tasks are addressed from the root");
            assertFalse(api.indexOf(":backend:") >= 0, label + "backend-only has no :backend: subproject");
            assertContains(readme, "./gradlew runBackend", label + "backend README should say how to run it");
        } else {
            String settingsProps = getText(entries, "codenameone_settings.properties");
            assertContains(settingsProps, "codename1.packageName=" + packageName, label + "settings package");
            assertContains(settingsProps, "codename1.mainName=" + mainClassName, label + "settings main class");
            assertContains(settingsProps, "codename1.kotlin=" + template.IS_KOTLIN, label + "settings kotlin flag");
            assertContains(settingsProps, "codename1.arg.java.version=17", label + "Gradle projects are Java 17");
            assertNotNull(entries.get("icon.png"), label + "icon belongs at the root");
            String themeCss = getText(entries, "src/main/css/theme.css");
            assertContains(themeCss, "useLargerTextScaleBool: true;", label + "theme defaults apply at the root too");
            String mainPath = template.IS_KOTLIN
                    ? "src/main/kotlin/" + packagePath + "/" + mainClassName + ".kt"
                    : "src/main/java/" + packagePath + "/" + mainClassName + ".java";
            assertContains(getText(entries, mainPath), "package " + packageName, label + "main source package");
            String buildScript = getText(entries, "build.gradle.kts");
            if (template.IS_KOTLIN) {
                assertContains(buildScript, "kotlin(\"jvm\") version \"" + GeneratorModel.KOTLIN_VERSION + "\"",
                        label + "Kotlin projects apply the Kotlin Gradle plugin");
                assertContains(buildScript, "org.jetbrains.kotlin:kotlin-stdlib:" + GeneratorModel.KOTLIN_VERSION,
                        label + "Kotlin projects declare kotlin-stdlib, as the Maven template does");
                assertTrue(buildScript.indexOf("plugins {") < buildScript.indexOf("dependencies {"),
                        label + "plugins {} must precede every other block");
            } else {
                assertFalse(buildScript.indexOf("kotlin(") >= 0, label + "Java projects do not apply Kotlin");
            }
            assertContains(getText(entries, "AGENTS.md"), "./gradlew run", label + "AGENTS.md should use Gradle");
            assertNotNull(entries.get(".agent-skills/codename-one/SKILL.md"), label + "the skill ships with Gradle apps");
            apiPath = "backend/src/main/java/" + packagePath + "/Api.java";
            if (type == ProjectOptions.ProjectType.APP_WITH_BACKEND) {
                assertNotNull(entries.get("backend/application.properties"), label + "backend settings");
                assertNotNull(entries.get("backend/application-dev.properties"), label + "backend dev profile");
                assertNotNull(entries.get("backend/build.gradle.kts"), label + "backend build script");
                String api = getText(entries, apiPath);
                assertContains(api, "package " + packageName + ";", label + "backend shares the app package");
                assertContains(api, "./gradlew :backend:runBackend", label + "backend tasks are addressed as a subproject");
                assertContains(getText(entries, "backend/application.properties"), ":backend:runBackend",
                        label + "backend settings comment should name the subproject task");
            } else {
                for (String path : entries.keySet()) {
                    assertFalse(path.startsWith("backend/"), label + "an App project has no backend: " + path);
                }
                assertContains(readme, "./gradlew addBackend", label + "README should say how to add a backend");
            }
        }

        if (ide == IDE.INTELLIJ) {
            assertContains(getText(entries, ".idea/gradle.xml"), "GradleProjectSettings",
                    label + "IntelliJ should link the Gradle build");
            assertNull(entries.get(".idea/workspace.xml"), label + "Maven run configurations must not ship with Gradle");
            if (type == ProjectOptions.ProjectType.BACKEND_ONLY) {
                assertContains(getText(entries, ".idea/runConfigurations/Run_Backend.xml"),
                        "<option value=\"runBackend\" />", label + "IntelliJ backend run configuration");
            } else {
                String[][] expected = {
                        {"Run_in_Simulator", "run"}, {"Debug_in_Simulator", "debug"},
                        {"Android_Build", "buildAndroid"}, {"iOS_Debug_Build", "buildIos"}
                };
                for (int i = 0; i < expected.length; i++) {
                    String xml = getText(entries, ".idea/runConfigurations/" + expected[i][0] + ".xml");
                    assertContains(xml, "type=\"GradleRunConfiguration\"", label + "IntelliJ Gradle run configuration");
                    assertContains(xml, "<option value=\"" + expected[i][1] + "\" />", label + "IntelliJ task " + expected[i][1]);
                }
            }
        } else if (ide == IDE.VS_CODE) {
            String tasks = getText(entries, ".vscode/tasks.json");
            assertContains(tasks, "\"command\": \"./gradlew\"", label + "VS Code tasks run the wrapper");
            assertContains(tasks, type == ProjectOptions.ProjectType.BACKEND_ONLY ? "[\"runBackend\"]" : "[\"run\"]",
                    label + "VS Code should have a run task");
            assertContains(getText(entries, ".vscode/extensions.json"), "vscjava.vscode-gradle",
                    label + "VS Code should recommend the Gradle extension");
            assertNull(entries.get(".vscode/settings.json"), label + "Maven favorites must not ship with Gradle");
        } else if (ide == IDE.ECLIPSE) {
            for (String path : entries.keySet()) {
                assertFalse(path.endsWith(".launch"), label + "Maven launch files must not ship with Gradle: " + path);
            }
            assertContains(readme, "Existing Gradle Project", label + "README should explain the Eclipse import");
        } else {
            assertNull(entries.get("nbactions.xml"), label + "Maven actions must not ship with Gradle");
            assertNull(entries.get("nb-configuration.xml"), label + "Maven configuration must not ship with Gradle");
            assertContains(readme, "File > Open Project", label + "README should explain the NetBeans import");
        }
    }

    private void validateGradleRefusesUnsupportedCombinations() throws Exception {
        ProjectOptions java8 = new ProjectOptions(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.DEFAULT,
                true, false, ProjectOptions.PreviewLanguage.ENGLISH, ProjectOptions.JavaVersion.JAVA_8, null,
                ProjectOptions.BuildTool.GRADLE, ProjectOptions.ProjectType.APP);
        assertRefused(GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, "NoJava8", "com.acme.nojava8", java8),
                "Java 17", "Gradle projects must refuse Java 8");
        for (Template template : Template.values()) {
            if (template.supportsGradle()) {
                continue;
            }
            assertRefused(GeneratorModel.create(IDE.INTELLIJ, template, "NoGradle", "com.acme.nogradle",
                    gradleOptions(ProjectOptions.ProjectType.APP)), template.GRADLE_UNSUPPORTED_REASON,
                    template + " cannot be generated for Gradle and must say why");
        }
        assertRefused(GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, "NoMavenBackend", "com.acme.nomavenbackend",
                ProjectOptions.defaults().withBuild(ProjectOptions.BuildTool.MAVEN, ProjectOptions.ProjectType.BACKEND_ONLY)),
                "backend-only", "Maven has no backend-only scaffold");
    }

    private void assertRefused(GeneratorModel model, String expectedReason, String message) {
        String reason = null;
        try {
            model.collectProjectEntries();
        } catch (IOException expected) {
            reason = expected.getMessage();
        }
        assertNotNull(reason, message);
        assertContains(reason, expectedReason, message);
    }

    /// Maven projects always carry the backend module, so "App" and "App + backend"
    /// are the same Maven download.
    private void validateMavenIgnoresAppProjectType() throws Exception {
        Map<String, byte[]> app = GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, "SameApp", "com.acme.same",
                ProjectOptions.defaults()).collectProjectEntries();
        Map<String, byte[]> withBackend = GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, "SameApp", "com.acme.same",
                ProjectOptions.defaults().withBuild(ProjectOptions.BuildTool.MAVEN,
                        ProjectOptions.ProjectType.APP_WITH_BACKEND)).collectProjectEntries();
        assertEqual(app.keySet(), withBackend.keySet(), "Maven App and App + backend should be the same download");
        assertNotNull(app.get("backend/pom.xml"), "Maven projects keep the backend module");
    }

    /// No template that has cn1libs generates for Gradle yet (see Template), so the
    /// translation from the template's pom to build.gradle.kts is checked directly.
    private void validateGradleTemplateDependencies() {
        String script = GeneratorModel.create(IDE.INTELLIJ, Template.TWEET, "LibsApp", "com.acme.libs",
                gradleOptions(ProjectOptions.ProjectType.APP)).gradleAppBuildScript("// c\ndependencies {\n}\n");
        assertContains(script, "dependencies {\n    cn1lib(\"com.codenameone:coderad-lib:2.0.5\")",
                "Template cn1libs should become cn1lib(...) lines inside dependencies {}");
        assertContains(script, "cn1lib(\"com.codenameone:tweet-app-ui-kit-lib:1.0-pre1\")",
                "Every template cn1lib should be declared");
        assertContains(script, "annotationProcessor(\"com.codenameone:coderad-annotation-processor:2.0.5\")",
                "The template's annotation processor should be declared");
        String bare = GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, "LibsApp", "com.acme.libs",
                gradleOptions(ProjectOptions.ProjectType.APP)).gradleAppBuildScript("dependencies {\n}\n");
        assertEqual("dependencies {\n}\n", bare, "A template with no libraries leaves the build script alone");
    }

    /// AGENTS.md is the one skill file that states commands outright, so it comes in
    /// a Maven and a Gradle variant; the SKILL.md body covers both layouts itself.
    private void validateAgentsMdMatchesBuildTool() throws Exception {
        Map<String, byte[]> gradle = readZipEntries(createProjectZip(IDE.INTELLIJ, Template.BAREBONES, "AgentsGradle",
                "com.acme.agents", gradleOptions(ProjectOptions.ProjectType.APP)));
        String gradleAgents = getText(gradle, "AGENTS.md");
        assertFalse(gradleAgents.indexOf("mvn ") >= 0, "A Gradle project's AGENTS.md must not give Maven commands");
        assertFalse(gradleAgents.indexOf("common/src") >= 0, "A Gradle project's AGENTS.md must not use Maven paths");
        assertContains(gradleAgents, "./gradlew run", "A Gradle project's AGENTS.md should say how to run it");
        assertContains(gradleAgents, ".agent-skills/codename-one/SKILL.md", "AGENTS.md should point at the skill");

        Map<String, byte[]> maven = readZipEntries(createProjectZip(IDE.INTELLIJ, Template.BAREBONES, "AgentsMaven",
                "com.acme.agents"));
        String mavenAgents = getText(maven, "AGENTS.md");
        assertContains(mavenAgents, "mvn -pl common cn1:run", "A Maven project's AGENTS.md keeps its Maven commands");
        assertFalse(mavenAgents.indexOf("gradlew") >= 0, "A Maven project's AGENTS.md must not give Gradle commands");

        String skill = getText(gradle, ".agent-skills/codename-one/SKILL.md");
        assertContains(skill, "settings.gradle.kts", "SKILL.md should teach an agent to recognise the Gradle layout");
    }

    private void validateGradleLocalization() throws Exception {
        ProjectOptions options = new ProjectOptions(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.DEFAULT,
                true, true, ProjectOptions.PreviewLanguage.ENGLISH, ProjectOptions.JavaVersion.JAVA_17, null,
                ProjectOptions.BuildTool.GRADLE, ProjectOptions.ProjectType.APP);
        for (Template template : new Template[] {Template.BAREBONES, Template.KOTLIN}) {
            Map<String, byte[]> entries = readZipEntries(createProjectZip(IDE.INTELLIJ, template, "L10nApp",
                    "com.acme.l10n", options));
            assertNotNull(entries.get("src/main/l10n/messages.properties"),
                    "Gradle bundles belong in src/main/l10n, where the CSS compiler bakes them into theme.res");
            assertNotNull(entries.get("src/main/l10n/messages_he.properties"), "Gradle should ship every bundle");
            String main = getText(entries, template.IS_KOTLIN
                    ? "src/main/kotlin/com/acme/l10n/L10nApp.kt" : "src/main/java/com/acme/l10n/L10nApp.java");
            assertContains(main, "setBundle", "The Gradle starter should install the localization bundle too");
        }
    }

    /// The Unix mode ProjectZipPermissions stored for `name`, read from the ZIP's
    /// central directory (zipme does not expose external attributes).
    private static int unixMode(byte[] zip, String name) throws IOException {
        int end = zip.length - 22;
        int count = (zip[end + 10] & 255) | ((zip[end + 11] & 255) << 8);
        int offset = readInt(zip, end + 16);
        for (int i = 0; i < count; i++) {
            int nameLength = (zip[offset + 28] & 255) | ((zip[offset + 29] & 255) << 8);
            int extra = (zip[offset + 30] & 255) | ((zip[offset + 31] & 255) << 8);
            int comment = (zip[offset + 32] & 255) | ((zip[offset + 33] & 255) << 8);
            String entryName = new String(zip, offset + 46, nameLength, "UTF-8");
            if (entryName.equals(name)) {
                return readInt(zip, offset + 38) >>> 16;
            }
            offset += 46 + nameLength + extra + comment;
        }
        throw new IOException("No entry " + name);
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 255) | ((data[offset + 1] & 255) << 8)
                | ((data[offset + 2] & 255) << 16) | ((data[offset + 3] & 255) << 24);
    }

    private void validateAppendedCustomCssGeneration() throws Exception {
        String mainClassName = "DemoAdvancedTheme";
        String packageName = "com.acme.advanced.theme";
        String customCss = "Button {\n    border-radius: 0;\n}\n";
        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.BLUE,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_8,
                customCss
        );

        byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName, options);
        Map<String, byte[]> entries = readZipEntries(zipData);

        String themeCss = getText(entries, "common/src/main/css/theme.css");
        assertContains(themeCss, "Initializr Theme Overrides", "Theme CSS should include generated theme overrides marker");
        assertContains(themeCss, "background-color: #1d4ed8", "Theme CSS should preserve selected accent overrides when custom CSS is appended");
        assertContains(themeCss, "Initializr Appended Custom CSS", "Theme CSS should include appended custom CSS marker");
        assertContains(themeCss, "border-radius: 0", "Theme CSS should include custom advanced CSS");
    }

    private void validateCustomCssWithoutPresetOverrides() throws Exception {
        String mainClassName = "DemoCustomOnly";
        String packageName = "com.acme.custom.only";
        String customCss = "Button {\n    border-radius: 0;\n}\n";
        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.DEFAULT,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_8,
                customCss
        );

        byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName, options);
        Map<String, byte[]> entries = readZipEntries(zipData);

        String themeCss = getText(entries, "common/src/main/css/theme.css");
        assertFalse(themeCss.indexOf("Initializr Theme Overrides") >= 0, "Theme CSS should not add preset overrides for light/default mode");
        assertContains(themeCss, "Initializr Appended Custom CSS", "Theme CSS should include custom CSS marker in custom-only mode");
        assertContains(themeCss, "border-radius: 0", "Theme CSS should include custom CSS in custom-only mode");
    }

    private void validateThemeCssCompilesForAllVariants() {
        for (ProjectOptions.ThemeMode mode : ProjectOptions.ThemeMode.values()) {
            for (ProjectOptions.Accent accent : ProjectOptions.Accent.values()) {
                validateThemeCssCompiles(new ProjectOptions(
                        mode,
                        accent,
                        true,
                        true,
                        ProjectOptions.PreviewLanguage.ENGLISH,
                        ProjectOptions.JavaVersion.JAVA_8
                ));
                validateThemeCssCompiles(new ProjectOptions(
                        mode,
                        accent,
                        false,
                        true,
                        ProjectOptions.PreviewLanguage.ENGLISH,
                        ProjectOptions.JavaVersion.JAVA_8,
                        "Button { border-radius: 0; }"
                ));
            }
        }
    }

    private void validateThemeCssCompiles(ProjectOptions options) {
        String css = GeneratorModel.buildThemeOverrides(options);
        if (css == null || css.trim().length() == 0) {
            return;
        }
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(css, resource, "CompileCheckTheme");
        assertNotNull(resource.getTheme("CompileCheckTheme"), "Generated CSS should compile to a theme");
    }

    private void validateClaudeSkillBundled() throws Exception {
        // Every generated project ships the Codename One authoring skill under
        // .claude/skills/codename-one/ so that Claude Code in the new project picks up
        // CN1 conventions automatically. Regression: catch accidental deletion of any
        // file in the skill set, and confirm template-token rewriting still applies to
        // markdown bodies (e.g. MyAppName -> the real main class name).
        String mainClassName = "DemoSkillBundle";
        String packageName = "com.acme.skillbundle";
        byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName);
        Map<String, byte[]> entries = readZipEntries(zipData);

        String[] expectedSkillEntries = new String[] {
                ".agent-skills/codename-one/SKILL.md",
                ".agent-skills/codename-one/references/build-and-run.md",
                ".agent-skills/codename-one/references/build-hints.md",
                ".agent-skills/codename-one/references/java-api-subset.md",
                ".agent-skills/codename-one/references/ui-components.md",
                ".agent-skills/codename-one/references/css.md",
                ".agent-skills/codename-one/references/swing-comparison.md",
                ".agent-skills/codename-one/references/html-css-cheatsheet.md",
                ".agent-skills/codename-one/references/android-to-cn1.md",
                ".agent-skills/codename-one/references/testing-and-screenshots.md",
                ".agent-skills/codename-one/references/mobile-adaptability.md",
                ".agent-skills/codename-one/references/native-interfaces.md",
                ".agent-skills/codename-one/references/cn1libs.md",
                ".agent-skills/codename-one/references/snapshot-builds.md",
                ".agent-skills/codename-one/references/debugging.md",
                ".agent-skills/codename-one/references/mcp-agent-control.md",
                ".agent-skills/codename-one/references/on-device-debugging.md",
                ".agent-skills/codename-one/references/ai-and-speech.md",
                ".agent-skills/codename-one/tools/README.md",
                ".agent-skills/codename-one/tools/IsApiSupported.java",
                ".agent-skills/codename-one/tools/IsCssValid.java"
        };
        for (int i = 0; i < expectedSkillEntries.length; i++) {
            assertNotNull(entries.get(expectedSkillEntries[i]),
                    "Skill bundle missing: " + expectedSkillEntries[i]);
        }

        String skillMd = getText(entries, ".agent-skills/codename-one/SKILL.md");
        assertContains(skillMd, "name: codename-one", "SKILL.md should preserve frontmatter slug");
        assertContains(skillMd, mainClassName + " extends Lifecycle",
                "SKILL.md placeholders should be rewritten to the project's main class");

        String testingMd = getText(entries, ".agent-skills/codename-one/references/testing-and-screenshots.md");
        assertContains(testingMd, mainClassName + "().runApp()",
                "Reference samples should be rewritten with the project's main class");
        assertContains(testingMd, packageName + "." + mainClassName,
                "Reference samples should be rewritten with the project's package");

        // AGENTS.md root pointer and Claude Code stub are also bundled so vendor-specific
        // discovery flows still work without forcing every agent to learn our path layout.
        String agentsMd = getText(entries, "AGENTS.md");
        assertContains(agentsMd, ".agent-skills/codename-one/SKILL.md",
                "AGENTS.md should point agents at the canonical skill location");
        // An agent that never learns the running app is drivable will only ever look at
        // screenshots, so the pointer to the MCP loop belongs in the root file too.
        assertContains(agentsMd, "references/mcp-agent-control.md",
                "AGENTS.md should point agents at the MCP control loop");
        // Same reasoning for the device loops: an agent that never learns the Android/iOS
        // build is attachable will give up at "cannot reproduce in the simulator".
        assertContains(agentsMd, "references/on-device-debugging.md",
                "AGENTS.md should point agents at the on-device debug/MCP loops");

        String claudeStub = getText(entries, ".claude/skills/codename-one/SKILL.md");
        assertContains(claudeStub, "name: codename-one", "Claude stub must keep the skill frontmatter");
        assertContains(claudeStub, ".agent-skills/codename-one/SKILL.md",
                "Claude stub should redirect to the canonical skill content");

        // The win/ module is the native win32 target (not the retired UWP module),
        // so Java 17 projects ship it just like Java 8 projects do.
        assertNotNull(entries.get("win/pom.xml"),
                "Java 17 projects should bundle the win32 (win/) module");
        String rootPom = getText(entries, "pom.xml");
        assertContains(rootPom, "<id>win</id>",
                "Java 17 root pom should retain the win32 module activation profile");
        assertCodenameOneRepository(rootPom, "Java 17");

        // The backend module ships in every download but builds only when asked for
        // by -Dcodename1.platform=backend, so a client-only app pays nothing for it.
        assertNotNull(entries.get("backend/pom.xml"),
                "projects should bundle the backend module");
        assertNotNull(entries.get("backend/src/main/java/" + packageName.replace('.', '/') + "/Api.java"),
                "the backend module should ship a working controller, not an empty module");
        assertContains(getText(entries, "backend/src/main/java/"
                        + packageName.replace('.', '/') + "/Api.java"),
                "@RestController",
                "the generated server should be annotated, not a hand-written main");
        assertContains(rootPom, "<id>backend</id>",
                "root pom should carry the backend module activation profile");
        String backendPom = getText(entries, "backend/pom.xml");
        assertContains(backendPom, "<artifactId>codenameone-backend</artifactId>",
                "the backend module should depend on the backend runtime");
        // A server has no display. Depending on the generated common module would drag
        // codenameone-core onto a classpath that cannot run it, which is the mistake the
        // module's own comment warns against -- so assert it is absent rather than trust it.
        assertFalse(backendPom.indexOf("${cn1app.name}-common") >= 0,
                "the backend module must not depend on the generated common module");
    }

    /**
     * Codename One releases are published to repo.codenameone.com, not Maven Central, so
     * a generated project must declare that repository. Losing it is invisible at
     * generation time -- Central still serves every version published before the
     * cutover -- and surfaces only as a project that never sees a new release.
     *
     * Both lists are asserted because Maven resolves dependencies through
     * &lt;repositories&gt; and build plugins through &lt;pluginRepositories&gt;: a
     * project holding only the first downloads codenameone-core and still fails to find
     * a newer codenameone-maven-plugin.
     */
    private void assertCodenameOneRepository(String rootPom, String label) {
        int repositories = rootPom.indexOf("<repositories>");
        int pluginRepositories = rootPom.indexOf("<pluginRepositories>");
        assertTrue(repositories >= 0, label + " root pom should declare <repositories>");
        assertTrue(pluginRepositories > repositories,
                label + " root pom should declare <pluginRepositories> after <repositories>");
        assertContains(rootPom.substring(repositories, pluginRepositories),
                "https://repo.codenameone.com/maven2",
                label + " root pom should resolve dependencies from the Codename One repository");
        assertContains(rootPom.substring(pluginRepositories),
                "https://repo.codenameone.com/maven2",
                label + " root pom should resolve plugins from the Codename One repository");
    }

    private void validateLegacyJava8Generation() throws Exception {
        String mainClassName = "DemoJava8";
        String packageName = "com.acme.java8";
        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.DEFAULT,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_8
        );

        byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName, options);
        Map<String, byte[]> entries = readZipEntries(zipData);

        assertCommonPom(entries, Template.BAREBONES, packageName, mainClassName, false);
        assertSettings(entries, Template.BAREBONES, packageName, mainClassName, false);
        assertMainSourceFile(entries, Template.BAREBONES, packageName, mainClassName, true);
        assertLocalizationBundles(entries, Template.BAREBONES, true);

        String intellijMisc = getText(entries, ".idea/misc.xml");
        assertContains(intellijMisc, "languageLevel=\"JDK_1_8\"", "Java 8 selection should write JDK_1_8 IntelliJ language level");

        // Skill is Java 17-only. Confirm we don't ship it into a Java 8 project,
        // where the skill's "use var / records / text blocks" guidance would be wrong.
        assertNull(entries.get(".agent-skills/codename-one/SKILL.md"),
                "Java 8 projects should not bundle the agent skill (Java 17-only)");
        assertNull(entries.get(".claude/skills/codename-one/SKILL.md"),
                "Java 8 projects should not bundle the Claude Code skill stub (Java 17-only)");
        assertNull(entries.get("AGENTS.md"),
                "Java 8 projects should not bundle the AGENTS.md root pointer (Java 17-only)");

        // Both Java 8 and Java 17 projects ship the native win32 (win/) module.
        assertNotNull(entries.get("win/pom.xml"),
                "Java 8 projects should retain the win32 (win/) module");
        String rootPom = getText(entries, "pom.xml");
        assertContains(rootPom, "<id>win</id>",
                "Java 8 root pom should retain the win32 module activation profile");
        assertCodenameOneRepository(rootPom, "Java 8");
    }

    private void validateCoordinateGuardRejectsBrokenArtifacts() throws Exception {
        String mainClassName = "CoordinateGuardApp";
        String packageName = "com.acme.coordinate.guard";
        GeneratorModel model = GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName);
        Map<String, byte[]> entries = model.collectProjectEntries();
        String javascriptPom = getText(entries, "javascript/pom.xml");

        entries.put("javascript/pom.xml", StringUtil.replaceAll(
                javascriptPom,
                packageName,
                "com.codename1.initializr"
        ).getBytes("UTF-8"));
        assertCoordinateGuardRejects(model, entries,
                "Initializr application coordinates must never leak into a generated platform POM");

        entries.put("javascript/pom.xml", StringUtil.replaceAll(
                javascriptPom,
                "1.0-SNAPSHOT",
                "9.9-BROKEN"
        ).getBytes("UTF-8"));
        assertCoordinateGuardRejects(model, entries,
                "A platform POM version that differs from the generated reactor must be rejected");
    }

    private void validateLocaleIndependentArtifactIds() throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            String mainClassName = "IntegrityIndex";
            String packageName = "com.acme.locale.guard";
            byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName);
            Map<String, byte[]> entries = readZipEntries(zipData);
            assertGeneratedPomCoordinates(entries, packageName, mainClassName);
            assertContains(getText(entries, "javascript/pom.xml"),
                    "<artifactId>integrityindex-javascript</artifactId>",
                    "Artifact IDs must use locale-independent lowercase under a Turkish default locale");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private void assertCoordinateGuardRejects(
            GeneratorModel model,
            Map<String, byte[]> entries,
            String message
    ) throws Exception {
        boolean rejected = false;
        try {
            model.validateGeneratedPomCoordinates(entries);
        } catch (IOException expected) {
            rejected = true;
        }
        assertTrue(rejected, message);
    }

    private void validateJava17DefaultRegressionFixes() throws Exception {
        String mainClassName = "DemoJava17Regression";
        String packageName = "com.acme.java17regression";
        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.DEFAULT,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_17
        );

        byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName, options);
        Map<String, byte[]> entries = readZipEntries(zipData);

        String intellijMisc = getText(entries, ".idea/misc.xml");
        assertContains(intellijMisc, "languageLevel=\"JDK_17\"", "IntelliJ misc.xml should use Java 17 language level for Java 17 projects");
        assertFalse(intellijMisc.indexOf("project-jdk-name=") >= 0, "IntelliJ misc.xml should not pin a specific JDK name");
        assertFalse(intellijMisc.indexOf("project-jdk-type=") >= 0, "IntelliJ misc.xml should not pin a specific JDK type");

        String intellijWorkspace = getText(entries, ".idea/workspace.xml");
        assertContains(intellijWorkspace, "<configuration name=\"Run in Simulator\"", "IntelliJ workspace should include Run in Simulator profile");
        assertContains(intellijWorkspace, "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
                "Run in Simulator profile should export com.apple.eawt for JDK 17+");

        String androidPom = getText(entries, "android/pom.xml");
        assertContains(androidPom, "<artifactId>maven-jar-plugin</artifactId>", "Android module should configure maven-jar-plugin explicitly");
        assertContains(androidPom, "<version>3.4.1</version>", "Android module should pin maven-jar-plugin version");
        assertContains(androidPom, "<id>default-jar</id>", "Android module should target default-jar execution");
        assertContains(androidPom, "<phase>none</phase>", "Android module should disable default-jar execution to avoid duplicate attach in cn1:build");

        String iosPom = getText(entries, "ios/pom.xml");
        assertContains(iosPom, "<artifactId>maven-jar-plugin</artifactId>", "iOS module should configure maven-jar-plugin explicitly");
        assertContains(iosPom, "<version>3.4.1</version>", "iOS module should pin maven-jar-plugin version");
        assertContains(iosPom, "<id>default-jar</id>", "iOS module should target default-jar execution");
        assertContains(iosPom, "<phase>none</phase>", "iOS module should disable default-jar execution to avoid duplicate attach in cn1:build");

        String javascriptPom = getText(entries, "javascript/pom.xml");
        assertContains(javascriptPom, "<artifactId>maven-jar-plugin</artifactId>", "JavaScript module should configure maven-jar-plugin explicitly");
        assertContains(javascriptPom, "<version>3.4.1</version>", "JavaScript module should pin maven-jar-plugin version");
        assertContains(javascriptPom, "<id>default-jar</id>", "JavaScript module should target default-jar execution");
        assertContains(javascriptPom, "<phase>none</phase>", "JavaScript module should disable default-jar execution to avoid duplicate attach in cn1:build");
    }

    private void validateCombination(Template template, IDE ide) throws Exception {
        String mainClassName = "Demo" + template.ordinal() + ide.ordinal() + "App";
        String packageName = "com.acme.t" + template.ordinal() + ".i" + ide.ordinal();

        byte[] zipData = createProjectZip(ide, template, mainClassName, packageName);
        Map<String, byte[]> entries = readZipEntries(zipData);

        assertIdeFiles(ide, entries, mainClassName);
        assertGitIgnore(entries);
        assertRootPom(entries, packageName, mainClassName);
        assertCommonPom(entries, template, packageName, mainClassName, true);
        assertGeneratedPomCoordinates(entries, packageName, mainClassName);
        assertSettings(entries, template, packageName, mainClassName, true);
        assertMainSourceFile(entries, template, packageName, mainClassName, false);
        assertThemeDefaults(entries, template);
        assertLocalizationBundles(entries, template, false);
        assertNoTemplatePlaceholders(entries, template);
    }

    private void assertThemeDefaults(Map<String, byte[]> entries, Template template) {
        if (template != Template.BAREBONES && template != Template.KOTLIN) {
            return;
        }
        String themeCss = getText(entries, "common/src/main/css/theme.css");
        assertContains(themeCss, "useLargerTextScaleBool: true;", "Barebones templates should default useLargerTextScaleBool to true");
        assertContains(themeCss, "@media (prefers-color-scheme: dark)", "Default theme should adapt to dark mode out of the box");
        assertFalse(themeCss.indexOf("Initializr Theme Overrides") >= 0, "Default theme must not carry baked-in top-level theme overrides");
    }

    private static byte[] createProjectZip(IDE ide, Template template, String appName, String packageName) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeneratorModel.create(ide, template, appName, packageName).writeProjectZip(output);
        return output.toByteArray();
    }

    private static byte[] createProjectZip(IDE ide, Template template, String appName, String packageName, ProjectOptions options) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeneratorModel.create(ide, template, appName, packageName, options).writeProjectZip(output);
        return output.toByteArray();
    }

    private static Map<String, byte[]> readZipEntries(byte[] zipData) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ByteArrayInputStream input = new ByteArrayInputStream(zipData);
        ZipInputStream zis = new ZipInputStream(input);
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    Util.copyNoClose(zis, bos, 8192);
                    entries.put(entry.getName(), bos.toByteArray());
                    bos.close();
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } finally {
            zis.close();
            input.close();
        }
        return entries;
    }

    private void assertIdeFiles(IDE ide, Map<String, byte[]> entries, String mainClassName) {
        String runSh = getText(entries, "run.sh");
        assertContains(runSh, "certificatewizard",
                "Generated shell launcher should include Certificate Wizard command");
        assertContains(runSh, "cn1:certificatewizard",
                "Generated shell launcher should launch cn1:certificatewizard");

        String runBat = getText(entries, "run.bat");
        assertContains(runBat, "certificatewizard",
                "Generated Windows launcher should include Certificate Wizard command");
        assertContains(runBat, "cn1:certificatewizard",
                "Generated Windows launcher should launch cn1:certificatewizard");

        String readme = getText(entries, "README.md");
        assertContains(readme, "mvn cn1:certificatewizard",
                "Generated README should document the Certificate Wizard");

        String buildSh = getText(entries, "build.sh");
        assertContains(buildSh, "-Dcodename1.buildTarget=local-javascript",
                "Generated shell build launcher should build JavaScript locally");
        assertContains(buildSh, "function javascript_cloud",
                "Generated shell build launcher should retain the cloud JavaScript target");

        String buildBat = getText(entries, "build.bat");
        assertContains(buildBat, "-Dcodename1.buildTarget^=local-javascript",
                "Generated Windows build launcher should build JavaScript locally");
        assertContains(buildBat, ":javascript_cloud",
                "Generated Windows build launcher should retain the cloud JavaScript target");

        String javascriptPom = getText(entries, "javascript/pom.xml");
        assertContains(javascriptPom, "<codename1.defaultBuildTarget>local-javascript</codename1.defaultBuildTarget>",
                "Generated JavaScript module should default to a local build");

        if (ide == IDE.INTELLIJ) {
            String workspaceXml = getText(entries, ".idea/workspace.xml");
            assertContains(workspaceXml, "<configuration name=\"Certificate Wizard\"",
                    "IntelliJ workspace should include Certificate Wizard run configuration");
            assertContains(workspaceXml, "<option value=\"cn1:certificatewizard\"",
                    "IntelliJ Certificate Wizard run configuration should launch cn1:certificatewizard");
            assertContains(workspaceXml, "<configuration name=\"JavaScript Local Build\"",
                    "IntelliJ workspace should include a local JavaScript build configuration");
            assertContains(workspaceXml, "value=\"local-javascript\"",
                    "IntelliJ local JavaScript configuration should use the local builder");
            return;
        }
        if (ide == IDE.ECLIPSE) {
            assertNotNull(entries.get(mainClassName + " - Run Simulator.launch"), "Missing Eclipse launch file");
            String launchXml = getText(entries, mainClassName + " - Certificate Wizard.launch");
            assertContains(launchXml, "cn1:certificatewizard",
                    "Eclipse launch files should include Certificate Wizard");
            String localJavaScriptLaunch = getText(entries, mainClassName + " - Build Javascript Locally.launch");
            assertContains(localJavaScriptLaunch, "codename1.buildTarget=local-javascript",
                    "Eclipse launch files should include a local JavaScript build");
            return;
        }
        if (ide == IDE.NETBEANS) {
            String nbActions = getText(entries, "nbactions.xml");
            assertContains(nbActions, "CUSTOM-Open Certificate Wizard",
                    "NetBeans actions should include Certificate Wizard");
            assertContains(nbActions, "<goal>cn1:certificatewizard</goal>",
                    "NetBeans Certificate Wizard action should launch cn1:certificatewizard");
            String nbConfiguration = getText(entries, "nb-configuration.xml");
            assertContains(nbConfiguration, "<configuration id=\"Local JavaScript App\"",
                    "NetBeans configurations should include a local JavaScript build");
            assertContains(nbConfiguration, "<property name=\"codename1.buildTarget\">local-javascript</property>",
                    "NetBeans local JavaScript configuration should use the local builder");
            return;
        }
        if (ide == IDE.VS_CODE) {
            String settingsJson = getText(entries, ".vscode/settings.json");
            assertContains(settingsJson, "Tools > Certificate Wizard",
                    "VS Code Maven favorites should include Certificate Wizard");
            assertContains(settingsJson, "cn1:certificatewizard",
                    "VS Code Certificate Wizard favorite should launch cn1:certificatewizard");
            assertContains(settingsJson, "Local > JavaScript Build",
                    "VS Code Maven favorites should include a local JavaScript build");
            assertContains(settingsJson, "-Dcodename1.buildTarget=local-javascript",
                    "VS Code local JavaScript favorite should use the local builder");
        }
    }

    private void assertRootPom(Map<String, byte[]> entries, String packageName, String mainClassName) {
        String pom = getText(entries, "pom.xml");
        assertContains(pom, packageName, "Root pom should include package as groupId");
        assertContains(pom, GeneratorModel.toLowerCaseInvariant(mainClassName), "Root pom should include app artifact/name");
        assertContains(pom, "<cn1.plugin.version>7.0.273</cn1.plugin.version>", "Root pom should use current CN1 plugin version");
        assertContains(pom, "<cn1.version>7.0.273</cn1.version>", "Root pom should align CN1 runtime version with plugin version");
        assertFalse(pom.indexOf("com.example.myapp") >= 0, "Root pom still contains placeholder package");
        assertFalse(pom.indexOf("myappname") >= 0, "Root pom still contains placeholder app name");
    }

    private void assertCommonPom(Map<String, byte[]> entries, Template template, String packageName, String mainClassName, boolean expectJava17) {
        String pom = getText(entries, "common/pom.xml");
        assertContains(pom, packageName, "Common pom should include package");
        assertContains(pom, GeneratorModel.toLowerCaseInvariant(mainClassName), "Common pom should include app artifact");
        assertContains(pom, "<artifactId>codenameone-javase</artifactId>", "Common pom should include codenameone-javase test dependency");
        assertContains(pom, "<artifactId>serializer</artifactId>", "Common pom should include xalan serializer for CN1 generate-gui-sources");
        assertContains(pom, "<version>2.7.3</version>", "Common pom should pin serializer version expected by CN1 plugin classpath");
        if (expectJava17) {
            assertContains(pom, "<source>17</source>", "Common pom should default to Java 17 source");
            assertContains(pom, "<target>17</target>", "Common pom should default to Java 17 target");
        } else {
            assertContains(pom, "<source>1.8</source>", "Common pom should use Java 8 source when legacy Java 8 is selected");
            assertContains(pom, "<target>1.8</target>", "Common pom should use Java 8 target when legacy Java 8 is selected");
        }
        if (template == Template.GRUB) {
            assertContains(pom, "<artifactId>" + GeneratorModel.toLowerCaseInvariant(mainClassName) + "-CodeRAD</artifactId>", "Grub common pom should include local CodeRAD cn1lib dependency");
            assertContains(pom, "<version>1.0-SNAPSHOT</version>", "Grub common pom should use local snapshot CodeRAD cn1lib");
        }
        if (template == Template.TWEET) {
            assertContains(pom, "tweet-app-ui-kit-lib", "Tweet common pom should include Tweet UI Kit dependency");
            assertContains(pom, "<artifactId>coderad-annotation-processor</artifactId>", "Tweet common pom should include CodeRAD annotation processor path");
            assertContains(pom, "<annotationProcessorPaths>", "Tweet common pom should configure annotation processors");
        }
        assertFalse(pom.indexOf("com.example.myapp") >= 0, "Common pom still contains placeholder package");
        assertFalse(pom.indexOf("myappname") >= 0, "Common pom still contains placeholder app name");
    }

    private void assertGeneratedPomCoordinates(Map<String, byte[]> entries, String packageName, String mainClassName) {
        String rootArtifactId = GeneratorModel.toLowerCaseInvariant(mainClassName);
        String version = "1.0-SNAPSHOT";
        String[] modules = new String[] {"common", "android", "ios", "javase", "javascript", "linux", "win"};

        for (int i = 0; i < modules.length; i++) {
            String module = modules[i];
            String pom = removeWhitespace(getText(entries, module + "/pom.xml"));
            String expectedCoordinates =
                    "</modelVersion><parent>"
                            + "<groupId>" + packageName + "</groupId>"
                            + "<artifactId>" + rootArtifactId + "</artifactId>"
                            + "<version>" + version + "</version>"
                            + "</parent>"
                            + "<groupId>" + packageName + "</groupId>"
                            + "<artifactId>" + rootArtifactId + "-" + module + "</artifactId>"
                            + "<version>" + version + "</version>";
            assertContains(pom, expectedCoordinates,
                    module + "/pom.xml must belong to the generated Maven reactor");
            assertFalse(pom.indexOf("com.codename1.initializr") >= 0,
                    module + "/pom.xml must not retain the Initializr application's groupId");
            assertFalse(pom.indexOf("<artifactId>initializr") >= 0,
                    module + "/pom.xml must not retain an Initializr application artifactId");
        }
    }

    private String removeWhitespace(String content) {
        StringBuilder out = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!Character.isWhitespace(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void assertSettings(Map<String, byte[]> entries, Template template, String packageName, String mainClassName, boolean expectJava17) {
        String settings = getText(entries, "common/codenameone_settings.properties");
        assertContains(settings, "codename1.packageName=" + packageName, "Settings should include requested package");
        assertContains(settings, "codename1.mainName=" + mainClassName, "Settings should include requested main class");
        assertContains(settings, "codename1.displayName=" + mainClassName, "Settings should include requested display name");
        assertContains(settings, "codename1.kotlin=" + String.valueOf(template.IS_KOTLIN), "Settings should include template kotlin flag");
        if (expectJava17) {
            assertContains(settings, "codename1.arg.java.version=17", "Settings should include Java 17 version by default");
        } else {
            assertFalse(settings.indexOf("codename1.arg.java.version=17") >= 0, "Settings should not force Java 17 when legacy Java 8 was selected");
        }
    }

    private void assertMainSourceFile(Map<String, byte[]> entries, Template template, String packageName, String mainClassName, boolean expectLocalizationBundles) {
        String packagePath = StringUtil.replaceAll(packageName, ".", "/");
        String path;
        if (template.IS_KOTLIN) {
            path = "common/src/main/kotlin/" + packagePath + "/" + mainClassName + ".kt";
        } else {
            path = "common/src/main/java/" + packagePath + "/" + mainClassName + ".java";
        }
        String mainSource = getText(entries, path);
        assertContains(mainSource, "package " + packageName, "Main source package was not refactored");
        assertContains(mainSource, mainClassName, "Main source class was not renamed");
        if (template == Template.BAREBONES || template == Template.KOTLIN) {
            if (expectLocalizationBundles) {
                assertContains(mainSource, "setBundle", "Barebones starter should install localization bundle");
                assertContains(mainSource, "messages", "Barebones starter should load i18n messages properties");
                if (template == Template.BAREBONES) {
                    assertContains(mainSource, "super.init(context);", "Java starter should call Lifecycle init before localization bootstrap");
                } else {
                    assertContains(mainSource, "super.init(context)", "Kotlin starter should call Lifecycle init before localization bootstrap");
                }
            } else {
                assertFalse(mainSource.indexOf("setBundle") >= 0, "Barebones starter should not install localization bundle by default");
            }
        }
        if (template == Template.GRUB) {
            String grubModel = getText(entries, "common/src/main/java/" + packagePath + "/models/AccountModel.java");
            assertContains(grubModel, "extends Entity", "Grub models should keep CodeRAD 1 Entity base class");
            assertFalse(grubModel.indexOf("extends BaseEntity") >= 0, "Grub models should not be rewritten to BaseEntity");
            assertNotNull(entries.get("cn1libs/pom.xml"), "Grub should include cn1libs parent module");
            assertNotNull(entries.get("cn1libs/CodeRAD/pom.xml"), "Grub should include bundled CodeRAD cn1lib pom");
            assertNotNull(entries.get("cn1libs/CodeRAD/jars/main.zip"), "Grub should include bundled CodeRAD common jar");
            assertNotNull(entries.get("cn1libs/CodeRAD/jars/css.zip"), "Grub should include bundled CodeRAD css artifact");
        }
    }


    private void assertLocalizationBundles(Map<String, byte[]> entries, Template template, boolean expectLocalizationBundles) {
        // Bundles MUST live under common/src/main/l10n -- that's where the CN1 maven plugin's
        // CSS compiler scans for properties files to bake into theme.res. Placing them under
        // src/main/resources causes Resources.getGlobalResources().getL10N("messages", lang)
        // to return null at runtime (see CompileCSSMojo.findLocalizationDirectory).
        if (template == Template.BAREBONES || template == Template.KOTLIN) {
            if (expectLocalizationBundles) {
                assertNotNull(entries.get("common/src/main/l10n/messages.properties"), "Barebones templates should include default localization bundle under l10n");
                assertNotNull(entries.get("common/src/main/l10n/messages_ar.properties"), "Barebones templates should include Arabic localization bundle under l10n");
                assertNotNull(entries.get("common/src/main/l10n/messages_he.properties"), "Barebones templates should include Hebrew localization bundle under l10n");
                assertNull(entries.get("common/src/main/resources/messages.properties"), "Bundles must not be written to src/main/resources -- the CN1 plugin will not bake them into theme.res");
                assertNull(entries.get("common/src/main/resources/messages_ar.properties"), "Bundles must not be written to src/main/resources -- the CN1 plugin will not bake them into theme.res");
                assertNull(entries.get("common/src/main/resources/messages_he.properties"), "Bundles must not be written to src/main/resources -- the CN1 plugin will not bake them into theme.res");
            } else {
                assertNull(entries.get("common/src/main/l10n/messages.properties"), "Barebones templates should not include localization bundles by default");
                assertNull(entries.get("common/src/main/l10n/messages_ar.properties"), "Barebones templates should not include Arabic localization bundle by default");
                assertNull(entries.get("common/src/main/l10n/messages_he.properties"), "Barebones templates should not include Hebrew localization bundle by default");
                assertNull(entries.get("common/src/main/resources/messages.properties"), "Barebones templates should not include localization bundles by default");
            }
            return;
        }
        assertNull(entries.get("common/src/main/l10n/messages.properties"), "Non-bare templates should not receive default localization bundle");
        assertNull(entries.get("common/src/main/resources/messages.properties"), "Non-bare templates should not receive default localization bundle");
    }

    private void assertNoTemplatePlaceholders(Map<String, byte[]> entries, Template template) {
        for (String path : entries.keySet()) {
            assertFalse(path.indexOf("com/example/myapp") >= 0, "Unrefactored placeholder path found: " + path);
            if (template == Template.GRUB) {
                assertFalse(path.indexOf("com/codename1/demos/grub") >= 0, "Unrefactored grub path found: " + path);
            }
        }
        String javasePom = getText(entries, "javase/pom.xml");
        // The top-level provided codenameone-core/codenameone-javase blocks are stripped by
        // GeneratorModel.normalizeJavasePom. Match the exact removed shape so we don't get
        // false positives from the legitimate nested <scope>provided</scope> blocks in the
        // build server activation profile further down the same pom.
        String topLevelProvidedCore =
                "<dependency>\n" +
                "          <groupId>com.codenameone</groupId>\n" +
                "          <artifactId>codenameone-core</artifactId>\n" +
                "          <scope>provided</scope>\n" +
                "      </dependency>";
        String topLevelProvidedJavase =
                "<dependency>\n" +
                "          <groupId>com.codenameone</groupId>\n" +
                "          <artifactId>codenameone-javase</artifactId>\n" +
                "          <scope>provided</scope>\n" +
                "      </dependency>";
        assertFalse(javasePom.indexOf(topLevelProvidedCore) >= 0,
                "javase/pom.xml should not contain duplicate provided codenameone-core dependency");
        assertFalse(javasePom.indexOf(topLevelProvidedJavase) >= 0,
                "javase/pom.xml should not contain duplicate provided codenameone-javase dependency");
    }

    private String getText(Map<String, byte[]> entries, String path) {
        byte[] data = entries.get(path);
        assertNotNull(data, "Missing expected entry: " + path);
        return StringUtil.newString(data);
    }

    private void assertContains(String content, String expected, String message) {
        assertTrue(content.indexOf(expected) >= 0, message + " | expected: " + expected);
    }

    private void assertGitIgnore(Map<String, byte[]> entries) {
        String gitIgnore = getText(entries, ".gitignore");
        assertContains(gitIgnore, "**/target/", "Generated project should ignore Maven targets");
        assertContains(gitIgnore, ".idea/", "Generated project should ignore IntelliJ metadata");
        assertContains(gitIgnore, "*.iml", "Generated project should ignore IntelliJ module files");
    }
}
