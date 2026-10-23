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
package com.codenameone.examples.reel.playback;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.domain.VideoKey;
import com.codenameone.examples.reel.repository.VideoKeyRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Which protection a video is served with, and the keys behind it.
///
/// Every title is packaged twice with one content key: an HLS AES-128 ladder,
/// and a CENC MP4. AES-128 and ClearKey are answered here, from the key table.
/// Widevine and FairPlay are offered only when a vendor is configured
/// ([LicenseProvider]) and the hardware DRM switch is on; the device then gets
/// the strongest scheme it supports that the server can license.
///
/// What this protects against, honestly: AES-128 and ClearKey keys reach the
/// device in the clear over TLS, so they stop casual copying of URLs and
/// files, not a determined attacker with a debugger. That is what the vendor
/// schemes are for.
public final class Licenses {
    public static final String AES_128 = "AES_128";
    public static final String CLEARKEY = "CLEARKEY";
    public static final String WIDEVINE = "WIDEVINE";
    public static final String FAIRPLAY = "FAIRPLAY";

    private final VideoKeyRepository keys;
    private final Settings settings;
    private final LicenseProvider widevine;
    private final LicenseProvider fairplay;

    public Licenses(VideoKeyRepository keys, Settings settings, LicenseProvider widevine,
            LicenseProvider fairplay) {
        this.keys = keys;
        this.settings = settings;
        this.widevine = widevine;
        this.fairplay = fairplay;
    }

    /// The scheme to serve: the first of `wanted` (the device's, strongest
    /// first) that the video was packaged for and this server can license.
    /// AES-128 when nothing matches, since every HLS player has it.
    public String choose(List<String> wanted, List<String> packaged) {
        boolean hardware = settings.enabled(Features.HARDWARE_DRM);
        if (wanted != null) {
            for (int iter = 0; iter < wanted.size(); iter++) {
                String scheme = wanted.get(iter);
                if (WIDEVINE.equals(scheme) && hardware && widevine != null && packaged.contains(CLEARKEY)) {
                    return WIDEVINE;
                }
                if (FAIRPLAY.equals(scheme) && hardware && fairplay != null && packaged.contains(AES_128)) {
                    return FAIRPLAY;
                }
                if (CLEARKEY.equals(scheme) && packaged.contains(CLEARKEY)) {
                    return CLEARKEY;
                }
                if (AES_128.equals(scheme) && packaged.contains(AES_128)) {
                    return AES_128;
                }
            }
        }
        return AES_128;
    }

    public LicenseProvider provider(String scheme) {
        return WIDEVINE.equals(scheme) ? widevine : FAIRPLAY.equals(scheme) ? fairplay : null;
    }

    /// The 16 bytes of a video's key, for the AES-128 key URL; null when the
    /// video has none.
    @Transactional(readOnly = true)
    public byte[] aesKey(String videoId) {
        VideoKey key = keys.findById(videoId);
        return key == null ? null : Ids.unhex(key.getKeyHex());
    }

    @Transactional(readOnly = true)
    public VideoKey key(String videoId) {
        return keys.findById(videoId);
    }

    /// The W3C ClearKey license for a request body: `{"kids":[...]}` in, a JSON
    /// Web Key set out. Only the video's own key id is answered; a request for
    /// any other kid gets an empty set, which the CDM reports as a key error.
    @Transactional(readOnly = true)
    public String clearKey(String videoId, String requestJson) throws IOException {
        VideoKey key = keys.findById(videoId);
        List<Object> out = new ArrayList<Object>();
        if (key != null) {
            String kid = Base64Url.encode(Ids.unhex(key.getKeyId()));
            Map request = Json.parseObject(requestJson);
            Object kids = request == null ? null : request.get("kids");
            if (kids instanceof List) {
                List list = (List) kids;
                for (int iter = 0; iter < list.size(); iter++) {
                    Object asked = list.get(iter);
                    if (asked instanceof String && kid.equals(strip((String) asked))) {
                        Map<String, Object> jwk = new HashMap<String, Object>();
                        jwk.put("kty", "oct");
                        jwk.put("kid", kid);
                        jwk.put("k", Base64Url.encode(Ids.unhex(key.getKeyHex())));
                        out.add(jwk);
                    }
                }
            }
        }
        Map<String, Object> set = new HashMap<String, Object>();
        set.put("keys", out);
        set.put("type", "temporary");
        return Json.write(set);
    }

    /// Base64url without the padding some CDMs add.
    private static String strip(String kid) {
        String text = kid;
        while (text.endsWith("=")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.replace('+', '-').replace('/', '_');
    }
}
