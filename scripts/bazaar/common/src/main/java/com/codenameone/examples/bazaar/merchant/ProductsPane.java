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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;
import com.codenameone.examples.bazaar.api.BulkEditDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The catalog. On a wide screen a data grid: sortable columns, a search and a
/// status filter, several rows selected at once for bulk changes, and price
/// and status edited right in their cells. On a phone a searchable list. Rows
/// open the product editor; CSV export and import go through the File menu.
final class ProductsPane extends MerchantPane {
    private static final String ALL = "ALL";
    private static final String[] FILTER = {ALL, "ACTIVE", "DRAFT", "ARCHIVED"};

    private final Rows<ProductCardDto> rows = new Rows<ProductCardDto>();
    private TextField search;
    private Picker status;
    private DataGrid grid;
    private Container list;
    private Container bulk;
    private Label selection;

    ProductsPane() {
        super("Products", "merchant-products");
        rows.col(Lang.tr("Product"), p -> p.title())
                .col(Lang.tr("Status"), p -> p.status(), (p, v) -> status(p, v))
                .col(Lang.tr("Type"), p -> p.productType())
                .col(Lang.tr("Vendor"), p -> p.vendor())
                .col(Lang.tr("Inventory"), p -> Integer.valueOf(p.available()))
                .col(Lang.tr("Variants"), p -> Integer.valueOf(p.variantCount()))
                .col(Lang.tr("Price"), p -> Long.valueOf(p.minPriceCents()), (p, v) -> price(p, v));
    }

    @Override
    void actions() {
        DesktopActions.set(DesktopActions.NEW_PRODUCT, () -> edit(null));
        DesktopActions.set(DesktopActions.EXPORT_CSV, this::export);
        DesktopActions.set(DesktopActions.IMPORT_CSV, this::importCsv);
    }

    @Override
    void find() {
        if (search != null) {
            search.startEditingAsync();
        }
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add product", "product-add", () -> edit(null));
        tool(FontImage.MATERIAL_UPLOAD_FILE, "Import", "product-import", this::importCsv);
        tool(FontImage.MATERIAL_DOWNLOAD, "Export", "product-export", this::export);
        if (search == null) {
            build();
        }
        reload();
    }

    private void build() {
        search = MUi.search("products-search", this::reload);
        status = MUi.picker(null, null, "products-status", FILTER, ALL);
        String[] labels = {Lang.tr("All"), Lang.tr("Active"), Lang.tr("Draft"), Lang.tr("Archived")};
        status.setStrings(labels);
        status.setSelectedStringIndex(0);
        status.setUIID("MeFilter");
        status.addActionListener(e -> reload());
        Container top = new Container(new BorderLayout());
        top.setUIID("MeFilters");
        top.add(BorderLayout.CENTER, search);
        top.add(BorderLayout.EAST, status);
        Container all = new Container(new BorderLayout());
        Container north = new Container(BoxLayout.y());
        north.add(top);
        all.add(BorderLayout.NORTH, north);
        if (wide) {
            bulk = new Container(new FlowLayout());
            bulk.setUIID("MeBulk");
            bulk.setName("products-bulk");
            selection = new Label("", "MeBulkText");
            bulk.add(selection);
            bulk.add(Ui.secondary(Lang.tr("Set active"), "bulk-active", e -> bulkStatus("ACTIVE")));
            bulk.add(Ui.secondary(Lang.tr("Set draft"), "bulk-draft", e -> bulkStatus("DRAFT")));
            bulk.add(Ui.secondary(Lang.tr("Archive"), "bulk-archive", e -> bulkStatus("ARCHIVED")));
            bulk.add(Ui.secondary(Lang.tr("Adjust prices"), "bulk-price", e -> bulkPrice()));
            bulk.setVisible(false);
            bulk.setHidden(true);
            north.add(bulk);
            grid = MUi.grid(rows, "products-grid");
            grid.getColumn(0).setFlex(1).setMinWidthMM(40);
            grid.getColumn(1).setKind(GridColumn.Kind.CHOICE).setChoices((Object[]) Words.PRODUCT_STATUSES)
                    .setEditable(true).setFormatter(new CodeFormat());
            grid.getColumn(4).setKind(GridColumn.Kind.NUMBER);
            grid.getColumn(5).setKind(GridColumn.Kind.NUMBER);
            grid.getColumn(6).setKind(GridColumn.Kind.NUMBER).setEditable(true)
                    .setFormatter(new OrdersPane.MoneyFormat());
            grid.setFrozenColumns(1);
            grid.addSelectionListener((was, now) -> selectionChanged());
            grid.addActionListener(e -> {
                ProductCardDto p = rows.at(grid.getModelRow(grid.getSelectedRow()));
                if (p != null) {
                    edit(p.id());
                }
            });
            RowMenus.attach(grid, () -> {
                ProductCardDto p = rows.at(grid.getModelRow(grid.getSelectedRow()));
                return p == null ? new Command[0] : menu(p);
            });
            all.add(BorderLayout.CENTER, grid);
        } else {
            list = Ui.list("products-list");
            Ui.pull(list, this::reload);
            all.add(BorderLayout.CENTER, list);
        }
        root(all);
    }

