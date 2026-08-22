package com.codename1.settings.hints;

import java.util.Collections;
import java.util.List;

public final class BuildHintMetadata {
    private final String name;
    private final String description;
    private final BuildHintType type;
    private final String platform;
    private final List<String> values;
    private final String defaultValue;
    private final String annotation;

    public BuildHintMetadata(String name, String description, BuildHintType type, String platform) {
        this(name, description, type, platform, null, null, null);
    }

    /**
     * @param values the closed value domain, or null when the hint is free-form
     * @param defaultValue the builder's own default, or null when it has none
     * @param annotation the annotation attribute that sets this hint, e.g.
     *                   {@code @Ios(pods)}, or null when it has none
     */
    public BuildHintMetadata(String name, String description, BuildHintType type, String platform,
                             List<String> values, String defaultValue, String annotation) {
        this.name = name;
        this.description = description == null ? "" : description.trim();
        this.type = type == null ? BuildHintType.TEXT : type;
        this.platform = platform == null ? "general" : platform;
        this.values = values == null || values.isEmpty()
                ? Collections.<String>emptyList() : Collections.unmodifiableList(values);
        this.defaultValue = defaultValue;
        this.annotation = annotation;
    }

    /** The accepted values, or empty when the hint is free-form. */
    public List<String> values() {
        return values;
    }

    /** The builder's own default, or null. */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * The annotation attribute that sets this hint, or null when the hint has no
     * checked form yet. Editing such a hint here is not wrong, but the annotation
     * is the form the compiler validates.
     */
    public String annotation() {
        return annotation;
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
