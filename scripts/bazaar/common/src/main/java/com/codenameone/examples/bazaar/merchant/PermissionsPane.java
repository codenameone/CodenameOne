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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.PermissionSetDto;
import com.codenameone.examples.bazaar.api.StaffPermissionDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// Permission sets and who has them. A set is read or write per area; a
/// staff member with one assigned is held to it instead of their role.
final class PermissionsPane extends MerchantPane {
    static final String[] LEVELS = {"N", "R", "W"};

    PermissionsPane() {
        super("Permissions", "merchant-permissions");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "New set", "permissions-new", this::create);
        Api.ops().permissionSets(MerchantState.storeId(), Net.to(sets -> {
            final Container page = new Container(BoxLayout.y());
            page.setName("permissions-page");
            for (int iter = 0; iter < sets.size(); iter++) {
                PermissionSetDto s = sets.get(iter);
                page.add(Ui.row((char) 0, s.name(), s.grants(), Ui.plain("", "SvRowValue")));
            }
            Api.ops().staffPermissions(MerchantState.storeId(), Net.to(as -> {
                Rows<StaffPermissionDto> rows = new Rows<StaffPermissionDto>()
                        .col(Lang.tr("Staff"), StaffPermissionDto::username)
                        .col(Lang.tr("Role"), a -> Lang.tr(Words.of(a.role())))
                        .col(Lang.tr("Location"), a -> a.locationId().length() == 0
                                ? Lang.tr("Every location") : a.locationId())
                        .col(Lang.tr("Set"), StaffPermissionDto::setName);
                rows.set(as);
                page.add(MUi.grid(rows, "permissions-grid"));
                Ui.relayout(page);
            }, Ui::fail));
            root(page);
        }, Ui::fail));
    }

    private void create() {
        Container form = new Container(BoxLayout.y());
        form.setName("permission-set-form");
        final TextField name = MUi.text(form, Lang.tr("Name"), "permission-set-name", "");
        final Picker[] levels = new Picker[Perms.AREAS.length];
        for (int iter = 0; iter < levels.length; iter++) {
            levels[iter] = MUi.picker(form, Lang.tr(Words.of(Perms.AREAS[iter])),
                    "permission-" + Perms.AREAS[iter], LEVELS, "N");
        }
        form.add(Ui.primary(Lang.tr("Create"), "permission-set-save", e -> {
            char[] chosen = new char[levels.length];
            for (int iter = 0; iter < levels.length; iter++) {
                chosen[iter] = MUi.code(levels[iter], LEVELS).charAt(0);
            }
            Api.ops().createPermissionSet(MerchantState.storeId(), new PermissionSetDto(null,
                    name.getText(), Perms.format(chosen)), Net.to(made -> {
                        pop();
                        load();
                    }, Ui::fail));
        }));
        push(Lang.tr("New permission set"), form);
    }
}
