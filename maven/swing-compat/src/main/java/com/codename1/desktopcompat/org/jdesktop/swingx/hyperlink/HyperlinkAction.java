/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.desktopcompat.org.jdesktop.swingx.hyperlink;

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.ui.Display;
import java.net.URI;

/// Opens its target address with whatever the device opens addresses
/// with: the browser for `http`, the mail client for `mailto`.
///
/// The address goes to Codename One's `Display.execute`. There is no
/// choice between browsing and mailing as on a desktop -- the scheme of
/// the address decides -- so the constructors and the accessor that take
/// a desktop action are absent. The action is marked visited once the
/// address was handed over; whether anything opened is not known.
public class HyperlinkAction extends AbstractHyperlinkAction<URI> {

    private static UrlOpener opener;

    /// An action for `uri`, which may be `null`.
    public static HyperlinkAction createHyperlinkAction(URI uri) {
        HyperlinkAction a = new HyperlinkAction();
        a.setTarget(uri);
        return a;
    }

    public HyperlinkAction() {
        super();
    }

    /// Replaces what opens addresses; `null` goes back to Codename One.
    static void setOpener(UrlOpener o) {
        opener = o;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        URI uri = getTarget();
        if (uri == null) {
            return;
        }
        String address = uri.toString();
        UrlOpener o = opener;
        if (o != null) {
            o.open(address);
        } else if (Display.isInitialized()) {
            Display.getInstance().execute(address);
        } else {
            return;
        }
        setVisited(true);
    }

    /// Names the action after the address and enables it only while there
    /// is one.
    @Override
    protected void installTarget() {
        super.installTarget();
        setEnabled(getTarget() != null);
    }
}
