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
        // Modern browsers fire BOTH ``pointerdown`` AND ``mousedown`` for the
        // same user click (pointer events first, then a follow-up mouse event
        // for backwards compat). Registering the SAME listener for both fires
        // it twice per real click. ``HTML5Implementation.onMouseDown`` /
        // ``onMouseUp`` try to dedupe via a stateful ``mouseDown`` flag
        // (``shouldIgnoreMousePress`` + ``!isMouseDown()`` early-returns), but
        // the dedup gets out of sync — ``mouseDown`` ends up cleared by one
        // event-pair half before the matching opposite half can run, so on
        // the JS port a Dialog OK click can land on a press whose release
        // gets dropped (or vice-versa). Net effect: the modal Dialog never
        // disposes, ``invokeAndBlock`` blocks the EDT forever, the UI freezes
        // — see PR #4795 dialog-freeze repro.
        //
        // Fix: register ONLY pointer events. Every browser this port supports
        // (Chrome 55+, Edge, Firefox 59+, Safari 13+) ships pointer events;
        // they cover mouse, touch, and pen input in one event family. The
        // legacy ``mousedown`` / ``mouseup`` registrations are redundant
        // and were the cause of the dedup race.
        if (mouseDownEnabled) {
            registrar.add("pointerdown", mouseDown, true);
        }
        registrar.add("hittest", hitTest, true);
        if (mouseUpEnabled) {
            registrar.add("pointerup", mouseUp, true);
            // ``pointercancel`` is the pointer-events equivalent of
            // ``mouseout`` for the click-aborted case (e.g. browser takes
            // focus elsewhere mid-drag); keep that side-channel so a stuck
            // ``mouseDown`` flag can still recover.
            registrar.add("pointercancel", mouseUp, true);
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
