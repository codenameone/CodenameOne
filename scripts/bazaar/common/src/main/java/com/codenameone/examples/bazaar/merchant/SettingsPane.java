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
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.FraudRuleDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.StaffDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The store's settings: its name and words, its accent colour (previewed
/// live, and the storefront re-themes the moment it is saved), logo,
/// policies, currency and tax mode, then its locations and its staff.
final class SettingsPane extends MerchantPane {
    /// Accent colours offered as swatches; any hex value can be typed too.
    static final int[] SWATCHES = {0x2563eb, 0x7c3aed, 0xdb2777, 0xdc2626, 0xea580c, 0xca8a04,
        0x16a34a, 0x0d9488, 0x0f172a, 0x6b7280};
    static final String[] CURRENCIES = {"USD", "EUR", "GBP", "ILS", "CAD", "AUD", "JPY"};

    private int accent;
    private String logoPhotoId;
    private String heroPhotoId;

    SettingsPane() {
        super("Settings", "merchant-settings");
    }

    @Override
    void load() {
        clearTools();
        Api.stores().storeSettings(MerchantState.storeId(), Net.to(this::show, Ui::fail));
    }

    /// Parses "#1a2b3c" or "1a2b3c"; -1 when it is not six hex digits.
    static int hex(String text) {
        String t = text == null ? "" : text.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        if (t.length() != 6) {
            return -1;
        }
        int out = 0;
        for (int iter = 0; iter < 6; iter++) {
            int d = Character.digit(t.charAt(iter), 16);
            if (d < 0) {
                return -1;
            }
            out = out * 16 + d;
        }
        return out;
    }

    static String hex(int rgb) {
        String s = Integer.toHexString(rgb & 0xffffff);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return "#" + s;
    }

