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
package com.codename1.spreadsheet;

import com.codename1.io.zip.ZipArchiveOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/// Writes a workbook as an OpenDocument spreadsheet.
final class OdsWriter {
    private static final String MIME = "application/vnd.oasis.opendocument.spreadsheet";
    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String NAMESPACES = " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
            + " xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\""
            + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\""
            + " xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\""
            + " xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\""
            + " xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\""
            + " xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\""
            + " xmlns:of=\"urn:oasis:names:tc:opendocument:xmlns:of:1.2\""
            + " xmlns:calcext=\"urn:org:documentfoundation:names:experimental:calc:xmlns:calcext:1.0\""
            + " office:version=\"1.3\"";

    /// The width OpenDocument gives a column that a spreadsheet counts as one character wide.
    /// Excel counts a column in characters of the default font and OpenDocument measures it;
    /// this is the ratio LibreOffice converts by, so that a width survives the trip through
    /// either program.
    static final double MM_PER_CHARACTER = 2.47;

    private final Workbook book;
    /// The merged regions of the sheet being written, by their first cell.
    private final java.util.HashMap<Long, int[]> anchors = new java.util.HashMap<Long, int[]>();

    private static Long key(int row, int column) {
        return Long.valueOf(((long) row << 20) | column);
    }

    OdsWriter(Workbook book) {
        this.book = book;
    }

    void write(OutputStream out) throws IOException {
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        // The type comes first and uncompressed, so that a program can tell what the file is
        // from its first bytes without knowing zip.
        byte[] mime = MIME.getBytes("UTF-8");
        ZipEntry first = new ZipEntry("mimetype");
        first.setMethod(ZipArchiveOutputStream.STORED);
        first.setSize(mime.length);
        first.setCompressedSize(mime.length);
        CRC32 crc = new CRC32();
        crc.update(mime, 0, mime.length);
        first.setCrc(crc.getValue());
        zip.putNextEntry(first);
        zip.write(mime, 0, mime.length);
        zip.closeEntry();

        XmlOut x = part(zip, "META-INF/manifest.xml");
        x.raw(HEAD).raw("<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\"")
                .raw(" manifest:version=\"1.3\">")
                .raw("<manifest:file-entry manifest:full-path=\"/\" manifest:version=\"1.3\" manifest:media-type=\"")
                .raw(MIME).raw("\"/>")
                .raw("<manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/>")
                .raw("<manifest:file-entry manifest:full-path=\"styles.xml\" manifest:media-type=\"text/xml\"/>")
                .raw("<manifest:file-entry manifest:full-path=\"settings.xml\" manifest:media-type=\"text/xml\"/>")
                .raw("</manifest:manifest>");
        end(zip, x);

        x = part(zip, "styles.xml");
        x.raw(HEAD).raw("<office:document-styles").raw(NAMESPACES).raw("/>");
        end(zip, x);

        x = part(zip, "settings.xml");
        writeSettings(x);
        end(zip, x);

        x = part(zip, "content.xml");
        writeContent(x);
        end(zip, x);
        zip.finish();
    }

    private static XmlOut part(ZipArchiveOutputStream zip, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        return new XmlOut(zip);
    }

    private static void end(ZipArchiveOutputStream zip, XmlOut x) throws IOException {
        x.flush();
        zip.closeEntry();
    }

    private static void item(XmlOut x, String name, String type, int value) throws IOException {
        x.raw("<config:config-item").attr("config:name", name).attr("config:type", type).raw(">").raw(value)
                .raw("</config:config-item>");
    }

    private void writeSettings(XmlOut x) throws IOException {
        x.raw(HEAD).raw("<office:document-settings").raw(NAMESPACES).raw("><office:settings>")
                .raw("<config:config-item-set config:name=\"ooo:view-settings\">")
                .raw("<config:config-item-map-indexed config:name=\"Views\"><config:config-item-map-entry>")
                .raw("<config:config-item config:name=\"ViewId\" config:type=\"string\">view1</config:config-item>")
                .raw("<config:config-item-map-named config:name=\"Tables\">");
        for (int i = 0; i < book.getSheetCount(); i++) {
            Sheet sheet = book.getSheet(i);
            int rows = sheet.getFrozenRows();
            int columns = sheet.getFrozenColumns();
            if (rows == 0 && columns == 0) {
                continue;
            }
            x.raw("<config:config-item-map-entry").attr("config:name", sheet.getName()).raw(">");
            item(x, "HorizontalSplitMode", "short", columns > 0 ? 2 : 0);
            item(x, "VerticalSplitMode", "short", rows > 0 ? 2 : 0);
            item(x, "HorizontalSplitPosition", "int", columns);
            item(x, "VerticalSplitPosition", "int", rows);
            item(x, "ActiveSplitRange", "short", 2);
            item(x, "PositionLeft", "int", 0);
            item(x, "PositionRight", "int", columns);
            item(x, "PositionTop", "int", 0);
            item(x, "PositionBottom", "int", rows);
            x.raw("</config:config-item-map-entry>");
        }
        x.raw("</config:config-item-map-named></config:config-item-map-entry></config:config-item-map-indexed>")
                .raw("</config:config-item-set></office:settings></office:document-settings>");
    }

