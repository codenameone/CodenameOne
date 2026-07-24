package com.codename1.flutter.rendering;

/**
 * Lays a grid out with tiles no wider than a maximum cross-axis extent —
 * Flutter's {@code SliverGridDelegateWithMaxCrossAxisExtent}. Signature-only.
 */
public class SliverGridDelegateWithMaxCrossAxisExtent extends SliverGridDelegate {

    private double maxCrossAxisExtent;
    private double mainAxisSpacing;
    private double crossAxisSpacing;
    private double childAspectRatio = 1.0;
    private double mainAxisExtent;

    public void maxCrossAxisExtent(double v) { this.maxCrossAxisExtent = v; }
    public void mainAxisSpacing(double v) { this.mainAxisSpacing = v; }
    public void crossAxisSpacing(double v) { this.crossAxisSpacing = v; }
    public void childAspectRatio(double v) { this.childAspectRatio = v; }
    public void mainAxisExtent(double v) { this.mainAxisExtent = v; }
}
