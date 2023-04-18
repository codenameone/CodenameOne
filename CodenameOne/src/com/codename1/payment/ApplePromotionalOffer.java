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
 * Encapsulates a promotional offer for use with in-app-purchase in Apple's App store.  This mirrors the information required to construct a <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount">SKPaymentDiscount</a> object.
 *
 * <p>See <a href="https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/subscriptions_and_offers/implementing_promotional_offers_in_your_app?language=objc">Apple's documentation for implementing promotional offers.</a></p>
 */
public class ApplePromotionalOffer implements PromotionalOffer {
    private String offerIdentifier;
    private String keyIdentifier;
    private String nonce;
    private String signature;
    private long timestamp;

    /**
     * A string used to uniquely identify a discount offer for a product.
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043528-identifier?language=objc">Apple docs</a>.
     *
     * @return The offer identifier.
     */
    public String getOfferIdentifier() {
        return offerIdentifier;
    }

    /**
     * A string used to uniquely identify a discount offer for a product.
     * @param offerIdentifier The offer identifier.
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043528-identifier?language=objc">Apple docs</a>.
     */
    public void setOfferIdentifier(String offerIdentifier) {
        this.offerIdentifier = offerIdentifier;
    }

    /**
     * A string that identifies the key used to generate the signature.
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043530-keyidentifier?language=objc">Apple's docs</a>.
     *
     * @return The key identifier.
     */
    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    /**
     * A string that identifies the key used to generate the signature.
     * @param keyIdentifier The key identifier.
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043530-keyidentifier?language=objc">Apple's docs</a>.
     */
    public void setKeyIdentifier(String keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    /**
     * A universally unique ID (UUID) value that you define. (As a string).
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043531-nonce?language=objc">Apple's docs</a>.
     * @return The nonce
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * A universally unique ID (UUID) value that you define. (As a string).
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043531-nonce?language=objc">Apple's docs</a>.
     *
     * @param nonce The nonce
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * A string representing the properties of a specific promotional offer, cryptographically signed.
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043532-signature?language=objc">Apple's docs</a>
     * @return The signature.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * A string representing the properties of a specific promotional offer, cryptographically signed.
     *
     * See <a href="https://developer.apple.com/documentation/storekit/skpaymentdiscount/3043532-signature?language=objc">Apple's docs</a>
     *
     * @param signature The signature.
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * The date and time of the signature's creation in milliseconds, formatted in Unix epoch time.
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The date and time of the signature's creation in milliseconds, formatted in Unix epoch time.
     *
     * @param timestamp The timestamp.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
