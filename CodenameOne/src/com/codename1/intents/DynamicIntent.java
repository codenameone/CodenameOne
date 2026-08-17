/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.intents;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// A named parameterization of an intent your application already declares --
/// "reorder my usual", "call Mum" -- built from data only known at runtime.
///
/// ```java
/// Map<String, Object> usual = new HashMap<String, Object>();
/// usual.put("shop", "shop-7");
/// usual.put("size", "large");
/// Intents.registerDynamicIntent(new DynamicIntent("usual_coffee", "order_coffee",
///         "Order my usual").bind(usual));
/// ```
///
/// #### Why it must name a base intent
///
/// It runs by running the intent it names, with the bound values filled in. It
/// cannot introduce a new capability: the native catalogue is compiled into the
/// app, so a genuinely new verb could never reach the platform, and a runtime
/// API that appeared to add one would work in the simulator and silently do
/// nothing on a device.
///
/// Binding to a base intent is also what makes it invokable at all -- there is a
/// handler to run, which is the whole point of registering it.
public final class DynamicIntent {

    private final String id;
    private final String baseIntentId;
    private final String title;
    private final Map<String, Object> bound = new HashMap<String, Object>();

    /// Creates a parameterization.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id this parameterization is known by; must not collide with a
    ///   declared intent
    /// - `baseIntentId`: the declared intent that actually runs
    /// - `title`: the name shown to the user
    public DynamicIntent(String id, String baseIntentId, String title) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is required");
        }
        if (baseIntentId == null || baseIntentId.length() == 0) {
            throw new IllegalArgumentException("baseIntentId is required: a parameterization "
                    + "runs by running an intent that was declared at build time");
        }
        this.id = id;
        this.baseIntentId = baseIntentId;
        this.title = title == null ? id : title;
    }

    /// Binds values for the base intent's parameters. Anything supplied at
    /// invocation time wins, so a bound value is a default rather than a lock.
    ///
    /// #### Parameters
    ///
    /// - `params`: the values to bind
    ///
    /// #### Returns
    ///
    /// this parameterization, for chaining
    public DynamicIntent bind(Map<String, Object> params) {
        if (params != null) {
            bound.putAll(params);
        }
        return this;
    }

    /// Binds one value.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter name
    /// - `value`: the value to bind
    ///
    /// #### Returns
    ///
    /// this parameterization, for chaining
    public DynamicIntent bind(String name, Object value) {
        if (name != null) {
            bound.put(name, value);
        }
        return this;
    }

    public String getId() {
        return id;
    }

    /// The declared intent this runs.
    public String getBaseIntentId() {
        return baseIntentId;
    }

    public String getTitle() {
        return title;
    }

    /// The bound values, never null.
    public Map<String, Object> getBoundParameters() {
        return Collections.unmodifiableMap(bound);
    }

    @Override
    public String toString() {
        return id + " -> " + baseIntentId;
    }
}
