package com.codename1.initializr.model;

public enum Template {
    BAREBONES(false, false, "/barebones-src.zip", "/barebones-css.zip", "/barebones-pom.xml", null, "com.example.myapp", "MyAppName", null),
    KOTLIN(true, false, "/kotlin-src.zip", "/barebones-css.zip", "/kotlin-pom.xml", null, "com.example.myapp", "MyAppName", null),
    GRUB(false, true, "/grub-src.zip", "/grub-css.zip", "/grub-pom.xml", "/grub-cn1libs.zip", "com.codename1.demos.grub", "Grub", "grub.png"),
    TWEET(false, true, "/tweet-src.zip", "/tweet-css.zip", "/tweet-pom.xml", null, "com.example.myapp", "MyAppName", "tweet.png");

    public final String IMAGE_NAME;
    public final boolean IS_KOTLIN;
    public final boolean USES_CODERAD;
    public final String SOURCE_ZIP;
    public final String CSS;
    public final String POM_XML;
    public final String CN1LIB_ZIP;
    public final String SOURCE_PACKAGE;
    public final String SOURCE_MAIN_CLASS;

    Template(boolean isKotlin, boolean usesCodeRad, String sourceZip, String css, String pomXml, String cn1libZip, String sourcePackage, String sourceMainClass,
             String imageName) {
        IS_KOTLIN = isKotlin;
        USES_CODERAD = usesCodeRad;
        SOURCE_ZIP = sourceZip;
        CSS = css;
        POM_XML = pomXml;
        CN1LIB_ZIP = cn1libZip;
        SOURCE_PACKAGE = sourcePackage;
        SOURCE_MAIN_CLASS = sourceMainClass;
        IMAGE_NAME = imageName;
    }
}
