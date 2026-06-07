package com.codename1.ai.mlkit.segmentation;


public class NativeSelfieSegmenterImpl {
    public byte[] segment(byte[] imageBytes) {
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
        if (bm == null) return new byte[0];
        com.google.mlkit.vision.common.InputImage img =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
        com.google.mlkit.vision.segmentation.Segmenter seg =
            com.google.mlkit.vision.segmentation.Segmentation.getClient(
                new com.google.mlkit.vision.segmentation.SelfieSegmenterOptions.Builder()
                    .setDetectorMode(
                        com.google.mlkit.vision.segmentation.SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build());
        final java.util.concurrent.atomic.AtomicReference<byte[]> out =
                new java.util.concurrent.atomic.AtomicReference<byte[]>(new byte[0]);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        seg.process(img)
           .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                   com.google.mlkit.vision.segmentation.SegmentationMask>() {
               public void onSuccess(com.google.mlkit.vision.segmentation.SegmentationMask mask) {
                   int w = mask.getWidth(), h = mask.getHeight();
                   java.nio.ByteBuffer buf = mask.getBuffer();
                   buf.rewind();
                   byte[] outb = new byte[w * h];
                   for (int i = 0; i < w * h; i++) {
                       float v = buf.getFloat();
                       outb[i] = (byte)(int)(v * 255);
                   }
                   out.set(outb);
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
