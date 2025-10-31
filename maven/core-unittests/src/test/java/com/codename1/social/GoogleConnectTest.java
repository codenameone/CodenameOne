package com.codename1.social;

import com.codename1.io.AccessToken;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class GoogleConnectTest extends UITestBase {

    private static final String PROFILE_URL = "https://www.googleapis.com/plus/v1/people/me";

    private GoogleConnect connect;

    @BeforeEach
    void setUpGoogleConnect() {
        implementation.clearConnections();
        clearStoredCredentials();
        connect = new GoogleConnect();
        connect.setClientId("client");
        connect.setClientSecret("secret");
        connect.setRedirectURI("redirect");
        connect.setScope("profile email");
    }

    @AfterEach
    void tearDownGoogleConnect() {
        clearStoredCredentials();
        implementation.clearConnections();
    }

    @FormTest
    void nativeLoginSupportIsDisabled() {
        assertFalse(connect.isNativeLoginSupported());
    }

    @FormTest
    void validateTokenReturnsFalseForUnauthorizedResponse() {
        TestConnection connection = implementation.createConnection(PROFILE_URL);
        connection.setResponseCode(401);
        connection.setResponseMessage("Unauthorized");

        boolean valid = connect.validateToken("invalid-token");

        assertFalse(valid);

        TestConnection performed = implementation.getConnection(PROFILE_URL);
        assertNotNull(performed);
        assertEquals("Bearer invalid-token", performed.getHeaders().get("Authorization"));
        assertFalse(performed.isPostRequest());
    }

    @FormTest
    void validateTokenReturnsTrueForSuccessfulResponse() {
        boolean valid = connect.validateToken("valid-token");

        assertTrue(valid);

        TestConnection performed = implementation.getConnection(PROFILE_URL);
        assertNotNull(performed);
        assertEquals("Bearer valid-token", performed.getHeaders().get("Authorization"));
        assertFalse(performed.isPostRequest());
    }

    @FormTest
    void setAccessTokenPersistsValueInStorage() {
        AccessToken token = new AccessToken("stored-token", null);
        connect.setAccessToken(token);

        assertNotNull(connect.getAccessToken());
        assertTrue(Storage.getInstance().storageFileExists(GoogleConnect.class.getName() + "AccessToken"));
        assertEquals("stored-token", connect.getAccessToken().getToken());
    }

    private void clearStoredCredentials() {
        Storage.getInstance().deleteStorageFile(GoogleConnect.class.getName() + "AccessToken");
        Preferences.delete(GoogleConnect.class.getName() + "Token");
    }
}
