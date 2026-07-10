package com.codename1.settings.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettingsProperties {
    public static final String BUILD_HINT_PREFIX = "codename1.arg.";

    private final String path;
    private final Map<String, String> properties = new LinkedHashMap<String, String>();
    private String originalText = "";
    private boolean modified;

    public SettingsProperties(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public boolean isModified() {
        return modified;
    }

    public void load() throws IOException {
        originalText = read(path);
        properties.clear();
        if (originalText.length() > 0) {
            parseProperties(originalText, properties);
        }
        modified = false;
    }

    public String get(String key) {
        return get(key, "");
    }

    public String get(String key, String def) {
        String v = properties.get(key);
        return v == null ? def : v;
    }

    public void set(String key, String value) {
        properties.put(key, value == null ? "" : value);
        modified = true;
    }

    public void remove(String key) {
        properties.remove(key);
        modified = true;
    }

    public void setBuildHint(String hint, String value) {
        set(fullBuildHintKey(hint), value);
    }

    public String getBuildHint(String hint) {
        return get(fullBuildHintKey(hint));
    }

    public void removeBuildHint(String hint) {
        remove(fullBuildHintKey(hint));
    }

    public Set<String> keys() {
        return properties.keySet();
    }

    public List<String> buildHintKeys() {
        ArrayList<String> out = new ArrayList<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith(BUILD_HINT_PREFIX)) {
                out.add(key.substring(BUILD_HINT_PREFIX.length()));
            }
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public int buildHintCount() {
        return buildHintKeys().size();
    }

    public void save() throws IOException {
        write(path, merge(originalText, properties));
        originalText = read(path);
        modified = false;
    }

    public static String fullBuildHintKey(String hint) {
        if (hint == null) {
            return BUILD_HINT_PREFIX;
        }
        return hint.startsWith(BUILD_HINT_PREFIX) ? hint : BUILD_HINT_PREFIX + hint;
    }

    static String merge(String original, Map<String, String> values) {
        String[] lines = original == null ? new String[0] : original.replace("\r\n", "\n").split("\n");
        StringBuilder out = new StringBuilder(original == null ? 256 : original.length() + 256);
        Map<String, String> remaining = new LinkedHashMap<String, String>();
        remaining.putAll(values);
        for (String line : lines) {
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && !trimmed.startsWith("#")) {
                String key = trimmed.substring(0, eq).trim();
                if (remaining.containsKey(key)) {
                    out.append(key).append('=').append(escape(remaining.get(key))).append('\n');
                    remaining.remove(key);
                    continue;
                }
            }
            if (line.length() > 0) {
                out.append(line).append('\n');
            }
        }
        ArrayList<String> keys = new ArrayList<String>();
        for (String key : remaining.keySet()) {
            keys.add(key);
        }
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
        for (String key : keys) {
            out.append(key).append('=').append(escape(remaining.get(key))).append('\n');
        }
        return out.toString();
    }

    private static void parseProperties(String text, Map<String, String> out) {
        String[] lines = text.replace("\r\n", "\n").split("\n");
        String pendingKey = null;
        StringBuilder pendingValue = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            if (pendingKey != null) {
                boolean continued = trimmed.endsWith("\\");
                pendingValue.append(continued ? trimmed.substring(0, trimmed.length() - 1) : trimmed);
                if (!continued) {
                    out.put(pendingKey, unescape(pendingValue.toString()));
                    pendingKey = null;
                    pendingValue.setLength(0);
                }
                continue;
            }
            int split = separatorIndex(line);
            if (split <= 0) {
                continue;
            }
            String key = line.substring(0, split).trim();
            String value = line.substring(split + 1).trim();
            boolean continued = value.endsWith("\\");
            if (continued) {
                pendingKey = key;
                pendingValue.append(value.substring(0, value.length() - 1));
            } else {
                out.put(key, unescape(value));
            }
        }
        if (pendingKey != null) {
            out.put(pendingKey, unescape(pendingValue.toString()));
        }
    }

    private static int separatorIndex(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '=' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    private static String read(String settingsPath) throws IOException {
        if (settingsPath == null || settingsPath.length() == 0) {
            return "";
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String url = ProjectIO.fsUrl(settingsPath);
        if (!fs.exists(url)) {
            return "";
        }
        InputStream in = null;
        try {
            in = fs.openInputStream(url);
            return Util.readToString(in, "UTF-8");
        } finally {
            Util.cleanup(in);
        }
    }

    private static void write(String settingsPath, String text) throws IOException {
        OutputStream out = null;
        try {
            out = FileSystemStorage.getInstance().openOutputStream(ProjectIO.fsUrl(settingsPath));
            out.write(text.getBytes("UTF-8"));
            out.flush();
        } finally {
            Util.cleanup(out);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped) {
                if (c == '\\') {
                    escaped = true;
                } else {
                    out.append(c);
                }
                continue;
            }
            switch (c) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                default -> out.append(c);
            }
            escaped = false;
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }
}
