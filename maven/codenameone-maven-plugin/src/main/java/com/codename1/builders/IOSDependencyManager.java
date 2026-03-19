package com.codename1.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum IOSDependencyManager {
    AUTO,
    COCOAPODS,
    SPM,
    BOTH,
    NONE;

    static IOSDependencyConfig resolve(BuildRequest request, String iosPods) throws BuildException {
        IOSDependencyManager explicitMode = fromHint(request.getArg("ios.dependencyManager", "auto"));
        List<SwiftPackageSpec> swiftPackages = SwiftPackageSpec.parse(request);
        return explicitMode.resolve(iosPods, swiftPackages);
    }

    static IOSDependencyManager fromHint(String value) throws BuildException {
        String normalized = value == null ? "auto" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            normalized = "auto";
        }
        switch (normalized) {
            case "auto":
                return AUTO;
            case "cocoapods":
                return COCOAPODS;
            case "spm":
                return SPM;
            case "both":
                return BOTH;
            case "none":
                return NONE;
            default:
                throw new BuildException("Unsupported ios.dependencyManager value " + value + ". Expected one of auto, cocoapods, spm, both, none");
        }
    }

    IOSDependencyConfig resolve(String iosPods, List<SwiftPackageSpec> swiftPackages) throws BuildException {
        boolean hasPods = iosPods != null && iosPods.trim().length() > 0;
        boolean hasSpm = swiftPackages != null && !swiftPackages.isEmpty();
        switch (this) {
            case AUTO:
                if (hasPods && hasSpm) {
                    return new IOSDependencyConfig(BOTH, iosPods, swiftPackages);
                }
                if (hasPods) {
                    return new IOSDependencyConfig(COCOAPODS, iosPods, swiftPackages);
                }
                if (hasSpm) {
                    return new IOSDependencyConfig(SPM, iosPods, swiftPackages);
                }
                return new IOSDependencyConfig(NONE, iosPods, swiftPackages);
            case COCOAPODS:
                if (!hasPods) {
                    throw new BuildException("ios.dependencyManager=cocoapods requires ios.pods to be set");
                }
                return new IOSDependencyConfig(COCOAPODS, iosPods, swiftPackages);
            case SPM:
                if (!hasSpm) {
                    throw new BuildException("ios.dependencyManager=spm requires ios.spm.packages to be set");
                }
                return new IOSDependencyConfig(SPM, iosPods, swiftPackages);
            case BOTH:
                if (!hasPods || !hasSpm) {
                    throw new BuildException("ios.dependencyManager=both requires both ios.pods and ios.spm.packages to be set");
                }
                return new IOSDependencyConfig(BOTH, iosPods, swiftPackages);
            case NONE:
                return new IOSDependencyConfig(NONE, iosPods, swiftPackages);
            default:
                throw new BuildException("Unsupported ios.dependencyManager value " + this);
        }
    }
}

final class IOSDependencyConfig {
    final IOSDependencyManager mode;
    final String iosPods;
    final List<SwiftPackageSpec> swiftPackages;

    IOSDependencyConfig(IOSDependencyManager mode, String iosPods, List<SwiftPackageSpec> swiftPackages) {
        this.mode = mode;
        this.iosPods = iosPods == null ? "" : iosPods.trim();
        this.swiftPackages = swiftPackages;
    }

    boolean usesCocoaPods() {
        return mode == IOSDependencyManager.COCOAPODS || mode == IOSDependencyManager.BOTH;
    }

    boolean usesSwiftPackages() {
        return mode == IOSDependencyManager.SPM || mode == IOSDependencyManager.BOTH;
    }
}

final class SwiftPackageSpec {
    final String identity;
    final String url;
    final String requirement;
    final List<String> products;

    SwiftPackageSpec(String identity, String url, String requirement, List<String> products) {
        this.identity = identity;
        this.url = url;
        this.requirement = requirement;
        this.products = products;
    }

    static List<SwiftPackageSpec> parse(BuildRequest request) throws BuildException {
        String packagesProp = request.getArg("ios.spm.packages", "");
        List<SwiftPackageSpec> out = new ArrayList<SwiftPackageSpec>();
        if (packagesProp == null || packagesProp.trim().length() == 0) {
            return out;
        }
        String[] packageEntries = packagesProp.split("[;]");
        for (String entry : packageEntries) {
            String trimmed = entry.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            String[] parts = trimmed.split("\\|");
            if (parts.length != 3) {
                throw new BuildException("Invalid ios.spm.packages entry '" + trimmed + "'. Expected <identity>|<url>|<requirement>");
            }
            String identity = parts[0].trim();
            String url = parts[1].trim();
            String requirement = parts[2].trim();
            if (identity.length() == 0 || url.length() == 0 || requirement.length() == 0) {
                throw new BuildException("Invalid ios.spm.packages entry '" + trimmed + "'. Identity, URL, and requirement are all required");
            }
            validateRequirement(requirement);
            String productsProp = request.getArg("ios.spm.products." + identity, "");
            List<String> products = new ArrayList<String>();
            for (String product : productsProp.split("[,]")) {
                product = product.trim();
                if (product.length() > 0) {
                    products.add(product);
                }
            }
            if (products.isEmpty()) {
                throw new BuildException("ios.spm.products." + identity + " must list at least one product");
            }
            out.add(new SwiftPackageSpec(identity, url, requirement, products));
        }
        return out;
    }

    static void validateRequirement(String requirement) throws BuildException {
        if (requirement.startsWith("from:") || requirement.startsWith("exact:") ||
                requirement.startsWith("branch:") || requirement.startsWith("revision:")) {
            if (requirement.substring(requirement.indexOf(':') + 1).trim().length() == 0) {
                throw new BuildException("Invalid SPM requirement '" + requirement + "'");
            }
            return;
        }
        if (requirement.startsWith("range:")) {
            String range = requirement.substring("range:".length()).trim();
            if (!range.contains("..<")) {
                throw new BuildException("Invalid SPM range requirement '" + requirement + "'. Expected range:min..<max");
            }
            String[] bounds = range.split("\\.\\.<");
            if (bounds.length != 2 || bounds[0].trim().length() == 0 || bounds[1].trim().length() == 0) {
                throw new BuildException("Invalid SPM range requirement '" + requirement + "'");
            }
            return;
        }
        throw new BuildException("Unsupported SPM requirement '" + requirement + "'. Expected from:, exact:, branch:, revision:, or range:");
    }
}
