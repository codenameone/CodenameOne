package com.codename1.io.rest;

import com.codename1.junit.UITestBase;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class RestTest extends UITestBase {

    @FormTest
    public void testRestGet() {
        String url = "http://example.com/api/resource";
        String responseBody = "{\"key\":\"value\"}";

        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(url, 200, "OK", responseBody.getBytes());

        Response<String> response = Rest.get(url).getAsString();
        Assertions.assertEquals(200, response.getResponseCode());
        Assertions.assertEquals(responseBody, response.getResponseData());
    }

    @FormTest
    public void testRestPost() {
        String url = "http://example.com/api/create";
        String responseBody = "{\"id\":\"123\"}";

        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(url, 201, "Created", responseBody.getBytes());

        Response<Map> response = Rest.post(url).jsonContent().body("{\"data\":\"test\"}").getAsJsonMap();
        Assertions.assertEquals(201, response.getResponseCode());
        Assertions.assertNotNull(response.getResponseData());
        Assertions.assertEquals("123", response.getResponseData().get("id"));
    }
}
