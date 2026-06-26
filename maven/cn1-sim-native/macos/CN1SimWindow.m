/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 * Pure-native windowing for the simulator: the dylib owns an NSWindow whose
 * content view hosts the rendering CALayer, and AppKit input events flow
 * directly into the Codename One display via JNI upcalls. No AWT/Swing
 * anywhere - the JVM runs with java.awt.headless=true and the launcher
 * dedicates the process main thread to the AppKit run loop
 * (-XstartOnFirstThread), exactly like SWT applications.
 */
#import <AppKit/AppKit.h>
#import <QuartzCore/QuartzCore.h>
#import <WebKit/WebKit.h>
#import <AVFoundation/AVFoundation.h>
#import <AVKit/AVKit.h>
#include <jni.h>
#include "cn1jni_runtime.h"
#import "CodenameOne_GLViewController.h"

/* defined in cn1jni_runtime.c */
extern JavaVM *cn1jni_javavm(void);

/* upcall targets on com.codename1.impl.ios.sim.CN1SimHost */
static jclass cn1simHostClass;
static jmethodID cn1simPointerEvent;
static jmethodID cn1simKeyEvent;
static jmethodID cn1simWindowClosed;
static jmethodID cn1simMenuCommand;
static jmethodID cn1simWindowResized;
static jmethodID cn1simEditingDone;
static jmethodID cn1simEditingUpdate;
static jmethodID cn1simScrollWheel;
static jmethodID cn1simFilePicked;

static JNIEnv *cn1sim_main_env(void) {
    JavaVM *vm = cn1jni_javavm();
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        (*vm)->AttachCurrentThreadAsDaemon(vm, (void **) &env, NULL);
    }
    return env;
}

/* pointer event types mirrored in CN1SimHost */
enum {
    CN1SIM_POINTER_PRESSED = 1,
    CN1SIM_POINTER_RELEASED = 2,
    CN1SIM_POINTER_DRAGGED = 3
};

@interface CN1SimView : NSView
@end

@implementation CN1SimView

- (BOOL)isFlipped {
    /* top-left origin so event coordinates match CN1 display space */
    return YES;
}

- (BOOL)acceptsFirstResponder {
    return YES;
}

/* trackpad / mouse-wheel scrolling - drives the shell's zoom viewport */
- (void)scrollWheel:(NSEvent *)event {
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simScrollWheel != NULL) {
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simScrollWheel,
                (jint) -[event scrollingDeltaX], (jint) -[event scrollingDeltaY]);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
}

- (void)dispatchPointer:(NSEvent *)event type:(int)type {
    NSPoint p = [self convertPoint:[event locationInWindow] fromView:nil];
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simPointerEvent != NULL) {
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simPointerEvent,
                (jint) type, (jint) p.x, (jint) p.y);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
}

- (void)mouseDown:(NSEvent *)event {
    [self dispatchPointer:event type:CN1SIM_POINTER_PRESSED];
}

- (void)mouseUp:(NSEvent *)event {
    [self dispatchPointer:event type:CN1SIM_POINTER_RELEASED];
}

- (void)mouseDragged:(NSEvent *)event {
    [self dispatchPointer:event type:CN1SIM_POINTER_DRAGGED];
}

- (void)keyDown:(NSEvent *)event {
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simKeyEvent != NULL) {
        NSString *chars = [event charactersIgnoringModifiers];
        int code = [chars length] > 0 ? [chars characterAtIndex:0] : 0;
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simKeyEvent,
                (jint) 1, (jint) code);
    }
}

- (void)keyUp:(NSEvent *)event {
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simKeyEvent != NULL) {
        NSString *chars = [event charactersIgnoringModifiers];
        int code = [chars length] > 0 ? [chars characterAtIndex:0] : 0;
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simKeyEvent,
                (jint) 2, (jint) code);
    }
}

@end

@interface CN1SimWindowDelegate : NSObject <NSWindowDelegate>
@end

@implementation CN1SimWindowDelegate

- (void)windowWillClose:(NSNotification *)notification {
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simWindowClosed != NULL) {
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simWindowClosed);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
}

/* user finished dragging the window edge - the launcher rescales the shell */
- (void)windowDidEndLiveResize:(NSNotification *)notification {
    NSWindow *win = [notification object];
    NSSize size = [[win contentView] bounds].size;
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simWindowResized != NULL) {
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simWindowResized,
                (jint) size.width, (jint) size.height);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
}

