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

#ifndef CN1_DEBUGGER_H
#define CN1_DEBUGGER_H

#ifdef CN1_ON_DEVICE_DEBUG

#include "cn1_globals.h"
// The translator-generated metadata ABI -- cn1_field_entry, cn1_invoke_arg,
// cn1_invoke_result, cn1_invoke_thunk_t and the three register_* entry points.
// These used to be declared here, which meant generated code could only be
// compiled by a target that shipped this iOS-port header, and tied the invoke
// thunks to the notion of a debugger session. The on-device interpreter binds
// through the same thunks with no proxy attached, so the translator owns them
// now; cn1_debugger.m still provides the real registry implementations, whose
// strong definitions override the weak sinks in cn1_reflect.
#include "cn1_reflect.h"


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

/*
 * cn1_field_entry and cn1_debugger_register_fields are declared in
 * cn1_reflect.h, included above. The runtime uses the field tables to answer
 * CMD_GET_OBJECT_FIELDS without any reflection / RTTI from ParparVM.
 */

/*
 * cn1_debugger_register_class is declared in cn1_reflect.h, included above.
 *
 * Everything the debugger is handed as an object reference is untrusted: the
 * IDE echoes back ids it was given earlier, and a local slot can hold a value
 * the frame never initialised on the branch it stopped in. With that registry
 * the runtime can decide whether a candidate pointer really is a Java object
 * by checking that its class word is the registered clazz for the classId it
 * claims — an exact identity test, not a range guess. Before it existed, a
 * bogus reference was dereferenced directly and took the app down mid-session
 * (issue #5333).
 */

/**
 * Copies {@code len} bytes from a possibly-invalid address, returning 0
 * instead of faulting when the address is not mapped.
 */
extern int cn1_debugger_safe_read(const void* addr, void* dst, size_t len);

/**
 * Resolves an untrusted reference to its class, or NULL when it cannot be
 * shown to be a live Java object. Every debugger path that is about to
 * dereference a reference — a local slot's contents, an objectID from the
 * IDE, an element of an object array — goes through this first.
 */
extern struct clazz* cn1_debugger_class_of(JAVA_OBJECT obj);

/** Whether obj can safely be dereferenced as a Java object. */
extern int cn1_debugger_is_valid_object(JAVA_OBJECT obj);

/**
 * Whether a reference is a tagged int rather than a heap object, and the value
 * it carries. A tagged int has no object header, so no caller may compute a
 * field address from one.
 */
extern int cn1_debugger_is_tagged_int(JAVA_OBJECT obj);
extern JAVA_INT cn1_debugger_tagged_int_value(JAVA_OBJECT obj);

/**
 * Records a reference as handed to the proxy, tests whether one was, and
 * forgets the whole set on resume.
 *
 * A registered class word survives reclamation, so it proves shape but not
 * liveness. Requiring that a wire objectID be one this debugger issued since
 * the last resume refuses a stale id from an earlier suspension instead of
 * dereferencing it.
 */
extern int cn1_debugger_note_issued(JAVA_OBJECT obj);
extern int cn1_debugger_was_issued(JAVA_OBJECT obj);
extern void cn1_debugger_forget_issued(void);

/**
 * Records a reference against the suspended thread it was obtained for, reads
 * back that owner, and drops one thread's records on its resume.
 *
 * Ownership is what lets a per-thread resume invalidate only its own ids. A
 * global clear would cut the ground from under a thread that is still stopped
 * and being inspected; clearing nothing would leave the resumed thread's ids
 * accepted after its objects can be collected. Owner 0 means the reference is
 * not tied to a suspension and survives until every thread runs again.
 */
extern int cn1_debugger_note_issued_for(JAVA_OBJECT obj, int64_t owner);
extern int64_t cn1_debugger_owner_of(JAVA_OBJECT obj);

/**
 * Records a reference reached through another, inheriting the parent's whole
 * claim. Used by the field and array commands, which arrive with only an
 * objectID and so have no thread of their own to attribute to.
 */
extern int cn1_debugger_note_issued_inheriting(JAVA_OBJECT obj, JAVA_OBJECT parent);
extern void cn1_debugger_forget_issued_for(int64_t owner);

/**
 * Opens a thread-list generation. Call before recording a refresh's threads;
 * pair with cn1_debugger_end_thread_list once every one has been noted.
 */
extern void cn1_debugger_begin_thread_list(void);

/**
 * Closes the generation, dropping objects that neither a suspension nor the
 * list just built claims, and that hang off nothing which survives. Each
 * refresh supersedes the last, so dead threads stop being rooted.
 */
extern void cn1_debugger_end_thread_list(void);

/**
 * The scalar component of an array class and its dimension count, or NULL if
 * the chain cannot be read. An array's own class is synthetic and absent from
 * the symbol table, so the component is what the proxy can actually name.
 */
extern struct clazz* cn1_debugger_array_component(struct clazz* arrayClass, int* dimsOut);

/** Resolves an objectID that arrived from the IDE, or NULL to refuse it. */
extern struct clazz* cn1_debugger_class_of_wire_id(JAVA_OBJECT obj);

/**
 * Whether a local described by one side-table row is in scope at a source
 * line. Locals out of scope are left out of the reply rather than reported
 * with the contents of storage that belongs to another scope.
 */
extern int cn1_debugger_var_in_scope(const struct cn1_var_entry* v, int line);

/* --- Method invocation -------------------------------------------------- */

/*
 * cn1_invoke_arg, cn1_invoke_result, cn1_invoke_thunk_t and
 * cn1_debugger_register_invoke_thunk are declared in cn1_reflect.h, included
 * above.
 *
 * When the debugger drives a call, the thunk runs on the suspended Java thread
 * so it has a valid threadStateData context and no collection can race it. The
 * interpreter calls the same thunks on a live thread instead, which is why the
 * constructor thunks hold their freshly allocated receiver in a C local -- the
 * collector's conservative native-stack scan is what keeps it alive there.
 *
 * methodId matches the value the symbol sidecar carries, so a consumer can look
 * up by name -> methodId and dispatch with no further mapping.
 */

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
