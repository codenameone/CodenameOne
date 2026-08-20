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
 * Emitted by the translator only for an application that configures database encryption.
 *
 * Nothing includes it for its contents: the file's existence is the message, tested with
 * __has_include by the engine (which compiles its ciphers only when it is here), by the shared
 * native bindings (whose keying entry points would otherwise reference symbols the engine did not
 * build) and by IOSNative.m (which answers isEncryptionSupported from it). A marker rather than a
 * compiler flag because those three are separate translation units on three platforms, and this
 * is the one thing they can all see without any per-target build configuration.
 */
#ifndef CN1_SQLITE3_CIPHER_H
#define CN1_SQLITE3_CIPHER_H

#define CN1_DB_CIPHER_PRESENT 1

#endif /* CN1_SQLITE3_CIPHER_H */
