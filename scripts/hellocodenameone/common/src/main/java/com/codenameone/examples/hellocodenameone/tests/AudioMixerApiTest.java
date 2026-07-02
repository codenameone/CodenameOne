package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.media.AudioBuffer;
import com.codename1.media.AudioEffects;
import com.codename1.media.AudioMixer;
import com.codename1.media.WAVWriter;
import com.codename1.ui.Display;

/**
 * On-device coverage for the platform-neutral PCM mixer.  The core unit tests
 * cover the same vectors on JavaSE; this test runs them inside the DeviceRunner
 * app so iOS, Android, Mac native, JavaSE, and JavaScript translation paths all
 * compile and execute the API.
 */
public class AudioMixerApiTest extends BaseTest {
    @Override
    public boolean runTest() {
        try {
            AudioBuffer mixed = runMixVector();
            runEffectsVector();
            if (!"HTML5".equals(Display.getInstance().getPlatformName())) {
                runWavWriterRoundTrip(mixed);
            }
        } catch (Throwable t) {
            fail("AudioMixer API test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    private AudioBuffer runMixVector() {
        AudioBuffer first = buffer(1000, 2, new float[]{
                0.25f, -0.25f,
                0.25f, -0.25f
        });
        AudioBuffer second = buffer(1000, 2, new float[]{
                0.5f, 0.5f,
                -0.5f, -0.5f
        });

        AudioMixer mixer = new AudioMixer(1000, 2);
        mixer.addTrack(first, 0, 1.0f);
        mixer.addTrack(second, 1, 0.5f);

        checkEqual(2, mixer.getTrackCount(), "track count");
        checkEqual(3, mixer.getOutputFrameCount(), "output frame count");
        checkEqual(6, mixer.getOutputSize(), "output sample count");

        AudioBuffer mixed = mixer.mix();
        checkEqual(1000, mixed.getSampleRate(), "mixed sample rate");
        checkEqual(2, mixed.getNumChannels(), "mixed channel count");
        assertSamples(new float[]{
                0.25f, -0.25f,
                0.5f, 0.0f,
                -0.25f, -0.25f
        }, mixed, "mixed PCM");

        assertSamples(new float[]{0.0f, 0.0f, 1.0f, -1.0f},
                new AudioMixer(48000, 1)
                        .addTrackAtFrame(new float[]{0.75f, -0.75f}, 0, 2, 2, 1.0f)
                        .addTrackAtFrame(new float[]{0.75f, -0.75f}, 0, 2, 2, 1.0f)
                        .mix(),
                "clipped PCM");

        assertThrows(new Runnable() {
            @Override
            public void run() {
                new AudioMixer(44100, 1).addTrack(buffer(48000, 1, new float[]{0.1f}), 0, 1.0f);
            }
        }, "mismatched sample rate");

        assertThrows(new Runnable() {
            @Override
            public void run() {
                new AudioMixer(44100, 2).addTrackAtFrame(new float[]{0.1f}, 0, 1, 0, 1.0f);
            }
        }, "odd interleaved sample count");

        return mixed;
    }

    private void runEffectsVector() {
        assertSamples(new float[]{0.5f, -1.0f},
                AudioEffects.gain(buffer(8000, 1, new float[]{0.25f, -0.75f}), 2.0f),
                "gain PCM");
        assertSamples(new float[]{0.5f, -1.0f},
                AudioEffects.normalize(buffer(8000, 1, new float[]{0.25f, -0.5f}), 1.0f),
                "normalized PCM");
        assertSamples(new float[]{0.0f, 0.0f, 0.0f, 0.0f},
                AudioEffects.equalize(buffer(8000, 1, new float[]{1.0f, -1.0f, 1.0f, -1.0f}),
                        0.0f, 0.0f, 0.0f, 500.0f, 2000.0f),
                "equalized mute PCM");
        assertSamples(new float[]{0.0f, 0.0f, 0.25f, -0.25f},
                AudioEffects.removeCenter(buffer(44100, 2, new float[]{
                        0.8f, 0.8f,
                        0.75f, 0.25f
                })),
                "center removed PCM");
        assertSamples(new float[]{0.8f, 0.8f, 0.5f, 0.5f},
                AudioEffects.isolateCenter(buffer(44100, 2, new float[]{
                        0.8f, 0.8f,
                        0.75f, 0.25f
                })),
                "center isolated PCM");

        assertThrows(new Runnable() {
            @Override
            public void run() {
                AudioEffects.removeCenter(buffer(8000, 1, new float[]{0.1f}));
            }
        }, "center removal mono input");
    }

    private void runWavWriterRoundTrip(AudioBuffer mixed) throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String outputPath = fs.getAppHomePath() + "audio-mixer-api-test.wav";
        if (fs.exists(outputPath)) {
            fs.delete(outputPath);
        }

        WAVWriter writer = new WAVWriter(new File(outputPath),
                mixed.getSampleRate(), mixed.getNumChannels(), 16);
        try {
            float[] pcm = new float[mixed.getSize()];
            mixed.copyTo(pcm);
            writer.write(pcm, 0, mixed.getSize());
            writer.close();
            writer = null;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        checkTrue(fs.exists(outputPath), "mixed WAV file was not created");
        checkEqual(44L + mixed.getSize() * 2L, fs.getLength(outputPath), "mixed WAV length");
        fs.delete(outputPath);
    }

    private AudioBuffer buffer(int sampleRate, int numChannels, float[] data) {
        AudioBuffer buffer = new AudioBuffer(data.length);
        buffer.copyFrom(sampleRate, numChannels, data);
        return buffer;
    }

    private void assertSamples(float[] expected, AudioBuffer actual, String label) {
        float[] actualSamples = new float[actual.getSize()];
        actual.copyTo(actualSamples);
        checkEqual(expected.length, actualSamples.length, label + " length");
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actualSamples[i]) > 0.0001f) {
                failNow(label + " mismatch at " + i + ": expected " + expected[i] + " got " + actualSamples[i]);
            }
        }
    }

    private void assertThrows(Runnable runnable, String label) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        failNow("Expected IllegalArgumentException for " + label);
    }

    private void checkEqual(int expected, int actual, String label) {
        if (expected != actual) {
            failNow(label + ": expected " + expected + " got " + actual);
        }
    }

    private void checkEqual(long expected, long actual, String label) {
        if (expected != actual) {
            failNow(label + ": expected " + expected + " got " + actual);
        }
    }

    private void checkTrue(boolean value, String label) {
        if (!value) {
            failNow(label);
        }
    }

    private void failNow(String message) {
        fail(message);
        throw new IllegalStateException(message);
    }
}
