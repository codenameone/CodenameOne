package com.codename1.flutter;

/**
 * A point within a rectangle expressed with a text-direction-relative
 * horizontal axis (start/end) — Flutter's {@code AlignmentDirectional}.
 *
 * <p>The {@code start} coordinate is -1 and {@code end} is +1; resolving to a
 * concrete {@link Alignment} assumes left-to-right text (start = left) for
 * this milestone.</p>
 */
public class AlignmentDirectional extends Alignment {

    public static final AlignmentDirectional topStart = new AlignmentDirectional(-1, -1);
    public static final AlignmentDirectional topCenter = new AlignmentDirectional(0, -1);
    public static final AlignmentDirectional topEnd = new AlignmentDirectional(1, -1);
    public static final AlignmentDirectional centerStart = new AlignmentDirectional(-1, 0);
    public static final AlignmentDirectional center = new AlignmentDirectional(0, 0);
    public static final AlignmentDirectional centerEnd = new AlignmentDirectional(1, 0);
    public static final AlignmentDirectional bottomStart = new AlignmentDirectional(-1, 1);
    public static final AlignmentDirectional bottomCenter = new AlignmentDirectional(0, 1);
    public static final AlignmentDirectional bottomEnd = new AlignmentDirectional(1, 1);

    public AlignmentDirectional(double start, double y) {
        // start maps to the x axis under the LTR assumption of this milestone.
        super(start, y);
    }

    public double start() {
        return x();
    }

    /**
     * Resolves to a concrete {@link Alignment} assuming left-to-right text.
     */
    public Alignment resolve() {
        return new Alignment(x(), y());
    }
}
