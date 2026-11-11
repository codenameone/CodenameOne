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
package com.codename1.ui.scheduler;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pages of a scheduler and of a Gantt chart: how many there are, that
/// rendering one leaves the view on screen as it was, and that the PDF the
/// pages are written into is one a reader can open.
class SchedulePrinterTest extends UITestBase {
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static long at(int day, int hour) {
        return ZonedDateTime.of(2026, 1, day, hour, 0, 0, 0, UTC).toInstant().toEpochMilli();
    }

    private Gantt gantt() {
        ProjectModel p = new ProjectModel(new WorkCalendar(UTC), at(5, 0));
        GanttTask a = new GanttTask("a", "Design", 5 * 8 * TimeMath.HOUR);
        GanttTask b = new GanttTask("b", "Build", 10 * 8 * TimeMath.HOUR);
        GanttTask c = new GanttTask("c", "Test", 5 * 8 * TimeMath.HOUR);
        p.addTask(a);
        p.addTask(b);
        p.addTask(c);
        p.link(a, b);
        p.link(b, c);
        p.update();
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Gantt g = new Gantt(p);
        g.setCurrentTime(at(5, 10));
        form.add(BorderLayout.CENTER, g);
        form.revalidate();
        GanttChart chart = g.surface();
        chart.layoutNow();
        double dayPx = TimeMath.DAY / chart.axis().getMillisPerPixel();
        chart.zoom((float) (60 / dayPx));
        chart.layoutNow();
        return g;
    }

