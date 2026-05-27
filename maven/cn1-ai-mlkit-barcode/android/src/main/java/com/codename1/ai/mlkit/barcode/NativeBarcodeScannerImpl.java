package com.codename1.ai.mlkit.barcode;


public class NativeBarcodeScannerImpl {
    public String[] scan(byte[] imageBytes) {
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
        if (bm == null) return new String[0];
        com.google.mlkit.vision.common.InputImage img =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
        com.google.mlkit.vision.barcode.BarcodeScannerOptions o =
                new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder().build();
        com.google.mlkit.vision.barcode.BarcodeScanner scanner =
                com.google.mlkit.vision.barcode.BarcodeScanning.getClient(o);
        final java.util.List<String> out = new java.util.ArrayList<String>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        scanner.process(img)
            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                    java.util.List<com.google.mlkit.vision.barcode.common.Barcode>>() {
                public void onSuccess(java.util.List<com.google.mlkit.vision.barcode.common.Barcode> rs) {
                    for (com.google.mlkit.vision.barcode.common.Barcode b : rs) {
                        String v = b.getRawValue();
                        if (v != null) out.add(v);
                    }
                    latch.countDown();
                }
            })
            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                public void onFailure(Exception e) { latch.countDown(); }
            });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return out.toArray(new String[0]);
    }

    public boolean isSupported() {
        return true;
    }
}
