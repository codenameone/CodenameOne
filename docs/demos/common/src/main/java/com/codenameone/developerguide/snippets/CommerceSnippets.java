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
package com.codenameone.developerguide.snippets;

import com.codename1.payment.CommerceManager;
import com.codename1.payment.CommercePackage;
import com.codename1.payment.CustomerInfo;
import com.codename1.payment.EntitlementInfo;
import com.codename1.payment.Offering;
import com.codename1.payment.Offerings;
import com.codename1.payment.ReferralReward;
import com.codename1.payment.center.CustomerCenter;
import com.codename1.payment.paywall.Paywall;
import com.codename1.payment.paywall.PaywallListener;
import com.codename1.payment.paywall.PaywallView;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.HashMap;
import java.util.Map;

/** Compiled source snippets for the Commerce chapter of the developer guide. */
public class CommerceSnippets {

    /** Check an entitlement and show a paywall. */
    public void quickStart() {
        // tag::commerce-quick-start[]
        CommerceManager cm = CommerceManager.getInstance();
        if (cm.isEntitled("pro")) {
            // unlock the paid features here
        } else {
            // getOfferings() blocks on the network, so it runs on a thread of
            // its own and the paywall is shown back on the EDT.
            new Thread(() -> {
                Offering current = cm.getOfferings().getCurrent();
                if (current != null) {
                    CN.callSerially(() -> Paywall.show(current, null));
                }
            }).start();
        }
        // end::commerce-quick-start[]
    }

    /** Read the details of an entitlement. */
    public void customerInfo() {
        // tag::commerce-customer-info[]
        CustomerInfo info = CommerceManager.getInstance().getCustomerInfo();
        EntitlementInfo pro = info.getEntitlement("pro");
        if (pro != null && pro.isActive()) {
            String product = pro.getProductId();    // the product that granted it
            String store = pro.getStore();          // where it was bought
            long expires = pro.getExpiresDate();    // 0 for a lifetime purchase
            boolean renews = pro.isWillRenew();
            String period = pro.getPeriodType();    // trial, introductory or normal
        }
        for (String sku : info.getActiveSubscriptions()) {
            // one line per subscription on an account screen
        }
        // end::commerce-customer-info[]
    }

    /** Follow entitlement changes. */
    public void listener() {
        // tag::commerce-listener[]
        CommerceManager.getInstance().addCustomerInfoListener(evt -> {
            // Called on the EDT. The source is the new customer information.
            CustomerInfo info = (CustomerInfo) evt.getSource();
            boolean pro = info.isEntitled("pro");
            // show or hide the paid features to match
        });
        // end::commerce-listener[]
    }

    /** Refresh on start and tell a missing answer from a denial. */
    public void refresh() {
        // tag::commerce-refresh[]
        CommerceManager cm = CommerceManager.getInstance();
        // refresh() blocks on the network, so it runs off the EDT.
        new Thread(() -> {
            cm.refresh();
            CN.callSerially(() -> {
                if (cm.isDegraded()) {
                    // The service gave no answer. isEntitled() is answering
                    // from the stored answer and the store's own receipts.
                }
                boolean pro = cm.isEntitled("pro");
            });
        }).start();
        // end::commerce-refresh[]
    }

    /** Sign in, sign out and restore. */
    public void identity() {
        // tag::commerce-log-in[]
        CommerceManager cm = CommerceManager.getInstance();
        String accountId = "account-1234";   // your own id for the signed-in user
        // logIn() blocks on the network, so it runs off the EDT.
        new Thread(() -> {
            if (cm.logIn(accountId)) {
                // Purchases made before signing in now belong to the account,
                // and the account's purchases from other platforms unlock here.
                cm.refresh();
            } else {
                // The service did not accept the change. The previous id is
                // still current and the call can be repeated.
            }
        }).start();
        // end::commerce-log-in[]
    }

    /** Sign out. */
    public void logOut() {
        // tag::commerce-log-out[]
        CommerceManager cm = CommerceManager.getInstance();
        cm.logOut();
        // The device may still hold purchases of its store account. Refreshing
        // attaches them to the new anonymous customer.
        new Thread(() -> cm.refresh()).start();
        // end::commerce-log-out[]
    }

