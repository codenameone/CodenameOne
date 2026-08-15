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
#include <unistd.h>
#include <signal.h>

// Mirror CodenameOne_GLAppDelegate's installSignalHandlers (that file is the
// UIApplication delegate, excluded on watchOS). The ParparVM runtime relies on
// converting a BAD_ACCESS (SIGSEGV) into a Java NullPointerException rather than
// crashing, so a stray null/dangling deref in a peer/native path is recoverable
// (the EDT unwinds to its run loop) instead of taking the whole app down.
extern void throwException(struct ThreadLocalData* threadStateData, JAVA_OBJECT exception);

// The translated lifecycle entry points, the same two CodenameOne_GLAppDelegate calls on iOS.
extern void com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(
        CN1_THREAD_STATE_SINGLE_ARG);
extern void com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(
        CN1_THREAD_STATE_SINGLE_ARG);

// Defined at the bottom of this file, where the rest of the watch-excluded iOS symbols live.
extern BOOL isAppSuspended;

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

/// Defined below, next to the phase-forwarding it belongs to.
static void cn1WatchReplayPendingPhase(void);

void cn1_watch_runtime_paint(void) {
    // Before the frame: a phase that arrived during launch is delivered as soon as the Java side
    // exists, rather than waiting for the next real transition that may never come.
    cn1WatchReplayPendingPhase();
    [[CodenameOne_GLViewController instance] drawFrame:CGRectZero];
}

// The watch equivalents of CodenameOne_GLAppDelegate's cn1ApplicationDidEnterBackground /
// cn1ApplicationWillEnterForeground, which are in a file the watch slice excludes.
//
// Stopping the paint pump is not the same thing as suspending the app: it only means no frames
// are produced. Without these the generated stub never received a lifecycle callback, so the
// application's stop() was never called on suspension and its start() was never called again on
// resume -- timers and resources stayed live in the background, and refresh-on-foreground logic
// that the same lifecycle class runs on the phone never ran on the watch.
//
// Guarded on the runtime being up: watchOS can send a transition before the VM thread has
// initialised the constant pool, and calling into a translated method then would fault.
/// Phases seen before the Java side could take them, in the order they happened: 1 background,
/// 2 foreground.
///
/// A QUEUE, not a single slot. Keeping only the last one looked equivalent -- surely all the app
/// needs is where it ended up -- and it is not, because readiness can change while the queue is
/// stalled. The watch backgrounds before the VM is up, applicationWillResignActive stops the paint
/// pump (the thing that drains this), the VM then finishes initialising and the stub's run() calls
/// start() while the app is in the background. On return, overwriting the queued background with
/// foreground meant the app was never told to stop and its foreground arrived unbalanced: the
/// resources start() acquired kept running through the whole suspension.
///
/// Four entries is more than the runtime can actually accumulate -- transitions alternate, and the
/// queue is drained on the next one -- but it is bounded rather than assumed.
#define CN1_WATCH_MAX_PENDING_PHASES 4
static int cn1WatchPendingPhases[CN1_WATCH_MAX_PENDING_PHASES];
static int cn1WatchPendingPhaseCount = 0;

/// Guards the queue. It used to be touched only from the main thread -- transitions and the paint
/// pump both run there -- but the drain below waits for readiness off that thread.
static pthread_mutex_t cn1WatchPhaseLock = PTHREAD_MUTEX_INITIALIZER;

static BOOL cn1WatchDrainThreadRunning = NO;

/// Records a phase for later delivery, collapsing a repeat of the one already at the tail.
///
/// watchOS can send the same transition twice; delivering it twice would hand the stub a second
/// stop() or a second start() that its `stopped` guard would swallow anyway, so the queue stays
/// the shortest faithful description of what happened.
static void cn1WatchQueuePhase(int phase) {
    if (cn1WatchPendingPhaseCount > 0
            && cn1WatchPendingPhases[cn1WatchPendingPhaseCount - 1] == phase) {
        return;
    }
    if (cn1WatchPendingPhaseCount >= CN1_WATCH_MAX_PENDING_PHASES) {
        // Cannot happen with alternating transitions. If it ever does, the OLDEST goes: the app's
        // current state is described by the newest entries, and losing the tail would strand it in
        // the wrong one.
        for (int i = 1; i < CN1_WATCH_MAX_PENDING_PHASES; i++) {
            cn1WatchPendingPhases[i - 1] = cn1WatchPendingPhases[i];
        }
        cn1WatchPendingPhaseCount = CN1_WATCH_MAX_PENDING_PHASES - 1;
    }
    cn1WatchPendingPhases[cn1WatchPendingPhaseCount++] = phase;
}

