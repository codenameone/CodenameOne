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
package com.codename1.continuity;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.JSONWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Turns an `AppState` into the two forms it has to travel in, and refuses payloads that cannot
/// make the trip.
///
/// The two forms are deliberately different. A *continuation* is handed to the operating system,
/// which stores it as a property list and may deliver it to another device, so it is a nested map
/// of plist-representable values. A *relay* payload crosses a network to a device that may not be
/// an Apple one at all, so it is JSON. Both are lossless for the value types the payload admits,
/// which is the whole reason the payload admits so few.
///
/// This class is public so that a `StateRelay` written by an application can use the same wire
/// format the built-in one does, and so tests can assert on it.
public final class StateCodec {
    private static final String KEY_ROUTES = "routes";
    private static final String KEY_PAYLOAD = "payload";

    /// Says that this document's payload values carry type tags.
    ///
    /// The decision has to be made once for the DOCUMENT, not guessed per value. decode() used to
    /// ask of every string whether it looked tagged, so an untagged payload -- what a
    /// hand-written endpoint or an older build produces, and which this codec deliberately
    /// accepts -- had any ordinary string of the form "i:5" or "s:note" silently reinterpreted:
    /// the first became an Integer, the second lost its prefix. That is application data changed
    /// in transit, and no per-string rule can tell the two apart, because "i:5" is a perfectly
    /// good string.
    ///
    /// A document written by this codec says so. One without the marker is read exactly as it
    /// arrived.
    private static final String KEY_ENCODING = "enc";

    /// The only encoding this codec writes. Absent means untagged.
    private static final String ENCODING_TAGGED = "1";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_TITLE = "title";
    private static final String KEY_SEQUENCE = "seq";
    private static final String KEY_TIMESTAMP = "ts";

    /// The fields this codec writes. A document carrying none of them is not a state, whatever
    /// else it contains.
    private static final String[] KNOWN_KEYS = {
        KEY_ROUTES, KEY_PAYLOAD, KEY_DEVICE, KEY_TITLE, KEY_SEQUENCE, KEY_TIMESTAMP,
    };

    private StateCodec() {
    }

