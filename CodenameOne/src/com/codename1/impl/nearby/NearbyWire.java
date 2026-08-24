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
package com.codename1.impl.nearby;

import com.codename1.nearby.NearbyError;
import com.codename1.nearby.NearbyException;
import com.codename1.nearby.companion.CompanionDevice;
import com.codename1.nearby.companion.CompanionProfile;
import com.codename1.nearby.companion.DeviceFilter;
import com.codename1.nearby.transport.Endpoint;

import java.util.ArrayList;
import java.util.List;

/// The encoding `com.codename1.nearby.spi.NearbyBridge` speaks, and the only
/// place that knows it.
///
/// Tab-delimited fields, one record per array entry, for the reason
/// `com.codename1.impl.home.HomeWire` gives: every port has to implement the
/// encoder by hand, several of them in Objective-C, and a wire a human can
/// read in a log repays the bytes it costs.
///
/// #### Every decoder here is total
///
/// A malformed record decodes to `null` and is skipped by the caller, never
/// thrown over. Records arrive from native code in batches, and a parser
/// that threw would discard the good rows alongside the bad one. The single
/// exception is [#decodeError], whose whole purpose is to produce an
/// exception.
///
/// @hidden not part of the public API.
public final class NearbyWire {

    /// The field separator.
    public static final char SEPARATOR = '\t';

    private NearbyWire() {
    }

    // ------------------------------------------------------------------
    // primitives
    // ------------------------------------------------------------------

