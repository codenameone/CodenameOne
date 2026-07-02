package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;


class MiscellaneousFeaturesJava025Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    void snippet() throws Exception {
        // tag::miscellaneous-features-java-025[]
        Form hi = new Form("Capture", BoxLayout.y());
        hi.setToolbar(new Toolbar());
        Style s = UIManager.getInstance().getComponentStyle("Title");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_MIC, s);

        FileSystemStorage fs = FileSystemStorage.getInstance();
        String recordingsDir = fs.getAppHomePath() + "recordings/";
        fs.mkdir(recordingsDir);
        try {
            for(String file : fs.listFiles(recordingsDir)) {
                MultiButton mb = new MultiButton(file.substring(file.lastIndexOf("/") + 1));
                mb.addActionListener((e) -> {
                    try {
                        Media m = MediaManager.createMedia(recordingsDir + file, false);
                        m.play();
                    } catch(IOException err) {
                        Log.e(err);
                    }
                });
                hi.add(mb);
            }

            hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
                try {
                    String file = Capture.captureAudio();
                    if(file != null) {
                        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MMM-dd-kk-mm");
                        String fileName =sd.format(new Date());
                        String filePath = recordingsDir + fileName;
                        Util.copy(fs.openInputStream(file), fs.openOutputStream(filePath));
                        MultiButton mb = new MultiButton(fileName);
                        mb.addActionListener((e) -> {
                            try {
                                Media m = MediaManager.createMedia(filePath, false);
                                m.play();
                            } catch(IOException err) {
                                Log.e(err);
                            }
                        });
                        hi.add(mb);
                        hi.revalidate();
                    }
                } catch(IOException err) {
                    Log.e(err);
                }
            });
        } catch(IOException err) {
            Log.e(err);
        }
        hi.show();
        // end::miscellaneous-features-java-025[]
    }
}
