package com.codename1.media;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicBoolean;

public class AudioBufferCoverageTest extends UITestBase {

    @FormTest
    public void testAddRemoveCallbackDuringFire() {
        AudioBuffer buffer = new AudioBuffer(1024);

        final AtomicBoolean callback2Ran = new AtomicBoolean(false);
        final AudioBuffer.AudioBufferCallback callback2 = new AudioBuffer.AudioBufferCallback() {
            @Override
            public void frameReceived(AudioBuffer buffer) {
                callback2Ran.set(true);
            }
        };

        final AudioBuffer.AudioBufferCallback callback3 = new AudioBuffer.AudioBufferCallback() {
            @Override
            public void frameReceived(AudioBuffer buffer) {
                // Do nothing
            }
        };
        buffer.addCallback(callback3);


        // Add a callback that adds another callback while firing
        // Covers AudioBuffer$1 (Runnable in addCallback)
        buffer.addCallback(new AudioBuffer.AudioBufferCallback() {
            @Override
            public void frameReceived(AudioBuffer b) {
                // This is called during fireFrameReceived
                // So inFireFrame is true.
                // Adding callback2 should be deferred.
                b.addCallback(callback2);

                // Removing callback3 should be deferred.
                // Covers AudioBuffer$2 (Runnable in removeCallback)
                b.removeCallback(callback3);
            }
        });

        float[] data = new float[1024];
        // This triggers fireFrameReceived
        buffer.copyFrom(44100, 1, data);

        // At this point, fireFrameReceived finished.
        // Pending ops should have run.
        // callback2 should be added.
        // callback3 should be removed.

        callback2Ran.set(false);
        buffer.copyFrom(44100, 1, data);

        Assertions.assertTrue(callback2Ran.get(), "Callback added during fire should be active in next fire");
    }
}
