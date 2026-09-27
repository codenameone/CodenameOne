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
package com.codename1.doclet.hugo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The two references the site publishes from one doclet: the client API and the
 * backend API.
 *
 * <p>A reader who lands on one page from a search engine sees nothing else, so
 * what matters is that every page says which reference it belongs to, that a
 * class compiled into both halves says so on both, and that its backend copy
 * names the client page as canonical rather than competing with it.
 *
 * <p>Both runs happen in this one JVM, backend first. The URL root is static in
 * {@link Refs}, so this is also the check that a run does not inherit the
 * previous one's root: the client run's URLs must still start at /javadoc/.
 */
class HugoDocletAudienceTest {

    @TempDir
    static Path workspace;

    private static Path backend;
    private static Path client;
    private static Path backendIndex;

    @BeforeAll
    static void generate() throws IOException {
        Path src = workspace.resolve("src");
        write(src, "com/codename1/impl/SharedWithBackend.java",
                "package com.codename1.impl;",
                "import java.lang.annotation.*;",
                "@Retention(RetentionPolicy.SOURCE)",
                "@Target(ElementType.TYPE)",
                "public @interface SharedWithBackend {}");
        write(src, "s/Shared.java",
                "package s;",
                "/// Compiled into the app and the server alike.",
                "@com.codename1.impl.SharedWithBackend",
                "public class Shared {",
                "    /// Nested, and shared along with its outer type.",
                "    public static class Inner {}",
                "}");
        write(src, "s/package-info.java",
                "/// A package whose every type is shared.",
                "package s;");
        write(src, "b/Server.java",
                "package b;",
                "/// Only on the server. Uses [s.Shared].",
                "public class Server {",
                "    /// Takes a shared type.",
                "    public void take(s.Shared value) {}",
                "}");
        write(src, "b/package-info.java",
                "/// Server-only package.",
                "package b;");
        // A separate source tree whose every type is shared, the way the java.*
        // classes under Ports/CLDC11/src are: no annotation, just where it lives.
        Path lib = workspace.resolve("lib");
        write(lib, "l/Lib.java",
                "package l;",
                "/// Part of the class library both halves compile against.",
                "public class Lib {}");

        backend = workspace.resolve("backend");
        client = workspace.resolve("client");
        backendIndex = workspace.resolve("backend-javadoc-search.json");

        run(src, lib, List.of("s/Shared.java", "b/Server.java", "s/package-info.java",
                        "b/package-info.java", "../lib/l/Lib.java"),
                backend, backendIndex,
                "--audience", "backend", "--url-root", "/backend/javadoc/",
                "--counterpart-root", "/javadoc/", "--shared-sources", lib.toString());
        run(src, lib, List.of("s/Shared.java", "s/package-info.java", "../lib/l/Lib.java"),
                client, workspace.resolve("javadoc-search.json"),
                "--audience", "client", "--counterpart-root", "backend/javadoc");
    }

