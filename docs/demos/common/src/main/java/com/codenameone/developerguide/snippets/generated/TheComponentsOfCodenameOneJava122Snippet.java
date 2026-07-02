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


class TheComponentsOfCodenameOneJava122Snippet {

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
        // tag::the-components-of-codename-one-java-122[]
        Form hi = new Form("Picker", new BoxLayout(BoxLayout.Y_AXIS));
        Picker datePicker = new Picker();
        datePicker.setType(Display.PICKER_TYPE_DATE);
        Picker dateTimePicker = new Picker();
        dateTimePicker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        Picker timePicker = new Picker();
        timePicker.setType(Display.PICKER_TYPE_TIME);
        Picker stringPicker = new Picker();
        stringPicker.setType(Display.PICKER_TYPE_STRINGS);
        Picker durationPicker = new Picker();
        durationPicker.setType(Display.PICKER_TYPE_DURATION);
        Picker minuteDurationPicker = new Picker();
        minuteDurationPicker.setType(Display.PICKER_TYPE_DURATION_MINUTES);
        Picker hourDurationPicker = new Picker();
        hourDurationPicker.setType(Display.PICKER_TYPE_DURATION_HOURS);

        datePicker.setDate(new Date());
        dateTimePicker.setDate(new Date());
        timePicker.setTime(10 * 60); // 10:00AM = Minutes since midnight
        stringPicker.setStrings("A Game of Thrones", "A Clash Of Kings", "A Storm Of Swords", "A Feast For Crows",
                "A Dance With Dragons", "The Winds of Winter", "A Dream of Spring");
        stringPicker.setSelectedString("A Game of Thrones");

        hi.add(datePicker).add(dateTimePicker).add(timePicker)
          .add(stringPicker).add(durationPicker)
          .add(minuteDurationPicker).add(hourDurationPicker);
        hi.show();
        // end::the-components-of-codename-one-java-122[]
    }
}
