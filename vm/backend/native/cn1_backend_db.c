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
 * SQLite persistence for server-side binaries, straight onto the engine the
 * translator already bundles (-Dcn1.sqlite=true drops cn1_sqlite3.c and the
 * amalgamation into the source root). Not com.codename1.db.Database, which routes
 * every call through CodenameOneImplementation.
 *
 * Handles are pointers cast to JAVA_LONG; 0 means "not open", so a failed open
 * needs nothing freed. The prepare/step/finalize cycle is exposed rather than
 * hidden behind an exec-string, because parameter binding is what keeps user data
 * out of the SQL text.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <string.h>
#ifndef _WIN32
#include <unistd.h> /* CN1_RESUME_THREAD expands to usleep */
#endif
/*
 * Built as stubs when the engine is left out (CN1_BACKEND_SQLITE=0), rather than
 * dropped from the build. A native whose C symbol is absent takes its JAVA method
 * with it -- see BytecodeMethod.isMethodUsedByNative -- so removing this file
 * would make Db.open link fine and do nothing. openImpl returning 0 is the "could
 * not open" answer Db already handles, so a program built without the engine gets
 * a clean IOException naming the path instead of silence.
 */
#ifdef CN1_BACKEND_NO_SQLITE

JAVA_LONG com_codename1_backend_Db_openImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_closeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    return 0;
}

JAVA_OBJECT com_codename1_backend_Db_errorImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    return newStringFromCString(threadStateData,
            "this binary was built without SQLite (CN1_BACKEND_SQLITE=0)");
}

JAVA_LONG com_codename1_backend_Db_prepareImpl___long_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT sql) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_bindStringImpl___long_int_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index, JAVA_OBJECT value) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_bindLongImpl___long_int_long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index, JAVA_LONG value) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_bindDoubleImpl___long_int_double_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index, JAVA_DOUBLE value) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_bindNullImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_bindBlobImpl___long_int_byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index, JAVA_OBJECT value) {
    return 0;
}

JAVA_INT com_codename1_backend_Db_stepImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt) {
    return -1;
}

JAVA_INT com_codename1_backend_Db_columnCountImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt) {
    return 0;
}

JAVA_OBJECT com_codename1_backend_Db_columnNameImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return JAVA_NULL;
}

JAVA_INT com_codename1_backend_Db_columnTypeImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return 5; /* TYPE_NULL */
}

JAVA_OBJECT com_codename1_backend_Db_columnStringImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return JAVA_NULL;
}

JAVA_LONG com_codename1_backend_Db_columnLongImpl___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return 0;
}

JAVA_DOUBLE com_codename1_backend_Db_columnDoubleImpl___long_int_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return 0;
}

JAVA_OBJECT com_codename1_backend_Db_columnBlobImpl___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt, JAVA_INT index) {
    return JAVA_NULL;
}

JAVA_VOID com_codename1_backend_Db_finalizeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmt) {
}

JAVA_INT com_codename1_backend_Db_changesImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    return 0;
}

JAVA_LONG com_codename1_backend_Db_lastInsertRowIdImpl___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    return 0;
}

#else

#include "cn1_sqlite3.h"

JAVA_LONG com_codename1_backend_Db_openImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    sqlite3* db = NULL;
    const char* p = path == JAVA_NULL ? NULL : stringToUTF8(threadStateData, path);
    if(p == NULL) {
        return 0;
    }
    if(sqlite3_open(p, &db) != SQLITE_OK) {
        /* sqlite3_open allocates a handle even on failure so the error can be read;
           close it here rather than leaking one per failed open. */
        if(db != NULL) {
            sqlite3_close(db);
        }
        return 0;
    }
    return (JAVA_LONG)(intptr_t)db;
}

JAVA_INT com_codename1_backend_Db_closeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    sqlite3* db = (sqlite3*)(intptr_t)handle;
    if(db == NULL) {
        return 0;
    }
    return sqlite3_close(db) == SQLITE_OK ? 0 : -1;
}

JAVA_OBJECT com_codename1_backend_Db_errorImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    sqlite3* db = (sqlite3*)(intptr_t)handle;
    const char* msg = db == NULL ? "database is not open" : sqlite3_errmsg(db);
    return msg == NULL ? JAVA_NULL : newStringFromCString(threadStateData, msg);
}

JAVA_LONG com_codename1_backend_Db_prepareImpl___long_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT sql) {
    sqlite3* db = (sqlite3*)(intptr_t)handle;
    sqlite3_stmt* stmt = NULL;
    const char* text = sql == JAVA_NULL ? NULL : stringToUTF8(threadStateData, sql);
    if(db == NULL || text == NULL) {
        return 0;
    }
    if(sqlite3_prepare_v2(db, text, -1, &stmt, NULL) != SQLITE_OK) {
        return 0;
    }
    return (JAVA_LONG)(intptr_t)stmt;
}