@end

static NSWindow *cn1simWindow;

/* ---- native text editing ---------------------------------------------------
 * editString floats a real NSTextField above the rendering layer at the
 * component's window rectangle; Enter or focus loss commits the text back
 * through CN1SimHost.nativeEditingDone.
 */

static NSTextField *cn1simEditField;

static void cn1sim_commit_edit(void) {
    if (cn1simEditField == nil) {
        return;
    }
    NSTextField *field = cn1simEditField;
    cn1simEditField = nil;
    NSString *text = [[field stringValue] copy];
    [field setDelegate:nil];
    [field removeFromSuperview];
    [field release];
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simEditingDone != NULL) {
        jstring jt = (*env)->NewString(env, (const jchar *) [text cStringUsingEncoding:NSUTF16LittleEndianStringEncoding],
                (jsize) [text length]);
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simEditingDone, jt);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        (*env)->DeleteLocalRef(env, jt);
    }
    [text release];
}

@interface CN1SimEditDelegate : NSObject <NSTextFieldDelegate>
@end

@implementation CN1SimEditDelegate

- (void)controlTextDidEndEditing:(NSNotification *)notification {
    cn1sim_commit_edit();
}

/* live per-keystroke updates - the CN1 component mirrors the text so
 * grow-by-content and dependent UI behave like on a device */
- (void)controlTextDidChange:(NSNotification *)notification {
    if (cn1simEditField == nil) {
        return;
    }
    NSString *text = [cn1simEditField stringValue];
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simEditingUpdate != NULL) {
        jstring jt = (*env)->NewString(env,
                (const jchar *) [text cStringUsingEncoding:NSUTF16LittleEndianStringEncoding],
                (jsize) [text length]);
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simEditingUpdate, jt);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        (*env)->DeleteLocalRef(env, jt);
    }
}

@end

static CN1SimEditDelegate *cn1simEditDelegate;

static void cn1sim_begin_edit_on_main(NSString *text, int x, int y, int w, int h,
        NSFont *font, int fgColor, BOOL multiline) {
    cn1sim_commit_edit();
    if (cn1simWindow == nil) {
        return;
    }
    if (cn1simEditDelegate == nil) {
        cn1simEditDelegate = [[CN1SimEditDelegate alloc] init];
    }
    NSView *content = [cn1simWindow contentView];
    /* CN1SimView isFlipped - subview frames already use top-left origin */
    NSRect frame = NSMakeRect(x, y, w, h);
    NSTextField *field = [[NSTextField alloc] initWithFrame:frame];
    [field setStringValue:text != nil ? text : @""];
    /* the editor inherits the COMPONENT's style: its CN1 font peer and
     * foreground color, borderless and transparent so the component's own
     * background/border keep showing through - the iOS port's approach */
    if (font != nil) {
        [field setFont:font];
    }
    [field setTextColor:[NSColor colorWithCalibratedRed:((fgColor >> 16) & 0xff) / 255.0
                                                  green:((fgColor >> 8) & 0xff) / 255.0
                                                   blue:(fgColor & 0xff) / 255.0
                                                  alpha:1.0]];
    [field setBezeled:NO];
    [field setBordered:NO];
    [field setDrawsBackground:NO];
    [field setFocusRingType:NSFocusRingTypeNone];
    if (multiline) {
        /* allow wrapping; Enter still commits in this v1 */
        [[field cell] setWraps:YES];
        [[field cell] setScrollable:NO];
    } else {
        [[field cell] setWraps:NO];
        [[field cell] setScrollable:YES];
    }
    [field setEditable:YES];
    [field setSelectable:YES];
    [field setDelegate:cn1simEditDelegate];
    [content addSubview:field];
    [cn1simWindow makeFirstResponder:field];
    cn1simEditField = field;
}

