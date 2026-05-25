package com.codename1.svg.transcoder.animation;

import com.codename1.svg.transcoder.model.SVGAnimation;

import java.util.ArrayList;
import java.util.List;

/** Parses SMIL clock values ("1s", "250ms", "1:30", "indefinite", etc.). */
public final class SMILParser {

    private SMILParser() { }

    public static long parseClock(String s, long fallback) {
        if (s == null) return fallback;
        String v = s.trim();
        if (v.isEmpty()) return fallback;
        if ("indefinite".equalsIgnoreCase(v)) return SVGAnimation.REPEAT_INDEFINITE;
        try {
            // h:m:s or m:s
            if (v.indexOf(':') >= 0) {
                String[] parts = v.split(":");
                double total = 0;
                for (String p : parts) total = total * 60 + Double.parseDouble(p.trim());
                return Math.round(total * 1000.0);
            }
            // unit suffix
            if (v.endsWith("ms")) {
                return Math.round(Double.parseDouble(v.substring(0, v.length() - 2).trim()));
            }
            if (v.endsWith("s")) {
                return Math.round(Double.parseDouble(v.substring(0, v.length() - 1).trim()) * 1000.0);
            }
            if (v.endsWith("min")) {
                return Math.round(Double.parseDouble(v.substring(0, v.length() - 3).trim()) * 60000.0);
            }
            if (v.endsWith("h")) {
                return Math.round(Double.parseDouble(v.substring(0, v.length() - 1).trim()) * 3600000.0);
            }
            // raw number = seconds
            return Math.round(Double.parseDouble(v) * 1000.0);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public static int parseRepeatCount(String s) {
        if (s == null) return 1;
        String v = s.trim();
        if (v.isEmpty()) return 1;
        if ("indefinite".equalsIgnoreCase(v)) return SVGAnimation.REPEAT_INDEFINITE;
        try {
            float f = Float.parseFloat(v);
            int i = (int) Math.max(1, Math.round(f));
            return i;
        } catch (RuntimeException e) {
            return 1;
        }
    }

    public static SVGAnimation.CalcMode parseCalcMode(String s) {
        if (s == null) return SVGAnimation.CalcMode.LINEAR;
        String v = s.trim().toLowerCase();
        if ("discrete".equals(v)) return SVGAnimation.CalcMode.DISCRETE;
        if ("paced".equals(v)) return SVGAnimation.CalcMode.PACED;
        return SVGAnimation.CalcMode.LINEAR;
    }

    public static SVGAnimation.TransformType parseTransformType(String s) {
        if (s == null) return SVGAnimation.TransformType.TRANSLATE;
        String v = s.trim();
        if ("rotate".equals(v)) return SVGAnimation.TransformType.ROTATE;
        if ("scale".equals(v)) return SVGAnimation.TransformType.SCALE;
        if ("skewX".equals(v)) return SVGAnimation.TransformType.SKEW_X;
        if ("skewY".equals(v)) return SVGAnimation.TransformType.SKEW_Y;
        return SVGAnimation.TransformType.TRANSLATE;
    }

    public static List<String> parseValues(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        List<String> out = new ArrayList<String>();
        for (String p : t.split(";")) out.add(p.trim());
        return out;
    }
}
