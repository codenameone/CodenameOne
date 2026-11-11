/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.payment.center;

import com.codename1.io.Log;
import com.codename1.payment.CommerceManager;
import com.codename1.payment.CustomerInfo;
import com.codename1.payment.Purchase;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The customer center: a form listing the customer's active subscriptions
/// and what can be done about them.
///
/// The console configures which paths the form offers:
///
/// - `MANAGE` opens the store's own subscription management, or the
///   customer's management page where the platform has none
/// - `RESTORE` restores the purchases of the store account
/// - `REFUND` asks the store for a refund, one row per subscription the
///   store can refund
/// - `CANCEL` asks why, reports the answer, shows the retention offer
///   configured for that reason if there is one, and otherwise continues to
///   the store's subscription management. Offered while a subscription is
///   active.
/// - `SUPPORT` opens the support page, or an email to the support address.
///   Offered when one of the two is configured.
///
/// When the configuration cannot be fetched the form offers `MANAGE` and
/// `RESTORE`.
///
/// #### Styling
///
/// - `CustomerCenterForm`: the form
/// - `CustomerCenterHeader`: the heading above the subscriptions
/// - `CustomerCenterSubscription`: one active subscription
/// - `CustomerCenterEmpty`: the line shown when no subscription is active
/// - `CustomerCenterRow`: the button of one path
/// - `CustomerCenterReason`: one cancel reason
public final class CustomerCenter {
    /// The path that opens subscription management.
    public static final String PATH_MANAGE = "MANAGE";
    /// The path that restores purchases.
    public static final String PATH_RESTORE = "RESTORE";
    /// The path that asks for a refund.
    public static final String PATH_REFUND = "REFUND";
    /// The path that cancels a subscription.
    public static final String PATH_CANCEL = "CANCEL";
    /// The path that reaches support.
    public static final String PATH_SUPPORT = "SUPPORT";

    private CustomerCenter() {
    }

    /// One thing the customer center offers to do.
    static final class Row {
        final String path;
        final String label;
        final String sku;

        Row(String path, String label, String sku) {
            this.path = path;
            this.label = label;
            this.sku = sku;
        }
    }

