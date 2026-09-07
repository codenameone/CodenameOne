/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.util.EasyThread;
import com.codename1.notifications.LocalNotification;
import com.codename1.ui.table.TableLayout;
import java.util.*;


class MiscellaneousFeaturesJava045Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    void snippet() throws Exception {
        // tag::miscellaneous-features-java-045[]
        Form hi = new Form("L10N", new TableLayout(16, 2));
        L10NManager l10n = L10NManager.getInstance();
        // the parsers take locale-formatted input -- a hard coded "34.35" is
        // read as 3435 anywhere "." groups digits -- so round-trip the
        // formatters' own output
        String localeDouble = l10n.format(34.35);
        String localeCurrency = l10n.formatCurrency(33.77);
        hi.add("format(double)").add(l10n.format(11.11)).
            add("format(int)").add(l10n.format(33)).
            add("formatCurrency").add(l10n.formatCurrency(53.267)).
            add("formatDateLongStyle").add(l10n.formatDateLongStyle(new Date())).
            add("formatDateShortStyle").add(l10n.formatDateShortStyle(new Date())).
            add("formatDateTime").add(l10n.formatDateTime(new Date())).
            add("formatDateTimeMedium").add(l10n.formatDateTimeMedium(new Date())).
            add("formatDateTimeShort").add(l10n.formatDateTimeShort(new Date())).
            add("getCurrencySymbol").add(l10n.getCurrencySymbol()).
            add("getLanguage").add(l10n.getLanguage()).
            add("getLocale").add(l10n.getLocale()).
            add("isRTLLocale").add("" + l10n.isRTLLocale()).
            add("parseCurrency").add(l10n.formatCurrency(l10n.parseCurrency(localeCurrency))).
            add("parseDouble").add(l10n.format(l10n.parseDouble(localeDouble))).
            add("parseInt").add(l10n.format(l10n.parseInt("56"))).
            add("parseLong").add("" + l10n.parseLong("4444444"));
        hi.show();
        // end::miscellaneous-features-java-045[]
    }
}
