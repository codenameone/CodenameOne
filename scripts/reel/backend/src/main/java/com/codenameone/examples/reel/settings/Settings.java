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
package com.codenameone.examples.reel.settings;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.api.BrandingDto;
import com.codenameone.examples.reel.api.FeatureFlagDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.PresetDto;
import com.codenameone.examples.reel.domain.Setting;
import com.codenameone.examples.reel.repository.SettingRepository;

import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The switches an admin turns Reel into a different service with, the preset
/// that sets them together, and the branding.
///
/// Every switch is read here on every request that depends on it, so a change
/// takes effect at once on every server instance: the rows are the truth and
/// nothing is cached. The client reads the same values from `/api/config` to
/// hide what is off, but it is this class the server asks before it serves.
@Component
public class Settings {
    private static final String FLAG = "flag.";
    private static final String PRESET = "preset";
    private static final String BRAND = "brand.";

    private final SettingRepository rows;
    private final String currency;

    public Settings(SettingRepository rows, @Value("${reel.currency:USD}") String currency) {
        this.rows = rows;
        this.currency = currency.trim().length() == 3 ? currency.trim() : "USD";
    }

    /// The currency every price is in.
    public String currency() {
        return currency;
    }

    /// Whether a switch is on. A switch nobody has set has the value the
    /// streaming preset gives it.
    @Transactional(readOnly = true)
    public boolean enabled(String feature) {
        Setting row = rows.findById(FLAG + feature);
        if (row == null) {
            return defaultFor(Features.PRESET_STREAMING, feature);
        }
        return "true".equals(row.getValue());
    }

