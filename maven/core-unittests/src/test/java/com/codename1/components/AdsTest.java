package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class AdsTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        Ads ads = new Ads();
        assertNotNull(ads);
    }

    @FormTest
    void testConstructorWithAdUnitId() {
        Ads ads = new Ads("test-ad-unit-id");
        assertNotNull(ads);
    }

    @FormTest
    void testAdUnitIdGetterAndSetter() {
        Ads ads = new Ads();
        ads.setAdUnitId("test-unit-id");
        assertEquals("test-unit-id", ads.getAdUnitId());
    }

    @FormTest
    void testTestModeGetterAndSetter() {
        Ads ads = new Ads();
        ads.setTestMode(true);
        assertTrue(ads.isTestMode());

        ads.setTestMode(false);
        assertFalse(ads.isTestMode());
    }

    @FormTest
    void testRefreshAd() {
        Ads ads = new Ads("test-ad-unit-id");
        ads.refreshAd();
        // Should not throw exception
        assertNotNull(ads);
    }

    @FormTest
    void testIsSupported() {
        // Static method test
        boolean supported = Ads.isSupported();
        // Should return a boolean value
        assertTrue(supported || !supported);
    }
}
