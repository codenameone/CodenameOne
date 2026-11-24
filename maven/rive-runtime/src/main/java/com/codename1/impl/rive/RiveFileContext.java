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

/// What an artboard can reach in the file it came from: the file's assets
/// (images, fonts, audio) by index, and the file's other artboards, which a
/// nested artboard instances.
public final class RiveFileContext {
    /// Builds a fresh copy of one of the file's artboards.
    public interface ArtboardFactory {
        /// A new, independent copy of artboard `index`, or null.
        Artboard instance(int index);
    }

    private final ArrayList<FileAsset> assets = new ArrayList<FileAsset>();
    private final ArtboardFactory factory;
    private final ArrayList<ViewModel> viewModels = new ArrayList<ViewModel>();
    private final ArrayList<DataEnum> enums = new ArrayList<DataEnum>();
    private final ArrayList<DataConverter> converters = new ArrayList<DataConverter>();
    private final ArrayList<Core> interpolators = new ArrayList<Core>();

    public RiveFileContext(ArtboardFactory factory) {
        this.factory = factory;
    }

    /// The most artboards one top-level instancing may build, nested
    /// artboards and list rows included. A file whose artboards each nest
    /// several others grows exponentially with depth; past this the rest
    /// come back null and simply do not draw.
    public static final int MAX_INSTANCES = 4096;

    // The artboard indices being built right now, outermost first.
    private int[] building = new int[8];
    private int buildDepth;
    private int builtSinceTop;

    /// A new copy of the file's artboard `index`, or null. Null as well when
    /// `index` is already being built further out (a nested artboard or list
    /// row that contains itself, directly or through others), and once
    /// [#MAX_INSTANCES] artboards were built under one top-level call.
    public Artboard instanceArtboard(int index) {
        for (int i = 0; i < buildDepth; i++) {
            if (building[i] == index) {
                return null;
            }
        }
        if (buildDepth == 0) {
            builtSinceTop = 0;
        }
        if (builtSinceTop >= MAX_INSTANCES) {
            return null;
        }
        builtSinceTop++;
        if (buildDepth == building.length) {
            int[] grown = new int[building.length * 2];
            System.arraycopy(building, 0, grown, 0, building.length);
            building = grown;
        }
        building[buildDepth++] = index;
        try {
            return factory.instance(index);
        } finally {
            buildDepth--;
        }
    }

    // The view model id of each artboard index, -2 while not yet known.
    private int[] artboardViewModels;

    /// Declares how many artboards the file has.
    public void setArtboardCount(int n) {
        artboardViewModels = new int[n];
        for (int i = 0; i < n; i++) {
            artboardViewModels[i] = -2;
        }
    }

    /// Records the view model of artboard `index` once it has been built.
    public void noteArtboard(int index, Artboard a) {
        if (artboardViewModels != null && index >= 0 && index < artboardViewModels.length && a != null) {
            artboardViewModels[index] = a.getViewModelId();
        }
    }

    /// The index of the first artboard whose view model is `viewModelId`, or
    /// -1. An artboard not built yet is built once to learn it.
    public int artboardIndexForViewModel(int viewModelId) {
        if (artboardViewModels == null) {
            return -1;
        }
        for (int i = 0; i < artboardViewModels.length; i++) {
            if (artboardViewModels[i] == -2) {
                Artboard a = instanceArtboard(i);
                if (a == null && buildDepth > 0) {
                    // Refused as a cycle or over the cap; learn it another time.
                    continue;
                }
                artboardViewModels[i] = a == null ? -1 : a.getViewModelId();
            }
            if (artboardViewModels[i] == viewModelId) {
                return i;
            }
        }
        return -1;
    }

    public void addAsset(FileAsset a) {
        assets.add(a);
    }

    public ArrayList<FileAsset> getAssets() {
        return assets;
    }

    public void addViewModel(ViewModel vm) {
        viewModels.add(vm);
    }

    /// The file's view model at `index`, or null.
    public ViewModel getViewModel(int index) {
        return index >= 0 && index < viewModels.size() ? viewModels.get(index) : null;
    }

    /// The file's view model named `name`, or null.
    public ViewModel getViewModel(String name) {
        for (int i = 0; i < viewModels.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            if (name != null && name.equals(viewModels.get(i).getName())) {
                return viewModels.get(i);
            }
        }
        return null;
    }

    public ArrayList<ViewModel> getViewModels() {
        return viewModels;
    }

    public void addEnum(DataEnum e) {
        enums.add(e);
    }

    /// The file's enum at `index`, or null.
    public DataEnum getEnum(int index) {
        return index >= 0 && index < enums.size() ? enums.get(index) : null;
    }

    public void addConverter(DataConverter c) {
        converters.add(c);
        c.setFile(this);
    }

    /// The file's converter at `index`, or null.
    public DataConverter getConverter(int index) {
        return index >= 0 && index < converters.size() ? converters.get(index) : null;
    }

    public void addInterpolator(Core i) {
        interpolators.add(i);
    }

    /// The file level interpolator at `index`, or null.
    public Core getInterpolator(int index) {
        return index >= 0 && index < interpolators.size() ? interpolators.get(index) : null;
    }

    /// A fresh copy of the default instance (the first) of the view model
    /// `artboard` names, or null: upstream's default instance rule.
    public ViewModelInstance createDefaultInstance(Artboard artboard) {
        ViewModel vm = getViewModel(artboard.getViewModelId());
        ViewModelInstance template = vm == null ? null : vm.defaultInstance();
        return template == null ? null : template.copyInstance(this, 0);
    }

    public FileAsset getAsset(int index) {
        return index >= 0 && index < assets.size() ? assets.get(index) : null;
    }
}