    /// Answers 403 when a switch is off: the one way a disabled feature is
    /// refused, so the message is the same everywhere.
    public void require(String feature) {
        if (!enabled(feature)) {
            throw new ResponseStatusException(403, "That is turned off on this service");
        }
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagDto> flags() {
        List<FeatureFlagDto> out = new ArrayList<FeatureFlagDto>();
        for (int iter = 0; iter < Features.ALL.length; iter++) {
            String key = Features.ALL[iter];
            boolean on = enabled(key);
            out.add(new FeatureFlagDto(key, label(key), group(key), on, on ? "true" : "false"));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public FeatureFlagDto setFlag(String by, String key, boolean on) throws IOException {
        if (!known(key)) {
            throw new ResponseStatusException(404, "No such switch");
        }
        put(FLAG + key, on ? "true" : "false", by);
        return new FeatureFlagDto(key, label(key), group(key), on, on ? "true" : "false");
    }

    @Transactional(readOnly = true)
    public String preset() {
        Setting row = rows.findById(PRESET);
        return row == null ? Features.PRESET_STREAMING : row.getValue();
    }

    @Transactional(readOnly = true)
    public List<PresetDto> presets() {
        String active = preset();
        List<PresetDto> out = new ArrayList<PresetDto>();
        out.add(new PresetDto(Features.PRESET_STREAMING, "Streaming service",
                "Curated titles behind a subscription, household profiles, downloads and hardware DRM.",
                Features.PRESET_STREAMING.equals(active)));
        out.add(new PresetDto(Features.PRESET_VIDEO_PLATFORM, "Video platform",
                "Anyone uploads, everything is free with ads, viewers report and moderators review.",
                Features.PRESET_VIDEO_PLATFORM.equals(active)));
        out.add(new PresetDto(Features.PRESET_ACADEMY, "Training academy",
                "Courses with quizzes, branching lessons and certificates; no ads.",
                Features.PRESET_ACADEMY.equals(active)));
        return out;
    }

    /// Sets every switch the way a preset says, and remembers the preset.
    @Transactional(rollbackFor = IOException.class)
    public List<FeatureFlagDto> applyPreset(String by, String preset) throws IOException {
        if (!Features.PRESET_STREAMING.equals(preset) && !Features.PRESET_VIDEO_PLATFORM.equals(preset)
                && !Features.PRESET_ACADEMY.equals(preset)) {
            throw new ResponseStatusException(404, "No such preset");
        }
        for (int iter = 0; iter < Features.ALL.length; iter++) {
            put(FLAG + Features.ALL[iter], defaultFor(preset, Features.ALL[iter]) ? "true" : "false", by);
        }
        put(PRESET, preset, by);
        return flags();
    }

    @Transactional(readOnly = true)
    public BrandingDto branding() {
        return new BrandingDto(read(BRAND + "name", "Reel"),
                read(BRAND + "tagline", "Stories, courses and creators in one place"),
                read(BRAND + "primary", "#e50914"), read(BRAND + "accent", "#f5c518"),
                read(BRAND + "logo", ""));
    }

    @Transactional(rollbackFor = IOException.class)
    public BrandingDto setBranding(String by, BrandingDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        put(BRAND + "name", bounded(wanted.serviceName(), 40, "the service name", true), by);
        put(BRAND + "tagline", bounded(wanted.tagline(), 120, "the tagline", false), by);
        put(BRAND + "primary", colour(wanted.primaryColor()), by);
        put(BRAND + "accent", colour(wanted.accentColor()), by);
        String logo = bounded(wanted.logoUrl(), 255, "the logo address", false);
        if (logo.length() > 0 && !logo.startsWith("https://") && !logo.startsWith("/")) {
            throw new ResponseStatusException(400, "The logo must be an https address");
        }
        put(BRAND + "logo", logo, by);
        return branding();
    }

    /// A value nobody has to be signed in to read, such as the media signing
    /// key's generation. Not for secrets.
    @Transactional(readOnly = true)
    public String read(String name, String fallback) {
        Setting row = rows.findById(name);
        return row == null ? fallback : row.getValue();
    }

    @Transactional(rollbackFor = IOException.class)
    public void put(String name, String value, String by) throws IOException {
        Setting row = rows.findById(name);
        Instant now = Instant.now();
        if (row == null) {
            row = new Setting();
            row.setName(name);
            row.setValue(value);
            row.setUpdatedAt(now);
            row.setUpdatedBy(by == null ? "" : by);
            rows.insert(row);
        } else {
            row.setValue(value);
            row.setUpdatedAt(now);
            row.setUpdatedBy(by == null ? "" : by);
            rows.save(row);
        }
    }

    /// What a preset sets a switch to.
    static boolean defaultFor(String preset, String feature) {
        if (Features.PRESET_VIDEO_PLATFORM.equals(preset)) {
            return Features.ADS.equals(feature) || Features.OPEN_UPLOADS.equals(feature)
                    || Features.REPORTS.equals(feature) || Features.RENTALS.equals(feature)
                    || Features.INTERACTIVE.equals(feature);
        }
        if (Features.PRESET_ACADEMY.equals(preset)) {
            return Features.SUBSCRIPTIONS.equals(feature) || Features.INTERACTIVE.equals(feature)
                    || Features.CERTIFICATES.equals(feature) || Features.SHARING_DETECTION.equals(feature)
                    || Features.WATERMARK.equals(feature) || Features.DOWNLOADS.equals(feature);
        }
        // Streaming: everything a subscription service has, courses and their
        // certificates included, and no uploads from strangers.
        return !Features.OPEN_UPLOADS.equals(feature) && !Features.WATERMARK.equals(feature);
    }

    private static boolean known(String key) {
        for (int iter = 0; iter < Features.ALL.length; iter++) {
            if (Features.ALL[iter].equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String label(String key) {
        if (Features.SUBSCRIPTIONS.equals(key)) {
            return "Paid plans";
        }
        if (Features.RENTALS.equals(key)) {
            return "Rentals";
        }
        if (Features.ADS.equals(key)) {
            return "Ads on ad-supported plans and free titles";
        }
        if (Features.OPEN_UPLOADS.equals(key)) {
            return "Anyone may upload";
        }
        if (Features.INTERACTIVE.equals(key)) {
            return "Quizzes, polls and branching";
        }
        if (Features.CERTIFICATES.equals(key)) {
            return "Course certificates";
        }
        if (Features.PROFILES.equals(key)) {
            return "Household profiles";
        }
        if (Features.DOWNLOADS.equals(key)) {
            return "Offline downloads";
        }
        if (Features.SHARING_DETECTION.equals(key)) {
            return "Password sharing detection";
        }
        if (Features.HARDWARE_DRM.equals(key)) {
            return "Hardware DRM where available";
        }
        if (Features.WATERMARK.equals(key)) {
            return "Viewer watermark";
        }
        return "Viewer reports";
    }

    private static String group(String key) {
        if (Features.SUBSCRIPTIONS.equals(key) || Features.RENTALS.equals(key) || Features.ADS.equals(key)) {
            return "Money";
        }
        if (Features.HARDWARE_DRM.equals(key) || Features.WATERMARK.equals(key)
                || Features.SHARING_DETECTION.equals(key) || Features.DOWNLOADS.equals(key)) {
            return "Protection";
        }
        if (Features.OPEN_UPLOADS.equals(key) || Features.REPORTS.equals(key)) {
            return "Community";
        }
        return "Viewing";
    }

    private static String bounded(String value, int max, String what, boolean required) {
        String text = value == null ? "" : value.trim();
        if (required && text.length() == 0) {
            throw new ResponseStatusException(400, "Fill in " + what);
        }
        if (text.length() > max) {
            throw new ResponseStatusException(400, "That is too long for " + what);
        }
        return text;
    }

    /// A `#rrggbb` colour, or 400.
    static String colour(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() != 7 || text.charAt(0) != '#') {
            throw new ResponseStatusException(400, "A colour is # and six hex digits");
        }
        for (int iter = 1; iter < 7; iter++) {
            char c = text.charAt(iter);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                throw new ResponseStatusException(400, "A colour is # and six hex digits");
            }
        }
        return text;
    }
}
