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

import java.util.ArrayList;

/// The keyed properties of one artboard object in a linear animation.
public class KeyedObject extends KeyedObjectBase {
    private final ArrayList<KeyedProperty> keyedProperties = new ArrayList<KeyedProperty>();
    private Core target;

    /// Appends a keyed property in file order.
    public void addKeyedProperty(KeyedProperty property) {
        keyedProperties.add(property);
    }

    public int getKeyedPropertyCount() {
        return keyedProperties.size();
    }

    public KeyedProperty getKeyedProperty(int index) {
        return keyedProperties.get(index);
    }

    /// The resolved target, valid after `initialize`.
    public Core getTarget() {
        return target;
    }

    /// Resolves the target object; false when it does not exist.
    public boolean initialize(Artboard artboard) {
        target = artboard.resolve(getObjectId());
        if (target == null) {
            return false;
        }
        for (int i = keyedProperties.size() - 1; i >= 0; i--) {
            if (!keyedProperties.get(i).initialize(artboard)) {
                keyedProperties.remove(i);
            }
        }
        return true;
    }

    /// Applies every keyed property at `seconds`.
    public void apply(Artboard artboard, float seconds, float mix) {
        Core object = target;
        if (object == null) {
            object = artboard.resolve(getObjectId());
            if (object == null) {
                return;
            }
        }
        int n = keyedProperties.size();
        for (int i = 0; i < n; i++) {
            keyedProperties.get(i).apply(object, seconds, mix);
        }
    }
}
