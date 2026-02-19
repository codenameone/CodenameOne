package com.codename1.initializr.model;

public enum IDE {
    INTELLIJ("/idea.zip"),
    ECLIPSE("/eclipse.zip"),
    NETBEANS("/netbeans.zip"),
    VS_CODE("/vscode.zip");

    public final String ZIP;

    IDE(String zip) {
        ZIP = zip;
    }
}