static void jni_CN1SimHost_editString(JNIEnv *env, jclass cls, jstring text,
        jint x, jint y, jint w, jint h, jlong fontPeer, jint fgColor, jboolean multiline) {
    const jchar *chars = (*env)->GetStringChars(env, text, NULL);
    NSString *t = chars != NULL
            ? [[NSString alloc] initWithCharacters:(const unichar *) chars
                                            length:(NSUInteger) (*env)->GetStringLength(env, text)]
            : [@"" retain];
    if (chars != NULL) {
        (*env)->ReleaseStringChars(env, text, chars);
    }
    NSFont *font = (NSFont *) (intptr_t) fontPeer;
    BOOL ml = multiline ? YES : NO;
    dispatch_async(dispatch_get_main_queue(), ^{
        cn1sim_begin_edit_on_main(t, x, y, w, h, font, fgColor, ml);
        [t release];
    });
}

/* ---- native menu built from Codename One commands -------------------------- */

@interface CN1SimMenuHandler : NSObject
- (void)fireMenuItem:(NSMenuItem *)item;
@end

@implementation CN1SimMenuHandler

- (void)fireMenuItem:(NSMenuItem *)item {
    JNIEnv *env = cn1sim_main_env();
    if (env != NULL && cn1simMenuCommand != NULL) {
        (*env)->CallStaticVoidMethod(env, cn1simHostClass, cn1simMenuCommand, (jint) [item tag]);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
}

@end

static CN1SimMenuHandler *cn1simMenuHandler;

/*
 * Builds the menu bar from encoded command rows
 * ("menuHint\tlabel\tkeyChar\tmodifiers[\tflags]" per line - the Mac Catalyst
 * port's format plus simulator extensions: a '>' separated hint nests
 * submenus ("Tools>Network"), the label "-" inserts a separator and a flags
 * column containing 'c' renders the item checked. Each item's tag is its row
 * index, fired back through CN1SimHost.nativeMenuCommand. Runs on the main
 * thread.
 */
static void cn1sim_install_menu(NSString *encoded) {
    if (cn1simMenuHandler == nil) {
        cn1simMenuHandler = [[CN1SimMenuHandler alloc] init];
    }
    NSMenu *mainMenu = [[NSMenu alloc] init];

    /* standard application menu with Quit */
    NSMenuItem *appItem = [[NSMenuItem alloc] init];
    NSMenu *appMenu = [[NSMenu alloc] init];
    NSString *appName = [[NSProcessInfo processInfo] processName];
    NSMenuItem *quit = [[NSMenuItem alloc]
            initWithTitle:[NSString stringWithFormat:@"Quit %@", appName]
                   action:@selector(terminate:)
            keyEquivalent:@"q"];
    [appMenu addItem:quit];
    [appItem setSubmenu:appMenu];
    [mainMenu addItem:appItem];

    /* menus keyed by full hint path; nested submenus created on demand */
    NSMutableDictionary *menusByName = [NSMutableDictionary dictionary];
    NSArray *rows = [encoded componentsSeparatedByString:@"\n"];
    int index = 0;
    for (NSString *row in rows) {
        if ([row length] == 0) {
            index++;
            continue;
        }
        NSArray *cols = [row componentsSeparatedByString:@"\t"];
        NSString *hint = [cols count] > 0 ? cols[0] : @"";
        NSString *label = [cols count] > 1 ? cols[1] : @"";
        int keyChar = [cols count] > 2 ? [cols[2] intValue] : 0;
        NSString *flags = [cols count] > 4 ? cols[4] : @"";
        if ([label length] == 0) {
            index++;
            continue;
        }
        if ([hint length] == 0) {
            hint = @"Simulator";
        }
        /* walk/create the menu path, e.g. "Tools>Network" */
        NSMenu *menu = nil;
        NSString *path = @"";
        for (NSString *part in [hint componentsSeparatedByString:@">"]) {
            if ([part length] == 0) {
                continue;
            }
            path = [path length] == 0 ? part : [NSString stringWithFormat:@"%@>%@", path, part];
            NSMenu *next = menusByName[path];
            if (next == nil) {
                next = [[NSMenu alloc] initWithTitle:part];
                menusByName[path] = next;
                NSMenuItem *holder = [[NSMenuItem alloc] initWithTitle:part
                                                                action:nil
                                                         keyEquivalent:@""];
                [holder setSubmenu:next];
                if (menu == nil) {
                    [mainMenu addItem:holder];
                } else {
                    [menu addItem:holder];
                }
            }
            menu = next;
        }
        if (menu == nil) {
            index++;
            continue;
        }
        if ([label isEqualToString:@"-"]) {
            [menu addItem:[NSMenuItem separatorItem]];
            index++;
            continue;
        }
        NSString *keyEquivalent = @"";
        if (keyChar > 0) {
            unichar c = (unichar) keyChar;
            keyEquivalent = [[NSString stringWithCharacters:&c length:1] lowercaseString];
        }
        NSMenuItem *item = [[NSMenuItem alloc] initWithTitle:label
                                                      action:@selector(fireMenuItem:)
                                               keyEquivalent:keyEquivalent];
        [item setTarget:cn1simMenuHandler];
        [item setTag:index];
        if ([flags rangeOfString:@"c"].location != NSNotFound) {
            [item setState:NSControlStateValueOn];
        }
        [menu addItem:item];
        index++;
    }
    [NSApp setMainMenu:mainMenu];
    if (getenv("CN1_SIM_DEBUG") != NULL) {
        NSMutableArray *titles = [NSMutableArray array];
        for (NSMenuItem *mi in [mainMenu itemArray]) {
            [titles addObject:[[mi submenu] title] ?: @"?"];
        }
        fprintf(stderr, "cn1sim: menu installed: %s (%d items)\n",
                [[titles componentsJoinedByString:@","] UTF8String], index);
    }
}

/* strong implementation of the Catalyst-format menu native */
void com_codename1_impl_ios_IOSNative_setNativeMenuCommands___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_OBJECT encoded) {
    @autoreleasepool {
        NSString *s = toNSString(threadStateData, encoded);
        NSString *copy = s != nil ? [s copy] : @"";
        dispatch_async(dispatch_get_main_queue(), ^{
            cn1sim_install_menu(copy);
            [copy release];
        });
    }
}

