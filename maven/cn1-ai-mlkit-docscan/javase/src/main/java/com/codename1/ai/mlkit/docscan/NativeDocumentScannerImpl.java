package com.codename1.ai.mlkit.docscan;

public class NativeDocumentScannerImpl {
    public String scanToFile(byte[] imageBytes) {
        try {
            java.io.File f = java.io.File.createTempFile("docscan-stub-", ".jpg");
            java.nio.file.Files.write(f.toPath(), imageBytes);
            return f.getAbsolutePath();
        } catch (java.io.IOException ioe) {
            return "";
        }
    }

    public boolean isSupported() {
        return true;
    }
}
