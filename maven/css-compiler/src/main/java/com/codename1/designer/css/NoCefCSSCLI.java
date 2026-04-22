/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 *
 * Minimal CLI entry point for compiling a CSS theme to a .res file with the
 * strictNoCef flag enabled. Unlike CN1CSSCLI (in the Designer module),
 * this class does NOT
 * pull in any JavaSE port classes or initialize a BrowserComponent host --
 * it is designed to live in a thin "css-compiler" jar that depends only on
 * codenameone-core, flute, and sac.
 *
 * The intended consumer is the native-themes build, which generates the
 * shipped platform themes from CSS source. Any CSS rule that would require
 * CEF-backed image rasterization fails the compile (see CSSTheme.enforceNoCef).
 *
 * Usage:
 *   java -jar codenameone-css-compiler-jar-with-dependencies.jar \
 *        -input path/to/theme.css \
 *        -output path/to/Theme.res
 */
package com.codename1.designer.css;

import java.io.File;
import java.net.URL;

public class NoCefCSSCLI {

    public static void main(String[] args) throws Exception {
        if (hasArg(args, "help") || hasArg(args, "h") || args.length == 0) {
            printUsage();
            return;
        }
        String inputPath = getArg(args, "input", "i");
        String outputPath = getArg(args, "output", "o");
        if (inputPath == null || outputPath == null) {
            printUsage();
            System.exit(1);
        }

        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        if (!inputFile.exists()) {
            System.err.println("Input CSS file does not exist: " + inputFile.getAbsolutePath());
            System.exit(2);
        }
        File parent = outputFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("Could not create output directory: " + parent.getAbsolutePath());
            System.exit(3);
        }

        CSSTheme.strictNoCef = true;
        System.out.println("Compiling " + inputFile.getName() + " -> " + outputFile.getName() + " (no-cef)");

        URL url = inputFile.toURI().toURL();
        CSSTheme theme = CSSTheme.load(url);
        if (theme == null) {
            System.err.println("CSSTheme.load returned null for " + inputFile
                    + " - parser probably failed to initialize. See stderr above for details.");
            System.exit(4);
        }
        theme.cssFile = inputFile;
        theme.resourceFile = outputFile;

        // createImageBorders walks every rule. With strictNoCef=true it
        // throws an IllegalStateException listing any rule that would need
        // CEF rasterization before the unreachable webview path would run.
        // The provider below is a safety net: if a post-enforceNoCef code
        // path still asks for a WebView, fail loud instead of NPE.
        theme.createImageBorders(new CSSTheme.WebViewProvider() {
            @Override
            public com.codename1.ui.BrowserComponent getWebView() {
                throw new IllegalStateException(
                        "CSS compile in no-cef mode must not request a WebView. "
                        + "enforceNoCef should have rejected the offending rule; "
                        + "please report this bug.");
            }
        });

        theme.updateResources();
        theme.save(outputFile);
        System.out.println("Wrote " + outputFile.getAbsolutePath());
    }

    private static boolean hasArg(String[] args, String... names) {
        return getArg(args, names) != null;
    }

    private static String getArg(String[] args, String... names) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null) {
                continue;
            }
            if (a.startsWith("-")) {
                String key = a.substring(1);
                while (key.startsWith("-")) {
                    key = key.substring(1);
                }
                int eq = key.indexOf('=');
                String value;
                if (eq >= 0) {
                    value = key.substring(eq + 1);
                    key = key.substring(0, eq);
                } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    value = args[i + 1];
                } else {
                    value = "true";
                }
                for (String n : names) {
                    if (n.equals(key)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("Codename One CSS Compiler (no-cef, native-themes build)");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar codenameone-css-compiler-<version>-jar-with-dependencies.jar \\");
        System.out.println("       -input <path/to/theme.css> \\");
        System.out.println("       -output <path/to/Theme.res>");
        System.out.println();
        System.out.println("Any CSS rule requiring CEF-backed image rasterization (box-shadow,");
        System.out.println("border-radius combined with visible border, filter, complex gradients)");
        System.out.println("fails the compile with the list of offending rules.");
    }
}
