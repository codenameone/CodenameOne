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

/// One change to make: set one [Trait] on one [AccessoryService] to one value.
///
/// ```java
/// TraitWrite w = new TraitWrite(lamp, lamp.getPrimaryService(),
///         Trait.BRIGHTNESS, TraitValue.of(40, TraitUnit.PERCENT));
/// SmartHome.getInstance().write(w).onResult((results, err) -> { ... });
/// ```
///
/// Writes name a **service**, not just an accessory, because an accessory can
/// have several -- the two halves of a two-gang switch both expose
/// [Trait#ON_OFF] and writing to "the switch" would be ambiguous.
public final class TraitWrite {

    private final String accessoryId;
    private final String serviceId;
    private final Trait trait;
    private final TraitValue value;
    private String authorizationData;

    /// Creates a write against a snapshot.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the accessory to change
    ///
    /// - `service`: the service on it
    ///
    /// - `trait`: the trait to set
    ///
    /// - `value`: the value to set it to
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null`, when the
    ///   value's kind does not match the trait's, or when the trait can never
    ///   be written
    public TraitWrite(Accessory accessory, AccessoryService service,
            Trait trait, TraitValue value) {
        this(idOf(accessory), idOf(service), trait, value);
    }

    /// Pulled out so the snapshot constructor can validate before delegating.
    /// A `this(...)` call has to be the first statement, so a null check
    /// written after it would report "accessory id is required" for a null
    /// accessory -- true, and unhelpful about which of the two the caller
    /// actually passed.
    private static String idOf(Accessory accessory) {
        if (accessory == null) {
            throw new IllegalArgumentException("accessory is required");
        }
        return accessory.getId();
    }

    private static String idOf(AccessoryService service) {
        if (service == null) {
            throw new IllegalArgumentException("service is required");
        }
        return service.getId();
    }

    /// Creates a write against identifiers, for code working from persisted
    /// ids that has not re-fetched the graph.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory to change
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait to set
    ///
    /// - `value`: the value to set it to
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null` or empty,
    ///   when the value's kind does not match the trait's, or when the trait
    ///   can never be written
    public TraitWrite(String accessoryId, String serviceId, Trait trait,
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
        if (trait.isReadOnly()) {
            throw new IllegalArgumentException(trait.getId()
                    + " is read-only on every accessory and every backend;"
                    + " it reports what the device is doing rather than"
                    + " telling it what to do");
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

    /// A credential the accessory needs before it will obey.
    ///
    /// The only thing that uses this today is a Matter door lock configured
    /// with `RequirePINforRemoteOperation`, which refuses
    /// [Trait#TARGET_LOCK_STATE] without a user PIN and fails the write with
    /// [HomeError#PIN_REQUIRED]. HomeKit never takes a PIN and ignores this.
    ///
    /// Treat the value the way you would any credential: do not log it, do not
    /// persist it in the clear, and prefer prompting for it over storing it.
    /// It is passed straight to the platform and is not retained here beyond
    /// the life of this object.
    ///
    /// #### Parameters
    ///
    /// - `authorizationData`: the credential, or `null` to clear it
    ///
    /// #### Returns
    ///
    /// this write, for chaining
    public TraitWrite setAuthorizationData(String authorizationData) {
        this.authorizationData = authorizationData;
        return this;
    }

    /// The credential set by
    /// [#setAuthorizationData(java.lang.String)].
    ///
    /// #### Returns
    ///
    /// the credential, or `null`
    public String getAuthorizationData() {
        return authorizationData;
    }

    /// The accessory this write changes.
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

    /// The trait this write sets.
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

    /// This write as a scene action, for building a [Scene] out of the
    /// changes a user just made.
    ///
    /// Drops any credential: a scene is replayed by the platform later, when
    /// this app is not running, and there is nowhere to put one.
    ///
    /// #### Returns
    ///
    /// an equivalent scene action
    public SceneAction toSceneAction() {
        return new SceneAction(accessoryId, serviceId, trait, value);
    }

    @Override
    public String toString() {
        return accessoryId + "/" + serviceId + " " + trait.getId() + ":="
                + value;
    }
}
