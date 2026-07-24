package com.codename1.flutter.rendering;

/**
 * Lays a grid out with a fixed number of tiles across the cross axis —
 * Flutter's {@code SliverGridDelegateWithFixedCrossAxisCount}. Signature-only.
 */
public class SliverGridDelegateWithFixedCrossAxisCount extends SliverGridDelegate {

    private long crossAxisCount;
    private double mainAxisSpacing;
    private double crossAxisSpacing;
    private double childAspectRatio = 1.0;
    private double mainAxisExtent;

    public void crossAxisCount(long v) { this.crossAxisCount = v; }
    public void mainAxisSpacing(double v) { this.mainAxisSpacing = v; }
    public void crossAxisSpacing(double v) { this.crossAxisSpacing = v; }
    public void childAspectRatio(double v) { this.childAspectRatio = v; }
    public void mainAxisExtent(double v) { this.mainAxisExtent = v; }

    public long getCrossAxisCount() { return crossAxisCount; }
}
