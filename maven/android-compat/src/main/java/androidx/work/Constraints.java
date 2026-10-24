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
package androidx.work;

/// What must hold before a request runs. Codename One's background scheduler
/// enforces all of them for work it runs while the application is in the
/// background; see [WorkManager] for what is checked in the foreground.
public final class Constraints {

    public static final Constraints NONE = new Builder().build();

    final NetworkType networkType;
    final boolean requiresCharging;
    final boolean requiresBatteryNotLow;
    final boolean requiresDeviceIdle;
    final boolean requiresStorageNotLow;

    Constraints(NetworkType networkType, boolean charging, boolean batteryNotLow, boolean idle,
            boolean storageNotLow) {
        this.networkType = networkType;
        this.requiresCharging = charging;
        this.requiresBatteryNotLow = batteryNotLow;
        this.requiresDeviceIdle = idle;
        this.requiresStorageNotLow = storageNotLow;
    }

    public NetworkType getRequiredNetworkType() {
        return networkType;
    }

    public boolean requiresCharging() {
        return requiresCharging;
    }

    public boolean requiresBatteryNotLow() {
        return requiresBatteryNotLow;
    }

    public boolean requiresDeviceIdle() {
        return requiresDeviceIdle;
    }

    public boolean requiresStorageNotLow() {
        return requiresStorageNotLow;
    }

    /// Constraints only the platform scheduler can judge: there is no
    /// Codename One API that reports charging, idle or storage state.
    boolean needsPlatformScheduler() {
        return requiresCharging || requiresBatteryNotLow || requiresDeviceIdle || requiresStorageNotLow;
    }

    public static final class Builder {
        private NetworkType networkType = NetworkType.NOT_REQUIRED;
        private boolean charging;
        private boolean batteryNotLow;
        private boolean idle;
        private boolean storageNotLow;

        public Builder setRequiredNetworkType(NetworkType networkType) {
            this.networkType = networkType;
            return this;
        }

        public Builder setRequiresCharging(boolean requiresCharging) {
            charging = requiresCharging;
            return this;
        }

        public Builder setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
            batteryNotLow = requiresBatteryNotLow;
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            idle = requiresDeviceIdle;
            return this;
        }

        public Builder setRequiresStorageNotLow(boolean requiresStorageNotLow) {
            storageNotLow = requiresStorageNotLow;
            return this;
        }

        public Builder addContentUriTrigger(android.net.Uri uri, boolean triggerForDescendants) {
            return this;
        }

        public Constraints build() {
            return new Constraints(networkType, charging, batteryNotLow, idle, storageNotLow);
        }
    }
}
