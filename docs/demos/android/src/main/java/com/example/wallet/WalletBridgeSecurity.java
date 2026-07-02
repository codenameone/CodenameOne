package com.example.wallet;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.util.Set;

/**
 * Android-native security helper snippets for wallet bridge examples.
 */
public final class WalletBridgeSecurity {
    private WalletBridgeSecurity() {
    }

    // tag::walletTrustedCallerSignature[]
    public static boolean isTrustedCallerSignature(Context ctx, String packageName, Set<String> allowedSha256) {
        if (ctx == null || packageName == null || packageName.isEmpty() || allowedSha256 == null || allowedSha256.isEmpty()) {
            return false;
        }
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 28) {
                info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                Signature[] sigs = info.signingInfo != null
                        ? info.signingInfo.getApkContentsSigners()
                        : null;
                return hasAllowedFingerprint(sigs, allowedSha256);
            } else {
                info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                return hasAllowedFingerprint(info.signatures, allowedSha256);
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean hasAllowedFingerprint(Signature[] sigs, Set<String> allowedSha256) throws Exception {
        if (sigs == null) return false;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (Signature s : sigs) {
            String fp = toHex(md.digest(s.toByteArray()));
            if (allowedSha256.contains(fp)) return true;
        }
        return false;
    }

    private static String toHex(byte[] data) {
        char[] digits = "0123456789ABCDEF".toCharArray();
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte b : data) {
            int value = b & 0xff;
            out.append(digits[value >>> 4]);
            out.append(digits[value & 0x0f]);
        }
        return out.toString();
    }
    // end::walletTrustedCallerSignature[]
}
