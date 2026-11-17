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
import java.util.Objects;

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

    public static final class Credential {
        private final boolean configured;
        private final String keyId;
        private final String issuerId;

        public Credential(boolean configured, String keyId, String issuerId) {
            this.configured = configured;
            this.keyId = keyId;
            this.issuerId = issuerId;
        }

        public boolean configured() {
            return configured;
        }

        public String keyId() {
            return keyId;
        }

        public String issuerId() {
            return issuerId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Credential)) {
                return false;
            }
            Credential other = (Credential) o;
            return configured == other.configured
                    && Objects.equals(keyId, other.keyId)
                    && Objects.equals(issuerId, other.issuerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(configured, keyId, issuerId);
        }

        @Override
        public String toString() {
            return "Credential[" + "configured=" + configured + ", " + "keyId=" + keyId + ", " + "issuerId=" + issuerId + "]";
        }
    }

    public static final class Certificate {
        private final Long id;
        private final String appleCertId;
        private final String certificateType;
        private final String displayName;
        private final String serialNumber;
        private final Long expiresAt;
        private final String status;
        private final boolean privateKeyPresent;

        public Certificate(Long id, String appleCertId, String certificateType, String displayName, String serialNumber, Long expiresAt, String status, boolean privateKeyPresent) {
            this.id = id;
            this.appleCertId = appleCertId;
            this.certificateType = certificateType;
            this.displayName = displayName;
            this.serialNumber = serialNumber;
            this.expiresAt = expiresAt;
            this.status = status;
            this.privateKeyPresent = privateKeyPresent;
        }

        public Long id() {
            return id;
        }

        public String appleCertId() {
            return appleCertId;
        }

        public String certificateType() {
            return certificateType;
        }

        public String displayName() {
            return displayName;
        }

        public String serialNumber() {
            return serialNumber;
        }

        public Long expiresAt() {
            return expiresAt;
        }

        public String status() {
            return status;
        }

        public boolean privateKeyPresent() {
            return privateKeyPresent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Certificate)) {
                return false;
            }
            Certificate other = (Certificate) o;
            return Objects.equals(id, other.id)
                    && Objects.equals(appleCertId, other.appleCertId)
                    && Objects.equals(certificateType, other.certificateType)
                    && Objects.equals(displayName, other.displayName)
                    && Objects.equals(serialNumber, other.serialNumber)
                    && Objects.equals(expiresAt, other.expiresAt)
                    && Objects.equals(status, other.status)
                    && privateKeyPresent == other.privateKeyPresent;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, appleCertId, certificateType, displayName, serialNumber, expiresAt, status, privateKeyPresent);
        }

        @Override
        public String toString() {
            return "Certificate[" + "id=" + id + ", " + "appleCertId=" + appleCertId + ", " + "certificateType=" + certificateType + ", " + "displayName=" + displayName + ", " + "serialNumber=" + serialNumber + ", " + "expiresAt=" + expiresAt + ", " + "status=" + status + ", " + "privateKeyPresent=" + privateKeyPresent + "]";
        }
    }

    /// An App ID. `pushEnabled` is null when the service did not report the App ID's
    /// capabilities: the listing endpoint carries the identifier, name and platform and
    /// nothing else, and reading that absence as "off" is how the wizard came to print
    /// Push: Off beside an App ID whose profiles Apple shows carrying Push Notifications
    /// (issue #5657).
    public static final class BundleId {
        private final String id;
        private final String identifier;
        private final String name;
        private final String platform;
        private final Boolean pushEnabled;

        public BundleId(String id, String identifier, String name, String platform, Boolean pushEnabled) {
            this.id = id;
            this.identifier = identifier;
            this.name = name;
            this.platform = platform;
            this.pushEnabled = pushEnabled;
        }

        public String id() {
            return id;
        }

        public String identifier() {
            return identifier;
        }

        public String name() {
            return name;
        }

        public String platform() {
            return platform;
        }

        public Boolean pushEnabled() {
            return pushEnabled;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BundleId)) {
                return false;
            }
            BundleId other = (BundleId) o;
            return Objects.equals(id, other.id)
                    && Objects.equals(identifier, other.identifier)
                    && Objects.equals(name, other.name)
                    && Objects.equals(platform, other.platform)
                    && Objects.equals(pushEnabled, other.pushEnabled);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, identifier, name, platform, pushEnabled);
        }

        @Override
        public String toString() {
            return "BundleId[" + "id=" + id + ", " + "identifier=" + identifier + ", " + "name=" + name + ", " + "platform=" + platform + ", " + "pushEnabled=" + pushEnabled + "]";
        }
    }

    public static final class Device {
        private final String id;
        private final String name;
        private final String udid;
        private final String platform;
        private final String status;

        public Device(String id, String name, String udid, String platform, String status) {
            this.id = id;
            this.name = name;
            this.udid = udid;
            this.platform = platform;
            this.status = status;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String udid() {
            return udid;
        }

        public String platform() {
            return platform;
        }

        public String status() {
            return status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Device)) {
                return false;
            }
            Device other = (Device) o;
            return Objects.equals(id, other.id)
                    && Objects.equals(name, other.name)
                    && Objects.equals(udid, other.udid)
                    && Objects.equals(platform, other.platform)
                    && Objects.equals(status, other.status);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, udid, platform, status);
        }

        @Override
        public String toString() {
            return "Device[" + "id=" + id + ", " + "name=" + name + ", " + "udid=" + udid + ", " + "platform=" + platform + ", " + "status=" + status + "]";
        }
    }

    public static final class Profile {
        private final Long id;
        private final String appleProfileId;
        private final String name;
        private final String profileType;
        private final String bundleId;
        private final String uuid;
        private final Long expiresAt;
        private final String status;

        public Profile(Long id, String appleProfileId, String name, String profileType, String bundleId, String uuid, Long expiresAt, String status) {
            this.id = id;
            this.appleProfileId = appleProfileId;
            this.name = name;
            this.profileType = profileType;
            this.bundleId = bundleId;
            this.uuid = uuid;
            this.expiresAt = expiresAt;
            this.status = status;
        }

        public Long id() {
            return id;
        }

        public String appleProfileId() {
            return appleProfileId;
        }

        public String name() {
            return name;
        }

        public String profileType() {
            return profileType;
        }

        public String bundleId() {
            return bundleId;
        }

        public String uuid() {
            return uuid;
        }

        public Long expiresAt() {
            return expiresAt;
        }

        public String status() {
            return status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Profile)) {
                return false;
            }
            Profile other = (Profile) o;
            return Objects.equals(id, other.id)
                    && Objects.equals(appleProfileId, other.appleProfileId)
                    && Objects.equals(name, other.name)
                    && Objects.equals(profileType, other.profileType)
                    && Objects.equals(bundleId, other.bundleId)
                    && Objects.equals(uuid, other.uuid)
                    && Objects.equals(expiresAt, other.expiresAt)
                    && Objects.equals(status, other.status);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, appleProfileId, name, profileType, bundleId, uuid, expiresAt, status);
        }

        @Override
        public String toString() {
            return "Profile[" + "id=" + id + ", " + "appleProfileId=" + appleProfileId + ", " + "name=" + name + ", " + "profileType=" + profileType + ", " + "bundleId=" + bundleId + ", " + "uuid=" + uuid + ", " + "expiresAt=" + expiresAt + ", " + "status=" + status + "]";
        }
    }

    public static final class ApnsKey {
        private final String keyId;
        private final String teamId;
        private final String displayName;
        private final Long createdAt;

        public ApnsKey(String keyId, String teamId, String displayName, Long createdAt) {
            this.keyId = keyId;
            this.teamId = teamId;
            this.displayName = displayName;
            this.createdAt = createdAt;
        }

        public String keyId() {
            return keyId;
        }

        public String teamId() {
            return teamId;
        }

        public String displayName() {
            return displayName;
        }

        public Long createdAt() {
            return createdAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ApnsKey)) {
                return false;
            }
            ApnsKey other = (ApnsKey) o;
            return Objects.equals(keyId, other.keyId)
                    && Objects.equals(teamId, other.teamId)
                    && Objects.equals(displayName, other.displayName)
                    && Objects.equals(createdAt, other.createdAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyId, teamId, displayName, createdAt);
        }

        @Override
        public String toString() {
            return "ApnsKey[" + "keyId=" + keyId + ", " + "teamId=" + teamId + ", " + "displayName=" + displayName + ", " + "createdAt=" + createdAt + "]";
        }
    }

    public static final class AppGroup {
        private final String id;
        private final String identifier;
        private final String name;

        public AppGroup(String id, String identifier, String name) {
            this.id = id;
            this.identifier = identifier;
            this.name = name;
        }

        public String id() {
            return id;
        }

        public String identifier() {
            return identifier;
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AppGroup)) {
                return false;
            }
            AppGroup other = (AppGroup) o;
            return Objects.equals(id, other.id)
                    && Objects.equals(identifier, other.identifier)
                    && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, identifier, name);
        }

        @Override
        public String toString() {
            return "AppGroup[" + "id=" + id + ", " + "identifier=" + identifier + ", " + "name=" + name + "]";
        }
    }
}
