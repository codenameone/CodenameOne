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


class TheComponentsOfCodenameOneJava081Snippet {

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
        // tag::the-components-of-codename-one-java-081[]
        Form hi = new Form("Swipe Tabs", new LayeredLayout());
        Tabs t = new Tabs();
        t.hideTabs();

        Style s = UIManager.getInstance().getComponentStyle("Button");
        FontImage radioEmptyImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, s);
        FontImage radioFullImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, s);
        ((DefaultLookAndFeel)UIManager.getInstance().getLookAndFeel()).setRadioButtonImages(radioFullImage, radioEmptyImage, radioFullImage, radioEmptyImage);

        Container container1 = BoxLayout.encloseY(new Label("Swipe the tab to see more"),
                new Label("You can put anything here"));
        t.addTab("Tab1", container1);
        t.addTab("Tab2", new SpanLabel("Some text directly in the tab"));

        RadioButton firstTab = new RadioButton("");
        RadioButton secondTab = new RadioButton("");
        firstTab.setUIID("Container");
        secondTab.setUIID("Container");
        new ButtonGroup(firstTab, secondTab);
        firstTab.setSelected(true);
        Container tabsFlow = FlowLayout.encloseCenter(firstTab, secondTab);

        hi.add(t);
        hi.add(BorderLayout.south(tabsFlow));

        t.addSelectionListener((i1, i2) -> {
            switch(i2) {
                case 0:
                    if(!firstTab.isSelected()) {
                        firstTab.setSelected(true);
                    }
                    break;
                case 1:
                    if(!secondTab.isSelected()) {
                        secondTab.setSelected(true);
                    }
                    break;
             }
        });
        // end::the-components-of-codename-one-java-081[]
    }
}
