package com.codename1.settings.extensions;

public final class ExtensionDescriptor {
    private final String name;
    private final String description;
    private final MavenDependency dependency;
    private final boolean mavenCentral;
    private final String fileName;
    private final String link;
    private final String license;
    private final String platforms;
    private final String author;
    private final String tags;
    private final String dependencies;
    private final String version;
    private final String status;
    private final String warning;

    public ExtensionDescriptor(String name, String description, MavenDependency dependency, boolean mavenCentral) {
        this(name, description, dependency, mavenCentral, "", "", "", "", "", "", "", "", "", "");
    }

    public ExtensionDescriptor(String name, String description, MavenDependency dependency, boolean mavenCentral,
            String fileName, String link, String license, String platforms, String author,
            String tags, String dependencies, String version) {
        this(name, description, dependency, mavenCentral, fileName, link, license, platforms, author,
                tags, dependencies, version, "", "");
    }

    public ExtensionDescriptor(String name, String description, MavenDependency dependency, boolean mavenCentral,
            String fileName, String link, String license, String platforms, String author,
            String tags, String dependencies, String version, String status, String warning) {
        this.name = clean(name);
        this.description = clean(description);
        this.dependency = dependency;
        this.mavenCentral = mavenCentral;
        this.fileName = clean(fileName);
        this.link = clean(link);
        this.license = clean(license);
        this.platforms = clean(platforms);
        this.author = clean(author);
        this.tags = clean(tags);
        this.dependencies = clean(dependencies);
        this.version = clean(version);
        this.status = clean(status);
        this.warning = clean(warning);
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public MavenDependency dependency() {
        return dependency;
    }

    public boolean isMavenCentral() {
        return mavenCentral;
    }

    public String fileName() {
        return fileName;
    }

    public String link() {
        return link;
    }

    public String license() {
        return license;
    }

    public String platforms() {
        return platforms;
    }

    public String author() {
        return author;
    }

    public String tags() {
        return tags;
    }

    public String dependencies() {
        return dependencies;
    }

    public String version() {
        return version;
    }

    public String status() {
        return status;
    }

    public String warning() {
        return warning;
    }

    public boolean hasCompatibilityWarning() {
        return warning.length() > 0 || "outdated".equalsIgnoreCase(status)
                || "unsupported".equalsIgnoreCase(status);
    }

    public ExtensionDescriptor withCompatibilityFallback(ExtensionDescriptor fallback) {
        if (fallback == null || hasCompatibilityWarning() || !fallback.hasCompatibilityWarning()) {
            return this;
        }
        return new ExtensionDescriptor(name, description, dependency, mavenCentral,
                fileName, link, license, platforms, author, tags, dependencies, version,
                fallback.status(), fallback.warning());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
