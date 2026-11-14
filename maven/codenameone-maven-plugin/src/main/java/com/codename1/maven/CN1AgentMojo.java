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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs the simulator for an AI agent: the loopback MCP server is listening from
 * the first frame (no menu to click), the window never takes the focus, and on a
 * Linux machine with no display an Xvfb server is started for it.
 *
 * <p>Meant to be started in the background; the agent then drives the app with
 * the bundled {@code Sim.java} client, or any MCP host, on the printed port.</p>
 */
@Mojo(name = "agent")
public class CN1AgentMojo extends CN1RunMojo {

    /** The loopback port the MCP server listens on. */
    @Parameter(property = "cn1.mcp.port", defaultValue = "8765")
    private int mcpPort;

    /** Read by the shutdown hook as well as this thread. */
    private final AtomicReference<Process> xvfbRef = new AtomicReference<>();

    private final AtomicReference<File> portFileRef = new AtomicReference<>();

    /** Where {@code Sim.java} finds the port when it is not told: {@code common/target/cn1-agent.port}. */
    static final String PORT_FILE = "cn1-agent.port";

    @Override
    protected String helpAction() {
        return "mvn cn1:agent";
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        // The goal is normally stopped by a signal (Ctrl-C, kill, an agent's task stop), which
        // never unwinds this method; only a shutdown hook runs then.
        Thread hook = new Thread(this::cleanUp, "cn1-agent-cleanup");
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            super.executeImpl();
        } finally {
            cleanUp();
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException ex) {
                // Already shutting down; the hook runs cleanUp again, which is harmless.
            }
        }
    }

    private void cleanUp() {
        File portFile = portFileRef.getAndSet(null);
        if (portFile != null && !portFile.delete()) {
            getLog().debug("Could not delete " + portFile);
        }
        Process xvfb = xvfbRef.getAndSet(null);
        if (xvfb != null) {
            xvfb.destroy();
        }
    }

    @Override
    protected void configureSimulatorRequest(InvocationRequest request) throws MojoExecutionException {
        boolean explicit = getSession() != null
                && getSession().getUserProperties().getProperty("cn1.mcp.port") != null;
        if (!isPortFree(mcpPort)) {
            if (explicit) {
                throw new MojoExecutionException("Port " + mcpPort + " is already in use on 127.0.0.1. "
                        + "Stop whatever holds it, or pass another -Dcn1.mcp.port.");
            }
            int requested = mcpPort;
            mcpPort = freePort();
            getLog().warn("Port " + requested + " is in use by another process; using " + mcpPort + " instead.");
        }
        writePortFile();
        request.addShellEnvironment("CN1_MCP_PORT", String.valueOf(mcpPort));
        request.addShellEnvironment("CN1_AGENT_BACKGROUND", "1");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean linux = os.contains("linux");
        String display = System.getenv("DISPLAY");
        String wayland = System.getenv("WAYLAND_DISPLAY");
        if (linux && isEmpty(display) && isEmpty(wayland)) {
            request.addShellEnvironment("DISPLAY", startXvfb());
        }
        getLog().info("[cn1.mcp] agent mode: the simulator will serve MCP on 127.0.0.1:" + mcpPort
                + " -- drive it with `java .agent-skills/codename-one/tools/Sim.java snapshot` "
                + "(it reads the port from common/target/" + PORT_FILE + ")");
    }

    private void writePortFile() {
        File dir = getCN1ProjectDir();
        if (dir == null) {
            return;
        }
        File target = new File(dir, "target");
        if (!target.isDirectory() && !target.mkdirs()) {
            return;
        }
        File portFile = new File(target, PORT_FILE);
        try {
            Files.write(portFile.toPath(), String.valueOf(mcpPort).getBytes(StandardCharsets.US_ASCII));
            portFileRef.set(portFile);
        } catch (IOException ex) {
            getLog().warn("Could not write " + portFile + ": " + ex.getMessage());
        }
    }

    private static boolean isPortFree(int port) {
        try (ServerSocket s = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static int freePort() throws MojoExecutionException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return s.getLocalPort();
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not find a free loopback port", ex);
        }
    }

    private String startXvfb() throws MojoExecutionException {
        int number = 99;
        // A display that is starting, or one that crashed, may have only its lock file.
        while (number < 200 && (new File("/tmp/.X11-unix/X" + number).exists()
                || new File("/tmp/.X" + number + "-lock").exists())) {
            number++;
        }
        String display = ":" + number;
        final Process xvfb;
        try {
            xvfb = new ProcessBuilder("Xvfb", display, "-screen", "0", "1920x1080x24", "-nolisten", "tcp")
                    .redirectErrorStream(true)
                    .redirectOutput(new File("/dev/null"))
                    .start();
            xvfbRef.set(xvfb);
        } catch (IOException ex) {
            throw new MojoExecutionException("This machine has no display and Xvfb could not be started ("
                    + ex.getMessage() + "). Install it (Debian/Ubuntu: apt-get install xvfb; "
                    + "Fedora: dnf install xorg-x11-server-Xvfb) or run with DISPLAY set.", ex);
        }
        File socket = new File("/tmp/.X11-unix/X" + number);
        for (int i = 0; i < 100 && !socket.exists(); i++) {
            if (!xvfb.isAlive()) {
                throw new MojoExecutionException("Xvfb exited with status " + xvfb.exitValue()
                        + " before display " + display + " came up.");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new MojoExecutionException("Interrupted waiting for Xvfb", ex);
            }
        }
        if (!socket.exists()) {
            xvfb.destroy();
            throw new MojoExecutionException("Xvfb did not open display " + display + " within 10 seconds.");
        }
        getLog().info("No display found; started Xvfb on " + display);
        return display;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
