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
package com.codename1.impl.home;

import com.codename1.home.Accessory;
import com.codename1.home.AccessoryCategory;
import com.codename1.home.AccessoryService;
import com.codename1.home.HomeError;
import com.codename1.home.HomeException;
import com.codename1.home.HomeRoom;
import com.codename1.home.HomeZone;
import com.codename1.home.Scene;
import com.codename1.home.SceneAction;
import com.codename1.home.SceneType;
import com.codename1.home.ServiceType;
import com.codename1.home.Trait;
import com.codename1.home.TraitConstraint;
import com.codename1.home.TraitReading;
import com.codename1.home.TraitUnit;
import com.codename1.home.TraitValue;
import com.codename1.home.TraitValueKind;

import java.util.ArrayList;
import java.util.List;

/// The encoding `com.codename1.home.spi.HomeBridge` speaks, and the only place
/// that knows it.
///
/// Tab-delimited fields, one record per array entry. Not JSON, for the reason
/// `com.codename1.impl.health.HealthWire` gives: a home with a hundred watched
/// accessories produces a steady stream of small deliveries, and an object
/// graph per delivery is allocation the ParparVM heap does not need. Not a
/// binary format either -- a wire a human can read in a log is worth more than
/// the bytes it costs, and every port has to implement the encoder by hand.
///
/// #### Every decoder here is total
///
/// A malformed record is **skipped**, never thrown over. Records arrive from
/// native code on a delivery that may carry ninety good ones alongside it, and
/// a parser that threw would discard the lot -- so a port from a newer build
/// naming a trait this one does not have costs that row and nothing else.
///
/// The one exception is a *failure* delivery, where the whole point is to
/// produce an exception; see [#decodeError(java.lang.String)].
public final class HomeWire {

    /// The field separator.
    public static final char SEPARATOR = '\t';

    /// The separator inside a field that holds a list -- a zone's rooms, an
    /// enum trait's valid ordinals.
    public static final char LIST_SEPARATOR = ',';

    private HomeWire() {
    }

    // ------------------------------------------------------------------
    // primitives
    // ------------------------------------------------------------------

    /// Splits one record into its fields.
    ///
    /// Trailing empty fields are preserved, unlike `String.split`, because an
    /// accessory with no room and no bridge ends in two empty fields and
    /// losing them would shift every index.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record, or `null`
    ///
    /// #### Returns
    ///
    /// the fields, never `null`
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
    ///
    /// - `index`: the field wanted
    ///
    /// #### Returns
    ///
    /// the field, never `null`
    public static String field(String[] fields, int index) {
        if (fields == null || index < 0 || index >= fields.length) {
            return "";
        }
        return fields[index] == null ? "" : fields[index];
    }

    /// One field as a flag: `"1"` is true and everything else is false.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    ///
    /// - `index`: the field wanted
    ///
    /// #### Returns
    ///
    /// the flag
    public static boolean flag(String[] fields, int index) {
        return "1".equals(field(fields, index));
    }

    /// One field as an integer, or `fallback` when it is missing or not a
    /// number.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    ///
    /// - `index`: the field wanted
    ///
    /// - `fallback`: what to answer when it cannot be read
    ///
    /// #### Returns
    ///
    /// the value, or `fallback`
    public static int integer(String[] fields, int index, int fallback) {
        String s = field(fields, index);
        if (s.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException notANumber) {
            // Total by contract: the caller is decoding a delivery that may
            // hold good records either side of this one.
            return fallback;
        }
    }

