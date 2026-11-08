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
package com.codenameone.examples.slate.owner;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.components.Switch;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.slate.api.LocationDto;
import com.codenameone.examples.slate.api.ProviderDto;
import com.codenameone.examples.slate.api.ResourceDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Split;
import com.codenameone.examples.slate.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/// The business's catalogue: services, providers, locations and the rooms
/// or equipment a service may need. One list screen and one editor per kind;
/// nothing is ever deleted, only switched off, so past appointments keep
/// the names they were booked under.
///
/// On a wide window the list keeps the left and the editor of the row picked
/// fills the right, so working down a catalogue never leaves the list; on a
/// phone each row opens its editor as a screen of its own.
public final class CatalogForm {
    static final String SERVICES = "services";
    static final String PROVIDERS = "providers";
    static final String LOCATIONS = "locations";
    static final String RESOURCES = "resources";

    private final Form form;
    private final Container list = Ui.list("catalog");
    private final String kind;
    private final Split split;

    private CatalogForm(Form previous, String kind) {
        this.kind = kind;
        form = Ui.form(SERVICES.equals(kind) ? "Services" : PROVIDERS.equals(kind) ? "Providers"
                : LOCATIONS.equals(kind) ? "Locations" : "Rooms and equipment", "Catalog");
        Ui.back(form, previous);
        Component add = PROVIDERS.equals(kind) ? null : Ui.primary("Add", "add", e -> edit(null));
        if (Layouts.wide()) {
            // Under the list it belongs to, not across the editor beside it.
            Container master = new Container(new BorderLayout());
            master.add(BorderLayout.CENTER, list);
            if (add != null) {
                master.add(BorderLayout.SOUTH, add);
            }
            split = new Split(form, null, master);
        } else {
            split = null;
            if (add != null) {
                form.add(BorderLayout.SOUTH, add);
            }
            form.add(BorderLayout.CENTER, list);
        }
        form.addShowListener(e -> load());
        Layouts.onChange(form, () -> new CatalogForm(previous, kind).form.show());
    }

    public static void show(Form previous, String kind) {
        new CatalogForm(previous, kind).form.show();
    }

    private void load() {
        Net.Failed failed = Ui.failed(list, this::load);
        if (SERVICES.equals(kind)) {
            Api.owner().services(Net.to(rows -> fill(new ArrayList<Object>(rows)), failed));
        } else if (PROVIDERS.equals(kind)) {
            Api.owner().providers(Net.to(rows -> fill(new ArrayList<Object>(rows)), failed));
        } else if (LOCATIONS.equals(kind)) {
            Api.owner().locations(Net.to(rows -> fill(new ArrayList<Object>(rows)), failed));
        } else {
            Api.owner().resources(Net.to(rows -> fill(new ArrayList<Object>(rows)), failed));
        }
    }

    private void fill(List<Object> rows) {
        list.removeAll();
        Container first = null;
        for (int iter = 0; iter < rows.size(); iter++) {
            final Object row = rows.get(iter);
            final Container[] tapped = new Container[1];
            tapped[0] = Ui.tap(Ui.row(icon(), title(row), detail(row), null), "row-" + iter,
                    e -> edit(row, tapped[0]));
            list.add(tapped[0]);
            if (first == null) {
                first = tapped[0];
            }
        }
        if (rows.isEmpty()) {
            list.add(Ui.empty(icon(), "Nothing here yet", "Add the first one below."));
        }
        Ui.relayout(list);
        if (split != null) {
            split.clear();
            if (first != null) {
                edit(rows.get(0), first);
            } else {
                edit(null);
            }
        }
    }

    private char icon() {
        return SERVICES.equals(kind) ? FontImage.MATERIAL_SPA : PROVIDERS.equals(kind)
                ? FontImage.MATERIAL_PERSON : LOCATIONS.equals(kind) ? FontImage.MATERIAL_PLACE
                : FontImage.MATERIAL_CHAIR;
    }

    private static String title(Object row) {
        if (row instanceof ServiceDto) {
            return ((ServiceDto) row).name();
        }
        if (row instanceof ProviderDto) {
            return ((ProviderDto) row).displayName();
        }
        if (row instanceof LocationDto) {
            return ((LocationDto) row).name();
        }
        return row instanceof ResourceDto ? ((ResourceDto) row).name() : "";
    }

    private static String detail(Object row) {
        if (row instanceof ServiceDto) {
            ServiceDto s = (ServiceDto) row;
            return Times.length(s.minutes()) + Ui.DOT + Ui.money(s.priceCents(), s.currency())
                    + (s.active() ? "" : Ui.DOT + "off");
        }
        if (row instanceof ProviderDto) {
            ProviderDto p = (ProviderDto) row;
            return (p.title() == null ? "" : p.title()) + (p.active() ? "" : Ui.DOT + "off");
        }
        if (row instanceof LocationDto) {
            LocationDto l = (LocationDto) row;
            return l.address() + Ui.DOT + l.timeZone();
        }
        return row instanceof ResourceDto ? ((ResourceDto) row).kind() : "";
    }

    private void edit(Object row) {
        edit(row, null);
    }

    /// Opens the editor of `row`, or of a new one when it is null: beside the
    /// list on a wide window, with `tapped` marked as the row picked; as a
    /// screen of its own anywhere else.
    private void edit(Object row, Container tapped) {
        if (split != null) {
            if (tapped != null) {
                split.pick(tapped);
            }
            Container detail = split.detail();
            detail.removeAll();
            detail.add(row == null ? Ui.label("Add", "SvHeading")
                    : Ui.plain(title(row), "SvHeading"));
            fields(detail, row);
            Layouts.dress(detail);
            Ui.relayout(detail);
            return;
        }
        final Form edit = Ui.form(row == null ? "Add" : title(row), "CatalogEdit");
        Ui.back(edit, form);
        Container page = Ui.page();
        fields(page, row);
        Layouts.broaden(page);
        edit.add(BorderLayout.CENTER, page);
        edit.show();
    }

