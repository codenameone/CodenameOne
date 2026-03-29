package com.codenameone.playground;

import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PlaygroundProjectExporter {
    static final class ExportedProject {
        final String fileName;
        final String base64Zip;

        ExportedProject(String fileName, String base64Zip) {
            this.fileName = fileName;
            this.base64Zip = base64Zip;
        }
    }

    private static final String PACKAGE_NAME = "com.cn1playground";
    private static final String CLASS_NAME = "MyApplication";
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+[^;]+;\\s*$");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+[^;]+;\\s*$");

    private PlaygroundProjectExporter() {
    }

    static ExportedProject build(String script, String customCss) {
        String javaSource = buildJavaSource(script == null ? "" : script);
        String themeCss = buildThemeCss(customCss == null ? "" : customCss);
        byte[] zip = buildZip(javaSource, themeCss);
        String base64 = Base64.encodeNoNewline(zip);
        return new ExportedProject("cn1-playground-app.zip", base64);
    }

    private static byte[] buildZip(String javaSource, String themeCss) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            putEntry(zos, "README.md", "# Playground App\n\nGenerated from CN1 Playground.".getBytes("UTF-8"));
            putEntry(zos, "common/src/main/java/com/cn1playground/MyApplication.java", javaSource.getBytes("UTF-8"));
            putEntry(zos, "common/src/main/css/theme.css", themeCss.getBytes("UTF-8"));
            zos.close();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate playground project zip", ex);
        }
    }

    private static void putEntry(ZipOutputStream zos, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private static String buildThemeCss(String customCss) {
        String normalized = customCss == null ? "" : customCss.trim();
        if (normalized.length() == 0) {
            return "";
        }
        return "\n/* Appended from CN1 Playground */\n" + normalized + "\n";
    }

    private static String buildJavaSource(String script) {
        if (isLifecycleClass(script)) {
            String noPackage = PACKAGE_PATTERN.matcher(script).replaceAll("").trim();
            return "package " + PACKAGE_NAME + ";\n\n" + noPackage + "\n";
        }

        LinkedHashSet<String> imports = collectImports(script);
        String body = stripPackageAndImports(script).trim();
        String[] splitBody = splitSnippetBody(body);
        String setup = splitBody[0];
        String finalExpression = splitBody[1];

        StringBuilder out = new StringBuilder();
        out.append("package ").append(PACKAGE_NAME).append(";\n\n");
        out.append("import com.codename1.ui.Component;\n");
        out.append("import com.codename1.ui.Form;\n");
        out.append("import com.codename1.ui.layouts.BorderLayout;\n");
        for (String line : imports) {
            if (line.contains("com.codenameone.playground")) {
                continue;
            }
            out.append(line).append("\n");
        }
        out.append("\n");
        out.append("public class ").append(CLASS_NAME).append(" {\n");
        out.append("    private Form current;\n\n");
        out.append("    public void init(Object context) {}\n\n");
        out.append("    public void start() {\n");
        out.append("        if (current != null) {\n");
        out.append("            current.show();\n");
        out.append("            return;\n");
        out.append("        }\n");
        out.append("        Form form = new Form(\"Playground App\", new BorderLayout());\n");
        if (setup.length() > 0) {
            out.append(indent(setup, "        ")).append("\n");
        }
        out.append("        form.add(BorderLayout.CENTER, ").append(finalExpression).append(");\n");
        out.append("        current = form;\n");
        out.append("        form.show();\n");
        out.append("    }\n\n");
        out.append("    public void stop() {\n");
        out.append("        current = com.codename1.ui.CN.getCurrentForm();\n");
        out.append("    }\n\n");
        out.append("    public void destroy() {}\n");
        out.append("}\n");
        return out.toString();
    }

    private static boolean isLifecycleClass(String script) {
        if (script == null) {
            return false;
        }
        String normalized = script;
        return normalized.indexOf(" class ") >= 0 && normalized.indexOf("void start(") >= 0;
    }

    private static LinkedHashSet<String> collectImports(String script) {
        LinkedHashSet<String> imports = new LinkedHashSet<String>();
        if (script == null || script.length() == 0) {
            return imports;
        }
        Matcher matcher = IMPORT_PATTERN.matcher(script);
        while (matcher.find()) {
            imports.add(matcher.group().trim());
        }
        return imports;
    }

    private static String stripPackageAndImports(String script) {
        if (script == null) {
            return "";
        }
        String out = PACKAGE_PATTERN.matcher(script).replaceAll("");
        return IMPORT_PATTERN.matcher(out).replaceAll("");
    }

    private static String[] splitSnippetBody(String body) {
        if (body == null || body.length() == 0) {
            return new String[]{"", "new com.codename1.ui.Label(\"Hello from Playground\")"};
        }

        String trimmed = body.trim();
        int semicolon = trimmed.lastIndexOf(';');
        if (semicolon < 0 || semicolon == trimmed.length() - 1) {
            return new String[]{"", sanitizeFinalExpression(trimmed)};
        }

        String before = trimmed.substring(0, semicolon).trim();
        String lastStatement = trimmed.substring(semicolon + 1).trim();
        if (lastStatement.length() > 0) {
            return new String[]{trimmed, "new com.codename1.ui.Label(\"Playground snippet complete\")"};
        }

        int prev = before.lastIndexOf(';');
        String setup = prev >= 0 ? before.substring(0, prev + 1).trim() : "";
        String finalStatement = prev >= 0 ? before.substring(prev + 1).trim() : before;
        String finalExpression = sanitizeFinalExpression(finalStatement);
        return new String[]{setup, finalExpression};
    }

    private static String sanitizeFinalExpression(String statement) {
        String out = statement == null ? "" : statement.trim();
        if (out.startsWith("return ")) {
            out = out.substring("return ".length()).trim();
        }
        if (out.endsWith(";")) {
            out = out.substring(0, out.length() - 1).trim();
        }
        if (out.length() == 0) {
            return "new com.codename1.ui.Label(\"Hello from Playground\")";
        }
        return out;
    }

    private static String indent(String text, String indent) {
        String[] lines = text.split("\\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().length() == 0) {
                continue;
            }
            out.append(indent).append(line.trim());
            if (i < lines.length - 1) {
                out.append("\n");
            }
        }
        return out.toString();
    }
}
