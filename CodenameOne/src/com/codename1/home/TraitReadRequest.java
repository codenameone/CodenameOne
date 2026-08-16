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

/// Which traits to read, and how fresh they have to be.
///
/// ```java
/// TraitReadRequest req = new TraitReadRequest()
///         .add(lamp, lamp.getPrimaryService(), Trait.ON_OFF)
///         .add(lamp, lamp.getPrimaryService(), Trait.BRIGHTNESS);
/// SmartHome.getInstance().read(req).onResult((readings, err) -> { ... });
/// ```
///
/// #### Batch, because the boundary is the cost
///
/// Reading four traits off one lamp as four calls is four round trips to the
/// platform and, on Matter, potentially four radio exchanges with a
/// battery-powered device. One request carrying four traits is one of each.
/// Where a request exceeds [SmartHome#getMaxReadBatchSize()] it is split for
/// you and the readings are recombined, so there is no size a caller has to
/// stay under -- but there is a real reason to fill it.
public final class TraitReadRequest {

    private final List<String> accessoryIds = new ArrayList<String>();
    private final List<String> serviceIds = new ArrayList<String>();
    private final List<Trait> traits = new ArrayList<Trait>();
    private boolean allowCached = true;

    /// Creates an empty request.
    public TraitReadRequest() {
    }

    /// Adds one trait on one service.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the accessory to read
    ///
    /// - `service`: the service on it
    ///
    /// - `trait`: the trait to read
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null`
    public TraitReadRequest add(Accessory accessory, AccessoryService service,
            Trait trait) {
        if (accessory == null) {
            throw new IllegalArgumentException("accessory is required");
        }
        if (service == null) {
            throw new IllegalArgumentException("service is required");
        }
        return add(accessory.getId(), service.getId(), trait);
    }

    /// Adds one trait on one service, by identifier.
    ///
    /// The identifier form exists for code working from persisted ids that has
    /// not re-fetched the graph. It skips the
    /// [AccessoryService#supports(Trait)] check the snapshot would have
    /// allowed, so an unsupported trait surfaces as a
    /// [HomeError#TRAIT_NOT_SUPPORTED] reading rather than as an argument
    /// error.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory to read
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait to read
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null` or empty
    public TraitReadRequest add(String accessoryId, String serviceId,
            Trait trait) {
        if (accessoryId == null || accessoryId.length() == 0) {
            throw new IllegalArgumentException("accessory id is required");
        }
        if (serviceId == null || serviceId.length() == 0) {
            throw new IllegalArgumentException("service id is required");
        }
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        accessoryIds.add(accessoryId);
        serviceIds.add(serviceId);
        traits.add(trait);
        return this;
    }

    /// Adds every trait one service exposes.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the accessory to read
    ///
    /// - `service`: the service whose traits to read
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when either argument is `null`
    public TraitReadRequest addAll(Accessory accessory,
            AccessoryService service) {
        if (accessory == null) {
            throw new IllegalArgumentException("accessory is required");
        }
        if (service == null) {
            throw new IllegalArgumentException("service is required");
        }
        List<TraitConstraint> cs = service.getConstraints();
        for (int i = 0; i < cs.size(); i++) {
            if (cs.get(i).isReadable()) {
                add(accessory.getId(), service.getId(), cs.get(i).getTrait());
            }
        }
        return this;
    }

    /// Whether the platform may answer from its own cache.
    ///
    /// `true` by default, which is almost always right: both platforms keep a
    /// current view of accessory state and answering from it is instant and
    /// costs a battery-powered accessory nothing. Set `false` only when you
    /// genuinely need to force the radio -- a diagnostic screen, a "refresh"
    /// the user explicitly asked for -- and expect it to be slower and to fail
    /// more often, because it can no longer paper over an accessory that is
    /// briefly unreachable.
    ///
    /// Backends that have no cache to bypass ignore this.
    ///
    /// #### Parameters
    ///
    /// - `allowCached`: whether cached values are acceptable
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public TraitReadRequest setAllowCached(boolean allowCached) {
        this.allowCached = allowCached;
        return this;
    }

    /// Whether the platform may answer from its own cache.
    ///
    /// #### Returns
    ///
    /// `true` when cached values are acceptable
    public boolean isAllowCached() {
        return allowCached;
    }

    /// How many traits this request asks for.
    ///
    /// #### Returns
    ///
    /// the count, zero for an empty request
    public int size() {
        return traits.size();
    }

    /// Whether this request asks for nothing.
    ///
    /// #### Returns
    ///
    /// `true` when nothing has been added
    public boolean isEmpty() {
        return traits.isEmpty();
    }

    /// The accessory ids, positionally aligned with [#getServiceIds()] and
    /// [#getTraits()].
    ///
    /// #### Returns
    ///
    /// an immutable list
    public List<String> getAccessoryIds() {
        return Collections.unmodifiableList(accessoryIds);
    }

    /// The service ids, positionally aligned with [#getAccessoryIds()] and
    /// [#getTraits()].
    ///
    /// #### Returns
    ///
    /// an immutable list
    public List<String> getServiceIds() {
        return Collections.unmodifiableList(serviceIds);
    }

    /// The traits, positionally aligned with [#getAccessoryIds()] and
    /// [#getServiceIds()].
    ///
    /// #### Returns
    ///
    /// an immutable list
    public List<Trait> getTraits() {
        return Collections.unmodifiableList(traits);
    }
}