    private void show(final StoreDto s) {
        accent = s.accentColor() & 0xffffff;
        logoPhotoId = s.logoPhotoId();
        heroPhotoId = s.heroPhotoId();
        Container page = MUi.page("settings-page");

        Ui.section(page, "Store");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final TextField name = MUi.text(card, "Store name", "settings-name", s.name());
        final TextField tagline = MUi.text(card, "Tagline", "settings-tagline", s.tagline());
        final TextField email = MUi.text(card, "Contact email", "settings-email", s.email());
        final TextField phone = MUi.text(card, "Phone", "settings-phone", s.phone());
        final TextField address = MUi.text(card, "Address", "settings-address", s.address());
        card.add(Ui.label("Description", "SvLabel"));
        final TextArea description = new TextArea(s.description() == null ? "" : s.description(), 3, 30);
        description.setUIID("SvField");
        description.setName("settings-description");
        card.add(description);
        page.add(card);

        Ui.section(page, "Look");
        Container look = new Container(BoxLayout.y());
        look.setUIID("MeCard");
        final Container preview = new Container(BoxLayout.y());
        preview.setUIID("MeAccentPreview");
        preview.setName("settings-preview");
        final Label previewName = Ui.plain(s.name(), "MeAccentName");
        final Label previewTag = Ui.plain(s.tagline() == null ? "" : s.tagline(), "MeAccentTagline");
        preview.add(previewName).add(previewTag);
        final TextField hexField = MUi.text(look, "Accent colour", "settings-accent", hex(accent));
        final Runnable paint = () -> {
            int ink = light(accent) ? 0x111827 : 0xffffff;
            preview.getAllStyles().setBgColor(accent);
            preview.getAllStyles().setBgTransparency(255);
            previewName.getAllStyles().setFgColor(ink);
            previewTag.getAllStyles().setFgColor(ink);
            previewName.setText(name.getText());
            previewTag.setText(tagline.getText());
            preview.repaint();
        };
        Container swatches = new Container(new FlowLayout());
        swatches.setName("settings-swatches");
        for (final int rgb : SWATCHES) {
            Button sw = new Button(" ", "MeSwatch");
            sw.setName("swatch-" + hex(rgb).substring(1));
            sw.getAllStyles().setBgColor(rgb);
            sw.getAllStyles().setBgTransparency(255);
            sw.addActionListener(e -> {
                accent = rgb;
                hexField.setText(hex(rgb));
                paint.run();
            });
            swatches.add(sw);
        }
        hexField.addDataChangedListener((t, i) -> {
            int v = hex(hexField.getText());
            if (v >= 0) {
                accent = v;
                paint.run();
            }
        });
        name.addDataChangedListener((t, i) -> paint.run());
        tagline.addDataChangedListener((t, i) -> paint.run());
        look.add(swatches);
        look.add(preview);
        final Container media = new Container(BoxLayout.y());
        media.setName("settings-media");
        final Runnable[] paintMedia = new Runnable[1];
        StoreMedia.Changed changed = saved -> {
            // Kept apart from the form: Save must not put back the old ids.
            logoPhotoId = saved.logoPhotoId();
            heroPhotoId = saved.heroPhotoId();
            MerchantState.updated(saved);
            paintMedia[0].run();
        };
        paintMedia[0] = () -> {
            media.removeAll();
            media.add(StoreMedia.editor(false, logoPhotoId, changed));
            media.add(StoreMedia.editor(true, heroPhotoId, changed));
            Ui.relayout(media);
        };
        paintMedia[0].run();
        look.add(media);
        page.add(look);
        paint.run();

        Ui.section(page, "Money");
        Container money = new Container(BoxLayout.y());
        money.setUIID("MeCard");
        final Picker currency = MUi.picker(money, Lang.tr("Currency"), "settings-currency", CURRENCIES,
                s.currency() == null ? "USD" : s.currency());
        final CheckBox inclusive = new CheckBox(Lang.tr("Prices include tax"));
        inclusive.setName("settings-tax-inclusive");
        inclusive.setSelected(s.taxInclusive());
        money.add(inclusive);
        page.add(money);

        Ui.section(page, "Policies");
        Container pol = new Container(BoxLayout.y());
        pol.setUIID("MeCard");
        final TextArea policies = new TextArea(s.policies() == null ? "" : s.policies(), 6, 30);
        policies.setUIID("SvField");
        policies.setName("settings-policies");
        pol.add(Ui.text(Lang.tr("Returns, shipping and privacy, shown at checkout."), "SvMuted"));
        pol.add(policies);
        page.add(pol);

        page.add(MUi.buttons(Ui.primary(Lang.tr("Save settings"), "settings-save", e -> {
            if (name.getText().trim().length() == 0) {
                Ui.say("The store needs a name");
                return;
            }
            StoreDto wanted = new StoreDto(s.id(), s.handle(), name.getText().trim(),
                    tagline.getText().trim(), description.getText(), MUi.code(currency, CURRENCIES),
                    accent, logoPhotoId, heroPhotoId, s.ownerUsername(), s.status(),
                    inclusive.isSelected(), email.getText().trim(), phone.getText().trim(),
                    address.getText().trim(), policies.getText());
            Api.stores().saveSettings(MerchantState.storeId(), wanted, Net.to(saved -> {
                Ui.say("Saved");
                MerchantState.updated(saved);
            }, Ui::fail));
        })));

        Ui.section(page, "Webhooks");
        Container hooks = new Container(BoxLayout.y());
        hooks.setUIID("MeCard");
        hooks.add(Ui.tap(Ui.row(FontImage.MATERIAL_WEBHOOK, "Webhooks",
                "Send order, product and stock events to another system", null),
                "settings-webhooks", e -> new WebhooksPage(this).show()));
        page.add(hooks);

        Ui.section(page, "Fraud filter rules");
        final Container rules = new Container(BoxLayout.y());
        rules.setUIID("MeCard");
        page.add(rules);

        Ui.section(page, "Locations");
        final Container locations = new Container(BoxLayout.y());
        locations.setUIID("MeCard");
        page.add(locations);
        Ui.section(page, "Staff");
        final Container staff = new Container(BoxLayout.y());
        staff.setUIID("MeCard");
        page.add(staff);
        root(page);
        loadLocations(locations);
        loadStaff(staff);
        loadRules(rules);
    }

    private static final String[] RULE_KINDS = {"TOTAL_OVER", "ORDERS_PER_HOUR", "COUNTRY_MISMATCH"};
    private static final String[] RULE_ACTIONS = {"HOLD", "CANCEL"};

