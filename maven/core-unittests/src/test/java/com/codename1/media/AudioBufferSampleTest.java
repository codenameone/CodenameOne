package com.codename1.media;

import com.codename1.capture.Capture;
import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class AudioBufferSampleTest extends UITestBase {

    @FormTest
    void captureAudioRedirectsFramesToBufferAndPersistsWav() throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        implementation.clearFileSystem();
        implementation.clearAudioCaptureFrames();
        String recordingsDir = fs.getAppHomePath() + "recordings/";
        fs.mkdir(recordingsDir);

        String bufferPath = "tmpBuffer.pcm";
        int wavSampleRate = 16000;
        AudioBuffer audioBuffer = MediaManager.getAudioBuffer(bufferPath, true, 8);
        final float[] captured = new float[audioBuffer.getMaxSize()];
        final int[] callbackCount = new int[1];
        final int[] lastSampleRate = new int[1];
        final int[] lastSize = new int[1];

        WAVWriter writer = new WAVWriter(new File("tmpBuffer.wav"), wavSampleRate, 1, 16);
        try {
            audioBuffer.addCallback(new AudioBuffer.AudioBufferCallback() {
                public void frameReceived(AudioBuffer buffer) {
                    if (buffer.getSampleRate() > wavSampleRate) {
                        buffer.downSample(wavSampleRate);
                    }
                    buffer.copyTo(captured);
                    lastSampleRate[0] = buffer.getSampleRate();
                    lastSize[0] = buffer.getSize();
                    callbackCount[0]++;
                    try {
                        writer.write(captured, 0, buffer.getSize());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            MediaRecorderBuilder options = new MediaRecorderBuilder()
                    .audioChannels(1)
                    .redirectToAudioBuffer(true)
                    .path(bufferPath);

            implementation.addAudioCaptureFrame(32000, 1, new float[]{1f, 0.5f, -0.5f, -1f});

            Capture.captureAudio(options);

            writer.close();

            String copyPath = recordingsDir + "sample.wav";
            Util.copy(fs.openInputStream(new File("tmpBuffer.wav").getAbsolutePath()), fs.openOutputStream(copyPath));

            assertEquals(1, callbackCount[0]);
            assertEquals(wavSampleRate, lastSampleRate[0]);
            assertTrue(lastSize[0] > 0);
            assertSame(options, implementation.getLastMediaRecorderBuilder());
            assertTrue(fs.exists(copyPath));
            assertTrue(fs.getFileLength(copyPath) > 44);
        } finally {
            MediaManager.releaseAudioBuffer(bufferPath);
            try {
                writer.close();
            } catch (Exception ignored) {
            }
            implementation.clearFileSystem();
        }
    }
}

