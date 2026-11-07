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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleApiTest {
    private TerminalBackend previous;

    private FakeTerminal install(FakeTerminal t) {
        TerminalBackend old = Console.install(t);
        if (previous == null) {
            previous = old;
        }
        return t;
    }

    @AfterEach
    void restore() {
        if (previous != null) {
            Console.install(previous);
            previous = null;
        }
    }

    // ---- Console ----

    @Test
    void sizeComesFromVariablesThenTerminal() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.columns = 132;
        t.rows = 50;
        assertEquals(132, Console.width());
        assertEquals(50, Console.height());
        t.env.put("COLUMNS", "100");
        t.env.put("LINES", "abc");
        assertEquals(100, Console.width());
        assertEquals(50, Console.height());
        t.columns = 0;
        t.rows = 0;
        t.env.remove("COLUMNS");
        assertEquals(80, Console.width());
        assertEquals(24, Console.height());
    }

    @Test
    void colorLevelFollowsTheEnvironment() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        assertEquals(Console.ColorLevel.NONE, Console.colorLevel());
        t.env.remove("NO_COLOR");
        assertEquals(Console.ColorLevel.ANSI256, Console.colorLevel());
        t.env.put("COLORTERM", "truecolor");
        assertEquals(Console.ColorLevel.TRUECOLOR, Console.colorLevel());
        t.env.remove("COLORTERM");
        t.env.put("TERM", "xterm");
        assertEquals(Console.ColorLevel.ANSI16, Console.colorLevel());
        t.stdoutTty = false;
        assertEquals(Console.ColorLevel.NONE, Console.colorLevel());
        t.env.put("CLICOLOR_FORCE", "1");
        assertEquals(Console.ColorLevel.ANSI16, Console.colorLevel());
    }

    @Test
    void unicodeFollowsTheLocale() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        assertTrue(Console.unicode());
        t.env.put("LC_ALL", "C");
        assertFalse(Console.unicode());
        t.env.put("LC_ALL", "de_DE.utf8");
        assertTrue(Console.unicode());
    }

    // ---- TextWidth ----

    @Test
    void displayWidthIgnoresEscapesAndCountsWideCharacters() {
        assertEquals(5, Console.displayWidth("\u001b[1;31mhello\u001b[0m"));
        assertEquals(4, Console.displayWidth("\u65e5\u672c"));
        assertEquals(2, Console.displayWidth("\ud83d\ude80"));
        assertEquals(1, Console.displayWidth("e\u0301"));
        assertEquals(4, Console.displayWidth("\u001b]8;;http://x\u001b\\link\u001b]8;;\u001b\\"));
        assertEquals("hello", Console.strip("\u001b[1;31mhello\u001b[0m"));
    }

    @Test
    void truncateKeepsEscapesAndCutsByColumns() {
        assertEquals("ab\u2026", TextWidth.truncate("abcdef", 3, "\u2026"));
        assertEquals("\u001b[1mab\u001b[0m.", TextWidth.truncate("\u001b[1mabcd", 3, "."));
        assertEquals("\u65e5", TextWidth.truncate("\u65e5\u672c", 3, ""));
        assertEquals("short", TextWidth.truncate("short", 10, "."));
    }

    @Test
    void wrapBreaksBetweenWordsAndInsideLongOnes() {
        assertEquals(Arrays.asList("the quick", "brown fox"), TextWidth.wrap("the quick brown fox", 10));
        assertEquals(Arrays.asList("abcd", "efgh", "ij"), TextWidth.wrap("abcdefghij", 4));
        assertEquals(Arrays.asList("a", "b"), TextWidth.wrap("a\nb", 10));
    }

    // ---- Ansi ----

    @Test
    void colorsConvertDownToTheLevel() {
        Ansi.Color orange = Ansi.Color.rgb(255, 135, 0);
        assertEquals("\u001b[38;2;255;135;0mx",
                Ansi.ansi(Console.ColorLevel.TRUECOLOR).fg(orange).a("x").toString());
        assertEquals("\u001b[38;5;208mx",
                Ansi.ansi(Console.ColorLevel.ANSI256).fg(orange).a("x").toString());
        assertEquals("\u001b[33mx",
                Ansi.ansi(Console.ColorLevel.ANSI16).fg(orange).a("x").toString());
        assertEquals("x", Ansi.ansi(Console.ColorLevel.NONE).fg(orange).bold().a("x").toString());
        assertEquals("\u001b[44m", Ansi.ansi(Console.ColorLevel.ANSI16).bg(Ansi.Color.BLUE).toString());
        assertEquals(232, Ansi.rgbTo256(0x080808));
        assertEquals(Ansi.Color.hex("#ff8700").sgr(Console.ColorLevel.TRUECOLOR, true),
                orange.sgr(Console.ColorLevel.TRUECOLOR, true));
    }

    @Test
    void linksAreOsc8WhenStyled() {
        assertEquals("site", Ansi.ansi(Console.ColorLevel.NONE).link("https://e.com", "site").toString());
        assertEquals("\u001b]8;;https://e.com\u001b\\site\u001b]8;;\u001b\\",
                Ansi.ansi(Console.ColorLevel.ANSI16).link("https://e.com", "site").toString());
    }

    // ---- Table and Tree ----

    @Test
    void tableAlignsAndBoxes() {
        install(FakeTerminal.tty(false));
        String out = new Table("Name", "Size").align(1, Table.Align.RIGHT)
                .border(Table.Border.ASCII)
                .addRow("api", "12 MB")
                .addRow("worker-\u65e5", "3 MB")
                .render();
        assertEquals("+-----------+-------+\n"
                + "| Name      |  Size |\n"
                + "+-----------+-------+\n"
                + "| api       | 12 MB |\n"
                + "| worker-\u65e5 |  3 MB |\n"
                + "+-----------+-------+\n", out);
    }

    @Test
    void tableShrinksToMaxWidthAndWraps() {
        install(FakeTerminal.tty(false));
        String out = new Table("Key", "Description").border(Table.Border.NONE).headerStyle(false)
                .maxWidth(20)
                .addRow("a", "one two three four five")
                .render();
        assertEquals("Key  Description\n"
                + "a    one two three\n"
                + "     four five\n", out);
    }

    @Test
    void minimalTableUnderlinesTheHeader() {
        install(FakeTerminal.tty(false));
        String out = new Table("ID", "State").border(Table.Border.MINIMAL).addRow(7, "ok").render();
        assertEquals("ID  State\n\u2500\u2500  \u2500\u2500\u2500\u2500\u2500\n7   ok\n", out);
    }

    @Test
    void treeDrawsGuides() {
        Tree root = new Tree("app");
        Tree src = root.add("src");
        src.add("Main.java");
        src.add("Util.java");
        root.add("pom.xml");
        assertEquals("app\n|-- src\n|   |-- Main.java\n|   `-- Util.java\n`-- pom.xml\n", root.render(false));
        assertEquals("app\n\u251c\u2500\u2500 src\n\u2502   \u251c\u2500\u2500 Main.java\n"
                + "\u2502   \u2514\u2500\u2500 Util.java\n\u2514\u2500\u2500 pom.xml\n", root.render(true));
    }

    // ---- KeyReader ----

    @Test
    void keysDecodeFromTerminalSequences() {
        FakeTerminal t = new FakeTerminal();
        t.type("\u001b[A\u001bOB\u001b[3~\u001b[5~\u001b[H\u001b[4~\u001b[Z\u001b[15~\u001bOP\u001b[1;3C");
        t.bytes(3, 4, 127, 1, 13, 9).type("\u00e9\ud83d\ude80\u001bx").bytes(27);
        KeyReader r = new KeyReader(t);
        Key.Type[] expected = {Key.Type.UP, Key.Type.DOWN, Key.Type.DELETE, Key.Type.PAGE_UP,
            Key.Type.HOME, Key.Type.END, Key.Type.BACKTAB, Key.Type.FUNCTION, Key.Type.FUNCTION,
            Key.Type.RIGHT, Key.Type.CTRL_C, Key.Type.CTRL_D, Key.Type.BACKSPACE, Key.Type.CTRL,
            Key.Type.ENTER, Key.Type.TAB};
        Key f5 = null;
        Key altRight = null;
        for (Key.Type type : expected) {
            Key k = r.read();
            assertEquals(type, k.type(), "after " + type);
            if (type == Key.Type.FUNCTION && f5 == null) {
                f5 = k;
            }
            if (type == Key.Type.RIGHT) {
                altRight = k;
            }
        }
        assertEquals(5, f5.number());
        assertTrue(altRight.alt());
        assertEquals(0xe9, r.read().codePoint());
        assertEquals(0x1f680, r.read().codePoint());
        Key altX = r.read();
        assertTrue(altX.alt() && altX.is('x'));
        assertEquals(Key.Type.ESCAPE, r.read().type());
        assertEquals(Key.Type.EOF, r.read().type());
    }

    // ---- Prompts, interactive ----

    @Test
    void textPromptEditsInPlace() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        // "helo", Left, "l" -> "hello"; End, then Ctrl-W after " world" deletes it again.
        t.type("helo\u001b[Dl\u001b[F").type(" world").bytes(23).enter();
        assertEquals("hello ", Prompt.text("Name"));
        assertFalse(t.raw);
        assertEquals(Arrays.asList("\u2714 Name hello"), VirtualScreen.play(t.output()).lines());
    }

    @Test
    void textPromptTakesTheDefaultAndValidates() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.enter();
        assertEquals("demo", Prompt.text("Project", "demo"));

        t = install(FakeTerminal.tty(true));
        t.type("12x").enter().bytes(127).enter();
        assertEquals(12, Prompt.number("Port", 80, 1, 65535));
        VirtualScreen screen = VirtualScreen.play(t.output());
        assertEquals(Arrays.asList("\u2714 Port 12"), screen.lines());
    }

    @Test
    void validationErrorShowsUnderThePrompt() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.type("0").enter();
        assertThrows(Prompt.Cancelled.class, () -> Prompt.number("Port", 80, 1, 65535));
        // The input ran out with the error on screen: the region shows the
        // cancellation over it, and nothing else is left behind.
        assertEquals(Arrays.asList("\u2716 Port cancelled"), VirtualScreen.play(t.output()).lines());
    }

    @Test
    void passwordIsMasked() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.type("s3cret").enter();
        assertEquals("s3cret", Prompt.password("Token"));
        String screen = VirtualScreen.play(t.output()).text();
        assertFalse(screen.contains("s3cret"), screen);
        assertEquals("\u2714 Token ******\n", screen);
    }

    @Test
    void confirmTakesKeysAndDefault() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.type("n");
        assertFalse(Prompt.confirm("Deploy?", true));
        t = install(FakeTerminal.tty(true));
        t.enter();
        assertTrue(Prompt.confirm("Deploy?", true));
        assertEquals("\u2714 Deploy? Yes\n", VirtualScreen.play(t.output()).text());
    }

    @Test
    void selectMovesWithArrowsAndWraps() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.down().down().down().up().enter();
        String db = Prompt.select("Database", Arrays.asList("PostgreSQL", "MySQL", "SQLite"));
        assertEquals("SQLite", db);
        assertEquals("\u2714 Database SQLite\n", VirtualScreen.play(t.output()).text());
    }

    @Test
    void selectFiltersAsYouType() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.type("SQL").down().enter();
        String db = Prompt.select("Database", Arrays.asList("PostgreSQL", "MySQL", "SQLite", "Mongo"));
        assertEquals("MySQL", db);
    }

    @Test
    void selectShowsAPageAndACounter() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.down().down().down();
        List<String> choices = Arrays.asList("a", "b", "c", "d", "e");
        assertThrows(Prompt.Cancelled.class,
                () -> new Prompt.Select<String>("Pick", choices).pageSize(2).filter(false).ask());
        // Before the input ran out, the last frame showed d with the pointer.
        String out = t.output();
        String lastFrame = out.substring(0, out.lastIndexOf("\r", out.lastIndexOf("\u2716")));
        List<String> lines = VirtualScreen.play(lastFrame).lines();
        assertEquals(Arrays.asList("? Pick", "  c", "\u276f d", "  4/5"), lines);
    }

    @Test
    void multiSelectTogglesAndEnforcesMinimum() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.enter().type(" ").down().down().type(" ").enter();
        List<String> picked = new Prompt.MultiSelect<String>("Features",
                Arrays.asList("auth", "mail", "jobs")).min(1).ask();
        assertEquals(Arrays.asList("auth", "jobs"), picked);
        assertEquals("\u2714 Features auth, jobs\n", VirtualScreen.play(t.output()).text());
    }

    @Test
    void multiSelectCtrlASelectsAll() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.bytes(1).enter();
        assertEquals(Arrays.asList(1, 2, 3), Prompt.multiSelect("N", Arrays.asList(1, 2, 3)));
    }

    @Test
    void ctrlCCancelsAndRestoresTheTerminal() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.type("ab").bytes(3);
        assertThrows(Prompt.Cancelled.class, () -> Prompt.text("Name"));
        assertFalse(t.raw);
        assertEquals(1, t.rawEntries);
        assertTrue(VirtualScreen.play(t.output()).cursorVisible);
    }

    // ---- Prompts, line mode ----

    @Test
    void pipedAnswersAreReadAsLines() {
        FakeTerminal t = install(FakeTerminal.tty(false));
        t.type("ada\n\nyes\n2\nmail, 3\n");
        assertEquals("ada", Prompt.text("Name"));
        assertEquals("x", Prompt.text("Other", "x"));
        assertTrue(Prompt.confirm("Sure?", false));
        assertEquals("b", Prompt.select("Pick", Arrays.asList("a", "b")));
        assertEquals(Arrays.asList("mail", "jobs"),
                Prompt.multiSelect("Features", Arrays.asList("auth", "mail", "jobs")));
        assertEquals(0, t.rawEntries);
        assertTrue(t.output().contains("  1) a\n  2) b\n"), t.output());
    }

    @Test
    void lineModeRepeatsUntilValidAndEndOfInputCancels() {
        FakeTerminal t = install(FakeTerminal.tty(false));
        t.type("7\nPostgres\n");
        assertEquals("postgres", Prompt.select("DB", Arrays.asList("mysql", "postgres")));
        assertTrue(t.output().contains("Not one of the choices: 7"));
        assertThrows(Prompt.Cancelled.class, () -> Prompt.text("Name"));
        assertTrue(Prompt.confirm("More?", true));
    }

    @Test
    void rawModeUnavailableFallsBackToLines() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.rawAllowed = false;
        t.type("bob\n");
        assertEquals("bob", Prompt.text("Name"));
    }

    // ---- Progress ----

    @Test
    void progressBarRendersCountRateAndEta() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        MultiProgress group = new MultiProgress(t, true, false);
        ProgressBar bar = group.add("upload", 200).unit("files");
        t.now += 2000;
        bar.stepBy(50);
        String line = bar.render(t.now, 80, 6);
        assertTrue(line.startsWith("upload  \u2588\u2588\u2588"), line);
        assertTrue(line.endsWith(" 25%  50/200 files  25 files/s  0:06"), line);
        bar.complete();
        group.close();
        List<String> lines = VirtualScreen.play(t.output()).lines();
        assertEquals(1, lines.size(), lines.toString());
        assertTrue(lines.get(0).endsWith("100%  200/200 files  100 files/s  0:02"), lines.get(0));
        assertTrue(VirtualScreen.play(t.output()).cursorVisible);
    }

    @Test
    void progressBarDrawsPartialBlocksAndAsciiFallback() {
        assertEquals("\u2588\u258c\u2591\u2591", ProgressBar.filled(0.375, 4, true));
        assertEquals("#---", ProgressBar.filled(0.375, 4, false));
        assertEquals("\u2588\u2588\u2588\u2588", ProgressBar.filled(1.0, 4, true));
        assertEquals("1.5 KB", ProgressBar.size(1536));
        assertEquals("1:01:05", ProgressBar.duration(3665000));
    }

    @Test
    void severalBarsRedrawInPlaceWithLogLinesAbove() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        MultiProgress group = new MultiProgress(t, true, false);
        ProgressBar a = group.add("a", 10);
        ProgressBar b = group.add("bb", 0);
        a.stepBy(5);
        group.println("note");
        b.stepBy(3);
        a.complete();
        group.close();
        List<String> lines = VirtualScreen.play(t.output()).lines();
        assertEquals(3, lines.size(), lines.toString());
        assertEquals("note", lines.get(0));
        assertTrue(lines.get(1).startsWith("a   "), lines.get(1));
        assertTrue(lines.get(2).startsWith("bb  "), lines.get(2));
    }

    @Test
    void offATerminalBarsPrintMilestonesAndASummary() {
        FakeTerminal t = install(FakeTerminal.tty(false));
        MultiProgress group = new MultiProgress(t, false, false);
        ProgressBar bar = group.add("sync", 100);
        for (int i = 0; i < 100; i++) {
            t.now += 100;
            bar.step();
        }
        bar.complete();
        group.close();
        String out = t.output();
        assertFalse(out.contains("\u001b"), out);
        assertTrue(out.contains("sync: 10% (10/100)\n"), out);
        assertTrue(out.endsWith("sync: done 100/100 in 0:10\n"), out);
        int lines = out.split("\n").length;
        assertTrue(lines <= 11, out);
    }

    @Test
    void trackStepsAsTheLoopAdvances() {
        FakeTerminal t = install(FakeTerminal.tty(false));
        int sum = 0;
        for (int v : ProgressBar.track(Arrays.asList(1, 2, 3), "sum")) {
            sum += v;
        }
        assertEquals(6, sum);
        assertTrue(t.output().contains("sum: done 3/3"), t.output());
    }

    // ---- Spinner ----

    @Test
    void spinnerOffATerminalPrintsStartAndStatus() {
        FakeTerminal t = install(FakeTerminal.tty(false));
        Spinner s = new Spinner(t, false, "Resolving").start();
        s.succeed("Resolved 3");
        s.close();
        assertEquals("Resolving...\n\u2714 Resolved 3\n", t.output());
    }

    @Test
    void spinnerOnATerminalEndsWithItsStatus() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        Spinner s = new Spinner(t, true, "Working").frames(Spinner.Frames.LINE).start();
        s.setMessage("Still working");
        s.fail("Broke");
        VirtualScreen screen = VirtualScreen.play(t.output());
        assertEquals(Arrays.asList("\u2716 Broke"), screen.lines());
        assertTrue(screen.cursorVisible);
    }

    @Test
    void spinnerClosedWithoutStatusClearsItsLine() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        Spinner s = new Spinner(t, true, "Working").start();
        s.close();
        assertEquals(Arrays.asList(), VirtualScreen.play(t.output()).lines());
    }

    // ---- CommandLine ----

    @Test
    void cancelledExitCodeIsTheShells() {
        assertEquals(130, Prompt.CANCELLED_EXIT_CODE);
    }

    // ---- Red-team regressions ----

    @Test
    void keyReaderPushesBackWhatItCannotUse() {
        FakeTerminal t = new FakeTerminal();
        // ESC ESC is two presses; an overlong NUL, a lone surrogate and a
        // bad continuation byte never become characters; Ctrl-Space is ' '.
        t.bytes(27, 27).type("q").bytes(0xc0, 0x80).bytes(0xed, 0xa0, 0x80).bytes(0xc3).type("z").bytes(0);
        KeyReader r = new KeyReader(t);
        Key first = r.read();
        assertEquals(Key.Type.ESCAPE, first.type());
        Key second = r.read();
        assertTrue(second.alt() && second.is('q') || second.type() == Key.Type.ESCAPE);
        java.util.List<Key> rest = new java.util.ArrayList<Key>();
        for (Key k = r.read(); k.type() != Key.Type.EOF; k = r.read()) {
            rest.add(k);
        }
        boolean sawZ = false;
        for (Key k : rest) {
            if (k.type() == Key.Type.CHAR) {
                assertTrue(k.codePoint() < 0xd800 || k.codePoint() > 0xdfff, "surrogate " + k.codePoint());
                assertTrue(k.codePoint() != 0, "overlong NUL decoded");
                sawZ |= k.is('z');
            }
        }
        assertTrue(sawZ, "the byte after a bad continuation was lost: " + rest);
        Key ctrlSpace = rest.get(rest.size() - 1);
        assertTrue(ctrlSpace.type() == Key.Type.CTRL && ctrlSpace.codePoint() == ' ', "Ctrl-Space");
    }

    @Test
    void liveRegionSplitsNewlinesAndClipsToTheScreen() {
        FakeTerminal t = FakeTerminal.tty(true);
        t.rows = 4;
        LiveRegion region = new LiveRegion(t, true);
        region.updateNow("a\nb", "c\td", "e", "f", "g");
        java.util.List<String> lines = VirtualScreen.play(t.output()).lines();
        assertTrue(lines.size() <= 3, "region taller than the screen: " + lines);
        assertEquals("a", lines.get(0));
        assertEquals("b", lines.get(1));
        assertEquals("c    d", lines.get(2));
        region.close();
        assertFalse(t.cursorHiddenFlag);
    }

    @Test
    void wrapAndTruncateNeverLoseOrBreakContent() {
        java.util.List<String> w = TextWidth.wrap("中文", 1);
        assertEquals(2, w.size(), "a wide character wider than the column is placed, not looped on");
        String link = "\u001b]8;;http://x\u001b\\link text\u001b]8;;\u001b\\";
        String cut = TextWidth.truncate(link, 4, "");
        assertTrue(cut.endsWith("\u001b]8;;\u001b\\"), "hyperlink left open: " + cut.replace("\u001b", "ESC"));
    }

    @Test
    void confirmWithoutADefaultWaitsForAnAnswer() {
        FakeTerminal t = install(FakeTerminal.tty(true));
        t.enter().type("y");
        assertTrue(Prompt.confirm("Deploy?"));
        t = install(FakeTerminal.tty(false));
        t.type("\nn\n");
        assertFalse(Prompt.confirm("Deploy?"));
    }

    @Test
    void multiSelectRejectsAMinimumItCannotMeet() {
        install(FakeTerminal.tty(true));
        assertThrows(IllegalArgumentException.class,
                () -> new Prompt.MultiSelect<String>("Pick", Arrays.asList("a", "b")).min(3).ask());
        assertThrows(IllegalArgumentException.class,
                () -> new Prompt.MultiSelect<String>("Pick", Arrays.asList("a", "b")).min(2).max(1).ask());
    }

    @Test
    void aSpinnerTellsTheTerminalTheCursorIsHidden() {
        FakeTerminal t = FakeTerminal.tty(true);
        LiveRegion region = new LiveRegion(t, true);
        region.updateNow("working");
        assertTrue(t.cursorHiddenFlag);
        region.close();
        assertFalse(t.cursorHiddenFlag);
    }

    @Test
    void aStyleThatWrapsIsClosedAndReopenedOnEachLine() {
        java.util.List<String> w = TextWidth.wrap("\u001b[1mlong header\u001b[0m", 6);
        assertEquals(Arrays.asList("\u001b[1mlong\u001b[0m", "\u001b[1mheader\u001b[0m"), w);
        java.util.List<String> cut = TextWidth.wrap("\u001b[31mabcdefgh\u001b[0m", 3);
        for (String line : cut) {
            assertTrue(line.startsWith("\u001b[31m") && line.endsWith("\u001b[0m"), line.replace("\u001b", "ESC"));
        }
        // A style from an earlier word survives cuts of a later, styled one.
        java.util.List<String> mixed = TextWidth.wrap("\u001b[31mred \u001b[1mverylongword", 6);
        String last = mixed.get(mixed.size() - 1);
        assertTrue(last.startsWith("\u001b[31m\u001b[1m"), last.replace("\u001b", "ESC"));
    }
}
