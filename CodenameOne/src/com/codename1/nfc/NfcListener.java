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
package com.codename1.nfc;

/// Callback for long-running tag-discovery sessions registered via
/// [Nfc#addTagListener(NfcListener)]. Unlike [Nfc#readTag(NfcReadOptions)]
/// -- which resolves once and ends the session -- a listener stays armed
/// and receives every tag the platform produces until [Nfc#removeTagListener(NfcListener)]
/// is called.
///
/// Useful on Android (foreground dispatch / reader-mode) where the tag
/// stream is naturally multi-tag. On iOS each [#tagDiscovered(Tag)]
/// callback corresponds to a full Core NFC session that is automatically
/// re-armed.
///
/// Callbacks run on the EDT.
public interface NfcListener {

    /// Called when a tag enters the field. The tag is alive only for the
    /// duration of this call plus any pending async transceives; after the
    /// next system event it may report [NfcError#TAG_LOST].
    void tagDiscovered(Tag tag);

    /// Called when the session ends because of an error. After this fires
    /// the listener is automatically removed; re-register it to resume.
    void sessionFailed(NfcException error);
}
