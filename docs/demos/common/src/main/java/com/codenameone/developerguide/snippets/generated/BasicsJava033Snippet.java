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


public class BasicsJava033Snippet {

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
    // tag::basics-java-033[]
    private static Button gridButton(String text) {
        Button button = new Button(text);
        button.setCapsText(false);
        Style style = button.getAllStyles();
        style.setBgColor(0xf4f8ff);
        style.setBgTransparency(255);
        style.setFgColor(0x0d47a1);
        style.setBorder(Border.createLineBorder(1, 0x2b5c9e));
        style.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        style.setPadding(2, 2, 2, 2);
        return button;
    }

    public static Form createForm() {
        Form hi = new Form("GridBagLayout", new BorderLayout());
        Container grid = new Container(new GridBagLayout());
        Style gridStyle = grid.getAllStyles();
        gridStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        gridStyle.setPadding(4, 4, 4, 4);
        Button button;
        GridBagConstraints c = new GridBagConstraints();
        //natural height, maximum width
        c.fill = GridBagConstraints.HORIZONTAL;
        button = gridButton("One");
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        grid.addComponent(c, button);

        button = gridButton("Two");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        grid.addComponent(c, button);

        button = gridButton("Three");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;
        grid.addComponent(c, button);

        button = gridButton("Long-Named Button 4");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 40;      //make this component tall
        c.weightx = 0.0;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 1;
        grid.addComponent(c, button);

        button = gridButton("5");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;       //reset to default
        c.weighty = 1.0;   //request any extra vertical space
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space
        c.insets = new Insets(10,0,0,0);  //top padding
        c.gridx = 1;       //aligned with button 2
        c.gridwidth = 2;   //2 columns wide
        c.gridy = 2;       //third row
        grid.addComponent(c, button);
        hi.add(BorderLayout.NORTH, grid);
        return hi;
    }
    // end::basics-java-033[]

    void snippet() throws Exception {
        createForm().show();
    }
}
