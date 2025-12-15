package com.codename1.payment;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class ProductTest extends UITestBase {

    @FormTest
    public void testProduct() {
        Product p = new Product();
        p.setDisplayName("Test Product");
        p.setDescription("Description");
        p.setLocalizedPrice("$1.99");
        p.setSku("sku123");

        Assertions.assertEquals("Test Product", p.getDisplayName());
        Assertions.assertEquals("Description", p.getDescription());
        Assertions.assertEquals("$1.99", p.getLocalizedPrice());
        Assertions.assertEquals("sku123", p.getSku());
    }
}
