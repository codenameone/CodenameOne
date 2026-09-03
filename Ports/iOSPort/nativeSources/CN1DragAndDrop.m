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
#import <MobileCoreServices/MobileCoreServices.h>
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

void CN1BeginNativeDragPayload(int sessionId) {
}

void CN1DeclareNativeDragPayload(NSString* mimeType) {
}

void CN1AddNativeDragFiles(NSString* paths) {
}

void CN1CancelNativeDrag(void) {
}

#else

/// The device pixel scale, owned by the view controller. UIKit reports drag positions in
/// points and the framework works in pixels, exactly as the touch path does.
extern float scaleValue;

/// The file URL an item of type public.file-url carries, whichever way the provider hands it
/// over.
///
/// This is the representation, decoded -- not a file *of* it. loadFileRepresentation writes a
/// copy of the data of the type it is given, and the data of type public.file-url is the URL
/// itself, so asking for a file of that yields a temporary file whose contents are a URL
/// string. Nor can the document be found by picking some other identifier the provider
/// happens to register first: that may be a thumbnail or a fallback, and a provider offering
/// nothing but the URL has no other identifier at all. The URL says where the document is,
/// and it is the only thing that does.
static NSURL* cn1FileUrlFromItem(id item) {
    if ([item isKindOfClass:[NSURL class]]) {
        return (NSURL*) item;
    }
    if ([item isKindOfClass:[NSData class]]) {
        // The bytes of a URL, which is what the type is.
        return [NSURL URLWithDataRepresentation:(NSData*) item relativeToURL:nil];
    }
    if ([item isKindOfClass:[NSString class]]) {
        return [NSURL URLWithString:(NSString*) item];
    }
    return nil;
}

/// What the press staged: the representations the drag could offer, what a receiver may do
/// with them, and the preview. Named but not built -- see CN1DragAndDrop.h.
static NSArray* cn1PreparedMimes = nil;

/// What a receiver is allowed to do with the drag. Staged by the press and replaced by the
/// authoritative set when a session really starts; it is what a *local* drop session is told
/// the source allows. UIKit carries no action information for a session that arrived from
/// another application, so those are told copy -- see cn1AllowedActionsFor.
static int cn1SessionActions = CN1_DND_ACTION_NONE;

/// The drag session this source started, retained for as long as it runs and released when
/// UIKit reports it finished -- which it always does for a session it started here.
///
/// Every drag begun anywhere in this application has a non-nil localDragSession, so "local"
/// alone said yes to a drag some other interaction started -- and cn1SessionActions, which
/// nothing cleared, then handed that unrelated drag the last Codename One drag's mask. A
/// move-only one would have proposed a move to a source that never offered it.
static id cn1OutgoingSession = nil;
static UIImage* cn1PreparedPreview = nil;
static CGPoint cn1PreparedTouch;

/// The payload of the session UIKit is currently running, delivered by
/// CN1DeclareNativeDragPayload once the drag has actually begun -- the MIME types it can offer,
/// not their values, which are fetched only if a receiver reads them.
static NSMutableArray* cn1DragMimes = nil;
static NSMutableArray* cn1DragFileUrls = nil;

/// The framework's id for the session being built, captured by every load handler it registers
/// so a late read resolves against its own operation and not the next drag's.
static int cn1DragSessionId = 0;

/// The last action the framework agreed to, reused when a drop arrives without one.
static int cn1LastDropAction = CN1_DND_ACTION_NONE;

/// True while this application is the source of the session in progress.
static BOOL cn1DraggingOut = NO;

/// The drop sessions whose representations are still loading.
///
/// UIKit ends a session as soon as performDrop: returns, which is long before the asynchronous
/// loads that drop depends on have answered. The end must not clear the hover state while the
/// drop about to use it is still in flight -- doing so made the commit find its own target gone
/// and fall back to the declarative answer, discarding whatever the target's callbacks had
/// decided.
///
/// Per session, not a single flag. A slow NSItemProvider can leave one assembly running while
/// the user completes a second drop, and one boolean answered for both: the first assembly's
/// completion cleared it, so the second session's end went on to dispatch an exit while its own
/// loads were still running -- and its commit then found no target of its own to honour, which
/// is the very failure the flag exists to prevent.
///
/// The table holds its sessions weakly; each assembly's own completion block is what keeps its
/// session alive for as long as the answer is needed.
static NSHashTable* cn1LoadingDropSessions = nil;

