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
package com.codename1.payment.paywall;

import com.codename1.payment.CommercePackage;
import com.codename1.payment.Offering;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Shows the paywall of an offering as a form of its own.
///
/// ```java
/// Offering offering = CommerceManager.getInstance().getOfferings().getCurrent();
/// Paywall.show(offering, listener);
/// ```
public final class Paywall {
    private Paywall() {
    }

    /// Shows the paywall of the offering in a new form over the current one.
    /// Closing the paywall, or going back, returns to the form that was
    /// showing. An offering with no paywall document is shown with
    /// [the built-in one][#createDefault(Offering)]. The form's UIID is
    /// `PaywallForm`.
    ///
    /// Call on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `offering`: the offering to sell
    ///
    /// - `listener`: told what happens on the paywall, or null
    ///
    /// #### Returns
    ///
    /// the view that was shown
    public static PaywallView show(Offering offering, PaywallListener listener) {
        final Form previous = Display.getInstance().getCurrent();
        final PaywallView view = new PaywallView(offering);
        view.setPaywallListener(listener);
        Form form = new Form(new BorderLayout());
        form.setUIID("PaywallForm");
        form.add(BorderLayout.CENTER, view);
        view.setCloseHandler(new Runnable() {
            @Override
            public void run() {
                if (previous != null) {
                    previous.showBack();
                }
            }
        });
        form.setBackCommand(new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                view.close();
            }
        });
        form.show();
        return view;
    }

    /// The built-in paywall document: a close button, a title, the packages
    /// of the offering as a list, a purchase button, a restore button and a
    /// footer. It names no colors, so the theme decides them all.
    ///
    /// #### Parameters
    ///
    /// - `offering`: the offering the document is for; may be null
    ///
    /// #### Returns
    ///
    /// a new paywall document, which the caller may change
    public static Map<String, Object> createDefault(Offering offering) {
        List<Object> children = new ArrayList<Object>();
        children.add(node("closeButton"));
        Map<String, Object> title = node("text");
        title.put("text", "Choose your plan");
        title.put("style", "title");
        title.put("align", "center");
        children.add(title);
        Map<String, Object> packages = node("packages");
        packages.put("style", "list");
        if (offering != null) {
            List<CommercePackage> all = offering.getPackages();
            if (!all.isEmpty() && all.get(0).getId() != null) {
                packages.put("selected", all.get(0).getId());
            }
        }
        children.add(packages);
        Map<String, Object> buy = node("purchaseButton");
        buy.put("text", "Continue");
        children.add(buy);
        Map<String, Object> restore = node("restoreButton");
        restore.put("text", "Restore purchases");
        children.add(restore);
        children.add(node("footer"));

        Map<String, Object> root = node("stack");
        root.put("axis", "y");
        root.put("gap", Integer.valueOf(2));
        root.put("children", children);

        Map<String, Object> doc = new HashMap<String, Object>();
        doc.put("version", Integer.valueOf(1));
        doc.put("id", "default");
        doc.put("defaultLocale", "en");
        doc.put("root", root);
        return doc;
    }

    private static Map<String, Object> node(String type) {
        Map<String, Object> n = new HashMap<String, Object>();
        n.put("type", type);
        return n;
    }
}
