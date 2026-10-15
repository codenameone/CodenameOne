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
package com.codename1.builders;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Finalizes a browser bundle after resource relocation and proxy URL injection.
 * Keep the BuildDaemon copy identical apart from its package and BuildRequest import. */
final class JavaScriptPwa {
    private static final String MARKER = "// CN1 PWA CACHE\n";
    private static final String HEAD = "<link rel=\"manifest\" href=\"manifest.json\">\n"
            + "<link rel=\"apple-touch-icon\" href=\"_icons/icon-180.png\">\n";
    private static final String SCRIPT = "<script src=\"cn1-pwa.js\"></script>\n";

    private JavaScriptPwa() { }

    static void write(File app, BuildRequest request) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        String name = request.getDisplayName();
        if (name == null || name.trim().isEmpty()) name = "Codename One App";
        manifest.put("name", name);
        manifest.put("short_name", request.getArg("javascript.short_name", name));
        manifest.put("display", "standalone");
        manifest.put("start_url", "./index.html");
        manifest.put("scope", "./");
        manifest.put("background_color", request.getArg("javascript.background_color", "#ffffff"));
        manifest.put("theme_color", request.getArg("javascript.theme_color", "#2367f1"));
        String orientation = request.getArg("javascript.orientation", null);
        if (orientation != null) manifest.put("orientation", orientation);
        if (request.getArg("gcm.sender_id", null) != null) {
            manifest.put("gcm_sender_id", request.getArg("gcm.sender_id", ""));
        }
        List<Map<String, String>> icons = new ArrayList<Map<String, String>>();
        BufferedImage icon = request.getIcon() == null ? null
                : ImageIO.read(new ByteArrayInputStream(request.getIcon()));
        if (icon == null && request.getIcon() != null) throw new IOException("Invalid JavaScript application icon");
        for (int size : new int[] {180, 192, 512}) {
            BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = resized.createGraphics();
            try {
                if (icon == null) {
                    g.setColor(new Color(35, 103, 241));
                    g.fillRect(0, 0, size, size);
                } else {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(icon, 0, 0, size, size, null);
                }
            } finally { g.dispose(); }
            String path = "_icons/icon-" + size + ".png";
            File target = new File(app, path);
            Files.createDirectories(target.toPath().getParent());
            ImageIO.write(resized, "png", target);
            Map<String, String> item = new LinkedHashMap<String, String>();
            item.put("src", path);
            item.put("sizes", size + "x" + size);
            item.put("type", "image/png");
            icons.add(item);
        }
        manifest.put("icons", icons);
        String prefix = "javascript.manifest.";
        Map<String, Map<String, String>> related = new TreeMap<String, Map<String, String>>();
        for (String key : new TreeSet<String>(request.getArgs())) {
            if (!key.startsWith(prefix)) continue;
            String field = key.substring(prefix.length());
            String value = request.getArg(key, "");
            if (field.startsWith("related_applications.")) {
                String[] parts = field.split("\\.", 3);
                if (parts.length != 3) throw new IOException("Invalid manifest hint " + key);
                Map<String, String> entry = related.get(parts[1]);
                if (entry == null) {
                    entry = new LinkedHashMap<String, String>();
                    entry.put("platform", parts[1]);
                    related.put(parts[1], entry);
                }
                entry.put(parts[2], value);
            } else {
                manifest.put(field, manifestValue(field, value));
            }
        }
        if (!related.isEmpty() && !manifest.containsKey("related_applications")) {
            manifest.put("related_applications", new ArrayList<Map<String, String>>(related.values()));
        }
        write(new File(app, "manifest.json"), json(manifest));
        File page = new File(app, "index.html");
        String html = read(page);
        if (!html.contains(HEAD)) html = html.replace("</head>", HEAD + "</head>");
        if (!html.contains(SCRIPT)) html = html.replace("</body>", SCRIPT + "</body>");
        write(page, html);
        write(new File(app, "cn1-pwa.js"), resource("pwa.js"));

        // Preserve the port's push/message/notification handlers, replacing only UpUp's
        // TeaVM cache adapter. A rebuild replaces our generated section as well.
        File worker = new File(app, "sw.js");
        String push = worker.isFile() ? read(worker) : "";
        int cut = push.indexOf(MARKER);
        if (cut < 0) cut = push.indexOf("//! UpUp Service Worker");
        if (cut >= 0) push = push.substring(0, cut);
        String cacheWorker = resource("pwa-worker.js");
        List<File> files = new ArrayList<File>();
        collect(app, files);
        Collections.sort(files);
        List<String> urls = new ArrayList<String>();
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
        digest.update(push.getBytes(StandardCharsets.UTF_8));
        digest.update(cacheWorker.getBytes(StandardCharsets.UTF_8));
        byte[] buffer = new byte[8192];
        for (File file : files) {
            String path = app.toURI().relativize(file.toURI()).toASCIIString();
            if ("sw.js".equals(path)) continue;
            urls.add(path);
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            try (InputStream in = new FileInputStream(file)) {
                int count;
                while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
        }
        StringBuilder version = new StringBuilder();
        for (byte b : digest.digest()) version.append(String.format(Locale.ROOT, "%02x", b & 255));
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("version", version.toString());
        config.put("assets", urls);
        config.put("startUrl", manifest.get("start_url"));
        write(worker, push + MARKER + "self.cn1Pwa = " + json(config) + ";\n" + cacheWorker);
    }

    private static Object manifestValue(String field, String value) throws IOException {
        if (Arrays.asList("name", "short_name", "description", "display", "orientation",
                "start_url", "scope", "id", "background_color", "theme_color", "lang", "dir").contains(field)) {
            return value;
        }
        String trimmed = value.trim();
        if ("true".equals(trimmed) || "false".equals(trimmed)) return Boolean.valueOf(trimmed);
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                JSONTokener tokens = new JSONTokener(trimmed);
                Object parsed = tokens.nextValue();
                if (tokens.nextClean() != 0) throw new JSONException("Trailing content");
                return parsed;
            } catch (JSONException invalid) {
                throw new IOException("Invalid JSON in javascript.manifest." + field, invalid);
            }
        }
        return value;
    }

    // org.json is already part of the daemon's JavaPNS runtime. Do not use the
    // Codename One UI runtime here: the production daemon's classpath omits it.
    private static String json(Map<String, Object> value) throws IOException {
        try {
            return new JSONObject(value).toString(2);
        } catch (JSONException invalid) {
            throw new IOException("Cannot encode PWA metadata", invalid);
        }
    }

    private static void collect(File dir, List<File> files) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) throw new IOException("Cannot list bundle " + dir);
        for (File child : children) {
            if (Files.isSymbolicLink(child.toPath())) throw new IOException("Symlink in browser bundle: " + child);
            if (child.isDirectory()) collect(child, files);
            else files.add(child);
        }
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = JavaScriptPwa.class.getResourceAsStream("javascript/" + name)) {
            if (in == null) throw new IOException("Missing PWA resource " + name);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
    private static void write(File file, String value) throws IOException {
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }
}
