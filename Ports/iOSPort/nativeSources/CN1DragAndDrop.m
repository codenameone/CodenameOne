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

#import "CN1DragAndDrop.h"

#if !TARGET_OS_OSX && !TARGET_OS_WATCH && !TARGET_OS_TV
#if __has_include(<UniformTypeIdentifiers/UniformTypeIdentifiers.h>)
#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>
#endif
#endif

#define CN1_DND_ACTION_NONE 0
#define CN1_DND_ACTION_COPY 1
#define CN1_DND_ACTION_MOVE 2
#define CN1_DND_ACTION_LINK 4

#if TARGET_OS_OSX || TARGET_OS_WATCH || TARGET_OS_TV

// No UIDragInteraction on these targets. The framework asks first and falls back to its
// lightweight in-form drag and drop, so these answers are the whole implementation.

BOOL CN1DragAndDropSupported(void) {
    return NO;
}

BOOL CN1DragOutsideAppSupported(void) {
    return NO;
}

#if TARGET_OS_WATCH
void CN1InstallDragAndDrop(id view) {
}
#else
void CN1InstallDragAndDrop(CN1View* view) {
}
#endif

void CN1EnableNativeDragSource(void) {
}

void CN1EnableNativeDropTarget(void) {
}

void CN1PrepareNativeDrag(NSString* mimeTypes, int allowedActions, NSData* dragImagePng,
                          int touchX, int touchY) {
}

void CN1BeginNativeDragPayload(void) {
}

void CN1AddNativeDragPayload(NSString* mimeType, NSString* text, NSData* binary) {
}

void CN1CancelNativeDrag(void) {
}

#else

/// The device pixel scale, owned by the view controller. UIKit reports drag positions in
/// points and the framework works in pixels, exactly as the touch path does.
extern float scaleValue;

/// What the press staged: the representations the drag could offer, what a receiver may do
/// with them, and the preview. Named but not built -- see CN1DragAndDrop.h.
static NSArray* cn1PreparedMimes = nil;

/// What a receiver is allowed to do with the drag. Staged by the press and replaced by the
/// authoritative set when a session really starts; it is what a *local* drop session is told
/// the source allows. UIKit carries no action information for a session that arrived from
/// another application, so those are told copy -- see cn1AllowedActionsFor.
static int cn1SessionActions = CN1_DND_ACTION_NONE;
static UIImage* cn1PreparedPreview = nil;
static CGPoint cn1PreparedTouch;

/// The payload of the session UIKit is currently running, delivered by
/// CN1AddNativeDragPayload once the drag has actually begun. Keyed by uniform type identifier
/// so the item provider can register each one directly.
static NSMutableDictionary* cn1DragData = nil;
static NSMutableArray* cn1DragFileUrls = nil;

/// The last action the framework agreed to, reused when a drop arrives without one.
static int cn1LastDropAction = CN1_DND_ACTION_NONE;

/// True while this application is the source of the session in progress.
static BOOL cn1DraggingOut = NO;

/// A drop of this application's own session onto its own surface, still loading.
///
/// UIKit asks the source what happened -- dragInteraction:session:didEndWithOperation: -- as
/// soon as performDrop: returns, which is before the asynchronous loads that drop depends on
/// have answered. Reporting the operation UIKit proposed at that moment would tell a source
/// its data had been moved even when the framework went on to refuse the drop, and a source
/// that deletes on ACTION_MOVE would then delete data nothing ever received. So the completion
/// waits for the real answer, whichever of the two arrives first.
///
/// All three are read and written on the main thread only, by callbacks UIKit serializes.
static BOOL cn1LocalDropInFlight = NO;
static BOOL cn1EndDeferred = NO;
static int cn1LocalDropResult = -1;

/// The surface, remembered so the drag interaction can be attached later, and the delegate that
/// serves both interactions. The delegate outlives the surface, which lives for the life of the
/// process, and the interactions hold it weakly.
static CN1View* cn1DragSurface = nil;
static id cn1DragDelegate = nil;

