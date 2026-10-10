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
package com.codenameone.examples.wayline;

import com.codename1.io.Preferences;
import com.codenameone.examples.wayline.api.PlaceDto;
import java.util.ArrayList;
import java.util.List;

/// What the user has chosen in Settings, and the places they go back to.
///
/// Every screen reads and writes a preference through this class and through
/// nothing else, so where the values live is decided in one place. Today that
/// is the device: `Preferences`, which survives a restart and is gone with the
/// app. A server that keeps the same values for the account plugs in at
/// [#onChange]: it is told the name of each value as it changes, and puts the
/// account's values in with the setters when it has fetched them. No screen
/// has to change for that.
public final class Prefs {
    /// Told when a value changes, with the value's name.
    public interface Listener {
        void changed(String name);
    }

    public static final String LANGUAGE = "language";
    public static final String THEME = "theme";
    public static final String UNITS = "units";
    public static final String NOTIFY_RIDE_UPDATES = "notifyRideUpdates";
    public static final String NOTIFY_RECEIPTS = "notifyReceipts";
    public static final String NOTIFY_PROMOTIONS = "notifyPromotions";
    public static final String DRIVER_GENDER = "driverGender";
    public static final String QUIET_RIDE = "quietRide";
    public static final String ACCESSIBLE_VEHICLE = "accessibleVehicle";
    public static final String PET_FRIENDLY = "petFriendly";
    public static final String PAYMENT_METHOD = "defaultPaymentMethodId";
    public static final String TIP_PERCENT = "defaultTipPercent";
    public static final String SHARE_TRIPS = "shareTrips";
    public static final String EMERGENCY_NAME = "emergencyContactName";
    public static final String EMERGENCY_PHONE = "emergencyContactPhone";
    public static final String HOME = "home";
    public static final String WORK = "work";
    public static final String RECENTS = "recents";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String UNITS_KM = "km";
    public static final String UNITS_MILES = "mi";
    public static final String GENDER_ANY = "any";
    public static final String GENDER_WOMEN = "women";
    /// The one way to pay that needs nothing set up.
    public static final String PAYMENT_CASH = "cash";

    private static final String PREFIX = "wayline.pref.";
    /// How many destinations are remembered.
    private static final int RECENT_LIMIT = 4;

    private static Listener listener;

    private Prefs() {
    }

    /// Names the one listener told of every change. A value put in by the
    /// listener itself is reported to it like any other, so a listener that
    /// stores what it is told must not answer by setting the value again.
    public static void onChange(Listener told) {
        listener = told;
    }

    // -------------------------------------------------------------- appearance

    /// The language chosen, as its two-letter code; empty while the user has
    /// chosen none and the device's language decides.
    public static String language() {
        return text(LANGUAGE, "");
    }

    public static void setLanguage(String code) {
        put(LANGUAGE, code);
    }

    /// [#THEME_SYSTEM], [#THEME_LIGHT] or [#THEME_DARK].
    public static String theme() {
        return text(THEME, THEME_SYSTEM);
    }

    public static void setTheme(String theme) {
        put(THEME, theme);
    }

    /// [#UNITS_KM] or [#UNITS_MILES].
    public static String units() {
        return text(UNITS, UNITS_KM);
    }

    public static void setUnits(String units) {
        put(UNITS, units);
    }

    // ----------------------------------------------------------- notifications

    public static boolean notifyRideUpdates() {
        return flag(NOTIFY_RIDE_UPDATES, true);
    }

    public static void setNotifyRideUpdates(boolean on) {
        put(NOTIFY_RIDE_UPDATES, on);
    }

    public static boolean notifyReceipts() {
        return flag(NOTIFY_RECEIPTS, true);
    }

    public static void setNotifyReceipts(boolean on) {
        put(NOTIFY_RECEIPTS, on);
    }

    public static boolean notifyPromotions() {
        return flag(NOTIFY_PROMOTIONS, false);
    }

    public static void setNotifyPromotions(boolean on) {
        put(NOTIFY_PROMOTIONS, on);
    }

    // -------------------------------------------------------------- the ride

    /// Who the rider wants at the wheel: [#GENDER_ANY] or [#GENDER_WOMEN].
    public static String driverGender() {
        return text(DRIVER_GENDER, GENDER_ANY);
    }

    public static void setDriverGender(String gender) {
        put(DRIVER_GENDER, gender);
    }

    public static boolean quietRide() {
        return flag(QUIET_RIDE, false);
    }

    public static void setQuietRide(boolean on) {
        put(QUIET_RIDE, on);
    }

    public static boolean accessibleVehicle() {
        return flag(ACCESSIBLE_VEHICLE, false);
    }

    public static void setAccessibleVehicle(boolean on) {
        put(ACCESSIBLE_VEHICLE, on);
    }

