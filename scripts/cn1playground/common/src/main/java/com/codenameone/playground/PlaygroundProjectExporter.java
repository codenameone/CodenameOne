package com.codenameone.playground;

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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.codename1.ui.CN.execute;
import static com.codename1.ui.CN.getResourceAsStream;
import static com.codename1.ui.CN.openFileOutputStream;

final class PlaygroundProjectExporter {
    private static final String PACKAGE_NAME = "com.cn1.playground.snippet";
    private static final String FALLBACK_APP_NAME = "PlaygroundSnippet";
    private static final String TEMPLATE_PACKAGE = "com.example.myapp";
    private static final String TEMPLATE_APP_NAME = "MyAppName";
    private static final String TEMPLATE_APP_NAME_LOWER = "myappname";
    private static final String TEMPLATE_PACKAGE_PATH = TEMPLATE_PACKAGE.replace('.', '/');
    private static final String CN1_PLUGIN_VERSION = "7.0.230";

    void export(String script, String css) {
        ExportModel model = ExportModel.fromScript(script, css);
        String filePath = model.appName.toLowerCase() + ".zip";
        try (OutputStream output = openFileOutputStream(filePath)) {
            writeZip(output, model);
            execute(filePath);
            ToastBar.showInfoMessage("Downloaded " + model.appName + ".zip");
        } catch (IOException ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Error creating project zip: " + ex.getMessage());
        }
    }

    private void writeZip(OutputStream out, ExportModel model) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();

        copyZipResource("/idea.zip", entries);
        copyZipResource("/common.zip", entries);