static void cn1sim_create_window_on_main(const char *title, int width, int height) {
    NSRect rect = NSMakeRect(0, 0, width, height);
    NSWindow *win = [[NSWindow alloc]
            initWithContentRect:rect
                      styleMask:(NSWindowStyleMaskTitled | NSWindowStyleMaskClosable
                              | NSWindowStyleMaskMiniaturizable | NSWindowStyleMaskResizable)
                        backing:NSBackingStoreBuffered
                          defer:NO];
    [win setTitle:[NSString stringWithUTF8String:title]];
    CN1SimView *view = [[CN1SimView alloc] initWithFrame:rect];
    [view setWantsLayer:YES];

    CALayer *layer = [CALayer layer];
    layer.opaque = YES;
    [[view layer] addSublayer:layer];
    [[CodenameOne_GLViewController instance] attachLayer:layer width:width height:height];

    [win setContentView:view];
    [win setDelegate:[[CN1SimWindowDelegate alloc] init]];
    [win center];
    [win makeKeyAndOrderFront:nil];
    [win makeFirstResponder:view];
    [NSApp activateIgnoringOtherApps:YES];
    cn1simWindow = win;
}

/* ---- JNI natives for CN1SimHost ------------------------------------------- */

static void jni_CN1SimHost_createNativeWindow(JNIEnv *env, jclass cls, jstring title, jint width, jint height) {
    const char *t = (*env)->GetStringUTFChars(env, title, NULL);
    char *titleCopy = strdup(t != NULL ? t : "Codename One");
    if (t != NULL) {
        (*env)->ReleaseStringUTFChars(env, title, t);
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        cn1sim_create_window_on_main(titleCopy, width, height);
        free(titleCopy);
    });
}

/*
 * Resizes the native window content and the rendering surface (used on
 * rotation / skin change). SYNCHRONOUS: when this returns the new (cleared)
 * surface is live, so repaints issued afterwards are guaranteed to land on
 * it - an async resize raced the universes' relayout repaints into the old
 * texture, which the swap then discarded (blank window after rotate).
 */
static void jni_CN1SimHost_resizeWindow(JNIEnv *env, jclass cls, jint width, jint height) {
    void (^resize)(void) = ^{
        if (cn1simWindow != nil) {
            [cn1simWindow setContentSize:NSMakeSize(width, height)];
            NSView *content = [cn1simWindow contentView];
            for (CALayer *sub in [[content layer] sublayers]) {
                sub.frame = CGRectMake(0, 0, width, height);
            }
        }
        [[CodenameOne_GLViewController instance] resizeSurface:width height:height];
    };
    if (NSThread.isMainThread) {
        resize();
    } else {
        dispatch_sync(dispatch_get_main_queue(), resize);
    }
}

