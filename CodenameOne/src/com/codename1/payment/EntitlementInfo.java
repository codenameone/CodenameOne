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

/// One entitlement of a customer, as the Codename One Commerce service
/// reported it. An entitlement is what the application checks ("pro"), and is
/// independent of the store and of the product that granted it.
///
/// Instances are immutable. They are created by [CustomerInfo] from the
/// service's answer and never by the application.
public final class EntitlementInfo {
    private final String id;
    private final boolean active;
    private final long expiresDate;
    private final String productId;
    private final String store;
    private final String periodType;
    private final String ownershipType;
    private final String billingStage;
    private final boolean willRenew;
    private final String provenance;

    /// Builds an entitlement from its parsed JSON object.
    ///
    /// #### Parameters
    ///
    /// - `id`: the entitlement id, used when the object does not carry one
    ///
    /// - `json`: the parsed object, may be null
    EntitlementInfo(String id, Map json) {
        Map m = json == null ? new HashMap() : json;
        String own = CustomerInfo.asString(m.get("entitlementId"));
        this.id = own == null ? id : own;
        this.active = CustomerInfo.asBoolean(m.get("active"));
        this.expiresDate = CustomerInfo.asLong(m.get("expiresDate"));
        this.productId = CustomerInfo.asString(m.get("productId"));
        this.store = CustomerInfo.asString(m.get("store"));
        this.periodType = CustomerInfo.asString(m.get("periodType"));
        this.ownershipType = CustomerInfo.asString(m.get("ownershipType"));
        this.billingStage = CustomerInfo.asString(m.get("billingStage"));
        this.willRenew = CustomerInfo.asBoolean(m.get("willRenew"));
        this.provenance = CustomerInfo.asString(m.get("provenance"));
    }

    /// The entitlement id, the name the application checks.
    public String getId() {
        return id;
    }

    /// Whether the service considers the entitlement held. This is the
    /// service's decision, made with its own clock; it is not recomputed on
    /// the device from [#getExpiresDate()].
    public boolean isActive() {
        return active;
    }

    /// When the entitlement ends, in milliseconds since the epoch, or 0 when
    /// it has no end (a lifetime purchase) or the service did not say.
    public long getExpiresDate() {
        return expiresDate;
    }

    /// The store product that granted the entitlement, or null.
    public String getProductId() {
        return productId;
    }

    /// The store the grant came from, as the service names it, or null.
    public String getStore() {
        return store;
    }

    /// The kind of period the customer is in (a trial, an introductory
    /// period, the normal one), as the service names it, or null.
    public String getPeriodType() {
        return periodType;
    }

    /// Whether the customer bought the product or received it through family
    /// sharing, as the service names it, or null.
    public String getOwnershipType() {
        return ownershipType;
    }

    /// Where the subscription is in its billing life (in a grace period, in
    /// billing retry and so on), as the service names it, or null.
    public String getBillingStage() {
        return billingStage;
    }

    /// Whether the subscription behind the entitlement is set to renew.
    public boolean isWillRenew() {
        return willRenew;
    }

    /// How the grant came to be (a purchase, a promotional grant, a
    /// transfer), as the service names it, or null.
    public String getProvenance() {
        return provenance;
    }

    /// This entitlement as the JSON object it was built from, for storage.
    ///
    /// #### Returns
    ///
    /// a new map, which the caller may change
    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        CustomerInfo.putIfSet(out, "entitlementId", id);
        out.put("active", active ? Boolean.TRUE : Boolean.FALSE);
        out.put("expiresDate", Long.valueOf(expiresDate));
        CustomerInfo.putIfSet(out, "productId", productId);
        CustomerInfo.putIfSet(out, "store", store);
        CustomerInfo.putIfSet(out, "periodType", periodType);
        CustomerInfo.putIfSet(out, "ownershipType", ownershipType);
        CustomerInfo.putIfSet(out, "billingStage", billingStage);
        out.put("willRenew", willRenew ? Boolean.TRUE : Boolean.FALSE);
        CustomerInfo.putIfSet(out, "provenance", provenance);
        return out;
    }

    @Override
    public String toString() {
        return "EntitlementInfo{" + id + ", active=" + active + ", expiresDate=" + expiresDate + "}";
    }
}
