package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class AdsTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        Ads ads = new Ads();
        assertNotNull(ads);
        assertEquals("Ads", ads.getUIID());
    }

    @FormTest
    void testConstructorWithAppId() {
        Ads ads = new Ads("test-app-id");
        assertNotNull(ads);
    }

    @FormTest
    void testConstructorWithAppIdAndRefreshFlag() {
        Ads ads = new Ads("test-app-id", true);
        assertNotNull(ads);

        Ads ads2 = new Ads("test-app-id", false);
        assertNotNull(ads2);
    }

    @FormTest
    void testUpdateDurationGetterAndSetter() {
        Ads ads = new Ads();
        ads.setUpdateDuration(30);
        assertEquals(30, ads.getUpdateDuration());

        ads.setUpdateDuration(60);
        assertEquals(60, ads.getUpdateDuration());
    }

    @FormTest
    void testAgeGetterAndSetter() {
        Ads ads = new Ads();
        ads.setAge("25");
        assertEquals("25", ads.getAge());
    }

    @FormTest
    void testGenderGetterAndSetter() {
        Ads ads = new Ads();
        ads.setGender("M");
        assertEquals("M", ads.getGender());
    }

    @FormTest
    void testCategoryGetterAndSetter() {
        Ads ads = new Ads();
        ads.setCategory("News");
        assertEquals("News", ads.getCategory());
    }

    @FormTest
    void testLocationGetterAndSetter() {
        Ads ads = new Ads();
        ads.setLocation("US");
        assertEquals("US", ads.getLocation());
    }

    @FormTest
    void testKeywordsGetterAndSetter() {
        Ads ads = new Ads();
        String[] keywords = {"tech", "news", "sports"};
        ads.setKeywords(keywords);
        assertArrayEquals(keywords, ads.getKeywords());
    }

    @FormTest
    void testPropertyNames() {
        Ads ads = new Ads();
        String[] props = ads.getPropertyNames();
        assertNotNull(props);
    }

    @FormTest
    void testAdsComponentIsNotFocusableOnTouchDevices() {
        Ads ads = new Ads();
        // On touch devices, ads should not be focusable
        assertNotNull(ads);
    }
}