/* Keeps the simulator window above other applications (Always on Top). */
static void jni_CN1SimHost_setAlwaysOnTop(JNIEnv *env, jclass cls, jboolean onTop) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (cn1simWindow != nil) {
            [cn1simWindow setLevel:onTop ? NSFloatingWindowLevel : NSNormalWindowLevel];
        }
    });
}

/*
 * Runs the AppKit application loop on the calling thread, which MUST be the
 * process main thread (launch the JVM with -XstartOnFirstThread). Blocks for
 * the lifetime of the simulator.
 */
static void jni_CN1SimHost_runEventLoop(JNIEnv *env, jclass cls) {
    if (!NSThread.isMainThread) {
        fprintf(stderr, "cn1sim: runEventLoop must run on the process main thread "
                        "- launch the JVM with -XstartOnFirstThread\n");
        return;
    }
    @autoreleasepool {
        [NSApplication sharedApplication];
        [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];
        [NSApp run];
    }
}

/* defined at the end of this file (peer/media/picker section) */
static jlong jni_CN1SimHost_peerCreateWebView(JNIEnv *env, jclass cls);
static void jni_CN1SimHost_peerWebLoadURL(JNIEnv *env, jclass cls, jlong peer, jstring url);
static void jni_CN1SimHost_peerWebLoadHTML(JNIEnv *env, jclass cls, jlong peer, jstring html, jstring baseUrl);
static void jni_CN1SimHost_peerSetFrame(JNIEnv *env, jclass cls, jlong peer, jint x, jint y, jint w, jint h);
static void jni_CN1SimHost_peerRemove(JNIEnv *env, jclass cls, jlong peer);
static void jni_CN1SimHost_peerRelease(JNIEnv *env, jclass cls, jlong peer);
static jlong jni_CN1SimHost_mediaCreate(JNIEnv *env, jclass cls, jstring url, jboolean video);
static void jni_CN1SimHost_mediaControl(JNIEnv *env, jclass cls, jlong peer, jint op, jint arg);
static jint jni_CN1SimHost_mediaQuery(JNIEnv *env, jclass cls, jlong peer, jint what);
static void jni_CN1SimHost_pickFile(JNIEnv *env, jclass cls);