    /// The store's fraud filter rules: each a condition and whether a match
    /// holds the order for review or cancels it.
    private void loadRules(final Container to) {
        Api.risk().fraudRules(MerchantState.storeId(), Net.to(found -> showRules(to, found), Ui::fail));
    }

    private void showRules(final Container to, List<FraudRuleDto> found) {
        to.removeAll();
        if (found.isEmpty()) {
            to.add(Ui.text(Lang.tr("No rules. Orders are only scored."), "SvMuted"));
        }
        for (final FraudRuleDto r : found) {
            String limit = "TOTAL_OVER".equals(r.kind())
                    ? MerchantState.money(r.threshold())
                    : "ORDERS_PER_HOUR".equals(r.kind()) ? String.valueOf(r.threshold()) : "";
            Container row = Ui.row(FontImage.MATERIAL_SECURITY, Lang.tr(Words.of(r.kind()))
                    + (limit.length() == 0 ? "" : " " + limit), Lang.tr(Words.of(r.action())), null);
            row.setName("fraud-rule-" + r.id());
            RowMenus.attach(row, Command.create(Lang.tr("Remove"), null, e -> Api.risk()
                    .removeFraudRule(MerchantState.storeId(), r.id(),
                            Net.to(left -> showRules(to, left), Ui::fail))));
            to.add(row);
        }
        to.add(Ui.secondary(Lang.tr("Add rule"), "fraud-rule-add", e -> addRule(to)));
        Ui.relayout(to);
    }

    private void addRule(final Container to) {
        Container body = new Container(BoxLayout.y());
        Picker kind = MUi.picker(body, Lang.tr("Condition"), "fraud-rule-kind", RULE_KINDS, "TOTAL_OVER");
        TextField threshold = MUi.number(body, Lang.tr("Threshold (amount or orders)"),
                "fraud-rule-threshold", 0);
        Picker action = MUi.picker(body, Lang.tr("Action"), "fraud-rule-action", RULE_ACTIONS, "HOLD");
        Command ok = new com.codename1.ui.Command(Lang.tr("Add"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Fraud filter rule"), body, cancel, ok) != ok) {
            return;
        }
        String k = MUi.code(kind, RULE_KINDS);
        long limit = "TOTAL_OVER".equals(k) ? MUi.cents(threshold) : wholeNumber(threshold.getText());
        Api.risk().addFraudRule(MerchantState.storeId(),
                new FraudRuleDto(null, k, limit, MUi.code(action, RULE_ACTIONS)),
                Net.to(all -> showRules(to, all), Ui::fail));
    }

    /// The digits of `text` as a number; 0 when it has none.
    static long wholeNumber(String text) {
        long out = 0;
        for (int iter = 0; iter < text.length() && out < 100000000L; iter++) {
            char c = text.charAt(iter);
            if (c >= '0' && c <= '9') {
                out = out * 10 + (c - '0');
            }
        }
        return out;
    }

    static boolean light(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return r * 299 + g * 587 + b * 114 > 150000;
    }

    private void loadLocations(final Container to) {
        Api.stores().locations(MerchantState.storeId(), Net.to(found -> {
            to.removeAll();
            for (final LocationDto l : found) {
                Container tap = Ui.tap(Ui.row(FontImage.MATERIAL_STORE, l.name(),
                        Lang.tr(Words.of(l.kind())) + (l.fulfilsOnline() ? Ui.DOT
                        + Lang.tr("Ships online orders") : ""), MUi.badge(l.active() ? "ACTIVE" : "DISABLED")),
                        "location-" + l.id(), e -> location(to, l, found.size()));
                RowMenus.attach(tap, Command.create(Lang.tr("Remove"), null, e -> Api.stores()
                        .removeLocation(MerchantState.storeId(), l.id(),
                                Net.to(x -> loadLocations(to), Ui::fail))));
                to.add(tap);
            }
            to.add(Ui.secondary(Lang.tr("Add location"), "location-add",
                    e -> location(to, null, found.size())));
            Ui.relayout(to);
        }, Ui::fail));
    }

    private void location(final Container to, LocationDto l, int count) {
        Container body = new Container(BoxLayout.y());
        TextField name = MUi.text(body, "Name", "location-name", l == null ? "" : l.name());
        Picker kind = MUi.picker(body, Lang.tr("Kind"), "location-kind",
                new String[] {"WAREHOUSE", "RETAIL"}, l == null ? "WAREHOUSE" : l.kind());
        TextField address = MUi.text(body, "Address", "location-address", l == null ? "" : l.address());
        CheckBox online = new CheckBox(Lang.tr("Ships online orders"));
        online.setSelected(l == null || l.fulfilsOnline());
        CheckBox active = new CheckBox(Lang.tr("Active"));
        active.setSelected(l == null || l.active());
        body.add(online).add(active);
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Location"), body, cancel, ok) != ok
                || name.getText().trim().length() == 0) {
            return;
        }
        LocationDto wanted = new LocationDto(l == null ? null : l.id(), name.getText().trim(),
                MUi.code(kind, new String[] {"WAREHOUSE", "RETAIL"}), address.getText().trim(),
                online.isSelected(), active.isSelected(), l == null ? count : l.position());
        if (l == null) {
            Api.stores().addLocation(MerchantState.storeId(), wanted, Net.to(x -> loadLocations(to), Ui::fail));
        } else {
            Api.stores().saveLocation(MerchantState.storeId(), l.id(), wanted,
                    Net.to(x -> loadLocations(to), Ui::fail));
        }
    }

