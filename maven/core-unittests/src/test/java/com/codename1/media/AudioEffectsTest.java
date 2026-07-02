package com.codename1.media;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

class AudioEffectsTest extends UITestBase {

    @FormTest
    void appliesGainAndClips() {
        AudioBuffer effected = AudioEffects.gain(buffer(8000, 1, new float[]{0.25f, -0.75f}), 2.0f);

        assertSamples(new float[]{0.5f, -1.0f}, effected);
        Assertions.assertEquals(8000, effected.getSampleRate());
        Assertions.assertEquals(1, effected.getNumChannels());
    }

    @FormTest
    void normalizesToTargetPeak() {
        AudioBuffer effected = AudioEffects.normalize(buffer(8000, 1, new float[]{0.25f, -0.5f}), 1.0f);

        assertSamples(new float[]{0.5f, -1.0f}, effected);
    }

    @FormTest
    void canShapeBandsWithEqualizer() {
        AudioBuffer source = buffer(8000, 1, new float[]{1.0f, -1.0f, 1.0f, -1.0f});

        AudioBuffer muted = AudioEffects.equalize(source, 0.0f, 0.0f, 0.0f, 500.0f, 2000.0f);
        AudioBuffer boosted = AudioEffects.equalize(source, 1.5f, 1.5f, 1.5f, 500.0f, 2000.0f);

        assertSamples(new float[]{0.0f, 0.0f, 0.0f, 0.0f}, muted);
        Assertions.assertEquals(4, boosted.getSize());
        float[] boostedSamples = samples(boosted);
        for (int i = 0; i < boostedSamples.length; i++) {
            Assertions.assertTrue(boostedSamples[i] <= 1.0f && boostedSamples[i] >= -1.0f,
                    "equalized sample should be clipped");
        }
    }

    @FormTest
    void supportsMidSideVoiceRemovalAndEnhancement() {
        AudioBuffer source = buffer(44100, 2, new float[]{
                0.8f, 0.8f,
                0.75f, 0.25f
        });

        AudioBuffer removed = AudioEffects.removeCenter(source);
        AudioBuffer isolated = AudioEffects.isolateCenter(source);
        AudioBuffer enhanced = AudioEffects.midSide(source, 1.5f, 0.5f);

        assertSamples(new float[]{
                0.0f, 0.0f,
                0.25f, -0.25f
        }, removed);
        assertSamples(new float[]{
                0.8f, 0.8f,
                0.5f, 0.5f
        }, isolated);
        assertSamples(new float[]{
                1.0f, 1.0f,
                0.875f, 0.625f
        }, enhanced);
    }

    @FormTest
    void validatesEffectInputs() {
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                AudioEffects.gain(null, 1.0f);
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                AudioEffects.normalize(buffer(8000, 1, new float[]{0.1f}), 2.0f);
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                AudioEffects.equalize(buffer(8000, 1, new float[]{0.1f}), 1.0f, 1.0f, 1.0f, 1000.0f, 500.0f);
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                AudioEffects.removeCenter(buffer(8000, 1, new float[]{0.1f}));
            }
        });
    }

    private AudioBuffer buffer(int sampleRate, int numChannels, float[] data) {
        AudioBuffer buffer = new AudioBuffer(data.length);
        buffer.copyFrom(sampleRate, numChannels, data);
        return buffer;
    }

    private float[] samples(AudioBuffer actual) {
        float[] actualSamples = new float[actual.getSize()];
        actual.copyTo(actualSamples);
        return actualSamples;
    }

    private void assertSamples(float[] expected, AudioBuffer actual) {
        Assertions.assertEquals(expected.length, actual.getSize());
        Assertions.assertArrayEquals(expected, samples(actual), 0.0001f);
    }

    private interface ThrowingRunnable extends Executable {
    }
}
