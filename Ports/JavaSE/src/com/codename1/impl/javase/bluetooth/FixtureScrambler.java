/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.CharacteristicRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.Device;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.DescriptorRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.RssiSample;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.ServiceRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, structure-preserving anonymization of a
 * {@link BluetoothFixture} before it is committed: real-world traces must
 * never leak device identities into the repository, but the replayed
 * shape (payload lengths, company ids, SIG UUIDs, GATT topology, RSSI
 * timelines) must stay exactly as captured so tests exercise realistic
 * data.
 *
 * <p>{@link #scramble(BluetoothFixture, long)} is a pure function
 * {@code fixture -> fixture}: the input is never mutated and the same
 * seed always produces the identical output. Rules:</p>
 *
 * <ul>
 *   <li>Device ids become synthetic {@code SC:RA:MB:xx:xx:xx} addresses
 *       -- a seeded substitution that is stable per device within the
 *       trace.</li>
 *   <li>Local names become {@code Device-XXXX} ({@code XXXX} from a
 *       seeded hash); the length class is preserved -- short names
 *       (&le; {@value #SHORT_NAME_LIMIT} chars) become the shorter
 *       {@code Dev-XXXX} form.</li>
 *   <li>Manufacturer-data payloads are replaced by seeded-random bytes
 *       of the same length; SIG company ids are kept.</li>
 *   <li>Service-data payloads are likewise length-preserving scrambled;
 *       their UUID keys follow the UUID rule below.</li>
 *   <li>Standard SIG short UUIDs ({@link BluetoothUuid#isShortUuid()})
 *       are kept verbatim; custom 128-bit UUIDs are remapped through a
 *       seeded substitution that is stable within the trace (the same
 *       custom UUID maps to the same replacement wherever it occurs --
 *       advertisement, service data, GATT).</li>
 *   <li>Captured characteristic values are replaced by seeded-random
 *       bytes of the same length.</li>
 *   <li>Timing (RSSI timelines), TX power, connectable flags, GATT
 *       properties/topology and the platform note are kept.</li>
 * </ul>
 */
public final class FixtureScrambler {

    /** Names at most this long keep the short {@code Dev-XXXX} form. */
    public static final int SHORT_NAME_LIMIT = 7;

    private FixtureScrambler() {
    }

    /** Scrambles the fixture (pure function; the input is not mutated). */
    public static BluetoothFixture scramble(BluetoothFixture in, long seed) {
        BluetoothFixture out = new BluetoothFixture()
                .setPlatform(in.getPlatform());
        HashMap<BluetoothUuid, BluetoothUuid> uuidMap =
                new HashMap<BluetoothUuid, BluetoothUuid>();
        HashSet<String> usedIds = new HashSet<String>();
        List<Device> devices = in.getDevices();
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            out.addDevice(scrambleDevice(devices.get(i), seed, uuidMap,
                    usedIds));
        }
        return out;
    }

    private static Device scrambleDevice(Device in, long seed,
            Map<BluetoothUuid, BluetoothUuid> uuidMap,
            HashSet<String> usedIds) {
        Device out = new Device(scrambleId(in.id, seed, usedIds));
        out.name = scrambleName(in.name, seed);
        out.connectable = in.connectable;
        out.txPower = in.txPower;
        int size = in.rssiTimeline.size();
        for (int i = 0; i < size; i++) {
            RssiSample s = in.rssiTimeline.get(i);
            out.rssiTimeline.add(new RssiSample(s.relTimeMs, s.rssi));
        }
        size = in.serviceUuids.size();
        for (int i = 0; i < size; i++) {
            out.serviceUuids.add(scrambleUuid(in.serviceUuids.get(i), seed,
                    uuidMap));
        }
        for (Map.Entry<Integer, byte[]> e : in.manufacturerData.entrySet()) {
            out.manufacturerData.put(e.getKey(), scrambleBytes(seed,
                    in.id + "|mfg|" + e.getKey(), e.getValue()));
        }
        for (Map.Entry<BluetoothUuid, byte[]> e
                : in.serviceData.entrySet()) {
            out.serviceData.put(scrambleUuid(e.getKey(), seed, uuidMap),
                    scrambleBytes(seed, in.id + "|svcdata|" + e.getKey(),
                            e.getValue()));
        }
        size = in.gatt.size();
        for (int i = 0; i < size; i++) {
            out.gatt.add(scrambleService(in.gatt.get(i), in.id, seed,
                    uuidMap));
        }
        return out;
    }

    private static ServiceRecord scrambleService(ServiceRecord in,
            String deviceId, long seed,
            Map<BluetoothUuid, BluetoothUuid> uuidMap) {
        ServiceRecord out = new ServiceRecord(
                scrambleUuid(in.uuid, seed, uuidMap));
        out.primary = in.primary;
        int size = in.characteristics.size();
        for (int i = 0; i < size; i++) {
            CharacteristicRecord c = in.characteristics.get(i);
            CharacteristicRecord sc = new CharacteristicRecord(
                    scrambleUuid(c.uuid, seed, uuidMap), c.properties);
            if (c.value != null) {
                sc.value = scrambleBytes(seed,
                        deviceId + "|value|" + in.uuid + "|" + c.uuid,
                        c.value);
            }
            int ds = c.descriptors.size();
            for (int j = 0; j < ds; j++) {
                sc.descriptors.add(new DescriptorRecord(scrambleUuid(
                        c.descriptors.get(j).uuid, seed, uuidMap)));
            }
            out.characteristics.add(sc);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // the individual scrambling rules
    // ------------------------------------------------------------------

    /**
     * A synthetic {@code SC:RA:MB:xx:xx:xx} address derived from the
     * original id -- stable per device, collision-resolved within the
     * trace by re-hashing.
     */
    static String scrambleId(String id, long seed, HashSet<String> usedIds) {
        for (int salt = 0; ; salt++) {
            long h = mix(seed, id, salt);
            String candidate = String.format("SC:RA:MB:%02X:%02X:%02X",
                    Long.valueOf(h & 0xFF), Long.valueOf((h >>> 8) & 0xFF),
                    Long.valueOf((h >>> 16) & 0xFF));
            if (usedIds.add(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * {@code Device-XXXX} (or {@code Dev-XXXX} for short originals) with
     * four seeded hash characters; {@code null}/empty names pass through.
     */
    static String scrambleName(String name, long seed) {
        if (name == null || name.length() == 0) {
            return name;
        }
        String hash = String.format("%04X",
                Long.valueOf(mix(seed, name, 100) & 0xFFFF));
        return (name.length() <= SHORT_NAME_LIMIT ? "Dev-" : "Device-")
                + hash;
    }

    /**
     * SIG short UUIDs pass through verbatim; custom 128-bit UUIDs map to
     * a seeded replacement, memoized so every occurrence of the same UUID
     * within the trace scrambles identically.
     */
    static BluetoothUuid scrambleUuid(BluetoothUuid uuid, long seed,
            Map<BluetoothUuid, BluetoothUuid> uuidMap) {
        if (uuid == null || uuid.isShortUuid()) {
            return uuid;
        }
        BluetoothUuid mapped = uuidMap.get(uuid);
        if (mapped == null) {
            String key = uuid.toString();
            long msb = mix(seed, key, 200);
            long lsb = mix(seed, key, 201);
            mapped = new BluetoothUuid(msb, lsb);
            if (mapped.isShortUuid()) {
                // astronomically unlikely, but a scrambled custom UUID
                // must never masquerade as a SIG assigned number
                mapped = new BluetoothUuid(msb ^ 1, lsb);
            }
            uuidMap.put(uuid, mapped);
        }
        return mapped;
    }

    /** Seeded-random replacement bytes of the same length. */
    static byte[] scrambleBytes(long seed, String context, byte[] original) {
        if (original == null) {
            return null;
        }
        byte[] out = new byte[original.length];
        long state = mix(seed, context, 300);
        for (int i = 0; i < out.length; i++) {
            state = splitmix(state);
            out[i] = (byte) (state >>> 32);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // verification
    // ------------------------------------------------------------------

    /**
     * The no-PII invariant check: returns every original identifier
     * (device id, local name, custom UUID) that still appears verbatim in
     * the scrambled fixture's JSON. Empty means clean; used by the tests
     * and by {@link FixtureCaptureMain} before a fixture is written.
     */
    public static List<String> findLeaks(BluetoothFixture original,
            BluetoothFixture scrambled) {
        String json = scrambled.toJson();
        String lower = json.toLowerCase();
        ArrayList<String> leaks = new ArrayList<String>();
        List<Device> devices = original.getDevices();
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            Device d = devices.get(i);
            checkLeak(lower, d.id, leaks);
            checkLeak(lower, d.name, leaks);
            int us = d.serviceUuids.size();
            for (int j = 0; j < us; j++) {
                checkUuidLeak(lower, d.serviceUuids.get(j), leaks);
            }
            for (BluetoothUuid u : d.serviceData.keySet()) {
                checkUuidLeak(lower, u, leaks);
            }
            int gs = d.gatt.size();
            for (int j = 0; j < gs; j++) {
                ServiceRecord s = d.gatt.get(j);
                checkUuidLeak(lower, s.uuid, leaks);
                int cs = s.characteristics.size();
                for (int k = 0; k < cs; k++) {
                    checkUuidLeak(lower, s.characteristics.get(k).uuid,
                            leaks);
                }
            }
        }
        return leaks;
    }

    private static void checkLeak(String scrambledJsonLower, String value,
            List<String> leaks) {
        if (value == null || value.length() < 3) {
            return;
        }
        if (scrambledJsonLower.contains(value.toLowerCase())
                && !leaks.contains(value)) {
            leaks.add(value);
        }
    }

    private static void checkUuidLeak(String scrambledJsonLower,
            BluetoothUuid uuid, List<String> leaks) {
        if (uuid == null || uuid.isShortUuid()) {
            return; // SIG UUIDs are kept by design
        }
        checkLeak(scrambledJsonLower, uuid.toString(), leaks);
    }

    // ------------------------------------------------------------------
    // deterministic hashing
    // ------------------------------------------------------------------

    /** A seeded 64-bit string hash (FNV walk + finalizer). */
    private static long mix(long seed, String s, int salt) {
        long h = seed ^ (0x9E3779B97F4A7C15L * (salt + 1));
        int len = s.length();
        for (int i = 0; i < len; i++) {
            h ^= s.charAt(i);
            h *= 0x100000001B3L;
            h ^= h >>> 29;
        }
        return finalize(h);
    }

    /** One step of the splitmix64 sequence. */
    private static long splitmix(long state) {
        return finalize(state + 0x9E3779B97F4A7C15L);
    }

    private static long finalize(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
