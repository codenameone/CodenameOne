package com.codename1.ai.mlkit.docscan;


public class NativeDocumentScannerImpl {
    public String scanToFile(byte[] imageBytes) {
        try {
            java.io.File f = java.io.File.createTempFile("docscan-", ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            fos.write(imageBytes);
            fos.close();
            return f.getAbsolutePath();
        } catch (java.io.IOException ioe) {
            return "";
        }
    }

    public boolean isSupported() {
        return true;
    }
}
