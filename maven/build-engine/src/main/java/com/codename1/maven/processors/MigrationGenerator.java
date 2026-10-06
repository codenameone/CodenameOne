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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.ProcessorContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;

/// Compiles a project's migration scripts into a class.
///
/// A script cannot be shipped as a file and found at run time. A device has a flat resource
/// namespace it cannot enumerate, and a packaged backend cannot read classpath resources at
/// all -- while the same server on the JVM can, which is the kind of difference that only
/// shows in production. So the build reads the scripts, checks them, and emits their text,
/// version, description and checksum as literals in `cn1app.ClientMigrations` or
/// `cn1app.BackendMigrations`. Both runtimes then run byte-identical scripts.
///
/// Driven by [OrmAnnotationProcessor], which already settles whether a module is an
/// application or a server, and which owns the bootstrap class the generated set is registered
/// from. It is not a processor of its own: the native packaging goal instantiates its
/// processors by name, so one that existed only as a service entry would work under
/// `cn1:backend` and be missing from the packaged binary.
final class MigrationGenerator {
    static final String CLIENT_BINARY = "cn1app.ClientMigrations";
    static final String BACKEND_BINARY = "cn1app.BackendMigrations";
    /// Where a server keeps its scripts: Flyway's default location.
    static final String BACKEND_LOCATION = "src/main/resources/db/migration";
    /// Where an application keeps its scripts. Deliberately not a resource root: resources
    /// ship in the application, where they would be dead weight beside the compiled copy, and
    /// a name such as `V1.1__x.sql` is one iOS resource packaging mishandles.
    static final String CLIENT_LOCATION = "src/main/db/migration";

    private static final String[] VENDORS = {"sqlite", "postgresql", "mysql"};
    /// Literals are cut well under the class file's 65535-byte constant limit: a character
    /// takes up to three bytes there.
    private static final int CHUNK = 8000;

    /// One script, or one annotated Java migration.
    static final class Item {
        String version;
        String description;
        String scriptName;
        String dialect;
        String text;
        int checksum;
        boolean transactional = true;
        String javaClass;
    }

    private MigrationGenerator() {
    }

    /// The directories a module's scripts may be under, most specific first: the project
    /// directory the build named, then the module the class directory sits in --
    /// `target/classes` under Maven, `build/classes/java/main` under Gradle.
    static List<File> moduleDirs(ProcessorContext ctx) {
        List<File> dirs = new ArrayList<File>();
        if (ctx.getProjectDir() != null) {
            dirs.add(ctx.getProjectDir());
        }
        File up = ctx.getOutputClassDir();
        for (int i = 0; i < 4 && up != null; i++) {
            up = up.getParentFile();
            if (up != null && ("target".equals(up.getName()) || "build".equals(up.getName()))
                    && up.getParentFile() != null && !dirs.contains(up.getParentFile())) {
                dirs.add(up.getParentFile());
                break;
            }
        }
        return dirs;
    }

    private static File moduleDir(ProcessorContext ctx, boolean backend) {
        List<File> dirs = moduleDirs(ctx);
        for (File dir : dirs) {
            if (new File(dir, backend ? BACKEND_LOCATION : CLIENT_LOCATION).isDirectory()) {
                return dir;
            }
        }
        return dirs.isEmpty() ? null : dirs.get(0);
    }

