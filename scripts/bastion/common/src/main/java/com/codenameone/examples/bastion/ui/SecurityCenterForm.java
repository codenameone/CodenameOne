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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codenameone.examples.bastion.security.SecurityStatus;

import java.util.List;

/// Every protection the app has, and whether it is in force here, now. See
/// [SecurityStatus] for where each answer comes from.
public final class SecurityCenterForm {
    private SecurityCenterForm() {
    }

    public static void show(final Runnable back) {
        Form form = Ui.form("Security Center", back);
        form.add(Ui.muted(Lang.tr("What protects your account on this device. "
                + "Nothing here is decoration: each line is read from the app as it runs.")));
        List<SecurityStatus.Item> items = SecurityStatus.items();
        String group = null;
        for (SecurityStatus.Item item : items) {
            if (!item.group.equals(group)) {
                group = item.group;
                form.add(new Label(Lang.tr(group), "BnLabel"));
            }
            form.add(row(item));
        }
        Ui.show(form, false);
    }

    private static Container row(SecurityStatus.Item item) {
        return Ui.row(Lang.tr(item.name), Lang.tr(item.detail, item.values),
                Lang.tr(stateName(item.state)), stateStyle(item.state), null);
    }

    static String stateName(int state) {
        switch (state) {
            case SecurityStatus.ACTIVE:
                return "Active";
            case SecurityStatus.SIMULATOR:
                return "Not in simulator";
            case SecurityStatus.CLOUD_BUILD:
                return "Cloud build";
            case SecurityStatus.WARNING:
                return "Warning";
            default:
                return "Off";
        }
    }

    private static String stateStyle(int state) {
        switch (state) {
            case SecurityStatus.ACTIVE:
                return "BnBadgeGood";
            case SecurityStatus.WARNING:
                return "BnBadgeBad";
            default:
                return "BnBadgeWarn";
        }
    }
}
