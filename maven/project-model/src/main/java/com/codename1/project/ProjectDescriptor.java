/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The project facts a build tool resolved, written to a file for tools that
/// run outside the build.
///
/// A build plugin knows things no directory walk can: the source roots a
/// profile added, the main class a `-D` override picked. It writes them here
/// (see [ProjectLayout#descriptorFile()]), and the simulator and the desktop
/// tools (Settings, GUI Builder, Game Builder, Certificate Wizard) read them
/// back instead of guessing.
///
/// The format is the one the desktop tools' launch bindings already parse: one
/// `key=value` per line, split at the first `=`, `#` comments, and a key
/// repeated for a list (`sourceRoot=`). Values are written raw, not with
/// `java.util.Properties` escaping, so a Windows path reads back unchanged in
/// a tool that has no `Properties` class. A tool adds its own keys beside the
/// standard ones and ignores keys it does not know.
///
/// Standard keys:
///
/// | key | value |
/// |---|---|
/// | `buildSystem` | `ANT`, `MAVEN` or `GRADLE` |
/// | `kind` | `APP`, `LIB` or `BACKEND` |
/// | `rootDir` | [ProjectLayout#rootDir()] |
/// | `projectDir` | [ProjectLayout#projectDir()] |
/// | `settings` | [ProjectLayout#settingsFile()] |
/// | `dependencyFile` | [ProjectLayout#dependencyFile()] |
/// | `pom` | the same file, Maven only (the key older tools read) |
/// | `multimoduleRoot` | [ProjectLayout#rootDir()] when it differs from `projectDir` |
/// | `sourceRoot` | one per resolved source root |
/// | `sourceEncoding`, `mainName`, `packageName` | as the build resolved them |
public final class ProjectDescriptor {
    /// The descriptor's file name.
    public static final String FILE_NAME = "project.properties";

    private final Map<String, List<String>> values = new LinkedHashMap<String, List<String>>();

    /// An empty descriptor.
    public ProjectDescriptor() {
    }

    /// A descriptor carrying everything `layout` knows.
    public static ProjectDescriptor fromLayout(ProjectLayout layout) {
        ProjectDescriptor d = new ProjectDescriptor();
        d.set("buildSystem", layout.buildSystem().name());
        d.set("kind", layout.kind().name());
        d.set("rootDir", layout.rootDir().getAbsolutePath());
        d.set("projectDir", layout.projectDir().getAbsolutePath());
        d.set("settings", layout.settingsFile().getAbsolutePath());
        d.set("dependencyFile", layout.dependencyFile().getAbsolutePath());
        if (layout.buildSystem() == BuildSystem.MAVEN) {
            d.set("pom", layout.dependencyFile().getAbsolutePath());
        }
        if (!layout.rootDir().equals(layout.projectDir())) {
            d.set("multimoduleRoot", layout.rootDir().getAbsolutePath());
        }
        for (File root : layout.sourceRoots()) {
            d.add("sourceRoot", root.getAbsolutePath());
        }
        return d;
    }

    /// Parses descriptor text. Never throws; malformed lines are skipped.
    public static ProjectDescriptor parse(String content) {
        ProjectDescriptor d = new ProjectDescriptor();
        if (content == null) {
            return d;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String val = trimmed.substring(eq + 1).trim();
            if (val.length() > 0) {
                d.add(key, val);
            }
        }
        return d;
    }

    /// Reads a descriptor file.
    public static ProjectDescriptor read(File f) throws IOException {
        return parse(ProjectLayouts.readText(f));
    }

    /// The text [parse(String)] reads back.
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Written by the Codename One build; read by the simulator and desktop tools.\n");
        for (Map.Entry<String, List<String>> e : values.entrySet()) {
            for (String v : e.getValue()) {
                sb.append(e.getKey()).append('=').append(v).append('\n');
            }
        }
        return sb.toString();
    }

    /// Writes [format()] to `f`, creating its directory.
    public void write(File f) throws IOException {
        File dir = f.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Could not create " + dir);
        }
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(format().getBytes(Charset.forName("UTF-8")));
        } finally {
            out.close();
        }
    }

    /// Replaces every value of `key` with `value`; a null value removes it.
    public ProjectDescriptor set(String key, String value) {
        values.remove(key);
        if (value != null) {
            add(key, value);
        }
        return this;
    }

    /// Adds one more value for `key`.
    public ProjectDescriptor add(String key, String value) {
        List<String> list = values.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            values.put(key, list);
        }
        list.add(value);
        return this;
    }

    /// The first value of `key`, or null.
    public String get(String key) {
        List<String> list = values.get(key);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /// Every value of `key`, possibly empty.
    public List<String> getAll(String key) {
        List<String> list = values.get(key);
        return list == null ? Collections.<String>emptyList() : Collections.unmodifiableList(list);
    }

    /// The layout this descriptor describes, or null when it lacks the
    /// build system, kind or directories.
    public ProjectLayout toLayout() {
        BuildSystem bs = enumValue(BuildSystem.class, get("buildSystem"));
        ProjectKind kind = enumValue(ProjectKind.class, get("kind"));
        String root = get("rootDir");
        String project = get("projectDir");
        if (bs == null || kind == null || root == null || project == null) {
            return null;
        }
        List<File> roots = new ArrayList<File>();
        for (String r : getAll("sourceRoot")) {
            roots.add(new File(r));
        }
        return new ProjectLayout(bs, kind, new File(root), new File(project), roots);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String name) {
        if (name == null) {
            return null;
        }
        for (T t : type.getEnumConstants()) {
            if (t.name().equals(name)) {
                return t;
            }
        }
        return null;
    }
}
