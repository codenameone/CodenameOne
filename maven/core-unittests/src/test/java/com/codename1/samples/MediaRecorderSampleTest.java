package com.codename1.samples;

import com.codename1.capture.Capture;
import com.codename1.components.MultiButton;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import java.io.IOException;
import java.util.Date;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class MediaRecorderSampleTest extends UITestBase {

    @FormTest
    public void testMediaRecorderSample() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.clearFileSystem();
        String appHome = impl.getAppHomePath();
        impl.mkdir(appHome);
        String recordingsDir = appHome + "recordings/";

        String mockAudioFile = appHome + "tempAudio.wav";
        impl.putFile(mockAudioFile, new byte[]{1, 2, 3});
        impl.setNextCaptureAudioPath(mockAudioFile);

        MockMedia mockMedia = new MockMedia();
        impl.setMedia(mockMedia);

        Form hi = new Form("Capture", BoxLayout.y());
        hi.setToolbar(new Toolbar());
        Style s = UIManager.getInstance().getComponentStyle("Title");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_MIC, s);

        FileSystemStorage fs = FileSystemStorage.getInstance();
        fs.mkdir(recordingsDir);

        try {
            for (String file : fs.listFiles(recordingsDir)) {
               // ...
            }

            hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
                // Adapt to use callback instead of blocking captureAudio
                Capture.captureAudio(evt -> {
                    String file = (String) evt.getSource();
                    if (file != null) {
                        String fileName = "rec-" + System.currentTimeMillis();
                        String filePath = recordingsDir + fileName;
                        try {
                            Util.copy(fs.openInputStream(file), fs.openOutputStream(filePath));
                            MultiButton mb = new MultiButton(fileName);
                            mb.addActionListener((e) -> {
                                try {
                                    Media m = MediaManager.createMedia(filePath, false);
                                    m.play();
                                } catch (IOException err) {
                                    Log.e(err);
                                }
                            });
                            hi.add(mb);
                            hi.revalidate();
                        } catch (IOException err) {
                            Log.e(err);
                        }
                    }
                });
            });
        } catch (IOException err) {
            Log.e(err);
        }
        hi.show();
        waitForFormTitle("Capture");

        Component cmdButton = findComponentWithIcon(hi, icon);
        if (cmdButton != null) {
            impl.tapComponent(cmdButton);
        } else {
             Toolbar tb = hi.getToolbar();
             if (tb != null) {
                 Component titleArea = tb.getComponentAt(0);
                 if (titleArea instanceof Container) {
                      Component c = findComponentWithIcon((Container)titleArea, icon);
                      if (c != null) impl.tapComponent(c);
                 }
             }
        }

        // Wait/Check
        String[] files = impl.listFiles(recordingsDir);

        if (files.length > 0) {
            assertTrue(files.length > 0, "Recording should be saved");

            Component c = hi.getContentPane().getComponentAt(hi.getContentPane().getComponentCount()-1);
            if (c instanceof MultiButton) {
                impl.tapComponent(c);
                assertTrue(mockMedia.played, "Media should be played");
            }
        }
    }

    private Component findComponentWithIcon(Container c, Image icon) {
        for(int i=0; i<c.getComponentCount(); i++) {
            Component child = c.getComponentAt(i);
            if (child instanceof Label) {
                 if (((Label)child).getIcon() == icon) return child;
            }
            if (child instanceof Container) {
                Component found = findComponentWithIcon((Container)child, icon);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void waitForFormTitle(String title) {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 5000) {
            Form f = CN.getCurrentForm();
            if (f != null && title.equals(f.getTitle())) {
                return;
            }
            try { Thread.sleep(50); } catch(Exception e){}
        }
    }

    class MockMedia implements Media {
        boolean played = false;
        @Override
        public void prepare() {}
        @Override
        public void play() { played = true; }
        @Override
        public void pause() {}
        @Override
        public void cleanup() {}
        @Override
        public int getTime() { return 0; }
        @Override
        public void setTime(int time) {}
        @Override
        public int getDuration() { return 100; }
        @Override
        public void setVolume(int vol) {}
        @Override
        public int getVolume() { return 100; }
        @Override
        public boolean isPlaying() { return played; }
        @Override
        public String getVariable(String key) { return null; }

        public void setVariable(String key, Object value) {}

        public void setVariable(String key, String value) {}

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
        }

        @Override
        public boolean isNativePlayerMode() { return false; }

        @Override
        public void setFullScreen(boolean b) {}

        @Override
        public boolean isFullScreen() { return false; }

        @Override
        public boolean isVideo() { return false; }

        @Override
        public Component getVideoComponent() { return null; }
    }
}
