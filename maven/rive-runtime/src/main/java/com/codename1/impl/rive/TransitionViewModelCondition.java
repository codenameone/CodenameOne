/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

/// A transition condition comparing a bound view model value (through a
/// bindable property) with a constant comparator.
public class TransitionViewModelCondition extends TransitionViewModelConditionBase {
    private TransitionComparator left;
    private TransitionComparator right;
    private DataBind selfBind;
    private int seenTrigger;
    private int seenChange = -1;

    /// The bindable property the left side reads, or null.
    public BindableProperty getBindable() {
        return left instanceof TransitionPropertyViewModelComparator
                ? ((TransitionPropertyViewModelComparator) left).getBindable() : null;
    }

    /// Kept for callers that attach the bindable directly: it becomes the
    /// left side.
    public void setBindable(BindableProperty p) {
        TransitionPropertyViewModelComparator c = new TransitionPropertyViewModelComparator();
        c.setBindable(p);
        left = c;
        noteBindable(p);
    }

    void noteBindable(BindableProperty p) {
        if (p instanceof BindablePropertyInteger) {
            seenTrigger = ((BindablePropertyInteger) p).getPropertyValue();
        }
    }

    public TransitionComparator getComparator() {
        return right;
    }

    /// Upstream `TransitionViewModelConditionImporter::setComparator`: the
    /// first comparator is the left side, the second the right.
    public void setComparator(TransitionComparator c) {
        if (left == null) {
            left = c;
            if (c instanceof TransitionPropertyViewModelComparator) {
                noteBindable(((TransitionPropertyViewModelComparator) c).getBindable());
            }
        } else {
            right = c;
        }
    }

    /// Whether this is a self-change condition.
    public boolean isSelf() {
        return right instanceof TransitionSelfComparator;
    }

    /// The data bind that feeds the left bindable, used by self conditions.
    public void setSelfBind(DataBind b) {
        selfBind = b;
    }

    /// Whether the condition holds now.
    public boolean evaluate() {
        BindableProperty bindable = getBindable();
        if (bindable == null || right == null) {
            return false;
        }
        if (right instanceof TransitionSelfComparator) {
            ViewModelInstanceValue src = selfBind == null ? null : selfBind.getSource();
            if (src == null) {
                return false;
            }
            int c = src.getChangeCount();
            if (seenChange < 0) {
                seenChange = c;
                return false;
            }
            if (c != seenChange) {
                seenChange = c;
                return true;
            }
            return false;
        }
        int op = getOpValue();
        if (right instanceof TransitionPropertyViewModelComparator) {
            return comparePropertyPair(op, bindable, ((TransitionPropertyViewModelComparator) right).getBindable());
        }
        TransitionComparator comparator = right;
        if (bindable instanceof BindablePropertyNumber && comparator instanceof TransitionValueNumberComparator) {
            return compare(op, ((BindablePropertyNumber) bindable).getPropertyValue(),
                    ((TransitionValueNumberComparator) comparator).getValue());
        }
        if (bindable instanceof BindablePropertyInteger && comparator instanceof TransitionValueNumberComparator) {
            return compare(op, ((BindablePropertyInteger) bindable).getPropertyValue(),
                    ((TransitionValueNumberComparator) comparator).getValue());
        }
        if (bindable instanceof BindablePropertyBoolean && comparator instanceof TransitionValueBooleanComparator) {
            return equality(op, ((BindablePropertyBoolean) bindable).isPropertyValue()
                    == ((TransitionValueBooleanComparator) comparator).isValue());
        }
        if (bindable instanceof BindablePropertyString && comparator instanceof TransitionValueStringComparator) {
            String a = ((BindablePropertyString) bindable).getPropertyValue();
            String b = ((TransitionValueStringComparator) comparator).getValue();
            return equality(op, a == null ? b == null : a.equals(b));
        }
        if (bindable instanceof BindablePropertyColor && comparator instanceof TransitionValueColorComparator) {
            return equality(op, ((BindablePropertyColor) bindable).getPropertyValue()
                    == ((TransitionValueColorComparator) comparator).getValue());
        }
        if (bindable instanceof BindablePropertyEnum && comparator instanceof TransitionValueIdComparator) {
            return equality(op, ((BindablePropertyEnum) bindable).getPropertyValue()
                    == ((TransitionValueIdComparator) comparator).getValue());
        }
        if (bindable instanceof BindablePropertyTrigger
                || bindable instanceof BindablePropertyInteger && comparator instanceof TransitionValueTriggerComparator) {
            int v = bindable instanceof BindablePropertyTrigger
                    ? ((BindablePropertyTrigger) bindable).getPropertyValue()
                    : ((BindablePropertyInteger) bindable).getPropertyValue();
            if (v != seenTrigger) {
                seenTrigger = v;
                return true;
            }
        }
        return false;
    }

    // Two bindable properties of one kind compared with each other.
    private static boolean comparePropertyPair(int op, BindableProperty a, BindableProperty b) {
        if (a instanceof BindablePropertyNumber && b instanceof BindablePropertyNumber) {
            return compare(op, ((BindablePropertyNumber) a).getPropertyValue(),
                    ((BindablePropertyNumber) b).getPropertyValue());
        }
        if (a instanceof BindablePropertyBoolean && b instanceof BindablePropertyBoolean) {
            return equality(op, ((BindablePropertyBoolean) a).isPropertyValue()
                    == ((BindablePropertyBoolean) b).isPropertyValue());
        }
        if (a instanceof BindablePropertyString && b instanceof BindablePropertyString) {
            String x = ((BindablePropertyString) a).getPropertyValue();
            String y = ((BindablePropertyString) b).getPropertyValue();
            return equality(op, x == null ? y == null : x.equals(y));
        }
        if (a instanceof BindablePropertyColor && b instanceof BindablePropertyColor) {
            return equality(op, ((BindablePropertyColor) a).getPropertyValue()
                    == ((BindablePropertyColor) b).getPropertyValue());
        }
        if (a instanceof BindablePropertyEnum && b instanceof BindablePropertyEnum) {
            return equality(op, ((BindablePropertyEnum) a).getPropertyValue()
                    == ((BindablePropertyEnum) b).getPropertyValue());
        }
        return false;
    }

    private static boolean equality(int op, boolean equal) {
        return op == 0 ? equal : op == 1 && !equal;
    }

    /// `TransitionConditionOp` applied to two numbers.
    static boolean compare(int op, float v, float target) {
        switch (op) {
            case 0:
                return v == target;
            case 1:
                return v != target;
            case 2:
                return v <= target;
            case 3:
                return v >= target;
            case 4:
                return v < target;
            case 5:
                return v > target;
            default:
                return false;
        }
    }
}
