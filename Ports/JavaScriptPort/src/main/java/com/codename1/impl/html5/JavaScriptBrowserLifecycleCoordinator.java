/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptBrowserLifecycleCoordinator {
    private JavaScriptBrowserLifecycleCoordinator() {
    }

    public interface InboxHooks {
        void stopPropagation();
        void preventDefault();
        void callSerially(Runnable runnable);
        void dispatchMessage(String message, int code);
    }

    public interface BackNavigationHooks {
        void callSerially(Runnable runnable);
        void runBackCommand();
    }

    public interface PasteHooks {
        void copyPlainText(String text);
        void copyHtmlText(String html);
        void copyFiles(String[] paths);
        void firePasteEvent();
    }

    public interface BacksideHooks {
        void installBacksideHooksInUserInteraction();
    }

    public static void handleInboxEvent(final InboxHooks hooks, final String detailString, final int eventCode) {
        hooks.stopPropagation();
        hooks.preventDefault();
        hooks.callSerially(new Runnable() {
            @Override
            public void run() {
                hooks.dispatchMessage(detailString, eventCode);
            }
        });
    }

    public static void handlePopState(final BackNavigationHooks hooks) {
        hooks.callSerially(new Runnable() {
            @Override
            public void run() {
                hooks.runBackCommand();
            }
        });
    }

    public static boolean handlePaste(PasteHooks hooks, String plainText, String htmlText, String[] filePaths) {
        if (plainText != null && plainText.length() > 0) {
            hooks.copyPlainText(plainText);
            hooks.firePasteEvent();
            return true;
        }
        if (htmlText != null && htmlText.length() > 0) {
            hooks.copyHtmlText(htmlText);
            hooks.firePasteEvent();
            return true;
        }
        if (filePaths != null && filePaths.length > 0) {
            hooks.copyFiles(filePaths);
            hooks.firePasteEvent();
            return true;
        }
        return false;
    }

    public static void handleInstallBacksideHooks(BacksideHooks hooks) {
        hooks.installBacksideHooksInUserInteraction();
    }
}