    /// One field as a long, or `fallback` when it is missing or not a number.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    ///
    /// - `index`: the field wanted
    ///
    /// - `fallback`: what to answer when it cannot be read
    ///
    /// #### Returns
    ///
    /// the value, or `fallback`
    public static long integer64(String[] fields, int index, long fallback) {
        String s = field(fields, index);
        if (s.length() == 0) {
            return fallback;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    /// One field as a double, or `fallback` when it is missing or not a
    /// number.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the split record
    ///
    /// - `index`: the field wanted
    ///
    /// - `fallback`: what to answer when it cannot be read
    ///
    /// #### Returns
    ///
    /// the value, or `fallback`
    public static double real(String[] fields, int index, double fallback) {
        String s = field(fields, index);
        if (s.length() == 0) {
            return fallback;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    /// A comma-separated list field.
    ///
    /// #### Parameters
    ///
    /// - `value`: the field, or `null`
    ///
    /// #### Returns
    ///
    /// the items with empties dropped, never `null`
    public static List<String> list(String value) {
        List<String> out = new ArrayList<String>();
        if (value == null || value.length() == 0) {
            return out;
        }
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == LIST_SEPARATOR) {
                String item = value.substring(start, i).trim();
                if (item.length() > 0) {
                    out.add(item);
                }
                start = i + 1;
            }
        }
        return out;
    }

    /// Turns an error field into the exception to fail an operation with.
    ///
    /// The field is `<HomeError name>\t<platform message>`. The **name**,
    /// never an ordinal: a port built against a different version of the enum
    /// would otherwise map every error past an inserted constant onto the
    /// wrong one, and a mis-mapped authorization failure is indistinguishable
    /// from a mis-mapped timeout to everything downstream. An unrecognized
    /// name degrades to [HomeError#UNKNOWN] with the platform's text intact.
    ///
    /// #### Parameters
    ///
    /// - `encoded`: the error field, or `null` or empty for success
    ///
    /// #### Returns
    ///
    /// the exception, or `null` when the operation succeeded
    public static HomeException decodeError(String encoded) {
        if (encoded == null || encoded.length() == 0) {
            return null;
        }
        String[] parts = split(encoded);
        HomeError error = HomeError.forName(field(parts, 0));
        String message = field(parts, 1);
        if (message.length() == 0) {
            message = error.name();
        }
        return new HomeException(error, message);
    }

    // ------------------------------------------------------------------
    // graph records
    // ------------------------------------------------------------------

    /// Decodes a room record: `id \t name`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// - `structureId`: the home the room belongs to
    ///
    /// #### Returns
    ///
    /// the room, or `null` when the record has no identifier
    public static HomeRoom decodeRoom(String line, String structureId) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        return new HomeRoom(id, field(f, 1), structureId);
    }

    /// Decodes a zone record: `id \t name \t roomId,roomId,...`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// #### Returns
    ///
    /// the zone, or `null` when the record has no identifier
    public static HomeZone decodeZone(String line) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        return new HomeZone(id, field(f, 1), list(field(f, 2)));
    }