    private void selectionChanged() {
        if (bulk == null) {
            return;
        }
        int n = grid.getSelectedRowCount();
        boolean show = n > 1;
        selection.setText(Lang.tr("{0} selected", String.valueOf(n)));
        if (show == bulk.isVisible()) {
            return;
        }
        bulk.setVisible(show);
        bulk.setHidden(!show);
        Ui.relayout(this);
    }

    private void reload() {
        if (search == null) {
            return;
        }
        String chosen = MUi.code(status, FILTER);
        String q = search.getText().trim();
        final String store = MerchantState.storeId();
        Api.catalog().merchantProducts(store, ALL.equals(chosen) ? null : chosen,
                q.length() == 0 ? null : q, null, null, null, null, "title", "0", "500",
                Net.to(page -> {
                    if (store.equals(MerchantState.storeId())) {
                        fill(page.items());
                    }
                }, Ui::fail));
    }

    private void fill(List<ProductCardDto> items) {
        rows.set(items);
        if (grid != null) {
            grid.refresh();
            selectionChanged();
            return;
        }
        list.removeAll();
        for (final ProductCardDto p : items) {
            Container row = new Container(new BorderLayout());
            row.setUIID("MeListRow");
            row.add(BorderLayout.WEST, ProductArt.label(p.id(), p.title(), p.productType(),
                    p.photoId(), com.codename1.ui.CN.convertToPixels(11f),
                    com.codename1.ui.CN.convertToPixels(11f), "MeThumb"));
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(p.title(), "SvRowTitle"));
            // Two short lines rather than one joined by a dot: on a phone the
            // joined line was cut to "8 var..." beside the price column.
            words.add(Ui.plain(Lang.tr("{0} in stock", String.valueOf(p.available())),
                    "SvRowDetail"));
            words.add(Ui.plain(Lang.tr("{0} variants", String.valueOf(p.variantCount())),
                    "SvRowDetail"));
            row.add(BorderLayout.CENTER, words);
            Container end = new Container(BoxLayout.y());
            end.add(Ui.plain(MerchantState.money(p.minPriceCents()), "SvRowValue"));
            end.add(MUi.badge(p.status()));
            row.add(BorderLayout.EAST, end);
            Container tap = Ui.tap(row, "product-" + p.handle(), e -> edit(p.id()));
            RowMenus.attach(tap, () -> menu(p));
            list.add(tap);
        }
        if (items.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_SELL, "No products",
                    "Add a product, or import a spreadsheet of them."));
        }
        Ui.relayout(list);
    }

    private Command[] menu(final ProductCardDto p) {
        return new Command[] {
            Command.create(Lang.tr("Edit"), null, e -> edit(p.id())),
            Command.create(Lang.tr("Duplicate"), null, e -> Api.catalog().duplicateProduct(
                    MerchantState.storeId(), p.id(), Net.to(copy -> {
                        Ui.say("Duplicated");
                        reload();
                    }, Ui::fail))),
            Command.create(Lang.tr("Archive"), null, e -> Api.catalog().archiveProduct(
                    MerchantState.storeId(), p.id(), Net.to(done -> reload(), Ui::fail)))
        };
    }

    void edit(String productId) {
        ProductEditor.open(this, productId, this::reload);
    }

    /// An inline status edit: the grid hands the chosen code.
    private void status(final ProductCardDto p, Object value) {
        if (!(value instanceof String) || value.equals(p.status())) {
            return;
        }
        List<String> ids = new ArrayList<String>();
        ids.add(p.id());
        Api.catalog().bulkEdit(MerchantState.storeId(), new BulkEditDto(ids, (String) value, 0, 0),
                Net.to(count -> reload(), Ui::fail));
    }

    /// An inline price edit sets every variant of the product to that price.
    private void price(final ProductCardDto p, Object value) {
        long cents = value instanceof Long ? ((Long) value).longValue() : 0;
        if (cents <= 0 || cents == p.minPriceCents()) {
            return;
        }
        List<String> ids = new ArrayList<String>();
        ids.add(p.id());
        Api.catalog().bulkEdit(MerchantState.storeId(), new BulkEditDto(ids, null, cents, 0),
                Net.to(count -> reload(), Ui::fail));
    }

    private List<String> selectedIds() {
        List<String> ids = new ArrayList<String>();
        int[] picked = grid.getSelectedRows();
        for (int iter = 0; picked != null && iter < picked.length; iter++) {
            ProductCardDto p = rows.at(grid.getModelRow(picked[iter]));
            if (p != null) {
                ids.add(p.id());
            }
        }
        return ids;
    }

    private void bulkStatus(String code) {
        List<String> ids = selectedIds();
        if (ids.isEmpty()) {
            return;
        }
        Api.catalog().bulkEdit(MerchantState.storeId(), new BulkEditDto(ids, code, 0, 0),
                Net.to(count -> {
                    Ui.say(Lang.tr("{0} products changed", String.valueOf(count.count())));
                    reload();
                }, Ui::fail));
    }

    private void bulkPrice() {
        final List<String> ids = selectedIds();
        if (ids.isEmpty()) {
            return;
        }
        Container body = new Container(BoxLayout.y());
        TextField percent = Ui.field(body, Lang.tr("Change prices by percent (-20 for 20% off)"),
                "bulk-percent", "10", TextField.DECIMAL);
        Command ok = new com.codename1.ui.Command(Lang.tr("Apply"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Adjust prices"), body, cancel, ok) != ok) {
            return;
        }
        // Percent typed as "12.5" is 1250 basis points.
        long bp = ProductCsv.cents(percent.getText());
        if (bp == 0 || bp < -9900 || bp > 100000) {
            return;
        }
        Api.catalog().bulkEdit(MerchantState.storeId(), new BulkEditDto(ids, null, 0, (int) bp),
                Net.to(count -> reload(), Ui::fail));
    }

    /// Exports the whole catalog with its variants: the list holds cards only,
    /// so each product is fetched in full first.
    private void export() {
        final List<ProductCardDto> cards = rows.all();
        final List<ProductDto> full = new ArrayList<ProductDto>();
        if (cards.isEmpty()) {
            Ui.say("No products to export");
            return;
        }
        Ui.say(Lang.tr("Exporting {0} products", String.valueOf(cards.size())));
        fetch(cards, 0, full);
    }

    private void fetch(final List<ProductCardDto> cards, final int at, final List<ProductDto> full) {
        if (at >= cards.size()) {
            Files.save("products.csv", ProductCsv.write(full), "text/csv");
            return;
        }
        Api.catalog().merchantProduct(MerchantState.storeId(), cards.get(at).id(), Net.to(p -> {
            full.add(p);
            fetch(cards, at + 1, full);
        }, Ui::fail));
    }

    private void importCsv() {
        Files.askCsv("Import products", text -> {
            List<ProductDto> products;
            try {
                products = ProductCsv.read(text);
            } catch (IOException | IllegalArgumentException bad) {
                Ui.say(Lang.tr("That file could not be read: {0}", bad.getMessage()));
                return;
            }
            if (products.isEmpty()) {
                Ui.say("The file has no products");
                return;
            }
            create(products, 0, 0);
        });
    }

    private void create(final List<ProductDto> products, final int at, final int failed) {
        if (at >= products.size()) {
            Ui.say(Lang.tr("Imported {0} products", String.valueOf(products.size() - failed)));
            reload();
            return;
        }
        Api.catalog().createProduct(MerchantState.storeId(), products.get(at), Net.to(
                made -> create(products, at + 1, failed),
                (code, message) -> create(products, at + 1, failed + 1)));
    }

    @Override
    void stockChanged(String variantId, String locationId) {
        // Counts in the grid are a snapshot; Refresh brings them up to date
        // without moving the merchant's selection under them.
    }

    /// Shows a status code as its word in the grid.
    static final class CodeFormat implements com.codename1.ui.table.GridFormatter {
        @Override
        public String format(Object value) {
            return value == null ? "" : Lang.tr(Words.of(String.valueOf(value)));
        }

        @Override
        public Object parse(String text, Object previous) {
            return text;
        }
    }

    @Override
    boolean splits() {
        return true;
    }

    @Override
    Component emptyDetail() {
        return Ui.empty(FontImage.MATERIAL_SELL, "Select a product",
                "Pick a product from the list to edit it here.");
    }
}
