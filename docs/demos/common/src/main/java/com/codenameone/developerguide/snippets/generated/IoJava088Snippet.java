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


class IoJava088Snippet {

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
    // tag::io-java-088[]
    public class Contact implements PropertyBusinessObject {
        public final IntProperty<Contact> id  = new IntProperty<>("id");
        public final Property<String, Contact> name = new Property<>("name");
        public final Property<String, Contact> email = new Property<>("email");
        public final Property<String, Contact> phone = new Property<>("phone");
        public final Property<Date, Contact> dateOfBirth = new Property<>("dateOfBirth", Date.class);
        public final Property<String, Contact> gender  = new Property<>("gender");
        public final IntProperty<Contact> rank  = new IntProperty<>("rank");
        public final PropertyIndex idx = new PropertyIndex(this, "Contact", id, name, email, phone, dateOfBirth, gender, rank);

        @Override
        public PropertyIndex getPropertyIndex() {
            return idx;
        }

        public Contact() {
            name.setLabel("Name");
            email.setLabel("E-Mail");
            phone.setLabel("Phone");
            dateOfBirth.setLabel("Date Of Birth");
            gender.setLabel("Gender");
            rank.setLabel("Rank");
        }
    }
    // end::io-java-088[]
}
