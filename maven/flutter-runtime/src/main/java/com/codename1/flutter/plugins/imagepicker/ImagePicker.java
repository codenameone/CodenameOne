/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.imagepicker;

import com.codename1.capture.Capture;
import com.codename1.flutter.plugins.crossfile.XFile;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartList;

/**
 * image_picker's {@code ImagePicker}: the camera through Capture, the gallery through
 * {@code Display.openGallery}. A cancelled pick, or one with no display to pick on,
 * answers null (an empty list for the multi-pickers), as image_picker does. The size
 * and quality options are accepted; the picked file is returned as captured.
 */
public class ImagePicker {

    public ImagePicker() {
    }

    public Future<XFile> pickImage(ImageSource source, Double maxWidth, Double maxHeight,
            Long imageQuality, Object preferredCameraDevice, Boolean requestFullMetadata) {
        return pick(source, Display.GALLERY_IMAGE);
    }

    public Future<XFile> pickVideo(ImageSource source, Object preferredCameraDevice, Object maxDuration) {
        return pick(source, Display.GALLERY_VIDEO);
    }

    public Future<XFile> pickMedia(Double maxWidth, Double maxHeight, Long imageQuality,
            Boolean requestFullMetadata) {
        return pick(ImageSource.gallery, Display.GALLERY_ALL);
    }

    public Future<DartList<XFile>> pickMultiImage(Double maxWidth, Double maxHeight,
            Long imageQuality, Long limit, Boolean requestFullMetadata) {
        return pickMany(Display.GALLERY_IMAGE_MULTI, Display.GALLERY_IMAGE, limit);
    }

    public Future<DartList<XFile>> pickMultipleMedia(Double maxWidth, Double maxHeight,
            Long imageQuality, Long limit, Boolean requestFullMetadata) {
        return pickMany(Display.GALLERY_ALL_MULTI, Display.GALLERY_ALL, limit);
    }

    private static Future<DartList<XFile>> asList(Future<XFile> one) {
        final Completer<DartList<XFile>> done = new Completer<DartList<XFile>>();
        one.then(new dart.runtime.Funcs.VoidFunc1<XFile>() {
            @Override
            public void call(XFile f) {
                DartList<XFile> out = new DartList<XFile>();
                if (f != null) {
                    out.add(f);
                }
                done.complete(out);
            }
        }, new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                // a failed single pick fails the list; without this the await never returned
                done.completeError(e);
            }
        });
        return done.future();
    }

    /**
     * The multi-select gallery where the platform offers it (iOS needs the
     * ios.enableGalleryMultiselect build hint), else a single pick as a list.
     */
    /// `limit` is the plugin's maximum selection. Codename One's gallery cannot be told
    /// a maximum, so the picker may let the user choose more; the result is capped to
    /// the first `limit` files, so the app never receives more than it allowed.
    private static Future<DartList<XFile>> pickMany(int multiType, int singleType, Long limit) {
        if (limit != null && limit.longValue() < 2) {
            return Future.error(new dart.core.ArgumentError("cannot be lower than 2: limit"));
        }
        final long max = limit == null ? Long.MAX_VALUE : limit.longValue();
        if (!Display.isInitialized()) {
            return Future.value(new DartList<XFile>());
        }
        if (!Display.getInstance().isGalleryTypeSupported(multiType)) {
            return asList(pick(ImageSource.gallery, singleType));
        }
        final Completer<DartList<XFile>> done = new Completer<DartList<XFile>>();
        Display.getInstance().openGallery(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                DartList<XFile> out = new DartList<XFile>();
                Object src = evt == null ? null : evt.getSource();
                if (src instanceof String[]) {
                    for (String path : (String[]) src) {
                        if (out.size() >= max) {
                            break;
                        }
                        out.add(new XFile(path));
                    }
                } else if (src instanceof String) {
                    out.add(new XFile((String) src));
                }
                done.complete(out);
            }
        }, multiType);
        return done.future();
    }

    private static Future<XFile> pick(ImageSource source, int galleryType) {
        if (!Display.isInitialized()) {
            return Future.value(null);
        }
        final Completer<XFile> done = new Completer<XFile>();
        ActionListener<ActionEvent> onPicked = new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object src = evt == null ? null : evt.getSource();
                done.complete(src instanceof String ? new XFile((String) src) : null);
            }
        };
        if (source == ImageSource.camera) {
            if (galleryType == Display.GALLERY_VIDEO) {
                Capture.captureVideo(onPicked);
            } else {
                Capture.capturePhoto(onPicked);
            }
        } else {
            Display.getInstance().openGallery(onPicked, galleryType);
        }
        return done.future();
    }

    /** Android's recovery of a pick interrupted by the activity being destroyed; nothing here. */
    public Future<Object> retrieveLostData() {
        return Future.value(null);
    }
}
