package com.codename1.designer.css;

/**
 * CSSTheme.load touches Display / Util, which need a CodenameOneImplementation
 * to be installed or they hand back nulls. Tests that drive the compiler
 * directly install the same minimal headless stub the no-cef CLI uses.
 *
 * @see NoCefCSSCLI
 */
final class HeadlessTestSupport {

    private HeadlessTestSupport() {}

    /**
     * Installs {@link HeadlessCssCompilerImplementation} into Display and Util.
     * Idempotent, so it is safe to call from every test class in the module.
     */
    static void installHeadlessImplementation() throws Exception {
        // Display.impl is package-private and there is no public installer, so
        // reflect into the field once. Util keeps its own copy of the
        // implementation reference which is settable through a public method.
        HeadlessCssCompilerImplementation stub = new HeadlessCssCompilerImplementation();
        Class<?> displayCls = Class.forName("com.codename1.ui.Display");
        java.lang.reflect.Field implField = displayCls.getDeclaredField("impl");
        implField.setAccessible(true);
        if (implField.get(null) == null) {
            implField.set(null, stub);
        }
        com.codename1.io.Util.setImplementation(stub);
    }
}
