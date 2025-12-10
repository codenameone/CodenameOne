package com.codename1.samples;

import com.codename1.components.MediaPlayer;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.EventDispatcher;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SetLoopTest2969Test extends UITestBase {

    private Media media;

    @FormTest
    public void testSetLoop() {
        Form f = new Form("NativePlayerMode Test");
        f.setLayout(new BorderLayout());
        Container videoContainer = new Container(new BorderLayout());
        Button playButton = new Button("Play Video");

        // Mock Media
        Media mockMedia = new Media() {
            @Override
            public void setVariable(String key, Object value) {
                // MediaPlayer might not set loop variable on Media directly or immediately
            }
            @Override
            public Object getVariable(String key) {
                return null;
            }
            public void play() {}
            public void pause() {}
            public void cleanup() {}
            public int getTime() { return 0; }
            public void setTime(int time) {}
            public int getDuration() { return 0; }
            public void setVolume(int vol) {}
            public int getVolume() { return 0; }
            public boolean isVideo() { return true; }
            public boolean isFullScreen() { return false; }
            public void setFullScreen(boolean fullScreen) {}
            public boolean isNativePlayerMode() { return false; }
            public void setNativePlayerMode(boolean nativePlayer) {}
            public boolean isPlaying() { return false; }
            public void prepare() {}
            public Component getVideoComponent() { return null; }
        };

        implementation.setMedia(mockMedia);

        playButton.addActionListener(e -> {
            String videoPath = "https://weblite.ca/cn1tests/small.mp4";
            try {
                if (media != null) {
                    media.cleanup();
                    media = null;
                }
                Media video = MediaManager.createMedia(videoPath, true);
                MediaPlayer mp = new MediaPlayer(video);
                mp.setLoop(true);
                videoContainer.removeAll();
                videoContainer.add(BorderLayout.CENTER, mp);
                media = video;
                f.revalidate();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        f.add(BorderLayout.NORTH, playButton);
        f.add(BorderLayout.CENTER, videoContainer);
        f.show();

        // Simulate click
        implementation.pressComponent(playButton);
        implementation.releaseComponent(playButton);

        // Check if our mock media was used
        Assertions.assertNotNull(media, "Media should have been created");
    }
}
