/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * Differential torture for OutputStreamWriter.
 *
 * The writer now accumulates into a byte buffer and encodes UTF-8 itself instead of
 * calling String.getBytes once per write, so it has to produce the same bytes the
 * JDK's encoder does -- including on the inputs an encoder is allowed to disagree
 * about. Every case prints its bytes in hex and the harness compares that against a
 * real JDK, so the expected answer is not written down here.
 *
 * What is deliberately covered:
 *
 *  - the four UTF-8 lengths, including astral characters written as surrogate pairs,
 *    which are the only case where one run of output bytes spans two input chars;
 *  - a surrogate pair split across two write calls, which the buffer must either
 *    join or replace -- whichever the JDK does;
 *  - unpaired high and low surrogates, where an encoder chooses between the CESU-8
 *    three byte form and a replacement, and the two disagree byte for byte;
 *  - write(String, off, len) with off greater than zero. substring takes an end
 *    index, and passing len there asked for the wrong range: short whenever off was
 *    positive, and an exception when len was below off;
 *  - writes that straddle the internal buffer boundary, so a character whose bytes
 *    do not fit is flushed whole rather than split;
 *  - flush and close ordering, since buffered bytes not flushed on close are lost.
 *
 * Every non-ASCII character below is written as an escape; a raw one would fail the
 * ASCII-only source rule this tree builds under.
 */
public class WriterT {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static void dump(String label, ByteArrayOutputStream bo) {
        byte[] b = bo.toByteArray();
        StringBuilder sb = new StringBuilder();
        sb.append(label).append('[').append(b.length).append("]=");
        for(int i = 0 ; i < b.length ; i++) {
            int v = b[i] & 0xff;
            sb.append(HEX[v >> 4]).append(HEX[v & 0xf]);
        }
        System.out.println(sb.toString());
    }

    private static int digest(byte[] b) {
        int sum = 0;
        for(int i = 0 ; i < b.length ; i++) {
            sum = sum * 31 + (b[i] & 0xff);
        }
        return sum;
    }

    public static void main(String[] args) throws Exception {
        String[] basics = new String[] {
            "",
            "hello",
            "A",
            "\u00ff\u07ff",
            "\u0800\u0fff\uffff",
            "a\u00e9b\u20acc",
            "\ud83d\ude00",
            "x\ud83d\ude00y\ud834\udd1ez"
        };
        for(int i = 0 ; i < basics.length ; i++) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            w.write(basics[i]);
            w.close();
            dump("basic" + i, bo);
        }

        String[] broken = new String[] {
            "\ud800",
            "\udc00",
            "a\ud800b",
            "a\udc00b",
            "\ud800\ud800",
            "\udc00\ud800",
            "end\ud800"
        };
        for(int i = 0 ; i < broken.length ; i++) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            w.write(broken[i]);
            w.close();
            dump("broken" + i, bo);
        }

        {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            w.write("\ud83d");
            w.write("\ude00");
            w.close();
            dump("split", bo);
        }

        {
            String s = "abcdefghij";
            for(int off = 0 ; off <= 5 ; off++) {
                for(int len = 0 ; len <= s.length() - off ; len++) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
                    w.write(s, off, len);
                    w.close();
                    dump("sub_" + off + "_" + len, bo);
                }
            }
        }

        {
            String s = "a\u00e9\u20ac\ud83d\ude00z";
            for(int off = 0 ; off < s.length() ; off++) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
                w.write(s, off, s.length() - off);
                w.close();
                dump("wsub" + off, bo);
            }
        }

        {
            char[] c = "0\u00e91\u20ac2\ud83d\ude003".toCharArray();
            for(int off = 0 ; off < c.length ; off++) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
                w.write(c, off, c.length - off);
                w.close();
                dump("carr" + off, bo);
            }
        }

        {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            w.write('a');
            w.write(0x7f);
            w.write(0x80);
            w.write(0x7ff);
            w.write(0x800);
            w.write(0xffff);
            w.write(0xd83d);
            w.write(0xde00);
            w.close();
            dump("wint", bo);
        }

        for(int pad = 8185 ; pad <= 8195 ; pad++) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            StringBuilder sb = new StringBuilder();
            for(int i = 0 ; i < pad ; i++) {
                sb.append('x');
            }
            w.write(sb.toString());
            w.write("\u20ac\ud83d\ude00");
            w.close();
            byte[] b = bo.toByteArray();
            System.out.println("bound" + pad + " len=" + b.length + " sum=" + digest(b));
        }

        {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            StringBuilder sb = new StringBuilder();
            for(int i = 0 ; i < 20000 ; i++) {
                sb.append((char)('a' + (i % 26)));
            }
            w.write("HEAD");
            w.write(sb.toString());
            w.write("TAIL");
            w.close();
            byte[] b = bo.toByteArray();
            System.out.println("big len=" + b.length + " sum=" + digest(b)
                    + " head=" + (char)b[0] + (char)b[1]
                    + " tail=" + (char)b[b.length - 4] + (char)b[b.length - 1]);
        }

        {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(bo, "UTF-8");
            w.write("before");
            System.out.println("preflush=" + bo.toByteArray().length);
            w.flush();
            System.out.println("postflush=" + bo.toByteArray().length);
            w.write("after");
            System.out.println("prewrite=" + bo.toByteArray().length);
            w.close();
            System.out.println("postclose=" + bo.toByteArray().length);
        }
    }
}
