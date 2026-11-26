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
package com.codename1.presentation;

import com.codename1.impl.xml.XmlOut;
import java.io.IOException;

/// A DrawingML chart part (`ppt/charts/chartN.xml`) for the PPTX writer. Categories and
/// values are written as literal caches (`c:strLit`, `c:numLit`), so the part needs no
/// embedded workbook.
final class PptxChart {
    private PptxChart() {
    }

    static void write(XmlOut x, ChartElement c) throws IOException {
        x.raw("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\""
                + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
                + " xmlns:r=\"" + PptxWriter.NS_R + "\">"
                + "<c:roundedCorners val=\"0\"/><c:chart>");
        boolean titled = c.title != null && c.title.length() > 0;
        if (titled) {
            x.raw("<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>")
                    .text(c.title, false)
                    .raw("</a:t></a:r></a:p></c:rich></c:tx><c:overlay val=\"0\"/></c:title>");
        }
        x.raw("<c:autoTitleDeleted").attr("val", titled ? "0" : "1").raw("/>");
        x.raw("<c:plotArea><c:layout/>");
        String k = c.kind;
        boolean axes = true;
        if (ChartElement.PIE.equals(k) || ChartElement.DOUGHNUT.equals(k)) {
            axes = false;
            x.raw(ChartElement.PIE.equals(k) ? "<c:pieChart>" : "<c:doughnutChart>");
            x.raw("<c:varyColors val=\"1\"/>");
            series(x, c, false, false, false);
            x.raw("<c:firstSliceAng val=\"0\"/>");
            x.raw(ChartElement.PIE.equals(k) ? "</c:pieChart>"
                    : "<c:holeSize val=\"50\"/></c:doughnutChart>");
        } else if (ChartElement.LINE.equals(k)) {
            x.raw("<c:lineChart>").raw(grouping(c, "standard")).raw("<c:varyColors val=\"0\"/>");
            series(x, c, true, true, false);
            x.raw("<c:marker val=\"1\"/>").raw(axisIds()).raw("</c:lineChart>");
        } else if (ChartElement.AREA.equals(k)) {
            x.raw("<c:areaChart>").raw(grouping(c, "standard")).raw("<c:varyColors val=\"0\"/>");
            series(x, c, false, false, false);
            x.raw(axisIds()).raw("</c:areaChart>");
        } else if (ChartElement.SCATTER.equals(k)) {
            x.raw("<c:scatterChart><c:scatterStyle val=\"lineMarker\"/><c:varyColors val=\"0\"/>");
            series(x, c, true, true, true);
            x.raw(axisIds()).raw("</c:scatterChart>");
        } else if (ChartElement.RADAR.equals(k)) {
            x.raw("<c:radarChart><c:radarStyle val=\"marker\"/><c:varyColors val=\"0\"/>");
            series(x, c, true, false, false);
            x.raw(axisIds()).raw("</c:radarChart>");
        } else {
            boolean bar = ChartElement.BAR.equals(k);
            x.raw("<c:barChart><c:barDir").attr("val", bar ? "bar" : "col").raw("/>")
                    .raw(grouping(c, "clustered")).raw("<c:varyColors val=\"0\"/>");
            series(x, c, false, false, false);
            x.raw("<c:gapWidth val=\"150\"/>");
            if (c.stacked) {
                x.raw("<c:overlap val=\"100\"/>");
            }
            x.raw(axisIds()).raw("</c:barChart>");
        }
        if (axes) {
            boolean bar = ChartElement.BAR.equals(k);
            if (ChartElement.SCATTER.equals(k)) {
                axis(x, "c:valAx", 1, 2, "b", false);
            } else {
                axis(x, "c:catAx", 1, 2, bar ? "l" : "b", false);
            }
            axis(x, "c:valAx", 2, 1, bar ? "b" : "l", true);
        }
        x.raw("</c:plotArea>");
        if (c.legend) {
            x.raw("<c:legend><c:legendPos val=\"r\"/><c:overlay val=\"0\"/></c:legend>");
        }
        x.raw("<c:plotVisOnly val=\"1\"/><c:dispBlanksAs val=\"gap\"/></c:chart></c:chartSpace>");
    }

    private static String grouping(ChartElement c, String plain) {
        return "<c:grouping val=\"" + (c.stacked ? "stacked" : plain) + "\"/>";
    }

