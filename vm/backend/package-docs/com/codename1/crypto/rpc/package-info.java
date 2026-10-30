/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
/// Chain network clients: [com.codename1.crypto.rpc.EvmRpc] for Ethereum JSON-RPC,
/// [com.codename1.crypto.rpc.EsploraClient] for the Bitcoin Esplora REST API and
/// [com.codename1.crypto.rpc.SolanaRpc] for Solana JSON-RPC, all written against one
/// [com.codename1.crypto.rpc.RpcTransport]. On the server that is
/// [com.codename1.backend.WebRpcTransport], which sends through a
/// [com.codename1.backend.WebClient] and shares its connection pool. Calls block the
/// handler's thread until the node answers.
package com.codename1.crypto.rpc;
