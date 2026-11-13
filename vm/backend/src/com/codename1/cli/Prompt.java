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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Questions a command asks the person running it:
///
/// ```java
/// String name = Prompt.text("Project name", "demo");
/// String db = Prompt.select("Database", Arrays.asList("PostgreSQL", "MySQL", "SQLite"));
/// List<String> features = Prompt.multiSelect("Features", Arrays.asList("auth", "mail", "jobs"));
/// String token = Prompt.password("API token");
/// if (!Prompt.confirm("Create it?", true)) {
///     return 1;
/// }
/// ```
///
/// On a terminal each question is answered key by key: a text prompt edits in
/// place (arrows, Home, End, Ctrl-U, Ctrl-W), a selection list moves with the
/// arrows and filters as you type, a multiple selection toggles with Space.
/// The question then collapses to one line showing the answer.
///
/// When standard input or output is not a terminal -- a script piping answers
/// in, a CI job -- the same calls read one line per question, so a tool stays
/// scriptable: a selection takes a number or the label, a multiple selection a
/// comma-separated list of either, a confirmation `y` or `n`, and an empty line
/// takes the default. End of input answers the default where there is one.
///
/// Ctrl-C, Escape, or end of input with no default throw [Cancelled]; a command
/// run through [CommandLine] that lets it propagate exits with
/// [#CANCELLED_EXIT_CODE], the code a shell gives an interrupted command.
public final class Prompt {
    /// The exit code [CommandLine] answers when a prompt was cancelled:
    /// 128 + SIGINT.
    public static final int CANCELLED_EXIT_CODE = 130;

    private Prompt() {
    }

