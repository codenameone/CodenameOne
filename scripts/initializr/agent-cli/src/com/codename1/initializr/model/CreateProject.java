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
package com.codename1.initializr.model;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// The Initializr's "Generate" button as a command, for agents and scripts that
/// have no browser. It runs the very generator start.codenameone.com runs, so a
/// project made here is byte for byte the site's download for the same choices.
///
/// Published by the website build as `/agents/cn1-create.jar` and driven by the
/// bootstrap skill's `create-app.sh` / `create-app.ps1`, which supply the
/// Codename One core and JavaSE jars on the class path at the release the
/// generator pins.
///
/// ```
/// java -cp <cp> com.codename1.initializr.model.CreateProject --list
/// java -cp <cp> com.codename1.initializr.model.CreateProject --name HabitTracker
///     --package com.acme.habits [--template BAREBONES] [--build maven|gradle]
///     [--type app|app-with-backend] [--theme light|dark] [--brand 1f5eff]
///     [--icon icon.png] [--dir HabitTracker | --zip out.zip]
/// ```
///
/// Exit status: 0 on success, 1 when generation fails, 2 for a bad command line.
public final class CreateProject {

    private CreateProject() {
    }

    /// One line per template, for `--list` and for the agent page: what the
    /// template is, so an agent can start from the closest one.
    private static String describe(Template t) {
        switch (t) {
            case BAREBONES: return "Empty Java app: one form, CSS theme. Start here for anything not listed below.";
            case KOTLIN: return "Empty Kotlin app: one form, CSS theme.";
            case WAYLINE: return "Ride hailing (Uber-style): rider and driver apps, admin console, Java backend, live tracking, payments.";
            case SAVOR: return "Food delivery (Uber Eats-style): restaurants, menus, cart, checkout, order tracking, Java backend.";
            case BAZAAR: return "Commerce platform (Shopify-style): storefront, merchant back office, point of sale, inventory, Java backend.";
            case SKIRMISH: return "Real-time card battle game (arena style) with matchmaking server.";
            case DASH: return "Endless runner game.";
            case SPLAT: return "3D first-person team paintball shooter with authoritative server, voice and replays.";
            case PARLEY: return "End-to-end encrypted messaging: chats, groups, WebRTC calls, Java backend.";
            case BASTION: return "Mobile banking: accounts, transfers, cards, biometric sign-in, security showcase, Java backend.";
            case REEL: return "Video streaming: catalogue, player, downloads, DRM, profiles, Java backend.";
            case PULSE: return "Gym / studio: class schedule, bookings, memberships, check-in, Java backend.";
            case SLATE: return "Appointment booking: providers, rooms, deposits, waitlist, calendar sync, Java backend.";
            default: return t.name();
        }
    }

    public static void main(String[] args) {
        String template = "BAREBONES";
        String name = null;
        String pkg = null;
        String build = "maven";
        String type = null;
        String theme = "light";
        String brand = null;
        String icon = null;
        String dir = null;
        String zip = null;
        boolean list = false;
        try {
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if ("--list".equals(a)) {
                    list = true;
                    continue;
                }
                if ("--help".equals(a) || "-h".equals(a)) {
                    usage(null);
                    System.exit(0);
                }
                if (i + 1 >= args.length) {
                    usage("missing value for " + a);
                    System.exit(2);
                }
                String v = args[++i];
                switch (a) {
                    case "--template": template = v.toUpperCase(java.util.Locale.ROOT); break;
                    case "--name": name = v; break;
                    case "--package": pkg = v; break;
                    case "--build": build = v.toLowerCase(java.util.Locale.ROOT); break;
                    case "--type": type = v.toLowerCase(java.util.Locale.ROOT); break;
                    case "--theme": theme = v.toLowerCase(java.util.Locale.ROOT); break;
                    case "--brand": brand = v.startsWith("#") ? v.substring(1) : v; break;
                    case "--icon": icon = v; break;
                    case "--dir": dir = v; break;
                    case "--zip": zip = v; break;
                    default:
                        usage("unknown option " + a);
                        System.exit(2);
                }
            }
        } catch (RuntimeException e) {
            usage(e.getMessage());
            System.exit(2);
        }

