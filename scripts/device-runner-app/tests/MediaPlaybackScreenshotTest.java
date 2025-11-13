package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.system.NativeLookup;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MediaPlaybackScreenshotTest extends AbstractTest {
    private static final int SAMPLE_RATE = 44100;
    private static final double TONE_FREQUENCY = 440.0;
    private static final double TONE_DURATION_SECONDS = 1.2;

    @Override
    public boolean runTest() throws Exception {
        final Label statusLabel = new Label("Preparing media sample…");
        final Form[] formHolder = new Form[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form form = new Form("Media Playback", new BorderLayout());
            Container content = new Container(BoxLayout.y());
            content.getAllStyles().setPadding(6, 6, 6, 6);
            content.add(new Label("Android media playback regression"));
            content.add(new Label("Verifies createMedia() with a content:// URI"));
            content.add(statusLabel);
            form.add(BorderLayout.CENTER, content);
            formHolder[0] = form;
            form.show();
        });

        String tonePath = writeToneWav();
        if (tonePath == null) {
            updateStatus(statusLabel, formHolder[0], "Failed to generate tone file");
            return false;
        }

        String contentUri = toContentUri(tonePath);
        if (contentUri == null) {
            updateStatus(statusLabel, formHolder[0], "Failed to resolve content URI");
            FileSystemStorage.getInstance().delete(tonePath);
            return false;
        }

        final Media[] mediaHolder = new Media[1];
        final boolean[] playbackFailed = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            try {
                Media media = MediaManager.createMedia(contentUri, false);
                if (media == null) {
                    updateStatus(statusLabel, formHolder[0], "Media creation returned null");
                    playbackFailed[0] = true;
                    return;
                }
                media.setTime(0);
                media.play();
                statusLabel.setText("Starting playback…");
                formHolder[0].revalidate();
                mediaHolder[0] = media;
            } catch (IOException ex) {
                TestUtils.log("Unable to create media: " + ex.getMessage());
                updateStatus(statusLabel, formHolder[0], "Unable to create media");
                playbackFailed[0] = true;
            }
        });

        if (playbackFailed[0]) {
            cleanupMedia(mediaHolder[0]);
            FileSystemStorage.getInstance().delete(tonePath);
            return false;
        }

        final boolean[] playbackStarted = new boolean[1];
        for (int elapsed = 0; elapsed < 5000 && !playbackStarted[0]; elapsed += 200) {
            Cn1ssDeviceRunnerHelper.waitForMillis(200);
            Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
                if (mediaHolder[0] != null && mediaHolder[0].isPlaying()) {
                    playbackStarted[0] = true;
                }
            });
        }

        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            if (playbackStarted[0]) {
                statusLabel.setText("Media playback started successfully");
            } else {
                statusLabel.setText("Media playback did not start");
            }
            formHolder[0].revalidate();
        });

        Cn1ssDeviceRunnerHelper.waitForMillis(800);
        boolean screenshotSuccess = Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("MediaPlayback");

        cleanupMedia(mediaHolder[0]);
        FileSystemStorage.getInstance().delete(tonePath);

        return playbackStarted[0] && screenshotSuccess;
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

    private static String toContentUri(String filePath) {
        if (filePath == null) {
            return null;
        }
        FileSystemStorage storage = FileSystemStorage.getInstance();
        if (!storage.exists(filePath)) {
            return null;
        }
        Display display = Display.getInstance();
        String platform = display != null ? display.getPlatformName() : null;
        if (platform == null || !platform.startsWith("and")) {
            // Only Android exposes content URIs through the FileProvider.
            return filePath;
        }
        MediaPlaybackNative nativeBridge = NativeLookup.create(MediaPlaybackNative.class);
        if (nativeBridge == null || !nativeBridge.isSupported()) {
            return filePath;
        }
        String resolved = nativeBridge.resolveContentUri(filePath);
        return resolved != null && resolved.length() > 0 ? resolved : filePath;
    }

}
