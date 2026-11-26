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
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.table.DataGrid;
import com.codenameone.examples.bazaar.api.CreditAdjustDto;
import com.codenameone.examples.bazaar.api.CustomerDetailDto;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.CustomerEditDto;
import com.codenameone.examples.bazaar.api.CustomerOrderDto;
import com.codenameone.examples.bazaar.api.ExportDto;
import com.codenameone.examples.bazaar.api.SavedAddressDto;
import com.codenameone.examples.bazaar.api.SegmentDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.shop.Addresses;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The store's customers: a grid on a wide screen and a list on a phone,
/// narrowed by segment chips and filters, each opening to their orders,
/// addresses, lifetime value, store credit, notes and tags. The current
/// segment exports to CSV.
final class CustomersPane extends MerchantPane {
    private static final String[] SORTS = {"recent", "spent", "orders", "name"};
    private static final String[] SORT_LABELS = {"Most recent", "Most spent", "Most orders",
        "Name"};

    private List<SegmentDto> segments = new ArrayList<SegmentDto>();
    private String segment;
    private String query = "";
    private String tag;
    private long minSpent;
    private int minOrders;
    private boolean marketingOnly;
    private int sort;
    private List<CustomerDto> customers;
    private TextField search;

    CustomersPane() {
        super("Customers", "merchant-customers");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_FILTER_LIST, "Filters", "customers-filters", this::filters);
        tool(FontImage.MATERIAL_DOWNLOAD, "Export CSV", "customers-export", this::export);
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "customers-refresh", this::load);
        Api.customers().segments(MerchantState.storeId(), Net.to(found -> {
            segments = found == null ? new ArrayList<SegmentDto>() : found;
            paint();
        }, (status, message) -> { }));
        fetch();
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

    private void fetch() {
        final String store = MerchantState.storeId();
        Api.customers().list(store, query.length() == 0 ? null : query, tag,
                minSpent > 0 ? Long.toString(minSpent) : null,
                minOrders > 0 ? Integer.toString(minOrders) : null,
                marketingOnly ? "true" : null, segment, SORTS[sort], Net.to(found -> {
                    if (store.equals(MerchantState.storeId())) {
                        customers = found;
                        paint();
                    }
                }, Ui::fail));
    }

    private void paint() {
        if (customers == null) {
            return;
        }
        Container root = new Container(new BorderLayout());
        Container top = new Container(BoxLayout.y());
        top.setUIID("MeFilters");
        if (search == null) {
            search = MUi.search("customers-search", () -> {
                query = search.getText().trim();
                fetch();
            });
        }
        search.remove();
        top.add(search);
        Container chips = new Container(new FlowLayout());
        chips.setName("customer-segments");
        chips.add(chip(Lang.tr("All"), null, -1));
        for (int iter = 0; iter < segments.size(); iter++) {
            SegmentDto s = segments.get(iter);
            chips.add(chip(Lang.tr(s.name()), s.id(), s.count()));
        }
        top.add(chips);
        String active = describeFilters();
        if (active.length() > 0) {
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.CENTER, Ui.plain(active, "SvMuted"));
            row.add(BorderLayout.EAST, Ui.link(Lang.tr("Clear"), "customers-clear", e -> {
                tag = null;
                minSpent = 0;
                minOrders = 0;
                marketingOnly = false;
                fetch();
            }));
            top.add(row);
        }
        root.add(BorderLayout.NORTH, top);
        if (wide) {
            final Rows<CustomerDto> rows = new Rows<CustomerDto>()
                    .col(Lang.tr("Name"), CustomersPane::name)
                    .col(Lang.tr("Email"), c -> c.email() == null ? "" : c.email())
                    .col(Lang.tr("Orders"), c -> String.valueOf(c.ordersCount()))
                    .col(Lang.tr("Spent"), c -> MerchantState.money(c.spentCents()))
                    .col(Lang.tr("Store credit"), c -> MerchantState.money(c.storeCreditCents()))
                    .col(Lang.tr("Tags"), c -> join(c.tags()))
                    .col(Lang.tr("Last order"), c -> MUi.when(c.lastOrderAt()));
            rows.set(customers);
            final DataGrid grid = MUi.grid(rows, "customers-grid");
            grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
            grid.addActionListener(e -> {
                CustomerDto c = rows.at(grid.getModelRow(grid.getSelectedRow()));
                if (c != null) {
                    open(c);
                }
            });
            RowMenus.attach(grid, () -> {
                final CustomerDto c = rows.at(grid.getModelRow(grid.getSelectedRow()));
                if (c == null) {
                    return null;
                }
                return new Command[] {
                    Command.create(Lang.tr("Open"), null, e -> open(c)),
                    Command.create(Lang.tr("Adjust store credit"), null, e -> credit(c, null))
                };
            });
            root.add(BorderLayout.CENTER, grid);
        } else {
            Container list = Ui.list("customers-list");
            // The rows are already here: drop the loading placeholder a new
            // list starts with, which otherwise sat above them.
            list.removeAll();
            if (customers.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_GROUP, "No customers",
                        "Nobody matches these filters."));
            }
            for (int iter = 0; iter < customers.size(); iter++) {
                final CustomerDto c = customers.get(iter);
                Container value = new Container(BoxLayout.y());
                value.add(Ui.plain(MerchantState.money(c.spentCents()), "SvRowValue"));
                value.add(Ui.plain(Lang.tr("{0} orders", String.valueOf(c.ordersCount())),
                        "SvRowDetail"));
                Container row = Ui.row(FontImage.MATERIAL_PERSON, name(c),
                        c.email() == null ? "" : c.email(), value);
                list.add(Ui.tap(row, "customer-" + iter, e -> open(c)));
            }
            root.add(BorderLayout.CENTER, list);
        }
        root(root);
    }

    private Button chip(String text, final String id, int count) {
        boolean on = id == null ? segment == null : id.equals(segment);
        Button b = new Button(count >= 0 ? text + " " + count : text, on ? "MeChipOn" : "MeChip");
        b.setName("segment-" + (id == null ? "all" : id));
        b.addActionListener(e -> {
            segment = id;
            fetch();
        });
        return b;
    }

    private String describeFilters() {
        StringBuilder out = new StringBuilder();
        if (tag != null) {
            out.append(Lang.tr("Tag {0}", tag));
        }
        if (minSpent > 0) {
            out.append(out.length() > 0 ? Ui.DOT : "").append(Lang.tr("Spent at least {0}",
                    MerchantState.money(minSpent)));
        }
        if (minOrders > 0) {
            out.append(out.length() > 0 ? Ui.DOT : "").append(Lang.tr("At least {0} orders",
                    String.valueOf(minOrders)));
        }
        if (marketingOnly) {
            out.append(out.length() > 0 ? Ui.DOT : "").append(Lang.tr("Accepts marketing"));
        }
        return out.toString();
    }

    private void filters() {
        Container body = new Container(BoxLayout.y());
        TextField tagField = MUi.text(body, "Tag", "filter-tag", tag);
        TextField spent = MUi.money(body, "Spent at least", "filter-spent", minSpent);
        TextField orders = MUi.number(body, "At least this many orders", "filter-orders",
                minOrders);
        CheckBox marketing = new CheckBox(Lang.tr("Accepts marketing"));
        marketing.setName("filter-marketing");
        marketing.setSelected(marketingOnly);
        body.add(marketing);
        com.codename1.ui.spinner.Picker sortBy = new com.codename1.ui.spinner.Picker();
        sortBy.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        sortBy.setUIID("SvField");
        sortBy.setName("filter-sort");
        String[] labels = new String[SORT_LABELS.length];
        for (int iter = 0; iter < labels.length; iter++) {
            labels[iter] = Lang.tr(SORT_LABELS[iter]);
        }
        sortBy.setStrings(labels);
        sortBy.setSelectedStringIndex(sort);
        body.add(Ui.label("Sort by", "SvLabel")).add(sortBy);
        Command ok = new com.codename1.ui.Command(Lang.tr("Apply"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Filters"), body, cancel, ok) == ok) {
            String t = tagField.getText().trim();
            tag = t.length() == 0 ? null : t;
            minSpent = Math.max(0, MUi.cents(spent));
            minOrders = Math.max(0, MUi.integer(orders));
            marketingOnly = marketing.isSelected();
            sort = Math.max(0, sortBy.getSelectedStringIndex());
            fetch();
        }
    }

    static String name(CustomerDto c) {
        String first = c.firstName() == null ? "" : c.firstName();
        String last = c.lastName() == null ? "" : c.lastName();
        String full = (first + " " + last).trim();
        if (full.length() > 0) {
            return full;
        }
        return c.username() != null ? c.username() : c.email() == null ? "" : c.email();
    }

    static String join(List<String> tags) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; tags != null && iter < tags.size(); iter++) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(tags.get(iter));
        }
        return out.toString();
    }

    /// Splits "a, b,,c" into its trimmed, non-empty parts.
    static List<String> tags(String text) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (text != null && start <= text.length()) {
            int end = text.indexOf(',', start);
            if (end < 0) {
                end = text.length();
            }
            String part = text.substring(start, end).trim();
            if (part.length() > 0 && !out.contains(part)) {
                out.add(part);
            }
            start = end + 1;
        }
        return out;
    }

    // ------------------------------------------------------------ the detail

    private void open(CustomerDto c) {
        final Container page = MUi.widePage("customer-detail");
        page.add(new com.codename1.appkit.Skeleton(4, "SvSkeleton"));
        pick(name(c), page);
        Api.customers().detail(MerchantState.storeId(), c.id(), Net.to(d -> detail(page, d),
                Ui::fail));
    }

    private void detail(final Container page, final CustomerDetailDto d) {
        page.removeAll();
        final CustomerDto c = d.customer();
        page.add(Ui.person(name(c), c.email() == null ? c.username() : c.email(), null));
        page.add(com.codenameone.examples.bazaar.ui.AppBlocks.admin(MerchantState.storeId(),
                "customer", c.id()));
        Container tiles = DashGrid.of(40f, 4);
        tiles.setUIID("MeTiles");
        tiles.add(MUi.tile(MerchantState.money(d.lifetimeValueCents()), Lang.tr("Lifetime value"),
                null, "customer-ltv"));
        tiles.add(MUi.tile(MerchantState.money(d.averageOrderCents()), Lang.tr("Average order"),
                null, "customer-aov"));
        tiles.add(MUi.tile(String.valueOf(c.ordersCount()), Lang.tr("Orders"), null,
                "customer-orders"));
        tiles.add(MUi.tile(MerchantState.money(c.storeCreditCents()), Lang.tr("Store credit"),
                null, "customer-credit"));
        page.add(tiles);
        // Four actions of different widths in a right-aligned flow wrapped
        // into a ragged staircase, and two columns cut "Adjust store credit"
        // short on a phone: one full-width button per line.
        Container actions = new Container(BoxLayout.y());
        actions.setUIID("MeActionGrid");
        actions.add(Ui.secondary(Lang.tr("Adjust store credit"), "customer-adjust",
                e -> credit(c, () -> open(c))));
        actions.add(Ui.secondary(Lang.tr("Merge a duplicate"), "customer-merge", e -> merge(c)));
        actions.add(Ui.secondary(Lang.tr("Export data"), "customer-data-export", e -> exportData(c)));
        actions.add(Ui.secondary(Lang.tr("Erase personal data"), "customer-erase", e -> erase(c)));
        page.add(actions);

        Ui.section(page, "Notes and tags");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final TextField tagField = MUi.text(card, "Tags", "customer-tags", join(c.tags()));
        card.add(Ui.label("Note", "SvLabel"));
        final TextArea note = new TextArea(c.note() == null ? "" : c.note(), 3, 30);
        note.setUIID("SvField");
        note.setName("customer-note");
        card.add(note);
        final CheckBox marketing = new CheckBox(Lang.tr("Accepts marketing"));
        marketing.setName("customer-marketing");
        marketing.setSelected(c.acceptsMarketing());
        card.add(marketing);
        card.add(MUi.cardButtons(Ui.primary(Lang.tr("Save"), "customer-save", e -> {
            Api.customers().edit(MerchantState.storeId(), c.id(), new CustomerEditDto(
                    note.getText(), tags(tagField.getText()), marketing.isSelected()),
                    Net.to(saved -> {
                        Ui.say("Saved");
                        fetch();
                    }, Ui::fail));
        })));
        page.add(card);

        Ui.section(page, "Orders");
        Container orders = new Container(BoxLayout.y());
        orders.setUIID("MeCard");
        if (d.orders() == null || d.orders().isEmpty()) {
            orders.add(Ui.text(Lang.tr("No orders yet."), "SvMuted"));
        }
        for (int iter = 0; d.orders() != null && iter < d.orders().size(); iter++) {
            final CustomerOrderDto o = d.orders().get(iter);
            Container end = new Container(BoxLayout.y());
            end.add(Ui.plain(MerchantState.money(o.totalCents()), "SvRowValue"));
            end.add(MUi.badge(o.fulfilment()));
            Container row = Ui.row(FontImage.MATERIAL_RECEIPT_LONG,
                    Lang.tr("Order #{0}", String.valueOf(o.number())),
                    Lang.tr(Words.of(o.channel())) + Ui.DOT + MUi.when(o.createdAt()), end);
            orders.add(Ui.tap(row, "customer-order-" + iter, e -> {
                OrdersPane.open = o.orderId();
                DashboardPane.go(com.codenameone.examples.bazaar.ui.Sections.ORDERS);
            }));
        }
        page.add(orders);

        Ui.section(page, "Addresses");
        Container addresses = new Container(BoxLayout.y());
        addresses.setUIID("MeCard");
        if (d.addresses() == null || d.addresses().isEmpty()) {
            addresses.add(Ui.text(Lang.tr("No saved addresses."), "SvMuted"));
        }
        for (int iter = 0; d.addresses() != null && iter < d.addresses().size(); iter++) {
            SavedAddressDto a = d.addresses().get(iter);
            if (a.address() == null) {
                continue;
            }
            Label badge = a.isDefault() ? Ui.badge(Lang.tr("Default"), "SvBadge") : null;
            addresses.add(Ui.row(FontImage.MATERIAL_LOCATION_ON, a.address().name(),
                    Addresses.summary(a.address()), badge));
        }
        page.add(addresses);
        Ui.relayout(page);
    }

    private void credit(final CustomerDto c, final Runnable after) {
        Container body = new Container(BoxLayout.y());
        body.add(Ui.text(Lang.tr("Now {0}. A negative amount takes credit away.",
                MerchantState.money(c.storeCreditCents())), "SvMuted"));
        TextField amount = Ui.field(body, "Amount", "credit-amount", "0.00", TextArea.ANY);
        TextField reason = MUi.text(body, "Reason", "credit-reason", "");
        TextField days = Ui.field(body, "Expires in days (blank: never)", "credit-expiry", "",
                TextArea.NUMERIC);
        Command ok = new com.codename1.ui.Command(Lang.tr("Adjust"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Adjust store credit"), body, cancel, ok) != ok) {
            return;
        }
        String text = amount.getText().trim();
        boolean negative = text.startsWith("-");
        long cents = ProductCsv.cents(negative ? text.substring(1) : text, MerchantState.currency());
        if (cents == 0) {
            return;
        }
        Api.customers().adjustCredit(MerchantState.storeId(), c.id(), new CreditAdjustDto(
                negative ? -cents : cents, reason.getText().trim(),
                negative ? 0 : CustomerMatches.daysFrom(days.getText(), System.currentTimeMillis())),
                Net.to(saved -> {
                    Ui.say("Store credit adjusted");
                    fetch();
                    if (after != null) {
                        after.run();
                    }
                }, Ui::fail));
    }

    // ---------------------------------------------------------- personal data

    private void merge(final CustomerDto c) {
        List<CustomerDto> dups = CustomerMatches.duplicates(customers, c);
        if (dups.isEmpty()) {
            Ui.say("No other customer has this e-mail, phone or name");
            return;
        }
        String[] labels = new String[dups.size()];
        for (int iter = 0; iter < labels.length; iter++) {
            CustomerDto d = dups.get(iter);
            labels[iter] = CustomerMatches.name(d) + " (" + d.email() + ")";
        }
        Container body = new Container(BoxLayout.y());
        body.add(Ui.text(Lang.tr("The other record's orders, addresses, credit, gift cards, "
                + "points, reviews and wishlist move here, and it is deleted."), "SvMuted"));
        com.codename1.ui.spinner.Picker p = new com.codename1.ui.spinner.Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setUIID("SvField");
        p.setName("customer-merge-pick");
        p.setStrings(labels);
        p.setSelectedStringIndex(0);
        body.add(p);
        Command ok = new com.codename1.ui.Command(Lang.tr("Merge"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Merge a duplicate"), body, cancel, ok) != ok) {
            return;
        }
        CustomerDto other = dups.get(Math.max(0, p.getSelectedStringIndex()));
        Api.customers().merge(MerchantState.storeId(), c.id(),
                new com.codenameone.examples.bazaar.api.CustomerMergeDto(other.id()),
                Net.to(merged -> {
                    Ui.say("Customers merged");
                    fetch();
                    open(merged);
                }, Ui::fail));
    }

    private void exportData(CustomerDto c) {
        Api.customers().dataExport(MerchantState.storeId(), c.id(), Net.to(x -> {
            try {
                Files.saveBytes(x.fileName(), com.codename1.util.Base64.decode(
                        x.zipBase64().getBytes("UTF-8")), "application/zip");
            } catch (java.io.IOException failed) {
                com.codename1.io.Log.e(failed);
            }
        }, Ui::fail));
    }

    private void erase(final CustomerDto c) {
        if (!MUi.confirm("Erase personal data", "The customer's name, contact details, "
                + "addresses, reviews and wishlist are erased. Orders keep their amounts. "
                + "This cannot be undone.", "Erase")) {
            return;
        }
        Api.customers().erase(MerchantState.storeId(), c.id(), Net.to(done -> {
            Ui.say("Personal data erased");
            fetch();
            open(done);
        }, Ui::fail));
    }

    // -------------------------------------------------------------- export

    private void export() {
        Api.customers().export(MerchantState.storeId(), segment, Net.to(this::save, Ui::fail));
    }

    private void save(ExportDto d) {
        int count = d.rows() == null ? 0 : d.rows().size();
        String[][] rows = new String[count + 1][];
        rows[0] = d.columns() == null ? new String[0]
                : d.columns().toArray(new String[d.columns().size()]);
        for (int iter = 0; iter < count; iter++) {
            List<String> cells = d.rows().get(iter).cells();
            rows[iter + 1] = cells == null ? new String[0] : cells.toArray(new String[cells.size()]);
        }
        Files.saveCsv("customers" + (segment == null ? "" : "-" + segment) + ".csv", rows);
    }

    @Override
    boolean splits() {
        return true;
    }

    @Override
    Component emptyDetail() {
        return Ui.empty(FontImage.MATERIAL_PERSON, "Select a customer",
                "Pick a customer from the list to see them here.");
    }
}
