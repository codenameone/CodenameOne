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

import com.codename1.io.FileSystemStorage;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
import com.codename1.printing.Printer;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/// Prints a `Scheduler` or a `Gantt` chart, or exports it as images or a PDF.
///
/// What is on screen is a window onto the view: a week scrolls under its
/// header, a project runs off the right edge. A printer lays the whole of it
/// out on pages instead. The view is painted off screen at the page's size,
/// one page of content at a time, and every page gets the headers that
/// belong to its part: the days or the time scale across the top, the hours,
/// the resources or the task table down the side.
///
/// ```java
/// SchedulePrinter printer = new SchedulePrinter(gantt);
/// printer.setTitle("Release plan");
/// printer.print(new PrintResultListener() {
///     public void onResult(PrintResult result) {
///         if (result.isFailed()) {
///             ToastBar.showErrorMessage(result.getError());
///         }
///     }
/// });
/// ```
///
/// Pages run left to right, then top to bottom. `getPageCount`, `renderPage`
/// and `renderImage` give the pages to an application that would sooner
/// share or store them, and `writePdf` writes them as one document.
///
/// A scheduler prints the view and the dates it is showing; a Gantt chart
/// prints the whole project at the zoom it is showing, so zooming out before
/// printing is how a long project is fitted to fewer pages. The resource
/// load under a chart is not printed.
///
/// A page is an image as large as the page, so rendering is done a page at a
/// time and the pages are not kept. Everything here runs on the EDT.
public final class SchedulePrinter {
    /// The most pages one print makes. A chart zoomed to hours over a year
    /// is thousands of pages nobody asked for.
    private static final int MAX_PAGES = 400;

    private final Scheduler scheduler;
    private final Gantt gantt;
    private int pageWidth;
    private int pageHeight;
    private String title;
    private boolean titleSet;
    private boolean pageNumbers = true;