    /** Fetch offerings and read packages. */
    public void offerings() {
        // tag::commerce-offerings[]
        CommerceManager cm = CommerceManager.getInstance();
        new Thread(() -> {
            // Pass null, or call getOfferings(), for the default offerings.
            Offerings offerings = cm.getOfferings("onboarding");
            Offering current = offerings.getCurrent();
            if (current == null) {
                return;
            }
            CommercePackage monthly = current.getMonthly();
            CommercePackage annual = current.getAnnual();
            for (CommercePackage pkg : current.getPackages()) {
                String period = pkg.getPeriod();             // "P1M", "P1Y"
                if (pkg.isEligibleForIntro() && pkg.getTrialPeriod() != null) {
                    // this customer can still take the free trial
                }
                if (pkg.getProduct() != null) {
                    // the store's localized price
                    String price = pkg.getProduct().getLocalizedPrice();
                }
            }
        }).start();
        // end::commerce-offerings[]
    }

    /** Buy a package. */
    public void purchasePackage() {
        // tag::commerce-purchase-package[]
        CommerceManager cm = CommerceManager.getInstance();
        cm.addCustomerInfoListener(evt -> {
            if (((CustomerInfo) evt.getSource()).isEntitled("pro")) {
                // the purchase was validated: unlock the paid features
            }
        });
        new Thread(() -> {
            Offering current = cm.getOfferings().getCurrent();
            if (current != null && current.getAnnual() != null) {
                CN.callSerially(() -> cm.purchase(current.getAnnual()));
            }
        }).start();
        // end::commerce-purchase-package[]
    }

    /** Read the experiment a customer is in. */
    public void experiment() {
        // tag::commerce-experiment[]
        new Thread(() -> {
            Offerings offerings = CommerceManager.getInstance().getOfferings();
            String experiment = offerings.getExperimentId();    // null when not in one
            String variant = offerings.getExperimentVariant();
        }).start();
        // end::commerce-experiment[]
    }

    /** Show a paywall and listen to it. */
    public void paywall() {
        // tag::commerce-paywall[]
        new Thread(() -> {
            Offering offering = CommerceManager.getInstance().getOfferings().getCurrent();
            if (offering == null) {
                return;
            }
            CN.callSerially(() -> Paywall.show(offering, new PaywallListener() {
                @Override
                public void purchaseStarted(CommercePackage pkg) {
                    // the store's purchase sheet is up
                }

                @Override
                public void purchaseCompleted(CustomerInfo info) {
                    // the service confirmed the purchase: unlock the features
                }

                @Override
                public void purchaseCancelled() {
                    // closed before the purchase was confirmed
                }

                @Override
                public void restoreCompleted(CustomerInfo info) {
                    // check info.isEntitled("pro") after a restore
                }

                @Override
                public void closed() {
                    // the previous form is showing again
                }
            }));
        }).start();
        // end::commerce-paywall[]
    }

    /** Embed a paywall in a form of the application. */
    public void paywallView() {
        // tag::commerce-paywall-view[]
        new Thread(() -> {
            Offering offering = CommerceManager.getInstance().getOfferings().getCurrent();
            CN.callSerially(() -> {
                Form previous = Display.getInstance().getCurrent();
                // Start from the built-in document and change it, or pass a
                // document of your own as a parsed JSON map.
                Map<String, Object> document = Paywall.createDefault(offering);
                PaywallView view = new PaywallView(offering, document);
                Form upgrade = new Form("Upgrade", new BorderLayout());
                upgrade.add(BorderLayout.CENTER, view);
                view.setCloseHandler(() -> previous.showBack());
                upgrade.show();
            });
        }).start();
        // end::commerce-paywall-view[]
    }

    /** Open the customer center. */
    public void customerCenter() {
        // tag::commerce-customer-center[]
        // Safe on any thread: the configuration is fetched in the background
        // and the form is shown on the EDT.
        CustomerCenter.show();
        // end::commerce-customer-center[]
    }

    /** Buy under a promotional offer. */
    public void promotionalOffer() {
        // tag::commerce-promotional-offer[]
        CommerceManager cm = CommerceManager.getInstance();
        // The ids are the ones App Store Connect shows for the offer.
        if (!cm.purchaseWithPromotionalOffer("pro_monthly", "winback_50")) {
            // No signature was available, so nothing was bought. Sell the
            // product at its regular price instead.
            cm.subscribe("pro_monthly");
        }
        // end::commerce-promotional-offer[]
    }

    /** Set subscriber attributes. */
    public void attributes() {
        // tag::commerce-attributes[]
        CommerceManager cm = CommerceManager.getInstance();
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("plan_interest", "annual");
        attributes.put("$email", "user@example.com");
        attributes.put("campaign", null);   // a null value deletes the attribute
        // setAttributes() blocks on the network, so it runs off the EDT.
        new Thread(() -> cm.setAttributes(attributes)).start();
        // end::commerce-attributes[]
    }

