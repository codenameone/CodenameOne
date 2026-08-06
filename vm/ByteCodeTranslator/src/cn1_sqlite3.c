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
 * The single translation unit that compiles the bundled SQLite engine.
 *
 * Build options are set here rather than on the target, because every C target applies its
 * compiler options to the whole target: setting them globally would leak SQLITE_* macros into
 * unrelated sources, and on iOS there is no per-target preprocessor hook to use anyway. Defining
 * them immediately before the #include keeps them to this file.
 *
 * See cn1_sqlite3_README.md in this directory for what the engine is and why the amalgamation is named .h.
 */

/*
 * Flipped to a real #define by the translator when it emits this file, which it only does for
 * applications that use com.codename1.db. Keeping the switch inside the file means all three C
 * targets behave the same way without any per-target compiler flags.
 */
//#define CN1_INCLUDE_SQLITE

#ifdef CN1_INCLUDE_SQLITE

/*
 * The precompiled prefix header on iOS pulls cn1_globals.h into every translation unit, and that
 * defines YES, NO and NSLog. The amalgamation does not currently use any of those as identifiers,
 * so this is defence rather than a fix for a present-day collision - but a SQLite update that
 * introduced one would otherwise fail in a 250,000 line file with no obvious cause.
 */
#ifdef YES
#undef YES
#endif
#ifdef NO
#undef NO
#endif
#ifdef NSLog
#undef NSLog
#endif

/* Codename One runs the database from several threads, and serialised mode is what makes that
 * safe without the caller holding a lock. */
#define SQLITE_THREADSAFE 1

/* Keep temporary tables and indices in memory: an app sandbox has no useful temp directory. */
#define SQLITE_TEMP_STORE 2

/* Nothing in Codename One loads SQLite extensions, and leaving the entry point out means an
 * application cannot be talked into loading one. */
#define SQLITE_OMIT_LOAD_EXTENSION 1

/* Reject double quoted string literals. They are a MySQL habit that SQLite tolerates by turning
 * an unknown identifier into a string, which silently converts a typo into a working query that
 * returns the wrong rows. */
#define SQLITE_DQS 0

/* Declared foreign keys are enforced. SQLite's default of ignoring them surprises everyone. */
#define SQLITE_DEFAULT_FOREIGN_KEYS 1

/* Column metadata is what Cursor.getColumnName and getColumnIndex report. */
#define SQLITE_ENABLE_COLUMN_METADATA 1

/* Full text search and JSON are small and are the two extensions applications ask for. */
#define SQLITE_ENABLE_FTS5 1
#define SQLITE_ENABLE_JSON1 1

#include "cn1_sqlite3_amalgamation.h"

#endif /* CN1_INCLUDE_SQLITE */
