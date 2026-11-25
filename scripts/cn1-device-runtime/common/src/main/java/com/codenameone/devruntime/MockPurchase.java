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
package com.codenameone.devruntime;

import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.PurchaseCallback;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Display;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A purchase API that completes every transaction and charges nobody.
 *
 * <p>The point is the edge cases. A store sandbox is slow to set up, needs
 * products configured in a console and an account that is allowed to buy them,
 * and none of that helps when the question is what your code does when a
 * purchase succeeds twice, or when a restore returns an item you no longer sell.
 * Those paths are the ones that ship broken.</p>
 *
 * <p><b>Nothing here represents a real store.</b> Prices are invented, every
 * purchase succeeds, and no receipt is valid anywhere. The runtime says so on
 * screen the first time a pushed program touches this, because a mock that
 * looks like the real thing is worse than no mock at all.</p>
 *
 * @author Shai Almog
 */
public class MockPurchase extends Purchase {
    /** SKUs currently owned, in memory and for this session only. */
    private final Vector owned = new Vector();

    /** SKU -> subscription flag, so unsubscribe can be exercised. */
    private final Hashtable subscriptions = new Hashtable();

    public boolean isManagedPaymentSupported() {
        return true;
    }

    public boolean isManualPaymentSupported() {
        return false;
    }

    public boolean isItemListingSupported() {
        return true;
    }

    public boolean isSubscriptionSupported() {
        return true;
    }

    public boolean isUnsubscribeSupported() {
        return true;
    }

    public boolean isRestoreSupported() {
        return true;
    }

    public boolean isRefundable(String sku) {
        return true;
    }

    /**
     * Invents a product per SKU.
     *
     * <p>A real store answers only for SKUs configured in its console; this
     * answers for anything, which is the useful behaviour when the point is to
     * exercise the code around the call.</p>
     */
    public Product[] getProducts(String[] skus) {
        if (skus == null) {
            return new Product[0];
        }
        Product[] out = new Product[skus.length];
        for (int i = 0; i < skus.length; i++) {
            Product p = new Product();
            p.setSku(skus[i]);
            p.setDisplayName(skus[i]);
            p.setDescription("Mock product. Not a real store listing.");
            p.setLocalizedPrice("$0.00 (mock)");
            out[i] = p;
        }
        return out;
    }

    public boolean wasPurchased(String sku) {
        return owned.contains(sku);
    }

    protected void purchaseImpl(String sku) {
        complete(sku, false);
    }

    public void subscribe(String sku) {
        complete(sku, true);
    }

    /**
     * Refunds what was bought, because {@link #isRefundable(String)} says it
     * can be.
     *
     * <p>{@code Purchase.refund} is a no-op in the base class, so inheriting it
     * while advertising the capability would leave refund-handling code
     * unexercised -- the SKU still owned, no callback delivered -- which is
     * precisely the path this mock exists to let somebody test.</p>
     */
    public void refund(String sku) {
        DeviceRuntimeMocks.warnOnce("in-app purchase");
        owned.removeElement(sku);
        subscriptions.remove(sku);
        deliverRefund(sku);
    }

    public void unsubscribe(String sku) {
        owned.removeElement(sku);
        subscriptions.remove(sku);
        deliverRefund(sku);
    }

    /// On the event thread, for the same reason a purchase is.
    private void deliverRefund(final String sku) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                PurchaseCallback c = callback();
                if (c != null) {
                    c.itemRefunded(sku);
                }
            }
        });
    }

    /**
     * Restores what this session bought.
     *
     * <p>Session-scoped on purpose: a restore that resurrected purchases from a
     * previous run of a different pushed program would be a confusing lie.</p>
     */
    public void restore() {
        PurchaseCallback c = callback();
        if (c == null) {
            return;
        }
        for (int i = 0; i < owned.size(); i++) {
            c.itemPurchased((String)owned.elementAt(i));
        }
    }

    private void complete(final String sku, boolean subscription) {
        DeviceRuntimeMocks.warnOnce("in-app purchase");
        if (!owned.contains(sku)) {
            owned.addElement(sku);
        }
        if (subscription) {
            subscriptions.put(sku, Boolean.TRUE);
        }
        // On the event thread, like a real store callback: pushed code updates
        // its UI from here, and delivering on the caller's thread would work in
        // the simulator and fail on a device.
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                PurchaseCallback c = callback();
                if (c != null) {
                    c.itemPurchased(sku);
                }
            }
        });
    }

    private PurchaseCallback callback() {
        return CodenameOneImplementation.getPurchaseCallback();
    }
}