    private Scheduler week() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(new ScheduleItem("m", "Meeting", at(5, 9), at(5, 11)));
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Scheduler s = new Scheduler(model);
        s.setZone(UTC);
        s.set24Hours(true);
        s.setCurrentTime(at(5, 10));
        s.setDate(at(5, 0));
        s.setView(Scheduler.View.WEEK);
        form.add(BorderLayout.CENTER, s);
        form.revalidate();
        ((ScrollSurface) s.surface()).layoutNow();
        return s;
    }

    @FormTest
    void aLongProjectTakesMorePagesOnASmallerSheet() {
        Gantt g = gantt();
        SchedulePrinter printer = new SchedulePrinter(g);
        printer.setPageSize(4000, 3000);
        int roomy = printer.getPageCount();
        printer.setPageSize(500, 400);
        int tight = printer.getPageCount();
        assertTrue(roomy >= 1);
        assertTrue(tight > roomy, tight + " pages on the small sheet, " + roomy + " on the large");
    }

    @FormTest
    void renderingAPageLeavesTheChartOnScreenAsItWas() {
        Gantt g = gantt();
        GanttChart chart = g.surface();
        chart.scrollTo(37, 0);
        int w = chart.getWidth();
        int h = chart.getHeight();
        int x = chart.getX();
        int y = chart.getY();
        int offX = chart.getOffX();
        double scale = chart.axis().getMillisPerPixel();
        int rows = chart.rows().size();

        SchedulePrinter printer = new SchedulePrinter(g);
        printer.setPageSize(500, 400);
        int count = printer.getPageCount();
        Image last = printer.renderPage(count - 1);
        assertEquals(500, last.getWidth());
        assertEquals(400, last.getHeight());

        assertEquals(w, chart.getWidth());
        assertEquals(h, chart.getHeight());
        assertEquals(x, chart.getX());
        assertEquals(y, chart.getY());
        assertEquals(offX, chart.getOffX());
        assertEquals(scale, chart.axis().getMillisPerPixel(), 0.0001);
        assertEquals(rows, chart.rows().size());

        // a sheet wide enough for the whole project at a coarser scale than
        // the screen allows is still not the screen's business
        printer.setPageSize(20000, 400);
        printer.renderPage(0);
        assertEquals(scale, chart.axis().getMillisPerPixel(), 0.0001, "printing does not zoom the screen");
        assertEquals(offX, chart.getOffX());
    }

    @FormTest
    void aPageIsPaintedAndNotLeftBlank() {
        Gantt g = gantt();
        SchedulePrinter printer = new SchedulePrinter(g);
        printer.setPageSize(600, 400);
        Image page = printer.renderPage(0);
        int[] rgb = page.getRGB();
        int paper = rgb[rgb.length - 1];
        boolean painted = false;
        for (int i = 0; i < rgb.length && !painted; i++) {
            painted = rgb[i] != paper;
        }
        assertTrue(painted, "the first page has something on it");
    }

    @FormTest
    void aWeekRunsDownSeveralPagesUnderTheSchedulersOwnTitle() {
        Scheduler s = week();
        SchedulePrinter printer = new SchedulePrinter(s);
        assertNotNull(printer.getTitle());
        assertTrue(printer.getTitle().length() > 0, "the range the toolbar names");
        DayGrid grid = (DayGrid) s.surface();
        float hour = grid.getHourHeight();
        int day = Math.round(24 * hour);
        // An hour is never drawn lower than 4 mm, so a day is at least 96
        // and a sheet with 40 or so of room for hours takes three or more.
        printer.setPageSize(1000, 110);
        assertTrue(printer.getPageCount() >= 3, printer.getPageCount() + " pages");
        assertEquals(hour, grid.getHourHeight(), 0.001f);

        // A sheet far taller than the screen stretches the hours to fill it,
        // on the page only: the view keeps the zoom it had.
        printer.setPageSize(1000, day * 4);
        assertEquals(1, printer.getPageCount());
        printer.renderPage(0);
        assertEquals(hour, grid.getHourHeight(), 0.001f, "printing does not zoom the screen");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                new SchedulePrinter(week()).renderPage(-1);
            }
        });
    }

    @FormTest
    void aMonthIsOnePageWithNoBandWhenThereIsNothingToSayInIt() {
        Scheduler s = week();
        s.setView(Scheduler.View.MONTH);
        ((ScrollSurface) s.surface()).layoutNow();
        SchedulePrinter printer = new SchedulePrinter(s);
        assertEquals(1, printer.getPageCount(), "a month fits the page it is given");
        printer.setTitle("");
        assertEquals(1, printer.getPageCount());
        Image whole = printer.renderImage(8000);
        assertEquals(printer.getPageWidth(), whole.getWidth());
        assertEquals(printer.getPageHeight(), whole.getHeight());
    }

    @FormTest
    void printingWhereThePlatformCannotIsReportedAndNotThrown() {
        Scheduler s = week();
        final PrintResult[] got = new PrintResult[1];
        new SchedulePrinter(s).print(new PrintResultListener() {
            @Override
            public void onResult(PrintResult result) {
                got[0] = result;
            }
        });
        if (!com.codename1.printing.Printer.isPrintingSupported()) {
            assertNotNull(got[0]);
            assertTrue(got[0].isFailed());
        }
        // and a null listener is allowed
        new SchedulePrinter(s).print(null);
    }

    /// The document's cross-reference table has to give the byte each object
    /// starts at, or a reader rebuilds the file or refuses it.
    @Test
    void thePdfCrossReferenceTablePointsAtEveryObject() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SchedulePrinter.PdfPages pdf = new SchedulePrinter.PdfPages(out, 2);
        byte[] first = new byte[300];
        byte[] second = new byte[17];
        for (int i = 0; i < first.length; i++) {
            first[i] = (byte) (i * 7);
        }
        pdf.page(first, 1200, 800);
        pdf.page(second, 600, 900);
        pdf.finish();
        byte[] bytes = out.toByteArray();
        String text = new String(bytes, "ISO-8859-1");
        assertTrue(text.startsWith("%PDF-1.4\n"));
        assertTrue(text.endsWith("%%EOF\n"));
        assertTrue(text.contains("/Count 2"));
        assertTrue(text.contains("/Kids [ 3 0 R 6 0 R ]"));
        // landscape fitted to the sheet's long side, portrait to its height
        assertTrue(text.contains("/MediaBox [0 0 842 561]"));
        assertTrue(text.contains("/MediaBox [0 0 561 842]"));
        assertTrue(text.contains("/Width 1200 /Height 800"));
        assertTrue(text.contains("/Filter /DCTDecode /Length 300"));

        int startxref = text.lastIndexOf("startxref\n");
        int xref = Integer.parseInt(text.substring(startxref + 10, text.indexOf('\n', startxref + 10)));
        assertTrue(text.startsWith("xref\n0 9\n", xref), "startxref points at the table");
        int row = xref + "xref\n0 9\n".length() + 20;
        for (int id = 1; id < 9; id++) {
            int offset = Integer.parseInt(text.substring(row, row + 10));
            assertTrue(text.startsWith(id + " 0 obj\n", offset), "object " + id + " is at " + offset);
            assertEquals(" 00000 n \n", text.substring(row + 10, row + 20));
            row += 20;
        }
        assertFalse(text.substring(row).startsWith("0"), "the table has a row per object and no more");
    }

    @Test
    void aPdfShortOfAPageIsNotFinished() throws Exception {
        final SchedulePrinter.PdfPages pdf = new SchedulePrinter.PdfPages(new ByteArrayOutputStream(), 2);
        pdf.page(new byte[4], 10, 10);
        assertThrows(java.io.IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                pdf.finish();
            }
        });
    }
}
