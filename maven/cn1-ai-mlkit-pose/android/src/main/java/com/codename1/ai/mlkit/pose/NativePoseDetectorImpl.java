package com.codename1.ai.mlkit.pose;


public class NativePoseDetectorImpl {
    public float[] detect(byte[] imageBytes) {
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
        if (bm == null) return new float[0];
        com.google.mlkit.vision.common.InputImage img =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
        com.google.mlkit.vision.pose.PoseDetector det =
                com.google.mlkit.vision.pose.PoseDetection.getClient(
                        new com.google.mlkit.vision.pose.defaults.PoseDetectorOptions.Builder().build());
        final float[] out = new float[33 * 3];
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        det.process(img)
           .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                   com.google.mlkit.vision.pose.Pose>() {
               public void onSuccess(com.google.mlkit.vision.pose.Pose p) {
                   java.util.List<com.google.mlkit.vision.pose.PoseLandmark> lms = p.getAllPoseLandmarks();
                   for (int i = 0; i < 33 && i < lms.size(); i++) {
                       com.google.mlkit.vision.pose.PoseLandmark lm = lms.get(i);
                       out[i * 3]     = lm.getPosition().x;
                       out[i * 3 + 1] = lm.getPosition().y;
                       out[i * 3 + 2] = lm.getInFrameLikelihood();
                   }
                   latch.countDown();
               }
           })
           .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
               public void onFailure(Exception e) { latch.countDown(); }
           });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return out;
    }

    public boolean isSupported() {
        return true;
    }
}
