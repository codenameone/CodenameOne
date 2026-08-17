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
#ifndef __CN1_REFLECT_H__
#define __CN1_REFLECT_H__

/**
 * The ABI between translator-generated metadata and whatever consumes it.
 *
 * ParparVM has no reflection: struct clazz carries no name-to-method table, so
 * nothing can call a method it did not name at compile time. What fills that
 * gap is generated metadata -- a per-method invoke thunk and a per-class field
 * offset table, both emitted under CN1_ON_DEVICE_DEBUG (see
 * ByteCodeClass#appendOnDeviceDebugInvokeThunks and
 * #appendOnDeviceDebugFieldTable) and registered at process load.
 *
 * These declarations used to live in the iOS port's cn1_debugger.h, which made
 * the generated metadata unbuildable on any other target and tied it to a
 * debugger session existing. They are not debugger-specific: the on-device
 * interpreter binds framework calls through exactly the same thunks, with no
 * proxy attached. They live here so the translator owns them and every target
 * can compile what it generates.
 *
 * cn1_debugger.h includes this header rather than redeclaring these types.
 */

#include "cn1_globals.h"

/**
 * One instance field of one class. Emitted per class as a static table and
 * published by a __attribute__((constructor)) shim the translator also emits.
 *
 * offset is from the start of the object struct (i.e. offsetof). type is a JVM
 * type-char ('I','J','F','D','Z','B','S','C','L' -- 'L' covers arrays too,
 * since an array is a JAVA_OBJECT in the struct).
 */
typedef struct cn1_field_entry {
    int fieldId;
    int offset;
    char type;
    const char* name;
} cn1_field_entry;

/**
 * Argument or scratch slot for a generically dispatched call. All arguments
 * travel as a flat array of these and the thunk reads the field matching each
 * declared parameter. Floats and doubles round-trip through the bit width of
 * their integer counterparts, since callers pass them as raw 32/64-bit values.
 */
typedef union cn1_invoke_arg {
    JAVA_INT     i;
    JAVA_LONG    j;
    JAVA_FLOAT   f;
    JAVA_DOUBLE  d;
    JAVA_OBJECT  o;
} cn1_invoke_arg;

/**
 * Result of a generically dispatched call. {@code type} is a JVM type-char
 * ('V','I','J','F','D','L','Z','B','S','C'), or 'X' if the call threw -- in
 * which case {@code value.o} carries the Throwable.
 *
 * A constructor thunk reports 'L' and returns the constructed object, even
 * though a constructor's Java return type is void.
 */
typedef struct cn1_invoke_result {
    char type;
    cn1_invoke_arg value;
} cn1_invoke_result;

/**
 * Translator-emitted per-method shim. Unpacks {@code args} into the typed C
 * parameters the translated function expects, dispatches through
 * {@code virtual_<sym>} (instance), the plain symbol (static or constructor),
 * and packs the return into {@code result}. Exceptions are caught and surfaced
 * as result.type=='X' rather than unwinding past the caller.
 *
 * For a constructor thunk {@code thisObj} is ignored: the thunk allocates its
 * own receiver and hands it back through {@code result}.
 */
typedef void (*cn1_invoke_thunk_t)(struct ThreadLocalData* threadStateData,
                                   JAVA_OBJECT thisObj,
                                   const cn1_invoke_arg* args,
                                   cn1_invoke_result* result);

/** Publishes a class's field table. Called from generated constructors. */
extern void cn1_debugger_register_fields(int classId,
                                         const cn1_field_entry* table,
                                         int count);

/**
 * Publishes a class's {@code clazz} address under its classId, so a consumer
 * can verify that something handed to it as an object reference really points
 * at an object of a known class rather than at arbitrary memory.
 */
extern void cn1_debugger_register_class(int classId, struct clazz* cls);

/** Publishes one method's invoke thunk under its methodId. */
extern void cn1_debugger_register_invoke_thunk(int methodId, cn1_invoke_thunk_t thunk);

/**
 * Translator-emitted accessor for one static field.
 *
 * A static field has no receiver, so the offsetof trick that covers instance
 * fields does not apply: the translator emits a named C global per field plus
 * typed {@code get_static_}/{@code set_static_} functions around it, and there
 * is no table to index. This wraps that pair in one uniform signature so a
 * caller holding only a fieldId can read or write it.
 *
 * When {@code write} is zero the accessor fills {@code value} and sets
 * {@code *type} to the JVM type-char; otherwise it stores {@code value}. Going
 * through the generated getter rather than the global directly is what runs the
 * class's static initializer first, which is the whole reason the getter exists.
 */
typedef void (*cn1_static_accessor_t)(struct ThreadLocalData* threadStateData,
                                      int write,
                                      cn1_invoke_arg* value,
                                      char* type);

/** Publishes one static field's accessor under its fieldId. */
extern void cn1_debugger_register_static_accessor(int fieldId, cn1_static_accessor_t accessor);

/** The accessor registered for a static fieldId, or null. */
extern cn1_static_accessor_t cn1_reflect_static_accessor_for(int fieldId);

/*
 * Lookups over the tables above.
 *
 * The debugger keeps these registries for its own use and reaches them through
 * file-static helpers. The on-device interpreter needs the same lookups from
 * another translation unit and with no debugger session attached, so they are
 * exported here. A target without the registries links the weak no-op
 * definitions in cn1_reflect and gets nulls, which every caller already has to
 * handle -- a pushed program may legitimately name a symbol this build lacks.
 */

/** The invoke thunk registered for a methodId, or null. */
extern cn1_invoke_thunk_t cn1_reflect_thunk_for_method(int methodId);

/** The field entry for (classId, fieldId), or null. */
extern const cn1_field_entry* cn1_reflect_field_for(int classId, int fieldId);

/** The clazz registered under a classId, or null. */
extern struct clazz* cn1_reflect_clazz_for(int classId);

#endif // __CN1_REFLECT_H__
