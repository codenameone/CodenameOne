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

/// A set of packages the application offers a customer together, and
/// optionally the paywall that presents them. Which offering a customer sees
/// is decided by the Codename One Commerce service, so it can be changed, or
/// tested against another, without a new build of the application.
///
/// Instances are immutable.
public final class Offering {
    private final String id;
    private final String description;
    private final Map<String, Object> metadata = new HashMap<String, Object>();
    private final List<CommercePackage> packages = new ArrayList<CommercePackage>();
    private final Map paywall;

    /// Builds an offering from the parsed JSON object of the service's
    /// offerings payload.
    Offering(Map json) {
        Map m = json == null ? new HashMap() : json;
        id = CustomerInfo.asString(m.get("id"));
        description = CustomerInfo.asString(m.get("description"));
        Object meta = m.get("metadata");
        if (meta instanceof Map) {
            for (Object o : ((Map) meta).entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if (e.getKey() != null) {
                    metadata.put(String.valueOf(e.getKey()), copy(e.getValue()));
                }
            }
        }
        Object pkgs = m.get("packages");
        if (pkgs instanceof List) {
            for (Object o : (List) pkgs) {
                if (o instanceof Map) {
                    CommercePackage pkg = new CommercePackage((Map) o);
                    pkg.setOfferingId(CustomerInfo.asString(m.get("id")));
                    packages.add(pkg);
                }
            }
        }
        Object pw = m.get("paywall");
        Object pwCopy = pw instanceof Map ? copy(pw) : null;
        paywall = pwCopy instanceof Map ? (Map) pwCopy : null;
    }

    /// The id of the offering.
    public String getId() {
        return id;
    }

    /// The description given to the offering in the console, or null.
    public String getDescription() {
        return description;
    }

    /// The free form metadata attached to the offering in the console.
    ///
    /// #### Returns
    ///
    /// a new map, which the caller may change
    public Map<String, Object> getMetadata() {
        Map<String, Object> out = new HashMap<String, Object>();
        for (Map.Entry<String, Object> e : metadata.entrySet()) {
            out.put(e.getKey(), copy(e.getValue()));
        }
        return out;
    }

    /// The packages of the offering, in the order the service listed them.
    ///
    /// #### Returns
    ///
    /// a new list, which the caller may change
    public List<CommercePackage> getPackages() {
        return new ArrayList<CommercePackage>(packages);
    }

    /// The paywall document designed for this offering, as parsed JSON.
    ///
    /// #### Returns
    ///
    /// a new map, which the caller may change, or null when the offering
    /// has no paywall
    public Map getPaywall() {
        if (paywall == null) {
            return null;
        }
        Object c = copy(paywall);
        return c instanceof Map ? (Map) c : null;
    }

    /// The package with the given id.
    ///
    /// #### Returns
    ///
    /// the package, or null when the offering has none by that id
    public CommercePackage getPackage(String packageId) {
        if (packageId == null) {
            return null;
        }
        for (CommercePackage p : packages) {
            if (packageId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    /// The first package billed monthly, or null.
    public CommercePackage getMonthly() {
        return firstOfType(CommercePackage.TYPE_MONTHLY);
    }

    /// The first package billed annually, or null.
    public CommercePackage getAnnual() {
        return firstOfType(CommercePackage.TYPE_ANNUAL);
    }

    /// The first lifetime package, or null.
    public CommercePackage getLifetime() {
        return firstOfType(CommercePackage.TYPE_LIFETIME);
    }

    /// The first package billed weekly, or null.
    public CommercePackage getWeekly() {
        return firstOfType(CommercePackage.TYPE_WEEKLY);
    }

    private CommercePackage firstOfType(String type) {
        for (CommercePackage p : packages) {
            if (p.isType(type)) {
                return p;
            }
        }
        return null;
    }

    /// This offering as the JSON object it was built from, for storage.
    Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        CustomerInfo.putIfSet(out, "id", id);
        CustomerInfo.putIfSet(out, "description", description);
        out.put("metadata", getMetadata());
        List<Object> pkgs = new ArrayList<Object>();
        for (CommercePackage p : packages) {
            pkgs.add(p.toMap());
        }
        out.put("packages", pkgs);
        if (paywall != null) {
            out.put("paywall", copy(paywall));
        }
        return out;
    }

    /// A deep copy of a parsed JSON value. Maps and lists are copied, and
    /// everything else in parsed JSON is immutable. `Object.clone()` is not
    /// used: it answers null on a device.
    static Object copy(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> out = new HashMap<Object, Object>();
            for (Object o : ((Map) value).entrySet()) {
                Map.Entry e = (Map.Entry) o;
                out.put(e.getKey(), copy(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<Object>();
            for (Object o : (List) value) {
                out.add(copy(o));
            }
            return out;
        }
        return value;
    }

    @Override
    public String toString() {
        return "Offering{" + id + ", packages=" + packages + "}";
    }
}
