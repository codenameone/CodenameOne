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
public final class DeviceDeployer {
    private final Log log;
    private File binary;
    private String host;
    private String user;
    private int port = 22;
    private String remoteDir = "/opt/codenameone";
    private String service;
    private String environment = "";
    private String profile = "device";
    private boolean followLogs;

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
        String target = checkedTarget(name);
        run(ssh(target, true, "sudo journalctl -f -n 100 -u " + name + ".service"));
    }

    public void deploy() throws BuildException {
        if (host == null || host.trim().isEmpty()) {
            throw new BuildException("No device host: set cn1.device.host (an address or an ~/.ssh/config alias)");
        }
        if (binary == null || !binary.isFile()) {
            throw new BuildException("No native Linux binary to deploy" + (binary == null ? "" : " at " + binary)
                    + ": build the linux-native target locally or set cn1.device.binary");
        }
        if (!isElf(binary)) {
            throw new BuildException(binary + " is not a Linux executable (no ELF header)");
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

    /// The newest ELF executable directly under one of `dirs`, or `null`: what a
    /// local linux-native build left behind.
    public static File findBinary(File... dirs) {
        File best = null;
        for (File dir : dirs) {
            File[] files = dir == null ? null : dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File f : files) {
                if (f.isFile() && f.canExecute() && isElf(f) && (best == null || f.lastModified() > best.lastModified())) {
                    best = f;
                }
            }
        }
        return best;
    }
}
