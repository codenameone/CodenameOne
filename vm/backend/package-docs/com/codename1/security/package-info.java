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
/// The part of the client's cryptography a server shares with it: the
/// portable Java digests and message authentication codes, and the one-time
/// passwords built on them.
///
/// - [Otp] -- RFC 4226 and RFC 6238 one-time passwords: the codes an
///   authenticator application shows. Being the same class the client runs,
///   a code made on a device is the code a server expects.
/// - [Base32] -- the encoding an authenticator's shared secret travels in.
/// - [Hash] / [Hmac] -- MD5, SHA-1 and the SHA-2 family, and HMAC over each,
///   written in Java. They are what [Otp] computes with.
///
/// A server reaches for these to verify a second factor and for little else.
/// What a request path computes -- a password hash, a token's signature, a
/// digest of something large -- goes through
/// [com.codename1.backend.Crypto], which is OpenSSL in a packaged server and
/// several times faster.
///
/// The client has more in this package -- ciphers, signatures, key storage --
/// which a server does not: see the client API reference.
package com.codename1.security;
