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

import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Table;

import java.util.ArrayList;
import java.util.List;

/// Charts and tables for the PowerPoint reader.
final class PptxChartReader {
    private static final int MAX_SERIES = 256;
    private static final int MAX_POINTS = 10000;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COLUMNS = 64;

    private static final String[] KINDS = {"barChart", "bar3DChart", "lineChart",
        "line3DChart", "areaChart", "area3DChart", "pieChart", "pie3DChart", "ofPieChart",
        "doughnutChart", "scatterChart", "bubbleChart", "radarChart"};

    private PptxChartReader() {
    }

    /// A chart from a chart part's root.
    static ChartElement chart(PptxNode space, String id, String name, float x, float y, float w,
            float h, PptxReader r) {
        PptxNode chart = space.child("chart");
        PptxNode plot = chart == null ? null : chart.child("plotArea");
        if (plot == null) {
            r.warn("A chart with no plot area was left out");
            return null;
        }
        PptxNode type = null;
        String tname = null;
        for (int i = 0; i < plot.children.size() && type == null; i++) {
            PptxNode c = plot.children.get(i);
            for (String item : KINDS) {
                if (item.equals(c.name)) {
                    type = c;
                    tname = c.name;
                    break;
                }
            }
        }
        if (type == null) {
            r.warn("A chart of a kind that cannot be converted was left out");
            return null;
        }
        String kind;
        boolean stacked = false;
        if (tname.startsWith("bar")) {
            PptxNode dir = type.child("barDir");
            kind = dir != null && "bar".equals(dir.attr("val")) ? ChartElement.BAR
                    : ChartElement.COLUMN;
        } else if (tname.startsWith("line")) {
            kind = ChartElement.LINE;
        } else if (tname.startsWith("area")) {
            kind = ChartElement.AREA;
        } else if (tname.startsWith("doughnut")) {
            kind = ChartElement.DOUGHNUT;
        } else if (tname.startsWith("scatter") || tname.startsWith("bubble")) {
            kind = ChartElement.SCATTER;
        } else if (tname.startsWith("radar")) {
            kind = ChartElement.RADAR;
        } else {
            kind = ChartElement.PIE;
        }
        if (tname.indexOf("3D") >= 0 || "ofPieChart".equals(tname) || "bubbleChart".equals(tname)) {
            r.warn("A " + tname + " was read as a flat " + kind + " chart");
        }
        PptxNode grouping = type.child("grouping");
        if (grouping != null) {
            String g = grouping.attr("val");
            stacked = "stacked".equals(g) || "percentStacked".equals(g);
            if ("percentStacked".equals(g)) {
                r.warn("A 100% stacked chart was read as a stacked chart of its values");
            }
        }
        List<PptxNode> sers = type.all("ser");
        int n = Math.min(MAX_SERIES, sers.size());
        String[] names = new String[n];
        double[][] values = new double[n][];
        int[] colors = new int[n];
        boolean anyColor = false;
        String[] cats = new String[0];
        for (int i = 0; i < n; i++) {
            PptxNode s = sers.get(i);
            PptxNode tx = s.child("tx");
            names[i] = tx == null ? null : tx.allText();
            PptxNode val = s.child("val");
            if (val == null) {
                val = s.child("yVal");
            }
            values[i] = numbers(val);
            PptxNode cat = s.child("cat");
            if (cat == null) {
                cat = s.child("xVal");
            }
            if (i == 0 && cat != null) {
                cats = strings(cat, r);
            }
            long c = PptxDrawing.color(s.path("spPr", "solidFill"), r, PptxDrawing.NONE);
            if (c != PptxDrawing.NONE) {
                colors[i] = PptxDrawing.rgb(c);
                anyColor = true;
            } else {
                colors[i] = r.theme.color(Theme.ACCENT1 + i % 6);
            }
        }
        String title = null;
        PptxNode t = chart.child("title");
        if (t != null) {
            String s = t.allText();
            title = s.length() == 0 ? null : s;
        }
        return new ChartElement(id, name, kind, x, y, w, h, 0, title, stacked,
                chart.child("legend") != null, cats, names, values, anyColor ? colors : null);
    }

    private static PptxNode cache(PptxNode ref) {
        if (ref == null) {
            return null;
        }
        String[] kinds = {"numRef", "strRef", "multiLvlStrRef"};
        for (String item : kinds) {
            PptxNode c = ref.child(item);
            if (c != null) {
                PptxNode cc = c.child("numCache");
                if (cc == null) {
                    cc = c.child("strCache");
                }
                if (cc == null) {
                    cc = c.child("multiLvlStrCache");
                }
                return cc;
            }
        }
        PptxNode lit = ref.child("numLit");
        return lit != null ? lit : ref.child("strLit");
    }

