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

/*
 * The ciphers are built only for an application that configures encryption, which the translator
 * says by emitting cn1_sqlite3_cipher.h beside this file. The engine is SQLite3MC and carries
 * several cipher implementations; an application that never encrypts has no use for any of them,
 * and with them compiled out this is plain SQLite -- measured at about 950KB smaller on an arm64
 * object. Tested with __has_include rather than a define so the shared bindings and IOSNative.m,
 * which are other translation units, can reach the same answer.
 */
#if defined(__has_include)
#  if __has_include("cn1_sqlite3_cipher.h")
#    define CN1_INCLUDE_SQLCIPHER 1
#  endif
#endif

#ifdef CN1_INCLUDE_SQLITE

#ifndef CN1_INCLUDE_SQLCIPHER
/*
 * No cipher was asked for, so none is built. SQLite3MC reads these before it compiles each
 * cipher, and with every one off it produces an engine that behaves exactly like upstream
 * SQLite -- which is what an application that only stores plaintext should be carrying.
 */
#define HAVE_CIPHER_AES_128_CBC 0
#define HAVE_CIPHER_AES_256_CBC 0
#define HAVE_CIPHER_CHACHA20 0
#define HAVE_CIPHER_SQLCIPHER 0
#define HAVE_CIPHER_RC4 0
#define HAVE_CIPHER_ASCON128 0
#define HAVE_CIPHER_AEGIS 0
#endif /* CN1_INCLUDE_SQLCIPHER */

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

/*
 * Deliberately NOT set here: SQLITE_DQS and SQLITE_DEFAULT_FOREIGN_KEYS.
 *
 * Both are compile-time only, and this engine is not the only one Codename One runs. Android uses
 * the platform's SQLite and the simulator uses the JDBC driver's, and neither can be told to
 * change either setting -- there is no pragma for DQS at all. So setting them here would not make
 * applications stricter; it would make the same SQL behave differently depending on the port, and
 * on iOS depending on whether encryption is switched on, since that is what replaces Apple's
 * engine with this one. `SELECT "value"` would work everywhere except where this build runs, and
 * a declared foreign key would be enforced only there.
 *
 * A portable database is the point of this whole exercise, so this build keeps SQLite's defaults
 * and applications ask for what they want: `PRAGMA foreign_keys = ON` works on every port, and
 * DatabaseConformanceSuite pins both behaviours so a future divergence is a test failure rather
 * than a surprise in somebody's app.
 */

/* Column metadata is what Cursor.getColumnName and getColumnIndex report. */
#define SQLITE_ENABLE_COLUMN_METADATA 1

/* Full text search and JSON are small and are the two extensions applications ask for. */
#define SQLITE_ENABLE_FTS5 1
#define SQLITE_ENABLE_JSON1 1

/*
 * Hardware-accelerated ciphers, only where the toolchain has actually enabled the instructions.
 *
 * The engine has two ways of reaching them. Where the compiler advertises the feature globally --
 * __ARM_FEATURE_CRYPTO on Apple's arm64, __AES__ on an x86 build with AES-NI turned on -- the
 * intrinsics are simply available and it uses them. Otherwise it tags individual functions with
 * __attribute__((target(...))), and neither the clang used for the Linux and Windows arm64
 * cross-builds nor clang-cl honours that for these intrinsics: the arm64 build fails on
 * "always_inline function 'vaeseq_u8' requires target feature 'aes'", and the x86 one on an
 * unknown __m256i.
 *
 * Forcing the whole binary to require crypto extensions or AVX2 would exclude real hardware, so
 * that path uses the software implementation instead. It is slower, and it runs everywhere.
 */
#if !defined(__ARM_FEATURE_CRYPTO) && !defined(__AES__)
#define SQLITE3MC_OMIT_AES_HARDWARE_SUPPORT 1
#endif

/*
 * AEGIS is not a cipher Codename One selects -- the on-disk format is SQLCipher's AES-256-CBC --
 * so its VAES/AVX2 acceleration is all liability and no benefit here. Off unconditionally.
 */
#define AEGIS_OMIT_AES_HARDWARE_SUPPORT 1

#include "cn1_sqlite3_amalgamation.h"

#endif /* CN1_INCLUDE_SQLITE */
