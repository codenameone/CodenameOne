package com.codename1.settings.hints;

public final class BuildHintMetadata {
    private final String name;
    private final String description;
    private final BuildHintType type;
    private final String platform;

    public BuildHintMetadata(String name, String description, BuildHintType type, String platform) {
        this.name = name;
        this.description = description == null ? "" : description.trim();
        this.type = type == null ? BuildHintType.TEXT : type;
        this.platform = platform == null ? "general" : platform;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public BuildHintType type() {
        return type;
    }

    public String platform() {
        return platform;
    }

    public boolean matches(String query) {
        if (query == null || query.trim().length() == 0) {
            return true;
        }
        String q = query.toLowerCase();
        return name.toLowerCase().contains(q)
                || description.toLowerCase().contains(q)
                || platform.toLowerCase().contains(q)
                || type.name().toLowerCase().contains(q);
    }
}
