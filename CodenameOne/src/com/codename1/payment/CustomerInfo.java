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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What the Codename One Commerce service knows about one customer: the
/// entitlements held, the subscriptions that are active and the attributes
/// the application attached.
///
/// Instances are immutable. [CommerceManager#getCustomerInfo()] returns the
/// last one the service answered with, and an empty one before any answer.
public final class CustomerInfo {
    private final String appUserId;
    private final String originalAppUserId;
    private final String managementUrl;
    private final long serverTime;
    private final Map<String, EntitlementInfo> entitlements = new HashMap<String, EntitlementInfo>();
    private final List<String> activeSubscriptions = new ArrayList<String>();
    private final Map<String, String> attributes = new HashMap<String, String>();

    /// Builds a customer from the parsed JSON object of the service's
    /// customer payload.
    ///
    /// #### Parameters
    ///
    /// - `json`: the parsed payload; null makes an empty customer
    CustomerInfo(Map json) {
        Map m = json == null ? new HashMap() : json;
        appUserId = asString(m.get("appUserId"));
        originalAppUserId = asString(m.get("originalAppUserId"));
        managementUrl = asString(m.get("managementUrl"));
        serverTime = asLong(m.get("serverTime"));
        Object ents = m.get("entitlements");
        if (ents instanceof Map) {
            for (Object o : ((Map) ents).entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if (e.getKey() == null) {
                    continue;
                }
                String id = String.valueOf(e.getKey());
                Object v = e.getValue();
                entitlements.put(id, new EntitlementInfo(id, v instanceof Map ? (Map) v : null));
            }
        }
        Object subs = m.get("activeSubscriptions");
        if (subs instanceof List) {
            for (Object o : (List) subs) {
                if (o != null) {
                    activeSubscriptions.add(String.valueOf(o));
                }
            }
        }
        Object attrs = m.get("attributes");
        if (attrs instanceof Map) {
            for (Object o : ((Map) attrs).entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if (e.getKey() != null && e.getValue() != null) {
                    attributes.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }
    }

    /// The id the service knows this customer by, or null for the empty
    /// customer.
    public String getAppUserId() {
        return appUserId;
    }

    /// The first id this customer was known by, when a later one replaced it
    /// (an anonymous id that became an account), or null.
    public String getOriginalAppUserId() {
        return originalAppUserId;
    }

    /// The page where the customer manages the subscription with the store,
    /// or null when there is none.
    public String getManagementUrl() {
        return managementUrl;
    }

    /// The service's clock when it answered, in milliseconds since the
    /// epoch, or 0 for the empty customer.
    public long getServerTime() {
        return serverTime;
    }

    /// The entitlement with this id, active or not.
    ///
    /// #### Returns
    ///
    /// the entitlement, or null when the service reported none by that id
    public EntitlementInfo getEntitlement(String entitlementId) {
        if (entitlementId == null) {
            return null;
        }
        return entitlements.get(entitlementId);
    }

    /// Whether the service reported this entitlement as active.
    public boolean isEntitled(String entitlementId) {
        EntitlementInfo e = getEntitlement(entitlementId);
        return e != null && e.isActive();
    }

    /// Every entitlement the service reported, active or not, by id.
    ///
    /// #### Returns
    ///
    /// a new map, which the caller may change
    public Map<String, EntitlementInfo> getEntitlements() {
        return new HashMap<String, EntitlementInfo>(entitlements);
    }

    /// The entitlements the service reported as active.
    ///
    /// #### Returns
    ///
    /// a new list, which the caller may change
    public List<EntitlementInfo> getActiveEntitlements() {
        List<EntitlementInfo> out = new ArrayList<EntitlementInfo>();
        for (EntitlementInfo e : entitlements.values()) {
            if (e.isActive()) {
                out.add(e);
            }
        }
        return out;
    }

    /// The store product ids of the subscriptions that are active.
    ///
    /// #### Returns
    ///
    /// a new list, which the caller may change
    public List<String> getActiveSubscriptions() {
        return new ArrayList<String>(activeSubscriptions);
    }

    /// The attributes attached to this customer.
    ///
    /// #### Returns
    ///
    /// a new map, which the caller may change
    public Map<String, String> getAttributes() {
        return new HashMap<String, String>(attributes);
    }

    /// This customer as the JSON object it was built from, for storage.
    /// Passing the result to the constructor gives an equal customer.
    ///
    /// #### Returns
    ///
    /// a new map, which the caller may change
    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        putIfSet(out, "appUserId", appUserId);
        putIfSet(out, "originalAppUserId", originalAppUserId);
        putIfSet(out, "managementUrl", managementUrl);
        out.put("serverTime", Long.valueOf(serverTime));
        Map<String, Object> ents = new HashMap<String, Object>();
        for (Map.Entry<String, EntitlementInfo> e : entitlements.entrySet()) {
            ents.put(e.getKey(), e.getValue().toMap());
        }
        out.put("entitlements", ents);
        out.put("activeSubscriptions", new ArrayList<String>(activeSubscriptions));
        out.put("attributes", new HashMap<String, Object>(attributes));
        return out;
    }

    /// Whether the other customer says the same thing as this one. The
    /// service's clock is left out: it differs in every answer, and a
    /// listener is told about a change of state, not about a new answer.
    boolean sameState(CustomerInfo other) {
        if (other == null) {
            return false;
        }
        Map<String, Object> a = toMap();
        Map<String, Object> b = other.toMap();
        a.remove("serverTime");
        b.remove("serverTime");
        return a.equals(b);
    }

    @Override
    public String toString() {
        return "CustomerInfo{" + appUserId + ", entitlements=" + entitlements.values() + "}";
    }

    static void putIfSet(Map<String, Object> out, String key, String value) {
        if (value != null) {
            out.put(key, value);
        }
    }

    static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    static boolean asBoolean(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        return o != null && "true".equalsIgnoreCase(o.toString());
    }

    /// A JSON number as a long. The parser hands numbers over as `Double` or
    /// `Long` depending on its settings, and a hand-edited value may be a
    /// string; anything else is 0.
    static long asLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            String s = (String) o;
            if (s.length() == 0) {
                return 0;
            }
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c < '0' || c > '9') && !(i == 0 && c == '-' && s.length() > 1)) {
                    return 0;
                }
            }
            if (s.length() > 18) {
                return 0;
            }
            return Long.parseLong(s);
        }
        return 0;
    }
}
