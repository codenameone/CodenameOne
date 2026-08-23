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
package com.codename1.nearby.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What to show the user in the system device chooser, built with
/// [AssociationRequest.Builder].
public final class AssociationRequest {

    private final CompanionProfile profile;
    private final boolean singleDevice;
    private final List<DeviceFilter> filters;

    private AssociationRequest(CompanionProfile profile, boolean singleDevice,
            List<DeviceFilter> filters) {
        this.profile = profile;
        this.singleDevice = singleDevice;
        this.filters = Collections.unmodifiableList(filters);
    }

    /// The profile requested. Never null.
    public CompanionProfile getProfile() {
        return profile;
    }

    /// Whether to associate immediately when exactly one device matches,
    /// rather than showing a one-item list.
    public boolean isSingleDevice() {
        return singleDevice;
    }

    /// The filters, OR-combined. Never null and possibly empty, in which
    /// case every visible device is offered.
    public List<DeviceFilter> getFilters() {
        return filters;
    }

    /// Assembles an [AssociationRequest].
    public static final class Builder {

        private CompanionProfile profile = CompanionProfile.GENERIC;
        private boolean singleDevice;
        private final List<DeviceFilter> filters = new ArrayList<DeviceFilter>();

        /// Sets the profile. Defaults to [CompanionProfile#GENERIC], which
        /// is what most accessories should ask for.
        ///
        /// #### Parameters
        ///
        /// - `profile`: the profile to request
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder profile(CompanionProfile profile) {
            this.profile = profile == null ? CompanionProfile.GENERIC : profile;
            return this;
        }

        /// Asks the platform to skip the chooser when exactly one device
        /// matches the filters. The user still consents -- they are shown
        /// one device and confirm it -- so this is a shortcut, not a way to
        /// associate silently.
        ///
        /// #### Parameters
        ///
        /// - `singleDevice`: whether to take the shortcut
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder singleDevice(boolean singleDevice) {
            this.singleDevice = singleDevice;
            return this;
        }

        /// Adds a filter. Filters are OR-combined.
        ///
        /// #### Parameters
        ///
        /// - `filter`: the filter to add
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder addFilter(DeviceFilter filter) {
            if (filter != null) {
                filters.add(filter);
            }
            return this;
        }

        /// Builds the request.
        ///
        /// #### Returns
        ///
        /// the immutable request
        public AssociationRequest build() {
            return new AssociationRequest(profile, singleDevice,
                    new ArrayList<DeviceFilter>(filters));
        }
    }
}
