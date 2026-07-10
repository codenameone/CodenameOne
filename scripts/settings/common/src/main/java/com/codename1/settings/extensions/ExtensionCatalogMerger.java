package com.codename1.settings.extensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtensionCatalogMerger {
    private ExtensionCatalogMerger() {
    }

    public static List<ExtensionDescriptor> preserveCompatibilityMetadata(
            List<ExtensionDescriptor> refreshed, List<ExtensionDescriptor> bundled) {
        Map<String, ExtensionDescriptor> fallback = new LinkedHashMap<String, ExtensionDescriptor>();
        if (bundled != null) {
            for (ExtensionDescriptor descriptor : bundled) {
                fallback.put(key(descriptor), descriptor);
            }
        }
        ArrayList<ExtensionDescriptor> merged = new ArrayList<ExtensionDescriptor>();
        if (refreshed == null) {
            return merged;
        }
        for (ExtensionDescriptor descriptor : refreshed) {
            ExtensionDescriptor bundledDescriptor = fallback.get(key(descriptor));
            merged.add(descriptor.withCompatibilityFallback(bundledDescriptor));
        }
        return merged;
    }

    private static String key(ExtensionDescriptor descriptor) {
        String name = descriptor == null ? "" : descriptor.name().toLowerCase();
        StringBuilder key = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') {
                key.append(ch);
            }
        }
        return key.toString();
    }
}
