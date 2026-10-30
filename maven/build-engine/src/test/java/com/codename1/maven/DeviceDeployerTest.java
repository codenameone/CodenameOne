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
package com.codename1.maven;

import com.codename1.builders.BuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceDeployerTest {

    @TempDir
    File dir;

    private File write(String name, byte[] content, boolean executable) throws IOException {
        File f = new File(dir, name);
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(content);
        }
        assertTrue(f.setExecutable(executable));
        return f;
    }

    @Test
    void theInstallRefusesADeviceOfAnotherArchitecture() throws IOException {
        byte[] arm64 = new byte[64];
        arm64[0] = 0x7f; arm64[1] = 'E'; arm64[2] = 'L'; arm64[3] = 'F';
        arm64[5] = 1;            // little endian
        arm64[18] = (byte) 183;  // EM_AARCH64
        assertEquals("aarch64|arm64", DeviceDeployer.unameFor(write("arm", arm64, true)));
        byte[] x64 = arm64.clone();
        x64[18] = 62;
        assertEquals("x86_64", DeviceDeployer.unameFor(write("x64", x64, true)));
        assertNull(DeviceDeployer.unameFor(write("short", new byte[] {0x7f, 'E'}, true)));

        String script = new DeviceDeployer(new NullLog()).remoteDir("/opt/app")
                .installScript("demo", "/opt/app/demo", "x86_64");
        int check = script.indexOf("case \"$(uname -m)\" in x86_64)");
        assertTrue(check > script.indexOf("cat > "), script);
        assertTrue(check < script.indexOf("sudo install"), script);
    }

    @Test
    void findsTheNewestElfExecutableOnly() throws IOException {
        byte[] elf = {0x7f, 'E', 'L', 'F', 2, 1, 1};
        write("script.sh", "#!/bin/sh\n".getBytes(StandardCharsets.US_ASCII), true);
        File old = write("old", elf, true);
        assertTrue(old.setLastModified(1000L));
        File app = write("app", elf, true);
        write("lib.so", elf, false);
        assertEquals(app, DeviceDeployer.findBinary(dir, new File(dir, "missing")));
        assertNull(DeviceDeployer.findBinary(new File(dir, "missing")));
    }

    @Test
    void refusesWhatItCannotDeploy() throws IOException {
        File script = write("script.sh", "#!/bin/sh\n".getBytes(StandardCharsets.US_ASCII), true);
        DeviceDeployer d = new DeviceDeployer(new NullLog()).binary(script);
        assertThrows(BuildException.class, d::deploy, "no host");
        d.host("pi.local");
        BuildException e = assertThrows(BuildException.class, d::deploy);
        assertTrue(e.getMessage().contains("ELF"), e.getMessage());
        File elf = write("app", new byte[] {0x7f, 'E', 'L', 'F'}, true);
        d.binary(elf).service("app; reboot");
        assertThrows(BuildException.class, d::deploy);
    }

    @Test
    void namesThatCouldBeReadAsOptionsAreRefused() throws Exception {
        File elf = write("app", new byte[] {0x7f, 'E', 'L', 'F'}, true);
        for (String host : new String[] {"-oProxyCommand=touch x", "a b", "-p"}) {
            DeviceDeployer d = new DeviceDeployer(new NullLog()).host(host).binary(elf);
            assertThrows(BuildException.class, d::deploy, host);
        }
        assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("pi").user("-x")
                .binary(elf)::deploy);
        for (String service : new String[] {"-h", "..", "."}) {
            assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("pi").binary(elf)
                    .service(service)::deploy, service);
        }
        assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("pi").binary(elf)
                .remoteDir("/opt/../etc")::deploy);
    }

    @Test
    void theServiceMayUseRealtimeSchedulingAndLockMemory() {
        String script = new DeviceDeployer(new NullLog()).remoteDir("/opt/app").installScript("demo", "/opt/app/demo");
        assertTrue(script.contains("LimitRTPRIO=infinity\n"), script);
        assertTrue(script.contains("LimitMEMLOCK=infinity\n"), script);
    }

    @Test
    void followingLogsNeedsAHostAndAServiceAndRefusesOptionLikeNames() {
        DeviceDeployer d = new DeviceDeployer(new NullLog());
        BuildException e = assertThrows(BuildException.class, d::tailLogs);
        assertTrue(e.getMessage().contains("cn1.device.service"), e.getMessage());
        d.service("demo");
        assertThrows(BuildException.class, d::tailLogs, "no host");
        d.host("-oProxyCommand=touch /tmp/x");
        assertThrows(BuildException.class, d::tailLogs);
        d.host("pi.local").service("demo; reboot");
        assertThrows(BuildException.class, d::tailLogs);
    }

    @Test
    void theInstallScriptIsValidShellAndCarriesTheEnvironment() throws Exception {
        String script = new DeviceDeployer(new NullLog()).remoteDir("/opt/app")
                .environment("CN1_ROTATION=90, CN1_DISPLAY=fbdev NAME=it's P=50%")
                .installScript("demo", "/opt/app/demo");
        assertTrue(script.contains("ExecStart=/opt/app/demo"));
        assertTrue(script.contains("Environment=\"CN1_ROTATION=90\"\n"));
        assertTrue(script.contains("Environment=\"CN1_DISPLAY=fbdev\"\n"));
        assertTrue(script.contains("Environment=\"CN1_PROFILE=device\"\n"), "a board runs the device role");
        String chosen = new DeviceDeployer(new NullLog()).remoteDir("/opt/app").environment("CN1_PROFILE=kiosk")
                .installScript("demo", "/opt/app/demo");
        assertTrue(chosen.contains("Environment=\"CN1_PROFILE=kiosk\"\n") && !chosen.contains("CN1_PROFILE=device"));
        String none = new DeviceDeployer(new NullLog()).remoteDir("/opt/app").profile("")
                .installScript("demo", "/opt/app/demo");
        assertTrue(!none.contains("CN1_PROFILE"));
        assertTrue(script.contains("systemctl restart -- demo.service"));
        File f = new File(dir, "install.sh");
        Files.write(f.toPath(), script.getBytes(StandardCharsets.UTF_8));
        Process p = new ProcessBuilder("sh", "-n", f.getAbsolutePath()).redirectErrorStream(true).start();
        assertEquals(0, p.waitFor(), "sh -n rejected the script");
        // The unit is printed through printf '%s' '...'; the quote in a value must
        // come out as written rather than ending the string.
        Process unit = new ProcessBuilder("sh", "-c", script.substring(script.indexOf("printf"),
                script.indexOf(" > \"$d/unit\""))).start();
        String text = new String(readAll(unit), StandardCharsets.UTF_8);
        assertEquals(0, unit.waitFor());
        assertTrue(text.contains("Environment=\"NAME=it's\"\n") && text.contains("Environment=\"P=50%%\"\n"), text);
        assertTrue(text.startsWith("[Unit]\n"), text);
    }

    private static byte[] readAll(Process p) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = p.getInputStream().read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static final class NullLog implements com.codename1.build.Log {
        public boolean isDebugEnabled() { return false; }
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable t) { }
        public void debug(Throwable t) { }
        public boolean isInfoEnabled() { return false; }
        public void info(CharSequence c) { }
        public void info(CharSequence c, Throwable t) { }
        public void info(Throwable t) { }
        public boolean isWarnEnabled() { return false; }
        public void warn(CharSequence c) { }
        public void warn(CharSequence c, Throwable t) { }
        public void warn(Throwable t) { }
        public boolean isErrorEnabled() { return false; }
        public void error(CharSequence c) { }
        public void error(CharSequence c, Throwable t) { }
        public void error(Throwable t) { }
    }
}
