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
package android.location;

/// What a caller wants from a provider; only the accuracy is consulted by
/// [LocationManager#getBestProvider(Criteria, boolean)].
public class Criteria {

    public static final int NO_REQUIREMENT = 0;
    public static final int POWER_LOW = 1;
    public static final int POWER_MEDIUM = 2;
    public static final int POWER_HIGH = 3;
    public static final int ACCURACY_FINE = 1;
    public static final int ACCURACY_COARSE = 2;
    public static final int ACCURACY_LOW = 1;
    public static final int ACCURACY_MEDIUM = 2;
    public static final int ACCURACY_HIGH = 3;

    private int accuracy = NO_REQUIREMENT;
    private int powerRequirement = NO_REQUIREMENT;
    private int horizontalAccuracy = NO_REQUIREMENT;
    private int verticalAccuracy = NO_REQUIREMENT;
    private int speedAccuracy = NO_REQUIREMENT;
    private int bearingAccuracy = NO_REQUIREMENT;
    private boolean altitudeRequired;
    private boolean bearingRequired;
    private boolean speedRequired;
    private boolean costAllowed = true;

    public Criteria() {
    }

    public Criteria(Criteria c) {
        accuracy = c.accuracy;
        powerRequirement = c.powerRequirement;
        horizontalAccuracy = c.horizontalAccuracy;
        verticalAccuracy = c.verticalAccuracy;
        speedAccuracy = c.speedAccuracy;
        bearingAccuracy = c.bearingAccuracy;
        altitudeRequired = c.altitudeRequired;
        bearingRequired = c.bearingRequired;
        speedRequired = c.speedRequired;
        costAllowed = c.costAllowed;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getPowerRequirement() {
        return powerRequirement;
    }

    public void setPowerRequirement(int level) {
        powerRequirement = level;
    }

    public int getHorizontalAccuracy() {
        return horizontalAccuracy;
    }

    public void setHorizontalAccuracy(int accuracy) {
        horizontalAccuracy = accuracy;
    }

    public int getVerticalAccuracy() {
        return verticalAccuracy;
    }

    public void setVerticalAccuracy(int accuracy) {
        verticalAccuracy = accuracy;
    }

    public int getSpeedAccuracy() {
        return speedAccuracy;
    }

    public void setSpeedAccuracy(int accuracy) {
        speedAccuracy = accuracy;
    }

    public int getBearingAccuracy() {
        return bearingAccuracy;
    }

    public void setBearingAccuracy(int accuracy) {
        bearingAccuracy = accuracy;
    }

    public boolean isAltitudeRequired() {
        return altitudeRequired;
    }

    public void setAltitudeRequired(boolean required) {
        altitudeRequired = required;
    }

    public boolean isBearingRequired() {
        return bearingRequired;
    }

    public void setBearingRequired(boolean required) {
        bearingRequired = required;
    }

    public boolean isSpeedRequired() {
        return speedRequired;
    }

    public void setSpeedRequired(boolean required) {
        speedRequired = required;
    }

    public boolean isCostAllowed() {
        return costAllowed;
    }

    public void setCostAllowed(boolean allowed) {
        costAllowed = allowed;
    }

    /// True when the caller asked for anything finer than coarse.
    boolean wantsFine() {
        return accuracy == ACCURACY_FINE || horizontalAccuracy == ACCURACY_HIGH
                || powerRequirement == POWER_HIGH;
    }
}