    /// Splits one record into its fields, preserving trailing empty ones --
    /// unlike `String.split`, whose dropping of them would shift every
    /// index for a record ending in an absent address.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record, or null
    ///
    /// #### Returns
    ///
    /// the fields, never null
    public static String[] split(String line) {
        if (line == null) {
            return new String[0];
        }
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == SEPARATOR) {
                out.add(line.substring(start, i));
                start = i + 1;
            }
        }
        out.add(line.substring(start));
        String[] result = new String[out.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = out.get(i);
        }
        return result;
    }

    /// One field, or the empty string when the record is shorter than that.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    /// - `index`: the field wanted
    ///
    /// #### Returns
    ///
    /// the field, never null
    public static String field(String[] fields, int index) {
        if (fields == null || index < 0 || index >= fields.length) {
            return "";
        }
        return fields[index] == null ? "" : fields[index];
    }

    /// One field as a flag, where `"1"` is true and anything else is false.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    /// - `index`: the field wanted
    ///
    /// #### Returns
    ///
    /// the flag
    public static boolean flag(String[] fields, int index) {
        return "1".equals(field(fields, index));
    }

    /// One field as an int, falling back when it is absent or not a number.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    /// - `index`: the field wanted
    /// - `fallback`: what to answer when the field is unusable
    ///
    /// #### Returns
    ///
    /// the parsed value or the fallback
    public static int integer(String[] fields, int index, int fallback) {
        String v = field(fields, index);
        if (v.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /// One field as a long, falling back when it is absent or not a number.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    /// - `index`: the field wanted
    /// - `fallback`: what to answer when the field is unusable
    ///
    /// #### Returns
    ///
    /// the parsed value or the fallback
    public static long integer64(String[] fields, int index, long fallback) {
        String v = field(fields, index);
        if (v.length() == 0) {
            return fallback;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /// Joins fields into a record, sanitizing each.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the fields
    ///
    /// #### Returns
    ///
    /// the record, never null
    public static String join(String[] fields) {
        if (fields == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                b.append(SEPARATOR);
            }
            b.append(sanitize(fields[i]));
        }
        return b.toString();
    }

    /// Makes a field safe to put in a record: null becomes empty, and tabs,
    /// carriage returns and newlines become spaces.
    ///
    /// #### Parameters
    ///
    /// - `value`: the field, or null
    ///
    /// #### Returns
    ///
    /// the safe field, never null
    public static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder b = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == SEPARATOR || c == '\n' || c == '\r') {
                if (b == null) {
                    b = new StringBuilder(value.substring(0, i));
                }
                b.append(' ');
            } else if (b != null) {
                b.append(c);
            }
        }
        return b == null ? value : b.toString();
    }

    /// The flag form of a boolean.
    ///
    /// #### Parameters
    ///
    /// - `value`: the flag
    ///
    /// #### Returns
    ///
    /// `"1"` or `"0"`
    public static String flag(boolean value) {
        return value ? "1" : "0";
    }

    // ------------------------------------------------------------------
    // records
    // ------------------------------------------------------------------

    /// Encodes a device filter as `kind SEP value`.
    ///
    /// #### Parameters
    ///
    /// - `filter`: the filter
    ///
    /// #### Returns
    ///
    /// the record
    public static String encodeFilter(DeviceFilter filter) {
        return join(new String[] {
            Integer.toString(filter.getKind()), filter.getValue()
        });
    }

    /// Encodes a companion device as
    /// `id SEP name SEP address SEP profileOrdinal SEP presentFlag`.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device
    ///
    /// #### Returns
    ///
    /// the record
    public static String encodeCompanionDevice(CompanionDevice device) {
        return join(new String[] {
            device.getId(),
            device.getDisplayName(),
            device.getAddress() == null ? "" : device.getAddress(),
            Integer.toString(device.getProfile().ordinal()),
            flag(device.isPresent())
        });
    }

    /// Decodes a companion device.
    ///
    /// An empty address field decodes to `null` rather than to the empty
    /// string, because "the platform withholds the address" is what the
    /// public getter documents and an empty string would be handed straight
    /// to `BluetoothLE.getPeripheral`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// #### Returns
    ///
    /// the device, or null when the record has no id
    public static CompanionDevice decodeCompanionDevice(String line) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        String address = field(f, 2);
        return new CompanionDevice(id, field(f, 1),
                address.length() == 0 ? null : address,
                profileFor(integer(f, 3, 0)), flag(f, 4));
    }

    /// Decodes an endpoint from `id SEP name SEP serviceId`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// #### Returns
    ///
    /// the endpoint, or null when the record has no id
    public static Endpoint decodeEndpoint(String line) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        return new Endpoint(id, field(f, 1), field(f, 2));
    }

    /// Encodes an endpoint, the inverse of [#decodeEndpoint].
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the endpoint
    ///
    /// #### Returns
    ///
    /// the record
    public static String encodeEndpoint(Endpoint endpoint) {
        return join(new String[] {
            endpoint.getId(), endpoint.getName(), endpoint.getServiceId()
        });
    }

    /// Turns an error ordinal and message into an exception. Unlike every
    /// other decoder here this is expected to produce a failure, so an
    /// unrecognised ordinal becomes [NearbyError#UNKNOWN] rather than being
    /// skipped.
    ///
    /// #### Parameters
    ///
    /// - `errorOrdinal`: the ordinal of a [NearbyError] constant
    /// - `message`: the detail, may be null
    ///
    /// #### Returns
    ///
    /// the exception, never null
    public static NearbyException decodeError(int errorOrdinal,
            String message) {
        NearbyError[] all = NearbyError.values();
        NearbyError e = errorOrdinal >= 0 && errorOrdinal < all.length
                ? all[errorOrdinal] : NearbyError.UNKNOWN;
        return new NearbyException(e, message == null || message.length() == 0
                ? e.name() : message);
    }

    /// The profile for an ordinal, falling back to
    /// [CompanionProfile#GENERIC] for one this build does not know -- a port
    /// from a newer build must not cost us the whole record.
    ///
    /// #### Parameters
    ///
    /// - `ordinal`: the ordinal
    ///
    /// #### Returns
    ///
    /// the profile, never null
    public static CompanionProfile profileFor(int ordinal) {
        CompanionProfile[] all = CompanionProfile.values();
        if (ordinal < 0 || ordinal >= all.length) {
            return CompanionProfile.GENERIC;
        }
        return all[ordinal];
    }
}
