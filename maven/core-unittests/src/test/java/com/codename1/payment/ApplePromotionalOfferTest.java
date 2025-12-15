package com.codename1.payment;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class ApplePromotionalOfferTest extends UITestBase {

    @FormTest
    public void testApplePromotionalOfferPOJO() {
        ApplePromotionalOffer offer = new ApplePromotionalOffer();

        offer.setOfferIdentifier("offer1");
        Assertions.assertEquals("offer1", offer.getOfferIdentifier());

        offer.setKeyIdentifier("key1");
        Assertions.assertEquals("key1", offer.getKeyIdentifier());

        offer.setNonce("nonce1");
        Assertions.assertEquals("nonce1", offer.getNonce());

        offer.setSignature("sig1");
        Assertions.assertEquals("sig1", offer.getSignature());

        long ts = System.currentTimeMillis();
        offer.setTimestamp(ts);
        Assertions.assertEquals(ts, offer.getTimestamp());
    }
}