static BOOL cn1DropIsLoading(id session) {
    return session != nil && cn1LoadingDropSessions != nil
            && [cn1LoadingDropSessions containsObject:session];
}

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
/// The uniform type identifier MobileCoreServices knows a MIME type by, or nil.
///
/// Deprecated from iOS 15, which is why it is reached only when UTType is unavailable; the
/// warning is silenced rather than the call avoided, because on those releases it is the only
/// way to name a type the system will recognize.
static NSString* cn1LegacyUtiForMime(NSString* mime) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    CFStringRef identifier = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType,
                                                                   (CFStringRef) mime, NULL);
#pragma clang diagnostic pop
    if (identifier == NULL) {
        return nil;
    }
    NSString* result = [(NSString*) identifier autorelease];
    // A MIME type it does not know produces a dynamic identifier, which carries no more meaning
    // to a receiver than the MIME type itself and reads far worse.
    return [result hasPrefix:@"dyn."] ? nil : result;
}

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
        return mime;
    }
#endif
    // Below iOS 14 UTType does not exist, and an application may deploy that far back through
    // the ios.deployment_target build hint. Without this every type not named above -- including
    // application/pdf -- was published under its MIME string, which no other application asking
    // for the standard identifier would ever match.
    NSString* legacy = cn1LegacyUtiForMime(mime);
    return legacy != nil ? legacy : mime;
}

/// The MIME type MobileCoreServices knows a uniform type identifier by, or nil. The reverse of
/// cn1LegacyUtiForMime, and deprecated from iOS 15 for the same reason.
static NSString* cn1LegacyMimeForUti(NSString* uti) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    CFStringRef mime = UTTypeCopyPreferredTagWithClass((CFStringRef) uti, kUTTagClassMIMEType);
#pragma clang diagnostic pop
    if (mime == NULL) {
        return nil;
    }
    NSString* result = [(NSString*) mime autorelease];
    return result.length > 0 ? [result lowercaseString] : nil;
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
        return nil;
    }
