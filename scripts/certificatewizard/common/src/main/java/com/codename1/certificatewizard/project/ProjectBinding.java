package com.codename1.certificatewizard.project;

public final class ProjectBinding {
    private String projectDir;
    private String settings;
    private String outputDir;
    private String user;
    private String token;
    private String baseUrl;

    public String projectDir() {
        return projectDir;
    }

    public String settings() {
        return settings;
    }

    public String outputDir() {
        return outputDir;
    }

    public String user() {
        return user;
    }

    public String token() {
        return token;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public boolean isValid() {
        return settings != null && !settings.isEmpty();
    }

    public static ProjectBinding parse(String content) {
        ProjectBinding b = new ProjectBinding();
        if (content == null) {
            return b;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String val = trimmed.substring(eq + 1).trim();
            switch (key) {
                case "projectDir" -> b.projectDir = val;
                case "settings" -> b.settings = val;
                case "outputDir" -> b.outputDir = val;
                case "user" -> b.user = val;
                case "token" -> b.token = val;
                case "baseUrl" -> b.baseUrl = val;
                default -> {
                }
            }
        }
        return b;
    }
}