    private static void write(Path root, String relative, String... lines) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static void run(Path src, Path lib, List<String> files, Path out, Path index,
                            String... extra) throws IOException {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager manager = tool.getStandardFileManager(null, null, null)) {
            List<Path> paths = new ArrayList<>();
            for (String file : files) {
                paths.add(src.resolve(file));
            }
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromPaths(paths);
            List<String> options = new ArrayList<>(List.of("-d", out.toString(),
                    "--search-index", index.toString(),
                    "-sourcepath", src + java.io.File.pathSeparator + lib,
                    "-protected", "-quiet"));
            options.addAll(List.of(extra));
            assertTrue(tool.getTask(null, manager, null, HugoDoclet.class, options, units).call(),
                    "the doclet run failed");
        }
    }

    private static String read(Path root, String relative) throws IOException {
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }

    @Test
    void labelsEveryBackendPageAsTheBackendAndRendersItThroughTheApiTemplates() throws IOException {
        String page = read(backend, "b/Server.md");
        assertTrue(page.contains("\"url\": \"/backend/javadoc/b/Server/\""), page);
        assertTrue(page.contains("\"audience\": \"backend\""), page);
        // The content lives under content/backend/javadoc, which Hugo calls the
        // "backend" section; without the type it would never reach layouts/javadoc.
        assertTrue(page.contains("\"type\": \"javadoc\""), page);
        // What a search result shows: the title and the description.
        assertTrue(page.contains("\"seoTitle\": \"Server (Backend API)\""), page);
        assertTrue(page.contains("\"description\": \"Backend API. Only on the server."), page);
        assertTrue(page.contains("\"shared\": false"), page);
        // Only a shared type has a page of its own in the other reference.
        assertTrue(page.contains("\"counterpart\": null"), page);
        assertFalse(page.contains("canonicalURL"), page);
    }

    @Test
    void linksWithinTheBackendReferenceStayUnderItsRoot() throws IOException {
        String page = read(backend, "b/Server.md");
        assertTrue(page.contains("/backend/javadoc/s/Shared/"), page);
        assertFalse(page.contains("\"/javadoc/s/Shared/\""), page);
    }

    @Test
    void marksASharedTypeInBothReferencesAndLinksItsCounterpart() throws IOException {
        String onBackend = read(backend, "s/Shared.md");
        assertTrue(onBackend.contains("\"shared\": true"), onBackend);
        assertTrue(onBackend.contains("\"counterpart\": \"/javadoc/s/Shared/\""), onBackend);
        assertTrue(onBackend.contains("\"seoTitle\": \"Shared (Client and Backend API)\""), onBackend);
        // The client page is the one kept; the backend copy defers to it.
        assertTrue(onBackend.contains("\"canonicalURL\": \"/javadoc/s/Shared/\""), onBackend);

        String onClient = read(client, "s/Shared.md");
        assertTrue(onClient.contains("\"shared\": true"), onClient);
        assertTrue(onClient.contains("\"counterpart\": \"/backend/javadoc/s/Shared/\""), onClient);
        assertFalse(onClient.contains("canonicalURL"), onClient);
    }

    @Test
    void sharesANestedTypeAlongWithItsOuterType() throws IOException {
        String nested = read(backend, "s/Shared.Inner.md");
        assertTrue(nested.contains("\"shared\": true"), nested);
        assertTrue(nested.contains("\"counterpart\": \"/javadoc/s/Shared.Inner/\""), nested);
    }

    @Test
    void callsAPackageSharedOnlyWhenEveryTypeInItIs() throws IOException {
        assertTrue(read(backend, "s/package-summary.md").contains("\"shared\": true"));
        assertTrue(read(backend, "b/package-summary.md").contains("\"shared\": false"));
    }

    @Test
    void publishesEachOverviewAtItsOwnRootAndKeepsTheLegacyAliasOnTheClient() throws IOException {
        String backendOverview = read(backend, "_index.md");
        assertTrue(backendOverview.contains("\"url\": \"/backend/javadoc/\""), backendOverview);
        assertTrue(backendOverview.contains("\"title\": \"Codename One Backend API\""), backendOverview);
        assertFalse(backendOverview.contains("/api/"), backendOverview);

        String clientOverview = read(client, "_index.md");
        assertTrue(clientOverview.contains("\"url\": \"/javadoc/\""), clientOverview);
        assertTrue(clientOverview.contains("\"/api/\""), clientOverview);
        // The option was written without slashes; it must still come out as a
        // site path, or the switch would be a relative link.
        assertTrue(clientOverview.contains("\"counterpartRoot\": \"/backend/javadoc/\""), clientOverview);
    }

    @Test
    void marksEveryTypeFromASharedSourceTreeAsShared() throws IOException {
        // The backend run names the tree; its type is shared and defers to the
        // client page.
        String onBackend = read(backend, "l/Lib.md");
        assertTrue(onBackend.contains("\"shared\": true"), onBackend);
        assertTrue(onBackend.contains("\"canonicalURL\": \"/javadoc/l/Lib/\""), onBackend);
        // The client run does not, so the same type is its own there. The
        // option is what decides, not the file's location on disk.
        String onClient = read(client, "l/Lib.md");
        assertTrue(onClient.contains("\"shared\": false"), onClient);
    }

    @Test
    void aClientRunAfterABackendRunDoesNotInheritItsRoot() throws IOException {
        String page = read(client, "s/Shared.md");
        assertTrue(page.contains("\"url\": \"/javadoc/s/Shared/\""), page);
        assertTrue(page.contains("\"audience\": \"client\""), page);
    }

    @Test
    void labelsTheSearchIndexSoTheSiteCanMergeBothWithoutDuplicates() throws IOException {
        String index = Files.readString(backendIndex, StandardCharsets.UTF_8);
        assertTrue(index.contains("\"audience\":\"backend\""), index);
        assertTrue(index.contains("\"n\":\"Server\""), index);
        assertTrue(index.contains("\"a\":\"backend\""), index);
        // The shared row is flagged, which is what the search page drops from
        // the backend index in favour of the client's copy.
        int shared = index.indexOf("\"n\":\"Shared\"");
        assertTrue(shared >= 0, index);
        int end = index.indexOf('}', shared);
        assertTrue(index.substring(shared, end).contains("\"sh\":1"), index);
    }
}