#endif
    // Below iOS 14, the same way round as cn1UtiForMime goes. Answering nil here regardless of
    // the identifier left a standard type such as com.adobe.pdf unnamed on those releases, so a
    // drag of one was neither discovered while it hovered nor materialized when it dropped --
    // the outgoing direction had its legacy conversion and the incoming one did not.
    NSString* legacy = cn1LegacyMimeForUti(uti);
    if (legacy != nil) {
        return legacy;
    }
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
    // The session this source started, not merely one that started somewhere in this
    // application: another interaction's drag is as foreign to this framework as one from
    // another application, and is told the same thing.
    if (session.localDragSession != nil && cn1OutgoingSession != nil
            && session.localDragSession == cn1OutgoingSession
            && cn1SessionActions != CN1_DND_ACTION_NONE) {
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
        // how a document dragged out of Files reaches a target that asked for files. Both
        // spellings of that, as the other two ports publish them: a component filtered to
        // MIME_URI_LIST refused an ordinary drag out of Files while the same component took
        // it from the Finder and from a file manager on Android.
        if ([item.itemProvider hasItemConformingToTypeIdentifier:@"public.file-url"]) {
            if (![mimes containsObject:@"application/x-file-list"]) {
                [mimes addObject:@"application/x-file-list"];
            }
            if (![mimes containsObject:@"text/uri-list"]) {
                [mimes addObject:@"text/uri-list"];
            }
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
    // At the screen's scale, because the framework renders in device pixels and UIKit lays
    // the preview out in points. Decoded at scale 1, a snapshot from a 2x or 3x screen is
    // that many times too big -- and the touch offset below is converted to points, so the
    // grab point lands somewhere else on an image of the wrong size as well.
    cn1PreparedPreview = dragImagePng == nil ? nil
            : [UIImage imageWithData:dragImagePng scale:(scaleValue > 0 ? scaleValue : 1)];
    cn1PreparedTouch = CGPointMake(touchX / scaleValue, touchY / scaleValue);
#ifndef CN1_USE_ARC
    [cn1PreparedMimes retain];
    [cn1PreparedPreview retain];
#endif
}

void CN1BeginNativeDragPayload(int sessionId) {
#ifndef CN1_USE_ARC
    [cn1DragMimes release];
    [cn1DragFileUrls release];
#endif
    cn1DragMimes = [[NSMutableArray alloc] init];
    cn1DragFileUrls = [[NSMutableArray alloc] init];
    cn1DragSessionId = sessionId;
}

void CN1DeclareNativeDragPayload(NSString* mimeType) {
    if (mimeType == nil || mimeType.length == 0 || cn1DragMimes == nil
            || [cn1DragMimes containsObject:mimeType]) {
        return;
    }
    [cn1DragMimes addObject:mimeType];
}

void CN1AddNativeDragFiles(NSString* paths) {
    if (paths == nil || paths.length == 0 || cn1DragFileUrls == nil) {
        return;
    }
    for (NSString* entry in [paths componentsSeparatedByString:@"\n"]) {
        if (entry.length == 0) {
            continue;
        }
        // ClipboardContent's file representation permits a raw local path as well as a file:
        // URI, and URLWithString: turns a path into a scheme-less relative URL that no receiver
        // can open. An absolute path is obvious; a relative one -- exports/report.pdf -- looks
        // enough like a URL to be parsed as one, and was then quietly dropped from the drag
        // because an item provider cannot vend it. Anything that does not come back with a
        // scheme is a path.
        NSURL* url;
        if ([entry hasPrefix:@"/"] || [entry hasPrefix:@"~"]) {
            url = [NSURL fileURLWithPath:[entry stringByExpandingTildeInPath]];
        } else {
            url = [NSURL URLWithString:entry];
            if (url == nil || url.scheme == nil) {
                url = [NSURL fileURLWithPath:entry];
            }
        }
        if (url != nil) {
            [cn1DragFileUrls addObject:url];
        }
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

/// Holds a drag's payload alive for exactly as long as something can still read it.
///
/// Every load handler this session registers captures the token, and copying a block retains
/// what it captures, so the token outlives the gesture precisely when an NSItemProvider does --
/// which is the case that matters, since a receiver may keep a provider and read it much later.
/// When the last provider goes the token deallocates and the framework drops the payload. It
/// carries only the session id: the bytes stay on the Java side, unbuilt until read.
@interface CN1DragPayloadToken : NSObject {
    int _sessionId;
}
@property (nonatomic) int sessionId;
@end

@implementation CN1DragPayloadToken

@synthesize sessionId = _sessionId;

- (void)dealloc {
    const int released = _sessionId;
    // dealloc runs on whichever thread released the last provider. Every other call into the
    // framework from this file is made on the main thread, and this one is no different.
    dispatch_async(dispatch_get_main_queue(), ^{
        CN1NativeDragDeliverPayloadReleased(released);
    });
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}

@end

/// Reads a text representation with the encoding its uniform type identifier declares.
///
/// cn1MimeForUti maps public.utf8-plain-text, public.utf16-plain-text and
/// public.utf16-external-plain-text all onto text/plain, because that is the MIME type they all
/// are -- but they do not agree about the bytes. Decoding UTF-16 as UTF-8 answers nil, which
/// dropped a representation the drag had advertised and refused the very target that accepted
/// it on the strength of it. Worse, UTF-16 in little endian without a byte order mark decodes
/// as UTF-8 *successfully*, into text full of NULs, so trying UTF-8 first and falling back is
/// not a substitute for reading what the identifier says.
///
/// Anything the identifier does not pin down is handed to NSString's own detection rather than
/// assumed, and only a representation nothing can read at all comes back nil.
/// The charset a uniform type identifier declares, by the name java.nio understands, or nil
/// when the identifier says nothing about the encoding.
///
/// A representation handed over as a file keeps only its path, so this is what travels with
/// it: the Java side reads the bytes later and would otherwise have to assume UTF-8, which
/// turns a public.utf16-plain-text alternative into rubbish.
static NSString* cn1CharsetNameForUti(NSString* uti) {
    if ([uti isEqualToString:@"public.utf16-plain-text"]
            || [uti isEqualToString:@"public.utf16-external-plain-text"]) {
        return @"UTF-16";
    }
    if ([uti isEqualToString:@"public.utf8-plain-text"]) {
        return @"UTF-8";
    }
    return nil;
}

static NSString* cn1TextFromData(NSData* data, NSString* uti) {
    NSStringEncoding declared = 0;
    NSString* charset = cn1CharsetNameForUti(uti);
    if ([charset isEqualToString:@"UTF-16"]) {
        declared = NSUTF16StringEncoding;
    } else if ([charset isEqualToString:@"UTF-8"]) {
        declared = NSUTF8StringEncoding;
    }
    if (declared != 0) {
        NSString* exact = [[[NSString alloc] initWithData:data encoding:declared] autorelease];
        if (exact != nil) {
            return exact;
        }
    }
    NSString* utf8 = [[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] autorelease];
    if (utf8 != nil) {
        return utf8;
    }
    NSString* detected = nil;
    [NSString stringEncodingForData:data
                    encodingOptions:@{NSStringEncodingDetectionSuggestedEncodingsKey:
                                          @[@(NSUTF16StringEncoding),
                                            @(NSUTF16LittleEndianStringEncoding),
                                            @(NSUTF16BigEndianStringEncoding),
                                            @(NSISOLatin1StringEncoding)]}
                    convertedString:&detected
                usedLossyConversion:NULL];
    return detected;
}

/// Copies a file a drop handed over into somewhere that outlives the handler, and returns the
/// path -- or nil when the copy failed.
///
/// Every URL an NSItemProvider produces is valid only for the duration of the completion handler
/// it arrives in, so a path handed to the framework without copying is unreadable by the time
/// the event dispatch thread sees it. The name is kept because a receiver commonly shows it.
static NSString* cn1CopyDroppedFile(NSURL* url) {
    NSString* name = url.lastPathComponent;
    if (name == nil || name.length == 0) {
        name = @"dropped";
    }
    NSString* target = [NSTemporaryDirectory() stringByAppendingPathComponent:
                        [NSString stringWithFormat:@"cn1-drop-%@-%@",
                         [[NSUUID UUID] UUIDString], name]];
    NSError* copyError = nil;
    if (![[NSFileManager defaultManager] copyItemAtURL:url
                                                 toURL:[NSURL fileURLWithPath:target]
                                                 error:&copyError]) {
        return nil;
    }
    return target;
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
    // unwritten until a drag really happens. The Java side names its representations from
    // inside this call, through CN1DeclareNativeDragPayload; their values are fetched only if a
    // receiver reads them.
    int allowed = CN1NativeDragDeliverSessionStarted();
    if (allowed == CN1_DND_ACTION_NONE) {
        // Nothing was staged, or nothing may be done with what was. Either way no session
        // begins; the framework side has already tidied up whatever it had.
        return @[];
    }
    // The authoritative set, which is what a local drop session is told the source allows --
    // and the session it belongs to, so no other drag in this application can inherit it.
    cn1SessionActions = allowed;
#ifndef CN1_USE_ARC
    [cn1OutgoingSession release];
#endif
    cn1OutgoingSession = session;
#ifndef CN1_USE_ARC
    [cn1OutgoingSession retain];
#endif
    cn1DraggingOut = YES;
    cn1LocalDropInFlight = NO;
    cn1EndDeferred = NO;
    cn1LocalDropResult = -1;

    NSMutableArray<UIDragItem *>* items = [NSMutableArray array];
    // The payload's keeper. Released once below, after everything that could read it has been
    // registered; from then on the load handlers are its only owners, so it dies with the last
    // of them -- and with no handlers at all it dies here, which is equally correct because
    // nothing can read the payload then either.
    CN1DragPayloadToken* payloadToken = [[CN1DragPayloadToken alloc] init];
    payloadToken.sessionId = cn1DragSessionId;

    // Every representation the operation declared, registered lazily. The value is fetched when
    // a receiver reads that type, not now: a drag that is begun and abandoned must not have
    // written the file or encoded the image it was merely offering. NSItemProvider allows a
    // load handler to answer asynchronously, which is what lets the fetch happen on the main
    // thread where every other call into the framework from this file happens. Nothing is
    // retained by hand either -- copying a block retains what it captures, and an explicit
    // retain here leaked the whole payload of every drag.
    void (^registerDeclared)(NSItemProvider*) = ^(NSItemProvider* provider) {
        for (NSString* mime in cn1DragMimes) {
            NSString* uti = cn1UtiForMime(mime);
            if (uti == nil) {
                continue;
            }
            [provider registerDataRepresentationForTypeIdentifier:uti
                                                       visibility:NSItemProviderRepresentationVisibilityAll
                                                      loadHandler:^NSProgress *(void (^completion)(NSData *, NSError *)) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    completion(CN1NativeDragDeliverResolve(mime, payloadToken.sessionId), nil);
                });
                return nil;
            }];
        }
    };

    // Files first, one item each: a receiver that copies documents expects one item per
    // document, and collapsing several into one loses all but the first.
    BOOL declaredAttached = NO;
    for (NSURL* url in cn1DragFileUrls) {
        NSItemProvider* provider = [[NSItemProvider alloc] initWithContentsOfURL:url];
        if (provider == nil) {
            continue;
        }
        if (!declaredAttached) {
            // The other representations belong to this same object, not to one of their own.
            // Given a file and a text fallback, adding a second item made UIKit expose them as
            // two dragged things, so a receiver could import the document *and* a stray piece
            // of text instead of choosing the best form of one.
            //
            // Note the one case this cannot express: a declared type whose UTI is also the
            // file's own -- a text/plain fallback beside a .txt -- registers a second
            // representation under an identifier the provider already vends, and which of the
            // two a receiver gets is NSItemProvider's choice rather than ours. Both are
            // honestly that type, so neither answer is wrong; there is simply no way to say
            // which was meant.
            registerDeclared(provider);
            declaredAttached = YES;
        }
        UIDragItem* item = [[UIDragItem alloc] initWithItemProvider:provider];
        [items addObject:item];
#ifndef CN1_USE_ARC
        [provider release];
        [item release];
#endif
    }
    if (!declaredAttached && cn1DragMimes.count > 0) {
        NSItemProvider* provider = [[NSItemProvider alloc] init];
        registerDeclared(provider);
        UIDragItem* item = [[UIDragItem alloc] initWithItemProvider:provider];
        [items addObject:item];
#ifndef CN1_USE_ARC
        [provider release];
        [item release];
#endif
    }
#ifndef CN1_USE_ARC
    // The registered load handlers own it from here; under ARC the local strong reference goes
    // at the end of this scope and does the same thing.
    [payloadToken release];
#endif
    if (items.count == 0) {
        cn1DraggingOut = NO;
        CN1NativeDragDeliverCompleted(CN1_DND_ACTION_NONE);
    }
    return items;
}