    private void writeContent(XmlOut x) throws IOException {
        x.raw(HEAD).raw("<office:document-content").raw(NAMESPACES).raw("><office:automatic-styles>");
        // column widths
        for (int s = 0; s < book.getSheetCount(); s++) {
            Sheet sheet = book.getSheet(s);
            int widths = Math.max(sheet.usedColumns(), sheet.widthExtent());
            for (int c = 0; c < widths; c++) {
                double w = sheet.getColumnWidth(c);
                if (w > 0) {
                    x.raw("<style:style").attr("style:name", "co" + s + "_" + c).raw(" style:family=\"table-column\">")
                            .raw("<style:table-column-properties")
                            .attr("style:column-width", millimetres(w * MM_PER_CHARACTER)).raw("/></style:style>");
                }
            }
        }
        int formats = book.formatCount();
        boolean[] hasDataStyle = new boolean[formats];
        for (int i = 1; i < formats; i++) {
            hasDataStyle[i] = book.format(i).writeOds(x, "N" + i);
        }
        for (int i = 0; i < formats; i++) {
            for (int bold = 0; bold < 2; bold++) {
                if (i == 0 && bold == 0) {
                    continue;
                }
                x.raw("<style:style").attr("style:name", (bold == 1 ? "ch" : "ce") + i)
                        .raw(" style:family=\"table-cell\" style:parent-style-name=\"Default\"");
                if (hasDataStyle[i]) {
                    x.attr("style:data-style-name", "N" + i);
                }
                x.raw(bold == 1 ? "><style:text-properties fo:font-weight=\"bold\"/></style:style>" : "/>");
            }
        }
        x.raw("</office:automatic-styles><office:body><office:spreadsheet>");
        // The formulas are written with the meaning they have here, which is Excel's: a
        // criterion is a wildcard pattern for the whole cell. An OpenDocument file that does not
        // say so is read with regular expressions, and COUNTIF(range,"*et") counts nothing.
        // Text compares without case here as in Excel, and OpenDocument's default is with it.
        x.raw("<table:calculation-settings table:case-sensitive=\"false\" table:use-wildcards=\"true\"")
                .raw(" table:use-regular-expressions=\"false\"")
                .raw(" table:search-criteria-must-apply-to-whole-cell=\"true\"");
        if (book.isDate1904()) {
            x.raw("><table:null-date table:date-value=\"1904-01-01\"/></table:calculation-settings>");
        } else {
            x.raw("/>");
        }
        for (int s = 0; s < book.getSheetCount(); s++) {
            writeSheet(x, book.getSheet(s), s);
        }
        String[] names = book.getDefinedNames();
        if (names.length > 0) {
            x.raw("<table:named-expressions>");
            for (String nameItem : names) {
                Node node = book.definedNameNode(nameItem);
                if (node == null) {
                    continue;
                }
                // a reference to the row above is stored wrapped, as the last row; LibreOffice
                // does not wrap, so the base cell is moved far enough that nothing has to
                int[] reach = FormulaParser.reach(node);
                Node expression = reach[0] == 0 && reach[1] == 0 ? node : FormulaParser.fromCell(node, reach[0], reach[1]);
                x.raw("<table:named-expression").attr("table:name", nameItem)
                        .attr("table:expression", FormulaParser.print(expression, true));
                if (book.getSheetCount() > 0) {
                    StringBuilder base = new StringBuilder("$");
                    FormulaParser.appendSheetName(base, book.getSheet(0).getName());
                    base.append(".$");
                    FormulaParser.appendColumn(base, reach[1]);
                    x.attr("table:base-cell-address", base.append('$').append(reach[0] + 1).toString());
                }
                x.raw("/>");
            }
            x.raw("</table:named-expressions>");
        }
        x.raw("</office:spreadsheet></office:body></office:document-content>");
    }

