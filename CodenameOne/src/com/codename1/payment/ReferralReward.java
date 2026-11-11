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

import java.util.HashMap;
import java.util.Map;

/// A reward the service granted this customer for a referral: either an
/// entitlement or an amount of a virtual currency. Rewards are configured in
/// the console and listed by [CustomerInfo#getReferralRewards()]. Immutable.
public final class ReferralReward {
    private final String entitlementId;
    private final String currencyCode;
    private final long amount;
    private final long expiresDate;
    private final String reason;

    /// Builds a reward from one entry of the customer payload's `rewards`.
    ///
    /// #### Parameters
    ///
    /// - `json`: the parsed entry; null makes an empty reward
    ReferralReward(Map json) {
        Map m = json == null ? new HashMap() : json;
        entitlementId = CustomerInfo.asString(m.get("entitlementId"));
        currencyCode = CustomerInfo.asString(m.get("currencyCode"));
        amount = CustomerInfo.asLong(m.get("amount"));
        expiresDate = CustomerInfo.asLong(m.get("expiresDate"));
        reason = CustomerInfo.asString(m.get("reason"));
    }

    /// The entitlement this reward grants, or null when it is a currency
    /// reward.
    ///
    /// #### Returns
    ///
    /// the entitlement id or null
    public String getEntitlementId() {
        return entitlementId;
    }

    /// The code of the virtual currency this reward adds to, or null when it
    /// is an entitlement reward.
    ///
    /// #### Returns
    ///
    /// the currency code or null
    public String getCurrencyCode() {
        return currencyCode;
    }

    /// Whether this reward is an amount of a virtual currency rather than an
    /// entitlement.
    ///
    /// #### Returns
    ///
    /// true for a currency reward
    public boolean isCurrency() {
        return currencyCode != null;
    }

    /// The amount of currency granted; 0 for an entitlement reward.
    ///
    /// #### Returns
    ///
    /// the amount
    public long getAmount() {
        return amount;
    }

    /// When the reward expires, in milliseconds since the epoch, or 0 when it
    /// does not expire.
    ///
    /// #### Returns
    ///
    /// the expiry time or 0
    public long getExpiresDate() {
        return expiresDate;
    }

    /// Why the reward was granted, as the service names it -- for example
    /// that a user this customer invited made a purchase -- or null.
    ///
    /// #### Returns
    ///
    /// the reason or null
    public String getReason() {
        return reason;
    }

    /// This reward as the JSON object it was built from, for storage.
    Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        CustomerInfo.putIfSet(out, "entitlementId", entitlementId);
        CustomerInfo.putIfSet(out, "currencyCode", currencyCode);
        out.put("amount", Long.valueOf(amount));
        out.put("expiresDate", Long.valueOf(expiresDate));
        CustomerInfo.putIfSet(out, "reason", reason);
        return out;
    }

    @Override
    public String toString() {
        return "ReferralReward{" + (currencyCode == null ? entitlementId : currencyCode + " x" + amount)
                + ", reason=" + reason + "}";
    }
}
