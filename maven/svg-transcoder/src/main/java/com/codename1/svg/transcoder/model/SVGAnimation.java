package com.codename1.svg.transcoder.model;

import java.util.List;

/**
 * SMIL animation: &lt;animate&gt;, &lt;animateTransform&gt;, &lt;set&gt;.
 *
 * Stored as data so the code generator can emit a runtime descriptor.
 * Time values are pre-parsed into milliseconds; "indefinite" repeat is
 * represented by {@link #REPEAT_INDEFINITE}.
 */
public final class SVGAnimation {

    public static final int REPEAT_INDEFINITE = -1;

    public enum Kind { ANIMATE, ANIMATE_TRANSFORM, SET }

    public enum TransformType { TRANSLATE, ROTATE, SCALE, SKEW_X, SKEW_Y }

    public enum CalcMode { LINEAR, DISCRETE, PACED }

    private Kind kind = Kind.ANIMATE;
    private String attributeName;
    private TransformType transformType;
    private CalcMode calcMode = CalcMode.LINEAR;
    private List<String> values; // raw value strings (already trimmed)
    private String from;
    private String to;
    private String by;
    private long beginMs;
    private long durMs;
    private int repeatCount = 1;
    private boolean freeze;

    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }

    public String getAttributeName() { return attributeName; }
    public void setAttributeName(String attributeName) { this.attributeName = attributeName; }

    public TransformType getTransformType() { return transformType; }
    public void setTransformType(TransformType transformType) { this.transformType = transformType; }

    public CalcMode getCalcMode() { return calcMode; }
    public void setCalcMode(CalcMode calcMode) { this.calcMode = calcMode; }

    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }

    public long getBeginMs() { return beginMs; }
    public void setBeginMs(long beginMs) { this.beginMs = beginMs; }

    public long getDurMs() { return durMs; }
    public void setDurMs(long durMs) { this.durMs = durMs; }

    public int getRepeatCount() { return repeatCount; }
    public void setRepeatCount(int repeatCount) { this.repeatCount = repeatCount; }

    public boolean isFreeze() { return freeze; }
    public void setFreeze(boolean freeze) { this.freeze = freeze; }
}
