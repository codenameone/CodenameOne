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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVWriterTest {
    @Test
    void quotesOnlyWhatNeedsIt() {
        String text = new CSVWriter().toString(new String[][]{
                {"plain", "a,b", "say \"hi\"", "two\nlines", " padded", null, ""}
        });
        assertEquals("plain,\"a,b\",\"say \"\"hi\"\"\",\"two\nlines\",\" padded\",,\r\n", text);
    }

    @Test
    void quoteAllAndCustomSeparatorAndLineEnding() {
        CSVWriter w = new CSVWriter('\t');
        w.setQuoteAll(true);
        w.setLineEnding("\n");
        assertEquals("\"a\"\t\"\"\n", w.toString(new String[][]{{"a", ""}}));
    }

    @Test
    void formulaLikeValuesAreNeutralizedButNumbersAreNot() {
        CSVWriter w = new CSVWriter();
        w.setEscapeFormulas(true);
        w.setLineEnding("\n");
        assertEquals("'=1+1,'@cmd,'+A1,-12.5,+3,'-x,ok\n",
                w.toString(new String[][]{{"=1+1", "@cmd", "+A1", "-12.5", "+3", "-x", "ok"}}));
        // a number in scientific notation is a number, and has nothing to run
        assertEquals("-1.5e3,+2E+10,'-1e,'-1e3x\n", w.toString(new String[][]{{"-1.5e3", "+2E+10", "-1e", "-1e3x"}}));
    }

    @Test
    void byteOrderMarkAndSeparatorHint() throws IOException {
        CSVWriter w = new CSVWriter(';');
        w.setWriteByteOrderMark(true);
        w.setWriteSeparatorHint(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        w.write(out, new String[][]{{"a", "\u00e9"}});
        byte[] bytes = out.toByteArray();
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        CSVParser parser = new CSVParser();
        parser.setDetectSeparator(true);
        String[][] rows = parser.parse(new ByteArrayInputStream(bytes));
        assertArrayEquals(new String[]{"a", "\u00e9"}, rows[0]);
    }

    @Test
    void whatIsWrittenReadsBackTheSame() throws IOException {
        String[][] rows = {
                {"id", "text", ""},
                {"1", "a,b;c\td", "\"quoted\""},
                {""},
                {"multi\r\nline", " lead", "trail "},
                {"", "", ""}
        };
        StringWriter text = new StringWriter();
        new CSVWriter().write(text, rows);
        final List<String[]> back = new ArrayList<String[]>();
        new CSVParser().parse(new StringReader(text.toString()), new CSVParser.RowHandler() {
            public boolean row(int index, String[] cells) {
                back.add(cells);
                return true;
            }
        });
        assertEquals(rows.length, back.size());
        for (int i = 0; i < rows.length; i++) {
            assertArrayEquals(rows[i], back.get(i), "row " + i);
        }
    }
}
