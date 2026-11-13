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

/// The offerings the Codename One Commerce service configured for this
/// customer: the one to present now, and every other one by id.
///
/// Instances are immutable. [CommerceManager#getOfferings()] returns the last
/// one the service answered with, and an empty one before any answer.
public final class Offerings {
    private final String currentOfferingId;
    private final String experimentId;
    private final String experimentVariant;
    private final List<Offering> offerings = new ArrayList<Offering>();

    /// Builds the offerings from the parsed JSON object of the service's
    /// offerings payload.
    ///
    /// #### Parameters
    ///
    /// - `json`: the parsed payload; null makes empty offerings
    Offerings(Map json) {
        Map m = json == null ? new HashMap() : json;
        currentOfferingId = CustomerInfo.asString(m.get("currentOfferingId"));
        Object exp = m.get("experiment");
        if (exp instanceof Map) {
            experimentId = CustomerInfo.asString(((Map) exp).get("id"));
            experimentVariant = CustomerInfo.asString(((Map) exp).get("variant"));
        } else {
            experimentId = null;
            experimentVariant = null;
        }
        Object all = m.get("offerings");
        if (all instanceof List) {
            for (Object o : (List) all) {
                if (o instanceof Map) {
                    offerings.add(new Offering((Map) o));
                }
            }
        }
    }

    /// The offering the service chose for this customer.
    ///
    /// #### Returns
    ///
    /// the offering, or null when the service named none or there was no
    /// answer yet
    public Offering getCurrent() {
        return getOffering(currentOfferingId);
    }

    /// Every offering of the application.
    ///
    /// #### Returns
    ///
    /// a new list, which the caller may change
    public List<Offering> getAll() {
        return new ArrayList<Offering>(offerings);
    }

    /// The offering with the given id.
    ///
    /// #### Returns
    ///
    /// the offering, or null when there is none by that id
    public Offering getOffering(String offeringId) {
        if (offeringId == null) {
            return null;
        }
        for (Offering o : offerings) {
            if (offeringId.equals(o.getId())) {
                return o;
            }
        }
        return null;
    }

    /// The id of the experiment this customer takes part in, or null when
    /// the current offering was not chosen by an experiment.
    public String getExperimentId() {
        return experimentId;
    }

    /// The variant of the experiment this customer was assigned, or null.
    public String getExperimentVariant() {
        return experimentVariant;
    }

    /// Whether there is no offering at all.
    public boolean isEmpty() {
        return offerings.isEmpty();
    }

    /// These offerings as the JSON object they were built from, for storage.
    Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<String, Object>();
        CustomerInfo.putIfSet(out, "currentOfferingId", currentOfferingId);
        if (experimentId != null) {
            Map<String, Object> exp = new HashMap<String, Object>();
            exp.put("id", experimentId);
            CustomerInfo.putIfSet(exp, "variant", experimentVariant);
            out.put("experiment", exp);
        }
        List<Object> all = new ArrayList<Object>();
        for (Offering o : offerings) {
            all.add(o.toMap());
        }
        out.put("offerings", all);
        return out;
    }

    @Override
    public String toString() {
        return "Offerings{current=" + currentOfferingId + ", " + offerings + "}";
    }
}