    private static String millimetres(double mm) {
        return FormulaParser.numberText(Math.floor(mm * 100 + 0.5) / 100) + "mm";
    }

    private void writeSheet(XmlOut x, Sheet sheet, int index) throws IOException {
        x.raw("<table:table").attr("table:name", sheet.getName()).raw(">");
        int columns = Math.max(1, Math.max(sheet.usedColumns(), sheet.widthExtent()));
        for (int c = 0; c < columns; c++) {
            x.raw("<table:table-column");
            if (sheet.getColumnWidth(c) > 0) {
                x.attr("table:style-name", "co" + index + "_" + c);
            }
            x.raw("/>");
        }
        int rows = Math.max(sheet.usedRows(), sheet.outlineExtent());
        anchors.clear();
        int[] lastAnchor = new int[0];
        for (int i = 0; i < sheet.getMergedRegionCount(); i++) {
            int[] region = sheet.getMergedRegion(i);
            anchors.put(key(region[0], region[1]), region);
            rows = Math.max(rows, region[0] + 1);
            if (region[0] >= lastAnchor.length) {
                int[] grown = new int[region[0] + 1];
                for (int k = lastAnchor.length; k < grown.length; k++) {
                    grown[k] = -1;
                }
                System.arraycopy(lastAnchor, 0, grown, 0, lastAnchor.length);
                lastAnchor = grown;
            }
            lastAnchor[region[0]] = Math.max(lastAnchor[region[0]], region[1]);
        }
        boolean header = sheet.isHeaderRow();
        int depth = 0;
        int emptyRows = 0;
        for (int r = 0; r < rows; r++) {
            int level = sheet.getRowOutlineLevel(r);
            Cell[] cells = sheet.rowCells(r);
            int last = -1;
            if (cells != null) {
                for (int c = cells.length - 1; c >= 0 && last < 0; c--) {
                    if (cells[c] != null && (cells[c].value != null || cells[c].formulaText != null)) {
                        last = c;
                    }
                }
            }
            if (r < lastAnchor.length) {
                // a merged region starts at a cell even when the cell is empty
                last = Math.max(last, lastAnchor[r]);
            }
            if (last < 0 && level == depth) {
                emptyRows++;
                continue;
            }
            emptyRows = flushEmptyRows(x, emptyRows);
            for (; depth < level; depth++) {
                x.raw("<table:table-row-group>");
            }
            for (; depth > level; depth--) {
                x.raw("</table:table-row-group>");
            }
            if (last < 0) {
                emptyRows++;
                continue;
            }
            x.raw("<table:table-row>");
            int emptyCells = 0;
            for (int c = 0; c <= last; c++) {
                Cell cell = cells != null && c < cells.length ? cells[c] : null;
                int[] region = anchors.isEmpty() ? null : anchors.get(key(r, c));
                boolean empty = cell == null || cell.value == null && cell.formulaText == null;
                if (empty && region == null) {
                    emptyCells++;
                    continue;
                }
                emptyCells = flushEmptyCells(x, emptyCells);
                writeCell(x, empty ? null : cell, header && r == 0, region);
            }
            x.raw("</table:table-row>");
        }
        flushEmptyRows(x, emptyRows);
        for (; depth > 0; depth--) {
            x.raw("</table:table-row-group>");
        }
        if (rows == 0) {
            x.raw("<table:table-row><table:table-cell/></table:table-row>");
        }
        x.raw("</table:table>");
    }

    private static int flushEmptyRows(XmlOut x, int count) throws IOException {
        if (count > 0) {
            x.raw("<table:table-row");
            if (count > 1) {
                x.attr("table:number-rows-repeated", count);
            }
            x.raw("><table:table-cell/></table:table-row>");
        }
        return 0;
    }

    private static int flushEmptyCells(XmlOut x, int count) throws IOException {
        if (count > 0) {
            x.raw("<table:table-cell");
            if (count > 1) {
                x.attr("table:number-columns-repeated", count);
            }
            x.raw("/>");
        }
        return 0;
    }

    private static void two(StringBuilder sb, long v) {
        if (v < 10) {
            sb.append('0');
        }
        sb.append(v);
    }

