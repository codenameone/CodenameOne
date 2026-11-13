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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Windows half of [DeviceDeployer]: a kiosk executable installed as a
/// Windows IoT Enterprise device's shell.
class WindowsDeviceDeployerTest {

    @TempDir
    File dir;

    /// A minimal PE image: "MZ", e_lfanew at 0x3c, then "PE\0\0" and the machine.
    private File pe(String name, int machine) throws IOException {
        byte[] b = new byte[0x100];
        b[0] = 'M';
        b[1] = 'Z';
        b[0x3c] = (byte) 0x80;
        b[0x80] = 'P';
        b[0x81] = 'E';
        b[0x84] = (byte) (machine & 0xff);
        b[0x85] = (byte) (machine >> 8);
        File f = new File(dir, name);
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(b);
        }
        return f;
    }

    @Test
    void readsTheArchitectureOfAWindowsExecutable() throws IOException {
        assertEquals("X64", DeviceDeployer.peMachine(pe("a.exe", 0x8664)));
        assertEquals("ARM64", DeviceDeployer.peMachine(pe("b.exe", 0xAA64)));
        assertNull(DeviceDeployer.peMachine(pe("c.exe", 0x14c)));
        assertTrue(DeviceDeployer.isPe(pe("d.exe", 0x8664)));
    }

    @Test
    void findsAWindowsExecutableToo() throws IOException {
        File exe = pe("Kiosk.exe", 0x8664);
        assertEquals(exe, DeviceDeployer.findBinary(dir));
        File notExe = pe("Kiosk.dll", 0x8664);
        assertTrue(notExe.setLastModified(exe.lastModified() + 5000));
        assertEquals(exe, DeviceDeployer.findBinary(dir), "a DLL is not an application");
    }

    @Test
    void theShellModeKeepsExplorerForAdministrators() {
        String ps = new DeviceDeployer(new NullLog()).environment("CN1_ROTATION=90")
                .windowsInstallScript("Kiosk", "ARM64");
        assertTrue(ps.contains("Client-EmbeddedShellLauncher"), ps);
        assertTrue(ps.contains("SetCustomShell('S-1-5-32-544', 'explorer.exe'"), ps);
        assertTrue(ps.contains("SetDefaultShell($exe, 0)"), ps);
        assertTrue(ps.contains("$dir = 'C:\\ProgramData\\CodenameOne'"), ps);
        assertTrue(ps.contains("SetEnvironmentVariable('CN1_ROTATION', '90', 'Machine')"), ps);
        assertTrue(ps.contains("SetEnvironmentVariable('CN1_PROFILE', 'device', 'Machine')"), ps);
        assertTrue(ps.contains("$arch -eq 'ARM64'"), ps);
        assertFalse(ps.contains("Client-KeyboardFilter"), ps);
        // Users may run the shell but never write beside it (exe swap, DLL plant);
        // only the logs folder is theirs.
        assertTrue(ps.contains("icacls $dir /inheritance:r /grant:r"), ps);
        assertTrue(ps.contains("'*S-1-5-32-545:(OI)(CI)RX'"), ps);
        assertFalse(ps.contains("icacls $dir /grant '*S-1-5-32-545:(OI)(CI)M'"), ps);
        assertTrue(ps.contains("icacls $logs /grant '*S-1-5-32-545:(OI)(CI)M'"), ps);
        assertTrue(ps.contains("(Join-Path $logs \"$name.log\")"), ps);
        // The architecture is checked before anything is replaced.
        assertTrue(ps.indexOf("PROCESSOR_ARCHITECTURE") < ps.indexOf("Move-Item"), ps);
    }

    @Test
    void anX64BuildMayGoToWindowsOnArm() {
        String ps = new DeviceDeployer(new NullLog()).windowsInstallScript("Kiosk", "X64");
        assertTrue(ps.contains("$arch -eq 'AMD64' -or $arch -eq 'ARM64'"), ps);
    }

    @Test
    void oneKioskAccountAndTheKeyboardFilter() {
        String ps = new DeviceDeployer(new NullLog()).kioskUser("kiosk").keyboardFilter(true)
                .windowsInstallScript("Kiosk", "X64");
        assertTrue(ps.contains("NTAccount('kiosk')"), ps);
        assertTrue(ps.contains("SetCustomShell($sid, $exe"), ps);
        assertFalse(ps.contains("SetDefaultShell"), ps);
        assertTrue(ps.contains("'Ctrl+Alt+Del'"), ps);
    }

    @Test
    void autostartRegistersALogonTaskAndNoShell() {
        String ps = new DeviceDeployer(new NullLog()).kioskMode("autostart").windowsInstallScript("Kiosk", "X64");
        assertTrue(ps.contains("New-ScheduledTaskTrigger -AtLogOn"), ps);
        assertFalse(ps.contains("WESL_UserSetting"), ps);
    }

    @Test
    void valuesCannotBreakOutOfTheirQuotes() {
        String ps = new DeviceDeployer(new NullLog()).environment("GREETING=it's")
                .windowsInstallScript("Kiosk", null);
        assertTrue(ps.contains("'GREETING', 'it''s'"), ps);
    }

    @Test
    void refusesWhatItCannotInstall() throws IOException {
        File exe = pe("Kiosk.exe", 0x8664);
        assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("kiosk").binary(exe)
                .kioskMode("assigned-access")::deploy);
        assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("kiosk").binary(exe)
                .kioskUser("a'; Remove-Item C:\\")::deploy);
        assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("kiosk").binary(exe)
                .remoteDir("C:\\..\\Windows")::deploy);
        assertThrows(BuildException.class, new DeviceDeployer(new NullLog()).host("-oProxyCommand=x").binary(exe)::deploy);
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