    /// Reads and checks the scripts of a module. Problems are reported through the context;
    /// the caller stops on `ctx.hasErrors()`.
    static List<Item> scan(ProcessorContext ctx, boolean backend) {
        List<Item> items = new ArrayList<Item>();
        File module = moduleDir(ctx, backend);
        if (module == null) {
            return items;
        }
        File dir = new File(module, backend ? BACKEND_LOCATION : CLIENT_LOCATION);
        if (!backend) {
            File misplaced = new File(module, BACKEND_LOCATION);
            if (misplaced.isDirectory() && hasSql(misplaced)) {
                ctx.error("Migration scripts found in " + misplaced + ". An application keeps them in "
                        + CLIENT_LOCATION + ": files under src/main/resources are packaged into the app, "
                        + "and the build compiles the scripts in instead.");
            }
        }
        if (!dir.isDirectory()) {
            return items;
        }
        readDirectory(ctx, dir, null, items);
        for (String vendor : VENDORS) {
            File sub = new File(dir, vendor);
            if (!sub.isDirectory()) {
                continue;
            }
            if (!backend && !"sqlite".equals(vendor)) {
                if (hasSql(sub)) {
                    ctx.error("Migration scripts found in " + sub + ". The application database is SQLite; "
                            + "only the scripts directly in " + CLIENT_LOCATION + " (or its sqlite directory) "
                            + "can run there.");
                }
                continue;
            }
            readDirectory(ctx, sub, vendor, items);
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !Arrays.asList(VENDORS).contains(child.getName()) && hasSql(child)) {
                    ctx.error("Migration scripts found in " + child + ". Only the directories sqlite, "
                            + "postgresql and mysql are read beside the common scripts; MariaDB uses mysql.");
                }
            }
        }
        checkDuplicates(ctx, items);
        return items;
    }

    private static boolean hasSql(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".sql")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void readDirectory(ProcessorContext ctx, File dir, String vendor, List<Item> items) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // Sorted, so the generated class is the same on every machine.
        Arrays.sort(files);
        for (File file : files) {
            String name = file.getName();
            if (!file.isFile() || !name.endsWith(".sql")) {
                continue;
            }
            Item item = parseName(ctx, file);
            if (item == null) {
                continue;
            }
            item.dialect = vendor;
            try {
                item.text = new String(readAll(file), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                ctx.error("Could not read migration " + file + ": " + failure.getMessage());
                continue;
            }
            if (!checkText(ctx, file, item.text)) {
                continue;
            }
            item.checksum = checksum(item.text);
            File conf = new File(dir, name + ".conf");
            if (conf.isFile()) {
                readConf(ctx, conf, item);
            }
            items.add(item);
        }
    }

    /// `V<version>__<description>.sql` or `R__<description>.sql`.
    private static Item parseName(ProcessorContext ctx, File file) {
        String name = file.getName();
        String stem = name.substring(0, name.length() - ".sql".length());
        int separator = stem.indexOf("__");
        if (stem.startsWith("U") && separator > 0 && isVersion(stem.substring(1, separator))) {
            ctx.error("Undo migrations are not supported: " + file + ". Keep rollback scripts outside the "
                    + "migration directory, and write a forward migration to undo a change.");
            return null;
        }
        if (separator < 1 || separator + 2 >= stem.length()) {
            ctx.error("Not a migration file name: " + file + ". Name it V<version>__<description>.sql, "
                    + "such as V3__add_created_column.sql, or R__<description>.sql for a repeatable script.");
            return null;
        }
        Item item = new Item();
        item.scriptName = name;
        item.description = stem.substring(separator + 2).replace('_', ' ');
        String prefix = stem.substring(0, separator);
        if ("R".equals(prefix)) {
            return item;
        }
        if (!prefix.startsWith("V") || !isVersion(prefix.substring(1))) {
            ctx.error("Not a migration file name: " + file + ". The part before the double underscore must "
                    + "be V followed by a version made of digits, dots and underscores, such as V3 or V2026_05_21.");
            return null;
        }
        item.version = prefix.substring(1).replace('_', '.');
        return item;
    }

    /// The runtime's rule, restated: digit runs of up to 18 digits separated by single dots or
    /// underscores. The build does not depend on the runtime classes, and a version the build
    /// accepted and the runtime refused would only surface on a device.
    static boolean isVersion(String version) {
        if (version.length() == 0) {
            return false;
        }
        int digits = 0;
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c >= '0' && c <= '9') {
                if (++digits > 18) {
                    return false;
                }
            } else if (c == '.' || c == '_') {
                if (digits == 0) {
                    return false;
                }
                digits = 0;
            } else {
                return false;
            }
        }
        return digits > 0;
    }

    /// A version with its parts as numbers and trailing zero parts dropped, so `1`, `1.0` and
    /// `01` are one key, as they are one version at run time.
    static String canonical(String version) {
        List<String> parts = new ArrayList<String>();
        StringBuilder part = new StringBuilder();
        for (int i = 0; i <= version.length(); i++) {
            char c = i < version.length() ? version.charAt(i) : '.';
            if (c == '.' || c == '_') {
                parts.add(String.valueOf(Long.parseLong(part.toString())));
                part.setLength(0);
            } else {
                part.append(c);
            }
        }
        while (parts.size() > 1 && "0".equals(parts.get(parts.size() - 1))) {
            parts.remove(parts.size() - 1);
        }
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (out.length() > 0) {
                out.append('.');
            }
            out.append(p);
        }
        return out.toString();
    }

    private static boolean checkText(ProcessorContext ctx, File file, String text) {
        boolean content = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < ' ' && c != '\t' && c != '\n' && c != '\r') {
                ctx.error("Migration " + file + " contains a control character (code " + (int) c
                        + ") at offset " + i + ".");
                return false;
            }
            if (c > ' ' && c != 0xFEFF) {
                content = true;
            }
        }
        if (!content) {
            ctx.error("Migration " + file + " is empty.");
            return false;
        }
        return true;
    }

    /// The one script setting that is honoured, in Flyway's file and spelling.
    private static void readConf(ProcessorContext ctx, File conf, Item item) {
        Properties settings = new Properties();
        try {
            InputStream in = new FileInputStream(conf);
            try {
                settings.load(in);
            } finally {
                in.close();
            }
        } catch (IOException failure) {
            ctx.error("Could not read " + conf + ": " + failure.getMessage());
            return;
        }
        for (String key : settings.stringPropertyNames()) {
            String value = settings.getProperty(key).trim();
            if ("executeInTransaction".equals(key) && ("true".equals(value) || "false".equals(value))) {
                item.transactional = "true".equals(value);
            } else {
                ctx.error("Unsupported setting in " + conf + ": " + key + "=" + value
                        + ". Only executeInTransaction=true|false is read.");
            }
        }
    }

    private static void checkDuplicates(ProcessorContext ctx, List<Item> items) {
        Map<String, Item> seen = new HashMap<String, Item>();
        Map<String, Item> common = new HashMap<String, Item>();
        for (Item item : items) {
            String identity = item.version == null ? "R:" + item.description : "V:" + canonical(item.version);
            String key = identity + "@" + (item.dialect == null ? "" : item.dialect);
            Item earlier = seen.put(key, item);
            if (earlier != null) {
                ctx.error("Two migrations share " + label(item) + ": " + where(earlier) + " and " + where(item)
                        + ".");
            }
            if (item.dialect == null) {
                common.put(identity, item);
            }
        }
        for (Item item : items) {
            if (item.dialect == null) {
                continue;
            }
            String identity = item.version == null ? "R:" + item.description : "V:" + canonical(item.version);
            Item shared = common.get(identity);
            if (shared != null) {
                ctx.error(label(item) + " has a script for every engine (" + where(shared) + ") and one for "
                        + item.dialect + " (" + where(item) + "). Keep one or the other.");
            }
        }
    }

    private static String label(Item item) {
        return item.version == null ? "repeatable migration '" + item.description + "'"
                : "version " + item.version;
    }

    private static String where(Item item) {
        if (item.javaClass != null) {
            return item.javaClass;
        }
        return (item.dialect == null ? "" : item.dialect + "/") + item.scriptName;
    }

    /// Adds the annotated Java migrations to the scripts and checks the two against each other.
    static void addJava(ProcessorContext ctx, List<Item> items, List<Item> java) {
        for (Item candidate : java) {
            for (Item item : items) {
                if (item.version != null && canonical(item.version).equals(canonical(candidate.version))) {
                    ctx.error("Two migrations share version " + candidate.version + ": " + where(item) + " and "
                            + candidate.javaClass + ".");
                }
            }
            items.add(candidate);
        }
    }

    private static byte[] readAll(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /// Flyway's checksum: a CRC32 over the UTF-8 bytes of every line, terminators excluded and
    /// a leading byte order mark dropped. The runtime computes the same value for a script
    /// registered as text, with its own CRC32, and a test holds the two equal.
    static int checksum(String text) {
        CRC32 crc = new CRC32();
        int start = text.length() > 0 && text.charAt(0) == 0xFEFF ? 1 : 0;
        StringBuilder line = new StringBuilder();
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                continue;
            }
            line.append(c);
            if (line.length() >= 4096 && !Character.isHighSurrogate(c)) {
                crc.update(line.toString().getBytes(StandardCharsets.UTF_8));
                line.setLength(0);
            }
        }
        crc.update(line.toString().getBytes(StandardCharsets.UTF_8));
        return (int) crc.getValue();
    }

    /// The source of the generated class.
    static String source(List<Item> items, boolean backend) {
        String simple = simpleName(backend);
        StringBuilder sb = new StringBuilder(4096);
        sb.append("package cn1app;\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// The project's migration scripts, compiled in. Registered from the dao bootstrap;\n");
        sb.append("/// each script becomes a string only when its migration runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        if (backend) {
            sb.append("@com.codename1.backend.annotations.Generated\n");
        }
        sb.append("public final class ").append(simple)
                .append(" implements com.codename1.impl.migration.MigrationSource {\n");
        sb.append("    public static com.codename1.migration.MigrationSet create() {\n");
        sb.append("        ").append(simple).append(" source = new ").append(simple).append("();\n");
        sb.append("        com.codename1.migration.MigrationSet.Builder set = "
                + "com.codename1.migration.MigrationSet.builder(\"default\");\n");
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.javaClass != null) {
                sb.append("        set.java(").append(literal(item.version)).append(", ")
                        .append(literal(item.description)).append(", new ").append(item.javaClass)
                        .append("());\n");
                continue;
            }
            sb.append("        set.compiled(").append(item.version == null ? "null" : literal(item.version))
                    .append(", ").append(literal(item.description)).append(", ").append(literal(item.scriptName))
                    .append(", ").append(item.dialect == null ? "null" : literal(item.dialect)).append(", ")
                    .append(item.checksum).append(", ").append(item.transactional).append(", source, ")
                    .append(i).append(");\n");
        }
        sb.append("        return set.build();\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    public String script(int id) {\n");
        sb.append("        switch (id) {\n");
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).javaClass == null) {
                sb.append("            case ").append(i).append(": return script").append(i).append("();\n");
            }
        }
        sb.append("            default: return null;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.javaClass != null) {
                continue;
            }
            sb.append("\n    private static String script").append(i).append("() {\n");
            sb.append("        StringBuilder text = new StringBuilder(").append(item.text.length()).append(");\n");
            for (int at = 0; at < item.text.length(); at += CHUNK) {
                int end = Math.min(item.text.length(), at + CHUNK);
                sb.append("        text.append(").append(literal(item.text.substring(at, end))).append(");\n");
            }
            sb.append("        return text.toString();\n");
            sb.append("    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    static String binaryName(boolean backend) {
        return backend ? BACKEND_BINARY : CLIENT_BINARY;
    }

    private static String simpleName(boolean backend) {
        String binary = binaryName(backend);
        return binary.substring(binary.lastIndexOf('.') + 1);
    }

    /// A Java string literal in pure ASCII: the generated source is compiled wherever the
    /// build runs, with whatever default encoding that machine has.
    static String literal(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < ' ' || c > '~') {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
        return out.toString();
    }
}
