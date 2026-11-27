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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.DefaultGridCellRenderer;
import com.codename1.ui.table.GridCell;
import com.codename1.ui.table.GridColumn;
import com.codenameone.examples.bazaar.api.AdjustDto;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.MoveDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Stock: every variant at every location -- available, committed, on hand
/// and incoming, low stock picked out -- with adjustments that say why, a
/// stock count mode driven by a barcode scanner, each level's history, and
/// tabs for transfers between locations, purchase orders and suppliers.
final class InventoryPane extends MerchantPane {
    private static final String LEVELS = "levels";
    private static final String TRANSFERS = "transfers";
    private static final String ORDERS = "orders";
    private static final String SUPPLIERS = "suppliers";

    private final Rows<LevelDto> rows = new Rows<LevelDto>();
    private final List<LocationDto> locations = new ArrayList<LocationDto>();
    private String tab = LEVELS;
    private TextField search;
    private CheckBox lowOnly;
    private DataGrid grid;
    private Container list;
    private List<LevelDto> all = new ArrayList<LevelDto>();
    private final Container body = new Container(new BorderLayout());

    InventoryPane() {
        super("Inventory", "merchant-inventory");
        rows.col(Lang.tr("Product"), l -> l.productTitle())
                .col(Lang.tr("Variant"), l -> l.variantTitle())
                .col(Lang.tr("SKU"), l -> l.sku())
                .col(Lang.tr("Location"), l -> l.locationName())
                .col(Lang.tr("Available"), l -> Integer.valueOf(l.available()))
                .col(Lang.tr("Committed"), l -> Integer.valueOf(l.committed()))
                .col(Lang.tr("On hand"), l -> Integer.valueOf(l.onHand()), (l, v) -> setOnHand(l, v))
                .col(Lang.tr("Incoming"), l -> Integer.valueOf(l.incoming()))
                .col(Lang.tr("Reorder at"), l -> Integer.valueOf(l.reorderPoint()),
                        (l, v) -> setReorder(l, v));
    }

    static boolean low(LevelDto l) {
        return l.available() <= l.reorderPoint();
    }

