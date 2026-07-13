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
package com.codename1.mcp;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Detects installed MCP hosts and registers a Codename One application's stdio MCP
/// server with them, so an end user can point Claude Desktop, Claude Code and similar
/// tools at the application without editing config by hand.
///
/// This is a plain reusable API. It is meant to be driven by Codename One tooling (the
/// certificate wizard, Game Builder, Settings, the simulator) and by applications
/// themselves, and it is exposed to the maven plugin as the `cn1:mcp-setup` goal.
///
/// Registration is a desktop concern. File access goes through
/// {@link com.codename1.io.FileSystemStorage} so the class links on every target, but
/// {@link #isSupported()} is false where the platform provides no reachable home
/// directory (mobile), and each detected host reports whether its config is writable.
public final class MCPClientRegistrar {
    private static final MCPClientRegistrar INSTANCE = new MCPClientRegistrar();

    private final List<KnownClient> knownClients = new ArrayList<KnownClient>();

    private MCPClientRegistrar() {
        // Table driven registry: adding a JSON, mcpServers style host is a data change.
        knownClients.add(new KnownClient("claude-desktop", "Claude Desktop",
                "Library/Application Support/Claude/claude_desktop_config.json",
                "Claude/claude_desktop_config.json",
                ".config/Claude/claude_desktop_config.json", true));
        knownClients.add(new KnownClient("claude-code", "Claude Code",
                ".claude.json", ".claude.json", ".claude.json", true));
        // Detect only for now: these hosts use non JSON or differently shaped configs
        // (Codex config.toml, opencode opencode.json "mcp" block) that need dedicated
        // writers. They are surfaced so the caller can guide the user manually.
        knownClients.add(new KnownClient("codex", "Codex CLI",
                ".codex/config.toml", ".codex/config.toml", ".codex/config.toml", false));
        knownClients.add(new KnownClient("opencode", "opencode",
                ".config/opencode/opencode.json",
                "opencode/opencode.json",
                ".config/opencode/opencode.json", false));
    }

    public static MCPClientRegistrar getInstance() {
        return INSTANCE;
    }

    /// Returns true when this platform exposes a home directory the registrar can reach.
    public boolean isSupported() {
        return homePath() != null;
    }

    /// Detects installed MCP hosts by looking for their config file or its parent
    /// directory under the user home.
    public List<MCPClient> detectClients() {
        List<MCPClient> found = new ArrayList<MCPClient>();
        String home = homePath();
        if (home == null) {
            return found;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        for (KnownClient known : knownClients) {
            String path = known.absolutePath(home);
            if (path == null) {
                continue;
            }
            boolean present = safeExists(fs, fsPath(path)) || safeExists(fs, fsPath(parentOf(path)));
            if (present) {
                found.add(new MCPClient(known.id, known.displayName, path, known.writable));
            }
        }
        return found;
    }

    /// Registers the descriptor with every detected, writable host. Returns the list of
    /// hosts that were updated.
    public List<MCPClient> register(MCPClientDescriptor descriptor) {
        return register(descriptor, detectClients());
    }

    /// Registers the descriptor with the given hosts. Non writable hosts are skipped.
    public List<MCPClient> register(MCPClientDescriptor descriptor, List<MCPClient> clients) {
        List<MCPClient> updated = new ArrayList<MCPClient>();
        if (descriptor == null || clients == null) {
            return updated;
        }
        for (MCPClient client : clients) {
            if (!client.isWritable()) {
                continue;
            }
            if (writeEntry(client, descriptor.getServerName(), descriptor.toServerEntry())) {
                updated.add(client);
            }
        }
        return updated;
    }

    /// Removes the named server entry from every detected, writable host. Returns the
    /// list of hosts that were updated.
    public List<MCPClient> unregister(String serverName) {
        List<MCPClient> updated = new ArrayList<MCPClient>();
        if (serverName == null) {
            return updated;
        }
        List<MCPClient> clients = detectClients();
        for (MCPClient client : clients) {
            if (client.isWritable() && writeEntry(client, serverName, null)) {
                updated.add(client);
            }
        }
        return updated;
    }

    private boolean writeEntry(MCPClient client, String serverName, Map<String, Object> entry) {
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String path = client.getConfigPath();
            String storagePath = fsPath(path);
            Map<String, Object> root = readJson(fs, storagePath);
            if (root == null) {
                root = new LinkedHashMap<String, Object>();
            }
            Object serversObj = root.get("mcpServers");
            Map<String, Object> servers;
            if (serversObj instanceof Map) {
                servers = asStringMap((Map) serversObj);
            } else {
                servers = new LinkedHashMap<String, Object>();
            }
            if (entry == null) {
                servers.remove(serverName);
            } else {
                servers.put(serverName, entry);
            }
            root.put("mcpServers", servers);
            String parent = parentOf(path);
            if (parent != null) {
                try {
                    fs.mkdir(fsPath(parent));
                } catch (Throwable ignored) {
                    // parent likely already exists
                }
            }
            OutputStream os = fs.openOutputStream(storagePath);
            try {
                os.write(JSONParser.toJson(root).getBytes("UTF-8"));
            } finally {
                os.close();
            }
            return true;
        } catch (Throwable ex) {
            Log.e(ex);
            return false;
        }
    }

