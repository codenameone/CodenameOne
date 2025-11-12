package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class MasterDetailTest extends UITestBase {

    @FormTest
    void testBindTabletLandscapeMasterWithNullIcon() {
        Form rootForm = new Form("Root", new BorderLayout());
        Container parentContainer = new Container(new BorderLayout());
        Component landscapeUI = new Label("Landscape");
        Component portraitUI = new Label("Portrait");

        MasterDetail.bindTabletLandscapeMaster(
            rootForm,
            parentContainer,
            landscapeUI,
            portraitUI,
            "Master",
            null
        );

        // Verify landscape UI is added
        assertTrue(parentContainer.contains(landscapeUI));
    }

    @FormTest
    void testBindTabletLandscapeMasterWithIcon() {
        Form rootForm = new Form("Root", new BorderLayout());
        Container parentContainer = new Container(new BorderLayout());
        Component landscapeUI = new Label("Landscape");
        Component portraitUI = new Label("Portrait");
        Image icon = Image.createImage(20, 20, 0xFF0000);

        MasterDetail.bindTabletLandscapeMaster(
            rootForm,
            parentContainer,
            landscapeUI,
            portraitUI,
            "Master",
            icon
        );

        // Verify landscape UI is added
        assertTrue(parentContainer.contains(landscapeUI));
    }

    @FormTest
    void testLandscapeUIIsHiddenInPortrait() {
        Form rootForm = new Form("Root", new BorderLayout());
        rootForm.show();

        Container parentContainer = new Container(new BorderLayout());
        rootForm.add(BorderLayout.CENTER, parentContainer);

        Component landscapeUI = new Label("Landscape");
        Component portraitUI = new Label("Portrait");

        MasterDetail.bindTabletLandscapeMaster(
            rootForm,
            parentContainer,
            landscapeUI,
            portraitUI,
            "Master",
            null
        );

        // Landscape UI should have hideInPortrait set
        assertTrue(landscapeUI.isHideInPortrait());
    }
}
