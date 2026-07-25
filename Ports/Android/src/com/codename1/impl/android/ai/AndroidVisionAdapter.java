/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import android.graphics.Rect;

import com.codename1.ai.vision.VisionException;
import com.codename1.ai.vision.VisionMetadata;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionRect;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.mlkit.vision.common.InputImage;

/** Shared contract and result helpers for feature-scoped Android adapters. */
abstract class AndroidVisionAdapter {
    static final VisionMetadata METADATA = new VisionMetadata("ml-kit");

    abstract void analyze(InputImage input, int imageWidth, int imageHeight,
                          VisionOptions options, AsyncResource<?> out);

    static VisionRect normalized(Rect rect, int imageWidth, int imageHeight) {
        if (rect == null) {
            return VisionRect.EMPTY;
        }
        return new VisionRect(rect.left / (float) imageWidth,
                rect.top / (float) imageHeight,
                rect.width() / (float) imageWidth,
                rect.height() / (float) imageHeight);
    }

    static <T> void complete(final AsyncResource<T> out, final T value) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                out.complete(value);
            }
        });
    }

    static OnFailureListener failure(final AsyncResource<?> out,
                                     final AutoCloseable client) {
        return new OnFailureListener() {
            public void onFailure(final Exception error) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        out.error(new VisionException(
                                VisionException.BACKEND_ERROR,
                                error.getMessage(), error));
                    }
                });
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }
        };
    }
}