    private void writeCell(XmlOut x, Cell cell, boolean bold, int[] region) throws IOException {
        if (cell == null) {
            x.raw("<table:table-cell");
            spans(x, region);
            x.raw("/>");
            return;
        }
        Object v = cell.value;
        if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                v = CellError.NUM;
            }
        }
        x.raw("<table:table-cell");
        if (cell.format != 0 || bold) {
            x.attr("table:style-name", (bold ? "ch" : "ce") + cell.format);
        }
        spans(x, region);
        if (cell.formula != null) {
            x.attr("table:formula", "of:=" + FormulaParser.print(cell.formula, true));
        } else if (cell.formulaText != null && cell.odsSyntax) {
            x.attr("table:formula", "of:=" + cell.formulaText);
        }
        String shown = null;
        CellFormat format = book.format(cell.format);
        if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            // past Excel's last day a date has no four-digit year, and the number is kept instead
            if (format.isDate() && d >= 0 && d <= DateFunctions.LAST_DAY) {
                StringBuilder sb = new StringBuilder();
                long seconds = (long) Math.floor((d - Math.floor(d)) * 86400 + 0.5);
                double day = Math.floor(d);
                if (seconds >= 86400) {
                    seconds = 0;
                    day += 1;
                }
                if (format.isTimeOnly()) {
                    long total = (long) Math.floor(d * 86400 + 0.5);
                    sb.append("PT");
                    two(sb, total / 3600);
                    sb.append('H');
                    two(sb, total / 60 % 60);
                    sb.append('M');
                    two(sb, total % 60);
                    sb.append('S');
                    x.raw(" office:value-type=\"time\"").attr("office:time-value", sb.toString());
                } else {
                    int[] f = DateUtil.fields(day, book.isDate1904());
                    // day 60 of the 1900 system is a day no calendar has
                    if (f[1] == 2 && f[2] == 29 && !DateUtil.isLeapYear(f[0])) {
                        f[2] = 28;
                    }
                    sb.append(f[0]).append('-');
                    two(sb, f[1]);
                    sb.append('-');
                    two(sb, f[2]);
                    if (seconds != 0) {
                        sb.append('T');
                        two(sb, seconds / 3600);
                        sb.append(':');
                        two(sb, seconds / 60 % 60);
                        sb.append(':');
                        two(sb, seconds % 60);
                    }
                    x.raw(" office:value-type=\"date\"").attr("office:date-value", sb.toString());
                }
            } else {
                x.raw(format.isGeneral() || format.code().indexOf('%') < 0 ? " office:value-type=\"float\""
                        : " office:value-type=\"percentage\"").attr("office:value", FormulaParser.numberText(d));
            }
            shown = format.format(v, book.isDate1904());
        } else if (v instanceof Boolean) {
            boolean b = ((Boolean) v).booleanValue();
            x.raw(" office:value-type=\"boolean\"").attr("office:boolean-value", b ? "true" : "false");
            shown = b ? "TRUE" : "FALSE";
        } else if (v instanceof CellError) {
            x.raw(" office:value-type=\"string\" office:string-value=\"\" calcext:value-type=\"error\"");
            shown = v == CellError.CIRCULAR ? "Err:522" : v.toString(); //NOPMD CompareObjectsWithEquals - an error value is a constant
        } else if (v instanceof String) {
            x.raw(" office:value-type=\"string\"");
            shown = (String) v;
        }
        if (shown == null || shown.length() == 0) {
            x.raw("/>");
            return;
        }
        x.raw("><text:p>");
        paragraphs(x, shown);
        x.raw("</text:p></table:table-cell>");
    }

    private static void spans(XmlOut x, int[] region) throws IOException {
        if (region != null) {
            x.attr("table:number-columns-spanned", region[3] - region[1] + 1)
                    .attr("table:number-rows-spanned", region[2] - region[0] + 1);
        }
    }

    /// Text inside a paragraph: XML collapses runs of spaces and has no place for a tab or a
    /// line break, so the format has elements for them.
    private static void paragraphs(XmlOut x, String s) throws IOException {
        int n = s.length();
        int start = 0;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            boolean space = c == ' ' && (i == 0 || i == n - 1 || s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\n');
            if (c != '\n' && c != '\t' && c != '\r' && !space) {
                continue;
            }
            if (i > start) {
                x.text(s.substring(start, i), false);
            }
            start = i + 1;
            if (c == '\n') {
                x.raw("</text:p><text:p>");
            } else if (c == '\t') {
                x.raw("<text:tab/>");
            } else if (space) {
                x.raw("<text:s/>");
            }
        }
        if (start < n) {
            x.text(s.substring(start), false);
        }
    }
}
