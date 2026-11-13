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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Pushes a native Linux build to a device over SSH and runs it there as a
/// systemd service: the Maven `deploy-device` goal and the Gradle
/// `cn1DeployDevice` task are both this class.
///
/// It drives the system's own `ssh`, so keys, agents, `~/.ssh/config`
/// host aliases and jump hosts work exactly as they do in a terminal; nothing
/// here stores a credential. The binary goes to `remoteDir`, a unit named after
/// the service is written to `/etc/systemd/system` and the service is
/// restarted, so a redeploy is one command. The unit starts the application on
/// boot, restarts it if it exits, and runs it on the console's virtual
/// terminal, which is what the embedded display needs to take the screen.
///
/// A Windows executable (`windows.embedded=true`) goes to a Windows IoT
/// Enterprise device the same way, over the OpenSSH server Windows ships:
/// copied with `scp`, then installed by a PowerShell script run as the deploy
/// user, who must be an administrator. By default it becomes the device's shell
/// through Shell Launcher v2 -- the application replaces explorer for every
/// account but the Administrators group, which keeps explorer so the device can
/// still be serviced, and Shell Launcher restarts the application when it
/// exits. [#kioskMode(String)] `autostart` instead starts it at logon from a
/// scheduled task, with no lockdown. Assigned Access is not offered: it hosts
/// packaged applications, and its Win32 path is a multi-app kiosk profile that
/// keeps the shell.
public final class DeviceDeployer {
    /// Where a Windows device keeps the application when no directory is given.
    static final String WINDOWS_DIR = "C:\\ProgramData\\CodenameOne";
    private static final String LINUX_DIR = "/opt/codenameone";
    private final Log log;
    private File binary;
    private String host;
    private String user;
    private int port = 22;
    private String remoteDir = LINUX_DIR;
    private String service;
    private String environment = "";
    private String profile = "device";
    private boolean followLogs;
    private String kioskMode = "shell";
    private String kioskUser;
    private boolean keyboardFilter;
    private Boolean windowsTarget;

    public DeviceDeployer(Log log) {
        this.log = log;
    }

    public DeviceDeployer binary(File binary) {
        this.binary = binary;
        return this;
    }

    public DeviceDeployer host(String host) {
        this.host = host;
        return this;
    }

    public DeviceDeployer user(String user) {
        this.user = user;
        return this;
    }

    public DeviceDeployer port(int port) {
        this.port = port;
        return this;
    }

