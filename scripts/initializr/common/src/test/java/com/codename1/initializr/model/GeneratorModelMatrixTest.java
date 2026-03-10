package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.util.StringUtil;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GeneratorModelMatrixTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        for (Template template : Template.values()) {
            for (IDE ide : IDE.values()) {
                validateCombination(template, ide);
            }
        }
        validateExperimentalJava17Generation();
        validateExperimentalJava17RegressionFixes();
        validateAdvancedThemeCssGeneration();
        return true;
    }



    private void validateAdvancedThemeCssGeneration() throws Exception {
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
        assertContains(themeCss, "Initializr Advanced Theme Overrides", "Theme CSS should include advanced mode marker");
        assertContains(themeCss, "border-radius: 0", "Theme CSS should include custom advanced CSS");
    }

    private void validateExperimentalJava17Generation() throws Exception {
        String mainClassName = "DemoExperimentalJava17";
        String packageName = "com.acme.experimental.java17";
        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.DEFAULT,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_17_EXPERIMENTAL
        );

        byte[] zipData = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, mainClassName, packageName, options);
        Map<String, byte[]> entries = readZipEntries(zipData);

        assertCommonPom(entries, Template.BAREBONES, packageName, mainClassName, true);
        assertSettings(entries, Template.BAREBONES, packageName, mainClassName, true);
        assertMainSourceFile(entries, Template.BAREBONES, packageName, mainClassName, true);
        assertLocalizationBundles(entries, Template.BAREBONES, true);
    }

    private void validateExperimentalJava17RegressionFixes() throws Exception {
        String mainClassName = "DemoExperimentalJava17Regression";
        String packageName = "com.acme.experimental.java17regression";
        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.DEFAULT,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_17_EXPERIMENTAL
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
    }

    private void validateCombination(Template template, IDE ide) throws Exception {
        String mainClassName = "Demo" + template.ordinal() + ide.ordinal() + "App";
        String packageName = "com.acme.t" + template.ordinal() + ".i" + ide.ordinal();

        byte[] zipData = createProjectZip(ide, template, mainClassName, packageName);
        Map<String, byte[]> entries = readZipEntries(zipData);

        assertIdeFiles(ide, entries, mainClassName);
        assertGitIgnore(entries);
        assertRootPom(entries, packageName, mainClassName);
        assertCommonPom(entries, template, packageName, mainClassName, false);
        assertSettings(entries, template, packageName, mainClassName, false);
        assertMainSourceFile(entries, template, packageName, mainClassName, false);
        assertLocalizationBundles(entries, template, false);
        assertNoTemplatePlaceholders(entries, template);
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
        if (ide == IDE.INTELLIJ) {
            assertNotNull(entries.get(".idea/workspace.xml"), "Missing IntelliJ workspace file");
            return;
        }
        if (ide == IDE.ECLIPSE) {
            assertNotNull(entries.get(mainClassName + " - Run Simulator.launch"), "Missing Eclipse launch file");
            return;
        }
        if (ide == IDE.NETBEANS) {
            assertNotNull(entries.get("nbactions.xml"), "Missing NetBeans actions file");
            return;
        }
        if (ide == IDE.VS_CODE) {
            assertNotNull(entries.get(".vscode/settings.json"), "Missing VS Code settings");
        }
    }

    private void assertRootPom(Map<String, byte[]> entries, String packageName, String mainClassName) {
        String pom = getText(entries, "pom.xml");
        assertContains(pom, packageName, "Root pom should include package as groupId");
        assertContains(pom, mainClassName.toLowerCase(), "Root pom should include app artifact/name");
        assertContains(pom, "<cn1.plugin.version>7.0.227</cn1.plugin.version>", "Root pom should use current CN1 plugin version");
        assertContains(pom, "<cn1.version>7.0.227</cn1.version>", "Root pom should align CN1 runtime version with plugin version");
        assertFalse(pom.indexOf("com.example.myapp") >= 0, "Root pom still contains placeholder package");
        assertFalse(pom.indexOf("myappname") >= 0, "Root pom still contains placeholder app name");
    }

    private void assertCommonPom(Map<String, byte[]> entries, Template template, String packageName, String mainClassName, boolean expectJava17) {
        String pom = getText(entries, "common/pom.xml");
        assertContains(pom, packageName, "Common pom should include package");
        assertContains(pom, mainClassName.toLowerCase(), "Common pom should include app artifact");
        assertContains(pom, "<artifactId>codenameone-javase</artifactId>", "Common pom should include codenameone-javase test dependency");
        assertContains(pom, "<artifactId>serializer</artifactId>", "Common pom should include xalan serializer for CN1 generate-gui-sources");
        assertContains(pom, "<version>2.7.3</version>", "Common pom should pin serializer version expected by CN1 plugin classpath");
        if (expectJava17) {
            assertContains(pom, "<source>17</source>", "Common pom should use Java 17 source when selected");
            assertContains(pom, "<target>17</target>", "Common pom should use Java 17 target when selected");
        } else {
            assertContains(pom, "<source>1.8</source>", "Common pom should default to Java 8 source");
            assertContains(pom, "<target>1.8</target>", "Common pom should default to Java 8 target");
        }
        if (template == Template.GRUB) {
            assertContains(pom, "<artifactId>" + mainClassName.toLowerCase() + "-CodeRAD</artifactId>", "Grub common pom should include local CodeRAD cn1lib dependency");
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

    private void assertSettings(Map<String, byte[]> entries, Template template, String packageName, String mainClassName, boolean expectJava17) {
        String settings = getText(entries, "common/codenameone_settings.properties");
        assertContains(settings, "codename1.packageName=" + packageName, "Settings should include requested package");
        assertContains(settings, "codename1.mainName=" + mainClassName, "Settings should include requested main class");
        assertContains(settings, "codename1.displayName=" + mainClassName, "Settings should include requested display name");
        assertContains(settings, "codename1.kotlin=" + String.valueOf(template.IS_KOTLIN), "Settings should include template kotlin flag");
        if (expectJava17) {
            assertContains(settings, "codename1.arg.java.version=17", "Settings should include Java 17 version when selected");
        } else {
            assertFalse(settings.indexOf("codename1.arg.java.version=17") >= 0, "Settings should not force Java 17 by default");
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
        if (template == Template.BAREBONES || template == Template.KOTLIN) {
            if (expectLocalizationBundles) {
                assertNotNull(entries.get("common/src/main/resources/messages.properties"), "Barebones templates should include default localization bundle");
                assertNotNull(entries.get("common/src/main/resources/messages_ar.properties"), "Barebones templates should include Arabic localization bundle");
                assertNotNull(entries.get("common/src/main/resources/messages_he.properties"), "Barebones templates should include Hebrew localization bundle");
            } else {
                assertNull(entries.get("common/src/main/resources/messages.properties"), "Barebones templates should not include localization bundles by default");
                assertNull(entries.get("common/src/main/resources/messages_ar.properties"), "Barebones templates should not include Arabic localization bundle by default");
                assertNull(entries.get("common/src/main/resources/messages_he.properties"), "Barebones templates should not include Hebrew localization bundle by default");
            }
            return;
        }
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
        assertFalse(javasePom.indexOf("<scope>provided</scope>") >= 0
                        && javasePom.indexOf("<artifactId>codenameone-core</artifactId>") >= 0,
                "javase/pom.xml should not contain duplicate provided codenameone-core dependency");
        assertFalse(javasePom.indexOf("<scope>provided</scope>") >= 0
                        && javasePom.indexOf("<artifactId>codenameone-javase</artifactId>") >= 0,
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