/// The MIME types the framework names, mapped onto the uniform type identifiers UIKit and
/// every other application on the system speak.
static NSString* cn1UtiForMime(NSString* mime) {
    if ([mime isEqualToString:@"text/plain"]) {
        return @"public.utf8-plain-text";
    }
    if ([mime isEqualToString:@"text/html"]) {
        return @"public.html";
    }
    if ([mime isEqualToString:@"text/rtf"]) {
        return @"public.rtf";
    }
    if ([mime isEqualToString:@"text/markdown"]) {
        return @"net.daringfireball.markdown";
    }
    if ([mime isEqualToString:@"image/png"]) {
        return @"public.png";
    }
    if ([mime isEqualToString:@"image/jpeg"]) {
        return @"public.jpeg";
    }
    if ([mime isEqualToString:@"image/gif"]) {
        return @"com.compuserve.gif";
    }
    if ([mime isEqualToString:@"application/x-file-list"]) {
        return @"public.file-url";
    }
    if ([mime isEqualToString:@"text/uri-list"]) {
        return @"public.url";
    }
    // Anything else -- text/asciidoc, application/pdf, an application's own type -- still has
    // to travel. Ask the system to name it, and fall back to the MIME type itself, which is an
    // opaque identifier that a receiver which does not know it simply never asks for.
    // Returning nil here would drop a representation the application deliberately published,
    // and a drag whose *only* representation was one of those would begin with no items at all
    // and be cancelled on the spot.
#if __has_include(<UniformTypeIdentifiers/UniformTypeIdentifiers.h>)
    if (@available(iOS 14.0, *)) {
        UTType* type = [UTType typeWithMIMEType:mime];
        if (type != nil && type.identifier.length > 0) {
            return type.identifier;
        }
    }
#endif
    return mime;
}

static NSString* cn1MimeForUti(NSString* uti) {
    if ([uti isEqualToString:@"public.utf8-plain-text"] || [uti isEqualToString:@"public.plain-text"]
            || [uti isEqualToString:@"public.text"]) {
        return @"text/plain";
    }
    if ([uti isEqualToString:@"public.html"]) {
        return @"text/html";
    }
    if ([uti isEqualToString:@"public.rtf"]) {
        return @"text/rtf";
    }
    if ([uti isEqualToString:@"net.daringfireball.markdown"]) {
        return @"text/markdown";
    }
    if ([uti isEqualToString:@"public.png"]) {
        return @"image/png";
    }
    if ([uti isEqualToString:@"public.jpeg"]) {
        return @"image/jpeg";
    }
    if ([uti isEqualToString:@"com.compuserve.gif"]) {
        return @"image/gif";
    }
    if ([uti isEqualToString:@"public.file-url"]) {
        return @"application/x-file-list";
    }
    if ([uti isEqualToString:@"public.url"]) {
        return @"text/uri-list";
    }
    if ([uti containsString:@"/"]) {
        // One of ours: cn1UtiForMime falls back to the MIME type as the identifier.
        return uti;
    }
#if __has_include(<UniformTypeIdentifiers/UniformTypeIdentifiers.h>)
    if (@available(iOS 14.0, *)) {
        UTType* type = [UTType typeWithIdentifier:uti];
        if (type != nil && type.preferredMIMEType.length > 0) {
            return type.preferredMIMEType;
        }
    }
#endif
    // A dynamic or private identifier with no MIME equivalent. Naming it anyway would fill the
    // content with identifiers no drop target could match on.
    return nil;
}

/// What the source of this drop session allows.
///
/// A session this application started knows exactly, and using copy for it -- which is what
/// this did at first -- meant a move-only drag had no action in common with a move-only target
/// and could not be dropped at all, while a copy-or-move drag could only ever be proposed as a
/// copy, so no in-application reorder could report a move to its source.
///
/// A session from another application is a different matter: UIKit tells a drop interaction
/// nothing about what the far side permits, so copy is the only defensible reading -- and the
/// safe one, since proposing a move the source never offered would have it delete data on the
/// strength of our guess.
static int cn1AllowedActionsFor(id<UIDropSession> session) {
    if (session.localDragSession != nil && cn1SessionActions != CN1_DND_ACTION_NONE) {
        return cn1SessionActions;
    }
    return CN1_DND_ACTION_COPY;
}

