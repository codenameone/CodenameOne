package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MediaPlaybackScreenshotTest extends BaseTest {
    private static final int SAMPLE_RATE = 44100;
    private static final double TONE_FREQUENCY = 440.0;
    private static final double TONE_DURATION_SECONDS = 1.2;

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Media Playback", new BorderLayout(), "MediaPlayback");
        String tonePath = writeToneWav();
        final Label statusLabel = new Label("Preparing media sample…");
        if (tonePath == null) {
            updateStatus(statusLabel, form, "Failed to generate tone file");
        } else {
            Media media = MediaManager.createMedia(tonePath, false);
            if (media == null) {
                updateStatus(statusLabel, form, "Media creation returned null");
            }
            media.setTime(0);
            media.play();
            statusLabel.setText("Starting playback…");
        }
        Container content = new Container(BoxLayout.y());
        content.getAllStyles().setPadding(6, 6, 6, 6);
        content.add(new Label("Media playback regression"));
        content.add(new Label("Verifies createMedia() against filesystem URI"));
        content.add(statusLabel);
        form.add(BorderLayout.CENTER, content);

        form.show();

        Cn1ssDeviceRunnerHelper.waitForMillis(800);
        return waitForDone();
    }

    private static void updateStatus(Label label, Form form, String message) {
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            label.setText(message);
            if (form != null) {
                form.revalidate();
            }
        });
    }

    private static void cleanupMedia(Media media) {
        if (media == null) {
            return;
        }
        Cn1ssDeviceRunnerHelper.runOnEdtSync(media::cleanup);
    }

    private static String writeToneWav() {
        byte[] wav = buildToneWav();
        if (wav == null || wav.length == 0) {
            return null;
        }
        String path = FileSystemStorage.getInstance().getAppHomePath() + "media-playback-test.wav";
        FileSystemStorage.getInstance().delete(path);
        OutputStream out = null;
        try {
            out = FileSystemStorage.getInstance().openOutputStream(path);
            out.write(wav);
            return path;
        } catch (IOException ex) {
            TestUtils.log("Unable to write tone wav: " + ex.getMessage());
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static byte[] buildToneWav() {
        int totalSamples = (int) (SAMPLE_RATE * TONE_DURATION_SECONDS);
        int bytesPerSample = 2;
        int dataSize = totalSamples * bytesPerSample;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);

        writeAscii(out, "RIFF");
        writeIntLE(out, 36 + dataSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLE(out, 16); // PCM chunk size
        writeShortLE(out, 1); // audio format (PCM)
        writeShortLE(out, 1); // channels
        writeIntLE(out, SAMPLE_RATE);
        writeIntLE(out, SAMPLE_RATE * bytesPerSample);
        writeShortLE(out, (short) bytesPerSample);
        writeShortLE(out, (short) (bytesPerSample * 8));
        writeAscii(out, "data");
        writeIntLE(out, dataSize);

        for (int i = 0; i < totalSamples; i++) {
            double angle = 2.0 * Math.PI * TONE_FREQUENCY * i / SAMPLE_RATE;
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE * 0.3);
            writeShortLE(out, sample);
        }
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) {
        if (text == null) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            out.write((byte) (text.charAt(i) & 0x7f));
        }
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    private static void writeShortLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }
}
