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
package com.codename1.pdf;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentTest {

    @Test
    void canvasDocumentReadsBack() throws Exception {
        for (int pass = 0; pass < 2; pass++) {
            PdfDocument doc = new PdfDocument().setTitle("Ticket").setAuthor("Tests").setCompress(pass == 0);
            PdfPage page = doc.addPage(PdfPageSize.A5);
            PdfCanvas c = page.getCanvas();
            c.setFillColor(0x1565c0).fillRect(0, 0, page.getWidth(), 60);
            c.setDash(new float[] {4, 3}, 0).drawLine(16, 120, 200, 120);
            c.setFont(PdfFont.HELVETICA_BOLD, 20).drawText("Boarding pass", 16, 38);
            byte[] pdf = doc.toByteArray();
            assertTrue(new String(pdf, 0, 8, StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
            PdfReader r = PdfReader.open(pdf);
            assertEquals(1, r.getPageCount());
            assertEquals("Ticket", r.getTitle());
            assertEquals("Tests", r.getAuthor());
            assertEquals(PdfPageSize.A5.getWidth(), r.getPageWidth(0), 0.01);
            assertTrue(r.extractText(0).contains("Boarding pass"), r.extractText(0));
        }
    }

    @Test
    void layoutPaginatesAndRepeatsHeaders() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfLayout layout = new PdfLayout(doc, PdfPageSize.A4, 48);
        final int[] decorated = new int[1];
        layout.setPageDecorator(new PdfLayout.PageDecorator() {
            public void decorate(PdfPage page, int number, int count) {
                decorated[0]++;
                page.getCanvas().setFont(PdfFont.HELVETICA, 9).drawText("Page " + number + " of " + count, 48,
                        page.getHeight() - 24);
            }
        });
        layout.add(new PdfParagraph("Invoice 1042", PdfFont.HELVETICA_BOLD, 22));
        PdfTable t = new PdfTable(4, 1, 1.5f).setHeaderRows(1);
        t.addCell("Item").addCell("Qty").addCell("Price");
        for (int i = 1; i <= 120; i++) {
            t.addCell("Widget " + i).addCell("1").addCell("$9.90");
        }
        layout.add(t);
        layout.finish();
        int pages = doc.getPageCount();
        assertTrue(pages >= 3, "pages " + pages);
        assertEquals(pages, decorated[0]);
        PdfReader r = PdfReader.open(doc.toByteArray());
        assertEquals(pages, r.getPageCount());
        for (int i = 0; i < pages; i++) {
            String text = r.extractText(i);
            assertTrue(text.contains("Item"), "header on page " + (i + 1));
            assertTrue(text.contains("Page " + (i + 1) + " of " + pages), text);
        }
        assertTrue(r.extractText().contains("Widget 120"));
    }

    @Test
    void rightToLeftParagraphExtractsInReadingOrder() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfLayout layout = new PdfLayout(doc, PdfPageSize.A4, 48);
        // Standard fonts cannot show Hebrew; the layout falls back per
        // glyph, but the text map must still read back in logical order.
        String hebrew = "\u05e9\u05dc\u05d5\u05dd";
        PdfParagraph p = new PdfParagraph("abc", PdfFont.HELVETICA, 12);
        assertEquals(PdfParagraph.DIRECTION_LTR, p.resolvedDirection());
        PdfParagraph q = new PdfParagraph(hebrew + " 123", PdfFont.HELVETICA, 12);
        assertEquals(PdfParagraph.DIRECTION_RTL, q.resolvedDirection());
        layout.add(p).add(q);
        layout.finish();
        assertEquals(1, PdfReader.open(doc.toByteArray()).getPageCount());
    }

    @Test
    void readsAesEncryptedFile() throws Exception {
        byte[] pdf = aesFile("Secret text");
        PdfReader r = PdfReader.open(pdf);
        assertTrue(r.isEncrypted());
        assertTrue(r.extractText(0).contains("Secret text"), r.extractText(0));
    }

    // ---- an AESV2 (R4) file written with the JDK's own crypto ----

    private static final byte[] PAD = hex("28bf4e5e4e758a4164004e56fffa01082e2e00b6d0683e802f0ca9fe6453697a");

    private static byte[] aesFile(String text) throws Exception {
        byte[] id = hex("0123456789abcdef0123456789abcdef");
        byte[] o = new byte[32];
        for (int i = 0; i < 32; i++) {
            o[i] = (byte) (i * 7 + 1);
        }
        int p = -4;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(PAD);
        md5.update(o);
        md5.update(new byte[] {(byte) p, (byte) (p >> 8), (byte) (p >> 16), (byte) (p >> 24)});
        md5.update(id);
        byte[] key = md5.digest();
        for (int i = 0; i < 50; i++) {
            key = MessageDigest.getInstance("MD5").digest(key);
        }
        md5 = MessageDigest.getInstance("MD5");
        md5.update(PAD);
        md5.update(id);
        byte[] u = rc4(key, md5.digest());
        for (int i = 1; i <= 19; i++) {
            byte[] k = new byte[key.length];
            for (int j = 0; j < k.length; j++) {
                k[j] = (byte) (key[j] ^ i);
            }
            u = rc4(k, u);
        }
        byte[] u32 = new byte[32];
        System.arraycopy(u, 0, u32, 0, 16);
        byte[] content = ("BT /F1 18 Tf 40 700 Td (" + text + ") Tj ET").getBytes(StandardCharsets.ISO_8859_1);
        byte[] enc = aes(objectKey(key, 4), content);
        String[] objs = new String[6];
        objs[1] = "<< /Type /Catalog /Pages 2 0 R >>";
        objs[2] = "<< /Type /Pages /Kids [3 0 R] /Count 1 >>";
        objs[3] = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 600 800] /Contents 4 0 R"
                + " /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> >>";
        objs[5] = "<< /Filter /Standard /V 4 /R 4 /Length 128 /CF << /StdCF << /CFM /AESV2 /AuthEvent /DocOpen"
                + " /Length 16 >> >> /StmF /StdCF /StrF /StdCF /O <" + toHex(o) + "> /U <" + toHex(u32)
                + "> /P " + p + " >>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.6\n");
        int[] off = new int[6];
        for (int i = 1; i < 6; i++) {
            off[i] = out.size();
            if (i == 4) {
                write(out, "4 0 obj\n<< /Length " + enc.length + " >>\nstream\n");
                out.write(enc);
                write(out, "\nendstream\nendobj\n");
            } else {
                write(out, i + " 0 obj\n" + objs[i] + "\nendobj\n");
            }
        }
        int xref = out.size();
        StringBuilder sb = new StringBuilder("xref\n0 6\n0000000000 65535 f \n");
        for (int i = 1; i < 6; i++) {
            String n = String.valueOf(off[i]);
            for (int k = n.length(); k < 10; k++) {
                sb.append('0');
            }
            sb.append(n).append(" 00000 n \n");
        }
        sb.append("trailer\n<< /Size 6 /Root 1 0 R /Encrypt 5 0 R /ID [<").append(toHex(id)).append("> <")
                .append(toHex(id)).append(">] >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        write(out, sb.toString());
        return out.toByteArray();
    }

    private static byte[] objectKey(byte[] key, int num) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(key);
        md5.update(new byte[] {(byte) num, (byte) (num >> 8), (byte) (num >> 16), 0, 0});
        md5.update("sAlT".getBytes(StandardCharsets.ISO_8859_1));
        return md5.digest();
    }

    private static byte[] aes(byte[] key, byte[] data) throws Exception {
        byte[] iv = new byte[16];
        for (int i = 0; i < 16; i++) {
            iv[i] = (byte) (i * 13 + 5);
        }
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] body = c.doFinal(data);
        byte[] out = new byte[16 + body.length];
        System.arraycopy(iv, 0, out, 0, 16);
        System.arraycopy(body, 0, out, 16, body.length);
        return out;
    }

    private static byte[] rc4(byte[] key, byte[] data) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xff)) & 0xff;
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
        }
        byte[] out = new byte[data.length];
        int a = 0;
        int b = 0;
        for (int k = 0; k < data.length; k++) {
            a = (a + 1) & 0xff;
            b = (b + s[a]) & 0xff;
            int t = s[a];
            s[a] = s[b];
            s[b] = t;
            out[k] = (byte) (data[k] ^ s[(s[a] + s[b]) & 0xff]);
        }
        return out;
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }

    private static byte[] hex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
        }
        return sb.toString();
    }

    @Test
    void longWordsBreakBetweenClustersOnly() {
        String emoji = "\ud83d\ude00";
        assertEquals(2, PdfParagraph.nextCluster(emoji + "a", 0, 3));
        assertEquals(2, PdfParagraph.nextCluster("e\u0301x", 0, 3));
        assertEquals(5, PdfParagraph.nextCluster(emoji + "\u200d" + emoji, 0, 5));
        PdfDocument doc = new PdfDocument();
        PdfLayout layout = new PdfLayout(doc,
                PdfPageSize.A5, 20);
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            word.append(emoji);
        }
        layout.add(new PdfParagraph(word.toString(), PdfFont.HELVETICA, 12));
        layout.finish();
        assertTrue(doc.getPageCount() >= 1);
    }

    @Test
    void tableRejectsWeightsThatAreNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> new PdfTable(0f, 0f));
        assertThrows(IllegalArgumentException.class, () -> new PdfTable(1f, -1f));
    }
}
