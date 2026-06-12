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

/// A card entry published to the Apple Wallet issuer-provisioning extension
/// through [WalletExtension]. Each entry describes one card the user can add
/// to Apple Wallet from inside the Wallet app.
///
/// The `identifier` must match the card's primary account identifier known to
/// the issuer backend; Wallet uses it to filter out cards that are already
/// provisioned on the device, and it is echoed back to the issuer endpoint
/// when the user adds the card.
///
/// The card art must be a PNG without personally identifiable information
/// (Apple requirement: square corners, no PII such as the full card number).
public class WalletPassEntry {
    private String identifier;
    private String title;
    private String cardholderName;
    private String primaryAccountSuffix;
    private String paymentNetwork;
    private String localizedDescription;
    private byte[] artPng;

    /// Creates a blank entry; populate it with the fluent setters.
    public WalletPassEntry() {
    }

    /// Creates an entry with the required fields.
    ///
    /// #### Parameters
    ///
    /// - `identifier`: the card's primary account identifier
    ///
    /// - `title`: user visible card title shown in Wallet
    ///
    /// - `artPng`: PNG bytes of the card art, e.g. `EncodedImage.getImageData()`
    public WalletPassEntry(String identifier, String title, byte[] artPng) {
        this.identifier = identifier;
        this.title = title;
        this.artPng = artPng;
    }

    /// Sets the card's primary account identifier. Required.
    public WalletPassEntry identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /// Sets the user visible card title shown in Wallet. Required.
    public WalletPassEntry title(String title) {
        this.title = title;
        return this;
    }

    /// Sets the cardholder name shown during provisioning.
    public WalletPassEntry cardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
        return this;
    }

    /// Sets the last digits of the card number shown during provisioning,
    /// e.g. `"1234"`.
    public WalletPassEntry primaryAccountSuffix(String primaryAccountSuffix) {
        this.primaryAccountSuffix = primaryAccountSuffix;
        return this;
    }

    /// Sets the payment network, e.g. `"Visa"` or `"MasterCard"`. Must match
    /// one of Apple's `PKPaymentNetwork` constant values.
    public WalletPassEntry paymentNetwork(String paymentNetwork) {
        this.paymentNetwork = paymentNetwork;
        return this;
    }

    /// Sets the description shown during provisioning, e.g. `"My Bank Debit Card"`.
    public WalletPassEntry localizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
        return this;
    }

    /// Sets the PNG bytes of the card art, e.g. `EncodedImage.getImageData()`.
    /// Required; entries without art are not shown by Wallet.
    public WalletPassEntry artPng(byte[] artPng) {
        this.artPng = artPng;
        return this;
    }

    /// Returns the card's primary account identifier.
    public String getIdentifier() {
        return identifier;
    }

    /// Returns the user visible card title.
    public String getTitle() {
        return title;
    }

    /// Returns the cardholder name.
    public String getCardholderName() {
        return cardholderName;
    }

    /// Returns the last digits of the card number.
    public String getPrimaryAccountSuffix() {
        return primaryAccountSuffix;
    }

    /// Returns the payment network.
    public String getPaymentNetwork() {
        return paymentNetwork;
    }

    /// Returns the description shown during provisioning.
    public String getLocalizedDescription() {
        return localizedDescription;
    }

    /// Returns the PNG bytes of the card art.
    public byte[] getArtPng() {
        return artPng;
    }
}