/// One action out of a set, preferring a copy because it is the one that cannot destroy the
/// source's data.
static int cn1DefaultAction(int actions) {
    if ((actions & CN1_DND_ACTION_COPY) != 0) {
        return CN1_DND_ACTION_COPY;
    }
    if ((actions & CN1_DND_ACTION_MOVE) != 0) {
        return CN1_DND_ACTION_MOVE;
    }
    if ((actions & CN1_DND_ACTION_LINK) != 0) {
        return CN1_DND_ACTION_LINK;
    }
    return CN1_DND_ACTION_NONE;
}

static UIDropOperation cn1DropOperationFor(int action) {
    if ((action & CN1_DND_ACTION_MOVE) != 0) {
        return UIDropOperationMove;
    }
    if ((action & CN1_DND_ACTION_LINK) != 0) {
        // UIKit has no "link"; a receiver that wanted one still wants the transfer to happen,
        // and copy is the operation that says so without claiming the source loses its data.
        return UIDropOperationCopy;
    }
    if ((action & CN1_DND_ACTION_COPY) != 0) {
        return UIDropOperationCopy;
    }
    return UIDropOperationCancel;
}

/// The MIME types a drag in progress is offering, newline separated, derived from the type
/// identifiers alone. UIKit does not hand over the data until the drop and the framework only
/// needs the names in order to decide whether any component wants the drag.
static NSString* cn1MimesForSession(id<UIDropSession> session) {
    NSMutableArray* mimes = [NSMutableArray array];
    for (UIDragItem* item in session.items) {
        for (NSString* uti in item.itemProvider.registeredTypeIdentifiers) {
            NSString* mime = cn1MimeForUti(uti);
            if (mime != nil && ![mimes containsObject:mime]) {
                [mimes addObject:mime];
            }
        }
        // A provider that can vend a file is a file drag whatever else it also offers, which is
        // how a document dragged out of Files reaches a target that asked for files.
        if ([item.itemProvider hasItemConformingToTypeIdentifier:@"public.file-url"]
                && ![mimes containsObject:@"application/x-file-list"]) {
            [mimes addObject:@"application/x-file-list"];
        }
    }
    return [mimes componentsJoinedByString:@"\n"];
}

BOOL CN1DragAndDropSupported(void) {
    if (@available(iOS 11.0, *)) {
        return YES;
    }
    return NO;
}

