/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * Maps a Flutter asset path onto the Codename One resource name that the build
 * emits for it.
 *
 * <p>Flutter asset keys are relative paths ({@code assets/studies/reply_card.png},
 * or {@code packages/<pkg>/<name>} for a package asset). Codename One resources,
 * however, are <em>flat</em>: {@code getResourceAsStream} rejects any name
 * containing a {@code '/'} past the leading one, on every port. So the build
 * flattens the asset tree into root-level resources and the runtime resolves
 * through the same mangling.</p>
 *
 * <p>The encoding makes {@code '_'} an escape that is ALWAYS followed by one
 * character: {@code "__"} for a literal underscore, {@code "_s"} for the path
 * separator. Every other character is itself. That is prefix-free, so it
 * decodes one way only and no two paths share a name. The earlier rule --
 * double every {@code '_'}, then use a single {@code '_'} as the separator --
 * was not: {@code a_/b} and {@code a/_b} both became {@code a___b}, and the
 * build silently wrote one asset over the other. No shell- or
 * bundler-hostile punctuation is introduced. The {@code cn1f_} prefix
 * namespaces Flutter assets away from other app resources (and keeps them off
 * the reserved {@code raw} prefix). File extensions survive, so native builders
 * that classify bundled files by extension still see a {@code .png} as a
 * {@code .png}.</p>
 *
 * <p>Example: {@code packages/gallery_assets/assets/studies/reply_card.png} →
 * {@code /cn1f_packages_sgallery__assets_sassets_sstudies_sreply__card.png}</p>
 *
 * <p><b>Keep in sync</b> with the identical encoder in the build's
 * {@code TranscodeFlutterMojo} — the two halves of this contract are
 * deliberately tiny and duplicated rather than sharing a module, because the
 * Maven plugin must not depend on the Flutter runtime.</p>
 */
public class FlutterAssets {

    /** Prefix marking a flattened Flutter asset resource. */
    public static final String PREFIX = "cn1f_";

    private FlutterAssets() {
    }

    /**
     * The absolute Codename One resource name for a Flutter asset path.
     *
     * @param assetPath the Flutter asset key, e.g. {@code assets/foo/bar.png}
     * @return the flat resource name, e.g. {@code /cn1f_assets_sfoo_sbar.png}
     */
    public static String resourceName(String assetPath) {
        return "/" + flatName(assetPath);
    }

    /**
     * The device-pixel-ratio variants Flutter recognises, ascending. A variant
     * of {@code dir/name.ext} lives at {@code dir/<ratio>x/name.ext}.
     */
    private static final double[] VARIANTS = {1.5, 2.0, 3.0, 4.0};

    /**
     * Asset paths to try for {@code assetPath} at the given device pixel
     * ratio, most preferred first — Flutter's resolution rule: the smallest
     * variant at least as dense as the screen, then denser ones, then the
     * closest lower ones, and finally the unscaled asset.
     *
     * <p>Flutter reads the available variants from a build-generated manifest;
     * we probe instead, so the order is what matters and a missing variant
     * simply falls through to the next candidate.</p>
     */
    public static String[] variantCandidates(String assetPath, double dpr) {
        int slash = assetPath.lastIndexOf('/');
        String dir = slash < 0 ? "" : assetPath.substring(0, slash + 1);
        String file = slash < 0 ? assetPath : assetPath.substring(slash + 1);

        String[] out = new String[VARIANTS.length + 1];
        int n = 0;
        // ascending from the first variant >= dpr
        for (int i = 0; i < VARIANTS.length; i++) {
            if (VARIANTS[i] >= dpr) {
                out[n++] = dir + ratioDir(VARIANTS[i]) + file;
            }
        }
        // then descending through the ones below it
        for (int i = VARIANTS.length - 1; i >= 0; i--) {
            if (VARIANTS[i] < dpr) {
                out[n++] = dir + ratioDir(VARIANTS[i]) + file;
            }
        }
        out[n++] = assetPath;

        String[] trimmed = new String[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    private static String ratioDir(double ratio) {
        // Flutter names these "1.5x", "2.0x", "3.0x" — one decimal, always
        long whole = (long) ratio;
        long tenth = Math.round((ratio - whole) * 10);
        return whole + "." + tenth + "x/";
    }

    /** An opened asset together with the density it was authored for. */
    public static class Resolved {
        private final java.io.InputStream stream;
        private final double ratio;

        Resolved(java.io.InputStream stream, double ratio) {
            this.stream = stream;
            this.ratio = ratio;
        }

        public java.io.InputStream stream() {
            return stream;
        }

        /**
         * Device pixels per logical pixel in the loaded file — 1 for the
         * unscaled asset, 3 for a {@code 3.0x} variant. Divide the decoded
         * pixel size by this to get the image's logical size.
         */
        public double ratio() {
            return ratio;
        }
    }

    /**
     * Opens the best available variant of a Flutter asset for the current
     * screen density, or null when no candidate resolves.
     */
    public static Resolved open(Class<?> cls, String assetPath) {
        double dpr = 1;
        try {
            if (com.codename1.ui.Display.isInitialized()) {
                dpr = com.codename1.flutter.rendering.Dp.scale();
            }
        } catch (Throwable t) {
            // headless: keep the unscaled asset
        }
        String[] candidates = variantCandidates(assetPath, dpr);
        for (int i = 0; i < candidates.length; i++) {
            java.io.InputStream is = com.codename1.ui.Display.getInstance()
                    .getResourceAsStream(cls, resourceName(candidates[i]));
            if (is != null) {
                return new Resolved(is, ratioOf(candidates[i], assetPath));
            }
        }
        return null;
    }

    /** The density a resolved candidate was authored for. */
    static double ratioOf(String candidate, String assetPath) {
        if (candidate.equals(assetPath)) {
            return 1;
        }
        int end = candidate.lastIndexOf('/');
        int start = candidate.lastIndexOf('/', end - 1);
        String dir = candidate.substring(start + 1, end);
        try {
            return Double.parseDouble(dir.substring(0, dir.length() - 1));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * The flattened file name (no leading slash) for a Flutter asset path —
     * what the build writes into the output directory.
     */
    public static String flatName(String assetPath) {
        String p = assetPath;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '_') {
                sb.append("__");
            } else if (c == '/') {
                sb.append("_s");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
