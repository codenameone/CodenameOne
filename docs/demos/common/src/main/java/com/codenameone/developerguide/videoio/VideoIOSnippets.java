package com.codenameone.developerguide.videoio;

import com.codename1.io.FileSystemStorage;
import com.codename1.media.AudioBuffer;
import com.codename1.media.VideoCodec;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Snippets that accompany the "Video encoding & decoding (VideoIO)" chapter of the
 * developer guide. They exercise the public {@code com.codename1.media.VideoIO} API.
 */
public class VideoIOSnippets {

    public boolean checkSupport() {
        // tag::support[]
        if (!VideoIO.isSupported()) {
            // Video encoding / decoding is not available on this platform
            // (for example TV, Watch or Car), so fall back gracefully.
            return false;
        }
        VideoIO io = VideoIO.getVideoIO();
        // end::support[]
        return io != null;
    }

    public void listCodecs() {
        // tag::codecs[]
        VideoIO io = VideoIO.getVideoIO();
        for (VideoCodec codec : io.getAvailableEncoders()) {
            System.out.println(codec.getId()
                    + " (" + codec.getName() + ")"
                    + (codec.isHardwareAccelerated() ? " [hardware]" : ""));
        }
        boolean canEncodeH264 = io.isEncoderSupported(VideoIO.CODEC_H264);
        // end::codecs[]
        System.out.println("H.264 encoding supported: " + canEncodeH264);
    }

    public String encode() throws IOException {
        // tag::encode[]
        String out = FileSystemStorage.getInstance().getAppHomePath() + "/generated.mp4";
        int w = 640, h = 480;
        float fps = 30;

        VideoWriter writer = new VideoWriterBuilder()
                .path(out)
                .width(w).height(h).frameRate(fps)
                .videoCodec(VideoIO.CODEC_H264).videoBitRate(4_000_000)
                .build();

        // Each frame is just an Image you fully control: draw whatever you like.
        for (int i = 0; i < 90; i++) {                 // 3 seconds at 30fps
            Image frame = Image.createImage(w, h, 0xff000000);
            Graphics g = frame.getGraphics();
            g.setColor(0xffffff);
            g.fillRect((i * 8) % w, h / 2 - 20, 60, 40);
            long presentationTimeMillis = Math.round(i * 1000f / fps);
            writer.writeFrame(frame, presentationTimeMillis);
        }
        writer.close();
        // end::encode[]
        return out;
    }

    public List<Image> decodeEveryFrame(String videoPath) throws IOException {
        // tag::decode[]
        VideoReader reader = VideoIO.getVideoIO().openReader(videoPath);
        System.out.println(reader.getWidth() + "x" + reader.getHeight()
                + " " + reader.getFrameRate() + "fps, "
                + reader.getDurationMillis() + "ms");

        final List<Image> thumbnails = new ArrayList<Image>();

        // Frame accurate single frame (unlike Media.setTime which snaps to keyframes):
        VideoFrame oneSecond = reader.frameAt(1000);
        if (oneSecond != null) {
            thumbnails.add(oneSecond.toImage());
        }

        // Resample the (possibly variable frame rate) clip to a constant 10fps stream:
        reader.readFrames(10, new VideoReader.FrameCallback() {
            @Override
            public boolean frame(VideoFrame f) {
                // f.getARGB() / f.toImage() give you the decoded RGBA pixels
                thumbnails.add(f.toImage());
                return thumbnails.size() < 50;   // stop after 50 frames
            }
        });
        reader.close();
        // end::decode[]
        return thumbnails;
    }

    public int decodeAudio(String videoPath) throws IOException {
        // tag::audio[]
        VideoReader reader = VideoIO.getVideoIO().openReader(videoPath);
        int samples = 0;
        if (reader.hasAudio()) {
            AudioBuffer pcm = reader.readAudio();
            samples = pcm.getSize();
            System.out.println("Decoded " + samples + " PCM samples at "
                    + reader.getAudioSampleRate() + "Hz, "
                    + reader.getAudioChannels() + " channels");
        }
        reader.close();
        // end::audio[]
        return samples;
    }
}
