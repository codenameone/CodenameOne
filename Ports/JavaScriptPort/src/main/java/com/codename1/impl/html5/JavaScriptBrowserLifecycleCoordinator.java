/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
