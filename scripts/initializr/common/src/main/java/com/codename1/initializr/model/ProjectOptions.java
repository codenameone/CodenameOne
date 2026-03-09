package com.codename1.initializr.model;

public final class ProjectOptions {
    public enum PreviewLanguage {
        ENGLISH("English", "en", false),
        FRENCH("Fran\u00e7ais", "fr", false),
        GERMAN("Deutsch", "de", false),
        SPANISH("Espa\u00f1ol", "es", false),
        ITALIAN("Italiano", "it", false),
        PORTUGUESE("Portugu\u00eas", "pt", false),
        DUTCH("Nederlands", "nl", false),
        CHINESE_SIMPLIFIED("\u4e2d\u6587 (\u7b80\u4f53)", "zh_CN", false),
        JAPANESE("\u65e5\u672c\u8a9e", "ja", false),
        KOREAN("\ud55c\uad6d\uc5b4", "ko", false),
        ARABIC("\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "ar", true),
        HEBREW("\u05e2\u05d1\u05e8\u05d9\u05ea", "he", true);

        public final String label;
        public final String bundleSuffix;
        public final boolean rtl;

        PreviewLanguage(String label, String bundleSuffix, boolean rtl) {
            this.label = label;
            this.bundleSuffix = bundleSuffix;
            this.rtl = rtl;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum ThemeMode {
        LIGHT,
        DARK
    }

    public enum Accent {
        DEFAULT,
        TEAL,
        BLUE,
        ORANGE
    }

    public enum JavaVersion {
        JAVA_8("Java 8"),
        JAVA_17_EXPERIMENTAL("Java 17 (Experimental)");

        public final String label;

        JavaVersion(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final ThemeMode themeMode;
    public final Accent accent;
    public final boolean roundedButtons;
    public final boolean includeLocalizationBundles;
    public final PreviewLanguage previewLanguage;
    public final JavaVersion javaVersion;

    public ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons,
                          boolean includeLocalizationBundles, PreviewLanguage previewLanguage,
                          JavaVersion javaVersion) {
        this.themeMode = themeMode;
        this.accent = accent;
        this.roundedButtons = roundedButtons;
        this.includeLocalizationBundles = includeLocalizationBundles;
        this.previewLanguage = previewLanguage == null ? PreviewLanguage.ENGLISH : previewLanguage;
        this.javaVersion = javaVersion == null ? JavaVersion.JAVA_8 : javaVersion;
    }

    public static ProjectOptions defaults() {
        return new ProjectOptions(ThemeMode.LIGHT, Accent.DEFAULT, true, true, PreviewLanguage.ENGLISH, JavaVersion.JAVA_8);
    }
}
