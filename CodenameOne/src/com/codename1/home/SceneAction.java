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

/// One thing a [Scene] does: set one [Trait] on one [AccessoryService] to one
/// value.
///
/// The same shape as a [TraitWrite], and deliberately a separate type: a write
/// happens now and can carry a door-lock PIN, while a scene action is stored
/// and replayed by the platform later, when this app may not be running. There
/// is nowhere to put a credential in that story, which is why locks are
/// generally not scriptable into scenes.
public final class SceneAction {

    private final String accessoryId;
    private final String serviceId;
    private final Trait trait;
    private final TraitValue value;

    /// Creates a scene action.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory to act on
    ///
    /// - `serviceId`: the service on that accessory
    ///
    /// - `trait`: the trait to set
    ///
    /// - `value`: the value to set it to
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null` or empty, or
    ///   when `value`'s kind does not match the trait's
    public SceneAction(String accessoryId, String serviceId, Trait trait,
            TraitValue value) {
        if (accessoryId == null || accessoryId.length() == 0) {
            throw new IllegalArgumentException("accessory id is required");
        }
        if (serviceId == null || serviceId.length() == 0) {
            throw new IllegalArgumentException("service id is required");
        }
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is required");
        }
        if (value.getKind() != trait.getValueKind()) {
            throw new IllegalArgumentException(trait.getId() + " takes a "
                    + trait.getValueKind().name() + " value, not a "
                    + value.getKind().name());
        }
        this.accessoryId = accessoryId;
        this.serviceId = serviceId;
        this.trait = trait;
        this.value = value;
    }

    /// The accessory this action acts on.
    ///
    /// #### Returns
    ///
    /// the accessory identifier, never `null`
    public String getAccessoryId() {
        return accessoryId;
    }

    /// The service on that accessory.
    ///
    /// #### Returns
    ///
    /// the service identifier, never `null`
    public String getServiceId() {
        return serviceId;
    }

    /// The trait this action sets.
    ///
    /// #### Returns
    ///
    /// the trait, never `null`
    public Trait getTrait() {
        return trait;
    }

    /// The value it sets the trait to.
    ///
    /// #### Returns
    ///
    /// the value, never `null`
    public TraitValue getValue() {
        return value;
    }

    @Override
    public String toString() {
        return accessoryId + "/" + serviceId + " " + trait.getId() + "="
                + value;
    }
}
