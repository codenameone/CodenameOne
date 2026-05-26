package com.codename1.ai.mlkit.face;


public class NativeFaceDetectorImpl {
    public int[] detect(byte[] imageBytes) {
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
        if (bm == null) return new int[0];
        com.google.mlkit.vision.common.InputImage img =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
        com.google.mlkit.vision.face.FaceDetector det =
                com.google.mlkit.vision.face.FaceDetection.getClient(
                        new com.google.mlkit.vision.face.FaceDetectorOptions.Builder().build());
        final java.util.List<int[]> rs = new java.util.ArrayList<int[]>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        det.process(img)
           .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                   java.util.List<com.google.mlkit.vision.face.Face>>() {
               public void onSuccess(java.util.List<com.google.mlkit.vision.face.Face> faces) {
                   for (com.google.mlkit.vision.face.Face f : faces) {
                       android.graphics.Rect r = f.getBoundingBox();
                       rs.add(new int[]{r.left, r.top, r.width(), r.height()});
                   }
                   latch.countDown();
               }
           })
           .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
               public void onFailure(Exception e) { latch.countDown(); }
           });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        int[] flat = new int[rs.size() * 4];
        int i = 0;
        for (int[] r : rs) { System.arraycopy(r, 0, flat, i, 4); i += 4; }
        return flat;
    }

    public boolean isSupported() {
        return true;
    }
}
