/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

import com.codename1.components.ToastBar;
import com.codename1.printing.PrintResult;
import com.codename1.printing.Printer;
import com.codename1.ui.Button;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/// A report as it goes on paper: the bank's name, what the report is, the
/// account and period it covers and when it was made; then its tables; then
/// its totals.
///
/// The page is laid out as lines of monospaced text ([#lines]) -- columns
/// line up without measuring a font, and the layout is a plain list of
/// strings a test can read -- and drawn onto a white image ([#render]), which
/// [Printer#printImage] hands to the platform's print dialog. Every text is
/// given already translated: the screen that builds the printout has the
/// words the reader sees.
public final class Printout {
    /// Characters across the page.
    public static final int COLUMNS = 96;
    /// The name at the top of every page.
    public static final String BANK = "Bastion";

    private final String title;
    private final long generated;
    /// The header's word for [#generated]: translated when the page is
    /// printed, so that laying it out needs no theme.
    private String generatedLabel = "Generated";
    private final List<String[]> facts = new ArrayList<String[]>();
    private final List<Section> sections = new ArrayList<Section>();
    private final List<String[]> totals = new ArrayList<String[]>();

    /// One table of the report.
    public static final class Section {
        String caption;
        final String[] headers;
        final int[] weights;
        final boolean[] numeric;
        final List<String[]> rows = new ArrayList<String[]>();
        /// Per row: 0 a line, 1 a total, 2 a note across the table.
        final List<Integer> kinds = new ArrayList<Integer>();

        Section(String caption, String[] headers, int[] weights, boolean[] numeric) {
            this.caption = caption;
            this.headers = headers;
            this.weights = weights;
            this.numeric = numeric;
        }

        /// Names the table on paper.
        public Section caption(String text) {
            caption = text;
            return this;
        }

        /// Adds a line of the table.
        public Section row(String... cells) {
            rows.add(cells);
            kinds.add(Integer.valueOf(0));
            return this;
        }

        /// Adds a total line, set apart from the lines above it.
        public Section total(String... cells) {
            rows.add(cells);
            kinds.add(Integer.valueOf(1));
            return this;
        }

        /// Adds a line of text across the whole table.
        public Section note(String text) {
            rows.add(new String[] {text});
            kinds.add(Integer.valueOf(2));
            return this;
        }

        public int size() {
            return rows.size();
        }
    }

    /// @param title what the report is, translated
    /// @param generated when it was made, in milliseconds
    public Printout(String title, long generated) {
        this.title = title;
        this.generated = generated;
    }

    /// A line of the header: the account, the period.
    public Printout fact(String label, String value) {
        facts.add(new String[] {label, value == null ? "" : value});
        return this;
    }

    /// A new table, `caption` (may be null) above it.
    public Section section(String caption, String[] headers, int[] weights, boolean[] numeric) {
        Section s = new Section(caption, headers, weights, numeric);
        sections.add(s);
        return s;
    }

    /// Adds a table the screen already filled.
    public Printout add(Section section) {
        sections.add(section);
        return this;
    }

    /// A line of the totals at the end.
    public Printout total(String label, String value) {
        totals.add(new String[] {label, value == null ? "" : value});
        return this;
    }

    public String title() {
        return title;
    }

    /// The page, as lines of at most [#COLUMNS] characters.
    public List<String> lines() {
        List<String> out = new ArrayList<String>();
        out.add(BANK);
        out.add(fit(title, COLUMNS));
        out.add(repeat('=', COLUMNS));
        int labelWidth = generatedLabel.length();
        for (String[] f : facts) {
            labelWidth = Math.max(labelWidth, f[0].length());
        }
        labelWidth = Math.min(labelWidth, COLUMNS / 3);
        for (String[] f : facts) {
            out.add(fit(pad(fit(f[0], labelWidth) + ":", labelWidth + 2) + f[1], COLUMNS));
        }
        out.add(pad(generatedLabel + ":", labelWidth + 2) + stamp(generated));
        for (Section s : sections) {
            out.add("");
            if (s.caption != null && s.caption.length() > 0) {
                out.add(fit(s.caption, COLUMNS));
            }
            int[] widths = widths(s.weights, COLUMNS);
            out.add(cells(s.headers, widths, s.numeric));
            out.add(repeat('-', COLUMNS));
            boolean totalsBegun = false;
            for (int iter = 0; iter < s.rows.size(); iter++) {
                int kind = s.kinds.get(iter).intValue();
                if (kind == 2) {
                    out.add(fit(s.rows.get(iter)[0], COLUMNS));
                    continue;
                }
                if (kind == 1 && !totalsBegun) {
                    out.add(repeat('-', COLUMNS));
                    totalsBegun = true;
                }
                out.add(cells(s.rows.get(iter), widths, s.numeric));
            }
        }
        if (!totals.isEmpty()) {
            out.add("");
            out.add(repeat('=', COLUMNS));
            for (String[] t : totals) {
                int room = COLUMNS - t[1].length() - 1;
                out.add(pad(fit(t[0], Math.max(1, room)), Math.max(1, room) + 1) + t[1]);
            }
        }
        return out;
    }

