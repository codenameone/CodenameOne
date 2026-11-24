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

/// Binds one property of a target object to a view model value; a port of
/// rive-runtime's `DataBind`. The flags pick the direction: to target
/// (source changes are written to the target), to source (the target's
/// value is written back), two way, or once (applied when bound, then left).
public class DataBind extends DataBindBase {
    public static final int FLAG_DIRECTION = 1;
    public static final int FLAG_TWO_WAY = 1 << 1;
    public static final int FLAG_ONCE = 1 << 2;
    public static final int FLAG_SOURCE_TO_TARGET_RUNS_FIRST = 1 << 3;

    private Core target;
    private ViewModelInstanceValue source;
    private DataConverter converter;
    private RiveFileContext file;
    private DataContext context;
    private boolean dirty = true;
    private boolean sampled;
    private final DataValue value = new DataValue();
    private final DataValue last = new DataValue();

    public void setTarget(Core t) {
        target = t;
    }

    public Core getTarget() {
        return target;
    }

    public ViewModelInstanceValue getSource() {
        return source;
    }

    public void setFile(RiveFileContext f) {
        file = f;
    }

    public DataConverter getConverter() {
        return converter;
    }

    public boolean toTarget() {
        return (getFlags() & FLAG_DIRECTION) == 0 || (getFlags() & FLAG_TWO_WAY) != 0;
    }

    public boolean toSource() {
        return (getFlags() & FLAG_DIRECTION) != 0 || (getFlags() & FLAG_TWO_WAY) != 0;
    }

    public boolean bindsOnce() {
        return (getFlags() & FLAG_ONCE) != 0;
    }

    /// Binds to the value this bind names in `root` (null unbinds).
    public void bind(ViewModelInstance root) {
        context = null;
        bindRoot(root);
    }

    /// Binds against `c`, whose parents resolve what its own instance does not.
    public void bind(DataContext c) {
        context = c;
        bindRoot(c == null ? null : c.getMain());
    }

    /// The context of the latest `bind(DataContext)`, or null.
    protected DataContext getContext() {
        return context;
    }

    private void bindRoot(ViewModelInstance root) {
        unbind();
        if (root == null) {
            return;
        }
        if (converter == null && file != null) {
            converter = copyOf(file.getConverter(getConverterId()));
        }
        if (converter != null) {
            converter.bind(root);
        }
        ViewModelInstanceValue s = resolveSource(root);
        if (s == null) {
            return;
        }
        source = s;
        if (!bindsOnce()) {
            s.addDependent(this);
        }
        dirty = true;
        sampled = false;
    }

    /// A copy of `c` for this bind alone, as upstream clones a converter for
    /// every bind using it: converters keep state (the interpolator's
    /// animation, the change counts of their own binds).
    static DataConverter copyOf(DataConverter c) {
        if (c == null) {
            return null;
        }
        Core copy = c.makeCopy();
        return copy instanceof DataConverter ? (DataConverter) copy : c;
    }

    /// Upstream's `DataBind::cloneWithTarget`: a copy of this bind writing
    /// to `newTarget`, with a copy of its converter.
    public DataBind cloneWithTarget(Core newTarget) {
        Core copy = makeCopy();
        if (!(copy instanceof DataBind)) {
            return this;
        }
        DataBind c = (DataBind) copy;
        c.setTarget(newTarget);
        c.setFile(file);
        DataConverter conv = converter;
        if (conv == null && file != null) {
            conv = file.getConverter(getConverterId());
        }
        c.converter = copyOf(conv);
        return c;
    }

    /// The value this bind reads; a plain bind names none.
    protected ViewModelInstanceValue resolveSource(ViewModelInstance root) {
        return null;
    }

    public void unbind() {
        if (source != null) {
            source.removeDependent(this);
            source = null;
        }
    }

    /// Advances this bind's converter (upstream `DataBind::advance`); a
    /// converter animating its output makes the bind convert again.
    public boolean advance(float elapsedSeconds) {
        if (converter != null && converter.advance(elapsedSeconds)) {
            dirty = true;
            return true;
        }
        return false;
    }

    /// The source changed.
    void sourceChanged() {
        dirty = true;
    }

