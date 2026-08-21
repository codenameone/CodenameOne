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

/*
 * SQLite bindings. The implementation is shared with the other native C port; see
 * cn1_db_sqlite_impl.h, which is always emitted and provides either the real engine bindings or
 * stubs that report no database support, so this always links.
 */
#include "cn1_db_sqlite_impl.h"

CN1_DB_DEFINE_NATIVES(cn1DbLinuxEngine)

/*
 * The entry points, spelled out rather than pasted together.
 *
 * The bodies still come from the shared macro above, instantiated under a private prefix, so
 * there is one copy of the SQLite logic for both native ports. What changed is that the names
 * ParparVM actually calls are now written out: the macro built them with "##", and a symbol
 * that only exists after preprocessing is invisible to the native signature verifier, which
 * reads the sources as text. It reported all 28 of these as unimplemented -- correctly, on the
 * evidence available to it.
 *
 * Each one forwards and nothing else. Keeping them literal also puts the exact signature in
 * front of anyone changing the Java side: a name that drifts fails the verifier here, with the
 * symbol it expected, rather than at link time inside a generated project.
 */


JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_sqlColIsNull___long_int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlColIsNull___long_int_R_boolean(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_sqlDbApplyKey___long_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlDbApplyKey___long_java_lang_String_R_boolean(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_sqlDbExists___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    return cn1DbLinuxEngine_sqlDbExists___java_lang_String_R_boolean(threadStateData, __cn1Arg1);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_sqlDbInTransaction___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return cn1DbLinuxEngine_sqlDbInTransaction___long_R_boolean(threadStateData, __cn1Arg1);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_sqlDbIsCipherAvailable___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return cn1DbLinuxEngine_sqlDbIsCipherAvailable___R_boolean(threadStateData);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_sqlStmtStep___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return cn1DbLinuxEngine_sqlStmtStep___long_R_boolean(threadStateData, __cn1Arg1);
}

JAVA_DOUBLE com_codename1_impl_linux_LinuxNative_sqlColDouble___long_int_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlColDouble___long_int_R_double(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_sqlColCount___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return cn1DbLinuxEngine_sqlColCount___long_R_int(threadStateData, __cn1Arg1);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_sqlDbApplyKeyStatus___long_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlDbApplyKeyStatus___long_java_lang_String_R_int(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_sqlStmtParameterCount___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return cn1DbLinuxEngine_sqlStmtParameterCount___long_R_int(threadStateData, __cn1Arg1);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_sqlColLong___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlColLong___long_int_R_long(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_sqlDbOpen___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    return cn1DbLinuxEngine_sqlDbOpen___java_lang_String_R_long(threadStateData, __cn1Arg1);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_sqlStmtPrepare___long_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlStmtPrepare___long_java_lang_String_R_long(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_sqlColBlob___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlColBlob___long_int_R_byte_1ARRAY(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_sqlColName___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlColName___long_int_R_byte_1ARRAY(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_sqlColText___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    return cn1DbLinuxEngine_sqlColText___long_int_R_byte_1ARRAY(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlDbClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    cn1DbLinuxEngine_sqlDbClose___long(threadStateData, __cn1Arg1);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlDbDelete___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    cn1DbLinuxEngine_sqlDbDelete___java_lang_String(threadStateData, __cn1Arg1);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlDbExecScript___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    cn1DbLinuxEngine_sqlDbExecScript___long_java_lang_String(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlDbRekey___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    cn1DbLinuxEngine_sqlDbRekey___long_java_lang_String(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtBindBlob___long_int_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_OBJECT __cn1Arg3) {
    cn1DbLinuxEngine_sqlStmtBindBlob___long_int_byte_1ARRAY(threadStateData, __cn1Arg1, __cn1Arg2, __cn1Arg3);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtBindDouble___long_int_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_DOUBLE __cn1Arg3) {
    cn1DbLinuxEngine_sqlStmtBindDouble___long_int_double(threadStateData, __cn1Arg1, __cn1Arg2, __cn1Arg3);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtBindLong___long_int_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_LONG __cn1Arg3) {
    cn1DbLinuxEngine_sqlStmtBindLong___long_int_long(threadStateData, __cn1Arg1, __cn1Arg2, __cn1Arg3);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtBindNull___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    cn1DbLinuxEngine_sqlStmtBindNull___long_int(threadStateData, __cn1Arg1, __cn1Arg2);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtBindText___long_int_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_OBJECT __cn1Arg3) {
    cn1DbLinuxEngine_sqlStmtBindText___long_int_byte_1ARRAY(threadStateData, __cn1Arg1, __cn1Arg2, __cn1Arg3);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtExecuteAndFinalize___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    cn1DbLinuxEngine_sqlStmtExecuteAndFinalize___long(threadStateData, __cn1Arg1);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtFinalize___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    cn1DbLinuxEngine_sqlStmtFinalize___long(threadStateData, __cn1Arg1);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sqlStmtReset___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    cn1DbLinuxEngine_sqlStmtReset___long(threadStateData, __cn1Arg1);
}
