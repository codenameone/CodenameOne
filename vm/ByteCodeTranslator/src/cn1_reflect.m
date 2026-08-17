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
 * Fallback sinks for the generated-metadata registries declared in
 * cn1_reflect.h.
 *
 * Generated code registers its field tables, clazz addresses and invoke thunks
 * from __attribute__((constructor)) shims, unconditionally, whenever the
 * metadata is emitted. Whether anything is listening is a property of the
 * target: the iOS port links cn1_debugger.m, which defines these for real and
 * — being strong definitions — overrides the weak ones here at link time.
 *
 * Every other target (the clean/C target, and a host-side build of generated
 * sources) has no such runtime. Without these it would not link at all, which
 * is what stopped an interp-host build from being testable anywhere but iOS.
 *
 * These are deliberately sinks rather than a second real registry: two
 * implementations of the same table is exactly the drift the generated-metadata
 * design exists to avoid. A non-iOS device runtime that needs to *call* thunks
 * will want a real registry, and that belongs in one place shared with the
 * debugger's, not duplicated here.
 */

#include "cn1_reflect.h"

__attribute__((weak))
void cn1_debugger_register_fields(int classId, const cn1_field_entry* table, int count) {
    (void)classId; (void)table; (void)count;
}

__attribute__((weak))
void cn1_debugger_register_class(int classId, struct clazz* cls) {
    (void)classId; (void)cls;
}

__attribute__((weak))
void cn1_debugger_register_invoke_thunk(int methodId, cn1_invoke_thunk_t thunk) {
    (void)methodId; (void)thunk;
}

__attribute__((weak))
void cn1_debugger_register_static_accessor(int fieldId, cn1_static_accessor_t accessor) {
    (void)fieldId; (void)accessor;
}

/*
 * Lookups. A target that registered nothing has nothing to find, so these
 * answer null -- which is the same answer the real registries give for an
 * unknown id, and which every caller already handles.
 */

__attribute__((weak))
cn1_invoke_thunk_t cn1_reflect_thunk_for_method(int methodId) {
    (void)methodId;
    return 0;
}

__attribute__((weak))
const cn1_field_entry* cn1_reflect_field_for(int classId, int fieldId) {
    (void)classId; (void)fieldId;
    return 0;
}

__attribute__((weak))
struct clazz* cn1_reflect_clazz_for(int classId) {
    (void)classId;
    return 0;
}

__attribute__((weak))
cn1_static_accessor_t cn1_reflect_static_accessor_for(int fieldId) {
    (void)fieldId;
    return 0;
}