    @Override
    void actions() {
        DesktopActions.set(DesktopActions.EXPORT_CSV, this::export);
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
        tool(FontImage.MATERIAL_FACT_CHECK, "Stock count", "inventory-count", this::countMode);
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "inventory-refresh", this::load);
        Api.stores().locations(MerchantState.storeId(), Net.to(found -> {
            locations.clear();
            locations.addAll(found);
        }, (s, m) -> { }));
        if (search == null) {
            Container top = new Container(BoxLayout.y());
            top.add(Ui.choice("inventory-tab", new String[] {Lang.tr("Levels"), Lang.tr("Transfers"),
                Lang.tr("Purchases"), Lang.tr("Suppliers")},
                    new String[] {LEVELS, TRANSFERS, ORDERS, SUPPLIERS}, tab, v -> {
                        tab = v;
                        showTab();
                    }));
            Container page = new Container(new BorderLayout());
            page.add(BorderLayout.NORTH, top);
            page.add(BorderLayout.CENTER, body);
            search = MUi.search("inventory-search", this::filter);
            lowOnly = new CheckBox(Lang.tr("Low stock only"));
            lowOnly.setName("inventory-low");
            lowOnly.addActionListener(e -> filter());
            root(page);
        }
        showTab();
    }

    private void showTab() {
        body.removeAll();
        if (LEVELS.equals(tab)) {
            levels();
        } else if (TRANSFERS.equals(tab)) {
            body.add(BorderLayout.CENTER, new Transfers(this, locations));
        } else if (ORDERS.equals(tab)) {
            body.add(BorderLayout.CENTER, new PurchaseOrders(this, locations));
        } else {
            body.add(BorderLayout.CENTER, new Suppliers(this));
        }
        Ui.relayout(this);
    }

    private void levels() {
        Container filters = new Container(new BorderLayout());
        filters.setUIID("MeFilters");
        filters.add(BorderLayout.CENTER, search);
        filters.add(BorderLayout.EAST, lowOnly);
        body.add(BorderLayout.NORTH, filters);
        if (wide) {
            grid = MUi.grid(rows, "inventory-grid");
            grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
            grid.getColumn(0).setFlex(1);
            for (int iter = 4; iter <= 8; iter++) {
                grid.getColumn(iter).setKind(GridColumn.Kind.NUMBER);
            }
            grid.getColumn(6).setEditable(true);
            grid.getColumn(8).setEditable(true);
            grid.getColumn(4).setRenderer(new DefaultGridCellRenderer() {
                @Override
                protected int textColor(GridCell cell, int defaultColor) {
                    LevelDto l = rows.at(cell.getGrid().getModelRow(cell.getRow()));
                    return l != null && low(l) ? MUi.color("MeLowStock") & 0xffffff : defaultColor;
                }
            });
            grid.setFrozenColumns(1);
            grid.addActionListener(e -> {
                LevelDto l = rows.at(grid.getModelRow(grid.getSelectedRow()));
                if (l != null) {
                    adjust(l);
                }
            });
            RowMenus.attach(grid, () -> {
                LevelDto l = rows.at(grid.getModelRow(grid.getSelectedRow()));
                return l == null ? new Command[0] : menu(l);
            });
            body.add(BorderLayout.CENTER, grid);
            list = null;
        } else {
            list = Ui.list("inventory-list");
            Ui.pull(list, this::reloadLevels);
            body.add(BorderLayout.CENTER, list);
            grid = null;
        }
        reloadLevels();
    }

    private Command[] menu(final LevelDto l) {
        return new Command[] {
            Command.create(Lang.tr("Adjust..."), null, e -> adjust(l)),
            Command.create(Lang.tr("History"), null, e -> history(l))
        };
    }

    void reloadLevels() {
        final String store = MerchantState.storeId();
        Api.inventory().levels(store, null, null, null, Net.to(found -> {
            if (store.equals(MerchantState.storeId())) {
                all = found;
                filter();
            }
        }, Ui::fail));
    }

    private void filter() {
        if (search == null) {
            return;
        }
        String q = search.getText().trim();
        List<LevelDto> shown = new ArrayList<LevelDto>();
        for (LevelDto l : all) {
            if (lowOnly.isSelected() && !low(l)) {
                continue;
            }
            if (MUi.has(l.productTitle(), q) || MUi.has(l.sku(), q) || MUi.has(l.barcode(), q)
                    || MUi.has(l.variantTitle(), q)) {
                shown.add(l);
            }
        }
        rows.set(shown);
        if (grid != null) {
            grid.refresh();
        } else if (list != null) {
            list.removeAll();
            for (final LevelDto l : shown) {
                Label count = Ui.plain(String.valueOf(l.available()), low(l) ? "MeLowStock" : "SvRowValue");
                Container row = Ui.row((char) 0, l.productTitle() + Ui.DOT + l.variantTitle(),
                        l.locationName() + Ui.DOT + Lang.tr("{0} on hand, {1} committed",
                                String.valueOf(l.onHand()), String.valueOf(l.committed())), count);
                Container tap = Ui.tap(row, "level-" + l.id(), e -> adjust(l));
                RowMenus.attach(tap, () -> menu(l));
                list.add(tap);
            }
            if (shown.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_INVENTORY_2, "Nothing to show",
                        "No stock matches."));
            }
            Ui.relayout(list);
        }
    }

    private void setOnHand(LevelDto l, Object v) {
        int wanted = v instanceof Number ? ((Number) v).intValue() : l.onHand();
        if (wanted == l.onHand()) {
            return;
        }
        Api.inventory().adjust(MerchantState.storeId(), new AdjustDto(l.variantId(), l.locationId(),
                wanted - l.onHand(), 0, "CORRECTION", "Edited in the grid"),
                Net.to(saved -> reloadLevels(), Ui::fail));
    }

    private void setReorder(LevelDto l, Object v) {
        int wanted = v instanceof Number ? ((Number) v).intValue() : l.reorderPoint();
        Api.inventory().reorderPoint(MerchantState.storeId(), new AdjustDto(l.variantId(),
                l.locationId(), 0, Math.max(0, wanted), "CORRECTION", null),
                Net.to(saved -> reloadLevels(), Ui::fail));
    }

    /// The adjust dialog: how many more or fewer, and why.
    void adjust(final LevelDto l) {
        Container body = new Container(BoxLayout.y());
        body.add(Ui.plain(l.productTitle() + Ui.DOT + l.variantTitle(), "SvHeading"));
        body.add(Ui.plain(Lang.tr("{0} on hand at {1}", String.valueOf(l.onHand()), l.locationName()),
                "SvMuted"));
        TextField delta = Ui.field(body, Lang.tr("Change by (negative to remove)"), "adjust-delta",
                "0", TextField.NUMERIC);
        Picker reason = MUi.picker(body, Lang.tr("Reason"), "adjust-reason", Words.ADJUST_REASONS,
                "RESTOCK");
        TextField note = Ui.field(body, Lang.tr("Note"), "adjust-note", "");
        Command ok = new Command(Lang.tr("Adjust"));
        Command history = new Command(Lang.tr("History"));
        Command cancel = new Command(Lang.tr("Cancel"));
        Command answer = Dialog.show(Lang.tr("Adjust stock"), body, cancel, history, ok);
        if (answer == history) {
            history(l);
            return;
        }
        if (answer != ok || MUi.integer(delta) == 0) {
            return;
        }
        Api.inventory().adjust(MerchantState.storeId(), new AdjustDto(l.variantId(), l.locationId(),
                MUi.integer(delta), 0, MUi.code(reason, Words.ADJUST_REASONS), note.getText()),
                Net.to(saved -> {
                    Ui.say("Stock adjusted");
                    reloadLevels();
                }, Ui::fail));
    }

    /// Every move of one variant at one location, newest first.
    void history(final LevelDto l) {
        Api.inventory().moves(MerchantState.storeId(), l.variantId(), l.locationId(), "100",
                Net.to(moves -> {
                    Container page = MUi.page("inventory-history");
                    Container card = new Container(BoxLayout.y());
                    card.setUIID("MeCard");
                    for (MoveDto m : moves) {
                        String delta = (m.onHandDelta() > 0 ? "+" : "") + m.onHandDelta();
                        String committed = m.committedDelta() == 0 ? ""
                                : Ui.DOT + Lang.tr("committed {0}", (m.committedDelta() > 0 ? "+" : "")
                                        + m.committedDelta());
                        card.add(Ui.row((char) 0, Lang.tr(Words.of(m.reason())) + committed,
                                MUi.when(m.createdAt()) + (m.username() == null ? "" : Ui.DOT + m.username())
                                        + (m.reference() == null ? "" : Ui.DOT + m.reference()),
                                Ui.plain(delta, m.onHandDelta() < 0 ? "MeLowStock" : "SvRowValue")));
                    }
                    if (moves.isEmpty()) {
                        card.add(Ui.empty(FontImage.MATERIAL_HISTORY, "No history",
                                "Nothing has moved yet."));
                    }
                    page.add(card);
                    pick(l.productTitle() + " - " + l.variantTitle(), page);
                }, Ui::fail));
    }

    /// Stock count: scan (or type) a code, enter what is on the shelf, next.
    /// The server records the difference as a COUNT move.
    private void countMode() {
        if (locations.isEmpty()) {
            Ui.say("Add a location first");
            return;
        }
        final Container page = MUi.page("stock-count");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        String[] ids = new String[locations.size()];
        String[] names = new String[locations.size()];
        for (int iter = 0; iter < ids.length; iter++) {
            ids[iter] = locations.get(iter).id();
            names[iter] = locations.get(iter).name();
        }
        final Picker where = new Picker();
        where.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        where.setUIID("SvField");
        where.setName("count-location");
        where.setStrings(names);
        where.setSelectedStringIndex(0);
        card.add(Ui.label("Location", "SvLabel")).add(where);
        card.add(Ui.text(Lang.tr("Scan or type a code, press Enter, then type the counted "
                + "quantity and press Enter again."), "SvMuted"));
        final Label found = new Label(" ", "SvHeading");
        found.setName("count-found");
        final TextField counted = new TextField("", Lang.tr("Counted"), 5, TextField.NUMERIC);
        counted.setUIID("SvField");
        counted.setName("count-qty");
        final String[] variant = new String[1];
        final Container done = new Container(BoxLayout.y());
        final TextField code = Scan.field("count-code", null);
        final Runnable lookup = () -> {
            String typed = code.getText().trim();
            if (typed.length() == 0) {
                return;
            }
            Api.inventory().lookup(MerchantState.storeId(), typed, Net.to(hit -> {
                variant[0] = hit.variant().id();
                found.setText(hit.productTitle() + Ui.DOT + hit.variant().title());
                counted.setText("");
                counted.startEditingAsync();
                Ui.relayout(page);
            }, (s, m) -> {
                variant[0] = null;
                found.setText(Lang.tr("No product has that code"));
                code.setText("");
                code.startEditingAsync();
            }));
        };
        code.setDoneListener(e -> lookup.run());
        counted.setDoneListener(e -> {
            if (variant[0] == null) {
                return;
            }
            final int qty = MUi.integer(counted);
            final String title = found.getText();
            Api.inventory().count(MerchantState.storeId(), new AdjustDto(variant[0],
                    locations.get(Math.max(0, where.getSelectedStringIndex())).id(), 0, qty, "COUNT",
                    null), Net.to(level -> {
                        done.add(0, Ui.row(FontImage.MATERIAL_CHECK, title,
                                Lang.tr("Counted {0}", String.valueOf(qty)), null));
                        variant[0] = null;
                        found.setText(" ");
                        code.setText("");
                        code.startEditingAsync();
                        Ui.relayout(page);
                    }, Ui::fail));
        });
        Container scanRow = new Container(new BorderLayout());
        scanRow.add(BorderLayout.CENTER, code);
        scanRow.add(BorderLayout.EAST, Scan.button("count-scan", code, lookup));
        card.add(scanRow).add(found).add(counted);
        page.add(card);
        Ui.section(page, "Counted");
        page.add(done);
        push(Lang.tr("Stock count"), page);
        code.startEditingAsync();
    }

    private void export() {
        String[][] table = new String[rows.getRowCount() + 1][];
        table[0] = new String[] {"Product", "Variant", "SKU", "Barcode", "Location", "Available",
            "Committed", "On hand", "Incoming", "Reorder at"};
        for (int iter = 0; iter < rows.getRowCount(); iter++) {
            LevelDto l = rows.at(iter);
            table[iter + 1] = new String[] {l.productTitle(), l.variantTitle(), l.sku(), l.barcode(),
                l.locationName(), String.valueOf(l.available()), String.valueOf(l.committed()),
                String.valueOf(l.onHand()), String.valueOf(l.incoming()),
                String.valueOf(l.reorderPoint())};
        }
        Files.saveCsv("inventory.csv", table);
    }

    @Override
    void stockChanged(String variantId, String locationId) {
        if (LEVELS.equals(tab)) {
            reloadLevels();
        }
    }

    /// Opens a detail screen from one of the tabs.
    void open(String heading, Component c) {
        pick(heading, c);
    }

    void back() {
        popAll();
    }

    /// A picker of the store's locations.
    static Picker locationPicker(Container to, String caption, String name,
            List<LocationDto> locations, String current) {
        Picker p = new Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setUIID("SvField");
        p.setName(name);
        String[] names = new String[locations.size()];
        int at = 0;
        for (int iter = 0; iter < names.length; iter++) {
            names[iter] = locations.get(iter).name();
            if (locations.get(iter).id().equals(current)) {
                at = iter;
            }
        }
        p.setStrings(names);
        if (names.length > 0) {
            p.setSelectedStringIndex(at);
        }
        to.add(Ui.label(caption, "SvLabel")).add(p);
        return p;
    }

    static String location(Picker p, List<LocationDto> locations) {
        int at = p.getSelectedStringIndex();
        return at >= 0 && at < locations.size() ? locations.get(at).id() : null;
    }

    static String locationName(String id, List<LocationDto> locations) {
        for (LocationDto l : locations) {
            if (l.id().equals(id)) {
                return l.name();
            }
        }
        return id == null ? "" : id;
    }

    @Override
    boolean splits() {
        return true;
    }

    @Override
    Component emptyDetail() {
        return Ui.empty(FontImage.MATERIAL_INVENTORY_2, "Select an item",
                "Pick a row from the list to see its history here.");
    }
}
