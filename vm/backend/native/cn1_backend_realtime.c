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
 * The headless backend's real-time natives (com.codename1.backend.BoardRealtime). Each
 * forwards to the shared body in cn1_realtime_impl.h, the one the Linux port uses.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "cn1_realtime_impl.h"

JAVA_INT com_codename1_backend_BoardRealtime_nFeatures___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1RtImpl_rtFeatures(threadStateData);
}

JAVA_INT com_codename1_backend_BoardRealtime_nEnter___int_int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT priority, JAVA_INT cpu, JAVA_BOOLEAN isolate) {
    return cn1RtImpl_rtEnter(threadStateData, priority, cpu, isolate);
}

JAVA_VOID com_codename1_backend_BoardRealtime_nLeave__(CODENAME_ONE_THREAD_STATE) {
    cn1RtImpl_rtLeave(threadStateData);
}

JAVA_VOID com_codename1_backend_BoardRealtime_nStartPeriod___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG periodNanos) {
    cn1RtImpl_rtStartPeriod(threadStateData, periodNanos);
}

JAVA_INT com_codename1_backend_BoardRealtime_nWaitNextPeriod___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1RtImpl_rtWaitNextPeriod(threadStateData);
}

JAVA_VOID com_codename1_backend_BoardRealtime_nFreeze___int_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_INT budget, JAVA_OBJECT error) {
    cn1RtImpl_rtFreeze(threadStateData, budget, error);
}

JAVA_VOID com_codename1_backend_BoardRealtime_nThaw__(CODENAME_ONE_THREAD_STATE) {
    cn1RtImpl_rtThaw(threadStateData);
}

JAVA_INT com_codename1_backend_BoardRealtime_nStatistics___long_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    return cn1RtImpl_rtStatistics(threadStateData, out);
}

JAVA_OBJECT com_codename1_backend_BoardRealtime_nLastViolation___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return cn1RtImpl_rtLastViolation(threadStateData);
}

JAVA_INT com_codename1_backend_BoardRealtime_nLockMemory___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1RtImpl_rtLockMemory(threadStateData);
}

JAVA_VOID com_codename1_backend_BoardRealtime_nSetMemoryLimit___long_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_LONG bytes, JAVA_OBJECT error) {
    cn1RtImpl_rtSetMemoryLimit(threadStateData, bytes, error);
}
