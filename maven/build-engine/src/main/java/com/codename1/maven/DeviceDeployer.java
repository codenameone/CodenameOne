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
/// It drives the system's own `ssh` and `scp`, so keys, agents, `~/.ssh/config`
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

    /// Streams the service's journal after the restart, until interrupted.
    public DeviceDeployer followLogs(boolean followLogs) {
        this.followLogs = followLogs;
        return this;
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
        String target = u.isEmpty() ? h : u + "@" + h;
        String remoteBinary = remoteDir + "/" + name;

        log.info("Copying " + binary.getName() + " (" + (binary.length() / 1024) + " KB) to " + target
                + " and installing the " + name + " service");
        run(ssh(target, false, installScript(name, remoteBinary)), binary);
        log.info("Deployed " + remoteBinary + "; it starts on boot and restarts if it exits");
        if (followLogs) {
            run(ssh(target, true, "sudo journalctl -f -n 50 -u " + name + ".service"));
        }
    }

    /// The shell run on the device: read the binary from standard input into a
    /// private directory, move it into place, write the unit only when it
    /// changed, and restart. The staging directory comes from `mktemp -d`, so
    /// it is mode 0700 and named unpredictably: another account on the device
    /// cannot plant or swap the files root is about to install. `sudo` because both destinations are
    /// root's; a device whose deploy user has no passwordless sudo fails here
    /// with sudo's own message.
    String installScript(String name, String remoteBinary) {
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
                .append("StandardInput=tty\nTTYPath=/dev/tty1\nTTYReset=yes\nTTYVHangup=yes\n")
                .append("StandardOutput=journal\nStandardError=journal\n");
        for (String pair : environment.split("[,\\s]+")) {
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
