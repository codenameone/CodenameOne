package com.codenameone.playground;

import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.io.Util;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.codename1.ui.CN.execute;
import static com.codename1.ui.CN.openFileOutputStream;

final class PlaygroundProjectExporter {
    private static final String PACKAGE_NAME = "com.cn1.playground.snippet";
    private static final String FALLBACK_APP_NAME = "PlaygroundSnippet";

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
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            addText(zos, "pom.xml", rootPom(model.appName));
            addText(zos, "build.sh", "#!/bin/sh\ncd \"$(dirname \"$0\")\" || exit 1\nmvn -DskipTests package\n");
            addText(zos, "run.sh", "#!/bin/sh\ncd \"$(dirname \"$0\")\" || exit 1\nmvn -pl javase -am cn1:java -Dcodename1.platform=javase\n");
            addText(zos, "build.bat", "@echo off\r\ncd /d %~dp0\r\nmvn package\r\n");
            addText(zos, "run.bat", "@echo off\r\ncd /d %~dp0\r\nmvn -pl javase -am cn1:java -Dcodename1.platform=javase\r\n");
            addText(zos, ".idea/misc.xml", ideaMiscXml());
            addText(zos, ".idea/runConfigurations/CN1_Simulator.xml", ideaSimulatorRunConfiguration());
            addText(zos, ".idea/runConfigurations/CN1_Build.xml", ideaBuildRunConfiguration());
            addText(zos, "common/pom.xml", commonPom(model.appName));
            addText(zos, "common/codenameone_settings.properties", codenameOneSettings(model.appName));
            addText(zos, "common/src/main/css/theme.css", themeCss(model.css));
            addText(zos, "common/src/main/java/" + PACKAGE_NAME.replace('.', '/') + "/" + model.mainClassName + ".java", model.javaSource);
            addText(zos, "android/pom.xml", platformPom(model.appName, "android"));
            addText(zos, "ios/pom.xml", platformPom(model.appName, "ios"));
            addText(zos, "javascript/pom.xml", platformPom(model.appName, "javascript"));
            addText(zos, "javase/pom.xml", javasePom(model.appName));
            addText(zos, "win/pom.xml", platformPom(model.appName, "win"));
            addText(zos, "README.md", readme(model.appName, model.mainClassName));
        }
    }

    private void addText(ZipOutputStream zos, String path, String text) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(text.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private String rootPom(String appName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>com.cn1.playground</groupId>\n"
                + "  <artifactId>" + appName.toLowerCase() + "</artifactId>\n"
                + "  <version>1.0-SNAPSHOT</version>\n"
                + "  <packaging>pom</packaging>\n"
                + "  <name>" + appName + "</name>\n"
                + "  <properties>\n"
                + "    <cn1.version>7.0.230</cn1.version>\n"
                + "    <cn1.plugin.version>${cn1.version}</cn1.plugin.version>\n"
                + "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
                + "    <maven.compiler.source>17</maven.compiler.source>\n"
                + "    <maven.compiler.target>17</maven.compiler.target>\n"
                + "  </properties>\n"
                + "  <modules><module>common</module><module>javase</module><module>android</module><module>ios</module><module>javascript</module><module>win</module></modules>\n"
                + "  <build><pluginManagement><plugins>\n"
                + "    <plugin><groupId>com.codenameone</groupId><artifactId>codenameone-maven-plugin</artifactId><version>${cn1.plugin.version}</version></plugin>\n"
                + "  </plugins></pluginManagement></build>\n"
                + "</project>\n";
    }

    private String commonPom(String appName) {
        String zipArtifact = appName.toLowerCase() + "-zipsupport";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <parent><groupId>com.cn1.playground</groupId><artifactId>" + appName.toLowerCase() + "</artifactId><version>1.0-SNAPSHOT</version></parent>\n"
                + "  <artifactId>" + appName.toLowerCase() + "-common</artifactId>\n"
                + "  <dependencies>\n"
                + "    <dependency><groupId>com.codenameone</groupId><artifactId>codenameone-core</artifactId><version>${cn1.version}</version></dependency>\n"
                + "    <dependency><groupId>com.cn1.playground</groupId><artifactId>" + zipArtifact + "</artifactId><version>1.0-SNAPSHOT</version><classifier>common</classifier><type>jar</type></dependency>\n"
                + "  </dependencies>\n"
                + "  <profiles>\n"
                + "    <profile><id>simulator</id><properties><codename1.targetPlatform>javase</codename1.targetPlatform></properties></profile>\n"
                + "  </profiles>\n"
                + "</project>\n";
    }

    private String platformPom(String appName, String module) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <parent><groupId>com.cn1.playground</groupId><artifactId>" + appName.toLowerCase() + "</artifactId><version>1.0-SNAPSHOT</version></parent>\n"
                + "  <artifactId>" + appName.toLowerCase() + "-" + module + "</artifactId>\n"
                + "  <packaging>pom</packaging>\n"
                + "</project>\n";
    }

    private String javasePom(String appName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <parent><groupId>com.cn1.playground</groupId><artifactId>" + appName.toLowerCase() + "</artifactId><version>1.0-SNAPSHOT</version></parent>\n"
                + "  <artifactId>" + appName.toLowerCase() + "-javase</artifactId>\n"
                + "  <dependencies><dependency><groupId>com.cn1.playground</groupId><artifactId>" + appName.toLowerCase() + "-common</artifactId><version>1.0-SNAPSHOT</version></dependency></dependencies>\n"
                + "</project>\n";
    }

    private String ideaMiscXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project version=\"4\">\n"
                + "  <component name=\"ProjectRootManager\" version=\"2\" languageLevel=\"JDK_17\" default=\"true\" />\n"
                + "</project>\n";
    }

    private String ideaSimulatorRunConfiguration() {
        return "<component name=\"ProjectRunConfigurationManager\">\n"
                + "  <configuration default=\"false\" name=\"Simulator\" type=\"MavenRunConfiguration\" factoryName=\"Maven\">\n"
                + "    <MavenSettings>\n"
                + "      <option name=\"myWorkingDirectory\" value=\"$PROJECT_DIR$\" />\n"
                + "      <option name=\"myGoals\">\n"
                + "        <list>\n"
                + "          <option value=\"cn1:java\" />\n"
                + "          <option value=\"-P\" />\n"
                + "          <option value=\"simulator\" />\n"
                + "          <option value=\"-Dcodename1.platform=javase\" />\n"
                + "        </list>\n"
                + "      </option>\n"
                + "    </MavenSettings>\n"
                + "  </configuration>\n"
                + "</component>\n";
    }

    private String ideaBuildRunConfiguration() {
        return "<component name=\"ProjectRunConfigurationManager\">\n"
                + "  <configuration default=\"false\" name=\"Build\" type=\"MavenRunConfiguration\" factoryName=\"Maven\">\n"
                + "    <MavenSettings>\n"
                + "      <option name=\"myWorkingDirectory\" value=\"$PROJECT_DIR$\" />\n"
                + "      <option name=\"myGoals\">\n"
                + "        <list>\n"
                + "          <option value=\"-DskipTests\" />\n"
                + "          <option value=\"package\" />\n"
                + "        </list>\n"
                + "      </option>\n"
                + "    </MavenSettings>\n"
                + "  </configuration>\n"
                + "</component>\n";
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

    private String readme(String appName, String mainClassName) {
        return "# " + appName + "\n\nGenerated from CN1 Playground.\n\nMain class: `" + PACKAGE_NAME + "." + mainClassName + "`\n";
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
            out.append("        Object root = null;\n");
            if (body.length() == 0) {
                out.append("        // Add snippet code here.\n");
            } else {
                out.append(body);
            }
            out.append("\n")
                    .append("        if (root instanceof com.codename1.ui.Form) {\n")
                    .append("            ((com.codename1.ui.Form) root).show();\n")
                    .append("        } else if (root instanceof com.codename1.ui.Container) {\n")
                    .append("            com.codename1.ui.Form playgroundForm = new com.codename1.ui.Form(\"Playground Snippet\", new com.codename1.ui.layouts.BorderLayout());\n")
                    .append("            playgroundForm.add(com.codename1.ui.layouts.BorderLayout.CENTER, (com.codename1.ui.Container) root);\n")
                    .append("            playgroundForm.show();\n")
                    .append("        }\n");
            out.append("    }\n");
            out.append("}\n");
            return out.toString();
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
