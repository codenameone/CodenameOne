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
package com.codename1.impl.call;

import com.codename1.call.CallEndReason;
import com.codename1.call.CallError;
import com.codename1.call.CallException;
import com.codename1.call.CallHandle;
import com.codename1.call.CallHandleType;
import com.codename1.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/// The encoding `com.codename1.call.spi.CallBridge` speaks, and the only
/// place that knows it.
///
/// Tab-delimited fields, one record per array entry, for the reason
/// `com.codename1.impl.nearby.NearbyWire` gives: every port has to implement
/// the encoder by hand, several of them in Objective-C, and a wire a human
/// can read in a log repays the bytes it costs.
///
/// #### Every decoder here is total
///
/// A malformed record decodes to `null` and is skipped by the caller, never
/// thrown over. Records arrive from native code in batches -- a drained
/// queue of calls that rang while the app was starting is exactly such a
/// batch -- and a parser that threw would discard the good rows alongside
/// the bad one, losing calls the user actually saw. The single exception is
/// [#decodeError], whose whole purpose is to produce an exception.
///
/// Note the decoders here never cast to a type they have not already tested
/// with `instanceof`: ParparVM's `CHECKCAST` is unchecked, so a cast that
/// fails on iOS does not throw and cannot be caught.
///
/// @hidden not part of the public API.
public final class CallWire {

    /// The field separator.
    public static final char SEPARATOR = '\t';

    private CallWire() {
    }

    // ------------------------------------------------------------------
    // primitives
    // ------------------------------------------------------------------

    /// Splits one record into its fields, preserving trailing empty ones --
    /// unlike `String.split`, whose dropping of them would shift every index
    /// for a record ending in an absent display name.
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
    public static String field(String[] fields, int index) {
        if (fields == null || index < 0 || index >= fields.length) {
            return "";
        }
        return fields[index] == null ? "" : fields[index];
    }

    /// One field as a flag, where `"1"` is true and anything else is false.
    public static boolean flag(String[] fields, int index) {
        return "1".equals(field(fields, index));
    }

    /// One field as an int, falling back when it is absent or not a number.
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
    public static String join(String[] fields) {
        if (fields == null || fields.length == 0) {
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

    /// Renders a flag the way [#flag] reads it.
    public static String flagOf(boolean value) {
        return value ? "1" : "0";
    }

    // ------------------------------------------------------------------
    // handles
    // ------------------------------------------------------------------

    /// Encodes a handle as `type-ordinal TAB value`.
    ///
    /// #### Parameters
    ///
    /// - `handle`: the handle, or null
    ///
    /// #### Returns
    ///
    /// the record, or the empty string when `handle` is null
    public static String encodeHandle(CallHandle handle) {
        if (handle == null) {
            return "";
        }
        return join(new String[]{
            String.valueOf(handle.getType().ordinal()),
            handle.getValue()
        });
    }

    /// Decodes a handle, answering null when the record is unusable.
    ///
    /// An out-of-range type ordinal decodes to
    /// [CallHandleType#GENERIC] rather than to null: a handle whose address
    /// survived is still worth showing, and the alternative is a call that
    /// rings with no caller at all because a newer port sent a type this
    /// build has not heard of.
    public static CallHandle decodeHandle(String record) {
        if (record == null || record.length() == 0) {
            return null;
        }
        String[] f = split(record);
        String value = field(f, 1);
        if (value.length() == 0) {
            return null;
        }
        return new CallHandle(handleType(integer(f, 0, 0)), value);
    }

    /// Maps a type ordinal, tolerating one this build does not know.
    /// Decodes the comma-separated handle-type ordinals a configuration
    /// carries in one field.
    ///
    /// Answers an EMPTY list for a field that is absent or blank, which the
    /// callers read as "the app named none" rather than as "the app wants
    /// none" -- the two differ, and only the caller knows its defaults.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    /// - `index`: the field holding the ordinals
    ///
    /// #### Returns
    ///
    /// the types, never null
    public static java.util.List<CallHandleType> handleTypes(String[] fields,
            int index) {
        java.util.List<CallHandleType> out =
                new java.util.ArrayList<CallHandleType>();
        String raw = field(fields, index);
        if (raw.length() == 0) {
            return out;
        }
        for (String part : StringUtil.tokenize(raw, ',')) {
            String trimmed = part.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            try {
                out.add(handleType(Integer.parseInt(trimmed)));
            } catch (NumberFormatException notAnOrdinal) {
                // A record this class did not write; the rest may still be
                // usable, and handleType already clamps an out-of-range one.
                continue;
            }
        }
        return out;
    }

    public static CallHandleType handleType(int ordinal) {
        CallHandleType[] values = CallHandleType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CallHandleType.GENERIC;
        }
        return values[ordinal];
    }

    /// Maps an end-reason ordinal, tolerating one this build does not know.
    public static CallEndReason endReason(int ordinal) {
        CallEndReason[] values = CallEndReason.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CallEndReason.FAILED;
        }
        return values[ordinal];
    }

    /// Maps an error ordinal, tolerating one this build does not know.
    public static CallError error(int ordinal) {
        CallError[] values = CallError.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CallError.UNKNOWN;
        }
        return values[ordinal];
    }

    // ------------------------------------------------------------------
    // errors
    // ------------------------------------------------------------------

    /// Builds the exception a port's failure answer describes.
    ///
    /// Unlike every other decoder here this one always produces something:
    /// it is called on a path that has already failed, and answering null
    /// would leave the caller's `AsyncResource` unsettled -- the one outcome
    /// the bridge contract rules out.
    ///
    /// #### Parameters
    ///
    /// - `errorOrdinal`: a [CallError] ordinal
    /// - `message`: what the platform said, may be null
    ///
    /// #### Returns
    ///
    /// the exception, never null
    public static CallException decodeError(int errorOrdinal, String message) {
        CallError e = error(errorOrdinal);
        if (message == null || message.length() == 0) {
            return new CallException(e);
        }
        return new CallException(e, message);
    }
}
