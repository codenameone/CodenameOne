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
package com.codename1.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One functional endpoint of an [Accessory]: HomeKit's `HMService`, one
/// Matter endpoint.
///
/// The distinction matters more than it looks. A two-gang wall switch is
/// **one** accessory with **two** services, and writing [Trait#ON_OFF] to the
/// accessory without saying which service is ambiguous. Everything in this API
/// that reads or writes a trait names a service.
///
/// An immutable snapshot. Nothing here calls into the platform.
public final class AccessoryService {

    private final String id;
    private final String name;
    private final ServiceType type;
    private final boolean primary;
    private final List<TraitConstraint> constraints;

    /// Creates a service snapshot. Called by the ports and by the local home;
    /// application code receives these rather than building them.
    ///
    /// #### Parameters
    ///
    /// - `id`: the service identifier, unique within its accessory
    ///
    /// - `name`: the user-visible name, or `null` for none
    ///
    /// - `type`: what the service is; `null` becomes [ServiceType#OTHER]
    ///
    /// - `primary`: whether this is the accessory's main service
    ///
    /// - `constraints`: what each trait on this service will accept; `null`
    ///   becomes empty
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `id` is `null` or empty
    public AccessoryService(String id, String name, ServiceType type,
            boolean primary, List<TraitConstraint> constraints) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("service id is required");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        this.type = type == null ? ServiceType.OTHER : type;
        this.primary = primary;
        if (constraints == null || constraints.isEmpty()) {
            this.constraints = Collections.<TraitConstraint>emptyList();
        } else {
            this.constraints = Collections.unmodifiableList(
                    new ArrayList<TraitConstraint>(constraints));
        }
    }

    /// The identifier this service is addressed by, unique within its
    /// accessory.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// The user-visible name, empty when the accessory gave none.
    ///
    /// #### Returns
    ///
    /// the name, never `null`
    public String getName() {
        return name;
    }

    /// What this service is.
    ///
    /// #### Returns
    ///
    /// the type, never `null`
    public ServiceType getType() {
        return type;
    }

    /// Whether this is the accessory's main service -- the one a UI showing a
    /// single control for the whole device should use.
    ///
    /// #### Returns
    ///
    /// `true` for the primary service
    public boolean isPrimary() {
        return primary;
    }

    /// Every trait this service exposes, with what each will accept.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<TraitConstraint> getConstraints() {
        return constraints;
    }

    /// What this service will accept for one trait.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait to look up, or `null`
    ///
    /// #### Returns
    ///
    /// the constraint, or `null` when this service does not expose the trait
    public TraitConstraint getConstraint(Trait trait) {
        if (trait == null) {
            return null;
        }
        for (TraitConstraint c : constraints) {
            // Reference equality on purpose: Trait instances are interned
            // constants, so == is the identity test the class documents and
            // is what every lookup here relies on. Trait does not override
            // equals, so .equals() would be the same comparison spelled
            // longer and would suggest a value comparison that does not
            // exist.
            if (c.getTrait() == trait) { //NOPMD CompareObjectsWithEquals
                return c;
            }
        }
        return null;
    }

    /// Whether this service exposes a trait at all.
    ///
    /// This is the question to ask instead of switching on [#getType()] or on
    /// [SmartHome#getBackend()]. A service exposes what it exposes, and two
    /// accessories of the same type routinely differ.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait to test, or `null`
    ///
    /// #### Returns
    ///
    /// `true` when the trait is present here
    public boolean supports(Trait trait) {
        return getConstraint(trait) != null;
    }

    /// Every trait this service exposes.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<Trait> getTraits() {
        if (constraints.isEmpty()) {
            return Collections.<Trait>emptyList();
        }
        List<Trait> out = new ArrayList<Trait>(constraints.size());
        for (TraitConstraint c : constraints) {
            out.add(c.getTrait());
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public String toString() {
        return type.name() + "[" + id + (name.length() > 0 ? " " + name : "")
                + "]";
    }
}
