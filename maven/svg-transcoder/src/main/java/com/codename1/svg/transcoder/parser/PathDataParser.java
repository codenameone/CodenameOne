package com.codename1.svg.transcoder.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the SVG path d="..." mini-language to a flat list of absolute-coordinate
 * {@link PathCommand}s. Implicit repeats, relative coordinates and smooth-curve
 * control-point reflection are resolved here so the code generator stays simple.
 */
public final class PathDataParser {

    private PathDataParser() { }

    public static List<PathCommand> parse(String d) {
        List<PathCommand> out = new ArrayList<PathCommand>();
        if (d == null || d.trim().isEmpty()) return out;

        // We scan the string by splitting on command letters but preserving them as separators.
        // It is easier to walk character-by-character.
        char[] cs = d.toCharArray();
        int i = 0;
        int len = cs.length;

        float curX = 0, curY = 0;          // current point
        float startX = 0, startY = 0;      // subpath start point (for Z)
        char lastCmd = 0;
        float prevCubicCtrlX = 0, prevCubicCtrlY = 0;
        float prevQuadCtrlX = 0, prevQuadCtrlY = 0;
        boolean haveCubic = false;
        boolean haveQuad = false;

        while (i < len) {
            // skip whitespace and commas
            while (i < len && (isWs(cs[i]) || cs[i] == ',')) i++;
            if (i >= len) break;

            char c = cs[i];
            char cmd;
            if (isCmdLetter(c)) {
                cmd = c;
                i++;
                lastCmd = cmd;
            } else {
                // Implicit repeat: re-use last cmd (M repeats become L, m become l).
                if (lastCmd == 0) {
                    throw new IllegalArgumentException("Path data starts with a number: '" + d + "'");
                }
                if (lastCmd == 'M') cmd = 'L';
                else if (lastCmd == 'm') cmd = 'l';
                else cmd = lastCmd;
            }

            // Find the end of this command's numeric arguments -- the next command letter.
            int argStart = i;
            while (i < len && !isCmdLetter(cs[i])) i++;
            String argStr = new String(cs, argStart, i - argStart);
            NumberParser np = new NumberParser(argStr);

            switch (cmd) {
                case 'M':
                case 'm': {
                    boolean rel = cmd == 'm';
                    boolean first = true;
                    while (np.hasMore()) {
                        float x = np.nextFloat();
                        float y = np.nextFloat();
                        if (rel) { x += curX; y += curY; }
                        if (first) {
                            out.add(new PathCommand(PathCommand.Type.MOVE, new float[]{x, y}));
                            startX = x; startY = y;
                            first = false;
                        } else {
                            out.add(new PathCommand(PathCommand.Type.LINE, new float[]{x, y}));
                        }
                        curX = x; curY = y;
                    }
                    haveCubic = haveQuad = false;
                    break;
                }
                case 'L':
                case 'l': {
                    boolean rel = cmd == 'l';
                    while (np.hasMore()) {
                        float x = np.nextFloat();
                        float y = np.nextFloat();
                        if (rel) { x += curX; y += curY; }
                        out.add(new PathCommand(PathCommand.Type.LINE, new float[]{x, y}));
                        curX = x; curY = y;
                    }
                    haveCubic = haveQuad = false;
                    break;
                }
                case 'H':
                case 'h': {
                    boolean rel = cmd == 'h';
                    while (np.hasMore()) {
                        float x = np.nextFloat();
                        if (rel) x += curX;
                        out.add(new PathCommand(PathCommand.Type.LINE, new float[]{x, curY}));
                        curX = x;
                    }
                    haveCubic = haveQuad = false;
                    break;
                }
                case 'V':
                case 'v': {
                    boolean rel = cmd == 'v';
                    while (np.hasMore()) {
                        float y = np.nextFloat();
                        if (rel) y += curY;
                        out.add(new PathCommand(PathCommand.Type.LINE, new float[]{curX, y}));
                        curY = y;
                    }
                    haveCubic = haveQuad = false;
                    break;
                }
                case 'C':
                case 'c': {
                    boolean rel = cmd == 'c';
                    while (np.hasMore()) {
                        float x1 = np.nextFloat(), y1 = np.nextFloat();
                        float x2 = np.nextFloat(), y2 = np.nextFloat();
                        float x = np.nextFloat(), y = np.nextFloat();
                        if (rel) { x1 += curX; y1 += curY; x2 += curX; y2 += curY; x += curX; y += curY; }
                        out.add(new PathCommand(PathCommand.Type.CUBIC, new float[]{x1, y1, x2, y2, x, y}));
                        prevCubicCtrlX = x2; prevCubicCtrlY = y2;
                        curX = x; curY = y;
                        haveCubic = true; haveQuad = false;
                    }
                    break;
                }
                case 'S':
                case 's': {
                    boolean rel = cmd == 's';
                    while (np.hasMore()) {
                        float x1, y1;
                        if (haveCubic) {
                            x1 = 2 * curX - prevCubicCtrlX;
                            y1 = 2 * curY - prevCubicCtrlY;
                        } else {
                            x1 = curX; y1 = curY;
                        }
                        float x2 = np.nextFloat(), y2 = np.nextFloat();
                        float x = np.nextFloat(), y = np.nextFloat();
                        if (rel) { x2 += curX; y2 += curY; x += curX; y += curY; }
                        out.add(new PathCommand(PathCommand.Type.CUBIC, new float[]{x1, y1, x2, y2, x, y}));
                        prevCubicCtrlX = x2; prevCubicCtrlY = y2;
                        curX = x; curY = y;
                        haveCubic = true; haveQuad = false;
                    }
                    break;
                }
                case 'Q':
                case 'q': {
                    boolean rel = cmd == 'q';
                    while (np.hasMore()) {
                        float x1 = np.nextFloat(), y1 = np.nextFloat();
                        float x = np.nextFloat(), y = np.nextFloat();
                        if (rel) { x1 += curX; y1 += curY; x += curX; y += curY; }
                        out.add(new PathCommand(PathCommand.Type.QUAD, new float[]{x1, y1, x, y}));
                        prevQuadCtrlX = x1; prevQuadCtrlY = y1;
                        curX = x; curY = y;
                        haveQuad = true; haveCubic = false;
                    }
                    break;
                }
                case 'T':
                case 't': {
                    boolean rel = cmd == 't';
                    while (np.hasMore()) {
                        float x1, y1;
                        if (haveQuad) {
                            x1 = 2 * curX - prevQuadCtrlX;
                            y1 = 2 * curY - prevQuadCtrlY;
                        } else {
                            x1 = curX; y1 = curY;
                        }
                        float x = np.nextFloat(), y = np.nextFloat();
                        if (rel) { x += curX; y += curY; }
                        out.add(new PathCommand(PathCommand.Type.QUAD, new float[]{x1, y1, x, y}));
                        prevQuadCtrlX = x1; prevQuadCtrlY = y1;
                        curX = x; curY = y;
                        haveQuad = true; haveCubic = false;
                    }
                    break;
                }
                case 'A':
                case 'a': {
                    boolean rel = cmd == 'a';
                    while (np.hasMore()) {
                        float rx = np.nextFloat();
                        float ry = np.nextFloat();
                        float xRot = np.nextFloat();
                        int largeArc = np.nextFlag();
                        int sweep = np.nextFlag();
                        float x = np.nextFloat();
                        float y = np.nextFloat();
                        if (rel) { x += curX; y += curY; }
                        out.add(new PathCommand(PathCommand.Type.ARC,
                                new float[]{curX, curY, rx, ry, xRot, largeArc, sweep, x, y}));
                        curX = x; curY = y;
                        haveCubic = haveQuad = false;
                    }
                    break;
                }
                case 'Z':
                case 'z': {
                    out.add(new PathCommand(PathCommand.Type.CLOSE, new float[0]));
                    curX = startX; curY = startY;
                    haveCubic = haveQuad = false;
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown path command '" + cmd + "' in '" + d + "'");
            }
        }
        return out;
    }

    private static boolean isCmdLetter(char c) {
        switch (c) {
            case 'M': case 'm': case 'L': case 'l':
            case 'H': case 'h': case 'V': case 'v':
            case 'C': case 'c': case 'S': case 's':
            case 'Q': case 'q': case 'T': case 't':
            case 'A': case 'a':
            case 'Z': case 'z':
                return true;
            default:
                return false;
        }
    }

    private static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
