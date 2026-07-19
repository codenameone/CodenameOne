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

import com.codename1.bluetooth.BluetoothException;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.Device;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line capture of a scrambled {@link BluetoothFixture} from this
 * machine's real radio -- the tool behind
 * {@code scripts/bluetooth/capture-fixture.sh}.
 *
 * <pre>
 * java com.codename1.impl.javase.bluetooth.FixtureCaptureMain \
 *     --seconds 12 --seed 42 --out ambient-scan.json \
 *     [--gatt &lt;address&gt; ...] [--gatt-strongest] [--library &lt;path&gt;]
 * </pre>
 *
 * <ul>
 *   <li>{@code --seconds N} -- scan duration (default 12)</li>
 *   <li>{@code --seed S} -- {@link FixtureScrambler} seed (default 42)</li>
 *   <li>{@code --out file.json} -- output path (required)</li>
 *   <li>{@code --gatt address} -- also capture the GATT database of the
 *       sighted device with this (real, unscrambled) address; repeatable
 *   </li>
 *   <li>{@code --gatt-strongest} -- attempt a GATT capture on the
 *       connectable device with the strongest sighting</li>
 *   <li>{@code --library path} -- explicit libcn1ble library (sets
 *       {@code cn1.bluetooth.libraryPath})</li>
 * </ul>
 *
 * <p>The written fixture is ALWAYS scrambled, and the scrambler's no-PII
 * invariant ({@link FixtureScrambler#findLeaks}) is verified before the
 * file is written; a leak aborts with exit code 3. Real identities only
 * ever appear in this process's stderr (for the operator). Exit codes:
 * 0 success, 1 bad usage, 2 capture failure, 3 leak detected.</p>
 */
public final class FixtureCaptureMain {

    private FixtureCaptureMain() {
    }

    public static void main(String[] args) {
        long seconds = 12;
        long seed = 42;
        String out = null;
        boolean gattStrongest = false;
        ArrayList<String> gattAddresses = new ArrayList<String>();
        try {
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if ("--seconds".equals(a)) {
                    seconds = Long.parseLong(args[++i]);
                } else if ("--seed".equals(a)) {
                    seed = Long.parseLong(args[++i]);
                } else if ("--out".equals(a)) {
                    out = args[++i];
                } else if ("--gatt".equals(a)) {
                    gattAddresses.add(args[++i]);
                } else if ("--gatt-strongest".equals(a)) {
                    gattStrongest = true;
                } else if ("--library".equals(a)) {
                    System.setProperty(
                            BleLibraryResolver.LIBRARY_PATH_PROPERTY,
                            args[++i]);
                } else {
                    throw new IllegalArgumentException(
                            "Unknown argument: " + a);
                }
            }
            if (out == null) {
                throw new IllegalArgumentException("--out is required");
            }
        } catch (RuntimeException ex) {
            System.err.println("Usage: FixtureCaptureMain --out file.json "
                    + "[--seconds N] [--seed S] [--gatt address ...] "
                    + "[--gatt-strongest] [--library path]");
            System.err.println(ex.getMessage());
            System.exit(1);
            return;
        }

        FixtureRecorder recorder = null;
        try {
            recorder = FixtureRecorder.forNativeBackend();
            System.err.println("Scanning for " + seconds + "s ...");
            BluetoothFixture raw = recorder.record(seconds * 1000,
                    gattAddresses, gattStrongest);
            BluetoothFixture scrambled =
                    FixtureScrambler.scramble(raw, seed);
            List<String> leaks = FixtureScrambler.findLeaks(raw, scrambled);
            if (!leaks.isEmpty()) {
                System.err.println("LEAK: original identifiers survived "
                        + "scrambling: " + leaks + " -- nothing written");
                System.exit(3);
                return;
            }
            writeFile(new File(out), scrambled.toJson());
            printSummary(raw, scrambled, out);
        } catch (BluetoothException ex) {
            System.err.println("Capture failed (" + ex.getError() + "): "
                    + ex.getMessage());
            System.exit(2);
        } catch (IOException ex) {
            System.err.println("Cannot write fixture: " + ex);
            System.exit(2);
        } finally {
            if (recorder != null) {
                recorder.close();
            }
        }
    }

    private static void writeFile(File file, String content)
            throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(file),
                "UTF-8");
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }

    /**
     * Operator summary: scrambled identities on stdout; the real-identity
     * mapping goes to stderr only (never into a file).
     */
    private static void printSummary(BluetoothFixture raw,
            BluetoothFixture scrambled, String out) {
        List<Device> rawDevices = raw.getDevices();
        List<Device> outDevices = scrambled.getDevices();
        System.out.println("Captured " + outDevices.size()
                + " device(s) -> " + out);
        for (int i = 0; i < outDevices.size(); i++) {
            Device d = outDevices.get(i);
            Device r = rawDevices.get(i);
            System.out.println("  " + d.id
                    + (d.name == null ? "" : " \"" + d.name + "\"")
                    + "  sightings=" + d.rssiTimeline.size()
                    + "  advUuids=" + d.serviceUuids.size()
                    + "  mfg=" + d.manufacturerData.size()
                    + "  gatt=" + (d.hasGatt()
                            ? d.gatt.size() + " service(s)" : "no"));
            System.err.println("  [mapping] " + r.id + " ("
                    + (r.name == null ? "?" : r.name) + ") -> " + d.id);
        }
    }
}
