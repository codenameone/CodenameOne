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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A report goes on paper as lines of text: the header, the table with its
/// numbers lined up at the end of their columns, the totals last.
class PrintoutTest {
    /// 2026-03-04 05:06 UTC.
    private static final long GENERATED = 1772600760000L;

    private static Printout statement() {
        Printout p = new Printout("Statement", GENERATED)
                .fact("Account", "Everyday - Checking - USD")
                .fact("Period", "Mar 1 to Mar 4");
        p.section(null, new String[] {"Date", "Description", "Balance"}, new int[] {1, 2, 1},
                new boolean[] {false, false, true})
                .row("Mar 1", "Coffee", "$10.00")
                .row("Mar 2", "A description far too long for the column it is printed in, "
                        + "which is cut", "$1,234.56")
                .total("", "Total", "$1,244.56");
        return p.total("Closing balance", "$1,244.56");
    }

    @Test
    void theHeaderNamesTheBankTheReportTheAccountThePeriodAndTheTime() {
        List<String> lines = statement().lines();
        assertEquals(Printout.BANK, lines.get(0));
        assertEquals("Statement", lines.get(1));
        assertTrue(lines.get(3).startsWith("Account:"), lines.get(3));
        assertTrue(lines.get(3).endsWith("Everyday - Checking - USD"), lines.get(3));
        assertTrue(lines.get(4).startsWith("Period:"), lines.get(4));
        assertTrue(lines.get(5).endsWith("2026-03-04 05:06 UTC"), lines.get(5));
    }

    @Test
    void columnsLineUpAndNothingIsWiderThanThePage() {
        List<String> lines = statement().lines();
        int[] widths = Printout.widths(new int[] {1, 2, 1}, Printout.COLUMNS);
        int numberEnd = Printout.COLUMNS;
        int rows = 0;
        for (String line : lines) {
            assertTrue(line.length() <= Printout.COLUMNS, line);
            if (line.endsWith("$10.00") || line.endsWith("$1,234.56")) {
                // Numbers end at the page's edge: right aligned.
                assertEquals(numberEnd, line.length(), line);
                rows++;
            }
        }
        assertEquals(2, rows);
        assertEquals(Printout.COLUMNS, widths[0] + widths[1] + widths[2] + 2);
        // A cell too long for its column is cut, and says so.
        boolean cut = false;
        for (String line : lines) {
            cut |= line.indexOf("A description far") >= 0 && line.indexOf("..") > 0;
        }
        assertTrue(cut);
    }

    @Test
    void totalsComeLastAndAreSetApart() {
        List<String> lines = statement().lines();
        String last = lines.get(lines.size() - 1);
        assertTrue(last.startsWith("Closing balance"), last);
        assertTrue(last.endsWith("$1,244.56"), last);
        assertEquals(Printout.COLUMNS, last.length());
        int total = -1;
        for (int iter = 0; iter < lines.size(); iter++) {
            if (lines.get(iter).indexOf("Total") >= 0) {
                total = iter;
            }
        }
        assertTrue(lines.get(total - 1).startsWith("-----"), "a rule above the table's total");
    }

    @Test
    void aNoteRunsAcrossTheTable() {
        Printout p = new Printout("Audit trail", GENERATED);
        p.section("Events", new String[] {"#", "Action"}, new int[] {1, 3},
                new boolean[] {true, false}).note("No events.");
        List<String> lines = p.lines();
        assertTrue(lines.contains("Events"));
        assertEquals("No events.", lines.get(lines.size() - 1));
    }

    private static Printout longStatement(int rows) {
        Printout p = new Printout("Statement", GENERATED).fact("Account", "Everyday");
        Printout.Section s = p.section("Transactions", new String[] {"Date", "Description", "Balance"},
                new int[] {1, 2, 1}, new boolean[] {false, false, true});
        for (int iter = 0; iter < rows; iter++) {
            s.row("Mar " + iter, "Row " + iter, "$" + iter + ".00");
        }
        s.total("", "Total", "$1.00");
        return p.total("Closing balance", "$1.00");
    }

    @Test
    void aLongReportBreaksIntoNumberedPagesThatRepeatTheTableHeader() {
        Printout p = longStatement(60);
        List<Printout.Page> pages = p.pages(20);
        assertTrue(pages.size() > 3, "pages: " + pages.size());
        int rows = 0;
        for (int iter = 0; iter < pages.size(); iter++) {
            List<String> lines = pages.get(iter).lines();
            assertTrue(lines.size() <= 20, "page " + iter + " has " + lines.size());
            String last = lines.get(lines.size() - 1);
            assertTrue(last.trim().equals("Page " + (iter + 1) + " of " + pages.size()), last);
            if (iter > 0) {
                assertTrue(lines.get(0).startsWith(Printout.BANK), lines.get(0));
                if (lines.contains("Transactions (continued)")) {
                    boolean header = false;
                    for (String line : lines) {
                        header |= line.startsWith("Date") && line.indexOf("Balance") > 0;
                    }
                    assertTrue(header, "page " + iter + " repeats the header");
                }
            }
            for (String line : lines) {
                if (line.matches("Mar \\d+ .*")) {
                    rows++;
                }
            }
        }
        assertEquals(60, rows, "every row is printed once");
        List<String> end = pages.get(pages.size() - 1).lines();
        boolean closing = false;
        for (String line : end) {
            closing |= line.startsWith("Closing balance");
        }
        assertTrue(closing, "the totals end the last page");
    }

    @Test
    void aShortReportIsOnePage() {
        assertEquals(1, statement().pages(60).size());
    }

    @Test
    void thePdfHasAPagePerPictureAndAValidCrossReference() throws Exception {
        PdfPages pdf = new PdfPages(595, 842);
        pdf.add(new int[] {0xffffff, 0, 0xff0000, 0x00ff00}, 2, 2);
        pdf.add(new int[] {0x123456}, 1, 1);
        byte[] bytes = pdf.toBytes();
        String text = new String(bytes, "ISO-8859-1");
        assertTrue(text.startsWith("%PDF-1.4"));
        assertTrue(text.indexOf("/Count 2") > 0);
        assertTrue(text.endsWith("%%EOF\n"));
        int xref = Integer.parseInt(text.substring(text.lastIndexOf("startxref\n") + 10,
                text.lastIndexOf("\n%%EOF")).trim());
        assertTrue(text.startsWith("xref", xref));
        String[] entries = text.substring(xref).split("\n");
        for (int obj = 1; obj <= 8; obj++) {
            int at = Integer.parseInt(entries[2 + obj].substring(0, 10));
            assertTrue(text.startsWith(obj + " 0 obj", at), "object " + obj);
        }
    }

    @Test
    void theStrongestWayInIsOfferedFirst() {
        assertEquals(SignInForm.PASSKEY, SignInForm.strongest(true, true));
        assertEquals(SignInForm.PASSKEY, SignInForm.strongest(true, false));
        assertEquals(SignInForm.DEVICE, SignInForm.strongest(false, true));
        assertEquals(SignInForm.PASSWORD, SignInForm.strongest(false, false));
    }
}
