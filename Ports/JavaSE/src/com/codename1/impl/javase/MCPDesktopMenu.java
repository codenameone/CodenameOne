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
package com.codename1.impl.javase;

import com.codename1.mcp.MCP;
import com.codename1.mcp.MCPClientDescriptor;
import com.codename1.mcp.MCPClientRegistrar;
import com.codename1.mcp.MCPVerbosity;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

/**
 * The native "MCP" pull-down menu that the JavaSE port adds to every desktop Codename One
 * tool (and the simulator). It lets a user expose the running tool to an LLM agent, install
 * or remove the tool from the MCP hosts on the machine, and control how much of the MCP
 * conversation is logged for debugging.
 *
 * The tools serve MCP over a loopback socket; {@link MCPStdioLauncher} bridges a host's
 * stdio to that socket, so "Install" makes the running tool drivable from stdio hosts such
 * as Claude Desktop, Codex and opencode.
 */
public final class MCPDesktopMenu {
    private static final int DEFAULT_PORT = 8765;

    private MCPDesktopMenu() {
    }

    /**
     * Builds the "MCP" menu for the given tool name. Safe to call on the AWT thread.
     *
     * @param toolName human-readable tool name used as the server name in host configs
     * @param anchor   a component used to anchor dialogs (may be null)
     */
    public static JMenu build(final String toolName, final Component anchor) {
        JMenu menu = new JMenu("MCP");

        final JCheckBoxMenuItem serve = new JCheckBoxMenuItem("Expose This Tool To Agents",
                MCP.isRunning());
        serve.setToolTipText("Serve MCP on 127.0.0.1:" + DEFAULT_PORT + " so an agent can drive this tool");
        serve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (serve.isSelected()) {
                    MCP.startSocketServer(DEFAULT_PORT);
                } else {
                    MCP.stop();
                }
            }
        });
        menu.add(serve);
        menu.addSeparator();

        JMenuItem install = new JMenuItem("Install in MCP Hosts...");
        install.setToolTipText("Register this tool with Claude Desktop and other detected MCP hosts");
        install.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!MCP.isRunning()) {
                    MCP.startSocketServer(DEFAULT_PORT);
                    serve.setSelected(true);
                }
                doInstall(toolName, anchor);
            }
        });
        menu.add(install);

        JMenuItem uninstall = new JMenuItem("Remove From MCP Hosts...");
        uninstall.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doUninstall(toolName, anchor);
            }
        });
        menu.add(uninstall);

        JMenuItem detect = new JMenuItem("Detect MCP Hosts...");
        detect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDetect(anchor);
            }
        });
        menu.add(detect);
        menu.addSeparator();

        JMenu verbose = new JMenu("Debug Logging");
        ButtonGroup group = new ButtonGroup();
        addVerbosity(verbose, group, "Off", MCPVerbosity.OFF);
        addVerbosity(verbose, group, "Errors only", MCPVerbosity.ERRORS);
        addVerbosity(verbose, group, "Summary (one line per call)", MCPVerbosity.SUMMARY);
        addVerbosity(verbose, group, "Full (every request and response)", MCPVerbosity.FULL);
        menu.add(verbose);

        return menu;
    }

    private static void addVerbosity(JMenu menu, ButtonGroup group, String label, final MCPVerbosity level) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, MCP.getVerbosity() == level);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MCP.setVerbosity(level);
            }
        });
        group.add(item);
        menu.add(item);
    }

    private static MCPClientDescriptor bridgeDescriptor(String toolName) {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        List<String> args = new ArrayList<String>();
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add("com.codename1.impl.javase.MCPStdioLauncher");
        args.add("--attach");
        args.add(String.valueOf(DEFAULT_PORT));
        return new MCPClientDescriptor(serverName(toolName), javaBin, args);
    }

    private static String serverName(String toolName) {
        String base = toolName == null || toolName.length() == 0 ? "codename1-tool" : toolName;
        return "cn1-" + base.toLowerCase().replace(' ', '-');
    }

    private static void doInstall(String toolName, Component anchor) {
        try {
            MCPClientRegistrar registrar = MCPClientRegistrar.getInstance();
            List<MCPClientRegistrar.MCPClient> updated = registrar.register(bridgeDescriptor(toolName));
            StringBuilder sb = new StringBuilder();
            if (updated.isEmpty()) {
                sb.append("No auto-configurable MCP hosts were found.\n")
                        .append("Use 'Detect MCP Hosts' to see what is installed.");
            } else {
                sb.append("Installed '").append(serverName(toolName)).append("' in:\n\n");
                for (int i = 0; i < updated.size(); i++) {
                    sb.append("  ").append(updated.get(i).getDisplayName()).append('\n');
                }
                sb.append("\nRestart the host and this tool will appear as an MCP server.");
            }
            JOptionPane.showMessageDialog(anchor, sb.toString(), "MCP Install", JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(anchor, "Install failed: " + t.getMessage(),
                    "MCP Install", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void doUninstall(String toolName, Component anchor) {
        try {
            List<MCPClientRegistrar.MCPClient> updated =
                    MCPClientRegistrar.getInstance().unregister(serverName(toolName));
            String msg = updated.isEmpty() ? "No matching MCP host entries were found."
                    : "Removed '" + serverName(toolName) + "' from " + updated.size() + " host(s).";
            JOptionPane.showMessageDialog(anchor, msg, "MCP Remove", JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(anchor, "Remove failed: " + t.getMessage(),
                    "MCP Remove", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void doDetect(Component anchor) {
        List<MCPClientRegistrar.MCPClient> clients = MCPClientRegistrar.getInstance().detectClients();
        StringBuilder sb = new StringBuilder();
        if (clients.isEmpty()) {
            sb.append("No MCP hosts detected on this machine.");
        } else {
            sb.append("Detected MCP hosts:\n\n");
            for (int i = 0; i < clients.size(); i++) {
                MCPClientRegistrar.MCPClient c = clients.get(i);
                sb.append(c.getDisplayName())
                        .append(c.isWritable() ? " (auto-configurable)" : " (manual config)")
                        .append("\n  ").append(c.getConfigPath()).append("\n");
            }
        }
        JOptionPane.showMessageDialog(anchor, sb.toString(), "MCP Hosts", JOptionPane.INFORMATION_MESSAGE);
    }
}
