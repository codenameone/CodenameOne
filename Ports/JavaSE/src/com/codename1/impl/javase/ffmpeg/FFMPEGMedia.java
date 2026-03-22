package com.codename1.impl.javase.ffmpeg;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AsyncMedia;
import com.codename1.media.Media;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import com.codename1.util.AsyncResource;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class FFMPEGMedia {
    private FFMPEGMedia() {
    }

    public static boolean isConfigured() {
        return resolveExecutable("ffmpeg") != null && resolveExecutable("ffprobe") != null;
    }

    public static AsyncResource<Media> createMediaAsync(JavaSEPort port, final String uriAddress, final boolean isVideo, final Runnable onCompletion) {
        AsyncResource<Media> out = new AsyncResource<Media>();
        try {
            out.complete(new FFMPEGMediaPlayer(port, uriAddress, isVideo, onCompletion));
        } catch (Exception ex) {
            out.error(ex);
        }
        return out;
    }

    public static AsyncResource<Media> createMediaAsync(JavaSEPort port, final InputStream stream, final String mimeType, final Runnable onCompletion) {
        AsyncResource<Media> out = new AsyncResource<Media>();
        File tempFile = null;
        try {
            String suffix = ".tmp";
            if (mimeType != null && !mimeType.isEmpty()) {
                suffix = port.guessSuffixForMimetype(mimeType);
                if (suffix == null || suffix.isEmpty()) {
                    suffix = ".tmp";
                }
            }
            tempFile = File.createTempFile("cn1-ffmpeg-media", suffix);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                Util.copy(stream, fos);
            }
            FFMPEGMediaPlayer player = new FFMPEGMediaPlayer(port, tempFile.getAbsolutePath(), mimeType != null && mimeType.startsWith("video/"), onCompletion);
            player.setTempFile(tempFile);
            out.complete(player);
        } catch (Exception ex) {
            if (tempFile != null) {
                tempFile.delete();
            }
            out.error(ex);
        }
        return out;
    }

    private static File resolveExecutable(String name) {
        String dir = System.getProperty("ffmpeg.dir", "");
        String executable = isWindows() ? name + ".exe" : name;
        if (!dir.isEmpty()) {
            File candidate = new File(dir, executable);
            if (candidate.exists()) {
                return candidate;
            }
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String entry : path.split(File.pathSeparator)) {
                File candidate = new File(entry, executable);
                if (candidate.exists() && candidate.canExecute()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static class FFMPEGMediaPlayer extends AbstractMedia {
        private final JavaSEPort port;
        private final String source;
        private final boolean videoRequested;
        private final Runnable onCompletion;
        private final Runnable deinitializeCallback = new Runnable() {
            @Override
            public void run() {
                cleanup();
            }
        };
        private final ProbeData probeData;
        private final VideoPanel videoPanel;
        private final JFrame frame;
        private File tempFile;
        private JavaSEPort.Peer videoComponent;
        private Process audioProcess;
        private Process videoProcess;
        private Thread audioThread;
        private Thread videoThread;
        private SourceDataLine audioLine;
        private volatile boolean playing;
        private volatile boolean stopRequested;
        private volatile boolean completing;
        private volatile long positionMillis;
        private volatile long playStartMillis;
        private volatile int volume = 100;
        private boolean nativePlayerMode;

        FFMPEGMediaPlayer(JavaSEPort port, String source, boolean isVideo, Runnable onCompletion) throws Exception {
            this.port = port;
            this.source = normalizeSource(source);
            this.videoRequested = isVideo;
            this.onCompletion = onCompletion;
            this.frame = findFrame(port);
            this.probeData = probe(this.source);
            this.videoPanel = probeData.hasVideo ? new VideoPanel(probeData.width, probeData.height) : null;
            port.addDeinitializeHook(deinitializeCallback);
        }

        void setTempFile(File tempFile) {
            this.tempFile = tempFile;
        }

        @Override
        protected synchronized void playImpl() {
            if (playing) {
                return;
            }
            stopRequested = false;
            completing = false;
            try {
                startVideoProcess();
                startAudioProcess();
                playStartMillis = System.currentTimeMillis() - positionMillis;
                playing = true;
                fireMediaStateChange(AsyncMedia.State.Playing);
                if (audioThread == null && videoThread == null) {
                    completePlayback();
                }
            } catch (IOException ex) {
                fireMediaError(new AsyncMedia.MediaException(AsyncMedia.MediaErrorType.Unknown, ex));
            }
        }

        @Override
        protected synchronized void pauseImpl() {
            if (!playing) {
                return;
            }
            stopRequested = true;
            positionMillis = Math.min(getDuration(), Math.max(0, System.currentTimeMillis() - playStartMillis));
            if (getDuration() < 0) {
                positionMillis = Math.max(0, System.currentTimeMillis() - playStartMillis);
            }
            destroyProcesses();
            playing = false;
            fireMediaStateChange(AsyncMedia.State.Paused);
        }

        @Override
        public void prepare() {
        }

        @Override
        public void cleanup() {
            stopRequested = true;
            destroyProcesses();
            port.removeDeinitializeHook(deinitializeCallback);
            if (tempFile != null) {
                tempFile.delete();
                tempFile = null;
            }
        }

        @Override
        public int getTime() {
            if (playing) {
                int duration = getDuration();
                long elapsed = Math.max(0, System.currentTimeMillis() - playStartMillis);
                return duration > 0 ? (int)Math.min(duration, elapsed) : (int)elapsed;
            }
            return (int)positionMillis;
        }

        @Override
        public void setTime(int time) {
            positionMillis = Math.max(0, time);
            if (playing) {
                stopRequested = true;
                destroyProcesses();
                playing = false;
                playImpl();
            }
        }

        @Override
        public int getDuration() {
            return probeData.durationMillis;
        }

        @Override
        public void setVolume(int vol) {
            volume = Math.max(0, Math.min(100, vol));
            if (audioLine != null && audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl control = (FloatControl)audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                float min = control.getMinimum();
                float max = control.getMaximum();
                float value = min + ((max - min) * (volume / 100f));
                control.setValue(value);
            }
        }

        @Override
        public int getVolume() {
            return volume;
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public Component getVideoComponent() {
            if (!isVideo()) {
                return new Label();
            }
            if (videoComponent != null) {
                return videoComponent;
            }
            final VideoPanel panel = videoPanel;
            if (panel == null) {
                return new Label("Video");
            }
            if (SwingUtilities.isEventDispatchThread()) {
                videoComponent = new JavaSEPort.Peer(frame, panel);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        videoComponent = new JavaSEPort.Peer(frame, panel);
                    }
                });
                CN.invokeAndBlock(new Runnable() {
                    @Override
                    public void run() {
                        while (videoComponent == null) {
                            try {
                                Thread.sleep(25);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                });
            }
            return videoComponent;
        }

        @Override
        public boolean isVideo() {
            return videoRequested && probeData.hasVideo;
        }

        @Override
        public boolean isFullScreen() {
            return false;
        }

        @Override
        public void setFullScreen(boolean fullScreen) {
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
            nativePlayerMode = nativePlayer;
        }

        @Override
        public boolean isNativePlayerMode() {
            return nativePlayerMode;
        }

        @Override
        public void setVariable(String key, Object value) {
        }

        @Override
        public Object getVariable(String key) {
            if ("ffmpeg.videoFrameRendered".equals(key)) {
                return Boolean.valueOf(videoPanel != null && videoPanel.hasFrame());
            }
            if ("ffmpeg.videoFrameCount".equals(key)) {
                return Integer.valueOf(videoPanel != null ? videoPanel.getFrameCount() : 0);
            }
            if ("ffmpeg.videoAverageColor".equals(key)) {
                return videoPanel != null ? videoPanel.getAverageColor() : "";
            }
            return null;
        }

        private void startVideoProcess() throws IOException {
            if (!isVideo()) {
                videoThread = null;
                return;
            }
            List<String> command = new ArrayList<String>();
            command.add(resolveExecutable("ffmpeg").getAbsolutePath());
            command.add("-loglevel");
            command.add("error");
            if (positionMillis > 0) {
                command.add("-ss");
                command.add(String.valueOf(positionMillis / 1000.0));
            }
            command.add("-i");
            command.add(source);
            command.add("-f");
            command.add("rawvideo");
            command.add("-pix_fmt");
            command.add("bgra");
            command.add("-an");
            command.add("-vcodec");
            command.add("rawvideo");
            command.add("-");
            videoProcess = new ProcessBuilder(command).start();
            final InputStream input = videoProcess.getInputStream();
            final int frameSize = probeData.width * probeData.height * 4;
            final long frameDelay = Math.max(1L, Math.round(1000d / probeData.frameRate));
            videoThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] frame = new byte[frameSize];
                    try {
                        while (!stopRequested && readFully(input, frame)) {
                            videoPanel.updateFrame(frame, probeData.width, probeData.height);
                            Thread.sleep(frameDelay);
                        }
                    } catch (Exception ex) {
                        if (!stopRequested) {
                            Log.e(ex);
                        }
                    } finally {
                        closeQuietly(input);
                        videoFinished();
                    }
                }
            }, "cn1-ffmpeg-video");
            videoThread.setDaemon(true);
            videoThread.start();
        }

        private void startAudioProcess() throws IOException {
            if (!probeData.hasAudio) {
                audioThread = null;
                return;
            }
            List<String> command = new ArrayList<String>();
            command.add(resolveExecutable("ffmpeg").getAbsolutePath());
            command.add("-loglevel");
            command.add("error");
            if (positionMillis > 0) {
                command.add("-ss");
                command.add(String.valueOf(positionMillis / 1000.0));
            }
            command.add("-i");
            command.add(source);
            command.add("-f");
            command.add("s16le");
            command.add("-acodec");
            command.add("pcm_s16le");
            command.add("-ac");
            command.add("2");
            command.add("-ar");
            command.add("44100");
            command.add("-vn");
            command.add("-");
            audioProcess = new ProcessBuilder(command).start();
            final InputStream input = audioProcess.getInputStream();
            audioThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[8192];
                    try {
                        AudioFormat format = new AudioFormat(44100f, 16, 2, true, false);
                        audioLine = AudioSystem.getSourceDataLine(format);
                        audioLine.open(format);
                        setVolume(volume);
                        audioLine.start();
                        int len;
                        while (!stopRequested && (len = input.read(buffer)) > 0) {
                            audioLine.write(buffer, 0, len);
                        }
                    } catch (Exception ex) {
                        if (!stopRequested) {
                            Log.e(ex);
                        }
                    } finally {
                        if (audioLine != null) {
                            try {
                                audioLine.drain();
                            } catch (Exception ignored) {
                            }
                            try {
                                audioLine.stop();
                            } catch (Exception ignored) {
                            }
                            try {
                                audioLine.close();
                            } catch (Exception ignored) {
                            }
                            audioLine = null;
                        }
                        closeQuietly(input);
                        audioFinished();
                    }
                }
            }, "cn1-ffmpeg-audio");
            audioThread.setDaemon(true);
            audioThread.start();
        }

        private synchronized void audioFinished() {
            audioThread = null;
            maybeFinish();
        }

        private synchronized void videoFinished() {
            videoThread = null;
            maybeFinish();
        }

        private synchronized void maybeFinish() {
            if (audioThread != null || videoThread != null || stopRequested || completing) {
                return;
            }
            completePlayback();
        }

        private synchronized void completePlayback() {
            completing = true;
            positionMillis = getDuration();
            playing = false;
            destroyProcesses();
            fireMediaStateChange(AsyncMedia.State.Paused);
            if (onCompletion != null) {
                onCompletion.run();
            }
        }

        private void destroyProcesses() {
            destroyProcess(videoProcess);
            destroyProcess(audioProcess);
            videoProcess = null;
            audioProcess = null;
        }

        private void destroyProcess(Process process) {
            if (process == null) {
                return;
            }
            try {
                process.destroy();
            } catch (Exception ignored) {
            }
        }

        private static ProbeData probe(String source) throws Exception {
            List<String> command = new ArrayList<String>();
            command.add(resolveExecutable("ffprobe").getAbsolutePath());
            command.add("-v");
            command.add("error");
            command.add("-print_format");
            command.add("json");
            command.add("-show_streams");
            command.add("-show_format");
            command.add(source);
            Process process = new ProcessBuilder(command).start();
            String json;
            try (InputStream input = process.getInputStream()) {
                json = readToString(input);
            }
            try (InputStream errors = process.getErrorStream()) {
                String stderr = readToString(errors);
                if (!stderr.trim().isEmpty()) {
                    Log.p(stderr);
                }
            }
            process.waitFor();
            JSONParser parser = new JSONParser();
            Map parsed = parser.parseJSON(new StringReader(json));
            List streams = (List)parsed.get("streams");
            Map format = (Map)parsed.get("format");
            ProbeData data = new ProbeData();
            if (format != null) {
                Object duration = format.get("duration");
                if (duration != null) {
                    data.durationMillis = (int)Math.round(Double.parseDouble(duration.toString()) * 1000d);
                }
            }
            if (streams != null) {
                for (Object entry : streams) {
                    Map stream = (Map)entry;
                    String codecType = String.valueOf(stream.get("codec_type"));
                    if ("video".equals(codecType) && !data.hasVideo) {
                        data.hasVideo = true;
                        data.width = parseInt(stream.get("width"), 640);
                        data.height = parseInt(stream.get("height"), 480);
                        String frameRate = String.valueOf(stream.get("avg_frame_rate"));
                        data.frameRate = parseFrameRate(frameRate);
                    } else if ("audio".equals(codecType)) {
                        data.hasAudio = true;
                    }
                }
            }
            if (data.frameRate <= 0d) {
                data.frameRate = 30d;
            }
            return data;
        }

        private static int parseInt(Object value, int fallback) {
            if (value == null) {
                return fallback;
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }

        private static double parseFrameRate(String frameRate) {
            if (frameRate == null || frameRate.isEmpty() || "0/0".equals(frameRate)) {
                return 30d;
            }
            if (frameRate.indexOf('/') > 0) {
                String[] parts = frameRate.split("/");
                if (parts.length == 2) {
                    try {
                        double numerator = Double.parseDouble(parts[0]);
                        double denominator = Double.parseDouble(parts[1]);
                        if (denominator != 0d) {
                            return numerator / denominator;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            try {
                return Double.parseDouble(frameRate);
            } catch (NumberFormatException ex) {
                return 30d;
            }
        }

        private static JFrame findFrame(JavaSEPort port) {
            java.awt.Container cnt = port.getCanvas() == null ? null : port.getCanvas().getParent();
            while (cnt != null && !(cnt instanceof JFrame)) {
                cnt = cnt.getParent();
            }
            return (JFrame)cnt;
        }

        private static String normalizeSource(String source) {
            if (source != null && source.startsWith("file:")) {
                try {
                    return new File(URI.create(source)).getAbsolutePath();
                } catch (Exception ex) {
                    return source.substring("file:".length());
                }
            }
            return source;
        }

        private static boolean readFully(InputStream input, byte[] buffer) throws IOException {
            int offset = 0;
            while (offset < buffer.length) {
                int len = input.read(buffer, offset, buffer.length - offset);
                if (len < 0) {
                    return false;
                }
                offset += len;
            }
            return true;
        }

        private static String readToString(InputStream input) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Util.copy(input, out);
            return new String(out.toByteArray(), "UTF-8");
        }

        private static void closeQuietly(InputStream input) {
            try {
                input.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static class ProbeData {
        private boolean hasVideo;
        private boolean hasAudio;
        private int width = 640;
        private int height = 480;
        private double frameRate = 30d;
        private int durationMillis = -1;
    }

    private static class VideoPanel extends JPanel implements HierarchyListener {
        private volatile BufferedImage image;
        private volatile int frameCount;

        VideoPanel(int width, int height) {
            setBackground(Color.BLACK);
            setOpaque(true);
            setSize(width, height);
            addHierarchyListener(this);
        }

        void updateFrame(byte[] bytes, int width, int height) {
            BufferedImage next = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] argb = new int[width * height];
            int offset = 0;
            for (int i = 0; i < argb.length; i++) {
                int b = bytes[offset] & 0xff;
                int g = bytes[offset + 1] & 0xff;
                int r = bytes[offset + 2] & 0xff;
                int a = bytes[offset + 3] & 0xff;
                argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
                offset += 4;
            }
            next.setRGB(0, 0, width, height, argb, 0, width);
            image = next;
            frameCount++;
            repaint();
        }

        boolean hasFrame() {
            return image != null;
        }

        int getFrameCount() {
            return frameCount;
        }

        String getAverageColor() {
            BufferedImage current = image;
            if (current == null) {
                return "";
            }
            long r = 0;
            long g = 0;
            long b = 0;
            int width = current.getWidth();
            int height = current.getHeight();
            int count = width * height;
            if (count == 0) {
                return "";
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = current.getRGB(x, y);
                    r += (rgb >> 16) & 0xff;
                    g += (rgb >> 8) & 0xff;
                    b += rgb & 0xff;
                }
            }
            return (r / count) + "," + (g / count) + "," + (b / count);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            BufferedImage current = image;
            if (current == null) {
                g2.dispose();
                return;
            }
            g2.drawImage(current, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            repaint();
        }
    }
}
