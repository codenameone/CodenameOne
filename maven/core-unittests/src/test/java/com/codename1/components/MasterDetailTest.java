package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Label;

import static org.junit.jupiter.api.Assertions.*;

class MasterDetailTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        MasterDetail md = new MasterDetail();
        assertNotNull(md);
    }

    @FormTest
    void testSetMasterAndDetailForms() {
        MasterDetail md = new MasterDetail();
        Container master = new Container();
        master.add(new Label("Master"));
        Container detail = new Container();
        detail.add(new Label("Detail"));

        md.setMaster(master, null);
        md.setDetail(detail, null);

        assertNotNull(md);
    }

    @FormTest
    void testPortraitModeGetterAndSetter() {
        MasterDetail md = new MasterDetail();
        md.setPortraitMode(true);
        assertTrue(md.isPortraitMode());

        md.setPortraitMode(false);
        assertFalse(md.isPortraitMode());
    }

    @FormTest
    void testLandscapeModeGetterAndSetter() {
        MasterDetail md = new MasterDetail();
        md.setLandscapeMode(true);
        assertTrue(md.isLandscapeMode());

        md.setLandscapeMode(false);
        assertFalse(md.isLandscapeMode());
    }

    @FormTest
    void testMasterContainerReturnsContainer() {
        MasterDetail md = new MasterDetail();
        Container master = new Container();
        md.setMaster(master, null);

        assertNotNull(md.getMaster());
    }

    @FormTest
    void testDetailContainerReturnsContainer() {
        MasterDetail md = new MasterDetail();
        Container detail = new Container();
        md.setDetail(detail, null);

        assertNotNull(md.getDetail());
    }
}