    /** Read a virtual currency balance. */
    public void currency() {
        // tag::commerce-currency[]
        CustomerInfo info = CommerceManager.getInstance().getCustomerInfo();
        long gems = info.getCurrencyBalance("GEMS");   // 0 when the customer holds none
        Map<String, Long> all = info.getCurrencies();
        // end::commerce-currency[]
    }

    /** Sell a package through a web checkout. */
    public void webPurchase() {
        // tag::commerce-web-purchase[]
        CommerceManager cm = CommerceManager.getInstance();
        new Thread(() -> {
            Offering current = cm.getOfferings().getCurrent();
            if (current == null || current.getMonthly() == null) {
                return;
            }
            CommercePackage pkg = current.getMonthly();
            CN.callSerially(() -> {
                if (cm.isWebPurchase(pkg)) {
                    // No store on this platform: purchase(pkg) opens the
                    // hosted checkout itself, as purchaseOnWeb(pkg) does.
                }
                cm.purchase(pkg);
            });
        }).start();
        // end::commerce-web-purchase[]
    }

    /** Ask for a checkout address to open in a page of the application's own. */
    public void webPurchaseUrl() {
        // tag::commerce-web-purchase-url[]
        CommerceManager cm = CommerceManager.getInstance();
        new Thread(() -> {
            Offering current = cm.getOfferings().getCurrent();
            if (current == null || current.getAnnual() == null) {
                return;
            }
            // Blocking. Null return addresses use the service's own pages.
            String url = cm.getWebPurchaseUrl(current.getAnnual(),
                    "https://example.com/thanks", "https://example.com/pricing");
            if (url != null) {
                CN.callSerially(() -> CN.execute(url));
            }
        }).start();
        // end::commerce-web-purchase-url[]
    }

    /** Open the page where the customer manages a subscription. */
    public void managementUrl() {
        // tag::commerce-management-url[]
        CommerceManager cm = CommerceManager.getInstance();
        // fetchManagementUrl() asks for a billing portal when the subscription
        // was bought on the web, so it blocks and runs off the EDT.
        new Thread(() -> {
            String url = cm.fetchManagementUrl();
            if (url != null) {
                CN.callSerially(() -> CN.execute(url));
            }
        }).start();
        // end::commerce-management-url[]
    }

    /** Report the events of a paywall the app draws itself. */
    public void analyticsEvents() {
        // tag::commerce-analytics-event[]
        CommerceManager cm = CommerceManager.getInstance();
        new Thread(() -> {
            Offering offering = cm.getOfferings("onboarding").getCurrent();
            if (offering == null || offering.getAnnual() == null) {
                return;
            }
            CommercePackage annual = offering.getAnnual();
            CN.callSerially(() -> {
                // The names the built-in paywall uses, so the reports read the same.
                cm.analyticsEvent("paywall_view", offering.getId(), null, null, null, null);
                cm.analyticsEvent("paywall_package_selected", offering.getId(),
                        annual.getId(), annual.getProductId(), null, null);
                // purchase(CommercePackage) labels the purchase events by itself.
                cm.purchase(annual);
            });
        }).start();
        // end::commerce-analytics-event[]
    }

    /** List the rewards a customer earned by inviting. */
    public void referralRewards() {
        // tag::commerce-referral-rewards[]
        CustomerInfo info = CommerceManager.getInstance().getCustomerInfo();
        for (ReferralReward reward : info.getReferralRewards()) {
            String earned = reward.isCurrency()
                    ? reward.getAmount() + " " + reward.getCurrencyCode()
                    : reward.getEntitlementId();
            Dialog.show("Thank you", "Your invite earned you " + earned, "OK", null);
        }
        // The reward itself is already in effect: an entitlement answers
        // isEntitled(), and a currency is part of the balance.
        boolean pro = info.isEntitled("pro");
        long coins = info.getCurrencyBalance("coins");
        // end::commerce-referral-rewards[]
    }

    /** Point the simulator at a service. */
    public void simulator() {
        // tag::commerce-simulator[]
        // In init(), before the first CommerceManager call, and only in the
        // simulator: a device build carries its own build key.
        if (CN.isSimulator()) {
            Display.getInstance().setProperty("build_key", "the-build-key-of-your-account");
        }
        // end::commerce-simulator[]
    }
}