jint cn1sim_register_window(JNIEnv *env) {
    jclass cls = (*env)->FindClass(env, "com/codename1/impl/ios/sim/CN1SimHost");
    if (cls == NULL) {
        return JNI_ERR;
    }
    cn1simHostClass = (*env)->NewGlobalRef(env, cls);
    cn1simPointerEvent = (*env)->GetStaticMethodID(env, cls, "nativePointerEvent", "(III)V");
    cn1simKeyEvent = (*env)->GetStaticMethodID(env, cls, "nativeKeyEvent", "(II)V");
    cn1simWindowClosed = (*env)->GetStaticMethodID(env, cls, "nativeWindowClosed", "()V");
    cn1simMenuCommand = (*env)->GetStaticMethodID(env, cls, "nativeMenuCommand", "(I)V");
    cn1simWindowResized = (*env)->GetStaticMethodID(env, cls, "nativeWindowResized", "(II)V");
    cn1simEditingDone = (*env)->GetStaticMethodID(env, cls, "nativeEditingDone",
            "(Ljava/lang/String;)V");
    cn1simEditingUpdate = (*env)->GetStaticMethodID(env, cls, "nativeEditingUpdate",
            "(Ljava/lang/String;)V");
    cn1simScrollWheel = (*env)->GetStaticMethodID(env, cls, "nativeScrollWheel", "(II)V");
    cn1simFilePicked = (*env)->GetStaticMethodID(env, cls, "nativeFilePicked",
            "(Ljava/lang/String;)V");
    if (cn1simPointerEvent == NULL || cn1simKeyEvent == NULL || cn1simWindowClosed == NULL
            || cn1simMenuCommand == NULL || cn1simWindowResized == NULL
            || cn1simEditingDone == NULL || cn1simEditingUpdate == NULL
            || cn1simScrollWheel == NULL || cn1simFilePicked == NULL) {
        return JNI_ERR;
    }
    static const JNINativeMethod methods[] = {
        {"createNativeWindow", "(Ljava/lang/String;II)V", (void *) jni_CN1SimHost_createNativeWindow},
        {"runEventLoop", "()V", (void *) jni_CN1SimHost_runEventLoop},
        {"resizeWindow", "(II)V", (void *) jni_CN1SimHost_resizeWindow},
        {"setAlwaysOnTop", "(Z)V", (void *) jni_CN1SimHost_setAlwaysOnTop},
        {"editString", "(Ljava/lang/String;IIIIJIZ)V", (void *) jni_CN1SimHost_editString},
        {"peerCreateWebView", "()J", (void *) jni_CN1SimHost_peerCreateWebView},
        {"peerWebLoadURL", "(JLjava/lang/String;)V", (void *) jni_CN1SimHost_peerWebLoadURL},
        {"peerWebLoadHTML", "(JLjava/lang/String;Ljava/lang/String;)V", (void *) jni_CN1SimHost_peerWebLoadHTML},
        {"peerSetFrame", "(JIIII)V", (void *) jni_CN1SimHost_peerSetFrame},
        {"peerRemove", "(J)V", (void *) jni_CN1SimHost_peerRemove},
        {"peerRelease", "(J)V", (void *) jni_CN1SimHost_peerRelease},
        {"mediaCreate", "(Ljava/lang/String;Z)J", (void *) jni_CN1SimHost_mediaCreate},
        {"mediaControl", "(JII)V", (void *) jni_CN1SimHost_mediaControl},
        {"mediaQuery", "(JI)I", (void *) jni_CN1SimHost_mediaQuery},
        {"pickFile", "()V", (void *) jni_CN1SimHost_pickFile},
    };
    return (*env)->RegisterNatives(env, cls, methods, sizeof(methods) / sizeof(JNINativeMethod));
}

/* ---- native peers: WKWebView, AVPlayer media, file picker -------------------
 * Peers are real NSViews floated above the rendering layer at the CN1
 * component's window rectangle, exactly like the text editor. Media wraps an
 * AVPlayer (audio) optionally hosted in an AVPlayerView (video).
 */

@interface CN1SimMediaPeer : NSObject {
@public
    AVPlayer *player;
    NSView *view;
    BOOL pendingPlay;
}
@end

@implementation CN1SimMediaPeer
@end

/* resolves a peer handle to the view to place (web view or media view) */
static NSView *cn1sim_peer_view(JAVA_LONG peer) {
    NSObject *o = (NSObject *) (intptr_t) peer;
    if ([o isKindOfClass:[CN1SimMediaPeer class]]) {
        return ((CN1SimMediaPeer *) o)->view;
    }
    if ([o isKindOfClass:[NSView class]]) {
        return (NSView *) o;
    }
    return nil;
}

static jlong jni_CN1SimHost_peerCreateWebView(JNIEnv *env, jclass cls) {
    __block WKWebView *web = nil;
    void (^make)(void) = ^{
        web = [[WKWebView alloc] initWithFrame:NSMakeRect(0, 0, 10, 10)];
    };
    if (NSThread.isMainThread) {
        make();
    } else {
        dispatch_sync(dispatch_get_main_queue(), make);
    }
    return (jlong) (intptr_t) web;
}

static void jni_CN1SimHost_peerWebLoadURL(JNIEnv *env, jclass cls, jlong peer, jstring url) {
    const char *u = (*env)->GetStringUTFChars(env, url, NULL);
    NSString *ns = [NSString stringWithUTF8String:u != NULL ? u : ""];
    if (u != NULL) {
        (*env)->ReleaseStringUTFChars(env, url, u);
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        WKWebView *web = (WKWebView *) (intptr_t) peer;
        NSURL *nsu = [NSURL URLWithString:ns];
        if (nsu != nil) {
            [web loadRequest:[NSURLRequest requestWithURL:nsu]];
        }
    });
}

