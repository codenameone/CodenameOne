package com.codenameone.developerguide.snippets.generated;

// tag::performance-java-fused[]
@com.codename1.annotations.Fused
class RgbImage {
    private final int[] pixels;   // laid out INSIDE the RgbImage allocation
    private final byte[] flags;
    RgbImage(int w, int h) {
        pixels = new int[w * h];  // computed sizes (w * h) fuse too
        flags = new byte[16];
    }
}
// end::performance-java-fused[]
