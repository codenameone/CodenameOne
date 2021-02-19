package com.codename1.maven;

import java.io.File;

/**
 * Utility for creatings file paths.
 */
class PathUtil {
    public static String path(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(File.separator);
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
