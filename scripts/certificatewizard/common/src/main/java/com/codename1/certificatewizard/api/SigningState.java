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
package com.codename1.certificatewizard.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SigningState {
    public final Credential credential;
    public final List<Certificate> certificates;
    public final List<BundleId> bundleIds;
    public final List<Device> devices;
    public final List<Profile> profiles;
    public final List<ApnsKey> apnsKeys;
    public final List<AppGroup> appGroups;

    public SigningState(Credential credential, List<Certificate> certificates, List<BundleId> bundleIds,
                        List<Device> devices, List<Profile> profiles, List<ApnsKey> apnsKeys,
                        List<AppGroup> appGroups) {
        this.credential = credential == null ? new Credential(false, null, null) : credential;
        this.certificates = immutable(certificates);
        this.bundleIds = immutable(bundleIds);
        this.devices = immutable(devices);
        this.profiles = immutable(profiles);
        this.apnsKeys = immutable(apnsKeys);
        this.appGroups = immutable(appGroups);
    }

    private static <T> List<T> immutable(List<T> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    public int expiringCount(long now) {
        int count = 0;
        for (Certificate c : certificates) {
            if (isExpiringSoon(c.expiresAt, now) && "ACTIVE".equals(c.status)) {
                count++;
            }
        }
        for (Profile p : profiles) {
            if (isExpiringSoon(p.expiresAt, now) && "ACTIVE".equals(p.status)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isExpiringSoon(Long expiresAt, long now) {
        if (expiresAt == null) {
            return false;
        }
        long days = (expiresAt.longValue() - now) / 86400000L;
        return days <= 30;
    }

    public static SigningState empty() {
        return new SigningState(new Credential(false, null, null), null, null, null, null, null, null);
    }

    public record Credential(boolean configured, String keyId, String issuerId) {}
    public record Certificate(Long id, String appleCertId, String certificateType, String displayName,
                              String serialNumber, Long expiresAt, String status, boolean privateKeyPresent) {}
    /// An App ID. `pushEnabled` is null when the service did not report the App ID's
    /// capabilities: the listing endpoint carries the identifier, name and platform and
    /// nothing else, and reading that absence as "off" is how the wizard came to print
    /// Push: Off beside an App ID whose profiles Apple shows carrying Push Notifications
    /// (issue #5657).
    public record BundleId(String id, String identifier, String name, String platform, Boolean pushEnabled) {}
    public record Device(String id, String name, String udid, String platform, String status) {}
    public record Profile(Long id, String appleProfileId, String name, String profileType, String bundleId,
                          String uuid, Long expiresAt, String status) {}
    public record ApnsKey(String keyId, String teamId, String displayName, Long createdAt) {}
    public record AppGroup(String id, String identifier, String name) {}
}