    private static int count(PptxNode cache) {
        PptxNode pc = cache.child("ptCount");
        int n = pc == null ? -1 : (int) Math.max(-1, Math.min(MAX_POINTS, pc.num("val", -1)));
        if (n < 0) {
            n = 0;
            List<PptxNode> pts = cache.all("pt");
            for (PptxNode item : pts) {
                n = (int) Math.max(n, Math.min(MAX_POINTS, item.num("idx", 0) + 1));
            }
        }
        return n;
    }

    private static double[] numbers(PptxNode ref) {
        PptxNode cache = cache(ref);
        if (cache == null) {
            return new double[0];
        }
        double[] out = new double[count(cache)];
        List<PptxNode> pts = cache.all("pt");
        for (PptxNode p : pts) {
            int idx = (int) p.num("idx", -1);
            if (idx >= 0 && idx < out.length) {
                PptxNode v = p.child("v");
                out[idx] = v == null ? 0 : PptxNode.number(v.text(), 0);
            }
        }
        return out;
    }

    private static String[] strings(PptxNode ref, PptxReader r) {
        PptxNode cache = cache(ref);
        if (cache == null) {
            return new String[0];
        }
        if ("multiLvlStrCache".equals(cache.name)) {
            r.warn("Multi-level chart categories were read as their first level");
            PptxNode lvl = cache.child("lvl");
            if (lvl == null) {
                return new String[0];
            }
            String[] out = new String[count(cache)];
            fill(lvl, out);
            return out;
        }
        String[] out = new String[count(cache)];
        fill(cache, out);
        return out;
    }

    private static void fill(PptxNode holder, String[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = "";
        }
        List<PptxNode> pts = holder.all("pt");
        for (PptxNode p : pts) {
            int idx = (int) p.num("idx", -1);
            if (idx >= 0 && idx < out.length) {
                PptxNode v = p.child("v");
                out[idx] = v == null ? "" : v.text();
            }
        }
    }

    /// A table from an `a:tbl`, or null.
    static Table table(PptxNode tbl, PptxReader r, PptxReader.Part part,
            PptxDrawing.Cascade cascade) {
        List<Float> widths = new ArrayList<Float>();
        PptxNode grid = tbl.child("tblGrid");
        if (grid != null) {
            List<PptxNode> cols = grid.all("gridCol");
            for (int i = 0; i < cols.size() && i < MAX_COLUMNS; i++) {
                widths.add(Float.valueOf(Math.max(1, Math.min(10000,
                        PptxDrawing.pt(cols.get(i).num("w", 914400))))));
            }
        }
        List<PptxNode> trs = tbl.all("tr");
        if (trs.size() > MAX_ROWS) {
            r.warn("Rows past " + MAX_ROWS + " of a table were left out");
        }
        int rows = Math.min(MAX_ROWS, trs.size());
        int cols = widths.size();
        for (int i = 0; i < rows; i++) {
            cols = Math.max(cols, Math.min(MAX_COLUMNS, trs.get(i).all("tc").size()));
        }
        if (rows == 0 || cols == 0) {
            r.warn("An empty table was left out");
            return null;
        }
        List<List<List<Block>>> cells = new ArrayList<List<List<Block>>>();
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        boolean[][] taken = new boolean[rows][cols];
        for (int i = 0; i < rows; i++) {
            List<List<Block>> row = new ArrayList<List<Block>>();
            List<PptxNode> tcs = trs.get(i).all("tc");
            for (int c = 0; c < cols; c++) {
                if (c >= tcs.size()) {
                    row.add(new ArrayList<Block>());
                    continue;
                }
                PptxNode tc = tcs.get(c);
                row.add(PptxDrawing.blocks(tc.child("txBody"), cascade, r, part));
                if (tc.flag("hMerge", false) || tc.flag("vMerge", false)) {
                    continue;
                }
                int span = (int) Math.max(1, Math.min(cols - c, tc.num("gridSpan", 1)));
                int down = (int) Math.max(1, Math.min(rows - i, tc.num("rowSpan", 1)));
                if (span > 1 || down > 1) {
                    boolean free = true;
                    for (int a = i; a < i + down && free; a++) {
                        for (int b = c; b < c + span; b++) {
                            if (taken[a][b]) {
                                free = false;
                                break;
                            }
                        }
                    }
                    if (free) {
                        for (int a = i; a < i + down; a++) {
                            for (int b = c; b < c + span; b++) {
                                taken[a][b] = true;
                            }
                        }
                        merges.add(new Table.Merge(i, c, down, span));
                    } else {
                        r.warn("Overlapping merged table cells were split");
                    }
                }
            }
            cells.add(row);
        }
        PptxNode pr = tbl.child("tblPr");
        int header = pr != null && pr.flag("firstRow", false) ? 1 : 0;
        Table t = new Table(r.nextId('b'), cells, merges, header);
        if (widths.size() == cols) {
            float[] w = new float[cols];
            for (int i = 0; i < cols; i++) {
                w[i] = widths.get(i).floatValue();
            }
            t = t.withColumnWidths(w);
        }
        return t;
    }
}
