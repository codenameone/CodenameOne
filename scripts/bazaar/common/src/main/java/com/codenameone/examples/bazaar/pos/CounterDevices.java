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

import com.codename1.io.Preferences;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The receipt printer this register prints to and the cash drawer wired to
/// it, kept on the device: printers sit on the shop's own network.
final class CounterDevices {
    private static final String PRINTER = "pos.printer";
    private static final String WIDTH = "pos.printerColumns";

    private CounterDevices() {
    }

    static String printerText() {
        return Preferences.get(PRINTER, "");
    }

    static PrinterAddress printer() {
        return PrinterAddress.parse(printerText());
    }

    static void setPrinter(String typed) {
        Preferences.set(PRINTER, typed == null ? "" : typed.trim());
    }

    static int columns() {
        return Preferences.get(WIDTH, EscPos.COLUMNS_80MM);
    }

    static void setColumns(int columns) {
        Preferences.set(WIDTH, columns);
    }

    /// Prints `r` on the receipt printer, opening the drawer first when the
    /// sale took cash.
    static void print(Receipt r, boolean kick) {
        List<String> header = new ArrayList<String>();
        header.add(r.register + "  " + Ui.when(r.when));
        int n = r.number();
        header.add(n > 0 ? "Order #" + n : "Sale " + r.clientId.substring(0,
                Math.min(8, r.clientId.length())));
        send(EscPos.receipt(r.store == null ? "" : r.store, header, r.lines, r.totals,
                "Thank you", columns(), kick), "Receipt printed");
    }

    /// Opens the cash drawer.
    static void kick() {
        send(EscPos.drawerKick(), "Drawer opened");
    }

    private static void send(byte[] job, final String ok) {
        PrinterAddress a = printer();
        if (a == null) {
            Ui.say("Set the receipt printer address under Devices");
            return;
        }
        new SocketPrinter(a).send(job, error -> Ui.say(error == null ? ok
                : com.codenameone.examples.bazaar.ui.Lang.tr("The printer: {0}", error)));
    }
}
