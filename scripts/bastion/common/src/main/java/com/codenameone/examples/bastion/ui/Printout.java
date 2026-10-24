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
import com.codename1.io.FileSystemStorage;
import com.codename1.printing.PrintResult;
import com.codename1.printing.Printer;
import com.codename1.ui.Button;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/// A report as it goes on paper: the bank's name, what the report is, the
/// account and period it covers and when it was made; then its tables; then
/// its totals.
///
/// The report is laid out as lines of monospaced text ([#lines]) -- columns
/// line up without measuring a font, and the layout is a plain list of
/// strings a test can read -- cut into A4 pages ([#pages]), each drawn onto a
/// white image ([#render]); the pages go into one PDF ([PdfPages]), which
/// [Printer#printPDF] hands to the platform's print dialog. Every text is
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
    /// "Page {0} of {1}" and "{0} (continued)", likewise translated at print time.
    private String pageLabel = "Page {0} of {1}";
    private String continuedLabel = "{0} (continued)";
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

    /// The report, as lines of at most [#COLUMNS] characters, before it is
    /// cut into pages ([#pages]).
    public List<String> lines() {
        List<String> out = new ArrayList<String>();
        for (Line line : body()) {
            out.add(line.text);
        }
        return out;
    }

    /// One line of the report, with what pagination needs to know about it.
    private static final class Line {
        final String text;
        /// Bold on paper: the bank, the title, the totals at the end.
        final boolean bold;
        /// Opens a table (the space, caption, column names and rule above
        /// its first row): never left at the foot of a page on its own.
        final boolean opens;
        /// For a row of a table, the lines that head the table again at the
        /// top of a page the table runs on to; null for anything else.
        final String[] heading;

        Line(String text, boolean bold, boolean opens, String[] heading) {
            this.text = text;
            this.bold = bold;
            this.opens = opens;
            this.heading = heading;
        }
    }

    private List<Line> body() {
        List<Line> out = new ArrayList<Line>();
        out.add(new Line(BANK, true, false, null));
        out.add(new Line(fit(title, COLUMNS), true, false, null));
        out.add(new Line(repeat('=', COLUMNS), false, false, null));
        int labelWidth = generatedLabel.length();
        for (String[] f : facts) {
            labelWidth = Math.max(labelWidth, f[0].length());
        }
        labelWidth = Math.min(labelWidth, COLUMNS / 3);
        for (String[] f : facts) {
            out.add(new Line(fit(pad(fit(f[0], labelWidth) + ":", labelWidth + 2) + f[1], COLUMNS),
                    false, false, null));
        }
        out.add(new Line(pad(generatedLabel + ":", labelWidth + 2) + stamp(generated), false, false, null));
        for (Section s : sections) {
            int[] widths = widths(s.weights, COLUMNS);
            String header = cells(s.headers, widths, s.numeric);
            String rule = repeat('-', COLUMNS);
            boolean captioned = s.caption != null && s.caption.length() > 0;
            String[] heading = captioned
                    ? new String[] {fit(format(continuedLabel, "{0}", s.caption), COLUMNS), header, rule}
                    : new String[] {header, rule};
            out.add(new Line("", false, true, null));
            if (captioned) {
                out.add(new Line(fit(s.caption, COLUMNS), false, true, null));
            }
            out.add(new Line(header, false, true, null));
            out.add(new Line(rule, false, true, null));
            boolean totalsBegun = false;
            for (int iter = 0; iter < s.rows.size(); iter++) {
                int kind = s.kinds.get(iter).intValue();
                if (kind == 2) {
                    out.add(new Line(fit(s.rows.get(iter)[0], COLUMNS), false, false, heading));
                    continue;
                }
                if (kind == 1 && !totalsBegun) {
                    out.add(new Line(rule, false, false, heading));
                    totalsBegun = true;
                }
                out.add(new Line(cells(s.rows.get(iter), widths, s.numeric), false, false, heading));
            }
        }
        if (!totals.isEmpty()) {
            out.add(new Line("", false, true, null));
            out.add(new Line(repeat('=', COLUMNS), false, true, null));
            for (String[] t : totals) {
                int room = COLUMNS - t[1].length() - 1;
                // The totals stay together: every one but the last opens.
                boolean last = t == totals.get(totals.size() - 1);
                out.add(new Line(pad(fit(t[0], Math.max(1, room)), Math.max(1, room) + 1) + t[1],
                        true, !last, null));
            }
        }
        return out;
    }

    /// A sheet of paper: its lines, top to bottom, the last one its page
    /// number.
    public static final class Page {
        final List<String> lines = new ArrayList<String>();
        final List<Boolean> bold = new ArrayList<Boolean>();

        public List<String> lines() {
            return lines;
        }

        boolean isBold(int index) {
            return bold.get(index).booleanValue();
        }

        void add(String text, boolean strong) {
            lines.add(text);
            bold.add(Boolean.valueOf(strong));
        }
    }

    /// The report cut into pages of `linesPerPage` lines. The first page has
    /// the full header; every page after it names the bank and the report,
    /// and a table that runs on to it repeats its column names there. A table
    /// never starts at the foot of a page with none of its rows under it, the
    /// totals at the end stay together, and the last line of every page is
    /// "Page X of Y".
    public List<Page> pages(int linesPerPage) {
        // Two lines at the foot: a space, then the page number.
        int room = Math.max(8, linesPerPage) - 2;
        List<Line> all = body();
        List<Page> pages = new ArrayList<Page>();
        Page page = new Page();
        int bodyLines = 0;
        for (int iter = 0; iter < all.size(); iter++) {
            Line line = all.get(iter);
            // What has to fit together: a run of opening lines and the line
            // after it.
            int together = 1;
            if (line.opens) {
                int at = iter;
                while (at < all.size() && all.get(at).opens) {
                    at++;
                }
                together = Math.min(at, all.size() - 1) - iter + 1;
            }
            if (page.lines.size() + together > room && bodyLines > 0) {
                pages.add(page);
                page = new Page();
                bodyLines = 0;
                page.add(fit(BANK + " - " + title, COLUMNS), true);
                page.add(repeat('=', COLUMNS), false);
                if (!line.opens && line.heading != null) {
                    for (String h : line.heading) {
                        page.add(h, false);
                    }
                }
            }
            if (bodyLines == 0 && pages.size() > 0 && line.text.length() == 0) {
                // A page does not start with a space.
                continue;
            }
            page.add(line.text, line.bold);
            bodyLines++;
        }
        pages.add(page);
        for (int iter = 0; iter < pages.size(); iter++) {
            Page p = pages.get(iter);
            while (p.lines.size() < room + 1) {
                p.add("", false);
            }
            String number = fit(format(format(pageLabel, "{0}", String.valueOf(iter + 1)), "{1}",
                    String.valueOf(pages.size())), COLUMNS);
            p.add(repeat(' ', COLUMNS - number.length()) + number, false);
        }
        return pages;
    }

    /// `pattern` with `mark` replaced by `value`.
    private static String format(String pattern, String mark, String value) {
        int at = pattern.indexOf(mark);
        return at < 0 ? pattern
                : pattern.substring(0, at) + value + pattern.substring(at + mark.length());
    }

    /// Paper is A4 upright: 210 by 297 millimetres.
    static final int PAGE_WIDTH_POINTS = 595;
    static final int PAGE_HEIGHT_POINTS = 842;

    /// The pages drawn in black on white, each the shape of a sheet of A4.
    public List<Image> render() {
        Font mono = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font bold = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL);
        int cell = Math.max(1, mono.charWidth('M'));
        int line = mono.getHeight() + 2;
        int margin = cell * 4;
        int width = cell * COLUMNS + margin * 2;
        int height = width * PAGE_HEIGHT_POINTS / PAGE_WIDTH_POINTS;
        List<Image> out = new ArrayList<Image>();
        for (Page p : pages((height - margin * 2) / line)) {
            Image image = Image.createImage(width, height, 0xffffffff);
            Graphics g = image.getGraphics();
            g.setAntiAliased(true);
            g.setAntiAliasedText(true);
            g.setColor(0x000000);
            for (int iter = 0; iter < p.lines.size(); iter++) {
                String text = p.lines.get(iter);
                g.setFont(p.isBold(iter) ? bold : mono);
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
            out.add(image);
        }
        return out;
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
        // Translated with their placeholders left in; the page fills them.
        printout.pageLabel = Lang.tr("Page {0} of {1}");
        printout.continuedLabel = Lang.tr("{0} (continued)");
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        final String path = fs.getAppHomePath() + "bastion-report.pdf";
        try {
            PdfPages pdf = new PdfPages(PAGE_WIDTH_POINTS, PAGE_HEIGHT_POINTS);
            for (Image page : printout.render()) {
                pdf.add(page.getRGB(), page.getWidth(), page.getHeight());
            }
            OutputStream out = fs.openOutputStream(path);
            try {
                out.write(pdf.toBytes());
            } finally {
                out.close();
            }
        } catch (IOException err) {
            ToastBar.showErrorMessage(Lang.tr("Could not print: {0}", String.valueOf(err.getMessage())));
            return;
        }
        // One document of many pages, so the print dialog paginates it and
        // shows them all, rather than shrinking one tall image to a sheet.
        Printer.printPDF(path, result -> {
            fs.delete(path);
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
