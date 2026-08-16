package com.codename1.flutter;

/**
 * An {@link ImageProvider} that loads a bundled asset — Flutter's
 * {@code AssetImage}. The optional {@code package} qualifies the asset's
 * owning package ({@code packages/<package>/<name>}), matching Flutter's
 * asset resolution.
 */
public class AssetImage extends ImageProvider {

    private final String assetName;
    private String packageName;
    private Object bundle;

    public AssetImage(String assetName) {
        this.assetName = assetName;
    }

    /**
     * Named parameter setter for the Dart {@code package:} parameter. The
     * transpiler escapes the reserved word {@code package} to {@code package_}.
     */
    public void package_(String v) {
        this.packageName = v;
    }

    /** Named parameter setter for the Dart {@code bundle:} parameter. */
    public void bundle(Object v) {
        this.bundle = v;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getPackage() {
        return packageName;
    }

    /**
     * The classpath-relative asset path, honoring the optional package
     * qualifier ({@code packages/<package>/<name>}).
     */
    public String resolvedName() {
        return qualify(assetName, packageName);
    }

    /**
     * Prefixes an asset name with its owning package, Flutter's
     * {@code packages/<package>/<name>}. Shared so every way of naming an asset —
     * {@code AssetImage}, {@code Image.asset} — resolves to the same file.
     */
    public static String qualify(String assetName, String packageName) {
        if (assetName != null && packageName != null && !assetName.startsWith("packages/")) {
            return "packages/" + packageName + "/" + assetName;
        }
        return assetName;
    }

    @Override
    public String sourceKey() {
        return "asset:" + resolvedName();
    }
}