        putText(entries, ".gitignore", "**/target/\n.idea/\n*.iml\n.DS_Store\nThumbs.db\n");
        putText(entries, "README.md", readme(model.appName));
        putText(entries, "common/pom.xml", readResourceToString("/barebones-pom.xml"));
        putText(entries, "common/codenameone_settings.properties", codenameOneSettings(model.appName));
        putText(entries, "common/src/main/css/theme.css", themeCss(model.css));
        putText(entries, "common/src/main/java/" + TEMPLATE_PACKAGE_PATH + "/" + TEMPLATE_APP_NAME + ".java", model.javaSource);

        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> fileEntry : entries.entrySet()) {
                String path = applyPathReplacements(fileEntry.getKey(), model);
                byte[] data = applyDataReplacements(path, fileEntry.getValue(), model);
                ZipEntry zipEntry = new ZipEntry(path);
                zos.putNextEntry(zipEntry);
                zos.write(data);
                zos.closeEntry();
            }
        }
    }

    private void copyZipResource(String resourcePath, Map<String, byte[]> entries) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(getResourceAsStream(resourcePath))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), readToBytesNoClose(zis));
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
    }

    private void putText(Map<String, byte[]> entries, String path, String text) throws IOException {
        entries.put(path, text.getBytes("UTF-8"));
    }

    private String applyPathReplacements(String path, ExportModel model) {
        String packagePath = PACKAGE_NAME.replace('.', '/');
        String replaced = path;
        replaced = StringUtil.replaceAll(replaced, TEMPLATE_PACKAGE_PATH, packagePath);
        replaced = StringUtil.replaceAll(replaced, TEMPLATE_APP_NAME, model.appName);
        replaced = StringUtil.replaceAll(replaced, TEMPLATE_APP_NAME_LOWER, model.appName.toLowerCase());
        return replaced;
    }

    private byte[] applyDataReplacements(String path, byte[] data, ExportModel model) throws IOException {
        if (!isTextFile(path)) {
            return data;
        }
        String content = StringUtil.newString(data);
        content = StringUtil.replaceAll(content, TEMPLATE_PACKAGE, PACKAGE_NAME);
        content = StringUtil.replaceAll(content, TEMPLATE_APP_NAME, model.appName);
        content = StringUtil.replaceAll(content, TEMPLATE_APP_NAME_LOWER, model.appName.toLowerCase());
        if ("pom.xml".equals(path)) {
            content = replaceTagValue(content, "cn1.plugin.version", CN1_PLUGIN_VERSION);
            content = replaceTagValue(content, "cn1.version", CN1_PLUGIN_VERSION);
        }
        if ("common/pom.xml".equals(path)) {
            content = StringUtil.replaceAll(content, "<source>1.8</source>", "<source>17</source>");
            content = StringUtil.replaceAll(content, "<target>1.8</target>", "<target>17</target>");
        }
        if (".idea/misc.xml".equals(path)) {
            content = removeXmlAttribute(content, "project-jdk-name");
            content = removeXmlAttribute(content, "project-jdk-type");
            content = setXmlAttribute(content, "languageLevel", "JDK_17");
        }
        if ("common/codenameone_settings.properties".equals(path)) {
            content = replaceProperty(content, "codename1.arg.java.version", "17");
        }
        if ("android/pom.xml".equals(path) || "ios/pom.xml".equals(path) || "javascript/pom.xml".equals(path)) {
            content = hardenPlatformModulePomAgainstDoubleJarAttach(content);
        }
        if ("javase/pom.xml".equals(path)) {
            content = normalizeJavasePom(content);
        }
        return content.getBytes("UTF-8");
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

    private static String readResourceToString(String resourcePath) throws IOException {
        try (InputStream inputStream = getResourceAsStream(resourcePath)) {
            return StringUtil.newString(readToBytesNoClose(inputStream));
        }
    }

    private static byte[] readToBytesNoClose(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Util.copyNoClose(is, bos, 8192);
        bos.close();
        return bos.toByteArray();
    }

    private String codenameOneSettings(String appName) {
        return "codename1.arg.java.version=17\n"
                + "codename1.mainName=" + appName + "\n"
                + "codename1.packageName=" + PACKAGE_NAME + "\n"
                + "codename1.displayName=" + appName + "\n"
                + "codename1.theme=basic\n";
    }

    private String themeCss(String css) {
        String base = "#Constants {\n  includeNativeBool: true;\n}\n";
        if (css == null || css.trim().isEmpty()) {
            return base;
        }
        return base + "\n/* Playground CSS */\n" + css + "\n";
    }

    private String readme(String appName) {
        return "# Codename One Project\n\n"
                + "This is a multi-module Maven project for a Codename One app.\n"
                + "You can write the app in Java and/or Kotlin, and build for Android, iOS, desktop, and web.\n\n"
                + "## Getting Started\n\n"
                + "You selected a Java template. Start here:\n"
                + "https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html\n\n"
                + "## IntelliJ Users\n\n"
                + "This project should work in IntelliJ out of the box.\n"
                + "You usually don't need to copy or tweak any project files.\n\n"
                + "## Help and Support\n\n"
                + "- Codename One website: https://www.codenameone.com\n"
                + "- Codename One GitHub: https://github.com/codenameone/CodenameOne\n";
    }

    private static final class ExportModel {
        final String appName;
        final String mainClassName;
        final String javaSource;
        final String css;

        private ExportModel(String appName, String mainClassName, String javaSource, String css) {
            this.appName = appName;
            this.mainClassName = mainClassName;
            this.javaSource = javaSource;
            this.css = css == null ? "" : css;
        }

        static ExportModel fromScript(String script, String css) {
            String safeScript = script == null ? "" : script;
            String lifecycleClassName = findLifecycleClassName(safeScript);
            if (lifecycleClassName != null) {
                String source = normalizeLifecycleSource(safeScript);
                return new ExportModel(lifecycleClassName, lifecycleClassName, source, css);
            }
            String source = buildSnippetLifecycle(safeScript);
            return new ExportModel(FALLBACK_APP_NAME, FALLBACK_APP_NAME, source, css);
        }

        private static String findLifecycleClassName(String source) {
            if (source == null || source.length() == 0) {
                return null;
            }
            String cleaned = stripCommentsAndStrings(source);
            int from = 0;
            while (from < cleaned.length()) {
                int classPos = indexOfWord(cleaned, "class", from);
                if (classPos < 0) {
                    return null;
                }
                int namePos = skipWhitespace(cleaned, classPos + 5);
                String className = readIdentifier(cleaned, namePos);
                if (className == null) {
                    from = classPos + 5;
                    continue;
                }
                int afterName = namePos + className.length();
                int extendsPos = indexOfWord(cleaned, "extends", afterName);
                if (extendsPos < 0) {
                    return null;
                }
                int bodyPos = cleaned.indexOf('{', afterName);
                if (bodyPos >= 0 && extendsPos > bodyPos) {
                    from = bodyPos + 1;
                    continue;
                }
                int parentPos = skipWhitespace(cleaned, extendsPos + 7);
                String parent = readIdentifier(cleaned, parentPos);
                if ("Lifecycle".equals(parent)) {
                    return className;
                }
                from = afterName;
            }
            return null;
        }

        private static int skipWhitespace(String text, int index) {
            int out = index;
            while (out < text.length() && Character.isWhitespace(text.charAt(out))) {
                out++;
            }
            return out;
        }

        private static int indexOfWord(String text, String word, int from) {
            int pos = text.indexOf(word, from);
            while (pos >= 0) {
                int before = pos - 1;
                int after = pos + word.length();
                boolean leftOk = before < 0 || !isIdentifierPart(text.charAt(before));
                boolean rightOk = after >= text.length() || !isIdentifierPart(text.charAt(after));
                if (leftOk && rightOk) {
                    return pos;
                }
                pos = text.indexOf(word, pos + 1);
            }
            return -1;
        }

        private static String readIdentifier(String text, int from) {
            if (from < 0 || from >= text.length()) {
                return null;
            }
            char first = text.charAt(from);
            if (!isIdentifierStart(first)) {
                return null;
            }
            int end = from + 1;
            while (end < text.length() && isIdentifierPart(text.charAt(end))) {
                end++;
            }
            return text.substring(from, end);
        }

        private static boolean isIdentifierStart(char ch) {
            return isAsciiLetter(ch) || ch == '_' || ch == '$';
        }

        private static boolean isIdentifierPart(char ch) {
            return isAsciiLetter(ch) || isAsciiDigit(ch) || ch == '_' || ch == '$';
        }

        private static boolean isAsciiLetter(char ch) {
            return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
        }

        private static boolean isAsciiDigit(char ch) {
            return ch >= '0' && ch <= '9';
        }

        private static String stripCommentsAndStrings(String source) {
            StringBuilder out = new StringBuilder(source.length());
            int i = 0;
            while (i < source.length()) {
                char ch = source.charAt(i);
                if (ch == '"' || ch == '\'') {
                    i = skipQuoted(source, i);
                    out.append(' ');
                    continue;
                }
                if (ch == '/' && i + 1 < source.length()) {
                    char next = source.charAt(i + 1);
                    if (next == '/') {
                        i = skipLineComment(source, i + 2);
                        out.append('\n');
                        continue;
                    }
                    if (next == '*') {
                        i = skipBlockComment(source, i + 2);
                        out.append(' ');
                        continue;
                    }
                }
                out.append(ch);
                i++;
            }
            return out.toString();
        }

        private static int skipQuoted(String text, int startQuote) {
            char quote = text.charAt(startQuote);
            int i = startQuote + 1;
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (ch == '\\') {
                    i += 2;
                    continue;
                }
                if (ch == quote) {
                    return i + 1;
                }
                i++;
            }
            return text.length();
        }

        private static int skipLineComment(String text, int from) {
            int i = from;
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (ch == '\n' || ch == '\r') {
                    return i;
                }
                i++;
            }
            return text.length();
        }

        private static int skipBlockComment(String text, int from) {
            int i = from;
            while (i + 1 < text.length()) {
                if (text.charAt(i) == '*' && text.charAt(i + 1) == '/') {
                    return i + 2;
                }
                i++;
            }
            return text.length();
        }

        private static String normalizeLifecycleSource(String script) {
            String withoutPackage = removePackageDeclaration(script).trim();
            return "package " + PACKAGE_NAME + ";\n\n" + withoutPackage + "\n";
        }

        private static String buildSnippetLifecycle(String script) {
            Set<String> imports = new LinkedHashSet<String>();
            StringBuilder body = new StringBuilder();
            String detectedFormVar = null;
            String detectedContainerVar = null;
            String[] lines = Util.split(script, "\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import ")) {
                    imports.add(trimmed.endsWith(";") ? trimmed : trimmed + ";");
                    continue;
                }
                if (trimmed.startsWith("package ")) {
                    continue;
                }
                if ("root;".equals(trimmed) || "ctx.log(\"Preview built successfully\");".equals(trimmed)) {
                    continue;
                }
                String formVar = detectDeclaredVariableName(trimmed, "Form");
                if (formVar == null) {
                    formVar = detectDeclaredVariableName(trimmed, "com.codename1.ui.Form");
                }
                if (formVar != null) {
                    detectedFormVar = formVar;
                }
                String containerVar = detectDeclaredVariableName(trimmed, "Container");
                if (containerVar == null) {
                    containerVar = detectDeclaredVariableName(trimmed, "com.codename1.ui.Container");
                }
                if (containerVar != null) {
                    detectedContainerVar = containerVar;
                }
                body.append("        ").append(line).append('\n');
            }
            StringBuilder out = new StringBuilder();
            out.append("package ").append(PACKAGE_NAME).append(";\n\n");
            out.append("import com.codename1.system.Lifecycle;\n");
            for (String importLine : imports) {
                out.append(importLine).append('\n');
            }
            out.append("\npublic class ").append(FALLBACK_APP_NAME).append(" extends Lifecycle {\n");
            out.append("    @Override\n");
            out.append("    public void runApp() {\n");
            if (body.length() == 0) {
                out.append("        // Add snippet code here.\n");
            } else {
                out.append(body);
            }
            out.append("\n");
            if (detectedFormVar != null) {
                out.append("        ").append(detectedFormVar).append(".show();\n");
            } else if (detectedContainerVar != null) {
                out.append("        if (").append(detectedContainerVar).append(" != null) {\n")
                    .append("            com.codename1.ui.Form playgroundForm = new com.codename1.ui.Form(\"Playground Snippet\", new com.codename1.ui.layouts.BorderLayout());\n")
                    .append("            playgroundForm.add(com.codename1.ui.layouts.BorderLayout.CENTER, ").append(detectedContainerVar).append(");\n")
                    .append("            playgroundForm.show();\n")
                    .append("        }\n");
            } else {
                out.append("        // No top-level Form/Container variable was detected. Show a Form here.\n");
            }
            out.append("    }\n");
            out.append("}\n");
            return out.toString();
        }

        private static String detectDeclaredVariableName(String line, String typeName) {
            String normalized = line;
            if (normalized.startsWith("final ")) {
                normalized = normalized.substring("final ".length()).trim();
            }
            String prefix = typeName + " ";
            if (!normalized.startsWith(prefix)) {
                return null;
            }
            int eq = normalized.indexOf('=');
            if (eq < 0) {
                return null;
            }
            String name = normalized.substring(prefix.length(), eq).trim();
            if (name.length() == 0) {
                return null;
            }
            if (!isIdentifierStart(name.charAt(0))) {
                return null;
            }
            for (int i = 1; i < name.length(); i++) {
                if (!isIdentifierPart(name.charAt(i))) {
                    return null;
                }
            }
            return name;
        }

        private static String removePackageDeclaration(String source) {
            String[] lines = Util.split(source, "\n");
            StringBuilder out = new StringBuilder();
            for (String line : lines) {
                if (line.trim().startsWith("package ")) {
                    continue;
                }
                out.append(line).append('\n');
            }
            return out.toString();
        }
    }
}
