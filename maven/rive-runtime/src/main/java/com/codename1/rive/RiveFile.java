/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.rive;

import com.codename1.impl.rive.Artboard;
import com.codename1.impl.rive.RiveBuilder;
import com.codename1.impl.rive.RiveFileContext;

import java.util.ArrayList;

/// A Rive (`.riv`) file, translated to Java by the build.
///
/// Put the file under `src/main/rive` and the build generates a subclass of
/// this class for it, named after the file: `coin.riv` becomes `CoinRive`.
/// The generated class creates the file's objects directly, so nothing is
/// parsed on the device, and an application ships only the parts of the
/// runtime its files use.
///
/// ```java
/// RiveView view = new RiveView(new CoinRive());
/// view.play();
/// ```
///
/// Every view gets an artboard of its own, so one file can play in several
/// places at once without the copies affecting each other.
public abstract class RiveFile {
    private final String[] artboardNames;
    private final String[][] animationNames;
    private final String[][] stateMachineNames;
    private RiveFileContext context;

    /// Called by the generated subclass with the names in the file: one entry
    /// per artboard, in file order, and for each artboard the names of its
    /// animations and of its state machines.
    protected RiveFile(String[] artboardNames, String[][] animationNames, String[][] stateMachineNames) {
        this.artboardNames = artboardNames;
        this.animationNames = animationNames;
        this.stateMachineNames = stateMachineNames;
    }

    /// Generated: creates the file's assets (images, fonts, audio) through `builder`.
    protected abstract void buildAssets(RiveBuilder builder);

    /// Generated: creates the objects of artboard `index` through `builder`.
    protected abstract void buildArtboard(int index, RiveBuilder builder);

    /// The names of the artboards, in file order.
    public String[] getArtboardNames() {
        return copy(artboardNames);
    }

    /// The names of the animations of the artboard `artboard` (null for the first).
    public String[] getAnimationNames(String artboard) {
        int i = indexOf(artboard);
        return i < 0 ? new String[0] : copy(animationNames[i]);
    }

    /// The names of the state machines of the artboard `artboard` (null for the first).
    public String[] getStateMachineNames(String artboard) {
        int i = indexOf(artboard);
        return i < 0 ? new String[0] : copy(stateMachineNames[i]);
    }

    private static String[] copy(String[] a) {
        String[] out = new String[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }

    private int indexOf(String name) {
        if (artboardNames.length == 0) {
            return -1;
        }
        if (name == null) {
            return 0;
        }
        for (int i = 0; i < artboardNames.length; i++) {
            if (name.equals(artboardNames[i])) {
                return i;
            }
        }
        return -1;
    }

    /// A fresh, independent instance of the artboard `name` (null for the
    /// first), or null if there is none by that name.
    Artboard instance(String name) {
        return instance(indexOf(name));
    }

    Artboard instance(int index) {
        if (index < 0 || index >= artboardNames.length) {
            return null;
        }
        if (context == null) {
            context = new RiveFileContext(new RiveFileContext.ArtboardFactory() {
                @Override
                public Artboard instance(int i) {
                    return RiveFile.this.instance(i);
                }
            });
            context.setArtboardCount(artboardNames.length);
            RiveBuilder assets = new RiveBuilder(context);
            buildAssets(assets);
            assets.finish();
        }
        RiveBuilder b = new RiveBuilder(context);
        buildArtboard(index, b);
        ArrayList<Artboard> built = b.finish();
        Artboard a = built.isEmpty() ? null : built.get(0);
        context.noteArtboard(index, a);
        return a;
    }
}
