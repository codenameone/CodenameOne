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

package com.codename1.guibuilder.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProjectIO {
    public static final String INPUT_PROPERTY = "guibuilder.input";

    private ProjectIO() { }

    public static ProjectBinding loadBinding() {
        String path = System.getProperty(INPUT_PROPERTY);
        if (path == null || path.trim().length() == 0) return null;
        try {
            String content = read(path);
            ProjectBinding binding = ProjectBinding.parse(content);
            return binding.isValid() ? binding : null;
        } catch (IOException ex) {
            return null;
        }
    }

    public static List<String> findGuiFiles(String guiDir) {
        List<String> files = new ArrayList<>();
        collect(fsUrl(guiDir), files);
        Collections.sort(files);
        return files;
    }

    private static void collect(String dir, List<String> files) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (dir == null || !fs.exists(dir) || !fs.isDirectory(dir)) return;
        try {
            for (String child : fs.listFiles(dir)) {
                String path = dir + (dir.endsWith("/") ? "" : "/") + child;
                if (fs.isDirectory(path)) collect(path, files);
                else if (path.endsWith(".gui")) files.add(path);
            }
        } catch (IOException ignored) { }
    }

    public static String read(String path) throws IOException {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(fsUrl(path));
            return Util.readToString(in, "UTF-8");
        } finally {
            Util.cleanup(in);
        }
    }

    public static boolean exists(String path) {
        return path != null && FileSystemStorage.getInstance().exists(fsUrl(path));
    }

    /**
     * Writes through a sibling temporary file so a failed write cannot destroy the previous
     * content. Opening the target directly truncates it before the first byte arrives, which turns
     * a full disk or a killed process into an empty form, stylesheet or source file.
     */
    public static void write(String path, String content) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String url = fsUrl(path);
        ensureParent(url);
        String temporary = url + ".cn1tmp";
        OutputStream out = null;
        try {
            out = fs.openOutputStream(temporary);
            out.write(content.getBytes("UTF-8"));
            out.close();
            out = null;
        } catch (IOException ex) {
            Util.cleanup(out);
            fs.delete(temporary);
            throw ex;
        } finally {
            Util.cleanup(out);
        }
        if (!fs.exists(temporary)) {
            throw new IOException("Failed to write " + path);
        }
        // rename() cannot replace an existing file on every platform, so the target is removed
        // first. The window this opens is one rename wide and only after the new content is
        // safely on disk, where the truncating write left it open for the whole serialization.
        if (fs.exists(url)) {
            fs.delete(url);
        }
        if (fs.exists(url)) {
            // delete() and rename() both fail silently, and the target that survives a failed
            // replacement still satisfies an exists() check, so a caller would be told the save
            // succeeded while the new content sat in the temporary file.
            fs.delete(temporary);
            throw new IOException("Could not replace " + path + "; it may be open in another program");
        }
        fs.rename(temporary, fileName(url));
        if (!fs.exists(url) || fs.exists(temporary)) {
            throw new IOException("Failed to replace " + path + " with the file just written");
        }
    }

    private static String fileName(String url) {
        int slash = url.lastIndexOf('/');
        return slash < 0 ? url : url.substring(slash + 1);
    }

    private static void ensureParent(String path) {
        int slash = path.lastIndexOf('/');
        if (slash <= "file://".length()) return;
        String parent = path.substring(0, slash);
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (fs.exists(parent)) return;
        ensureParent(parent);
        fs.mkdir(parent);
    }

    /**
     * Normalizes to forward slashes before building the URL. The Maven plugin hands this editor
     * native paths, so on Windows every path arrives with backslashes; leaving them in place makes
     * the separator arithmetic in {@code ensureParent} and {@code fileName} silently find nothing.
     * A drive-lettered path becomes {@code file://C:/...}, which the JavaSE port maps back to
     * {@code C:\...}.
     */
    public static String fsUrl(String path) {
        if (path == null) return null;
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("file://") || normalized.indexOf("://") > 0) return normalized;
        return "file://" + normalized;
    }
}