    /// Decodes an accessory record:
    /// `id \t name \t roomId \t categoryOrdinal \t manufacturer \t model \t
    /// firmware \t reachable \t bridgeAccessoryId`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// - `services`: the services to attach, already decoded
    ///
    /// #### Returns
    ///
    /// the accessory, or `null` when the record has no identifier
    public static Accessory decodeAccessory(String line,
            List<AccessoryService> services) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        String roomId = field(f, 2);
        String bridgeId = field(f, 8);
        return new Accessory(id, field(f, 1),
                roomId.length() == 0 ? null : roomId,
                categoryFor(integer(f, 3, -1)), field(f, 4), field(f, 5),
                field(f, 6), flag(f, 7),
                bridgeId.length() == 0 ? null : bridgeId, services);
    }

    /// Decodes a service record: `id \t name \t typeOrdinal \t primary`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// - `constraints`: the traits to attach, already decoded
    ///
    /// #### Returns
    ///
    /// the service, or `null` when the record has no identifier
    public static AccessoryService decodeService(String line,
            List<TraitConstraint> constraints) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        return new AccessoryService(id, field(f, 1),
                serviceTypeFor(integer(f, 2, -1)), flag(f, 3), constraints);
    }

    /// Decodes a trait record:
    /// `traitId \t readable \t writable \t notifies \t hasRange \t min \t max
    /// \t step \t validOrdinalsCsv`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// #### Returns
    ///
    /// the constraint, or `null` when the trait is unknown to this build --
    /// which is a normal outcome against a newer port, not an error
    public static TraitConstraint decodeTraitConstraint(String line) {
        String[] f = split(line);
        Trait trait = Trait.forId(field(f, 0));
        if (trait == null) {
            return null;
        }
        boolean readable = flag(f, 1);
        boolean writable = flag(f, 2);
        boolean notifies = flag(f, 3);
        if (trait.getValueKind() == TraitValueKind.ENUM) {
            List<String> ordinals = list(field(f, 8));
            if (ordinals.isEmpty()) {
                return TraitConstraint.of(trait, readable, writable, notifies);
            }
            int[] parsed = new int[ordinals.size()];
            int count = 0;
            for (String ordinal : ordinals) {
                try {
                    parsed[count] = Integer.parseInt(ordinal);
                    count++;
                } catch (NumberFormatException skip) {
                    // One unreadable ordinal must not cost the whole trait.
                    // Left out of the list, which widens what the accessory
                    // is believed to accept rather than narrowing it -- the
                    // safe direction, since an empty list already means "did
                    // not say".
                    continue;
                }
            }
            if (count == 0) {
                return TraitConstraint.of(trait, readable, writable, notifies);
            }
            int[] values = new int[count];
            for (int i = 0; i < count; i++) {
                values[i] = parsed[i];
            }
            return TraitConstraint.choices(trait, readable, writable, notifies,
                    values);
        }
        if (flag(f, 4)) {
            return TraitConstraint.ranged(trait, readable, writable, notifies,
                    real(f, 5, 0), real(f, 6, 0), real(f, 7, 0));
        }
        return TraitConstraint.of(trait, readable, writable, notifies);
    }

    /// Decodes a scene record: `id \t name \t typeOrdinal \t executable`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// - `structureId`: the home the scene belongs to
    ///
    /// - `actions`: the actions to attach, already decoded, possibly empty
    ///
    /// #### Returns
    ///
    /// the scene, or `null` when the record has no identifier
    public static Scene decodeScene(String line, String structureId,
            List<SceneAction> actions) {
        String[] f = split(line);
        String id = field(f, 0);
        if (id.length() == 0) {
            return null;
        }
        return new Scene(id, field(f, 1), structureId,
                sceneTypeFor(integer(f, 2, -1)), flag(f, 3), actions);
    }

    /// Decodes a scene-action record:
    /// `accessoryId \t serviceId \t traitId \t kindOrdinal \t numericValue \t
    /// stringValue \t unitWireId`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// #### Returns
    ///
    /// the action, or `null` when it names an unknown trait or is malformed
    public static SceneAction decodeSceneAction(String line) {
        String[] f = split(line);
        String accessoryId = field(f, 0);
        String serviceId = field(f, 1);
        Trait trait = Trait.forId(field(f, 2));
        if (accessoryId.length() == 0 || serviceId.length() == 0
                || trait == null) {
            return null;
        }
        TraitValue value = decodeValue(trait, integer(f, 3, -1),
                real(f, 4, 0), field(f, 5), integer(f, 6, -1), 0, false);
        if (value == null) {
            return null;
        }
        return new SceneAction(accessoryId, serviceId, trait, value);
    }

    /// Decodes a reading record, used for both a read answer and a change
    /// delivery:
    /// `accessoryId \t serviceId \t traitId \t kindOrdinal \t numericValue \t
    /// stringValue \t unitWireId \t rawPlatformValue \t hasValue \t
    /// timestampMillis \t errorName \t errorMessage`.
    ///
    /// #### Parameters
    ///
    /// - `line`: the record
    ///
    /// #### Returns
    ///
    /// the reading, or `null` when it names an unknown trait
    public static TraitReading decodeReading(String line) {
        String[] f = split(line);
        String accessoryId = field(f, 0);
        String serviceId = field(f, 1);
        Trait trait = Trait.forId(field(f, 2));
        if (trait == null) {
            return null;
        }
        String errorName = field(f, 10);
        if (errorName.length() > 0) {
            return TraitReading.failed(accessoryId, serviceId, trait,
                    HomeError.forName(errorName), field(f, 11));
        }
        if (!flag(f, 8)) {
            return TraitReading.absent(accessoryId, serviceId, trait);
        }
        boolean hasRaw = field(f, 7).length() > 0;
        // No fallback for the number, and nothing non-finite either. A
        // record that says it has a value and then does not carry a readable
        // finite one is malformed, and defaulting it to zero is the worst
        // available answer: zero is SECURED for a lock and false for a flag,
        // so a truncated record would read as a locked door rather than as
        // the missing data it is. NaN is no better -- it casts to ordinal
        // zero and compares non-zero -- and no trait carries one: a reading
        // with no value says so with its own flag.
        double numeric = real(f, 4, Double.NaN);
        boolean numericReadable = !Double.isNaN(numeric)
                && !Double.isInfinite(numeric);
        TraitValue value = numericReadable
                ? decodeValue(trait, integer(f, 3, -1), numeric, field(f, 5),
                        integer(f, 6, -1), integer(f, 7, 0), hasRaw)
                : null;
        if (value == null) {
            return TraitReading.failed(accessoryId, serviceId, trait,
                    HomeError.INVALID_DATA,
                    "the port sent a value this build could not read");
        }
        return TraitReading.of(accessoryId, serviceId, trait, value,
                integer64(f, 9, 0));
    }

    /// Rebuilds a value from its wire form.
    ///
    /// The trait decides the kind, not the wire: a port and this build
    /// disagreeing about a trait's kind is a bug worth surfacing as a
    /// malformed value rather than papering over by trusting whichever side
    /// spoke last. `kindOrdinal` is checked against the trait and a mismatch
    /// answers `null`.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait the value belongs to
    ///
    /// - `kindOrdinal`: the kind the port claims
    ///
    /// - `numeric`: the numeric component
    ///
    /// - `text`: the text component
    ///
    /// - `unitWireId`: the unit's wire identifier
    ///
    /// - `rawPlatformValue`: the backend's own ordinal
    ///
    /// - `hasRawPlatformValue`: whether `rawPlatformValue` is meaningful
    ///
    /// #### Returns
    ///
    /// the value, or `null` when the record is malformed
    public static TraitValue decodeValue(Trait trait, int kindOrdinal,
            double numeric, String text, int unitWireId, int rawPlatformValue,
            boolean hasRawPlatformValue) {
        if (trait == null) {
            return null;
        }
        TraitValueKind[] kinds = TraitValueKind.values();
        if (kindOrdinal < 0 || kindOrdinal >= kinds.length) {
            return null;
        }
        if (kinds[kindOrdinal] != trait.getValueKind()) {
            return null;
        }
        TraitValue value;
        switch (trait.getValueKind()) {
            case BOOLEAN:
                value = TraitValue.of(numeric != 0);
                break;
            case INT:
                value = TraitValue.of((int) numeric);
                break;
            case STRING:
                value = TraitValue.of(text);
                break;
            case ENUM:
                value = TraitValue.ofEnumOrdinal((int) numeric);
                break;
            default:
                TraitUnit unit = TraitUnit.forWireId(unitWireId);
                if (unit == null) {
                    unit = trait.getUnit();
                }
                value = TraitValue.of(numeric, unit);
                break;
        }
        if (hasRawPlatformValue) {
            value = value.withRawPlatformValue(rawPlatformValue);
        }
        return value;
    }

    // ------------------------------------------------------------------
    // ordinal lookups, all total
    // ------------------------------------------------------------------

    /// An accessory category by ordinal, total.
    ///
    /// #### Parameters
    ///
    /// - `ordinal`: the ordinal from the wire
    ///
    /// #### Returns
    ///
    /// the category, [AccessoryCategory#OTHER] when out of range
    public static AccessoryCategory categoryFor(int ordinal) {
        AccessoryCategory[] all = AccessoryCategory.values();
        if (ordinal < 0 || ordinal >= all.length) {
            return AccessoryCategory.OTHER;
        }
        return all[ordinal];
    }

    /// A service type by ordinal, total.
    ///
    /// #### Parameters
    ///
    /// - `ordinal`: the ordinal from the wire
    ///
    /// #### Returns
    ///
    /// the type, [ServiceType#OTHER] when out of range
    public static ServiceType serviceTypeFor(int ordinal) {
        ServiceType[] all = ServiceType.values();
        if (ordinal < 0 || ordinal >= all.length) {
            return ServiceType.OTHER;
        }
        return all[ordinal];
    }

    /// A scene type by ordinal, total.
    ///
    /// #### Parameters
    ///
    /// - `ordinal`: the ordinal from the wire
    ///
    /// #### Returns
    ///
    /// the type, [SceneType#USER_DEFINED] when out of range
    public static SceneType sceneTypeFor(int ordinal) {
        SceneType[] all = SceneType.values();
        if (ordinal < 0 || ordinal >= all.length) {
            return SceneType.USER_DEFINED;
        }
        return all[ordinal];
    }

    // ------------------------------------------------------------------
    // encoding, for the local bridge and for tests
    // ------------------------------------------------------------------

    /// Joins fields into one record, inserting the separator between them.
    ///
    /// A `null` field becomes empty. **A field containing a tab or a newline
    /// would corrupt the record**, so both are replaced with a space -- the
    /// only fields that can carry user text are names, and a name with a tab
    /// in it is worth less than a decodable record.
    ///
    /// #### Parameters
    ///
    /// - `fields`: the fields in order
    ///
    /// #### Returns
    ///
    /// the record, never `null`
    public static String join(String[] fields) {
        StringBuilder b = new StringBuilder();
        if (fields == null) {
            return "";
        }
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                b.append(SEPARATOR);
            }
            b.append(sanitize(fields[i]));
        }
        return b.toString();
    }

    /// Makes a field safe to put in a record: `null` becomes empty, and tabs,
    /// carriage returns and newlines become spaces.
    ///
    /// #### Parameters
    ///
    /// - `value`: the field, or `null`
    ///
    /// #### Returns
    ///
    /// the safe field, never `null`
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

    /// The flag form of a boolean: `"1"` or `"0"`.
    ///
    /// #### Parameters
    ///
    /// - `value`: the flag
    ///
    /// #### Returns
    ///
    /// the encoded flag
    public static String flag(boolean value) {
        return value ? "1" : "0";
    }

    /// Encodes a reading, the inverse of [#decodeReading(java.lang.String)].
    ///
    /// #### Parameters
    ///
    /// - `reading`: the reading to encode
    ///
    /// #### Returns
    ///
    /// the record, never `null`
    public static String encodeReading(TraitReading reading) {
        TraitValue v = reading.getValue();
        String[] f = new String[12];
        f[0] = reading.getAccessoryId();
        f[1] = reading.getServiceId();
        f[2] = reading.getTrait().getId();
        f[3] = Integer.toString(reading.getTrait().getValueKind().ordinal());
        f[4] = v == null ? "0" : Double.toString(numericOf(v));
        f[5] = v != null && v.getKind() == TraitValueKind.STRING
                ? v.getString() : "";
        f[6] = v == null ? "" : Integer.toString(v.getUnit().getWireId());
        f[7] = v != null && v.hasRawPlatformValue()
                ? Integer.toString(v.getRawPlatformValue()) : "";
        f[8] = flag(v != null);
        f[9] = Long.toString(reading.getTimestampMillis());
        f[10] = reading.getError() == null ? "" : reading.getError().name();
        f[11] = reading.getErrorMessage() == null ? ""
                : reading.getErrorMessage();
        return join(f);
    }

    /// The numeric component a value carries on the wire.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value
    ///
    /// #### Returns
    ///
    /// the numeric component; zero for a string
    public static double numericOf(TraitValue value) {
        switch (value.getKind()) {
            case BOOLEAN:
                return value.getBoolean() ? 1 : 0;
            case INT:
                return value.getInt();
            case ENUM:
                return value.getEnumOrdinal();
            case STRING:
                return 0;
            default:
                return value.getRawDouble();
        }
    }
}
