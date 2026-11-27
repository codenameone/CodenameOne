/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.pos;

import com.codenameone.examples.bazaar.merchant.CommissionRate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The counter's hardware logic with no UI: ESC/POS bytes, the printer's
/// address, polling a card reader and commission rates.
class CounterHardwareTest {
    /// Collects jobs instead of opening a socket.
    static final class MemorySink implements PrinterSink {
        final List<byte[]> jobs = new ArrayList<byte[]>();

        @Override
        public void send(byte[] job, Done done) {
            jobs.add(job);
            done.done(null);
        }
    }

    private static byte[] cat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) {
            n += p.length;
        }
        byte[] out = new byte[n];
        int at = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    private static byte[] b(int... v) {
        byte[] out = new byte[v.length];
        for (int iter = 0; iter < v.length; iter++) {
            out[iter] = (byte) v[iter];
        }
        return out;
    }

    private static byte[] ascii(String s) {
        byte[] out = new byte[s.length()];
        for (int iter = 0; iter < s.length(); iter++) {
            out[iter] = (byte) s.charAt(iter);
        }
        return out;
    }

    @Test
    void theDrawerKickIsEscP() {
        assertArrayEquals(b(0x1B, 0x70, 0x00, 0x19, 0xFA), EscPos.drawerKick());
    }

    @Test
    void commandsAreTheEscPosBytes() {
        byte[] got = new EscPos(16).align(EscPos.CENTER).bold(true).large(true).text("Hi")
                .large(false).bold(false).align(EscPos.LEFT).feed(2).cut().bytes();
        assertArrayEquals(cat(b(0x1B, 0x40), b(0x1B, 0x61, 1), b(0x1B, 0x45, 1),
                b(0x1D, 0x21, 0x11), ascii("Hi"), b(0x1D, 0x21, 0), b(0x1B, 0x45, 0),
                b(0x1B, 0x61, 0), b(0x1B, 0x64, 2), b(0x1D, 0x56, 66, 0)), got);
    }

    @Test
    void pairsFillTheLineAndCutTheLeftSide() {
        assertEquals("Mug         $10.00", EscPos.pad("Mug", "$10.00", 18));
        assertEquals("A very lon $10.00", EscPos.pad("A very long title", "$10.00", 17));
        assertArrayEquals(cat(b(0x1B, 0x40), ascii("Tea     $2.50"), b(0x0A)),
                new EscPos(13).pair("Tea", "$2.50").bytes());
    }

    @Test
    void textIsSentAsAscii() {
        assertEquals("Cafe creme EUR3 - ok?", EscPos.ascii("Caf\u00e9 cr\u00e8me \u20ac3 \u2013 ok\u4e2d"));
        assertEquals("Strasse", EscPos.ascii("Stra\u00dfe"));
    }

    @Test
    void aReceiptKicksTheDrawerFirstAndCutsLast() {
        List<String[]> lines = new ArrayList<String[]>();
        lines.add(new String[] {"1 x Mug", "$10.00"});
        List<String[]> totals = new ArrayList<String[]>();
        totals.add(new String[] {"Total", "$10.40"});
        byte[] r = EscPos.receipt("Shop", Arrays.asList("Front"), lines, totals, "Thanks", 20,
                true);
        byte[] expected = cat(b(0x1B, 0x40), EscPos.drawerKick(),
                b(0x1B, 0x61, 1), b(0x1D, 0x21, 0x11), b(0x1B, 0x45, 1), ascii("Shop"), b(0x0A),
                b(0x1D, 0x21, 0), b(0x1B, 0x45, 0), ascii("Front"), b(0x0A),
                b(0x1B, 0x61, 0), ascii("--------------------"), b(0x0A),
                ascii("1 x Mug       $10.00"), b(0x0A),
                ascii("--------------------"), b(0x0A),
                b(0x1B, 0x45, 1), ascii("Total         $10.40"), b(0x0A), b(0x1B, 0x45, 0),
                b(0x1B, 0x64, 1), b(0x1B, 0x61, 1), ascii("Thanks"), b(0x0A), b(0x1B, 0x61, 0),
                b(0x1B, 0x64, 3), b(0x1D, 0x56, 66, 0));
        assertArrayEquals(expected, r);
        MemorySink sink = new MemorySink();
        final String[] error = {"unset"};
        sink.send(r, e -> error[0] = e);
        assertNull(error[0]);
        assertArrayEquals(expected, sink.jobs.get(0));
        byte[] noKick = EscPos.receipt("Shop", null, lines, totals, "", 20, false);
        assertFalse(contains(noKick, EscPos.drawerKick()));
    }

    private static boolean contains(byte[] in, byte[] what) {
        for (int iter = 0; iter + what.length <= in.length; iter++) {
            boolean all = true;
            for (int j = 0; all && j < what.length; j++) {
                all = in[iter + j] == what[j];
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    @Test
    void thePrinterAddressDefaultsToTheRawPort() {
        PrinterAddress a = PrinterAddress.parse(" 192.168.1.50 ");
        assertEquals("192.168.1.50", a.host);
        assertEquals(9100, a.port);
        assertEquals(515, PrinterAddress.parse("printer.local:515").port);
        assertEquals("fe80::1", PrinterAddress.parse("[fe80::1]:9101").host);
        assertEquals(9101, PrinterAddress.parse("[fe80::1]:9101").port);
        assertEquals("[fe80::1]:9101", PrinterAddress.parse("[fe80::1]:9101").toString());
        assertEquals(9100, PrinterAddress.parse("fe80::1").port);
        assertNull(PrinterAddress.parse(""));
        assertNull(PrinterAddress.parse("host:"));
        assertNull(PrinterAddress.parse("host:70000"));
        assertNull(PrinterAddress.parse("host:9x"));
        assertNull(PrinterAddress.parse(":9100"));
        assertNull(PrinterAddress.parse("http://host"));
        assertNull(PrinterAddress.parse("[fe80::1"));
    }

    @Test
    void theRegisterPollsUntilTheReaderAnswers() {
        assertEquals(ReaderPoll.WAIT, ReaderPoll.next("PENDING", 1, 0));
        assertEquals(ReaderPoll.PAID, ReaderPoll.next("SUCCEEDED", 3, 0));
        assertEquals(ReaderPoll.STOP, ReaderPoll.next("FAILED", 3, 0));
        assertEquals(ReaderPoll.STOP, ReaderPoll.next("CANCELED", 3, 0));
        assertEquals(ReaderPoll.WAIT, ReaderPoll.next(null, 3, 1));
        assertEquals(ReaderPoll.STOP, ReaderPoll.next(null, 9, ReaderPoll.MAX_MISSES));
        assertEquals(ReaderPoll.STOP, ReaderPoll.next("PENDING", ReaderPoll.MAX_ASKS, 0));
        assertTrue(ReaderPoll.standalone(409));
        assertTrue(ReaderPoll.standalone(0));
        assertFalse(ReaderPoll.standalone(502));
    }

    @Test
    void commissionRatesAreTypedAsPercentages() {
        assertEquals(500, CommissionRate.parse("5"));
        assertEquals(750, CommissionRate.parse("7.5"));
        assertEquals(1225, CommissionRate.parse("12,25 %"));
        assertEquals(0, CommissionRate.parse("0"));
        assertEquals(10000, CommissionRate.parse("100"));
        assertEquals(-1, CommissionRate.parse("100.5"));
        assertEquals(-1, CommissionRate.parse("1.234"));
        assertEquals(-1, CommissionRate.parse("five"));
        assertEquals(-1, CommissionRate.parse(""));
        assertEquals("5%", CommissionRate.show(500));
        assertEquals("7.5%", CommissionRate.show(750));
        assertEquals("12.25%", CommissionRate.show(1225));
        assertEquals("0.05%", CommissionRate.show(5));
    }
}