    public static boolean petFriendly() {
        return flag(PET_FRIENDLY, false);
    }

    public static void setPetFriendly(boolean on) {
        put(PET_FRIENDLY, on);
    }

    // --------------------------------------------------------------- payment

    /// The id of the way the user pays unless they say otherwise for a ride.
    public static String paymentMethod() {
        return text(PAYMENT_METHOD, PAYMENT_CASH);
    }

    public static void setPaymentMethod(String id) {
        put(PAYMENT_METHOD, id);
    }

    /// The tip offered first on a receipt, as a percentage of the fare.
    public static int tipPercent() {
        return Preferences.get(PREFIX + TIP_PERCENT, 0);
    }

    public static void setTipPercent(int percent) {
        if (percent != tipPercent()) {
            Preferences.set(PREFIX + TIP_PERCENT, percent);
            changed(TIP_PERCENT);
        }
    }

    // ---------------------------------------------------- privacy and safety

    /// Whether a ride under way is shared with the emergency contact.
    public static boolean shareTrips() {
        return flag(SHARE_TRIPS, false);
    }

    public static void setShareTrips(boolean on) {
        put(SHARE_TRIPS, on);
    }

    public static String emergencyContactName() {
        return text(EMERGENCY_NAME, "");
    }

    public static void setEmergencyContactName(String name) {
        put(EMERGENCY_NAME, name);
    }

    public static String emergencyContactPhone() {
        return text(EMERGENCY_PHONE, "");
    }

    public static void setEmergencyContactPhone(String phone) {
        put(EMERGENCY_PHONE, phone);
    }

    // ---------------------------------------------------------------- places

    /// Home, or null while the user has named none.
    public static PlaceDto home() {
        return place(HOME);
    }

    public static void setHome(PlaceDto place) {
        store(HOME, place);
        changed(HOME);
    }

    /// Work, or null while the user has named none.
    public static PlaceDto work() {
        return place(WORK);
    }

    public static void setWork(PlaceDto place) {
        store(WORK, place);
        changed(WORK);
    }

    /// The last few destinations, the newest first.
    public static List<PlaceDto> recents() {
        List<PlaceDto> all = new ArrayList<PlaceDto>();
        for (int iter = 0; iter < RECENT_LIMIT; iter++) {
            PlaceDto place = place(RECENTS + "." + iter);
            if (place != null) {
                all.add(place);
            }
        }
        return all;
    }

    /// Remembers `place` as the destination chosen last.
    public static void addRecent(PlaceDto place) {
        if (place == null || place.name == null || place.name.length() == 0) {
            return;
        }
        List<PlaceDto> all = recents();
        for (int iter = all.size() - 1; iter >= 0; iter--) {
            if (place.name.equals(all.get(iter).name)) {
                all.remove(iter);
            }
        }
        all.add(0, place);
        for (int iter = 0; iter < RECENT_LIMIT; iter++) {
            store(RECENTS + "." + iter, iter < all.size() ? all.get(iter) : null);
        }
        changed(RECENTS);
    }

    // ------------------------------------------------------------- the store

    private static String text(String name, String fallback) {
        return Preferences.get(PREFIX + name, fallback);
    }

    private static boolean flag(String name, boolean fallback) {
        return Preferences.get(PREFIX + name, fallback);
    }

    private static void put(String name, String value) {
        String wanted = value == null ? "" : value.trim();
        if (!wanted.equals(text(name, null))) {
            Preferences.set(PREFIX + name, wanted);
            changed(name);
        }
    }

    private static void put(String name, boolean value) {
        // Compared with the opposite as the fallback, so a value never stored
        // is stored the first time whatever it is.
        if (flag(name, !value) != value) {
            Preferences.set(PREFIX + name, value);
            changed(name);
        }
    }

    private static PlaceDto place(String name) {
        String title = text(name + ".name", "");
        if (title.length() == 0) {
            return null;
        }
        PlaceDto place = new PlaceDto();
        place.name = title;
        place.address = text(name + ".address", "");
        place.lat = Preferences.get(PREFIX + name + ".lat", 0d);
        place.lng = Preferences.get(PREFIX + name + ".lng", 0d);
        return place;
    }

    private static void store(String name, PlaceDto place) {
        Preferences.set(PREFIX + name + ".name", place == null ? "" : place.name);
        Preferences.set(PREFIX + name + ".address",
                place == null || place.address == null ? "" : place.address);
        Preferences.set(PREFIX + name + ".lat", place == null ? 0d : place.lat);
        Preferences.set(PREFIX + name + ".lng", place == null ? 0d : place.lng);
    }

    private static void changed(String name) {
        if (listener != null) {
            listener.changed(name);
        }
    }
}
