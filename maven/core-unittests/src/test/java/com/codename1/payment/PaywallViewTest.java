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

import com.codename1.components.SpanLabel;
import com.codename1.junit.FormTest;
import com.codename1.payment.paywall.Paywall;
import com.codename1.payment.paywall.PaywallListener;
import com.codename1.payment.paywall.PaywallView;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaywallViewTest extends CommerceTestBase {
    private static final String PAYWALL =
            "{\"version\":1,\"id\":\"pw1\",\"defaultLocale\":\"en\","
                    + "\"strings\":{\"en\":{\"title.key\":\"Go Pro\"}},"
                    + "\"colors\":{\"light\":{\"background\":\"#101010\",\"text\":\"#222222\",\"muted\":\"#888888\","
                    + "\"accent\":\"#ff0000\",\"accentText\":\"#FFFFFF\"},\"dark\":{\"accent\":\"#00ff00\"}},"
                    + "\"root\":{\"type\":\"stack\",\"axis\":\"y\",\"gap\":2,\"padding\":3,\"children\":["
                    + "{\"type\":\"closeButton\"},"
                    + "{\"type\":\"image\",\"url\":\"https://img.test/hero.png\",\"height\":30},"
                    + "{\"type\":\"icon\",\"name\":\"star\"},"
                    + "{\"type\":\"text\",\"text\":\"title.key\",\"style\":\"title\",\"align\":\"center\"},"
                    + "{\"type\":\"text\",\"text\":\"Only {{price}} per {{ period }}\",\"style\":\"headline\"},"
                    + "{\"type\":\"text\",\"text\":\"Everything, everywhere\"},"
                    + "{\"type\":\"text\",\"text\":\"{{packageId}} {{unknown}} {{trialPeriod}}\",\"style\":\"caption\"},"
                    + "{\"type\":\"features\",\"items\":[{\"icon\":\"lock\",\"text\":\"Secure\"},{\"icon\":\"nope\",\"text\":\"Fast\"}]},"
                    + "{\"type\":\"packages\",\"style\":\"tiles\",\"selected\":\"annual\"},"
                    + "{\"type\":\"carousel\",\"children\":[{\"type\":\"text\",\"text\":\"Slide\"},{\"type\":\"hologram\"}]},"
                    + "{\"type\":\"purchaseButton\",\"text\":\"Buy {{productName}}\"},"
                    + "{\"type\":\"restoreButton\",\"text\":\"Restore\"},"
                    + "{\"type\":\"footer\",\"termsUrl\":\"https://t.test\",\"privacyUrl\":\"https://p.test\"},"
                    + "{\"type\":\"spacer\",\"size\":4},"
                    + "{\"type\":\"hologram\",\"text\":\"from the future\"},"
                    + "{\"type\":\"stack\",\"axis\":\"x\",\"align\":\"center\",\"children\":[{\"type\":\"icon\",\"name\":\"bolt\"}]}"
                    + "]}}";

    private static final String OFFERING =
            "{\"currentOfferingId\":\"main\",\"offerings\":[{\"id\":\"main\",\"paywall\":" + PAYWALL + ","
                    + "\"packages\":["
                    + "{\"id\":\"monthly\",\"type\":\"MONTHLY\",\"productId\":\"pro_monthly\",\"period\":\"P1M\",\"trialPeriod\":\"P7D\"},"
                    + "{\"id\":\"annual\",\"type\":\"ANNUAL\",\"productId\":\"pro_annual\",\"period\":\"P1Y\"}]},"
                    + "{\"id\":\"plain\",\"packages\":["
                    + "{\"id\":\"forever\",\"type\":\"LIFETIME\",\"productId\":\"pro_forever\"}]}]}";

    @BeforeEach
    void cloudOff() {
        // The views report events; none of these tests is about the cloud.
        Display.getInstance().setProperty("commerce.cloud.enabled", "false");
    }

    private static String text(Component c) {
        if (c instanceof SpanLabel) {
            return ((SpanLabel) c).getText();
        }
        return ((Button) c).getText();
    }

    @FormTest
    void buildsEveryNodeTypeAndSkipsUnknownOnes() throws Exception {
        Offering main = offerings(OFFERING).getCurrent();
        attach(main.getAnnual(), "Pro Annual", "$49.99");
        PaywallView view = new PaywallView(main);

        assertEquals("pw1", view.getPaywallId());
        assertSame(main, view.getOffering());
        assertEquals("Paywall", view.getUIID());
        assertEquals(0x101010, view.getUnselectedStyle().getBgColor());

        List<Component> titles = byUiid(view, "PaywallTitle");
        assertEquals(1, titles.size());
        assertEquals("Go Pro", text(titles.get(0)));
        assertEquals("Only $49.99 per year", text(byUiid(view, "PaywallHeadline").get(0)));
        // body: the plain text and the carousel's slide
        assertEquals(2, byUiid(view, "PaywallBody").size());
        // an unknown variable stays, a variable with no value is empty
        assertEquals("annual {{unknown}} ", text(byUiid(view, "PaywallCaption").get(0)));
        assertEquals(1, byUiid(view, "PaywallImage").size());
        assertEquals(2, byUiid(view, "PaywallIcon").size());
        assertEquals(2, byUiid(view, "PaywallFeature").size());
        assertEquals(2, byUiid(view, "PaywallFeatureIcon").size());
        assertEquals("Fast", text(byUiid(view, "PaywallFeatureText").get(1)));
        assertEquals(1, byUiid(view, "PaywallPackages").size());
        List<Component> carousel = byUiid(view, "PaywallCarousel");
        assertEquals(1, carousel.size());
        assertTrue(carousel.get(0) instanceof Tabs);
        assertEquals(1, ((Tabs) carousel.get(0)).getTabCount());
        List<Component> buy = byUiid(view, "PaywallPurchaseButton");
        assertEquals(1, buy.size());
        assertEquals("Buy Pro Annual", text(buy.get(0)));
        assertEquals(0xff0000, buy.get(0).getUnselectedStyle().getBgColor());
        assertEquals(0xffffff, buy.get(0).getUnselectedStyle().getFgColor());
        assertEquals("Restore", text(byUiid(view, "PaywallRestoreButton").get(0)));
        assertEquals(1, byUiid(view, "PaywallCloseButton").size());
        assertEquals(2, byUiid(view, "PaywallFooterLink").size());
        assertFalse(byUiid(view, "PaywallSpacer").isEmpty());
        assertNull(findText(view, "from the future"));
    }

    private static Component findText(Component root, String wanted) {
        String[] uiids = {"PaywallTitle", "PaywallHeadline", "PaywallBody", "PaywallCaption"};
        for (String uiid : uiids) {
            for (Component c : byUiid(root, uiid)) {
                if (wanted.equals(text(c))) {
                    return c;
                }
            }
        }
        return null;
    }

    @FormTest
    void selectingAPackageMovesTheSelectedUiidAndTheTexts() throws Exception {
        Offering main = offerings(OFFERING).getCurrent();
        attach(main.getAnnual(), "Pro Annual", "$49.99");
        PaywallView view = new PaywallView(main);
        Component annual = byName(view, "PaywallPackage_annual");
        Component monthly = byName(view, "PaywallPackage_monthly");
        assertEquals("annual", view.getSelectedPackage().getId());
        assertEquals("PaywallPackageSelected", annual.getUIID());
        assertEquals("PaywallPackage", monthly.getUIID());
        assertEquals(1, byUiid(view, "PaywallPackageTitleSelected").size());
        assertEquals("Pro Annual", text(byUiid(view, "PaywallPackageTitleSelected").get(0)));
        assertEquals(1, byUiid(view, "PaywallPackagePriceSelected").size());
        assertEquals(1, byUiid(view, "PaywallPackagePrice").size());

        view.selectPackage("monthly");
        assertEquals("monthly", view.getSelectedPackage().getId());
        assertEquals("PaywallPackage", annual.getUIID());
        assertEquals("PaywallPackageSelected", monthly.getUIID());
        assertEquals("Buy Monthly", text(byUiid(view, "PaywallPurchaseButton").get(0)));
        assertEquals("monthly {{unknown}} 7 days", text(byUiid(view, "PaywallCaption").get(0)));

        view.selectPackage("missing");
        assertEquals("monthly", view.getSelectedPackage().getId());
    }

    @FormTest
    void anOfferingWithNoDocumentGetsTheDefaultPaywall() throws Exception {
        Offering plain = offerings(OFFERING).getOffering("plain");
        assertNull(plain.getPaywall());
        Map<String, Object> doc = Paywall.createDefault(plain);
        assertEquals("default", doc.get("id"));
        PaywallView view = new PaywallView(plain);
        assertEquals("default", view.getPaywallId());
        assertEquals(1, byUiid(view, "PaywallTitle").size());
        assertEquals("forever", view.getSelectedPackage().getId());
        assertEquals("PaywallPackageSelected", byName(view, "PaywallPackage_forever").getUIID());
        assertEquals("Continue", text(byUiid(view, "PaywallPurchaseButton").get(0)));
        assertEquals(1, byUiid(view, "PaywallRestoreButton").size());
        assertEquals(1, byUiid(view, "PaywallFooter").size());

        // No offering at all still builds, with nothing to sell.
        PaywallView empty = new PaywallView(null);
        assertNull(empty.getSelectedPackage());
        empty.purchase();
        assertTrue(storeCalls.isEmpty());
    }

    @FormTest
    void purchaseAsksTheStoreAndClosingReportsItCancelled() throws Exception {
        Offering main = offerings(OFFERING).getCurrent();
        PaywallView view = new PaywallView(main);
        final List<String> heard = new ArrayList<String>();
        view.setPaywallListener(new PaywallListener() {
            @Override
            public void purchaseStarted(CommercePackage pkg) {
                heard.add("started:" + pkg.getId());
            }

            @Override
            public void purchaseCompleted(CustomerInfo info) {
                heard.add("completed");
            }

            @Override
            public void purchaseCancelled() {
                heard.add("cancelled");
            }

            @Override
            public void restoreCompleted(CustomerInfo info) {
                heard.add("restored");
            }

            @Override
            public void closed() {
                heard.add("closed");
            }
        });
        view.setCloseHandler(new Runnable() {
            @Override
            public void run() {
                heard.add("handler");
            }
        });
        view.purchase();
        assertEquals("[subscribe:pro_annual]", storeCalls.toString());
        view.close();
        view.close();
        assertEquals("[started:annual, cancelled, closed, handler]", heard.toString());
    }

    private static final String OWNS_ANNUAL =
            "{\"accepted\":true,\"degraded\":false,\"serverTime\":1700000000000,"
                    + "\"appUserId\":\"u1\",\"activeSubscriptions\":[\"pro_annual\"],"
                    + "\"entitlements\":{}}";

    private static PaywallListener recorder(final List<String> heard) {
        return new PaywallListener() {
            @Override
            public void purchaseStarted(CommercePackage pkg) {
                heard.add("started:" + pkg.getId());
            }

            @Override
            public void purchaseCompleted(CustomerInfo info) {
                heard.add("completed over " + Display.getInstance().getCurrent().getName());
            }

            @Override
            public void purchaseCancelled() {
                heard.add("cancelled");
            }

            @Override
            public void restoreCompleted(CustomerInfo info) {
                heard.add("restored");
            }

            @Override
            public void closed() {
                heard.add("closed");
            }
        };
    }

    @FormTest
    void shownPaywallReturnsToThePreviousFormWhenThePurchaseCompletes() throws Exception {
        Display.getInstance().setProperty("commerce.cloud.enabled", "true");
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        Form home = new Form("Home");
        home.setName("home");
        home.show();
        com.codename1.ui.DisplayTest.flushEdt();

        final List<String> heard = new ArrayList<String>();
        PaywallView view = Paywall.show(offerings(OFFERING).getCurrent(), recorder(heard));
        com.codename1.ui.DisplayTest.flushEdt();
        assertSame(view.getComponentForm(), Display.getInstance().getCurrent());

        view.purchase();
        mock("/customer", OWNS_ANNUAL);
        cm.refresh();
        com.codename1.ui.DisplayTest.flushEdt();

        assertEquals("[started:annual, completed over home, closed]", heard.toString());
        assertSame(home, Display.getInstance().getCurrent());
        // Already closed: closing again reports nothing.
        view.close();
        assertEquals(3, heard.size());
    }

    @FormTest
    void embeddedViewStaysWhenThePurchaseCompletes() throws Exception {
        Display.getInstance().setProperty("commerce.cloud.enabled", "true");
        CommerceManager cm = CommerceManager.getInstance();
        cm.setAppUserId("u1");
        PaywallView view = new PaywallView(offerings(OFFERING).getCurrent());
        Form own = new Form("Own", new com.codename1.ui.layouts.BorderLayout());
        own.setName("own");
        own.add(com.codename1.ui.layouts.BorderLayout.CENTER, view);
        own.show();
        com.codename1.ui.DisplayTest.flushEdt();

        final List<String> heard = new ArrayList<String>();
        view.setPaywallListener(recorder(heard));
        view.setCloseHandler(new Runnable() {
            @Override
            public void run() {
                heard.add("handler");
            }
        });
        view.purchase();
        mock("/customer", OWNS_ANNUAL);
        cm.refresh();
        com.codename1.ui.DisplayTest.flushEdt();

        assertEquals("[started:annual, completed over own]", heard.toString());
        assertSame(own, Display.getInstance().getCurrent());
    }
}
