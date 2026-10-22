/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.api;

import com.codename1.annotations.Mapped;

/// How one person wants the app, and what they let others see. The privacy
/// switches are enforced by the server, not only by the app: a last seen that
/// is hidden is never sent.
///
/// @param language The language, or empty for the device's.
/// @param theme "light", "dark" or "system".
/// @param notifyMessages Push for direct messages.
/// @param notifyGroups Push for group messages.
/// @param notifyCalls Ring for incoming calls.
/// @param readReceipts Send read receipts; off also hides other people's.
/// @param typingIndicators Send and show "typing".
/// @param lastSeen Who sees when this account was last online: everyone, contacts or nobody.
/// @param profilePhoto Who sees the profile photo: everyone, contacts or nobody.
/// @param disappearingSeconds The timer new chats start with, 0 for off.
/// @param screenSecurity Hide the app's content from screenshots and the app switcher.
/// @param appLock Ask for biometrics when the app opens.
@Mapped
public record PreferencesDto(
        String language,
        String theme,
        boolean notifyMessages,
        boolean notifyGroups,
        boolean notifyCalls,
        boolean readReceipts,
        boolean typingIndicators,
        String lastSeen,
        String profilePhoto,
        int disappearingSeconds,
        boolean screenSecurity,
        boolean appLock) {
}
