package com.codename1.settings.hints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildHintCatalog {
    private final Map<String, BuildHintMetadata> hints = new LinkedHashMap<String, BuildHintMetadata>();

    public Collection<BuildHintMetadata> all() {
        return hints.values();
    }

    public BuildHintMetadata get(String name) {
        return hints.get(name);
    }

    public boolean contains(String name) {
        return hints.containsKey(name);
    }

    public List<BuildHintMetadata> search(String query) {
        ArrayList<BuildHintMetadata> out = new ArrayList<BuildHintMetadata>();
        for (BuildHintMetadata hint : hints.values()) {
            if (hint.matches(query)) {
                out.add(hint);
            }
        }
        return out;
    }

    public void add(BuildHintMetadata hint) {
        if (hint != null && hint.name() != null && hint.name().trim().length() > 0) {
            hints.put(hint.name().trim(), hint);
        }
    }

    public static BuildHintCatalog fromAsciiDoc(String asciidoc) {
        BuildHintCatalog catalog = new BuildHintCatalog();
        if (asciidoc == null) {
            return fallback();
        }
        String[] lines = asciidoc.replace("\r\n", "\n").split("\n");
        boolean inTable = false;
        boolean buildHintTable = false;
        String currentName = null;
        StringBuilder currentDescription = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim();
            if ("|===".equals(line)) {
                if (inTable) {
                    if (buildHintTable) {
                        flush(catalog, currentName, currentDescription.toString());
                        break;
                    }
                    inTable = false;
                } else {
                    inTable = true;
                    buildHintTable = false;
                    currentName = null;
                    currentDescription.setLength(0);
                }
                continue;
            }
            if (!inTable) {
                continue;
            }
            if (line.startsWith("//")) {
                continue;
            }
            if (!buildHintTable) {
                String header = line.startsWith("|") ? line.substring(1).trim() : line;
                if (header.startsWith("Name") && header.contains("|Description")) {
                    buildHintTable = true;
                }
                continue;
            }
            if (line.startsWith("|")) {
                String cell = line.substring(1).trim();
                if (currentName == null) {
                    currentName = cell;
                    currentDescription.setLength(0);
                } else if (currentDescription.length() == 0) {
                    currentDescription.append(cell);
                } else {
                    flush(catalog, currentName, currentDescription.toString());
                    currentName = cell;
                    currentDescription.setLength(0);
                }
            } else if (currentName != null && line.length() > 0) {
                if (currentDescription.length() > 0) {
                    currentDescription.append(' ');
                }
                currentDescription.append(line);
            }
        }
        if (catalog.hints.isEmpty()) {
            return fallback();
        }
        return catalog;
    }

    private static void flush(BuildHintCatalog catalog, String rawName, String description) {
        if (rawName == null || rawName.trim().length() == 0) {
            return;
        }
        for (String name : splitNames(rawName)) {
            catalog.add(new BuildHintMetadata(name, description, inferType(name, description), inferPlatform(name)));
        }
    }

    private static List<String> splitNames(String raw) {
        ArrayList<String> names = new ArrayList<String>();
        String normalized = raw.replace("`", "").replace("(a.k.a.", "/").replace(")", "");
        String[] parts = normalized.split(",");
        for (String part : parts) {
            String[] slashParts = part.split("/");
            for (String slashPart : slashParts) {
                String name = slashPart.trim();
                if (name.indexOf(' ') >= 0 || name.length() == 0 || name.startsWith("(")) {
                    continue;
                }
                names.add(name);
            }
        }
        return names.isEmpty() ? Collections.singletonList(raw.trim()) : names;
    }

    private static BuildHintType inferType(String name, String description) {
        String n = name.toLowerCase();
        String d = description == null ? "" : description.toLowerCase();
        if ("java.version".equals(name)) {
            return BuildHintType.INTEGER;
        }
        if ("android.targetSDKVersion".equals(name)) {
            return BuildHintType.INTEGER;
        }
        if ("android.useAndroidX".equals(name)) {
            return BuildHintType.BOOLEAN;
        }
        if ("build.cn1Version".equals(name) || "ios.bundleVersion".equals(name)) {
            return BuildHintType.VERSION;
        }
        if (n.contains("password") || n.contains("secret") || n.contains("token")) {
            return BuildHintType.SECRET;
        }
        if (n.contains("certificate") || n.contains("provision") || n.contains("sdkroot") || d.contains("path to")) {
            return BuildHintType.PATH;
        }
        if (n.contains("url") || d.contains("https://") || d.contains("http://")) {
            return BuildHintType.URL;
        }
        if (d.contains("true/false") || d.contains("boolean true/false") || d.contains("`true`") || d.contains("`false`")) {
            return BuildHintType.BOOLEAN;
        }
        if (d.contains("comma") || d.contains("comma-delimited") || d.contains("comma delimited")) {
            return BuildHintType.CSV;
        }
        if (d.contains("<") && d.contains(">") || n.contains("xml") || n.contains("plistinject") || n.contains("xpermissions")) {
            return BuildHintType.XML;
        }
        if (n.contains("version") || d.contains("version")) {
            return BuildHintType.VERSION;
        }
        if (d.contains("can be ") || d.contains("supported values") || d.contains("accepts ")) {
            return BuildHintType.ENUM;
        }
        if (d.contains("integer") || d.contains("size in bytes") || n.endsWith("port")) {
            return BuildHintType.INTEGER;
        }
        return BuildHintType.TEXT;
    }

    private static String inferPlatform(String name) {
        if (name.startsWith("android.") || name.startsWith("and.")) {
            return "android";
        }
        if (name.startsWith("ios.")) {
            return "ios";
        }
        if (name.startsWith("macNative.") || name.startsWith("codename1.mac.") || name.startsWith("desktop.mac.")) {
            return "mac";
        }
        if (name.startsWith("windows.") || name.startsWith("win.")) {
            return "windows";
        }
        if (name.startsWith("linux.")) {
            return "linux";
        }
        if (name.startsWith("javascript.")) {
            return "javascript";
        }
        if (name.startsWith("desktop.")) {
            return "desktop";
        }
        return "general";
    }

    public static BuildHintCatalog fallback() {
        BuildHintCatalog catalog = new BuildHintCatalog();
        catalog.add(new BuildHintMetadata("build.cn1Version", "Pins the cloud build to a released Codename One version such as 7.0.250, or master.", BuildHintType.VERSION, "general"));
        catalog.add(new BuildHintMetadata("java.version", "Build server Java version.", BuildHintType.INTEGER, "general"));
        catalog.add(new BuildHintMetadata("android.debug", "Whether to include an Android debug build.", BuildHintType.BOOLEAN, "android"));
        catalog.add(new BuildHintMetadata("android.release", "Whether to include an Android release build.", BuildHintType.BOOLEAN, "android"));
        catalog.add(new BuildHintMetadata("android.xpermissions", "Additional Android manifest permissions XML.", BuildHintType.XML, "android"));
        catalog.add(new BuildHintMetadata("ios.bundleVersion", "Version number of the generated iOS bundle.", BuildHintType.VERSION, "ios"));
        catalog.add(new BuildHintMetadata("ios.deployment_target", "Minimum iOS version.", BuildHintType.VERSION, "ios"));
        catalog.add(new BuildHintMetadata("ios.plistInject", "Raw XML injected into the iOS Info.plist.", BuildHintType.XML, "ios"));
        catalog.add(new BuildHintMetadata("macNative.distribution", "Mac native distribution: appStore, developerID, or both.", BuildHintType.ENUM, "mac"));
        catalog.add(new BuildHintMetadata("windows.signing.timestampUrl", "RFC 3161 timestamp server URL for Windows signing.", BuildHintType.URL, "windows"));
        catalog.add(new BuildHintMetadata("desktop.width", "Desktop window width.", BuildHintType.INTEGER, "desktop"));
        catalog.add(new BuildHintMetadata("desktop.height", "Desktop window height.", BuildHintType.INTEGER, "desktop"));
        return catalog;
    }
}
