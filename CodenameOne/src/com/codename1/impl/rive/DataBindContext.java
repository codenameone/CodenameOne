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

/// A data bind whose source is named by a path of ids from the bound view
/// model instance: the view model id, the ids of nested view model
/// properties, then the property id (upstream `DataContext::getViewModelProperty`).
public class DataBindContext extends DataBindContextBase {
    private int[] path;

    @Override
    protected ViewModelInstanceValue resolveSource(ViewModelInstance root) {
        if (path == null) {
            path = decodePath(getSourcePathIds());
        }
        DataContext c = getContext();
        return c != null ? c.resolve(path) : resolvePath(root, path);
    }

    /// The var-uint ids in `bytes`.
    /// The number of ids in the source path.
    public int sourcePathLength() {
        if (path == null) {
            path = decodePath(getSourcePathIds());
        }
        return path.length;
    }

    public static int[] decodePath(byte[] bytes) {
        if (bytes == null) {
            return new int[0];
        }
        int count = 0;
        for (byte b : bytes) {
            if ((b & 0x80) == 0) {
                count++;
            }
        }
        int[] out = new int[count];
        int n = 0;
        int v = 0;
        int shift = 0;
        for (int i = 0; i < bytes.length && n < count; i++) {
            v |= (bytes[i] & 0x7f) << shift;
            shift += 7;
            if ((bytes[i] & 0x80) == 0) {
                out[n++] = v;
                v = 0;
                shift = 0;
            }
        }
        return out;
    }

    /// The value `path` names from `root`, or null.
    public static ViewModelInstanceValue resolvePath(ViewModelInstance root, int[] path) {
        if (root == null || path.length < 2 || root.getViewModelId() != path[0]) {
            return null;
        }
        ViewModelInstance current = root;
        for (int i = 1; i < path.length - 1; i++) {
            ViewModelInstanceValue v = current.propertyValue(path[i]);
            if (!(v instanceof ViewModelInstanceViewModel)) {
                return null;
            }
            current = ((ViewModelInstanceViewModel) v).getReference();
            if (current == null) {
                return null;
            }
        }
        return current.propertyValue(path[path.length - 1]);
    }
}