    private Map<String, Object> readJson(FileSystemStorage fs, String storagePath) {
        try {
            if (!fs.exists(storagePath)) {
                return null;
            }
            String json = Util.readToString(fs.openInputStream(storagePath), "UTF-8");
            if (json.trim().length() == 0) {
                return null;
            }
            return JSONParser.parseJSON(json);
        } catch (Throwable ex) {
            Log.e(ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringMap(Map raw) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Object entryObj : raw.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    private static boolean safeExists(FileSystemStorage fs, String storagePath) {
        try {
            return storagePath != null && fs.exists(storagePath);
        } catch (Throwable ex) {
            return false;
        }
    }

    /// Converts an absolute OS path to the form {@link FileSystemStorage} expects. On a
    /// unix style absolute path this yields a `file://` URI that round trips through the
    /// JavaSE port; other paths are passed through for the exposed filesystem case.
    private static String fsPath(String absolute) {
        if (absolute == null) {
            return null;
        }
        String forward = absolute.replace('\\', '/');
        if (forward.startsWith("/")) {
            return "file://" + forward;
        }
        if (forward.length() > 1 && forward.charAt(1) == ':') {
            // Windows drive path such as C:/Users/...
            return "file:///" + forward;
        }
        return forward;
    }

    private static String parentOf(String path) {
        if (path == null) {
            return null;
        }
        String forward = path.replace('\\', '/');
        int slash = forward.lastIndexOf('/');
        if (slash <= 0) {
            return null;
        }
        return path.substring(0, slash);
    }

    private static String homePath() {
        try {
            String home = System.getProperty("user.home");
            if (home != null && home.length() > 0) {
                return home;
            }
        } catch (Throwable ignored) {
            // property access unavailable on this platform
        }
        return null;
    }

    private static String appDataPath() {
        // Derived from the home directory rather than the APPDATA environment variable:
        // System.getenv is not available on every Codename One target, and this class
        // lives in the portable core, so it must link everywhere.
        String home = homePath();
        return home == null ? null : home + "/AppData/Roaming";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("win") >= 0;
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("mac") >= 0;
    }

    /// A detected MCP host and where its config lives.
    public static final class MCPClient {
        private final String id;
        private final String displayName;
        private final String configPath;
        private final boolean writable;

        MCPClient(String id, String displayName, String configPath, boolean writable) {
            this.id = id;
            this.displayName = displayName;
            this.configPath = configPath;
            this.writable = writable;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getConfigPath() {
            return configPath;
        }

        /// True when the registrar can write this host's config automatically. False
        /// for hosts whose config format is not yet supported, which the caller should
        /// surface as a manual step.
        public boolean isWritable() {
            return writable;
        }
    }

    private static final class KnownClient {
        private final String id;
        private final String displayName;
        private final String macRelative;
        private final String winRelative;
        private final String linuxRelative;
        private final boolean writable;

        KnownClient(String id, String displayName, String macRelative, String winRelative,
                    String linuxRelative, boolean writable) {
            this.id = id;
            this.displayName = displayName;
            this.macRelative = macRelative;
            this.winRelative = winRelative;
            this.linuxRelative = linuxRelative;
            this.writable = writable;
        }

        String absolutePath(String home) {
            if (isWindows()) {
                String base = appDataPath();
                return base == null ? null : base + "/" + winRelative;
            }
            if (isMac()) {
                return home + "/" + macRelative;
            }
            return home + "/" + linuxRelative;
        }
    }
}