- (UITargetedDragPreview *)dragInteraction:(UIDragInteraction *)interaction
                     previewForLiftingItem:(UIDragItem *)item
                                   session:(id<UIDragSession>)session {
    // UITargetedDragPreview, which is what UIDragInteractionDelegate declares. This returned a
    // UIDragPreview instead -- an unrelated class -- so UIKit was handed an object it would go
    // on to send UITargetedDragPreview messages to. Clang does not warn about the mismatch, and
    // nothing but a device would have shown it.
    if (cn1PreparedPreview == nil || interaction.view == nil) {
        // Without one UIKit snapshots the interaction's view, which is the whole Codename One
        // surface; nil here leaves UIKit to its default rather than dragging the entire screen.
        return nil;
    }
    UIImageView* view = [[UIImageView alloc] initWithImage:cn1PreparedPreview];
    // Positioned so the point the finger grabbed stays under the finger. Untargeted, the
    // preview is centred whereever UIKit chooses and the image jumps out from under the touch
    // the moment the drag lifts -- which is also why setDragImageOffset had no effect at all.
    CGPoint touch = [session locationInView:interaction.view];
    CGSize size = cn1PreparedPreview.size;
    CGPoint centre = CGPointMake(touch.x - cn1PreparedTouch.x + size.width / 2.0,
                                 touch.y - cn1PreparedTouch.y + size.height / 2.0);
    UIDragPreviewTarget* target = [[UIDragPreviewTarget alloc] initWithContainer:interaction.view
                                                                         center:centre];
    UIDragPreviewParameters* parameters = [[UIDragPreviewParameters alloc] init];
    UITargetedDragPreview* preview = [[UITargetedDragPreview alloc] initWithView:view
                                                                     parameters:parameters
                                                                         target:target];
#ifndef CN1_USE_ARC
    [view release];
    [target release];
    [parameters release];
    [preview autorelease];
#endif
    return preview;
}

