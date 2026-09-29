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

/* The C half of NativeScopeApp. A port's natives look like this: Java natives whose
   bodies call back into Java, static helpers, a function with external linkage, and a
   feature behind a CN1_INCLUDE_ switch the builder left commented out. */
#include "cn1_globals.h"
#include "NativeScopeFeature.h"
#include "NativeScopeHelperTarget.h"
#include "NativeScopeRooted.h"
#include "NativeScopeDead.h"

//#define CN1_INCLUDE_SCOPETEST

#if defined(CN1_INCLUDE_SCOPETEST)
#define CN1_SCOPETEST_HAS_EXTRA 1
#endif

static JAVA_INT cn1ScopeHelper(CODENAME_ONE_THREAD_STATE, JAVA_INT v) {
    return NativeScopeHelperTarget_viaHelper___int_R_int(threadStateData, v);
}

JAVA_INT NativeScopeApp_featureStart___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT v) {
    return NativeScopeFeature_onEvent___int_R_int(threadStateData, v) + cn1ScopeHelper(threadStateData, v);
}

JAVA_INT NativeScopeApp_unusedNative___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT v) {
    return NativeScopeDead_onEvent___int_R_int(threadStateData, v);
}

JAVA_INT cn1NativeScopeExported(CODENAME_ONE_THREAD_STATE, JAVA_INT v) {
    return NativeScopeRooted_fromFreeFunction___int_R_int(threadStateData, v);
}

#ifdef CN1_INCLUDE_SCOPETEST
#include "NativeScopeSwitched.h"
JAVA_INT cn1NativeScopeSwitchedEntry(CODENAME_ONE_THREAD_STATE, JAVA_INT v) {
    return NativeScopeSwitched_cb___int_R_int(threadStateData, v);
}
#endif

#ifdef CN1_SCOPETEST_HAS_EXTRA
#include "NativeScopeDerived.h"
JAVA_INT cn1NativeScopeDerivedEntry(CODENAME_ONE_THREAD_STATE, JAVA_INT v) {
    return NativeScopeDerived_cb___int_R_int(threadStateData, v);
}
#endif