    /// Thrown when the person cancels a question.
    public static final class Cancelled extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /// A cancellation with `message`.
        public Cancelled(String message) {
            super(message);
        }
    }

    /// Checks an answer before a text prompt accepts it.
    public interface Validator {
        /// Null to accept `value`, otherwise the message shown under the
        /// prompt while the person corrects it.
        String validate(String value);
    }

    /// Turns a choice into the label a selection list shows.
    public interface Labeler<T> {
        /// The label for `choice`.
        String label(T choice);
    }

    /// Whether questions are answered key by key: standard input and output
    /// are both terminals. Otherwise they read lines.
    public static boolean interactive() {
        return Console.isInputTerminal() && Console.isTerminal();
    }

    /// Raw mode for code that reads [Key]s itself; close it to restore the
    /// terminal. Null when standard input is not a terminal.
    public static AutoCloseable rawMode() {
        final TerminalBackend term = Console.backend();
        if (!term.enterRaw()) {
            return null;
        }
        return new AutoCloseable() {
            @Override
            public void close() {
                term.restore();
            }
        };
    }

    /// Asks a yes-or-no question; Enter takes `defaultValue`.
    public static boolean confirm(String question, boolean defaultValue) {
        return confirm(question, defaultValue ? 1 : 0);
    }

    /// Asks a yes-or-no question with no default: Enter alone answers
    /// nothing and the question stays. At the end of piped input the answer
    /// is no, the choice that changes nothing.
    public static boolean confirm(String question) {
        return confirm(question, -1);
    }

    private static boolean confirm(String question, int def) {
        boolean defaultValue = def == 1;
        TerminalBackend term = Console.backend();
        String hint = def < 0 ? "(y/n)" : defaultValue ? "(Y/n)" : "(y/N)";
        if (!interactive() || !term.enterRaw()) {
            while (true) {
                term.write(question + " " + hint + " ");
                String line = readLine(term);
                if (line == null) {
                    term.write("\n");
                    return defaultValue;
                }
                String t = line.trim();
                if (t.length() == 0 && def >= 0) {
                    return defaultValue;
                }
                if ("y".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t)) {
                    return true;
                }
                if ("n".equalsIgnoreCase(t) || "no".equalsIgnoreCase(t)) {
                    return false;
                }
                term.write("Please answer y or n.\n");
            }
        }
        LiveRegion region = new LiveRegion(term, true); // NOPMD CloseResource - finish/cancel close it
        KeyReader keys = new KeyReader(term);
        try {
            String head = active(question) + " " + Console.style(hint, Console.Style.FAINT) + " ";
            region.updateWithCursor(new String[] {head}, 0, TextWidth.width(head));
            while (true) {
                Key k = keys.read();
                if (k.type() == Key.Type.ENTER) {
                    if (def < 0) {
                        continue;
                    }
                    return finish(region, question, defaultValue ? "Yes" : "No", defaultValue);
                }
                if (k.is('y') || k.is('Y')) {
                    return finish(region, question, "Yes", true);
                }
                if (k.is('n') || k.is('N')) {
                    return finish(region, question, "No", false);
                }
                checkCancel(k, region, question, true);
            }
        } finally {
            term.restore();
        }
    }

    /// Asks for a line of text.
    public static String text(String question) {
        return new Text(question).ask();
    }

    /// Asks for a line of text; Enter on an empty line takes `defaultValue`.
    public static String text(String question, String defaultValue) {
        return new Text(question).defaultValue(defaultValue).ask();
    }

    /// Asks for a secret, echoed as `*`.
    public static String password(String question) {
        return new Text(question).mask('*').ask();
    }

    /// Asks for a whole number between `min` and `max`; Enter takes
    /// `defaultValue`.
    public static long number(String question, long defaultValue, final long min, final long max) {
        String answer = new Text(question).defaultValue(Long.toString(defaultValue))
                .validator(new Validator() {
                    @Override
                    public String validate(String value) {
                        try {
                            long v = Long.parseLong(value.trim());
                            if (v < min || v > max) {
                                return "Enter a number from " + min + " to " + max;
                            }
                            return null;
                        } catch (NumberFormatException e) {
                            return "Enter a whole number";
                        }
                    }
                }).ask();
        return Long.parseLong(answer.trim());
    }

    /// Asks for one of `choices` and answers it.
    public static <T> T select(String question, List<T> choices) {
        return new Select<T>(question, choices).ask();
    }

    /// Asks for any number of `choices` and answers them in list order.
    public static <T> List<T> multiSelect(String question, List<T> choices) {
        return new MultiSelect<T>(question, choices).ask();
    }

    /// A text question with options: a default, a validator, masking.
    public static final class Text {
        private final String question;
        private String defaultValue;
        private Validator validator;
        private char mask;
        private boolean masked;
        private String placeholder;

        /// A text question.
        public Text(String question) {
            this.question = question;
        }

        /// The answer an empty line gives, shown in the prompt.
        public Text defaultValue(String value) {
            this.defaultValue = value;
            return this;
        }

        /// Checks the answer before it is accepted.
        public Text validator(Validator validator) {
            this.validator = validator;
            return this;
        }

        /// Hides the answer, echoing `maskChar` for each character, or nothing
        /// at all when it is `0`.
        public Text mask(char maskChar) {
            this.mask = maskChar;
            this.masked = true;
            return this;
        }

        /// Faint text shown while the answer is empty.
        public Text placeholder(String text) {
            this.placeholder = text;
            return this;
        }

        /// Asks the question and answers what was typed.
        public String ask() {
            TerminalBackend term = Console.backend();
            if (!interactive() || !term.enterRaw()) {
                return askLines(term);
            }
            try {
                return askRaw(term);
            } finally {
                term.restore();
            }
        }

        private String askLines(TerminalBackend term) {
            while (true) {
                term.write(question + (defaultValue != null && !masked ? " [" + defaultValue + "]" : "")
                        + ": ");
                String line;
                if (masked && term.isTerminal(0)) {
                    // A person is typing but raw mode failed: reading the
                    // line plainly would show the secret on screen.
                    line = term.readHidden();
                    if (line == null) {
                        throw new IllegalStateException("cannot read a hidden answer on this terminal");
                    }
                    term.write("\n");
                } else {
                    line = readLine(term);
                }
                if (line == null) {
                    term.write("\n");
                    if (defaultValue != null) {
                        return defaultValue;
                    }
                    throw new Cancelled("end of input");
                }
                String value = line.length() == 0 && defaultValue != null ? defaultValue : line;
                String error = validator == null ? null : validator.validate(value);
                if (error == null) {
                    return value;
                }
                term.write(error + "\n");
            }
        }

        private String askRaw(TerminalBackend term) {
            LiveRegion region = new LiveRegion(term, true); // NOPMD CloseResource - finish/cancel close it
            KeyReader keys = new KeyReader(term);
            LineEditor editor = new LineEditor();
            String error = null;
            String head = active(question) + " "
                    + (defaultValue != null && !masked
                            ? Console.style("(" + defaultValue + ") ", Console.Style.FAINT) : "");
            while (true) {
                draw(region, head, editor, error);
                Key k = keys.read();
                if (k.type() == Key.Type.ENTER) {
                    String value = editor.text();
                    if (value.length() == 0 && defaultValue != null) {
                        value = defaultValue;
                    }
                    error = validator == null ? null : validator.validate(value);
                    if (error == null) {
                        String shown = masked ? masked(value) : value;
                        return finish(region, question, shown, value);
                    }
                    continue;
                }
                if (k.type() == Key.Type.CTRL_D && editor.length() == 0) {
                    cancel(region, question);
                }
                if (k.type() == Key.Type.CTRL_C || k.type() == Key.Type.ESCAPE
                        || k.type() == Key.Type.EOF) {
                    cancel(region, question);
                }
                if (editor.handle(k)) {
                    error = null;
                }
            }
        }

        private String masked(String value) {
            if (mask == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            int n = value.codePointCount(0, value.length());
            for (int i = 0; i < n; i++) {
                sb.append(mask);
            }
            return sb.toString();
        }

        private void draw(LiveRegion region, String head, LineEditor editor, String error) {
            int headWidth = TextWidth.width(head);
            int available = Math.max(4, Console.width() - 1 - headWidth);
            String value = editor.text();
            String shown = masked ? masked(value) : value;
            int cursor = masked && mask == 0 ? 0 : editor.cursor();
            // Scroll sideways so the cursor stays on screen.
            int start = 0;
            while (start < cursor && columns(shown, start, cursor) > available - 1) {
                start++;
            }
            String visible = TextWidth.truncate(sub(shown, start, Integer.MAX_VALUE), available, "");
            String line = head + (shown.length() == 0 && placeholder != null
                    ? Console.style(placeholder, Console.Style.FAINT) : visible);
            int col = headWidth + columns(shown, start, cursor);
            String[] lines = error == null ? new String[] {line}
                    : new String[] {line, Console.style(error, Console.Style.RED)};
            region.updateWithCursor(lines, 0, col);
        }
    }

    /// A single-choice list with options: labels, a default, page size,
    /// filtering.
    public static final class Select<T> {
        private final String question;
        private final List<T> choices;
        private Labeler<T> labeler;
        private int defaultIndex;
        private int pageSize = 10;
        private boolean filter = true;

        /// A selection among `choices`.
        public Select(String question, List<T> choices) {
            this.question = question;
            this.choices = new ArrayList<T>(choices);
        }

        /// How a choice is shown; `String.valueOf` by default.
        public Select<T> labeler(Labeler<T> labeler) {
            this.labeler = labeler;
            return this;
        }

        /// The choice the cursor starts on.
        public Select<T> defaultIndex(int index) {
            this.defaultIndex = index;
            return this;
        }

        /// How many choices are shown at once; 10 by default.
        public Select<T> pageSize(int size) {
            this.pageSize = Math.max(1, size);
            return this;
        }

        /// Whether typing filters the list; on by default.
        public Select<T> filter(boolean on) {
            this.filter = on;
            return this;
        }

        /// Asks and answers the choice made.
        ///
        /// #### Throws
        ///
        /// - `IllegalArgumentException`: when there are no choices.
        public T ask() {
            if (choices.isEmpty()) {
                throw new IllegalArgumentException("No choices");
            }
            List<String> labels = labels(choices, labeler);
            TerminalBackend term = Console.backend();
            if (!interactive() || !term.enterRaw()) {
                return choices.get(askLines(term, question, labels, defaultIndex));
            }
            try {
                ListState state = new ListState(labels, false, pageSize, filter, defaultIndex);
                int index = state.run(term, question, 0, Integer.MAX_VALUE).get(0);
                return choices.get(index);
            } finally {
                term.restore();
            }
        }
    }

    /// A multiple-choice list with options: labels, preselected choices,
    /// limits.
    public static final class MultiSelect<T> {
        private final String question;
        private final List<T> choices;
        private Labeler<T> labeler;
        private int[] preselected = new int[0];
        private int pageSize = 10;
        private int min;
        private int max = Integer.MAX_VALUE;
        private boolean filter = true;

        /// A multiple selection among `choices`.
        public MultiSelect(String question, List<T> choices) {
            this.question = question;
            this.choices = new ArrayList<T>(choices);
        }

        /// How a choice is shown; `String.valueOf` by default.
        public MultiSelect<T> labeler(Labeler<T> labeler) {
            this.labeler = labeler;
            return this;
        }

        /// The choices selected when the list opens, by index.
        public MultiSelect<T> preselect(int... indexes) {
            this.preselected = indexes == null ? new int[0] : indexes.clone();
            return this;
        }

        /// How many choices are shown at once; 10 by default.
        public MultiSelect<T> pageSize(int size) {
            this.pageSize = Math.max(1, size);
            return this;
        }

        /// The fewest choices the answer may have.
        public MultiSelect<T> min(int count) {
            this.min = count;
            return this;
        }

        /// The most choices the answer may have.
        public MultiSelect<T> max(int count) {
            this.max = count;
            return this;
        }

        /// Whether typing filters the list; on by default.
        public MultiSelect<T> filter(boolean on) {
            this.filter = on;
            return this;
        }

        /// Asks and answers the choices made, in list order.
        public List<T> ask() {
            List<String> labels = labels(choices, labeler);
            if (min > labels.size() || min > max) {
                // No answer could satisfy it; asking would never end.
                throw new IllegalArgumentException("min " + min + " cannot be met with "
                        + labels.size() + " choices and max " + max);
            }
            TerminalBackend term = Console.backend();
            List<Integer> picked;
            if (!interactive() || !term.enterRaw()) {
                picked = askManyLines(term, question, labels, preselected, min, max);
            } else {
                try {
                    ListState state = new ListState(labels, true, pageSize, filter, 0);
                    for (int i : preselected) {
                        if (i >= 0 && i < labels.size()) {
                            state.selected[i] = true;
                        }
                    }
                    picked = state.run(term, question, min, max);
                } finally {
                    term.restore();
                }
            }
            List<T> out = new ArrayList<T>(picked.size());
            for (int i : picked) {
                out.add(choices.get(i));
            }
            return out;
        }
    }

    /// The cursor, filter and selection of an open list, and the loop that
    /// runs it.
    static final class ListState {
        final List<String> labels;
        final boolean multi;
        final int pageSize;
        final boolean filtering;
        final boolean[] selected;
        final StringBuilder filter = new StringBuilder(); // NOPMD AvoidStringBufferField - one line of typing
        List<Integer> visible = new ArrayList<Integer>();
        int cursor;
        int top;

        ListState(List<String> labels, boolean multi, int pageSize, boolean filtering, int start) {
            this.labels = labels;
            this.multi = multi;
            // An unknown height reads as 0; the configured size stands then.
            int rows = Console.height();
            this.pageSize = Math.max(1, rows > 3 ? Math.min(pageSize, rows - 3) : pageSize);
            this.filtering = filtering;
            this.selected = new boolean[labels.size()];
            refilter();
            cursor = Math.max(0, Math.min(start, visible.size() - 1));
            scroll();
        }

        void refilter() {
            List<Integer> v = new ArrayList<Integer>();
            String f = filter.toString();
            for (int i = 0; i < labels.size(); i++) {
                if (f.length() == 0 || Console.containsIgnoreCase(TextWidth.strip(labels.get(i)), f)) {
                    v.add(i);
                }
            }
            visible = v;
            cursor = 0;
            top = 0;
        }

        void scroll() {
            if (cursor < top) {
                top = cursor;
            }
            if (cursor >= top + pageSize) {
                top = cursor - pageSize + 1;
            }
        }

        void move(int delta, boolean wrap) {
            int n = visible.size();
            if (n == 0) {
                return;
            }
            int c = cursor + delta;
            if (wrap) {
                c = ((c % n) + n) % n;
            } else {
                c = Math.max(0, Math.min(n - 1, c));
            }
            cursor = c;
            scroll();
        }

        int count() {
            int n = 0;
            for (boolean b : selected) {
                if (b) {
                    n++;
                }
            }
            return n;
        }

        String[] frame(String question, String error) {
            boolean u = Console.unicode();
            List<String> out = new ArrayList<String>();
            String hint;
            if (filter.length() > 0) {
                hint = filter.toString();
            } else if (multi) {
                hint = Console.style("space to select, enter to confirm"
                        + (filtering ? ", type to filter" : ""), Console.Style.FAINT);
            } else {
                hint = filtering ? Console.style("type to filter", Console.Style.FAINT) : "";
            }
            out.add(active(question) + " " + hint);
            if (visible.isEmpty()) {
                out.add(Console.style("  no matches", Console.Style.FAINT));
            }
            int end = Math.min(visible.size(), top + pageSize);
            for (int row = top; row < end; row++) {
                int i = visible.get(row);
                boolean here = row == cursor;
                String pointer = here ? (u ? "\u276f " : "> ") : "  ";
                String box = "";
                if (multi) {
                    box = selected[i] ? (u ? "\u25c9 " : "[x] ") : (u ? "\u25ef " : "[ ] ");
                    if (selected[i]) {
                        box = Console.style(box, Console.Style.GREEN);
                    }
                }
                String label = labels.get(i);
                out.add(here ? Console.style(pointer, Console.Style.CYAN) + box
                        + Console.style(label, Console.Style.CYAN) : pointer + box + label);
            }
            if (visible.size() > pageSize) {
                out.add(Console.style("  " + (cursor + 1) + "/" + visible.size(),
                        Console.Style.FAINT));
            }
            if (error != null) {
                out.add(Console.style(error, Console.Style.RED));
            }
            return out.toArray(new String[out.size()]);
        }

        List<Integer> run(TerminalBackend term, String question, int min, int max) {
            LiveRegion region = new LiveRegion(term, true); // NOPMD CloseResource - finish/cancel close it
            KeyReader keys = new KeyReader(term);
            String error = null;
            while (true) {
                region.update(frame(question, error));
                Key k = keys.read();
                error = null;
                switch (k.type()) {
                    case UP:
                    case BACKTAB:
                        move(-1, true);
                        continue;
                    case DOWN:
                    case TAB:
                        move(1, true);
                        continue;
                    case PAGE_UP:
                        move(-pageSize, false);
                        continue;
                    case PAGE_DOWN:
                        move(pageSize, false);
                        continue;
                    case HOME:
                        move(-visible.size(), false);
                        continue;
                    case END:
                        move(visible.size(), false);
                        continue;
                    case BACKSPACE:
                        if (filter.length() > 0) {
                            filter.setLength(filter.offsetByCodePoints(filter.length(), -1));
                            refilter();
                        }
                        continue;
                    case ENTER:
                        if (!multi) {
                            if (visible.isEmpty()) {
                                continue;
                            }
                            int index = visible.get(cursor);
                            finish(region, question, labels.get(index), null);
                            return Collections.singletonList(index);
                        }
                        int n = count();
                        if (n < min) {
                            error = "Select at least " + min;
                            continue;
                        }
                        if (n > max) {
                            error = "Select at most " + max;
                            continue;
                        }
                        List<Integer> picked = new ArrayList<Integer>();
                        StringBuilder shown = new StringBuilder();
                        for (int i = 0; i < selected.length; i++) {
                            if (selected[i]) {
                                picked.add(i);
                                if (shown.length() > 0) {
                                    shown.append(", ");
                                }
                                shown.append(labels.get(i));
                            }
                        }
                        finish(region, question, shown.toString(), null);
                        return picked;
                    case CTRL:
                        if (multi && k.codePoint() == 'a') {
                            boolean all = true;
                            for (int i : visible) {
                                all &= selected[i];
                            }
                            for (int i : visible) {
                                selected[i] = !all;
                            }
                        } else if (k.codePoint() == 'u') {
                            filter.setLength(0);
                            refilter();
                        }
                        continue;
                    case CHAR:
                        if (multi && k.codePoint() == ' ') {
                            if (!visible.isEmpty()) {
                                int i = visible.get(cursor);
                                selected[i] = !selected[i];
                            }
                        } else if (filtering) {
                            filter.appendCodePoint(k.codePoint());
                            refilter();
                        }
                        continue;
                    default:
                        checkCancel(k, region, question, true);
                }
            }
        }
    }

    /// Text being edited: code points and a cursor among them.
    static final class LineEditor {
        private final StringBuilder text = new StringBuilder(); // NOPMD AvoidStringBufferField - the line being edited
        private int cursor;

        String text() {
            return text.toString();
        }

        int length() {
            return text.length();
        }

        /// The cursor as a code point index.
        int cursor() {
            return text.codePointCount(0, cursor);
        }

        /// Applies `k`; true when the text changed.
        boolean handle(Key k) {
            switch (k.type()) {
                case CHAR:
                    String s = new String(Character.toChars(k.codePoint()));
                    text.insert(cursor, s);
                    cursor += s.length();
                    return true;
                case BACKSPACE:
                    if (k.alt()) {
                        return deleteWord();
                    }
                    if (cursor > 0) {
                        int prev = text.offsetByCodePoints(cursor, -1);
                        text.delete(prev, cursor);
                        cursor = prev;
                        return true;
                    }
                    return false;
                case DELETE:
                    if (cursor < text.length()) {
                        text.delete(cursor, text.offsetByCodePoints(cursor, 1));
                        return true;
                    }
                    return false;
                case LEFT:
                    if (cursor > 0) {
                        cursor = text.offsetByCodePoints(cursor, -1);
                    }
                    return false;
                case RIGHT:
                    if (cursor < text.length()) {
                        cursor = text.offsetByCodePoints(cursor, 1);
                    }
                    return false;
                case HOME:
                    cursor = 0;
                    return false;
                case END:
                    cursor = text.length();
                    return false;
                case CTRL:
                    switch (k.codePoint()) {
                        case 'a':
                            cursor = 0;
                            return false;
                        case 'e':
                            cursor = text.length();
                            return false;
                        case 'u':
                            text.delete(0, cursor);
                            cursor = 0;
                            return true;
                        case 'k':
                            text.setLength(cursor);
                            return true;
                        case 'w':
                            return deleteWord();
                        default:
                            return false;
                    }
                default:
                    return false;
            }
        }

        private boolean deleteWord() {
            int end = cursor;
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }
            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }
            if (i == end) {
                return false;
            }
            text.delete(i, end);
            cursor = i;
            return true;
        }
    }

    static String active(String question) {
        return Console.style("?", Console.Style.CYAN, Console.Style.BOLD) + " "
                + Console.style(question, Console.Style.BOLD);
    }

    private static <R> R finish(LiveRegion region, String question, String shown, R result) {
        String check = Console.style(Console.unicode() ? "\u2714" : "v", Console.Style.GREEN);
        String line = check + " " + Console.style(question, Console.Style.BOLD)
                + (shown.length() > 0 ? " " + Console.style(shown, Console.Style.CYAN) : "");
        region.update(line);
        region.close();
        return result;
    }

    private static void checkCancel(Key k, LiveRegion region, String question, boolean escape) {
        if (k.type() == Key.Type.CTRL_C || k.type() == Key.Type.EOF
                || (escape && k.type() == Key.Type.ESCAPE)) {
            cancel(region, question);
        }
    }

    private static void cancel(LiveRegion region, String question) {
        String cross = Console.style(Console.unicode() ? "\u2716" : "x", Console.Style.RED);
        region.update(cross + " " + Console.style(question, Console.Style.BOLD)
                + Console.style(" cancelled", Console.Style.FAINT));
        region.close();
        throw new Cancelled(question);
    }

    private static <T> List<String> labels(List<T> choices, Labeler<T> labeler) {
        List<String> out = new ArrayList<String>(choices.size());
        for (T c : choices) {
            out.add(labeler == null ? String.valueOf(c) : labeler.label(c));
        }
        return out;
    }

    private static int askLines(TerminalBackend term, String question, List<String> labels,
            int defaultIndex) {
        term.write(question + "\n" + numbered(labels));
        while (true) {
            term.write("Enter a number" + (defaultIndex >= 0 && defaultIndex < labels.size()
                    ? " [" + (defaultIndex + 1) + "]" : "") + ": ");
            String line = readLine(term);
            if (line == null || line.trim().length() == 0) {
                if (line == null) {
                    term.write("\n");
                }
                if (defaultIndex >= 0 && defaultIndex < labels.size()) {
                    return defaultIndex;
                }
                if (line == null) {
                    throw new Cancelled("end of input");
                }
                continue;
            }
            int index = choice(line.trim(), labels);
            if (index >= 0) {
                return index;
            }
            term.write("Not one of the choices: " + line.trim() + "\n");
        }
    }

    private static List<Integer> askManyLines(TerminalBackend term, String question,
            List<String> labels, int[] preselected, int min, int max) {
        term.write(question + "\n" + numbered(labels));
        StringBuilder def = new StringBuilder();
        for (int i : preselected) {
            if (i < 0 || i >= labels.size()) {
                // Out of range: dropped here as the interactive list drops it,
                // or Enter would answer with something no choice matches.
                continue;
            }
            if (def.length() > 0) {
                def.append(',');
            }
            def.append(i + 1);
        }
        while (true) {
            term.write("Enter numbers separated by commas"
                    + (def.length() > 0 ? " [" + def + "]" : "") + ": ");
            String line = readLine(term);
            if (line == null) {
                term.write("\n");
            }
            String answer = line == null || line.trim().length() == 0 ? def.toString() : line;
            boolean[] picked = new boolean[labels.size()];
            String bad = null;
            // No String.split on the device: its regex members are absent.
            int from = 0;
            while (from <= answer.length()) {
                int comma = answer.indexOf(',', from);
                int to = comma < 0 ? answer.length() : comma;
                String p = answer.substring(from, to).trim();
                from = to + 1;
                if (p.length() == 0) {
                    continue;
                }
                int index = choice(p, labels);
                if (index < 0) {
                    bad = p;
                    break;
                }
                picked[index] = true;
            }
            List<Integer> out = new ArrayList<Integer>();
            for (int i = 0; i < picked.length; i++) {
                if (picked[i]) {
                    out.add(i);
                }
            }
            String error = bad != null ? "Not one of the choices: " + bad
                    : out.size() < min ? "Select at least " + min
                    : out.size() > max ? "Select at most " + max : null;
            if (error == null) {
                return out;
            }
            if (line == null) {
                throw new Cancelled("end of input");
            }
            term.write(error + "\n");
        }
    }

    private static String numbered(List<String> labels) {
        StringBuilder sb = new StringBuilder();
        int digits = Integer.toString(labels.size()).length();
        for (int i = 0; i < labels.size(); i++) {
            sb.append("  ").append(TextWidth.pad(Integer.toString(i + 1), digits, Table.Align.RIGHT))
                    .append(") ").append(labels.get(i)).append('\n');
        }
        return sb.toString();
    }

    /// The index `answer` names, as a 1-based number or a label (case
    /// ignored), or -1.
    private static int choice(String answer, List<String> labels) {
        boolean digits = answer.length() > 0 && answer.length() < 10;
        for (int i = 0; i < answer.length() && digits; i++) {
            char c = answer.charAt(i);
            digits = c >= '0' && c <= '9';
        }
        if (digits) {
            int n = Integer.parseInt(answer);
            return n >= 1 && n <= labels.size() ? n - 1 : -1;
        }
        for (int i = 0; i < labels.size(); i++) {
            if (TextWidth.strip(labels.get(i)).equalsIgnoreCase(answer)) {
                return i;
            }
        }
        return -1;
    }

    /// One line of standard input without its line break, decoded as UTF-8,
    /// or null at end of input.
    static String readLine(TerminalBackend term) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (true) {
            int b = term.readByte(-1);
            if (b < 0) {
                if (bytes.size() == 0) {
                    return null;
                }
                break;
            }
            if (b == '\n') {
                break;
            }
            bytes.write(b);
        }
        byte[] data = bytes.toByteArray();
        int len = data.length;
        if (len > 0 && data[len - 1] == '\r') {
            len--;
        }
        try {
            return new String(data, 0, len, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Every runtime has UTF-8.
            throw new IllegalStateException(e);
        }
    }

    private static int columns(String s, int fromCodePoint, int toCodePoint) {
        return TextWidth.width(sub(s, fromCodePoint, toCodePoint));
    }

    private static String sub(String s, int fromCodePoint, int toCodePoint) {
        int n = s.codePointCount(0, s.length());
        int a = Math.min(fromCodePoint, n);
        int b = Math.min(toCodePoint, n);
        if (a >= b) {
            return "";
        }
        return s.substring(s.offsetByCodePoints(0, a), s.offsetByCodePoints(0, b));
    }
}
