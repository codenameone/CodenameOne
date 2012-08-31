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

/**
 * Callback interface that the main class must implement in order for in-app-purchasing
 * to work. Once the main class implements this interface the methods within it
 * are invoked to indicate the various purchase states.
 *
 * @author Shai Almog
 */
public interface PurchaseCallback {
    /**
     * Indicates a the given SKU was purchased by a user. When purchasing multiple 
     * SKU's at once multiple calls to this method will be performed.
     * @param sku the sku purchased
     */
    public void itemPurchased(String sku);
    
    /**
     * Callback indicating a the given SKU purchase failed
     * @param sku the id
     */
    public void itemPurchaseError(String sku, String errorMessage);

    /**
     * Invoked if a refund was granted for a purchase
     * 
     * @param sku the sku purchased
     */
    public void itemRefunded(String sku);

    /**
     * Invoked when a subscription SKU is started
     * 
     * @param sku the sku purchased
     */
    public void subscriptionStarted(String sku);

    /**
     * Invoked when a subscription SKU is canceled
     * 
     * @param sku the sku purchased
     */
    public void subscriptionCanceled(String sku);
    
    /**
     * Indicates that a manual payment has failed
     * 
     * @param paymentCode the transaction id of the payment
     */
    public void paymentFailed(String paymentCode, String failureReason);
    
    /**
     * Indicates that a manual payment has passed
     * 
     * @param paymentCode the transaction id of the payment
     */
    public void paymentSucceeded(String paymentCode, double amount, String currency);
}
