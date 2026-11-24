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
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.GridTileDto;
import com.codenameone.examples.bazaar.api.PosGridDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
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
    /// The width the tiles were last laid out for; when the grid turns out to
    /// be another width (a side menu took part of the screen, the window
    /// resized) it is filled again for the width it really has.
    private int filledWidth;
    private final Container grid = new Container() {
        @Override
        protected void laidOut() {
            super.laidOut();
            int w = getWidth();
            if (w > 0 && filledWidth > 0 && w != filledWidth) {
                filledWidth = w;
                CN.callSerially(() -> fillGrid());
            }
        }
    };
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
        scan.setUIID("PosScanField");
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
        // The field sits in a rounded box with a search glyph ahead of it,
        // so the hint never touches the border.
        Container field = new Container(new BorderLayout());
        field.setUIID("PosScan");
        Label glass = new Label("", "PosScanIcon");
        FontImage.setMaterialIcon(glass, FontImage.MATERIAL_SEARCH, 4f);
        field.add(BorderLayout.WEST, glass);
        field.add(BorderLayout.CENTER, scan);
        top.add(BorderLayout.CENTER, field);
        // Centred on the row, so the status pill keeps its own height
        // instead of stretching to the search field's.
        Container tools = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
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
        chips.setScrollVisible(false);
        chips.setUIID("PosChips");
        Container north = new Container(BoxLayout.y());
        north.add(top).add(chips);
        products.add(BorderLayout.NORTH, north);
        grid.setScrollableY(true);
        grid.setScrollVisible(false);
        grid.setUIID("PosGrid");
        products.add(BorderLayout.CENTER, grid);

        Container cart = cartPanel();
        if (PosUi.wide()) {
            root.add(BorderLayout.CENTER, products);
            cart.setPreferredW(cartWidth());
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
        int width = gridWidth();
        int cols = columns(width, c);
        grid.setLayout(new GridLayout(Math.max(1, (products.size() + cols - 1) / cols), cols));
        int side = tileSide(width, cols);
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
        int width = gridWidth();
        int cols = columns(width, c);
        int side = tileSide(width, cols);
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

    /// The grid's own width once it has been laid out; before that, the
    /// screen less the cart, which the first layout then corrects.
    private int gridWidth() {
        int w = grid.getWidth();
        if (w <= 0) {
            w = PosUi.wide() ? CN.getDisplayWidth() - cartWidth() : CN.getDisplayWidth();
        }
        filledWidth = w;
        return w;
    }

    /// As many columns as tiles fit, where a tile is never narrower than its
    /// longest word and its widest price: no word loses letters and the
    /// price is never cut; a long name wraps to two lines (`fitTwoLines`).
    static int columns(int width, PosCatalogDto c) {
        int inner = width - CN.convertToPixels(3);
        int tile = Math.max(CN.convertToPixels(20), minTileWidth(c));
        return Math.max(1, Math.min(6, inner / Math.max(1, tile)));
    }

    /// The narrowest a tile can be: the widest word of any title and the
    /// widest price, plus the text block's padding and the tile's margin and
    /// border.
    static int minTileWidth(PosCatalogDto c) {
        UIManager m = UIManager.getInstance();
        Style title = m.getComponentStyle("PosTileTitle");
        Style price = m.getComponentStyle("PosTilePrice");
        Font tf = title.getFont();
        Font pf = price.getFont();
        int text = 0;
        int money = 0;
        List<PosItemDto> items = c == null ? null : c.items();
        if (items != null) {
            for (int iter = 0; iter < items.size(); iter++) {
                PosItemDto it = items.get(iter);
                text = Math.max(text, widestWord(twoLines(it.title()), tf));
                money = Math.max(money, pf.stringWidth(Lang.tr("From {0}", PosState.money(it.priceCents()))));
            }
        }
        Style body = m.getComponentStyle("PosTileBody");
        Style tileStyle = m.getComponentStyle("PosTile");
        int chrome = body.getHorizontalPadding() + tileStyle.getHorizontalMargins() + 2;
        return Math.max(text + title.getHorizontalPadding(), money + price.getHorizontalPadding()) + chrome;
    }

    /// The name broken between words into at most two lines of `width`, the
    /// second ending in "..." when the rest does not fit. Broken here, where
    /// the tile's width is known, so a name is never cut off by the label.
    static String fitTwoLines(String s, Font f, int width) {
        StringBuilder[] lines = {new StringBuilder(), new StringBuilder()};
        int line = 0;
        int start = 0;
        int len = s.length();
        boolean rest = false;
        for (int iter = 0; iter <= len; iter++) {
            if (iter < len && s.charAt(iter) != ' ') {
                continue;
            }
            if (iter > start) {
                String word = s.substring(start, iter);
                StringBuilder b = lines[line];
                String joined = b.length() == 0 ? word : b + " " + word;
                if (f.stringWidth(joined) <= width || b.length() == 0) {
                    b.setLength(0);
                    b.append(joined);
                } else if (line == 0) {
                    line = 1;
                    lines[1].append(word);
                } else {
                    rest = true;
                    break;
                }
            }
            start = iter + 1;
        }
        if (rest) {
            StringBuilder b = lines[1];
            while (b.length() > 0 && f.stringWidth(b + "...") > width) {
                b.setLength(b.length() - 1);
            }
            while (b.length() > 0 && b.charAt(b.length() - 1) == ' ') {
                b.setLength(b.length() - 1);
            }
            b.append("...");
        }
        return lines[1].length() == 0 ? lines[0].toString() : lines[0] + "\n" + lines[1];
    }

    /// The width of the longest run of characters between spaces.
    static int widestWord(String s, Font f) {
        int widest = 0;
        int start = 0;
        int len = s.length();
        for (int iter = 0; iter <= len; iter++) {
            if (iter == len || s.charAt(iter) == ' ') {
                if (iter > start) {
                    widest = Math.max(widest, f.stringWidth(s.substring(start, iter)));
                }
                start = iter + 1;
            }
        }
        return widest;
    }

    /// The width of a tile's photo: the grid's share per column, less the
    /// grid's padding and each tile's border and margin.
    private static int tileSide(int width, int cols) {
        int inner = width - CN.convertToPixels(3);
        return Math.max(CN.convertToPixels(18), inner / Math.max(1, cols) - CN.convertToPixels(2) - 2);
    }

    /// A product name cut to what two tile lines hold.
    static String twoLines(String title) {
        String t = title == null ? "" : title;
        int max = 34;
        if (t.length() <= max) {
            return t;
        }
        int cut = t.lastIndexOf(' ', max - 1);
        return t.substring(0, cut > 12 ? cut : max - 1) + "...";
    }

    private Component tile(final List<PosItemDto> variants, int side) {
        PosItemDto first = variants.get(0);
        Container tile = new Container(BoxLayout.y());
        tile.setUIID("PosTile");
        // The photo runs edge to edge across the card's top, 4:3, with the
        // text in a padded block below it.
        Label art = TileArt.create(first.productId(), first.title(), first.photoId(), side,
                side * 3 / 4);
        tile.add(art);
        Container text = new Container(BoxLayout.y());
        text.setUIID("PosTileBody");
        Style ts = UIManager.getInstance().getComponentStyle("PosTileTitle");
        int textW = side - ts.getHorizontalPadding()
                - UIManager.getInstance().getComponentStyle("PosTileBody").getHorizontalPadding();
        SpanLabel name = new SpanLabel(fitTwoLines(twoLines(first.title()), ts.getFont(), textW));
        name.setTextUIID("PosTileTitle");
        name.setUIID("Container");
        // Always two lines tall: a grid row is as tall as its first tile, so
        // a one-line title beside a two-line one used to cut off the price.
        name.getTextComponent().setRows(2);
        name.getTextComponent().setGrowByContent(false);
        text.add(name);
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
        // A range never fits a tile; the lowest price says enough.
        String price = min == max ? PosState.money(min) : Lang.tr("From {0}", PosState.money(min));
        text.add(PosUi.data(price, "PosTilePrice"));
        tile.add(text);
        if (tracked) {
            text.add(PosUi.data(stock <= 0 ? Lang.tr("Out of stock") : Lang.tr("{0} left", stock),
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
        com.codenameone.examples.bazaar.ui.Ui.accessible(tile, lead);
        return tile;
    }

    private void pickVariant(List<PosItemDto> variants) {
        final Sheet sheet = PosSheet.open(variants.get(0).title());
        sheet.setName("pos-variants");
        Container content = sheet.getContentPane();
        content.setLayout(new GridLayout((variants.size() + 1) / 2, 2));
        for (int iter = 0; iter < variants.size(); iter++) {
            final PosItemDto v = variants.get(iter);
            // Two lines: the option on top, the price and stock under it, so
            // "M / Charcoal" is never cut off to make room for the price.
            Container card = new Container(BoxLayout.y());
            card.setUIID("PosChoice");
            Label name = PosUi.data(v.variantTitle(), "PosChoiceTitle");
            name.setAutoSizeMode(true);
            card.add(name);
            String meta = PosState.money(v.priceCents());
            if (v.trackInventory()) {
                meta += Ui.DOT + Lang.tr("{0} left", v.available());
            }
            card.add(PosUi.data(meta, "PosChoiceMeta"));
            Button b = new Button();
            b.setName("pos-variant-" + iter);
            b.addActionListener(e -> {
                sheet.back();
                add(v, 1);
            });
            card.add(b);
            b.setHidden(true);
            card.setLeadComponent(b);
            com.codenameone.examples.bazaar.ui.Ui.accessible(card, b);
            content.add(card);
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
        // One even row of icon-over-label tools, so all six fit a phone
        // sheet instead of the last ones scrolling off the edge.
        Container head = new Container(new GridLayout(1, 6));
        head.setUIID("PosCartHead");
        head.add(PosUi.icon(FontImage.MATERIAL_PERSON_ADD, "Customer", "pos-customer", "PosCartTool",
                e -> customer()));
        head.add(PosUi.icon(FontImage.MATERIAL_LOCAL_OFFER, "Discount", "pos-discount", "PosCartTool",
                e -> orderDiscount()));
        head.add(PosUi.icon(FontImage.MATERIAL_DELETE_SWEEP, "Clear", "pos-clear", "PosCartTool",
                e -> clearSale()));
        head.add(PosUi.icon(FontImage.MATERIAL_POST_ADD, "Custom", "pos-custom", "PosCartTool",
                e -> customItem()));
        head.add(PosUi.icon(FontImage.MATERIAL_PAUSE_CIRCLE, "Park", "pos-park", "PosCartTool",
                e -> park()));
        head.add(PosUi.icon(FontImage.MATERIAL_PRINT, "Labels", "pos-labels", "PosCartTool",
                e -> labels()));
        for (int iter = 0; iter < head.getComponentCount(); iter++) {
            ((Button) head.getComponentAt(iter)).setTextPosition(Component.BOTTOM);
        }
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
        // An empty sale reads as a quiet hint, not a dead grey slab; a full
        // one says what it will charge and for how many items.
        if (cart.isEmpty()) {
            pay.setText(Lang.tr("Add items to charge"));
            pay.setUIID("PosPayIdle");
        } else {
            int count = cart.units();
            pay.setText(Lang.tr("Charge {0}", PosState.money(t.totalCents)) + Ui.DOT
                    + (count == 1 ? Lang.tr("1 item") : Lang.tr("{0} items", count)));
            pay.setUIID("PosPay");
        }
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
        // CENTER, not WEST: a long promotion name ends in "..." before the
        // amount instead of running underneath it.
        row.add(BorderLayout.CENTER, PosUi.data(caption, uiid + "Label"));
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
        // The name gets the full width on top, with the line total beside it;
        // the stepper sits under it, so a phone sheet never cuts the name.
        Container row = new Container(BoxLayout.y());
        row.setUIID("PosCartRow");
        Container top = new Container(new BorderLayout());
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
        com.codenameone.examples.bazaar.ui.Ui.accessible(text, lead);
        top.add(BorderLayout.CENTER, text);
        top.add(BorderLayout.EAST, PosUi.data(PosState.money(unit * item.quantity), "PosLineTotal"));
        row.add(top);
        Container step = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
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
        row.add(step);
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
        // A sheet takes its height when it opens, and an empty list made it
        // just the field: the matches that came back after had no room and
        // could not be seen or tapped. The list reserves room up front.
        final Container results = new Container(BoxLayout.y()) {
            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                com.codename1.ui.geom.Dimension d = super.calcPreferredSize();
                d.setHeight(CN.getDisplayHeight() * 2 / 5);
                return d;
            }
        };
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
            // The tool row sits right under the title's rule: the sheet's own
            // top padding left an empty band between them.
            content.getAllStyles().setPaddingTop(0);
            content.getAllStyles().setMarginTop(0);
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
            PosPricing.Totals t = PosState.totals();
            String kind = PosRules.saleApproval(PosState.policy(), PosState.role(),
                    lines, cart.listUnits(), t.discountCents - t.automaticCents);
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

    /// The cart panel's width beside the grid: two fifths of what the sidebar
    /// leaves, at least 90mm where that is wide and 64mm on a tablet, where
    /// 90mm beside the sidebar left the grid a single column.
    private static int cartWidth() {
        Shell shell = Shell.current();
        int beside = CN.getDisplayWidth() - (shell == null ? 0 : shell.sidebarWidth());
        int min = CN.convertToPixels(beside >= CN.convertToPixels(220) ? 90 : 64);
        return Math.max(min, beside * 2 / 5);
    }
}
