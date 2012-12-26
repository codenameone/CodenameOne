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
package com.codename1.payment;

import com.codename1.ui.Display;


/**
 * Represents the status of in-app-purchase goods, this class provides information
 * about products purchased by a user as well as the ability to purchase additional
 * products. There are generally two types of payment systems: Manual and managed.
 * In manual payments we pay a specific amount in a specific currency  while with managed
 * payment systems we work against a product catalog defined in the server.
 * <p>In-app-purchase API's rely on managed server based products, other payment systems
 * use the manual approach. An application dealing with virtual goods must support both 
 * since not all devices feature in-app-purchase API's. An application dealing with physical
 * goods & services must use the latter according to the TOS of current in-app-purchase
 * solutions.
 *
 * @author Shai Almog
 */
public abstract class Purchase {    
    /**
     * Indicates whether the purchasing platform supports manual payments which 
     * are just payments of a specific amount of money.
     * 
     * @return true if manual payments are supported
     */
    public boolean isManualPaymentSupported() {
        return false;
    }
    
    /**
     * Indicates whether the purchasing platform supports managed payments which 
     * work by picking products that are handled by the servers/OS of the platform vendor.
     * 
     * @return true if managed payments are supported
     */
    public boolean isManagedPaymentSupported() {
        return false;
    }

    /**
     * Performs payment of a specific amount based on the manual payment API, notice that
     * this doesn't use the in-app-purchase functionality of the device!
     * 
     * @param amount the amount to pay
     * @param currency the three letter currency type
     * @return a token representing the pending transaction which will be matched 
     * when receiving a callback from the platform or a null if the payment as 
     * failed or canceled
     * @throws RuntimeException This method is a part of the manual payments API and will fail if
     * isManualPaymentSupported() returns false
     */
    public String pay(double amount, String currency) {
        throw new RuntimeException("Unsupported");
    }
    
    /**
     * Indicates whether the payment platform supports things such as "item listing" or
     * requires that items be coded into the system. iOS provides listing and pricing
     * where Android expects developers to redirect into the Play application for
     * application details.
     * @return true if the OS supports this behavior
     */
    public boolean isItemListingSupported() {
        return false;
    }
    
    /**
     * Returns the product list for the given SKU array
     * 
     * @param sku the ids for the specific products
     * @return the product instances
     * @throws RuntimeException This method is a part of the managed payments API and will fail if
     * isManagedPaymentSupported() returns false
     * @throws RuntimeException This method works only if isItemListingSupported() returns true
     */
    public Product[] getProducts(String[] skus) {
        throw new RuntimeException("Unsupported");
    }
    
    /**
     * Returns true if the given SKU was purchased in the past, notice this method might not 
     * work as expected for Unmanaged/consumable products which can be purchased multiple
     * times.
     * 
     * @param sku the id of the product
     * @return true if the product was purchased
     * @throws RuntimeException This method is a part of the managed payments API and will fail if
     * isManagedPaymentSupported() returns false
     */
    public boolean wasPurchased(String sku) {
        throw new RuntimeException("Unsupported");
    }
    
    /**
     * Begins the purchase process for the given SKU
     * 
     * @param sku the SKU with which to perform the purchase process
     * @throws RuntimeException This method is a part of the managed payments API and will fail if
     * isManagedPaymentSupported() returns false
     */
    public void purchase(String sku) {
        throw new RuntimeException("Unsupported");
    }


    /**
     * Begins subscribe process for the given subscription SKU
     * 
     * @param sku the SKU with which to perform the purchase process
     * @throws RuntimeException This method is a part of the managed payments API and will fail if
     * isManagedPaymentSupported() returns false
     */
    public void subscribe(String sku) {
        throw new RuntimeException("Unsupported");
    }
    
    /**
     * Cancels the subscription to a given SKU
     * 
     * @param sku the SKU with which to perform the purchase process
     * @throws RuntimeException This method is a part of the managed payments API and will fail if
     * isManagedPaymentSupported() returns false
     */
    public void unsubscribe(String sku) {
        throw new RuntimeException("Unsupported");
    }
    
    
    /**
     * Indicates whether refunding is possible when the SKU is purchased
     * @param sku the sku
     * @return true if the SKU can be refunded
     */
    public boolean isRefundable(String sku) {
        return false;
    }
    
    /**
     * Tries to refund the given SKU if applicable in the current market/product
     * 
     * @param sku the id for the product
     */
    public void refund(String sku) {
    }

    /**
     * Returns the native OS purchase implementation if applicable, if not this
     * method will fallback to a cross platform purchase manager. 
     * 
     * @param physicalGoods set to true to indicate that you are interested in purchasing
     * physical goods which are normally not allowed in the OS in-app-purchase solutions.
     * @return instance of the purchase class
     */
    public static Purchase getInAppPurchase(boolean physicalGoods) {
        return Display.getInstance().getInAppPurchase(physicalGoods);
    }
    
    /**
     * Returns true if the subscription API is supported in this platform
     * 
     * @return true if the subscription API is supported in this platform
     */
    public boolean isSubscriptionSupported() {
        return false;
    }

    /**
     * Some platforms support subscribing but don't support unsubscribe
     * 
     * @return true if the subscription API allows for unsubscribe
     */
    public boolean isUnsubscribeSupported() {
        return isSubscriptionSupported();
    }
}