    /// Renders a state as the nested map an operating system can carry between devices.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state, must not be null
    ///
    /// #### Returns
    ///
    /// a map of plist-representable values
    public static Map<String, Object> toMap(AppState state) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(KEY_ROUTES, new ArrayList<String>(state.getRoutes()));
        m.put(KEY_PAYLOAD, encode(state.getPayload()));
        m.put(KEY_ENCODING, ENCODING_TAGGED);
        m.put(KEY_DEVICE, state.getDeviceId());
        if (state.getTitle() != null) {
            m.put(KEY_TITLE, state.getTitle());
        }
        // Written as strings rather than as numbers. Both survive a property list, but a JSON
        // round trip through JSONParser reads every number back as a Double, and a millisecond
        // timestamp is past the range a double represents exactly -- so the same two fields would
        // come back changed on the relay path and unchanged on the continuation path. One
        // encoding for both keeps a state comparable with itself however it arrived.
        m.put(KEY_SEQUENCE, Long.toString(state.getSequence()));
        m.put(KEY_TIMESTAMP, Long.toString(state.getTimestamp()));
        return m;
    }

    /// Rebuilds a state from the map form. Unknown keys are ignored, so a newer build of the app
    /// on another device can add fields without breaking this one.
    ///
    /// #### Parameters
    ///
    /// - `m`: the map, or null
    ///
    /// #### Returns
    ///
    /// the state, or null when the map is null or carries nothing recognizable
    public static AppState fromMap(Map<String, Object> m) {
        if (m == null) {
            return null;
        }
        // Something recognizable has to be in there. An empty object, or an unrelated one, used
        // to come back as a default AppState -- which the continuation callback then CLAIMED and
        // delivered, so a relay answering "{}" ran the application's listeners and could put a
        // "continue what you were doing?" prompt in front of the user over nothing at all.
        boolean recognized = false;
        for (String known : KNOWN_KEYS) {
            if (m.containsKey(known)) {
                recognized = true;
                break;
            }
        }
        if (!recognized) {
            return null;
        }
        AppState state = new AppState();
        Object routes = m.get(KEY_ROUTES);
        if (routes instanceof List) {
            List<String> paths = new ArrayList<String>();
            for (Object path : (List<?>) routes) {
                if (path instanceof String) {
                    paths.add((String) path);
                }
            }
            // Unchecked, exactly as the payload below is: this document came from another
            // device, and one route past this device's stored-string limit used to throw out of
            // fromJson -- which the relay reads as a failed fetch, and the document never
            // changes, so this device stopped publishing for good.
            state.setRoutesUnchecked(paths);
        }
        // Whether the values are tagged is the DOCUMENT's answer, not a guess made per string.
        // See KEY_ENCODING: without it, "i:5" is just a string and stays one.
        Object encoding = m.get(KEY_ENCODING);
        boolean tagged = encoding instanceof String && ENCODING_TAGGED.equals(encoding);
        Object payload = m.get(KEY_PAYLOAD);
        if (payload instanceof Map) {
            Map<String, Object> copy = new HashMap<String, Object>();
            Map<?, ?> read = (Map<?, ?>) payload;
            for (Map.Entry<?, ?> entry : read.entrySet()) {
                if (entry.getKey() instanceof String) {
                    copy.put((String) entry.getKey(),
                            tagged ? decode(entry.getValue()) : entry.getValue());
                }
            }
            // Not validated on the way in. This map came from another device, and refusing it
            // would turn that device's mistake into an exception on this one at a moment the user
            // cannot connect to anything they did.
            state.setPayloadUnchecked(copy);
        }
        Object device = m.get(KEY_DEVICE);
        if (device instanceof String) {
            state.setDeviceIdUnchecked((String) device);
        }
        Object title = m.get(KEY_TITLE);
        if (title instanceof String) {
            state.setTitleUnchecked((String) title);
        }
        state.setSequence(asLong(m.get(KEY_SEQUENCE)));
        state.setTimestamp(asLong(m.get(KEY_TIMESTAMP)));
        return state;
    }

    /// Renders a state as JSON, for a relay.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state, must not be null
    ///
    /// #### Returns
    ///
    /// the JSON document
    public static String toJson(AppState state) {
        return JSONWriter.toJson(toMap(state));
    }

    /// Parses the JSON form.
    ///
    /// #### Parameters
    ///
    /// - `json`: the document, or null
    ///
    /// #### Returns
    ///
    /// the state, or null when the document is null, empty or not an object
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: when the document is malformed
    public static AppState fromJson(String json) throws IOException {
        if (json == null || json.trim().length() == 0) {
            return null;
        }
        if (!isValidJsonObject(json)) {
            // JSONParser does NOT throw on a malformed document: it logs the failure, closes the
            // reader, and returns whatever partial map it had built. So a truncated relay response
            // came back as a valid-looking state, and the shape it takes is the worst one -- a
            // document cut off after "device" and "seq" has no routes and no payload, which is an
            // EMPTY state, which this framework reads as a tombstone. The origin is then recorded
            // as having cleared its work, durably, and fetch() reports a SUCCESSFUL read, which
            // releases a queued POST over the relay's real document.
            //
            // So the check is here rather than left to the parser. Truncation is the corruption
            // that actually happens on a network, and it is exactly what a structural scan
            // catches -- and so is a bad token, which a structural scan alone let through:
            // {"device":"d","seq":"2","payload":tru} is balanced, quoted, and invalid.
            throw new IOException("The continuity relay returned a document that is not a "
                    + "valid JSON object. Treated as a failed read rather than as an empty "
                    + "relay, because a truncated document is indistinguishable from one that "
                    + "says the other device has nothing.");
        }
        // Parsed with NULLS KEPT. The convenience parser drops a null-valued field before
        // anything can look at it, so {"payload":null} reached the checks below as an ABSENT
        // payload -- and absent routes plus an absent payload is an empty state, which this
        // framework reads as a tombstone. A field that is present and null has to stay
        // distinguishable from one that was never sent.
        JSONParser parser = new JSONParser();
        parser.setIncludeNullsInstance(true);
        Map<String, Object> parsed = parser.parseJSON(new java.io.StringReader(json));
        requireKnownTypes(parsed);
        return fromMap(parsed);
    }

    /// Refuses a document whose known fields carry the wrong kind of value.
    ///
    /// Valid syntax is not a valid STATE. `{"device":"other","seq":"10","payload":[]}` parses
    /// cleanly, and fromMap then ignores the array where a payload belongs -- leaving routes and
    /// payload both empty, which is an EMPTY state, which the framework reads as a tombstone. So
    /// a relay serving one wrong type has the origin recorded as having cleared its work, marked
    /// durably, and a queued publish released over the server's document. The same shape with
    /// valid routes restores and acknowledges a state whose payload was silently dropped.
    ///
    /// Only fields that are PRESENT are checked, and only ones this codec knows. An absent field
    /// is an older or smaller document, which is legitimate; an unknown field belongs to a
    /// sender that knows something this build does not, and ignoring it is how the format stays
    /// extensible. What is refused is a known field that cannot mean what it says.
    private static void requireKnownTypes(Map<String, Object> m) throws IOException {
        if (m == null) {
            return;
        }
        requireType(m, KEY_ROUTES, List.class, "an array of route strings");
        requireRouteStrings(m);
        requireType(m, KEY_PAYLOAD, Map.class, "an object");
        requireType(m, KEY_ENCODING, String.class, "a string");
        requireType(m, KEY_DEVICE, String.class, "a string");
        requireType(m, KEY_TITLE, String.class, "a string");
        requireNumberLike(m, KEY_SEQUENCE);
        requireNumberLike(m, KEY_TIMESTAMP);
    }

    /// Every ELEMENT of the route array, not just the array.
    ///
    /// Checking the container alone left {"routes":[1]} passing: the list is a list, the loop
    /// that reads it drops the element it cannot use, and what comes out has no routes and no
    /// payload -- an empty state, which this framework reads as a tombstone. The same door as a
    /// wrong payload type and a null field, one level further in.
    private static void requireRouteStrings(Map<String, Object> m) throws IOException {
        Object routes = m.get(KEY_ROUTES);
        if (!(routes instanceof List)) {
            return;
        }
        List<?> list = (List<?>) routes;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof String) {
                continue;
            }
            throw new IOException("The continuity relay returned a document whose route at index "
                    + i + " is not a string. Dropping it would leave a state with fewer routes "
                    + "than the sender meant, and dropping the only one would make it an empty "
                    + "state -- which means the sending device cleared its work.");
        }
    }

    private static void requireType(Map<String, Object> m, String key, Class<?> type, String what)
            throws IOException {
        if (!m.containsKey(key)) {
            return;
        }
        Object value = m.get(key);
        if (value == null) {
            throw new IOException("The continuity relay returned a document whose \"" + key
                    + "\" is null. A sender that means \"absent\" leaves the key out; a key that "
                    + "is present and empty is a document this codec cannot read, and reading it "
                    + "as absent would make it an empty state -- which means the sending device "
                    + "cleared its work.");
        }
        if (type.isInstance(value)) {
            return;
        }
        throw new IOException("The continuity relay returned a document whose \"" + key
                + "\" is not " + what + ". Treated as a failed read rather than as a state, "
                + "because a field this codec cannot use is indistinguishable from one that is "
                + "absent -- and an absent payload and routes make an empty state, which means "
                + "the sending device cleared its work.");
    }

    /// seq and ts, which this codec writes as strings and older senders may write as numbers.
    private static void requireNumberLike(Map<String, Object> m, String key) throws IOException {
        if (!m.containsKey(key)) {
            return;
        }
        Object value = m.get(key);
        if (value instanceof Number) {
            // NOT just "a number". JSONParser answers a bare 1e100 with a Double, and asLong()
            // then converts it to Long.MAX_VALUE -- so one such document raises this origin's
            // durable high-water mark to the largest value there is, and every ordinary sequence
            // it sends afterwards is refused as already seen, for the life of the installation.
            // A fractional value is refused for the same reason in miniature: 5.7 becomes 5, and
            // the sender's 5 is then indistinguishable from it.
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)
                    || d != Math.floor(d)
                    || d < (double) Long.MIN_VALUE || d > (double) Long.MAX_VALUE) {
                throw new IOException("The continuity relay returned a document whose \"" + key
                        + "\" is " + value + ", which is not a whole number this device can "
                        + "hold. Accepting it would clamp the value to the largest sequence "
                        + "there is and refuse every later state from that device as already "
                        + "seen.");
            }
            return;
        }
        if (value instanceof String) {
            try {
                Long.parseLong(((String) value).trim());
                return;
            } catch (NumberFormatException err) {
                // Falls through to the refusal below: a string that is not a number cannot be a
                // sequence, and asLong() would silently answer 0 -- which is a valid-looking
                // sequence that every later state supersedes.
                Log.e(err);
            }
        }
        throw new IOException("The continuity relay returned a document whose \"" + key
                + "\" is not a number. Read as zero it would be a sequence every later state "
                + "supersedes, so it is refused instead.");
    }

    /// Whether `json` is ONE syntactically valid JSON object and nothing else.
    ///
    /// A real grammar check, because a structural one was not enough. Counting braces and closing
    /// strings catches a document cut in half, and lets
    /// `{"device":"d","seq":"2","payload":tru}` through -- balanced, quoted, and invalid. The
    /// parser then logs the bad token and returns the map it had built up to that point, which is
    /// a partial state with the same consequences as a truncated one.
    ///
    /// Nothing is built here and no value is interpreted: this answers only "is the whole of this
    /// document well formed", so it cannot disagree with the parser about what anything MEANS.
    /// The alternative was asking the parser, and it has no way to say -- its exception handler
    /// logs, closes the reader, and returns the partial result.
    static boolean isValidJsonObject(String json) {
        String t = json.trim();
        if (t.length() < 2 || t.charAt(0) != '{') {
            return false;
        }
        int[] at = new int[1];
        if (!scanValue(t, at)) {
            return false;
        }
        skipWhitespace(t, at);
        // Trailing content is not a second document, it is a broken one.
        return at[0] == t.length();
    }

    private static void skipWhitespace(String s, int[] at) {
        while (at[0] < s.length()) {
            char c = s.charAt(at[0]);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            at[0]++;
        }
    }

    private static boolean scanValue(String s, int[] at) {
        skipWhitespace(s, at);
        if (at[0] >= s.length()) {
            return false;
        }
        char c = s.charAt(at[0]);
        if (c == '{') {
            return scanObject(s, at);
        }
        if (c == '[') {
            return scanArray(s, at);
        }
        if (c == '"') {
            return scanString(s, at);
        }
        if (c == 't') {
            return scanLiteral(s, at, "true");
        }
        if (c == 'f') {
            return scanLiteral(s, at, "false");
        }
        if (c == 'n') {
            return scanLiteral(s, at, "null");
        }
        return scanNumber(s, at);
    }

    private static boolean scanObject(String s, int[] at) {
        at[0]++;
        skipWhitespace(s, at);
        if (at[0] < s.length() && s.charAt(at[0]) == '}') {
            at[0]++;
            return true;
        }
        for (;;) {
            skipWhitespace(s, at);
            if (at[0] >= s.length() || s.charAt(at[0]) != '"' || !scanString(s, at)) {
                return false;
            }
            skipWhitespace(s, at);
            if (at[0] >= s.length() || s.charAt(at[0]) != ':') {
                return false;
            }
            at[0]++;
            if (!scanValue(s, at)) {
                return false;
            }
            skipWhitespace(s, at);
            if (at[0] >= s.length()) {
                return false;
            }
            char c = s.charAt(at[0]);
            at[0]++;
            if (c == '}') {
                return true;
            }
            if (c != ',') {
                return false;
            }
        }
    }

    private static boolean scanArray(String s, int[] at) {
        at[0]++;
        skipWhitespace(s, at);
        if (at[0] < s.length() && s.charAt(at[0]) == ']') {
            at[0]++;
            return true;
        }
        for (;;) {
            if (!scanValue(s, at)) {
                return false;
            }
            skipWhitespace(s, at);
            if (at[0] >= s.length()) {
                return false;
            }
            char c = s.charAt(at[0]);
            at[0]++;
            if (c == ']') {
                return true;
            }
            if (c != ',') {
                return false;
            }
        }
    }

    private static boolean scanString(String s, int[] at) {
        at[0]++;
        while (at[0] < s.length()) {
            char c = s.charAt(at[0]);
            at[0]++;
            if (c == '"') {
                return true;
            }
            if (c == '\\') {
                if (at[0] >= s.length()) {
                    return false;
                }
                char e = s.charAt(at[0]);
                at[0]++;
                if (e == 'u') {
                    if (at[0] + 4 > s.length()) {
                        return false;
                    }
                    for (int i = 0; i < 4; i++) {
                        if (hexValue(s.charAt(at[0] + i)) < 0) {
                            return false;
                        }
                    }
                    at[0] += 4;
                } else if ("\"\\/bfnrt".indexOf(e) < 0) {
                    return false;
                }
            }
        }
        return false;
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static boolean scanLiteral(String s, int[] at, String literal) {
        if (!s.startsWith(literal, at[0])) {
            return false;
        }
        at[0] += literal.length();
        return true;
    }

    private static boolean scanNumber(String s, int[] at) {
        int start = at[0];
        if (at[0] < s.length() && s.charAt(at[0]) == '-') {
            at[0]++;
        }
        // JSON's integer rule exactly: a single 0, or a non-zero digit and any digits after it.
        // A permissive loop accepted "01", which is not JSON -- and being laxer than the grammar
        // is the whole failure this validator exists to correct, so it does not get to make its
        // own small version of it.
        if (at[0] >= s.length()) {
            return false;
        }
        char first = s.charAt(at[0]);
        if (first == '0') {
            at[0]++;
        } else if (first >= '1' && first <= '9') {
            while (at[0] < s.length() && s.charAt(at[0]) >= '0' && s.charAt(at[0]) <= '9') {
                at[0]++;
            }
        } else {
            return false;
        }
        if (at[0] < s.length() && s.charAt(at[0]) == '.') {
            at[0]++;
            int frac = 0;
            while (at[0] < s.length() && s.charAt(at[0]) >= '0' && s.charAt(at[0]) <= '9') {
                at[0]++;
                frac++;
            }
            if (frac == 0) {
                return false;
            }
        }
        if (at[0] < s.length() && (s.charAt(at[0]) == 'e' || s.charAt(at[0]) == 'E')) {
            at[0]++;
            if (at[0] < s.length() && (s.charAt(at[0]) == '+' || s.charAt(at[0]) == '-')) {
                at[0]++;
            }
            int exp = 0;
            while (at[0] < s.length() && s.charAt(at[0]) >= '0' && s.charAt(at[0]) <= '9') {
                at[0]++;
                exp++;
            }
            if (exp == 0) {
                return false;
            }
        }
        return at[0] > start;
    }

    /// Throws when any value in the map could not survive being written to a property list, sent
    /// as JSON and read back by another build of the app on another device.
    ///
    /// The admitted types are `String`, `Integer`, `Long`, `Double`, `Boolean`, and `List` and
    /// `Map` of those. `Map` keys must be strings, because neither destination format has any
    /// other kind of key.
    ///
    /// #### Parameters
    ///
    /// - `payload`: the payload, or null
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: naming the path to the first offending value
    public static void requireRepresentable(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("A continuity payload cannot have a null key.");
            }
            // Keys go through the same writeUTF as values, so an oversized one loses the
            // checkpoint just as quietly.
            requireWritable(entry.getKey(), entry.getKey());
            check(entry.getValue(), entry.getKey(), 0);
        }
    }

    /// The number of characters the rendered JSON form occupies, which is the closest portable
    /// stand-in for what a payload costs on any of the transports.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state
    ///
    /// #### Returns
    ///
    /// the encoded size in characters
    public static int encodedSize(AppState state) {
        return toJson(state).length();
    }

    /// Renders a payload value so its Java type survives every transport.
    ///
    /// Neither destination format preserves the types this payload admits. `JSONParser` reads
    /// every JSON number back as a `Double` -- so an `Integer` returns as `3.0`, and a `Long`
    /// past 2^53 comes back a different number -- and it reads `true` back as the *string*
    /// `"true"`. A property list is kinder but not identical. The result was an application
    /// casting a value to the type it stored and getting a `ClassCastException` on Android and
    /// the desktop, and on iOS something worse: ParparVM does not throw for a failed cast, so
    /// the wrong object is handed to the next instruction.
    ///
    /// So every scalar crosses as a tagged string and is put back together on arrival. Strings
    /// are tagged too, which is what stops an application's own `"i:5"` from being read as an
    /// integer. Lists and maps stay themselves -- both formats carry those natively -- and their
    /// contents are encoded element by element.
    private static Object encodeValue(Object value) {
        if (value instanceof String) {
            return "s:" + value;
        }
        if (value instanceof Integer) {
            return "i:" + value;
        }
        if (value instanceof Long) {
            return "l:" + value;
        }
        if (value instanceof Double) {
            return "d:" + value;
        }
        if (value instanceof Boolean) {
            return "b:" + value;
        }
        if (value instanceof List) {
            List<?> in = (List<?>) value;
            List<Object> out = new ArrayList<Object>();
            for (Object element : in) {
                out.add(encodeValue(element));
            }
            return out;
        }
        if (value instanceof Map) {
            return encode(castToStringKeyed((Map<?, ?>) value));
        }
        // Unreachable for a payload that went through requireRepresentable, which is every
        // payload this framework produces. A hand-built map handed straight to toMap reaches
        // here, and its own toString is a better answer than dropping the entry.
        return "s:" + String.valueOf(value);
    }

    private static Map<String, Object> encode(Map<String, Object> payload) {
        Map<String, Object> out = new HashMap<String, Object>();
        if (payload == null) {
            return out;
        }
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            out.put(e.getKey(), encodeValue(e.getValue()));
        }
        return out;
    }

    /// Rebuilds a value `encodeValue` wrote.
    ///
    /// Only ever called for a document that DECLARED its values tagged, through KEY_ENCODING. A
    /// document without that marker -- a hand-written endpoint, or a build older than the
    /// tagging -- is passed through untouched by the caller, because there is no way to tell an
    /// encoded "i:5" from a string whose value happens to be "i:5", and guessing corrupts the
    /// second to rescue the first.
    ///
    /// A value that is untagged INSIDE a tagged document is still passed through: nested
    /// containers are walked, and anything unrecognized is more useful untyped than discarded.
    private static Object decode(Object value) {
        if (value instanceof List) {
            List<?> in = (List<?>) value;
            List<Object> out = new ArrayList<Object>();
            for (Object element : in) {
                out.add(decode(element));
            }
            return out;
        }
        if (value instanceof Map) {
            Map<?, ?> in = (Map<?, ?>) value;
            Map<String, Object> out = new HashMap<String, Object>();
            for (Map.Entry<?, ?> e : in.entrySet()) {
                if (e.getKey() instanceof String) {
                    out.put((String) e.getKey(), decode(e.getValue()));
                }
            }
            return out;
        }
        if (!(value instanceof String)) {
            return value;
        }
        String text = (String) value;
        if (text.length() < 2 || text.charAt(1) != ':') {
            return text;
        }
        String body = text.substring(2);
        char tag = text.charAt(0);
        try {
            if (tag == 's') {
                return body;
            }
            // parseX rather than valueOf(String). Core is compiled a second time against
            // Ports/CLDC11 and translated against vm/JavaAPI, and neither carries the
            // String-taking valueOf overloads -- only valueOf(primitive). The Maven build accepts
            // them against the full JDK, so the mistake only appears in the Ant leg.
            if (tag == 'i') {
                return Integer.valueOf(Integer.parseInt(body));
            }
            if (tag == 'l') {
                return Long.valueOf(Long.parseLong(body));
            }
            if (tag == 'd') {
                return Double.valueOf(Double.parseDouble(body));
            }
            if (tag == 'b') {
                return Boolean.valueOf(body);
            }
        } catch (NumberFormatException malformed) {
            // A tag whose body will not parse came from somewhere this build does not control.
            // The text is the honest answer; throwing would lose the whole state over one key.
            return text;
        }
        return text;
    }

    private static Map<String, Object> castToStringKeyed(Map<?, ?> in) {
        Map<String, Object> out = new HashMap<String, Object>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            if (e.getKey() instanceof String) {
                out.put((String) e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /// The most modified-UTF-8 bytes a single string in a payload may occupy.
    ///
    /// `Util.writeObject` writes every String with `DataOutputStream.writeUTF`, which cannot
    /// encode more than this and throws when asked to. `Continuity.persist()` logs that failure
    /// and carries on, so an oversized payload produced a checkpoint that LOOKED successful and
    /// simply was not there after the process died -- the one thing state restoration exists to
    /// prevent, arriving with nothing said. Refused here instead, naming the key, which is the
    /// same contract an unrepresentable type already gets.
    static final int MAX_STRING_BYTES = 65535;

    /// A map key trimmed to something a message can carry.
    ///
    /// The key itself may be the oversized thing being reported, and reproducing all of it in the
    /// exception would bury the sentence that names the problem.
    private static String keyLabel(String key) {
        return key.length() <= 64 ? key
                : key.substring(0, 64) + "...(" + key.length() + " chars)";
    }

    /// Refuses a string the local checkpoint could not store.
    static void requireWritable(String value, String path) {
        if (exceedsWritableLength(value)) {
            throw new IllegalArgumentException("The continuity payload at \"" + path + "\" is "
                    + "longer than " + MAX_STRING_BYTES + " bytes of modified UTF-8, which is the "
                    + "most a stored checkpoint can hold. Keep the payload small -- it is a "
                    + "pointer to where the user was, not the document they were working on -- "
                    + "and load the rest from your own storage when the state is restored.");
        }
    }

    /// Whether `s` encodes to more than MAX_STRING_BYTES.
    ///
    /// Counted rather than approximated from `length()`, because the limit is on BYTES and a
    /// string of accented or CJK characters reaches it at a third of the character count. Stops
    /// at the limit, so a huge string costs the limit rather than its own length, and the running
    /// total cannot overflow.
    static boolean exceedsWritableLength(String s) {
        return writableLength(s) > MAX_STRING_BYTES;
    }

    /// The number of bytes `s` occupies in the modified UTF-8 a stored string is written as.
    ///
    /// Counted rather than approximated from `length()`, because the limit is on BYTES and a
    /// string of accented or CJK characters reaches it at a third of the character count.
    ///
    /// Stops counting once past the limit, so a huge string costs the limit rather than its own
    /// length and the running total cannot overflow. Callers may therefore read the answer as
    /// "this many bytes, or more than the limit" -- which is all either of them needs, one to
    /// refuse the string and the other to budget for it.
    static int writableLength(String s) {
        int len = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                len++;
            } else if (c > 0x07FF) {
                len += 3;
            } else {
                len += 2;
            }
            if (len > MAX_STRING_BYTES) {
                return len;
            }
        }
        return len;
    }

    private static void check(Object value, String path, int depth) {
        if (depth > 16) {
            // A payload cannot legitimately be this deep, and a cycle looks exactly like a very
            // deep tree until the stack runs out. Refused with the path so the shape is findable.
            throw new IllegalArgumentException("The continuity payload at \"" + path
                    + "\" nests more than 16 levels deep, or contains a cycle. Neither a property "
                    + "list nor JSON can represent a cycle.");
        }
        if (value instanceof String) {
            requireWritable((String) value, path);
            return;
        }
        if (value instanceof Integer
                || value instanceof Long || value instanceof Double || value instanceof Boolean) {
            return;
        }
        if (value == null) {
            // Refused rather than carried. A property list has no null: the iOS sanitizer drops a
            // null-valued entry and drops a null LIST ELEMENT, which shifts every index after it,
            // so the payload that arrives on the other device is a different shape from the one
            // that was sent. Saying so here, where the key is known, beats a list that is quietly
            // one shorter on an iPad.
            throw new IllegalArgumentException("The continuity payload at \"" + path + "\" is "
                    + "null. A property list cannot carry one, and dropping it would change the "
                    + "shape of what arrives on another device -- a null list element would shift "
                    + "every index after it. Leave the key out instead.");
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            int index = 0;
            for (Object element : list) {
                check(element, path + "[" + index + "]", depth + 1);
                index++;
            }
            return;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("The continuity payload at \"" + path
                            + "\" has a map key of type "
                            + (key == null ? "null" : key.getClass().getName())
                            + ". Only string keys can be written to a property list or to JSON.");
                }
                // Nested keys reach Util.writeObject's writeUTF exactly as top-level ones do.
                // Validating only the top level left a deep key able to throw inside
                // externalize(), which Continuity.persist() logs and carries on from -- so the
                // checkpoint went out to the other device and was silently absent from local
                // storage, the failure this validation exists to prevent.
                requireWritable((String) key, path + "." + keyLabel((String) key));
                check(entry.getValue(), path + "." + keyLabel((String) key), depth + 1);
            }
            return;
        }
        throw new IllegalArgumentException("The continuity payload at \"" + path + "\" is a "
                + value.getClass().getName() + ". A continuity payload has to survive being "
                + "written to a property list and delivered to another device, possibly running a "
                + "different build of this app, so it admits only String, Integer, Long, Double, "
                + "Boolean, and List and Map of those. Convert this value before adding it.");
    }

    private static long asLong(Object o) {
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (NumberFormatException err) {
                return 0;
            }
        }
        // Never a cast: on ParparVM a failed CHECKCAST does not throw, so the guarded instanceof
        // is the only portable way to ask. A relay written before the string encoding, or a
        // hand-written server, can still send a number here.
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }
}
