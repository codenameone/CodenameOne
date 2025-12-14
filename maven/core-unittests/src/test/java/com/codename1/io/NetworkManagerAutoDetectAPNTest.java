package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;

public class NetworkManagerAutoDetectAPNTest extends UITestBase {

    @FormTest
    public void testAutoDetectAPN() throws IOException {
        NetworkManager nm = NetworkManager.getInstance();
        NetworkManager.AutoDetectAPN request = nm.new AutoDetectAPN();

        // Test readResponse
        String response = "hi";
        ByteArrayInputStream is = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        request.readResponse(is);

        // Test readResponse with different value triggering retry
        String badResponse = "bad";
        ByteArrayInputStream isBad = new ByteArrayInputStream(badResponse.getBytes(StandardCharsets.UTF_8));

        // We can check if it throws or not.
        request.readResponse(isBad);

        // Test handleErrorResponseCode
        request.handleErrorResponseCode(404, "Not Found");

        // Test handleException
        request.handleException(new IOException("Fail"));
    }
}
