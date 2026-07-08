package com.codenameone.developerguide.snippets.generated;

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
import java.util.ArrayList;
import java.util.List;

class VideoIoJava001Snippet {
    String videoPath = FileSystemStorage.getInstance().getAppHomePath() + "/source.mp4";

    void checkSupport() throws Exception {
        // tag::video-io-java-001[]
        if (!VideoIO.isSupported()) {
            // Video encoding / decoding is not available on this platform
            // (for example TV, Watch or Car), so fall back gracefully.
            return;
        }
        VideoIO io = VideoIO.getVideoIO();
        // end::video-io-java-001[]
    }

    void encodeVideo() throws Exception {
        // tag::video-io-java-002[]
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
            writer.writeFrame(frame, Math.round(i * 1000f / fps));
        }
        writer.close();
        // end::video-io-java-002[]
    }

    void decodeFrames() throws Exception {
        // tag::video-io-java-003[]
        VideoReader reader = VideoIO.getVideoIO().openReader(videoPath);
        System.out.println(reader.getWidth() + "x" + reader.getHeight()
                + " " + reader.getFrameRate() + "fps, "
                + reader.getDurationMillis() + "ms");

        List<Image> thumbnails = new ArrayList<>();

        // Frame accurate single frame (unlike Media.setTime which snaps to key frames):
        VideoFrame oneSecond = reader.frameAt(1000);
        if (oneSecond != null) {
            thumbnails.add(oneSecond.toImage());
        }

        // Resample the (possibly variable frame rate) clip to a constant 10fps stream:
        reader.readFrames(10, f -> {
            // f.getARGB() / f.toImage() give you the decoded RGBA pixels
            thumbnails.add(f.toImage());
            return thumbnails.size() < 50;   // stop after 50 frames
        });
        reader.close();
        // end::video-io-java-003[]
    }

    void decodeAudio() throws Exception {
        // tag::video-io-java-004[]
        VideoReader reader = VideoIO.getVideoIO().openReader(videoPath);
        if (reader.hasAudio()) {
            AudioBuffer pcm = reader.readAudio();
            System.out.println("Decoded " + pcm.getSize() + " PCM samples at "
                    + reader.getAudioSampleRate() + "Hz, "
                    + reader.getAudioChannels() + " channels");
        }
        reader.close();
        // end::video-io-java-004[]
    }

    void discoverCodecs() {
        // tag::video-io-java-005[]
        VideoIO io = VideoIO.getVideoIO();
        for (VideoCodec codec : io.getAvailableEncoders()) {
            System.out.println(codec.getId()
                    + " (" + codec.getName() + ")"
                    + (codec.isHardwareAccelerated() ? " [hardware]" : ""));
        }
        boolean canEncodeH264 = io.isEncoderSupported(VideoIO.CODEC_H264);
        // end::video-io-java-005[]
    }
}
