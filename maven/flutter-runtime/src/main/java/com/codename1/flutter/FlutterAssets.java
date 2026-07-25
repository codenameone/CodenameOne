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
 * <p>The encoding doubles every {@code '_'} and then uses {@code '_'} as the
 * path separator, which makes it unambiguous (and reversible) while emitting
 * only characters that were already legal in the source path — no shell- or
 * bundler-hostile punctuation is introduced. The {@code cn1f_} prefix
 * namespaces Flutter assets away from other app resources (and keeps them off
 * the reserved {@code raw} prefix). File extensions survive, so native builders
 * that classify bundled files by extension still see a {@code .png} as a
 * {@code .png}.</p>
 *
 * <p>Example: {@code packages/gallery_assets/assets/studies/reply_card.png} →
 * {@code /cn1f_packages_gallery__assets_assets_studies_reply__card.png}</p>
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
     * @return the flat resource name, e.g. {@code /cn1f_assets_foo_bar.png}
     */
    public static String resourceName(String assetPath) {
        return "/" + flatName(assetPath);
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
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
