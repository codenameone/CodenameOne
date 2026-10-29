/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.mixpanel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dart.core.DartList;

/**
 * mixpanel_flutter's {@code People}: each call queues one {@code /engage} update for the
 * current distinct id, sent with the events on the next flush.
 */
public class People {
    private final Mixpanel mixpanel;

    People(Mixpanel mixpanel) {
        this.mixpanel = mixpanel;
    }

    private void op(String operation, Object value) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(operation, value);
        mixpanel.engage(m);
    }

    private static Map<String, Object> one(String prop, Object value) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(prop, value);
        return m;
    }

    public void set(String prop, Object to) {
        op("$set", one(prop, to));
    }

    public void setOnce(String prop, Object to) {
        op("$set_once", one(prop, to));
    }

    public void increment(String prop, double by) {
        op("$add", one(prop, Double.valueOf(by)));
    }

    public void append(String name, Object value) {
        op("$append", one(name, value));
    }

    public void union(String name, List<?> value) {
        op("$union", one(name, value));
    }

    public void remove(String name, Object value) {
        op("$remove", one(name, value));
    }

    public void unset(String name) {
        DartList<String> names = new DartList<String>();
        names.add(name);
        op("$unset", names);
    }

    public void trackCharge(double amount, Map<?, ?> properties) {
        Map<String, Object> charge = new LinkedHashMap<String, Object>();
        charge.put("$amount", Double.valueOf(amount));
        charge.put("$time", Mixpanel.isoNow());
        if (properties != null) {
            for (Map.Entry<?, ?> e : properties.entrySet()) {
                charge.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        op("$append", one("$transactions", charge));
    }

    public void clearCharges() {
        op("$set", one("$transactions", new DartList<Object>()));
    }

    public void deleteUser() {
        op("$delete", "");
    }
}
