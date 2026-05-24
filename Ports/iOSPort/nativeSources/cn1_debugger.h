/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

#ifndef CN1_DEBUGGER_H
#define CN1_DEBUGGER_H

#ifdef CN1_ON_DEVICE_DEBUG

#include "cn1_globals.h"


/**
 * Boots the on-device-debug listener thread (non-blocking). Reads the
 * desktop proxy host and port from Info.plist keys CN1ProxyHost /
 * CN1ProxyPort, spawns a background thread that opens an outbound TCP
 * connection, sends a HELLO event, and services commands (set/clear
 * breakpoint, resume, step, get stack, get locals) in a loop.
 *
 * Returns immediately. If Info.plist has CN1ProxyWaitForAttach=YES, the
 * function also installs a "Waiting for debugger" overlay UIWindow so the
 * user sees something other than the splash while the wait is in progress;
 * the overlay is dismissed automatically when {@link
 * cn1_debugger_run_when_ready} fires its block.
 */
extern void cn1_debugger_start(void);

/**
 * Per-class instance-field descriptor emitted by the translator (one
 * static array per generated class), then registered with the debugger
 * runtime by a __attribute__((constructor)) shim that the translator also
 * emits. The runtime uses these to answer CMD_GET_OBJECT_FIELDS without
 * any reflection / RTTI from ParparVM.
 *
 * offset is from the start of the object struct (i.e. offsetof). type is
 * a JVM type-char ('I','J','F','D','Z','B','S','C','L' — 'L' covers
 * arrays too since arrays are JAVA_OBJECT in the struct).
 */
typedef struct cn1_field_entry {
    int fieldId;
    int offset;
    char type;
    const char* name;
} cn1_field_entry;

/**
 * Translator-generated constructors call this once at process load to
 * publish the class's field table to the debugger runtime. classId is
 * the cn1_class_id_XXX constant.
 */
extern void cn1_debugger_register_fields(int classId,
                                         const cn1_field_entry* table,
                                         int count);

/* --- Method invocation -------------------------------------------------- */

/**
 * Argument or scratch slot for a debugger-driven method invocation. All
 * args travel as a flat array of these; the thunk reads the right field
 * for each declared parameter. Floats/doubles round-trip through the bit
 * width of their integer counterparts since debug clients pass them as
 * raw 32/64-bit values.
 */
typedef union cn1_invoke_arg {
    JAVA_INT     i;
    JAVA_LONG    j;
    JAVA_FLOAT   f;
    JAVA_DOUBLE  d;
    JAVA_OBJECT  o;
} cn1_invoke_arg;

/**
 * Result of a debugger-driven method invocation. {@code type} is a JVM
 * type-char ('V', 'I', 'J', 'F', 'D', 'L', 'Z', 'B', 'S', 'C') or 'X'
 * if the call threw — in which case {@code value.o} carries the
 * Throwable.
 */
typedef struct cn1_invoke_result {
    char type;
    cn1_invoke_arg value;
} cn1_invoke_result;

/**
 * Translator-emitted per-method shim. The thunk unpacks {@code args}
 * into the typed C parameters the underlying translated function
 * expects, dispatches through {@code virtual_<sym>(...)} (instance) or
 * the static symbol (static), and packs the return into {@code result}.
 * Exceptions are caught and surfaced as result.type='X'.
 *
 * Runs on the suspended Java thread so it has a valid
 * {@code threadStateData} context.
 */
typedef void (*cn1_invoke_thunk_t)(struct ThreadLocalData* threadStateData,
                                   JAVA_OBJECT thisObj,
                                   const cn1_invoke_arg* args,
                                   cn1_invoke_result* result);

/**
 * Translator-emitted constructor registers each method's thunk at
 * process load. methodId matches the same value the sidecar carries,
 * so the proxy can look up by name → methodId and forward to the
 * device with no further mapping.
 */
extern void cn1_debugger_register_invoke_thunk(int methodId, cn1_invoke_thunk_t thunk);

#ifdef __BLOCKS__
/**
 * Defers the VM callback until the proxy reports the IDE has attached, so
 * the AppDelegate can keep `didFinishLaunchingWithOptions` returning
 * promptly and let UIKit draw the waiting overlay.
 *
 * If CN1ProxyWaitForAttach=NO (or the on-device-debug listener isn't
 * configured), the block is invoked synchronously on the calling thread.
 * Otherwise the block is stored and the proxy listener invokes it on the
 * main queue once it receives the first RESUME from the desktop proxy.
 */
extern void cn1_debugger_run_when_ready(void (^onReady)(void));
#endif

#endif // CN1_ON_DEVICE_DEBUG
#endif // CN1_DEBUGGER_H
