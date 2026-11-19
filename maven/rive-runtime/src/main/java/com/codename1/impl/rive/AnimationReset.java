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

/// Port of `AnimationReset` (`fromStates`): when a transition starts, the
/// values every double and color property the two states' animations key
/// hold right now. Applied before the states each frame of the mix, it is the
/// baseline both mix from -- so a transition that interrupts another mixes
/// from the blend the interrupted one had reached, not from whatever the
/// previous frame left behind. Instances are kept per layer and refilled, so
/// a transition allocates only when it keys more values than any before it.
final class AnimationReset {
    private Core[] objects = new Core[8];
    private int[] keys = new int[8];
    private boolean[] color = new boolean[8];
    private float[] doubles = new float[8];
    private int[] colors = new int[8];
    private int count;

    /// Upstream `clear`.
    void clear() {
        for (int i = 0; i < count; i++) {
            objects[i] = null;
        }
        count = 0;
    }

    /// Upstream `build(animations, count, artboard, false)`: snapshots the
    /// current values of each distinct object/property the animations key.
    void build(LinearAnimation a, LinearAnimation b, Artboard artboard) {
        clear();
        add(a, artboard);
        add(b, artboard);
    }

    private void add(LinearAnimation animation, Artboard artboard) {
        if (animation == null) {
            return;
        }
        for (int o = 0; o < animation.getKeyedObjectCount(); o++) {
            KeyedObject ko = animation.getKeyedObject(o);
            Core object = artboard.resolve(ko.getObjectId());
            if (object == null) {
                continue;
            }
            for (int k = 0; k < ko.getKeyedPropertyCount(); k++) {
                KeyedProperty kp = ko.getKeyedProperty(k);
                if (kp.getKeyFrameCount() == 0) {
                    continue;
                }
                KeyFrame first = kp.getKeyFrame(0);
                boolean isColor = first instanceof KeyFrameColor;
                if (!isColor && !(first instanceof KeyFrameDouble)) {
                    continue;
                }
                int key = kp.getPropertyKey();
                if (contains(object, key)) {
                    continue;
                }
                grow();
                objects[count] = object;
                keys[count] = key;
                color[count] = isColor;
                if (isColor) {
                    colors[count] = object.getColorProperty(key);
                } else {
                    doubles[count] = object.getDoubleProperty(key);
                }
                count++;
            }
        }
    }

    private boolean contains(Core object, int key) {
        for (int i = 0; i < count; i++) {
            if (objects[i] == object && keys[i] == key) { // NOPMD CompareObjectsWithEquals - identity is the intent
                return true;
            }
        }
        return false;
    }

    private void grow() {
        if (count < objects.length) {
            return;
        }
        int n = objects.length * 2;
        Core[] no = new Core[n];
        int[] nk = new int[n];
        boolean[] nc = new boolean[n];
        float[] nd = new float[n];
        int[] ncol = new int[n];
        System.arraycopy(objects, 0, no, 0, count);
        System.arraycopy(keys, 0, nk, 0, count);
        System.arraycopy(color, 0, nc, 0, count);
        System.arraycopy(doubles, 0, nd, 0, count);
        System.arraycopy(colors, 0, ncol, 0, count);
        objects = no;
        keys = nk;
        color = nc;
        doubles = nd;
        colors = ncol;
    }

    /// Upstream `apply`: writes every snapshot value back.
    void apply() {
        for (int i = 0; i < count; i++) {
            if (color[i]) {
                objects[i].setColorProperty(keys[i], colors[i]);
            } else {
                objects[i].setDoubleProperty(keys[i], doubles[i]);
            }
        }
    }
}
