package com.codename1.media;

import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

class AudioMixerTest extends UITestBase {

    @FormTest
    void mixesAudioBufferTracksWithMillisecondOffsetsAndGain() {
        AudioBuffer first = buffer(1000, 2, new float[]{
                0.25f, -0.25f,
                0.25f, -0.25f
        });
        AudioBuffer second = buffer(1000, 2, new float[]{
                0.5f, 0.5f,
                -0.5f, -0.5f
        });

        AudioBuffer mixed = new AudioMixer(1000, 2)
                .addTrack(first, 0, 1.0f)
                .addTrack(second, 1, 0.5f)
                .mix();

        Assertions.assertEquals(1000, mixed.getSampleRate());
        Assertions.assertEquals(2, mixed.getNumChannels());
        assertSamples(new float[]{
                0.25f, -0.25f,
                0.5f, 0.0f,
                -0.25f, -0.25f
        }, mixed);
    }

    @FormTest
    void supportsExactFrameOffsetsAndClipsOutput() {
        AudioMixer mixer = new AudioMixer(48000, 1);
        mixer.addTrackAtFrame(new float[]{0.75f, -0.75f}, 0, 2, 2, 1.0f);
        mixer.addTrackAtFrame(new float[]{0.75f, -0.75f}, 0, 2, 2, 1.0f);

        Assertions.assertEquals(4, mixer.getOutputFrameCount());
        Assertions.assertEquals(4, mixer.getOutputSize());
        assertSamples(new float[]{0.0f, 0.0f, 1.0f, -1.0f}, mixer.mix());
    }

    @FormTest
    void copiesSourceTracksWhenAdded() {
        float[] source = new float[]{0.2f, 0.4f};
        AudioMixer mixer = new AudioMixer(8000, 1).addTrack(source, 0, 1.0f);
        source[0] = 1.0f;
        source[1] = 1.0f;

        assertSamples(new float[]{0.2f, 0.4f}, mixer.mix());
    }

    @FormTest
    void rejectsTracksThatDoNotUseMixerClock() {
        AudioMixer mixer = new AudioMixer(44100, 1);

        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                mixer.addTrack(buffer(48000, 1, new float[]{0.1f}), 0, 1.0f);
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                mixer.addTrack(buffer(44100, 2, new float[]{0.1f, 0.2f}), 0, 1.0f);
            }
        });
    }

    @FormTest
    void validatesRawPcmTracks() {
        final AudioMixer mixer = new AudioMixer(44100, 2);

        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                mixer.addTrackAtFrame(new float[]{0.1f}, 0, 1, 0, 1.0f);
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                mixer.addTrack(new float[]{0.1f, 0.2f}, -1, 1.0f);
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void execute() {
                mixer.addTrack(new float[]{0.1f, 0.2f}, 0, Float.NaN);
            }
        });
    }

    @FormTest
    void mixedBufferCanBeWrittenToWav() throws Exception {
        implementation.clearFileSystem();
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String outputPath = "mixed.wav";
        String absolutePath = new File(outputPath).getAbsolutePath();
        WAVWriter writer = new WAVWriter(new File(outputPath), 8000, 1, 16);
        try {
            AudioBuffer mixed = new AudioMixer(8000, 1)
                    .addTrack(buffer(8000, 1, new float[]{0.1f, 0.2f}), 0, 1.0f)
                    .addTrack(buffer(8000, 1, new float[]{0.3f}), 1, 1.0f)
                    .mix();
            float[] pcm = new float[mixed.getSize()];
            mixed.copyTo(pcm);
            writer.write(pcm, 0, mixed.getSize());
            writer.close();
            writer = null;

            Assertions.assertTrue(fs.exists(absolutePath));
            Assertions.assertEquals(44 + mixed.getSize() * 2, fs.getLength(absolutePath));
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception ignored) {
            }
            implementation.clearFileSystem();
        }
    }

    private AudioBuffer buffer(int sampleRate, int numChannels, float[] data) {
        AudioBuffer buffer = new AudioBuffer(data.length);
        buffer.copyFrom(sampleRate, numChannels, data);
        return buffer;
    }

    private void assertSamples(float[] expected, AudioBuffer actual) {
        Assertions.assertEquals(expected.length, actual.getSize());
        float[] actualSamples = new float[actual.getSize()];
        actual.copyTo(actualSamples);
        Assertions.assertArrayEquals(expected, actualSamples, 0.0001f);
    }

    private interface ThrowingRunnable extends Executable {
    }
}
