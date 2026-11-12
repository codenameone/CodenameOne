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

/// One thing a customer can buy from an [Offering]: a store product together
/// with the role it plays in the offering (the monthly plan, the annual plan,
/// a lifetime unlock).
///
/// Everything the Codename One Commerce service said about the package is
/// fixed when the instance is made. The store's own [Product], which carries
/// the localized price, is attached later, once the store has been asked.
public final class CommercePackage {
    /// A one time purchase that never expires.
    public static final String TYPE_LIFETIME = "LIFETIME";
    /// A subscription billed once a year.
    public static final String TYPE_ANNUAL = "ANNUAL";
    /// A subscription billed every six months.
    public static final String TYPE_SIX_MONTH = "SIX_MONTH";
    /// A subscription billed every three months.
    public static final String TYPE_THREE_MONTH = "THREE_MONTH";
    /// A subscription billed every two months.
    public static final String TYPE_TWO_MONTH = "TWO_MONTH";
    /// A subscription billed once a month.
    public static final String TYPE_MONTHLY = "MONTHLY";
    /// A subscription billed once a week.
    public static final String TYPE_WEEKLY = "WEEKLY";
    /// A package that is none of the standard ones.
    public static final String TYPE_CUSTOM = "CUSTOM";

    private final String id;
    private final String type;
    private final String productId;
    private final String period;
    private final String trialPeriod;
    private final String introPrice;
    private final String introPeriod;
    private final boolean eligibleForIntro;
    private Product product;
    private String offeringId;

    /// Builds a package from the parsed JSON object of the service's
    /// offerings payload.
    CommercePackage(Map json) {
        Map m = json == null ? new HashMap() : json;
        id = CustomerInfo.asString(m.get("id"));
        String t = CustomerInfo.asString(m.get("type"));
        type = t == null || t.length() == 0 ? TYPE_CUSTOM : t;
        productId = CustomerInfo.asString(m.get("productId"));
        period = emptyAsNull(CustomerInfo.asString(m.get("period")));
        trialPeriod = emptyAsNull(CustomerInfo.asString(m.get("trialPeriod")));
        introPrice = emptyAsNull(CustomerInfo.asString(m.get("introPrice")));
        introPeriod = emptyAsNull(CustomerInfo.asString(m.get("introPeriod")));
        eligibleForIntro = CustomerInfo.asBoolean(m.get("eligibleForIntro"));
    }

    private static String emptyAsNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    /// The id of the offering this package was listed in, or null for a
    /// package that came from no offering. A web checkout names the package
    /// by both ids.
    public String getOfferingId() {
        return offeringId;
    }

    /// Set by the offering that lists this package.
    void setOfferingId(String offeringId) {
        this.offeringId = offeringId;
    }

    /// The id of the package inside its offering.
    public String getId() {
        return id;
    }

    /// The role of the package, one of the `TYPE_` constants. A package the
    /// service gave no type is [#TYPE_CUSTOM].
    public String getType() {
        return type;
    }

    /// Whether the package is of the given type. Types are compared without
    /// regard to case.
    ///
    /// #### Parameters
    ///
    /// - `packageType`: one of the `TYPE_` constants
    public boolean isType(String packageType) {
        return type.equalsIgnoreCase(packageType);
    }

    /// The store product id (SKU) this package sells.
    public String getProductId() {
        return productId;
    }

    /// The billing period as an ISO-8601 duration such as `P1M`, or null for
    /// a package that is not billed periodically.
    public String getPeriod() {
        return period;
    }

    /// The free trial as an ISO-8601 duration such as `P7D`, or null when
    /// the package has none.
    public String getTrialPeriod() {
        return trialPeriod;
    }

    /// The introductory price as text, or null when the package has none.
    public String getIntroPrice() {
        return introPrice;
    }

    /// How long the introductory price lasts, as an ISO-8601 duration, or
    /// null when the package has none.
    public String getIntroPeriod() {
        return introPeriod;
    }

    /// Whether this customer may still take the free trial or introductory
    /// price of the package, as far as the service knows.
    public boolean isEligibleForIntro() {
        return eligibleForIntro;
    }

    /// Whether buying the package starts a subscription. Everything is a
    /// subscription except a [#TYPE_LIFETIME] package and a [#TYPE_CUSTOM]
    /// one with no billing period.
    public boolean isSubscription() {
        if (isType(TYPE_LIFETIME)) {
            return false;
        }
        return !(isType(TYPE_CUSTOM) && period == null);
    }

    /// The store's product for this package, which carries the localized
    /// price and name.
    ///
    /// #### Returns
    ///
    /// the product, or null while the store has not been asked or does not
    /// list products on this platform
    public Product getProduct() {
        return product;
    }

    /// Attaches the store's product once the store answered.
    void setProduct(Product product) {
        this.product = product;
    }

    /// This package as the JSON object it was built from, for storage.
    Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        CustomerInfo.putIfSet(out, "id", id);
        CustomerInfo.putIfSet(out, "type", type);
        CustomerInfo.putIfSet(out, "productId", productId);
        CustomerInfo.putIfSet(out, "period", period);
        CustomerInfo.putIfSet(out, "trialPeriod", trialPeriod);
        CustomerInfo.putIfSet(out, "introPrice", introPrice);
        CustomerInfo.putIfSet(out, "introPeriod", introPeriod);
        out.put("eligibleForIntro", eligibleForIntro ? Boolean.TRUE : Boolean.FALSE);
        return out;
    }

    @Override
    public String toString() {
        return "CommercePackage{" + id + ", " + type + ", " + productId + "}";
    }
}
