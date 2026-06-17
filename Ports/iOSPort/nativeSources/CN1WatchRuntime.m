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
#include "TargetConditionals.h"
#if TARGET_OS_WATCH
// Watch runtime glue. The app-agnostic half of the watchOS bootstrap:
//   * cn1_watch_runtime_start - initialise the ParparVM constant pool then hand
//     off to the app-specific cn1_watch_app_main() (emitted into the generated
//     CN1WatchBootstrap.m) which sets the main class, inits Display (starting
//     the EDT) and schedules the lifecycle callback.
//   * cn1_watch_runtime_paint  - drain the op queue into the Core Graphics
//     surface by driving the render-driver's drawFrame (CN1WatchViewController).
//   * cn1_watch_runtime_pointer* - translate crown/tap input into CN1 pointer
//     events.
// It also provides no-op stubs for the symbols that live in watch-excluded
// sources (the 3D GL bridge in CN1GL3D.m, and the app-suspend bookkeeping that
// the iOS app delegate owns) so the watch slice links.
#import "CN1WatchHost.h"
#import "CodenameOne_GLViewController.h"
#include "cn1_globals.h"
#include "java_lang_NullPointerException.h"
#include "java_lang_RuntimeException.h"
#include <pthread.h>
#include <signal.h>

// Mirror CodenameOne_GLAppDelegate's installSignalHandlers (that file is the
// UIApplication delegate, excluded on watchOS). The ParparVM runtime relies on
// converting a BAD_ACCESS (SIGSEGV) into a Java NullPointerException rather than
// crashing, so a stray null/dangling deref in a peer/native path is recoverable
// (the EDT unwinds to its run loop) instead of taking the whole app down.
extern void throwException(struct ThreadLocalData* threadStateData, JAVA_OBJECT exception);

static void cn1WatchSignalHandler(int sig) {
    if (sig == SIGSEGV || sig == SIGBUS) {
        throwException(getThreadLocalData(), __NEW_INSTANCE_java_lang_NullPointerException(getThreadLocalData()));
    } else {
        throwException(getThreadLocalData(), __NEW_INSTANCE_java_lang_RuntimeException(getThreadLocalData()));
    }
}

static void cn1WatchInstallSignalHandlers(void) {
    signal(SIGABRT, cn1WatchSignalHandler);
    signal(SIGILL, cn1WatchSignalHandler);
    signal(SIGSEGV, cn1WatchSignalHandler);
    signal(SIGFPE, cn1WatchSignalHandler);
    signal(SIGBUS, cn1WatchSignalHandler);
    signal(SIGPIPE, cn1WatchSignalHandler);
}

extern void initConstantPool(void);
// Emitted per-app into CN1WatchBootstrap.m: runs <Main>Stub.main, which inits
// Display (starting the EDT) and blocks this thread inside initVM (see
// IOSNative.m) exactly like UIApplicationMain blocks the iOS main thread.
extern void cn1_watch_app_main(void);

extern void pointerPressedC(int* x, int* y, int length);
extern void pointerDraggedC(int* x, int* y, int length);
extern void pointerReleasedC(int* x, int* y, int length);

static BOOL cn1WatchRuntimeStarted = NO;

// Dedicated bootstrap thread. On iOS the main thread runs Stub.main and is then
// consumed forever by UIApplicationMain. On watchOS the SwiftUI run loop already
// owns the main thread, so the VM bootstrap (which blocks forever in initVM)
// must run on its own thread to keep SwiftUI + the paint pump alive.
static void *cn1WatchVMThread(void *arg) {
    (void)arg;
    cn1_watch_app_main();
    return NULL;
}

void cn1_watch_runtime_start(const char *watchMainClass) {
    (void)watchMainClass;
    if (cn1WatchRuntimeStarted) {
        return;
    }
    cn1WatchRuntimeStarted = YES;
    cn1WatchInstallSignalHandlers();
    initConstantPool();
    pthread_t vmThread;
    if (pthread_create(&vmThread, NULL, cn1WatchVMThread, NULL) == 0) {
        pthread_detach(vmThread);
    }
}

void cn1_watch_runtime_paint(void) {
    [[CodenameOne_GLViewController instance] drawFrame:CGRectZero];
}

void cn1_watch_runtime_pointerPressed(int x, int y) {
    int xs[1] = { x };
    int ys[1] = { y };
    pointerPressedC(xs, ys, 1);
}

void cn1_watch_runtime_pointerDragged(int x, int y) {
    int xs[1] = { x };
    int ys[1] = { y };
    pointerDraggedC(xs, ys, 1);
}

void cn1_watch_runtime_pointerReleased(int x, int y) {
    int xs[1] = { x };
    int ys[1] = { y };
    pointerReleasedC(xs, ys, 1);
}

// --- App-suspend bookkeeping (owned by CodenameOne_GLAppDelegate.m on iOS,
// which is excluded from the watch slice). ---
BOOL isAppSuspended = NO;
int mallocWhileSuspended = 0;

// --- 3D GL bridge (CN1GL3D.m) is OpenGL/Metal based and excluded on watchOS.
// Provide no-op stubs so NativeLookup registration links; 3D is unsupported. ---
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateContext___R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetViewPeer___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dDestroyContext___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {}
void com_codename1_impl_ios_IOSNative_gl3dSetContinuous___long_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_BOOLEAN continuous) {}
void com_codename1_impl_ios_IOSNative_gl3dRequestRender___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateFloatBuffer___float_1ARRAY_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT data, JAVA_INT floatCount) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dUpdateFloatBuffer___long_float_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT floatCount) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateShortBuffer___short_1ARRAY_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT data, JAVA_INT indexCount) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dUpdateShortBuffer___long_short_1ARRAY_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT indexCount) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateTexture___int_1ARRAY_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dDisposeBuffer___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG bufferPeer) {}
void com_codename1_impl_ios_IOSNative_gl3dDisposeTexture___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG texturePeer) {}
void com_codename1_impl_ios_IOSNative_gl3dDisposePipeline___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG pipelinePeer) {}
JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_OBJECT key, JAVA_OBJECT mslSource, JAVA_INT blendMode, JAVA_INT cullMode, JAVA_INT depthTest, JAVA_INT depthWrite) { return 0; }
void com_codename1_impl_ios_IOSNative_gl3dClear___long_int_boolean_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_INT argbColor, JAVA_BOOLEAN clearColor, JAVA_BOOLEAN clearDepth) {}
void com_codename1_impl_ios_IOSNative_gl3dSetViewport___long_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {}
void com_codename1_impl_ios_IOSNative_gl3dDrawIndexed___long_long_long_int_long_int_int_float_1ARRAY_int_long_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_LONG iboPeer, JAVA_INT indexCount, JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats, JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap) {}
void com_codename1_impl_ios_IOSNative_gl3dDrawArrays___long_long_long_int_int_int_float_1ARRAY_int_long_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG contextPeer, JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_INT vertexCount, JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats, JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap) {}

#endif // TARGET_OS_WATCH
