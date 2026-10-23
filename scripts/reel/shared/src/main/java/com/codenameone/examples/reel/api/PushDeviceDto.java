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
package com.codenameone.examples.reel.api;

import com.codename1.annotations.Mapped;

/// A device that can be sent push notifications, as the app registers it.
///
/// `provider` is the push service the device's token belongs to -- `fcm`,
/// `apns`, `huawei`, `wns` or `web` -- and `token` the token that service gave
/// the app. `platform` is the device's platform name, for the record only.
/// `newEpisodes` and `account` are what the viewer chose to be told on this
/// device: new releases of titles on their list, and their account's own news
/// (a sign-in, a failed payment, a moderation decision).
@Mapped
public record PushDeviceDto(String provider, String token, String platform, boolean newEpisodes,
        boolean account) {
}
