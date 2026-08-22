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

/*
 * Additional desktop windows for the Mac Catalyst slice of the iOS port.
 *
 * The whole implementation is inside #if TARGET_OS_MACCATALYST, so the object
 * file an iPhone or iPad build produces from CN1MacWindows.m is empty and the
 * plain iOS binary is byte-for-byte what it was.
 *
 * A Codename One Window becomes a UIWindowScene. The scene hosts a plain UIView
 * whose layer contents are set from a raster the framework renders on its own
 * side, rather than a second Metal or GL surface. That is deliberate: the render
 * path caches its device, pipeline state and glyph atlas against the one
 * rendering view, and making those per-scene is a large refactor of the hottest
 * code in the product -- with manual retain and release, since this port builds
 * without ARC. Because the scene still owns a real UIView hierarchy, native peers
 * and native text editing work normally inside a window; only the Codename One
 * drawing arrives as a bitmap.
 *
 * Multiple scenes must be enabled in Info.plist for any of this to work. That key
 * is process-wide and changing it once destabilised the Catalyst screenshot
 * suite, so the builder only emits it when an application explicitly asks for
 * multi-window through the macNative.multiWindow build hint.
 */

#ifndef CN1_MAC_WINDOWS_H
#define CN1_MAC_WINDOWS_H

#import <Foundation/Foundation.h>
#include <TargetConditionals.h>

#if TARGET_OS_MACCATALYST

#import <UIKit/UIKit.h>

/* Creates a window scene and returns its slot, or -1 on failure. windowId is the
 * framework's own id, stored so every callback can echo it back. */
int CN1MacWindowCreate(int windowId, NSString* title, int x, int y, int width, int height,
        BOOL decorated, BOOL resizable, BOOL positionSet);
void CN1MacWindowDestroy(int slot);
void CN1MacWindowShow(int slot, BOOL visible);
void CN1MacWindowSetTitle(int slot, NSString* title);
void CN1MacWindowSetBounds(int slot, int x, int y, int width, int height);
void CN1MacWindowGetBounds(int slot, int* out);
int  CN1MacWindowGetWidth(int slot);
int  CN1MacWindowGetHeight(int slot);
void CN1MacWindowFocus(int slot);
void CN1MacWindowSetState(int slot, int state);

/* Presents one frame. The bytes are premultiplied BGRA in the window's own size,
 * which is what the framework's mutable image hands back. */
void CN1MacWindowPresent(int slot, void* argb, int width, int height);

/* The UIView a native peer or text editor should be added to. */
UIView* CN1MacWindowContentView(int slot);

/* True when the app's Info.plist actually enables multiple scenes. This is the
 * single source of truth for whether windows can work: without the key the
 * system refuses to activate a second scene, so the API must report unsupported
 * rather than hand back windows that never appear. */
BOOL CN1MacMultiWindowSupported(void);

int CN1MacMonitorCount(void);
int CN1MacPrimaryMonitor(void);
void CN1MacMonitorBounds(int monitor, BOOL workArea, int* out);
int CN1MacMonitorDpi(int monitor);
double CN1MacMonitorScale(int monitor);
int CN1MacMonitorForWindow(int slot);

/** The screen the application's own scene is on; the main window has no slot. */
int CN1MacMonitorForMainWindow(void);

/** Applies a resizability change to a window that may already have a scene. */
void CN1MacWindowSetResizable(int slot, BOOL resizable);

/** Applies a decoration change to a window that may already have a scene. */
void CN1MacWindowSetDecorated(int slot, BOOL decorated);

/** Records a minimum size and applies it to an existing scene. */
void CN1MacWindowSetMinimumSize(int slot, int width, int height);

/** Records which window is being edited, so the native editor lands in its view. */
void CN1MacWindowSetEditingSlot(int slot);

/** The view the native editor belongs in, or nil for the application's main view. */
UIView* CN1MacWindowEditingHostView(void);

/* Invoked from the scene delegate when a Codename One window scene connects, so
 * a scene the system restored on launch is adopted rather than orphaned. */
void CN1MacWindowSceneConnected(UIWindowScene* scene);

/* Claims a newly connected scene for a Codename One window if one is waiting for
 * it. Returns NO when the scene belongs to the application's main form. */
BOOL CN1MacWindowAdoptScene(UIWindowScene* scene);

/** Requests a scene again for a window whose scene was destroyed unasked. */
BOOL CN1MacWindowReopen(int slot);

/** Enables or disables touch input for a window, used while a modal blocks it. */
void CN1MacWindowSetInputEnabled(int slot, BOOL enabled);
void CN1MacMainWindowSetInputEnabled(BOOL enabled);
BOOL CN1MacMainWindowGetBounds(int* out);

/** Starts reporting display attach/remove/mode changes; idempotent. */
void CN1MacWindowWatchScreens(void);

/** The window id a scene belongs to, or -1 for the application's main scene. */
int CN1MacWindowIdForScene(UIWindowScene* scene);

/** The scene of a Codename One window disconnected; reported as a close request. */
void CN1MacWindowSceneDisconnected(UIWindowScene* scene);

#endif /* TARGET_OS_MACCATALYST */

#endif /* CN1_MAC_WINDOWS_H */
