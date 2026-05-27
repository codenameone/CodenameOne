package com.codename1.ai.mlkit.text;


public class NativeTextRecognizerImpl {
    public String recognize(byte[] imageBytes) {
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
        if (bm == null) return "";
        com.google.mlkit.vision.common.InputImage img =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
        com.google.mlkit.vision.text.TextRecognizer rec =
                com.google.mlkit.vision.text.TextRecognition.getClient(
                        com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
        final java.util.concurrent.atomic.AtomicReference<String> out =
                new java.util.concurrent.atomic.AtomicReference<String>("");
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        rec.process(img)
           .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                   com.google.mlkit.vision.text.Text>() {
               public void onSuccess(com.google.mlkit.vision.text.Text t) {
                   out.set(t.getText() == null ? "" : t.getText());
                   latch.countDown();
               }
           })
           .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
               public void onFailure(Exception e) { latch.countDown(); }
           });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return out.get();
    }

    public boolean isSupported() {
        return true;
    }
}