    private void loadStaff(final Container to) {
        Api.stores().staff(MerchantState.storeId(), Net.to(found -> {
            to.removeAll();
            for (final StaffDto st : found) {
                Container tap = Ui.tap(Ui.row(FontImage.MATERIAL_BADGE, st.displayName(),
                        st.username() + (st.hasPin() ? Ui.DOT + Lang.tr("PIN set") : ""),
                        Ui.plain(Lang.tr(Words.role(st.role())), "SvRowValue")),
                        "staff-" + st.username(), e -> staff(to, st));
                if (!"OWNER".equals(st.role())) {
                    RowMenus.attach(tap, Command.create(Lang.tr("Remove"), null, e -> Api.stores()
                            .removeStaff(MerchantState.storeId(), st.id(), Net.to(x -> loadStaff(to), Ui::fail))));
                }
                to.add(tap);
            }
            to.add(Ui.secondary(Lang.tr("Add staff"), "staff-add", e -> staff(to, null)));
            Ui.relayout(to);
        }, Ui::fail));
    }

    /// A PIN is four to eight digits; empty keeps the current one.
    static boolean pinOk(String pin) {
        if (pin.length() == 0) {
            return true;
        }
        if (pin.length() < 4 || pin.length() > 8) {
            return false;
        }
        for (int iter = 0; iter < pin.length(); iter++) {
            if (pin.charAt(iter) < '0' || pin.charAt(iter) > '9') {
                return false;
            }
        }
        return true;
    }

    private void staff(final Container to, StaffDto st) {
        Container body = new Container(BoxLayout.y());
        TextField user = MUi.text(body, "Username", "staff-username", st == null ? "" : st.username());
        user.setEditable(st == null);
        TextField display = MUi.text(body, "Name", "staff-name", st == null ? "" : st.displayName());
        Picker role = MUi.picker(body, Lang.tr("Role"), "staff-role", Words.ROLES,
                st == null ? "CASHIER" : st.role());
        TextField pin = Ui.field(body, Lang.tr("Register PIN (4-8 digits, empty to keep)"), "staff-pin",
                "", TextField.NUMERIC | TextField.PASSWORD);
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Staff member"), body, cancel, ok) != ok
                || user.getText().trim().length() == 0) {
            return;
        }
        String p = pin.getText().trim();
        if (!pinOk(p)) {
            Ui.say("A PIN is four to eight digits");
            return;
        }
        Api.stores().saveStaff(MerchantState.storeId(), new StaffDto(st == null ? null : st.id(),
                user.getText().trim(), display.getText().trim(), MUi.code(role, Words.ROLES),
                st == null ? null : st.locationId(), p.length() == 0 ? null : p,
                st != null && st.hasPin()), Net.to(x -> loadStaff(to), Ui::fail));
    }
}
