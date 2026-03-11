package com.codename1.initializr.model;

import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.util.StringUtil;
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
    private static final String CN1_PLUGIN_VERSION = "7.0.227";
    private static final String GENERATED_GITIGNORE =
            "**/target/\n" +
            ".idea/\n" +
            "*.iml\n" +
            ".DS_Store\n" +
            "Thumbs.db\n";

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
        String filePath = getAppHomePath() + appName.toLowerCase() + ".zip";
        try (OutputStream fos = openFileOutputStream(filePath)) {
            writeProjectZip(fos);
        } catch (IOException err) {
            Log.e(err);
            ToastBar.showErrorMessage("Error generating zip: " + err);
            return;
        }
        execute(filePath);
    }

    void writeProjectZip(OutputStream outputStream) throws IOException {
        Map<String, byte[]> mergedEntries = new LinkedHashMap<String, byte[]>();

        copyZipEntriesToMap(ide.ZIP, mergedEntries, ZipEntryType.IDE);
        copyZipEntriesToMap("/common.zip", mergedEntries, ZipEntryType.COMMON);
        copySingleTextEntryToMap(".gitignore", GENERATED_GITIGNORE, mergedEntries, ZipEntryType.COMMON);
        copySingleTextEntryToMap("README.md", buildReadmeMarkdown(), mergedEntries, ZipEntryType.COMMON);
        copySingleTextEntryToMap("common/pom.xml", readResourceToString(template.POM_XML), mergedEntries, ZipEntryType.TEMPLATE_POM);
        if (template.CN1LIB_ZIP != null) {
            copyZipEntriesToMap(template.CN1LIB_ZIP, mergedEntries, ZipEntryType.TEMPLATE_CN1LIB);
        }
        copyZipEntriesToMap(template.CSS, mergedEntries, ZipEntryType.TEMPLATE_CSS);
        copyZipEntriesToMap(template.SOURCE_ZIP, mergedEntries, ZipEntryType.TEMPLATE_SOURCE);
        addLocalizationEntries(mergedEntries);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, byte[]> fileEntry : mergedEntries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(fileEntry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(fileEntry.getValue());
                zos.closeEntry();
            }
        }
    }


    private void addLocalizationEntries(Map<String, byte[]> mergedEntries) throws IOException {
        if (!isBareTemplate() || !options.includeLocalizationBundles) {
            return;
        }
        copySingleTextEntryToMap(
                "common/src/main/resources/messages.properties",
                readResourceToString("/messages.properties"),
                mergedEntries,
                ZipEntryType.COMMON
        );
        for (ProjectOptions.PreviewLanguage language : ProjectOptions.PreviewLanguage.values()) {
            if (language == ProjectOptions.PreviewLanguage.ENGLISH) {
                continue;
            }
            copySingleTextEntryToMap(
                    "common/src/main/resources/messages_" + language.bundleSuffix + ".properties",
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
            content += buildThemeOverrides();
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
        }
        if ("android/pom.xml".equals(targetPath) || "ios/pom.xml".equals(targetPath)) {
            content = hardenPlatformModulePomAgainstDoubleJarAttach(content);
        }
        if ("javase/pom.xml".equals(targetPath)) {
            content = normalizeJavasePom(content);
        }
        return content.getBytes("UTF-8");
    }



    private String applyJavaVersionSettings(String content) {
        if (options.javaVersion == ProjectOptions.JavaVersion.JAVA_17_EXPERIMENTAL) {
            content = replaceProperty(content, "codename1.arg.java.version", "17");
        }
        return content;
    }

    private String applyJavaVersionToPom(String content) {
        if (options.javaVersion != ProjectOptions.JavaVersion.JAVA_17_EXPERIMENTAL) {
            return content;
        }
        content = StringUtil.replaceAll(content, "<source>1.8</source>", "<source>17</source>");
        content = StringUtil.replaceAll(content, "<target>1.8</target>", "<target>17</target>");
        return content;
    }

    private String normalizeIntellijMiscXml(String content) {
        String languageLevel = options.javaVersion == ProjectOptions.JavaVersion.JAVA_17_EXPERIMENTAL ? "JDK_17" : "JDK_1_8";
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
                + "        Hashtable<String, String> bundle = Resources.getGlobalResources().getL10N(\"messages\", language);\n"
                + "        UIManager.getInstance().setBundle(bundle);\n"
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
                + "        val bundle: Hashtable<String, String>? = Resources.getGlobalResources().getL10N(\"messages\", language)\n"
                + "        UIManager.getInstance().setBundle(bundle)\n"
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

        out.append("## Help and Support\n\n")
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

    private String buildThemeOverrides() {
        if (isDefaultBarebonesOptions()) {
            return "";
        }
        StringBuilder out = new StringBuilder("\n\n/* Initializr Theme Overrides */\n");

        if (options.themeMode == ProjectOptions.ThemeMode.DARK) {
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

            if (options.accent == ProjectOptions.Accent.DEFAULT) {
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
                return out.toString();
            }
        } else if (options.accent == ProjectOptions.Accent.DEFAULT) {
            // Light + Clean intentionally inherits template defaults (rounded ignored).
            return "";
        }

        int accent = resolveAccentColor();
        int accentPressed = darkenColor(accent, 0.22f);
        String buttonRadius = options.roundedButtons ? "3mm" : "0";
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
        return out.toString();
    }

    private boolean isDefaultBarebonesOptions() {
        return options.themeMode == ProjectOptions.ThemeMode.LIGHT
                && options.accent == ProjectOptions.Accent.DEFAULT;
    }

    private int resolveAccentColor() {
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
