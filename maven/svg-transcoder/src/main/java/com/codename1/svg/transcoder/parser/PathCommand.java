package com.codename1.svg.transcoder.parser;

/**
 * One absolute-coordinate command from an SVG path's d="..." attribute.
 *
 * The parser collapses relative commands to absolute, S/T smooth curves to
 * the equivalent C/Q with the implicit control point already resolved, and
 * H/V to L. Arc commands are kept as ARC so the generator can decompose
 * them into cubic Bezier segments at codegen time.
 */
public final class PathCommand {

    public enum Type { MOVE, LINE, CUBIC, QUAD, ARC, CLOSE }

    private final Type type;
    private final float[] args;

    public PathCommand(Type type, float[] args) {
        this.type = type;
        this.args = args;
    }

    public Type getType() { return type; }
    public float[] getArgs() { return args; }
}