JAVA_INT com_codename1_backend_Db_bindStringImpl___long_int_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index, JAVA_OBJECT value) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    if(stmt == NULL) {
        return SQLITE_MISUSE;
    }
    if(value == JAVA_NULL) {
        return sqlite3_bind_null(stmt, index);
    }
    /* SQLITE_TRANSIENT: the scratch buffer stringToUTF8 returns is reused by the
       next conversion on this thread, so sqlite must take its own copy. */
    return sqlite3_bind_text(stmt, index, stringToUTF8(threadStateData, value), -1,
                             SQLITE_TRANSIENT);
}

JAVA_INT com_codename1_backend_Db_bindLongImpl___long_int_long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index, JAVA_LONG value) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    if(stmt == NULL) {
        return SQLITE_MISUSE;
    }
    return sqlite3_bind_int64(stmt, index, (sqlite3_int64)value);
}

JAVA_INT com_codename1_backend_Db_bindDoubleImpl___long_int_double_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index, JAVA_DOUBLE value) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    if(stmt == NULL) {
        return SQLITE_MISUSE;
    }
    return sqlite3_bind_double(stmt, index, value);
}

JAVA_INT com_codename1_backend_Db_bindNullImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    if(stmt == NULL) {
        return SQLITE_MISUSE;
    }
    return sqlite3_bind_null(stmt, index);
}

/* 1 = a row is available, 0 = finished, -1 = error. */
JAVA_INT com_codename1_backend_Db_stepImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    int rc;
    if(stmt == NULL) {
        return -1;
    }
    CN1_YIELD_THREAD;
    rc = sqlite3_step(stmt);
    CN1_RESUME_THREAD;
    if(rc == SQLITE_ROW) {
        return 1;
    }
    if(rc == SQLITE_DONE) {
        return 0;
    }
    return -1;
}

JAVA_INT com_codename1_backend_Db_columnCountImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    return stmt == NULL ? 0 : sqlite3_column_count(stmt);
}

JAVA_OBJECT com_codename1_backend_Db_columnNameImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    const char* name = stmt == NULL ? NULL : sqlite3_column_name(stmt, index);
    return name == NULL ? JAVA_NULL : newStringFromCString(threadStateData, name);
}

/* Mirrors SQLITE_INTEGER/FLOAT/TEXT/BLOB/NULL as 1..5. */
JAVA_INT com_codename1_backend_Db_columnTypeImpl___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    return stmt == NULL ? 5 : sqlite3_column_type(stmt, index);
}

JAVA_OBJECT com_codename1_backend_Db_columnStringImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    const unsigned char* text = stmt == NULL ? NULL : sqlite3_column_text(stmt, index);
    return text == NULL ? JAVA_NULL : newStringFromCString(threadStateData, (const char*)text);
}

JAVA_LONG com_codename1_backend_Db_columnLongImpl___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    return stmt == NULL ? 0 : (JAVA_LONG)sqlite3_column_int64(stmt, index);
}

JAVA_DOUBLE com_codename1_backend_Db_columnDoubleImpl___long_int_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    return stmt == NULL ? 0 : sqlite3_column_double(stmt, index);
}

JAVA_INT com_codename1_backend_Db_bindBlobImpl___long_int_byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index, JAVA_OBJECT value) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    JAVA_ARRAY arr;
    if(stmt == NULL) {
        return SQLITE_MISUSE;
    }
    if(value == JAVA_NULL) {
        return sqlite3_bind_null(stmt, index);
    }
    arr = (JAVA_ARRAY)value;
    /* SQLITE_TRANSIENT: sqlite copies, so the array may be collected or moved the
       moment this returns. */
    return sqlite3_bind_blob(stmt, index, (const void*)(JAVA_ARRAY_BYTE*)arr->data,
                             (int)arr->length, SQLITE_TRANSIENT);
}

JAVA_OBJECT com_codename1_backend_Db_columnBlobImpl___long_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle, JAVA_INT index) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    const void* data;
    int length;
    JAVA_OBJECT arr;
    if(stmt == NULL) {
        return JAVA_NULL;
    }
    /* sqlite3_column_bytes must be called AFTER sqlite3_column_blob: the blob call
       is what performs any needed type conversion, and the length is only correct
       once it has. */
    data = sqlite3_column_blob(stmt, index);
    length = sqlite3_column_bytes(stmt, index);
    if(data == NULL) {
        length = 0;
    }
    arr = allocArray(threadStateData, length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(length > 0) {
        memcpy((JAVA_ARRAY_BYTE*)((JAVA_ARRAY)arr)->data, data, (size_t)length);
    }
    return arr;
}

JAVA_VOID com_codename1_backend_Db_finalizeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG stmtHandle) {
    sqlite3_stmt* stmt = (sqlite3_stmt*)(intptr_t)stmtHandle;
    if(stmt != NULL) {
        sqlite3_finalize(stmt);
    }
}

JAVA_INT com_codename1_backend_Db_changesImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    sqlite3* db = (sqlite3*)(intptr_t)handle;
    return db == NULL ? 0 : sqlite3_changes(db);
}

JAVA_LONG com_codename1_backend_Db_lastInsertRowIdImpl___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    sqlite3* db = (sqlite3*)(intptr_t)handle;
    return db == NULL ? 0 : (JAVA_LONG)sqlite3_last_insert_rowid(db);
}

#endif /* CN1_BACKEND_NO_SQLITE */