/// Whether the Java lifecycle is installed, which is a different question from whether the runtime
/// was started.
///
/// cn1_watch_runtime_start sets its flag and then hands off to a THREAD that has yet to reach
/// Display.init, so the flag is true for a window in which IOSImplementation.instance is still
/// null. Forwarding a phase into that window dereferences null inside translated code, on a thread
/// with no Java frame to unwind to.
///
/// Published from IOSNative.initVM's watch branch, immediately after the IOSImplementation
/// callback that installs the lifecycle and schedules the app start. That branch then blocks its
/// thread forever, mirroring UIApplicationMain, so there is no later point to publish from: a call
/// placed after the stub's main would never run.
///
/// It used to be inferred from `[CodenameOne_GLViewController instance] != nil` on the reasoning
/// that the implementation creates the view controller during Display.init. That accessor
/// LAZY-ALLOCATES: asking made it non-nil, so the test answered yes from the moment the runtime
/// flag was set and proved nothing at all. A phase forwarded in that window reached a half-built
/// VM, or delivered stop() before init() and start() had ever run.
static volatile BOOL cn1WatchJavaLifecycleReady = NO;

static BOOL cn1WatchJavaReady(void) {
    return cn1WatchJavaLifecycleReady;
}

static void cn1WatchDeliverPhase(int phase) {
    if (phase == 1) {
        com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(
                CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    } else if (phase == 2) {
        com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(
                CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
}

/// Called from IOSNative.initVM's watch branch once the Java lifecycle callback has run.
///
/// Anything queued while the VM was coming up is handed over immediately: the paint pump is the
/// other drain and it is stopped while the watch is in the background, which is exactly when a
/// queued background transition is waiting.
void cn1_watch_runtime_markJavaReady(void) {
    cn1WatchJavaLifecycleReady = YES;
    cn1WatchReplayPendingPhase();
}

/// Hands over, in order, every phase the app could not be told about yet.
///
/// Called from the paint pump AND from each incoming transition. The pump alone is not enough: it
/// is stopped while the watch is in the background, which is exactly when a queued background
/// transition is waiting, so a foreground arriving next has to flush the backlog itself before
/// recording anything of its own.
/// True while a drain is copying and delivering the queue.
///
/// Clearing the queue before delivering is necessary -- a delivery runs translated code and must
/// not be replayed re-entrantly -- but on its own it opened a window: the bootstrap thread could
/// copy a queued BACKGROUND phase and be descheduled, the main thread's foreground transition then
/// found an empty queue and delivered itself first, and the older background arrived after it. The
/// app ends up stopped and minimized while visibly on screen.
///
/// So the queue being empty is not the question a new transition should ask. It asks whether a
/// drain is in flight, and if one is, it queues behind the batch already being delivered.
static BOOL cn1WatchDraining = NO;

static void cn1WatchReplayPendingPhase(void) {
    if (!cn1WatchJavaReady()) {
        return;
    }
    int pending[CN1_WATCH_MAX_PENDING_PHASES];
    int count;
    pthread_mutex_lock(&cn1WatchPhaseLock);
    if (cn1WatchDraining) {
        // Another thread owns the batch. Its own loop picks up anything queued meanwhile.
        pthread_mutex_unlock(&cn1WatchPhaseLock);
        return;
    }
    cn1WatchDraining = YES;
    // Invariant for the loop below: the lock is held on entry to every iteration. That is what
    // makes "the queue is empty" and "ownership is released" one indivisible decision.
    //
    // Testing the queue, releasing the lock, and THEN clearing ownership left a window of exactly
    // the length of that gap. A transition arriving inside it was queued by cn1WatchHandlePhase --
    // correctly, a drain was still in flight -- and then declined a drain thread of its own,
    // because this drain's thread had not finished either. Ownership was cleared a moment later
    // with the phase still sitting in the queue and nothing left to deliver it. In the background
    // case that is unrecoverable on its own: the paint pump is stopped, so no later paint comes to
    // notice, and the app stays logically foregrounded for the whole suspension.
    for (;;) {
        count = cn1WatchPendingPhaseCount;
        for (int i = 0; i < count; i++) {
            pending[i] = cn1WatchPendingPhases[i];
        }
        // Cleared BEFORE delivering, and under the lock: a delivery runs translated code, and
        // neither a re-entrant call nor the drain thread must replay what is already on its way.
        cn1WatchPendingPhaseCount = 0;
        if (count == 0) {
            // Nothing queued, observed under the same lock that now hands ownership back. A
            // transition can only arrive after this unlock, and it finds no drain in flight.
            cn1WatchDraining = NO;
            pthread_mutex_unlock(&cn1WatchPhaseLock);
            return;
        }
        pthread_mutex_unlock(&cn1WatchPhaseLock);
        for (int i = 0; i < count; i++) {
            cn1WatchDeliverPhase(pending[i]);
        }
        // Round again, because a transition that arrived during those deliveries queued behind
        // them rather than overtaking them -- and this drain is the one that owes it a delivery.
        pthread_mutex_lock(&cn1WatchPhaseLock);
    }
}

/// Records or delivers one transition, preserving order against anything still queued.
///
/// A transition is never delivered ahead of an earlier one that is still waiting -- that is what
/// turned a background/foreground pair across the readiness boundary into a foreground the stub
/// could not balance.
/// Waits for the Java side to come up and then drains the queue.
///
/// The pump cannot be the only drain. applicationWillResignActive stops it, so a phase queued
/// while the watch is in the background sits there while the VM finishes initialising -- the stub's
/// run() calls start() and nothing delivers the stop() that should follow it, for the whole
/// suspension. Waiting on the readiness transition itself is the trigger that does not depend on
/// something else happening first.
///
/// A detached thread rather than a timer, because timers are scheduled on a run loop the watch is
/// no longer servicing. It exits as soon as the queue drains or the wait is hopeless.
static void *cn1WatchPhaseDrainThread(void *arg) {
    (void)arg;
    // 30s at 50ms. Display.init is milliseconds away in practice; the bound exists so a VM that
    // never comes up does not leave a thread spinning for the life of the process.
    for (int i = 0; i < 600; i++) {
        pthread_mutex_lock(&cn1WatchPhaseLock);
        BOOL done = cn1WatchPendingPhaseCount == 0;
        pthread_mutex_unlock(&cn1WatchPhaseLock);
        if (done) {
            break;
        }
        if (cn1WatchJavaReady()) {
            cn1WatchReplayPendingPhase();
            break;
        }
        usleep(50 * 1000);
    }
    pthread_mutex_lock(&cn1WatchPhaseLock);
    cn1WatchDrainThreadRunning = NO;
    pthread_mutex_unlock(&cn1WatchPhaseLock);
    return NULL;
}

/// Arms the drain thread, at most one at a time.
static void cn1WatchArmPhaseDrain(void) {
    pthread_mutex_lock(&cn1WatchPhaseLock);
    BOOL alreadyRunning = cn1WatchDrainThreadRunning;
    cn1WatchDrainThreadRunning = YES;
    pthread_mutex_unlock(&cn1WatchPhaseLock);
    if (alreadyRunning) {
        return;
    }
    pthread_t drain;
    if (pthread_create(&drain, NULL, cn1WatchPhaseDrainThread, NULL) == 0) {
        pthread_detach(drain);
    } else {
        pthread_mutex_lock(&cn1WatchPhaseLock);
        cn1WatchDrainThreadRunning = NO;
        pthread_mutex_unlock(&cn1WatchPhaseLock);
    }
}

static void cn1WatchHandlePhase(int phase) {
    cn1WatchReplayPendingPhase();
    pthread_mutex_lock(&cn1WatchPhaseLock);
    // cn1WatchDraining, not just a non-empty queue: a drain that has already copied the batch has
    // emptied the queue while the older phases are still on their way, and delivering directly
    // into that window is what let a foreground overtake the background before it.
    BOOL queued = !cn1WatchJavaReady() || cn1WatchDraining || cn1WatchPendingPhaseCount > 0;
    if (queued) {
        cn1WatchQueuePhase(phase);
    }
    pthread_mutex_unlock(&cn1WatchPhaseLock);
    if (queued) {
        // Do not wait for the next paint or the next transition: neither is guaranteed to come
        // while the watch is suspended, which is exactly when this queue is non-empty.
        cn1WatchArmPhaseDrain();
        return;
    }
    cn1WatchDeliverPhase(phase);
}

void cn1_watch_runtime_didEnterBackground(void) {
    // Native bookkeeping is unconditional -- it is this file's own state, not the VM's.
    isAppSuspended = YES;
    cn1WatchHandlePhase(1);
}

void cn1_watch_runtime_willEnterForeground(void) {
    isAppSuspended = NO;
    cn1WatchHandlePhase(2);
}

/// Whether the gesture in progress had its press delivered.
///
/// Dropping input before Java exists is not enough on its own: a press at t0 that is dropped,
/// followed by readiness at t1 and a release at t2, hands Java a release for a press it never
/// saw and leaves the drag state machine believing a pointer is still down. A gesture is
/// forwarded whole or not at all.
///
/// Written and read only from the SwiftUI gesture callbacks, which run on the main thread.
static volatile BOOL cn1WatchGestureLive = NO;

/// The three below are the input half of the readiness gate the phase path already has.
///
/// cn1_watch_runtime_start() launches the VM on a background pthread and returns, but the
/// generated SwiftUI gesture is live from onAppear -- so a touch in that window reached
/// IOSImplementation.pointerPressedCallback() through a still-null static `instance`. On the
/// watch that window is easy to hit: the face is already under the user's finger.
///
/// DROPPED rather than queued. A press recorded before the first form existed would be replayed
/// onto whatever is under those coordinates once it does, which is a synthetic tap the user never
/// aimed anywhere; the phases are queued because a lifecycle transition missed is a state the app
/// never learns about, while an input event missed is one the user simply repeats.
void cn1_watch_runtime_pointerPressed(int x, int y) {
    if (!cn1WatchJavaReady()) {
        return;
    }
    cn1WatchGestureLive = YES;
    int xs[1] = { x };
    int ys[1] = { y };
    pointerPressedC(xs, ys, 1);
}

void cn1_watch_runtime_pointerDragged(int x, int y) {
    if (!cn1WatchJavaReady() || !cn1WatchGestureLive) {
        return;
    }
    int xs[1] = { x };
    int ys[1] = { y };
    pointerDraggedC(xs, ys, 1);
}

void cn1_watch_runtime_pointerReleased(int x, int y) {
    if (!cn1WatchJavaReady() || !cn1WatchGestureLive) {
        return;
    }
    cn1WatchGestureLive = NO;
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