    /// A printer of the view and the dates `scheduler` is showing.
    public SchedulePrinter(Scheduler scheduler) {
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler");
        }
        this.scheduler = scheduler;
        this.gantt = null;
        defaultPage();
    }

    /// A printer of the whole of the project `gantt` shows.
    public SchedulePrinter(Gantt gantt) {
        if (gantt == null) {
            throw new IllegalArgumentException("gantt");
        }
        this.scheduler = null;
        this.gantt = gantt;
        defaultPage();
    }

    // A landscape sheet about 250 mm of screen wide: text the size it is on
    // screen stays readable on paper, and a page stays a few megabytes.
    private void defaultPage() {
        int w = Display.getInstance().convertToPixels(250f);
        pageWidth = Math.max(1000, Math.min(2400, w));
        pageHeight = pageWidth * 210 / 297;
    }

    /// Sets the size of a page in pixels. The default is a landscape sheet
    /// in the proportions of A4. Text and rows keep the size they have on
    /// screen, so a larger page holds more of the view and prints it
    /// smaller.
    public void setPageSize(int widthPixels, int heightPixels) {
        if (widthPixels < 100 || heightPixels < 100) {
            throw new IllegalArgumentException("page size");
        }
        pageWidth = widthPixels;
        pageHeight = heightPixels;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    /// The line at the top of every page. Unless one is set, a scheduler
    /// prints the range its toolbar names and a chart prints none.
    public String getTitle() {
        if (titleSet) {
            return title;
        }
        return scheduler != null ? scheduler.titleText() : null;
    }

    /// Sets the line at the top of every page; null or empty prints none.
    public void setTitle(String title) {
        this.title = title;
        titleSet = true;
    }

    /// Whether every page says which page of how many it is. On by default;
    /// a single page is never numbered.
    public boolean isPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(boolean pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    private ScrollSurface surface() {
        Component c = scheduler != null ? scheduler.surface() : gantt.surface();
        return c instanceof ScrollSurface ? (ScrollSurface) c : null;
    }

    private static Style headingStyle() {
        return Look.style("SchedulerTitle");
    }

    // The height of the band the title and the page number are printed in.
    private int bandHeight(int pages) {
        String t = getTitle();
        boolean hasTitle = t != null && t.length() > 0;
        if (!hasTitle && !(pageNumbers && pages > 1)) {
            return 0;
        }
        return headingStyle().getFont().getHeight() + Look.mm(2);
    }

    // Columns and rows of pages, and the content one page holds. The band
    // depends on whether pages are numbered, which depends on the count, so
    // the count is taken with a band and taken again without one only when a
    // single untitled page turns out not to need it.
    private int[] grid() {
        ScrollSurface s = surface();
        if (s == null) {
            return new int[]{0, 0, 0, 0, 0};
        }
        int band = bandHeight(2);
        int[] g = s.pageGrid(pageWidth, Math.max(1, pageHeight - band));
        if (band > 0 && g[0] * g[1] == 1 && bandHeight(1) == 0) {
            band = 0;
            g = s.pageGrid(pageWidth, pageHeight);
        }
        return new int[]{g[0], g[1], g[2], g[3], band};
    }

    /// The number of pages the view takes at this page size.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: if it is more than a print can sensibly
    ///   be; zoom the view out or set a larger page
    public int getPageCount() {
        int[] g = grid();
        long n = (long) g[0] * g[1];
        if (n > MAX_PAGES) {
            throw new IllegalStateException("The view is " + n + " pages; zoom out or use a larger page");
        }
        return (int) n;
    }

    /// Renders one page, counted from 0, left to right and then top to
    /// bottom.
    public Image renderPage(int page) {
        int[] g = grid();
        int count = getPageCount();
        if (page < 0 || page >= count) {
            throw new IllegalArgumentException("page " + page + " of " + count);
        }
        Style paper = Look.style(scheduler != null ? "Scheduler" : "Gantt");
        Image img = Image.createImage(pageWidth, pageHeight, 0xff000000 | paper.getBgColor());
        Graphics gr = img.getGraphics();
        int band = g[4];
        if (band > 0) {
            Style st = headingStyle();
            Font f = st.getFont();
            gr.setFont(f);
            gr.setColor(st.getFgColor());
            int ty = (band - f.getHeight()) / 2;
            String number = pageNumbers && count > 1 ? (page + 1) + " / " + count : null;
            int numberW = number == null ? 0 : f.stringWidth(number) + Look.mm(2);
            String t = getTitle();
            if (t != null && t.length() > 0) {
                gr.drawString(Look.fit(t, f, pageWidth - numberW - Look.mm(2)), Look.mm(1), ty);
            }
            if (number != null) {
                gr.drawString(number, pageWidth - numberW, ty);
            }
        }
        int col = page % g[0];
        int row = page / g[0];
        surface().paintPage(gr, 0, band, pageWidth, pageHeight - band, col * g[2], row * g[3]);
        return img;
    }

    /// Renders the whole view as one image, its headers once: the picture
    /// to share or to put in a report. It is as large as the content, so a
    /// long project at a close zoom is a very large image; `renderPage` is
    /// the one for those.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: if either side would pass `maxSide` pixels
    public Image renderImage(int maxSide) {
        ScrollSurface s = surface();
        if (s == null) {
            throw new IllegalStateException("nothing to render");
        }
        // Lay out at the page's size to learn how much content there is,
        // then at the size that holds all of it.
        int[] g = s.pageGrid(pageWidth, pageHeight);
        int w = pageWidth - g[2] + g[0] * g[2];
        int h = pageHeight - g[3] + g[1] * g[3];
        if (w > maxSide || h > maxSide) {
            throw new IllegalStateException("The view is " + w + " by " + h + " pixels; zoom out or render it in pages");
        }
        Style paper = Look.style(scheduler != null ? "Scheduler" : "Gantt");
        Image img = Image.createImage(w, h, 0xff000000 | paper.getBgColor());
        s.paintPage(img.getGraphics(), 0, 0, w, h, 0, 0);
        return img;
    }

    /// Whether `writePdf` works here: it needs the platform to encode JPEG.
    public static boolean isPdfSupported() {
        ImageIO io = ImageIO.getImageIO();
        return io != null && io.isFormatSupported(ImageIO.FORMAT_JPEG);
    }

    /// Writes every page into one PDF document at `path`, a path of
    /// `FileSystemStorage`. Each page of the document is a picture of the
    /// page, so its text cannot be selected or searched.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the file cannot be written, or the platform
    ///   cannot encode JPEG (see `isPdfSupported`)
    public void writePdf(String path) throws IOException {
        ImageIO io = ImageIO.getImageIO();
        if (io == null || !io.isFormatSupported(ImageIO.FORMAT_JPEG)) {
            throw new IOException("JPEG encoding is not available on this platform");
        }
        int count = getPageCount();
        OutputStream out = FileSystemStorage.getInstance().openOutputStream(path);
        try {
            PdfPages pdf = new PdfPages(out, count);
            for (int i = 0; i < count; i++) {
                Image page = renderPage(i);
                ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
                io.save(page, jpeg, ImageIO.FORMAT_JPEG, 0.92f);
                pdf.page(jpeg.toByteArray(), page.getWidth(), page.getHeight());
            }
            pdf.finish();
        } finally {
            out.close();
        }
    }

    /// Prints every page through the platform's print dialog.
    ///
    /// The pages go to the printer as one PDF document. On a platform that
    /// cannot write one, a single page is printed as an image, and more
    /// than one is reported as a failure, since the platform prints one
    /// image at a time and a dialog per page is not a print.
    ///
    /// #### Parameters
    ///
    /// - `listener`: told how it ended, on the EDT; may be null
    public void print(final PrintResultListener listener) {
        if (!Printer.isPrintingSupported()) {
            report(listener, PrintResult.failed("Printing is not supported on this platform"));
            return;
        }
        int count;
        try {
            count = getPageCount();
        } catch (IllegalStateException tooMany) {
            report(listener, PrintResult.failed(tooMany.getMessage()));
            return;
        }
        if (count == 0) {
            report(listener, PrintResult.failed("Nothing to print"));
            return;
        }
        if (!isPdfSupported()) {
            if (count == 1) {
                Printer.printImage(renderPage(0), listener);
            } else {
                report(listener, PrintResult.failed("This platform prints one page at a time and the view is "
                        + count + " pages; zoom out or use a larger page"));
            }
            return;
        }
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        final String path = fs.getAppHomePath() + "cn1-schedule-print-temp.pdf";
        try {
            writePdf(path);
        } catch (IOException err) {
            report(listener, PrintResult.failed(err.toString()));
            return;
        }
        Printer.printPDF(path, new PrintResultListener() {
            @Override
            public void onResult(PrintResult result) {
                fs.delete(path);
                report(listener, result);
            }
        });
    }

    private static void report(PrintResultListener listener, PrintResult result) {
        if (listener != null) {
            listener.onResult(result);
        }
    }

    /// The smallest PDF that holds a JPEG per page: a catalog, the page
    /// tree, and for each page the page, a content stream that draws its
    /// image over the whole of it, and the image. Objects are written as
    /// they come, so a document of many pages holds one page in memory.
    static final class PdfPages {
        // A landscape A4 sheet in points; a page of other proportions is
        // fitted to the long side.
        private static final int SHEET = 842;
        private final OutputStream out;
        private final int count;
        private final int[] offsets;
        private int written;
        private int position;

        PdfPages(OutputStream out, int count) throws IOException {
            this.out = out;
            this.count = count;
            // 1 catalog, 2 page tree, then page, content and image per page
            offsets = new int[3 + count * 3];
            text("%PDF-1.4\n");
            object(1, "<< /Type /Catalog /Pages 2 0 R >>");
            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < count; i++) {
                kids.append(3 + i * 3).append(" 0 R ");
            }
            object(2, "<< /Type /Pages /Count " + count + " /Kids [ " + kids + "] >>");
        }

        void page(byte[] jpeg, int width, int height) throws IOException {
            int id = 3 + written * 3;
            int w;
            int h;
            if (width >= height) {
                w = SHEET;
                h = Math.max(1, SHEET * height / width);
            } else {
                h = SHEET;
                w = Math.max(1, SHEET * width / height);
            }
            object(id, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + w + " " + h + "] /Resources << /XObject << /Im0 "
                    + (id + 2) + " 0 R >> >> /Contents " + (id + 1) + " 0 R >>");
            String draw = "q " + w + " 0 0 " + h + " 0 0 cm /Im0 Do Q\n";
            offsets[id + 1] = position;
            text((id + 1) + " 0 obj\n<< /Length " + draw.length() + " >>\nstream\n" + draw + "endstream\nendobj\n");
            offsets[id + 2] = position;
            text((id + 2) + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + width + " /Height " + height
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + jpeg.length
                    + " >>\nstream\n");
            out.write(jpeg);
            position += jpeg.length;
            text("\nendstream\nendobj\n");
            written++;
        }

        void finish() throws IOException {
            if (written != count) {
                throw new IOException("PDF has " + written + " of " + count + " pages");
            }
            int xref = position;
            StringBuilder b = new StringBuilder();
            b.append("xref\n0 ").append(offsets.length).append("\n0000000000 65535 f \n");
            for (int i = 1; i < offsets.length; i++) {
                String n = String.valueOf(offsets[i]);
                for (int pad = n.length(); pad < 10; pad++) {
                    b.append('0');
                }
                b.append(n).append(" 00000 n \n");
            }
            b.append("trailer\n<< /Size ").append(offsets.length).append(" /Root 1 0 R >>\nstartxref\n")
                    .append(xref).append("\n%%EOF\n");
            text(b.toString());
            out.flush();
        }

        private void object(int id, String body) throws IOException {
            offsets[id] = position;
            text(id + " 0 obj\n" + body + "\nendobj\n");
        }

        // Everything but the image data is ASCII, so a character is a byte.
        private void text(String s) throws IOException {
            int n = s.length();
            byte[] b = new byte[n];
            for (int i = 0; i < n; i++) {
                b[i] = (byte) s.charAt(i);
            }
            out.write(b);
            position += n;
        }
    }
}