        bootResources();
        if (list) {
            for (Template t : Template.values()) {
                if (GeneratorModel.isTemplateOffered(t)) {
                    System.out.println(t.name() + (t.isFullStack() ? " [app + backend]" : "")
                            + (t.supportsGradle() ? "" : " [maven only]") + " - " + describe(t));
                }
            }
            System.exit(0);
        }

        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9]*")) {
            usage("--name must be a Java class name, e.g. HabitTracker");
            System.exit(2);
        }
        if (pkg == null || !pkg.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) {
            usage("--package must be a lower-case Java package with at least two parts, e.g. com.acme.habits");
            System.exit(2);
        }
        Template t;
        try {
            t = Template.valueOf(template);
        } catch (IllegalArgumentException e) {
            usage("unknown template " + template + "; run with --list");
            System.exit(2);
            return;
        }
        if (!GeneratorModel.isTemplateOffered(t)) {
            usage("template " + template + " is not offered at this release; run with --list");
            System.exit(2);
        }
        // A complete-app template ships as its own download (templates/<name>.jar
        // beside cn1-create.jar), so the common case stays small. create-app.sh
        // adds it to the class path; without it the generator would fail deep
        // inside with a missing resource, so say what is missing here.
        String probe = "/" + template.toLowerCase(java.util.Locale.ROOT) + "-pom.xml";
        if (CreateProject.class.getResource(probe) == null && t != Template.BAREBONES && t != Template.KOTLIN) {
            System.err.println("The " + template + " template is not on the class path. It is downloaded"
                    + " separately: create-app.sh --template " + template + " fetches templates/"
                    + template.toLowerCase(java.util.Locale.ROOT) + ".jar from codenameone.com/agents/.");
            System.exit(1);
        }
        if (dir == null && zip == null) {
            dir = name;
        }

        try {
            ProjectOptions options = ProjectOptions.defaults().withScheme(-1, true);
            if ("dark".equals(theme)) {
                options = new ProjectOptions(ProjectOptions.ThemeMode.DARK, ProjectOptions.Accent.DEFAULT, true,
                        false, ProjectOptions.PreviewLanguage.ENGLISH, ProjectOptions.JavaVersion.JAVA_17);
            } else if (!"light".equals(theme)) {
                usage("--theme is light or dark");
                System.exit(2);
            }
            ProjectOptions.BuildTool tool;
            if ("maven".equals(build)) {
                tool = ProjectOptions.BuildTool.MAVEN;
            } else if ("gradle".equals(build)) {
                tool = ProjectOptions.BuildTool.GRADLE;
            } else {
                usage("--build is maven or gradle");
                System.exit(2);
                return;
            }
            ProjectOptions.ProjectType projectType = ProjectOptions.ProjectType.APP;
            if ("app-with-backend".equals(type)) {
                projectType = ProjectOptions.ProjectType.APP_WITH_BACKEND;
            } else if (type != null && !"app".equals(type)) {
                usage("--type is app or app-with-backend");
                System.exit(2);
            }
            options = options.withBuild(tool, projectType).withPlatformModules(false);
            if (t.isFullStack()) {
                options = options.forFullStack();
            }
            if (brand != null) {
                if (!brand.matches("[0-9a-fA-F]{6}")) {
                    usage("--brand is a colour as RRGGBB, e.g. 1f5eff");
                    System.exit(2);
                }
                options = options.withScheme(Integer.parseInt(brand, 16), true);
            }
            if (icon != null) {
                options = options.withIcon(Files.readAllBytes(Paths.get(icon)));
            }
            GeneratorModel model = GeneratorModel.create(IDE.INTELLIJ, t, name, pkg, options);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            model.writeProjectZip(bytes);
            if (zip != null) {
                Files.write(Paths.get(zip), bytes.toByteArray());
                System.out.println("Wrote " + zip);
            }
            if (dir != null) {
                Path target = Paths.get(dir).toAbsolutePath();
                if (Files.exists(target) && target.toFile().list() != null && target.toFile().list().length > 0) {
                    usage(target + " exists and is not empty");
                    System.exit(2);
                }
                extract(new ByteArrayInputStream(bytes.toByteArray()), target);
                System.out.println("Created " + t.name() + " project " + name + " in " + target);
                System.out.println("Next: read AGENTS.md in that directory.");
            }
            System.exit(0);
        } catch (IOException | RuntimeException e) {
            System.err.println("Generation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /// The generator reads its templates through Codename One's resource lookup,
    /// which needs an implementation installed. The JavaSE port answers it from
    /// the class path; no window, simulator or account is involved.
    private static void bootResources() {
        try {
            java.lang.reflect.Field impl = Display.class.getDeclaredField("impl");
            impl.setAccessible(true);
            impl.set(null, new JavaSEPort());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot initialize resource lookup", e);
        }
    }

    private static void extract(InputStream in, Path target) throws IOException {
        Files.createDirectories(target);
        try (ZipInputStream z = new ZipInputStream(in)) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = z.getNextEntry()) != null) {
                Path out = target.resolve(e.getName()).normalize();
                if (!out.startsWith(target)) {
                    throw new IOException("entry outside the project: " + e.getName());
                }
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                try (OutputStream o = Files.newOutputStream(out)) {
                    int n;
                    while ((n = z.read(buf)) > 0) {
                        o.write(buf, 0, n);
                    }
                }
                String base = out.getFileName().toString();
                if (base.endsWith(".sh") || "mvnw".equals(base) || "gradlew".equals(base)) {
                    File f = out.toFile();
                    if (!f.setExecutable(true, false)) {
                        System.err.println("warning: could not mark " + out + " executable");
                    }
                }
            }
        }
    }

    private static void usage(String error) {
        if (error != null) {
            System.err.println("error: " + error);
        }
        System.err.println("usage: CreateProject --list");
        System.err.println("       CreateProject --name AppName --package com.example.app [--template BAREBONES]");
        System.err.println("           [--build maven|gradle] [--type app|app-with-backend] [--theme light|dark]");
        System.err.println("           [--brand RRGGBB] [--icon icon.png] [--dir path | --zip file.zip]");
    }
}
