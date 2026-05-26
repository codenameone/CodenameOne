package com.codename1.ai.mlkit.labeling;


public class NativeImageLabelerImpl {
    public String[] label(byte[] imageBytes) {
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
        if (bm == null) return new String[0];
        com.google.mlkit.vision.common.InputImage img =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
        com.google.mlkit.vision.label.ImageLabeler labeler =
                com.google.mlkit.vision.label.ImageLabeling.getClient(
                        com.google.mlkit.vision.label.defaults.ImageLabelerOptions.DEFAULT_OPTIONS);
        final java.util.List<String> out = new java.util.ArrayList<String>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        labeler.process(img)
            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                    java.util.List<com.google.mlkit.vision.label.ImageLabel>>() {
                public void onSuccess(java.util.List<com.google.mlkit.vision.label.ImageLabel> rs) {
                    for (com.google.mlkit.vision.label.ImageLabel l : rs) out.add(l.getText());
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
