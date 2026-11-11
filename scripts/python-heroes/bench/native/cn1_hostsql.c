/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Licensed under the GNU General Public License version 2 with the Classpath
 * exception, as the rest of the Codename One Python runtime.
 *
 * HostSql's natives: the host's libsqlite3 for a native benchmark binary.
 * Strings cross as UTF-8 with explicit lengths, so a value holding U+0000
 * survives in both directions.
 */
#include "cn1_globals.h"
#include <sqlite3.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/random.h>

#define STMT ((sqlite3_stmt*)(intptr_t)st)
#define DB ((sqlite3*)(intptr_t)db)

JAVA_LONG com_pyhero_HostSql_openImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    sqlite3* h = NULL;
    const char* p = stringToUTF8(threadStateData, path);
    if (sqlite3_open(p, &h) != SQLITE_OK) {
        if (h != NULL) {
            sqlite3_close(h);
        }
        return 0;
    }
    return (JAVA_LONG)(intptr_t)h;
}

JAVA_INT com_pyhero_HostSql_closeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG db) {
    return sqlite3_close(DB);
}

JAVA_OBJECT com_pyhero_HostSql_errorImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG db) {
    const char* m = sqlite3_errmsg(DB);
    return newStringFromUtf8Len(threadStateData, m, (int)strlen(m));
}

JAVA_LONG com_pyhero_HostSql_prepareImpl___long_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG db, JAVA_OBJECT sql) {
    sqlite3_stmt* s = NULL;
    JAVA_INT n = 0;
    const char* text = stringToUTF8Len(threadStateData, sql, &n);
    if (sqlite3_prepare_v2(DB, text, n, &s, NULL) != SQLITE_OK) {
        return 0;
    }
    return (JAVA_LONG)(intptr_t)s;
}

JAVA_INT com_pyhero_HostSql_bindStringImpl___long_int_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i, JAVA_OBJECT v) {
    JAVA_INT n = 0;
    const char* text = stringToUTF8Len(threadStateData, v, &n);
    return sqlite3_bind_text(STMT, i, text, n, SQLITE_TRANSIENT);
}

JAVA_INT com_pyhero_HostSql_bindLongImpl___long_int_long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i, JAVA_LONG v) {
    return sqlite3_bind_int64(STMT, i, (sqlite3_int64)v);
}

JAVA_INT com_pyhero_HostSql_bindDoubleImpl___long_int_double_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i, JAVA_DOUBLE v) {
    return sqlite3_bind_double(STMT, i, v);
}

JAVA_INT com_pyhero_HostSql_bindNullImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    return sqlite3_bind_null(STMT, i);
}

JAVA_INT com_pyhero_HostSql_bindBlobImpl___long_int_byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i, JAVA_OBJECT v) {
    JAVA_ARRAY a = (JAVA_ARRAY)v;
    return sqlite3_bind_blob(STMT, i, (const void*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(a), (int)a->length,
                             SQLITE_TRANSIENT);
}

JAVA_INT com_pyhero_HostSql_stepImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st) {
    return sqlite3_step(STMT);
}

JAVA_INT com_pyhero_HostSql_repreparesImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st) {
    return sqlite3_stmt_status(STMT, SQLITE_STMTSTATUS_REPREPARE, 0);
}

JAVA_INT com_pyhero_HostSql_columnCountImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st) {
    return sqlite3_column_count(STMT);
}

JAVA_OBJECT com_pyhero_HostSql_columnNameImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    const char* m = sqlite3_column_name(STMT, i);
    return m == NULL ? JAVA_NULL : newStringFromUtf8Len(threadStateData, m, (int)strlen(m));
}

JAVA_INT com_pyhero_HostSql_columnTypeImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    return sqlite3_column_type(STMT, i);
}

JAVA_OBJECT com_pyhero_HostSql_columnStringImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    const unsigned char* t = sqlite3_column_text(STMT, i);
    return t == NULL ? JAVA_NULL
            : newStringFromUtf8Len(threadStateData, (const char*)t, sqlite3_column_bytes(STMT, i));
}

JAVA_LONG com_pyhero_HostSql_columnLongImpl___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    return (JAVA_LONG)sqlite3_column_int64(STMT, i);
}

JAVA_DOUBLE com_pyhero_HostSql_columnDoubleImpl___long_int_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    return sqlite3_column_double(STMT, i);
}

JAVA_OBJECT com_pyhero_HostSql_columnBlobImpl___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG st, JAVA_INT i) {
    const void* data = sqlite3_column_blob(STMT, i);
    int n = data == NULL ? 0 : sqlite3_column_bytes(STMT, i);
    JAVA_OBJECT arr = allocArray(threadStateData, n, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if (n > 0) {
        memcpy((JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), data, (size_t)n);
    }
    return arr;
}

JAVA_VOID com_pyhero_HostSql_finalizeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG st) {
    sqlite3_finalize(STMT);
}

/* Ready to run again with nothing bound: a kept statement holds no lock and
   no reference to the values of its last run. */