static void jni_CN1SimHost_peerWebLoadHTML(JNIEnv *env, jclass cls, jlong peer,
        jstring html, jstring baseUrl) {
    const char *h = (*env)->GetStringUTFChars(env, html, NULL);
    NSString *nh = [NSString stringWithUTF8String:h != NULL ? h : ""];
    if (h != NULL) {
        (*env)->ReleaseStringUTFChars(env, html, h);
    }
    NSString *nb = nil;
    if (baseUrl != NULL) {
        const char *b = (*env)->GetStringUTFChars(env, baseUrl, NULL);
        if (b != NULL) {
            nb = [NSString stringWithUTF8String:b];
            (*env)->ReleaseStringUTFChars(env, baseUrl, b);
        }
    }
    NSString *base = nb;
    dispatch_async(dispatch_get_main_queue(), ^{
        WKWebView *web = (WKWebView *) (intptr_t) peer;
        [web loadHTMLString:nh baseURL:base != nil ? [NSURL URLWithString:base] : nil];
    });
}

static void jni_CN1SimHost_peerSetFrame(JNIEnv *env, jclass cls, jlong peer,
        jint x, jint y, jint w, jint h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSView *v = cn1sim_peer_view(peer);
        if (v == nil || cn1simWindow == nil) {
            return;
        }
        if ([v superview] == nil) {
            [[cn1simWindow contentView] addSubview:v];
        }
        /* the content view is flipped - frames are top-left based */
        [v setFrame:NSMakeRect(x, y, MAX(1, w), MAX(1, h))];
    });
}

static void jni_CN1SimHost_peerRemove(JNIEnv *env, jclass cls, jlong peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSView *v = cn1sim_peer_view(peer);
        [v removeFromSuperview];
    });
}

static void jni_CN1SimHost_peerRelease(JNIEnv *env, jclass cls, jlong peer) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSObject *o = (NSObject *) (intptr_t) peer;
        if ([o isKindOfClass:[CN1SimMediaPeer class]]) {
            CN1SimMediaPeer *m = (CN1SimMediaPeer *) o;
            [m->player pause];
            [m->view removeFromSuperview];
            [m->view release];
            [m->player release];
        } else if ([o isKindOfClass:[NSView class]]) {
            [(NSView *) o removeFromSuperview];
        }
        [o release];
    });
}

static jlong jni_CN1SimHost_mediaCreate(JNIEnv *env, jclass cls, jstring url, jboolean video) {
    const char *u = (*env)->GetStringUTFChars(env, url, NULL);
    NSString *ns = [NSString stringWithUTF8String:u != NULL ? u : ""];
    if (u != NULL) {
        (*env)->ReleaseStringUTFChars(env, url, u);
    }
    __block CN1SimMediaPeer *m = nil;
    void (^make)(void) = ^{
        BOOL network = [ns hasPrefix:@"http:"] || [ns hasPrefix:@"https:"];
        NSURL *nsu = [ns hasPrefix:@"file:"] ? [NSURL URLWithString:ns]
                : [ns hasPrefix:@"/"] ? [NSURL fileURLWithPath:ns]
                : [NSURL URLWithString:ns];
        if (nsu == nil) {
            return;
        }
        m = [[CN1SimMediaPeer alloc] init];
        if (video) {
            AVPlayerView *pv = [[AVPlayerView alloc] initWithFrame:NSMakeRect(0, 0, 10, 10)];
            pv.controlsStyle = AVPlayerViewControlsStyleMinimal;
            m->view = pv;
        }
        if (!network) {
            m->player = [[AVPlayer playerWithURL:nsu] retain];
            if (m->view != nil) {
                ((AVPlayerView *) m->view).player = m->player;
            }
            return;
        }
        /*
         * Network media: AVFoundation's loader daemon refuses processes
         * without a bundle identity (NSURLError -1102 / OSStatus -12660 in a
         * bare JVM) - download through NSURLSession (which works fine) and
         * play the local file.
         */
        CN1SimMediaPeer *mm = m;
        [mm retain];
        NSString *ext = [[nsu path] pathExtension];
        NSString *tmp = [NSTemporaryDirectory() stringByAppendingPathComponent:
                [NSString stringWithFormat:@"cn1sim-media-%u.%@",
                        arc4random(), [ext length] > 0 ? ext : @"mp4"]];
        NSURLSessionDownloadTask *task = [[NSURLSession sharedSession]
                downloadTaskWithURL:nsu
                  completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {
            if (error != nil || location == nil) {
                NSLog(@"cn1sim media download failed: %@", error);
                [mm release];
                return;
            }
            [[NSFileManager defaultManager] moveItemAtURL:location
                                                    toURL:[NSURL fileURLWithPath:tmp]
                                                    error:nil];
            dispatch_async(dispatch_get_main_queue(), ^{
                mm->player = [[AVPlayer playerWithURL:[NSURL fileURLWithPath:tmp]] retain];
                if (mm->view != nil) {
                    ((AVPlayerView *) mm->view).player = mm->player;
                }
                if (mm->pendingPlay) {
                    [mm->player play];
                }
                [mm release];
            });
        }];
        [task resume];
    };
    if (NSThread.isMainThread) {
        make();
    } else {
        dispatch_sync(dispatch_get_main_queue(), make);
    }
    return (jlong) (intptr_t) m;
}

