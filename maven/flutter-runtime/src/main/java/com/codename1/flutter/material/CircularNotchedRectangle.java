package com.codename1.flutter.material;

/**
 * A {@link NotchedShape} that cuts a circular notch with small flanking fillets
 * — Flutter's {@code CircularNotchedRectangle}. Signature-only this pass.
 */
public class CircularNotchedRectangle extends NotchedShape {

    private boolean inverted;

    public void inverted(boolean v) { this.inverted = v; }
}
