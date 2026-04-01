/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptEventWiring {
    private JavaScriptEventWiring() {
    }

    public interface WindowRegistrar {
        void add(String eventName, Object listener, boolean capture);
    }

    public interface DocumentRegistrar {
        void add(String eventName, Object listener);
    }

    public interface ElementRegistrar {
        void add(String eventName, Object listener, boolean capture);
    }

    public static void registerCoreWindowEvents(WindowRegistrar registrar, boolean hoverEnabled,
                                                Object cn1Inbox, Object popstate, Object resize, Object hover,
                                                Object installBacksideHooks, Object keydown, Object keyup, Object keypress) {
        registrar.add("cn1inbox", cn1Inbox, false);
        registrar.add("popstate", popstate, true);
        registrar.add("resize", resize, false);
        if (hoverEnabled) {
            registrar.add("mousemove", hover, false);
        }
        registrar.add("installbacksidehooks", installBacksideHooks, false);
        registrar.add("keydown", keydown, false);
        registrar.add("keyup", keyup, false);
        registrar.add("keypress", keypress, false);
    }

    public static void registerDocumentEvents(DocumentRegistrar registrar, Object paste) {
        registrar.add("paste", paste);
    }

    public static void registerPeerPointerEvents(ElementRegistrar registrar, boolean mouseDownEnabled, boolean mouseUpEnabled,
                                                 boolean touchStartEnabled, boolean touchEndEnabled, boolean wheelEnabled,
                                                 String wheelEventType, Object mouseDown, Object hitTest, Object mouseUp,
                                                 Object touchStart, Object touchEnd, Object wheel) {
        if (mouseDownEnabled) {
            registrar.add("mousedown", mouseDown, true);
            registrar.add("pointerdown", mouseDown, true);
        }
        registrar.add("hittest", hitTest, true);
        if (mouseUpEnabled) {
            registrar.add("mouseup", mouseUp, true);
            registrar.add("pointerup", mouseUp, true);
            registrar.add("mouseout", mouseUp, true);
            registrar.add("pointerout", mouseUp, true);
        }
        if (touchStartEnabled) {
            registrar.add("touchstart", touchStart, true);
        }
        if (touchEndEnabled) {
            registrar.add("touchend", touchEnd, true);
        }
        if (wheelEnabled) {
            registrar.add(wheelEventType, wheel, true);
        }
    }
}
