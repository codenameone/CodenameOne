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

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.CardReaderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The counter's devices: the receipt printer and cash drawer on the shop's
/// network, and the card reader paired with this register.
final class DevicesPane {
    private DevicesPane() {
    }

    static com.codename1.ui.Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-devices");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Devices", null));
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        root.add(BorderLayout.CENTER, page);
        build(page);
        return root;
    }

    private static void build(final Container page) {
        page.removeAll();
        page.add(PosUi.label("Receipt printer", "PosGroup"));
        final TextField address = Ui.field(page, Lang.tr("Printer address"), "pos-printer",
                "192.168.1.50:9100");
        address.setText(CounterDevices.printerText());
        Container widths = new Container(new GridLayout(1, 2));
        widths.add(PosUi.key(Lang.tr("80 mm paper"), "pos-printer-80", "PosChoice",
                e -> CounterDevices.setColumns(EscPos.COLUMNS_80MM)));
        widths.add(PosUi.key(Lang.tr("58 mm paper"), "pos-printer-58", "PosChoice",
                e -> CounterDevices.setColumns(EscPos.COLUMNS_58MM)));
        page.add(widths);
        Container actions = new Container(new GridLayout(1, 3));
        actions.add(PosUi.icon(FontImage.MATERIAL_SAVE, "Save", "pos-printer-save", "PosChoice",
                e -> {
                    if (address.getText().trim().length() > 0
                            && PrinterAddress.parse(address.getText()) == null) {
                        Ui.say("That is not a printer address");
                        return;
                    }
                    CounterDevices.setPrinter(address.getText());
                    Ui.say("Saved");
                }));
        actions.add(PosUi.icon(FontImage.MATERIAL_PRINT, "Test print", "pos-printer-test",
                "PosChoice", e -> testPrint()));
        actions.add(PosUi.icon(FontImage.MATERIAL_POINT_OF_SALE, "Open drawer",
                "pos-drawer-open", "PosChoice", e -> CounterDevices.kick()));
        page.add(actions);
        page.add(PosUi.label("Card reader", "PosGroup"));
        page.add(PosUi.label("Looking...", "PosMuted"));
        page.revalidateLater();
        Api.posCounter().readers(PosState.storeId(), Net.to(list -> {
            page.removeComponent(page.getComponentAt(page.getComponentCount() - 1));
            CardReaderDto here = null;
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                if (PosState.registerId().equals(list.get(iter).registerId())) {
                    here = list.get(iter);
                }
            }
            if (here != null) {
                final CardReaderDto r = here;
                page.add(PosUi.data(r.label() + Ui.DOT + r.serial(), "PosCartTitle"));
                page.add(PosUi.key(Lang.tr("Unpair the reader"), "pos-reader-remove",
                        "PosChoice", e -> Api.posCounter().removeReader(PosState.storeId(),
                            r.id(), Net.to(done -> build(page), Ui::fail))));
            } else {
                page.add(PosUi.label("No card reader at this register; card payments use "
                        + "the standalone terminal.", "PosMuted"));
            }
            final TextField code = Ui.field(page, Lang.tr("Code shown on the reader"),
                    "pos-reader-code", "simulated-front");
            page.add(PosUi.key(Lang.tr("Pair a reader"), "pos-reader-pair", "PosPay", e ->
                    Api.posCounter().addReader(PosState.storeId(), new CardReaderDto(null,
                        PosState.registerName(), PosState.registerId(), code.getText(), null,
                        null, null), Net.to(done -> build(page), Ui::fail))));
            page.revalidateLater();
        }, (status, message) -> {
            page.removeComponent(page.getComponentAt(page.getComponentCount() - 1));
            page.add(PosUi.label(status == 403 ? "Only a manager pairs card readers"
                    : "The card readers need the network", "PosMuted"));
            page.revalidateLater();
        }));
    }

    private static void testPrint() {
        java.util.List<String[]> lines = new java.util.ArrayList<String[]>();
        lines.add(new String[] {"1 x Test", PosState.money(0)});
        PrinterAddress a = CounterDevices.printer();
        if (a == null) {
            Ui.say("Set the receipt printer address under Devices");
            return;
        }
        new SocketPrinter(a).send(EscPos.receipt(PosState.storeName(),
                java.util.Arrays.asList(PosState.registerName()), lines, null, "Printer test",
                CounterDevices.columns(), false), error -> Ui.say(error == null
                    ? "Receipt printed" : Lang.tr("The printer: {0}", error)));
    }
}
