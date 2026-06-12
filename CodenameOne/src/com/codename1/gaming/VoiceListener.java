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
package com.codename1.gaming;

/// Notified when a voice started with `SoundPool#play(SoundEffect)` finishes playing
/// (best effort). Register one with `SoundPool#setVoiceListener(VoiceListener)`; the
/// callback is delivered on the EDT.
///
/// Completion is reported by the cross-platform fallback mixer. Some purpose-built
/// native engines -- notably Android's `android.media.SoundPool`, which exposes no
/// per-stream completion event -- cannot report it; check
/// `SoundPool#isVoiceCompletionSupported()` to find out.
public interface VoiceListener {
    /// Called when the given voice id has finished on its own (it was not looping and
    /// was not stopped early).
    void onComplete(int voiceId);
}
