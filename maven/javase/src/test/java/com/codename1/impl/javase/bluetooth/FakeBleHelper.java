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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A scripted stand-in for the Rust {@code cn1-ble-helper} binary, launched
 * as a real subprocess by {@link NativeBleBackendFakeHelperTest} so the
 * {@link NativeBleBackend} reader/writer/crash paths are exercised
 * deterministically without Bluetooth hardware.
 *
 * <p>Speaks the wire protocol from
 * {@code Ports/JavaSE/native/cn1-ble-helper/PROTOCOL.md}. Runs in a bare
 * JVM with only the test-classes directory on the classpath, so it must
 * not reference JUnit or Codename One classes; commands are picked apart
 * with regexes, which is fine because {@code NativeBleBackend.Wire}
 * serializes them in a stable single-line form.</p>
 *
 * <p>Scenario (args[0]):</p>
 * <ul>
 *   <li>{@code happy} - answers every command successfully</li>
 *   <li>{@code crash-on-connect} - exits with status 1 when a connect
 *       command arrives (helper-crash-mid-flight simulation)</li>
 *   <li>{@code hang-on-connect} - never answers connect (shutdown-with-
 *       in-flight-op simulation)</li>
 *   <li>{@code rssi-unsupported} - readRssi answers a typed notSupported
 *       error</li>
 * </ul>
 */
public final class FakeBleHelper {

    static final String HR_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    static final String HR_MEASUREMENT =
            "00002a37-0000-1000-8000-00805f9b34fb";
    static final String HR_CONTROL = "00002a39-0000-1000-8000-00805f9b34fb";
    static final String CCCD = "00002902-0000-1000-8000-00805f9b34fb";

    private static final Pattern CMD = Pattern.compile("\"cmd\":\"(\\w+)\"");
    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern ADDRESS =
            Pattern.compile("\"address\":\"([^\"]*)\"");
    private static final Pattern CHARACTERISTIC =
            Pattern.compile("\"characteristic\":\"([^\"]*)\"");

    private FakeBleHelper() {
    }

