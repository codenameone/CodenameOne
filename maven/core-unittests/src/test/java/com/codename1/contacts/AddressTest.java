package com.codename1.contacts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class AddressTest extends UITestBase {

    @FormTest
    public void testAddressPOJO() {
        Address addr = new Address();

        addr.setStreetAddress("123 Main St");
        Assertions.assertEquals("123 Main St", addr.getStreetAddress());

        addr.setLocality("City");
        Assertions.assertEquals("City", addr.getLocality());

        addr.setRegion("State");
        Assertions.assertEquals("State", addr.getRegion());

        addr.setPostalCode("12345");
        Assertions.assertEquals("12345", addr.getPostalCode());

        addr.setCountry("Country");
        Assertions.assertEquals("Country", addr.getCountry());
    }
}
