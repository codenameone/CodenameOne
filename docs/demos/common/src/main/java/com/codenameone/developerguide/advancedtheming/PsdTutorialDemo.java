package com.codenameone.developerguide.advancedtheming;

import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.Resources;
import com.codename1.ui.FontImage;
import com.codename1.ui.table.TableLayout;

/**
 * Code sample from the PSD to theme conversion tutorial.
 */
public class PsdTutorialDemo {
    private Form current;
    private final Resources theme;

    public PsdTutorialDemo(Resources theme) {
        this.theme = theme;
    }

    // tag::createSeparator[]
    private Label createSeparator() {
        Label sep = new Label();
        sep.setUIID("Separator");
        // the separator line  is implemented in the theme using padding and background color, by default labels
        // are hidden when they have no content, this method disables that behavior
        sep.setShowEvenIfBlank(true);
        return sep;
    }
    // end::createSeparator[]

    public void start() {
        // tag::psdStart[]
        if (current != null) {
            current.show();
            return;
        }
        // The toolbar uses the layered mode so it resides on top of the background image, the theme makes
        // it transparent so we will see the image below it, we use border layout to place the background image on
        // top and the "Get started" button in the south
        Form psdTutorial = new Form("Signup", new BorderLayout());
        Toolbar tb = new Toolbar(true);
        psdTutorial.setToolbar(tb);

        // we create 4mm material arrow images for the back button and the Get started button
        Style iconStyle = psdTutorial.getUIManager().getComponentStyle("Title");
        FontImage leftArrow = FontImage.createMaterial(FontImage.MATERIAL_ARROW_BACK, iconStyle, 4);
        FontImage rightArrow = FontImage.createMaterial(FontImage.MATERIAL_ARROW_FORWARD, iconStyle, 4);

        // we place the back and done commands in the toolbar, we need to change UIID of the "Done" command
        // so we can color it in Red
        tb.addCommandToLeftBar("", leftArrow, (e) -> Log.p("Back pressed"));
        Command doneCommand = tb.addCommandToRightBar("Done", null, (e) -> Log.p("Done pressed"));
        tb.findCommandComponent(doneCommand).setUIID("RedCommand");

        // The camera button is comprised of 3 pieces. A label containing the image and the transparent button
        // with the camera icon on top. This is all wrapped in the title container where the title background image
        // is placed using the theme. We chose to use a Label rather than a background using the cameraLayer so
        // the label will preserve the original size of the image without scaling it and take up the space it needs
        Button cameraButton = new Button(theme.getImage("camera.png"));
        Container cameraLayer = LayeredLayout.encloseIn(
                new Label(theme.getImage("camera-button.png")),
                cameraButton);
        cameraButton.setUIID("CameraButton");
        Container titleContainer = Container.encloseIn(
                new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER),
                cameraLayer, BorderLayout.CENTER);
        titleContainer.setUIID("TitleContainer");

        TextField firstName = new TextField("", "First Name");
        TextField lastName = new TextField("", "Last Name");
        TextField email = new TextField("", "Email Address", 20, TextField.EMAILADDR);
        TextField password = new TextField("", "Choose a Password", 20, TextField.PASSWORD);
        TextField phone = new TextField("", "Phone Number", 20, TextField.PHONENUMBER);
        Label phonePrefix = new Label("+1");
        phonePrefix.setUIID("TextField");

        // The phone and full name have vertical separators, we use two table layouts to arrange them correctly
        // so the vertical separator will be in the right place
        TableLayout fullNameLayout = new TableLayout(1, 3);
        Container fullName = new Container(fullNameLayout);
        fullName.add(fullNameLayout.createConstraint().widthPercentage(49), firstName).
                add(fullNameLayout.createConstraint().widthPercentage(1), createSeparator()).
                add(fullNameLayout.createConstraint().widthPercentage(50), lastName);
        Container fullPhone = TableLayout.encloseIn(3, phonePrefix, createSeparator(), phone);

        // The button in the south portion needs the arrow icon to be on the right side so we place the text on the left
        Button southButton = new Button("Get started", rightArrow);
        southButton.setTextPosition(Component.LEFT);
        southButton.setUIID("SouthButton");

        // we add the components and the separators the center portion contains all of the elements in a box
        // Y container which we allow to scroll. BorderLayout Containers implicitly disable scrolling
        Container by = BoxLayout.encloseY(
                fullName,
                createSeparator(),
                email,
                createSeparator(),
                password,
                createSeparator(),
                fullPhone,
                createSeparator()
        );
        by.setScrollableY(true);
        psdTutorial.add(BorderLayout.NORTH, titleContainer).
                add(BorderLayout.SOUTH, southButton).
                add(BorderLayout.CENTER, by);

        psdTutorial.show();
        // end::psdStart[]
    }
}
