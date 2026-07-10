package com.codename1.settings.extensions;

public final class MavenDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;

    public MavenDependency(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, "");
    }

    public MavenDependency(String groupId, String artifactId, String version, String type) {
        this.groupId = groupId == null ? "" : groupId;
        this.artifactId = artifactId == null ? "" : artifactId;
        this.version = version == null ? "" : version;
        this.type = type == null ? "" : type;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public String type() {
        return type;
    }

    public boolean isValid() {
        return groupId.length() > 0 && artifactId.length() > 0 && version.length() > 0;
    }

    public String coordinates() {
        return groupId + ":" + artifactId + ":" + version + (type.length() == 0 ? "" : ":" + type);
    }
}
