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
package com.codenameone.developerguide.pdf;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfFont;
import com.codename1.pdf.PdfImage;
import com.codename1.pdf.PdfLayout;
import com.codename1.pdf.PdfPage;
import com.codename1.pdf.PdfPageSize;
import com.codename1.pdf.PdfParagraph;
import com.codename1.pdf.PdfReader;
import com.codename1.pdf.PdfTable;
import com.codename1.pdf.ui.PdfViewer;
import com.codename1.printing.Printer;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfSnippets {

    public byte[] canvas() throws IOException {
        // tag::pdf-canvas[]
        PdfDocument doc = new PdfDocument().setTitle("Ticket");
        PdfPage page = doc.addPage(PdfPageSize.A5);
        PdfCanvas c = page.getCanvas();
        c.setFillColor(0x1565c0).fillRect(0, 0, page.getWidth(), 60);
        c.setFillColor(0xffffff).setFont(PdfFont.HELVETICA_BOLD, 20).drawText("Boarding pass", 16, 38);
        c.setStrokeColor(0x9e9e9e).setDash(new float[] {4, 3}, 0).drawLine(16, 120, page.getWidth() - 16, 120);
        c.setFillColor(0x212121).setFont(PdfFont.HELVETICA, 12).drawText("Gate B12, seat 14C", 16, 150);
        byte[] pdf = doc.toByteArray();
        // end::pdf-canvas[]
        return pdf;
    }

    public void layout(byte[] logoPng, byte[] fontTtf) throws IOException {
        // tag::pdf-layout[]
        PdfFont body = PdfFont.loadTrueType(fontTtf);
        PdfDocument doc = new PdfDocument().setTitle("Invoice 1042");
        PdfLayout layout = new PdfLayout(doc, PdfPageSize.A4, 48);
        layout.setPageDecorator(new PdfLayout.PageDecorator() {
            public void decorate(PdfPage page, int number, int count) {
                page.getCanvas().setFont(PdfFont.HELVETICA, 9).setFillColor(0x757575)
                        .drawText("Page " + number + " of " + count, 48, page.getHeight() - 24);
            }
        });
        layout.addImage(PdfImage.create(logoPng), 120, PdfParagraph.ALIGN_LEFT);
        layout.add(new PdfParagraph("Invoice 1042", PdfFont.HELVETICA_BOLD, 22).setSpaceAfter(12));
        layout.add(new PdfParagraph("Thank you for your order. Payment is due within ", body, 11)
                .append("30 days", PdfFont.HELVETICA_BOLD, 11)
                .append(". Questions? ", body, 11)
                .appendLink("Contact us", "mailto:billing@example.com", body, 11, 0x1565c0)
                .setAlignment(PdfParagraph.ALIGN_JUSTIFY));
        PdfTable items = new PdfTable(4, 1, 1.5f).setHeaderRows(1).setFont(body, 10).setBorder(0.5f, 0xbdbdbd);
        items.addCell("Item").addCell("Qty").addCell("Price");
        for (int i = 1; i <= 60; i++) {
            items.addCell("Widget " + i).addCell("1").addCell("$9.90");
        }
        layout.add(items);
        layout.finish();
        OutputStream out = FileSystemStorage.getInstance().openOutputStream(invoicePath());
        try {
            doc.save(out);
        } finally {
            Util.cleanup(out);
        }
        // end::pdf-layout[]
    }

    public void rightToLeft(PdfLayout layout, PdfFont hebrewAndArabic) {
        // tag::pdf-rtl[]
        layout.add(new PdfParagraph("\u05e9\u05dc\u05d5\u05dd \u05e2\u05d5\u05dc\u05dd (Hello world)",
                hebrewAndArabic, 14));
        layout.add(new PdfParagraph("\u0645\u0631\u062d\u0628\u0627 \u0628\u0627\u0644\u0639\u0627\u0644\u0645",
                hebrewAndArabic, 14).setAlignment(PdfParagraph.ALIGN_JUSTIFY));
        // end::pdf-rtl[]
    }

    public void read(InputStream in) throws IOException {
        // tag::pdf-read[]
        PdfReader reader = PdfReader.open(in);
        Log.p(reader.getTitle() + ": " + reader.getPageCount() + " pages");
        String firstPage = reader.extractText(0);
        // end::pdf-read[]
        Log.p(firstPage);
    }

    public void view(Form form, byte[] pdf) throws IOException {
        // tag::pdf-viewer[]
        PdfViewer viewer = new PdfViewer(pdf);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, viewer);
        form.show();
        viewer.setPage(2);
        // end::pdf-viewer[]
    }

    public void print() {
        // tag::pdf-print[]
        if (Printer.isPrintingSupported()) {
            Printer.printPDF(invoicePath(), null);
        }
        // end::pdf-print[]
    }

    private static String invoicePath() {
        return FileSystemStorage.getInstance().getAppHomePath() + "invoice-1042.pdf";
    }
}