    private void fields(Container page, Object row) {
        if (SERVICES.equals(kind)) {
            service(page, (ServiceDto) row);
        } else if (PROVIDERS.equals(kind)) {
            provider(page, (ProviderDto) row);
        } else if (LOCATIONS.equals(kind)) {
            location(page, (LocationDto) row);
        } else {
            resource(page, (ResourceDto) row);
        }
    }

    private void service(Container page, final ServiceDto s) {
        final TextField name = text(page, "Name", "name", s == null ? "" : s.name());
        final TextField minutes = number(page, "Minutes", "minutes", s == null ? 60 : s.minutes());
        final TextField price = number(page, "Price, in cents", "price",
                s == null ? 5000 : (int) s.priceCents());
        final TextField before = number(page, "Buffer before, minutes", "buffer-before",
                s == null ? 0 : s.bufferBefore());
        final TextField after = number(page, "Buffer after, minutes", "buffer-after",
                s == null ? 10 : s.bufferAfter());
        final TextField capacity = number(page, "Clients at once", "capacity",
                s == null ? 1 : s.capacity());
        final TextField deposit = number(page, "Deposit, percent (-1 uses the policy)", "deposit",
                s == null ? -1 : s.depositPercent());
        final Switch active = Ui.toggle("active", s == null || s.active(), on -> { });
        page.add(Ui.row((char) 0, "Bookable", "", active));
        page.add(Ui.primary("Save", "save", e -> Api.owner().saveService(new ServiceDto(
                s == null ? null : s.id(), name.getText(), s == null ? "" : s.description(),
                s == null ? "" : s.category(), num(minutes), num(price),
                s == null ? null : s.currency(), num(before), num(after), num(capacity),
                s == null ? 1 : s.providersNeeded(), s == null ? null : s.resourceKind(),
                s != null && s.online(), s == null ? -1 : s.minNoticeMinutes(), num(deposit),
                s == null ? null : s.color(), s == null ? "" : s.intakePrompt(), active.isOn(),
                s == null ? new ArrayList<String>() : s.providers()), saved())));
    }

    private void provider(Container page, final ProviderDto p) {
        final TextField name = text(page, "Name", "name", p.displayName());
        final TextField title = text(page, "Title", "title", p.title());
        final TextField perDay = number(page, "Most appointments a day (0 for no limit)",
                "max-per-day", p.maxPerDay());
        final TextField continuous = number(page, "Longest stretch without a break, minutes",
                "max-continuous", p.maxContinuousMinutes());
        final TextField rest = number(page, "Break after it, minutes", "break", p.breakMinutes());
        final Switch active = Ui.toggle("active", p.active(), on -> { });
        page.add(Ui.row((char) 0, "Takes bookings", "", active));
        page.add(Ui.primary("Save", "save", e -> Api.owner().saveProvider(new ProviderDto(
                p.username(), name.getText(), title.getText(), p.bio(), p.color(),
                p.defaultLocation(), num(perDay), num(continuous), num(rest), active.isOn(),
                p.services()), saved())));
    }

    private void location(Container page, final LocationDto l) {
        final TextField name = text(page, "Name", "name", l == null ? "" : l.name());
        final TextField address = text(page, "Address", "address", l == null ? "" : l.address());
        final TextField zone = text(page, "Time zone", "zone", l == null ? "UTC" : l.timeZone());
        final Switch active = Ui.toggle("active", l == null || l.active(), on -> { });
        page.add(Ui.row((char) 0, "Open", "", active));
        page.add(Ui.primary("Save", "save", e -> Api.owner().saveLocation(new LocationDto(
                l == null ? null : l.id(), name.getText(), address.getText(), zone.getText(),
                active.isOn()), saved())));
    }

    private void resource(Container page, final ResourceDto r) {
        final TextField name = text(page, "Name", "name", r == null ? "" : r.name());
        final TextField what = text(page, "Kind (services ask for a kind)", "kind",
                r == null ? "" : r.kind());
        final TextField where = text(page, "Location id", "location",
                r == null ? "loc-main" : r.locationId());
        final Switch active = Ui.toggle("active", r == null || r.active(), on -> { });
        page.add(Ui.row((char) 0, "In use", "", active));
        page.add(Ui.primary("Save", "save", e -> Api.owner().saveResource(new ResourceDto(
                r == null ? null : r.id(), where.getText(), what.getText(), name.getText(),
                active.isOn()), saved())));
    }

    private <T> com.codename1.util.OnComplete<com.codename1.io.rest.Response<T>> saved() {
        return Net.to(done -> {
            Ui.say("Saved");
            if (split != null) {
                load();
            } else {
                form.showBack();
            }
        }, Ui::fail);
    }

    static TextField text(Container page, String caption, String name, String value) {
        TextField field = Ui.field(page, caption, name, "");
        field.setText(value == null ? "" : value);
        return field;
    }

    static TextField number(Container page, String caption, String name, int value) {
        TextField field = Ui.field(page, caption, name, "", TextArea.NUMERIC);
        field.setText(String.valueOf(value));
        return field;
    }

    static int num(TextField field) {
        String t = field.getText().trim();
        boolean negative = t.startsWith("-");
        int out = 0;
        for (int iter = negative ? 1 : 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9' || out > 100000000) {
                break;
            }
            out = out * 10 + (c - '0');
        }
        return negative ? -out : out;
    }
}