- (BOOL)dragInteraction:(UIDragInteraction *)interaction
sessionAllowsMoveOperation:(id<UIDragSession>)session {
    // What the operation actually permits. UIKit allows a move by default for a session that
    // stays inside the application, and storing the mask for this framework's own drop delegate
    // does not constrain a *different* UIDropInteraction here -- so a copy-only drag landing on
    // one of those could be moved, didEndWithOperation: would report the move, and a source
    // following the documented advice would delete data the operation had explicitly refused to
    // allow moving.
    //
    // The mask belongs to the session this interaction started; see cn1OutgoingSession.
    return (cn1SessionActions & CN1_DND_ACTION_MOVE) != 0;
}

- (void)dragInteraction:(UIDragInteraction *)interaction
                session:(id<UIDragSession>)session
    didEndWithOperation:(UIDropOperation)operation {
    cn1DraggingOut = NO;
    const int allowed = (cn1OutgoingSession == session) ? cn1SessionActions : CN1_DND_ACTION_NONE;
    if (cn1OutgoingSession == session) {
#ifndef CN1_USE_ARC
        [cn1OutgoingSession release];
#endif
        cn1OutgoingSession = nil;
        cn1SessionActions = CN1_DND_ACTION_NONE;
    }
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
    // Never more than the source allowed. The refusal above is what should keep a move from
    // being performed at all; this is the second half of it, because the cost of being wrong
    // here is a source deleting data on the strength of an action it never offered.
    //
    // With one reading rather than a refusal. UIKit has no link operation, so a receiver
    // that takes a link-only drag is reported as having copied it -- and clamping that to
    // nothing told the source its drag had been cancelled when it had in fact been
    // accepted. Only when link is the whole of what was offered, where there is nothing
    // else the copy could have meant.
    if (allowed != CN1_DND_ACTION_NONE && (action & allowed) != action) {
        action = (action == CN1_DND_ACTION_COPY && allowed == CN1_DND_ACTION_LINK)
                ? CN1_DND_ACTION_LINK : CN1_DND_ACTION_NONE;
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

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidEnd:(id<UIDropSession>)session {
    // UIKit sends this whether or not sessionDidExit ran: a session cancelled, or ended, while
    // still inside this surface never exits. Without it the framework kept the last target
    // hovered, and the next session entering that same component was routed as a move over it
    // -- inheriting the ended session's answer, with no enter callback ever arriving.
    //
    // Not while *this* session's drop is still loading, though. This arrives as soon as
    // performDrop: returns, which is before the asynchronous loads have answered, and clearing
    // the target there would leave the commit to find it gone. That path clears the hover state
    // itself.
    if (cn1DropIsLoading(session)) {
        return;
    }
    cn1LastDropAction = CN1_DND_ACTION_NONE;
    CN1NativeDragDeliverExit();
}

- (void)dropInteraction:(UIDropInteraction *)interaction performDrop:(id<UIDropSession>)session {
    CGPoint point = [session locationInView:interaction.view];
    const int x = (int)(point.x * scaleValue);
    const int y = (int)(point.y * scaleValue);
    const int action = cn1LastDropAction == CN1_DND_ACTION_NONE
            ? cn1DefaultAction(cn1AllowedActionsFor(session)) : cn1LastDropAction;
    if (cn1LoadingDropSessions == nil) {
        cn1LoadingDropSessions = [NSHashTable weakObjectsHashTable];
#ifndef CN1_USE_ARC
        [cn1LoadingDropSessions retain];
#endif
    }
    [cn1LoadingDropSessions addObject:session];
    // Two different questions, and they were one answer.
    //
    // Whether the drag started inside this application is what the framework reports as
    // NativeDropEvent.isLocal(), and a drag any interaction here started is local by that
    // reading.
    const BOOL localAssembly = (session.localDragSession != nil);
    // Whether *this* framework's source is the one that started it is a narrower thing, and it
    // is what the completion bookkeeping below belongs to. Another UIDragInteraction's drag also
    // has a localDragSession, so answering the first question for the second had an unrelated
    // drop take ownership of the state a Codename One source was waiting on -- and complete that
    // source with its own result. The session identity that already answers this for the action
    // mask answers it here too.
    const BOOL ownsCompletion = localAssembly && cn1OutgoingSession != nil
            && session.localDragSession == cn1OutgoingSession;
    // The mask this session offers, taken now. By the time a slow provider has finished the
    // framework's own memory of it belongs to whatever drag is running then.
    const int sessionActions = cn1AllowedActionsFor(session);
    if (ownsCompletion) {
        cn1LocalDropInFlight = YES;
        cn1EndDeferred = NO;
        cn1LocalDropResult = -1;
    }

    // Every representation is loaded asynchronously and independently, so the framework is only
    // told about the drop once they have all answered. Delivering per representation instead
    // would give the application several drops for one gesture, each missing the others.
    //
    // MIME type -> {data, the uniform type identifier it arrived under}. The identifier is kept
    // because it is what says how to read the bytes; see cn1TextFromData.
    NSMutableDictionary* collected = [[NSMutableDictionary alloc] init];
    NSMutableArray* files = [[NSMutableArray alloc] init];
    // The representations a file-vending provider also advertises, each named against a file on
    // disk rather than read into memory: {MIME type, path, charset}. One that is another name
    // for the document shares the document's own copy; one that is a representation of its own
    // gets a copy of its own. The charset is empty unless the identifier declared one, which
    // is the only thing that can tell the Java side how to read a text file it never saw the
    // identifier for.
    NSMutableArray* fileBacked = [[NSMutableArray alloc] init];
    dispatch_group_t group = dispatch_group_create();

    for (UIDragItem* item in session.items) {
        NSItemProvider* provider = item.itemProvider;
        BOOL vendsFile = [provider hasItemConformingToTypeIdentifier:@"public.file-url"];
        if (vendsFile) {
            // A document provider commonly offers both a file URL and the document's own
            // content type. Taking only the file made cn1MimesForSession advertise a type the
            // drop could not then produce, so a target filtered to it accepted the hover and
            // was refused the drop.
            dispatch_group_enter(group);
            // The item, not a file representation of it: see cn1FileUrlFromItem.
            [provider loadItemForTypeIdentifier:@"public.file-url"
                                        options:nil
                              completionHandler:^(id<NSSecureCoding> item, NSError* error) {
                NSURL* url = cn1FileUrlFromItem((id) item);
                // The URL is only valid inside this handler, so the file is copied out before
                // it is named to the application. A path handed over without copying is
                // unreadable by the time the event dispatch thread sees it.
                //
                // A failure here is not the end of the item: a cloud-backed document that
                // will not materialize still leaves whatever else the provider advertised,
                // and the hover has already promised those. Nesting the alternatives under
                // this meant an unavailable file took every one of them with it and the
                // target that accepted the drag got nothing at all.
                //
                // A document in another application's container is reachable only for as
                // long as its security scope is held, and only some URLs have one -- the
                // start call says which by its answer.
                BOOL scoped = url != nil && [url startAccessingSecurityScopedResource];
                NSString* target = url == nil ? nil : cn1CopyDroppedFile(url);
                if (scoped) {
                    [url stopAccessingSecurityScopedResource];
                }
                // The document's own location, which is what tells one of its other names
                // apart from a representation of its own. Nil when there is no document.
                NSString* documentPath = target == nil ? nil : url.path;
                if (target != nil) {
                    @synchronized (files) {
                        [files addObject:target];
                    }
                }
                {
                    {
                        // Issued from in here, rather than beside the file load, so that the
                        // comparison above is possible at all.
                        for (NSString* uti in provider.registeredTypeIdentifiers) {
                            if ([uti isEqualToString:@"public.file-url"]) {
                                continue;
                            }
                            NSString* mime = cn1MimeForUti(uti);
                            if (mime == nil) {
                                continue;
                            }
                            dispatch_group_enter(group);
                            // As a file rather than as data, deliberately: a representation
                            // that really is the document would otherwise be read whole into
                            // memory on top of the copy, which is how an application runs out
                            // of it. Naming every one of them against the document instead --
                            // which is what this did -- is wrong the other way: a drag that
                            // offers a file *and* something else, as this framework's own
                            // source does for a file with a text fallback, then served the
                            // file's bytes under the fallback's type.
                            [provider loadFileRepresentationForTypeIdentifier:uti
                                                           completionHandler:^(NSURL* alt, NSError* altError) {
                                if (alt != nil) {
                                    NSString* altTarget = (documentPath != nil
                                            && [alt.path isEqualToString:documentPath])
                                            ? target             // another name for the document
                                            : cn1CopyDroppedFile(alt);
                                    if (altTarget != nil) {
                                        NSString* charset = cn1CharsetNameForUti(uti);
                                        @synchronized (fileBacked) {
                                            [fileBacked addObject:@[mime, altTarget,
                                                                    charset == nil ? @"" : charset]];
                                        }
                                    }
                                }
                                dispatch_group_leave(group);
                            }];
                        }
                    }
                }
                dispatch_group_leave(group);
            }];
            // Its other representations were loaded above, where the document is known.
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
                        NSArray* existing = [collected objectForKey:mime];
                        if (existing == nil) {
                            // The identifier is kept beside the bytes because it is what says
                            // how to read them: several standard text UTIs map to the same MIME
                            // type and disagree about the encoding.
                            [collected setObject:@[data, uti] forKey:mime];
                        } else if ([mime isEqualToString:@"text/uri-list"]) {
                            // A URI list is a list. Several public.url items all arrive under
                            // this one type, and keeping whichever asynchronous load happened to
                            // finish first threw away every URL the user dragged but one. RFC
                            // 2483 separates them with CRLF, which is what everything else here
                            // writes and reads.
                            NSMutableData* joined =
                                    [NSMutableData dataWithData:[existing objectAtIndex:0]];
                            [joined appendBytes:"\r\n" length:2];
                            [joined appendData:data];
                            [collected setObject:@[joined, [existing objectAtIndex:1]] forKey:mime];
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
            NSArray* pair = [collected objectForKey:mime];
            NSData* data = [pair objectAtIndex:0];
            if ([mime hasPrefix:@"text/"]) {
                CN1NativeDragDeliverDropAdd(mime, cn1TextFromData(data, [pair objectAtIndex:1]), nil);
            } else {
                CN1NativeDragDeliverDropAdd(mime, nil, data);
            }
        }
        for (NSArray* named in fileBacked) {
            NSString* charset = [named objectAtIndex:2];
            CN1NativeDragDeliverDropAddFile([named objectAtIndex:0], [named objectAtIndex:1],
                                            charset.length == 0 ? nil : charset);
        }
        if (files.count > 0) {
            CN1NativeDragDeliverDropAdd(@"application/x-file-list",
                                        [files componentsJoinedByString:@"\n"], nil);
            // The same files as a URI list, which is what the session advertised and so what
            // a target filtered to it accepted the hover on. RFC 2483 separates them with
            // CRLF, which is what the other ports write and read.
            NSMutableString* uris = [NSMutableString string];
            for (NSString* path in files) {
                [uris appendString:[[NSURL fileURLWithPath:path] absoluteString]];
                [uris appendString:@"\r\n"];
            }
            CN1NativeDragDeliverDropAdd(@"text/uri-list", uris, nil);
        }
        // An assembly overtaken by a newer session still commits. The framework keeps one hover
        // state, because one drag is what a platform runs, so a late commit does disturb a drag
        // begun since -- the newer target is sent an exit and its next update re-enters it, a
        // flicker that repairs itself. Withholding the commit instead would lose a drop the
        // user actually performed, and unperformed work is worse than a repaired frame.
        int accepted = CN1NativeDragDeliverDropCommit(x, y, action, sessionActions, localAssembly);
        cn1LastDropAction = CN1_DND_ACTION_NONE;
        // This assembly's commit cleared the hover state itself, so its own end -- which went
        // past long ago -- can stop holding off. Only this one: another drop still loading is
        // still entitled to its answer.
        [cn1LoadingDropSessions removeObject:session];
        if (ownsCompletion && cn1LocalDropInFlight) {
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
