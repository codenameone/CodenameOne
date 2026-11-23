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
import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.GridTileDto;
import com.codenameone.examples.bazaar.api.PosGridDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.shop.Money;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Shell;
import com.codenameone.examples.bazaar.ui.Ui;
import com.codenameone.examples.bazaar.ui.Windows;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Ringing up a sale: the products on one side, the cart on the other (on a
/// phone the cart is a sheet behind a "Charge" bar).
///
/// The scan field takes a camera scan, a keyboard-wedge scanner (which
/// types the code and presses Enter) and plain typing, which filters the
/// grid. On a desktop it keeps the focus so a scanner always lands in it,
/// and Enter with nothing typed pays.
final class SellScreen {
    private final Container root = new Container(new BorderLayout());
    private final Container grid = new Container();
    private final Container chips = new Container(BoxLayout.x());
    private final Container cartBox = new Container(BoxLayout.y());
    private final Container totalsBox = new Container(BoxLayout.y());
    private final Button pay;
    private final TextField scan;
    private String collection = "";
    private String query = "";
    private Sheet cartSheet;

    private SellScreen() {
        root.setName("pos-sell");
        root.setUIID("PosRoot");
        scan = new TextField("", Lang.tr("Scan or search"), 20, TextField.ANY);
        scan.setUIID("PosScan");
        scan.getHintLabel().setUIID("PosScanHint");
        scan.setName("pos-scan");
        scan.setDoneListener(e -> entered());
        scan.addDataChangedListener((type, index) -> {
            String t = scan.getText();
            // A scanner types fast; only a search filters as it goes.
            if (!Wedge.looksLikeCode(t)) {
                query = t;
                fillGrid();
            }
        });
        pay = PosUi.key("", "pos-pay", "PosPay", e -> pay());
    }

    static Container create() {
        SellScreen s = new SellScreen();
        s.build();
        return s.root;
    }

    /// The sell screen left the till.
    static void left(Container stage) {
        if ("pos-sell".equals(stage.getName())) {
            PosKeys.clear(PosKeys.PAY);
            PosKeys.clear(PosKeys.CLEAR);
        }
    }

    private void build() {
        Container products = new Container(new BorderLayout());
        products.setUIID("PosProducts");
        Container top = new Container(new BorderLayout());
        top.setUIID("PosScanRow");
        top.add(BorderLayout.CENTER, scan);
        Container tools = new Container(BoxLayout.x());
        if (cameraScan()) {
            tools.add(PosUi.icon(FontImage.MATERIAL_QR_CODE_SCANNER, "", "pos-camera", "PosTool",
                    e -> camera()));
        }
        if (Windows.supported()) {
            tools.add(PosUi.icon(FontImage.MATERIAL_CO_PRESENT, "", "pos-display", "PosTool",
                    e -> CustomerDisplay.toggle()));
        }
        tools.add(PosUi.pill());
        top.add(BorderLayout.EAST, tools);
        chips.setScrollableX(true);
        chips.setUIID("PosChips");
        Container north = new Container(BoxLayout.y());
        north.add(top).add(chips);
        products.add(BorderLayout.NORTH, north);
        grid.setScrollableY(true);
        grid.setUIID("PosGrid");
        products.add(BorderLayout.CENTER, grid);

        Container cart = cartPanel();
        if (PosUi.wide()) {
            root.add(BorderLayout.CENTER, products);
            cart.setPreferredW(Math.max(CN.convertToPixels(90), CN.getDisplayWidth() * 2 / 5));
            root.add(BorderLayout.EAST, cart);
        } else {
            root.add(BorderLayout.CENTER, products);
            root.add(BorderLayout.SOUTH, pay);
            cartSheetContent = cart;
        }
        fillChips();
        fillGrid();
        fillCart();
        PosUi.live(root, () -> {
            fillChips();
            fillGrid();
            fillCart();
            CustomerDisplay.update();
        }, true);
        PosKeys.set(PosKeys.PAY, this::pay);
        PosKeys.set(PosKeys.CLEAR, this::clearSale);
        Shell shell = Shell.current();
        if (shell != null) {
            PosKeys.install(shell.form());
        }
        if (PosUi.holdFocus()) {
            CN.callSerially(() -> scan.requestFocus());
        }
    }

    private Container cartSheetContent;

    // ---------------------------------------------------------------- grid

    private void fillChips() {
        chips.removeAll();
        Map<String, String> titles = PosState.collectionTitles();
        if (homeTiles() != null) {
            chip(HOME, Lang.tr("Home"));
        }
        chip("", Lang.tr("All"));
        for (Map.Entry<String, String> e : titles.entrySet()) {
            chip(e.getKey(), e.getValue());
        }
        chips.setHidden(titles.isEmpty());
    }

    private void chip(final String id, String title) {
        Button b = new Button(title, id.equals(collection) ? "PosChipOn" : "PosChip");
        b.setShouldLocalize(false);
        b.setName("pos-chip-" + (id.length() == 0 ? "all" : id));
        b.addActionListener(e -> {
            collection = id;
            fillChips();
            fillGrid();
            chips.revalidateLater();
        });
        chips.add(b);
    }

    private void fillGrid() {
        grid.removeAll();
        PosCatalogDto c = PosState.catalog();
        if (c == null || c.items() == null) {
            grid.setLayout(new BorderLayout());
            grid.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_CLOUD_DOWNLOAD,
                    "No catalog yet", "The register downloads the catalog once it is online."));
            grid.revalidateLater();
            return;
        }
        if (HOME.equals(collection) && homeTiles() != null) {
            fillHome(c, homeTiles());
            return;
        }
        List<String> ids = collection.length() == 0 ? null : PosState.collections().get(collection);
        List<List<PosItemDto>> products = Snapshot.products(Snapshot.filter(c.items(), query, ids));
        int width = PosUi.wide() ? CN.getDisplayWidth() * 3 / 5 : CN.getDisplayWidth();
        int cols = Layouts.columns(width, 30f, 6);
        grid.setLayout(new GridLayout(Math.max(1, (products.size() + cols - 1) / cols), cols));
        int side = CN.convertToPixels(22);
        for (int iter = 0; iter < products.size(); iter++) {
            grid.add(tile(products.get(iter), side));
        }
        if (products.isEmpty()) {
            grid.setLayout(new BorderLayout());
            grid.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_SEARCH, "Nothing matches",
                    "Try another word, or scan the item."));
        }
        grid.revalidateLater();
    }

    private static final String HOME = "home";

    /// The server's grid for this location, sorted by position, or null
    /// when the store has not laid one out.
    private static List<GridTileDto> homeTiles() {
        PosGridDto g = PosState.grid();
        if (g == null || g.tiles() == null || g.tiles().isEmpty()) {
            return null;
        }
        List<GridTileDto> out = new ArrayList<GridTileDto>(g.tiles());
        for (int a = 1; a < out.size(); a++) {
            GridTileDto t = out.get(a);
            int b = a - 1;
            while (b >= 0 && out.get(b).position() > t.position()) {
                out.set(b + 1, out.get(b));
                b--;
            }
            out.set(b + 1, t);
        }
        return out;
    }

    /// The smart grid: a tile per product, variant or collection the store
    /// placed, in its order; a tile whose target left the catalog is skipped.
    private void fillHome(PosCatalogDto c, List<GridTileDto> tiles) {
        int width = PosUi.wide() ? CN.getDisplayWidth() * 3 / 5 : CN.getDisplayWidth();
        int cols = Layouts.columns(width, 30f, 6);
        int side = CN.convertToPixels(22);
        List<Component> shown = new ArrayList<Component>();
        for (int iter = 0; iter < tiles.size(); iter++) {
            final GridTileDto t = tiles.get(iter);
            String kind = t.kind() == null ? "" : t.kind();
            String ref = t.ref() == null ? "" : t.ref();
            if ("COLLECTION".equalsIgnoreCase(kind)) {
                if (!PosState.collections().containsKey(ref)) {
                    continue;
                }
                String label = t.label() != null && t.label().length() > 0 ? t.label()
                        : PosState.collectionTitles().get(ref);
                Button b = new Button(label == null ? "" : label, "PosChoice");
                b.setShouldLocalize(false);
                b.setName("pos-grid-" + iter);
                if (t.color() != 0) {
                    b.getAllStyles().setBgColor(t.color() & 0xffffff);
                    b.getAllStyles().setBgTransparency(255);
                }
                b.addActionListener(e -> {
                    collection = t.ref();
                    fillChips();
                    fillGrid();
                    chips.revalidateLater();
                });
                shown.add(b);
                continue;
            }
            if ("ACTION".equalsIgnoreCase(kind)) {
                final String action = ref;
                Button b = new Button(t.label() != null && t.label().length() > 0 ? t.label()
                        : action, "PosChoice");
                b.setShouldLocalize(false);
                b.setName("pos-grid-" + iter);
                b.addActionListener(e -> action(action));
                shown.add(b);
                continue;
            }
            // A PRODUCT tile names one variant.
            List<PosItemDto> variants = new ArrayList<PosItemDto>();
            for (int v = 0; v < c.items().size(); v++) {
                PosItemDto it = c.items().get(v);
                if (ref.equals(it.variantId())) {
                    variants.add(it);
                }
            }
            if (!variants.isEmpty()) {
                shown.add(tile(variants, side));
            }
        }
        grid.setLayout(new GridLayout(Math.max(1, (shown.size() + cols - 1) / cols), cols));
        for (int iter = 0; iter < shown.size(); iter++) {
            grid.add(shown.get(iter));
        }
        if (shown.isEmpty()) {
            grid.setLayout(new BorderLayout());
            grid.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_APPS, "Nothing on the grid",
                    "The grid's items are not in this register's catalog yet."));
        }
        grid.revalidateLater();
    }

    /// What an ACTION tile of the smart grid does.
    private void action(String action) {
        if ("CUSTOM_ITEM".equals(action)) {
            customItem();
        } else if ("PARK".equals(action)) {
            park();
        } else if ("PARKED".equals(action)) {
            PosSections.show(PosSections.PARKED);
        } else if ("LAYAWAY".equals(action)) {
            PosSections.show(PosSections.LAYAWAYS);
        } else if ("CUSTOMER".equals(action)) {
            customer();
        } else if ("DISCOUNT".equals(action)) {
            orderDiscount();
        }
    }

    private Component tile(final List<PosItemDto> variants, int side) {
        PosItemDto first = variants.get(0);
        Container tile = new Container(BoxLayout.y());
        tile.setUIID("PosTile");
        Label art = ProductArt.label(first.productId(), first.title(), "", first.photoId(), side,
                side, "PosTileArt");
        tile.add(art);
        tile.add(PosUi.data(first.title(), "PosTileTitle"));
        long min = first.priceCents();
        long max = first.priceCents();
        int stock = 0;
        boolean tracked = false;
        for (int iter = 0; iter < variants.size(); iter++) {
            PosItemDto v = variants.get(iter);
            min = Math.min(min, v.priceCents());
            max = Math.max(max, v.priceCents());
            if (v.trackInventory()) {
                tracked = true;
                stock += v.available();
            }
        }
        tile.add(PosUi.data(Money.range(min, max, PosState.currency(), "USD"), "PosTilePrice"));
        if (tracked) {
            tile.add(PosUi.data(stock <= 0 ? Lang.tr("Out of stock") : Lang.tr("{0} left", stock),
                    stock <= 3 ? "PosTileLow" : "PosTileStock"));
        }
        Button lead = new Button();
        lead.setName("pos-tile-" + first.productId());
        lead.addActionListener(e -> {
            if (variants.size() == 1) {
                add(variants.get(0), 1);
            } else {
                pickVariant(variants);
            }
        });
        tile.add(lead);
        lead.setHidden(true);
        tile.setLeadComponent(lead);
        return tile;
    }

    private void pickVariant(List<PosItemDto> variants) {
        final Sheet sheet = PosSheet.open(variants.get(0).title());
        sheet.setName("pos-variants");
        Container content = sheet.getContentPane();
        content.setLayout(new GridLayout((variants.size() + 1) / 2, 2));
        for (int iter = 0; iter < variants.size(); iter++) {
            final PosItemDto v = variants.get(iter);
            String stock = v.trackInventory() ? "  (" + v.available() + ")" : "";
            Button b = new Button(v.variantTitle() + "  " + PosState.money(v.priceCents()) + stock,
                    "PosChoice");
            b.setShouldLocalize(false);
            b.setName("pos-variant-" + iter);
            b.addActionListener(e -> {
                sheet.back();
                add(v, 1);
            });
            content.add(b);
        }
        sheet.show();
    }

    private void add(PosItemDto item, int quantity) {
        PosState.cart().add(item, quantity);
        PosState.fireCart();
        refocus();
    }

    private void refocus() {
        if (PosUi.holdFocus()) {
            scan.requestFocus();
        }
    }

    // ---------------------------------------------------------------- scan

    static boolean cameraScan() {
        return !CN.isDesktop() && CodeScanner.getInstance() != null;
    }

    private void camera() {
        CodeScanner.getInstance().scanBarCode(new ScanResult() {
            @Override
            public void scanCompleted(String contents, String formatName, byte[] rawBytes) {
                CN.callSerially(() -> {
                    scan.setText(contents == null ? "" : contents);
                    entered();
                });
            }

            @Override
            public void scanCanceled() {
            }

            @Override
            public void scanError(int errorCode, String message) {
                CN.callSerially(() -> Ui.say("The code could not be read"));
            }
        });
    }

    /// Enter in the scan field: pay when it is empty, otherwise a code.
    private void entered() {
        String text = scan.getText();
        Wedge.Scan s = Wedge.parse(text);
        if (s == null) {
            if (!PosState.cart().isEmpty()) {
                pay();
            }
            return;
        }
        PosCatalogDto c = PosState.catalog();
        PosItemDto found = Wedge.find(c == null ? null : c.items(), s.code);
        if (found != null) {
            scan.setText("");
            query = "";
            add(found, s.quantity);
            fillGrid();
            return;
        }
        if (Wedge.orderNumber(text) > 0) {
            scan.setText("");
            ReturnsPane.lookUpLater(Wedge.orderNumber(text));
            PosSections.show(PosSections.RETURNS);
            return;
        }
        if (Wedge.looksLikeCode(text)) {
            scan.setText("");
            Ui.say(Lang.tr("No item has the code {0}", s.code));
            CN.vibrate(200);
        } else {
            query = text;
            fillGrid();
        }
        refocus();
    }

    // ---------------------------------------------------------------- cart

    private Container cartPanel() {
        Container panel = new Container(new BorderLayout());
        panel.setUIID("PosCart");
        Container head = new Container(BoxLayout.x());
        head.setUIID("PosCartHead");
        head.add(PosUi.icon(FontImage.MATERIAL_PERSON_ADD, "Customer", "pos-customer", "PosTool",
                e -> customer()));
        head.add(PosUi.icon(FontImage.MATERIAL_LOCAL_OFFER, "Discount", "pos-discount", "PosTool",
                e -> orderDiscount()));
        head.add(PosUi.icon(FontImage.MATERIAL_DELETE_SWEEP, "Clear", "pos-clear", "PosTool",
                e -> clearSale()));
        head.add(PosUi.icon(FontImage.MATERIAL_POST_ADD, "Custom", "pos-custom", "PosTool",
                e -> customItem()));
        head.add(PosUi.icon(FontImage.MATERIAL_PAUSE_CIRCLE, "Park", "pos-park", "PosTool",
                e -> park()));
        head.add(PosUi.icon(FontImage.MATERIAL_SELL, "Labels", "pos-labels", "PosTool",
                e -> labels()));
        head.setScrollableX(true);
        panel.add(BorderLayout.NORTH, head);
        cartBox.setScrollableY(true);
        cartBox.setName("pos-cart-lines");
        panel.add(BorderLayout.CENTER, cartBox);
        Container south = new Container(BoxLayout.y());
        south.add(totalsBox);
        if (PosUi.wide()) {
            south.add(pay);
        }
        panel.add(BorderLayout.SOUTH, south);
        return panel;
    }

    private void fillCart() {
        PosCart cart = PosState.cart();
        cartBox.removeAll();
        if (cart.customer() != null) {
            PosCustomerDto cu = cart.customer();
            Container row = new Container(new BorderLayout());
            row.setUIID("PosCustomerRow");
            row.add(BorderLayout.CENTER, PosUi.data(name(cu)
                    + (cu.storeCreditCents() > 0 ? Ui.DOT + Lang.tr("Credit {0}",
                    PosState.money(cu.storeCreditCents())) : ""), "PosCartTitle"));
            row.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CLOSE, "pos-customer-remove", e -> {
                cart.setCustomer(null);
                PosState.fireCart();
            }));
            cartBox.add(row);
        }
        if (cart.isEmpty()) {
            cartBox.add(Ui.empty(FontImage.MATERIAL_SHOPPING_CART, "No items yet",
                    "Tap a product or scan a barcode."));
        }
        long[] units = cart.unitPrices();
        for (int iter = 0; iter < cart.items().size(); iter++) {
            cartBox.add(line(iter, cart.items().get(iter), units[iter]));
        }
        totalsBox.removeAll();
        PosPricing.Totals t = PosState.totals();
        totalsBox.setUIID("PosTotals");
        long registerOff = cart.items().isEmpty() ? 0 : registerDiscount(cart, units);
        total("Subtotal", t.subtotalCents + registerOff, "PosTotalRow");
        if (registerOff > 0) {
            total("Register discount", -registerOff, "PosTotalRow");
        }
        if (t.discountCents > 0) {
            String names = t.applied.isEmpty() ? Lang.tr("Discounts") : join(t.applied);
            totalRow(names, -t.discountCents, "PosTotalRow");
        }
        if (t.codeProblem != null && cart.code().length() > 0) {
            totalsBox.add(PosUi.label(t.codeProblem, "PosError"));
        }
        if (t.taxCents > 0) {
            total(t.taxInclusive ? "Tax included" : "Tax", t.taxInclusive ? t.taxCents
                    : t.taxCents, "PosTotalRow");
        }
        total("Total", t.totalCents, "PosGrand");
        pay.setText(cart.isEmpty() ? Lang.tr("Charge")
                : Lang.tr("Charge {0}", PosState.money(t.totalCents)));
        pay.setEnabled(!cart.isEmpty());
        root.revalidateLater();
        if (cartSheetContent != null) {
            cartSheetContent.revalidateLater();
        }
    }

    /// What the register's own discounts took off, lines and sale.
    private static long registerDiscount(PosCart cart, long[] units) {
        long off = 0;
        for (int iter = 0; iter < cart.items().size(); iter++) {
            PosCart.Item i = cart.items().get(iter);
            off += (i.item.priceCents() - units[iter]) * i.quantity;
        }
        return Math.max(0, off);
    }

    private void total(String caption, long cents, String uiid) {
        totalRow(Lang.tr(caption), cents, uiid);
    }

    private void totalRow(String caption, long cents, String uiid) {
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid);
        row.add(BorderLayout.WEST, PosUi.data(caption, uiid + "Label"));
        row.add(BorderLayout.EAST, PosUi.data(PosState.money(cents), uiid + "Value"));
        totalsBox.add(row);
    }

    private static String join(List<String> names) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < names.size(); iter++) {
            b.append(iter == 0 ? "" : ", ").append(names.get(iter));
        }
        return b.toString();
    }

    static String name(PosCustomerDto c) {
        String n = ((c.firstName() == null ? "" : c.firstName()) + " "
                + (c.lastName() == null ? "" : c.lastName())).trim();
        return n.length() > 0 ? n : c.email() != null && c.email().length() > 0 ? c.email()
                : c.phone();
    }

    private Component line(final int index, final PosCart.Item item, long unit) {
        Container row = new Container(new BorderLayout());
        row.setUIID("PosCartRow");
        row.setName("pos-line-" + index);
        Container text = new Container(BoxLayout.y());
        text.add(PosUi.data(item.item.title(), "PosCartTitle"));
        String detail = item.item.variantTitle() == null || "Default".equals(item.item.variantTitle())
                ? "" : item.item.variantTitle() + Ui.DOT;
        detail += PosState.money(unit);
        if (unit != item.item.priceCents()) {
            detail += "  (" + PosState.money(item.item.priceCents()) + ")";
        }
        text.add(PosUi.data(detail, unit != item.item.priceCents() ? "PosCartDeal" : "PosCartDetail"));
        Button lead = new Button();
        lead.setName("pos-line-edit-" + index);
        lead.addActionListener(e -> editLine(index));
        text.add(lead);
        lead.setHidden(true);
        text.setLeadComponent(lead);
        row.add(BorderLayout.CENTER, text);
        Container step = new Container(BoxLayout.x());
        step.setUIID("PosStepper");
        step.add(PosUi.icon(FontImage.MATERIAL_REMOVE, "", "pos-minus-" + index, "PosStep", e -> {
            PosState.cart().setQuantity(index, item.quantity - 1);
            PosState.fireCart();
        }));
        step.add(PosUi.data(String.valueOf(item.quantity), "PosQty"));
        step.add(PosUi.icon(FontImage.MATERIAL_ADD, "", "pos-plus-" + index, "PosStep", e -> {
            PosState.cart().setQuantity(index, item.quantity + 1);
            PosState.fireCart();
        }));
        step.add(PosUi.data(PosState.money(unit * item.quantity), "PosLineTotal"));
        row.add(BorderLayout.EAST, step);
        return row;
    }

    // ---------------------------------------------------------------- edits

    /// Asks for an amount on a till pad; `got` gets the cents (or basis
    /// points when `percent`).
    interface Amount {
        void got(long value);
    }

    static void askAmount(String title, final boolean percent, final Amount got) {
        final Sheet sheet = PosSheet.open(Lang.tr(title));
        Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        final long[] value = {0};
        final Label shown = PosUi.data(percent ? "0%" : PosState.money(0), "PosAmount");
        c.add(shown);
        c.add(PosUi.pad("pos-amount", percent ? "" : "00", k -> {
            if (k == '\n') {
                value[0] = PosUi.typed(PosUi.typed(value[0], '0'), '0');
            } else {
                value[0] = PosUi.typed(value[0], k);
            }
            if (percent) {
                value[0] = Math.min(value[0], 100);
            }
            shown.setText(percent ? value[0] + "%" : PosState.money(value[0]));
            shown.getParent().revalidateLater();
        }));
        c.add(PosUi.key(Lang.tr("Apply"), "pos-amount-apply", "PosPay", e -> {
            sheet.back();
            got.got(percent ? value[0] * 100 : value[0]);
        }));
        sheet.show();
    }

    private void editLine(final int index) {
        final PosCart cart = PosState.cart();
        if (index >= cart.items().size()) {
            return;
        }
        PosCart.Item item = cart.items().get(index);
        final Sheet sheet = PosSheet.open(item.item.title());
        sheet.setName("pos-line-sheet");
        Container c = sheet.getContentPane();
        c.setLayout(new GridLayout(3, 2));
        c.add(PosUi.icon(FontImage.MATERIAL_PERCENT, "Percent off", "pos-line-percent", "PosChoice",
                e -> {
                    sheet.back();
                    askAmount("Percent off each", true, v -> {
                        cart.linePercent(index, v);
                        PosState.fireCart();
                    });
                }));
        c.add(PosUi.icon(FontImage.MATERIAL_MONEY_OFF, "Amount off", "pos-line-amount", "PosChoice",
                e -> {
                    sheet.back();
                    askAmount("Amount off each", false, v -> {
                        cart.lineAmount(index, v);
                        PosState.fireCart();
                    });
                }));
        c.add(PosUi.icon(FontImage.MATERIAL_EDIT, "Set price", "pos-line-price", "PosChoice", e -> {
            sheet.back();
            askAmount("Price each", false, v -> {
                cart.linePrice(index, v);
                PosState.fireCart();
            });
        }));
        c.add(PosUi.icon(FontImage.MATERIAL_RESTART_ALT, "No discount", "pos-line-reset",
                "PosChoice", e -> {
                    sheet.back();
                    cart.linePrice(index, -1);
                    cart.linePercent(index, 0);
                    PosState.fireCart();
                }));
        c.add(PosUi.icon(FontImage.MATERIAL_DELETE, "Remove", "pos-line-remove", "PosChoice", e -> {
            sheet.back();
            cart.remove(index);
            PosState.fireCart();
        }));
        sheet.show();
    }

    private void orderDiscount() {
        final PosCart cart = PosState.cart();
        final Sheet sheet = PosSheet.open(Lang.tr("Discount"));
        sheet.setName("pos-order-discount");
        Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        Container row = new Container(new GridLayout(1, 3));
        row.add(PosUi.key(Lang.tr("Percent off"), "pos-order-percent", "PosChoice", e -> {
            sheet.back();
            askAmount("Percent off the sale", true, v -> {
                cart.orderPercent(v);
                PosState.fireCart();
            });
        }));
        row.add(PosUi.key(Lang.tr("Amount off"), "pos-order-amount", "PosChoice", e -> {
            sheet.back();
            askAmount("Amount off the sale", false, v -> {
                cart.orderAmount(v);
                PosState.fireCart();
            });
        }));
        row.add(PosUi.key(Lang.tr("None"), "pos-order-none", "PosChoice", e -> {
            sheet.back();
            cart.orderAmount(0);
            cart.setCode("");
            PosState.fireCart();
        }));
        c.add(row);
        c.add(PosUi.label("Discount code", "PosMuted"));
        final TextField code = new TextField(cart.code(), Lang.tr("Code"), 16, TextField.ANY);
        code.setUIID("PosField");
        code.setName("pos-code");
        code.setDoneListener(e -> {
            sheet.back();
            cart.setCode(code.getText());
            PosState.fireCart();
        });
        c.add(code);
        PosCatalogDto cat = PosState.catalog();
        Container codes = new Container(new FlowLayout());
        for (int iter = 0; cat != null && cat.discounts() != null
                && iter < cat.discounts().size(); iter++) {
            final DiscountDto d = cat.discounts().get(iter);
            if (d.automatic() || d.code() == null || d.code().length() == 0 || !d.active()) {
                continue;
            }
            Button b = new Button(d.code(), "PosChip");
            b.setShouldLocalize(false);
            b.setName("pos-code-" + d.code());
            b.addActionListener(e -> {
                sheet.back();
                cart.setCode(d.code());
                PosState.fireCart();
            });
            codes.add(b);
        }
        c.add(codes);
        sheet.show();
    }

    private void customer() {
        final Sheet sheet = PosSheet.open(Lang.tr("Customer"));
        sheet.setName("pos-customer-sheet");
        Container c = sheet.getContentPane();
        c.setLayout(new BorderLayout());
        final TextField q = new TextField("", Lang.tr("Name, e-mail or phone"), 20, TextField.ANY);
        q.setUIID("PosField");
        q.setName("pos-customer-q");
        final Container results = new Container(BoxLayout.y());
        results.setScrollableY(true);
        q.setDoneListener(e -> {
            results.removeAll();
            String text = q.getText().trim();
            if (text.length() == 0) {
                return;
            }
            Api.pos().customers(PosState.storeId(), text, Net.to(list -> {
                results.removeAll();
                for (int iter = 0; list != null && iter < list.size(); iter++) {
                    final PosCustomerDto cu = list.get(iter);
                    Button b = new Button(name(cu) + Ui.DOT + PosState.money(cu.spentCents()),
                            "PosChoice");
                    b.setShouldLocalize(false);
                    b.setName("pos-customer-" + iter);
                    b.addActionListener(ev -> {
                        sheet.back();
                        PosState.cart().setCustomer(cu);
                        PosState.fireCart();
                    });
                    results.add(b);
                }
                if (list == null || list.isEmpty()) {
                    results.add(PosUi.label("No customer matches", "PosMuted"));
                }
                results.revalidateLater();
            }, (status, message) -> Ui.fail(status, status <= 0
                    ? "Finding a customer needs the network" : message)));
        });
        c.add(BorderLayout.NORTH, q);
        c.add(BorderLayout.CENTER, results);
        sheet.show();
        q.startEditingAsync();
    }

    private void clearSale() {
        if (PosState.cart().isEmpty()) {
            return;
        }
        PosSheet.confirm(Lang.tr("Clear sale"), Lang.tr("Remove every item from this sale?"),
                Lang.tr("Clear"), Lang.tr("Keep"), "pos-clear", true, () -> {
                    PosState.cart().clear();
                    PosState.fireCart();
                    refocus();
                });
    }

    private void pay() {
        if (PosState.cart().isEmpty()) {
            return;
        }
        if (!PosUi.wide() && (cartSheet == null || Sheet.getCurrentSheet() != cartSheet)) {
            // On a phone the cart is behind the bar: show it with Pay in it.
            cartSheet = PosSheet.open(Lang.tr("Cart"));
            cartSheet.setName("pos-cart-sheet");
            Container content = cartSheet.getContentPane();
            content.setLayout(new BorderLayout());
            if (cartSheetContent.getParent() != null) {
                cartSheetContent.remove();
            }
            content.add(BorderLayout.CENTER, cartSheetContent);
            content.add(BorderLayout.SOUTH, PosUi.key(pay.getText(), "pos-tender", "PosPay",
                    e -> {
                        cartSheet.back();
                        tender();
                    }));
            cartSheet.show();
            return;
        }
        tender();
    }

    /// Opens the tender sheet, first asking a manager when the store's
    /// policy says this sale's discounts or changed prices need one.
    static void tender() {
        final PosCart cart = PosState.cart();
        if (cart.approvalId().length() == 0) {
            List<PosLineDto> lines = cart.uploadLines();
            long[] list = new long[cart.items().size()];
            for (int iter = 0; iter < list.length; iter++) {
                // A custom line has no list price but the one it goes up at,
                // which is what the server compares it with too.
                list[iter] = PosCart.custom(cart.items().get(iter).item)
                        ? lines.get(iter).unitCents() : cart.items().get(iter).item.priceCents();
            }
            PosPricing.Totals t = PosState.totals();
            String kind = PosRules.saleApproval(PosState.policy(), PosState.role(),
                    lines, list, t.discountCents);
            if (kind != null) {
                ManagerApproval.ask(kind, t.totalCents, "Sale", id -> {
                    cart.setApprovalId(id);
                    TenderSheet.show();
                });
                return;
            }
        }
        TenderSheet.show();
    }

    // ---------------------------------------------------------------- tools
    /// A sale line of a typed title and price, for something not in the catalog.
    private void customItem() {
        final Sheet sheet = PosSheet.open(Lang.tr("Custom item"));
        sheet.setName("pos-custom-sheet");
        Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        final TextField title = new TextField("", Lang.tr("What is it?"), 20, TextField.ANY);
        title.setUIID("PosField");
        title.setName("pos-custom-title");
        final boolean[] taxable = {true};
        c.add(title);
        c.add(Ui.toggle("pos-custom-taxable", true, v -> taxable[0] = v));
        c.add(PosUi.key(Lang.tr("Price"), "pos-custom-price", "PosPay", e -> {
            final String t = title.getText().trim();
            if (t.length() == 0) {
                Ui.say("Name the item first");
                return;
            }
            sheet.back();
            askAmount("Price", false, cents -> {
                if (cents > 0) {
                    add(PosCart.customItem(t, cents, taxable[0]), 1);
                }
            });
        }));
        sheet.show();
        title.startEditingAsync();
    }

    /// Price labels for the catalog items in the cart, one per unit, as a
    /// PDF to print or share.
    private void labels() {
        PosCart cart = PosState.cart();
        StringBuilder ids = new StringBuilder();
        for (int iter = 0; iter < cart.items().size(); iter++) {
            PosItemDto it = cart.items().get(iter).item;
            if (!PosCart.custom(it) && ids.indexOf(it.variantId()) < 0) {
                ids.append(ids.length() > 0 ? "," : "").append(it.variantId());
            }
        }
        if (ids.length() == 0) {
            Ui.say("Add the items to label to the sale first");
            return;
        }
        Pdfs.open(Lang.tr("Price labels"), "labels.pdf", "/api/merchant/stores/"
                + PosState.storeId() + "/pos/labels.pdf?variants=" + ids + "&copies=1");
    }

    /// Puts the sale aside under a label, to ring up the next customer.
    private void park() {
        if (PosState.cart().isEmpty()) {
            return;
        }
        ParkedPane.parkCurrent();
        refocus();
    }
}