BOOL CN1DragOutsideAppSupported(void) {
#if TARGET_OS_MACCATALYST
    if (@available(iOS 11.0, *)) {
        return YES;
    }
    return NO;
#else
    // iOS 15 brought drag and drop between applications to the phone -- hold the item with one
    // finger, switch applications with another, drop -- so from there on the idiom no longer
    // decides it. Before that a drag could only leave the application on an iPad, where a
    // second application can be on screen to receive it; on a phone the same session worked but
    // could only end on one of this application's own components.
    if (@available(iOS 15.0, *)) {
        return YES;
    }
    if (@available(iOS 11.0, *)) {
        return [UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad;
    }
    return NO;
#endif
}

void CN1PrepareNativeDrag(NSString* mimeTypes, int allowedActions, NSData* dragImagePng,
                          int touchX, int touchY) {
    NSArray* mimes = mimeTypes == nil || mimeTypes.length == 0
            ? nil : [mimeTypes componentsSeparatedByString:@"\n"];
#ifndef CN1_USE_ARC
    [cn1PreparedMimes release];
    [cn1PreparedPreview release];
#endif
    cn1PreparedMimes = mimes;
    cn1SessionActions = allowedActions;
    cn1PreparedPreview = dragImagePng == nil ? nil : [UIImage imageWithData:dragImagePng];
    cn1PreparedTouch = CGPointMake(touchX / scaleValue, touchY / scaleValue);
#ifndef CN1_USE_ARC
    [cn1PreparedMimes retain];
    [cn1PreparedPreview retain];
#endif
}

void CN1BeginNativeDragPayload(void) {
#ifndef CN1_USE_ARC
    [cn1DragData release];
    [cn1DragFileUrls release];
#endif
    cn1DragData = [[NSMutableDictionary alloc] init];
    cn1DragFileUrls = [[NSMutableArray alloc] init];
}

void CN1AddNativeDragPayload(NSString* mimeType, NSString* text, NSData* binary) {
    if (mimeType == nil || mimeType.length == 0 || cn1DragData == nil) {
        return;
    }
    if ([mimeType isEqualToString:@"application/x-file-list"]) {
        if (text == nil || text.length == 0) {
            return;
        }
        for (NSString* entry in [text componentsSeparatedByString:@"\n"]) {
            if (entry.length == 0) {
                continue;
            }
            // ClipboardContent's file representation permits a raw local path as well as a
            // file: URI, and URLWithString: turns a path into a scheme-less relative URL that
            // no receiver can open.
            NSURL* url = ([entry hasPrefix:@"/"] || [entry hasPrefix:@"~"])
                    ? [NSURL fileURLWithPath:[entry stringByExpandingTildeInPath]]
                    : [NSURL URLWithString:entry];
            if (url != nil) {
                [cn1DragFileUrls addObject:url];
            }
        }
        return;
    }
    NSData* data = binary;
    if (data == nil && text != nil) {
        data = [text dataUsingEncoding:NSUTF8StringEncoding];
    }
    if (data == nil || data.length == 0) {
        return;
    }
    NSString* uti = cn1UtiForMime(mimeType);
    if (uti != nil && [cn1DragData objectForKey:uti] == nil) {
        [cn1DragData setObject:data forKey:uti];
    }
}

void CN1CancelNativeDrag(void) {
#ifndef CN1_USE_ARC
    [cn1PreparedMimes release];
    [cn1PreparedPreview release];
#endif
    cn1PreparedMimes = nil;
    cn1PreparedPreview = nil;
    cn1SessionActions = CN1_DND_ACTION_NONE;
}

API_AVAILABLE(ios(11.0))
@interface CN1DragAndDropDelegate : NSObject <UIDragInteractionDelegate, UIDropInteractionDelegate>
@end

@implementation CN1DragAndDropDelegate

// ---- the drag out half ------------------------------------------------------------------

- (NSArray<UIDragItem *> *)dragInteraction:(UIDragInteraction *)interaction
                  itemsForBeginningSession:(id<UIDragSession>)session {
    if (cn1PreparedMimes == nil || cn1PreparedMimes.count == 0) {
        return @[];
    }
    // Asking the framework now, rather than on the press, is what lets a promised file stay
    // unwritten until a drag really happens. The Java side fills cn1DragData from inside this
    // call, one representation at a time, through CN1AddNativeDragPayload.
    int allowed = CN1NativeDragDeliverSessionStarted();
    if (allowed == CN1_DND_ACTION_NONE) {
        return @[];
    }
    // The authoritative set, which is what a local drop session is told the source allows.
    cn1SessionActions = allowed;
    cn1DraggingOut = YES;
    cn1LocalDropInFlight = NO;
    cn1EndDeferred = NO;
    cn1LocalDropResult = -1;

    NSMutableArray<UIDragItem *>* items = [NSMutableArray array];
    // Files first, one item each: a receiver that copies documents expects one item per
    // document, and collapsing several into one loses all but the first.
    for (NSURL* url in cn1DragFileUrls) {
        NSItemProvider* provider = [[NSItemProvider alloc] initWithContentsOfURL:url];
        if (provider == nil) {
            continue;
        }
        UIDragItem* item = [[UIDragItem alloc] initWithItemProvider:provider];
        [items addObject:item];
#ifndef CN1_USE_ARC
        [provider release];
        [item release];
#endif
    }
    if (cn1DragData.count > 0) {
        NSItemProvider* provider = [[NSItemProvider alloc] init];
        for (NSString* uti in cn1DragData) {
            NSData* payload = [cn1DragData objectForKey:uti];
#ifndef CN1_USE_ARC
            [payload retain];
#endif
            [provider registerDataRepresentationForTypeIdentifier:uti
                                                       visibility:NSItemProviderRepresentationVisibilityAll
                                                      loadHandler:^NSProgress *(void (^completion)(NSData *, NSError *)) {
                completion(payload, nil);
                return nil;
            }];
        }
        UIDragItem* item = [[UIDragItem alloc] initWithItemProvider:provider];
        [items addObject:item];
#ifndef CN1_USE_ARC
        [provider release];
        [item release];
#endif
    }
    if (items.count == 0) {
        cn1DraggingOut = NO;
        CN1NativeDragDeliverCompleted(CN1_DND_ACTION_NONE);
    }
    return items;
}

- (UIDragPreview *)dragInteraction:(UIDragInteraction *)interaction
                previewForLiftingItem:(UIDragItem *)item
                              session:(id<UIDragSession>)session {
    if (cn1PreparedPreview == nil) {
        // Without one UIKit snapshots the interaction's view, which is the whole Codename One
        // surface; nil here leaves UIKit to its default rather than dragging the entire screen.
        return nil;
    }
    UIImageView* view = [[UIImageView alloc] initWithImage:cn1PreparedPreview];
    UIDragPreview* preview = [[UIDragPreview alloc] initWithView:view];
#ifndef CN1_USE_ARC
    [view release];
    [preview autorelease];
#endif
    return preview;
}

- (void)dragInteraction:(UIDragInteraction *)interaction
                session:(id<UIDragSession>)session
    didEndWithOperation:(UIDropOperation)operation {
    cn1DraggingOut = NO;
    if (cn1LocalDropInFlight) {
        // The drop landed here and is still assembling. Its answer is the true one, so the
        // completion goes out when it arrives rather than on UIKit's proposal.
        cn1EndDeferred = YES;
        return;
    }
    int action = CN1_DND_ACTION_NONE;
    if (cn1LocalDropResult >= 0) {
        action = cn1LocalDropResult;
        cn1LocalDropResult = -1;
    } else if (operation == UIDropOperationCopy) {
        action = CN1_DND_ACTION_COPY;
    } else if (operation == UIDropOperationMove) {
        action = CN1_DND_ACTION_MOVE;
    }
    CN1NativeDragDeliverCompleted(action);
}

// ---- the drop half ----------------------------------------------------------------------

- (BOOL)dropInteraction:(UIDropInteraction *)interaction canHandleSession:(id<UIDropSession>)session {
    return YES;
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidEnter:(id<UIDropSession>)session {
    CGPoint point = [session locationInView:interaction.view];
    cn1LastDropAction = CN1NativeDragDeliverOver((int)(point.x * scaleValue), (int)(point.y * scaleValue),
                                                 cn1MimesForSession(session),
                                                 cn1AllowedActionsFor(session), YES);
}

- (UIDropProposal *)dropInteraction:(UIDropInteraction *)interaction
                   sessionDidUpdate:(id<UIDropSession>)session {
    CGPoint point = [session locationInView:interaction.view];
    cn1LastDropAction = CN1NativeDragDeliverOver((int)(point.x * scaleValue), (int)(point.y * scaleValue),
                                                 cn1MimesForSession(session),
                                                 cn1AllowedActionsFor(session), NO);
    UIDropProposal* proposal = [[UIDropProposal alloc] initWithDropOperation:cn1DropOperationFor(cn1LastDropAction)];
#ifndef CN1_USE_ARC
    [proposal autorelease];
#endif
    return proposal;
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidExit:(id<UIDropSession>)session {
    cn1LastDropAction = CN1_DND_ACTION_NONE;
    CN1NativeDragDeliverExit();
}

- (void)dropInteraction:(UIDropInteraction *)interaction performDrop:(id<UIDropSession>)session {
    CGPoint point = [session locationInView:interaction.view];
    const int x = (int)(point.x * scaleValue);
    const int y = (int)(point.y * scaleValue);
    const int action = cn1LastDropAction == CN1_DND_ACTION_NONE
            ? cn1DefaultAction(cn1AllowedActionsFor(session)) : cn1LastDropAction;
    if (session.localDragSession != nil) {
        cn1LocalDropInFlight = YES;
        cn1EndDeferred = NO;
        cn1LocalDropResult = -1;
    }

    // Every representation is loaded asynchronously and independently, so the framework is only
    // told about the drop once they have all answered. Delivering per representation instead
    // would give the application several drops for one gesture, each missing the others.
    NSMutableDictionary* collected = [[NSMutableDictionary alloc] init];
    NSMutableArray* files = [[NSMutableArray alloc] init];
    // The representations a file-vending provider also advertises, named against the copy of
    // its file rather than loaded: pairs of {MIME type, path}.
    NSMutableArray* fileBacked = [[NSMutableArray alloc] init];
    dispatch_group_t group = dispatch_group_create();

    for (UIDragItem* item in session.items) {
        NSItemProvider* provider = item.itemProvider;
        BOOL vendsFile = [provider hasItemConformingToTypeIdentifier:@"public.file-url"];
        if (vendsFile) {
            // A document provider commonly offers both a file URL and the document's own
            // content type. Taking only the file made cn1MimesForSession advertise a type the
            // drop could not then produce, so a target filtered to it accepted the hover and
            // was refused the drop. The other types are named against the copied file below
            // rather than loaded, because they all describe that same file and reading a large
            // one into memory on top of copying it is how an application runs out of it.
            dispatch_group_enter(group);
            [provider loadFileRepresentationForTypeIdentifier:@"public.file-url"
                                           completionHandler:^(NSURL* url, NSError* error) {
                if (url != nil) {
                    // The URL is only valid inside this handler, so the file is copied out
                    // before it is named to the application. A path handed over without
                    // copying is unreadable by the time the event dispatch thread sees it.
                    NSString* name = url.lastPathComponent;
                    if (name == nil || name.length == 0) {
                        name = @"dropped";
                    }
                    NSString* target = [NSTemporaryDirectory() stringByAppendingPathComponent:
                                        [NSString stringWithFormat:@"cn1-drop-%@-%@",
                                         [[NSUUID UUID] UUIDString], name]];
                    NSError* copyError = nil;
                    if ([[NSFileManager defaultManager] copyItemAtURL:url
                                                                toURL:[NSURL fileURLWithPath:target]
                                                                error:&copyError]) {
                        @synchronized (files) {
                            [files addObject:target];
                        }
                        for (NSString* uti in provider.registeredTypeIdentifiers) {
                            if ([uti isEqualToString:@"public.file-url"]) {
                                continue;
                            }
                            NSString* mime = cn1MimeForUti(uti);
                            if (mime == nil) {
                                continue;
                            }
                            @synchronized (fileBacked) {
                                [fileBacked addObject:@[mime, target]];
                            }
                        }
                    }
                }
                dispatch_group_leave(group);
            }];
        }
        if (vendsFile) {
            // Its other representations are the file, handled above.
            continue;
        }
        for (NSString* uti in provider.registeredTypeIdentifiers) {
            NSString* mime = cn1MimeForUti(uti);
            if (mime == nil) {
                continue;
            }
            dispatch_group_enter(group);
            [provider loadDataRepresentationForTypeIdentifier:uti
                                            completionHandler:^(NSData* data, NSError* error) {
                if (data != nil) {
                    @synchronized (collected) {
                        if ([collected objectForKey:mime] == nil) {
                            [collected setObject:data forKey:mime];
                        }
                    }
                }
                dispatch_group_leave(group);
            }];
        }
    }

    dispatch_group_notify(group, dispatch_get_main_queue(), ^{
        // Every representation that answered, not a fixed list: a drag offering markdown, a GIF
        // or an application's own type was accepted while it hovered on the strength of its
        // advertised types, and materializing fewer of them refused the very target that took
        // it.
        CN1NativeDragDeliverDropBegin();
        for (NSString* mime in collected) {
            NSData* data = [collected objectForKey:mime];
            if ([mime hasPrefix:@"text/"]) {
                NSString* text = [[[NSString alloc] initWithData:data
                                                        encoding:NSUTF8StringEncoding] autorelease];
                CN1NativeDragDeliverDropAdd(mime, text, nil);
            } else {
                CN1NativeDragDeliverDropAdd(mime, nil, data);
            }
        }
        for (NSArray* pair in fileBacked) {
            CN1NativeDragDeliverDropAddFile([pair objectAtIndex:0], [pair objectAtIndex:1]);
        }
        if (files.count > 0) {
            CN1NativeDragDeliverDropAdd(@"application/x-file-list",
                                        [files componentsJoinedByString:@"\n"], nil);
        }
        int accepted = CN1NativeDragDeliverDropCommit(x, y, action);
        cn1LastDropAction = CN1_DND_ACTION_NONE;
        if (cn1LocalDropInFlight) {
            cn1LocalDropInFlight = NO;
            if (cn1EndDeferred) {
                cn1EndDeferred = NO;
                CN1NativeDragDeliverCompleted(accepted);
            } else {
                cn1LocalDropResult = accepted;
            }
        }
#ifndef CN1_USE_ARC
        [collected release];
        [files release];
        [fileBacked release];
#endif
    });
#ifndef CN1_USE_ARC
    dispatch_release(group);
#endif
}

@end

void CN1InstallDragAndDrop(CN1View* view) {
    if (view == nil || !CN1DragAndDropSupported()) {
        return;
    }
    if (@available(iOS 11.0, *)) {
        cn1DragSurface = view;
        // The delegate is deliberately not released: the interactions hold their delegate
        // weakly and it has to outlive the surface, which lives for the life of the process.
        cn1DragDelegate = [[CN1DragAndDropDelegate alloc] init];
    }
}

/// True when an interaction of this class is already on the surface. Both enable calls run for
/// every component an application marks, which for a list is every row.
static BOOL cn1HasInteraction(Class kind) {
    for (id<UIInteraction> existing in cn1DragSurface.interactions) {
        if ([existing isKindOfClass:kind]) {
            return YES;
        }
    }
    return NO;
}

void CN1EnableNativeDragSource(void) {
    if (!CN1DragAndDropSupported()) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 11.0, *)) {
            if (cn1DragSurface == nil || cn1DragDelegate == nil
                    || cn1HasInteraction([UIDragInteraction class])) {
                return;
            }
            UIDragInteraction* drag = [[UIDragInteraction alloc] initWithDelegate:cn1DragDelegate];
            // Without this a drag never begins on iPhone: UIKit enables drag interactions on
            // iPad by default and leaves them off elsewhere.
            drag.enabled = YES;
            [cn1DragSurface addInteraction:drag];
#ifndef CN1_USE_ARC
            [drag release];
#endif
        }
    });
}

void CN1EnableNativeDropTarget(void) {
    if (!CN1DragAndDropSupported()) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 11.0, *)) {
            if (cn1DragSurface == nil || cn1DragDelegate == nil
                    || cn1HasInteraction([UIDropInteraction class])) {
                return;
            }
            UIDropInteraction* drop = [[UIDropInteraction alloc] initWithDelegate:cn1DragDelegate];
            [cn1DragSurface addInteraction:drop];
#ifndef CN1_USE_ARC
            [drop release];
#endif
        }
    });
}

#endif
