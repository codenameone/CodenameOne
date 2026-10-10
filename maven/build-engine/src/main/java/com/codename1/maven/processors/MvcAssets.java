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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.ProcessingException;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Embeds explicitly public assets; templates never enter the public registry. */
final class MvcAssets {
    private MvcAssets() {}

    static Map<String, String> sources(File project) {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        StringBuilder registry =
                new StringBuilder(
                        "package com.codename1.generated.mvc;\n"
                                + "@com.codename1.backend.annotations.Generated public final class"
                                + " Assets implements com.codename1.backend.HttpServer.Handler {\n"
                                + "public com.codename1.backend.HttpServer.Response"
                                + " handle(com.codename1.backend.HttpServer.Request request) {\n"
                                + "if(!\"GET\".equals(request.getMethod()) &&"
                                + " !\"HEAD\".equals(request.getMethod())) return null;\n");
        File root = new File(project, "src/main/resources/static");
        if (Files.isSymbolicLink(root.toPath()))
            throw new IllegalArgumentException("Static asset symlinks are not supported: " + root);
        collect(root, root, sources, registry);
        registry.append("return null; } }\n");
        sources.put("com.codename1.generated.mvc.Assets", registry.toString());
        return sources;
    }

    /** Remove only obsolete bytecode carrying our generator marker, after a successful compile. */
    static void removeObsoleteClasses(File output, Set<String> currentSources)
            throws IOException, ProcessingException {
        File directory = new File(output, "com/codename1/generated/mvc");
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (!name.matches("(Views|Assets|Asset[0-9]+)\\.class")) continue;
            String type = "com.codename1.generated.mvc." + name.substring(0, name.length() - 6);
            if (!currentSources.contains(type)
                    && ClassScanner.readClass(file)
                            .getClassAnnotations()
                            .containsKey("Lcom/codename1/backend/annotations/Generated;")) {
                Files.delete(file.toPath());
            }
        }
    }

    private static void collect(
            File root, File directory, Map<String, String> sources, StringBuilder registry) {
        File[] files = directory.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File file : files) {
            if (Files.isSymbolicLink(file.toPath()))
                throw new IllegalArgumentException(
                        "Static asset symlinks are not supported: " + file);
            if (file.isDirectory()) {
                collect(root, file, sources, registry);
                continue;
            }
            String path =
                    "/static/"
                            + root.toPath()
                                    .relativize(file.toPath())
                                    .toString()
                                    .replace(File.separatorChar, '/');
            if (!path.matches("/static/[A-Za-z0-9_./-]+") || path.contains("/../"))
                throw new IllegalArgumentException(
                        "Asset path requires ASCII URL-safe characters: " + path);
            byte[] data;
            try {
                checkSize(Files.size(file.toPath()), file);
                data = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read asset " + file, e);
            }
            checkSize(data.length, file);
            String name = "Asset" + sources.size();
            StringBuilder s =
                    new StringBuilder(
                            "package com.codename1.generated.mvc;\n"
                                    + "@com.codename1.backend.annotations.Generated final class "
                                    + name
                                    + " {\n"
                                    + "static final byte[] DATA = load();\n"
                                    + "static byte[] load() { byte[] data = new byte["
                                    + data.length
                                    + "];\n");
            int chunks = (data.length + 1023) / 1024;
            for (int chunk = 0; chunk < chunks; chunk++)
                s.append("System.arraycopy(part")
                        .append(chunk)
                        .append("(), 0, data, ")
                        .append(chunk * 1024)
                        .append(", ")
                        .append(Math.min(1024, data.length - chunk * 1024))
                        .append(");\n");
            s.append("return data; }\n");
            for (int chunk = 0; chunk < chunks; chunk++) {
                s.append("static byte[] part").append(chunk).append("() { return new byte[] {");
                for (int i = chunk * 1024; i < Math.min(data.length, (chunk + 1) * 1024); i++) {
                    if (i > chunk * 1024) s.append(',');
                    s.append(data[i]);
                }
                s.append("}; }\n");
            }
            s.append("}\n");
            sources.put("com.codename1.generated.mvc." + name, s.toString());
            registry.append("if(")
                    .append(MvcForms.q(path))
                    .append(
                            ".equals(request.pathFrom(0))) return new"
                                    + " com.codename1.backend.HttpServer.Response(200, ")
                    .append(MvcForms.q(mime(path)))
                    .append(", ")
                    .append(name)
                    .append(
                            ".DATA).header(\"Cache-Control\","
                                    + " \"no-cache\").header(\"X-Content-Type-Options\","
                                    + " \"nosniff\");\n");
        }
    }

    private static void checkSize(long size, File file) {
        if (size > 2 * 1024 * 1024)
            throw new IllegalArgumentException(
                    "Embedded MVC assets must be at most 2 MiB; serve larger files with"
                            + " cn1.static.root: "
                            + file);
    }

    private static String mime(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        if ("html".equals(ext) || "htm".equals(ext)) {
            return "text/html; charset=utf-8";
        }
        if ("css".equals(ext)) {
            return "text/css; charset=utf-8";
        }
        if ("js".equals(ext) || "mjs".equals(ext)) {
            return "text/javascript; charset=utf-8";
        }
        if ("json".equals(ext)) {
            return "application/json; charset=utf-8";
        }
        if ("svg".equals(ext)) {
            return "image/svg+xml";
        }
        if ("png".equals(ext)) {
            return "image/png";
        }
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "image/jpeg";
        }
        if ("gif".equals(ext)) {
            return "image/gif";
        }
        if ("webp".equals(ext)) {
            return "image/webp";
        }
        if ("ico".equals(ext)) {
            return "image/x-icon";
        }
        if ("woff2".equals(ext)) {
            return "font/woff2";
        }
        if ("woff".equals(ext)) {
            return "font/woff";
        }
        if ("ttf".equals(ext)) {
            return "font/ttf";
        }
        if ("wasm".equals(ext)) {
            return "application/wasm";
        }
        if ("pdf".equals(ext)) {
            return "application/pdf";
        }
        if ("txt".equals(ext) || "md".equals(ext)) {
            return "text/plain; charset=utf-8";
        }
        if ("xml".equals(ext)) {
            return "application/xml";
        }
        if ("mp4".equals(ext)) {
            return "video/mp4";
        }
        if ("zip".equals(ext)) {
            return "application/zip";
        }
        return "application/octet-stream";
    }
}