    /// The page drawn in black on white.
    public Image render() {
        List<String> lines = lines();
        Font mono = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font bold = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL);
        int cell = Math.max(1, mono.charWidth('M'));
        int line = mono.getHeight() + 2;
        int margin = cell * 4;
        Image page = Image.createImage(cell * COLUMNS + margin * 2,
                line * lines.size() + margin * 2, 0xffffffff);
        Graphics g = page.getGraphics();
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
        g.setColor(0x000000);
        for (int iter = 0; iter < lines.size(); iter++) {
            String text = lines.get(iter);
            // The bank, the title, and the totals at the end are bold.
            g.setFont(iter < 2 || isTotal(lines, iter) ? bold : mono);
            // Monospaced by position, not by the font's own advance: a
            // character the font lacks must not move the next column.
            int y = margin + iter * line;
            for (int at = 0; at < text.length(); at++) {
                char c = text.charAt(at);
                if (c != ' ') {
                    g.drawChar(c, margin + at * cell, y);
                }
            }
        }
        return page;
    }

    private boolean isTotal(List<String> lines, int index) {
        if (totals.isEmpty()) {
            return false;
        }
        return index >= lines.size() - totals.size();
    }

    /// Makes the printout when it is asked for, so that it holds what the
    /// screen shows by then.
    public interface Source {
        Printout get();
    }

    /// Whether this platform can print. Where it cannot, a screen shows no
    /// print button rather than one that fails.
    public static boolean supported() {
        return Printer.isPrintingSupported();
    }

    /// A print button for what `source` makes, or null where this platform
    /// cannot print.
    public static Button button(final Source source) {
        if (!supported()) {
            return null;
        }
        Button print = new Button(Lang.tr("Print"), "BnSecondary");
        FontImage.setMaterialIcon(print, FontImage.MATERIAL_PRINT);
        print.addActionListener(evt -> print(source.get()));
        return print;
    }

    /// Hands `printout` to the platform's print dialog, and says how it went.
    public static void print(Printout printout) {
        if (!Printer.isPrintingSupported()) {
            ToastBar.showErrorMessage(Lang.tr("Printing is not available on this device."));
            return;
        }
        printout.generatedLabel = Lang.tr("Generated");
        Printer.printImage(printout.render(), result -> {
            if (result.getStatus() == PrintResult.STATUS_FAILED) {
                ToastBar.showErrorMessage(Lang.tr("Could not print: {0}",
                        String.valueOf(result.getError())));
            } else if (result.getStatus() == PrintResult.STATUS_COMPLETED) {
                ToastBar.showInfoMessage(Lang.tr("Sent to the printer."));
            }
        });
    }

    /// `millis` as `yyyy-MM-dd HH:mm UTC`: a printed report is read later and
    /// elsewhere, so it names its time zone rather than assume the reader's.
    static String stamp(long millis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(new Date(millis));
        return c.get(Calendar.YEAR) + "-" + two(c.get(Calendar.MONTH) + 1) + "-"
                + two(c.get(Calendar.DAY_OF_MONTH)) + " " + two(c.get(Calendar.HOUR_OF_DAY))
                + ":" + two(c.get(Calendar.MINUTE)) + " UTC";
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    /// Each column's characters, shared out by weight, one space between.
    static int[] widths(int[] weights, int columns) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int room = columns - (weights.length - 1);
        int[] out = new int[weights.length];
        int used = 0;
        for (int iter = 0; iter < weights.length; iter++) {
            out[iter] = iter == weights.length - 1 ? room - used
                    : Math.max(1, room * weights[iter] / Math.max(1, total));
            used += out[iter];
        }
        return out;
    }

    private static String cells(String[] cells, int[] widths, boolean[] numeric) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < widths.length; iter++) {
            String text = iter < cells.length && cells[iter] != null ? cells[iter] : "";
            text = fit(text, widths[iter]);
            if (iter > 0) {
                out.append(' ');
            }
            if (numeric[iter]) {
                out.append(repeat(' ', widths[iter] - text.length())).append(text);
            } else {
                out.append(text).append(repeat(' ', widths[iter] - text.length()));
            }
        }
        // No trailing spaces: a line ends where its text does.
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == ' ') {
            end--;
        }
        return out.substring(0, end);
    }

    /// `text` cut to `width`, ending in `..` when it was cut.
    static String fit(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return width <= 2 ? text.substring(0, width) : text.substring(0, width - 2) + "..";
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text : text + repeat(' ', width - text.length());
    }

    private static String repeat(char c, int count) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < count; iter++) {
            out.append(c);
        }
        return out.toString();
    }
}
