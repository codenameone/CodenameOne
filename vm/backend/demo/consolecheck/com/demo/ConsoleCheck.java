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
package com.demo;

import com.codename1.cli.Ansi;
import com.codename1.cli.Console;
import com.codename1.cli.MultiProgress;
import com.codename1.cli.ProgressBar;
import com.codename1.cli.Prompt;
import com.codename1.cli.Spinner;
import com.codename1.cli.Table;
import com.codename1.cli.Tree;

import java.util.Arrays;
import java.util.List;

/// Exercises the console API end to end on a real terminal:
/// `scripts/check-cli-console.py` runs it under a pseudo-terminal, types the
/// answers and checks what came back, and that the terminal was left as it
/// was found -- on the native binary and on the JVM.
public class ConsoleCheck {
    public static void main(String[] args) throws Exception {
        Console.println("SIZE " + Console.width() + "x" + Console.height()
                + " tty=" + Console.isTerminal() + " in=" + Console.isInputTerminal());
        Console.println(Ansi.ansi().fg(Ansi.Color.GREEN).bold().a("styled").reset().toString());
        Console.print(new Table("Name", "Size").align(1, Table.Align.RIGHT)
                .addRow("api", "12 MB").addRow("worker", "3 MB").render());
        Tree root = new Tree("app");
        root.add("src").add("Main.java");
        Console.print(root.render());

        try (ProgressBar bar = new ProgressBar("work", 20)) {
            for (int i = 0; i < 20; i++) {
                Thread.sleep(10);
                bar.step();
            }
        }
        try (MultiProgress all = new MultiProgress()) {
            ProgressBar a = all.add("a", 5);
            ProgressBar b = all.add("b", 0);
            for (int i = 0; i < 5; i++) {
                Thread.sleep(10);
                a.step();
                b.step();
            }
            all.println("log line");
            a.complete();
            b.complete();
        }
        Spinner s = Spinner.start("spinning");
        Thread.sleep(200);
        s.succeed("spun");

        try {
            String name = Prompt.text("Name", "anon");
            List<String> choices = Arrays.asList("PostgreSQL", "MySQL", "SQLite");
            String db = Prompt.select("Database", choices);
            List<String> features = Prompt.multiSelect("Features",
                    Arrays.asList("auth", "mail", "jobs"));
            String secret = Prompt.password("Token");
            boolean ok = Prompt.confirm("Proceed?", false);
            Console.println("ANSWERS name=" + name + " db=" + db + " features=" + features
                    + " secret=" + secret.length() + " ok=" + ok);
        } catch (Prompt.Cancelled e) {
            Console.println("CANCELLED " + e.getMessage());
            System.exit(Prompt.CANCELLED_EXIT_CODE);
        }
    }
}
