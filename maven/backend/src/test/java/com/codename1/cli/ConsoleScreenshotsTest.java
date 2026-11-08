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
package com.codename1.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/// The developer guide's screenshots of the console API
/// (`docs/developer-guide/img/cli-*.svg`), drawn from what the real classes
/// write to a scripted color terminal. The clock is the fake terminal's, so
/// rates and times are the same on every run, and each test fails when a
/// committed image no longer matches what the code draws.
///
/// After changing how the console looks, regenerate them with
/// `mvn -pl backend test -Dtest=ConsoleScreenshotsTest -Dcn1.cli.screenshots=write`.
class ConsoleScreenshotsTest {
    private static final int COLUMNS = 80;

    /// A 24-bit color terminal, the way a current terminal app reports one.
    private static FakeTerminal colorTerminal() {
        FakeTerminal t = FakeTerminal.tty(true);
        t.env.remove("NO_COLOR");
        t.env.put("COLORTERM", "truecolor");
        t.columns = COLUMNS;
        t.rows = 24;
        return t;
    }

    private interface Scene {
        void run(FakeTerminal t) throws Exception;
    }

    private static void shot(String name, String title, int rows, FakeTerminal t, Scene scene) throws Exception {
        TerminalBackend previous = Console.install(t);
        try {
            scene.run(t);
        } catch (FakeTerminal.Snapshot expected) {
            // The prompt is on screen, waiting for the key the script stops at.
        } finally {
            Console.install(previous);
        }
        String out = t.snapshot >= 0 ? t.output().substring(0, t.snapshot) : t.output();
        assertTrue(out.indexOf("\u001b[3") >= 0, "no colors were written for " + name);
        ColorScreen screen = new ColorScreen(COLUMNS);
        screen.feed(out);
        String svg = screen.svg(title, rows);
        File file = new File(imageDir(), name);
        if ("write".equals(System.getProperty("cn1.cli.screenshots"))) {
            Files.write(file.toPath(), svg.getBytes(StandardCharsets.UTF_8));
            return;
        }
        String committed = file.exists() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "";
        assertEquals(committed, svg, file + " no longer matches the console's output; regenerate it with "
                + "mvn -pl backend test -Dtest=ConsoleScreenshotsTest -Dcn1.cli.screenshots=write");
    }

    private static File imageDir() throws IOException {
        File dir = new File(System.getProperty("user.dir")).getCanonicalFile();
        while (dir != null) {
            File img = new File(dir, "docs/developer-guide/img");
            if (img.isDirectory()) {
                return img;
            }
            dir = dir.getParentFile();
        }
        throw new IOException("docs/developer-guide/img not found above " + System.getProperty("user.dir"));
    }

    @Test
    void output() throws Exception {
        shot("cli-console-output.svg", "deploy status", 0, colorTerminal(), t -> {
            Console.println(Console.style("Release", Console.Style.BOLD) + " 2.4.0 "
                    + Console.style("(4f2a91c)", Console.Style.FAINT));
            Console.print(new Table("Service", "Status", "Replicas", "Memory")
                    .border(Table.Border.ROUNDED)
                    .align(2, Table.Align.RIGHT)
                    .align(3, Table.Align.RIGHT)
                    .addRow("api", Console.style("running", Console.Style.GREEN), 3, "412 MB")
                    .addRow("worker", Console.style("degraded", Console.Style.YELLOW), 2, "1.1 GB")
                    .addRow("mailer", Console.style("stopped", Console.Style.RED), 0, "-")
                    .render());
            Tree root = new Tree(Console.style("dist/", Console.Style.BLUE, Console.Style.BOLD));
            Tree lib = root.add(Console.style("lib/", Console.Style.BLUE, Console.Style.BOLD));
            lib.add("app.jar");
            lib.add("backend.jar");
            root.add(Console.style("deploy.sh", Console.Style.GREEN));
            root.add("README.md");
            Console.print(root.render());
            StringBuilder ramp = new StringBuilder();
            String word = "24-bit color ramp";
            for (int i = 0; i < word.length(); i++) {
                int r = 137 + (243 - 137) * i / (word.length() - 1);
                int b = 250 + (168 - 250) * i / (word.length() - 1);
                ramp.append(Ansi.ansi().fg(Ansi.Color.rgb(r, 180, b), word.substring(i, i + 1)));
            }
            Console.println(ramp + "  " + Ansi.ansi().bold().a("bold").reset() + " "
                    + Ansi.ansi().italic().a("italic").reset() + " "
                    + Ansi.ansi().underline().a("underline").reset() + " "
                    + Ansi.ansi().strikethrough().a("struck").reset() + " "
                    + Ansi.ansi().inverse().a(" inverse ").reset());
        });
    }

    @Test
    void progress() throws Exception {
        shot("cli-console-progress.svg", "deploy", 0, colorTerminal(), t -> {
            new Spinner(t, true, "checking credentials").start().succeed("credentials valid");
            new Spinner(t, true, "reading settings").start().warn("2 deprecated settings in deploy.yml");
            new Spinner(t, true, "resolving region").start().info("using eu-west-1");
            MultiProgress bars = new MultiProgress(t, true, false);
            ProgressBar assets = bars.add("assets", 128).unit("files");
            ProgressBar upload = bars.add("upload", 120L * 1024 * 1024).bytes();
            ProgressBar migrate = bars.add("migrate", 14).unit("steps");
            t.now += 4200;
            assets.stepTo(128);
            assets.complete();
            t.now += 3100;
            upload.stepTo(53L * 1024 * 1024);
            upload.setMessage("backend.jar");
            migrate.stepTo(5);
            t.now += 200;
            upload.stepBy(1024);
        });
    }

    @Test
    void select() throws Exception {
        FakeTerminal t = colorTerminal();
        t.type("api").enter().down();
        t.snapshotWhenIdle = true;
        shot("cli-console-select.svg", "deploy", 0, t, term -> {
            Prompt.text("Service", "api");
            Prompt.select("Environment", Arrays.asList("development", "staging", "production"));
        });
    }

    @Test
    void checkbox() throws Exception {
        FakeTerminal t = colorTerminal();
        t.down().type(" ").down().down();
        t.snapshotWhenIdle = true;
        shot("cli-console-checkbox.svg", "deploy", 0, t, term ->
            new Prompt.MultiSelect<String>("Regions", Arrays.asList("us-east-1", "eu-west-1", "ap-south-1",
                    "sa-east-1")).preselect(0).ask());
    }

    @Test
    void prompts() throws Exception {
        FakeTerminal t = colorTerminal();
        t.type("s3cr3t-t0ken").enter().type("y").type("12").enter();
        t.snapshotWhenIdle = true;
        shot("cli-console-prompts.svg", "deploy", 0, t, term -> {
            Prompt.password("API token");
            Prompt.confirm("Deploy to production?", false);
            Prompt.number("Replicas", 3, 1, 10);
        });
    }
}
