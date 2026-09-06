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
import com.codename1.io.Log;
import com.codename1.io.Util;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Detects installed MCP hosts and registers a Codename One application's stdio MCP
/// server with them, so an end user can point Claude Desktop, Claude Code, Codex and
/// similar tools at the application without editing config by hand.
///
/// Two config shapes are written: the JSON `mcpServers` object the Claude hosts use, and
/// the `[mcp_servers.<name>]` tables Codex keeps in `~/.codex/config.toml` (see
/// [MCPToml]). Either way the user's other servers and settings are preserved, and a
/// config that cannot be edited safely is reported and left alone.
///
/// This is a plain reusable API. It is meant to be driven by Codename One tooling (the
/// certificate wizard, Game Builder, Settings, the simulator) and by applications
/// themselves.
///
/// Registration is a desktop concern. File access goes through
/// {@link com.codename1.io.FileSystemStorage} so the class links on every target, but
/// {@link #isSupported()} is false where the platform provides no reachable home
/// directory (mobile), and each detected host reports whether its config is writable.
public final class MCPClientRegistrar {
    private static final MCPClientRegistrar INSTANCE = new MCPClientRegistrar();

    /// A host whose config this class cannot write. The caller surfaces it as a manual step.
    private static final int FORMAT_MANUAL = 0;
    /// A JSON config with an `mcpServers` object at the root.
    private static final int FORMAT_JSON = 1;
    /// A TOML config with one `[mcp_servers.<name>]` table per server.
    private static final int FORMAT_TOML = 2;

    /// The Windows path is relative to the HOME directory, as a dotfile config is.
    private static final boolean WIN_UNDER_HOME = false;
    /// The Windows path is relative to %APPDATA%, as an installed application's own
    /// per user directory is.
    private static final boolean WIN_UNDER_APP_DATA = true;

    private final List<KnownClient> knownClients = new ArrayList<KnownClient>();

