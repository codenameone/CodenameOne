#!/usr/bin/env python3
"""Compile the actual frame-delivery methods without the rest of the CN1 UI runtime."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[2]
out = Path(sys.argv[1])

def copy(relative, package):
    target = out / package / Path(relative).name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text((root / relative).read_text())

copy('Ports/Android/src/com/codename1/impl/android/AndroidCameraFrameConverter.java', 'com/codename1/impl/android')
for name in ['CameraFrame', 'FrameFormat']:
    copy('CodenameOne/src/com/codename1/camera/' + name + '.java', 'com/codename1/camera')
source = (root / 'Ports/Android/src/com/codename1/impl/android/AndroidCameraImpl.java').read_text()

def method(signature):
    start = source.index(signature)
    end = source.index('{', start) + 1
    depth = 1
    while depth:
        if source[end] == '{': depth += 1
        if source[end] == '}': depth -= 1
        end += 1
    return source[start:end]

methods = '\n'.join(method(signature) for signature in [
    'public void setFrameListener(', 'private void onImageProxy(',
    'private static void closeImageProxy(', 'private static CameraFrame createFrame('])
(out / 'com/codename1/impl/android/CameraFrameHarness.java').write_text('''
package com.codename1.impl.android;
import android.media.Image;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.util.Log;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import com.codename1.camera.*;

public class CameraFrameHarness {
    private static final String TAG = "CameraRegression";
    private final AtomicBoolean listenerBusy = new AtomicBoolean();
    private volatile FrameListener frameListener;
    private volatile FrameFormat frameFormat = FrameFormat.JPEG;
    private volatile long frameIntervalNanos;
    private long lastFrameNanos;
    public interface FrameListener { void onFrame(CameraFrame frame); }
    public void deliver(Object proxy) { onImageProxy(proxy); }
''' + methods + '\n}\n')
