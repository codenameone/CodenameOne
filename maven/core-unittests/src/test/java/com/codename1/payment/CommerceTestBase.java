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
package com.codename1.payment;

import com.codename1.analytics.AbstractAnalyticsProvider;
import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsCapability;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.analytics.ConsentMode;
import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// What the commerce tests share: a cold manager pointed at a faked cloud,
/// and helpers to answer its requests and to look into a component tree.
public abstract class CommerceTestBase extends UITestBase {
    protected static final String ENDPOINT = "http://commerce.test/api";

    /// SKUs the faked store was asked to subscribe to or purchase.
    protected final List<String> storeCalls = new ArrayList<String>();

    @BeforeEach
    void setUpCommerce() throws Exception {
        Storage.setStorageInstance(null);
        Storage.getInstance().clearStorage();
        Storage.getInstance().clearCache();
        Preferences.clearAll();
        storeCalls.clear();
        implementation.clearNetworkMocks();
        implementation.setInAppPurchase(createStore());
        Display d = Display.getInstance();
        d.setProperty("build_key", "test-build-key");
        d.setProperty("package_name", "com.example.app");
        d.setProperty("commerce.cloud.endpoint", ENDPOINT);
        d.setProperty("commerce.cloud.enabled", "true");
        d.setProperty("commerce.cloud.publicKey", "");
        CommerceManager.resetForTests();
    }

    @AfterEach
    void tearDownCommerce() {
        Display d = Display.getInstance();
        d.setProperty("build_key", "");
        d.setProperty("commerce.cloud.enabled", "true");
        implementation.clearNetworkMocks();
        implementation.setInAppPurchase(null);
        CommerceManager.resetForTests();
        Analytics.clearProviders();
        Analytics.setConsent(null);
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.attributionAvailable(false);
    }

    /// Registers a provider that records every event, with the user's
    /// consent on record. Without this call a test has no analytics at all,
    /// which is the state of an application that never registered a provider.
    protected static AnalyticsRecorder analyticsOn() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.granted());
        AnalyticsRecorder recorder = new AnalyticsRecorder();
        Analytics.addProvider(recorder);
        return recorder;
    }

    /// An analytics provider that keeps the events it is handed.
    public static final class AnalyticsRecorder extends AbstractAnalyticsProvider {
        private final List<AnalyticsEvent> events = new ArrayList<AnalyticsEvent>();

        @Override
        public String getName() {
            return "commerce-recorder";
        }

        @Override
        public void trackEvent(AnalyticsEvent event) {
            events.add(event);
        }

        @Override
        public boolean supports(AnalyticsCapability capability) {
            return true;
        }

        public int count(String name) {
            int n = 0;
            for (AnalyticsEvent e : events) {
                if (name.equals(e.getName())) {
                    n++;
                }
            }
            return n;
        }

        /// The last event of a name, or null.
        public AnalyticsEvent last(String name) {
            AnalyticsEvent found = null;
            for (AnalyticsEvent e : events) {
                if (name.equals(e.getName())) {
                    found = e;
                }
            }
            return found;
        }

        /// A parameter of the last event of a name; null when there is no
        /// such event or it has no such parameter.
        public Object param(String name, String key) {
            AnalyticsEvent e = last(name);
            return e == null ? null : e.getParameters().get(key);
        }
    }

    /// The store of the test: it records what it is asked to sell.
    protected Purchase createStore() {
        return new Purchase() {
            @Override
            public void subscribe(String sku) {
                storeCalls.add("subscribe:" + sku);
            }

            @Override
            public void purchase(String sku) {
                storeCalls.add("purchase:" + sku);
            }
        };
    }

    protected void mock(String path, String payload) {
        implementation.addNetworkMockResponse(ENDPOINT + path, 200, "OK",
                envelope(payload).getBytes(StandardCharsets.UTF_8));
    }

    protected void mockFailure(String path) {
        implementation.addNetworkMockResponse(ENDPOINT + path, 500, "Error", new byte[0]);
    }

    /// The unsigned envelope of the contract around a payload.
    protected static String envelope(String payload) {
        StringBuilder sb = new StringBuilder("{\"payload\":\"");
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append("\",\"signature\":null,\"keyId\":null}");
        return sb.toString();
    }

    protected static Offerings offerings(String json) throws Exception {
        return new Offerings(JSONParser.parseJSON(json));
    }

    protected static void attach(CommercePackage pkg, String name, String price) {
        Product p = new Product();
        p.setSku(pkg.getProductId());
        p.setDisplayName(name);
        p.setLocalizedPrice(price);
        pkg.setProduct(p);
    }

    /// Every component under a root whose UIID, or text UIID for a span
    /// label, is the given one.
    protected static List<Component> byUiid(Component root, String uiid) {
        List<Component> out = new ArrayList<Component>();
        collect(root, uiid, out);
        return out;
    }

    private static void collect(Component c, String uiid, List<Component> out) {
        if (c instanceof SpanLabel) {
            if (uiid.equals(((SpanLabel) c).getTextUIID())) {
                out.add(c);
            }
            return;
        }
        if (uiid.equals(c.getUIID())) {
            out.add(c);
        }
        if (c instanceof Container) {
            Container cnt = (Container) c;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                collect(cnt.getComponentAt(i), uiid, out);
            }
        }
    }

    protected static Component byName(Component c, String name) {
        if (name.equals(c.getName())) {
            return c;
        }
        if (c instanceof Container) {
            Container cnt = (Container) c;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                Component found = byName(cnt.getComponentAt(i), name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
