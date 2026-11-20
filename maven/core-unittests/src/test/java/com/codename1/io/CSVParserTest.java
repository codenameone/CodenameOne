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
package com.codename1.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVParserTest {
    @Test
    void parsesSimpleRowsAndColumns() throws IOException {
        String csv = "name,age,city\nAlice,30,Paris\nBob,25,London";
        CSVParser parser = new CSVParser();

        String[][] rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, rows.length);
        assertArrayEquals(new String[]{"name", "age", "city"}, rows[0]);
        assertArrayEquals(new String[]{"Alice", "30", "Paris"}, rows[1]);
        assertArrayEquals(new String[]{"Bob", "25", "London"}, rows[2]);
    }

    @Test
    void supportsQuotedValuesAndEscapedQuotes() throws IOException {
        String csv = "\"Name\",\"Address\",\"Notes\"\n" +
                "\"Doe, John\",\"\"\"Main\"\" Street\",\"Line with \"\"quote\"\" inside\"";
        CSVParser parser = new CSVParser();

        String[][] rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, rows.length);
        assertArrayEquals(new String[]{"Name", "Address", "Notes"}, rows[0]);
        assertArrayEquals(new String[]{"Doe, John", "\"Main\" Street", "Line with \"quote\" inside"}, rows[1]);
    }

    @Test
    void honorsCustomSeparatorAndEmptyValues() throws IOException {
        String csv = "one;two;;\n;three;four;";
        CSVParser parser = new CSVParser(';');

        String[][] rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, rows.length);
        assertArrayEquals(new String[]{"one", "two", "", ""}, rows[0]);
        assertArrayEquals(new String[]{"", "three", "four"}, rows[1]);
    }

    @Test
    void streamingKeepsEmptyLinesAndTrailingEmptyValues() throws IOException {
        final List<String[]> rows = new ArrayList<String[]>();
        new CSVParser().parse(new StringReader("a,b,\r\n\r\n\"\",x\nlast,"), new CSVParser.RowHandler() {
            public boolean row(int index, String[] cells) {
                assertEquals(rows.size(), index);
                rows.add(cells);
                return true;
            }
        });
        assertEquals(4, rows.size());
        assertArrayEquals(new String[]{"a", "b", ""}, rows.get(0));
        assertArrayEquals(new String[0], rows.get(1));
        assertArrayEquals(new String[]{"", "x"}, rows.get(2));
        assertArrayEquals(new String[]{"last", ""}, rows.get(3));
    }

    @Test
    void streamingStopsWhenTheHandlerSaysSo() throws IOException {
        final int[] count = new int[1];
        new CSVParser().parse(new StringReader("1\n2\n3\n"), new CSVParser.RowHandler() {
            public boolean row(int index, String[] cells) {
                count[0]++;
                return index < 1;
            }
        });
        assertEquals(2, count[0]);
    }

    @Test
    void quotedValueMayHoldSeparatorsAndLineBreaks() throws IOException {
        List<String[]> rows = stream(new CSVParser(), "\"a,\nb\",\"say \"\"hi\"\"\"\n");
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"a,\nb", "say \"hi\""}, rows.get(0));
    }

    @Test
    void detectsTheSeparatorFromContent() throws IOException {
        CSVParser parser = new CSVParser();
        parser.setDetectSeparator(true);
        List<String[]> rows = stream(parser, "name;note\nAnn;\"a, b, c\"\nBob;x\n");
        assertEquals(';', parser.getSeparatorChar());
        assertArrayEquals(new String[]{"Ann", "a, b, c"}, rows.get(1));

        assertEquals('\t', CSVParser.detectSeparator("a\tb\tc\n1,5\t2\t3\n", ','));
        assertEquals('|', CSVParser.detectSeparator("a|b\n1|2\n", ','));
        assertEquals(';', CSVParser.detectSeparator("no separators here\n", ';'));
        // free text full of commas does not outvote a separator that is the same on every line
        assertEquals(';', CSVParser.detectSeparator("a;b\nx, y, z, w;1\nq;2\n", ','));
    }

    @Test
    void readsTheSeparatorHintLine() throws IOException {
        CSVParser parser = new CSVParser();
        parser.setDetectSeparator(true);
        List<String[]> rows = stream(parser, "sep=|\r\na|b,c\r\n");
        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"a", "b,c"}, rows.get(0));

        CSVParser legacy = new CSVParser();
        legacy.setDetectSeparator(true);
        String[][] all = legacy.parse(new StringReader("sep=;\na;b\n1;2"));
        assertArrayEquals(new String[]{"a", "b"}, all[0]);
        assertArrayEquals(new String[]{"1", "2"}, all[1]);
    }

    @Test
    void dropsTheUtf8ByteOrderMark() throws IOException {
        byte[] text = "id,name\n1,x".getBytes(StandardCharsets.UTF_8);
        byte[] withMark = new byte[text.length + 3];
        withMark[0] = (byte) 0xEF;
        withMark[1] = (byte) 0xBB;
        withMark[2] = (byte) 0xBF;
        System.arraycopy(text, 0, withMark, 3, text.length);
        String[][] rows = new CSVParser().parse(new ByteArrayInputStream(withMark));
        assertEquals("id", rows[0][0]);
    }

    @Test
    void readsUtf16WithEitherByteOrder() throws IOException {
        String text = "na\u00efve\t\u4e2d\n1\t2\n";
        for (int pass = 0; pass < 2; pass++) {
            boolean little = pass == 0;
            byte[] body = text.getBytes(little ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE);
            byte[] file = new byte[body.length + 2];
            file[0] = (byte) (little ? 0xFF : 0xFE);
            file[1] = (byte) (little ? 0xFE : 0xFF);
            System.arraycopy(body, 0, file, 2, body.length);
            CSVParser parser = new CSVParser();
            parser.setDetectSeparator(true);
            final List<String[]> rows = new ArrayList<String[]>();
            parser.parse(new ByteArrayInputStream(file), new CSVParser.RowHandler() {
                public boolean row(int index, String[] cells) {
                    rows.add(cells);
                    return true;
                }
            });
            assertEquals(2, rows.size());
            assertArrayEquals(new String[]{"na\u00efve", "\u4e2d"}, rows.get(0));
            assertArrayEquals(new String[]{"1", "2"}, rows.get(1));
        }
    }

    @Test
    void shortStreamsAreNotMistakenForAByteOrderMark() throws IOException {
        assertArrayEquals(new String[]{"a"}, new CSVParser().parse(new ByteArrayInputStream(new byte[]{'a'}))[0]);
        assertArrayEquals(new String[]{"ab"}, new CSVParser().parse(new ByteArrayInputStream(new byte[]{'a', 'b'}))[0]);
        assertEquals(1, new CSVParser().parse(new ByteArrayInputStream(new byte[0])).length);
    }

    @Test
    void customQuoteCharacter() throws IOException {
        CSVParser parser = new CSVParser();
        parser.setQuoteChar('\'');
        List<String[]> rows = stream(parser, "'a,b','it''s'\n");
        assertArrayEquals(new String[]{"a,b", "it's"}, rows.get(0));
    }

    private static List<String[]> stream(CSVParser parser, String text) throws IOException {
        final List<String[]> rows = new ArrayList<String[]>();
        parser.parse(new StringReader(text), new CSVParser.RowHandler() {
            public boolean row(int index, String[] cells) {
                rows.add(cells);
                return true;
            }
        });
        return rows;
    }
}
