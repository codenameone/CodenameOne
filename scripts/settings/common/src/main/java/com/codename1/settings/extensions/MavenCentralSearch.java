package com.codename1.settings.extensions;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MavenCentralSearch {
    private static final String SEARCH_URL = "https://search.maven.org/solrsearch/select?q=";

    private MavenCentralSearch() {
    }

    public static List<ExtensionDescriptor> curated() {
        ArrayList<ExtensionDescriptor> out = new ArrayList<ExtensionDescriptor>();
        out.add(maven("Google Maps", "Native Google Maps integration for Codename One apps.",
                "com.codenameone", "googlemaps-lib", "1.0.1", "pom"));
        out.add(maven("ML Kit Barcode", "Decode QR, EAN, Code 128, and other ML Kit barcode formats.",
                "com.codenameone", "cn1-ai-mlkit-barcode-lib", "LATEST", "pom"));
        out.add(maven("ML Kit Document Scanner", "Capture and crop document photos with native VisionKit and Google Play Services support.",
                "com.codenameone", "cn1-ai-mlkit-docscan-lib", "LATEST", "pom"));
        out.add(maven("ML Kit Face Detection", "Detect faces and bounding rectangles using ML Kit-backed native providers.",
                "com.codenameone", "cn1-ai-mlkit-face-lib", "LATEST", "pom"));
        out.add(maven("TensorFlow Lite", "TensorFlow Lite inference bridge packaged as a Codename One cn1lib.",
                "com.codenameone", "cn1-ai-tflite-lib", "LATEST", "pom"));
        out.add(maven("Whisper", "Speech-to-text support through the Codename One AI Whisper cn1lib.",
                "com.codenameone", "cn1-ai-whisper-lib", "LATEST", "pom"));
        out.add(legacy("Bouncy Castle SDK", "Legacy cn1lib catalog entry for Bouncy Castle cryptography support."));
        out.add(legacy("SSLCertificateFingerprint", "Legacy cn1lib catalog entry for certificate fingerprint verification."));
        out.add(legacy("QRMaker", "Legacy cn1lib catalog entry for QR code generation."));
        return out;
    }

    private static ExtensionDescriptor maven(String name, String description, String group, String artifact, String version, String type) {
        return new ExtensionDescriptor(name, description, new MavenDependency(group, artifact, version, type), true);
    }

    private static ExtensionDescriptor legacy(String name, String description) {
        return new ExtensionDescriptor(name, description, null, false);
    }

    public static List<ExtensionDescriptor> search(String query) throws Exception {
        String q = query == null || query.trim().length() == 0
                ? "codenameone cn1lib"
                : query.trim() + " codenameone";
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl(SEARCH_URL + encode(q) + "&rows=40&wt=json");
        req.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(req);
        if (req.getResponseCode() >= 400) {
            throw new IOException("Maven Central returned HTTP " + req.getResponseCode());
        }
        JSONParser parser = new JSONParser();
        Map root = parser.parseJSON(new InputStreamReader(new ByteArrayInputStream(req.getResponseData()), "UTF-8"));
        Map response = (Map) root.get("response");
        List docs = response == null ? null : (List) response.get("docs");
        ArrayList<ExtensionDescriptor> out = new ArrayList<ExtensionDescriptor>();
        if (docs == null) {
            return out;
        }
        for (Object o : docs) {
            Map doc = (Map) o;
            String group = string(doc.get("g"));
            String artifact = string(doc.get("a"));
            String version = string(doc.get("latestVersion"));
            if (group.length() == 0 || artifact.length() == 0 || version.length() == 0) {
                continue;
            }
            String packaging = string(doc.get("p"));
            String name = artifact;
            String desc = group + ":" + artifact + ":" + version;
            if (packaging.length() > 0) {
                desc += " (" + packaging + ")";
            }
            out.add(new ExtensionDescriptor(name, desc, new MavenDependency(group, artifact, version), true));
        }
        return out;
    }

    private static String encode(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else if (c == ' ') {
                out.append("%20");
            } else {
                out.append('%');
                String hex = Integer.toHexString(c).toUpperCase();
                if (hex.length() == 1) {
                    out.append('0');
                }
                out.append(hex);
            }
        }
        return out.toString();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static final class IOException extends Exception {
        IOException(String message) {
            super(message);
        }
    }
}