    /// Writes the source to the target if it changed since the last write.
    public void updateSourceToTarget() {
        if (source == null || target == null || !toTarget()) {
            return;
        }
        if (converter != null && converter.refreshBinds()) {
            dirty = true;
        }
        if (!dirty) {
            return;
        }
        dirty = false;
        source.read(value);
        if (converter != null) {
            converter.convert(value, this);
        }
        if (target instanceof ArtboardComponentList) {
            ((ArtboardComponentList) target).setListSourceValue(value.type == DataValue.LIST && value.list != null ? value.list : source);
            return;
        }
        apply(target, getPropertyKey(), value);
        if (bindsOnce()) {
            source = null;
        }
    }

    /// Writes the target's value back to the source when it changed.
    public void updateTargetToSource() {
        updateTargetToSource(false);
    }

    /// Writes the target's value to the source; `force` writes it even when it
    /// did not change since the last sample (upstream `updateSourceBinding(true)`).
    public void updateTargetToSource(boolean force) {
        if (source == null || target == null || !toSource()) {
            return;
        }
        source.read(value);
        int sourceType = value.type;
        if (converter != null && converter.outputType() != DataValue.NONE) {
            sourceType = converter.outputType();
        }
        readTarget(sourceType);
        if (!force && sampled && value.sameAs(last)) {
            return;
        }
        boolean first = !sampled;
        sampled = true;
        last.copyFrom(value);
        if (!force && first && toTarget()) {
            return;
        }
        if (converter != null) {
            converter.reverseConvert(value, this);
        }
        source.write(value);
        dirty = false;
    }

    private void readTarget(int type) {
        int key = getPropertyKey();
        switch (type) {
            case DataValue.STRING:
                value.setString(target.getStringProperty(key));
                break;
            case DataValue.BOOLEAN:
                value.setBoolean(target.getBoolProperty(key));
                break;
            case DataValue.COLOR:
                value.setColor(target.getColorProperty(key));
                break;
            case DataValue.ENUM:
                value.setEnum(value.dataEnum, target.getUintProperty(key));
                break;
            case DataValue.TRIGGER:
                value.setTrigger(target.getUintProperty(key));
                break;
            case DataValue.VIEW_MODEL:
                value.setViewModel(target instanceof BindablePropertyViewModel
                        ? ((BindablePropertyViewModel) target).getViewModelInstanceValue() : null);
                break;
            default:
                value.setNumber(target.getDoubleProperty(key));
                break;
        }
    }

    /// Writes `v` to property `key` of `t`, converting it to the type the
    /// property has.
    public static void apply(Core t, int key, DataValue v) {
        switch (v.type) {
            case DataValue.NUMBER:
                if (t instanceof StateTransition && key == StateTransitionBase.DURATION_PROPERTY_KEY) {
                    // Upstream resolvedDuration(): a bound transition duration
                    // is rounded to whole milliseconds and never negative.
                    // Artboard instances are built fresh, so the transition
                    // written here belongs to this instance alone, which is
                    // what upstream's per-instance BindablePropertyNumber buys.
                    ((StateTransition) t).setDuration(v.number < 0 ? 0 : Math.round(v.number));
                    break;
                }
                if (!t.setDoubleProperty(key, v.number) && !t.setStringProperty(key, v.asString())) {
                    t.setUintProperty(key, (int) v.number);
                }
                break;
            case DataValue.STRING:
                if (!t.setStringProperty(key, v.string)) {
                    t.setDoubleProperty(key, v.asNumber());
                }
                break;
            case DataValue.BOOLEAN:
                if (!t.setBoolProperty(key, v.bool)) {
                    t.setDoubleProperty(key, v.bool ? 1 : 0);
                }
                break;
            case DataValue.COLOR:
                t.setColorProperty(key, v.integer);
                break;
            case DataValue.VIEW_MODEL:
                if (t instanceof BindablePropertyViewModel) {
                    ((BindablePropertyViewModel) t).setViewModelInstanceValue(v.viewModel);
                }
                break;
            case DataValue.ENUM:
                if (!t.setUintProperty(key, v.integer)) {
                    t.setStringProperty(key, v.asString());
                }
                break;
            case DataValue.TRIGGER:
            case DataValue.LIST:
                if (!t.setUintProperty(key, v.integer)) {
                    t.setDoubleProperty(key, v.integer);
                }
                break;
            default:
                break;
        }
    }
}