static void jni_CN1SimHost_mediaControl(JNIEnv *env, jclass cls, jlong peer, jint op, jint arg) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSObject *o = (NSObject *) (intptr_t) peer;
        if (![o isKindOfClass:[CN1SimMediaPeer class]]) {
            return;
        }
        AVPlayer *p = ((CN1SimMediaPeer *) o)->player;
        if (p == nil && op == 0) {
            /* still downloading - start as soon as the player exists */
            ((CN1SimMediaPeer *) o)->pendingPlay = YES;
            return;
        }
        switch (op) {
            case 0:
                [p play];
                /* diagnostics: surface load errors that would otherwise be
                 * silent (status stays failed, nothing plays) */
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t) (3 * NSEC_PER_SEC)),
                        dispatch_get_main_queue(), ^{
                    AVPlayerItem *item = [p currentItem];
                    NSLog(@"cn1sim media after play: playerStatus=%ld itemStatus=%ld rate=%f error=%@ itemError=%@",
                            (long) [p status], (long) [item status], [p rate],
                            [p error], [item error]);
                });
                break;
            case 1:
                [p pause];
                break;
            case 2:
                [p seekToTime:CMTimeMakeWithSeconds(arg / 1000.0, 600)];
                break;
            default:
                break;
        }
    });
}

static jint jni_CN1SimHost_mediaQuery(JNIEnv *env, jclass cls, jlong peer, jint what) {
    NSObject *o = (NSObject *) (intptr_t) peer;
    if (![o isKindOfClass:[CN1SimMediaPeer class]]) {
        return 0;
    }
    AVPlayer *p = ((CN1SimMediaPeer *) o)->player;
    switch (what) {
        case 0: { /* time ms */
            CMTime t = [p currentTime];
            return CMTIME_IS_NUMERIC(t) ? (jint) (CMTimeGetSeconds(t) * 1000) : 0;
        }
        case 1: { /* duration ms */
            CMTime d = [[p currentItem] duration];
            return CMTIME_IS_NUMERIC(d) ? (jint) (CMTimeGetSeconds(d) * 1000) : 0;
        }
        case 2: /* playing */
            return [p rate] > 0 ? 1 : 0;
        default:
            return 0;
    }
}

/* file picker - the simulator's "camera"/gallery */
static void jni_CN1SimHost_pickFile(JNIEnv *env, jclass cls) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSOpenPanel *panel = [NSOpenPanel openPanel];
        [panel setCanChooseFiles:YES];
        [panel setCanChooseDirectories:NO];
        [panel setAllowsMultipleSelection:NO];
        [panel beginWithCompletionHandler:^(NSModalResponse result) {
            NSString *path = nil;
            if (result == NSModalResponseOK && [[panel URLs] count] > 0) {
                path = [[[panel URLs] objectAtIndex:0] path];
            }
            JNIEnv *e2 = cn1sim_main_env();
            if (e2 != NULL && cn1simFilePicked != NULL) {
                jstring jp = path != nil
                        ? (*e2)->NewStringUTF(e2, [path UTF8String]) : NULL;
                (*e2)->CallStaticVoidMethod(e2, cn1simHostClass, cn1simFilePicked, jp);
                if ((*e2)->ExceptionCheck(e2)) {
                    (*e2)->ExceptionDescribe(e2);
                    (*e2)->ExceptionClear(e2);
                }
                if (jp != NULL) {
                    (*e2)->DeleteLocalRef(e2, jp);
                }
            }
        }];
    });
}