    public DeviceDeployer remoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
        return this;
    }

    /// The systemd unit name, without `.service`. Defaults to the binary's name.
    public DeviceDeployer service(String service) {
        this.service = service;
        return this;
    }

    /// `KEY=value` pairs, separated by spaces or commas, for the unit's
    /// `Environment=` line: `CN1_ROTATION=90`, `CN1_DISPLAY=fbdev`, ...
    public DeviceDeployer environment(String environment) {
        this.environment = environment == null ? "" : environment;
        return this;
    }

    /// The backend profile the unit runs under, `device` by default: a backend
    /// deployed to a board starts its `@Profile("device")` beans and reads
    /// `application-device.properties`, while the same project packaged for the
    /// cloud runs its own. An explicit `CN1_PROFILE` in [#environment(String)]
    /// wins; null or empty sets none. A UI application ignores it.
    public DeviceDeployer profile(String profile) {
        this.profile = profile == null ? "" : profile.trim();
        return this;
    }

    /// Windows only: `shell` (the default) installs the application as the
    /// device's shell through Shell Launcher; `autostart` starts it at logon
    /// and changes nothing else.
    public DeviceDeployer kioskMode(String kioskMode) {
        this.kioskMode = kioskMode == null || kioskMode.trim().isEmpty() ? "shell" : kioskMode.trim();
        return this;
    }

    /// Windows only: the one account whose shell becomes the application. By
    /// default it is every account but the Administrators group.
    public DeviceDeployer kioskUser(String kioskUser) {
        this.kioskUser = kioskUser;
        return this;
    }

    /// Windows only: also turns on Keyboard Filter and blocks the shortcuts the
    /// application cannot filter itself, Ctrl+Alt+Del among them. Off by
    /// default: with it on, only Keyboard Filter's breakout key (the left
    /// Windows key pressed five times on the welcome screen) leaves the kiosk.
    public DeviceDeployer keyboardFilter(boolean keyboardFilter) {
        this.keyboardFilter = keyboardFilter;
        return this;
    }

    /// Whether the device runs Windows, for [#tailLogs()] when there is no
    /// binary to tell; null decides from the binary.
    public DeviceDeployer windowsTarget(Boolean windowsTarget) {
        this.windowsTarget = windowsTarget;
        return this;
    }

    /// Streams the service's journal after the restart, until interrupted.
    public DeviceDeployer followLogs(boolean followLogs) {
        this.followLogs = followLogs;
        return this;
    }

    /// Follows the log of the service a deploy installed, without deploying: the
    /// service is `cn1.device.service`, else the name of `binary`.
    public void tailLogs() throws BuildException {
        String name = service != null && !service.trim().isEmpty() ? service.trim()
                : binary != null ? binary.getName() : null;
        if (name == null) {
            throw new BuildException("Name the service: set cn1.device.service (or build the app, whose"
                    + " executable names it)");
        }
        if (isWindowsTarget()) {
            name = stripExe(name);
            String target = checkedWindowsTarget(name);
            run(ssh(target, true, windowsLogCommand(name, 100)));
            return;
        }
        String target = checkedTarget(name);
        run(ssh(target, true, "sudo journalctl -f -n 100 -u " + name + ".service"));
    }

    private boolean isWindowsTarget() {
        if (windowsTarget != null) {
            return windowsTarget.booleanValue();
        }
        return binary != null && binary.isFile() && isPe(binary);
    }

    public void deploy() throws BuildException {
        if (host == null || host.trim().isEmpty()) {
            throw new BuildException("No device host: set cn1.device.host (an address or an ~/.ssh/config alias)");
        }
        if (binary == null || !binary.isFile()) {
            throw new BuildException("No native Linux or Windows binary to deploy" + (binary == null ? "" : " at " + binary)
                    + ": build the linux-native target locally or set cn1.device.binary");
        }
        if (isPe(binary)) {
            deployWindows();
            return;
        }
        if (!isElf(binary)) {
            throw new BuildException(binary + " is neither a Linux executable (no ELF header) nor a Windows one");
        }
        String name = service != null && !service.trim().isEmpty() ? service.trim() : binary.getName();
        String target = checkedTarget(name);
        String remoteBinary = remoteDir + "/" + name;

        log.info("Copying " + binary.getName() + " (" + (binary.length() / 1024) + " KB) to " + target
                + " and installing the " + name + " service");
        run(ssh(target, false, installScript(name, remoteBinary, unameFor(binary))), binary);
        log.info("Deployed " + remoteBinary + "; it starts on boot and restarts if it exits");
        if (followLogs) {
            run(ssh(target, true, "sudo journalctl -f -n 50 -u " + name + ".service"));
        }
    }

    /// Validates the host, user, service name and remote directory and returns the
    /// ssh destination.
    private String checkedTarget(String name) throws BuildException {
        if (host == null || host.trim().isEmpty()) {
            throw new BuildException("No device host: set cn1.device.host (an address or an ~/.ssh/config alias)");
        }
        // The first character is a letter or digit so that nothing here can reach
        // ssh, install or systemctl as an option ("-oProxyCommand=..." runs a
        // local command), and a name cannot be "." or "..".
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9_.@-]*") || !remoteDir.matches("/[A-Za-z0-9_./-]*")
                || remoteDir.contains("..")) {
            throw new BuildException("The service name may only use letters, digits and _.@- and must start with a"
                    + " letter or digit; the remote directory is an absolute path of the same characters and /");
        }
        String h = host.trim();
        String u = user == null ? "" : user.trim();
        if (!h.matches("[A-Za-z0-9_\\[][A-Za-z0-9_.:%\\[\\]-]*") || !u.matches("([A-Za-z0-9_][A-Za-z0-9_.-]*)?")) {
            throw new BuildException("cn1.device.host and cn1.device.user must start with a letter or digit and use"
                    + " only letters, digits and _.-: (" + h + ")");
        }
        return u.isEmpty() ? h : u + "@" + h;
    }

    /// The shell run on the device: read the binary from standard input into a
    /// private directory, move it into place, write the unit only when it
    /// changed, and restart. The staging directory comes from `mktemp -d`, so
    /// it is mode 0700 and named unpredictably: another account on the device
    /// cannot plant or swap the files root is about to install. `sudo` because both destinations are
    /// root's; a device whose deploy user has no passwordless sudo fails here
    /// with sudo's own message.
    String installScript(String name, String remoteBinary) {
        return installScript(name, remoteBinary, null);
    }

    /// As above, refusing before anything is installed when the device's
    /// `uname -m` does not match `machines` (a `case` pattern, see [#unameFor]).
    /// A binary for the wrong architecture would otherwise fail with "Exec format
    /// error" and the unit's `Restart=always` would retry it every second.
    String installScript(String name, String remoteBinary, String machines) {
        StringBuilder unit = new StringBuilder();
        unit.append("[Unit]\n")
                .append("Description=Codename One application ").append(name).append('\n')
                .append("After=systemd-udev-settle.service network-online.target\n")
                .append("Wants=network-online.target\n")
                .append("Conflicts=getty@tty1.service\n\n")
                .append("[Service]\n")
                .append("ExecStart=").append(remoteBinary).append('\n')
                .append("WorkingDirectory=").append(remoteDir).append('\n')
                .append("Restart=always\nRestartSec=1\n")
                // The service runs as root, which may use SCHED_FIFO and lock memory;
                // the limits are raised too so a real-time thread
                // (com.codename1.realtime) gets them under any User= a site adds.
                .append("LimitRTPRIO=infinity\nLimitMEMLOCK=infinity\n")
                .append("StandardInput=tty\nTTYPath=/dev/tty1\nTTYReset=yes\nTTYVHangup=yes\n")
                .append("StandardOutput=journal\nStandardError=journal\n");
        String pairs = environment;
        if (!profile.isEmpty() && !(" " + environment.replace(',', ' ')).contains(" CN1_PROFILE=")) {
            pairs = environment + " CN1_PROFILE=" + profile;
        }
        for (String pair : pairs.split("[,\\s]+")) {
            if (pair.contains("=")) {
                // Quoted, with systemd's specifier and escape characters doubled,
                // so a value reaches the program exactly as written.
                unit.append("Environment=\"").append(pair.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("%", "%%")).append("\"\n");
            }
        }
        unit.append("\n[Install]\nWantedBy=multi-user.target\n");
        String unitPath = "/etc/systemd/system/" + name + ".service";
        return "set -e\n"
                + "d=$(mktemp -d)\n"
                + "trap 'rm -rf \"$d\"' EXIT\n"
                + "cat > \"$d/upload\"\n"
                + (machines == null ? "" : "case \"$(uname -m)\" in " + machines + ") ;; *) echo \"" + name
                        + " was built for " + machines + ", but this device is $(uname -m): build with"
                        + " -Dcn1.board=<board>\" >&2; exit 1;; esac\n")
                + "sudo mkdir -p -- " + remoteDir + "\n"
                + "sudo install -m 0755 -- \"$d/upload\" " + remoteBinary + "\n"
                + "printf '%s' '" + unit.toString().replace("'", "'\\''") + "' > \"$d/unit\"\n"
                + "if ! sudo cmp -s -- \"$d/unit\" " + unitPath + "; then\n"
                + "  sudo install -m 0644 -- \"$d/unit\" " + unitPath + "\n"
                + "  sudo systemctl daemon-reload\n"
                + "  sudo systemctl enable -- " + name + ".service\n"
                + "fi\n"
                + "sudo systemctl restart -- " + name + ".service\n";
    }

    private static String stripExe(String name) {
        return name.regionMatches(true, Math.max(0, name.length() - 4), ".exe", 0, 4)
                ? name.substring(0, name.length() - 4) : name;
    }

    private String windowsDir() {
        return LINUX_DIR.equals(remoteDir) ? WINDOWS_DIR : remoteDir;
    }

    /// As [#checkedTarget(String)], for a Windows device: the directory is a
    /// drive-letter path.
    private String checkedWindowsTarget(String name) throws BuildException {
        String dir = windowsDir();
        if (!dir.matches("[A-Za-z]:\\\\[A-Za-z0-9_. \\\\-]*") || dir.contains("..")) {
            throw new BuildException("On Windows cn1.device.dir is a drive-letter path of letters, digits, spaces"
                    + " and _.- (" + dir + ")");
        }
        String saved = remoteDir;
        remoteDir = LINUX_DIR;
        try {
            return checkedTarget(name);
        } finally {
            remoteDir = saved;
        }
    }

    private void deployWindows() throws BuildException {
        String name = stripExe(service != null && !service.trim().isEmpty() ? service.trim() : binary.getName());
        String target = checkedWindowsTarget(name);
        if (!"shell".equals(kioskMode) && !"autostart".equals(kioskMode)) {
            throw new BuildException("cn1.device.kioskMode is shell or autostart, not " + kioskMode);
        }
        String u = kioskUser == null ? "" : kioskUser.trim();
        if (!u.matches("([A-Za-z0-9_][A-Za-z0-9_. -]*(\\\\[A-Za-z0-9_][A-Za-z0-9_. -]*)?)?")) {
            throw new BuildException("cn1.device.kioskUser is an account name, optionally DOMAIN\\user (" + u + ")");
        }
        File script;
        try {
            script = File.createTempFile("cn1-deploy-", ".ps1");
            java.nio.file.Files.write(script.toPath(),
                    windowsInstallScript(name, peMachine(binary)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BuildException("Could not write the install script", ex);
        }
        try {
            String staged = "cn1-deploy-" + name;
            log.info("Copying " + binary.getName() + " (" + (binary.length() / 1024) + " KB) to " + target
                    + " and installing it " + ("shell".equals(kioskMode) ? "as the device's shell" : "to start at logon"));
            List<String> scp = new ArrayList<String>(Arrays.asList("scp", "-q", "-P", String.valueOf(port), "--",
                    binary.getAbsolutePath(), target + ":" + staged + ".exe"));
            run(scp);
            scp.set(scp.size() - 2, script.getAbsolutePath());
            scp.set(scp.size() - 1, target + ":" + staged + ".ps1");
            run(scp);
            // The default OpenSSH shell on Windows is cmd; the staged files are in
            // the deploy user's profile, which is where both commands start.
            run(ssh(target, false, "powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "
                    + staged + ".ps1"));
        } finally {
            if (!script.delete()) {
                script.deleteOnExit();
            }
        }
        log.info("Deployed " + windowsDir() + "\\" + name + ".exe");
        if (followLogs) {
            run(ssh(target, true, windowsLogCommand(name, 50)));
        }
    }

    private String windowsLogCommand(String name, int lines) {
        return "powershell -NoProfile -Command \"Get-Content -Wait -Tail " + lines + " -LiteralPath '"
                + windowsDir() + "\\logs\\" + name + ".log'\"";
    }

    private static String psQuote(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    /// The PowerShell run on a Windows device, from the deploy user's profile
    /// where `scp` left the executable beside it. It refuses an executable the
    /// device cannot run, stops the running copy, installs the new one, writes
    /// the environment and the log path as machine variables (a shell has no
    /// other environment to inherit), and then configures the kiosk. Turning a
    /// Windows feature on needs a restart, which the script does itself, once.
    String windowsInstallScript(String name, String machine) {
        String dir = windowsDir();
        StringBuilder ps = new StringBuilder();
        ps.append("$ErrorActionPreference = 'Stop'\n")
                .append("$name = ").append(psQuote(name)).append("\n")
                .append("$dir = ").append(psQuote(dir)).append("\n")
                .append("$exe = Join-Path $dir \"$name.exe\"\n")
                .append("$staged = Join-Path $PSScriptRoot \"cn1-deploy-$name.exe\"\n")
                .append("$reboot = $false\n");
        if (machine != null) {
            // An x64 build also runs on Windows on ARM, under emulation.
            ps.append("$arch = $env:PROCESSOR_ARCHITECTURE\n")
                    .append("if (-not (").append("ARM64".equals(machine)
                            ? "$arch -eq 'ARM64'" : "$arch -eq 'AMD64' -or $arch -eq 'ARM64'").append(")) {\n")
                    .append("  Write-Error \"$name was built for ").append(machine)
                    .append(", but this device is $arch: set windows.arch or windows.embedded.board\"\n")
                    .append("}\n");
        }
        ps.append("New-Item -ItemType Directory -Force -Path $dir | Out-Null\n")
                // The exe is the shell of every non-administrator: only
                // Administrators and SYSTEM may change anything beside it. With
                // write access a user could replace it, or plant a DLL the exe
                // would load from its own directory first, and run code as the
                // next user to log on. ProgramData's inherited ACL lets Users
                // create files, so inheritance is cut and the grants replaced.
                .append("icacls $dir /inheritance:r /grant:r '*S-1-5-32-544:(OI)(CI)F' '*S-1-5-18:(OI)(CI)F'"
                        + " '*S-1-5-32-545:(OI)(CI)RX' | Out-Null\n")
                // icacls is a native command: Stop does not see its failure.
                .append("if ($LASTEXITCODE -ne 0) { throw \"icacls could not lock down $dir\" }\n")
                // The kiosk account is not an administrator and writes its log
                // here, the one place it can write.
                .append("$logs = Join-Path $dir 'logs'\n")
                .append("New-Item -ItemType Directory -Force -Path $logs | Out-Null\n")
                .append("icacls $logs /grant '*S-1-5-32-545:(OI)(CI)M' | Out-Null\n")
                .append("if ($LASTEXITCODE -ne 0) { throw \"icacls could not open $logs to Users\" }\n")
                .append("Get-Process -Name $name -ErrorAction SilentlyContinue | Stop-Process -Force\n")
                .append("Start-Sleep -Milliseconds 300\n")
                .append("Move-Item -Force -LiteralPath $staged -Destination $exe\n")
                .append("Remove-Item -Force -LiteralPath $PSCommandPath\n")
                .append("[Environment]::SetEnvironmentVariable('CN1_LOG_FILE', (Join-Path $logs \"$name.log\"), 'Machine')\n");
        String pairs = environment;
        if (!profile.isEmpty() && !(" " + environment.replace(',', ' ')).contains(" CN1_PROFILE=")) {
            pairs = environment + " CN1_PROFILE=" + profile;
        }
        for (String pair : pairs.split("[,\\s]+")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).matches("[A-Za-z_][A-Za-z0-9_]*")) {
                ps.append("[Environment]::SetEnvironmentVariable(").append(psQuote(pair.substring(0, eq)))
                        .append(", ").append(psQuote(pair.substring(eq + 1))).append(", 'Machine')\n");
            }
        }
        if ("shell".equals(kioskMode)) {
            ps.append("if ((Get-WindowsOptionalFeature -Online -FeatureName Client-EmbeddedShellLauncher).State -ne 'Enabled') {\n")
                    .append("  Enable-WindowsOptionalFeature -Online -All -NoRestart -FeatureName Client-EmbeddedShellLauncher | Out-Null\n")
                    .append("  $reboot = $true\n")
                    .append("}\n")
                    .append("$sl = [wmiclass]'\\\\localhost\\root\\standardcimv2\\embedded:WESL_UserSetting'\n")
                    // Restart actions: 0 restarts the shell, 3 does nothing.
                    .append("$sl.SetEnabled($true) | Out-Null\n")
                    .append("$sl.SetCustomShell('S-1-5-32-544', 'explorer.exe', $null, $null, 3) | Out-Null\n");
            if (kioskUser != null && !kioskUser.trim().isEmpty()) {
                ps.append("$sid = (New-Object System.Security.Principal.NTAccount(").append(psQuote(kioskUser.trim()))
                        .append(")).Translate([System.Security.Principal.SecurityIdentifier]).Value\n")
                        .append("$sl.SetCustomShell($sid, $exe, $null, $null, 0) | Out-Null\n");
            } else {
                ps.append("$sl.SetDefaultShell($exe, 0) | Out-Null\n");
            }
            ps.append("Unregister-ScheduledTask -TaskName \"Codename One $name\" -Confirm:$false -ErrorAction SilentlyContinue\n");
        } else {
            ps.append("$action = New-ScheduledTaskAction -Execute $exe -WorkingDirectory $dir\n")
                    .append("$trigger = New-ScheduledTaskTrigger -AtLogOn\n")
                    .append("$principal = New-ScheduledTaskPrincipal -GroupId 'S-1-5-32-545' -RunLevel Limited\n")
                    .append("$settings = New-ScheduledTaskSettingsSet -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1)"
                            + " -ExecutionTimeLimit 0 -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries\n")
                    .append("Register-ScheduledTask -Force -TaskName \"Codename One $name\" -Action $action -Trigger $trigger"
                            + " -Principal $principal -Settings $settings | Out-Null\n");
        }
        if (keyboardFilter) {
            ps.append("if ((Get-WindowsOptionalFeature -Online -FeatureName Client-KeyboardFilter).State -ne 'Enabled') {\n")
                    .append("  Enable-WindowsOptionalFeature -Online -All -NoRestart -FeatureName Client-KeyboardFilter | Out-Null\n")
                    .append("  $reboot = $true\n")
                    .append("} else {\n")
                    .append("  foreach ($k in 'Ctrl+Alt+Del', 'Ctrl+Shift+Esc', 'Win+L', 'Alt+Tab', 'Ctrl+Esc', 'Alt+F4', 'Windows') {\n")
                    .append("    $p = Get-CimInstance -Namespace root\\standardcimv2\\embedded -ClassName WEKF_PredefinedKey"
                            + " | Where-Object { $_.Id -eq $k }\n")
                    .append("    if ($p) { Set-CimInstance -InputObject $p -Property @{ Enabled = $true } }\n")
                    .append("  }\n")
                    .append("}\n");
        }
        ps.append("if ($reboot) {\n")
                .append("  Write-Output 'A Windows feature was turned on; restarting the device to finish. Deploy again"
                        + " after it is back.'\n")
                .append("  Restart-Computer -Force\n")
                .append("  exit 0\n")
                .append("}\n");
        if ("shell".equals(kioskMode)) {
            // Shell Launcher started the old copy and restarts its shell when it
            // exits, so stopping it above relaunches the new one. A device on which
            // nobody is logged on yet starts it at the next logon.
            ps.append("Write-Output \"$name is the shell; it restarts when it exits\"\n");
        } else {
            ps.append("Start-ScheduledTask -TaskName \"Codename One $name\" -ErrorAction SilentlyContinue\n")
                    .append("Write-Output \"$name starts at logon\"\n");
        }
        return ps.toString();
    }

    private List<String> ssh(String target, boolean tty, String script) {
        List<String> cmd = new ArrayList<String>(Arrays.asList("ssh", "-p", String.valueOf(port)));
        if (tty) {
            cmd.add("-t");
        }
        // "--" ends ssh's options, so the target can never be read as one.
        cmd.add("--");
        cmd.add(target);
        cmd.add(script);
        return cmd;
    }

    private void run(List<String> command) throws BuildException {
        run(command, null);
    }

    /// Runs command with the console attached, or with input as its standard
    /// input when it is not null.
    private void run(List<String> command, File input) throws BuildException {
        try {
            ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
            if (input != null) {
                pb.redirectInput(input);
            }
            Process p = pb.start();
            int rc = p.waitFor();
            if (rc != 0) {
                throw new BuildException(command.get(0) + " exited with " + rc);
            }
        } catch (IOException ex) {
            throw new BuildException("Could not run " + command.get(0) + "; is OpenSSH installed?", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BuildException("Interrupted", ex);
        }
    }

    /// The `uname -m` values that can run `f`, as a shell `case` pattern, from its
    /// ELF `e_machine`; `null` for an unreadable file or a machine not listed. A
    /// 32-bit binary also accepts the 64-bit kernel of its family: Raspberry Pi OS
    /// 32-bit runs an aarch64 kernel on a Pi 4 or 5 and runs armhf binaries.
    static String unameFor(File f) {
        byte[] h = new byte[20];
        try (InputStream in = new FileInputStream(f)) {
            if (in.read(h) != 20) {
                return null;
            }
        } catch (IOException ex) {
            return null;
        }
        int m = h[5] == 2 ? ((h[18] & 0xff) << 8) | (h[19] & 0xff) : (h[18] & 0xff) | ((h[19] & 0xff) << 8);
        switch (m) {
            case 62:
                return "x86_64";
            case 183:
                return "aarch64|arm64";
            case 40:
                return "arm*|aarch64";
            case 243:
                return "riscv64";
            case 3:
                return "i?86|x86_64";
            default:
                return null;
        }
    }

    static boolean isElf(File f) {
        byte[] magic = new byte[4];
        try (InputStream in = new FileInputStream(f)) {
            return in.read(magic) == 4 && magic[0] == 0x7f && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F';
        } catch (IOException ex) {
            return false;
        }
    }

    static boolean isPe(File f) {
        byte[] magic = new byte[2];
        try (InputStream in = new FileInputStream(f)) {
            return in.read(magic) == 2 && magic[0] == 'M' && magic[1] == 'Z';
        } catch (IOException ex) {
            return false;
        }
    }

    /// `X64` or `ARM64` from a PE header's machine field, else null.
    static String peMachine(File f) {
        byte[] h = new byte[4096];
        int n;
        try (InputStream in = new FileInputStream(f)) {
            n = in.read(h);
        } catch (IOException ex) {
            return null;
        }
        if (n < 0x40) {
            return null;
        }
        int pe = (h[0x3c] & 0xff) | ((h[0x3d] & 0xff) << 8) | ((h[0x3e] & 0xff) << 16) | ((h[0x3f] & 0xff) << 24);
        if (pe < 0 || pe + 6 > n || h[pe] != 'P' || h[pe + 1] != 'E') {
            return null;
        }
        int m = (h[pe + 4] & 0xff) | ((h[pe + 5] & 0xff) << 8);
        return m == 0x8664 ? "X64" : m == 0xAA64 ? "ARM64" : null;
    }

    /// The newest executable directly under one of `dirs` -- an ELF binary, or a
    /// Windows `.exe` -- or `null`: what a local native build left behind.
    public static File findBinary(File... dirs) {
        File best = null;
        for (File dir : dirs) {
            File[] files = dir == null ? null : dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File f : files) {
                boolean exe = f.isFile() && ((f.canExecute() && isElf(f))
                        || (f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".exe") && isPe(f)));
                if (exe && (best == null || f.lastModified() > best.lastModified())) {
                    best = f;
                }
            }
        }
        return best;
    }
}