    private MCPClientRegistrar() {
        // Table driven registry: adding a host whose config is one of the shapes below is
        // a data change.
        // The Windows column is relative to %APPDATA% for a host that keeps a per user
        // application directory there, and to the HOME directory for one whose config is a
        // dotfile. Getting that wrong is silent: the host is simply never detected, and the
        // registrar has nothing to write to. Claude Desktop is an Electron app and really
        // does live under %APPDATA%\Claude; the dotfile hosts do not.
        knownClients.add(new KnownClient("claude-desktop", "Claude Desktop",
                "Library/Application Support/Claude/claude_desktop_config.json",
                "Claude/claude_desktop_config.json", WIN_UNDER_APP_DATA,
                ".config/Claude/claude_desktop_config.json", FORMAT_JSON));
        knownClients.add(new KnownClient("claude-code", "Claude Code",
                ".claude.json", ".claude.json", WIN_UNDER_HOME, ".claude.json", FORMAT_JSON));
        // Codex keeps its servers as [mcp_servers.<name>] tables in a TOML file that the
        // ChatGPT desktop app, the Codex CLI and the Codex IDE extension all share, so one
        // writer serves all three. CODEX_HOME defaults to ~/.codex on every platform,
        // which on Windows is %USERPROFILE%, not %APPDATA%.
        knownClients.add(new KnownClient("codex", "Codex",
                ".codex/config.toml", ".codex/config.toml", WIN_UNDER_HOME,
                ".codex/config.toml", FORMAT_TOML));
        // Detect only for now: opencode nests its servers in an "mcp" block whose entries
        // have a different shape, so it needs a writer of its own. It is surfaced so the
        // caller can guide the user manually.
        knownClients.add(new KnownClient("opencode", "opencode",
                ".config/opencode/opencode.json",
                "opencode/opencode.json", WIN_UNDER_APP_DATA,
                ".config/opencode/opencode.json", FORMAT_MANUAL));
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
            boolean present = safeExists(fs, fsPath(path));
            if (!present) {
                // The config file itself is absent. A present parent directory can still
                // signal the host is installed (e.g. .../Application Support/Claude/), but
                // only when that parent is a dedicated subdirectory. A dotfile such as
                // ~/.claude.json has the home directory as its parent, which exists on
                // every desktop and would otherwise be a false positive.
                String parent = parentOf(path);
                if (parent != null && !samePath(parent, home)) {
                    present = safeExists(fs, fsPath(parent));
                }
            }
            if (present) {
                found.add(new MCPClient(known.id, known.displayName, path, known.format));
            }
        }
        return found;
    }

    /// Where the named host's config would live under the given home on THIS platform, or
    /// null when the id is unknown. Package private: it exists so a test can pin each
    /// host's per platform path convention without a filesystem, which is the only way a
    /// wrong Windows base shows up - the failure is otherwise silent, the host simply
    /// never being detected.
    String configPathFor(String id, String home) {
        for (KnownClient known : knownClients) {
            if (known.id.equals(id)) {
                return known.absolutePath(home);
            }
        }
        return null;
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
            if (writeEntry(client, descriptor.getServerName(), descriptor)) {
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

    /// Writes or removes one server entry in a host config, in whichever format that host
    /// uses. A null descriptor removes the entry.
    private boolean writeEntry(MCPClient client, String serverName, MCPClientDescriptor descriptor) {
        if (client.format == FORMAT_TOML) {
            return writeTomlEntry(client, serverName, descriptor);
        }
        return writeJsonEntry(client, serverName, descriptor);
    }

    private boolean writeJsonEntry(MCPClient client, String serverName, MCPClientDescriptor descriptor) {
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String path = client.getConfigPath();
            String storagePath = fsPath(path);
            Map<String, Object> root;
            if (safeExists(fs, storagePath)) {
                root = readExistingConfig(fs, storagePath);
                if (root == null) {
                    // The file is there but could not be read, or is not a complete JSON
                    // object (a truncated or corrupt config). Codename One's parser is
                    // lenient and would hand back a partial map, so rewriting would drop
                    // the user's other settings. Leave the file untouched instead.
                    Log.p("MCP: leaving " + path + " unchanged; it could not be safely parsed");
                    return false;
                }
            } else {
                if (descriptor == null) {
                    // Nothing to remove from a config that does not exist.
                    return false;
                }
                root = new LinkedHashMap<String, Object>();
            }
            Object serversObj = root.get("mcpServers");
            Map<String, Object> servers;
            if (serversObj instanceof Map) {
                servers = asStringMap((Map) serversObj);
            } else {
                servers = new LinkedHashMap<String, Object>();
            }
            if (descriptor == null) {
                servers.remove(serverName);
            } else {
                servers.put(serverName, descriptor.toServerEntry());
            }
            root.put("mcpServers", servers);
            // mapToJson preserves booleans, integers and null values, so the user's other
            // settings survive the round trip; toJson would drop null-valued entries.
            return writeConfigAtomic(fs, path, storagePath, MCPJson.toJson(root));
        } catch (Throwable ex) {
            Log.e(ex);
            return false;
        }
    }

    /// Edits a Codex style TOML config. The document is rewritten as text with only the
    /// one `[mcp_servers.<name>]` table replaced, so the user's other servers, settings,
    /// comments and formatting are left exactly as they were. A document the editor
    /// cannot make sense of is reported and left untouched.
    private boolean writeTomlEntry(MCPClient client, String serverName, MCPClientDescriptor descriptor) {
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String path = client.getConfigPath();
            String storagePath = fsPath(path);
            String existing;
            if (safeExists(fs, storagePath)) {
                existing = readExistingText(fs, storagePath);
                if (existing == null) {
                    Log.p("MCP: leaving " + path + " unchanged; it could not be read");
                    return false;
                }
            } else {
                if (descriptor == null) {
                    // Nothing to remove from a config that does not exist.
                    return false;
                }
                existing = "";
            }
            MCPToml.Result result = MCPToml.applyServerEntry(existing, serverName, descriptor);
            if (!result.isApplied()) {
                Log.p("MCP: leaving " + path + " unchanged; " + result.getProblem());
                return false;
            }
            if (result.getText().equals(existing)) {
                // Removing an entry the config never had. Report nothing was updated
                // rather than rewriting the file to itself.
                return false;
            }
            return writeConfigAtomic(fs, path, storagePath, result.getText());
        } catch (Throwable ex) {
            Log.e(ex);
            return false;
        }
    }

    /// Reads an existing host config, returning null when it cannot be trusted so the
    /// caller refuses to overwrite it. An empty file is treated as a fresh, empty config.
    private Map<String, Object> readExistingConfig(FileSystemStorage fs, String storagePath) {
        try {
            String json = Util.readToString(fs.openInputStream(storagePath), "UTF-8");
            String trimmed = json.trim();
            if (trimmed.length() == 0) {
                return new LinkedHashMap<String, Object>();
            }
            if (!isCompleteJsonObject(trimmed)) {
                return null;
            }
            // Parse faithfully so rewriting keeps booleans, integers and nulls intact.
            return MCPJson.parse(json);
        } catch (Throwable ex) {
            Log.e(ex);
            return null;
        }
    }

    /// Reads a host config as text, returning null when it cannot be read so the caller
    /// refuses to overwrite it.
    private String readExistingText(FileSystemStorage fs, String storagePath) {
        try {
            return Util.readToString(fs.openInputStream(storagePath), "UTF-8");
        } catch (Throwable ex) {
            Log.e(ex);
            return null;
        }
    }

    /// Lightweight structural check that the text is a single, complete JSON object with
    /// balanced braces, brackets and quotes. Codename One's JSON parser does not throw on
    /// malformed input (it returns a partial map), so this guards against silently writing
    /// over a truncated or corrupt config.
    static boolean isCompleteJsonObject(String s) {
        int n = s.length();
        if (n < 2 || s.charAt(0) != '{' || s.charAt(n - 1) != '}') {
            return false;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0 && !inString;
    }

    /// Writes the config through a temporary file that is renamed into place, so an
    /// interrupted write can never truncate the user's existing config.
    private boolean writeConfigAtomic(FileSystemStorage fs, String path, String storagePath,
                                      String content) {
        try {
            String parent = parentOf(path);
            if (parent != null) {
                try {
                    fs.mkdir(fsPath(parent));
                } catch (Throwable ignored) {
                    // parent likely already exists
                }
            }
            String fileName = fileNameOf(path);
            String tmpName = fileName + ".cn1mcp-tmp";
            String tmpPath = parent == null ? fsPath(tmpName) : fsPath(parent + "/" + tmpName);
            byte[] data = content.getBytes("UTF-8");
            OutputStream os = fs.openOutputStream(tmpPath);
            try {
                os.write(data);
            } finally {
                os.close();
            }
            // rename() takes a bare name and moves within the same directory. renameTo
            // cannot overwrite an existing target on every platform, so remove it first;
            // the temporary file is already fully written, so there is no truncation risk.
            if (safeExists(fs, storagePath)) {
                try {
                    fs.delete(storagePath);
                } catch (Throwable ignored) {
                    // fall through and let rename report the real failure
                }
            }
            fs.rename(tmpPath, fileName);
            return true;
        } catch (Throwable ex) {
            Log.e(ex);
            return false;
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

    private static String fileNameOf(String path) {
        if (path == null) {
            return null;
        }
        String forward = path.replace('\\', '/');
        int slash = forward.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    /// True when two paths refer to the same location, ignoring separator style and
    /// trailing separators.
    private static boolean samePath(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return normalizePath(a).equals(normalizePath(b));
    }

    private static String normalizePath(String p) {
        String forward = p.replace('\\', '/');
        while (forward.length() > 1 && forward.charAt(forward.length() - 1) == '/') {
            forward = forward.substring(0, forward.length() - 1);
        }
        return forward;
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

    /// %APPDATA% for the given home, derived from the home directory rather than read from
    /// the APPDATA environment variable: System.getenv is not available on every Codename
    /// One target, and this class lives in the portable core, so it must link everywhere.
    ///
    /// Takes the home as an argument rather than reading it back, so a path is a pure
    /// function of the home it is resolved against and cannot half-follow one home and
    /// half-follow another.
    private static String appDataPath(String home) {
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
        /// One of the FORMAT_ constants. Kept package private: which writer a host needs
        /// is the registrar's business, and callers only ever ask whether it is writable.
        final int format;

        MCPClient(String id, String displayName, String configPath, int format) {
            this.id = id;
            this.displayName = displayName;
            this.configPath = configPath;
            this.format = format;
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
            return format != FORMAT_MANUAL;
        }
    }

    private static final class KnownClient {
        private final String id;
        private final String displayName;
        private final String macRelative;
        private final String winRelative;
        private final boolean winUnderAppData;
        private final String linuxRelative;
        private final int format;

        KnownClient(String id, String displayName, String macRelative, String winRelative,
                    boolean winUnderAppData, String linuxRelative, int format) {
            this.id = id;
            this.displayName = displayName;
            this.macRelative = macRelative;
            this.winRelative = winRelative;
            this.winUnderAppData = winUnderAppData;
            this.linuxRelative = linuxRelative;
            this.format = format;
        }

        String absolutePath(String home) {
            if (isWindows()) {
                if (!winUnderAppData) {
                    return home + "/" + winRelative;
                }
                String base = appDataPath(home);
                return base == null ? null : base + "/" + winRelative;
            }
            if (isMac()) {
                return home + "/" + macRelative;
            }
            return home + "/" + linuxRelative;
        }
    }
}
