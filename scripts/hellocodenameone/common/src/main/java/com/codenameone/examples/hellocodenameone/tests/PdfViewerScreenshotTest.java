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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfFont;
import com.codename1.pdf.PdfLayout;
import com.codename1.pdf.PdfPage;
import com.codename1.pdf.PdfPageSize;
import com.codename1.pdf.PdfParagraph;
import com.codename1.pdf.PdfTable;
import com.codename1.pdf.ui.PdfViewer;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// A generated invoice in a PdfViewer: text in the standard fonts, a
/// justified paragraph, a table with a header row, a dashed rule and
/// translucent shapes, drawn by the pure-Java renderer.
public class PdfViewerScreenshotTest extends DualAppearanceBaseTest {
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=PdfViewer status=SKIPPED reason=pdf-viewer-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "PdfViewer";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        try {
            form.add(BorderLayout.CENTER, new PdfViewer(invoice()));
        } catch (Exception e) {
            form.add(BorderLayout.CENTER, new Label("PDF failed: " + e));
        }
    }

    private static byte[] invoice() throws java.io.IOException {
        PdfDocument doc = new PdfDocument().setTitle("Invoice 1042");
        PdfLayout layout = new PdfLayout(doc, PdfPageSize.A5, 36);
        PdfPage first = layout.getCurrentPage();
        PdfCanvas c = first.getCanvas();
        c.setFillColor(0x1565c0).fillRect(0, 0, first.getWidth(), 56);
        c.setFillColor(0xffffff).setFont(PdfFont.HELVETICA_BOLD, 18).drawText("Invoice 1042", 36, 36);
        c.setFillColor(0xff9800).setFillAlpha(0.5f).ellipse(first.getWidth() - 90, 8, 70, 70).fill();
        c.setFillAlpha(1f);
        layout.addSpace(50);
        layout.add(new PdfParagraph("Thank you for your order. Payment is due within ", PdfFont.TIMES_ROMAN, 11)
                .append("30 days", PdfFont.TIMES_BOLD, 11)
                .append(" of the invoice date. Late payments carry a fee as described in the terms"
                        + " of service you accepted when you placed the order.", PdfFont.TIMES_ROMAN, 11)
                .setAlignment(PdfParagraph.ALIGN_JUSTIFY).setSpaceAfter(10));
        PdfTable t = new PdfTable(4, 1, 1.5f).setHeaderRows(1).setFont(PdfFont.HELVETICA, 10)
                .setBorder(0.5f, 0xbdbdbd).setCellPadding(4);
        t.addCell(new PdfParagraph("Item", PdfFont.HELVETICA_BOLD, 10), 1, 0xe3f2fd);
        t.addCell(new PdfParagraph("Qty", PdfFont.HELVETICA_BOLD, 10), 1, 0xe3f2fd);
        t.addCell(new PdfParagraph("Price", PdfFont.HELVETICA_BOLD, 10), 1, 0xe3f2fd);
        String[] items = {"Espresso machine", "Grinder", "Milk jug", "Descaler", "Cups (6)"};
        String[] prices = {"$499.00", "$129.00", "$19.90", "$9.90", "$34.00"};
        for (int i = 0; i < items.length; i++) {
            t.addCell(items[i]).addCell("1").addCell(prices[i]);
        }
        layout.add(t);
        float y = layout.getY() + 8;
        PdfCanvas cc = layout.getCurrentPage().getCanvas();
        cc.setStrokeColor(0x757575).setLineWidth(1).setDash(new float[] {4, 3}, 0).drawLine(36, y, 384, y);
        layout.addSpace(16);
        layout.add(new PdfParagraph("Total $691.80", PdfFont.HELVETICA_BOLD, 13)
                .setAlignment(PdfParagraph.ALIGN_RIGHT));
        layout.finish();
        return doc.toByteArray();
    }
}
