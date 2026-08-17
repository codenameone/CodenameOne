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
 * Runtime clazz synthesis -- the mechanism that lets a class the AOT compiler
 * never saw be subclassed from interpreted code and still have its overrides
 * called by AOT callers.
 *
 * ParparVM allocates each class's vtable on the heap and fills it by slot
 * (see ByteCodeClass's __INIT_VTABLE_<cls>), so a subclass that exists only at
 * runtime can be built by copying the parent's clazz, copying its vtable, and
 * repointing the slots the subclass overrides at an interpreter trampoline.
 * Nothing is generated and nothing is written to executable memory, which is
 * what makes this viable on iOS.
 *
 * This file is the Phase 0 spike: the trampoline returns a fixed string instead
 * of entering an interpreter, so what is under test is purely the object model
 * -- dispatch, instanceof, and GC survival.
 */

#include "cn1_globals.h"
#include "Main_Base.h"

#include <stdlib.h>
#include <string.h>

extern struct clazz class__Main_Base;

/* The AOT-compiled Base.greet(), i.e. what occupies the slot before patching. */
extern JAVA_OBJECT Main_Base_greet___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);

/*
 * A class's vtable is malloc'd and filled by its static initializer, not at
 * load time -- so a parent that has never been touched still has vtable == 0.
 * Anything that reads or copies the parent's layout has to force that first.
 * The real interpreter has the same obligation before synthesizing a subclass.
 */
extern void __STATIC_INITIALIZER_Main_Base(CODENAME_ONE_THREAD_STATE);

/* The synthesized subclass. One is enough for the spike. */
static struct clazz* interpClazz = 0;

/*
 * Stands in for "the interpreter evaluates the overridden method". Its
 * signature has to match the slot it replaces exactly -- the AOT caller casts
 * the slot to the parent declaration's function-pointer type and calls it.
 */
static JAVA_OBJECT interp_trampoline_greet(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {
    (void)me;
    return newStringFromCString(threadStateData, "interpreted");
}

/*
 * Independently recovers the vtable slot of the AOT Base.greet() by scanning
 * for its address. The Java side separately reads the slot out of the
 * translator's symbol table; the test asserts the two agree, which is what
 * makes the emitted vtable rows trustworthy rather than merely present.
 */
JAVA_INT Main_scanSlotOfBaseGreet___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT slotCount) {
    void** vt;
    JAVA_INT i;
    __STATIC_INITIALIZER_Main_Base(threadStateData);
    vt = class__Main_Base.vtable;
    if (vt == 0) {
        return -1;
    }
    for (i = 0; i < slotCount; i++) {
        if (vt[i] == (void*)&Main_Base_greet___R_java_lang_String) {
            return i;
        }
    }
    return -1;
}

/*
 * Builds the synthetic clazz. slot and slotCount both come from the symbol
 * table the translator emitted for this build.
 *
 * The clazz is memcpy'd rather than assigned because several of its members are
 * const -- they are compile-time constants for every AOT class, and this is the
 * one caller that legitimately produces a clazz at runtime.
 *
 * classId is deliberately left as the parent's. instanceofFunction indexes a
 * static classInstanceOf[destId] table, so a fresh id would have no row and
 * every type check against the synthetic class would fail. Inheriting the
 * parent's id makes `instanceof Base` answer true, which is the semantics an
 * interpreted subclass wants; the cost is that getClass().getName() reports the
 * parent, which the interpreter overrides at its own level.
 */
JAVA_VOID Main_installInterpSubclass___int_int(CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT slotCount) {
    struct clazz* parent = &class__Main_Base;
    struct clazz* synth;
    void** vt;

    /* The parent's vtable does not exist until its static initializer runs. */
    __STATIC_INITIALIZER_Main_Base(threadStateData);

    synth = (struct clazz*)malloc(sizeof(struct clazz));
    vt = (void**)malloc(sizeof(void*) * (size_t)slotCount);

    memcpy(synth, parent, sizeof(struct clazz));
    memcpy(vt, parent->vtable, sizeof(void*) * (size_t)slotCount);
    vt[slot] = (void*)&interp_trampoline_greet;

    synth->vtable = vt;
    synth->clsName = "Main_Base$Interp";
    /* Cleared so the GC's exact clazz registry takes this address on its own
     * merits rather than inheriting the parent's "already registered" flag --
     * otherwise the conservative mark guard would not recognise it and every
     * instance would look like a false positive. */
    synth->cn1ClazzRegistered = 0;
    CN1_CLAZZ_REGISTER(synth);

    interpClazz = synth;
    (void)threadStateData;
}

/*
 * Allocates an instance of the synthetic class. Sized as the parent so every
 * inherited field sits at the offset AOT code compiled for; an interpreted
 * subclass's own fields live outside the object in interpreter state.
 */
JAVA_OBJECT Main_newInterpObject___R_java_lang_Object(CODENAME_ONE_THREAD_STATE) {
    return codenameOneGcMalloc(threadStateData, sizeof(struct obj__Main_Base), interpClazz);
}