    private static String axisIds() {
        return "<c:axId val=\"1\"/><c:axId val=\"2\"/>";
    }

    private static void axis(XmlOut x, String tag, int id, int cross, String pos, boolean grid)
            throws IOException {
        x.raw("<").raw(tag).raw("><c:axId").attr("val", id).raw("/>")
                .raw("<c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/>")
                .raw("<c:axPos").attr("val", pos).raw("/>");
        if (grid) {
            x.raw("<c:majorGridlines/>");
        }
        x.raw("<c:crossAx").attr("val", cross).raw("/></").raw(tag).raw(">");
    }

    private static void series(XmlOut x, ChartElement c, boolean lineColor, boolean smooth,
            boolean scatter) throws IOException {
        boolean bar = ChartElement.COLUMN.equals(c.kind) || ChartElement.BAR.equals(c.kind)
                || c.kind == null;
        for (int i = 0; i < c.seriesCount(); i++) {
            x.raw("<c:ser><c:idx").attr("val", i).raw("/><c:order").attr("val", i)
                    .raw("/><c:tx><c:v>").text(name(c.seriesName(i), i), false)
                    .raw("</c:v></c:tx>");
            int col = c.seriesColor(i);
            if (col >= 0) {
                String clr = "<a:solidFill><a:srgbClr val=\"" + PptxWriter.hex(col)
                        + "\"/></a:solidFill>";
                x.raw("<c:spPr>");
                if (lineColor) {
                    x.raw("<a:ln w=\"28575\">").raw(clr).raw("</a:ln>");
                } else {
                    x.raw(clr);
                }
                x.raw("</c:spPr>");
            }
            if (bar) {
                x.raw("<c:invertIfNegative val=\"0\"/>");
            }
            double[] v = c.values(i);
            x.raw(scatter ? "<c:xVal>" : "<c:cat>");
            categories(x, c, scatter);
            x.raw(scatter ? "</c:xVal><c:yVal>" : "</c:cat><c:val>");
            x.raw("<c:numLit><c:formatCode>General</c:formatCode><c:ptCount")
                    .attr("val", v.length).raw("/>");
            for (int j = 0; j < v.length; j++) {
                if (!Double.isNaN(v[j]) && !Double.isInfinite(v[j])) {
                    x.raw("<c:pt").attr("idx", j).raw("><c:v>").raw(String.valueOf(v[j]))
                            .raw("</c:v></c:pt>");
                }
            }
            x.raw("</c:numLit>").raw(scatter ? "</c:yVal>" : "</c:val>");
            if (smooth) {
                x.raw("<c:smooth val=\"0\"/>");
            }
            x.raw("</c:ser>");
        }
    }

    private static String name(String n, int i) {
        return n == null ? "Series " + (i + 1) : n;
    }

    private static void categories(XmlOut x, ChartElement c, boolean numeric) throws IOException {
        int n = c.categoryCount();
        double[] nums = numeric ? numbers(c) : null;
        if (nums != null) {
            x.raw("<c:numLit><c:formatCode>General</c:formatCode><c:ptCount").attr("val", n)
                    .raw("/>");
            for (int j = 0; j < n; j++) {
                x.raw("<c:pt").attr("idx", j).raw("><c:v>").raw(String.valueOf(nums[j]))
                        .raw("</c:v></c:pt>");
            }
            x.raw("</c:numLit>");
            return;
        }
        x.raw("<c:strLit><c:ptCount").attr("val", n).raw("/>");
        for (int j = 0; j < n; j++) {
            String s = c.category(j);
            x.raw("<c:pt").attr("idx", j).raw("><c:v>").text(s == null ? "" : s, false)
                    .raw("</c:v></c:pt>");
        }
        x.raw("</c:strLit>");
    }

    private static double[] numbers(ChartElement c) {
        int n = c.categoryCount();
        double[] d = new double[n];
        for (int j = 0; j < n; j++) {
            String s = c.category(j);
            if (s == null || s.length() == 0 || !isNumber(s)) {
                return null;
            }
            d[j] = Double.parseDouble(s.trim());
        }
        return d;
    }

    private static boolean isNumber(String s) {
        String t = s.trim();
        int digits = 0;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits++;
            } else if (ch != '.' && !(i == 0 && ch == '-')) {
                return false;
            }
        }
        return digits > 0 && t.indexOf('.') == t.lastIndexOf('.');
    }
}
