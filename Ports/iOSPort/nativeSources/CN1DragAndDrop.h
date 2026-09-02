/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

#ifndef CN1DragAndDrop_h
#define CN1DragAndDrop_h

#import <Foundation/Foundation.h>
#include "TargetConditionals.h"
#import "CN1AppleUI.h"

/*
 * Native drag and drop for the UIKit ports: iOS, iPadOS and Mac Catalyst.
 *
 * UIKit owns the drag gesture. UIDragInteraction has its own recognizer -- a long press on a
 * phone, a lift on a trackpad -- and asks the delegate what is being dragged at the moment it
 * fires. So the framework cannot start a session on its own drag threshold the way the desktop
 * port does; instead it stages the operation on the press (CN1PrepareNativeDrag) and this file
 * calls back into Java when UIKit decides a drag has begun.
 *
 * The payload itself is fetched at that later moment rather than on the press, because a drag
 * that may carry a file the application has not written yet must not write it every time the
 * user merely touches the component.
 *
 * watchOS, tvOS and the native AppKit port have no UIDragInteraction; there the whole file
 * compiles to the unsupported answers below and the framework keeps its lightweight in-form
 * drag and drop.
 */

/// True when this build can drag through the operating system at all.
BOOL CN1DragAndDropSupported(void);

/// True when a drag started here can be dropped outside the application. On iPadOS and Mac
/// Catalyst it can; on iPhone a drag stays inside the application, because there is no second
/// application on screen to drop it into.
BOOL CN1DragOutsideAppSupported(void);

/// Remembers the Codename One surface, so the interactions can be attached to it later.
///
/// Neither interaction is attached here. UIDragInteraction recognizes its gesture with a
/// recognizer installed on the view, and installing one changes how every touch on that view is
/// delivered -- a plain tap stopped reaching the framework at all. The overwhelming majority of
/// applications never drag anything and must not pay for that; the drop half is withheld on the
/// same principle rather than on measurement.
///
/// The parameter degrades to `id` on watchOS, which has no CN1View at all -- WatchKit draws
/// through WKInterface objects and CN1AppleUI.h deliberately leaves the alias undefined there.
/// CN1RenderingView's peer argument does the same thing for the same reason. Naming the type
/// unconditionally broke every watch build, since this header reaches that slice too.
#if TARGET_OS_WATCH
void CN1InstallDragAndDrop(id view);
#else
void CN1InstallDragAndDrop(CN1View* view);
#endif

/// Attaches the drag interaction, because the application has a component that can be dragged
/// out. Idempotent, and safe to call from any thread.
void CN1EnableNativeDragSource(void);

/// Attaches the drop interaction, because the application has a component that accepts drops.
/// Idempotent, and safe to call from any thread.
void CN1EnableNativeDropTarget(void);

/// Stages the drag a press has made possible: which representations it can offer, what the
/// receiver may do with them, and the image to show under the finger. The representations are
/// named but not built -- CN1AddNativeDragPayload delivers the bytes once the drag really
/// starts.
///
/// mimeTypes is newline separated.
void CN1PrepareNativeDrag(NSString* mimeTypes, int allowedActions, NSData* dragImagePng,
                          int touchX, int touchY);

/// Clears the payload, ready for the representations of the session UIKit has just started,
/// and records the id the framework has given that session.
///
/// The id travels with every load handler this session registers, so a representation read
/// late -- after the user has begun another drag -- is resolved against the operation it
/// belongs to rather than whatever is being dragged by then.
void CN1BeginNativeDragPayload(int sessionId);

/// Names a representation the drag can offer without producing it.
///
/// The bytes are fetched through CN1NativeDragDeliverResolve when a receiver actually reads
/// that type, which is what setDataProvider promises: beginning a drag and abandoning it must
/// not build anything. Every MIME type the operation advertises is declared here, rather than a
/// fixed list of the framework's own -- an operation carrying only `text/markdown` was
/// advertised and never forwarded, so the drag began with no items and UIKit cancelled it.
void CN1DeclareNativeDragPayload(NSString* mimeType);

/// Adds the file list, which is the one representation that cannot be deferred: UIKit needs the
/// number of items when the session begins, and that is the number of files.
///
/// `paths` is newline separated.
void CN1AddNativeDragFiles(NSString* paths);

/// Drops whatever CN1PrepareNativeDrag staged, because the press turned out to be a tap.
void CN1CancelNativeDrag(void);

/*
 * The Java side of the bridge. Defined in IOSNative.m, where every piece of ParparVM thread
 * state handling lives, so that this file stays plain UIKit.
 */

/// Reports a drag moving over the surface and returns the action a drop would perform, or 0.
int CN1NativeDragDeliverOver(int x, int y, NSString* mimeTypes, int allowedActions, BOOL entering);

/// Reports a drag leaving the surface.
void CN1NativeDragDeliverExit(void);

/// Starts a drop, clearing whatever a previous one left.
void CN1NativeDragDeliverDropBegin(void);

/// Adds one representation to the drop being assembled. Every representation UIKit loaded goes
/// through here rather than a fixed list, so a drag carrying markdown, a GIF or an
/// application's own type is not accepted while it hovers and then found empty at the drop --
/// which would refuse the very target that agreed to take it.
///
/// `text` and `binary` are alternatives; `application/x-file-list` arrives as newline separated
/// paths in `text`.
void CN1NativeDragDeliverDropAdd(NSString* mimeType, NSString* text, NSData* binary);

/// Adds a representation of the drop that is backed by a file already on disk.
///
/// A provider that vends a file commonly advertises the document's own content type as well.
/// Loading that as data would read the whole document into memory on top of the copy this
/// already makes -- fatal for a large one -- so the type is named against the copy instead and
/// read only if a target asks for it.
void CN1NativeDragDeliverDropAddFile(NSString* mimeType, NSString* path);

/// Delivers the assembled drop and returns the action actually accepted, or 0 when nothing took
/// it.
int CN1NativeDragDeliverDropCommit(int x, int y, int action);

/// Produces one representation of a drag on demand. Returns nil when the operation that
/// session belongs to can no longer supply it.
NSData* CN1NativeDragDeliverResolve(NSString* mimeType, int sessionId);

/// Announces that UIKit has started a drag session. Returns the actions the framework's staged
/// operation allows, or 0 when it has none -- in which case no drag begins. The Java side calls
/// CN1BeginNativeDragPayload and CN1AddNativeDragPayload from inside this call.
int CN1NativeDragDeliverSessionStarted(void);

/// Reports the outcome of a session this application started.
void CN1NativeDragDeliverCompleted(int action);

#endif