    private static String group(Pattern p, String line) {
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1) : "";
    }

    public static void main(String[] args) throws Exception {
        String scenario = args.length > 0 ? args[0] : "happy";
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        out.println("{\"event\":\"capabilities\",\"version\":1,"
                + "\"helperVersion\":\"fake\",\"platform\":\"fake\","
                + "\"descriptors\":true,\"rssi\":\"lastSeen\","
                + "\"bonding\":false}");
        out.println("{\"event\":\"stateChanged\",\"state\":\"poweredOn\"}");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            String cmd = group(CMD, line);
            String id = group(ID, line);
            String address = group(ADDRESS, line);
            if ("shutdown".equals(cmd)) {
                System.exit(0);
            } else if ("scanStart".equals(cmd)) {
                out.println("{\"event\":\"scanStarted\",\"requestId\":"
                        + id + "}");
                out.println("{\"event\":\"scanResult\","
                        + "\"address\":\"aa:01\","
                        + "\"name\":\"Heart Monitor\",\"rssi\":-42,"
                        + "\"txPower\":4,"
                        + "\"serviceUuids\":[\"" + HR_SERVICE + "\"],"
                        + "\"manufacturerData\":{\"76\":\"AQI=\"},"
                        + "\"serviceData\":{}}");
                out.println("{\"event\":\"scanResult\","
                        + "\"address\":\"aa:02\","
                        + "\"name\":\"Thermometer\",\"rssi\":-77,"
                        + "\"serviceUuids\":[],"
                        + "\"manufacturerData\":{},\"serviceData\":{}}");
            } else if ("scanStop".equals(cmd)) {
                out.println("{\"event\":\"scanStopped\",\"requestId\":"
                        + id + "}");
            } else if ("connect".equals(cmd)) {
                if ("crash-on-connect".equals(scenario)) {
                    System.exit(1);
                }
                if ("hang-on-connect".equals(scenario)) {
                    continue;
                }
                out.println("{\"event\":\"connected\",\"requestId\":" + id
                        + ",\"address\":\"" + address
                        + "\",\"name\":\"Heart Monitor\"}");
            } else if ("disconnect".equals(cmd)) {
                out.println("{\"event\":\"disconnected\",\"requestId\":"
                        + id + ",\"address\":\"" + address + "\"}");
            } else if ("discover".equals(cmd)) {
                out.println("{\"event\":\"discovered\",\"requestId\":" + id
                        + ",\"address\":\"" + address
                        + "\",\"name\":\"Heart Monitor\",\"services\":["
                        + "{\"uuid\":\"" + HR_SERVICE
                        + "\",\"primary\":true,\"characteristics\":["
                        + "{\"uuid\":\"" + HR_MEASUREMENT
                        + "\",\"properties\":[\"read\",\"notify\"],"
                        + "\"descriptors\":[{\"uuid\":\"" + CCCD + "\"}]},"
                        + "{\"uuid\":\"" + HR_CONTROL
                        + "\",\"properties\":[\"write\"],"
                        + "\"descriptors\":[]}]}]}");
            } else if ("read".equals(cmd)) {
                out.println("{\"event\":\"readResult\",\"requestId\":" + id
                        + ",\"address\":\"" + address + "\",\"service\":\""
                        + HR_SERVICE + "\",\"characteristic\":\""
                        + group(CHARACTERISTIC, line)
                        + "\",\"value\":\"AQI=\"}");
            } else if ("write".equals(cmd)) {
                out.println("{\"event\":\"writeResult\",\"requestId\":" + id
                        + ",\"address\":\"" + address + "\",\"service\":\""
                        + HR_SERVICE + "\",\"characteristic\":\""
                        + group(CHARACTERISTIC, line) + "\"}");
            } else if ("subscribe".equals(cmd)) {
                out.println("{\"event\":\"subscribed\",\"requestId\":" + id
                        + ",\"address\":\"" + address + "\",\"service\":\""
                        + HR_SERVICE + "\",\"characteristic\":\""
                        + group(CHARACTERISTIC, line) + "\"}");
                // unsolicited notification for a characteristic nobody
                // listens to: exercises the routing path headlessly (the
                // core early-returns before any EDT dispatch)
                out.println("{\"event\":\"notification\",\"address\":\""
                        + address + "\",\"service\":\"" + HR_SERVICE
                        + "\",\"characteristic\":\"" + HR_CONTROL
                        + "\",\"value\":\"kg==\"}");
            } else if ("unsubscribe".equals(cmd)) {
                out.println("{\"event\":\"unsubscribed\",\"requestId\":" + id
                        + ",\"address\":\"" + address + "\",\"service\":\""
                        + HR_SERVICE + "\",\"characteristic\":\""
                        + group(CHARACTERISTIC, line) + "\"}");
            } else if ("readDescriptor".equals(cmd)) {
                out.println("{\"event\":\"descriptorReadResult\","
                        + "\"requestId\":" + id + ",\"address\":\"" + address
                        + "\",\"service\":\"" + HR_SERVICE
                        + "\",\"characteristic\":\""
                        + group(CHARACTERISTIC, line)
                        + "\",\"descriptor\":\"" + CCCD
                        + "\",\"value\":\"kg==\"}");
            } else if ("writeDescriptor".equals(cmd)) {
                out.println("{\"event\":\"descriptorWriteResult\","
                        + "\"requestId\":" + id + ",\"address\":\"" + address
                        + "\",\"service\":\"" + HR_SERVICE
                        + "\",\"characteristic\":\""
                        + group(CHARACTERISTIC, line)
                        + "\",\"descriptor\":\"" + CCCD + "\"}");
            } else if ("readRssi".equals(cmd)) {
                if ("rssi-unsupported".equals(scenario)) {
                    out.println("{\"event\":\"error\",\"requestId\":" + id
                            + ",\"command\":\"readRssi\",\"address\":\""
                            + address + "\",\"code\":\"notSupported\","
                            + "\"message\":\"no live RSSI\"}");
                } else {
                    out.println("{\"event\":\"rssiResult\",\"requestId\":"
                            + id + ",\"address\":\"" + address
                            + "\",\"rssi\":-55,\"source\":\"lastSeen\"}");
                }
            } else {
                out.println("{\"event\":\"error\",\"requestId\":" + id
                        + ",\"command\":\"" + cmd
                        + "\",\"code\":\"badRequest\","
                        + "\"message\":\"unknown command\"}");
            }
        }
        System.exit(0);
    }
}