JAVA_INT com_pyhero_HostSql_resetImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG st) {
    int rc = sqlite3_reset(STMT);
    sqlite3_clear_bindings(STMT);
    return rc;
}

JAVA_LONG com_pyhero_HostSql_changesImpl___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG db) {
    return (JAVA_LONG)sqlite3_changes(DB);
}

JAVA_LONG com_pyhero_HostSql_lastInsertRowIdImpl___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG db) {
    return (JAVA_LONG)sqlite3_last_insert_rowid(DB);
}

/* User-defined functions. The engine calls cn1_hostsql_function on the thread
 * that stepped the statement; it hands the call to HostSql.callFunction, whose
 * symbol appearing here is also what keeps the translator from dropping it. */
#define ARGV ((sqlite3_value**)(intptr_t)argv)
#define CTX ((sqlite3_context*)(intptr_t)ctx)

extern JAVA_VOID com_pyhero_HostSql_callFunction___long_int_int_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx, JAVA_INT id, JAVA_INT argc, JAVA_LONG argv);

static void cn1_hostsql_function(sqlite3_context* c, int argc, sqlite3_value** v) {
    com_pyhero_HostSql_callFunction___long_int_int_long(getThreadLocalData(), (JAVA_LONG)(intptr_t)c,
            (JAVA_INT)(intptr_t)sqlite3_user_data(c), argc, (JAVA_LONG)(intptr_t)v);
}

JAVA_INT com_pyhero_HostSql_createFunctionImpl___long_java_lang_String_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG db, JAVA_OBJECT name, JAVA_INT nargs, JAVA_INT id) {
    const char* n = stringToUTF8(threadStateData, name);
    return sqlite3_create_function_v2(DB, n, nargs, SQLITE_UTF8, (void*)(intptr_t)id,
            cn1_hostsql_function, NULL, NULL, NULL);
}

JAVA_INT com_pyhero_HostSql_valueTypeImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG argv, JAVA_INT i) {
    return sqlite3_value_type(ARGV[i]);
}

JAVA_LONG com_pyhero_HostSql_valueLongImpl___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG argv, JAVA_INT i) {
    return (JAVA_LONG)sqlite3_value_int64(ARGV[i]);
}

JAVA_DOUBLE com_pyhero_HostSql_valueDoubleImpl___long_int_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG argv, JAVA_INT i) {
    return sqlite3_value_double(ARGV[i]);
}

JAVA_OBJECT com_pyhero_HostSql_valueStringImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG argv, JAVA_INT i) {
    const unsigned char* t = sqlite3_value_text(ARGV[i]);
    return t == NULL ? JAVA_NULL : newStringFromUtf8Len(threadStateData, (const char*)t, sqlite3_value_bytes(ARGV[i]));
}

JAVA_OBJECT com_pyhero_HostSql_valueBlobImpl___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG argv, JAVA_INT i) {
    const void* data = sqlite3_value_blob(ARGV[i]);
    int n = data == NULL ? 0 : sqlite3_value_bytes(ARGV[i]);
    JAVA_OBJECT arr = allocArray(threadStateData, n, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if (n > 0) {
        memcpy((JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), data, (size_t)n);
    }
    return arr;
}

JAVA_VOID com_pyhero_HostSql_resultNullImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx) {
    sqlite3_result_null(CTX);
}

JAVA_VOID com_pyhero_HostSql_resultLongImpl___long_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx, JAVA_LONG v) {
    sqlite3_result_int64(CTX, (sqlite3_int64)v);
}

JAVA_VOID com_pyhero_HostSql_resultDoubleImpl___long_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx, JAVA_DOUBLE v) {
    sqlite3_result_double(CTX, v);
}

JAVA_VOID com_pyhero_HostSql_resultStringImpl___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx, JAVA_OBJECT v) {
    JAVA_INT n = 0;
    const char* text = stringToUTF8Len(threadStateData, v, &n);
    sqlite3_result_text(CTX, text, n, SQLITE_TRANSIENT);
}

JAVA_VOID com_pyhero_HostSql_resultBlobImpl___long_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx, JAVA_OBJECT v) {
    JAVA_ARRAY a = (JAVA_ARRAY)v;
    sqlite3_result_blob(CTX, CN1_ARRAY_DATA(a), a->length, SQLITE_TRANSIENT);
}

JAVA_VOID com_pyhero_HostSql_resultErrorImpl___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctx, JAVA_OBJECT m) {
    JAVA_INT n = 0;
    const char* text = stringToUTF8Len(threadStateData, m, &n);
    sqlite3_result_error(CTX, text, n);
}

/* HostEntropy: getentropy answers at most 256 bytes a call. */
JAVA_INT com_pyhero_HostEntropy_fillImpl___byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    JAVA_ARRAY a = (JAVA_ARRAY)out;
    unsigned char* p = (unsigned char*)(JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(a);
    size_t left = (size_t)a->length;
    while (left > 0) {
        size_t n = left > 256 ? 256 : left;
        if (getentropy(p, n) != 0) {
            return 1;
        }
        p += n;
        left -= n;
    }
    return 0;
}