    /// Fetches the customer center configuration and shows the form over
    /// the current one; going back returns there. May be called on any
    /// thread: the form is shown on the EDT, which is not blocked while the
    /// configuration is fetched.
    public static void show() {
        Display d = Display.getInstance();
        if (!d.isEdt()) {
            d.callSerially(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
            return;
        }
        final Map[] fetched = new Map[1];
        d.invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                try {
                    fetched[0] = CommerceManager.getInstance().getCustomerCenterConfiguration();
                } catch (Throwable t) {
                    Log.p("CustomerCenter: no configuration: " + t);
                }
            }
        });
        Map config = fetched[0] == null ? defaultConfiguration() : fetched[0];
        createForm(config, CommerceManager.getInstance().getCustomerInfo(),
                Purchase.getInAppPurchase(), d.getCurrent()).show();
    }

    /// The configuration used when the cloud's cannot be fetched.
    static Map defaultConfiguration() {
        Map<String, Object> config = new HashMap<String, Object>();
        List<Object> paths = new ArrayList<Object>();
        paths.add(PATH_MANAGE);
        paths.add(PATH_RESTORE);
        config.put("paths", paths);
        return config;
    }

    /// The rows of a configuration for a customer, in the configured order.
    /// A path the client does not know is skipped.
    static List<Row> buildRows(Map config, CustomerInfo info, Purchase store) {
        List<Row> rows = new ArrayList<Row>();
        List<String> active = info == null ? new ArrayList<String>() : info.getActiveSubscriptions();
        String firstSku = active.isEmpty() ? null : active.get(0);
        Object paths = config == null ? null : config.get("paths");
        if (!(paths instanceof List)) {
            return rows;
        }
        for (Object o : (List) paths) {
            String path = o == null ? "" : o.toString();
            if (PATH_MANAGE.equalsIgnoreCase(path)) {
                rows.add(new Row(PATH_MANAGE, string(config, "path.MANAGE", "Manage subscription"), firstSku));
            } else if (PATH_RESTORE.equalsIgnoreCase(path)) {
                rows.add(new Row(PATH_RESTORE, string(config, "path.RESTORE", "Restore purchases"), null));
            } else if (PATH_REFUND.equalsIgnoreCase(path)) {
                String label = string(config, "path.REFUND", "Request a refund");
                for (String sku : active) {
                    if (store != null && store.isRefundable(sku)) {
                        rows.add(new Row(PATH_REFUND, active.size() > 1 ? label + " (" + sku + ")" : label, sku));
                    }
                }
            } else if (PATH_CANCEL.equalsIgnoreCase(path)) {
                if (firstSku != null) {
                    rows.add(new Row(PATH_CANCEL, string(config, "path.CANCEL", "Cancel subscription"), firstSku));
                }
            } else if (PATH_SUPPORT.equalsIgnoreCase(path)) {
                if (supportAddress(config) != null) {
                    rows.add(new Row(PATH_SUPPORT, string(config, "path.SUPPORT", "Contact support"), null));
                }
            }
        }
        return rows;
    }

    /// What the support row opens: the support page, else an email to the
    /// support address, else null.
    static String supportAddress(Map config) {
        String url = text(config.get("supportUrl"));
        if (url != null) {
            return url;
        }
        String email = text(config.get("supportEmail"));
        return email == null ? null : "mailto:" + email;
    }

    /// Builds the form of a configuration without showing it.
    static Form createForm(final Map config, CustomerInfo info, final Purchase store, final Form previous) {
        final Form form = new Form(string(config, "title", "Subscriptions"), BoxLayout.y());
        form.setUIID("CustomerCenterForm");
        if (previous != null) {
            form.setBackCommand(new Command("") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    previous.showBack();
                }
            });
        }
        Label header = new Label(string(config, "subscriptions", "Active subscriptions"));
        header.setUIID("CustomerCenterHeader");
        form.add(header);
        List<String> active = info == null ? new ArrayList<String>() : info.getActiveSubscriptions();
        for (String sku : active) {
            Label l = new Label(sku);
            l.setUIID("CustomerCenterSubscription");
            form.add(l);
        }
        if (active.isEmpty()) {
            Label none = new Label(string(config, "none", "No active subscription"));
            none.setUIID("CustomerCenterEmpty");
            form.add(none);
        }
        for (final Row row : buildRows(config, info, store)) {
            Button b = new Button(row.label);
            b.setUIID("CustomerCenterRow");
            b.setName("CustomerCenterRow_" + row.path);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    perform(row, config, store, form);
                }
            });
            form.add(b);
        }
        return form;
    }

    private static void perform(Row row, Map config, Purchase store, Form form) {
        if (PATH_MANAGE.equals(row.path)) {
            manage(store, row.sku);
        } else if (PATH_RESTORE.equals(row.path)) {
            CommerceManager.getInstance().restore();
        } else if (PATH_REFUND.equals(row.path)) {
            if (store != null) {
                store.refund(row.sku);
            }
        } else if (PATH_CANCEL.equals(row.path)) {
            cancel(config, store, row.sku, form);
        } else if (PATH_SUPPORT.equals(row.path)) {
            String address = supportAddress(config);
            if (address != null) {
                Display.getInstance().execute(address);
            }
        }
    }

    /// Opens the store's subscription management, or the customer's
    /// management page on a platform with none.
    private static void manage(Purchase store, String sku) {
        if (store != null && store.isManageSubscriptionsSupported()) {
            store.manageSubscriptions(sku);
            return;
        }
        // A web subscription is managed in a billing portal the cloud has
        // to be asked for, which must not happen on the EDT.
        final String[] url = new String[1];
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                url[0] = CommerceManager.getInstance().fetchManagementUrl();
            }
        });
        if (url[0] != null && url[0].length() > 0) {
            Display.getInstance().execute(url[0]);
        }
    }

    /// The cancel reasons of a configuration that have an id, each as its
    /// parsed JSON object.
    static List<Map> cancelReasons(Map config) {
        List<Map> out = new ArrayList<Map>();
        Object reasons = config.get("cancelReasons");
        if (reasons instanceof List) {
            for (Object o : (List) reasons) {
                if (o instanceof Map && text(((Map) o).get("id")) != null) {
                    out.add((Map) o);
                }
            }
        }
        return out;
    }

    private static void cancel(final Map config, final Purchase store, final String sku, final Form center) {
        List<Map> reasons = cancelReasons(config);
        if (reasons.isEmpty()) {
            manage(store, sku);
            return;
        }
        Form picker = new Form(string(config, "cancel.title", "Why are you leaving?"), BoxLayout.y());
        picker.setUIID("CustomerCenterForm");
        picker.setBackCommand(new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                center.showBack();
            }
        });
        for (final Map reason : reasons) {
            String id = text(reason.get("id"));
            String label = text(reason.get("label"));
            Button b = new Button(string(config, "reason." + id, label == null ? id : label));
            b.setUIID("CustomerCenterReason");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    reasonChosen(config, reason, store, sku);
                    center.showBack();
                }
            });
            picker.add(b);
        }
        picker.show();
    }

    private static void reasonChosen(Map config, Map reason, Purchase store, String sku) {
        CommerceManager cm = CommerceManager.getInstance();
        cm.logEvent("CANCEL_REASON", null, null, null, text(reason.get("id")));
        Object offer = reason.get("offer");
        if (offer instanceof Map) {
            String productId = text(((Map) offer).get("productId"));
            String offerText = text(((Map) offer).get("text"));
            if (productId != null && offerText != null) {
                boolean accepted = Dialog.show(string(config, "offer.title", "Before you go"), offerText,
                        string(config, "offer.accept", "Accept offer"),
                        string(config, "offer.decline", "No thanks"));
                if (accepted) {
                    // An offer that names a promotional offer is sold under
                    // it where the store has such offers and the cloud signs
                    // it; anywhere else the product is sold as it is.
                    String offerId = text(((Map) offer).get("offerId"));
                    if (offerId == null || !cm.purchaseWithPromotionalOffer(productId, offerId)) {
                        cm.subscribe(productId);
                    }
                    return;
                }
            }
        }
        manage(store, sku);
    }

    /// A string of the configuration by key: the device's language and
    /// country, then its language, then English, then the fallback.
    static String string(Map config, String key, String fallback) {
        Object strings = config == null ? null : config.get("strings");
        if (!(strings instanceof Map)) {
            return fallback;
        }
        Map all = (Map) strings;
        String language = null;
        String country = null;
        try {
            language = Display.getInstance().getLocalizationManager().getLanguage();
            country = Display.getInstance().getLocalizationManager().getLocale();
        } catch (Throwable t) {
            // no localization on this port: English
            Log.p("CustomerCenter: no locale: " + t);
        }
        String found = null;
        if (language != null && country != null) {
            found = entry(all.get(language + "_" + country), key);
        }
        if (found == null && language != null) {
            found = entry(all.get(language), key);
        }
        if (found == null) {
            found = entry(all.get("en"), key);
        }
        return found == null ? fallback : found;
    }

    private static String entry(Object table, String key) {
        if (!(table instanceof Map)) {
            return null;
        }
        return text(((Map) table).get(key));
    }

    private static String text(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString();
        return s.length() == 0 ? null : s;
    }
}
